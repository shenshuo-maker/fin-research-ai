package com.finresearch.repository;

import com.finresearch.domain.Revision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RevisionRepository extends JpaRepository<Revision, Long> {
    List<Revision> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
