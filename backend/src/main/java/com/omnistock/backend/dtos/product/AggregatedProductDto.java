package com.omnistock.backend.dtos.product;

import com.omnistock.backend.dtos.DisponibilidadStatus;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO del listado consolidado de productos: un producto maestro con precio mínimo entre ofertas,
 * disponibilidad agregada y número de proveedores. Se usa en GET /api/v1/productos y en la búsqueda.
 */
public record AggregatedProductDto(
    Integer id,
    String mpn,
    String model,
    String brand,
    String category,
    BigDecimal minCostPrice,
    BigDecimal minRetailPrice,
    DisponibilidadStatus availability,
    int supplierCount,
    Map<String, String> specifications
) {}
