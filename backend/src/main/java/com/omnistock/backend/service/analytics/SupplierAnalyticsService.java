package com.omnistock.backend.service.analytics;

import com.omnistock.backend.dtos.analytics.*;
import com.omnistock.backend.repository.*;
import com.omnistock.backend.repository.MasterProductRepository.CatalogGrowthProjection;
import com.omnistock.backend.repository.PriceHistoryRepository.PriceIndexVariationProjection;
import com.omnistock.backend.repository.PriceHistoryRepository.PriceStabilityProjection;
import com.omnistock.backend.repository.ProductSupplierRepository.*;
import com.omnistock.backend.repository.ProductSupplierRepository.TradingOpportunityProjection;
import com.omnistock.backend.repository.StockHistoryRepository.StockVolatilityProjection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de analytics de Supply Chain.
 *
 * Implementa los tres KPIs avanzados definidos en el contexto de negocio:
 *   1. Price Index Variation — variacion mensual de costes por proveedor (LAG).
 *   2. Stockout Rate         — tasa de ruptura de stock por proveedor.
 *   3. ML Dataset            — vista desnormalizada para modelos de prediccion.
 *
 * Regla de normalizacion de monedas:
 *   Antes de agregar KPIs de precios, la query de PriceIndexVariation aplica
 *   la tabla currency_rates para convertir a EUR. Si no existe tasa registrada
 *   para un par de monedas, se asume factor 1.0 (misma moneda).
 *
 * Regla de deduplicacion:
 *   Los KPIs operan sobre MasterProduct.mpn como SKU canonico. Un mismo MPN
 *   de diferentes proveedores se trata como una sola entidad en el inventario global.
 *
 * Nota sobre proyecciones JPA:
 *   Todos los metodos usan interface-based projections de Spring Data JPA en lugar
 *   de Object[] nativos, eliminando la necesidad de conversion manual de tipos.
 *   Las queries nativas originales se mantienen como fallback (metodos sin "Projected").
 */
