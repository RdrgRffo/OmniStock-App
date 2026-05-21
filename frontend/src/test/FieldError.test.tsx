import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { FieldError } from 'components/form/FieldError';

describe('FieldError', () => {
  it('debe renderizar mensaje de error cuando se proporciona', () => {
    render(<FieldError field="email" message="Email is required" />);
    const error = screen.getByText('Email is required');
    expect(error).toBeInTheDocument();
    expect(error.getAttribute('role')).toBe('alert');
    expect(error.getAttribute('id')).toBe('error-email');
  });

  it('debe ocultarse cuando el mensaje está vacío', () => {
    const { container } = render(<FieldError field="email" message="" />);
    expect(container.querySelector('.field-error')).toBeNull();
  });

  it('debe ocultarse cuando el mensaje es undefined', () => {
    const { container } = render(<FieldError field="email" />);
    expect(container.querySelector('.field-error')).toBeNull();
  });
});
