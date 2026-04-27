package com.omnistock.backend.service.pricing;

import com.omnistock.backend.dtos.pricing.PriceHistoryItemDto;
import com.omnistock.backend.dtos.pricing.PriceStatisticsDto;
import com.omnistock.backend.entity.MasterProduct;
import com.omnistock.backend.entity.PriceHistory;
import com.omnistock.backend.entity.Supplier;
import com.omnistock.backend.repository.MasterProductRepository;
import com.omnistock.backend.repository.PriceHistoryRepository;
import com.omnistock.backend.repository.SupplierRepository;
import com.omnistock.backend.service.notification.NotificationService;
import com.omnistock.backend.service.pricing.PriceHistoryService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceHistoryServiceTest {

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private MasterProductRepository masterProductRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private NotificationService notificationService;

    private PriceHistoryService priceHistoryService;

    private MasterProduct testProduct;
    private Supplier testSupplier;

    @BeforeEach
    void setUp() {
        priceHistoryService = new PriceHistoryService(priceHistoryRepository, masterProductRepository,
                supplierRepository, notificationService);

        testProduct = new MasterProduct();
        testProduct.setId(1);
        testProduct.setModel("TestModel");
        testProduct.setBrand("TestBrand");

        testSupplier = new Supplier();
        testSupplier.setId(1);
        testSupplier.setName("TestSupplier");
    }

    @Nested
    @DisplayName("registerPriceChange()")
    class RegisterPriceChange {

        @Test
        @DisplayName("Debe registrar cambio de precio cuando no hay historial previo")
        void shouldRegisterPriceChangeWhenNoHistory() {
            when(priceHistoryRepository.findFirstByMasterProductAndSupplierOrderByRegisteredAtDesc(
                    testProduct, testSupplier)).thenReturn(Optional.empty());

            priceHistoryService.registerPriceChange(testProduct, testSupplier, new BigDecimal("100.00"));

            verify(priceHistoryRepository).save(any(PriceHistory.class));
            verify(notificationService, never()).createOfferNotification(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Debe registrar cambio cuando el precio es diferente")
        void shouldRegisterPriceChangeWhenPriceDiffers() {
            PriceHistory last = new PriceHistory(testProduct, testSupplier, new BigDecimal("80.00"));
            last.setRegisteredAt(LocalDateTime.now().minusDays(1));
            when(priceHistoryRepository.findFirstByMasterProductAndSupplierOrderByRegisteredAtDesc(
                    testProduct, testSupplier)).thenReturn(Optional.of(last));

            priceHistoryService.registerPriceChange(testProduct, testSupplier, new BigDecimal("100.00"));

            verify(priceHistoryRepository).save(any(PriceHistory.class));
        }

        @Test
        @DisplayName("No debe registrar cambio cuando el precio es igual")
        void shouldNotRegisterWhenPriceIsSame() {
            PriceHistory last = new PriceHistory(testProduct, testSupplier, new BigDecimal("100.00"));
            last.setRegisteredAt(LocalDateTime.now().minusDays(1));
            when(priceHistoryRepository.findFirstByMasterProductAndSupplierOrderByRegisteredAtDesc(
                    testProduct, testSupplier)).thenReturn(Optional.of(last));

            priceHistoryService.registerPriceChange(testProduct, testSupplier, new BigDecimal("100.00"));

            verify(priceHistoryRepository, never()).save(any(PriceHistory.class));
        }

        @Test
        @DisplayName("Debe crear notificación de oferta cuando el precio baja")
        void shouldCreateOfferNotificationWhenPriceDrops() {
            PriceHistory last = new PriceHistory(testProduct, testSupplier, new BigDecimal("100.00"));
            last.setRegisteredAt(LocalDateTime.now().minusDays(1));
            when(priceHistoryRepository.findFirstByMasterProductAndSupplierOrderByRegisteredAtDesc(
                    testProduct, testSupplier)).thenReturn(Optional.of(last));

            priceHistoryService.registerPriceChange(testProduct, testSupplier, new BigDecimal("80.00"));

            verify(priceHistoryRepository).save(any(PriceHistory.class));
            verify(notificationService).createOfferNotification(testProduct, testSupplier,
                    new BigDecimal("100.00"), new BigDecimal("80.00"));
        }

        @Test
        @DisplayName("No debe crear notificación cuando el precio sube")
        void shouldNotCreateOfferNotificationWhenPriceRises() {
            PriceHistory last = new PriceHistory(testProduct, testSupplier, new BigDecimal("80.00"));
            last.setRegisteredAt(LocalDateTime.now().minusDays(1));
            when(priceHistoryRepository.findFirstByMasterProductAndSupplierOrderByRegisteredAtDesc(
                    testProduct, testSupplier)).thenReturn(Optional.of(last));

            priceHistoryService.registerPriceChange(testProduct, testSupplier, new BigDecimal("100.00"));

            verify(priceHistoryRepository).save(any(PriceHistory.class));
            verify(notificationService, never()).createOfferNotification(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("getHistoryDto()")
    class GetHistoryDto {

        @Test
        @DisplayName("Debe devolver historial con tendencias calculadas")
        void shouldReturnHistoryWithTrends() {
            when(masterProductRepository.findById(1)).thenReturn(Optional.of(testProduct));
            when(supplierRepository.findById(1)).thenReturn(Optional.of(testSupplier));

            PriceHistory h1 = new PriceHistory(testProduct, testSupplier, new BigDecimal("100.00"));
            h1.setRegisteredAt(LocalDateTime.now().minusDays(2));
            PriceHistory h2 = new PriceHistory(testProduct, testSupplier, new BigDecimal("80.00"));
            h2.setRegisteredAt(LocalDateTime.now().minusDays(1));

            when(priceHistoryRepository.findByMasterProductAndSupplierOrderByRegisteredAtDesc(
                    testProduct, testSupplier)).thenReturn(List.of(h2, h1));

            List<PriceHistoryItemDto> result = priceHistoryService.getHistoryDto(1, 1);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).trend()).isEqualTo("BAJA ↓"); // h2 (80.00) vs h1 (100.00)
        }

        @Test
        @DisplayName("Debe lanzar excepción si el producto no existe")
        void shouldThrowWhenProductNotFound() {
            when(masterProductRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> priceHistoryService.getHistoryDto(999, 1))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Producto no encontrado");
        }

        @Test
        @DisplayName("Debe lanzar excepción si el proveedor no existe")
        void shouldThrowWhenSupplierNotFound() {
            when(masterProductRepository.findById(1)).thenReturn(Optional.of(testProduct));
            when(supplierRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> priceHistoryService.getHistoryDto(1, 999))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Proveedor no encontrado");
        }
    }

    @Nested
    @DisplayName("computeStatistics()")
    class ComputeStatistics {

        @Test
        @DisplayName("Debe devolver estadísticas vacías para historial vacío")
        void shouldReturnEmptyForEmptyHistory() {
            PriceStatisticsDto stats = priceHistoryService.computeStatistics(List.of(), testProduct, testSupplier);

            assertThat(stats.averagePrice()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(stats.minPrice()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(stats.maxPrice()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(stats.totalRecords()).isEqualTo(0);
        }

        @Test
        @DisplayName("Debe calcular estadísticas correctamente")
        void shouldComputeStatisticsCorrectly() {
            PriceHistory h1 = new PriceHistory(testProduct, testSupplier, new BigDecimal("100.00"));
            h1.setRegisteredAt(LocalDateTime.now().minusDays(2));
            PriceHistory h2 = new PriceHistory(testProduct, testSupplier, new BigDecimal("80.00"));
            h2.setRegisteredAt(LocalDateTime.now().minusDays(1));
            PriceHistory h3 = new PriceHistory(testProduct, testSupplier, new BigDecimal("120.00"));
            h3.setRegisteredAt(LocalDateTime.now());

            List<PriceHistory> history = List.of(h1, h2, h3);

            PriceStatisticsDto stats = priceHistoryService.computeStatistics(history, testProduct, testSupplier);

            assertThat(stats.averagePrice()).isEqualByComparingTo(new BigDecimal("100.00")); // (100+80+120)/3
            assertThat(stats.minPrice()).isEqualByComparingTo(new BigDecimal("80.00"));
            assertThat(stats.maxPrice()).isEqualByComparingTo(new BigDecimal("120.00"));
            assertThat(stats.totalRecords()).isEqualTo(3);
            assertThat(stats.productName()).isEqualTo("TestModel");
            assertThat(stats.supplierName()).isEqualTo("TestSupplier");
        }
    }

    @Nested
    @DisplayName("getHistoryForProduct()")
    class GetHistoryForProduct {

        @Test
        @DisplayName("Debe devolver historial para producto y proveedor")
        void shouldReturnHistoryForProductAndSupplier() {
            PriceHistory h = new PriceHistory(testProduct, testSupplier, new BigDecimal("100.00"));
            when(priceHistoryRepository.findByMasterProductAndSupplierOrderByRegisteredAtDesc(
                    testProduct, testSupplier)).thenReturn(List.of(h));

            List<PriceHistory> result = priceHistoryService.getHistoryForProduct(testProduct, testSupplier);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCostPrice()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("Debe devolver historial para producto sin proveedor")
        void shouldReturnHistoryForProductWithoutSupplier() {
            when(priceHistoryRepository.findByMasterProductAndSupplierOrderByRegisteredAtDesc(
                    testProduct, null)).thenReturn(List.of());

            List<PriceHistory> result = priceHistoryService.getHistoryForProduct(testProduct, null);

            assertThat(result).isEmpty();
        }
    }
}
