package com.jason.ai.knowledgebase.common.util;

/**
 * 已完成默认值和上下界处理的分页参数。
 *
 * @param page 页码，从 1 开始
 * @param size 每页数量
 */
public record PageBounds(long page, long size) {

    /**
     * 规范化可空分页参数。
     *
     * @param page 请求页码
     * @param size 请求每页数量
     * @param defaultSize 默认每页数量
     * @param maximumSize 允许的最大每页数量
     * @return 安全的分页参数
     */
    public static PageBounds of(Long page, Long size, long defaultSize, long maximumSize) {
        long safePage = page == null ? 1 : Math.max(1, page);
        long requestedSize = size == null ? defaultSize : size;
        long safeSize = Math.min(maximumSize, Math.max(1, requestedSize));
        return new PageBounds(safePage, safeSize);
    }

    /**
     * 规范化不可空分页参数。
     *
     * @param page 请求页码
     * @param size 请求每页数量
     * @param maximumSize 允许的最大每页数量
     * @return 安全的分页参数
     */
    public static PageBounds of(long page, long size, long maximumSize) {
        return new PageBounds(Math.max(1, page), Math.min(maximumSize, Math.max(1, size)));
    }
}
