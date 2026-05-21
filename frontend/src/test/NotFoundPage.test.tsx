import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import NotFoundPage from 'pages/NotFoundPage';

describe('NotFoundPage', () => {
  it('debe renderizar título 404', () => {
    render(
      <BrowserRouter>
        <NotFoundPage />
      </BrowserRouter>
    );
    expect(screen.getByText('404')).toBeInTheDocument();
  });

  it('debe renderizar mensaje "Página no encontrada"', () => {
    render(
      <BrowserRouter>
        <NotFoundPage />
      </BrowserRouter>
    );
    expect(screen.getByText('Página no encontrada')).toBeInTheDocument();
  });

  it('debe renderizar enlace de vuelta al inicio', () => {
    render(
      <BrowserRouter>
        <NotFoundPage />
      </BrowserRouter>
    );
    const link = screen.getByText('Volver al inicio');
    expect(link).toBeInTheDocument();
    expect(link.getAttribute('href')).toBe('/');
  });
});
