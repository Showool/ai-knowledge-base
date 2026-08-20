package com.jason.ai.knowledgebase.service;

import com.jason.ai.knowledgebase.model.internal.NormalizedInput;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Component;

/** 统一 Unicode、空白、重复标点与比较大小写。 */
@Component
public class RequestInputNormalizer {

    /**
     * 将原始输入转换为展示文本和稳定比较文本。
     *
     * @param input 原始输入
     * @return 同时保留原文、规范化文本和比较文本的对象
     */
    public NormalizedInput normalize(String input) {
        String compatibility = Normalizer.normalize(input, Normalizer.Form.NFKC);
        String normalized = normalizeCharacters(compatibility);
        String comparable = stripBoundaryPunctuation(normalized).toLowerCase(Locale.ROOT);
        return new NormalizedInput(input, normalized, comparable);
    }

    /**
     * 合并 Unicode 空白并去除重复标点，同时保留 URL 协议中的双斜线。
     *
     * @param input 已完成 NFKC 处理的输入
     * @return 字符级规范化结果
     */
    private String normalizeCharacters(String input) {
        StringBuilder result = new StringBuilder(input.length());
        boolean pendingWhitespace = false;
        int previous = -1;
        for (int offset = 0; offset < input.length();) {
            int codePoint = input.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (Character.isWhitespace(codePoint)) {
                pendingWhitespace = result.length() > 0;
                previous = -1;
                continue;
            }
            if (pendingWhitespace) {
                result.append(' ');
                pendingWhitespace = false;
            }
            if (isPunctuation(codePoint) && previous == codePoint && !isSchemeSecondSlash(result, codePoint)) {
                continue;
            }
            result.appendCodePoint(codePoint);
            previous = codePoint;
        }
        return result.toString();
    }

    private boolean isSchemeSecondSlash(StringBuilder result, int codePoint) {
        int length = result.length();
        return codePoint == '/' && length >= 2 && result.charAt(length - 2) == ':'
                && result.charAt(length - 1) == '/';
    }

    /**
     * 按 Unicode code point 移除首尾标点，避免拆分代理字符。
     *
     * @param input 规范化文本
     * @return 去除边界标点后的文本
     */
    private String stripBoundaryPunctuation(String input) {
        int start = 0;
        int end = input.length();
        while (start < end && isPunctuation(input.codePointAt(start))) {
            start += Character.charCount(input.codePointAt(start));
        }
        while (end > start && isPunctuation(input.codePointBefore(end))) {
            end -= Character.charCount(input.codePointBefore(end));
        }
        return input.substring(start, end).trim();
    }

    private boolean isPunctuation(int codePoint) {
        return switch (Character.getType(codePoint)) {
            case Character.CONNECTOR_PUNCTUATION, Character.DASH_PUNCTUATION,
                    Character.START_PUNCTUATION, Character.END_PUNCTUATION,
                    Character.INITIAL_QUOTE_PUNCTUATION, Character.FINAL_QUOTE_PUNCTUATION,
                    Character.OTHER_PUNCTUATION -> true;
            default -> false;
        };
    }
}
