package com.finresearch.web;

import com.finresearch.common.ApiResponse;
import com.finresearch.domain.Project;
import com.finresearch.repository.ProjectRepository;
import com.finresearch.service.AuditService;
import com.finresearch.service.ProjectAccessService;
import com.finresearch.service.ProjectProgressService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final ProjectAccessService projectAccessService;
    private final AuditService auditService;
    private final ProjectProgressService projectProgressService;

    @GetMapping
    public ApiResponse<List<Project>> list() {
        Long uid = AuthHelper.currentUserId();
        return ApiResponse.ok(projectRepository.findByOwnerIdOrderByUpdatedAtDesc(uid));
    }

    public record CreateProjectRequest(@NotBlank String name, String description) {}

    @PostMapping
    public ApiResponse<Project> create(@RequestBody CreateProjectRequest req) {
        Long uid = AuthHelper.currentUserId();
        Project p = new Project();
        p.setOwnerId(uid);
        p.setName(req.name());
        p.setDescription(req.description());
        p.setStage("选题/数据");
        p.setProgressPercent(0);
        projectRepository.save(p);
        auditService.log(uid, "PROJECT_CREATE", "PROJECT", p.getId(), req.name());
        return ApiResponse.ok(p);
    }

    @GetMapping("/{id}")
    public ApiResponse<Project> get(@PathVariable Long id) {
        Long uid = AuthHelper.currentUserId();
        return ApiResponse.ok(projectAccessService.requireOwner(id, uid));
    }

    public record UpdateProjectRequest(String name, String description) {}

    @PutMapping("/{id}")
    public ApiResponse<Project> update(@PathVariable Long id, @RequestBody UpdateProjectRequest req) {
        Long uid = AuthHelper.currentUserId();
        Project p = projectAccessService.requireOwner(id, uid);
        if (req.name() != null) {
            p.setName(req.name());
        }
        if (req.description() != null) {
            p.setDescription(req.description());
        }
        p.setUpdatedAt(Instant.now());
        projectRepository.save(p);
        auditService.log(uid, "PROJECT_UPDATE", "PROJECT", id, null);
        return ApiResponse.ok(p);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(id, uid);
        projectRepository.deleteById(id);
        auditService.log(uid, "PROJECT_DELETE", "PROJECT", id, null);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/progress/refresh")
    public ApiResponse<Map<String, Object>> refreshProgress(@PathVariable Long id) {
        Long uid = AuthHelper.currentUserId();
        projectAccessService.requireOwner(id, uid);
        projectProgressService.refresh(id);
        Project p = projectRepository.findById(id).orElseThrow();
        return ApiResponse.ok(Map.of(
                "progressPercent", p.getProgressPercent(),
                "stage", p.getStage() != null ? p.getStage() : ""
        ));
    }
}
