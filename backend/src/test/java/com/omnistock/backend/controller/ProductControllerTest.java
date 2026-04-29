package com.omnistock.backend.controller;

import com.omnistock.backend.configuration.SecurityConfig;
import com.omnistock.backend.dtos.DisponibilidadStatus;
import com.omnistock.backend.dtos.product.AggregatedProductDto;
import com.omnistock.backend.dtos.product.ProductDetailDto;
import com.omnistock.backend.dtos.pricing.PriceHistoryItemDto;
import com.omnistock.backend.security.JwtService;
import com.omnistock.backend.service.product.IStockService;
import com.omnistock.backend.service.pricing.PriceHistoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IStockService stockService;

    @MockBean
    private PriceHistoryService priceHistoryService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    private AggregatedProductDto createSampleProduct() {
        return new AggregatedProductDto(
                1, "MPN001", "ModelX", "BrandX", "CategoryX",
                BigDecimal.valueOf(100), BigDecimal.valueOf(150),
                DisponibilidadStatus.DISPONIBLE, 5, Map.of("weight", "1.5kg")
        );
    }

    @Nested
    @DisplayName("GET /api/v1/productos")
    class GetAllProducts {

        @Test
        @DisplayName("Debe listar productos paginados con CLIENTE autenticado")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldListProducts() throws Exception {
            Page<AggregatedProductDto> page = new PageImpl<>(List.of(createSampleProduct()));
            when(stockService.searchProducts(any(), any(PageRequest.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/productos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].model").value("ModelX"))
                    .andExpect(jsonPath("$.data.content[0].brand").value("BrandX"));
        }

        @Test
        @DisplayName("Debe devolver 401 sin autenticación")
        void shouldReturn401WithoutAuth() throws Exception {
            mockMvc.perform(get("/api/v1/productos"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/productos/search")
    class SearchProducts {

        @Test
        @DisplayName("Debe buscar productos con filtros")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldSearchProducts() throws Exception {
            Page<AggregatedProductDto> page = new PageImpl<>(List.of(createSampleProduct()));
            when(stockService.searchProducts(any(), any(PageRequest.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/productos/search")
                            .param("query", "ModelX")
                            .param("categoria", "BrandX"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].model").value("ModelX"));
        }

        @Test
        @DisplayName("Debe devolver 401 sin autenticación")
        void shouldReturn401WithoutAuth() throws Exception {
            mockMvc.perform(get("/api/v1/productos/search"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/productos/{id}")
    class GetProductDetails {

        @Test
        @DisplayName("Debe obtener detalle de producto existente")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldGetProductDetails() throws Exception {
            ProductDetailDto detail = new ProductDetailDto(1, "MPN001", "ModelX", "BrandX", "CategoryX",
                    Map.of("weight", "1.5kg"), List.of());
            when(stockService.findProductDetailsById(1)).thenReturn(Optional.of(detail));

            mockMvc.perform(get("/api/v1/productos/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.model").value("ModelX"));
        }

        @Test
        @DisplayName("Debe devolver 404 si el producto no existe")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldReturn404WhenNotFound() throws Exception {
            when(stockService.findProductDetailsById(999)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/productos/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/productos/{id}/historial/{proveedorId}")
    class GetHistorialPrecios {

        @Test
        @DisplayName("Debe obtener historial de precios")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldGetPriceHistory() throws Exception {
            PriceHistoryItemDto item = new PriceHistoryItemDto(BigDecimal.valueOf(100), LocalDateTime.now(), "UP");
            when(priceHistoryService.getHistoryDto(1, 1)).thenReturn(List.of(item));

            mockMvc.perform(get("/api/v1/productos/1/historial/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].price").value(100));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/productos/sync-all")
    class SyncAllProducts {

        @Test
        @DisplayName("Debe iniciar sincronización masiva")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldSyncAllProducts() throws Exception {
            mockMvc.perform(post("/api/v1/productos/sync-all"))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Sincronización masiva iniciada en segundo plano"));
        }

        @Test
        @DisplayName("Debe devolver 403 si no es ADMIN")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldReturn403WhenNotAdmin() throws Exception {
            mockMvc.perform(post("/api/v1/productos/sync-all"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/productos/sync-status")
    class GetSyncStatus {

        @Test
        @DisplayName("Debe obtener estado de sincronización")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldGetSyncStatus() throws Exception {
            when(stockService.getSyncStatus()).thenReturn(com.omnistock.backend.dtos.SyncStatus.IDLE);

            mockMvc.perform(get("/api/v1/productos/sync-status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("IDLE"));
        }
    }
}
