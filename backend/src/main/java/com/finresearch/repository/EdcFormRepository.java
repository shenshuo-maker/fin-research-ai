package com.finresearch.repository;

import com.finresearch.domain.EdcForm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EdcFormRepository extends JpaRepository<EdcForm, Long> {
    List<EdcForm> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    Optional<EdcForm> findByShareToken(String shareToken);
}
