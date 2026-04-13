package com.omnistock.backend.service.integration;

import com.omnistock.backend.entity.DeadLetterQueue;
import com.omnistock.backend.entity.DeadLetterQueue.DlqStatus;
import com.omnistock.backend.entity.Supplier;
import com.omnistock.backend.repository.DeadLetterQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeadLetterQueueService {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterQueueService.class);

    private static final int MAX_RETRIES = 3;

    private final DeadLetterQueueRepository dlqRepository;

    public DeadLetterQueueService(DeadLetterQueueRepository dlqRepository) {
        this.dlqRepository = dlqRepository;
    }

    /**
     * Registra un producto fallido en la Dead Letter Queue.
     */
    @Transactional
    public DeadLetterQueue recordFailure(Supplier supplier, String mpn, String rawPayload,
                                          String errorMessage, String errorType) {
        DeadLetterQueue entry = new DeadLetterQueue(supplier, mpn, rawPayload, errorMessage, errorType);
        DeadLetterQueue saved = dlqRepository.save(entry);
        logger.warn("📥 DLQ: Producto {} de {} registrado como fallido (tipo: {})",
                mpn, supplier.getName(), errorType);
        return saved;
    }

    /**
     * Obtiene todos los registros pendientes de reprocesar.
     */
    public List<DeadLetterQueue> getPendingEntries() {
        return dlqRepository.findByStatus(DlqStatus.PENDING);
    }

    /**
     * Obtiene los registros pendientes de un proveedor específico.
     */
    public List<DeadLetterQueue> getPendingBySupplier(Long supplierId) {
        return dlqRepository.findBySupplierIdAndStatus(supplierId, DlqStatus.PENDING);
    }

    /**
     * Marca un registro como RESOLVED tras reprocesarlo exitosamente.
     */
    @Transactional
    public void markResolved(Long dlqId) {
        dlqRepository.findById(dlqId).ifPresent(entry -> {
            entry.setStatus(DlqStatus.RESOLVED);
            entry.setResolvedAt(LocalDateTime.now());
            dlqRepository.save(entry);
            logger.info("✅ DLQ: Registro {} resuelto", dlqId);
        });
    }

    /**
     * Marca un registro como DISCARDED (error permanente, no reintentar).
     */
    @Transactional
    public void markDiscarded(Long dlqId) {
        dlqRepository.findById(dlqId).ifPresent(entry -> {
            entry.setStatus(DlqStatus.DISCARDED);
            entry.setResolvedAt(LocalDateTime.now());
            dlqRepository.save(entry);
            logger.warn("🗑️ DLQ: Registro {} descartado", dlqId);
        });
    }

    /**
     * Incrementa el contador de reintentos y actualiza el estado.
     * Si se supera MAX_RETRIES, se deja como PENDING para revisión manual.
     */
    @Transactional
    public void incrementRetry(Long dlqId) {
        dlqRepository.findById(dlqId).ifPresent(entry -> {
            entry.setRetryCount(entry.getRetryCount() + 1);
            entry.setLastRetryAt(LocalDateTime.now());
            if (entry.getRetryCount() >= MAX_RETRIES) {
                entry.setStatus(DlqStatus.PENDING); // queda pendiente para revisión manual
                logger.warn("⚠️ DLQ: Registro {} ha superado {} reintentos. Pendiente de revisión manual.",
                        dlqId, MAX_RETRIES);
            } else {
                entry.setStatus(DlqStatus.RETRYING);
            }
            dlqRepository.save(entry);
        });
    }

    /**
     * Cuenta el total de entradas pendientes.
     */
    public long countPending() {
        return dlqRepository.countByStatus(DlqStatus.PENDING);
    }
}
