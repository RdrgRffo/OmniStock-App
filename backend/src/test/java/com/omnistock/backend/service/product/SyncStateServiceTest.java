package com.omnistock.backend.service.product;

import com.omnistock.backend.dtos.SyncStatus;
import com.omnistock.backend.entity.GlobalSyncState;
import com.omnistock.backend.repository.GlobalSyncStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncStateServiceTest {

    @Mock
    private GlobalSyncStateRepository repository;

    private SyncStateService syncStateService;

    @BeforeEach
    void setUp() {
        syncStateService = new SyncStateService(repository);
    }

    private GlobalSyncState createState(SyncStatus status) {
        GlobalSyncState state = new GlobalSyncState();
        state.setStatus(status);
        return state;
    }


    @Nested
    @DisplayName("getStatus()")
    class GetStatus {

        @Test
        @DisplayName("Debe devolver el estado actual cuando existe")
        void shouldReturnCurrentStatus() {
            when(repository.findById(1L)).thenReturn(Optional.of(createState(SyncStatus.IDLE)));

            SyncStatus status = syncStateService.getStatus();

            assertEquals(SyncStatus.IDLE, status);
        }

        @Test
        @DisplayName("Debe devolver IDLE cuando no existe fila")
        void shouldReturnIdleWhenNoRow() {
            when(repository.findById(1L)).thenReturn(Optional.empty());

            SyncStatus status = syncStateService.getStatus();

            assertEquals(SyncStatus.IDLE, status);
        }
    }

    @Nested
    @DisplayName("tryAcquireLock()")
    class TryAcquireLock {

        @Test
        @DisplayName("Debe adquirir el lock cuando el estado es IDLE")
        void shouldAcquireLockWhenIdle() {
            GlobalSyncState state = createState(SyncStatus.IDLE);
            when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(state));

            boolean acquired = syncStateService.tryAcquireLock();

            assertTrue(acquired);
            assertEquals(SyncStatus.IN_PROGRESS, state.getStatus());
            verify(repository).ensureRowExists();
            verify(repository).save(state);
        }

        @Test
        @DisplayName("Debe adquirir el lock cuando el estado es COMPLETED")
        void shouldAcquireLockWhenCompleted() {
            GlobalSyncState state = createState(SyncStatus.COMPLETED);
            when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(state));

            boolean acquired = syncStateService.tryAcquireLock();

            assertTrue(acquired);
            assertEquals(SyncStatus.IN_PROGRESS, state.getStatus());
        }

        @Test
        @DisplayName("Debe adquirir el lock cuando el estado es ERROR")
        void shouldAcquireLockWhenError() {
            GlobalSyncState state = createState(SyncStatus.ERROR);
            when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(state));

            boolean acquired = syncStateService.tryAcquireLock();

            assertTrue(acquired);
            assertEquals(SyncStatus.IN_PROGRESS, state.getStatus());
        }

        @Test
        @DisplayName("No debe adquirir el lock si ya está IN_PROGRESS")
        void shouldNotAcquireLockWhenInProgress() {
            GlobalSyncState state = createState(SyncStatus.IN_PROGRESS);
            when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(state));

            boolean acquired = syncStateService.tryAcquireLock();

            assertFalse(acquired);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("setStatus()")
    class SetStatus {

        @Test
        @DisplayName("Debe actualizar el estado a COMPLETED")
        void shouldSetStatusToCompleted() {
            GlobalSyncState state = createState(SyncStatus.IN_PROGRESS);
            when(repository.findById(1L)).thenReturn(Optional.of(state));

            syncStateService.setStatus(SyncStatus.COMPLETED);

            assertEquals(SyncStatus.COMPLETED, state.getStatus());
            verify(repository).save(state);
        }

        @Test
        @DisplayName("Debe actualizar el estado a ERROR")
        void shouldSetStatusToError() {
            GlobalSyncState state = createState(SyncStatus.IN_PROGRESS);
            when(repository.findById(1L)).thenReturn(Optional.of(state));

            syncStateService.setStatus(SyncStatus.ERROR);

            assertEquals(SyncStatus.ERROR, state.getStatus());
            verify(repository).save(state);
        }
    }
}
