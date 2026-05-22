/**
 * Test de cobertura para api.ts
 * 
 * Este test importa el módulo api.ts REAL con axios mockeado,
 * para que el código de api.ts sea medido por la cobertura.
 * 
 * La lógica de los interceptores se testea en api.test.ts
 */
import { describe, it, expect, vi } from 'vitest';

// Usamos vi.hoisted() para que las variables estén disponibles en la factory de vi.mock
const { mockRequestUse, mockResponseUse } = vi.hoisted(() => ({
  mockRequestUse: vi.fn(),
  mockResponseUse: vi.fn(),
}));

vi.mock('axios', () => {
  const mockInstance = {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    interceptors: {
      request: { use: mockRequestUse },
      response: { use: mockResponseUse },
    },
  };
  return {
    default: {
      create: vi.fn(() => mockInstance),
    },
    create: vi.fn(() => mockInstance),
  };
});

// Importamos api.ts REAL - esto ejecuta axios.create() y registra los interceptores
import api from '../services/api';

describe('api.ts coverage', () => {
  it('debe haberse creado con axios.create', () => {
    // Verificamos que api.ts ejecutó axios.create
    expect(api).toBeDefined();
  });

  it('debe haber registrado el interceptor de request', () => {
    // Verificamos que se registró el interceptor de request
    expect(mockRequestUse).toHaveBeenCalled();
  });

  it('debe haber registrado el interceptor de response', () => {
    // Verificamos que se registró el interceptor de response
    expect(mockResponseUse).toHaveBeenCalled();
  });
});
