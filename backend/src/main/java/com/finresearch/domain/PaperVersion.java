package com.finresearch.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "paper_versions")
@Getter
@Setter
@NoArgsConstructor
public class PaperVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "paper_id")
    private Long paperId;

    @Column(nullable = false, name = "version_no")
    private int versionNo;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(length = 512)
    private String note;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
