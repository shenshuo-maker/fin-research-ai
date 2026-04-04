package com.finresearch.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "stat_runs")
@Getter
@Setter
@NoArgsConstructor
public class StatRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "project_id")
    private Long projectId;

    @Column(name = "dataset_id")
    private Long datasetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StatType type;

    @Column(name = "config_json", columnDefinition = "LONGTEXT")
    private String configJson;

    @Column(name = "result_json", columnDefinition = "LONGTEXT")
    private String resultJson;

    @Column(name = "chart_spec_json", columnDefinition = "LONGTEXT")
    private String chartSpecJson;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public enum StatType {
        DESCRIPTIVE, TTEST, OLS
    }
}
