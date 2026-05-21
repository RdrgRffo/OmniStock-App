import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { FormActions } from 'components/form/FormActions';

describe('FormActions', () => {
  it('debe renderizar hijos', () => {
    render(
      <FormActions>
        <button>Save</button>
        <button>Cancel</button>
      </FormActions>
    );
    expect(screen.getByText('Save')).toBeInTheDocument();
    expect(screen.getByText('Cancel')).toBeInTheDocument();
  });

  it('debe tener clase form-actions', () => {
    const { container } = render(
      <FormActions>
        <button>Submit</button>
      </FormActions>
    );
    expect(container.firstChild).toHaveClass('form-actions');
  });
});
