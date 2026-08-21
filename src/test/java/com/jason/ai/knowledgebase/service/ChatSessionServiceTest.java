package com.jason.ai.knowledgebase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.memory.ChatMemory;

import com.jason.ai.knowledgebase.model.response.ApiResponse;
import com.jason.ai.knowledgebase.model.response.ChatResponses.SessionView;
import com.jason.ai.knowledgebase.repository.mapper.ChatSessionMapper;
import com.jason.ai.knowledgebase.repository.mapper.ConversationMessageMapper;
import com.jason.ai.knowledgebase.repository.projection.ChatSessionSummary;
import com.jason.ai.knowledgebase.common.util.SnowflakeIdGenerator;
import com.jason.ai.knowledgebase.service.converter.ChatResponseConverter;

@ExtendWith(MockitoExtension.class)
class ChatSessionServiceTest {

    @Mock
    private ChatSessionMapper sessionMapper;
    @Mock
    private ConversationMessageMapper messageMapper;
    @Mock
    private SnowflakeIdGenerator idGenerator;
    @Mock
    private ChatMemory chatMemory;
    @Mock
    private ActiveRequestLookup activeRequests;
    @Mock
    private ChatResponseConverter responseConverter;
    @InjectMocks
    private ChatSessionService service;

    @Test
    void listReturnsEverySessionWithLatestMessagePreview() {
        ChatSessionSummary first = summary(101L, "最近会话", "最近消息", Instant.parse("2026-08-18T02:00:00Z"));
        ChatSessionSummary second = summary(100L, "较早会话", null, Instant.parse("2026-08-18T01:00:00Z"));
        SessionView firstView = new SessionView(101L, "最近会话", "最近消息",
                first.getCreateTime(), first.getUpdateTime());
        SessionView secondView = new SessionView(100L, "较早会话", null,
                second.getCreateTime(), second.getUpdateTime());
        when(sessionMapper.findSummaries(7L)).thenReturn(List.of(first, second));
        when(responseConverter.toSessionView(first)).thenReturn(firstView);
        when(responseConverter.toSessionView(second)).thenReturn(secondView);

        ApiResponse<List<SessionView>> response = service.list(7L);

        assertThat(response.data()).isNotNull();
        assertThat(response.data()).extracting(SessionView::id).containsExactly(101L, 100L);
        assertThat(response.data()).extracting(SessionView::lastMessagePreview).containsExactly("最近消息", null);
        verify(sessionMapper).findSummaries(7L);
        verifyNoInteractions(messageMapper);
    }

    private ChatSessionSummary summary(long id, String title, String preview, Instant updateTime) {
        ChatSessionSummary summary = new ChatSessionSummary();
        summary.setId(id);
        summary.setTitle(title);
        summary.setLastMessagePreview(preview);
        summary.setCreateTime(updateTime.minusSeconds(60));
        summary.setUpdateTime(updateTime);
        return summary;
    }
}