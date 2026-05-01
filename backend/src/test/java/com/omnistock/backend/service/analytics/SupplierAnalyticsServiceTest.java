package com.omnistock.backend.service.analytics;

import com.omnistock.backend.dtos.analytics.*;
import com.omnistock.backend.repository.*;
import com.omnistock.backend.repository.MasterProductRepository.CatalogGrowthProjection;
import com.omnistock.backend.repository.PriceHistoryRepository.PriceIndexVariationProjection;
import com.omnistock.backend.repository.PriceHistoryRepository.PriceStabilityProjection;
import com.omnistock.backend.repository.ProductSupplierRepository.*;
import com.omnistock.backend.repository.StockHistoryRepository.StockVolatilityProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierAnalyticsServiceTest {

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private ProductSupplierRepository productSupplierRepository;

    @Mock
    private MasterProductRepository masterProductRepository;

    @Mock
    private StockHistoryRepository stockHistoryRepository;

    private SupplierAnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new SupplierAnalyticsService(priceHistoryRepository,
                productSupplierRepository, masterProductRepository, stockHistoryRepository);
    }

    @Nested
    @DisplayName("getPriceIndexVariation()")
    class GetPriceIndexVariation {

        @Test
        @DisplayName("Debe devolver variación de precios")
        void shouldReturnPriceIndexVariation() {
            PriceIndexVariationProjection row = mock(PriceIndexVariationProjection.class);
            when(row.getSupplierId()).thenReturn(1);
            when(row.getSupplierName()).thenReturn("Supplier1");
            when(row.getProductId()).thenReturn(1);
            when(row.getMpn()).thenReturn("MPN-001");
            when(row.getMonth()).thenReturn("2025-01");
            when(row.getAvgPrice()).thenReturn(new BigDecimal("100.00"));
            when(row.getPrevAvgPrice()).thenReturn(new BigDecimal("90.00"));
            when(row.getVariationPct()).thenReturn(new BigDecimal("11.11"));

            when(priceHistoryRepository.findPriceIndexVariationProjected(6, 50.0))
                    .thenReturn(List.of(row));

            List<PriceIndexVariationDto> result = analyticsService.getPriceIndexVariation(6, 50.0);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).supplierName()).isEqualTo("Supplier1");
            assertThat(result.get(0).variationPct()).isEqualByComparingTo(new BigDecimal("11.11"));
            assertThat(result.get(0).isOutlier()).isFalse();
        }

        @Test
        @DisplayName("Debe marcar outliers cuando superan el umbral")
        void shouldMarkOutliers() {
            PriceIndexVariationProjection row = mock(PriceIndexVariationProjection.class);
            when(row.getSupplierId()).thenReturn(1);
            when(row.getSupplierName()).thenReturn("Supplier1");
            when(row.getProductId()).thenReturn(1);
            when(row.getMpn()).thenReturn("MPN-001");
            when(row.getMonth()).thenReturn("2025-01");
            when(row.getAvgPrice()).thenReturn(new BigDecimal("200.00"));
            when(row.getPrevAvgPrice()).thenReturn(new BigDecimal("100.00"));
            when(row.getVariationPct()).thenReturn(new BigDecimal("100.00"));

            when(priceHistoryRepository.findPriceIndexVariationProjected(6, 50.0))
                    .thenReturn(List.of(row));

            List<PriceIndexVariationDto> result = analyticsService.getPriceIndexVariation(6, 50.0);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isOutlier()).isTrue();
        }

        @Test
        @DisplayName("Debe usar fallback cuando falla la consulta principal")
        void shouldUseFallbackOnDataAccessException() {
            when(priceHistoryRepository.findPriceIndexVariationProjected(6, 50.0))
                    .thenThrow(mock(DataAccessException.class));
            when(priceHistoryRepository.findPriceIndexVariationWithoutCurrencyRatesProjected(6, 50.0))
                    .thenReturn(List.of());

            List<PriceIndexVariationDto> result = analyticsService.getPriceIndexVariation(6, 50.0);

            assertThat(result).isEmpty();
            verify(priceHistoryRepository).findPriceIndexVariationWithoutCurrencyRatesProjected(6, 50.0);
        }
    }

    @Nested
    @DisplayName("getStockoutRates()")
    class GetStockoutRates {

        @Test
        @DisplayName("Debe devolver tasas de ruptura de stock")
        void shouldReturnStockoutRates() {
            StockoutRateProjection row = mock(StockoutRateProjection.class);
            when(row.getSupplierId()).thenReturn(1);
            when(row.getSupplierName()).thenReturn("Supplier1");
            when(row.getTotalSkus()).thenReturn(100L);
            when(row.getOutOfStockCount()).thenReturn(10L);

            when(productSupplierRepository.findStockoutRatesGroupedBySupplierProjected())
                    .thenReturn(List.of(row));

            List<StockoutRateDto> result = analyticsService.getStockoutRates();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).supplierName()).isEqualTo("Supplier1");
            assertThat(result.get(0).stockoutRate()).isEqualTo(10.0); // 10/100 * 100
        }

        @Test
        @DisplayName("Debe devolver lista vacía si no hay datos")
        void shouldReturnEmptyList() {
            when(productSupplierRepository.findStockoutRatesGroupedBySupplierProjected())
                    .thenReturn(List.of());

            List<StockoutRateDto> result = analyticsService.getStockoutRates();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getPriceDispersion()")
    class GetPriceDispersion {

        @Test
        @DisplayName("Debe devolver dispersión de precios")
        void shouldReturnPriceDispersion() {
            PriceDispersionProjection row = mock(PriceDispersionProjection.class);
            when(row.getProductId()).thenReturn(1);
            when(row.getMpn()).thenReturn("MPN-001");
            when(row.getCategory()).thenReturn("CPU");
            when(row.getSupplierCount()).thenReturn(3);
            when(row.getMinPrice()).thenReturn(new BigDecimal("100.00"));
            when(row.getMaxPrice()).thenReturn(new BigDecimal("150.00"));
            when(row.getAvgPrice()).thenReturn(new BigDecimal("125.00"));
            when(row.getDispersionPct()).thenReturn(33.33);

            when(productSupplierRepository.findPriceDispersionIndexProjected(100))
                    .thenReturn(List.of(row));

            List<PriceDispersionDto> result = analyticsService.getPriceDispersion(100);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).mpn()).isEqualTo("MPN-001");
            assertThat(result.get(0).supplierCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("getMoqDistribution()")
    class GetMoqDistribution {

        @Test
        @DisplayName("Debe devolver distribución de MOQ")
        void shouldReturnMoqDistribution() {
            MoqDistributionProjection row = mock(MoqDistributionProjection.class);
            when(row.getSupplierId()).thenReturn(1);
            when(row.getSupplierName()).thenReturn("Supplier1");
            when(row.getAvgMoq()).thenReturn(15.0);
            when(row.getMaxMoq()).thenReturn(100);
            when(row.getMinMoq()).thenReturn(1);
            when(row.getSkusMoqAbove10()).thenReturn(5L);

            when(productSupplierRepository.findMoqDistributionPerSupplierProjected())
                    .thenReturn(List.of(row));

            List<MoqDistributionDto> result = analyticsService.getMoqDistribution();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).supplierName()).isEqualTo("Supplier1");
            assertThat(result.get(0).avgMoq()).isEqualTo(15.0);
        }
    }

    @Nested
    @DisplayName("getConditionMix()")
    class GetConditionMix {

        @Test
        @DisplayName("Debe devolver mezcla de condiciones")
        void shouldReturnConditionMix() {
            ConditionMixProjection row = mock(ConditionMixProjection.class);
            when(row.getSupplierId()).thenReturn(1);
            when(row.getSupplierName()).thenReturn("Supplier1");
            when(row.getTotalSkus()).thenReturn(100L);
            when(row.getNewCount()).thenReturn(60L);
            when(row.getRefurbishedCount()).thenReturn(20L);
            when(row.getBoxDamagedCount()).thenReturn(10L);
            when(row.getUsedCount()).thenReturn(10L);

            when(productSupplierRepository.findConditionMixPerSupplierProjected())
                    .thenReturn(List.of(row));

            List<ConditionMixDto> result = analyticsService.getConditionMix();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).supplierName()).isEqualTo("Supplier1");
            assertThat(result.get(0).newPct()).isEqualTo(60.0);
        }
    }

    @Nested
    @DisplayName("getCostCoverage()")
    class GetCostCoverage {

        @Test
        @DisplayName("Debe devolver cobertura de costes")
        void shouldReturnCostCoverage() {
            CostCoverageProjection row = mock(CostCoverageProjection.class);
            when(row.getProductId()).thenReturn(1);
            when(row.getMpn()).thenReturn("MPN-001");
            when(row.getCategory()).thenReturn("CPU");
            when(row.getTotalSuppliers()).thenReturn(3L);
            when(row.getSuppliersWithStock()).thenReturn(2L);
            when(row.getCoverageStatus()).thenReturn("PARCIAL");

            when(productSupplierRepository.findCostCoverageRatioProjected(200))
                    .thenReturn(List.of(row));

            List<CostCoverageDto> result = analyticsService.getCostCoverage(200);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).coverageStatus()).isEqualTo("PARCIAL");
        }
    }

    @Nested
    @DisplayName("getStockVolatility()")
    class GetStockVolatility {

        @Test
        @DisplayName("Debe devolver volatilidad de stock")
        void shouldReturnStockVolatility() {
            StockVolatilityProjection row = mock(StockVolatilityProjection.class);
            when(row.getSupplierId()).thenReturn(1);
            when(row.getSupplierName()).thenReturn("Supplier1");
            when(row.getProductId()).thenReturn(1);
            when(row.getMpn()).thenReturn("MPN-001");
            when(row.getChangesIn24h()).thenReturn(5);
            when(row.getMaxStock()).thenReturn(100);
            when(row.getMinStock()).thenReturn(10);
            when(row.getVolatilityType()).thenReturn("ALTA");

            when(stockHistoryRepository.findStockVolatilityProjected(30, 100))
                    .thenReturn(List.of(row));

            List<StockVolatilityDto> result = analyticsService.getStockVolatility(30, 100);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).volatilityType()).isEqualTo("ALTA");
        }
    }

    @Nested
    @DisplayName("getCatalogGrowth()")
    class GetCatalogGrowth {

        @Test
        @DisplayName("Debe devolver crecimiento del catálogo")
        void shouldReturnCatalogGrowth() {
            CatalogGrowthProjection row = mock(CatalogGrowthProjection.class);
            when(row.getYear()).thenReturn(2025);
            when(row.getWeek()).thenReturn(1);
            when(row.getNewProducts()).thenReturn(10L);

            when(masterProductRepository.findCatalogGrowthPerWeekProjected(12))
                    .thenReturn(List.of(row));

            List<CatalogGrowthDto> result = analyticsService.getCatalogGrowth(12);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).year()).isEqualTo(2025);
            assertThat(result.get(0).week()).isEqualTo(1);
            assertThat(result.get(0).newProducts()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("getPriceStability()")
    class GetPriceStability {

        @Test
        @DisplayName("Debe devolver estabilidad de precios")
        void shouldReturnPriceStability() {
            PriceStabilityProjection row = mock(PriceStabilityProjection.class);
            when(row.getProductId()).thenReturn(1);
            when(row.getMpn()).thenReturn("MPN-001");
            when(row.getSupplierId()).thenReturn(1);
            when(row.getSupplierName()).thenReturn("Supplier1");
            when(row.getAvgPrice()).thenReturn(new BigDecimal("100.00"));
            when(row.getStddevPrice()).thenReturn(new BigDecimal("5.00"));
            when(row.getCvPct()).thenReturn(5.0);
            when(row.getStabilityLabel()).thenReturn("ESTABLE");
            when(row.getPricePoints()).thenReturn(10);

            when(priceHistoryRepository.findPriceStabilityScoresProjected(90, 100))
                    .thenReturn(List.of(row));

            List<PriceStabilityDto> result = analyticsService.getPriceStability(90, 100);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).stabilityLabel()).isEqualTo("ESTABLE");
        }
    }
}
