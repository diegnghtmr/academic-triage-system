package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "academic_requests", indexes = {
        @Index(name = "idx_requests_status", columnList = "status"),
        @Index(name = "idx_requests_priority", columnList = "priority"),
        @Index(name = "idx_requests_applicant", columnList = "applicant_id"),
        @Index(name = "idx_requests_responsible", columnList = "responsible_id"),
        @Index(name = "idx_requests_registration_date", columnList = "registration_date")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"history", "appliedRules"})
public class AcademicRequestJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(length = 20)
    private String priority;

    @Column(length = 30, nullable = false)
    private String status;

    @Column
    private LocalDate deadline;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDateTime;

    @Column(name = "priority_justification", length = 1000)
    private String priorityJustification;

    @Column(name = "rejection_reason", length = 2000)
    private String rejectionReason;

    @Column(name = "ai_suggested", nullable = false)
    private boolean aiSuggested;

    @Column(name = "closing_observation", length = 2000)
    private String closingObservation;

    @Column(name = "cancellation_reason", length = 2000)
    private String cancellationReason;

    @Column(name = "attendance_observation", length = 2000)
    private String attendanceObservation;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private UserJpaEntity applicant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_id")
    private UserJpaEntity responsible;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_channel_id", nullable = false)
    private OriginChannelJpaEntity originChannel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_type_id", nullable = false)
    private RequestTypeJpaEntity requestType;

    @OneToMany(mappedBy = "request", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RequestRuleJpaEntity> appliedRules = new ArrayList<>();

    @OneToMany(mappedBy = "request", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RequestHistoryJpaEntity> history = new ArrayList<>();
}
