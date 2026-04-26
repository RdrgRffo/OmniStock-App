package com.omnistock.backend.service.supplier;

import com.omnistock.backend.dtos.supplier.SupplierMappingDto;
import com.omnistock.backend.dtos.supplier.SupplierMappingRequestDto;
import com.omnistock.backend.entity.ProviderMapping;
import com.omnistock.backend.entity.Supplier;
import com.omnistock.backend.repository.ProviderMappingRepository;
import com.omnistock.backend.repository.SupplierRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderMappingServiceTest {

    @Mock
    private ProviderMappingRepository mappingRepository;
    @Mock
    private SupplierRepository supplierRepository;

    private ProviderMappingService mappingService;

    @BeforeEach
    void setUp() {
        mappingService = new ProviderMappingService(mappingRepository, supplierRepository);
    }

    private Supplier createSupplier(Integer id) {
        Supplier s = new Supplier();
        s.setId(id);
        s.setName("Test Supplier");
        return s;
    }

    private ProviderMapping createMapping(Integer id, Supplier supplier, String internalField, String externalField) {
        ProviderMapping pm = new ProviderMapping(supplier, internalField, externalField, "DIRECT");
        setMappingId(pm, id);
        return pm;
    }

    private void setMappingId(ProviderMapping pm, Integer id) {
        try {
            Field field = ProviderMapping.class.getDeclaredField("mappingId");
            field.setAccessible(true);
            field.set(pm, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("getMappingsForProvider")
    class GetMappingsForProvider {

        @Test
        @DisplayName("Debe devolver mappings de un proveedor")
        void shouldReturnMappings() {
            Supplier supplier = createSupplier(1);
            when(supplierRepository.existsById(1)).thenReturn(true);
            ProviderMapping pm1 = createMapping(1, supplier, "mpn", "sku");
            ProviderMapping pm2 = createMapping(2, supplier, "price", "precio");
            when(mappingRepository.findBySupplier_Id(1)).thenReturn(List.of(pm1, pm2));

            List<SupplierMappingDto> result = mappingService.getMappingsForProvider(1);

            assertEquals(2, result.size());
            assertEquals("mpn", result.get(0).internalField());
            assertEquals("sku", result.get(0).externalField());
        }

        @Test
        @DisplayName("Debe lanzar excepción si proveedor no existe")
        void shouldThrowWhenSupplierNotFound() {
            when(supplierRepository.existsById(99)).thenReturn(false);

            assertThrows(EntityNotFoundException.class, () -> mappingService.getMappingsForProvider(99));
        }

        @Test
        @DisplayName("Debe devolver lista vacía si no hay mappings")
        void shouldReturnEmptyList() {
            when(supplierRepository.existsById(1)).thenReturn(true);
            when(mappingRepository.findBySupplier_Id(1)).thenReturn(List.of());

            List<SupplierMappingDto> result = mappingService.getMappingsForProvider(1);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("createMapping")
    class CreateMapping {

        @Test
        @DisplayName("Debe crear mapping exitosamente")
        void shouldCreateMapping() {
            Supplier supplier = createSupplier(1);
            when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));
            when(mappingRepository.findBySupplier_IdAndInternalField(1, "mpn")).thenReturn(Optional.empty());
            when(mappingRepository.save(any(ProviderMapping.class))).thenAnswer(inv -> inv.getArgument(0));

            SupplierMappingRequestDto dto = new SupplierMappingRequestDto("mpn", "sku", "DIRECT");
            SupplierMappingDto result = mappingService.createMapping(1, dto);

            assertEquals("mpn", result.internalField());
            assertEquals("sku", result.externalField());
        }

        @Test
        @DisplayName("Debe lanzar excepción si proveedor no existe")
        void shouldThrowWhenSupplierNotFound() {
            when(supplierRepository.findById(99)).thenReturn(Optional.empty());

            SupplierMappingRequestDto dto = new SupplierMappingRequestDto("mpn", "sku", "DIRECT");
            assertThrows(EntityNotFoundException.class, () -> mappingService.createMapping(99, dto));
        }

        @Test
        @DisplayName("Debe lanzar excepción si el campo interno ya existe")
        void shouldThrowWhenInternalFieldExists() {
            Supplier supplier = createSupplier(1);
            when(supplierRepository.findById(1)).thenReturn(Optional.of(supplier));
            when(mappingRepository.findBySupplier_IdAndInternalField(1, "mpn"))
                    .thenReturn(Optional.of(new ProviderMapping(supplier, "mpn", "sku", "DIRECT")));

            SupplierMappingRequestDto dto = new SupplierMappingRequestDto("mpn", "otro", "DIRECT");
            assertThrows(IllegalArgumentException.class, () -> mappingService.createMapping(1, dto));
        }
    }

    @Nested
    @DisplayName("updateMapping")
    class UpdateMapping {

        @Test
        @DisplayName("Debe actualizar mapping exitosamente")
        void shouldUpdateMapping() {
            Supplier supplier = createSupplier(1);
            ProviderMapping existing = new ProviderMapping(supplier, "mpn", "sku", "DIRECT");
            setMappingId(existing, 1);
            when(mappingRepository.findById(1)).thenReturn(Optional.of(existing));
            when(mappingRepository.findBySupplier_IdAndInternalField(1, "brand")).thenReturn(Optional.empty());
            when(mappingRepository.save(any(ProviderMapping.class))).thenReturn(existing);

            SupplierMappingRequestDto dto = new SupplierMappingRequestDto("brand", "marca", "DIRECT");
            SupplierMappingDto result = mappingService.updateMapping(1, dto);

            assertEquals("brand", result.internalField());
            assertEquals("marca", result.externalField());
        }

        @Test
        @DisplayName("Debe lanzar excepción si mapping no existe")
        void shouldThrowWhenMappingNotFound() {
            when(mappingRepository.findById(99)).thenReturn(Optional.empty());

            SupplierMappingRequestDto dto = new SupplierMappingRequestDto("mpn", "sku", "DIRECT");
            assertThrows(EntityNotFoundException.class, () -> mappingService.updateMapping(99, dto));
        }

        @Test
        @DisplayName("Debe lanzar excepción si otro mapping ya tiene el campo interno")
        void shouldThrowWhenOtherMappingHasInternalField() {
            Supplier supplier = createSupplier(1);
            ProviderMapping existing = new ProviderMapping(supplier, "mpn", "sku", "DIRECT");
            setMappingId(existing, 1);
            ProviderMapping other = new ProviderMapping(supplier, "brand", "marca", "DIRECT");
            setMappingId(other, 2);
            when(mappingRepository.findById(1)).thenReturn(Optional.of(existing));
            when(mappingRepository.findBySupplier_IdAndInternalField(1, "brand"))
                    .thenReturn(Optional.of(other));

            SupplierMappingRequestDto dto = new SupplierMappingRequestDto("brand", "marca", "DIRECT");
            assertThrows(IllegalArgumentException.class, () -> mappingService.updateMapping(1, dto));
        }

        @Test
        @DisplayName("Debe permitir actualizar al mismo campo interno")
        void shouldAllowUpdatingSameInternalField() {
            Supplier supplier = createSupplier(1);
            ProviderMapping existing = new ProviderMapping(supplier, "mpn", "sku", "DIRECT");
            setMappingId(existing, 1);
            when(mappingRepository.findById(1)).thenReturn(Optional.of(existing));
            when(mappingRepository.findBySupplier_IdAndInternalField(1, "mpn"))
                    .thenReturn(Optional.of(existing)); // mismo mappingId
            when(mappingRepository.save(any(ProviderMapping.class))).thenReturn(existing);

            SupplierMappingRequestDto dto = new SupplierMappingRequestDto("mpn", "new-sku", "DIRECT");
            SupplierMappingDto result = mappingService.updateMapping(1, dto);

            assertEquals("mpn", result.internalField());
            assertEquals("new-sku", result.externalField());
        }
    }

    @Nested
    @DisplayName("deleteMapping")
    class DeleteMapping {

        @Test
        @DisplayName("Debe eliminar mapping exitosamente")
        void shouldDeleteMapping() {
            when(mappingRepository.existsById(1)).thenReturn(true);

            mappingService.deleteMapping(1);

            verify(mappingRepository).deleteById(1);
        }

        @Test
        @DisplayName("Debe lanzar excepción si mapping no existe")
        void shouldThrowWhenNotFound() {
            when(mappingRepository.existsById(99)).thenReturn(false);

            assertThrows(EntityNotFoundException.class, () -> mappingService.deleteMapping(99));
        }
    }
}
