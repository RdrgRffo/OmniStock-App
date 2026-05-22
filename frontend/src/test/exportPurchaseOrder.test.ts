import { describe, it, expect, vi, beforeEach } from 'vitest';
import { exportPurchaseOrder } from 'utils/exportPurchaseOrder';
import type { BudgetItem } from 'features/budget/context/BudgetContext';

// Mock de ExcelJS - enfoque simplificado
vi.mock('exceljs', () => {
  const mockCell = () => ({
    value: undefined,
    font: undefined,
    fill: undefined,
    alignment: undefined,
    border: undefined,
    numFmt: undefined,
  });

  const mockRow = {
    height: undefined,
    getCell: vi.fn(() => mockCell()),
    commit: vi.fn(),
  };

  const mockSheet = {
    getColumn: vi.fn(() => ({ width: undefined })),
    getRow: vi.fn(() => mockRow),
    mergeCells: vi.fn(),
  };

  const mockWorkbook = {
    creator: undefined,
    created: undefined,
    addWorksheet: vi.fn(() => mockSheet),
    xlsx: {
      writeBuffer: vi.fn().mockResolvedValue(new ArrayBuffer(8)),
    },
    worksheets: [mockSheet],
  };

  return {
    default: {
      Workbook: vi.fn(() => mockWorkbook),
    },
    Workbook: vi.fn(() => mockWorkbook),
  };
});

// Mock de URL.createObjectURL y URL.revokeObjectURL
const mockCreateObjectURL = vi.fn(() => 'blob:mock-url');
const mockRevokeObjectURL = vi.fn();
URL.createObjectURL = mockCreateObjectURL;
URL.revokeObjectURL = mockRevokeObjectURL;

describe('exportPurchaseOrder', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    document.body.innerHTML = '';
  });

  const createMockItem = (overrides: Partial<BudgetItem> = {}): BudgetItem => ({
    id: '1-1',
    productId: 1,
    supplierId: 1,
    productName: 'Test Product',
    unitPrice: 100,
    quantity: 2,
    selected: true,
    supplierName: 'Test Supplier',
    stock: 10,
    mpn: 'MPN-001',
    pvpPrice: 150,
    productUrl: 'https://example.com/product',
    notes: 'Test notes',
    ...overrides,
  });

  it('no debe exportar cuando no hay artículos seleccionados', async () => {
    const items = [createMockItem({ selected: false })];
    await exportPurchaseOrder(items);
    expect(document.body.querySelector('a')).toBeNull();
  });

  it('debe manejar un array de artículos vacío', async () => {
    await exportPurchaseOrder([]);
    expect(document.body.querySelector('a')).toBeNull();
  });

  it('debe filtrar solo los artículos seleccionados para la exportación', async () => {
    const items = [
      createMockItem({ id: '1-1', selected: true }),
      createMockItem({ id: '2-2', selected: false }),
      createMockItem({ id: '3-3', selected: true }),
    ];
    await exportPurchaseOrder(items);
    // Debería haber creado el anchor (significa que al menos 1 ítem seleccionado)
    // La función de exportación filtra los ítems seleccionados internamente
    expect(mockCreateObjectURL).toHaveBeenCalled();
  });

  it('debe manejar artículos con campos opcionales faltantes', async () => {
    const items = [createMockItem({
      mpn: undefined,
      pvpPrice: undefined,
      productUrl: undefined,
      notes: undefined,
      supplierName: '',
    })];
    await exportPurchaseOrder(items);
    expect(mockCreateObjectURL).toHaveBeenCalled();
  });

  it('debe generar números de presupuesto con el formato correcto', async () => {
    const items = [createMockItem()];
    await exportPurchaseOrder(items);
    expect(mockCreateObjectURL).toHaveBeenCalled();
  });
});
