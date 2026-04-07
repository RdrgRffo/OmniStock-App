package com.omnistock.backend.dtos.budget;

import java.math.BigDecimal;

/**
 * DTO que representa una línea individual dentro de un presupuesto simulado.
 * Cada línea vincula un producto con un proveedor, cantidad y precios.
 */
public class BudgetItemDto {

    private Integer productId;
    private String mpn;
    private String productName;
    private String brand;
    private String model;
    private Integer supplierId;
    private String supplierName;
    private Integer quantity;
    private Integer stockAvailable;
    private BigDecimal unitPrice;       // Precio Partner (coste)
    private BigDecimal retailPrice;     // PVP recomendado
    private String productUrl;
    private String notes;

    public BudgetItemDto() {}

    public BudgetItemDto(Integer productId, String mpn, String productName, String brand, String model,
                         Integer supplierId, String supplierName, Integer quantity, Integer stockAvailable,
                         BigDecimal unitPrice, BigDecimal retailPrice, String productUrl, String notes) {
        this.productId = productId;
        this.mpn = mpn;
        this.productName = productName;
        this.brand = brand;
        this.model = model;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.quantity = quantity;
        this.stockAvailable = stockAvailable;
        this.unitPrice = unitPrice;
        this.retailPrice = retailPrice;
        this.productUrl = productUrl;
        this.notes = notes;
    }

    // --- Getters y Setters ---

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public String getMpn() { return mpn; }
    public void setMpn(String mpn) { this.mpn = mpn; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Integer getSupplierId() { return supplierId; }
    public void setSupplierId(Integer supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getStockAvailable() { return stockAvailable; }
    public void setStockAvailable(Integer stockAvailable) { this.stockAvailable = stockAvailable; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getRetailPrice() { return retailPrice; }
    public void setRetailPrice(BigDecimal retailPrice) { this.retailPrice = retailPrice; }

    public String getProductUrl() { return productUrl; }
    public void setProductUrl(String productUrl) { this.productUrl = productUrl; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
