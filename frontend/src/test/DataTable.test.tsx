import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { DataTable, Column } from 'components/ui/DataTable';

interface TestItem {
  id: number;
  name: string;
  value: number;
}

describe('DataTable', () => {
  const columns: Column<TestItem>[] = [
    { header: 'ID', accessorKey: 'id' },
    { header: 'Name', accessorKey: 'name' },
    { header: 'Value', cell: (item) => `$${item.value}` },
  ];

  const data: TestItem[] = [
    { id: 1, name: 'Item 1', value: 100 },
    { id: 2, name: 'Item 2', value: 200 },
  ];

  it('debe mostrar mensaje de carga cuando isLoading es true', () => {
    render(
      <DataTable
        columns={columns}
        data={[]}
        isLoading={true}
        keyExtractor={(item) => item.id}
      />
    );
    expect(screen.getByText('Cargando datos...')).toBeInTheDocument();
  });

  it('debe mostrar mensaje de carga personalizado', () => {
    render(
      <DataTable
        columns={columns}
        data={[]}
        isLoading={true}
        loadingMessage="Loading..."
        keyExtractor={(item) => item.id}
      />
    );
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  it('debe mostrar mensaje de error cuando isError es true', () => {
    render(
      <DataTable
        columns={columns}
        data={[]}
        isError={true}
        keyExtractor={(item) => item.id}
      />
    );
    expect(screen.getByText('Error al cargar los datos.')).toBeInTheDocument();
  });

  it('debe mostrar mensaje de error personalizado', () => {
    render(
      <DataTable
        columns={columns}
        data={[]}
        isError={true}
        errorMessage="Custom error"
        keyExtractor={(item) => item.id}
      />
    );
    expect(screen.getByText('Custom error')).toBeInTheDocument();
  });

  it('debe mostrar mensaje vacío cuando no hay datos', () => {
    render(
      <DataTable
        columns={columns}
        data={[]}
        keyExtractor={(item) => item.id}
      />
    );
    expect(screen.getByText('No hay datos disponibles.')).toBeInTheDocument();
  });

  it('debe mostrar mensaje vacío personalizado', () => {
    render(
      <DataTable
        columns={columns}
        data={[]}
        emptyMessage="Nothing here"
        keyExtractor={(item) => item.id}
      />
    );
    expect(screen.getByText('Nothing here')).toBeInTheDocument();
  });

  it('debe renderizar filas de datos', () => {
    render(
      <DataTable
        columns={columns}
        data={data}
        keyExtractor={(item) => item.id}
      />
    );
    expect(screen.getByText('Item 1')).toBeInTheDocument();
    expect(screen.getByText('Item 2')).toBeInTheDocument();
    expect(screen.getByText('$100')).toBeInTheDocument();
    expect(screen.getByText('$200')).toBeInTheDocument();
  });

  it('debe renderizar encabezados de columna', () => {
    render(
      <DataTable
        columns={columns}
        data={data}
        keyExtractor={(item) => item.id}
      />
    );
    expect(screen.getByText('ID')).toBeInTheDocument();
    expect(screen.getByText('Name')).toBeInTheDocument();
    expect(screen.getByText('Value')).toBeInTheDocument();
  });

  it('debe llamar a onRowClick al hacer clic en una fila', () => {
    const onRowClick = vi.fn();
    render(
      <DataTable
        columns={columns}
        data={data}
        onRowClick={onRowClick}
        keyExtractor={(item) => item.id}
      />
    );
    fireEvent.click(screen.getByText('Item 1'));
    expect(onRowClick).toHaveBeenCalledWith(data[0]);
  });

  it('debe usar accessorKey para renderizar contenido de celda', () => {
    render(
      <DataTable
        columns={columns}
        data={data}
        keyExtractor={(item) => item.id}
      />
    );
    expect(screen.getByText('1')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
  });
});
