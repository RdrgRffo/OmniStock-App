import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { BudgetProvider } from 'features/budget/context/BudgetContext';
import { useBudget } from 'features/budget/context/useBudget';
import type { AddBudgetItem } from 'features/budget/context/BudgetContext';
import { ReactNode } from 'react';

// Mock de AuthContext
vi.mock('features/auth/context/AuthContext', () => ({
  useAuth: () => ({ username: 'testuser' }),
}));

// Mock de api
vi.mock('@/services/api', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

// Componente helper para testear el contexto
const TestComponent = () => {
  const {
    items,
    addItem,
    removeItem,
    updateItemQuantity,
    toggleItemSelection,
    totalPrice,
    itemCount,
    clearItems,
  } = useBudget();

  return (
    <div>
      <div data-testid="item-count">{itemCount}</div>
      <div data-testid="total-price">{totalPrice.toFixed(2)}</div>
      <div data-testid="items-length">{items.length}</div>
      <ul data-testid="items-list">
        {items.map((item) => (
          <li key={item.id} data-testid={`item-${item.id}`}>
            <span data-testid={`name-${item.id}`}>{item.productName}</span>
            <span data-testid={`qty-${item.id}`}>{item.quantity}</span>
            <span data-testid={`selected-${item.id}`}>{item.selected ? 'yes' : 'no'}</span>
          </li>
        ))}
      </ul>
      <button
        data-testid="add-item"
        onClick={() => {
          const item: AddBudgetItem = {
            productId: 1,
            supplierId: 1,
            productName: 'Test Product',
            supplierName: 'Test Supplier',
            unitPrice: 100,
            quantity: 2,
            stock: 10,
          };
          addItem(item);
        }}
      >
        Add Item
      </button>
      <button
        data-testid="add-item-no-stock"
        onClick={() => {
          const item: AddBudgetItem = {
            productId: 2,
            supplierId: 2,
            productName: 'No Stock Product',
            supplierName: 'Supplier 2',
            unitPrice: 50,
            quantity: 1,
            stock: 0,
          };
          addItem(item);
        }}
      >
        Add No Stock
      </button>
      <button
        data-testid="remove-item"
        onClick={() => removeItem('1-1')}
      >
        Remove Item
      </button>
      <button
        data-testid="update-qty"
        onClick={() => updateItemQuantity('1-1', 5)}
      >
        Update Qty
      </button>
      <button
        data-testid="toggle-select"
        onClick={() => toggleItemSelection('1-1')}
      >
        Toggle Select
      </button>
      <button
        data-testid="clear-items"
        onClick={() => clearItems()}
      >
        Clear Items
      </button>
    </div>
  );
};

const renderWithProvider = (ui: ReactNode) => {
  return render(<BudgetProvider>{ui}</BudgetProvider>);
};

describe('BudgetContext', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('debe iniciar con la lista de artículos vacía', () => {
    renderWithProvider(<TestComponent />);
    expect(screen.getByTestId('item-count').textContent).toBe('0');
    expect(screen.getByTestId('total-price').textContent).toBe('0.00');
    expect(screen.getByTestId('items-length').textContent).toBe('0');
  });

  it('debe agregar un artículo al presupuesto', () => {
    renderWithProvider(<TestComponent />);
    fireEvent.click(screen.getByTestId('add-item'));
    expect(screen.getByTestId('item-count').textContent).toBe('2');
    expect(screen.getByTestId('items-length').textContent).toBe('1');
    expect(screen.getByTestId('name-1-1').textContent).toBe('Test Product');
    expect(screen.getByTestId('qty-1-1').textContent).toBe('2');
  });

  it('no debe agregar un artículo con stock cero', () => {
    renderWithProvider(<TestComponent />);
    fireEvent.click(screen.getByTestId('add-item-no-stock'));
    expect(screen.getByTestId('items-length').textContent).toBe('0');
  });

  it('debe actualizar la cantidad de un artículo existente', () => {
    renderWithProvider(<TestComponent />);
    fireEvent.click(screen.getByTestId('add-item'));
    fireEvent.click(screen.getByTestId('update-qty'));
    expect(screen.getByTestId('qty-1-1').textContent).toBe('5');
  });

  it('debe eliminar un artículo', () => {
    renderWithProvider(<TestComponent />);
    fireEvent.click(screen.getByTestId('add-item'));
    expect(screen.getByTestId('items-length').textContent).toBe('1');
    fireEvent.click(screen.getByTestId('remove-item'));
    expect(screen.getByTestId('items-length').textContent).toBe('0');
  });

  it('debe alternar la selección de un artículo', () => {
    renderWithProvider(<TestComponent />);
    fireEvent.click(screen.getByTestId('add-item'));
    expect(screen.getByTestId('selected-1-1').textContent).toBe('yes');
    fireEvent.click(screen.getByTestId('toggle-select'));
    expect(screen.getByTestId('selected-1-1').textContent).toBe('no');
    fireEvent.click(screen.getByTestId('toggle-select'));
    expect(screen.getByTestId('selected-1-1').textContent).toBe('yes');
  });

  it('debe calcular el precio total correctamente', () => {
    renderWithProvider(<TestComponent />);
    fireEvent.click(screen.getByTestId('add-item'));
    // unitPrice=100, qty=2 => total=200
    expect(screen.getByTestId('total-price').textContent).toBe('200.00');
  });

  it('debe limpiar todos los artículos', () => {
    renderWithProvider(<TestComponent />);
    fireEvent.click(screen.getByTestId('add-item'));
    expect(screen.getByTestId('items-length').textContent).toBe('1');
    fireEvent.click(screen.getByTestId('clear-items'));
    expect(screen.getByTestId('items-length').textContent).toBe('0');
    expect(screen.getByTestId('total-price').textContent).toBe('0.00');
  });

  it('debe persistir los artículos en localStorage', () => {
    renderWithProvider(<TestComponent />);
    fireEvent.click(screen.getByTestId('add-item'));
    const stored = localStorage.getItem('inventory-budget-items:testuser');
    expect(stored).not.toBeNull();
    const parsed = JSON.parse(stored!);
    expect(parsed).toHaveLength(1);
    expect(parsed[0].productName).toBe('Test Product');
  });

  it('debe lanzar error cuando useBudget se usa fuera del proveedor', () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(() => render(<TestComponent />)).toThrow(
      'useBudget must be used within a BudgetProvider'
    );
    consoleSpy.mockRestore();
  });
});
