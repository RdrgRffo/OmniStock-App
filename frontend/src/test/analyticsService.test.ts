import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  getPriceIndexVariation,
  getStockoutRates,
  getPriceDispersion,
  getMoqDistribution,
  getConditionMix,
  getCostCoverage,
  getStockVolatility,
  getCatalogGrowth,
  getPriceStability,
} from 'features/dashboard/services/analyticsService';

const { mockGet } = vi.hoisted(() => ({
  mockGet: vi.fn(),
}));

vi.mock('@/services/api', () => ({
  default: {
    get: mockGet,
  },
}));

describe('analyticsService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getPriceIndexVariation', () => {
    it('debe hacer GET a price variation con parámetros por defecto', async () => {
      mockGet.mockResolvedValue({ data: { data: [] } });
      await getPriceIndexVariation();
      expect(mockGet).toHaveBeenCalledWith('/analytics/price-variation', {
        params: { months: 6, outlierThreshold: 50.0 },
      });
    });

    it('debe hacer GET a price variation con parámetros personalizados', async () => {
      mockGet.mockResolvedValue({ data: { data: [] } });
      await getPriceIndexVariation(12, 75.0);
      expect(mockGet).toHaveBeenCalledWith('/analytics/price-variation', {
        params: { months: 12, outlierThreshold: 75.0 },
      });
    });
  });

  describe('getStockoutRates', () => {
    it('debe hacer GET a stockout rates', async () => {
      const mockData = [
        { supplierId: 1, supplierName: 'S1', totalSkus: 10, outOfStockSkus: 2, stockoutRate: 20.0 },
      ];
      mockGet.mockResolvedValue({ data: { data: mockData } });
      const result = await getStockoutRates();
      expect(mockGet).toHaveBeenCalledWith('/analytics/stockout-rates');
      expect(result).toHaveLength(1);
      expect(result[0].stockoutRate).toBe(20.0);
    });
  });

  describe('getPriceDispersion', () => {
    it('debe hacer GET a price dispersion con maxRows por defecto', async () => {
      mockGet.mockResolvedValue({ data: { data: [] } });
      await getPriceDispersion();
      expect(mockGet).toHaveBeenCalledWith('/analytics/price-dispersion', {
        params: { maxRows: 100 },
      });
    });
  });

  describe('getMoqDistribution', () => {
    it('debe hacer GET a MOQ distribution', async () => {
      mockGet.mockResolvedValue({ data: { data: [] } });
      await getMoqDistribution();
      expect(mockGet).toHaveBeenCalledWith('/analytics/moq');
    });
  });

  describe('getConditionMix', () => {
    it('debe hacer GET a condition mix', async () => {
      mockGet.mockResolvedValue({ data: { data: [] } });
      await getConditionMix();
      expect(mockGet).toHaveBeenCalledWith('/analytics/condition-mix');
    });
  });

  describe('getCostCoverage', () => {
    it('debe hacer GET a cost coverage con maxRows por defecto', async () => {
      mockGet.mockResolvedValue({ data: { data: [] } });
      await getCostCoverage();
      expect(mockGet).toHaveBeenCalledWith('/analytics/cost-coverage', {
        params: { maxRows: 200 },
      });
    });
  });

  describe('getStockVolatility', () => {
    it('debe hacer GET a stock volatility con parámetros por defecto', async () => {
      mockGet.mockResolvedValue({ data: { data: [] } });
      await getStockVolatility();
      expect(mockGet).toHaveBeenCalledWith('/analytics/stock-volatility', {
        params: { days: 30, maxRows: 100 },
      });
    });
  });

  describe('getCatalogGrowth', () => {
    it('debe hacer GET a catalog growth con semanas por defecto', async () => {
      mockGet.mockResolvedValue({ data: { data: [] } });
      await getCatalogGrowth();
      expect(mockGet).toHaveBeenCalledWith('/analytics/catalog-growth', {
        params: { weeks: 12 },
      });
    });
  });

  describe('getPriceStability', () => {
    it('debe hacer GET a price stability con parámetros por defecto', async () => {
      mockGet.mockResolvedValue({ data: { data: [] } });
      await getPriceStability();
      expect(mockGet).toHaveBeenCalledWith('/analytics/price-stability', {
        params: { days: 90, maxRows: 100 },
      });
    });
  });
});
