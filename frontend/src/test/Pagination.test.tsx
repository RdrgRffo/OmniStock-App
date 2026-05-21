import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import Pagination from 'features/inventory/components/Pagination';

describe('Pagination', () => {
  it('debe mostrar información de la página actual', () => {
    render(<Pagination page={0} totalPages={5} onPageChange={() => {}} />);
    expect(screen.getByText('Página 1 de 5')).toBeInTheDocument();
  });

  it('debe deshabilitar botón anterior en primera página', () => {
    render(<Pagination page={0} totalPages={5} onPageChange={() => {}} />);
    expect(screen.getByText('Anterior')).toBeDisabled();
  });

  it('debe deshabilitar botón siguiente en última página', () => {
    render(<Pagination page={4} totalPages={5} onPageChange={() => {}} />);
    expect(screen.getByText('Siguiente')).toBeDisabled();
  });

  it('debe llamar a onPageChange con página anterior', () => {
    const onPageChange = vi.fn();
    render(<Pagination page={2} totalPages={5} onPageChange={onPageChange} />);
    fireEvent.click(screen.getByText('Anterior'));
    expect(onPageChange).toHaveBeenCalledWith(1);
  });

  it('debe llamar a onPageChange con página siguiente', () => {
    const onPageChange = vi.fn();
    render(<Pagination page={2} totalPages={5} onPageChange={onPageChange} />);
    fireEvent.click(screen.getByText('Siguiente'));
    expect(onPageChange).toHaveBeenCalledWith(3);
  });

  it('debe llamar a onPageChange con número de página específico', () => {
    const onPageChange = vi.fn();
    render(<Pagination page={0} totalPages={5} onPageChange={onPageChange} />);
    fireEvent.click(screen.getByText('3'));
    expect(onPageChange).toHaveBeenCalledWith(2);
  });

  it('debe resaltar la página activa', () => {
    render(<Pagination page={2} totalPages={5} onPageChange={() => {}} />);
    const activeButton = screen.getByText('3');
    expect(activeButton.className).toContain('active');
  });

  it('debe mostrar elipsis para muchos páginas', () => {
    render(<Pagination page={10} totalPages={20} onPageChange={() => {}} />);
    const ellipsis = screen.getAllByText('…');
    expect(ellipsis.length).toBeGreaterThanOrEqual(1);
  });

  it('debe mostrar botones de primera y última página con elipsis', () => {
    render(<Pagination page={10} totalPages={20} onPageChange={() => {}} />);
    expect(screen.getByText('1')).toBeInTheDocument();
    expect(screen.getByText('20')).toBeInTheDocument();
  });
});
