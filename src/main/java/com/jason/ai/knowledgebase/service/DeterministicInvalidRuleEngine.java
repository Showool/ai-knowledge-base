package com.jason.ai.knowledgebase.service;

import com.jason.ai.knowledgebase.model.internal.NormalizedInput;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import com.jason.ai.knowledgebase.model.enums.AdmissionRejectReason;

import org.springframework.stereotype.Component;

/** 在访问 Redis 和扣减额度前执行的确定性拦截规则。 */
@Component
public class DeterministicInvalidRuleEngine {

    private static final Pattern HTTP_URL = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);
    private static final Set<String> PLACEHOLDERS = Set.of("null", "undefined", "n/a", "tbd", "xxx");

    /**
     * 按固定顺序执行无需外部依赖的拒绝规则。
     *
     * @param input 已规范化输入
     * @return 首个命中的拒绝原因；未命中时为空
     */
    public Optional<AdmissionRejectReason> evaluate(NormalizedInput input) {
        String text = input.normalized();
        if (text.isBlank()) {
            return Optional.of(AdmissionRejectReason.EMPTY);
        }
        if (isControlCharacters(text)) {
            return Optional.of(AdmissionRejectReason.CONTROL_CHARACTERS);
        }
        if (HTTP_URL.matcher(text).matches()) {
            return Optional.of(AdmissionRejectReason.URL_ONLY);
        }
        if (isPureEmoji(input.original())) {
            return Optional.of(AdmissionRejectReason.PURE_EMOJI);
        }
        if (isPureNumber(text)) {
            return Optional.of(AdmissionRejectReason.PURE_NUMBER);
        }
        if (isPurePunctuation(text)) {
            return Optional.of(AdmissionRejectReason.PURE_PUNCTUATION);
        }
        if (isGarbled(text)) {
            return Optional.of(AdmissionRejectReason.GARBLED_TEXT);
        }
        if (isRepeatedCharacters(input.comparable())) {
            return Optional.of(AdmissionRejectReason.REPEATED_CHARACTERS);
        }
        if (PLACEHOLDERS.contains(input.comparable())) {
            return Optional.of(AdmissionRejectReason.PLACEHOLDER);
        }
        return Optional.empty();
    }

    private boolean isControlCharacters(String text) {
        int count = 0;
        for (int codePoint : text.codePoints().toArray()) {
            if (Character.isWhitespace(codePoint)) {
                continue;
            }
            count++;
            int type = Character.getType(codePoint);
            if (type != Character.CONTROL && type != Character.FORMAT) {
                return false;
            }
        }
        return count > 0;
    }

    /**
     * 按 Unicode emoji 与 emoji component 属性判断是否仅含表情。
     *
     * @param text 原始文本
     * @return 仅含表情和空白时返回 true
     */
    private boolean isPureEmoji(String text) {
        boolean found = false;
        for (int codePoint : text.codePoints().toArray()) {
            if (Character.isWhitespace(codePoint) || Character.isEmojiComponent(codePoint)
                    || codePoint == 0xFE0E || codePoint == 0xFE0F) {
                continue;
            }
            if (!Character.isEmoji(codePoint)) {
                return false;
            }
            found = true;
        }
        return found;
    }

    private boolean isPureNumber(String text) {
        boolean digit = false;
        for (int codePoint : text.codePoints().toArray()) {
            if (Character.isWhitespace(codePoint)) {
                continue;
            }
            if (Character.isDigit(codePoint)) {
                digit = true;
            } else if ("+-.,/%:".indexOf(codePoint) < 0) {
                return false;
            }
        }
        return digit;
    }

    private boolean isPurePunctuation(String text) {
        int count = 0;
        for (int codePoint : text.codePoints().toArray()) {
            if (Character.isWhitespace(codePoint)) {
                continue;
            }
            count++;
            int type = Character.getType(codePoint);
            if (!isPunctuation(type) && !isSymbol(type)) {
                return false;
            }
        }
        return count > 0;
    }

    private boolean isPunctuation(int type) {
        return type == Character.CONNECTOR_PUNCTUATION
                || type == Character.DASH_PUNCTUATION
                || type == Character.START_PUNCTUATION
                || type == Character.END_PUNCTUATION
                || type == Character.INITIAL_QUOTE_PUNCTUATION
                || type == Character.FINAL_QUOTE_PUNCTUATION
                || type == Character.OTHER_PUNCTUATION;
    }

    private boolean isSymbol(int type) {
        return type == Character.MATH_SYMBOL
                || type == Character.CURRENCY_SYMBOL
                || type == Character.MODIFIER_SYMBOL
                || type == Character.OTHER_SYMBOL;
    }

    /**
     * 当替换字符占非空白字符至少一半时判定为乱码。
     *
     * @param text 规范化文本
     * @return 达到乱码阈值时返回 true
     */
    private boolean isGarbled(String text) {
        long count = text.codePoints().filter(codePoint -> !Character.isWhitespace(codePoint)).count();
        long replacements = text.codePoints().filter(codePoint -> codePoint == 0xFFFD).count();
        return count > 0 && replacements * 2 >= count;
    }

    /**
     * 判断是否至少八个非空白 code point 完全相同。
     *
     * @param text 比较文本
     * @return 命中重复字符规则时返回 true
     */
    private boolean isRepeatedCharacters(String text) {
        int[] points = text.codePoints().filter(codePoint -> !Character.isWhitespace(codePoint)).toArray();
        if (points.length < 8) {
            return false;
        }
        for (int point : points) {
            if (point != points[0]) {
                return false;
            }
        }
        return true;
    }
}
