import { useState, ReactNode, useMemo, useEffect, useCallback, useContext } from 'react';
import { useAuth } from 'features/auth/context/AuthContext';
import { BudgetContext } from './BudgetContextStore';
import { NotificationContext } from 'features/notifications/context/NotificationContextStore';
import api from '@/services/api';
import type { BudgetResponseDto, BudgetRequestDto, BudgetLineDto } from '@/types';
/**
 * Representa un ítem del presupuesto en la UI local (antes de simular vía API).
 * Estructura similar a BudgetItemDto pero con campos adicionales para la UI.
 */
export interface BudgetItem {
  id: string; // Composite ID: `${productId}-${supplierId}`
  productId: number;
  supplierId: number;
  productName: string;
  unitPrice: number;
  quantity: number;
  selected: boolean;
  supplierName: string;
  stock: number;
  mpn?: string;
  pvpPrice?: number;
  unit?: string;
  productUrl?: string;
  supplierRef?: string;
  notes?: string;
}

/**
 * Datos mínimos para añadir un ítem al presupuesto.
 */
export interface AddBudgetItem {
  productId: number;
  supplierId: number;
  productName: string;
  supplierName: string;
  unitPrice: number;
  quantity: number;
  stock: number;
  mpn?: string;
  pvpPrice?: number;
  unit?: string;
  productUrl?: string;
  supplierRef?: string;
  notes?: string;
}

/**
 * Resultado de intentar añadir un producto al presupuesto.
 */
export interface AddBudgetItemResult {
  status: 'added' | 'updated' | 'max-stock-reached';
  quantity: number;
  maxQuantity: number;
}

/**
 * Estado de la simulación vía API.
 */
export interface SimulationState {
  loading: boolean;
  result: BudgetResponseDto | null;
  error: string | null;
}

/**
 * Contrato del contexto del presupuesto.
 */
export interface BudgetContextType {
  items: BudgetItem[];
  addItem: (itemToAdd: AddBudgetItem) => AddBudgetItemResult;
  removeItem: (id: string) => void;
  updateItemQuantity: (id: string, newQuantity: number) => void;
  toggleItemSelection: (id: string) => void;
  totalPrice: number;
  itemCount: number;
  simulation: SimulationState;
  simulateBudget: (budgetName: string, notes?: string) => Promise<void>;
  clearSimulation: () => void;
  clearItems: () => void;
}

const getBudgetStorageKey = (username: string | null): string =>
  username ? `inventory-budget-items:${username}` : 'inventory-budget-items:guest';

/**
 * Normaliza un ítem cargado de localStorage.
 */
const normalizeBudgetItem = (item: Partial<BudgetItem>): BudgetItem | null => {
  if (!item.id || typeof item.productId !== 'number' || typeof item.supplierId !== 'number') {
    return null;
  }

  const stock = Math.max(0, Number(item.stock) || 0);
  const maxQuantity = Math.max(1, stock || 1);
  const quantity = Math.max(1, Math.min(Number(item.quantity) || 1, maxQuantity));

  return {
    id: String(item.id),
    productId: item.productId,
    supplierId: item.supplierId,
    productName: item.productName ?? '',
    unitPrice: Number(item.unitPrice) || 0,
    quantity,
    selected: item.selected ?? true,
    supplierName: item.supplierName ?? '',
    stock,
    mpn: item.mpn,
    pvpPrice: item.pvpPrice,
    unit: item.unit,
    productUrl: item.productUrl,
    supplierRef: item.supplierRef,
    notes: item.notes,
  };
};

const loadStoredItems = (storageKey: string): BudgetItem[] => {
  if (typeof window === 'undefined') {
    return [];
  }

  try {
    const rawItems = window.localStorage.getItem(storageKey);

    if (!rawItems) {
      return [];
    }

    const parsedItems = JSON.parse(rawItems);

    if (!Array.isArray(parsedItems)) {
      return [];
    }

    return parsedItems
      .map((item) => normalizeBudgetItem(item))
      .filter((item): item is BudgetItem => item !== null);
  } catch {
    return [];
  }
};

/**
 * Proveedor del contexto de presupuesto.
 * Gestiona los ítems localmente (con persistencia en localStorage)
 * y permite simular el presupuesto vía API.
 */
