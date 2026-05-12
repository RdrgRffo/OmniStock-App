import { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { getUserBudgets, deleteBudget } from '../services/budgetService';
import { formatCurrency } from '@/utils/formatting';
import type { BudgetResponseDto } from '@/types';
import {
  FileText,
  Plus,
  Trash2,
  Eye,
  Loader2,
  AlertCircle,
  CheckCircle2,
  Clock,
  DownloadCloud,
} from 'lucide-react';
import './BudgetListPage.css';

const STATUS_CONFIG: Record<string, { label: string; icon: React.ReactNode; className: string }> = {
  DRAFT: {
    label: 'Borrador',
    icon: <Clock size={14} />,
    className: 'status-draft',
  },
  FINALIZED: {
    label: 'Finalizado',
    icon: <CheckCircle2 size={14} />,
    className: 'status-finalized',
  },
  EXPORTED: {
    label: 'Exportado',
    icon: <DownloadCloud size={14} />,
    className: 'status-exported',
  },
};

const BudgetStatusBadge = ({ status }: { status: string }) => {
  const config = STATUS_CONFIG[status] ?? {
    label: status,
    icon: <AlertCircle size={14} />,
    className: 'status-draft',
  };
  return (
    <span className={`budget-status-badge ${config.className}`}>
      {config.icon}
      <span>{config.label}</span>
    </span>
  );
};

const BudgetListPage = () => {
  const navigate = useNavigate();
  const [budgets, setBudgets] = useState<BudgetResponseDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const loadBudgets = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await getUserBudgets();
      setBudgets(data);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Error al cargar presupuestos.';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadBudgets();
  }, [loadBudgets]);

  const handleDelete = async (id: number) => {
    if (!window.confirm('¿Estás seguro de eliminar este presupuesto?')) {
      return;
    }
    setDeletingId(id);
    try {
      await deleteBudget(id);
      toast.success('Presupuesto eliminado.');
      setBudgets((prev) => prev.filter((b) => b.id !== id));
    } catch {
      toast.error('Error al eliminar el presupuesto.');
    } finally {
      setDeletingId(null);
    }
  };

  const formatDate = (dateStr: string) => {
    return new Intl.DateTimeFormat('es-ES', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(dateStr));
  };

  return (
    <div className="budget-list-container">
      <header className="budget-list-header">
        <div className="budget-list-title-wrap">
          <FileText size={28} className="budget-list-title-icon" aria-hidden="true" />
          <div>
            <h1 className="budget-list-title">Presupuestos</h1>
            <p className="budget-list-subtitle">
              Historial de presupuestos simulados y exportados
            </p>
          </div>
        </div>
        <Link to="/presupuesto/nuevo" className="btn btn-primary budget-new-btn">
          <Plus size={18} aria-hidden="true" />
          Nuevo Presupuesto
        </Link>
      </header>

      {isLoading ? (
        <div className="budget-list-loading">
          <Loader2 size={32} className="spin-icon" aria-hidden="true" />
          <p>Cargando presupuestos...</p>
        </div>
      ) : error ? (
        <div className="budget-list-error">
          <AlertCircle size={24} />
          <p>{error}</p>
          <button className="btn btn-secondary" onClick={() => void loadBudgets()}>
            Reintentar
          </button>
        </div>
      ) : budgets.length === 0 ? (
        <div className="budget-list-empty">
          <FileText size={48} className="empty-icon" aria-hidden="true" />
          <h2>No hay presupuestos aún</h2>
          <p>Simula tu primer presupuesto desde el catálogo de productos.</p>
          <Link to="/presupuesto/nuevo" className="btn btn-primary">
            <Plus size={18} aria-hidden="true" />
            Crear Presupuesto
          </Link>
        </div>
      ) : (
        <div className="budget-list-table-wrap">
          <table className="budget-list-table">
            <thead>
              <tr>
                <th>Nº Presupuesto</th>
                <th>Nombre</th>
                <th>Estado</th>
                <th>Total</th>
                <th>Productos</th>
                <th>Creado</th>
                <th>Actualizado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {budgets.map((budget) => (
                <tr key={budget.id} className="budget-list-row">
                  <td className="budget-number-cell">
                    <span className="budget-number">{budget.budgetNumber}</span>
                  </td>
                  <td className="budget-name-cell">{budget.budgetName}</td>
                  <td>
                    <BudgetStatusBadge status={budget.status} />
                  </td>
                  <td className="budget-amount-cell">
                    {formatCurrency(budget.totalAmount)}
                  </td>
                  <td className="budget-items-count">
                    {budget.items?.length ?? 0} ítems
                  </td>
                  <td className="budget-date-cell">{formatDate(budget.createdAt)}</td>
                  <td className="budget-date-cell">{formatDate(budget.updatedAt)}</td>
                  <td className="budget-actions-cell">
                    <button
                      className="btn-icon"
                      title="Ver detalle"
                      onClick={() => navigate(`/presupuesto/${budget.id}`)}
                    >
                      <Eye size={16} />
                    </button>
                    <button
                      className="btn-icon btn-icon-danger"
                      title="Eliminar presupuesto"
                      onClick={() => void handleDelete(budget.id!)}
                      disabled={deletingId === budget.id}
                    >
                      {deletingId === budget.id ? (
                        <Loader2 size={16} className="spin-icon" />
                      ) : (
                        <Trash2 size={16} />
                      )}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default BudgetListPage;
