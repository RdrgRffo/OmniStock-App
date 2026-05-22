import { describe, it, expect } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useFieldErrors } from '../hooks/useFieldErrors';

type TestFields = 'name' | 'email' | 'password';

describe('useFieldErrors', () => {
  it('debe comenzar sin errores', () => {
    const { result } = renderHook(() => useFieldErrors<TestFields>());
    expect(result.current.errors).toEqual({});
  });

  it('debe establecer un error de campo', () => {
    const { result } = renderHook(() => useFieldErrors<TestFields>());

    act(() => {
      result.current.setFieldError('name', 'El nombre es requerido');
    });

    expect(result.current.errors.name).toBe('El nombre es requerido');
  });

  it('debe limpiar un error de campo específico', () => {
    const { result } = renderHook(() => useFieldErrors<TestFields>());

    act(() => {
      result.current.setFieldError('name', 'El nombre es requerido');
      result.current.setFieldError('email', 'Email inválido');
    });

    expect(Object.keys(result.current.errors)).toHaveLength(2);

    act(() => {
      result.current.clearFieldError('name');
    });

    expect(result.current.errors.name).toBeUndefined();
    expect(result.current.errors.email).toBe('Email inválido');
  });

  it('debe limpiar todos los errores', () => {
    const { result } = renderHook(() => useFieldErrors<TestFields>());

    act(() => {
      result.current.setFieldError('name', 'Error 1');
      result.current.setFieldError('email', 'Error 2');
      result.current.setFieldError('password', 'Error 3');
    });

    expect(Object.keys(result.current.errors)).toHaveLength(3);

    act(() => {
      result.current.clearAllErrors();
    });

    expect(result.current.errors).toEqual({});
  });

  it('debe devolver atributos aria correctos mediante bindField', () => {
    const { result } = renderHook(() => useFieldErrors<TestFields>());

    // Sin error
    let bindings = result.current.bindField('name');
    expect(bindings['aria-invalid']).toBeUndefined();
    expect(bindings['aria-describedby']).toBeUndefined();

    // Con error
    act(() => {
      result.current.setFieldError('name', 'Error message');
    });

    bindings = result.current.bindField('name');
    expect(bindings['aria-invalid']).toBe(true);
    expect(bindings['aria-describedby']).toBe('error-name');

    // Después de limpiar error
    act(() => {
      result.current.clearFieldError('name');
    });

    bindings = result.current.bindField('name');
    expect(bindings['aria-invalid']).toBeUndefined();
    expect(bindings['aria-describedby']).toBeUndefined();
  });

  it('debe manejar múltiples campos de forma independiente', () => {
    const { result } = renderHook(() => useFieldErrors<TestFields>());

    act(() => {
      result.current.setFieldError('name', 'Name error');
      result.current.setFieldError('email', 'Email error');
    });

    expect(result.current.errors.name).toBe('Name error');
    expect(result.current.errors.email).toBe('Email error');
    expect(result.current.errors.password).toBeUndefined();

    act(() => {
      result.current.clearFieldError('email');
    });

    expect(result.current.errors.name).toBe('Name error');
    expect(result.current.errors.email).toBeUndefined();
  });

  it('debe actualizar un mensaje de error existente', () => {
    const { result } = renderHook(() => useFieldErrors<TestFields>());

    act(() => {
      result.current.setFieldError('name', 'First message');
    });
    expect(result.current.errors.name).toBe('First message');

    act(() => {
      result.current.setFieldError('name', 'Updated message');
    });
    expect(result.current.errors.name).toBe('Updated message');
  });
});
