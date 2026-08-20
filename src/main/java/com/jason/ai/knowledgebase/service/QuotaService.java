package com.jason.ai.knowledgebase.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.jason.ai.knowledgebase.repository.mapper.UserQuotaMapper;
import com.jason.ai.knowledgebase.common.exception.AppException;
import com.jason.ai.knowledgebase.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/** 查询并原子扣减非重置型对话额度。 */
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final UserQuotaMapper mapper;

    /**
     * 查询用户当前可用额度。
     *
     * @param userId 用户 ID
     * @return 可用次数；额度记录不存在时返回 0
     */
    public int available(long userId) {
        Integer value = mapper.available(userId);
        return value == null ? 0 : value;
    }

    /**
     * 原子扣减一次额度并返回剩余次数。
     *
     * @param userId 用户 ID
     * @return 扣减后的可用次数
     * @throws AppException 额度不存在或已经耗尽时抛出
     */
    public int consume(long userId) {
        if (mapper.consume(userId, Instant.now()) != 1) {
            throw new AppException(ErrorCode.QUOTA_EXHAUSTED);
        }
        return available(userId);
    }
}