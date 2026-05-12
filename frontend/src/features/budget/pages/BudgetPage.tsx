import { useState } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { useBudget } from '../context/useBudget';
import { exportPurchaseOrder } from '@/utils/exportPurchaseOrder';
import { formatCurrency } from '@/utils/formatting';
import {
  ArrowRight,
  Download,
  FileText,
  Calculator,
  Trash2,
  Loader2,
  CheckCircle2,
  XCircle,
} from 'lucide-react';
import './BudgetPage.css';

/**
 * Página de presupuesto: muestra los productos añadidos, permite ajustar cantidades,
 * seleccionar productos, simular el presupuesto vía API y exportar a Excel.
 */
const BudgetPage = () => {
  const {
    items,
    removeItem,
    toggleItemSelection,
    totalPrice,
    updateItemQuantity,
    simulation,
    simulateBudget,
    clearItems,
  } = useBudget();

  const [budgetName, setBudgetName] = useState('');
  const [budgetNotes, setBudgetNotes] = useState('');

  const selectedItems = items.filter((item) => item.selected);
  const selectedCount = selectedItems.length;
  const selectedTotal = selectedItems.reduce(
    (acc, item) => acc + item.unitPrice * item.quantity,
    0,
  );

  /**
   * Simula el presupuesto vía API.
   */
  const handleSimulate = async () => {
    if (selectedCount === 0) {
      toast.error('No hay productos seleccionados para simular.');
      return;
    }

    const name = budgetName.trim() || `Presupuesto ${new Date().toLocaleDateString()}`;

    try {
      await simulateBudget(name, budgetNotes);
      toast.success('Presupuesto simulado correctamente.');
    } catch {
      toast.error('Error al simular el presupuesto.');
    }
  };

  /**
   * Exporta a Excel usando el resultado de la simulación o los ítems locales.
   */
  const handleExportToExcel = async () => {
    if (selectedCount === 0) {
      toast.error('No hay productos seleccionados para exportar.');
      return;
    }

    try {
      // Si hay simulación, usar los datos del backend; si no, usar los locales
      const exportItems = simulation.result
        ? simulation.result.items.map((item) => ({
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
          }))
        : selectedItems;

      await exportPurchaseOrder(exportItems);
      toast.success('Orden de compra generada con éxito.');
    } catch (err) {
      console.error(err);
      toast.error('Error al generar el Excel. Revisa la consola.');
    }
  };

  const hasItems = items.length > 0;
  const hasSelected = selectedCount > 0;

  return (
    <div className="presupuesto-container">
      <div className="budget-header">
        <div>
          <div className="presupuesto-title-wrap">
            <Calculator size={24} className="presupuesto-title-icon" aria-hidden="true" />
            <h1 className="presupuesto-title">Simulador de Presupuesto</h1>
          </div>
          <p className="budget-subtitle">
            Añade productos desde el catálogo, ajusta cantidades y simula el presupuesto
            con los precios actuales de los proveedores.
          </p>
        </div>

        {hasItems && (
          <div className="budget-metrics">
            <div className="budget-metric">
              <span className="budget-metric-label">Productos totales</span>
              <span className="budget-metric-value">{items.length}</span>
            </div>
            <div className="budget-metric">
              <span className="budget-metric-label">Seleccionados</span>
              <span className="budget-metric-value">{selectedCount}</span>
            </div>
            <div className="budget-metric">
              <span className="budget-metric-label">Total estimado</span>
              <span className="budget-metric-value">
                {hasSelected ? formatCurrency(selectedTotal) : '—'}
              </span>
            </div>
          </div>
        )}
      </div>

      <div className={`presupuesto-content ${!hasItems ? 'presupuesto-vacio-grid' : ''}`}>
        <div className="presupuesto-items">
          {!hasItems ? (
            <div className="presupuesto-vacio">
              <p>No hay productos en el presupuesto.</p>
              <Link to="/" className="seguir-explorando">
                <ArrowRight size={16} aria-hidden="true" />
                Explorar catálogo
              </Link>
            </div>
          ) : (
            items.map((item) => (
              <div
                key={item.id}
                className={`presupuesto-item ${!item.selected ? 'deseleccionado' : ''}`}
              >
                <div className="presupuesto-item-checkbox">
                  <input
                    type="checkbox"
                    checked={item.selected}
                    onChange={() => toggleItemSelection(item.id)}
                    title="Seleccionar para incluir en el presupuesto"
                  />
                </div>
                <div className="presupuesto-item-detalles">
                  <h3 className="presupuesto-item-nombre">{item.productName}</h3>
                  <div className="presupuesto-item-linea-secundaria">
                    <p className="presupuesto-item-proveedor">
                      Proveedor: {item.supplierName}
                    </p>
                    <div className="control-cantidad">
                      <button
                        onClick={() =>
                          updateItemQuantity(item.id, item.quantity - 1)
                        }
                        disabled={item.quantity <= 1}
                      >
                        -
                      </button>
                      <input
                        type="number"
                        value={item.quantity}
                        onChange={(e) =>
                          updateItemQuantity(
                            item.id,
                            parseInt(e.target.value, 10) || 1,
                          )
                        }
                        onBlur={(e) => {
                          if (parseInt(e.target.value, 10) < 1) {
                            updateItemQuantity(item.id, 1);
                          }
                        }}
                      />
                      <button
                        onClick={() =>
                          updateItemQuantity(item.id, item.quantity + 1)
                        }
                        disabled={item.quantity >= item.stock}
                      >
                        +
                      </button>
                    </div>
                    <div className="presupuesto-item-precio">
                      {formatCurrency(item.unitPrice * item.quantity)}
                    </div>
                  </div>
                </div>
                <button
                  className="boton-eliminar"
                  onClick={() => removeItem(item.id)}
                  title="Eliminar producto"
                >
                  <Trash2 size={18} aria-hidden="true" />
                </button>
              </div>
            ))
          )}
        </div>

        {hasItems && (
          <div className="presupuesto-resumen">
            <h2 className="summary-title">
              <FileText size={20} aria-hidden="true" />
              Resumen del Presupuesto
            </h2>

            {!hasSelected ? (
              <p className="summary-empty-message">
                No hay productos seleccionados. Marca al menos uno para incluirlo en el
                presupuesto.
              </p>
            ) : (
              <>
                {/* Nombre del presupuesto */}
                <div className="budget-name-input">
                  <label htmlFor="budgetName">Nombre del presupuesto</label>
                  <input
                    id="budgetName"
                    type="text"
                    placeholder={`Presupuesto ${new Date().toLocaleDateString()}`}
                    value={budgetName}
                    onChange={(e) => setBudgetName(e.target.value)}
                    maxLength={200}
                  />
                </div>

                {/* Notas */}
                <div className="budget-notes-input">
                  <label htmlFor="budgetNotes">Notas (opcional)</label>
                  <textarea
                    id="budgetNotes"
                    placeholder="Notas para el presupuesto..."
                    value={budgetNotes}
                    onChange={(e) => setBudgetNotes(e.target.value)}
                    maxLength={500}
                    rows={2}
                  />
                </div>

                <div className="resumen-items">
                  {selectedItems.map((item) => (
                    <div className="resumen-fila" key={item.id}>
                      <span className="resumen-item-nombre">
                        {item.productName}{' '}
                        <span className="resumen-item-cantidad">
                          (x{item.quantity})
                        </span>
                      </span>
                      <span>{formatCurrency(item.unitPrice * item.quantity)}</span>
                    </div>
                  ))}
                </div>
              </>
            )}

            <div className="resumen-fila total">
              <span>Total estimado</span>
              <span>{formatCurrency(totalPrice)}</span>
            </div>

            {/* Resultado de la simulación */}
            {simulation.result && (
              <div className="simulation-result">
                <div className="simulation-result-header">
                  <CheckCircle2 size={18} />
                  <span>Presupuesto simulado</span>
                </div>
                <div className="simulation-result-details">
                  <p>
                    <strong>Nº:</strong> {simulation.result.budgetNumber}
                  </p>
                  <p>
                    <strong>Nombre:</strong> {simulation.result.budgetName}
                  </p>
                  <p>
                    <strong>Total:</strong> {formatCurrency(simulation.result.totalAmount)}
                  </p>
                  <p>
                    <strong>Estado:</strong> {simulation.result.status}
                  </p>
                </div>
              </div>
            )}

            {simulation.error && (
              <div className="simulation-error">
                <XCircle size={18} />
                <span>{simulation.error}</span>
              </div>
            )}

            <div className="budget-actions">
              <button
                className="boton-simular"
                onClick={handleSimulate}
                disabled={!hasSelected || simulation.loading}
                title={
                  hasSelected
                    ? 'Simular presupuesto con precios actuales de los proveedores'
                    : 'Selecciona al menos un producto para simular'
                }
              >
                {simulation.loading ? (
                  <>
                    <Loader2 size={18} className="spin-icon" aria-hidden="true" />
                    <span>Simulando...</span>
                  </>
                ) : (
                  <>
                    <Calculator size={18} aria-hidden="true" />
                    <span>Simular Presupuesto</span>
                  </>
                )}
              </button>

              <button
                className="boton-exportar"
                onClick={handleExportToExcel}
                disabled={!hasSelected}
                title={
                  hasSelected
                    ? 'Generar y descargar un archivo Excel con los productos seleccionados'
                    : 'Selecciona al menos un producto para exportar'
                }
              >
                <Download size={18} aria-hidden="true" />
                <span>Exportar a Excel</span>
              </button>

              {hasItems && (
                <button
                  className="boton-limpiar"
                  onClick={() => {
                    clearItems();
                    toast.success('Presupuesto limpiado.');
                  }}
                  title="Limpiar todos los productos del presupuesto"
                >
                  <Trash2 size={18} aria-hidden="true" />
                  <span>Limpiar</span>
                </button>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default BudgetPage;
