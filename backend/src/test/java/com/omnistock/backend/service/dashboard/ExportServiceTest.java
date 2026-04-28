package com.omnistock.backend.service.dashboard;

import com.omnistock.backend.dtos.dashboard.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExportServiceTest {

    private ExportService exportService;

    @BeforeEach
    void setUp() {
        exportService = new ExportService();
    }

    private DashboardSummaryDto createSampleSummary() {
        return new DashboardSummaryDto(
                100L,                       // totalProducts
                5L,                         // totalSuppliers
                70L,                        // productsAvailable
                20L,                        // productsLowStock
                10L,                        // productsOutOfStock
                150.0,                      // avgLatencyOverall
                List.of(                    // productsByBrand
                        new ChartDataDto("BrandA", 50L),
                        new ChartDataDto("BrandB", 30L)
                ),
                List.of(                    // stockBySupplier
                        new ChartDataDto("SupplierA", 200L),
                        new ChartDataDto("SupplierB", 150L)
                ),
                List.of(                    // syncHistory
                        new SyncHistoryDto("01/01", 10, 2),
                        new SyncHistoryDto("02/01", 8, 1)
                ),
                List.of(                    // topExpensiveProducts
                        new TopProductDto(1, "Product A", "SupplierA", BigDecimal.valueOf(999.99)),
                        new TopProductDto(2, "Product B", "SupplierB", BigDecimal.valueOf(499.99))
                ),
                List.of(                    // topPriceIncreases
                        new TopMoverDto(1, "Product A", "SupplierA",
                                BigDecimal.valueOf(100), BigDecimal.valueOf(150), 50.0)
                ),
                List.of(                    // topPriceDecreases
                        new TopMoverDto(2, "Product B", "SupplierB",
                                BigDecimal.valueOf(200), BigDecimal.valueOf(150), -25.0)
                ),
                List.of(                    // staleProducts
                        new StaleProductDto(1, "MPN001", "Product A", LocalDateTime.of(2025, 1, 1, 0, 0), 30L),
                        new StaleProductDto(2, "MPN002", "Product B", LocalDateTime.of(2025, 1, 15, 0, 0), 16L)
                ),
                List.of()                   // failedProviders
        );
    }

    @Nested
    @DisplayName("generateDashboardZip")
    class GenerateDashboardZip {

        @Test
        @DisplayName("Debe generar XLSX con todas las hojas esperadas")
        void shouldGenerateXlsxWithAllSheets() throws Exception {
            DashboardSummaryDto summary = createSampleSummary();
            byte[] xlsxBytes = exportService.generateDashboardZip(summary);

            assertNotNull(xlsxBytes);
            assertTrue(xlsxBytes.length > 0);

            // Verificar que el XLSX contiene las hojas esperadas
            try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
                List<String> expectedSheets = List.of(
                        "KPIs Generales",
                        "Top Marcas",
                        "Stock Proveedores",
                        "Historial Sync",
                        "Top Productos Caros",
                        "Alertas Subidas Precio",
                        "Oportunidades Bajadas Precio",
                        "Productos Zombies"
                );

                for (String sheetName : expectedSheets) {
                    Sheet sheet = workbook.getSheet(sheetName);
                    assertNotNull(sheet, "Hoja esperada no encontrada: " + sheetName);
                }
            }
        }

        @Test
        @DisplayName("Debe generar hoja de KPIs correctamente")
        void shouldGenerateKpisSheet() throws Exception {
            DashboardSummaryDto summary = createSampleSummary();
            byte[] xlsxBytes = exportService.generateDashboardZip(summary);

            try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
                Sheet sheet = workbook.getSheet("KPIs Generales");
                assertNotNull(sheet);

                // Verificar contenido de KPIs
                boolean foundTotalProducts = false;
                boolean foundTotalSuppliers = false;
                for (Row row : sheet) {
                    String cellValue = getCellValueAsString(row.getCell(0));
                    if ("Total Productos".equals(cellValue)) {
                        assertEquals("100", getCellValueAsString(row.getCell(1)));
                        foundTotalProducts = true;
                    }
                    if ("Total Proveedores".equals(cellValue)) {
                        assertEquals("5", getCellValueAsString(row.getCell(1)));
                        foundTotalSuppliers = true;
                    }
                }
                assertTrue(foundTotalProducts, "Debe contener Total Productos");
                assertTrue(foundTotalSuppliers, "Debe contener Total Proveedores");
            }
        }

        @Test
        @DisplayName("Debe generar hoja de Top Marcas")
        void shouldGenerateTopBrandsSheet() throws Exception {
            DashboardSummaryDto summary = createSampleSummary();
            byte[] xlsxBytes = exportService.generateDashboardZip(summary);

            try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
                Sheet sheet = workbook.getSheet("Top Marcas");
                assertNotNull(sheet);

                boolean foundBrandA = false;
                boolean foundBrandB = false;
                for (Row row : sheet) {
                    String cellValue = getCellValueAsString(row.getCell(0));
                    if ("BrandA".equals(cellValue)) {
                        assertEquals("50", getCellValueAsString(row.getCell(1)));
                        foundBrandA = true;
                    }
                    if ("BrandB".equals(cellValue)) {
                        assertEquals("30", getCellValueAsString(row.getCell(1)));
                        foundBrandB = true;
                    }
                }
                assertTrue(foundBrandA, "Debe contener BrandA");
                assertTrue(foundBrandB, "Debe contener BrandB");
            }
        }

        @Test
        @DisplayName("Debe generar hoja de Historial Sync")
        void shouldGenerateSyncHistorySheet() throws Exception {
            DashboardSummaryDto summary = createSampleSummary();
            byte[] xlsxBytes = exportService.generateDashboardZip(summary);

            try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
                Sheet sheet = workbook.getSheet("Historial Sync");
                assertNotNull(sheet);

                boolean foundFirstEntry = false;
                boolean foundSecondEntry = false;
                for (Row row : sheet) {
                    String cellValue = getCellValueAsString(row.getCell(0));
                    if ("01/01".equals(cellValue)) {
                        assertEquals("10", getCellValueAsString(row.getCell(1)));
                        assertEquals("2", getCellValueAsString(row.getCell(2)));
                        foundFirstEntry = true;
                    }
                    if ("02/01".equals(cellValue)) {
                        assertEquals("8", getCellValueAsString(row.getCell(1)));
                        assertEquals("1", getCellValueAsString(row.getCell(2)));
                        foundSecondEntry = true;
                    }
                }
                assertTrue(foundFirstEntry, "Debe contener entrada 01/01");
                assertTrue(foundSecondEntry, "Debe contener entrada 02/01");
            }
        }

        @Test
        @DisplayName("Debe generar hoja de Top Productos Caros")
        void shouldGenerateTopProductsSheet() throws Exception {
            DashboardSummaryDto summary = createSampleSummary();
            byte[] xlsxBytes = exportService.generateDashboardZip(summary);

            try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
                Sheet sheet = workbook.getSheet("Top Productos Caros");
                assertNotNull(sheet);

                boolean foundProductA = false;
                for (Row row : sheet) {
                    String cellValue = getCellValueAsString(row.getCell(1));
                    if ("Product A".equals(cellValue)) {
                        assertEquals("999.99", getCellValueAsString(row.getCell(3)));
                        foundProductA = true;
                    }
                }
                assertTrue(foundProductA, "Debe contener Product A");
            }
        }

        @Test
        @DisplayName("Debe generar hoja de Top Movers (subidas y bajadas)")
        void shouldGenerateTopMoversSheet() throws Exception {
            DashboardSummaryDto summary = createSampleSummary();
            byte[] xlsxBytes = exportService.generateDashboardZip(summary);

            try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
                Sheet subidas = workbook.getSheet("Alertas Subidas Precio");
                assertNotNull(subidas);
                boolean foundIncrease = false;
                for (Row row : subidas) {
                    String cellValue = getCellValueAsString(row.getCell(1));
                    if ("Product A".equals(cellValue)) {
                        assertEquals("50.0", getCellValueAsString(row.getCell(5)));
                        foundIncrease = true;
                    }
                }
                assertTrue(foundIncrease, "Debe contener subida de precio");

                Sheet bajadas = workbook.getSheet("Oportunidades Bajadas Precio");
                assertNotNull(bajadas);
                boolean foundDecrease = false;
                for (Row row : bajadas) {
                    String cellValue = getCellValueAsString(row.getCell(1));
                    if ("Product B".equals(cellValue)) {
                        assertEquals("-25.0", getCellValueAsString(row.getCell(5)));
                        foundDecrease = true;
                    }
                }
                assertTrue(foundDecrease, "Debe contener bajada de precio");
            }
        }

        @Test
        @DisplayName("Debe generar hoja de Productos Zombies")
        void shouldGenerateZombiesSheet() throws Exception {
            DashboardSummaryDto summary = createSampleSummary();
            byte[] xlsxBytes = exportService.generateDashboardZip(summary);

            try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
                Sheet sheet = workbook.getSheet("Productos Zombies");
                assertNotNull(sheet);

                boolean foundMpn001 = false;
                for (Row row : sheet) {
                    String cellValue = getCellValueAsString(row.getCell(1));
                    if ("MPN001".equals(cellValue)) {
                        assertEquals("30", getCellValueAsString(row.getCell(4)));
                        foundMpn001 = true;
                    }
                }
                assertTrue(foundMpn001, "Debe contener MPN001");
            }
        }

        @Test
        @DisplayName("Debe escapar valores con comas correctamente")
        void shouldEscapeCsvValues() throws Exception {
            DashboardSummaryDto summary = new DashboardSummaryDto(
                    1L, 1L, 1L, 0L, 0L, 0.0,
                    List.of(new ChartDataDto("Brand, Inc.", 10L)),
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
            );
            byte[] xlsxBytes = exportService.generateDashboardZip(summary);

            try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
                Sheet sheet = workbook.getSheet("Top Marcas");
                assertNotNull(sheet);

                boolean foundBrandWithComma = false;
                for (Row row : sheet) {
                    String cellValue = getCellValueAsString(row.getCell(0));
                    if ("Brand, Inc.".equals(cellValue)) {
                        assertEquals("10", getCellValueAsString(row.getCell(1)));
                        foundBrandWithComma = true;
                    }
                }
                assertTrue(foundBrandWithComma, "Debe contener Brand, Inc.");
            }
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}
