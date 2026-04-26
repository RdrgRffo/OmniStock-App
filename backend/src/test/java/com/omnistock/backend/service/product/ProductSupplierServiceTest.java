package com.omnistock.backend.service.product;

import com.omnistock.backend.entity.MasterProduct;
import com.omnistock.backend.entity.PriceHistory;
import com.omnistock.backend.entity.ProductSupplier;
import com.omnistock.backend.entity.StockHistory;
import com.omnistock.backend.entity.Supplier;
import com.omnistock.backend.repository.ProductSupplierRepository;
import com.omnistock.backend.repository.StockHistoryRepository;
import com.omnistock.backend.service.notification.NotificationService;
import com.omnistock.backend.service.pricing.PriceHistoryService;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductSupplierServiceTest {

    @Mock
    private ProductSupplierRepository productSupplierRepository;

    @Mock
    private PriceHistoryService priceHistoryService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private StockHistoryRepository stockHistoryRepository;

    private ProductSupplierService productSupplierService;

    private MasterProduct testProduct;
    private Supplier testSupplier;
    private UniversalMapperService.NormalizedData testData;

    @Captor
    private ArgumentCaptor<ProductSupplier> psCaptor;

    @BeforeEach
    void setUp() {
        productSupplierService = new ProductSupplierService(
                productSupplierRepository, priceHistoryService,
                notificationService, stockHistoryRepository);

        testProduct = new MasterProduct();
        testProduct.setId(1);
        testProduct.setMpn("MPN001");
        testProduct.setBrand("TestBrand");
        testProduct.setModel("TestModel");

        testSupplier = new Supplier();
        testSupplier.setId(1);
        testSupplier.setName("TestSupplier");

        testData = new UniversalMapperService.NormalizedData(
                "MPN001", "TestBrand", "TestModel", "Electronics",
                "{\"color\":\"red\"}", "EXT001",
                new BigDecimal("100.0"), new BigDecimal("150.0"),
                10, "1234567890123", 5, "NEW");
    }

    @Nested
    @DisplayName("upsertProductSupplier()")
    class UpsertProductSupplier {

        @Test
        @DisplayName("Debe crear nueva oferta cuando no existe")
        void shouldCreateNewProductSupplier() {
            when(productSupplierRepository.findByMasterProductIdAndSupplierId(1, 1))
                    .thenReturn(Optional.empty());
            when(productSupplierRepository.save(any(ProductSupplier.class)))
                    .thenAnswer(i -> i.getArgument(0));

            ProductSupplier result = productSupplierService.upsertProductSupplier(testProduct, testSupplier, testData);

            assertNotNull(result);
            assertEquals(testProduct, result.getMasterProduct());
            assertEquals(testSupplier, result.getSupplier());
            assertEquals(new BigDecimal("100.0"), result.getPrice());
            assertEquals(Integer.valueOf(10), result.getStock());
            assertEquals(new BigDecimal("150.0"), result.getRetailPrice());
            assertEquals("1234567890123", result.getEan());
            assertEquals(Integer.valueOf(5), result.getMoq());
            assertEquals(ProductSupplier.ProductCondition.NEW, result.getProductCondition());
            assertTrue(result.getAvailable());
            assertTrue(result.isFresh());

            verify(priceHistoryService).registerPriceChange(testProduct, testSupplier, new BigDecimal("100.0"));
            verify(notificationService).createNewProductSupplierNotification(testProduct, testSupplier);
            verify(stockHistoryRepository).save(any(StockHistory.class));
        }

        @Test
        @DisplayName("Debe actualizar oferta existente")
        void shouldUpdateExistingProductSupplier() {
            ProductSupplier existing = new ProductSupplier();
            existing.setId(1);
            existing.setMasterProduct(testProduct);
            existing.setSupplier(testSupplier);
            existing.setPrice(new BigDecimal("50.0"));
            existing.setStock(5);
            existing.setAvailable(true);

            when(productSupplierRepository.findByMasterProductIdAndSupplierId(1, 1))
                    .thenReturn(Optional.of(existing));
            when(productSupplierRepository.save(any(ProductSupplier.class)))
                    .thenAnswer(i -> i.getArgument(0));

            ProductSupplier result = productSupplierService.upsertProductSupplier(testProduct, testSupplier, testData);

            assertEquals(new BigDecimal("100.0"), result.getPrice());
            assertEquals(Integer.valueOf(10), result.getStock());
            assertEquals(new BigDecimal("150.0"), result.getRetailPrice());

            verify(priceHistoryService).registerPriceChange(testProduct, testSupplier, new BigDecimal("100.0"));
            verify(notificationService, never()).createNewProductSupplierNotification(any(), any());
            verify(stockHistoryRepository).save(any(StockHistory.class));
        }

        @Test
        @DisplayName("No debe registrar cambio de precio si el precio no cambió")
        void shouldNotRegisterPriceChangeWhenPriceUnchanged() {
            ProductSupplier existing = new ProductSupplier();
            existing.setId(1);
            existing.setMasterProduct(testProduct);
            existing.setSupplier(testSupplier);
            existing.setPrice(new BigDecimal("100.0"));
            existing.setStock(10);

            when(productSupplierRepository.findByMasterProductIdAndSupplierId(1, 1))
                    .thenReturn(Optional.of(existing));
            when(productSupplierRepository.save(any(ProductSupplier.class)))
                    .thenAnswer(i -> i.getArgument(0));

            productSupplierService.upsertProductSupplier(testProduct, testSupplier, testData);

            verify(priceHistoryService, never()).registerPriceChange(any(), any(), any());
            verify(stockHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar excepción si el producto maestro no tiene ID")
        void shouldThrowWhenMasterProductHasNoId() {
            MasterProduct productWithoutId = new MasterProduct();
            productWithoutId.setMpn("MPN001");

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> productSupplierService.upsertProductSupplier(productWithoutId, testSupplier, testData));

            assertTrue(exception.getMessage().contains("debe tener un ID"));
        }

        @Test
        @DisplayName("Debe mapear condición REFURBISHED correctamente")
        void shouldMapRefurbishedCondition() {
            UniversalMapperService.NormalizedData refurbData = new UniversalMapperService.NormalizedData(
                    "MPN002", "Brand", "Model", null, null, "EXT002",
                    new BigDecimal("80.0"), null, 5, null, null, "Refurbished");

            when(productSupplierRepository.findByMasterProductIdAndSupplierId(1, 1))
                    .thenReturn(Optional.empty());
            when(productSupplierRepository.save(any(ProductSupplier.class)))
                    .thenAnswer(i -> i.getArgument(0));

            ProductSupplier result = productSupplierService.upsertProductSupplier(testProduct, testSupplier, refurbData);

            assertEquals(ProductSupplier.ProductCondition.REFURBISHED, result.getProductCondition());
        }

        @Test
        @DisplayName("Debe mapear condición USED correctamente")
        void shouldMapUsedCondition() {
            UniversalMapperService.NormalizedData usedData = new UniversalMapperService.NormalizedData(
                    "MPN003", "Brand", "Model", null, null, "EXT003",
                    new BigDecimal("60.0"), null, 3, null, null, "Used");

            when(productSupplierRepository.findByMasterProductIdAndSupplierId(1, 1))
                    .thenReturn(Optional.empty());
            when(productSupplierRepository.save(any(ProductSupplier.class)))
                    .thenAnswer(i -> i.getArgument(0));

            ProductSupplier result = productSupplierService.upsertProductSupplier(testProduct, testSupplier, usedData);

            assertEquals(ProductSupplier.ProductCondition.USED, result.getProductCondition());
        }

        @Test
        @DisplayName("Debe mapear condición BOX_DAMAGED correctamente")
        void shouldMapBoxDamagedCondition() {
            UniversalMapperService.NormalizedData damagedData = new UniversalMapperService.NormalizedData(
                    "MPN004", "Brand", "Model", null, null, "EXT004",
                    new BigDecimal("40.0"), null, 1, null, null, "Damaged");

            when(productSupplierRepository.findByMasterProductIdAndSupplierId(1, 1))
                    .thenReturn(Optional.empty());
            when(productSupplierRepository.save(any(ProductSupplier.class)))
                    .thenAnswer(i -> i.getArgument(0));

            ProductSupplier result = productSupplierService.upsertProductSupplier(testProduct, testSupplier, damagedData);

            assertEquals(ProductSupplier.ProductCondition.BOX_DAMAGED, result.getProductCondition());
        }

        @Test
        @DisplayName("No debe registrar stock_history si el stock no cambió")
        void shouldNotRegisterStockHistoryWhenStockUnchanged() {
            ProductSupplier existing = new ProductSupplier();
            existing.setId(1);
            existing.setMasterProduct(testProduct);
            existing.setSupplier(testSupplier);
            existing.setPrice(new BigDecimal("100.0"));
            existing.setStock(10);

            when(productSupplierRepository.findByMasterProductIdAndSupplierId(1, 1))
                    .thenReturn(Optional.of(existing));
            when(productSupplierRepository.save(any(ProductSupplier.class)))
                    .thenAnswer(i -> i.getArgument(0));

            productSupplierService.upsertProductSupplier(testProduct, testSupplier, testData);

            verify(stockHistoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("logProviderError()")
    class LogProviderError {

        @Test
        @DisplayName("Debe registrar error en oferta existente")
        void shouldLogErrorOnExistingProductSupplier() {
            ProductSupplier existing = new ProductSupplier();
            existing.setId(1);
            existing.setMasterProduct(testProduct);
            existing.setSupplier(testSupplier);
            existing.setPrice(new BigDecimal("100.0"));
            existing.setStock(10);

            when(productSupplierRepository.findByMasterProductIdAndSupplierId(1, 1))
                    .thenReturn(Optional.of(existing));
            when(productSupplierRepository.save(any(ProductSupplier.class)))
                    .thenAnswer(i -> i.getArgument(0));

            productSupplierService.logProviderError(testProduct, testSupplier, "Error de conexión");

            verify(productSupplierRepository).save(psCaptor.capture());
            ProductSupplier saved = psCaptor.getValue();
            assertNotNull(saved.getLastError());
            assertTrue(saved.getDataStale());

        }

        @Test
        @DisplayName("Debe crear oferta dummy si no existe para registrar error")
        void shouldCreateDummyEntryWhenNotExists() {
            when(productSupplierRepository.findByMasterProductIdAndSupplierId(1, 1))
                    .thenReturn(Optional.empty());
            when(productSupplierRepository.save(any(ProductSupplier.class)))
                    .thenAnswer(i -> i.getArgument(0));

            productSupplierService.logProviderError(testProduct, testSupplier, "Timeout");

            verify(productSupplierRepository).save(psCaptor.capture());
            ProductSupplier saved = psCaptor.getValue();
            assertEquals(testProduct, saved.getMasterProduct());
            assertEquals(testSupplier, saved.getSupplier());
            assertEquals(BigDecimal.ZERO, saved.getPrice());
            assertEquals(Integer.valueOf(0), saved.getStock());
            assertFalse(saved.getAvailable());
            assertNotNull(saved.getLastError());
            assertTrue(saved.getDataStale());

        }
    }
}
