package com.omnistock.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnistock.backend.configuration.SecurityConfig;
import com.omnistock.backend.dtos.budget.BudgetLineDto;
import com.omnistock.backend.dtos.budget.BudgetRequestDto;
import com.omnistock.backend.dtos.budget.BudgetResponseDto;
import com.omnistock.backend.security.JwtService;
import com.omnistock.backend.service.budget.BudgetService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BudgetController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BudgetService budgetService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    private BudgetResponseDto createSampleResponse() {
        BudgetResponseDto response = new BudgetResponseDto();
        response.setId(1L);
        response.setBudgetNumber("PRES-20260520-001");
        response.setBudgetName("Presupuesto Test");
        response.setNotes("Notas de prueba");
        response.setStatus("DRAFT");
        response.setCreatedBy("testuser");
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());
        response.setTotalAmount(new BigDecimal("750.00"));
        response.setItems(List.of());
        return response;
    }

    @Nested
    @DisplayName("POST /api/v1/budget/simulate")
    class SimulateBudget {

        @Test
        @DisplayName("Debe simular presupuesto con CLIENTE autenticado")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldSimulateBudgetWithAuthenticatedUser() throws Exception {
            // Arrange
            BudgetLineDto line = new BudgetLineDto(1, 1, 5, null);
            BudgetRequestDto request = new BudgetRequestDto("Presupuesto Test", "Notas", List.of(line));
            BudgetResponseDto mockResponse = createSampleResponse();

            when(budgetService.simulateBudget(any(BudgetRequestDto.class), anyString()))
                    .thenReturn(mockResponse);

            // Act & Assert
            mockMvc.perform(post("/api/v1/budget/simulate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Presupuesto simulado correctamente"))
                    .andExpect(jsonPath("$.data.budgetNumber").value("PRES-20260520-001"))
                    .andExpect(jsonPath("$.data.budgetName").value("Presupuesto Test"))
                    .andExpect(jsonPath("$.data.status").value("DRAFT"))
                    .andExpect(jsonPath("$.data.totalAmount").value(750.00));
        }

        @Test
        @DisplayName("Debe simular presupuesto con CLIENTE ADMIN")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldSimulateBudgetWithAdminUser() throws Exception {
            // Arrange
            BudgetLineDto line = new BudgetLineDto(1, 1, 3, null);
            BudgetRequestDto request = new BudgetRequestDto("Admin Budget", null, List.of(line));
            BudgetResponseDto mockResponse = createSampleResponse();
            mockResponse.setBudgetName("Admin Budget");
            mockResponse.setCreatedBy("admin");

            when(budgetService.simulateBudget(any(BudgetRequestDto.class), anyString()))
                    .thenReturn(mockResponse);

            // Act & Assert
            mockMvc.perform(post("/api/v1/budget/simulate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.budgetName").value("Admin Budget"));
        }

        @Test
        @DisplayName("Debe devolver 401 sin autenticación")
        void shouldReturn401WithoutAuth() throws Exception {
            // Arrange
            BudgetLineDto line = new BudgetLineDto(1, 1, 5, null);
            BudgetRequestDto request = new BudgetRequestDto("Test", null, List.of(line));

            // Act & Assert
            mockMvc.perform(post("/api/v1/budget/simulate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Debe devolver 400 cuando el nombre del presupuesto está vacío")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldReturn400WhenBudgetNameIsBlank() throws Exception {
            // Arrange
            BudgetLineDto line = new BudgetLineDto(1, 1, 5, null);
            BudgetRequestDto request = new BudgetRequestDto("", null, List.of(line));

            // Act & Assert
            mockMvc.perform(post("/api/v1/budget/simulate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe devolver 400 cuando la lista de líneas está vacía")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldReturn400WhenLinesAreEmpty() throws Exception {
            // Arrange
            BudgetRequestDto request = new BudgetRequestDto("Test", null, List.of());

            // Act & Assert
            mockMvc.perform(post("/api/v1/budget/simulate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe devolver 400 cuando productId es nulo")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldReturn400WhenProductIdIsNull() throws Exception {
            // Arrange
            String invalidJson = """
                    {
                        "budgetName": "Test",
                        "lines": [
                            {
                                "supplierId": 1,
                                "quantity": 5
                            }
                        ]
                    }
                    """;

            // Act & Assert
            mockMvc.perform(post("/api/v1/budget/simulate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe devolver 400 cuando supplierId es nulo")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldReturn400WhenSupplierIdIsNull() throws Exception {
            // Arrange
            String invalidJson = """
                    {
                        "budgetName": "Test",
                        "lines": [
                            {
                                "productId": 1,
                                "quantity": 5
                            }
                        ]
                    }
                    """;

            // Act & Assert
            mockMvc.perform(post("/api/v1/budget/simulate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe devolver 400 cuando quantity es menor que 1")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldReturn400WhenQuantityIsLessThanOne() throws Exception {
            // Arrange
            String invalidJson = """
                    {
                        "budgetName": "Test",
                        "lines": [
                            {
                                "productId": 1,
                                "supplierId": 1,
                                "quantity": 0
                            }
                        ]
                    }
                    """;

            // Act & Assert
            mockMvc.perform(post("/api/v1/budget/simulate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe devolver 400 cuando el nombre excede 200 caracteres")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldReturn400WhenBudgetNameExceedsMaxLength() throws Exception {
            // Arrange
            String longName = "A".repeat(201);
            BudgetLineDto line = new BudgetLineDto(1, 1, 5, null);
            BudgetRequestDto request = new BudgetRequestDto(longName, null, List.of(line));

            // Act & Assert
            mockMvc.perform(post("/api/v1/budget/simulate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe devolver 400 cuando las notas exceden 500 caracteres")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldReturn400WhenNotesExceedMaxLength() throws Exception {
            // Arrange
            String longNotes = "A".repeat(501);
            BudgetLineDto line = new BudgetLineDto(1, 1, 5, null);
            BudgetRequestDto request = new BudgetRequestDto("Test", longNotes, List.of(line));

            // Act & Assert
            mockMvc.perform(post("/api/v1/budget/simulate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/budget")
    class GetUserBudgets {

        @Test
        @DisplayName("Debe listar presupuestos del usuario autenticado")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldListUserBudgets() throws Exception {
            // Arrange
            BudgetResponseDto budget = createSampleResponse();
            when(budgetService.getUserBudgets("testuser")).thenReturn(List.of(budget));

            // Act & Assert
            mockMvc.perform(get("/api/v1/budget"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].budgetNumber").value("PRES-20260520-001"))
                    .andExpect(jsonPath("$.data[0].budgetName").value("Presupuesto Test"));
        }

        @Test
        @DisplayName("Debe devolver lista vacía si no hay presupuestos")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldReturnEmptyList() throws Exception {
            when(budgetService.getUserBudgets("testuser")).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/budget"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("Debe devolver 401 sin autenticación")
        void shouldReturn401WithoutAuth() throws Exception {
            mockMvc.perform(get("/api/v1/budget"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/budget/{id}")
    class GetBudgetById {

        @Test
        @DisplayName("Debe obtener presupuesto por ID")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldGetBudgetById() throws Exception {
            // Arrange
            BudgetResponseDto budget = createSampleResponse();
            when(budgetService.getBudgetById(1L)).thenReturn(budget);

            // Act & Assert
            mockMvc.perform(get("/api/v1/budget/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.budgetNumber").value("PRES-20260520-001"));
        }

        @Test
        @DisplayName("Debe devolver 401 sin autenticación")
        void shouldReturn401WithoutAuth() throws Exception {
            mockMvc.perform(get("/api/v1/budget/1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/budget/{id}/status")
    class UpdateBudgetStatus {

        @Test
        @DisplayName("Debe actualizar estado del presupuesto")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldUpdateBudgetStatus() throws Exception {
            // Arrange
            BudgetResponseDto updated = createSampleResponse();
            updated.setStatus("FINALIZED");
            when(budgetService.updateBudgetStatus(1L, "FINALIZED")).thenReturn(updated);

            // Act & Assert
            mockMvc.perform(put("/api/v1/budget/1/status")
                            .param("status", "FINALIZED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("FINALIZED"));
        }

        @Test
        @DisplayName("Debe devolver 400 si status está vacío")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldReturn400WhenStatusIsBlank() throws Exception {
            mockMvc.perform(put("/api/v1/budget/1/status")
                            .param("status", ""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe devolver 401 sin autenticación")
        void shouldReturn401WithoutAuth() throws Exception {
            mockMvc.perform(put("/api/v1/budget/1/status")
                            .param("status", "FINALIZED"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/budget/{id}")
    class DeleteBudget {

        @Test
        @DisplayName("Debe eliminar presupuesto")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldDeleteBudget() throws Exception {
            // Arrange
            doNothing().when(budgetService).deleteBudget(1L);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/budget/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Presupuesto eliminado correctamente"));
        }

        @Test
        @DisplayName("Debe devolver 401 sin autenticación")
        void shouldReturn401WithoutAuth() throws Exception {
            mockMvc.perform(delete("/api/v1/budget/1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/budget/{id}/export")
    class NotifyExport {

        @Test
        @DisplayName("Debe notificar exportación")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldNotifyExport() throws Exception {
            // Arrange
            doNothing().when(budgetService).notifyBudgetExported(1L, "testuser");

            // Act & Assert
            mockMvc.perform(post("/api/v1/budget/1/export"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Exportación notificada correctamente"));
        }

        @Test
        @DisplayName("Debe devolver 401 sin autenticación")
        void shouldReturn401WithoutAuth() throws Exception {
            mockMvc.perform(post("/api/v1/budget/1/export"))
                    .andExpect(status().isForbidden());
        }
    }
}
