package com.finresearch.web;

import com.finresearch.common.ApiResponse;
import com.finresearch.service.ProjectAccessService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/documents")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    private final ProjectAccessService projectAccessService;

    @GetMapping("/{type}/markdown")
    public ApiResponse<Map<String, String>> markdown(@PathVariable Long projectId, @PathVariable String type) {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        String content = switch (type) {
            case "requirements" -> requirementsMd(projectId);
            case "database-selection" -> dbSelectionMd();
            case "tech-stack" -> techStackMd();
            default -> throw new IllegalArgumentException("未知文档类型");
        };
        return ApiResponse.ok(Map.of("markdown", content));
    }

    @GetMapping("/{type}/download")
    public void download(@PathVariable Long projectId, @PathVariable String type, HttpServletResponse response)
            throws Exception {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        String content = switch (type) {
            case "requirements" -> requirementsMd(projectId);
            case "database-selection" -> dbSelectionMd();
            case "tech-stack" -> techStackMd();
            default -> throw new IllegalArgumentException("未知文档类型");
        };
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String filename = type + "-" + projectId + ".md";
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
        response.getOutputStream().write(content.getBytes(StandardCharsets.UTF_8));
    }

    private String requirementsMd(Long projectId) {
        return """
                # 金融科研 AI 辅助平台 — 需求说明书（MVP）

                **项目 ID**：%d

                ## 1. 目标用户
                高校与金融机构科研人员，聚焦金融学实证论文全流程。

                ## 2. 范围
                - 选题与文献解读、EDC 采集、数据清洗与基础统计、论文写作辅助、审稿回修辅助。
                - 数据脱敏后调用大模型；原始数据加密存储。

                ## 3. 非目标（后续版本）
                - CSMAR/Akshare 生产级直连与高可用 SLA
                - 超大规模面板分布式计算

                ## 4. 验收
                完成从登录 → 项目 → 选题 → 数据 → 写作 → 回修的闭环演示。
                """.formatted(projectId);
    }

    private String dbSelectionMd() {
        return """
                # 数据库选型说明（MVP）

                | 组件 | 选型 | 说明 |
                |------|------|------|
                | 业务库 | MySQL 8 | 事务、成熟生态 |
                | 文件 | 本地/卷加密 | MVP 可映射到对象存储 |
                | 缓存 | 可选 Redis | 二期限流与会话 |
                """;
    }

    private String techStackMd() {
        return """
                # 技术选型说明（MVP）

                - 前端：Vue 3 + TypeScript + Element Plus + Pinia + Vite
                - 后端：Spring Boot 3 + Java 17 + Spring Security + JPA
                - AI：Anthropic Claude Messages API（Skill 化提示词）
                - 部署：Docker Compose（MySQL + API + Nginx 静态站点）
                """;
    }
}
