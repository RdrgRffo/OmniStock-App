package com.omnistock.backend.controller;

import com.omnistock.backend.configuration.SecurityConfig;
import com.omnistock.backend.dtos.dashboard.DashboardSummaryDto;
import com.omnistock.backend.dtos.dashboard.SupplierHealthDto;
import com.omnistock.backend.security.JwtService;
import com.omnistock.backend.service.dashboard.DashboardService;
import com.omnistock.backend.service.dashboard.ExportService;
import com.omnistock.backend.service.dashboard.SupplierHealthService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private ExportService exportService;

    @MockBean
    private SupplierHealthService supplierHealthService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Nested
    @DisplayName("GET /api/v1/dashboard/summary")
    class GetSummary {

        @Test
        @DisplayName("Debe obtener resumen del dashboard con CLIENTE autenticado")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldGetSummary() throws Exception {
            DashboardSummaryDto summary = new DashboardSummaryDto(
                    100L, 5L, 80L, 10L, 10L, 150.0,
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
            );
            when(dashboardService.getDashboardSummary()).thenReturn(summary);

            mockMvc.perform(get("/api/v1/dashboard/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalProducts").value(100))
                    .andExpect(jsonPath("$.data.totalSuppliers").value(5));
        }

        @Test
        @DisplayName("Debe devolver 401 sin autenticación")
        void shouldReturn401WithoutAuth() throws Exception {
            mockMvc.perform(get("/api/v1/dashboard/summary"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/dashboard/supplier-health")
    class GetSupplierHealth {

        @Test
        @DisplayName("Debe obtener supplier health KPIs con ADMIN")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldGetSupplierHealth() throws Exception {
            SupplierHealthDto dto = new SupplierHealthDto(1, "Proveedor A", 95.0, 98.5, 150.0, 1200L, 2.0, 5.0, "A");
            when(supplierHealthService.getAllSupplierHealth()).thenReturn(List.of(dto));

            mockMvc.perform(get("/api/v1/dashboard/supplier-health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].supplierName").value("Proveedor A"))
                    .andExpect(jsonPath("$.data[0].slaScore").value(95.0));
        }

        @Test
        @DisplayName("Debe devolver 403 si no es ADMIN")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldReturn403WhenNotAdmin() throws Exception {
            mockMvc.perform(get("/api/v1/dashboard/supplier-health"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/dashboard/export/csv")
    class ExportCsv {

        @Test
        @DisplayName("Debe exportar dashboard XLSX con CLIENTE autenticado")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldExportCsv() throws Exception {
            DashboardSummaryDto summary = new DashboardSummaryDto(
                    100L, 5L, 80L, 10L, 10L, 150.0,
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
            );
            when(dashboardService.getDashboardSummary()).thenReturn(summary);
            when(exportService.generateDashboardZip(summary)).thenReturn(new byte[]{0x50, 0x4B, 0x03, 0x04});

            mockMvc.perform(get("/api/v1/dashboard/export/csv"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"dashboard_data.xlsx\""));
        }
    }
}
