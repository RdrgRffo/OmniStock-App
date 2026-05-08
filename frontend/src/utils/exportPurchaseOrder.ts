import ExcelJS from 'exceljs';
import type { BudgetItem } from 'features/budget/context/BudgetContext';

// Layout del Excel generado
const DATA_START_ROW = 6; // Fila 6: primer producto

const COL = {
  index: 1,          // A  #
  mpn: 2,            // B  MPN
  product: 3,        // C  Producto
  supplier: 4,       // D  Proveedor
  quantity: 5,       // E  Cantidad
  currentStock: 6,   // F  Stock Actual
  partnerPrice: 7,   // G  Precio Partner
  retailPrice: 8,    // H  Precio P.V.P
  notes: 9,          // I  Notas
} as const;

const FMT = {
  integer: '#,##0',
  currency: '#,##0.00\\ "€"',
} as const;

/** Colores corporativos */
const BRAND_DARK = 'FF000000';
const BRAND_GRAY = 'FFF8FAFC';
const WHITE = 'FFFFFFFF';
const BORDER_COLOR = 'FFE2E8F0';
const HEADER_BG = 'FF374151'; // gris oscuro para encabezados
const TOTAL_BG = 'FF1F2937';  // gris muy oscuro para total

/**
 * Genera un número de presupuesto con formato PRES-YYYYMMDD-NNN.
 */
function generateBudgetNumber(): string {
  const now = new Date();
  const yyyymmdd =
    `${now.getFullYear()}` +
    `${String(now.getMonth() + 1).padStart(2, '0')}` +
    `${String(now.getDate()).padStart(2, '0')}`;
  const seq = String(Math.floor(Math.random() * 999) + 1).padStart(3, '0');
  return `PRES-${yyyymmdd}-${seq}`;
}

/**
 * Crea un borde fino para celdas.
 */
function thinBorder(): Partial<ExcelJS.Borders> {
  return {
    top: { style: 'thin', color: { argb: BORDER_COLOR } },
    left: { style: 'thin', color: { argb: BORDER_COLOR } },
    bottom: { style: 'thin', color: { argb: BORDER_COLOR } },
    right: { style: 'thin', color: { argb: BORDER_COLOR } },
  };
}

/**
 * Construye el libro Excel desde cero con el diseño de Presupuesto.
 */
