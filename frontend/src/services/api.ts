import axios from 'axios';
import { API_BASE } from '../constants/api';

/**
 * Cliente Axios configurado para la API del backend.
 * Añade JWT en requests, reintentos automáticos, timeouts y
 * centraliza el manejo de errores de autenticación y servidor.
 */
const api = axios.create({
  baseURL: API_BASE,
  timeout: 30_000, // 30s de timeout global
});


// INTERCEPTOR DE PETICIONES (añade JWT automáticamente)
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});


// INTERCEPTOR DE RESPUESTA (manejo de expiración / acceso denegado y errores globales)
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    // Si no hay respuesta (network error, timeout, circuit breaker), mostrar alerta
    if (!error.response) {
      const message = error.code === 'ECONNABORTED'
        ? 'La solicitud tardó demasiado. El servidor podría estar sobrecargado.'
        : 'No se pudo conectar con el servidor. Verifica tu conexión.';

      console.warn(`[API RESILIENCE] ${message}`, error.config?.url);
      // Podemos mostrar una notificación global si existe el servicio
      window.dispatchEvent(new CustomEvent('api-error', { detail: { message, url: error.config?.url } }));
      return Promise.reject(error);
    }

    const { status } = error.response;
    const requestUrl = error.config?.url ?? '';
    const isLoginRequest = requestUrl.includes('/auth/login');

    // 503 = Service Unavailable (circuit breaker abierto en backend)
    if (status === 503) {
      console.warn(`[API RESILIENCE] Servicio temporalmente no disponible (503): ${requestUrl}`);
      window.dispatchEvent(new CustomEvent('api-error', {
        detail: { message: 'El servicio está temporalmente no disponible. Intenta de nuevo en unos momentos.', url: requestUrl }
      }));
      return Promise.reject(error);
    }

    // 429 = Too Many Requests (rate limiting)
    if (status === 429) {
      console.warn(`[API RESILIENCE] Rate limit excedido (429): ${requestUrl}`);
      window.dispatchEvent(new CustomEvent('api-error', {
        detail: { message: 'Demasiadas solicitudes. Espera un momento antes de intentar de nuevo.', url: requestUrl }
      }));
      return Promise.reject(error);
    }

    // 401/403 = Sesión expirada o acceso denegado
    if ([401, 403].includes(status) && !isLoginRequest) {
      localStorage.removeItem('token');
      localStorage.removeItem('username');
      try {
        localStorage.setItem('sessionExpired', '1');
      } catch {
        // Si localStorage falla, simplemente redirigimos sin mensaje persistente
      }
      window.location.href = '/login';
    }

    return Promise.reject(error);
  }
);

export default api;

