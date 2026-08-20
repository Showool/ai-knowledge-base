package com.jason.ai.knowledgebase.common.util;

/**
 * Unicode 文本处理工具。
 */
public final class UnicodeText {

    private UnicodeText() {
    }

    /**
     * 按 Unicode code point 截取文本，避免截断代理字符对。
     *
     * @param value 原始文本
     * @param maximum 最大 code point 数
     * @return 不超过指定长度的文本；原值为空时返回空
     * @throws IllegalArgumentException 最大长度小于 0 时抛出
     */
    public static String truncate(String value, int maximum) {
        if (value == null) {
            return null;
        }
        if (maximum < 0) {
            throw new IllegalArgumentException("最大长度不能为负数");
        }
        int count = value.codePointCount(0, value.length());
        return count <= maximum ? value : value.substring(0, value.offsetByCodePoints(0, maximum));
    }
}
