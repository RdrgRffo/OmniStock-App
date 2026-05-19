import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  getProducts,
  searchProducts,
  syncAllProducts,
  getSyncStatus,
  getProductDetails,
} from 'features/inventory/services/productService';

const { mockGet, mockPost } = vi.hoisted(() => ({
  mockGet: vi.fn(),
  mockPost: vi.fn(),
}));

vi.mock('@/services/api', () => ({
  default: {
    get: mockGet,
    post: mockPost,
  },
}));

describe('productService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getProducts', () => {
    it('debe hacer GET a /productos con los parámetros correctos', async () => {
      const mockResponse = {
        data: {
          data: {
            content: [],
            totalPages: 5,
            totalElements: 100,
          },
        },
      };
      mockGet.mockResolvedValue(mockResponse);

      const result = await getProducts(0, 20, 'model', 'ASC');

      expect(mockGet).toHaveBeenCalledWith('/productos', {
        params: { page: 0, size: 20, sortBy: 'model', direction: 'ASC' },
      });
      expect(result.totalPages).toBe(5);
    });

    it('debe mapear ordenación por disponibilidad a disponibilidadRank', async () => {
      mockGet.mockResolvedValue({ data: { data: { content: [], totalPages: 1, totalElements: 0 } } });

      await getProducts(0, 20, 'disponibilidad', 'ASC');

      expect(mockGet).toHaveBeenCalledWith('/productos', {
        params: { page: 0, size: 20, sortBy: 'disponibilidadRank', direction: 'ASC' },
      });
    });
  });

  describe('searchProducts', () => {
    it('debe hacer GET a /productos/search con parámetros de búsqueda', async () => {
      const mockResponse = {
        data: {
          data: {
            content: [{ id: 1, mpn: 'MPN-001', model: 'Model A', brand: 'Brand A', availability: 'DISPONIBLE', supplierCount: 2 }],
            totalPages: 1,
            totalElements: 1,
          },
        },
      };
      mockGet.mockResolvedValue(mockResponse);

      const result = await searchProducts('test', 0, 20, 'model', 'ASC');

      expect(mockGet).toHaveBeenCalledWith('/productos/search', {
        params: { query: 'test', page: 0, size: 20, sortBy: 'model', direction: 'ASC' },
      });
      expect(result.content).toHaveLength(1);
    });

    it('debe incluir filtros opcionales cuando se proporcionan', async () => {
      mockGet.mockResolvedValue({ data: { data: { content: [], totalPages: 1, totalElements: 0 } } });

      await searchProducts('test', 0, 20, 'model', 'ASC', 'PC', 'Componentes', '{"ram":"8GB"}');

      expect(mockGet).toHaveBeenCalledWith('/productos/search', {
        params: {
          query: 'test', page: 0, size: 20, sortBy: 'model', direction: 'ASC',
          categoria: 'PC', productCategory: 'Componentes', specsFilter: '{"ram":"8GB"}',
        },
      });
    });
  });

  describe('syncAllProducts', () => {
    it('debe hacer POST a /productos/sync-all', async () => {
      mockPost.mockResolvedValue({});
      await syncAllProducts();
      expect(mockPost).toHaveBeenCalledWith('/productos/sync-all');
    });
  });

  describe('getSyncStatus', () => {
    it('debe hacer GET a /productos/sync-status', async () => {
      mockGet.mockResolvedValue({ data: { data: { status: 'IDLE' } } });
      const result = await getSyncStatus();
      expect(mockGet).toHaveBeenCalledWith('/productos/sync-status');
      expect(result.status).toBe('IDLE');
    });
  });

  describe('getProductDetails', () => {
    it('debe hacer GET a /productos/:id', async () => {
      const mockDetail = {
        id: 1,
        mpn: 'MPN-001',
        model: 'Model A',
        brand: 'Brand A',
        category: 'PC',
        specifications: {},
        offers: [],
      };
      mockGet.mockResolvedValue({ data: { data: mockDetail } });

      const result = await getProductDetails(1);

      expect(mockGet).toHaveBeenCalledWith('/productos/1');
      expect(result.id).toBe(1);
      expect(result.mpn).toBe('MPN-001');
    });
  });
});
