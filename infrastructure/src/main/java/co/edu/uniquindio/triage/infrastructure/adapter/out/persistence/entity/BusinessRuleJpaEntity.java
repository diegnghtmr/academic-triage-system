package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "business_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString
public class BusinessRuleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 100, nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "condition_type", length = 50, nullable = false)
    private String conditionType;

    @Column(name = "condition_value", length = 255, nullable = false)
    private String conditionValue;

    @Column(name = "resulting_priority", length = 20, nullable = false)
    private String resultingPriority;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_type_id")
    private RequestTypeJpaEntity requestType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RequestTypeJpaEntity getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestTypeJpaEntity requestType) {
        this.requestType = requestType;
    }
}
