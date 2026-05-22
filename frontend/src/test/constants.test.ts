import { describe, it, expect } from 'vitest';
import { API_BASE, API_ROUTES } from '@/constants/api';
import { GRAFANA_DASHBOARD_URL, PROMETHEUS_DASHBOARD_URL } from '@/constants/links';

describe('API Constants', () => {
  it('debe tener API_BASE definido', () => {
    expect(API_BASE).toBe('/api/v1');
  });

  describe('API_ROUTES', () => {
    it('debe tener rutas de autenticación', () => {
      expect(API_ROUTES.auth).toBe('/api/v1/auth');
      expect(API_ROUTES.login).toBe('/api/v1/auth/login');
      expect(API_ROUTES.register).toBe('/api/v1/auth/register');
      expect(API_ROUTES.validate).toBe('/api/v1/auth/validate');
      expect(API_ROUTES.refresh).toBe('/api/v1/auth/refresh');
    });

    it('debe tener rutas de productos', () => {
      expect(API_ROUTES.products).toBe('/api/v1/productos');
      expect(API_ROUTES.productSearch).toBe('/api/v1/productos/search');
    });

    it('debe tener rutas de proveedores', () => {
      expect(API_ROUTES.suppliers).toBe('/api/v1/proveedores');
      expect(API_ROUTES.supplierList).toBe('/api/v1/proveedores/list');
      expect(API_ROUTES.supplierMappings(1)).toBe('/api/v1/proveedores/1/mappings');
    });

    it('debe tener rutas de dashboard', () => {
      expect(API_ROUTES.dashboard).toBe('/api/v1/dashboard');
      expect(API_ROUTES.dashboardSummary).toBe('/api/v1/dashboard/summary');
      expect(API_ROUTES.dashboardExport).toBe('/api/v1/dashboard/export/csv');
    });

    it('debe tener rutas de historial de precios', () => {
      expect(API_ROUTES.priceHistory).toBe('/api/v1/historial-precios');
      expect(API_ROUTES.priceHistoryByProduct(1)).toBe('/api/v1/historial-precios/producto/1');
      expect(API_ROUTES.priceHistoryByProductAndSupplier(1, 2)).toBe('/api/v1/historial-precios/producto/1/proveedor/2');
      expect(API_ROUTES.priceHistoryAnalytics(1, 2)).toBe('/api/v1/historial-precios/producto/1/proveedor/2/analytics');
      expect(API_ROUTES.priceHistoryRegister).toBe('/api/v1/historial-precios/registrar');
    });

    it('debe tener rutas de administración de usuarios', () => {
      expect(API_ROUTES.adminUsers).toBe('/api/v1/admin/usuarios');
      expect(API_ROUTES.adminUserById(1)).toBe('/api/v1/admin/usuarios/1');
      expect(API_ROUTES.userProfile).toBe('/api/v1/usuarios/perfil');
    });

    it('debe tener rutas de mapeos', () => {
      expect(API_ROUTES.mappings).toBe('/api/v1/mappings');
      expect(API_ROUTES.mappingById(1)).toBe('/api/v1/mappings/1');
    });

    it('debe tener rutas de analytics', () => {
      expect(API_ROUTES.analytics).toBe('/api/v1/analytics');
      expect(API_ROUTES.analyticsPriceVariation).toBe('/api/v1/analytics/price-variation');
      expect(API_ROUTES.analyticsStockoutRates).toBe('/api/v1/analytics/stockout-rates');
      expect(API_ROUTES.analyticsPriceDispersion).toBe('/api/v1/analytics/price-dispersion');
      expect(API_ROUTES.analyticsMoq).toBe('/api/v1/analytics/moq');
      expect(API_ROUTES.analyticsConditionMix).toBe('/api/v1/analytics/condition-mix');
      expect(API_ROUTES.analyticsCostCoverage).toBe('/api/v1/analytics/cost-coverage');
      expect(API_ROUTES.analyticsStockVolatility).toBe('/api/v1/analytics/stock-volatility');
      expect(API_ROUTES.analyticsCatalogGrowth).toBe('/api/v1/analytics/catalog-growth');
      expect(API_ROUTES.analyticsPriceStability).toBe('/api/v1/analytics/price-stability');
      expect(API_ROUTES.dashboardSupplierHealth).toBe('/api/v1/dashboard/supplier-health');
    });

    it('debe tener rutas de presupuesto', () => {
      expect(API_ROUTES.budget).toBe('/api/v1/budget');
      expect(API_ROUTES.budgetSimulate).toBe('/api/v1/budget/simulate');
      expect(API_ROUTES.budgetById(1)).toBe('/api/v1/budget/1');
      expect(API_ROUTES.budgetStatus(1)).toBe('/api/v1/budget/1/status');
      expect(API_ROUTES.budgetExport(1)).toBe('/api/v1/budget/1/export');
    });
  });
});

describe('External Links Constants', () => {
  it('debe tener GRAFANA_DASHBOARD_URL como undefined cuando no hay env', () => {
    expect(GRAFANA_DASHBOARD_URL).toBeUndefined();
  });

  it('debe tener PROMETHEUS_DASHBOARD_URL como undefined cuando no hay env', () => {
    expect(PROMETHEUS_DASHBOARD_URL).toBeUndefined();
  });
});
