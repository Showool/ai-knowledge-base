package com.jason.ai.knowledgebase.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jason.ai.knowledgebase.model.response.PhraseResponses.PhraseView;
import com.jason.ai.knowledgebase.model.request.PhraseRequests.SaveRequest;
import com.jason.ai.knowledgebase.model.entity.MeaninglessPhrase;
import com.jason.ai.knowledgebase.model.event.MeaninglessPhraseChangedEvent;
import com.jason.ai.knowledgebase.repository.mapper.MeaninglessPhraseMapper;
import com.jason.ai.knowledgebase.common.api.PageResult;
import com.jason.ai.knowledgebase.common.exception.AppException;
import com.jason.ai.knowledgebase.common.exception.ErrorCode;
import com.jason.ai.knowledgebase.common.util.PageBounds;
import com.jason.ai.knowledgebase.common.util.SnowflakeIdGenerator;

import lombok.RequiredArgsConstructor;

/** 管理用于请求拦截的无意义短语，并在事务提交后触发缓存刷新。 */
@Service
@RequiredArgsConstructor
public class MeaninglessPhraseService {

    private static final long MAXIMUM_PAGE_SIZE = 100;

    private final MeaninglessPhraseMapper mapper;
    private final RequestInputNormalizer normalizer;
    private final SnowflakeIdGenerator idGenerator;
    private final ApplicationEventPublisher publisher;

    /**
     * 新增并规范化一条无意义短语。
     *
     * @param request 新增参数
     * @return 新短语 ID
     * @throws AppException 短语为空或重复时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public long create(SaveRequest request) {
        String phrase = normalizedPhrase(request.phrase());
        ensureUnique(phrase);
        MeaninglessPhrase entity = new MeaninglessPhrase();
        entity.setId(idGenerator.nextId());
        apply(entity, request, phrase);
        entity.setEnabled(true);
        entity.setDeleted(0);
        try {
            mapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw new AppException(ErrorCode.CONFLICT, "短语已存在");
        }
        changed();
        return entity.getId();
    }

    /**
     * 更新指定短语内容和分类信息。
     *
     * @param id 短语 ID
     * @param request 更新参数
     * @throws AppException 短语不存在、为空或与其他记录重复时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(long id, SaveRequest request) {
        MeaninglessPhrase entity = require(id);
        String phrase = normalizedPhrase(request.phrase());
        if (!phrase.equals(entity.getPhrase())) {
            ensureUnique(phrase);
        }
        apply(entity, request, phrase);
        mapper.updateById(entity);
        changed();
    }

    /**
     * 幂等更新短语启用状态。
     *
     * @param id 短语 ID
     * @param enabled 是否启用
     * @throws AppException 短语不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(long id, boolean enabled) {
        MeaninglessPhrase entity = require(id);
        if (enabled == Boolean.TRUE.equals(entity.getEnabled())) {
            return;
        }
        entity.setEnabled(enabled);
        mapper.updateById(entity);
        changed();
    }

    /**
     * 查询单条短语。
     *
     * @param id 短语 ID
     * @return 短语视图
     * @throws AppException 短语不存在时抛出
     */
    public PhraseView get(long id) {
        return view(require(id));
    }

    /**
     * 按短语、分类和状态分页查询。
     *
     * @param phrase 可选短语条件
     * @param category 可选分类条件
     * @param enabled 可选启用状态
     * @param page 页码
     * @param size 每页数量
     * @return 短语分页
     */
    public PageResult<PhraseView> list(String phrase, String category, Boolean enabled, long page, long size) {
        PageBounds bounds = PageBounds.of(page, size, MAXIMUM_PAGE_SIZE);
        Page<MeaninglessPhrase> result = mapper.selectPage(new Page<>(bounds.page(), bounds.size()),
                Wrappers.<MeaninglessPhrase>lambdaQuery()
                        .like(phrase != null && !phrase.isBlank(), MeaninglessPhrase::getPhrase, phrase)
                        .eq(category != null && !category.isBlank(), MeaninglessPhrase::getCategory, category)
                        .eq(enabled != null, MeaninglessPhrase::getEnabled, enabled)
                        .orderByDesc(MeaninglessPhrase::getPriority).orderByAsc(MeaninglessPhrase::getId));
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(),
                result.getRecords().stream().map(this::view).toList());
    }

    private MeaninglessPhrase require(long id) {
        MeaninglessPhrase entity = mapper.selectById(id);
        if (entity == null) {
            throw new AppException(ErrorCode.NOT_FOUND);
        }
        return entity;
    }

    private void apply(MeaninglessPhrase entity, SaveRequest request, String phrase) {
        entity.setPhrase(phrase);
        entity.setCategory(request.category());
        entity.setPriority(request.priority() == null ? 0 : request.priority());
        entity.setRemark(request.remark());
    }

    private String normalizedPhrase(String value) {
        String phrase = normalizer.normalize(value).comparable();
        if (phrase.isBlank()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT);
        }
        return phrase;
    }

    private void ensureUnique(String phrase) {
        if (mapper.countByPhrase(phrase) > 0) {
            throw new AppException(ErrorCode.CONFLICT, "短语已存在");
        }
    }

    private PhraseView view(MeaninglessPhrase entity) {
        return new PhraseView(entity.getId(), entity.getPhrase(), entity.getCategory(),
                Boolean.TRUE.equals(entity.getEnabled()), entity.getPriority(), entity.getRemark(),
                entity.getCreateTime(), entity.getUpdateTime());
    }

    /** 发布事务提交后刷新缓存所需的领域事件。 */
    private void changed() {
        publisher.publishEvent(new MeaninglessPhraseChangedEvent());
    }
}