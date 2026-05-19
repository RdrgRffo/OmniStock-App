import { describe, it, expect, vi, beforeEach } from 'vitest';
import { simulateBudget, getUserBudgets, getBudgetById, updateBudgetStatus, deleteBudget, notifyBudgetExported } from 'features/budget/services/budgetService';

// Mock de api usando vi.hoisted para evitar problemas de hoisting
const { mockPost, mockGet, mockPut, mockDelete } = vi.hoisted(() => ({
  mockPost: vi.fn(),
  mockGet: vi.fn(),
  mockPut: vi.fn(),
  mockDelete: vi.fn(),
}));

vi.mock('@/services/api', () => ({
  default: {
    post: mockPost,
    get: mockGet,
    put: mockPut,
    delete: mockDelete,
  },
}));

describe('budgetService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('simulateBudget', () => {
    it('debe hacer POST al endpoint de simulación y devolver los datos', async () => {
      const mockResponse = {
        data: {
          success: true,
          message: 'Presupuesto simulado correctamente',
          data: {
            id: 1,
            budgetNumber: 'PRES-20260522-001',
            budgetName: 'Test Budget',
            status: 'DRAFT',
            totalAmount: 200,
            items: [],
          },
        },
      };

      mockPost.mockResolvedValue(mockResponse);

      const request = {
        budgetName: 'Test Budget',
        lines: [{ productId: 1, supplierId: 1, quantity: 2 }],
      };

      const result = await simulateBudget(request);

      expect(mockPost).toHaveBeenCalledWith(
        '/budget/simulate',
        request,
      );
      expect(result.budgetNumber).toBe('PRES-20260522-001');
      expect(result.totalAmount).toBe(200);
    });

    it('debe lanzar error cuando la API devuelve un error', async () => {
      mockPost.mockRejectedValue(new Error('Network Error'));

      const request = {
        budgetName: 'Test',
        lines: [{ productId: 1, supplierId: 1, quantity: 1 }],
      };

      await expect(simulateBudget(request)).rejects.toThrow('Network Error');
    });
  });

  describe('getUserBudgets', () => {
    it('debe hacer GET a la lista de presupuestos y devolver un array de datos', async () => {
      const mockResponse = {
        data: {
          success: true,
          message: 'OK',
          data: [
            { id: 1, budgetNumber: 'PRES-001', budgetName: 'Budget 1', status: 'DRAFT', totalAmount: 100, items: [] },
            { id: 2, budgetNumber: 'PRES-002', budgetName: 'Budget 2', status: 'FINALIZED', totalAmount: 200, items: [] },
          ],
        },
      };

      mockGet.mockResolvedValue(mockResponse);

      const result = await getUserBudgets();

      expect(mockGet).toHaveBeenCalledWith('/budget');
      expect(result).toHaveLength(2);
      expect(result[0].budgetName).toBe('Budget 1');
    });

    it('debe devolver un array vacío cuando no hay presupuestos', async () => {
      const mockResponse = {
        data: {
          success: true,
          message: 'OK',
          data: [],
        },
      };

      mockGet.mockResolvedValue(mockResponse);

      const result = await getUserBudgets();
      expect(result).toHaveLength(0);
    });
  });

  describe('getBudgetById', () => {
    it('debe hacer GET al presupuesto por id y devolver los datos', async () => {
      const mockResponse = {
        data: {
          success: true,
          message: 'OK',
          data: {
            id: 42,
            budgetNumber: 'PRES-042',
            budgetName: 'Specific Budget',
            status: 'DRAFT',
            totalAmount: 500,
            items: [{ productId: 1, productName: 'Test', quantity: 5 }],
          },
        },
      };

      mockGet.mockResolvedValue(mockResponse);

      const result = await getBudgetById(42);

      expect(mockGet).toHaveBeenCalledWith('/budget/42');
      expect(result.id).toBe(42);
      expect(result.budgetName).toBe('Specific Budget');
    });
  });

  describe('updateBudgetStatus', () => {
    it('debe hacer PUT del estado y devolver el presupuesto actualizado', async () => {
      const mockResponse = {
        data: {
          success: true,
          message: 'Estado actualizado',
          data: {
            id: 1,
            budgetNumber: 'PRES-001',
            status: 'FINALIZED',
            totalAmount: 100,
            items: [],
          },
        },
      };

      mockPut.mockResolvedValue(mockResponse);

      const result = await updateBudgetStatus(1, 'FINALIZED');

      expect(mockPut).toHaveBeenCalledWith(
        '/budget/1/status',
        null,
        { params: { status: 'FINALIZED' } },
      );
      expect(result.status).toBe('FINALIZED');
    });
  });

  describe('deleteBudget', () => {
    it('debe hacer DELETE del presupuesto por id', async () => {
      mockDelete.mockResolvedValue({});

      await deleteBudget(5);

      expect(mockDelete).toHaveBeenCalledWith('/budget/5');
    });
  });

  describe('notifyBudgetExported', () => {
    it('debe hacer POST al endpoint de exportación', async () => {
      mockPost.mockResolvedValue({});

      await notifyBudgetExported(3);

      expect(mockPost).toHaveBeenCalledWith('/budget/3/export');
    });
  });
});
