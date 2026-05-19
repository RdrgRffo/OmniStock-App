import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  getSuppliers,
  getSupplierDetails,
  createSupplier,
  updateSupplier,
  deleteSupplier,
  getSupplierMappings,
  createSupplierMapping,
  updateSupplierMapping,
  deleteSupplierMapping,
} from 'features/suppliers/services/supplierService';

const { mockGet, mockPost, mockPut, mockDelete } = vi.hoisted(() => ({
  mockGet: vi.fn(),
  mockPost: vi.fn(),
  mockPut: vi.fn(),
  mockDelete: vi.fn(),
}));

vi.mock('@/services/api', () => ({
  default: {
    get: mockGet,
    post: mockPost,
    put: mockPut,
    delete: mockDelete,
  },
}));

describe('supplierService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getSuppliers', () => {
    it('debe hacer GET a /proveedores/list', async () => {
      mockGet.mockResolvedValue({ data: { data: [{ id: 1, name: 'Supplier A' }] } });
      const result = await getSuppliers();
      expect(mockGet).toHaveBeenCalledWith('/proveedores/list');
      expect(result).toHaveLength(1);
    });
  });

  describe('getSupplierDetails', () => {
    it('debe hacer GET a /proveedores/:id', async () => {
      mockGet.mockResolvedValue({ data: { data: { id: 1, name: 'Supplier A', baseUrlApi: 'https://api.example.com', active: true, supportsBulkSync: true, catalogEndpoint: '/catalog', detailEndpoint: '/detail', searchEndpoint: '/search' } } });
      const result = await getSupplierDetails(1);
      expect(mockGet).toHaveBeenCalledWith('/proveedores/1');
      expect(result.name).toBe('Supplier A');
    });
  });

  describe('createSupplier', () => {
    it('debe hacer POST a /proveedores', async () => {
      const supplier = {
        name: 'New Supplier',
        baseUrlApi: 'https://api.example.com',
        active: true,
        supportsBulkSync: true,
        catalogEndpoint: '/catalog',
        detailEndpoint: '/detail',
        searchEndpoint: '/search',
      };
      mockPost.mockResolvedValue({ data: { data: { id: 1, ...supplier } } });
      const result = await createSupplier(supplier);
      expect(mockPost).toHaveBeenCalledWith('/proveedores', supplier);
      expect(result.id).toBe(1);
    });
  });

  describe('updateSupplier', () => {
    it('debe hacer PUT a /proveedores/:id', async () => {
      const supplier = {
        name: 'Updated Supplier',
        baseUrlApi: 'https://api.example.com',
        active: true,
        supportsBulkSync: true,
        catalogEndpoint: '/catalog',
        detailEndpoint: '/detail',
        searchEndpoint: '/search',
      };
      mockPut.mockResolvedValue({ data: { data: { id: 1, ...supplier } } });
      const result = await updateSupplier(1, supplier);
      expect(mockPut).toHaveBeenCalledWith('/proveedores/1', supplier);
      expect(result.name).toBe('Updated Supplier');
    });
  });

  describe('deleteSupplier', () => {
    it('debe hacer DELETE a /proveedores/:id', async () => {
      mockDelete.mockResolvedValue({});
      await deleteSupplier(1);
      expect(mockDelete).toHaveBeenCalledWith('/proveedores/1');
    });
  });

  describe('getSupplierMappings', () => {
    it('debe hacer GET a /proveedores/:id/mappings', async () => {
      mockGet.mockResolvedValue({ data: { data: [{ mappingId: 1, supplierId: 1, internalField: 'name', externalField: 'nombre', transformationType: 'DIRECT' }] } });
      const result = await getSupplierMappings(1);
      expect(mockGet).toHaveBeenCalledWith('/proveedores/1/mappings');
      expect(result).toHaveLength(1);
    });
  });

  describe('createSupplierMapping', () => {
    it('debe hacer POST a /proveedores/:id/mappings', async () => {
      const mapping = { internalField: 'name', externalField: 'nombre', transformationType: 'DIRECT' as const };
      mockPost.mockResolvedValue({ data: { data: { mappingId: 1, supplierId: 1, ...mapping } } });
      const result = await createSupplierMapping(1, mapping);
      expect(mockPost).toHaveBeenCalledWith('/proveedores/1/mappings', mapping);
      expect(result.mappingId).toBe(1);
    });
  });

  describe('updateSupplierMapping', () => {
    it('debe hacer PUT a /mappings/:id', async () => {
      const mapping = { internalField: 'name', externalField: 'nombre_updated', transformationType: 'DIRECT' as const };
      mockPut.mockResolvedValue({ data: { data: { mappingId: 1, supplierId: 1, ...mapping } } });
      const result = await updateSupplierMapping(1, mapping);
      expect(mockPut).toHaveBeenCalledWith('/mappings/1', mapping);
      expect(result.externalField).toBe('nombre_updated');
    });
  });

  describe('deleteSupplierMapping', () => {
    it('debe hacer DELETE a /mappings/:id', async () => {
      mockDelete.mockResolvedValue({});
      await deleteSupplierMapping(1);
      expect(mockDelete).toHaveBeenCalledWith('/mappings/1');
    });
  });
});
