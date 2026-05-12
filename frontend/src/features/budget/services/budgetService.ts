import api from '@/services/api';
import type { BudgetResponseDto, BudgetRequestDto } from '@/types';

interface ApiEnvelope<T> {
  success: boolean;
  message: string;
  data: T;
}

/**
 * Simula un presupuesto: envía líneas al backend y persiste el resultado.
 */
export const simulateBudget = async (
  request: BudgetRequestDto,
): Promise<BudgetResponseDto> => {
  const { data } = await api.post<ApiEnvelope<BudgetResponseDto>>(
    '/budget/simulate',
    request,
  );
  return data.data;
};

/**
 * Obtiene todos los presupuestos del usuario autenticado.
 */
export const getUserBudgets = async (): Promise<BudgetResponseDto[]> => {
  const { data } = await api.get<ApiEnvelope<BudgetResponseDto[]>>(
    '/budget',
  );
  return data.data;
};

/**
 * Obtiene un presupuesto por su ID.
 */
export const getBudgetById = async (id: number): Promise<BudgetResponseDto> => {
  const { data } = await api.get<ApiEnvelope<BudgetResponseDto>>(
    `/budget/${id}`,
  );
  return data.data;
};

/**
 * Cambia el estado de un presupuesto (DRAFT / FINALIZED / EXPORTED).
 */
export const updateBudgetStatus = async (
  id: number,
  status: string,
): Promise<BudgetResponseDto> => {
  const { data } = await api.put<ApiEnvelope<BudgetResponseDto>>(
    `/budget/${id}/status`,
    null,
    { params: { status } },
  );
  return data.data;
};

/**
 * Elimina un presupuesto.
 */
export const deleteBudget = async (id: number): Promise<void> => {
  await api.delete(`/budget/${id}`);
};

/**
 * Notifica que un presupuesto ha sido exportado a Excel.
 */
export const notifyBudgetExported = async (id: number): Promise<void> => {
  await api.post(`/budget/${id}/export`);
};
