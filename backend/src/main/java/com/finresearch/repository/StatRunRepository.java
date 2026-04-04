package com.finresearch.repository;

import com.finresearch.domain.StatRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatRunRepository extends JpaRepository<StatRun, Long> {
    List<StatRun> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
