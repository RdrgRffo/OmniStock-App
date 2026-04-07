package com.omnistock.backend.dtos.budget;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO que representa una línea individual dentro de la request de presupuesto.
 * Cada línea especifica producto, proveedor y cantidad deseada.
 */
public class BudgetLineDto {

    @NotNull(message = "El ID del producto es obligatorio")
    private Integer productId;

    @NotNull(message = "El ID del proveedor es obligatorio")
    private Integer supplierId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima es 1")
    private Integer quantity;

    private String notes;

    public BudgetLineDto() {}

    public BudgetLineDto(Integer productId, Integer supplierId, Integer quantity, String notes) {
        this.productId = productId;
        this.supplierId = supplierId;
        this.quantity = quantity;
        this.notes = notes;
    }

    public @NotNull Integer getProductId() { return productId; }
    public void setProductId(@NotNull Integer productId) { this.productId = productId; }

    public @NotNull Integer getSupplierId() { return supplierId; }
    public void setSupplierId(@NotNull Integer supplierId) { this.supplierId = supplierId; }

    public @NotNull @Min(1) Integer getQuantity() { return quantity; }
    public void setQuantity(@NotNull @Min(1) Integer quantity) { this.quantity = quantity; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
