package co.edu.uniquindio.triage.infrastructure.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "request_history", indexes = {
        @Index(name = "idx_history_request", columnList = "request_id"),
        @Index(name = "idx_history_timestamp", columnList = "timestamp")
})
public class RequestHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private AcademicRequestEntity request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_id", nullable = false)
    private UserEntity performedBy;

    public RequestHistoryEntity() {
    }

    public RequestHistoryEntity(Long id, String action, String observations,
                                 AcademicRequestEntity request, UserEntity performedBy) {
        this.id = id;
        this.action = action;
        this.observations = observations;
        this.timestamp = LocalDateTime.now();
        this.request = request;
        this.performedBy = performedBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public AcademicRequestEntity getRequest() {
        return request;
    }

    public void setRequest(AcademicRequestEntity request) {
        this.request = request;
    }

    public UserEntity getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(UserEntity performedBy) {
        this.performedBy = performedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestHistoryEntity that = (RequestHistoryEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "RequestHistoryEntity{" +
                "id=" + id +
                ", action='" + action + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
