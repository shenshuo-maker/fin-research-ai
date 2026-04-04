package com.finresearch.web;

import com.finresearch.ai.AIService;
import com.finresearch.ai.SkillId;
import com.finresearch.common.ApiResponse;
import com.finresearch.domain.Literature;
import com.finresearch.repository.LiteratureRepository;
import com.finresearch.service.AuditService;
import com.finresearch.service.DesensitizationService;
import com.finresearch.service.FileStorageService;
import com.finresearch.service.PdfTextExtractor;
import com.finresearch.service.ProjectAccessService;
import com.finresearch.service.ProjectProgressService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/literatures")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class LiteratureController {

    private final LiteratureRepository literatureRepository;
    private final ProjectAccessService projectAccessService;
    private final FileStorageService fileStorageService;
    private final PdfTextExtractor pdfTextExtractor;
    private final DesensitizationService desensitizationService;
    private final AIService aiService;
    private final AuditService auditService;
    private final ProjectProgressService projectProgressService;

    @GetMapping
    public ApiResponse<List<Literature>> list(@PathVariable Long projectId) {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        return ApiResponse.ok(literatureRepository.findByProjectIdOrderByCreatedAtDesc(projectId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Literature> upload(
            @PathVariable Long projectId,
            @RequestPart("file") MultipartFile file) throws Exception {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("请上传 PDF 文献");
        }
        String path = fileStorageService.saveEncrypted(uid, file);
        Literature lit = new Literature();
        lit.setProjectId(projectId);
        lit.setFileName(name);
        lit.setStoredPath(path);
        lit.setStatus(Literature.Status.UPLOADED);
        literatureRepository.save(lit);
        auditService.log(uid, "LITERATURE_UPLOAD", "LITERATURE", lit.getId(), name);
        return ApiResponse.ok(lit);
    }

    @PostMapping("/{id}/analyze")
    public ApiResponse<Literature> analyze(@PathVariable Long projectId, @PathVariable Long id) throws Exception {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        Literature lit = literatureRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("文献不存在"));
        if (!lit.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("文献不属于该项目");
        }
        lit.setStatus(Literature.Status.ANALYZING);
        literatureRepository.save(lit);

        byte[] pdf = fileStorageService.readEncryptedAbsolutePath(lit.getStoredPath());
        String text = pdfTextExtractor.extract(pdf);
        lit.setExtractedText(text);
        var des = desensitizationService.desensitize(text, "LITERATURE_TEXT", uid);
        String out = aiService.invokeSkill(SkillId.FINANCE_LITERATURE_ANALYSIS, Map.of("text", des.desensitized()));
        lit.setAnalysisJson(out);
        lit.setStatus(Literature.Status.DONE);
        literatureRepository.save(lit);
        auditService.log(uid, "LITERATURE_ANALYZE", "LITERATURE", id, "desensitizedHash=" + des.contentHash());
        projectProgressService.refresh(projectId);
        return ApiResponse.ok(lit);
    }
}