function buildWorkbook(items: BudgetItem[]): ExcelJS.Workbook {
  const workbook = new ExcelJS.Workbook();
  workbook.creator = 'OmniStock';
  workbook.created = new Date();

  const sheet = workbook.addWorksheet('Presupuesto', {
    pageSetup: { orientation: 'landscape', fitToPage: true, margins: {
      left: 0.5, right: 0.5, top: 0.5, bottom: 0.5, header: 0.3, footer: 0.3,
    }},
  });

  // --- Anchuras de columna ---
  sheet.getColumn(COL.index).width = 5;
  sheet.getColumn(COL.mpn).width = 18;
  sheet.getColumn(COL.product).width = 35;
  // Columna URL eliminada — el modelo de datos no incluye URL por proveedor
  sheet.getColumn(COL.supplier).width = 20;
  sheet.getColumn(COL.quantity).width = 10;
  sheet.getColumn(COL.currentStock).width = 12;
  sheet.getColumn(COL.partnerPrice).width = 14;
  sheet.getColumn(COL.retailPrice).width = 14;
  sheet.getColumn(COL.notes).width = 20;

  // ================================================================
  // FILA 1: Título "PRESUPUESTO" + número
  // ================================================================
  const budgetNumber = generateBudgetNumber();
  const titleRow = sheet.getRow(1);
  titleRow.height = 36;

  const titleCell = titleRow.getCell(1);
  titleCell.value = 'PRESUPUESTO';
  titleCell.font = { name: 'Calibri', size: 20, bold: true, color: { argb: BRAND_DARK } };
  // Fusionar A1:I1 para el título
  sheet.mergeCells(1, 1, 1, COL.notes);

  const numCell = titleRow.getCell(COL.notes);
  numCell.value = budgetNumber;
  numCell.font = { name: 'Calibri', size: 12, bold: true, color: { argb: BRAND_DARK } };
  numCell.alignment = { horizontal: 'right', vertical: 'middle' };

  // ================================================================
  // FILA 2: Subtítulo / información
  // ================================================================
  const subRow = sheet.getRow(2);
  subRow.height = 20;
  const subCell = subRow.getCell(1);
  subCell.value = 'Documento generado desde OmniStock — Módulo de Presupuestos';
  subCell.font = { name: 'Calibri', size: 10, color: { argb: 'FF64748B' } };
  sheet.mergeCells(2, 1, 2, COL.notes);

  // ================================================================
  // FILA 3: Fecha de generación
  // ================================================================
  const dateRow = sheet.getRow(3);
  dateRow.height = 18;
  const dateCell = dateRow.getCell(1);
  dateCell.value = `Fecha: ${new Date().toLocaleDateString('es-ES', {
    year: 'numeric', month: 'long', day: 'numeric',
  })}`;
  dateCell.font = { name: 'Calibri', size: 10, italic: true, color: { argb: 'FF64748B' } };
  sheet.mergeCells(3, 1, 3, COL.notes);

  // ================================================================
  // FILA 4: Vacía (separador)
  // ================================================================
  sheet.getRow(4).height = 6;

  // ================================================================
  // FILA 5: Encabezados de columna
  // ================================================================
  const headerRow = sheet.getRow(5);
  headerRow.height = 28;

  const headers = [
    { col: COL.index, text: '#' },
    { col: COL.mpn, text: 'MPN' },
    { col: COL.product, text: 'Producto' },
    { col: COL.supplier, text: 'Proveedor' },
    { col: COL.quantity, text: 'Cantidad' },
    { col: COL.currentStock, text: 'Stock Actual' },
    { col: COL.partnerPrice, text: 'Precio Partner' },
    { col: COL.retailPrice, text: 'Precio P.V.P' },
    { col: COL.notes, text: 'Notas' },
  ];

  for (const h of headers) {
    const cell = headerRow.getCell(h.col);
    cell.value = h.text;
    cell.font = { name: 'Calibri', size: 11, bold: true, color: { argb: WHITE } };
    cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: HEADER_BG } };
    cell.alignment = { horizontal: 'center', vertical: 'middle' };
    cell.border = thinBorder();
  }

  // ================================================================
  // FILAS 6–10 (y más si es necesario): Datos de productos
  // ================================================================
  const itemCount = items.length;
  const totalDataRows = itemCount;

  for (let i = 0; i < totalDataRows; i++) {
    const rowNum = DATA_START_ROW + i;
    const row = sheet.getRow(rowNum);
    row.height = 22;

    const item = items[i];

    const setCell = (col: number, value: ExcelJS.CellValue, numFmt?: string) => {
      const cell = row.getCell(col);
      cell.value = value;
      cell.font = { name: 'Calibri', size: 10, color: { argb: BRAND_DARK } };
      cell.alignment = { vertical: 'middle', wrapText: col === COL.notes || col === COL.product };
      cell.border = thinBorder();
      if (numFmt) cell.numFmt = numFmt;
    };

    // Alternar color de fondo
    const bgColor = i % 2 === 0 ? WHITE : BRAND_GRAY;
    const bgFill = { type: 'pattern' as const, pattern: 'solid' as const, fgColor: { argb: bgColor } };

    setCell(COL.index, i + 1);
    row.getCell(COL.index).fill = bgFill;
    row.getCell(COL.index).alignment = { horizontal: 'center', vertical: 'middle' };

    setCell(COL.mpn, item.mpn ?? '');
    row.getCell(COL.mpn).fill = bgFill;

    setCell(COL.product, item.productName);
    row.getCell(COL.product).fill = bgFill;

    setCell(COL.supplier, item.supplierName ?? '');
    row.getCell(COL.supplier).fill = bgFill;

    setCell(COL.quantity, item.quantity, FMT.integer);
    row.getCell(COL.quantity).fill = bgFill;
    row.getCell(COL.quantity).alignment = { horizontal: 'center', vertical: 'middle' };

    setCell(COL.currentStock, item.stock ?? '', FMT.integer);
    row.getCell(COL.currentStock).fill = bgFill;
    row.getCell(COL.currentStock).alignment = { horizontal: 'center', vertical: 'middle' };

    setCell(COL.partnerPrice, item.unitPrice, FMT.currency);
    row.getCell(COL.partnerPrice).fill = bgFill;
    row.getCell(COL.partnerPrice).alignment = { horizontal: 'right', vertical: 'middle' };

    setCell(COL.retailPrice, item.pvpPrice ?? '', FMT.currency);
    row.getCell(COL.retailPrice).fill = bgFill;
    row.getCell(COL.retailPrice).alignment = { horizontal: 'right', vertical: 'middle' };

    setCell(COL.notes, item.notes ?? '');
    row.getCell(COL.notes).fill = bgFill;

    row.commit();
  }

  // ================================================================
  // FILA DE TOTAL (justo debajo del último producto)
  // ================================================================
  const lastDataRow = DATA_START_ROW + itemCount - 1;
  const summaryRowNum = lastDataRow + 1;
  const summaryRow = sheet.getRow(summaryRowNum);
  summaryRow.height = 28;

  // Etiqueta "TOTAL" en columna A (fusionada de A a G)
  sheet.mergeCells(summaryRowNum, 1, summaryRowNum, COL.currentStock);
  const labelCell = summaryRow.getCell(1);
  labelCell.value = 'TOTAL PRESUPUESTO';
  labelCell.font = { name: 'Calibri', size: 12, bold: true, color: { argb: WHITE } };
  labelCell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: TOTAL_BG } };
  labelCell.alignment = { horizontal: 'right', vertical: 'middle' };
  labelCell.border = thinBorder();

  // Celdas de H e I con fondo oscuro
  for (let c = COL.currentStock + 1; c <= COL.notes; c++) {
    const cell = summaryRow.getCell(c);
    cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: TOTAL_BG } };
    cell.border = thinBorder();
  }

  // Total en columna G (Precio Partner) = SUMPRODUCT(E * G)
  const totalCell = summaryRow.getCell(COL.partnerPrice);
  totalCell.value = {
    formula: `SUMPRODUCT(E${DATA_START_ROW}:E${lastDataRow},G${DATA_START_ROW}:G${lastDataRow})`,
  };
  totalCell.numFmt = FMT.currency;
  totalCell.font = { name: 'Calibri', size: 12, bold: true, color: { argb: WHITE } };
  totalCell.alignment = { horizontal: 'right', vertical: 'middle' };

  // Columna H (Precio P.V.P) — también sumamos
  const retailTotalCell = summaryRow.getCell(COL.retailPrice);
  retailTotalCell.value = {
    formula: `SUMPRODUCT(E${DATA_START_ROW}:E${lastDataRow},H${DATA_START_ROW}:H${lastDataRow})`,
  };
  retailTotalCell.numFmt = FMT.currency;
  retailTotalCell.font = { name: 'Calibri', size: 12, bold: true, color: { argb: WHITE } };
  retailTotalCell.alignment = { horizontal: 'right', vertical: 'middle' };

  summaryRow.commit();

  // ================================================================
  // PIE DE PÁGINA
  // ================================================================
  const footerRowNum = summaryRowNum + 1;
  const footerRow = sheet.getRow(footerRowNum);
  footerRow.height = 18;
  const footerCell = footerRow.getCell(1);
  footerCell.value = 'Este documento es un presupuesto generado automáticamente. Los precios están sujetos a cambios sin previo aviso.';
  footerCell.font = { name: 'Calibri', size: 8, italic: true, color: { argb: 'FF94A3B8' } };
  sheet.mergeCells(footerRowNum, 1, footerRowNum, COL.notes);

  return workbook;
}

/**
 * Exporta el presupuesto a Excel: genera el libro desde cero y lo descarga.
 */
export async function exportPurchaseOrder(items: BudgetItem[]): Promise<void> {
  const selectedItems = items.filter((item) => item.selected);
  const itemCount = selectedItems.length;
  if (itemCount === 0) {
    return;
  }

  // Construir el workbook desde cero (sin plantilla externa)
  const workbook = buildWorkbook(selectedItems);

  // Obtener el número de presupuesto desde la celda I1 (COL.notes)
  const sheet = workbook.worksheets[0];
  const budgetNumber = sheet.getRow(1).getCell(COL.notes).value as string;

  // Descargar el archivo
  const buffer = await workbook.xlsx.writeBuffer();
  const blob = new Blob([buffer], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = `Presupuesto_${budgetNumber}.xlsx`;
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  setTimeout(() => URL.revokeObjectURL(url), 1_000);
}
