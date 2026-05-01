package com.omnistock.backend.service.transformation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnistock.backend.entity.ProviderMapping;
import com.omnistock.backend.entity.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UniversalMapperServiceTest {

    private UniversalMapperService mapperService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // Create a simple DIRECT strategy for testing
        TransformationStrategy direct = new TransformationStrategy() {
            @Override
            public JsonNode transform(JsonNode root, String path) {
                if (path == null || path.isBlank()) return null;
                String[] parts = path.split("\\.");
                JsonNode current = root;
                for (String part : parts) {
                    if (current != null && current.isObject()) {
                        current = current.get(part);
                    } else {
                        return null;
                    }
                }
                return current;
            }

            @Override
            public String getStrategyName() {
                return "DIRECT";
            }
        };
        mapperService = new UniversalMapperService(objectMapper, List.of(direct));
    }

    private Supplier createSupplierWithMappings(Map<String, String> fieldMappings) {
        Supplier supplier = new Supplier();
        supplier.setId(1);
        supplier.setName("Test Supplier");

        List<ProviderMapping> mappings = new java.util.ArrayList<>();
        for (Map.Entry<String, String> entry : fieldMappings.entrySet()) {
            ProviderMapping mapping = new ProviderMapping();
            mapping.setInternalField(entry.getKey());
            mapping.setExternalField(entry.getValue());
            mapping.setTransformationType("DIRECT");
            mappings.add(mapping);
        }
        supplier.setMappingConfigurations(mappings);
        return supplier;
    }

    @Nested
    @DisplayName("normalizeResponse")
    class NormalizeResponse {

        @Test
        @DisplayName("Debe normalizar un array de items correctamente")
        void shouldNormalizeArrayItems() {
            String json = """
                    [
                        {
                            "mpn": "MPN001",
                            "brand": "BrandX",
                            "model": "ModelX",
                            "price": 100.50,
                            "stock": 10
                        }
                    ]
                    """;

            Supplier supplier = createSupplierWithMappings(Map.of(
                "mpn", "mpn",
                "brand", "brand",
                "model", "model",
                "price", "price",
                "stock", "stock"
            ));

            List<UniversalMapperService.NormalizedData> results = mapperService.normalizeResponse(json, supplier);

            assertEquals(1, results.size());
            assertEquals("MPN001", results.get(0).mpn());
            assertEquals("BrandX", results.get(0).brand());
            assertEquals("ModelX", results.get(0).model());
            assertTrue(results.get(0).price().compareTo(new BigDecimal("100.50")) == 0);
            assertEquals(Integer.valueOf(10), results.get(0).stock());
        }

        @Test
        @DisplayName("Debe normalizar items dentro de un contenedor 'data'")
        void shouldNormalizeDataContainer() {
            String json = """
                    {
                        "data": [
                            {
                                "id": "EXT001",
                                "name": "Product A",
                                "precio": 200.00
                            }
                        ]
                    }
                    """;

            Supplier supplier = createSupplierWithMappings(Map.of(
                "mpn", "id",
                "model", "name",
                "price", "precio"
            ));

            List<UniversalMapperService.NormalizedData> results = mapperService.normalizeResponse(json, supplier);

            assertEquals(1, results.size());
            assertEquals("EXT001", results.get(0).mpn());
            assertEquals("Product A", results.get(0).model());
        }

        @Test
        @DisplayName("Debe ignorar items sin MPN")
        void shouldSkipItemsWithoutMpn() {
            String json = """
                    [
                        { "name": "No MPN" },
                        { "mpn": "MPN001", "brand": "BrandX" }
                    ]
                    """;

            Supplier supplier = createSupplierWithMappings(Map.of(
                "mpn", "mpn",
                "brand", "brand"
            ));

            List<UniversalMapperService.NormalizedData> results = mapperService.normalizeResponse(json, supplier);

            assertEquals(1, results.size());
            assertEquals("MPN001", results.get(0).mpn());
        }

        @Test
        @DisplayName("Debe devolver lista vacía para JSON inválido")
        void shouldReturnEmptyForInvalidJson() {
            Supplier supplier = createSupplierWithMappings(Map.of("mpn", "mpn"));
            List<UniversalMapperService.NormalizedData> results = mapperService.normalizeResponse("invalid json", supplier);
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Debe extraer especificaciones automáticas")
        void shouldExtractAutoSpecs() {
            String json = """
                    [
                        {
                            "mpn": "MPN001",
                            "brand": "BrandX",
                            "weight": "1.5kg",
                            "color": "red",
                            "price": 100
                        }
                    ]
                    """;

            Supplier supplier = createSupplierWithMappings(Map.of(
                "mpn", "mpn",
                "brand", "brand",
                "price", "price"
            ));

            List<UniversalMapperService.NormalizedData> results = mapperService.normalizeResponse(json, supplier);

            assertEquals(1, results.size());
            String specs = results.get(0).techSpecs();
            assertTrue(specs.contains("weight"));
            assertTrue(specs.contains("color"));
            assertFalse(specs.contains("price")); // price is a commercial field
        }

        @Test
        @DisplayName("Debe manejar root_list mapping")
        void shouldHandleRootListMapping() {
            String json = """
                    {
                        "products": [
                            { "mpn": "MPN001", "brand": "BrandX" }
                        ]
                    }
                    """;

            Supplier supplier = createSupplierWithMappings(Map.of(
                "mpn", "mpn",
                "brand", "brand",
                "root_list", "products"
            ));

            List<UniversalMapperService.NormalizedData> results = mapperService.normalizeResponse(json, supplier);
            assertEquals(1, results.size());
            assertEquals("MPN001", results.get(0).mpn());
        }
    }

    @Nested
    @DisplayName("NormalizedData record")
    class NormalizedDataRecord {

        @Test
        @DisplayName("Debe crear registro con todos los campos")
        void shouldCreateRecordWithAllFields() {
            UniversalMapperService.NormalizedData data = new UniversalMapperService.NormalizedData(
                "MPN001", "BrandX", "ModelX", "CategoryX",
                "{\"weight\":\"1.5kg\"}", "EXT001",
                BigDecimal.valueOf(100), BigDecimal.valueOf(150),
                10, "1234567890123", 5, "NEW"
            );

            assertEquals("MPN001", data.mpn());
            assertEquals("BrandX", data.brand());
            assertEquals("ModelX", data.model());
            assertEquals("CategoryX", data.category());
            assertEquals("EXT001", data.externalId());
            assertEquals(BigDecimal.valueOf(100), data.price());
            assertEquals(BigDecimal.valueOf(150), data.retailPrice());
            assertEquals(Integer.valueOf(10), data.stock());
            assertEquals("1234567890123", data.ean());
            assertEquals(Integer.valueOf(5), data.moq());
            assertEquals("NEW", data.condition());
        }
    }

    @Nested
    @DisplayName("extractString / extractDecimal / extractInteger")
    class ExtractionMethods {

        @Test
        @DisplayName("Debe extraer string correctamente")
        void shouldExtractString() {
            String json = """
                    [{ "name": "Product A", "price": "invalid", "stock": "abc" }]
                    """;

            Supplier supplier = createSupplierWithMappings(Map.of(
                "mpn", "name",
                "price", "price",
                "stock", "stock"
            ));

            List<UniversalMapperService.NormalizedData> results = mapperService.normalizeResponse(json, supplier);

            assertEquals(1, results.size());
            assertEquals("Product A", results.get(0).mpn());
            // price is invalid decimal, should be null
            assertNull(results.get(0).price());
            // stock "abc" -> asInt() returns 0, not null, so we check it's 0
            assertEquals(Integer.valueOf(0), results.get(0).stock());
        }


        @Test
        @DisplayName("Debe extraer precio con coma decimal")
        void shouldExtractPriceWithComma() {
            String json = """
                    [{ "mpn": "MPN001", "price": "99,90", "stock": 5 }]
                    """;

            Supplier supplier = createSupplierWithMappings(Map.of(
                "mpn", "mpn",
                "price", "price",
                "stock", "stock"
            ));

            List<UniversalMapperService.NormalizedData> results = mapperService.normalizeResponse(json, supplier);

            assertEquals(1, results.size());
            assertTrue(results.get(0).price().compareTo(new BigDecimal("99.90")) == 0);
        }
    }

    @Nested
    @DisplayName("extractConfiguredSpecs")
    class ExtractConfiguredSpecs {

        @Test
        @DisplayName("Debe extraer especificaciones configuradas con prefijo spec_")
        void shouldExtractConfiguredSpecs() {
            String json = """
                    [{ "mpn": "MPN001", "weight_kg": "1.5", "color_name": "red" }]
                    """;

            Supplier supplier = createSupplierWithMappings(Map.of(
                "mpn", "mpn",
                "spec_weight", "weight_kg",
                "spec_color", "color_name"
            ));

            List<UniversalMapperService.NormalizedData> results = mapperService.normalizeResponse(json, supplier);

            assertEquals(1, results.size());
            String specs = results.get(0).techSpecs();
            assertTrue(specs.contains("weight"));
            assertTrue(specs.contains("color"));
        }
    }

    @Nested
    @DisplayName("normalizeSpecKey")
    class NormalizeSpecKey {

        @Test
        @DisplayName("Debe normalizar claves con guiones y guiones bajos")
        void shouldNormalizeSpecKeys() {
            String json = """
                    [{ "mpn": "MPN001", "product_weight": "1.5", "product-length": "10" }]
                    """;

            Supplier supplier = createSupplierWithMappings(Map.of(
                "mpn", "mpn"
            ));

            List<UniversalMapperService.NormalizedData> results = mapperService.normalizeResponse(json, supplier);

            assertEquals(1, results.size());
            String specs = results.get(0).techSpecs();
            assertTrue(specs.contains("productWeight") || specs.contains("productLength"));
        }
    }

    @Nested
    @DisplayName("detectRootArray")
    class DetectRootArray {

        @Test
        @DisplayName("Debe detectar array en campo 'items'")
        void shouldDetectItemsArray() {
            String json = """
                    { "items": [{ "mpn": "MPN001", "brand": "BrandX" }] }
                    """;

            Supplier supplier = createSupplierWithMappings(Map.of(
                "mpn", "mpn",
                "brand", "brand"
            ));

            List<UniversalMapperService.NormalizedData> results = mapperService.normalizeResponse(json, supplier);

            assertEquals(1, results.size());
            assertEquals("MPN001", results.get(0).mpn());
        }

        @Test
        @DisplayName("Debe detectar array en campo 'products'")
        void shouldDetectProductsArray() {
            String json = """
                    { "products": [{ "mpn": "MPN001" }] }
                    """;

            Supplier supplier = createSupplierWithMappings(Map.of("mpn", "mpn"));

            List<UniversalMapperService.NormalizedData> results = mapperService.normalizeResponse(json, supplier);

            assertEquals(1, results.size());
        }

        @Test
        @DisplayName("Debe devolver vacío si no hay array detectable")
        void shouldReturnEmptyWhenNoArrayDetected() {
            String json = """
                    { "single": { "mpn": "MPN001" } }
                    """;

            Supplier supplier = createSupplierWithMappings(Map.of("mpn", "mpn"));

            List<UniversalMapperService.NormalizedData> results = mapperService.normalizeResponse(json, supplier);

            assertTrue(results.isEmpty());
        }
    }
}