@Service
public class SupplierAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(SupplierAnalyticsService.class);

    private static final int DEFAULT_MONTHS        = 6;
    private static final double DEFAULT_OUTLIER_THRESHOLD = 50.0;
    private static final int DEFAULT_ML_MAX_ROWS   = 1000;

    private final PriceHistoryRepository    priceHistoryRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final MasterProductRepository   masterProductRepository;
    private final StockHistoryRepository    stockHistoryRepository;

    public SupplierAnalyticsService(
            PriceHistoryRepository priceHistoryRepository,
            ProductSupplierRepository productSupplierRepository,
            MasterProductRepository masterProductRepository,
            StockHistoryRepository stockHistoryRepository) {
        this.priceHistoryRepository    = priceHistoryRepository;
        this.productSupplierRepository = productSupplierRepository;
        this.masterProductRepository   = masterProductRepository;
        this.stockHistoryRepository    = stockHistoryRepository;
    }

    // =========================================================================
    // KPI A — Price Index Variation
    // =========================================================================

    /**
     * Calcula la variacion mensual del indice de precios por proveedor y SKU.
     *
     * Usa window function LAG() sobre historial_precios para comparar el precio
     * promedio mensual con el del mes anterior. Los registros cuya variacion
     * supera el umbral de outlier se marcan como isOutlier=true pero se incluyen
     * en la respuesta para que el frontend pueda filtrarlos visualmente.
     *
     * @param months           ventana de analisis en meses (default 6)
     * @param outlierThreshold umbral de variacion absoluta para clasificar outliers (default 50.0%)
     * @return lista de variaciones mensuales ordenada por proveedor, producto y mes
     */
    @Transactional(readOnly = true)
    public List<PriceIndexVariationDto> getPriceIndexVariation(int months, double outlierThreshold) {
        log.info("Calculando Price Index Variation: {} meses, umbral outlier {}%", months, outlierThreshold);

        List<PriceIndexVariationProjection> rows;
        try {
            rows = priceHistoryRepository.findPriceIndexVariationProjected(months, outlierThreshold);
        } catch (DataAccessException ex) {
            // Fallback defensivo para entornos con tabla currency_rates dañada o ausente.
            log.warn("No se pudo aplicar normalizacion por currency_rates; usando fallback sin conversion de divisa. Causa: {}",
                ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage());
            rows = priceHistoryRepository.findPriceIndexVariationWithoutCurrencyRatesProjected(months, outlierThreshold);
        }

        List<PriceIndexVariationDto> result = new ArrayList<>(rows.size());

        for (PriceIndexVariationProjection row : rows) {
            BigDecimal variationPct = row.getVariationPct();
            boolean isOutlier = variationPct != null
                && variationPct.abs().doubleValue() > outlierThreshold;

            result.add(new PriceIndexVariationDto(
                row.getSupplierId(),
                row.getSupplierName(),
                row.getProductId(),
                row.getMpn(),
                row.getMonth(),
                row.getAvgPrice(),
                row.getPrevAvgPrice(),
                variationPct,
                isOutlier
            ));
        }

        log.info("Price Index Variation: {} registros generados", result.size());
        return result;
    }

    // =========================================================================
    // KPI B — Stockout Rate
    // =========================================================================

    /**
     * Calcula la tasa de ruptura de stock (Stockout Rate) para cada proveedor activo.
     *
     * Stockout Rate = (SKUs con stock = 0) / (total SKUs del proveedor) * 100
     *
     * Un proveedor con stockout rate > 30% indica problemas sistemicos de disponibilidad.
     * Los datos provienen de ProductSupplier.stock (valor actual; no es historico).
     *
     * @return lista de StockoutRateDto ordenada por stockoutRate descendente
     */
    @Transactional(readOnly = true)
    public List<StockoutRateDto> getStockoutRates() {
        log.info("Calculando Stockout Rates por proveedor");

        List<StockoutRateProjection> rows = productSupplierRepository.findStockoutRatesGroupedBySupplierProjected();
        List<StockoutRateDto> result = new ArrayList<>(rows.size());

        for (StockoutRateProjection row : rows) {
            long total       = row.getTotalSkus() != null ? row.getTotalSkus() : 0L;
            long outOfStock  = row.getOutOfStockCount() != null ? row.getOutOfStockCount() : 0L;
            double rate      = total > 0 ? (outOfStock * 100.0) / total : 0.0;

            result.add(new StockoutRateDto(
                row.getSupplierId(),
                row.getSupplierName(),
                total,
                outOfStock,
                Math.round(rate * 100.0) / 100.0
            ));
        }

        result.sort((a, b) -> Double.compare(b.stockoutRate(), a.stockoutRate()));

        log.info("Stockout Rates: {} proveedores procesados", result.size());
        return result;
    }

    // ML dataset generation removed from service

    // =========================================================================
    // KPIs nuevos — alta y media viabilidad
    // =========================================================================

    @Transactional(readOnly = true)
    public List<PriceDispersionDto> getPriceDispersion(int maxRows) {
        log.info("Calculando Price Dispersion Index (maxRows={})", maxRows);
        List<PriceDispersionProjection> rows = productSupplierRepository.findPriceDispersionIndexProjected(maxRows);
        List<PriceDispersionDto> result = new ArrayList<>(rows.size());
        for (PriceDispersionProjection row : rows) {
            result.add(new PriceDispersionDto(
                row.getProductId(),
                row.getMpn(),
                row.getCategory(),
                row.getSupplierCount() != null ? row.getSupplierCount() : 0,
                row.getMinPrice(),
                row.getMaxPrice(),
                row.getAvgPrice(),
                row.getDispersionPct() != null ? row.getDispersionPct() : 0.0
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<MoqDistributionDto> getMoqDistribution() {
        log.info("Calculando MOQ Distribution");
        List<MoqDistributionProjection> rows = productSupplierRepository.findMoqDistributionPerSupplierProjected();
        List<MoqDistributionDto> result = new ArrayList<>(rows.size());
        for (MoqDistributionProjection row : rows) {
            result.add(new MoqDistributionDto(
                row.getSupplierId(),
                row.getSupplierName(),
                row.getAvgMoq() != null ? row.getAvgMoq() : 0.0,
                row.getMaxMoq() != null ? row.getMaxMoq() : 0,
                row.getMinMoq() != null ? row.getMinMoq() : 0,
                row.getSkusMoqAbove10() != null ? row.getSkusMoqAbove10() : 0L
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ConditionMixDto> getConditionMix() {
        log.info("Calculando Condition Mix");
        List<ConditionMixProjection> rows = productSupplierRepository.findConditionMixPerSupplierProjected();
        List<ConditionMixDto> result = new ArrayList<>(rows.size());
        for (ConditionMixProjection row : rows) {
            long total      = row.getTotalSkus() != null ? row.getTotalSkus() : 0L;
            long newC       = row.getNewCount() != null ? row.getNewCount() : 0L;
            long refurb     = row.getRefurbishedCount() != null ? row.getRefurbishedCount() : 0L;
            long boxDmg     = row.getBoxDamagedCount() != null ? row.getBoxDamagedCount() : 0L;
            long used       = row.getUsedCount() != null ? row.getUsedCount() : 0L;
            result.add(new ConditionMixDto(
                row.getSupplierId(),
                row.getSupplierName(),
                total,
                newC, refurb, boxDmg, used,
                total > 0 ? Math.round(newC   * 1000.0 / total) / 10.0 : 0,
                total > 0 ? Math.round(refurb * 1000.0 / total) / 10.0 : 0,
                total > 0 ? Math.round(boxDmg * 1000.0 / total) / 10.0 : 0,
                total > 0 ? Math.round(used   * 1000.0 / total) / 10.0 : 0
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<CostCoverageDto> getCostCoverage(int maxRows) {
        log.info("Calculando Cost Coverage Ratio (maxRows={})", maxRows);
        List<CostCoverageProjection> rows = productSupplierRepository.findCostCoverageRatioProjected(maxRows);
        List<CostCoverageDto> result = new ArrayList<>(rows.size());
        for (CostCoverageProjection row : rows) {
            result.add(new CostCoverageDto(
                row.getProductId(),
                row.getMpn(),
                row.getCategory(),
                row.getTotalSuppliers() != null ? row.getTotalSuppliers().intValue() : 0,
                row.getSuppliersWithStock() != null ? row.getSuppliersWithStock().intValue() : 0,
                row.getCoverageStatus()
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<StockVolatilityDto> getStockVolatility(int days, int maxRows) {
        log.info("Calculando Stock Volatility Index (days={}, maxRows={})", days, maxRows);
        List<StockVolatilityProjection> rows = stockHistoryRepository.findStockVolatilityProjected(days, maxRows);
        List<StockVolatilityDto> result = new ArrayList<>(rows.size());
        for (StockVolatilityProjection row : rows) {
            result.add(new StockVolatilityDto(
                row.getSupplierId(),
                row.getSupplierName(),
                row.getProductId(),
                row.getMpn(),
                row.getChangesIn24h() != null ? row.getChangesIn24h() : 0,
                row.getMaxStock() != null ? row.getMaxStock() : 0,
                row.getMinStock() != null ? row.getMinStock() : 0,
                row.getVolatilityType()
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<CatalogGrowthDto> getCatalogGrowth(int weeks) {
        log.info("Calculando Catalog Growth Trend (weeks={})", weeks);
        List<CatalogGrowthProjection> rows = masterProductRepository.findCatalogGrowthPerWeekProjected(weeks);
        List<CatalogGrowthDto> result = new ArrayList<>(rows.size());
        for (CatalogGrowthProjection row : rows) {
            int year = row.getYear() != null ? row.getYear() : 0;
            int week = row.getWeek() != null ? row.getWeek() : 0;
            long count = row.getNewProducts() != null ? row.getNewProducts() : 0L;
            result.add(new CatalogGrowthDto(year, week, count,
                    String.format("%d-W%02d", year, week)));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<PriceStabilityDto> getPriceStability(int days, int maxRows) {
        log.info("Calculando Price Stability Score (days={}, maxRows={})", days, maxRows);
        List<PriceStabilityProjection> rows = priceHistoryRepository.findPriceStabilityScoresProjected(days, maxRows);
        List<PriceStabilityDto> result = new ArrayList<>(rows.size());
        for (PriceStabilityProjection row : rows) {
            result.add(new PriceStabilityDto(
                row.getProductId(),
                row.getMpn(),
                row.getSupplierId(),
                row.getSupplierName(),
                row.getAvgPrice(),
                row.getStddevPrice(),
                row.getCvPct() != null ? row.getCvPct() : 0.0,
                row.getStabilityLabel(),
                row.getPricePoints() != null ? row.getPricePoints() : 0
            ));
        }
        return result;
    }

    // =========================================================================
    // Trading Opportunities — margen entre coste y PVP
    // =========================================================================

    /**
     * Obtiene las mejores oportunidades de trading: productos con margen positivo
     * entre el mejor precio de coste y el PVP recomendado mínimo.
     * <p>
     * Calcula gap = minRetailPrice - minCostPrice y marginPct = (gap / minCostPrice) * 100.
     * Ordena por gap descendente (mayor oportunidad primero).
     *
     * @param maxRows numero maximo de resultados (default 50)
     * @return lista de TradingOpportunityDto ordenada por gap descendente
     */
    @Transactional(readOnly = true)
    public List<TradingOpportunityDto> getTradingOpportunities(int maxRows) {
        log.info("Calculando Trading Opportunities (maxRows={})", maxRows);
        List<TradingOpportunityProjection> rows = productSupplierRepository.findTradingOpportunitiesProjected(maxRows);
        List<TradingOpportunityDto> result = new ArrayList<>(rows.size());
        for (TradingOpportunityProjection row : rows) {
            result.add(new TradingOpportunityDto(
                row.getId(),
                row.getMpn(),
                row.getModel(),
                row.getBrand(),
                row.getCategory(),
                row.getMinCostPrice(),
                row.getMinRetailPrice(),
                row.getGap(),
                row.getMarginPct()
            ));
        }
        log.info("Trading Opportunities: {} oportunidades encontradas", result.size());
        return result;
    }
}
