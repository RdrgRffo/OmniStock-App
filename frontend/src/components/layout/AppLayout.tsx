import { ReactNode, useState } from 'react';
import { useAuth } from 'features/auth/context/AuthContext';
import { BudgetProvider } from 'features/budget/context/BudgetContext';
import { useBudget } from 'features/budget/context/useBudget';
import { Toaster, toast } from 'react-hot-toast';
import { Link, useLocation } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { syncAllProducts } from 'features/inventory/services/productService';
import NotificationBell from 'features/notifications/components/NotificationBell';
import { BarChart3, LineChart, LogOut, Package, RefreshCw, Calculator, Truck, Users, FileText, Menu, X, ChevronLeft } from 'lucide-react';
import './AppLayout.css';

const AppLayoutContent = ({ children }: { children: ReactNode }) => {
  const { username, logout, isAdmin } = useAuth();
  const { itemCount } = useBudget();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const syncMutation = useMutation({
    mutationFn: syncAllProducts,
    onSuccess: () => toast.success('Sincronización iniciada en segundo plano.'),
    onError: () => toast.error('No se pudo iniciar la sincronización.'),
  });

  const closeSidebar = () => setSidebarOpen(false);

  const isActive = (path: string) => location.pathname === path || location.pathname.startsWith(path + '/');

  return (
    <div className="app-layout">
      <Toaster
        position="top-right"
        toastOptions={{
          style: {
            borderRadius: 'var(--radius-md, 8px)',
            padding: '12px 16px',
            fontSize: '0.9rem',
          },
          success: {
            style: {
              background: 'var(--color-success, #059669)',
              color: '#ffffff',
            },
            iconTheme: {
              primary: '#ffffff',
              secondary: '#059669',
            },
          },
          error: {
            style: {
              background: 'var(--color-danger, #dc2626)',
              color: '#ffffff',
            },
            iconTheme: {
              primary: '#ffffff',
              secondary: '#dc2626',
            },
          },
        }}
      />

      {/* Header superior */}
      <header className="app-header">
        <div className="header-container">
          <button
            className="sidebar-toggle-btn"
            onClick={() => setSidebarOpen(!sidebarOpen)}
            aria-label={sidebarOpen ? 'Cerrar menú' : 'Abrir menú'}
            aria-expanded={sidebarOpen}
          >
            {sidebarOpen ? <X size={22} /> : <Menu size={22} />}
          </button>

          <Link to="/" className="header-logo-link" onClick={closeSidebar}>
            <span className="header-logo-text">OmniStock</span>
          </Link>

          <div className="header-actions">
            <NotificationBell />
            <Link
              to="/presupuesto"
              className="cart-link"
              title="Simulador de presupuesto"
            >
              <Calculator size={22} aria-hidden="true" />
              {itemCount > 0 && (
                <span className="cart-badge">{itemCount}</span>
              )}
            </Link>
            <span className="user-greeting">Hola, {username}</span>
          </div>
        </div>
      </header>

      <div className="app-body">
        {/* Overlay para móvil */}
        {sidebarOpen && <div className="sidebar-overlay" onClick={closeSidebar} />}

        {/* Sidebar lateral */}
        <aside className={`sidebar ${sidebarOpen ? 'sidebar--open' : ''}`}>
          <div className="sidebar-header">
            <span className="sidebar-title">Navegación</span>
            <button
              className="sidebar-close-btn"
              onClick={closeSidebar}
              aria-label="Cerrar menú"
            >
              <ChevronLeft size={20} />
            </button>
          </div>

          <nav className="sidebar-nav">
            <Link
              to="/products"
              className={`sidebar-link ${isActive('/products') || (isActive('/') && location.pathname === '/') ? 'sidebar-link--active' : ''}`}
              onClick={closeSidebar}
            >
              <Package size={18} aria-hidden="true" />
              <span>Productos</span>
            </Link>
            <Link
              to="/dashboard"
              className={`sidebar-link ${isActive('/dashboard') ? 'sidebar-link--active' : ''}`}
              onClick={closeSidebar}
            >
              <BarChart3 size={18} aria-hidden="true" />
              <span>Dashboard</span>
            </Link>
            <Link
              to="/analytics"
              className={`sidebar-link ${isActive('/analytics') ? 'sidebar-link--active' : ''}`}
              onClick={closeSidebar}
            >
              <LineChart size={18} aria-hidden="true" />
              <span>Analytics</span>
            </Link>
            <Link
              to="/presupuestos"
              className={`sidebar-link ${isActive('/presupuestos') ? 'sidebar-link--active' : ''}`}
              onClick={closeSidebar}
            >
              <FileText size={18} aria-hidden="true" />
              <span>Presupuestos</span>
            </Link>
            <Link
              to="/suppliers"
              className={`sidebar-link ${isActive('/suppliers') ? 'sidebar-link--active' : ''}`}
              onClick={closeSidebar}
            >
              <Truck size={18} aria-hidden="true" />
              <span>Proveedores</span>
            </Link>
            {isAdmin && (
              <Link
                to="/users"
                className={`sidebar-link ${isActive('/users') ? 'sidebar-link--active' : ''}`}
                onClick={closeSidebar}
              >
                <Users size={18} aria-hidden="true" />
                <span>Usuarios</span>
              </Link>
            )}
          </nav>

          <div className="sidebar-footer">
            {isAdmin && (
              <button
                onClick={() => {
                  syncMutation.mutate();
                }}
                disabled={syncMutation.isPending}
                className="sidebar-sync-btn"
              >
                <RefreshCw size={16} className={syncMutation.isPending ? 'spin-icon' : ''} aria-hidden="true" />
                {syncMutation.isPending ? 'Sincronizando...' : 'Sincronizar'}
              </button>
            )}
            <button onClick={logout} className="sidebar-logout-btn">
              <LogOut size={16} aria-hidden="true" />
              Cerrar Sesión
            </button>
          </div>
        </aside>

        {/* Contenido principal */}
        <main className="app-main">
          {children}
        </main>
      </div>
    </div>
  );
};

const AppLayout = ({ children }: { children: ReactNode }) => {
  return (
    <BudgetProvider>
      <AppLayoutContent>{children}</AppLayoutContent>
    </BudgetProvider>
  );
};

export default AppLayout;
