import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Badge } from 'components/badge/Badge';

describe('Badge', () => {
  it('debe renderizar con variante por defecto (neutral)', () => {
    render(<Badge>Test Badge</Badge>);
    const badge = screen.getByText('Test Badge');
    expect(badge).toBeInTheDocument();
    expect(badge.className).toContain('badge-neutral');
  });

  it('debe renderizar con variante success', () => {
    render(<Badge variant="success">Success</Badge>);
    const badge = screen.getByText('Success');
    expect(badge.className).toContain('badge-success');
  });

  it('debe renderizar con variante error', () => {
    render(<Badge variant="error">Error</Badge>);
    const badge = screen.getByText('Error');
    expect(badge.className).toContain('badge-error');
  });

  it('debe renderizar con variante warning', () => {
    render(<Badge variant="warning">Warning</Badge>);
    const badge = screen.getByText('Warning');
    expect(badge.className).toContain('badge-warning');
  });

  it('debe renderizar con variante info', () => {
    render(<Badge variant="info">Info</Badge>);
    const badge = screen.getByText('Info');
    expect(badge.className).toContain('badge-info');
  });

  it('debe aplicar className personalizado', () => {
    render(<Badge className="custom-class">Custom</Badge>);
    const badge = screen.getByText('Custom');
    expect(badge.className).toContain('custom-class');
  });
});
