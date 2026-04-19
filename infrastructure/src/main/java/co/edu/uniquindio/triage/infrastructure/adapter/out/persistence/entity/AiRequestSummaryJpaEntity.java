package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ai_request_summaries",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_ai_summary_request_version",
                columnNames = {"request_id", "request_version"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRequestSummaryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "request_version", nullable = false)
    private long requestVersion;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String summary;

    @Column(length = 100)
    private String provider;

    @Column(length = 100)
    private String model;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
}
