package com.omnistock.backend.service.dashboard;

import com.omnistock.backend.dtos.dashboard.SupplierHealthDto;
import com.omnistock.backend.repository.ProductSupplierRepository;
import com.omnistock.backend.repository.SupplierRepository;
import com.omnistock.backend.repository.SyncLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierHealthServiceTest {

    @Mock
    private SyncLogRepository syncLogRepository;

    @Mock
    private ProductSupplierRepository productSupplierRepository;

    @Mock
    private SupplierRepository supplierRepository;

    private SupplierHealthService supplierHealthService;

    @BeforeEach
    void setUp() {
        supplierHealthService = new SupplierHealthService(syncLogRepository,
                productSupplierRepository, supplierRepository);
    }

    @Nested
    @DisplayName("getAllSupplierHealth()")
    class GetAllSupplierHealth {

        @Test
        @DisplayName("Debe devolver lista vacía cuando no hay datos")
        void shouldReturnEmptyListWhenNoData() {
            when(syncLogRepository.findSyncSuccessRatePerSupplier()).thenReturn(List.of());
            when(syncLogRepository.findAvgItemsPerSyncPerSupplier()).thenReturn(List.of());
            when(syncLogRepository.findLatencyTrendPerSupplier()).thenReturn(List.of());
            when(productSupplierRepository.findApiErrorRatePerSupplier()).thenReturn(List.of());
            when(productSupplierRepository.findStaleRatePerSupplier()).thenReturn(List.of());

            List<SupplierHealthDto> result = supplierHealthService.getAllSupplierHealth();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Debe calcular KPIs de salud correctamente")
        void shouldCalculateHealthKPIs() {
            // Mock data: supplier 1 with all metrics
            Object[] successRow = {1, "Supplier1", null, null, 95.0};
            Object[] avgItemsRow = {1, null, 15.5};
            Object[] latencyRow = {1, null, null, 500L};
            Object[] errorRow = {1, "Supplier1", null, null, 2.5};
            Object[] staleRow = {1, null, null, null, 5.0};

            when(syncLogRepository.findSyncSuccessRatePerSupplier()).thenReturn(List.<Object[]>of(successRow));
            when(syncLogRepository.findAvgItemsPerSyncPerSupplier()).thenReturn(List.<Object[]>of(avgItemsRow));
            when(syncLogRepository.findLatencyTrendPerSupplier()).thenReturn(List.<Object[]>of(latencyRow));
            when(productSupplierRepository.findApiErrorRatePerSupplier()).thenReturn(List.<Object[]>of(errorRow));
            when(productSupplierRepository.findStaleRatePerSupplier()).thenReturn(List.<Object[]>of(staleRow));

            List<SupplierHealthDto> result = supplierHealthService.getAllSupplierHealth();

            assertThat(result).hasSize(1);
            SupplierHealthDto dto = result.get(0);
            assertThat(dto.supplierId()).isEqualTo(1);
            assertThat(dto.supplierName()).isEqualTo("Supplier1");
            assertThat(dto.syncSuccessRate()).isEqualTo(95.0);
            assertThat(dto.avgItemsPerSync()).isEqualTo(15.5);
            assertThat(dto.avgLatencyMs()).isEqualTo(500L);
            assertThat(dto.apiErrorRate()).isEqualTo(2.5);
            assertThat(dto.staleRate()).isEqualTo(5.0);
            assertThat(dto.slaScore()).isGreaterThan(0);
            assertThat(dto.slaGrade()).isIn("A", "B", "C", "D");
        }

        @Test
        @DisplayName("Debe ordenar por SLA Score descendente")
        void shouldSortBySlaScoreDescending() {
            Object[] successRow1 = {1, "Supplier1", null, null, 95.0};
            Object[] avgItemsRow1 = {1, null, 10.0};
            Object[] latencyRow1 = {1, null, null, 100L};
            Object[] errorRow1 = {1, "Supplier1", null, null, 1.0};
            Object[] staleRow1 = {1, null, null, null, 2.0};

            Object[] successRow2 = {2, "Supplier2", null, null, 50.0};
            Object[] avgItemsRow2 = {2, null, 5.0};
            Object[] latencyRow2 = {2, null, null, 5000L};
            Object[] errorRow2 = {2, "Supplier2", null, null, 20.0};
            Object[] staleRow2 = {2, null, null, null, 30.0};

            when(syncLogRepository.findSyncSuccessRatePerSupplier())
                    .thenReturn(List.<Object[]>of(successRow1, successRow2));
            when(syncLogRepository.findAvgItemsPerSyncPerSupplier())
                    .thenReturn(List.<Object[]>of(avgItemsRow1, avgItemsRow2));
            when(syncLogRepository.findLatencyTrendPerSupplier())
                    .thenReturn(List.<Object[]>of(latencyRow1, latencyRow2));
            when(productSupplierRepository.findApiErrorRatePerSupplier())
                    .thenReturn(List.<Object[]>of(errorRow1, errorRow2));
            when(productSupplierRepository.findStaleRatePerSupplier())
                    .thenReturn(List.<Object[]>of(staleRow1, staleRow2));

            List<SupplierHealthDto> result = supplierHealthService.getAllSupplierHealth();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).slaScore()).isGreaterThan(result.get(1).slaScore());
        }

        @Test
        @DisplayName("Debe manejar valores nulos en los datos")
        void shouldHandleNullValues() {
            Object[] successRow = {1, "Supplier1", null, null, null};
            Object[] avgItemsRow = {1, null, null};
            Object[] latencyRow = {1, null, null, null};
            Object[] errorRow = {1, "Supplier1", null, null, null};
            Object[] staleRow = {1, null, null, null, null};

            when(syncLogRepository.findSyncSuccessRatePerSupplier()).thenReturn(List.<Object[]>of(successRow));
            when(syncLogRepository.findAvgItemsPerSyncPerSupplier()).thenReturn(List.<Object[]>of(avgItemsRow));
            when(syncLogRepository.findLatencyTrendPerSupplier()).thenReturn(List.<Object[]>of(latencyRow));
            when(productSupplierRepository.findApiErrorRatePerSupplier()).thenReturn(List.<Object[]>of(errorRow));
            when(productSupplierRepository.findStaleRatePerSupplier()).thenReturn(List.<Object[]>of(staleRow));

            List<SupplierHealthDto> result = supplierHealthService.getAllSupplierHealth();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).syncSuccessRate()).isEqualTo(0.0);
            assertThat(result.get(0).slaScore()).isGreaterThanOrEqualTo(0);
        }
    }
}
