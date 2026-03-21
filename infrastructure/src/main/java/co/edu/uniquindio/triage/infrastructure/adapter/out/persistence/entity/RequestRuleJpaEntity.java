package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "request_rules",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_request_rules_rule_request",
                        columnNames = {"rule_id", "request_id"})
        },
        indexes = {
                @Index(name = "idx_request_rules_rule", columnList = "rule_id"),
                @Index(name = "idx_request_rules_request", columnList = "request_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString
public class RequestRuleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private BusinessRuleJpaEntity rule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private AcademicRequestJpaEntity request;
}
