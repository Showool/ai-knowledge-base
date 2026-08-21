package com.jason.ai.knowledgebase.model.response;

import java.util.List;

/** 通用分页响应。 */
public record PageResult<T>(List<T> items, Long total) {
}
