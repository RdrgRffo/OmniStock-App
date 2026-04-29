package com.omnistock.backend.controller;

import com.omnistock.backend.configuration.SecurityConfig;
import com.omnistock.backend.dtos.supplier.SupplierDashboardDto;
import com.omnistock.backend.dtos.supplier.SupplierRequestDto;
import com.omnistock.backend.dtos.supplier.SupplierResponseDto;
import com.omnistock.backend.dtos.supplier.SupplierSimpleDto;
import com.omnistock.backend.security.JwtService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SupplierController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SupplierService supplierService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Nested
    @DisplayName("GET /api/v1/proveedores/list")
    class GetAllSuppliersSimple {

        @Test
        @DisplayName("Debe listar proveedores simples con ADMIN")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldListSimpleWithAdmin() throws Exception {
            when(supplierService.getAllSuppliersSimple()).thenReturn(List.of(new SupplierSimpleDto(1, "Proveedor A")));

            mockMvc.perform(get("/api/v1/proveedores/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].name").value("Proveedor A"));
        }

        @Test
        @DisplayName("Debe listar proveedores simples con CLIENTE")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldListSimpleWithCliente() throws Exception {
            when(supplierService.getAllSuppliersSimple()).thenReturn(List.of(new SupplierSimpleDto(1, "Proveedor A")));

            mockMvc.perform(get("/api/v1/proveedores/list"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Debe devolver 401 sin autenticación")
        void shouldReturn401WithoutAuth() throws Exception {
            mockMvc.perform(get("/api/v1/proveedores/list"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/proveedores/{id}/dashboard")
    class GetSupplierDashboard {

        @Test
        @DisplayName("Debe obtener dashboard del proveedor con ADMIN")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldGetDashboardWithAdmin() throws Exception {
            SupplierDashboardDto dto = new SupplierDashboardDto(1, "Proveedor A", 100L, 80L, 20L, 0.5);
            when(supplierService.getDashboardData(1)).thenReturn(dto);

            mockMvc.perform(get("/api/v1/proveedores/1/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalProducts").value(100));
        }

        @Test
        @DisplayName("Debe obtener dashboard del proveedor con CLIENTE")
        @WithMockUser(username = "cliente", roles = {"CLIENTE"})
        void shouldGetDashboardWithCliente() throws Exception {
            SupplierDashboardDto dto = new SupplierDashboardDto(1, "Proveedor A", 50L, 40L, 10L, 0.2);
            when(supplierService.getDashboardData(1)).thenReturn(dto);

            mockMvc.perform(get("/api/v1/proveedores/1/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalProducts").value(50));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/proveedores")
    class GetAllSuppliers {

        @Test
        @DisplayName("Debe listar todos los proveedores con ADMIN")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldListAllWithAdmin() throws Exception {
            SupplierResponseDto dto = new SupplierResponseDto(1, "Proveedor A", "https://api.example.com",
                    "Contacto", "email@test.com", "9-18", "123456789", "https://example.com",
                    "ES", "EUR", true, true, "/catalog", "/detail/{sku}", "/search?q={query}");
            when(supplierService.getAllSuppliers()).thenReturn(List.of(dto));

            mockMvc.perform(get("/api/v1/proveedores"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].name").value("Proveedor A"));
        }

        @Test
        @DisplayName("Debe devolver 403 si no es ADMIN")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldReturn403WhenNotAdmin() throws Exception {
            mockMvc.perform(get("/api/v1/proveedores"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/proveedores/{id}")
    class GetSupplierById {

        @Test
        @DisplayName("Debe obtener proveedor por ID con ADMIN")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldGetById() throws Exception {
            SupplierResponseDto dto = new SupplierResponseDto(1, "Proveedor A", null, null, null, null, null, null, null, null, true, true, null, null, null);
            when(supplierService.getSupplierById(1)).thenReturn(dto);

            mockMvc.perform(get("/api/v1/proveedores/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Proveedor A"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/proveedores")
    class CreateSupplier {

        @Test
        @DisplayName("Debe crear proveedor con ADMIN")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldCreateSupplier() throws Exception {
            SupplierResponseDto dto = new SupplierResponseDto(1, "Nuevo Proveedor", null, null, null, null, null, null, null, null, true, true, null, null, null);
            when(supplierService.createSupplier(any(SupplierRequestDto.class))).thenReturn(dto);

            String json = """
                    {
                        "name": "Nuevo Proveedor",
                        "baseUrlApi": "https://api.example.com",
                        "active": true,
                        "supportsBulkSync": true
                    }
                    """;

            mockMvc.perform(post("/api/v1/proveedores")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Nuevo Proveedor"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/proveedores/{id}")
    class UpdateSupplier {

        @Test
        @DisplayName("Debe actualizar proveedor con ADMIN")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldUpdateSupplier() throws Exception {
            SupplierResponseDto dto = new SupplierResponseDto(1, "Actualizado", null, null, null, null, null, null, null, null, true, true, null, null, null);
            when(supplierService.updateSupplier(anyInt(), any(SupplierRequestDto.class))).thenReturn(dto);

            String json = """
                    {
                        "name": "Actualizado",
                        "baseUrlApi": "https://api.example.com",
                        "active": true,
                        "supportsBulkSync": true
                    }
                    """;

            mockMvc.perform(put("/api/v1/proveedores/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Actualizado"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/proveedores/{id}")
    class DeleteSupplier {

        @Test
        @DisplayName("Debe eliminar proveedor con ADMIN")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldDeleteSupplier() throws Exception {
            doNothing().when(supplierService).deleteSupplier(1);

            mockMvc.perform(delete("/api/v1/proveedores/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Proveedor eliminado correctamente"));
        }
    }
}
