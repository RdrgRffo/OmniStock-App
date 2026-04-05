package com.omnistock.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Dead Letter Queue — almacena productos que fallaron repetidamente durante la
 * sincronización para su posterior reprocesamiento manual o automático.
 */
@Entity
@Table(name = "dead_letter_queue", indexes = {
    @Index(name = "idx_dlq_supplier", columnList = "proveedor_id"),
    @Index(name = "idx_dlq_status", columnList = "status"),
    @Index(name = "idx_dlq_created", columnList = "created_at")
})
public class DeadLetterQueue {

    public enum DlqStatus {
        PENDING,
        RETRYING,
        RESOLVED,
        DISCARDED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Supplier supplier;

    /** MPN del producto que falló */
    @Column(nullable = false)
    private String mpn;

    /** JSON crudo del producto que falló (para reintentar sin llamar a la API) */
    @Column(columnDefinition = "TEXT")
    private String rawPayload;

    /** Mensaje de error original */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String errorMessage;

    /** Tipo de error: TIMEOUT, PARSING, MAPPING, UNKNOWN */
    @Column(nullable = false)
    private String errorType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DlqStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastRetryAt;

    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.status = DlqStatus.PENDING;
        this.retryCount = 0;
    }

    public DeadLetterQueue() {}

    public DeadLetterQueue(Supplier supplier, String mpn, String rawPayload,
                           String errorMessage, String errorType) {
        this.supplier = supplier;
        this.mpn = mpn;
        this.rawPayload = rawPayload;
        this.errorMessage = errorMessage;
        this.errorType = errorType;
    }

    // Getters y Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }

    public String getMpn() { return mpn; }
    public void setMpn(String mpn) { this.mpn = mpn; }

    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }

    public DlqStatus getStatus() { return status; }
    public void setStatus(DlqStatus status) { this.status = status; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastRetryAt() { return lastRetryAt; }
    public void setLastRetryAt(LocalDateTime lastRetryAt) { this.lastRetryAt = lastRetryAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
