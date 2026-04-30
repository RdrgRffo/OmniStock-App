package com.omnistock.backend.controller;

import com.omnistock.backend.configuration.SecurityConfig;
import com.omnistock.backend.dtos.analytics.*;
import com.omnistock.backend.security.JwtService;
import com.omnistock.backend.service.analytics.SupplierAnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyticsController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SupplierAnalyticsService analyticsService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Nested
    @DisplayName("GET /api/v1/analytics/price-variation")
    class GetPriceVariation {

        @Test
        @DisplayName("Debe obtener variación de precios")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldGetPriceVariation() throws Exception {
            when(analyticsService.getPriceIndexVariation(6, 50.0)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/analytics/price-variation"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Debe devolver 401 sin autenticación")
        void shouldReturn401WithoutAuth() throws Exception {
            mockMvc.perform(get("/api/v1/analytics/price-variation"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/analytics/stockout-rates")
    class GetStockoutRates {

        @Test
        @DisplayName("Debe obtener tasas de ruptura de stock")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldGetStockoutRates() throws Exception {
            when(analyticsService.getStockoutRates()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/analytics/stockout-rates"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/analytics/price-dispersion")
    class GetPriceDispersion {

        @Test
        @DisplayName("Debe obtener dispersión de precios")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldGetPriceDispersion() throws Exception {
            when(analyticsService.getPriceDispersion(100)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/analytics/price-dispersion"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/analytics/moq")
    class GetMoqDistribution {

        @Test
        @DisplayName("Debe obtener distribución MOQ")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldGetMoqDistribution() throws Exception {
            when(analyticsService.getMoqDistribution()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/analytics/moq"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/analytics/condition-mix")
    class GetConditionMix {

        @Test
        @DisplayName("Debe obtener mezcla de condiciones")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldGetConditionMix() throws Exception {
            when(analyticsService.getConditionMix()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/analytics/condition-mix"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/analytics/cost-coverage")
    class GetCostCoverage {

        @Test
        @DisplayName("Debe obtener cobertura de costos")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldGetCostCoverage() throws Exception {
            when(analyticsService.getCostCoverage(200)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/analytics/cost-coverage"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/analytics/stock-volatility")
    class GetStockVolatility {

        @Test
        @DisplayName("Debe obtener volatilidad de stock")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldGetStockVolatility() throws Exception {
            when(analyticsService.getStockVolatility(30, 100)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/analytics/stock-volatility"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/analytics/catalog-growth")
    class GetCatalogGrowth {

        @Test
        @DisplayName("Debe obtener crecimiento del catálogo")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldGetCatalogGrowth() throws Exception {
            when(analyticsService.getCatalogGrowth(12)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/analytics/catalog-growth"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/analytics/price-stability")
    class GetPriceStability {

        @Test
        @DisplayName("Debe obtener estabilidad de precios")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldGetPriceStability() throws Exception {
            when(analyticsService.getPriceStability(90, 100)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/analytics/price-stability"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
