package com.finresearch.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "revisions")
@Getter
@Setter
@NoArgsConstructor
public class Revision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "project_id")
    private Long projectId;

    @Column(name = "paper_id")
    private Long paperId;

    @Column(name = "raw_comments", columnDefinition = "LONGTEXT")
    private String rawComments;

    @Column(name = "parsed_json", columnDefinition = "LONGTEXT")
    private String parsedJson;

    @Column(name = "suggestions_json", columnDefinition = "LONGTEXT")
    private String suggestionsJson;

    @Column(name = "response_letter", columnDefinition = "LONGTEXT")
    private String responseLetter;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
