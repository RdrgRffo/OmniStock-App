import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import {
  getBudgetById,
  updateBudgetStatus,
  notifyBudgetExported,
  deleteBudget,
} from '../services/budgetService';
import { exportPurchaseOrder } from '@/utils/exportPurchaseOrder';
import { formatCurrency } from '@/utils/formatting';
import type { BudgetResponseDto, BudgetItemDto } from '@/types';
import {
  FileText,
  ArrowLeft,
  Download,
  CheckCircle2,
  Trash2,
  Loader2,
  AlertCircle,
  Clock,
  DownloadCloud,
  ExternalLink,
} from 'lucide-react';
import './BudgetDetailPage.css';

const STATUS_CONFIG: Record<string, { label: string; icon: React.ReactNode; className: string }> = {
  DRAFT: {
    label: 'Borrador',
    icon: <Clock size={16} />,
    className: 'detail-status-draft',
  },
  FINALIZED: {
    label: 'Finalizado',
    icon: <CheckCircle2 size={16} />,
    className: 'detail-status-finalized',
  },
  EXPORTED: {
    label: 'Exportado',
    icon: <DownloadCloud size={16} />,
    className: 'detail-status-exported',
  },
};

const BudgetDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [budget, setBudget] = useState<BudgetResponseDto | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isUpdating, setIsUpdating] = useState(false);
  const [isExporting, setIsExporting] = useState(false);

  const loadBudget = useCallback(async () => {
    if (!id) return;
    setIsLoading(true);
    setError(null);
    try {
      const data = await getBudgetById(Number(id));
      setBudget(data);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Error al cargar el presupuesto.';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  useEffect(() => {
    void loadBudget();
  }, [loadBudget]);

  const handleFinalize = async () => {
    if (!budget?.id) return;
    setIsUpdating(true);
    try {
      const updated = await updateBudgetStatus(budget.id, 'FINALIZED');
      setBudget(updated);
      toast.success('Presupuesto finalizado.');
    } catch {
      toast.error('Error al finalizar el presupuesto.');
    } finally {
      setIsUpdating(false);
    }
  };

  const handleExport = async () => {
    if (!budget?.id) return;
    setIsExporting(true);
    try {
      // Notificar al backend que se exportó
      await notifyBudgetExported(budget.id);

      // Convertir items al formato que espera exportPurchaseOrder
      const exportItems = budget.items.map((item: BudgetItemDto) => ({
        id: `${item.productId}-${item.supplierId}`,
        productId: item.productId,
        supplierId: item.supplierId,
        productName: item.productName,
        unitPrice: item.unitPrice,
        quantity: item.quantity,
        selected: true,
        supplierName: item.supplierName,
        stock: item.stockAvailable,
        mpn: item.mpn,
        pvpPrice: item.retailPrice ?? undefined,
        productUrl: item.productUrl ?? undefined,
        notes: item.notes ?? undefined,
      }));

      await exportPurchaseOrder(exportItems);
      toast.success('Presupuesto exportado a Excel.');

      // Recargar para ver estado actualizado
      await loadBudget();
    } catch {
      toast.error('Error al exportar el presupuesto.');
    } finally {
      setIsExporting(false);
    }
  };

  const handleDelete = async () => {
    if (!budget?.id) return;
    if (!window.confirm('¿Estás seguro de eliminar este presupuesto?')) {
      return;
    }
    try {
      await deleteBudget(budget.id);
      toast.success('Presupuesto eliminado.');
      navigate('/presupuestos');
    } catch {
      toast.error('Error al eliminar el presupuesto.');
    }
  };

  const formatDate = (dateStr: string) => {
    return new Intl.DateTimeFormat('es-ES', {
      day: '2-digit',
      month: 'long',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(dateStr));
  };

  if (isLoading) {
    return (
      <div className="detail-loading">
        <Loader2 size={32} className="spin-icon" />
        <p>Cargando presupuesto...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="detail-error">
        <AlertCircle size={24} />
        <p>{error}</p>
        <button className="btn btn-secondary" onClick={() => void loadBudget()}>
          Reintentar
        </button>
        <button className="btn btn-secondary" onClick={() => navigate('/presupuestos')}>
          Volver a la lista
        </button>
      </div>
    );
  }

  if (!budget) {
    return (
      <div className="detail-error">
        <AlertCircle size={24} />
        <p>Presupuesto no encontrado.</p>
        <button className="btn btn-secondary" onClick={() => navigate('/presupuestos')}>
          Volver a la lista
        </button>
      </div>
    );
  }

  const statusConfig = STATUS_CONFIG[budget.status] ?? STATUS_CONFIG.DRAFT;
  const canFinalize = budget.status === 'DRAFT';
  const canExport = budget.status === 'DRAFT' || budget.status === 'FINALIZED';

  return (
    <div className="detail-container">
      {/* Header */}
      <header className="detail-header">
        <button className="btn btn-secondary detail-back-btn" onClick={() => navigate('/presupuestos')}>
          <ArrowLeft size={16} aria-hidden="true" />
          Volver
        </button>
        <div className="detail-title-wrap">
          <FileText size={28} className="detail-title-icon" aria-hidden="true" />
          <div>
            <h1 className="detail-title">{budget.budgetName}</h1>
            <p className="detail-number">{budget.budgetNumber}</p>
          </div>
        </div>
        <div className={`detail-status-badge ${statusConfig.className}`}>
          {statusConfig.icon}
          <span>{statusConfig.label}</span>
        </div>
      </header>

      {/* Info cards */}
      <div className="detail-info-grid">
        <div className="detail-info-card">
          <span className="detail-info-label">Creado por</span>
          <span className="detail-info-value">{budget.createdBy}</span>
        </div>
        <div className="detail-info-card">
          <span className="detail-info-label">Fecha de creación</span>
          <span className="detail-info-value">{formatDate(budget.createdAt)}</span>
        </div>
        <div className="detail-info-card">
          <span className="detail-info-label">Última actualización</span>
          <span className="detail-info-value">{formatDate(budget.updatedAt)}</span>
        </div>
        <div className="detail-info-card">
          <span className="detail-info-label">Total</span>
          <span className="detail-info-value detail-total-amount">
            {formatCurrency(budget.totalAmount)}
          </span>
        </div>
      </div>

      {/* Notes */}
      {budget.notes && (
        <div className="detail-notes">
          <h3>Notas</h3>
          <p>{budget.notes}</p>
        </div>
      )}

      {/* Items table */}
      <div className="detail-items-section">
        <h3 className="detail-items-title">
          Productos ({budget.items.length})
        </h3>
        <div className="detail-items-table-wrap">
          <table className="detail-items-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Producto</th>
                <th>MPN</th>
                <th>Proveedor</th>
                <th>Cantidad</th>
                <th>Stock</th>
                <th>Precio Unit.</th>
                <th>Subtotal</th>
                <th>URL</th>
              </tr>
            </thead>
            <tbody>
              {budget.items.map((item, index) => (
                <tr key={`${item.productId}-${item.supplierId}`}>
                  <td className="detail-item-index">{index + 1}</td>
                  <td className="detail-item-name">{item.productName}</td>
                  <td className="detail-item-mpn">{item.mpn}</td>
                  <td>{item.supplierName}</td>
                  <td className="detail-item-qty">{item.quantity}</td>
                  <td className="detail-item-stock">{item.stockAvailable}</td>
                  <td className="detail-item-price">{formatCurrency(item.unitPrice)}</td>
                  <td className="detail-item-subtotal">
                    {formatCurrency(item.unitPrice * item.quantity)}
                  </td>
                  <td>
                    {item.productUrl ? (
                      <a
                        href={item.productUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="detail-item-url"
                        title="Abrir URL del producto"
                      >
                        <ExternalLink size={14} />
                      </a>
                    ) : (
                      <span className="text-muted">—</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr className="detail-total-row">
                <td colSpan={7} className="detail-total-label">TOTAL</td>
                <td className="detail-total-value">
                  {formatCurrency(budget.totalAmount)}
                </td>
                <td></td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>

      {/* Actions */}
      <div className="detail-actions">
        {canFinalize && (
          <button
            className="btn btn-primary"
            onClick={() => void handleFinalize()}
            disabled={isUpdating}
          >
            {isUpdating ? (
              <Loader2 size={18} className="spin-icon" />
            ) : (
              <CheckCircle2 size={18} />
            )}
            <span>Finalizar Presupuesto</span>
          </button>
        )}

        {canExport && (
          <button
            className="btn btn-success"
            onClick={() => void handleExport()}
            disabled={isExporting}
          >
            {isExporting ? (
              <Loader2 size={18} className="spin-icon" />
            ) : (
              <Download size={18} />
            )}
            <span>Exportar a Excel</span>
          </button>
        )}

        <button
          className="btn btn-danger-outline"
          onClick={() => void handleDelete()}
        >
          <Trash2 size={18} />
          <span>Eliminar</span>
        </button>
      </div>
    </div>
  );
};

export default BudgetDetailPage;
