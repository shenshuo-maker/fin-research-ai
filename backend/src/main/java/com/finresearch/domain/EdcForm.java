package com.finresearch.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "edc_forms")
@Getter
@Setter
@NoArgsConstructor
public class EdcForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "project_id")
    private Long projectId;

    @Column(nullable = false, length = 256)
    private String name;

    /** JSON: [{id,name,type,csmarField,akshareField}] */
    @Column(name = "schema_json", columnDefinition = "LONGTEXT", nullable = false)
    private String schemaJson = "[]";

    @Column(nullable = false, unique = true, length = 64, name = "share_token")
    private String shareToken = UUID.randomUUID().toString().replace("-", "");

    @Column(name = "submissions_json", columnDefinition = "LONGTEXT")
    private String submissionsJson = "[]";

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
