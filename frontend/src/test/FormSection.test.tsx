import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { FormSection } from 'components/form/FormSection';

describe('FormSection', () => {
  it('debe renderizar título e hijos', () => {
    render(
      <FormSection title="User Information">
        <input placeholder="Name" />
      </FormSection>
    );
    expect(screen.getByText('User Information')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Name')).toBeInTheDocument();
  });

  it('debe tener clase section-title en h2', () => {
    render(
      <FormSection title="Details">
        <span>Content</span>
      </FormSection>
    );
    const title = screen.getByText('Details');
    expect(title.tagName).toBe('H2');
    expect(title.className).toContain('section-title');
  });
});
