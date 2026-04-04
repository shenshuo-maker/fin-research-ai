package com.finresearch.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.finresearch.ai.AIService;
import com.finresearch.ai.SkillId;
import com.finresearch.common.ApiResponse;
import com.finresearch.domain.Paper;
import com.finresearch.domain.PaperVersion;
import com.finresearch.repository.PaperRepository;
import com.finresearch.repository.PaperVersionRepository;
import com.finresearch.service.AuditService;
import com.finresearch.service.ProjectAccessService;
import com.finresearch.service.ProjectProgressService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/paper")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PaperController {

    private final PaperRepository paperRepository;
    private final PaperVersionRepository paperVersionRepository;
    private final ProjectAccessService projectAccessService;
    private final AIService aiService;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;
    private final ProjectProgressService projectProgressService;

    @GetMapping
    public ApiResponse<Paper> get(@PathVariable Long projectId) {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        return ApiResponse.ok(getOrCreate(projectId));
    }

    public record PaperUpdateRequest(String title, String content) {}

    @PutMapping
    public ApiResponse<Paper> save(@PathVariable Long projectId, @RequestBody PaperUpdateRequest req) {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        Paper p = getOrCreate(projectId);
        if (req.title() != null) {
            p.setTitle(req.title());
        }
        if (req.content() != null) {
            p.setContent(req.content());
        }
        paperRepository.save(p);
        auditService.log(uid, "PAPER_SAVE", "PAPER", p.getId(), null);
        projectProgressService.refresh(projectId);
        return ApiResponse.ok(p);
    }

    public record GenerateRequest(String title, String identification, String results) {}

    @PostMapping("/generate")
    public ApiResponse<Paper> generate(@PathVariable Long projectId, @RequestBody GenerateRequest req) {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        Paper p = getOrCreate(projectId);
        String out = aiService.invokeSkill(SkillId.PAPER_GENERATION, Map.of(
                "title", req.title() == null ? "" : req.title(),
                "identification", req.identification() == null ? "" : req.identification(),
                "results", req.results() == null ? "" : req.results()));
        p.setTitle(req.title());
        p.setContent(out);
        snapshotVersion(p, "AI 初稿生成");
        paperRepository.save(p);
        auditService.log(uid, "PAPER_GENERATE", "PAPER", p.getId(), null);
        projectProgressService.refresh(projectId);
        return ApiResponse.ok(p);
    }

    public record RewriteRequest(String paragraph, String mode) {}

    @PostMapping("/rewrite")
    public ApiResponse<Map<String, String>> rewrite(@PathVariable Long projectId, @RequestBody RewriteRequest req) {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        String mode = req.mode() == null ? "润色" : req.mode();
        String out = aiService.invokeSkill(SkillId.PARAGRAPH_REWRITE, Map.of(
                "paragraph", req.paragraph() == null ? "" : req.paragraph(),
                "mode", mode));
        auditService.log(uid, "PAPER_REWRITE", "PAPER", getOrCreate(projectId).getId(), mode);
        return ApiResponse.ok(Map.of("text", out));
    }

    public record CitationRequest(String snippet, String style) {}

    @PostMapping("/citations")
    public ApiResponse<Map<String, String>> citations(@PathVariable Long projectId, @RequestBody CitationRequest req) {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        String out = aiService.invokeSkill(SkillId.CITATION_MATCH, Map.of(
                "snippet", req.snippet() == null ? "" : req.snippet(),
                "style", req.style() == null ? "GB/T 7714" : req.style()));
        auditService.log(uid, "PAPER_CITATION", "PAPER", getOrCreate(projectId).getId(), null);
        return ApiResponse.ok(Map.of("json", out));
    }

    public record InterpretRequest(String stats) {}

    @PostMapping("/interpret-stats")
    public ApiResponse<Map<String, String>> interpret(@PathVariable Long projectId, @RequestBody InterpretRequest req) {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        String out = aiService.invokeSkill(SkillId.STATS_INTERPRET, Map.of(
                "stats", req.stats() == null ? "" : req.stats()));
        auditService.log(uid, "PAPER_STATS_INTERPRET", "PAPER", getOrCreate(projectId).getId(), null);
        return ApiResponse.ok(Map.of("text", out));
    }

    @GetMapping("/export/json")
    public void exportJson(@PathVariable Long projectId, HttpServletResponse response) throws Exception {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        Paper p = getOrCreate(projectId);
        ObjectNode root = objectMapper.createObjectNode();
        root.put("title", p.getTitle());
        root.put("content", p.getContent());
        root.put("exportMeta", p.getExportMetaJson());
        byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(root);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + URLEncoder.encode("paper.json", StandardCharsets.UTF_8));
        response.getOutputStream().write(bytes);
    }

    @GetMapping("/export/xlsx")
    public void exportXlsx(@PathVariable Long projectId, HttpServletResponse response) throws Exception {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        Paper p = getOrCreate(projectId);
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet s1 = wb.createSheet("摘要信息");
            Row r0 = s1.createRow(0);
            r0.createCell(0).setCellValue("标题");
            r0.createCell(1).setCellValue(p.getTitle() == null ? "" : p.getTitle());
            Sheet s2 = wb.createSheet("正文");
            String content = p.getContent() == null ? "" : p.getContent();
            String[] lines = content.split("\n");
            for (int i = 0; i < lines.length; i++) {
                Row r = s2.createRow(i);
                r.createCell(0).setCellValue(lines[i]);
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + URLEncoder.encode("paper.xlsx", StandardCharsets.UTF_8));
            wb.write(response.getOutputStream());
        }
    }

    @PostMapping("/versions")
    public ApiResponse<PaperVersion> snapshot(@PathVariable Long projectId, @RequestBody Map<String, String> body) {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        Paper p = getOrCreate(projectId);
        String note = body.getOrDefault("note", "手动快照");
        PaperVersion v = snapshotVersion(p, note);
        return ApiResponse.ok(v);
    }

    @GetMapping("/versions")
    public ApiResponse<List<PaperVersion>> versions(@PathVariable Long projectId) {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        Paper p = getOrCreate(projectId);
        return ApiResponse.ok(paperVersionRepository.findByPaperIdOrderByVersionNoDesc(p.getId()));
    }

    public record RollbackRequest(int versionNo) {}

    @PostMapping("/rollback")
    public ApiResponse<Paper> rollback(@PathVariable Long projectId, @RequestBody RollbackRequest req) {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        Paper p = getOrCreate(projectId);
        PaperVersion v = paperVersionRepository.findByPaperIdAndVersionNo(p.getId(), req.versionNo())
                .orElseThrow(() -> new IllegalArgumentException("版本不存在"));
        p.setContent(v.getContent());
        snapshotVersion(p, "回滚到 v" + req.versionNo());
        paperRepository.save(p);
        auditService.log(uid, "PAPER_ROLLBACK", "PAPER", p.getId(), "v" + req.versionNo());
        return ApiResponse.ok(p);
    }

    private Paper getOrCreate(Long projectId) {
        return paperRepository.findByProjectId(projectId).orElseGet(() -> {
            Paper p = new Paper();
            p.setProjectId(projectId);
            p.setTitle("未命名论文");
            p.setContent("");
            paperRepository.save(p);
            return p;
        });
    }

    private PaperVersion snapshotVersion(Paper p, String note) {
        int next = p.getCurrentVersion() + 1;
        PaperVersion v = new PaperVersion();
        v.setPaperId(p.getId());
        v.setVersionNo(next);
        v.setContent(p.getContent());
        v.setNote(note);
        paperVersionRepository.save(v);
        p.setCurrentVersion(next);
        paperRepository.save(p);
        return v;
    }
}
