package com.finresearch.service;

import com.finresearch.domain.*;
import com.finresearch.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 根据里程碑粗略计算项目完成度（演示用）。
 * <p>与前端「实验三」闭环对应关系（满分 100，见 {@link #refresh(Long)} 内加分顺序）：
 * 文献上传、选题运行、EDC 表单、数据集上传、统计运行、论文正文长度、回修记录 —
 * 分别映射任务二～五中的可观测操作；任务七由用户点击「刷新完成度」触发本服务重算并写回 {@link com.finresearch.domain.Project}。
 */
@Service
@RequiredArgsConstructor
public class ProjectProgressService {

    private final ProjectRepository projectRepository;
    private final LiteratureRepository literatureRepository;
    private final TopicRunRepository topicRunRepository;
    private final EdcFormRepository edcFormRepository;
    private final DatasetRepository datasetRepository;
    private final StatRunRepository statRunRepository;
    private final PaperRepository paperRepository;
    private final RevisionRepository revisionRepository;

    public void refresh(Long projectId) {
        int score = 0;
        if (!literatureRepository.findByProjectIdOrderByCreatedAtDesc(projectId).isEmpty()) {
            score += 15;
        }
        if (topicRunRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .anyMatch(t -> t.getType() == TopicRun.TopicType.GENERATE
                        || t.getType() == TopicRun.TopicType.TIMELY
                        || t.getType() == TopicRun.TopicType.CURRENT_AFFAIRS)) {
            score += 15;
        }
        if (!edcFormRepository.findByProjectIdOrderByCreatedAtDesc(projectId).isEmpty()) {
            score += 15;
        }
        if (!datasetRepository.findByProjectIdOrderByCreatedAtDesc(projectId).isEmpty()) {
            score += 15;
        }
        if (!statRunRepository.findByProjectIdOrderByCreatedAtDesc(projectId).isEmpty()) {
            score += 10;
        }
        if (paperRepository.findByProjectId(projectId)
                .map(p -> p.getContent() != null && p.getContent().length() > 200)
                .orElse(false)) {
            score += 15;
        }
        if (!revisionRepository.findByProjectIdOrderByCreatedAtDesc(projectId).isEmpty()) {
            score += 15;
        }
        final int s = Math.min(100, score);
        projectRepository.findById(projectId).ifPresent(p -> {
            p.setProgressPercent(s);
            p.setUpdatedAt(Instant.now());
            if (s >= 90) {
                p.setStage("接近结项");
            } else if (s >= 50) {
                p.setStage("写作/分析");
            } else {
                p.setStage("选题/数据");
            }
            projectRepository.save(p);
        });
    }
}
