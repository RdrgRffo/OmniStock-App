package com.omnistock.backend.dtos.budget;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta que representa un presupuesto simulado completo.
 * Incluye metadatos, líneas de producto y total calculado.
 */
public class BudgetResponseDto {

    private Long id;
    private String budgetNumber;       // Formato: PC-YYYYMMDD-NNN
    private String budgetName;
    private String notes;
    private String status;             // DRAFT, FINALIZED, EXPORTED
    private String createdBy;          // username que creó el presupuesto
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal totalAmount;    // Suma de (unitPrice * quantity) de todas las líneas
    private List<BudgetItemDto> items;

    public BudgetResponseDto() {}

    public BudgetResponseDto(Long id, String budgetNumber, String budgetName, String notes,
                             String status, String createdBy, LocalDateTime createdAt,
                             LocalDateTime updatedAt, BigDecimal totalAmount, List<BudgetItemDto> items) {
        this.id = id;
        this.budgetNumber = budgetNumber;
        this.budgetName = budgetName;
        this.notes = notes;
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.totalAmount = totalAmount;
        this.items = items;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBudgetNumber() { return budgetNumber; }
    public void setBudgetNumber(String budgetNumber) { this.budgetNumber = budgetNumber; }

    public String getBudgetName() { return budgetName; }
    public void setBudgetName(String budgetName) { this.budgetName = budgetName; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public List<BudgetItemDto> getItems() { return items; }
    public void setItems(List<BudgetItemDto> items) { this.items = items; }
}
