import { describe, it, expect, vi, beforeEach } from 'vitest';
import { getDashboardSummary } from 'features/dashboard/services/dashboardService';

const { mockGet } = vi.hoisted(() => ({
  mockGet: vi.fn(),
}));

vi.mock('@/services/api', () => ({
  default: {
    get: mockGet,
  },
}));

describe('dashboardService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('debe hacer GET a /dashboard/summary y devolver DashboardSummaryDto', async () => {
    const mockResponse = {
      data: {
        data: {
          totalProducts: 100,
          totalSuppliers: 5,
          productsAvailable: 80,
          productsLowStock: 10,
          productsOutOfStock: 10,
          avgLatencyOverall: 2.5,
          productsByBrand: [],
          stockBySupplier: [],
          syncHistory: [],
          topExpensiveProducts: [],
          topPriceIncreases: [],
          topPriceDecreases: [],
          staleProducts: [],
          failedProviders: [],
        },
      },
    };

    mockGet.mockResolvedValue(mockResponse);

    const result = await getDashboardSummary();

    expect(mockGet).toHaveBeenCalledWith('/dashboard/summary');
    expect(result.totalProducts).toBe(100);
    expect(result.totalSuppliers).toBe(5);
  });

  it('debe lanzar error cuando la API devuelve error', async () => {
    mockGet.mockRejectedValue(new Error('Network Error'));
    await expect(getDashboardSummary()).rejects.toThrow('Network Error');
  });
});
