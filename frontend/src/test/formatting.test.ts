import { describe, it, expect } from 'vitest';
import { formatCurrency } from '../utils/formatting';

describe('formatCurrency', () => {
  it('debe formatear un número positivo como moneda EUR', () => {
    const result = formatCurrency(1234.56);
    expect(result).toContain('56');
    expect(result).toContain('€');
    expect(result).toMatch(/[\d.,\s]+/);
  });

  it('debe formatear cero', () => {
    const result = formatCurrency(0);
    expect(result).toContain('0');
    expect(result).toContain('€');
  });

  it('debe formatear un número negativo', () => {
    const result = formatCurrency(-50.5);
    expect(result).toContain('50');
    expect(result).toContain('€');
  });

  it('debe devolver N/A para null', () => {
    expect(formatCurrency(null)).toBe('N/A');
  });

  it('debe devolver N/A para undefined', () => {
    expect(formatCurrency(undefined)).toBe('N/A');
  });

  it('debe formatear valores enteros', () => {
    const result = formatCurrency(100);
    expect(result).toContain('100');
    expect(result).toContain('€');
  });

  it('debe formatear números grandes', () => {
    const result = formatCurrency(9999999.99);
    expect(result).toContain('€');
    expect(result.length).toBeGreaterThan(5);
  });
});
