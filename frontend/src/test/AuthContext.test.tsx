import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { AuthProvider, useAuth } from 'features/auth/context/AuthContext';
import { ReactNode } from 'react';

// Mock de jwt-decode
const mockJwtDecode = vi.fn();
vi.mock('jwt-decode', () => ({
  jwtDecode: (...args: unknown[]) => mockJwtDecode(...args),
}));

const TestLoginComponent = () => {
  const { login, logout, isAuthenticated, isAdmin, username, token } = useAuth();
  return (
    <div>
      <div data-testid="is-authenticated">{isAuthenticated ? 'true' : 'false'}</div>
      <div data-testid="is-admin">{isAdmin ? 'true' : 'false'}</div>
      <div data-testid="username">{username ?? 'null'}</div>
      <div data-testid="token">{token ?? 'null'}</div>
      <button
        data-testid="login-btn"
        onClick={() =>
          login({ token: 'valid-token', username: 'testuser' })
        }
      >
        Login
      </button>
      <button
        data-testid="logout-btn"
        onClick={() => logout()}
      >
        Logout
      </button>
    </div>
  );
};

const renderWithProvider = (ui: ReactNode) => {
  return render(<AuthProvider>{ui}</AuthProvider>);
};

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  afterEach(() => {
    localStorage.clear();
  });

  describe('Estado inicial', () => {
    it('debe iniciar sin autenticación cuando no hay token en localStorage', () => {
      renderWithProvider(<TestLoginComponent />);
      expect(screen.getByTestId('is-authenticated').textContent).toBe('false');
      expect(screen.getByTestId('username').textContent).toBe('null');
      expect(screen.getByTestId('token').textContent).toBe('null');
      expect(screen.getByTestId('is-admin').textContent).toBe('false');
    });

    it('debe restaurar la sesión desde localStorage con un token válido', () => {
      localStorage.setItem('token', 'valid-token');
      localStorage.setItem('username', 'testuser');
      mockJwtDecode.mockReturnValue({
        sub: 'testuser',
        iat: Math.floor(Date.now() / 1000) - 100,
        exp: Math.floor(Date.now() / 1000) + 86400,
        roles: ['ROLE_ADMIN'],
      });

      renderWithProvider(<TestLoginComponent />);
      expect(screen.getByTestId('is-authenticated').textContent).toBe('true');
      expect(screen.getByTestId('username').textContent).toBe('testuser');
      expect(screen.getByTestId('is-admin').textContent).toBe('true');
    });

    it('debe limpiar el token expirado al iniciar', () => {
      localStorage.setItem('token', 'expired-token');
      localStorage.setItem('username', 'testuser');
      mockJwtDecode.mockReturnValue({
        sub: 'testuser',
        iat: Math.floor(Date.now() / 1000) - 7200,
        exp: Math.floor(Date.now() / 1000) - 3600,
        roles: ['ROLE_CLIENTE'],
      });

      renderWithProvider(<TestLoginComponent />);
      expect(screen.getByTestId('is-authenticated').textContent).toBe('false');
      expect(localStorage.getItem('token')).toBeNull();
      expect(localStorage.getItem('username')).toBeNull();
    });

    it('debe manejar un token inválido al iniciar', () => {
      localStorage.setItem('token', 'invalid-token');
      localStorage.setItem('username', 'testuser');
      mockJwtDecode.mockImplementation(() => {
        throw new Error('Invalid token');
      });

      renderWithProvider(<TestLoginComponent />);
      expect(screen.getByTestId('is-authenticated').textContent).toBe('false');
      expect(localStorage.getItem('token')).toBeNull();
    });
  });

  describe('Inicio de sesión', () => {
    it('debe autenticar al usuario con un token válido', async () => {
      mockJwtDecode.mockReturnValue({
        sub: 'testuser',
        iat: Math.floor(Date.now() / 1000) - 100,
        exp: Math.floor(Date.now() / 1000) + 3600,
        roles: ['ROLE_CLIENTE'],
      });

      renderWithProvider(<TestLoginComponent />);

      await act(async () => {
        fireEvent.click(screen.getByTestId('login-btn'));
      });

      expect(screen.getByTestId('is-authenticated').textContent).toBe('true');
      expect(screen.getByTestId('username').textContent).toBe('testuser');
      expect(screen.getByTestId('is-admin').textContent).toBe('false');
      expect(localStorage.getItem('token')).toBe('valid-token');
    });

    it('debe establecer isAdmin cuando el usuario tiene ROLE_ADMIN', async () => {
      mockJwtDecode.mockReturnValue({
        sub: 'admin',
        iat: Math.floor(Date.now() / 1000) - 100,
        exp: Math.floor(Date.now() / 1000) + 3600,
        roles: ['ROLE_ADMIN'],
      });

      const AdminTestComponent = () => {
        const { login, isAdmin } = useAuth();
        return (
          <div>
            <div data-testid="is-admin">{isAdmin ? 'true' : 'false'}</div>
            <button data-testid="login-btn" onClick={() => login({ token: 'admin-token', username: 'admin' })}>
              Login
            </button>
          </div>
        );
      };

      renderWithProvider(<AdminTestComponent />);

      await act(async () => {
        fireEvent.click(screen.getByTestId('login-btn'));
      });

      expect(screen.getByTestId('is-admin').textContent).toBe('true');
    });

    it('debe rechazar un token expirado al iniciar sesión', async () => {
      mockJwtDecode.mockReturnValue({
        sub: 'testuser',
        iat: Math.floor(Date.now() / 1000) - 7200,
        exp: Math.floor(Date.now() / 1000) - 1,
        roles: ['ROLE_CLIENTE'],
      });

      renderWithProvider(<TestLoginComponent />);

      await act(async () => {
        fireEvent.click(screen.getByTestId('login-btn'));
      });

      expect(screen.getByTestId('is-authenticated').textContent).toBe('false');
      expect(localStorage.getItem('token')).toBeNull();
    });

    it('debe manejar errores de decodificación al iniciar sesión de forma graceful', async () => {
      mockJwtDecode.mockImplementation(() => {
        throw new Error('Decode failed');
      });

      renderWithProvider(<TestLoginComponent />);

      await act(async () => {
        fireEvent.click(screen.getByTestId('login-btn'));
      });

      expect(screen.getByTestId('is-authenticated').textContent).toBe('false');
      expect(localStorage.getItem('token')).toBeNull();
    });
  });

  describe('Cierre de sesión', () => {
    it('debe limpiar el estado de autenticación', async () => {
      mockJwtDecode.mockReturnValue({
        sub: 'testuser',
        iat: Math.floor(Date.now() / 1000) - 100,
        exp: Math.floor(Date.now() / 1000) + 3600,
        roles: ['ROLE_CLIENTE'],
      });

      renderWithProvider(<TestLoginComponent />);

      await act(async () => {
        fireEvent.click(screen.getByTestId('login-btn'));
      });

      expect(screen.getByTestId('is-authenticated').textContent).toBe('true');

      await act(async () => {
        fireEvent.click(screen.getByTestId('logout-btn'));
      });

      expect(screen.getByTestId('is-authenticated').textContent).toBe('false');
      expect(screen.getByTestId('username').textContent).toBe('null');
      expect(localStorage.getItem('token')).toBeNull();
      expect(localStorage.getItem('username')).toBeNull();
    });
  });

  describe('Hook useAuth', () => {
    it('debe lanzar error cuando se usa fuera del proveedor', () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      expect(() => render(<TestLoginComponent />)).toThrow(
        'useAuth must be used within an AuthContextProvider'
      );
      consoleSpy.mockRestore();
    });
  });
});
