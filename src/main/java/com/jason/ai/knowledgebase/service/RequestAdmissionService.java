package com.jason.ai.knowledgebase.service;

import org.springframework.stereotype.Service;

import com.jason.ai.knowledgebase.repository.cache.MeaninglessPhraseCache;
import com.jason.ai.knowledgebase.model.internal.NormalizedInput;
import com.jason.ai.knowledgebase.common.exception.AppException;
import com.jason.ai.knowledgebase.common.exception.ErrorCode;
import com.jason.ai.knowledgebase.config.AdmissionProperties;
import com.jason.ai.knowledgebase.config.ChatInputProperties;

import lombok.RequiredArgsConstructor;

/**
 * 在占用稀缺资源和扣减额度前规范化并校验请求。
 */
@Service
@RequiredArgsConstructor
public class RequestAdmissionService {

    private final RequestInputNormalizer normalizer;
    private final DeterministicInvalidRuleEngine ruleEngine;
    private final MeaninglessPhraseCache phraseCache;
    private final ChatInputProperties inputProperties;
    private final AdmissionProperties admissionProperties;

    /**
     * 规范化并校验用户问题。
     *
     * @param message 用户提交的原始问题
     * @return 同时保留原文、规范化文本和比较文本的输入
     * @throws AppException 消息过长、命中拒绝规则或依赖不可用时抛出
     */
    public NormalizedInput evaluate(String message) {
        NormalizedInput normalized = normalizer.normalize(message);
        if (normalized.normalized().codePointCount(0, normalized.normalized().length())
                > inputProperties.getMessageMaxLength()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, "消息长度超过配置上限");
        }
        if (!admissionProperties.isEnabled()) {
            return normalized;
        }
        ruleEngine.evaluate(normalized).ifPresent(reason -> {
            throw new AppException(ErrorCode.MESSAGE_REJECTED, "请求内容被规则拦截: " + reason.name());
        });
        try {
            if (phraseCache.contains(normalized.comparable())) {
                throw new AppException(ErrorCode.MESSAGE_REJECTED, "请求内容属于无意义短语");
            }
        } catch (AppException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            if (!admissionProperties.isFailOpen()) {
                throw new AppException(ErrorCode.DEPENDENCY_UNAVAILABLE);
            }
        }
        return normalized;
    }
}
