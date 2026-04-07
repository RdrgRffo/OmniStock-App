package com.omnistock.backend.dtos.analytics;

import java.math.BigDecimal;

/**
 * DTO que representa una oportunidad de trading: producto con margen positivo
 * entre el mejor precio de coste y el PVP recomendado mínimo.
 * <p>
 * Se usa en GET /api/v1/analytics/trading-opportunities para alimentar
 * el componente TradingView del dashboard de Analytics.
 *
 * @param id            identificador del producto maestro
 * @param mpn           MPN del producto
 * @param model         modelo del producto
 * @param brand         marca del producto
 * @param category      categoría del producto
 * @param minCostPrice  mejor precio de coste entre todos los proveedores
 * @param minRetailPrice PVP recomendado mínimo entre todos los proveedores
 * @param gap           brecha nominal = minRetailPrice - minCostPrice
 * @param marginPct     margen porcentual = (gap / minCostPrice) * 100
 */
public record TradingOpportunityDto(
    Integer id,
    String mpn,
    String model,
    String brand,
    String category,
    BigDecimal minCostPrice,
    BigDecimal minRetailPrice,
    BigDecimal gap,
    BigDecimal marginPct
) {}
