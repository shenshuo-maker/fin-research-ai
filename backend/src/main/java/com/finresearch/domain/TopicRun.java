package com.finresearch.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "topic_runs")
@Getter
@Setter
@NoArgsConstructor
public class TopicRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "project_id")
    private Long projectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TopicType type;

    @Column(name = "input_text", columnDefinition = "LONGTEXT")
    private String inputText;

    @Column(name = "output_json", columnDefinition = "LONGTEXT")
    private String outputJson;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public enum TopicType {
        GENERATE,
        TIMELY,
        /** 已从前端移除，保留枚举以兼容历史数据 */
        EVALUATE,
        /** 时政/政策与高频数据结合的选题设计 */
        CURRENT_AFFAIRS
    }
}
