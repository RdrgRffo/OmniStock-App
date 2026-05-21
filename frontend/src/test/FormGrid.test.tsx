import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { FormGrid } from 'components/form/FormGrid';

describe('FormGrid', () => {
  it('debe renderizar hijos', () => {
    render(
      <FormGrid>
        <input placeholder="Field 1" />
        <input placeholder="Field 2" />
      </FormGrid>
    );
    expect(screen.getByPlaceholderText('Field 1')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Field 2')).toBeInTheDocument();
  });

  it('debe tener clase form-grid', () => {
    const { container } = render(
      <FormGrid>
        <span>Content</span>
      </FormGrid>
    );
    expect(container.firstChild).toHaveClass('form-grid');
  });
});
