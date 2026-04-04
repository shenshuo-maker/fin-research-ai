package com.finresearch.repository;

import com.finresearch.domain.Paper;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaperRepository extends JpaRepository<Paper, Long> {
    Optional<Paper> findByProjectId(Long projectId);
}
