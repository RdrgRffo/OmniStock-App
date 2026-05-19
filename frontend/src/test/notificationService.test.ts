import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  fetchNotifications,
  fetchNotificationCounts,
  markNotificationAsRead,
  markAllNotificationsAsRead,
} from 'features/notifications/services/notificationService';

const { mockGet, mockPut } = vi.hoisted(() => ({
  mockGet: vi.fn(),
  mockPut: vi.fn(),
}));

vi.mock('@/services/api', () => ({
  default: {
    get: mockGet,
    put: mockPut,
  },
}));

describe('notificationService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('fetchNotifications', () => {
    it('debe hacer GET a /notificaciones con parámetros tab y limit', async () => {
      mockGet.mockResolvedValue({
        data: {
          data: [
            { id: 1, type: 'GENERAL', title: 'Test', message: 'Test message', read: false, createdAt: '2025-01-01T00:00:00Z' },
          ],
        },
      });

      const result = await fetchNotifications('general', 50);

      expect(mockGet).toHaveBeenCalledWith('/notificaciones', {
        params: { tab: 'general', limit: 50 },
      });
      expect(result).toHaveLength(1);
      expect(result[0].title).toBe('Test');
    });

    it('debe devolver array vacío cuando no hay notificaciones', async () => {
      mockGet.mockResolvedValue({ data: { data: [] } });
      const result = await fetchNotifications('errores', 10);
      expect(result).toHaveLength(0);
    });
  });

  describe('fetchNotificationCounts', () => {
    it('debe hacer GET a /notificaciones/counts', async () => {
      const counts = { general: 5, errores: 2, ofertas: 0, nuevoProducto: 1 };
      mockGet.mockResolvedValue({ data: { data: counts } });

      const result = await fetchNotificationCounts();

      expect(mockGet).toHaveBeenCalledWith('/notificaciones/counts');
      expect(result.general).toBe(5);
      expect(result.errores).toBe(2);
    });
  });

  describe('markNotificationAsRead', () => {
    it('debe hacer PUT a /notificaciones/:id/read', async () => {
      mockPut.mockResolvedValue({});
      await markNotificationAsRead(1);
      expect(mockPut).toHaveBeenCalledWith('/notificaciones/1/read');
    });
  });

  describe('markAllNotificationsAsRead', () => {
    it('debe hacer PUT a /notificaciones/read-all con parámetro tab', async () => {
      mockPut.mockResolvedValue({});
      await markAllNotificationsAsRead('general');
      expect(mockPut).toHaveBeenCalledWith('/notificaciones/read-all', null, {
        params: { tab: 'general' },
      });
    });
  });
});
