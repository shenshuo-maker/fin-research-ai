package com.finresearch.repository;

import com.finresearch.domain.DesensitizationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DesensitizationLogRepository extends JpaRepository<DesensitizationLog, Long> {
    Page<DesensitizationLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
