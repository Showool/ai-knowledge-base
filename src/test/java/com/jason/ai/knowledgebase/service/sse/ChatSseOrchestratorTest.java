package com.jason.ai.knowledgebase.service.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;

import com.jason.ai.knowledgebase.model.internal.NormalizedInput;
import com.jason.ai.knowledgebase.service.RequestAdmissionService;
import com.jason.ai.knowledgebase.repository.cache.ChatUserLockStore;
import com.jason.ai.knowledgebase.repository.cache.ChatUserLockStore.LockToken;
import com.jason.ai.knowledgebase.model.internal.AdmissionReceipt;
import com.jason.ai.knowledgebase.model.response.ChatSseEvent;
import com.jason.ai.knowledgebase.model.enums.ChatCloseReason;
import com.jason.ai.knowledgebase.model.enums.ChatEventType;
import com.jason.ai.knowledgebase.model.enums.ChatFailureCode;
import com.jason.ai.knowledgebase.model.enums.MessageStatus;
import com.jason.ai.knowledgebase.service.ChatPersistenceService;
import com.jason.ai.knowledgebase.service.ChatSessionService;
import com.jason.ai.knowledgebase.common.exception.AppException;
import com.jason.ai.knowledgebase.common.exception.ErrorCode;
import com.jason.ai.knowledgebase.config.ChatProperties;
import com.jason.ai.knowledgebase.common.util.SnowflakeIdGenerator;

import reactor.core.publisher.Flux;

class ChatSseOrchestratorTest {

    private static final long REQUEST_ID = 9001L;
    private static final long USER_ID = 7L;
    private static final long SESSION_ID = 31L;
    private static final LockToken LOCK_TOKEN = new LockToken("akb:chat:user-lock:7", "token");

