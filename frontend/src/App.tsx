import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from 'features/auth/context/AuthContext';
import LoginPage from 'features/auth/pages/LoginPage';
import InventoryDashboardPage from 'features/inventory/pages/InventoryDashboardPage';
import DashboardPage from 'features/dashboard/pages/DashboardPage';
import AnalyticsPage from 'features/analytics/pages/AnalyticsPage';
import ProductDetailPage from 'features/inventory/pages/ProductDetailPage';
import NotFoundPage from 'pages/NotFoundPage';
import ProtectedRoute from 'router/ProtectedRoute';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import SupplierListPage from 'features/suppliers/pages/SupplierListPage';
import SupplierDetailPage from 'features/suppliers/pages/SupplierDetailPage';
import SupplierFormPage from 'features/suppliers/pages/SupplierFormPage';
import SupplierDashboardPage from 'features/suppliers/pages/SupplierDashboardPage';
import UserListPage from 'features/users/pages/UserListPage';
import UserFormPage from 'features/users/pages/UserFormPage';
import PriceHistoryPage from 'features/priceHistory/pages/PriceHistoryPage';
import BudgetPage from 'features/budget/pages/BudgetPage';
import BudgetListPage from 'features/budget/pages/BudgetListPage';
import BudgetDetailPage from 'features/budget/pages/BudgetDetailPage';
import { BudgetProvider } from 'features/budget/context/BudgetContext';
import { NotificationProvider } from 'features/notifications/context/NotificationContext';
import ErrorBoundary from 'components/ui/ErrorBoundary';
import NotificationsPage from 'features/notifications/pages/NotificationsPage';

const queryClient = new QueryClient();

const AppRoutes = () => {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/" element={<InventoryDashboardPage />} />
        <Route path="/products" element={<InventoryDashboardPage />} />
        <Route path="/product/:id" element={<ProductDetailPage />} />
        <Route path="/analytics" element={<AnalyticsPage />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/suppliers/:id/dashboard" element={<SupplierDashboardPage />} />
        <Route path="/suppliers" element={<SupplierListPage />} />
        <Route path="/suppliers/new" element={<SupplierFormPage />} />
        <Route path="/suppliers/:id" element={<SupplierDetailPage />} />
        <Route path="/suppliers/:id/edit" element={<SupplierFormPage />} />
        <Route path="/users" element={<UserListPage />} />
        <Route path="/users/new" element={<UserFormPage />} />
        <Route path="/users/edit/:id" element={<UserFormPage />} />
        <Route path="/historial-precios/:productoId/:proveedorId" element={<PriceHistoryPage />} />
        <Route path="/presupuesto" element={<BudgetPage />} />
        <Route path="/presupuesto/nuevo" element={<BudgetPage />} />
        <Route path="/presupuestos" element={<BudgetListPage />} />
        <Route path="/presupuesto/:id" element={<BudgetDetailPage />} />
        <Route path="/notificaciones" element={<NotificationsPage />} />
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
};

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <Router>
        <AuthProvider>
          <BudgetProvider>
            <NotificationProvider>
              <ErrorBoundary fallbackMessage="Ocurrió un error en la aplicación.">
                <AppRoutes />
              </ErrorBoundary>
            </NotificationProvider>
          </BudgetProvider>
        </AuthProvider>
      </Router>
    </QueryClientProvider>
  );
}

export default App;
