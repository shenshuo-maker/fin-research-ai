package com.finresearch.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "literatures")
@Getter
@Setter
@NoArgsConstructor
public class Literature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "project_id")
    private Long projectId;

    @Column(nullable = false, length = 512)
    private String fileName;

    @Column(nullable = false, length = 1024, name = "stored_path")
    private String storedPath;

    @Column(name = "extracted_text", columnDefinition = "LONGTEXT")
    private String extractedText;

    @Column(name = "analysis_json", columnDefinition = "LONGTEXT")
    private String analysisJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status = Status.UPLOADED;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public enum Status {
        UPLOADED, ANALYZING, DONE, FAILED
    }
}
