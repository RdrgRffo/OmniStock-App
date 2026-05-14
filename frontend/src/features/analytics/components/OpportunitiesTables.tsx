import { Link } from 'react-router-dom';
import { ArrowUp, ArrowDown } from 'lucide-react';
import { TopMoverDto, StaleProductDto } from '@/types';
import { DataTable } from '@/components/ui/DataTable';
import { formatCurrency } from '@/utils/formatting';

export const TopMoversTable = ({ data, type }: { data: TopMoverDto[]; type: 'up' | 'down' }) => {
  const isUp = type === 'up';
  const Icon = isUp ? ArrowUp : ArrowDown;
  return (
    <DataTable
      columns={[
        { header: 'Producto', sortKey: 'productName', cell: (item) => <div className="table-product-name">{item.productName}</div> },
        { header: 'Proveedor', accessorKey: 'supplierName', sortKey: 'supplierName' },
        { header: 'Precio Actual', sortKey: 'currentPrice', cell: (item) => formatCurrency(item.currentPrice), align: 'right' },
        {
          header: 'Variación',
          sortKey: 'percentChange',
          align: 'center',
          cell: (item) => (
            <span className={`price-variation ${isUp ? 'price-up' : 'price-down'}`}>
              <Icon size={14} /> {(item.percentChange ?? 0).toFixed(2)}%
            </span>
          ),
        },
        {
          header: 'Acciones',
          sortable: false,
          align: 'center',
          cell: (item) =>
            item.productId ? (
              <Link to={`/product/${item.productId}`}>
                <button type="button" className="btn-action btn-action-edit">
                  Ver Detalle
                </button>
              </Link>
            ) : null,
        },
      ]}
      data={data}
      emptyMessage="No hay datos de movimiento de precios."
      keyExtractor={(item) => item.productId ?? `${item.productName}-${item.supplierName}`}
      defaultSortKey="percentChange"
      defaultSortDirection={isUp ? 'desc' : 'asc'}
    />
  );
};

export const ZombiesTable = ({ products }: { products: StaleProductDto[] }) => {
  if (!products || products.length === 0) return null;
  return (
    <DataTable
      columns={[
        { header: 'MPN', sortKey: 'mpn', cell: (p) => <span className="font-mono text-xs">{p.mpn}</span> },
        { header: 'Producto', sortKey: 'name', cell: (p) => <span className="table-product-name">{p.name}</span> },
        { header: 'Última Act.', sortKey: 'lastUpdatedAt', cell: (p) => new Date(p.lastUpdatedAt).toLocaleDateString() },
        {
          header: 'Días Inactivo',
          sortKey: 'daysWithoutUpdate',
          align: 'center',
          cell: (p) => <span className="inactive-days-badge">{p.daysWithoutUpdate} días</span>,
        },
        {
          header: 'Acciones',
          sortable: false,
          align: 'center',
          cell: (p) =>
            p.id ? (
              <Link to={`/product/${p.id}`}>
                <button type="button" className="btn-action btn-action-edit">
                  Ver Detalle
                </button>
              </Link>
            ) : null,
        },
      ]}
      data={products}
      emptyMessage="No hay productos zombies."
      keyExtractor={(p) => p.id}
      defaultSortKey="daysWithoutUpdate"
      defaultSortDirection="desc"
    />
  );
};
