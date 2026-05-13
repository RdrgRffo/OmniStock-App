import api from 'services/api';
import {
  PriceIndexVariationDto, StockoutRateDto,
  PriceDispersionDto, MoqDistributionDto, ConditionMixDto,
  CostCoverageDto, StockVolatilityDto, CatalogGrowthDto, PriceStabilityDto,
  TradingOpportunityDto,
} from 'src/types';

/**
 * Obtiene la variacion mensual del indice de precios por proveedor y SKU.
 * Usa window function LAG() sobre historial_precios.
 *
 * @param months           ventana de analisis en meses (default 6)
 * @param outlierThreshold umbral de variacion para marcar outliers (default 50.0)
 */
export const getPriceIndexVariation = async (
  months = 6,
  outlierThreshold = 50.0
): Promise<PriceIndexVariationDto[]> => {
  const response = await api.get('/analytics/price-variation', {
    params: { months, outlierThreshold },
  });
  return response.data.data;
};

/**
 * Obtiene la tasa de ruptura de stock (Stockout Rate) por proveedor.
 * Ordenado por stockoutRate descendente.
 */
export const getStockoutRates = async (): Promise<StockoutRateDto[]> => {
  const response = await api.get('/analytics/stockout-rates');
  return response.data.data;
};

// API de dataset ML eliminada — función obsoleta y eliminada.

export const getPriceDispersion = async (maxRows = 100): Promise<PriceDispersionDto[]> => {
  const response = await api.get('/analytics/price-dispersion', { params: { maxRows } });
  return response.data.data;
};

export const getMoqDistribution = async (): Promise<MoqDistributionDto[]> => {
  const response = await api.get('/analytics/moq');
  return response.data.data;
};

export const getConditionMix = async (): Promise<ConditionMixDto[]> => {
  const response = await api.get('/analytics/condition-mix');
  return response.data.data;
};

export const getCostCoverage = async (maxRows = 200): Promise<CostCoverageDto[]> => {
  const response = await api.get('/analytics/cost-coverage', { params: { maxRows } });
  return response.data.data;
};

export const getStockVolatility = async (days = 30, maxRows = 100): Promise<StockVolatilityDto[]> => {
  const response = await api.get('/analytics/stock-volatility', { params: { days, maxRows } });
  return response.data.data;
};

export const getCatalogGrowth = async (weeks = 12): Promise<CatalogGrowthDto[]> => {
  const response = await api.get('/analytics/catalog-growth', { params: { weeks } });
  return response.data.data;
};

export const getPriceStability = async (days = 90, maxRows = 100): Promise<PriceStabilityDto[]> => {
  const response = await api.get('/analytics/price-stability', { params: { days, maxRows } });
  return response.data.data;
};

/**
 * Obtiene oportunidades de trading (productos con brecha positiva entre coste y PVP).
 * Ordenado por gap descendente.
 *
 * @param maxRows número máximo de filas (default 50)
 */
export const getTradingOpportunities = async (maxRows = 50): Promise<TradingOpportunityDto[]> => {
  const response = await api.get('/analytics/trading-opportunities', { params: { maxRows } });
  return response.data.data;
};
