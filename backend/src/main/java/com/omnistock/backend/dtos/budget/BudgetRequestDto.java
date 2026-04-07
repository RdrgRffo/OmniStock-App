package com.omnistock.backend.dtos.budget;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * DTO de entrada para crear/simular un presupuesto.
 * Contiene metadatos del presupuesto y la lista de líneas (productos + proveedores).
 */
public class BudgetRequestDto {

    @NotBlank(message = "El nombre del presupuesto es obligatorio")
    @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    private String budgetName;

    @Size(max = 500, message = "Las notas no pueden exceder 500 caracteres")
    private String notes;

    @NotEmpty(message = "Debe incluir al menos un producto en el presupuesto")
    @Valid
    private List<BudgetLineDto> lines;

    public BudgetRequestDto() {}

    public BudgetRequestDto(String budgetName, String notes, List<BudgetLineDto> lines) {
        this.budgetName = budgetName;
        this.notes = notes;
        this.lines = lines;
    }

    public @NotBlank String getBudgetName() { return budgetName; }
    public void setBudgetName(@NotBlank String budgetName) { this.budgetName = budgetName; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public @NotEmpty List<BudgetLineDto> getLines() { return lines; }
    public void setLines(@NotEmpty List<BudgetLineDto> lines) { this.lines = lines; }
}
