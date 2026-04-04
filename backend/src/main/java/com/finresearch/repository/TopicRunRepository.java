package com.finresearch.repository;

import com.finresearch.domain.TopicRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicRunRepository extends JpaRepository<TopicRun, Long> {
    List<TopicRun> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
