package com.omnistock.backend.controller;

import com.omnistock.backend.configuration.SecurityConfig;
import com.omnistock.backend.dtos.supplier.SupplierMappingDto;
import com.omnistock.backend.dtos.supplier.SupplierMappingRequestDto;
import com.omnistock.backend.security.JwtService;
import com.omnistock.backend.service.supplier.ProviderMappingService;
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

@WebMvcTest(ProviderMappingController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class ProviderMappingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProviderMappingService mappingService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Nested
    @DisplayName("GET /api/v1/proveedores/{proveedorId}/mappings")
    class GetMappingsForProvider {

        @Test
        @DisplayName("Debe obtener mappings con ADMIN")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldGetMappings() throws Exception {
            SupplierMappingDto dto = new SupplierMappingDto(1, 1, "price", "precio", "DIRECT");
            when(mappingService.getMappingsForProvider(1)).thenReturn(List.of(dto));

            mockMvc.perform(get("/api/v1/proveedores/1/mappings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].internalField").value("price"));
        }

        @Test
        @DisplayName("Debe devolver 403 si no es ADMIN")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldReturn403WhenNotAdmin() throws Exception {
            mockMvc.perform(get("/api/v1/proveedores/1/mappings"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/proveedores/{proveedorId}/mappings")
    class CreateMapping {

        @Test
        @DisplayName("Debe crear mapping con ADMIN")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldCreateMapping() throws Exception {
            SupplierMappingDto dto = new SupplierMappingDto(1, 1, "price", "precio", "DIRECT");
            when(mappingService.createMapping(anyInt(), any(SupplierMappingRequestDto.class))).thenReturn(dto);

            String json = """
                    {
                        "internalField": "price",
                        "externalField": "precio",
                        "transformationType": "DIRECT"
                    }
                    """;

            mockMvc.perform(post("/api/v1/proveedores/1/mappings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.internalField").value("price"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/mappings/{mappingId}")
    class UpdateMapping {

        @Test
        @DisplayName("Debe actualizar mapping con ADMIN")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldUpdateMapping() throws Exception {
            SupplierMappingDto dto = new SupplierMappingDto(1, 1, "stock", "cantidad", "DIRECT");
            when(mappingService.updateMapping(anyInt(), any(SupplierMappingRequestDto.class))).thenReturn(dto);

            String json = """
                    {
                        "internalField": "stock",
                        "externalField": "cantidad",
                        "transformationType": "DIRECT"
                    }
                    """;

            mockMvc.perform(put("/api/v1/mappings/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.internalField").value("stock"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/mappings/{mappingId}")
    class DeleteMapping {

        @Test
        @DisplayName("Debe eliminar mapping con ADMIN")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldDeleteMapping() throws Exception {
            doNothing().when(mappingService).deleteMapping(1);

            mockMvc.perform(delete("/api/v1/mappings/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Mapeo eliminado correctamente."));
        }
    }
}
