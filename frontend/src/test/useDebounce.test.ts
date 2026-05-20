import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useDebounce } from '../hooks/useDebounce';

describe('useDebounce', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('debe devolver el valor inicial inmediatamente', () => {
    const { result } = renderHook(() => useDebounce('hello', 500));
    expect(result.current).toBe('hello');
  });

  it('debe aplicar debounce a los cambios de valor', () => {
    const { result, rerender } = renderHook(
      ({ value, delay }) => useDebounce(value, delay),
      { initialProps: { value: 'hello', delay: 500 } }
    );

    expect(result.current).toBe('hello');

    // Cambiar valor
    rerender({ value: 'world', delay: 500 });

    // El valor debe seguir siendo el anterior antes del timeout
    expect(result.current).toBe('hello');

    // Avanzar el tiempo
    act(() => {
      vi.advanceTimersByTime(500);
    });

    // Ahora debe estar actualizado
    expect(result.current).toBe('world');
  });

  it('debe cancelar el timeout anterior en cambios rápidos', () => {
    const { result, rerender } = renderHook(
      ({ value, delay }) => useDebounce(value, delay),
      { initialProps: { value: 'a', delay: 300 } }
    );

    rerender({ value: 'b', delay: 300 });
    act(() => { vi.advanceTimersByTime(100); });

    rerender({ value: 'c', delay: 300 });
    act(() => { vi.advanceTimersByTime(100); });

    rerender({ value: 'd', delay: 300 });

    // Debe seguir siendo 'a' porque no han pasado 300ms desde el último cambio
    expect(result.current).toBe('a');

    // Completar el timeout
    act(() => { vi.advanceTimersByTime(300); });
    expect(result.current).toBe('d');
  });

  it('debe funcionar con valores numéricos', () => {
    const { result, rerender } = renderHook(
      ({ value, delay }) => useDebounce(value, delay),
      { initialProps: { value: 0, delay: 200 } }
    );

    expect(result.current).toBe(0);

    rerender({ value: 42, delay: 200 });
    act(() => { vi.advanceTimersByTime(200); });

    expect(result.current).toBe(42);
  });

  it('debe funcionar con delay de 0', () => {
    const { result, rerender } = renderHook(
      ({ value, delay }) => useDebounce(value, delay),
      { initialProps: { value: 'first', delay: 0 } }
    );

    rerender({ value: 'second', delay: 0 });

    // Con delay 0, el timeout se dispara en el siguiente tick
    act(() => { vi.advanceTimersByTime(0); });
    expect(result.current).toBe('second');
  });

  it('debe limpiar el timeout al desmontar', () => {
    const clearTimeoutSpy = vi.spyOn(global, 'clearTimeout');
    const { unmount } = renderHook(() => useDebounce('test', 500));

    unmount();

    expect(clearTimeoutSpy).toHaveBeenCalled();
    clearTimeoutSpy.mockRestore();
  });
});
