package com.finresearch.web;

import com.finresearch.ai.AIService;
import com.finresearch.ai.SkillId;
import com.finresearch.common.ApiResponse;
import com.finresearch.domain.Revision;
import com.finresearch.repository.RevisionRepository;
import com.finresearch.service.AuditService;
import com.finresearch.service.ProjectAccessService;
import com.finresearch.service.ProjectProgressService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/revisions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class RevisionController {

    private final RevisionRepository revisionRepository;
    private final ProjectAccessService projectAccessService;
    private final AIService aiService;
    private final AuditService auditService;
    private final ProjectProgressService projectProgressService;

    @GetMapping
    public ApiResponse<List<Revision>> list(@PathVariable Long projectId) {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        return ApiResponse.ok(revisionRepository.findByProjectIdOrderByCreatedAtDesc(projectId));
    }

    public record ParseRequest(String comments, Long paperId) {}

    @PostMapping("/parse")
    public ApiResponse<Revision> parse(@PathVariable Long projectId, @RequestBody ParseRequest req) {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        String parsed = aiService.invokeSkill(SkillId.REVIEW_PARSE, Map.of(
                "comments", req.comments() == null ? "" : req.comments()));
        Revision r = new Revision();
        r.setProjectId(projectId);
        r.setPaperId(req.paperId());
        r.setRawComments(req.comments());
        r.setParsedJson(parsed);
        revisionRepository.save(r);
        auditService.log(uid, "REVISION_PARSE", "REVISION", r.getId(), null);
        projectProgressService.refresh(projectId);
        return ApiResponse.ok(r);
    }

    public record SuggestRequest(String paperContent, String commentItemJson) {}

    @PostMapping("/{id}/suggest")
    public ApiResponse<Map<String, String>> suggest(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestBody SuggestRequest req) {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        Revision r = revisionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("记录不存在"));
        if (!r.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("记录不属于该项目");
        }
        String out = aiService.invokeSkill(SkillId.REVIEW_EDIT, Map.of(
                "paper", req.paperContent() == null ? "" : req.paperContent(),
                "item", req.commentItemJson() == null ? "" : req.commentItemJson()));
        auditService.log(uid, "REVISION_SUGGEST", "REVISION", id, null);
        return ApiResponse.ok(Map.of("json", out));
    }

    public record ResponseLetterRequest(String editsJson) {}

    @PostMapping("/{id}/response-letter")
    public ApiResponse<Revision> responseLetter(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestBody ResponseLetterRequest req) {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        Revision r = revisionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("记录不存在"));
        if (!r.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("记录不属于该项目");
        }
        String letter = aiService.invokeSkill(SkillId.RESPONSE_LETTER, Map.of(
                "parsed", r.getParsedJson() == null ? "[]" : r.getParsedJson(),
                "edits", req.editsJson() == null ? "[]" : req.editsJson()));
        r.setResponseLetter(letter);
        if (req.editsJson() != null) {
            r.setSuggestionsJson(req.editsJson());
        }
        revisionRepository.save(r);
        auditService.log(uid, "REVISION_RESPONSE", "REVISION", id, null);
        projectProgressService.refresh(projectId);
        return ApiResponse.ok(r);
    }
}
