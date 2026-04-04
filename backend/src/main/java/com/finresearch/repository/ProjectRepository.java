package com.finresearch.repository;

import com.finresearch.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);
}
