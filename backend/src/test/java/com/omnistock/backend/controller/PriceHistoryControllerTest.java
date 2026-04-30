package com.omnistock.backend.controller;

import com.omnistock.backend.configuration.SecurityConfig;
import com.omnistock.backend.dtos.pricing.PriceHistoryRequestDto;
import com.omnistock.backend.dtos.pricing.PriceHistoryResponseDto;
import com.omnistock.backend.dtos.pricing.PriceStatisticsDto;
import com.omnistock.backend.entity.MasterProduct;
import com.omnistock.backend.entity.PriceHistory;
import com.omnistock.backend.entity.Supplier;
import com.omnistock.backend.security.JwtService;
import com.omnistock.backend.service.product.MasterProductService;
import com.omnistock.backend.service.pricing.PriceHistoryService;
import com.omnistock.backend.service.supplier.SupplierService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PriceHistoryController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class PriceHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PriceHistoryService priceHistoryService;

    @MockBean
    private MasterProductService masterProductService;

    @MockBean
    private SupplierService supplierService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    private MasterProduct createProduct() {
        MasterProduct p = new MasterProduct();
        p.setId(1);
        p.setModel("ModelX");
        return p;
    }

    private Supplier createSupplier() {
        Supplier s = new Supplier();
        s.setId(1);
        s.setName("Proveedor A");
        return s;
    }

    private PriceHistory createPriceHistory() {
        PriceHistory h = new PriceHistory();
        h.setHistoryId(1);
        h.setMasterProduct(createProduct());
        h.setSupplier(createSupplier());
        h.setCostPrice(BigDecimal.valueOf(100));
        h.setRegisteredAt(LocalDateTime.now());
        return h;
    }

    @Nested
    @DisplayName("GET /api/v1/historial-precios/producto/{productoId}/proveedor/{proveedorId}")
    class GetHistoryByProductAndSupplier {

        @Test
        @DisplayName("Debe obtener historial de precios")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldGetHistory() throws Exception {
            MasterProduct product = createProduct();
            Supplier supplier = createSupplier();
            when(masterProductService.getById(1)).thenReturn(product);
            when(supplierService.getById(1)).thenReturn(supplier);
            when(priceHistoryService.getHistoryForProduct(product, supplier)).thenReturn(List.of(createPriceHistory()));

            mockMvc.perform(get("/api/v1/historial-precios/producto/1/proveedor/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].costPrice").value(100));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/historial-precios/producto/{productoId}")
    class GetHistoryByProduct {

        @Test
        @DisplayName("Debe obtener historial de precios de un producto")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldGetHistoryByProduct() throws Exception {
            MasterProduct product = createProduct();
            when(masterProductService.getById(1)).thenReturn(product);
            when(priceHistoryService.getHistoryForProduct(product, null)).thenReturn(List.of(createPriceHistory()));

            mockMvc.perform(get("/api/v1/historial-precios/producto/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/historial-precios/producto/{productoId}/proveedor/{proveedorId}/analytics")
    class GetAnalytics {

        @Test
        @DisplayName("Debe obtener análisis de precios")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldGetAnalytics() throws Exception {
            MasterProduct product = createProduct();
            Supplier supplier = createSupplier();
            when(masterProductService.getById(1)).thenReturn(product);
            when(supplierService.getById(1)).thenReturn(supplier);
            when(priceHistoryService.getHistoryForProduct(product, supplier)).thenReturn(List.of(createPriceHistory()));
            when(priceHistoryService.computeStatistics(anyList(), eq(product), eq(supplier)))
                    .thenReturn(new PriceStatisticsDto(
                            BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100),
                            BigDecimal.ZERO, BigDecimal.valueOf(100), BigDecimal.valueOf(100),
                            LocalDateTime.now(), LocalDateTime.now().minusDays(30),
                            "ProductX", "SupplierA", 1));

            mockMvc.perform(get("/api/v1/historial-precios/producto/1/proveedor/1/analytics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.averagePrice").value(100));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/historial-precios/producto/{productoId}/comparativa-proveedores")
    class GetSupplierComparison {

        @Test
        @DisplayName("Debe obtener comparativa entre proveedores")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldGetComparison() throws Exception {
            MasterProduct product = createProduct();
            Supplier supplier = createSupplier();
            when(masterProductService.getById(1)).thenReturn(product);
            when(priceHistoryService.getHistoryForProduct(product, null)).thenReturn(List.of(createPriceHistory()));
            when(priceHistoryService.computeStatistics(anyList(), eq(product), any()))
                    .thenReturn(new PriceStatisticsDto(
                            BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.valueOf(100),
                            BigDecimal.ZERO, BigDecimal.valueOf(100), BigDecimal.valueOf(100),
                            LocalDateTime.now(), LocalDateTime.now().minusDays(30),
                            "ProductX", "Proveedor A", 1));

            mockMvc.perform(get("/api/v1/historial-precios/producto/1/comparativa-proveedores"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/historial-precios/producto/{productoId}/proveedor/{proveedorId}/por-mes")
    class GetHistoryByMonth {

        @Test
        @DisplayName("Debe obtener historial agrupado por mes")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldGetHistoryByMonth() throws Exception {
            MasterProduct product = createProduct();
            Supplier supplier = createSupplier();
            when(masterProductService.getById(1)).thenReturn(product);
            when(supplierService.getById(1)).thenReturn(supplier);
            when(priceHistoryService.getHistoryForProduct(product, supplier)).thenReturn(List.of(createPriceHistory()));

            mockMvc.perform(get("/api/v1/historial-precios/producto/1/proveedor/1/por-mes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/historial-precios/registrar")
    class RegisterPriceChange {

        @Test
        @DisplayName("Debe registrar cambio de precio")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldRegisterPriceChange() throws Exception {
            MasterProduct product = createProduct();
            Supplier supplier = createSupplier();
            when(masterProductService.getById(1)).thenReturn(product);
            when(supplierService.getById(1)).thenReturn(supplier);
            when(priceHistoryService.getHistoryForProduct(product, supplier)).thenReturn(List.of(createPriceHistory()));

            String json = """
                    {
                        "masterProductId": 1,
                        "supplierId": 1,
                        "costPrice": 100.00
                    }
                    """;

            mockMvc.perform(post("/api/v1/historial-precios/registrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/historial-precios/producto/{productoId}/proveedor/{proveedorId}/ultimo-precio")
    class GetLastPrice {

        @Test
        @DisplayName("Debe obtener último precio registrado")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldGetLastPrice() throws Exception {
            MasterProduct product = createProduct();
            Supplier supplier = createSupplier();
            when(masterProductService.getById(1)).thenReturn(product);
            when(supplierService.getById(1)).thenReturn(supplier);
            when(priceHistoryService.getHistoryForProduct(product, supplier)).thenReturn(List.of(createPriceHistory()));

            mockMvc.perform(get("/api/v1/historial-precios/producto/1/proveedor/1/ultimo-precio"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.costPrice").value(100));
        }

        @Test
        @DisplayName("Debe devolver error si no hay histórico")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldReturnErrorWhenNoHistory() throws Exception {
            MasterProduct product = createProduct();
            Supplier supplier = createSupplier();
            when(masterProductService.getById(1)).thenReturn(product);
            when(supplierService.getById(1)).thenReturn(supplier);
            when(priceHistoryService.getHistoryForProduct(product, supplier)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/historial-precios/producto/1/proveedor/1/ultimo-precio"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/historial-precios/producto/{productoId}/proveedor/{proveedorId}/tendencia")
    class GetTrend {

        @Test
        @DisplayName("Debe obtener tendencia de precios")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldGetTrend() throws Exception {
            MasterProduct product = createProduct();
            Supplier supplier = createSupplier();
            when(masterProductService.getById(1)).thenReturn(product);
            when(supplierService.getById(1)).thenReturn(supplier);
            when(priceHistoryService.getHistoryForProduct(product, supplier)).thenReturn(List.of(createPriceHistory()));

            mockMvc.perform(get("/api/v1/historial-precios/producto/1/proveedor/1/tendencia")
                            .param("dias", "30"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.tendencia").exists());
        }
    }
}
