package com.jason.ai.knowledgebase.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jason.ai.knowledgebase.model.response.ApiResponse;
import com.jason.ai.knowledgebase.model.response.PageResult;
import com.jason.ai.knowledgebase.model.response.PhraseResponses.PhraseView;
import com.jason.ai.knowledgebase.model.request.PhraseRequests.SaveRequest;
import com.jason.ai.knowledgebase.model.request.PhraseRequests.StatusRequest;
import com.jason.ai.knowledgebase.service.MeaninglessPhraseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 仅管理员可用的无意义短语管理接口。 */
@RestController
@RequestMapping("/api/admin/request-admission/meaningless-phrases")
@RequiredArgsConstructor
public class MeaninglessPhraseAdminController {

    private final MeaninglessPhraseService service;

    /**
     * 新增无意义短语。
     *
     * @param request 短语参数
     * @return 新短语 ID
     */
    @PostMapping("/create")
    public ApiResponse<Long> create(@Valid @RequestBody SaveRequest request) {
        return service.create(request);
    }

    /**
     * 更新无意义短语。
     *
     * @param id 短语 ID
     * @param request 更新参数
     * @return 空成功响应
     */
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody SaveRequest request) {
        service.update(id, request);
        return ApiResponse.success();
    }

    /**
     * 幂等更新短语启用状态。
     *
     * @param id 短语 ID
     * @param request 目标状态
     * @return 空成功响应
     */
    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        service.updateStatus(id, request.enabled());
        return ApiResponse.success();
    }

    /**
     * 查询单条短语。
     *
     * @param id 短语 ID
     * @return 短语信息
     */
    @GetMapping("/{id}")
    public ApiResponse<PhraseView> get(@PathVariable Long id) {
        return service.get(id);
    }

    /**
     * 按条件分页查询短语。
     *
     * @param phrase 可选短语
     * @param category 可选分类
     * @param enabled 可选状态
     * @param page 页码
     * @param size 每页数量
     * @return 短语分页
     */
    @GetMapping("/list")
    public ApiResponse<PageResult<PhraseView>> list(@RequestParam(required = false) String phrase,
            @RequestParam(required = false) String category, @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "1") Long page, @RequestParam(defaultValue = "20") Long size) {
        return service.list(phrase, category, enabled, page, size);
    }
}
