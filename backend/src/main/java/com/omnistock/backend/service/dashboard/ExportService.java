package com.omnistock.backend.service.dashboard;

import com.omnistock.backend.dtos.analytics.*;
import com.omnistock.backend.dtos.dashboard.*;
import com.omnistock.backend.dtos.supplier.ProviderStatusDto;
import com.omnistock.backend.service.analytics.SupplierAnalyticsService;
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

    private final SupplierAnalyticsService analyticsService;

    public ExportService(SupplierAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    public byte[] generateDashboardExcel(DashboardSummaryDto summary) throws IOException {
        logger.info("Iniciando generación de Excel de exportación del dashboard...");
        long startTime = System.currentTimeMillis();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Workbook workbook = new XSSFWorkbook()) {

            // Estilo común: texto negro, sin relleno de color
            CellStyle blackStyle = createBlackStyle(workbook);
            // Estilo para encabezados: texto negro, negrita
            CellStyle headerStyle = createHeaderStyle(workbook);

            // ========== HOJAS EXISTENTES (Dashboard) ==========

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

            // 9. Estado Proveedores (failedProviders)
            addFailedProvidersSheet(workbook, summary.failedProviders(), headerStyle, blackStyle);

            // ========== HOJAS NUEVAS (Analytics) ==========

            // 10. Variación de Precios
            addPriceVariationSheet(workbook, headerStyle, blackStyle);

            // 11. Ruptura de Stock
            addStockoutRatesSheet(workbook, headerStyle, blackStyle);

            // 12. Dispersión de Precios
            addPriceDispersionSheet(workbook, headerStyle, blackStyle);

            // 13. MOQ por Proveedor
            addMoqDistributionSheet(workbook, headerStyle, blackStyle);

            // 14. Condición de Productos
            addConditionMixSheet(workbook, headerStyle, blackStyle);

            // 15. Cobertura de Costes
            addCostCoverageSheet(workbook, headerStyle, blackStyle);

            // 16. Volatilidad de Stock
            addStockVolatilitySheet(workbook, headerStyle, blackStyle);

            // 17. Crecimiento del Catálogo
            addCatalogGrowthSheet(workbook, headerStyle, blackStyle);

            // 18. Estabilidad de Precios
            addPriceStabilitySheet(workbook, headerStyle, blackStyle);

            // 19. Oportunidades de Trading
            addTradingOpportunitiesSheet(workbook, headerStyle, blackStyle);

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

    // ========== HOJAS ANALYTICS ==========

    private void addFailedProvidersSheet(Workbook workbook, List<ProviderStatusDto> data,
                                         CellStyle headerStyle, CellStyle cellStyle) {
        String[][] rows = data.stream()
                .map(item -> new String[]{
                        String.valueOf(item.id()),
                        item.name(),
                        item.lastError()
                })
                .toArray(String[][]::new);
        addSheet(workbook, "Estado Proveedores", new String[]{"ID", "Proveedor", "Estado"},
                rows, headerStyle, cellStyle);
    }

    private void addPriceVariationSheet(Workbook workbook, CellStyle headerStyle, CellStyle cellStyle) {
        try {
            List<PriceIndexVariationDto> data = analyticsService.getPriceIndexVariation(6, 50.0);
            String[][] rows = data.stream()
                    .map(item -> new String[]{
                            String.valueOf(item.supplierId()),
                            item.supplierName(),
                            String.valueOf(item.productId()),
                            item.mpn(),
                            item.month(),
                            String.valueOf(item.avgPrice()),
                            item.prevAvgPrice() != null ? String.valueOf(item.prevAvgPrice()) : "N/A",
                            item.variationPct() != null ? String.format("%.2f", item.variationPct()) + "%" : "N/A",
                            item.isOutlier() ? "SÍ" : "NO"
                    })
                    .toArray(String[][]::new);
            addSheet(workbook, "Variación Precios",
                    new String[]{"SupplierId", "Proveedor", "ProductoId", "MPN", "Mes", "Precio Prom.", "Precio Prev.", "Variación %", "Outlier"},
                    rows, headerStyle, cellStyle);
        } catch (Exception e) {
            logger.warn("Error al generar hoja Variación Precios: {}", e.getMessage());
        }
    }

    private void addStockoutRatesSheet(Workbook workbook, CellStyle headerStyle, CellStyle cellStyle) {
        try {
            List<StockoutRateDto> data = analyticsService.getStockoutRates();
            String[][] rows = data.stream()
                    .map(item -> new String[]{
                            String.valueOf(item.supplierId()),
                            item.supplierName(),
                            String.valueOf(item.totalSkus()),
                            String.valueOf(item.outOfStockSkus()),
                            String.format("%.2f%%", item.stockoutRate())
                    })
                    .toArray(String[][]::new);
            addSheet(workbook, "Ruptura Stock",
                    new String[]{"SupplierId", "Proveedor", "Total SKUs", "SKUs Sin Stock", "Tasa Ruptura"},
                    rows, headerStyle, cellStyle);
        } catch (Exception e) {
            logger.warn("Error al generar hoja Ruptura Stock: {}", e.getMessage());
        }
    }

    private void addPriceDispersionSheet(Workbook workbook, CellStyle headerStyle, CellStyle cellStyle) {
        try {
            List<PriceDispersionDto> data = analyticsService.getPriceDispersion(100);
            String[][] rows = data.stream()
                    .map(item -> new String[]{
                            String.valueOf(item.productId()),
                            item.mpn(),
                            item.category(),
                            String.valueOf(item.supplierCount()),
                            String.valueOf(item.minPrice()),
                            String.valueOf(item.maxPrice()),
                            String.valueOf(item.avgPrice()),
                            String.format("%.2f%%", item.dispersionPct())
                    })
                    .toArray(String[][]::new);
            addSheet(workbook, "Dispersión Precios",
                    new String[]{"ProductoId", "MPN", "Categoría", "Proveedores", "Precio Min", "Precio Max", "Precio Prom", "Dispersión %"},
                    rows, headerStyle, cellStyle);
        } catch (Exception e) {
            logger.warn("Error al generar hoja Dispersión Precios: {}", e.getMessage());
        }
    }

    private void addMoqDistributionSheet(Workbook workbook, CellStyle headerStyle, CellStyle cellStyle) {
        try {
            List<MoqDistributionDto> data = analyticsService.getMoqDistribution();
            String[][] rows = data.stream()
                    .map(item -> new String[]{
                            String.valueOf(item.supplierId()),
                            item.supplierName(),
                            String.format("%.2f", item.avgMoq()),
                            String.valueOf(item.minMoq()),
                            String.valueOf(item.maxMoq()),
                            String.valueOf(item.skusWithMoqAbove10())
                    })
                    .toArray(String[][]::new);
            addSheet(workbook, "MOQ Proveedores",
                    new String[]{"SupplierId", "Proveedor", "MOQ Prom", "MOQ Min", "MOQ Max", "SKUs MOQ>10"},
                    rows, headerStyle, cellStyle);
        } catch (Exception e) {
            logger.warn("Error al generar hoja MOQ: {}", e.getMessage());
        }
    }

    private void addConditionMixSheet(Workbook workbook, CellStyle headerStyle, CellStyle cellStyle) {
        try {
            List<ConditionMixDto> data = analyticsService.getConditionMix();
            String[][] rows = data.stream()
                    .map(item -> new String[]{
                            String.valueOf(item.supplierId()),
                            item.supplierName(),
                            String.valueOf(item.totalSkus()),
                            String.valueOf(item.newCount()),
                            String.valueOf(item.refurbishedCount()),
                            String.valueOf(item.boxDamagedCount()),
                            String.valueOf(item.usedCount()),
                            String.format("%.1f%%", item.newPct()),
                            String.format("%.1f%%", item.refurbishedPct()),
                            String.format("%.1f%%", item.boxDamagedPct()),
                            String.format("%.1f%%", item.usedPct())
                    })
                    .toArray(String[][]::new);
            addSheet(workbook, "Condición Productos",
                    new String[]{"SupplierId", "Proveedor", "Total", "New", "Refurbished", "Box Damaged", "Used", "% New", "% Refurb", "% BoxDmg", "% Used"},
                    rows, headerStyle, cellStyle);
        } catch (Exception e) {
            logger.warn("Error al generar hoja Condición Productos: {}", e.getMessage());
        }
    }

    private void addCostCoverageSheet(Workbook workbook, CellStyle headerStyle, CellStyle cellStyle) {
        try {
            List<CostCoverageDto> data = analyticsService.getCostCoverage(200);
            String[][] rows = data.stream()
                    .map(item -> new String[]{
                            String.valueOf(item.productId()),
                            item.mpn(),
                            item.category(),
                            String.valueOf(item.totalSuppliers()),
                            String.valueOf(item.suppliersWithStock()),
                            item.coverageStatus()
                    })
                    .toArray(String[][]::new);
            addSheet(workbook, "Cobertura Costes",
                    new String[]{"ProductoId", "MPN", "Categoría", "Total Proveedores", "Con Stock", "Estado Cobertura"},
                    rows, headerStyle, cellStyle);
        } catch (Exception e) {
            logger.warn("Error al generar hoja Cobertura Costes: {}", e.getMessage());
        }
    }

    private void addStockVolatilitySheet(Workbook workbook, CellStyle headerStyle, CellStyle cellStyle) {
        try {
            List<StockVolatilityDto> data = analyticsService.getStockVolatility(30, 100);
            String[][] rows = data.stream()
                    .map(item -> new String[]{
                            String.valueOf(item.supplierId()),
                            item.supplierName(),
                            String.valueOf(item.productId()),
                            item.mpn(),
                            String.valueOf(item.changesIn24h()),
                            String.valueOf(item.maxStock()),
                            String.valueOf(item.minStock()),
                            item.volatilityType()
                    })
                    .toArray(String[][]::new);
            addSheet(workbook, "Volatilidad Stock",
                    new String[]{"SupplierId", "Proveedor", "ProductoId", "MPN", "Cambios 24h", "Stock Max", "Stock Min", "Tipo Volatilidad"},
                    rows, headerStyle, cellStyle);
        } catch (Exception e) {
            logger.warn("Error al generar hoja Volatilidad Stock: {}", e.getMessage());
        }
    }

    private void addCatalogGrowthSheet(Workbook workbook, CellStyle headerStyle, CellStyle cellStyle) {
        try {
            List<CatalogGrowthDto> data = analyticsService.getCatalogGrowth(12);
            String[][] rows = data.stream()
                    .map(item -> new String[]{
                            String.valueOf(item.year()),
                            String.valueOf(item.week()),
                            item.weekLabel(),
                            String.valueOf(item.newProducts())
                    })
                    .toArray(String[][]::new);
            addSheet(workbook, "Crecimiento Catálogo",
                    new String[]{"Año", "Semana", "Etiqueta", "Nuevos Productos"},
                    rows, headerStyle, cellStyle);
        } catch (Exception e) {
            logger.warn("Error al generar hoja Crecimiento Catálogo: {}", e.getMessage());
        }
    }

    private void addPriceStabilitySheet(Workbook workbook, CellStyle headerStyle, CellStyle cellStyle) {
        try {
            List<PriceStabilityDto> data = analyticsService.getPriceStability(90, 100);
            String[][] rows = data.stream()
                    .map(item -> new String[]{
                            String.valueOf(item.productId()),
                            item.mpn(),
                            String.valueOf(item.supplierId()),
                            item.supplierName(),
                            String.valueOf(item.avgPrice()),
                            String.valueOf(item.stddevPrice()),
                            String.format("%.2f%%", item.cvPct()),
                            item.stabilityLabel(),
                            String.valueOf(item.pricePoints())
                    })
                    .toArray(String[][]::new);
            addSheet(workbook, "Estabilidad Precios",
                    new String[]{"ProductoId", "MPN", "SupplierId", "Proveedor", "Precio Prom", "StdDev", "CV %", "Etiqueta", "Puntos"},
                    rows, headerStyle, cellStyle);
        } catch (Exception e) {
            logger.warn("Error al generar hoja Estabilidad Precios: {}", e.getMessage());
        }
    }

    private void addTradingOpportunitiesSheet(Workbook workbook, CellStyle headerStyle, CellStyle cellStyle) {
        try {
            List<TradingOpportunityDto> data = analyticsService.getTradingOpportunities(50);
            String[][] rows = data.stream()
                    .map(item -> new String[]{
                            String.valueOf(item.id()),
                            item.mpn(),
                            item.model(),
                            item.brand(),
                            item.category(),
                            String.valueOf(item.minCostPrice()),
                            String.valueOf(item.minRetailPrice()),
                            String.valueOf(item.gap()),
                            String.format("%.2f%%", item.marginPct())
                    })
                    .toArray(String[][]::new);
            addSheet(workbook, "Oportunidades Trading",
                    new String[]{"ID", "MPN", "Modelo", "Marca", "Categoría", "Coste Min", "PVP Min", "Gap", "Margen %"},
                    rows, headerStyle, cellStyle);
        } catch (Exception e) {
            logger.warn("Error al generar hoja Oportunidades Trading: {}", e.getMessage());
        }
    }
}
