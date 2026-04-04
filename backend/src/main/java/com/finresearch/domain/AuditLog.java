package com.finresearch.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 128)
    private String action;

    @Column(length = 64, name = "resource_type")
    private String resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(length = 64)
    private String ip;

    @Column(nullable = false, name = "created_at")
    private Instant createdAt = Instant.now();
}
