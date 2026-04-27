package com.omnistock.backend.service.integration;

import com.omnistock.backend.entity.DeadLetterQueue;
import com.omnistock.backend.entity.DeadLetterQueue.DlqStatus;
import com.omnistock.backend.entity.Supplier;
import com.omnistock.backend.repository.DeadLetterQueueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeadLetterQueueServiceTest {

    @Mock
    private DeadLetterQueueRepository dlqRepository;

    private DeadLetterQueueService deadLetterQueueService;

    @Captor
    private ArgumentCaptor<DeadLetterQueue> dlqCaptor;

    private Supplier testSupplier;
    private DeadLetterQueue testEntry;

    @BeforeEach
    void setUp() {
        deadLetterQueueService = new DeadLetterQueueService(dlqRepository);

        testSupplier = new Supplier();
        testSupplier.setId(1);
        testSupplier.setName("Test Supplier");

        testEntry = new DeadLetterQueue(testSupplier, "MPN001", "{\"mpn\":\"MPN001\"}", "Error de prueba", "UNKNOWN");
        testEntry.setId(100L);
        testEntry.setCreatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("recordFailure")
    class RecordFailure {

        @Test
        @DisplayName("Debe guardar un nuevo registro en DLQ")
        void shouldSaveNewEntry() {
            when(dlqRepository.save(any(DeadLetterQueue.class))).thenReturn(testEntry);

            DeadLetterQueue result = deadLetterQueueService.recordFailure(
                    testSupplier, "MPN001", "{\"mpn\":\"MPN001\"}", "Error de prueba", "UNKNOWN");

            assertNotNull(result);
            assertEquals("MPN001", result.getMpn());
            assertEquals(testSupplier, result.getSupplier());
            assertEquals("UNKNOWN", result.getErrorType());
            verify(dlqRepository).save(any(DeadLetterQueue.class));
        }

        @Test
        @DisplayName("Debe guardar con tipo TIMEOUT")
        void shouldSaveWithTimeoutType() {
            when(dlqRepository.save(any(DeadLetterQueue.class))).thenReturn(testEntry);

            deadLetterQueueService.recordFailure(testSupplier, "MPN002", null, "Connection timed out", "TIMEOUT");

            verify(dlqRepository).save(dlqCaptor.capture());
            assertEquals("TIMEOUT", dlqCaptor.getValue().getErrorType());
        }
    }

    @Nested
    @DisplayName("getPendingEntries")
    class GetPendingEntries {

        @Test
        @DisplayName("Debe retornar lista de entradas pendientes")
        void shouldReturnPendingEntries() {
            when(dlqRepository.findByStatus(DlqStatus.PENDING)).thenReturn(List.of(testEntry));

            List<DeadLetterQueue> result = deadLetterQueueService.getPendingEntries();

            assertEquals(1, result.size());
            assertEquals("MPN001", result.get(0).getMpn());
            verify(dlqRepository).findByStatus(DlqStatus.PENDING);
        }

        @Test
        @DisplayName("Debe retornar lista vacía si no hay pendientes")
        void shouldReturnEmptyListWhenNoPending() {
            when(dlqRepository.findByStatus(DlqStatus.PENDING)).thenReturn(List.of());

            List<DeadLetterQueue> result = deadLetterQueueService.getPendingEntries();

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getPendingBySupplier")
    class GetPendingBySupplier {

        @Test
        @DisplayName("Debe filtrar por proveedor")
        void shouldFilterBySupplier() {
            when(dlqRepository.findBySupplierIdAndStatus(1L, DlqStatus.PENDING)).thenReturn(List.of(testEntry));

            List<DeadLetterQueue> result = deadLetterQueueService.getPendingBySupplier(1L);

            assertEquals(1, result.size());
            verify(dlqRepository).findBySupplierIdAndStatus(1L, DlqStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("markResolved")
    class MarkResolved {

        @Test
        @DisplayName("Debe marcar como RESOLVED")
        void shouldMarkAsResolved() {
            when(dlqRepository.findById(100L)).thenReturn(Optional.of(testEntry));
            when(dlqRepository.save(any(DeadLetterQueue.class))).thenReturn(testEntry);

            deadLetterQueueService.markResolved(100L);

            assertEquals(DlqStatus.RESOLVED, testEntry.getStatus());
            assertNotNull(testEntry.getResolvedAt());
            verify(dlqRepository).save(testEntry);
        }

        @Test
        @DisplayName("No debe hacer nada si el ID no existe")
        void shouldDoNothingWhenNotFound() {
            when(dlqRepository.findById(999L)).thenReturn(Optional.empty());

            deadLetterQueueService.markResolved(999L);

            verify(dlqRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("markDiscarded")
    class MarkDiscarded {

        @Test
        @DisplayName("Debe marcar como DISCARDED")
        void shouldMarkAsDiscarded() {
            when(dlqRepository.findById(100L)).thenReturn(Optional.of(testEntry));
            when(dlqRepository.save(any(DeadLetterQueue.class))).thenReturn(testEntry);

            deadLetterQueueService.markDiscarded(100L);

            assertEquals(DlqStatus.DISCARDED, testEntry.getStatus());
            assertNotNull(testEntry.getResolvedAt());
            verify(dlqRepository).save(testEntry);
        }
    }

    @Nested
    @DisplayName("incrementRetry")
    class IncrementRetry {

        @Test
        @DisplayName("Debe incrementar retryCount y poner RETRYING si no supera máximo")
        void shouldIncrementAndSetRetrying() {
            testEntry.setRetryCount(0);
            when(dlqRepository.findById(100L)).thenReturn(Optional.of(testEntry));
            when(dlqRepository.save(any(DeadLetterQueue.class))).thenReturn(testEntry);

            deadLetterQueueService.incrementRetry(100L);

            assertEquals(1, testEntry.getRetryCount());
            assertEquals(DlqStatus.RETRYING, testEntry.getStatus());
            assertNotNull(testEntry.getLastRetryAt());
        }

        @Test
        @DisplayName("Debe dejar como PENDING si supera MAX_RETRIES")
        void shouldStayPendingWhenExceedsMaxRetries() {
            testEntry.setRetryCount(2); // después de incrementar será 3 = MAX_RETRIES
            when(dlqRepository.findById(100L)).thenReturn(Optional.of(testEntry));
            when(dlqRepository.save(any(DeadLetterQueue.class))).thenReturn(testEntry);

            deadLetterQueueService.incrementRetry(100L);

            assertEquals(3, testEntry.getRetryCount());
            assertEquals(DlqStatus.PENDING, testEntry.getStatus()); // queda PENDING para revisión manual
        }
    }

    @Nested
    @DisplayName("countPending")
    class CountPending {

        @Test
        @DisplayName("Debe retornar el conteo de pendientes")
        void shouldReturnPendingCount() {
            when(dlqRepository.countByStatus(DlqStatus.PENDING)).thenReturn(5L);

            long count = deadLetterQueueService.countPending();

            assertEquals(5L, count);
            verify(dlqRepository).countByStatus(DlqStatus.PENDING);
        }
    }
}
