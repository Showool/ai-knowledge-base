package com.jason.ai.knowledgebase.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jason.ai.knowledgebase.model.response.ApiResponse;
import com.jason.ai.knowledgebase.model.response.QuotaResponses.QuotaView;
import com.jason.ai.knowledgebase.security.SecurityUtils;
import com.jason.ai.knowledgebase.service.QuotaService;
import lombok.RequiredArgsConstructor;

/** 查询当前用户的非重置型对话额度。 */
@RestController
@RequestMapping("/api/quota")
@RequiredArgsConstructor
public class QuotaController {

    private final QuotaService service;

    /**
     * 查询当前用户剩余额度。
     *
     * @return 剩余额度
     */
    @GetMapping("/get")
    public ApiResponse<QuotaView> get() {
        return service.available(SecurityUtils.currentUserId());
    }
}
