package co.edu.uniquindio.triage.infrastructure.adapter.persistence.entity;

import co.edu.uniquindio.triage.domain.enums.PriorityEnum;
import co.edu.uniquindio.triage.domain.enums.RequestStatusEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "academic_requests", indexes = {
        @Index(name = "idx_requests_status", columnList = "status"),
        @Index(name = "idx_requests_priority", columnList = "priority"),
        @Index(name = "idx_requests_applicant", columnList = "applicant_id"),
        @Index(name = "idx_requests_responsible", columnList = "responsible_id"),
        @Index(name = "idx_requests_registration_date", columnList = "registration_date")
})
public class AcademicRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PriorityEnum priority;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private RequestStatusEnum status;

    @Column
    private LocalDate deadline;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDateTime;

    @Column(name = "priority_justification", length = 500)
    private String priorityJustification;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "ai_suggested", nullable = false)
    private boolean aiSuggested;

    @Column(name = "closing_observation", length = 500)
    private String closingObservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private UserEntity applicant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_id")
    private UserEntity responsible;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_channel_id", nullable = false)
    private OriginChannelEntity originChannel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_type_id", nullable = false)
    private RequestTypeEntity requestType;

    @OneToMany(mappedBy = "request", fetch = FetchType.LAZY)
    private List<RequestRuleEntity> appliedRules = new ArrayList<>();

    @OneToMany(mappedBy = "request", fetch = FetchType.LAZY)
    private List<RequestHistoryEntity> history = new ArrayList<>();

    public AcademicRequestEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PriorityEnum getPriority() {
        return priority;
    }

    public void setPriority(PriorityEnum priority) {
        this.priority = priority;
    }

    public RequestStatusEnum getStatus() {
        return status;
    }

    public void setStatus(RequestStatusEnum status) {
        this.status = status;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public LocalDateTime getRegistrationDateTime() {
        return registrationDateTime;
    }

    public void setRegistrationDateTime(LocalDateTime registrationDateTime) {
        this.registrationDateTime = registrationDateTime;
    }

    public String getPriorityJustification() {
        return priorityJustification;
    }

    public void setPriorityJustification(String priorityJustification) {
        this.priorityJustification = priorityJustification;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public boolean isAiSuggested() {
        return aiSuggested;
    }

    public void setAiSuggested(boolean aiSuggested) {
        this.aiSuggested = aiSuggested;
    }

    public String getClosingObservation() {
        return closingObservation;
    }

    public void setClosingObservation(String closingObservation) {
        this.closingObservation = closingObservation;
    }

    public UserEntity getApplicant() {
        return applicant;
    }

    public void setApplicant(UserEntity applicant) {
        this.applicant = applicant;
    }

    public UserEntity getResponsible() {
        return responsible;
    }

    public void setResponsible(UserEntity responsible) {
        this.responsible = responsible;
    }

    public OriginChannelEntity getOriginChannel() {
        return originChannel;
    }

    public void setOriginChannel(OriginChannelEntity originChannel) {
        this.originChannel = originChannel;
    }

    public RequestTypeEntity getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestTypeEntity requestType) {
        this.requestType = requestType;
    }

    public List<RequestRuleEntity> getAppliedRules() {
        return appliedRules;
    }

    public void setAppliedRules(List<RequestRuleEntity> appliedRules) {
        this.appliedRules = appliedRules;
    }

    public List<RequestHistoryEntity> getHistory() {
        return history;
    }

    public void setHistory(List<RequestHistoryEntity> history) {
        this.history = history;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AcademicRequestEntity that = (AcademicRequestEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AcademicRequestEntity{" +
                "id=" + id +
                ", status=" + status +
                ", priority=" + priority +
                '}';
    }
}
