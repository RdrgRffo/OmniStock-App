package com.omnistock.backend.service.dashboard;

import com.omnistock.backend.dtos.dashboard.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);

    public byte[] generateDashboardZip(DashboardSummaryDto summary) throws IOException {
        logger.info("Iniciando generación de Excel de exportación del dashboard...");
        long startTime = System.currentTimeMillis();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Workbook workbook = new XSSFWorkbook()) {

            // Estilo común: texto negro, sin relleno de color
            CellStyle blackStyle = createBlackStyle(workbook);
            // Estilo para encabezados: texto negro, negrita
            CellStyle headerStyle = createHeaderStyle(workbook);

            // 1. KPIs Generales
            addSheet(workbook, "KPIs Generales", new String[]{"Métrica", "Valor"},
                    new String[][]{
                            {"Total Productos", String.valueOf(summary.totalProducts())},
                            {"Total Proveedores", String.valueOf(summary.totalSuppliers())},
                            {"Latencia Promedio (ms)", String.valueOf(summary.avgLatencyOverall())},
                            {"Productos Disponibles", String.valueOf(summary.productsAvailable())},
                            {"Productos Bajo Stock", String.valueOf(summary.productsLowStock())},
                            {"Productos Sin Stock", String.valueOf(summary.productsOutOfStock())}
                    }, headerStyle, blackStyle);

            // 2. Top Marcas
            addChartDataSheet(workbook, "Top Marcas", new String[]{"Marca", "Cantidad"},
                    summary.productsByBrand(), headerStyle, blackStyle);

            // 3. Stock por Proveedor
            addChartDataSheet(workbook, "Stock Proveedores", new String[]{"Proveedor", "Stock Total"},
                    summary.stockBySupplier(), headerStyle, blackStyle);

            // 4. Historial Sincronización
            addSyncHistorySheet(workbook, summary.syncHistory(), headerStyle, blackStyle);

            // 5. Top Productos Caros
            addTopProductsSheet(workbook, summary.topExpensiveProducts(), headerStyle, blackStyle);

            // 6. Top Movers (Subidas)
            addTopMoversSheet(workbook, "Alertas Subidas Precio", summary.topPriceIncreases(), headerStyle, blackStyle);

            // 7. Top Movers (Bajadas)
            addTopMoversSheet(workbook, "Oportunidades Bajadas Precio", summary.topPriceDecreases(), headerStyle, blackStyle);

            // 8. Productos Zombies
            addZombiesSheet(workbook, summary.staleProducts(), headerStyle, blackStyle);

            workbook.write(baos);
        }

        byte[] result = baos.toByteArray();
        long endTime = System.currentTimeMillis();
        logger.info("Excel generado exitosamente. Tamaño: {} bytes. Tiempo: {} ms", result.length, (endTime - startTime));
        return result;
    }

    private CellStyle createBlackStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setColor(IndexedColors.BLACK.getIndex());
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setColor(IndexedColors.BLACK.getIndex());
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void addSheet(Workbook workbook, String sheetName, String[] headers, String[][] data,
                          CellStyle headerStyle, CellStyle cellStyle) {
        Sheet sheet = workbook.createSheet(sheetName);
        // Encabezados
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        // Datos
        for (int r = 0; r < data.length; r++) {
            Row row = sheet.createRow(r + 1);
            for (int c = 0; c < data[r].length; c++) {
                Cell cell = row.createCell(c);
                cell.setCellValue(data[r][c]);
                cell.setCellStyle(cellStyle);
            }
        }
        // Autoajustar columnas
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void addChartDataSheet(Workbook workbook, String sheetName, String[] headers,
                                   List<ChartDataDto> data, CellStyle headerStyle, CellStyle cellStyle) {
        String[][] rows = data.stream()
                .map(item -> new String[]{item.label(), String.valueOf(item.value())})
                .toArray(String[][]::new);
        addSheet(workbook, sheetName, headers, rows, headerStyle, cellStyle);
    }

    private void addSyncHistorySheet(Workbook workbook, List<SyncHistoryDto> data,
                                     CellStyle headerStyle, CellStyle cellStyle) {
        String[][] rows = data.stream()
                .map(item -> new String[]{item.date(), String.valueOf(item.successCount()), String.valueOf(item.errorCount())})
                .toArray(String[][]::new);
        addSheet(workbook, "Historial Sync", new String[]{"Fecha", "Éxitos", "Errores"}, rows, headerStyle, cellStyle);
    }

    private void addTopProductsSheet(Workbook workbook, List<TopProductDto> data,
                                     CellStyle headerStyle, CellStyle cellStyle) {
        String[][] rows = data.stream()
                .map(item -> new String[]{
                        String.valueOf(item.productId()),
                        item.name(),
                        item.supplierName(),
                        String.valueOf(item.price())
                })
                .toArray(String[][]::new);
        addSheet(workbook, "Top Productos Caros", new String[]{"ProductoId", "Producto", "Proveedor", "Precio"},
                rows, headerStyle, cellStyle);
    }

    private void addTopMoversSheet(Workbook workbook, String sheetName, List<TopMoverDto> data,
                                   CellStyle headerStyle, CellStyle cellStyle) {
        String[][] rows = data.stream()
                .map(item -> new String[]{
                        String.valueOf(item.productId()),
                        item.productName(),
                        item.supplierName(),
                        String.valueOf(item.previousPrice()),
                        String.valueOf(item.currentPrice()),
                        String.valueOf(item.percentChange())
                })
                .toArray(String[][]::new);
        addSheet(workbook, sheetName, new String[]{"ProductoId", "Producto", "Proveedor", "Precio Anterior", "Precio Actual", "Variación %"},
                rows, headerStyle, cellStyle);
    }

    private void addZombiesSheet(Workbook workbook, List<StaleProductDto> data,
                                 CellStyle headerStyle, CellStyle cellStyle) {
        String[][] rows = data.stream()
                .map(item -> new String[]{
                        String.valueOf(item.id()),
                        item.mpn(),
                        item.name(),
                        String.valueOf(item.lastUpdatedAt()),
                        String.valueOf(item.daysWithoutUpdate())
                })
                .toArray(String[][]::new);
        addSheet(workbook, "Productos Zombies", new String[]{"ID", "MPN", "Producto", "Última Actualización", "Días Inactivo"},
                rows, headerStyle, cellStyle);
    }
}
