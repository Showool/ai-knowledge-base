package com.jason.ai.knowledgebase.common.util;

import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

/** 数据库 JSON 字段的统一编解码器。 */
@Component
public class JsonCodec {

    private final ObjectMapper objectMapper;

    public JsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将对象序列化为 JSON。
     *
     * @param value 待序列化对象
     * @return JSON 字符串；输入为 null 时返回 null
     */
    public String write(Object value) {
        return value == null ? null : objectMapper.writeValueAsString(value);
    }

    /**
     * 将数据库 JSON 解析为通用对象结构。
     *
     * @param json JSON 字符串
     * @return Map、List 或标量；空值返回 null
     */
    public Object readObject(String json) {
        return json == null || json.isBlank() ? null : objectMapper.readValue(json, Object.class);
    }
}