import { describe, it, expect, vi, beforeEach } from 'vitest';
import { getSupplierHealth } from 'features/dashboard/services/supplierHealthService';
const { mockGet } = vi.hoisted(() => ({
  mockGet: vi.fn(),
}));

vi.mock('@/services/api', () => ({
  default: {
    get: mockGet,
  },
}));

describe('supplierHealthService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('debe hacer GET a supplier health y devolver array de SupplierHealthDto', async () => {
    const mockData = [
      {
        supplierId: 1,
        supplierName: 'Supplier A',
        slaScore: 95,
        syncSuccessRate: 98.5,
        avgItemsPerSync: 50,
        avgLatencyMs: 1200,
        apiErrorRate: 1.5,
        staleRate: 2.0,
        slaGrade: 'A',
      },
    ];

    mockGet.mockResolvedValue({ data: { data: mockData } });

    const result = await getSupplierHealth();

    expect(mockGet).toHaveBeenCalledWith('/dashboard/supplier-health');
    expect(result).toHaveLength(1);
    expect(result[0].slaGrade).toBe('A');
    expect(result[0].slaScore).toBe(95);
  });

  it('debe lanzar error cuando la API devuelve error', async () => {
    mockGet.mockRejectedValue(new Error('Network Error'));
    await expect(getSupplierHealth()).rejects.toThrow('Network Error');
  });
});
