package com.finresearch.web;

import com.finresearch.ai.AIService;
import com.finresearch.ai.SkillId;
import com.finresearch.common.ApiResponse;
import com.finresearch.domain.TopicRun;
import com.finresearch.repository.TopicRunRepository;
import com.finresearch.service.AuditService;
import com.finresearch.service.ProjectAccessService;
import com.finresearch.service.ProjectProgressService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/topics")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TopicController {

    private final TopicRunRepository topicRunRepository;
    private final ProjectAccessService projectAccessService;
    private final AIService aiService;
    private final AuditService auditService;
    private final ProjectProgressService projectProgressService;

    @GetMapping("/runs")
    public ApiResponse<List<TopicRun>> runs(@PathVariable Long projectId) {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        return ApiResponse.ok(topicRunRepository.findByProjectIdOrderByCreatedAtDesc(projectId));
    }

    @PostMapping("/generate")
    public ApiResponse<TopicRun> generate(@PathVariable Long projectId, @RequestBody Map<String, String> body) {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        String direction = body.getOrDefault("direction", "");
        String out = aiService.invokeSkill(SkillId.TOPIC_GENERATION, Map.of("direction", direction));
        TopicRun run = new TopicRun();
        run.setProjectId(projectId);
        run.setType(TopicRun.TopicType.GENERATE);
        run.setInputText(direction);
        run.setOutputJson(out);
        topicRunRepository.save(run);
        auditService.log(uid, "TOPIC_GENERATE", "PROJECT", projectId, null);
        projectProgressService.refresh(projectId);
        return ApiResponse.ok(run);
    }

    @PostMapping("/timely")
    public ApiResponse<TopicRun> timely(@PathVariable Long projectId, @RequestBody Map<String, String> body) {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        String theme = body.getOrDefault("theme", "");
        String out = aiService.invokeSkill(SkillId.TOPIC_TIMELY, Map.of("theme", theme));
        TopicRun run = new TopicRun();
        run.setProjectId(projectId);
        run.setType(TopicRun.TopicType.TIMELY);
        run.setInputText(theme);
        run.setOutputJson(out);
        topicRunRepository.save(run);
        auditService.log(uid, "TOPIC_TIMELY", "PROJECT", projectId, null);
        projectProgressService.refresh(projectId);
        return ApiResponse.ok(run);
    }

    /**
     * 已从前端下线「发表前景评估」；保留接口供旧客户端或脚本调用。
     */
    @PostMapping("/evaluate")
    public ApiResponse<TopicRun> evaluate(@PathVariable Long projectId, @RequestBody Map<String, String> body) {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        String topic = body.getOrDefault("topic", "");
        String out = aiService.invokeSkill(SkillId.PUBLICATION_FIT, Map.of("topic", topic));
        TopicRun run = new TopicRun();
        run.setProjectId(projectId);
        run.setType(TopicRun.TopicType.EVALUATE);
        run.setInputText(topic);
        run.setOutputJson(out);
        topicRunRepository.save(run);
        auditService.log(uid, "TOPIC_EVALUATE", "PROJECT", projectId, null);
        projectProgressService.refresh(projectId);
        return ApiResponse.ok(run);
    }

    /** 时政选题：政策焦点 + 数据频率 + 字段 + 与 EDC 伪表头联动 */
    @PostMapping("/current-affairs")
    public ApiResponse<TopicRun> currentAffairs(@PathVariable Long projectId, @RequestBody Map<String, String> body) {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(projectId, uid);
        String theme = body.getOrDefault("theme", "");
        String frequency = body.getOrDefault("frequency", "月频");
        String dataFields = body.getOrDefault("dataFields", "");
        String edcHint = body.getOrDefault("edcHint", "");
        String out = aiService.invokeSkill(SkillId.TOPIC_CURRENT_AFFAIRS, Map.of(
                "theme", theme,
                "frequency", frequency,
                "dataFields", dataFields,
                "edcHint", edcHint
        ));
        TopicRun run = new TopicRun();
        run.setProjectId(projectId);
        run.setType(TopicRun.TopicType.CURRENT_AFFAIRS);
        run.setInputText(theme + " | " + frequency);
        run.setOutputJson(out);
        topicRunRepository.save(run);
        auditService.log(uid, "TOPIC_CURRENT_AFFAIRS", "PROJECT", projectId, null);
        projectProgressService.refresh(projectId);
        return ApiResponse.ok(run);
    }
}
