package com.omnistock.backend.seeder;

import com.omnistock.backend.entity.MasterProduct;
import com.omnistock.backend.entity.PriceHistory;
import com.omnistock.backend.entity.ProductSupplier;
import com.omnistock.backend.entity.StockHistory;
import com.omnistock.backend.repository.MasterProductRepository;
import com.omnistock.backend.repository.PriceHistoryRepository;
import com.omnistock.backend.repository.ProductSupplierRepository;
import com.omnistock.backend.repository.StockHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * Seeder que genera datos históricos simulados para poblar los gráficos de Analytics.
 * <p>
 * Se ejecuta después de {@link StartupDataSynchronizer} (Order 5) y solo si la base de datos
 * ya contiene productos sincronizados. Genera:
 * <ul>
 *   <li><b>PriceHistory</b> (historial_precios): 6-12 meses de precios con variaciones realistas</li>
 *   <li><b>StockHistory</b> (stock_history): 20-40 cambios de stock simulando patrones de venta/inconsistencia</li>
 *   <li><b>fechaCreacion</b> distribuida en semanas para Catalog Growth</li>
 * </ul>
 * <p>
 * Diseñado para ser idempotente: si ya existen datos históricos, no los regenera.
 */
@Component
@Profile("!test")
@Order(6)
public class AnalyticsDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsDataSeeder.class);

    private static final Random RANDOM = new Random(42L); // Semilla fija para reproducibilidad

    // Configuración PriceHistory
    private static final int PRICE_HISTORY_MONTHS = 8;       // 8 meses hacia atrás
    private static final double MAX_VARIATION = 0.15;         // ±15% normal
    private static final double OUTLIER_PROBABILITY = 0.08;   // 8% de ser outlier
    private static final double OUTLIER_MIN = 0.25;           // +25% mínimo como outlier
    private static final double OUTLIER_MAX = 0.45;           // +45% máximo como outlier

    // Configuración StockHistory
    private static final int STOCK_HISTORY_ENTRIES_MIN = 20;
    private static final int STOCK_HISTORY_ENTRIES_MAX = 40;
    private static final int STOCK_HISTORY_DAYS_BACK = 30;    // Últimos 30 días

    // Configuración Catalog Growth
    private static final int CATALOG_GROWTH_WEEKS = 12;       // Distribuir en 12 semanas

    private final MasterProductRepository masterProductRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final StockHistoryRepository stockHistoryRepository;

    public AnalyticsDataSeeder(MasterProductRepository masterProductRepository,
                               ProductSupplierRepository productSupplierRepository,
                               PriceHistoryRepository priceHistoryRepository,
                               StockHistoryRepository stockHistoryRepository) {
        this.masterProductRepository = masterProductRepository;
        this.productSupplierRepository = productSupplierRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.stockHistoryRepository = stockHistoryRepository;
    }

    @Override
    public void run(String... args) throws InterruptedException {
        log.info("=== AnalyticsDataSeeder: Verificando si es necesario generar datos históricos... ===");

        // Idempotencia: si ya hay price_history, no regeneramos
        if (priceHistoryRepository.count() > 0) {
            log.info("Ya existen {} registros en historial_precios. Se salta la generación.", priceHistoryRepository.count());
            return;
        }

        // Esperar a que la sincronización asíncrona de catálogos termine
        // (StartupDataSynchronizer lanza sync con @Async, puede tardar varios segundos)
        // NOTA: NO usamos @Transactional aquí para evitar que la transacción del seeder
        // impida ver los datos insertados por otras transacciones (las de la sincronización).
        // También esperamos a que existan ProductSupplier con precio > 0, ya que la
        // sincronización asíncrona puede crear MasterProduct pero aún no los ProductSupplier.
        List<MasterProduct> products = List.of();
        int maxRetries = 60; // 60 * 2s = 120 segundos máximo de espera
        int retryDelay = 2000; // 2 segundos entre reintentos

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            products = masterProductRepository.findAll();
            if (!products.isEmpty()) {
                // Verificar que también existan ProductSupplier con precio
                long suppliersWithPrice = productSupplierRepository.countByPriceGreaterThan(BigDecimal.ZERO);
                if (suppliersWithPrice > 0) {
                    log.info("Productos maestros encontrados en intento {}/{}: {} productos, {} suppliers con precio",
                            attempt, maxRetries, products.size(), suppliersWithPrice);
                    break;
                }
                log.info("Esperando suppliers con precio... intento {}/{} ({} productos encontrados, {} suppliers con precio)",
                        attempt, maxRetries, products.size(), suppliersWithPrice);
            } else {
                log.info("Esperando productos maestros... intento {}/{}", attempt, maxRetries);
            }
            Thread.sleep(retryDelay);
        }

        if (products.isEmpty()) {
            log.warn("No hay productos maestros después de {} intentos. Se omite AnalyticsDataSeeder.", maxRetries);
            return;
        }

        // Verificar si hay suppliers con precio; si no, abortar
        long suppliersWithPrice = productSupplierRepository.countByPriceGreaterThan(BigDecimal.ZERO);
        if (suppliersWithPrice == 0) {
            log.warn("No hay ProductSupplier con precio después de {} intentos. Se omite AnalyticsDataSeeder.", maxRetries);
            return;
        }


        log.info("Generando datos históricos para {} productos maestros...", products.size());

        // 1. Generar PriceHistory
        generatePriceHistory(products);

        // 2. Generar StockHistory
        generateStockHistory(products);

        // 3. Distribuir fechaCreacion para Catalog Growth
        distributeCreationDates(products);

        log.info("=== AnalyticsDataSeeder: Datos históricos generados correctamente ===");
    }

    // =========================================================================
    // 1. Price History — 8 meses de datos con variaciones realistas
    // =========================================================================

    private void generatePriceHistory(List<MasterProduct> products) {
        int totalGenerated = 0;
        LocalDateTime now = LocalDateTime.now();

        for (MasterProduct product : products) {
            List<ProductSupplier> suppliers = productSupplierRepository.findByMasterProductId(product.getId());
            if (suppliers.isEmpty()) continue;

            for (ProductSupplier ps : suppliers) {
                BigDecimal basePrice = ps.getPrice();
                if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) continue;

                // Generar 1 entry por mes durante PRICE_HISTORY_MONTHS meses
                BigDecimal currentPrice = basePrice;
                for (int monthOffset = PRICE_HISTORY_MONTHS - 1; monthOffset >= 0; monthOffset--) {
                    LocalDateTime entryDate = now.minusMonths(monthOffset)
                            .withDayOfMonth(1)
                            .withHour(10).withMinute(0).withSecond(0).withNano(0);

                    // Variación normal
                    double variation = (RANDOM.nextDouble() * 2 - 1) * MAX_VARIATION;

                    // Posible outlier (pico de precio)
                    if (RANDOM.nextDouble() < OUTLIER_PROBABILITY) {
                        variation = OUTLIER_MIN + RANDOM.nextDouble() * (OUTLIER_MAX - OUTLIER_MIN);
                    }

                    BigDecimal newPrice = currentPrice.multiply(BigDecimal.valueOf(1.0 + variation))
                            .setScale(2, RoundingMode.HALF_UP);

                    // No permitir precios negativos o cero
                    if (newPrice.compareTo(BigDecimal.ONE) < 0) {
                        newPrice = BigDecimal.ONE;
                    }

                    PriceHistory ph = new PriceHistory(product, ps.getSupplier(), newPrice);
                    ph.setRegisteredAt(entryDate);
                    priceHistoryRepository.save(ph);
                    totalGenerated++;

                    currentPrice = newPrice;
                }

                // Generar entries adicionales semanales en los últimos 2 meses (más densidad)
                for (int weekOffset = 1; weekOffset <= 8; weekOffset++) {
                    LocalDateTime weeklyDate = now.minusWeeks(weekOffset)
                            .withHour(14).withMinute(30).withSecond(0).withNano(0);

                    // Pequeña variación adicional
                    double microVar = (RANDOM.nextDouble() * 2 - 1) * 0.03; // ±3%
                    BigDecimal weeklyPrice = currentPrice.multiply(BigDecimal.valueOf(1.0 + microVar))
                            .setScale(2, RoundingMode.HALF_UP);

                    if (weeklyPrice.compareTo(BigDecimal.ONE) < 0) {
                        weeklyPrice = BigDecimal.ONE;
                    }

                    PriceHistory ph = new PriceHistory(product, ps.getSupplier(), weeklyPrice);
                    ph.setRegisteredAt(weeklyDate);
                    priceHistoryRepository.save(ph);
                    totalGenerated++;
                }
            }
        }

        log.info("PriceHistory: {} registros generados para {} productos", totalGenerated, products.size());
    }

    // =========================================================================
    // 2. Stock History — cambios simulados en los últimos 30 días
    // =========================================================================

    private void generateStockHistory(List<MasterProduct> products) {
        int totalGenerated = 0;
        LocalDateTime now = LocalDateTime.now();

        for (MasterProduct product : products) {
            List<ProductSupplier> suppliers = productSupplierRepository.findByMasterProductId(product.getId());
            if (suppliers.isEmpty()) continue;

            for (ProductSupplier ps : suppliers) {
                int baseStock = ps.getStock() != null ? ps.getStock() : 0;
                if (baseStock <= 0) continue;

                int numEntries = STOCK_HISTORY_ENTRIES_MIN
                        + RANDOM.nextInt(STOCK_HISTORY_ENTRIES_MAX - STOCK_HISTORY_ENTRIES_MIN + 1);

                int currentStock = baseStock;
                LocalDateTime lastEntry = now.minusDays(STOCK_HISTORY_DAYS_BACK);

                for (int i = 0; i < numEntries; i++) {
                    // Distribuir entries en los últimos 30 días
                    long randomMinutes = (long) (RANDOM.nextDouble() * STOCK_HISTORY_DAYS_BACK * 24 * 60);
                    LocalDateTime entryDate = now.minusMinutes(randomMinutes)
                            .withSecond(0).withNano(0);

                    // Asegurar orden cronológico aproximado
                    if (entryDate.isBefore(lastEntry)) {
                        entryDate = lastEntry.plusHours(1 + RANDOM.nextInt(6));
                    }
                    if (entryDate.isAfter(now)) {
                        entryDate = now.minusMinutes(1);
                    }

                    // Simular cambios de stock
                    int newStock;
                    double changeType = RANDOM.nextDouble();

                    if (changeType < 0.15) {
                        // 15%: caída a 0 (simula error de API o venta total)
                        newStock = 0;
                    } else if (changeType < 0.35) {
                        // 20%: caída significativa (venta probable)
                        newStock = Math.max(0, currentStock - (int)(currentStock * (0.3 + RANDOM.nextDouble() * 0.5)));
                    } else if (changeType < 0.60) {
                        // 25%: subida (reaprovisionamiento)
                        newStock = currentStock + (int)(baseStock * (0.1 + RANDOM.nextDouble() * 0.3));
                    } else if (changeType < 0.80) {
                        // 20%: pequeña fluctuación
                        newStock = currentStock + (RANDOM.nextBoolean() ? 1 : -1) * (1 + RANDOM.nextInt(3));
                        newStock = Math.max(0, newStock);
                    } else {
                        // 20%: recuperación desde 0 (simula inconsistencia API)
                        newStock = currentStock == 0
                                ? (int)(baseStock * (0.5 + RANDOM.nextDouble() * 0.5))
                                : currentStock + (int)(baseStock * 0.1);
                    }

                    // No permitir stock negativo
                    newStock = Math.max(0, newStock);

                    // Solo guardar si el stock cambió respecto al anterior
                    if (newStock != currentStock || i == 0) {
                        StockHistory sh = new StockHistory(product, ps.getSupplier(), newStock);
                        sh.setRecordedAt(entryDate);
                        stockHistoryRepository.save(sh);
                        totalGenerated++;
                        currentStock = newStock;
                        lastEntry = entryDate;
                    }
                }
            }
        }

        log.info("StockHistory: {} registros generados para {} productos", totalGenerated, products.size());
    }

    // =========================================================================
    // 3. Distribuir fechaCreacion para Catalog Growth
    // =========================================================================

    private void distributeCreationDates(List<MasterProduct> products) {
        int updated = 0;
        LocalDateTime now = LocalDateTime.now();

        for (MasterProduct product : products) {
            // Distribuir fechaCreacion en las últimas CATALOG_GROWTH_WEEKS semanas
            int weeksAgo = RANDOM.nextInt(CATALOG_GROWTH_WEEKS);
            int daysOffset = RANDOM.nextInt(7); // Día dentro de la semana
            LocalDateTime newCreationDate = now.minusWeeks(weeksAgo).minusDays(daysOffset)
                    .withHour(9).withMinute(0).withSecond(0).withNano(0);

            product.setFechaCreacion(newCreationDate);
            masterProductRepository.save(product);
            updated++;
        }

        log.info("CatalogGrowth: {} productos con fechaCreacion distribuida en {} semanas", updated, CATALOG_GROWTH_WEEKS);
    }
}
