package com.omnistock.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entidad que representa una línea individual dentro de un presupuesto persistido.
 * Cada línea vincula un producto con un proveedor, cantidad y precios en el momento de la simulación.
 */
@Entity
@Table(name = "budget_lines")
public class BudgetLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(name = "mpn", length = 100)
    private String mpn;

    @Column(name = "product_name", nullable = false, length = 300)
    private String productName;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "model", length = 200)
    private String model;

    @Column(name = "supplier_id", nullable = false)
    private Integer supplierId;

    @Column(name = "supplier_name", nullable = false, length = 200)
    private String supplierName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "stock_available")
    private Integer stockAvailable;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "retail_price", precision = 10, scale = 2)
    private BigDecimal retailPrice;

    @Column(name = "product_url", length = 500)
    private String productUrl;

    @Column(name = "notes", length = 500)
    private String notes;

    public BudgetLine() {}

    public BudgetLine(Integer productId, String mpn, String productName, String brand, String model,
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Budget getBudget() { return budget; }
    public void setBudget(Budget budget) { this.budget = budget; }

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
