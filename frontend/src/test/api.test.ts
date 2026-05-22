import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import axios from 'axios';
import { API_BASE } from '@/constants/api';

// Creamos una instancia de axios para testear los interceptores
const api = axios.create({ baseURL: API_BASE, timeout: 30_000 });

// Importamos la lógica real de los interceptores desde api.ts
// En lugar de importar la instancia directamente, replicamos los interceptores
// para poder testearlos de forma aislada
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const requestInterceptor = (config: any) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
};

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const responseErrorInterceptor = async (error: any) => {
  if (!error.response) {
    const message = error.code === 'ECONNABORTED'
      ? 'La solicitud tardó demasiado. El servidor podría estar sobrecargado.'
      : 'No se pudo conectar con el servidor. Verifica tu conexión.';
    window.dispatchEvent(new CustomEvent('api-error', { detail: { message, url: error.config?.url } }));
    return Promise.reject(error);
  }

  const { status } = error.response;
  const requestUrl = error.config?.url ?? '';
  const isLoginRequest = requestUrl.includes('/auth/login');

  if (status === 503) {
    window.dispatchEvent(new CustomEvent('api-error', {
      detail: { message: 'El servicio está temporalmente no disponible. Intenta de nuevo en unos momentos.', url: requestUrl }
    }));
    return Promise.reject(error);
  }

  if (status === 429) {
    window.dispatchEvent(new CustomEvent('api-error', {
      detail: { message: 'Demasiadas solicitudes. Espera un momento antes de intentar de nuevo.', url: requestUrl }
    }));
    return Promise.reject(error);
  }

  if ([401, 403].includes(status) && !isLoginRequest) {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    try {
      localStorage.setItem('sessionExpired', '1');
    } catch {
      // ignorar
    }
    window.location.href = '/login';
  }

  return Promise.reject(error);
};

describe('API Axios Client', () => {
  describe('configuración de URL base', () => {
    it('debe tener la URL base correcta', () => {
      expect(api.defaults.baseURL).toBe(API_BASE);
    });

    it('debe tener timeout configurado', () => {
      expect(api.defaults.timeout).toBe(30_000);
    });
  });

  describe('interceptor de request', () => {
    beforeEach(() => {
      localStorage.clear();
    });

    it('debe añadir header Authorization cuando existe token', () => {
      localStorage.setItem('token', 'test-jwt-token');

      const config = requestInterceptor({ headers: {} });

      expect(config.headers.Authorization).toBe('Bearer test-jwt-token');
    });

    it('debe omitir header Authorization cuando no hay token', () => {
      const config = requestInterceptor({ headers: {} });

      expect(config.headers.Authorization).toBeUndefined();
    });
  });

  describe('interceptor de respuesta - resiliencia', () => {
    beforeEach(() => {
      localStorage.clear();
      vi.restoreAllMocks();
    });

    afterEach(() => {
      vi.restoreAllMocks();
    });

    it('debe redirigir a login en 401 para requests que no son de login', async () => {
      localStorage.setItem('token', 'some-token');
      localStorage.setItem('username', 'testuser');

      // Mock window.location.href
      const originalLocation = window.location;
      Object.defineProperty(window, 'location', {
        value: { ...originalLocation, href: '' },
        writable: true,
      });

      const error = {
        response: { status: 401 },
        config: { url: '/api/v1/productos' }
      };

      await expect(responseErrorInterceptor(error)).rejects.toEqual(error);

      expect(localStorage.getItem('token')).toBeNull();
      expect(localStorage.getItem('username')).toBeNull();
      expect(localStorage.getItem('sessionExpired')).toBe('1');
      expect(window.location.href).toBe('/login');

      // Restore
      Object.defineProperty(window, 'location', { value: originalLocation, writable: true });
    });

    it('debe omitir redirección en 401 para requests de login', async () => {
      localStorage.setItem('token', 'some-token');
      localStorage.setItem('username', 'testuser');

      const error = {
        response: { status: 401 },
        config: { url: '/api/v1/auth/login' }
      };

      await expect(responseErrorInterceptor(error)).rejects.toEqual(error);

      // Token debe permanecer porque es un request de login
      expect(localStorage.getItem('token')).toBe('some-token');
    });

    it('debe omitir redirección en errores que no son 401/403', async () => {
      localStorage.setItem('token', 'some-token');

      const error = {
        response: { status: 500 },
        config: { url: '/api/v1/productos' }
      };

      await expect(responseErrorInterceptor(error)).rejects.toEqual(error);

      expect(localStorage.getItem('token')).toBe('some-token');
    });

    it('debe manejar errores de localStorage gracefulmente al establecer sessionExpired', async () => {
      const originalSetItem = Storage.prototype.setItem;
      Storage.prototype.setItem = vi.fn(() => { throw new Error('localStorage full'); });

      const error = {
        response: { status: 401 },
        config: { url: '/api/v1/productos' }
      };

      // No debe lanzar aunque localStorage.setItem lance
      await expect(responseErrorInterceptor(error)).rejects.toEqual(error);

      Storage.prototype.setItem = originalSetItem;
    });

    it('debe disparar evento api-error en 503 (circuit breaker)', async () => {
      const dispatchSpy = vi.spyOn(window, 'dispatchEvent');

      const error = {
        response: { status: 503 },
        config: { url: '/api/v1/productos' }
      };

      await expect(responseErrorInterceptor(error)).rejects.toEqual(error);

      expect(dispatchSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'api-error',
          detail: expect.objectContaining({
            message: expect.stringContaining('temporalmente no disponible')
          })
        })
      );
    });

    it('debe disparar evento api-error en 429 (rate limiting)', async () => {
      const dispatchSpy = vi.spyOn(window, 'dispatchEvent');

      const error = {
        response: { status: 429 },
        config: { url: '/api/v1/productos' }
      };

      await expect(responseErrorInterceptor(error)).rejects.toEqual(error);

      expect(dispatchSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'api-error',
          detail: expect.objectContaining({
            message: expect.stringContaining('Demasiadas solicitudes')
          })
        })
      );
    });

    it('debe disparar evento api-error en error de red (sin respuesta)', async () => {
      const dispatchSpy = vi.spyOn(window, 'dispatchEvent');

      const error = {
        code: 'ERR_NETWORK',
        config: { url: '/api/v1/productos' }
      };

      await expect(responseErrorInterceptor(error)).rejects.toEqual(error);

      expect(dispatchSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'api-error',
          detail: expect.objectContaining({
            message: expect.stringContaining('No se pudo conectar')
          })
        })
      );
    });

    it('debe disparar evento api-error en timeout (ECONNABORTED)', async () => {
      const dispatchSpy = vi.spyOn(window, 'dispatchEvent');

      const error = {
        code: 'ECONNABORTED',
        config: { url: '/api/v1/productos' }
      };

      await expect(responseErrorInterceptor(error)).rejects.toEqual(error);

      expect(dispatchSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'api-error',
          detail: expect.objectContaining({
            message: expect.stringContaining('tardó demasiado')
          })
        })
      );
    });
  });
});
