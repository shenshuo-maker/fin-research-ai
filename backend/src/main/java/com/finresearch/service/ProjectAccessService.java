package com.finresearch.service;

import com.finresearch.domain.Project;
import com.finresearch.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectAccessService {

    private final ProjectRepository projectRepository;

    public Project requireOwner(Long projectId, Long userId) {
        Project p = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在"));
        if (!p.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException("无权操作该项目");
        }
        return p;
    }
}
