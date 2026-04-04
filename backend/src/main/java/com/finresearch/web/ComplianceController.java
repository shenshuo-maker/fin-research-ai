package com.finresearch.web;

import com.finresearch.common.ApiResponse;
import com.finresearch.domain.AuditLog;
import com.finresearch.domain.DesensitizationLog;
import com.finresearch.service.AuditService;
import com.finresearch.service.DesensitizationService;
import com.finresearch.repository.DesensitizationLogRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/compliance")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ComplianceController {

    private final DesensitizationService desensitizationService;
    private final AuditService auditService;
    private final DesensitizationLogRepository desensitizationLogRepository;

    public record DesensitizeRequest(String text, String sourceType) {}

    @PostMapping("/desensitize")
    public ApiResponse<Map<String, Object>> desensitize(@RequestBody DesensitizeRequest req) {
        Long uid = AuthHelper.currentUserId();
        var r = desensitizationService.desensitize(
                req.text(),
                req.sourceType() == null ? "MANUAL" : req.sourceType(),
                uid);
        return ApiResponse.ok(Map.of(
                "desensitized", r.desensitized(),
                "hitTypes", r.hitTypes(),
                "contentHash", r.contentHash(),
                "ruleVersion", DesensitizationService.RULE_VERSION
        ));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<Page<AuditLog>> auditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long uid = AuthHelper.currentUserId();
        return ApiResponse.ok(auditService.pageForUser(uid, PageRequest.of(page, size)));
    }

    @GetMapping("/desensitization-logs")
    public ApiResponse<Page<DesensitizationLog>> desLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long uid = AuthHelper.currentUserId();
        return ApiResponse.ok(desensitizationLogRepository.findByUserIdOrderByCreatedAtDesc(uid, PageRequest.of(page, size)));
    }
}
