package com.jason.ai.knowledgebase.common.api;

import java.util.List;

/** 通用分页响应。 */
public record PageResult<T>(Long page, Long size, Long total, List<T> items) {
}
