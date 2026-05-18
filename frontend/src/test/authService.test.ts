import { describe, it, expect, vi, beforeEach } from 'vitest';
import { login } from 'features/auth/services/authService';

const { mockPost } = vi.hoisted(() => ({
  mockPost: vi.fn(),
}));

vi.mock('@/services/api', () => ({
  default: {
    post: mockPost,
  },
}));

describe('authService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('login', () => {
    it('debe hacer POST a /auth/login y devolver AuthResponse', async () => {
      const mockResponse = {
        data: {
          data: {
            token: 'jwt-token',
            username: 'testuser',
            roles: ['ROLE_CLIENTE'],
          },
        },
      };

      mockPost.mockResolvedValue(mockResponse);

      const credentials = { username: 'testuser', password: 'password123' };
      const result = await login(credentials);

      expect(mockPost).toHaveBeenCalledWith('/auth/login', credentials);
      expect(result.token).toBe('jwt-token');
      expect(result.username).toBe('testuser');
    });

    it('debe lanzar error cuando la API devuelve error', async () => {
      mockPost.mockRejectedValue(new Error('Invalid credentials'));

      const credentials = { username: 'testuser', password: 'wrong' };
      await expect(login(credentials)).rejects.toThrow('Invalid credentials');
    });
  });
});
