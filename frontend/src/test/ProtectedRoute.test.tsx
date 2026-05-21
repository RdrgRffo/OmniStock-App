import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import ProtectedRoute from 'router/ProtectedRoute';

// Mock de AuthContext
vi.mock('features/auth/context/AuthContext', () => ({
  useAuth: vi.fn(),
}));

// Mock de NotificationContext
vi.mock('features/notifications/context/NotificationContextStore', () => ({
  NotificationContext: {
    Provider: ({ children }: { children: React.ReactNode }) => children,
  },
}));

// Mock de NotificationContext
vi.mock('features/notifications/context/NotificationContext', () => ({
  NotificationProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

// Mock de BudgetContext
vi.mock('features/budget/context/BudgetContext', () => ({
  BudgetProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  useBudget: () => ({ itemCount: 0 }),
}));

// Mock de AppLayout
vi.mock('components/layout/AppLayout', () => ({
  default: ({ children }: { children: React.ReactNode }) => <div data-testid="app-layout">{children}</div>,
}));

import { useAuth } from 'features/auth/context/AuthContext';

describe('ProtectedRoute', () => {
  it('debe redirigir a /login cuando no está autenticado', () => {
    (useAuth as ReturnType<typeof vi.fn>).mockReturnValue({
      isAuthenticated: false,
    });

    render(
      <MemoryRouter initialEntries={['/protected']}>
        <Routes>
          <Route path="/login" element={<div>Login Page</div>} />
          <Route element={<ProtectedRoute />}>
            <Route path="/protected" element={<div>Protected Content</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Login Page')).toBeInTheDocument();
    expect(screen.queryByText('Protected Content')).toBeNull();
  });

  it('debe renderizar hijos cuando está autenticado', () => {
    (useAuth as ReturnType<typeof vi.fn>).mockReturnValue({
      isAuthenticated: true,
    });

    render(
      <MemoryRouter initialEntries={['/protected']}>
        <Routes>
          <Route path="/login" element={<div>Login Page</div>} />
          <Route element={<ProtectedRoute />}>
            <Route path="/protected" element={<div>Protected Content</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Protected Content')).toBeInTheDocument();
    expect(screen.queryByText('Login Page')).toBeNull();
  });
});
