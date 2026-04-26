package com.omnistock.backend.service.supplier;

import com.omnistock.backend.dtos.supplier.SupplierDashboardDto;
import com.omnistock.backend.dtos.supplier.SupplierRequestDto;
import com.omnistock.backend.dtos.supplier.SupplierResponseDto;
import com.omnistock.backend.dtos.supplier.SupplierSimpleDto;
import com.omnistock.backend.entity.Supplier;
import com.omnistock.backend.repository.ProductSupplierRepository;
import com.omnistock.backend.repository.SupplierRepository;
import com.omnistock.backend.repository.SyncLogRepository;
import com.omnistock.backend.service.auth.EncryptionService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private ProductSupplierRepository productSupplierRepository;
    @Mock
    private SyncLogRepository syncLogRepository;
    @Mock
    private EncryptionService encryptionService;

    @Captor
    private ArgumentCaptor<Supplier> supplierCaptor;

    private SupplierService supplierService;

    @BeforeEach
    void setUp() {
        supplierService = new SupplierService(
                supplierRepository, productSupplierRepository,
                syncLogRepository, encryptionService);
    }

    private Supplier createSupplier(Integer id, String name) {
        Supplier s = new Supplier();
        s.setId(id);
        s.setName(name);
        s.setBaseUrlApi("https://api.test.com");
        s.setActive(true);
        s.setSupportsBulkSync(true);
        s.setCatalogEndpoint("/catalog");
        s.setDetailEndpoint("/detail");
        s.setSearchEndpoint("/search");
        return s;
    }

    @Nested
    @DisplayName("getAllSuppliers")
    class GetAllSuppliers {

        @Test
        @DisplayName("Debe devolver lista de proveedores")
        void shouldReturnAllSuppliers() {
            when(supplierRepository.findAll()).thenReturn(List.of(
                    createSupplier(1, "Proveedor A"),
                    createSupplier(2, "Proveedor B")
            ));

            List<SupplierResponseDto> result = supplierService.getAllSuppliers();

            assertEquals(2, result.size());
            assertEquals("Proveedor A", result.get(0).name());
            assertEquals("Proveedor B", result.get(1).name());
        }

        @Test
        @DisplayName("Debe devolver lista vacía si no hay proveedores")
        void shouldReturnEmptyList() {
            when(supplierRepository.findAll()).thenReturn(List.of());

            List<SupplierResponseDto> result = supplierService.getAllSuppliers();

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getAllSuppliersSimple")
    class GetAllSuppliersSimple {

        @Test
        @DisplayName("Debe devolver solo activos en formato simple")
        void shouldReturnActiveSuppliersSimple() {
            when(supplierRepository.findByActiveTrue()).thenReturn(List.of(
                    createSupplier(1, "Proveedor A"),
                    createSupplier(2, "Proveedor B")
            ));

            List<SupplierSimpleDto> result = supplierService.getAllSuppliersSimple();

            assertEquals(2, result.size());
            assertEquals(1, result.get(0).id());
            assertEquals("Proveedor A", result.get(0).name());
        }
    }

    @Nested
    @DisplayName("getSupplierById")
    class GetSupplierById {

        @Test
        @DisplayName("Debe devolver proveedor por ID")
        void shouldReturnSupplierById() {
            when(supplierRepository.findById(1)).thenReturn(Optional.of(createSupplier(1, "Proveedor A")));

            SupplierResponseDto result = supplierService.getSupplierById(1);

            assertEquals("Proveedor A", result.name());
        }

        @Test
        @DisplayName("Debe lanzar excepción si no existe")
        void shouldThrowWhenNotFound() {
            when(supplierRepository.findById(99)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> supplierService.getSupplierById(99));
        }
    }

    @Nested
    @DisplayName("getById (entity)")
    class GetByIdEntity {

        @Test
        @DisplayName("Debe devolver entidad por ID")
        void shouldReturnEntityById() {
            when(supplierRepository.findById(1)).thenReturn(Optional.of(createSupplier(1, "Proveedor A")));

            Supplier result = supplierService.getById(1);

            assertEquals("Proveedor A", result.getName());
        }

        @Test
        @DisplayName("Debe lanzar excepción si no existe")
        void shouldThrowWhenNotFound() {
            when(supplierRepository.findById(99)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> supplierService.getById(99));
        }
    }

    @Nested
    @DisplayName("createSupplier")
    class CreateSupplier {

        @Test
        @DisplayName("Debe crear proveedor exitosamente")
        void shouldCreateSupplier() {
            SupplierRequestDto dto = new SupplierRequestDto(
                    "Nuevo Proveedor", "https://api.test.com", "Contacto",
                    "email@test.com", "L-V 9-18", "123456789",
                    "www.test.com", "ES", "EUR", "api-key-123",
                    true, true, "/catalog", "/detail", "/search"
            );
            when(supplierRepository.existsByName("Nuevo Proveedor")).thenReturn(false);
            when(encryptionService.encrypt("api-key-123")).thenReturn("encrypted-key");
            when(supplierRepository.save(any(Supplier.class))).thenAnswer(inv -> {
                Supplier s = inv.getArgument(0);
                s.setId(1);
                return s;
            });

            SupplierResponseDto result = supplierService.createSupplier(dto);

            assertEquals("Nuevo Proveedor", result.name());
            verify(supplierRepository).save(supplierCaptor.capture());
            assertEquals("encrypted-key", supplierCaptor.getValue().getApiKey());
        }

        @Test
        @DisplayName("Debe lanzar excepción si el nombre ya existe")
        void shouldThrowWhenNameExists() {
            SupplierRequestDto dto = new SupplierRequestDto(
                    "Existente", "https://api.test.com", null, null, null, null,
                    null, null, null, null, true, false, null, null, null
            );
            when(supplierRepository.existsByName("Existente")).thenReturn(true);

            assertThrows(IllegalArgumentException.class, () -> supplierService.createSupplier(dto));
        }

        @Test
        @DisplayName("Debe encriptar NO_API_KEY si apiKey es blank")
        void shouldEncryptNoApiKeyWhenBlank() {
            SupplierRequestDto dto = new SupplierRequestDto(
                    "Sin Key", "https://api.test.com", null, null, null, null,
                    null, null, null, "", true, false, null, null, null
            );
            when(supplierRepository.existsByName("Sin Key")).thenReturn(false);
            when(encryptionService.encrypt("NO_API_KEY")).thenReturn("encrypted-no-key");
            when(supplierRepository.save(any(Supplier.class))).thenAnswer(inv -> {
                Supplier s = inv.getArgument(0);
                s.setId(2);
                return s;
            });

            SupplierResponseDto result = supplierService.createSupplier(dto);

            assertEquals("Sin Key", result.name());
            verify(encryptionService).encrypt("NO_API_KEY");
        }
    }

    @Nested
    @DisplayName("updateSupplier")
    class UpdateSupplier {

        @Test
        @DisplayName("Debe actualizar proveedor exitosamente")
        void shouldUpdateSupplier() {
            Supplier existing = createSupplier(1, "Original");
            when(supplierRepository.findByIdWithMappings(1)).thenReturn(Optional.of(existing));
            when(encryptionService.encrypt("new-key")).thenReturn("encrypted-new");
            when(supplierRepository.save(any(Supplier.class))).thenReturn(existing);

            SupplierRequestDto dto = new SupplierRequestDto(
                    "Actualizado", "https://new-api.com", null, null, null, null,
                    null, null, null, "new-key", true, false, null, null, null
            );

            SupplierResponseDto result = supplierService.updateSupplier(1, dto);

            assertEquals("Actualizado", result.name());
        }

        @Test
        @DisplayName("Debe lanzar excepción si no existe")
        void shouldThrowWhenNotFound() {
            when(supplierRepository.findByIdWithMappings(99)).thenReturn(Optional.empty());

            SupplierRequestDto dto = new SupplierRequestDto(
                    "Test", null, null, null, null, null,
                    null, null, null, null, true, false, null, null, null
            );

            assertThrows(EntityNotFoundException.class, () -> supplierService.updateSupplier(99, dto));
        }
    }

    @Nested
    @DisplayName("deleteSupplier")
    class DeleteSupplier {

        @Test
        @DisplayName("Debe eliminar proveedor exitosamente")
        void shouldDeleteSupplier() {
            when(supplierRepository.existsById(1)).thenReturn(true);

            supplierService.deleteSupplier(1);

            verify(supplierRepository).deleteById(1);
        }

        @Test
        @DisplayName("Debe lanzar excepción si no existe")
        void shouldThrowWhenNotFound() {
            when(supplierRepository.existsById(99)).thenReturn(false);

            assertThrows(EntityNotFoundException.class, () -> supplierService.deleteSupplier(99));
        }
    }

    @Nested
    @DisplayName("getDashboardData")
    class GetDashboardData {

        @Test
        @DisplayName("Debe calcular métricas del dashboard")
        void shouldCalculateDashboardMetrics() {
            when(supplierRepository.findById(1)).thenReturn(Optional.of(createSupplier(1, "Proveedor A")));
            when(productSupplierRepository.countBySupplierId(1)).thenReturn(100L);
            when(productSupplierRepository.countBySupplierIdAndStockIsZeroOrNull(1)).thenReturn(20L);
            when(syncLogRepository.findAverageLatencyBySupplierId(1)).thenReturn(3600000.0); // 1 hora

            SupplierDashboardDto result = supplierService.getDashboardData(1);

            assertEquals(1, result.supplierId());
            assertEquals("Proveedor A", result.supplierName());
            assertEquals(100, result.totalProducts());
            assertEquals(80, result.productsInStock());
            assertEquals(20, result.productsOutOfStock());
            assertEquals(1.0, result.avgSyncLatencyHours(), 0.001);
        }

        @Test
        @DisplayName("Debe manejar latencia nula")
        void shouldHandleNullLatency() {
            when(supplierRepository.findById(1)).thenReturn(Optional.of(createSupplier(1, "Proveedor A")));
            when(productSupplierRepository.countBySupplierId(1)).thenReturn(50L);
            when(productSupplierRepository.countBySupplierIdAndStockIsZeroOrNull(1)).thenReturn(5L);
            when(syncLogRepository.findAverageLatencyBySupplierId(1)).thenReturn(null);

            SupplierDashboardDto result = supplierService.getDashboardData(1);

            assertEquals(0.0, result.avgSyncLatencyHours(), 0.001);
        }

        @Test
        @DisplayName("Debe lanzar excepción si proveedor no existe")
        void shouldThrowWhenSupplierNotFound() {
            when(supplierRepository.findById(99)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> supplierService.getDashboardData(99));
        }
    }
}
