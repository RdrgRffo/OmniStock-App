import { describe, it, expect, vi } from 'vitest';
import { render } from '@testing-library/react';
import { useBudget } from 'features/budget/context/useBudget';

describe('useBudget', () => {
  it('debe lanzar error cuando se usa fuera de BudgetProvider', () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    const TestComponent = () => {
      useBudget();
      return null;
    };

    expect(() => render(<TestComponent />)).toThrow(
      'useBudget must be used within a BudgetProvider'
    );

    consoleSpy.mockRestore();
  });
});
