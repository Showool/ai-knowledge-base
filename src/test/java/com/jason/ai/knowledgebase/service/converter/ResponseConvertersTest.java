package com.jason.ai.knowledgebase.service.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.jason.ai.knowledgebase.common.util.JsonCodec;
import com.jason.ai.knowledgebase.model.entity.ChatSession;
import com.jason.ai.knowledgebase.model.entity.ConversationMessage;
import com.jason.ai.knowledgebase.model.entity.MeaninglessPhrase;
import com.jason.ai.knowledgebase.model.entity.SysUser;
import com.jason.ai.knowledgebase.model.response.AdminResponses.UserListItem;
import com.jason.ai.knowledgebase.model.response.ChatResponses.CreateSessionResponse;
import com.jason.ai.knowledgebase.model.response.ChatResponses.MessageView;
import com.jason.ai.knowledgebase.model.response.ChatResponses.SessionView;
import com.jason.ai.knowledgebase.model.response.PhraseResponses.PhraseView;
import com.jason.ai.knowledgebase.repository.projection.ChatSessionSummary;

import tools.jackson.databind.json.JsonMapper;

class ResponseConvertersTest {

    private final AdminUserResponseConverter adminConverter = new AdminUserResponseConverterImpl();
    private final AuthResponseConverter authConverter = new AuthResponseConverterImpl();
    private final PhraseResponseConverter phraseConverter = new PhraseResponseConverterImpl();
    private final ChatResponseConverter chatConverter = new ChatResponseConverterImpl(
            new DatabaseJsonConverter(new JsonCodec(JsonMapper.builder().build())));

    @Test
    void userConvertersExposeOnlyTheirResponseFields() {
        SysUser user = new SysUser();
        user.setId(42L);
        user.setUsername("alice");
        user.setPasswordHash("must-not-be-exposed");
        user.setRole("USER");
        user.setStatus("ENABLED");
        user.setCreateTime(Instant.parse("2026-08-18T01:00:00Z"));
        user.setUpdateTime(Instant.parse("2026-08-18T02:00:00Z"));

        UserListItem listItem = adminConverter.toListItem(user);

        assertThat(listItem.id()).isEqualTo(42L);
        assertThat(listItem.username()).isEqualTo("alice");
        assertThat(adminConverter.toView(user).status()).isEqualTo("ENABLED");
        assertThat(authConverter.toUserView(user).id()).isEqualTo(42L);
    }

    @Test
    void phraseConverterDefaultsNullableDatabasePrimitives() {
        MeaninglessPhrase phrase = new MeaninglessPhrase();
        phrase.setId(7L);
        phrase.setPhrase("test");
        phrase.setEnabled(null);
        phrase.setPriority(null);

        PhraseView view = phraseConverter.toView(phrase);

        assertThat(view.enabled()).isFalse();
        assertThat(view.priority()).isZero();
    }

    @Test
    void chatConverterMapsCreateResponseAndTruncatesPreviewByCodePoint() {
        Instant now = Instant.parse("2026-08-18T02:00:00Z");
        ChatSession session = new ChatSession();
        session.setId(101L);
        session.setTitle("会话");
        session.setCreateTime(now);
        ChatSessionSummary summary = new ChatSessionSummary();
        summary.setId(101L);
        summary.setTitle("会话");
        summary.setLastMessagePreview("😀".repeat(81));
        summary.setCreateTime(now.minusSeconds(60));
        summary.setUpdateTime(now);

        CreateSessionResponse created = chatConverter.toCreateResponse(session);
        SessionView view = chatConverter.toSessionView(summary);

        assertThat(created).isEqualTo(new CreateSessionResponse(101L, "会话", now));
        assertThat(view.lastMessagePreview()).isEqualTo("😀".repeat(80));
        assertThat(view.lastMessagePreview().codePointCount(0, view.lastMessagePreview().length())).isEqualTo(80);
    }

    @Test
    @SuppressWarnings("unchecked")
    void chatConverterParsesMessageMetadataJson() {
        ConversationMessage message = new ConversationMessage();
        message.setId(201L);
        message.setRequestId(301L);
        message.setRole("ASSISTANT");
        message.setContent("answer");
        message.setStatus("GENERATED");
        message.setMetadata("{\"model\":\"gpt-test\",\"version\":1}");

        MessageView view = chatConverter.toMessageView(message);

        assertThat((Map<String, Object>) view.metadata())
                .containsEntry("model", "gpt-test")
                .containsEntry("version", 1);
    }

    @Test
    void convertersReturnNullForNullSources() {
        assertThat(adminConverter.toListItem(null)).isNull();
        assertThat(adminConverter.toView(null)).isNull();
        assertThat(authConverter.toUserView(null)).isNull();
        assertThat(phraseConverter.toView(null)).isNull();
        assertThat(chatConverter.toCreateResponse(null)).isNull();
        assertThat(chatConverter.toSessionView(null)).isNull();
        assertThat(chatConverter.toMessageView(null)).isNull();
    }
}
