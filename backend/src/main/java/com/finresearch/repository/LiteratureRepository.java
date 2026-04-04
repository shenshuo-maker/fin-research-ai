package com.finresearch.repository;

import com.finresearch.domain.Literature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LiteratureRepository extends JpaRepository<Literature, Long> {
    List<Literature> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
