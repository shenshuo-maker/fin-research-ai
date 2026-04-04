package com.finresearch.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.finresearch.common.ApiResponse;
import com.finresearch.domain.Dataset;
import com.finresearch.domain.StatRun;
import com.finresearch.repository.DatasetRepository;
import com.finresearch.repository.StatRunRepository;
import com.finresearch.service.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/datasets")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class DatasetController {

    private final DatasetRepository datasetRepository;
    private final StatRunRepository statRunRepository;
    private final ProjectAccessService projectAccessService;
    private final FileStorageService fileStorageService;
    private final DatasetProcessingService datasetProcessingService;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;
    private final ProjectProgressService projectProgressService;

    @GetMapping
    public ApiResponse<List<Dataset>> list(@PathVariable Long projectId) {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        return ApiResponse.ok(datasetRepository.findByProjectIdOrderByCreatedAtDesc(projectId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Dataset> upload(
            @PathVariable Long projectId,
            @RequestPart("file") MultipartFile file) throws Exception {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        String name = file.getOriginalFilename();
        if (name == null) {
            throw new IllegalArgumentException("无效文件");
        }
        String lower = name.toLowerCase();
        if (!lower.endsWith(".csv") && !lower.endsWith(".xlsx") && !lower.endsWith(".xls")) {
            throw new IllegalArgumentException("仅支持 CSV / Excel");
        }
        String path = fileStorageService.saveEncrypted(uid, file);
        Dataset d = new Dataset();
        d.setProjectId(projectId);
        d.setFileName(name);
        d.setStoredPath(path);
        datasetRepository.save(d);
        auditService.log(uid, "DATASET_UPLOAD", "DATASET", d.getId(), name);
        projectProgressService.refresh(projectId);
        return ApiResponse.ok(d);
    }

    @PostMapping("/{id}/clean/analyze")
    public ApiResponse<Dataset> analyzeClean(@PathVariable Long projectId, @PathVariable Long id) throws Exception {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        Dataset d = loadDs(projectId, id);
        byte[] raw = fileStorageService.readEncryptedAbsolutePath(d.getStoredPath());
        List<String[]> rows = datasetProcessingService.parseTabular(raw, d.getFileName());
        ObjectNode report = datasetProcessingService.cleanReport(rows);
        d.setCleanReportJson(objectMapper.writeValueAsString(report));
        datasetRepository.save(d);
        auditService.log(uid, "DATASET_CLEAN_ANALYZE", "DATASET", id, null);
        return ApiResponse.ok(d);
    }

    @PostMapping("/{id}/clean/apply")
    public ApiResponse<Dataset> applyClean(@PathVariable Long projectId, @PathVariable Long id) throws Exception {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        Dataset d = loadDs(projectId, id);
        byte[] raw = fileStorageService.readEncryptedAbsolutePath(d.getStoredPath());
        List<String[]> rows = datasetProcessingService.parseTabular(raw, d.getFileName());
        List<String[]> cleaned = datasetProcessingService.applyClean(rows);
        byte[] out = toCsvBytes(cleaned);
        String procPath = fileStorageService.savePlainInUserDir(uid, "_cleaned.csv", out);
        d.setProcessedPath(procPath);
        datasetRepository.save(d);
        auditService.log(uid, "DATASET_CLEAN_APPLY", "DATASET", id, null);
        projectProgressService.refresh(projectId);
        return ApiResponse.ok(d);
    }

    @PostMapping("/{id}/stats/descriptive")
    public ApiResponse<StatRun> descriptive(@PathVariable Long projectId, @PathVariable Long id) throws Exception {
        return runStats(projectId, id, StatRun.StatType.DESCRIPTIVE, null, null, null, null);
    }

    public record TTestBody(String valueColumn, String groupColumn) {}

    @PostMapping("/{id}/stats/ttest")
    public ApiResponse<StatRun> ttest(@PathVariable Long projectId, @PathVariable Long id, @RequestBody TTestBody body)
            throws Exception {
        return runStats(projectId, id, StatRun.StatType.TTEST, body.valueColumn(), body.groupColumn(), null, null);
    }

    public record OlsBody(String yColumn, List<String> xColumns) {}

    @PostMapping("/{id}/stats/ols")
    public ApiResponse<StatRun> ols(@PathVariable Long projectId, @PathVariable Long id, @RequestBody OlsBody body)
            throws Exception {
        return runStats(projectId, id, StatRun.StatType.OLS, null, null, body.yColumn(), body.xColumns());
    }

    private ApiResponse<StatRun> runStats(
            Long projectId,
            Long id,
            StatRun.StatType type,
            String valueCol,
            String groupCol,
            String yCol,
            List<String> xCols) throws Exception {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        Dataset d = loadDs(projectId, id);
        byte[] raw = fileStorageService.readEncryptedAbsolutePath(
                d.getProcessedPath() != null ? d.getProcessedPath() : d.getStoredPath());
        List<String[]> rows = datasetProcessingService.parseTabular(raw, d.getFileName());
        ObjectNode config = objectMapper.createObjectNode();
        ObjectNode result;
        ArrayNode charts = objectMapper.createArrayNode();
        switch (type) {
            case DESCRIPTIVE -> {
                config.put("type", "DESCRIPTIVE");
                result = datasetProcessingService.descriptive(rows);
                if (rows.size() > 2) {
                    String[] h = rows.get(0);
                    if (h.length >= 2) {
                        ArrayNode scatter = datasetProcessingService.chartSpecScatter(rows, h[0], h[1]);
                        ObjectNode spec = objectMapper.createObjectNode();
                        spec.put("type", "scatter");
                        spec.set("data", scatter);
                        charts.add(spec);
                    }
                }
            }
            case TTEST -> {
                config.put("type", "TTEST");
                config.put("valueColumn", valueCol);
                config.put("groupColumn", groupCol);
                result = datasetProcessingService.ttest(rows, valueCol, groupCol);
            }
            case OLS -> {
                config.put("type", "OLS");
                config.put("yColumn", yCol);
                config.set("xColumns", objectMapper.valueToTree(xCols));
                result = datasetProcessingService.ols(rows, yCol, xCols);
            }
            default -> throw new IllegalStateException();
        }
        StatRun run = new StatRun();
        run.setProjectId(projectId);
        run.setDatasetId(id);
        run.setType(type);
        run.setConfigJson(config.toString());
        run.setResultJson(result.toString());
        run.setChartSpecJson(charts.toString());
        statRunRepository.save(run);
        auditService.log(uid, "STAT_RUN", "DATASET", id, type.name());
        projectProgressService.refresh(projectId);
        return ApiResponse.ok(run);
    }

    private Dataset loadDs(Long projectId, Long id) {
        Dataset d = datasetRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("数据集不存在"));
        if (!d.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("数据不属于该项目");
        }
        return d;
    }

    private static byte[] toCsvBytes(List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                String cell = row[i] == null ? "" : row[i].replace("\"", "\"\"");
                if (cell.contains(",") || cell.contains("\"") || cell.contains("\n")) {
                    sb.append('"').append(cell).append('"');
                } else {
                    sb.append(cell);
                }
            }
            sb.append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
