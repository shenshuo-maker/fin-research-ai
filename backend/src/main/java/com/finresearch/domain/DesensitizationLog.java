package com.finresearch.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "desensitization_logs")
@Getter
@Setter
@NoArgsConstructor
public class DesensitizationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 64, name = "source_type")
    private String sourceType;

    @Column(nullable = false, length = 32, name = "rule_version")
    private String ruleVersion = "v1";

    @Column(name = "hits_json", columnDefinition = "TEXT")
    private String hitsJson;

    @Column(length = 128, name = "content_hash")
    private String contentHash;

    @Column(nullable = false, name = "created_at")
    private Instant createdAt = Instant.now();
}
