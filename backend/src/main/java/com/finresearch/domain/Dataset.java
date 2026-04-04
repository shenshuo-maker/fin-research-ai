package com.finresearch.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "datasets")
@Getter
@Setter
@NoArgsConstructor
public class Dataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "project_id")
    private Long projectId;

    @Column(nullable = false, length = 512)
    private String fileName;

    @Column(nullable = false, length = 1024, name = "stored_path")
    private String storedPath;

    @Column(name = "clean_report_json", columnDefinition = "LONGTEXT")
    private String cleanReportJson;

    @Column(length = 1024, name = "processed_path")
    private String processedPath;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
