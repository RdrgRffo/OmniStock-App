package com.omnistock.backend.controller;

import com.omnistock.backend.dtos.analytics.*;
import com.omnistock.backend.service.analytics.SupplierAnalyticsService;
import com.omnistock.backend.util.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para los KPIs avanzados de Supply Chain Analytics.
 *
 * Todos los endpoints requieren autenticacion (ADMIN o CLIENTE).
 * La ruta base es /api/v1/analytics.
 *
 * Endpoints disponibles:
 *   GET /price-variation   — variacion mensual de indices de precio (LAG)
 *   GET /stockout-rates    — tasa de ruptura de stock por proveedor
 *   GET /price-dispersion  — dispersion de precio entre proveedores por SKU
 *   GET /moq               — distribucion de MOQ por proveedor
 *   GET /condition-mix     — mezcla de condicion de producto por proveedor
 *   GET /cost-coverage     — cobertura de proveedor por SKU (riesgo proveedor unico)
 *   GET /stock-volatility  — indice de volatilidad de stock (24h)
 *   GET /catalog-growth    — tendencia de crecimiento del catalogo (semanal)
 *   GET /price-stability   — score de estabilidad de precio por SKU/proveedor
 */
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final SupplierAnalyticsService analyticsService;

    public AnalyticsController(SupplierAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Devuelve la variacion mensual del indice de precios por proveedor y SKU.
     *
     * Usa window function LAG() sobre historial_precios agrupado por mes.
     * Los precios se normalizan a EUR usando currency_rates.
     *
     * @param months           numero de meses a analizar (default 6)
     * @param outlierThreshold variacion porcentual maxima antes de marcar como outlier (default 50.0)
     * @return lista de PriceIndexVariationDto ordenada por proveedor, SKU y mes
     */
    @GetMapping("/price-variation")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PriceIndexVariationDto>>> getPriceVariation(
            @RequestParam(defaultValue = "6")    int    months,
            @RequestParam(defaultValue = "50.0") double outlierThreshold) {

        log.info("GET /api/v1/analytics/price-variation months={} outlierThreshold={}", months, outlierThreshold);
        try {
            List<PriceIndexVariationDto> data = analyticsService.getPriceIndexVariation(months, outlierThreshold);
            return ResponseEntity.ok(ApiResponse.success(data, "Price Index Variation calculado correctamente"));
        } catch (Exception ex) {
            log.error("Error al calcular Price Index Variation: {}", ex.getMessage(), ex);
            ApiResponse<List<PriceIndexVariationDto>> errorResp = ApiResponse.error(
                "No se pudo calcular Price Index Variation: " + ex.getMessage());
            errorResp.setData(List.of());
            errorResp.getMetadata().put("exception", ex.getClass().getSimpleName());
            errorResp.getMetadata().put("reason", ex.getMessage());
            return ResponseEntity.ok(errorResp);
        }
    }

    /**
     * Devuelve la tasa de ruptura de stock (Stockout Rate) por proveedor.
     *
     * Calcula: (SKUs con stock = 0) / total SKUs * 100 para cada proveedor activo.
     * Ordena por stockoutRate descendente para destacar los proveedores mas criticos.
     *
     * @return lista de StockoutRateDto ordenada por tasa de ruptura descendente
     */
    @GetMapping("/stockout-rates")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StockoutRateDto>>> getStockoutRates() {

        log.info("GET /api/v1/analytics/stockout-rates");
        List<StockoutRateDto> data = analyticsService.getStockoutRates();
        return ResponseEntity.ok(ApiResponse.success(data, "Stockout Rates calculados correctamente"));
    }

    @GetMapping("/price-dispersion")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PriceDispersionDto>>> getPriceDispersion(
            @RequestParam(defaultValue = "100") int maxRows) {
        log.info("GET /api/v1/analytics/price-dispersion maxRows={}", maxRows);
        List<PriceDispersionDto> data = analyticsService.getPriceDispersion(maxRows);
        return ResponseEntity.ok(ApiResponse.success(data, "Price Dispersion Index calculado correctamente"));
    }

    @GetMapping("/moq")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<MoqDistributionDto>>> getMoqDistribution() {
        log.info("GET /api/v1/analytics/moq");
        List<MoqDistributionDto> data = analyticsService.getMoqDistribution();
        return ResponseEntity.ok(ApiResponse.success(data, "MOQ Distribution calculado correctamente"));
    }

    @GetMapping("/condition-mix")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ConditionMixDto>>> getConditionMix() {
        log.info("GET /api/v1/analytics/condition-mix");
        List<ConditionMixDto> data = analyticsService.getConditionMix();
        return ResponseEntity.ok(ApiResponse.success(data, "Condition Mix calculado correctamente"));
    }

    @GetMapping("/cost-coverage")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CostCoverageDto>>> getCostCoverage(
            @RequestParam(defaultValue = "200") int maxRows) {
        log.info("GET /api/v1/analytics/cost-coverage maxRows={}", maxRows);
        List<CostCoverageDto> data = analyticsService.getCostCoverage(maxRows);
        return ResponseEntity.ok(ApiResponse.success(data, "Cost Coverage Ratio calculado correctamente"));
    }

    @GetMapping("/stock-volatility")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StockVolatilityDto>>> getStockVolatility(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "100") int maxRows) {
        log.info("GET /api/v1/analytics/stock-volatility days={} maxRows={}", days, maxRows);
        List<StockVolatilityDto> data = analyticsService.getStockVolatility(days, maxRows);
        return ResponseEntity.ok(ApiResponse.success(data, "Stock Volatility Index calculado correctamente"));
    }

    @GetMapping("/catalog-growth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CatalogGrowthDto>>> getCatalogGrowth(
            @RequestParam(defaultValue = "12") int weeks) {
        log.info("GET /api/v1/analytics/catalog-growth weeks={}", weeks);
        List<CatalogGrowthDto> data = analyticsService.getCatalogGrowth(weeks);
        return ResponseEntity.ok(ApiResponse.success(data, "Catalog Growth Trend calculado correctamente"));
    }

    @GetMapping("/price-stability")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PriceStabilityDto>>> getPriceStability(
            @RequestParam(defaultValue = "90") int days,
            @RequestParam(defaultValue = "100") int maxRows) {
        log.info("GET /api/v1/analytics/price-stability days={} maxRows={}", days, maxRows);
        List<PriceStabilityDto> data = analyticsService.getPriceStability(days, maxRows);
        return ResponseEntity.ok(ApiResponse.success(data, "Price Stability Score calculado correctamente"));
    }

    /**
     * Devuelve las mejores oportunidades de trading: productos con margen positivo
     * entre el mejor precio de coste y el PVP recomendado mínimo.
     * <p>
     * Calcula gap = minRetailPrice - minCostPrice y marginPct = (gap / minCostPrice) * 100.
     * Ordena por gap descendente (mayor oportunidad primero).
     *
     * @param maxRows numero maximo de resultados (default 50)
     * @return lista de TradingOpportunityDto ordenada por gap descendente
     */
    @GetMapping("/trading-opportunities")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<TradingOpportunityDto>>> getTradingOpportunities(
            @RequestParam(defaultValue = "50") int maxRows) {
        log.info("GET /api/v1/analytics/trading-opportunities maxRows={}", maxRows);
        List<TradingOpportunityDto> data = analyticsService.getTradingOpportunities(maxRows);
        return ResponseEntity.ok(ApiResponse.success(data, "Trading Opportunities calculadas correctamente"));
    }
}
