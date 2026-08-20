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

import com.jason.ai.knowledgebase.model.response.ChatResponses.SessionView;
import com.jason.ai.knowledgebase.repository.mapper.ChatSessionMapper;
import com.jason.ai.knowledgebase.repository.mapper.ConversationMessageMapper;
import com.jason.ai.knowledgebase.repository.projection.ChatSessionSummary;
import com.jason.ai.knowledgebase.common.util.SnowflakeIdGenerator;
import com.jason.ai.knowledgebase.common.util.JsonCodec;

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
    private JsonCodec jsonCodec;
    @InjectMocks
    private ChatSessionService service;

    @Test
    void listReturnsEverySessionWithLatestMessagePreview() {
        ChatSessionSummary first = summary(101L, "最近会话", "最近消息", Instant.parse("2026-08-18T02:00:00Z"));
        ChatSessionSummary second = summary(100L, "较早会话", null, Instant.parse("2026-08-18T01:00:00Z"));
        when(sessionMapper.findSummaries(7L)).thenReturn(List.of(first, second));

        List<SessionView> result = service.list(7L);

        assertThat(result).extracting(SessionView::id).containsExactly(101L, 100L);
        assertThat(result).extracting(SessionView::lastMessagePreview).containsExactly("最近消息", null);
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