    private ChatSessionService sessionService;
    private RequestAdmissionService admissionService;
    private SseConnectionRegistry registry;
    private ChatGenerationQueue queue;
    private ChatPersistenceService persistence;
    private ChatLifecycleService lifecycle;
    private ChatUserLockStore userLockStore;
    private SnowflakeIdGenerator idGenerator;
    private ChatSseOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        sessionService = mock(ChatSessionService.class);
        admissionService = mock(RequestAdmissionService.class);
        registry = mock(SseConnectionRegistry.class);
        queue = mock(ChatGenerationQueue.class);
        persistence = mock(ChatPersistenceService.class);
        lifecycle = mock(ChatLifecycleService.class);
        userLockStore = mock(ChatUserLockStore.class);
        idGenerator = mock(SnowflakeIdGenerator.class);
        ChatProperties properties = new ChatProperties();
        orchestrator = new ChatSseOrchestrator(sessionService, admissionService, registry, queue, persistence,
                lifecycle, userLockStore, properties, idGenerator);
        when(idGenerator.nextId()).thenReturn(REQUEST_ID);
        when(admissionService.evaluate("hello")).thenReturn(new NormalizedInput("hello", "hello", "hello"));
        when(registry.reserveCapacity()).thenReturn(true);
        when(queue.reserveSlot()).thenReturn(true);
        when(userLockStore.tryAcquire(USER_ID)).thenReturn(LOCK_TOKEN);
    }

    @AfterEach
    void tearDown() {
        orchestrator.stopTimeoutExecutor();
    }

    @Test
    void quotaExhaustionReturnsFailedEventWithRequestIdAndReleasesResources() {
        when(persistence.admit(USER_ID, SESSION_ID, REQUEST_ID, "hello", "hello"))
                .thenThrow(new AppException(ErrorCode.QUOTA_EXHAUSTED));

        ChatSseEvent event = failedEvent(orchestrator.start(USER_ID, SESSION_ID, "hello"));

        assertThat(event.requestId()).isEqualTo(REQUEST_ID);
        assertThat(event.sessionId()).isEqualTo(SESSION_ID);
        assertThat(event.remainingQuota()).isZero();
        assertThat(event.closeReason()).isEqualTo("QUOTA_EXHAUSTED");
        assertThat(event.errorCode()).isEqualTo("QUOTA_EXHAUSTED");
        assertThat(event.errorMessage()).isEqualTo("可用额度不足");
        verify(queue).releaseReservation();
        verify(registry).releaseCapacity();
        verify(userLockStore).release(LOCK_TOKEN);
    }

    @Test
    void businessValidationExceptionReturnsSingleFailedEventWithRequestId() {
        when(sessionService.requireOwned(USER_ID, SESSION_ID))
                .thenThrow(new AppException(ErrorCode.NOT_FOUND));

        ChatSseEvent event = failedEvent(orchestrator.start(USER_ID, SESSION_ID, "hello"));

        assertThat(event.requestId()).isEqualTo(REQUEST_ID);
        assertThat(event.closeReason()).isEqualTo("NOT_FOUND");
        assertThat(event.errorCode()).isEqualTo("NOT_FOUND");
        verifyNoInteractions(registry, queue, persistence, userLockStore);
    }

    @Test
    void capacityRejectionReturnsSingleFailedEventWithRequestId() {
        when(registry.reserveCapacity()).thenReturn(false);

        ChatSseEvent event = failedEvent(orchestrator.start(USER_ID, SESSION_ID, "hello"));

        assertThat(event.requestId()).isEqualTo(REQUEST_ID);
        assertThat(event.closeReason()).isEqualTo("CAPACITY_FULL");
        assertThat(event.errorCode()).isEqualTo("CAPACITY_FULL");
    }

    @Test
    void dependencyFailureReturnsSingleFailedEventAndReleasesReservations() {
        when(userLockStore.tryAcquire(USER_ID)).thenThrow(new IllegalStateException("redis unavailable"));

        ChatSseEvent event = failedEvent(orchestrator.start(USER_ID, SESSION_ID, "hello"));

        assertThat(event.requestId()).isEqualTo(REQUEST_ID);
        assertThat(event.closeReason()).isEqualTo("DEPENDENCY_UNAVAILABLE");
        assertThat(event.errorCode()).isEqualTo("DEPENDENCY_UNAVAILABLE");
        verify(queue).releaseReservation();
        verify(registry).releaseCapacity();
    }

    @Test
    void unexpectedAdmissionExceptionReturnsSingleInternalFailureWithRequestId() {
        when(admissionService.evaluate("hello")).thenThrow(new IllegalStateException("unexpected"));

        ChatSseEvent event = failedEvent(orchestrator.start(USER_ID, SESSION_ID, "hello"));

        assertThat(event.requestId()).isEqualTo(REQUEST_ID);
        assertThat(event.closeReason()).isEqualTo("INTERNAL_ERROR");
        assertThat(event.errorCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(event.errorMessage()).isEqualTo("服务内部错误");
        verifyNoInteractions(registry, queue, persistence, userLockStore);
    }

    @Test
    void enqueueExceptionReturnsTheConnectionFailedEventInsteadOfThrowing() {
        when(persistence.admit(USER_ID, SESSION_ID, REQUEST_ID, "hello", "hello"))
                .thenReturn(new AdmissionReceipt(REQUEST_ID, 101L, 102L, 8));
        doThrow(new IllegalStateException("enqueue failed")).when(queue).submitReserved(any(GenerationTask.class));
        doAnswer(invocation -> {
            SseConnection connection = invocation.getArgument(0);
            connection.emit(ChatEventType.FAILED, null, MessageStatus.FAILED, ChatCloseReason.ENQUEUE_FAILED,
                    ChatFailureCode.INTERNAL_ERROR, "服务内部错误");
            return null;
        }).when(lifecycle).admissionFailed(any(SseConnection.class), any(RuntimeException.class));

        ChatSseEvent event = failedEvent(orchestrator.start(USER_ID, SESSION_ID, "hello"));

        assertThat(event.requestId()).isEqualTo(REQUEST_ID);
        assertThat(event.assistantMessageId()).isEqualTo(102L);
        assertThat(event.closeReason()).isEqualTo("ENQUEUE_FAILED");
        assertThat(event.errorCode()).isEqualTo("INTERNAL_ERROR");
        verify(lifecycle).admissionFailed(any(SseConnection.class), any(RuntimeException.class));
    }

    @Test
    void successfulAdmissionPersistsOriginalMessageAndPublishesRequestId() {
        when(admissionService.evaluate("ｈｅｌｌｏ")).thenReturn(new NormalizedInput("ｈｅｌｌｏ", "hello", "hello"));
        when(persistence.admit(USER_ID, SESSION_ID, REQUEST_ID, "ｈｅｌｌｏ", "hello"))
                .thenReturn(new AdmissionReceipt(REQUEST_ID, 101L, 102L, 8));

        ServerSentEvent<ChatSseEvent> serverEvent = orchestrator.start(USER_ID, SESSION_ID, "ｈｅｌｌｏ")
                .blockFirst(Duration.ofSeconds(1));

        assertThat(serverEvent).isNotNull();
        assertThat(serverEvent.data()).isNotNull();
        assertThat(serverEvent.data().event()).isEqualTo(ChatEventType.QUEUED);
        assertThat(serverEvent.data().requestId()).isEqualTo(REQUEST_ID);
        verify(persistence).admit(USER_ID, SESSION_ID, REQUEST_ID, "ｈｅｌｌｏ", "hello");
        verify(queue).submitReserved(any(GenerationTask.class));
    }

    private ChatSseEvent failedEvent(Flux<ServerSentEvent<ChatSseEvent>> flux) {
        List<ServerSentEvent<ChatSseEvent>> events = flux.collectList().block(Duration.ofSeconds(1));
        assertThat(events).isNotNull().hasSize(1);
        ServerSentEvent<ChatSseEvent> serverEvent = events.getFirst();
        assertThat(serverEvent.id()).isEqualTo(REQUEST_ID + ":1");
        assertThat(serverEvent.event()).isEqualTo(ChatEventType.FAILED.wireValue());
        assertThat(serverEvent.data()).isNotNull();
        assertThat(serverEvent.data().event()).isEqualTo(ChatEventType.FAILED);
        assertThat(serverEvent.data().sequence()).isEqualTo(1L);
        assertThat(serverEvent.data().messageStatus()).isEqualTo("FAILED");
        return serverEvent.data();
    }
}
