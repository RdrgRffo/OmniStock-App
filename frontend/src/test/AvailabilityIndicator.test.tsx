import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import AvailabilityIndicator from 'features/inventory/components/AvailabilityIndicator';

describe('AvailabilityIndicator', () => {
  it('debe renderizar "Disponible" para estado DISPONIBLE', () => {
    render(<AvailabilityIndicator status="DISPONIBLE" />);
    expect(screen.getByText('Disponible')).toBeInTheDocument();
  });

  it('debe renderizar "Bajo Stock" para estado BAJO_STOCK', () => {
    render(<AvailabilityIndicator status="BAJO_STOCK" />);
    expect(screen.getByText('Bajo Stock')).toBeInTheDocument();
  });

  it('debe renderizar "Agotado" para estado SIN_STOCK', () => {
    render(<AvailabilityIndicator status="SIN_STOCK" />);
    expect(screen.getByText('Agotado')).toBeInTheDocument();
  });

  it('debe renderizar "Sin Info" para estado desconocido', () => {
    render(<AvailabilityIndicator status="UNKNOWN" />);
    expect(screen.getByText('Sin Info')).toBeInTheDocument();
  });

  it('debe renderizar "Sin Info" para estado vacío', () => {
    render(<AvailabilityIndicator status="" />);
    expect(screen.getByText('Sin Info')).toBeInTheDocument();
  });
});
