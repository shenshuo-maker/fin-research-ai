package com.finresearch.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "papers")
@Getter
@Setter
@NoArgsConstructor
public class Paper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "project_id")
    private Long projectId;

    @Column(length = 512)
    private String title;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "export_meta_json", columnDefinition = "LONGTEXT")
    private String exportMetaJson;

    @Column(name = "current_version")
    private int currentVersion = 1;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();
}
