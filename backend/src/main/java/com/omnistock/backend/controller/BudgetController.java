package com.omnistock.backend.controller;

import com.omnistock.backend.dtos.budget.BudgetRequestDto;
import com.omnistock.backend.dtos.budget.BudgetResponseDto;
import com.omnistock.backend.service.budget.BudgetService;
import com.omnistock.backend.util.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión completa de presupuestos.
 *
 * Endpoints:
 * - POST   /api/v1/budget/simulate    → Simular y persistir un presupuesto
 * - GET    /api/v1/budget              → Listar presupuestos del usuario
 * - GET    /api/v1/budget/{id}         → Obtener presupuesto por ID
 * - PUT    /api/v1/budget/{id}/status  → Cambiar estado (DRAFT/FINALIZED/EXPORTED)
 * - DELETE /api/v1/budget/{id}         → Eliminar presupuesto
 * - POST   /api/v1/budget/{id}/export  → Notificar exportación a Excel
 */
@RestController
@RequestMapping("/api/v1/budget")
public class BudgetController {

    private static final Logger log = LoggerFactory.getLogger(BudgetController.class);

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    /**
     * Simula un presupuesto con los productos, proveedores y cantidades especificadas.
     * Busca los precios actuales en la BD, calcula el total y PERSISTE el resultado.
     */
    @PostMapping("/simulate")
    public ResponseEntity<ApiResponse<BudgetResponseDto>> simulateBudget(
            @Valid @RequestBody BudgetRequestDto request,
            Authentication auth) {

        String username = auth != null ? auth.getName() : "anonymous";
        log.info("POST /api/v1/budget/simulate por usuario '{}'", username);

        BudgetResponseDto budget = budgetService.simulateBudget(request, username);

        return ResponseEntity.ok(ApiResponse.success(budget, "Presupuesto simulado correctamente"));
    }

    /**
     * Lista todos los presupuestos del usuario autenticado.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BudgetResponseDto>>> getUserBudgets(Authentication auth) {
        String username = auth != null ? auth.getName() : "anonymous";
        log.info("GET /api/v1/budget por usuario '{}'", username);

        List<BudgetResponseDto> budgets = budgetService.getUserBudgets(username);

        return ResponseEntity.ok(ApiResponse.success(budgets, "Presupuestos obtenidos correctamente"));
    }

    /**
     * Obtiene un presupuesto por su ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetResponseDto>> getBudgetById(@PathVariable Long id) {
        log.info("GET /api/v1/budget/{}", id);

        BudgetResponseDto budget = budgetService.getBudgetById(id);

        return ResponseEntity.ok(ApiResponse.success(budget, "Presupuesto obtenido correctamente"));
    }

    /**
     * Cambia el estado de un presupuesto.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<BudgetResponseDto>> updateBudgetStatus(
            @PathVariable Long id,
            @RequestParam @NotBlank String status) {

        log.info("PUT /api/v1/budget/{}/status -> {}", id, status);

        BudgetResponseDto budget = budgetService.updateBudgetStatus(id, status.toUpperCase());

        return ResponseEntity.ok(ApiResponse.success(budget, "Estado del presupuesto actualizado correctamente"));
    }

    /**
     * Elimina un presupuesto.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(@PathVariable Long id) {
        log.info("DELETE /api/v1/budget/{}", id);

        budgetService.deleteBudget(id);

        return ResponseEntity.ok(ApiResponse.success(null, "Presupuesto eliminado correctamente"));
    }

    /**
     * Notifica que un presupuesto ha sido exportado a Excel.
     */
    @PostMapping("/{id}/export")
    public ResponseEntity<ApiResponse<Void>> notifyExport(@PathVariable Long id, Authentication auth) {
        String username = auth != null ? auth.getName() : "anonymous";
        log.info("POST /api/v1/budget/{}/export por usuario '{}'", id, username);

        budgetService.notifyBudgetExported(id, username);

        return ResponseEntity.ok(ApiResponse.success(null, "Exportación notificada correctamente"));
    }
}
