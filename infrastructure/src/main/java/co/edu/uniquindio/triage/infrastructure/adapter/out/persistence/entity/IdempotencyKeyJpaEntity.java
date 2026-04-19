package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "idempotency_keys",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_idempotency_scope_principal_key",
                        columnNames = {"scope", "principal_scope", "idempotency_key"})
        },
        indexes = {
                @Index(name = "idx_idempotency_status", columnList = "status"),
                @Index(name = "idx_idempotency_scope_status", columnList = "scope,status"),
                @Index(name = "idx_idempotency_expires_at", columnList = "expires_at")
        }
)
public class IdempotencyKeyJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String scope;

    @Column(name = "principal_scope", nullable = false, length = 255)
    private String principalScope;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(nullable = false, columnDefinition = "CHAR(64)")
    private String fingerprint;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "response_status_code")
    private Integer responseStatusCode;

    @Column(name = "response_headers", columnDefinition = "TEXT")
    private String responseHeaders;

    @Column(name = "response_body", columnDefinition = "LONGTEXT")
    private String responseBody;

    @Column(name = "response_content_type", length = 150)
    private String responseContentType;

    // V25 metadata — resource_type, resource_id, and correlation_id remain null until a
    // future batch implements resource-tracking and distributed tracing integration.
    // DO NOT remove without a migration that drops the columns as well.
    @Column(name = "resource_type", length = 100)
    private String resourceType;

    @Column(name = "resource_id", length = 255)
    private String resourceId;

    @Column(name = "correlation_id", length = 255)
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // expires_at: set at claim time using IdempotencyTtlPolicy; used by IdempotencyCleanupService.
    // last_seen_at: set at initial claim; refreshed on every replay hit.
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getPrincipalScope() { return principalScope; }
    public void setPrincipalScope(String principalScope) { this.principalScope = principalScope; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getResponseStatusCode() { return responseStatusCode; }
    public void setResponseStatusCode(Integer responseStatusCode) { this.responseStatusCode = responseStatusCode; }
    public String getResponseHeaders() { return responseHeaders; }
    public void setResponseHeaders(String responseHeaders) { this.responseHeaders = responseHeaders; }
    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
    public String getResponseContentType() { return responseContentType; }
    public void setResponseContentType(String responseContentType) { this.responseContentType = responseContentType; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}
