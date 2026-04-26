package com.omnistock.backend.service.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnistock.backend.dtos.SyncStatus;
import com.omnistock.backend.entity.MasterProduct;
import com.omnistock.backend.entity.Supplier;
import com.omnistock.backend.repository.MasterProductRepository;
import com.omnistock.backend.repository.SupplierRepository;
import com.omnistock.backend.service.integration.ApiClientService;
import com.omnistock.backend.service.integration.DeadLetterQueueService;
import com.omnistock.backend.service.notification.NotificationService;
import com.omnistock.backend.service.transformation.UniversalMapperService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StockCatalogSyncServiceTest {

    @Mock
    private MasterProductRepository masterProductRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private ApiClientService apiClientService;
    @Mock
    private UniversalMapperService universalMapperService;
    @Mock
    private ProductSupplierService productSupplierService;
    @Mock
    private SyncStateService syncStateService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private DeadLetterQueueService deadLetterQueueService;

    private ObjectMapper objectMapper;
    private StockCatalogSyncService stockCatalogSyncService;

    private Supplier testSupplier;
    private UniversalMapperService.NormalizedData testData;

    @Captor
    private ArgumentCaptor<MasterProduct> masterCaptor;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        stockCatalogSyncService = new StockCatalogSyncService(
                masterProductRepository, supplierRepository, apiClientService,
                universalMapperService, productSupplierService, syncStateService,
                notificationService, deadLetterQueueService, objectMapper);

        testSupplier = new Supplier();
        testSupplier.setId(1);
        testSupplier.setName("Test Supplier");

        testData = new UniversalMapperService.NormalizedData(
                "MPN001", "TestBrand", "TestModel", "Electronics",
                "{\"color\":\"red\",\"weight\":\"1kg\"}", "EXT001",
                new BigDecimal("100.0"), null, 10, null, 5, "NEW");

        // Establecer pageSize manualmente ya que @Value no se inyecta en tests unitarios
        ReflectionTestUtils.setField(stockCatalogSyncService, "pageSize", 500);
    }

    @Nested
    @DisplayName("syncAllProducts")
    class SyncAllProducts {

        @Test
        @DisplayName("Debe omitir si el lock no está disponible")
        void shouldSkipWhenLockNotAvailable() {
            when(syncStateService.tryAcquireLock()).thenReturn(false);

            stockCatalogSyncService.syncAllProducts();

            verify(supplierRepository, never()).findAllByActiveTrueWithMappings();
            verify(syncStateService, never()).setStatus(any());
        }

        @Test
        @DisplayName("Debe sincronizar todos los proveedores activos")
        void shouldSyncAllActiveSuppliers() {
            when(syncStateService.tryAcquireLock()).thenReturn(true);
            when(supplierRepository.findAllByActiveTrueWithMappings()).thenReturn(List.of(testSupplier));
            when(apiClientService.fetchCatalog(testSupplier)).thenReturn("[]");
            when(universalMapperService.normalizeResponse("[]", testSupplier)).thenReturn(List.of());

            stockCatalogSyncService.syncAllProducts();

            verify(syncStateService).setStatus(SyncStatus.COMPLETED);
        }

        @Test
        @DisplayName("Debe marcar ERROR si ocurre una excepción")
        void shouldSetErrorOnException() {
            when(syncStateService.tryAcquireLock()).thenReturn(true);
            when(supplierRepository.findAllByActiveTrueWithMappings()).thenThrow(new RuntimeException("DB error"));

            stockCatalogSyncService.syncAllProducts();

            verify(syncStateService).setStatus(SyncStatus.ERROR);
        }
    }

    @Nested
    @DisplayName("syncProviderCatalog")
    class SyncProviderCatalog {

        @Test
        @DisplayName("Debe procesar productos del catálogo")
        void shouldProcessCatalogProducts() {
            when(apiClientService.fetchCatalog(testSupplier)).thenReturn("[{}]");
            when(universalMapperService.normalizeResponse("[{}]", testSupplier)).thenReturn(List.of(testData));
            when(masterProductRepository.findByMpn("MPN001")).thenReturn(Optional.empty());
            when(masterProductRepository.save(any(MasterProduct.class))).thenAnswer(i -> i.getArgument(0));

            stockCatalogSyncService.syncProviderCatalog(testSupplier);

            verify(masterProductRepository).save(masterCaptor.capture());
            MasterProduct saved = masterCaptor.getValue();
            assertEquals("MPN001", saved.getMpn());
            assertEquals("TestBrand", saved.getBrand());
            verify(productSupplierService).upsertProductSupplier(any(), eq(testSupplier), eq(testData));
        }

        @Test
        @DisplayName("Debe actualizar producto existente en lugar de crear nuevo")
        void shouldUpdateExistingProduct() {
            MasterProduct existing = new MasterProduct();
            existing.setMpn("MPN001");
            existing.setBrand("OldBrand");

            when(apiClientService.fetchCatalog(testSupplier)).thenReturn("[{}]");
            when(universalMapperService.normalizeResponse("[{}]", testSupplier)).thenReturn(List.of(testData));
            when(masterProductRepository.findByMpn("MPN001")).thenReturn(Optional.of(existing));
            when(masterProductRepository.save(any(MasterProduct.class))).thenAnswer(i -> i.getArgument(0));

            stockCatalogSyncService.syncProviderCatalog(testSupplier);

            verify(masterProductRepository).save(masterCaptor.capture());
            assertEquals("TestBrand", masterCaptor.getValue().getBrand());
            // No debe llamar a createNewProductGlobalNotification porque ya existía
            verify(notificationService, never()).createNewProductGlobalNotification(any(), any());
        }

        @Test
        @DisplayName("Debe enviar a DLQ si un producto falla (partial sync)")
        void shouldSendToDlqOnProductFailure() {
            when(apiClientService.fetchCatalog(testSupplier)).thenReturn("[{}]");
            when(universalMapperService.normalizeResponse("[{}]", testSupplier)).thenReturn(List.of(testData));
            when(masterProductRepository.findByMpn("MPN001")).thenThrow(new RuntimeException("DB connection error"));

            stockCatalogSyncService.syncProviderCatalog(testSupplier);

            verify(deadLetterQueueService).recordFailure(
                    eq(testSupplier), eq("MPN001"), anyString(), anyString(), eq("UNKNOWN"));
        }

        @Test
        @DisplayName("Debe clasificar error TIMEOUT correctamente")
        void shouldClassifyTimeoutError() {
            when(apiClientService.fetchCatalog(testSupplier)).thenReturn("[{}]");
            when(universalMapperService.normalizeResponse("[{}]", testSupplier)).thenReturn(List.of(testData));
            when(masterProductRepository.findByMpn("MPN001")).thenThrow(new RuntimeException("Connection timed out"));

            stockCatalogSyncService.syncProviderCatalog(testSupplier);

            verify(deadLetterQueueService).recordFailure(
                    eq(testSupplier), eq("MPN001"), anyString(), anyString(), eq("TIMEOUT"));
        }

        @Test
        @DisplayName("Debe clasificar error PARSING correctamente")
        void shouldClassifyParsingError() {
            when(apiClientService.fetchCatalog(testSupplier)).thenReturn("[{}]");
            when(universalMapperService.normalizeResponse("[{}]", testSupplier)).thenReturn(List.of(testData));
            when(masterProductRepository.findByMpn("MPN001")).thenThrow(new RuntimeException("Unexpected character"));

            stockCatalogSyncService.syncProviderCatalog(testSupplier);

            verify(deadLetterQueueService).recordFailure(
                    eq(testSupplier), eq("MPN001"), anyString(), anyString(), eq("PARSING"));
        }

        @Test
        @DisplayName("Debe continuar procesando si un producto falla (partial sync)")
        void shouldContinueOnProductFailure() {
            UniversalMapperService.NormalizedData data2 = new UniversalMapperService.NormalizedData(
                    "MPN002", "Brand2", null, null, null, null, null, null, null, null, null, null);

            when(apiClientService.fetchCatalog(testSupplier)).thenReturn("[{},{}]");
            when(universalMapperService.normalizeResponse("[{},{}]", testSupplier)).thenReturn(List.of(testData, data2));
            when(masterProductRepository.findByMpn("MPN001")).thenThrow(new RuntimeException("Error"));
            when(masterProductRepository.findByMpn("MPN002")).thenReturn(Optional.empty());
            when(masterProductRepository.save(any(MasterProduct.class))).thenAnswer(i -> i.getArgument(0));

            stockCatalogSyncService.syncProviderCatalog(testSupplier);

            // MPN001 falló → DLQ, MPN002 se procesó bien
            verify(deadLetterQueueService).recordFailure(any(), eq("MPN001"), anyString(), anyString(), anyString());
            verify(productSupplierService).upsertProductSupplier(any(), eq(testSupplier), eq(data2));
        }

        @Test
        @DisplayName("Debe manejar catálogo vacío")
        void shouldHandleEmptyCatalog() {
            when(apiClientService.fetchCatalog(testSupplier)).thenReturn("[]");
            when(universalMapperService.normalizeResponse("[]", testSupplier)).thenReturn(List.of());

            stockCatalogSyncService.syncProviderCatalog(testSupplier);

            verify(masterProductRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe ignorar productos sin MPN")
        void shouldSkipProductsWithoutMpn() {
            UniversalMapperService.NormalizedData noMpn = new UniversalMapperService.NormalizedData(
                    null, "Brand", null, null, null, null, null, null, null, null, null, null);

            when(apiClientService.fetchCatalog(testSupplier)).thenReturn("[{}]");
            when(universalMapperService.normalizeResponse("[{}]", testSupplier)).thenReturn(List.of(noMpn));

            stockCatalogSyncService.syncProviderCatalog(testSupplier);

            verify(masterProductRepository, never()).save(any());
        }
    }
}