export const BudgetProvider = ({ children }: { children: ReactNode }) => {
  const { username } = useAuth();
  const storageKey = useMemo(() => getBudgetStorageKey(username ?? null), [username]);

  const [items, setItems] = useState<BudgetItem[]>(() => loadStoredItems(storageKey));
  const [simulation, setSimulation] = useState<SimulationState>({
    loading: false,
    result: null,
    error: null,
  });

  // Conexión opcional al sistema global de notificaciones.
  const notificationCtx = useContext(NotificationContext);

  // Recargar cuando cambia el usuario
  useEffect(() => {
    setItems(loadStoredItems(storageKey));
  }, [storageKey]);

  // Persistir en localStorage
  useEffect(() => {
    if (typeof window === 'undefined') {
      return;
    }

    window.localStorage.setItem(storageKey, JSON.stringify(items));
  }, [items, storageKey]);

  /**
   * Añade un producto al presupuesto o incrementa su cantidad.
   */
  const addItem = (itemToAdd: AddBudgetItem): AddBudgetItemResult => {
    const newItemId = `${itemToAdd.productId}-${itemToAdd.supplierId}`;
    const normalizedStock = Math.max(0, itemToAdd.stock);
    const requestedQuantity = Math.max(1, itemToAdd.quantity);
    let result: AddBudgetItemResult = {
      status: 'added',
      quantity: 0,
      maxQuantity: normalizedStock,
    };

    if (normalizedStock <= 0) {
      return {
        status: 'max-stock-reached',
        quantity: 0,
        maxQuantity: 0,
      };
    }

    setItems((prev) => {
      const existingItem = prev.find((item) => item.id === newItemId);

      if (existingItem) {
        const nextQuantity = Math.min(existingItem.quantity + requestedQuantity, normalizedStock);

        result = {
          status: nextQuantity === existingItem.quantity ? 'max-stock-reached' : 'updated',
          quantity: nextQuantity,
          maxQuantity: normalizedStock,
        };

        return prev.map((item) =>
          item.id === newItemId
            ? {
                ...item,
                ...itemToAdd,
                id: newItemId,
                quantity: nextQuantity,
                stock: normalizedStock,
                selected: item.selected,
              }
            : item,
        );
      } else {
        const nextQuantity = Math.min(requestedQuantity, normalizedStock);

        result = {
          status: 'added',
          quantity: nextQuantity,
          maxQuantity: normalizedStock,
        };

        const newItem: BudgetItem = {
          ...itemToAdd,
          id: newItemId,
          quantity: nextQuantity,
          stock: normalizedStock,
          selected: true,
        };
        return [...prev, newItem];
      }
    });

    return result;
  };

  /**
   * Elimina un ítem del presupuesto.
   */
  const removeItem = (id: string) => {
    setItems((prev) => prev.filter((item) => item.id !== id));
  };

  /**
   * Actualiza la cantidad de un ítem.
   */
  const updateItemQuantity = (id: string, newQuantity: number) => {
    setItems((prev) =>
      prev.map((item) => {
        if (item.id === id) {
          const maxQuantity = Math.max(1, item.stock);
          const clampedQuantity = Math.max(1, Math.min(newQuantity, maxQuantity));
          return { ...item, quantity: clampedQuantity };
        }
        return item;
      }),
    );
  };

  /**
   * Alterna la selección de un ítem.
   */
  const toggleItemSelection = (id: string) => {
    setItems((prev) =>
      prev.map((item) =>
        item.id === id ? { ...item, selected: !item.selected } : item,
      ),
    );
  };

  /**
   * Simula el presupuesto vía API.
   * Envía los ítems seleccionados al backend y recibe los precios actualizados.
   */
  const simulateBudget = useCallback(async (budgetName: string, notes?: string) => {
    const selectedItems = items.filter((item) => item.selected);

    if (selectedItems.length === 0) {
      setSimulation({ loading: false, result: null, error: 'No hay productos seleccionados.' });
      return;
    }

    setSimulation({ loading: true, result: null, error: null });

    try {
      const lines: BudgetLineDto[] = selectedItems.map((item) => ({
        productId: item.productId,
        supplierId: item.supplierId,
        quantity: item.quantity,
        notes: item.notes,
      }));

      const request: BudgetRequestDto = {
        budgetName,
        notes: notes || '',
        lines,
      };

      // NOTA: api ya tiene baseURL = API_BASE, usar ruta relativa
      const response = await api.post('/budget/simulate', request);
      const budgetResult: BudgetResponseDto = response.data.data;

      setSimulation({ loading: false, result: budgetResult, error: null });
      if (notificationCtx && typeof notificationCtx.refreshCounts === 'function') {
        void notificationCtx.refreshCounts();
      }
    } catch (err: unknown) {
      const errorMessage =
        err instanceof Error ? err.message : 'Error al simular el presupuesto.';
      setSimulation({ loading: false, result: null, error: errorMessage });
      if (notificationCtx && typeof notificationCtx.refreshCounts === 'function') {
        void notificationCtx.refreshCounts();
      }
    }
  }, [items, notificationCtx]);

  /**
   * Limpia el resultado de la simulación.
   */
  const clearSimulation = () => {
    setSimulation({ loading: false, result: null, error: null });
  };

  /**
   * Limpia todos los ítems del presupuesto.
   */
  const clearItems = () => {
    setItems([]);
    clearSimulation();
  };

  const totalPrice = useMemo(
    () =>
      items
        .filter((item) => item.selected)
        .reduce((acc, item) => acc + item.unitPrice * item.quantity, 0),
    [items],
  );

  const itemCount = useMemo(
    () => items.reduce((acc, item) => acc + item.quantity, 0),
    [items],
  );

  return (
    <BudgetContext.Provider
      value={{
        items,
        addItem,
        removeItem,
        updateItemQuantity,
        toggleItemSelection,
        totalPrice,
        itemCount,
        simulation,
        simulateBudget,
        clearSimulation,
        clearItems,
      }}
    >
      {children}
    </BudgetContext.Provider>
  );
};
