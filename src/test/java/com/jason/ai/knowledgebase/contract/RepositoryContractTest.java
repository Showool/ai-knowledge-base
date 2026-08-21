package com.jason.ai.knowledgebase.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class RepositoryContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void staticOpenApiDocumentIsValidAndUsesApiPrefix() throws IOException {
        JsonNode document = objectMapper.readTree(resource("openapi/ai-knowledge-base-openapi.json"));

        assertThat(document.path("openapi").asText()).startsWith("3.");
        assertThat(document.path("paths").fieldNames()).toIterable()
                .isNotEmpty()
                .allMatch(path -> path.startsWith("/api"));
        JsonNode sessionList = document.path("paths").path("/api/chat/sessions/list").path("get");
        assertThat(sessionList.has("parameters")).isFalse();

        JsonNode chatStream = document.path("paths").path("/api/chat/sessions/stream").path("post");
        assertThat(chatStream.has("parameters")).isFalse();
        assertThat(chatStream.path("requestBody").path("content").path("application/json")
                .path("schema").path("required"))
                .isEqualTo(objectMapper.readTree("[\"sessionId\",\"message\"]"));
        assertThat(document.path("paths").has("/api/chat/sessions/{sessionId}/stream")).isFalse();
        assertThat(chatStream.path("requestBody").path("content").path("application/json")
                .path("schema").path("properties").path("sessionId").path("type").asText())
                .isEqualTo("string");

        JsonNode paths = document.path("paths");
        JsonNode userList = paths.path("/api/admin/users/list").path("post");
        assertThat(userList.has("parameters")).isFalse();
        assertThat(userList.path("requestBody").path("content").path("application/json")
                .path("schema").path("$ref").asText())
                .isEqualTo("#/components/schemas/AdminUserPageRequest");
        assertThat(paths.has("/api/admin/users/{id}")).isTrue();
        JsonNode userStatus = paths.path("/api/admin/users/status").path("put");
        assertThat(userStatus.path("requestBody").path("content").path("application/json")
                .path("schema").path("$ref").asText())
                .isEqualTo("#/components/schemas/AdminUserStatusRequest");
        assertThat(paths.has("/api/admin/users/{id}/enable")).isFalse();
        assertThat(paths.has("/api/admin/users/{id}/disable")).isFalse();
        JsonNode userPageData = document.path("components").path("schemas")
                .path("AdminUserPageResponse").path("properties").path("data");
        assertThat(userPageData.path("required"))
                .isEqualTo(objectMapper.readTree("[\"items\",\"total\"]"));
        assertThat(userPageData.path("properties").fieldNames()).toIterable()
                .containsExactlyInAnyOrder("items", "total");
        JsonNode userListItemProperties = userPageData.path("properties")
                .path("items").path("items").path("properties");
        assertThat(userListItemProperties.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("id", "username", "role", "status", "createTime", "updateTime");
        assertThat(userListItemProperties.path("id").path("type").asText()).isEqualTo("string");
        assertThat(document.path("components").path("schemas").path("AdminUserResponse")
                .path("properties").path("data").path("properties").fieldNames()).toIterable()
                .containsExactlyInAnyOrder("username", "role", "status", "createTime", "updateTime");
        assertThat(resource("openapi/ai-knowledge-base-openapi.json"))
                .doesNotContain("\"format\": \"int64\"");
        JsonNode chatSseEvent = document.path("components").path("schemas").path("ChatSseEvent")
                .path("properties");
        assertThat(chatSseEvent.path("requestId").path("type").asText()).isEqualTo("string");
        assertThat(document.path("components").path("schemas").path("ChatSseEvent")
                .path("required").toString()).contains("\"requestId\"");
        assertThat(chatSseEvent.path("sessionId").path("type").asText()).isEqualTo("string");
        assertThat(chatSseEvent.path("sequence").path("type").asText()).isEqualTo("string");
        assertThat(chatSseEvent.path("assistantMessageId").path("type"))
                .isEqualTo(objectMapper.readTree("[\"string\",\"null\"]"));
    }

    @Test
    void schemaKeepsDeliberateTableAndColumnDecisions() throws IOException {
        String schema = resource("../../sql/init.sql");

        assertThat(schema).contains("CREATE TABLE IF NOT EXISTS sys_user")
                .contains("password_hash")
                .contains("metadata JSON")
                .doesNotContain("ai_chat_request")
                .doesNotContain("paired_message_id")
                .doesNotContain("title_initialized");
    }

    private String resource(String path) throws IOException {
        if (path.startsWith("../")) {
            return java.nio.file.Files.readString(java.nio.file.Path.of("sql/init.sql"), StandardCharsets.UTF_8);
        }
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}






