import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

// Mock react-router-dom to avoid nested Router issue (App already has BrowserRouter)
vi.mock('react-router-dom', () => ({
  BrowserRouter: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  Routes: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  Route: ({ element }: { element: React.ReactNode }) => element ?? null,
  Outlet: () => <div data-testid="outlet" />,
}));

// Mock de módulos pesados
vi.mock('features/auth/context/AuthContext', () => ({
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  useAuth: () => ({
    username: 'testuser',
    logout: vi.fn(),
    isAdmin: false,
    token: 'fake-token',
    roles: ['ROLE_CLIENTE'],
    login: vi.fn(),
    isAuthenticated: true,
  }),
}));

vi.mock('features/budget/context/BudgetContext', () => ({
  BudgetProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

vi.mock('features/budget/context/useBudget', () => ({
  useBudget: () => ({ itemCount: 0 }),
}));

vi.mock('features/notifications/components/NotificationBell', () => ({
  default: () => <div data-testid="notification-bell">Bell</div>,
}));

vi.mock('features/inventory/services/productService', () => ({
  syncAllProducts: vi.fn(),
}));

// Mock pages to avoid heavy imports
vi.mock('features/auth/pages/LoginPage', () => ({
  default: () => <div data-testid="login-page">Login Page</div>,
}));

vi.mock('features/inventory/pages/InventoryDashboardPage', () => ({
  default: () => <div data-testid="inventory-page">Inventory</div>,
}));

vi.mock('features/dashboard/pages/DashboardPage', () => ({
  default: () => <div data-testid="dashboard-page">Dashboard</div>,
}));

vi.mock('features/analytics/pages/AnalyticsPage', () => ({
  default: () => <div data-testid="analytics-page">Analytics</div>,
}));

vi.mock('features/dashboard/pages/OpportunitiesPage', () => ({
  default: () => <div data-testid="opportunities-page">Opportunities</div>,
}));

vi.mock('features/inventory/pages/ProductDetailPage', () => ({
  default: () => <div data-testid="product-detail-page">Product Detail</div>,
}));

vi.mock('pages/NotFoundPage', () => ({
  default: () => <div data-testid="not-found-page">404 - Página no encontrada</div>,
}));

vi.mock('router/ProtectedRoute', () => ({
  default: () => <div data-testid="protected-route"><div data-testid="outlet" /></div>,
}));

vi.mock('features/suppliers/pages/SupplierListPage', () => ({
  default: () => <div data-testid="supplier-list-page">Suppliers</div>,
}));

vi.mock('features/suppliers/pages/SupplierDetailPage', () => ({
  default: () => <div data-testid="supplier-detail-page">Supplier Detail</div>,
}));

vi.mock('features/suppliers/pages/SupplierFormPage', () => ({
  default: () => <div data-testid="supplier-form-page">Supplier Form</div>,
}));

vi.mock('features/suppliers/pages/SupplierDashboardPage', () => ({
  default: () => <div data-testid="supplier-dashboard-page">Supplier Dashboard</div>,
}));

vi.mock('features/users/pages/UserListPage', () => ({
  default: () => <div data-testid="user-list-page">Users</div>,
}));

vi.mock('features/users/pages/UserFormPage', () => ({
  default: () => <div data-testid="user-form-page">User Form</div>,
}));

vi.mock('features/priceHistory/pages/PriceHistoryPage', () => ({
  default: () => <div data-testid="price-history-page">Price History</div>,
}));

vi.mock('features/budget/pages/BudgetPage', () => ({
  default: () => <div data-testid="budget-page">Budget</div>,
}));

vi.mock('features/budget/pages/BudgetListPage', () => ({
  default: () => <div data-testid="budget-list-page">Budget List</div>,
}));

vi.mock('features/budget/pages/BudgetDetailPage', () => ({
  default: () => <div data-testid="budget-detail-page">Budget Detail</div>,
}));

vi.mock('features/notifications/pages/NotificationsPage', () => ({
  default: () => <div data-testid="notifications-page">Notifications</div>,
}));

vi.mock('components/ui/ErrorBoundary', () => ({
  default: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

import App from '../App';

describe('App', () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  const renderApp = () => {
    return render(
      <QueryClientProvider client={queryClient}>
        <App />
      </QueryClientProvider>
    );
  };

  beforeEach(() => {
    vi.clearAllMocks();
    window.history.pushState({}, '', '/');
  });

  it('debe renderizar sin errores', () => {
    const { container } = renderApp();
    expect(container).toBeTruthy();
  });

  it('debe renderizar la app con providers y rutas', () => {
    renderApp();
    // App renders QueryClientProvider > Router > AuthProvider > BudgetProvider > ErrorBoundary > AppRoutes
    // Since we mocked BrowserRouter, Routes, and Route, the app should render without errors
    expect(screen.getByTestId('outlet')).toBeTruthy();
  });
});
