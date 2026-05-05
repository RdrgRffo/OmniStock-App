import React, { useState, useMemo } from 'react';

export interface Column<T> {
    header: React.ReactNode;
    accessorKey?: keyof T;
    cell?: (item: T, index: number) => React.ReactNode;
    align?: 'left' | 'center' | 'right';
    sortable?: boolean;
    sortKey?: string;
}

interface DataTableProps<T> {
    columns: Column<T>[];
    data: T[];
    isLoading?: boolean;
    isError?: boolean;
    loadingMessage?: string;
    errorMessage?: string;
    emptyMessage?: string;
    onRowClick?: (item: T) => void;
    keyExtractor: (item: T, index: number) => string | number;
    pageSize?: number;
    defaultSortKey?: string;
    defaultSortDirection?: 'asc' | 'desc';
}

/**
 * Componente presentacional genérico para renderizar tablas
 * usando los estilos globales de index.css.
 * Soporta ordenamiento por columna (SortableHeader) y paginación.
 */
export function DataTable<T>({
    columns,
    data,
    isLoading = false,
    isError = false,
    loadingMessage = 'Cargando datos...',
    errorMessage = 'Error al cargar los datos.',
    emptyMessage = 'No hay datos disponibles.',
    onRowClick,
    keyExtractor,
    pageSize = 20,
    defaultSortKey,
    defaultSortDirection = 'desc',
}: DataTableProps<T>) {

    const [currentPage, setCurrentPage] = useState(0);
    const [sortKey, setSortKey] = useState<string | undefined>(defaultSortKey);
    const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>(defaultSortDirection);

    // --- Ordenamiento ---
    // NOTA: useMemo debe ir antes de cualquier early return para cumplir con las reglas de hooks
    const sortedData = useMemo(() => {
        if (!sortKey || !data) return data ?? [];
        return [...data].sort((a, b) => {
            const aRecord = a as Record<string, unknown>;
            const bRecord = b as Record<string, unknown>;
            const aVal = aRecord[sortKey];
            const bVal = bRecord[sortKey];

            // Manejar nulos/undefined
            if (aVal == null && bVal == null) return 0;
            if (aVal == null) return 1;
            if (bVal == null) return -1;

            let comparison = 0;
            if (typeof aVal === 'number' && typeof bVal === 'number') {
                comparison = aVal - bVal;
            } else if (typeof aVal === 'string' && typeof bVal === 'string') {
                comparison = aVal.localeCompare(bVal);
            } else {
                comparison = String(aVal).localeCompare(String(bVal));
            }

            return sortDirection === 'asc' ? comparison : -comparison;
        });
    }, [data, sortKey, sortDirection]);

    // --- Early returns (después de todos los hooks) ---
    if (isLoading) {
        return <div className="table-message-center">{loadingMessage}</div>;
    }

    if (isError) {
        return <div className="table-message-center" style={{ color: 'var(--color-danger)' }}>{errorMessage}</div>;
    }

    if (!data || data.length === 0) {
        return <div className="table-message-center">{emptyMessage}</div>;
    }

    // --- Paginación ---
    const totalPages = Math.ceil(sortedData.length / pageSize);
    const safePage = Math.min(currentPage, Math.max(0, totalPages - 1));
    const startIndex = safePage * pageSize;
    const pageData = sortedData.slice(startIndex, startIndex + pageSize);

    const goToPage = (page: number) => {
        setCurrentPage(Math.max(0, Math.min(page, totalPages - 1)));
    };

    const handleSort = (col: Column<T>) => {
        const key = col.sortKey ?? (col.accessorKey as string | undefined);
        if (!key) return;

        if (sortKey === key) {
            // Cambiar dirección
            setSortDirection(prev => (prev === 'asc' ? 'desc' : 'asc'));
        } else {
            setSortKey(key);
            setSortDirection('desc');
        }
        setCurrentPage(0);
    };

    const renderSortIndicator = (col: Column<T>) => {
        const key = col.sortKey ?? (col.accessorKey as string | undefined);
        if (!key) return null;
        if (sortKey !== key) {
            return <span style={{ marginLeft: '0.3rem', opacity: 0.3, fontSize: '0.65rem' }}>↕</span>;
        }
        return (
            <span style={{ marginLeft: '0.3rem', fontSize: '0.65rem' }}>
                {sortDirection === 'asc' ? '▲' : '▼'}
            </span>
        );
    };

    return (
        <div className="table-container">
            <table className="data-table">
                <thead>
                    <tr>
                        {columns.map((col, index) => {
                            const isSortable = col.sortable !== false && (!!col.sortKey || !!col.accessorKey);
                            return (
                                <th
                                    key={index}
                                    style={{
                                        textAlign: col.align || 'left',
                                        cursor: isSortable ? 'pointer' : 'default',
                                        userSelect: 'none',
                                    }}
                                    onClick={() => isSortable && handleSort(col)}
                                    title={isSortable ? 'Ordenar por esta columna' : undefined}
                                >
                                    {col.header}
                                    {isSortable && renderSortIndicator(col)}
                                </th>
                            );
                        })}
                    </tr>
                </thead>
                <tbody>
                    {pageData.map((item, rowIndex) => (
                        <tr
                            key={keyExtractor(item, startIndex + rowIndex)}
                            className={onRowClick ? "clickable-row" : ""}
                            onClick={() => onRowClick && onRowClick(item)}
                        >
                            {columns.map((col, idx) => {
                                let content: React.ReactNode = null;

                                if (col.cell) {
                                    content = col.cell(item, startIndex + rowIndex);
                                } else if (col.accessorKey) {
                                    const val = item[col.accessorKey];
                                    content = val as React.ReactNode;
                                }

                                return <td key={idx} style={{ textAlign: col.align || 'left' }}>{content}</td>;
                            })}
                        </tr>
                    ))}
                </tbody>
            </table>
            {totalPages > 1 && (
                <div style={{
                    display: 'flex',
                    justifyContent: 'center',
                    alignItems: 'center',
                    gap: '0.5rem',
                    padding: '0.75rem 0 0.25rem 0',
                    fontSize: '0.8rem',
                }}>
                    <button
                        type="button"
                        onClick={() => goToPage(safePage - 1)}
                        disabled={safePage === 0}
                        style={{
                            padding: '4px 10px',
                            borderRadius: '6px',
                            border: '1px solid var(--bg-tertiary)',
                            background: 'var(--bg-secondary)',
                            cursor: safePage === 0 ? 'not-allowed' : 'pointer',
                            opacity: safePage === 0 ? 0.4 : 1,
                            fontSize: '0.8rem',
                        }}
                    >
                        ◀ Anterior
                    </button>
                    <span style={{ color: 'var(--text-secondary)' }}>
                        Pág. {safePage + 1} de {totalPages}
                        <span style={{ marginLeft: '0.5rem', color: 'var(--text-muted)' }}>
                            ({sortedData.length} registros)
                        </span>
                    </span>
                    <button
                        type="button"
                        onClick={() => goToPage(safePage + 1)}
                        disabled={safePage >= totalPages - 1}
                        style={{
                            padding: '4px 10px',
                            borderRadius: '6px',
                            border: '1px solid var(--bg-tertiary)',
                            background: 'var(--bg-secondary)',
                            cursor: safePage >= totalPages - 1 ? 'not-allowed' : 'pointer',
                            opacity: safePage >= totalPages - 1 ? 0.4 : 1,
                            fontSize: '0.8rem',
                        }}
                    >
                        Siguiente ▶
                    </button>
                </div>
            )}
        </div>
    );
}
