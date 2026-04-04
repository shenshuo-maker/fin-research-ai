package com.finresearch.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.finresearch.common.ApiResponse;
import com.finresearch.domain.EdcForm;
import com.finresearch.repository.EdcFormRepository;
import com.finresearch.service.AuditService;
import com.finresearch.service.ProjectAccessService;
import com.finresearch.service.ProjectProgressService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class EdcController {

    private final EdcFormRepository edcFormRepository;
    private final ProjectAccessService projectAccessService;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;
    private final ProjectProgressService projectProgressService;

    @GetMapping("/api/projects/{projectId}/edc/forms")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<List<EdcForm>> list(@PathVariable Long projectId) {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        return ApiResponse.ok(edcFormRepository.findByProjectIdOrderByCreatedAtDesc(projectId));
    }

    public record EdcFormBody(String name, String schemaJson) {}

    @PostMapping("/api/projects/{projectId}/edc/forms")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<EdcForm> create(@PathVariable Long projectId, @RequestBody EdcFormBody body) throws Exception {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        if (body.name() == null || body.name().isBlank()) {
            throw new IllegalArgumentException("表单名称必填");
        }
        String schema = body.schemaJson() != null ? body.schemaJson() : "[]";
        objectMapper.readTree(schema);
        EdcForm f = new EdcForm();
        f.setProjectId(projectId);
        f.setName(body.name());
        f.setSchemaJson(schema);
        edcFormRepository.save(f);
        auditService.log(uid, "EDC_CREATE", "EDC_FORM", f.getId(), body.name());
        projectProgressService.refresh(projectId);
        return ApiResponse.ok(f);
    }

    @PutMapping("/api/projects/{projectId}/edc/forms/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<EdcForm> update(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestBody EdcFormBody body) throws Exception {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        EdcForm f = edcFormRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("表单不存在"));
        if (!f.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("表单不属于该项目");
        }
        if (body.name() != null) {
            f.setName(body.name());
        }
        if (body.schemaJson() != null) {
            objectMapper.readTree(body.schemaJson());
            f.setSchemaJson(body.schemaJson());
        }
        edcFormRepository.save(f);
        auditService.log(uid, "EDC_UPDATE", "EDC_FORM", id, null);
        return ApiResponse.ok(f);
    }

    @GetMapping("/api/edc/public/{token}")
    public ApiResponse<Map<String, Object>> publicForm(@PathVariable String token) throws Exception {
        EdcForm f = edcFormRepository.findByShareToken(token).orElseThrow(() -> new IllegalArgumentException("链接无效"));
        return ApiResponse.ok(Map.of(
                "formId", f.getId(),
                "name", f.getName(),
                "schemaJson", readJson(f.getSchemaJson())
        ));
    }

    @PostMapping("/api/edc/public/{token}/submit")
    public ApiResponse<Map<String, Object>> publicSubmit(@PathVariable String token, @RequestBody Map<String, Object> row)
            throws Exception {
        EdcForm f = edcFormRepository.findByShareToken(token).orElseThrow(() -> new IllegalArgumentException("链接无效"));
        ArrayNode arr = (ArrayNode) objectMapper.readTree(
                f.getSubmissionsJson() == null || f.getSubmissionsJson().isBlank() ? "[]" : f.getSubmissionsJson());
        arr.add(objectMapper.valueToTree(row));
        f.setSubmissionsJson(objectMapper.writeValueAsString(arr));
        edcFormRepository.save(f);
        return ApiResponse.ok(Map.of("submitted", true, "total", arr.size()));
    }

    /** 项目内模拟一条采集数据（闭环演示，需登录） */
    @PostMapping("/api/projects/{projectId}/edc/forms/{formId}/simulate-row")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Map<String, Object>> simulateRow(@PathVariable Long projectId, @PathVariable Long formId)
            throws Exception {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        EdcForm f = edcFormRepository.findById(formId).orElseThrow(() -> new IllegalArgumentException("表单不存在"));
        if (!f.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("表单不属于该项目");
        }
        Map<String, Object> row = Map.of(
                "stockCode", "000001",
                "tradeDate", "2024-12-31",
                "ret", "0.012",
                "note", "模拟录入"
        );
        ArrayNode arr = (ArrayNode) objectMapper.readTree(
                f.getSubmissionsJson() == null || f.getSubmissionsJson().isBlank() ? "[]" : f.getSubmissionsJson());
        arr.add(objectMapper.valueToTree(row));
        f.setSubmissionsJson(objectMapper.writeValueAsString(arr));
        edcFormRepository.save(f);
        auditService.log(uid, "EDC_SIMULATE", "EDC_FORM", formId, null);
        projectProgressService.refresh(projectId);
        return ApiResponse.ok(Map.of("submitted", true, "total", arr.size()));
    }

    private JsonNode readJson(String s) throws Exception {
        return objectMapper.readTree(s == null ? "[]" : s);
    }
}
