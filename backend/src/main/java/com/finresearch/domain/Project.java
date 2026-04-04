package com.finresearch.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "owner_id")
    private Long ownerId;

    @Column(nullable = false, length = 256)
    private String name;

    @Column(length = 2000)
    private String description;

    /** 0-100 完成度 */
    @Column(nullable = false)
    private int progressPercent = 0;

    @Column(length = 64)
    private String stage;

    @Column(nullable = false, name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();
}
