package com.omnistock.backend.service.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnistock.backend.dtos.SyncStatus;
import com.omnistock.backend.entity.MasterProduct;
import com.omnistock.backend.entity.Supplier;
import com.omnistock.backend.repository.MasterProductRepository;
import com.omnistock.backend.repository.SupplierRepository;
import com.omnistock.backend.service.integration.ApiClientService;
import com.omnistock.backend.service.integration.DeadLetterQueueService;
import com.omnistock.backend.service.notification.NotificationService;
import com.omnistock.backend.service.transformation.UniversalMapperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class StockCatalogSyncService {

    private static final Logger logger = LoggerFactory.getLogger(StockCatalogSyncService.class);

    private final MasterProductRepository masterProductRepository;
    private final SupplierRepository supplierRepository;
    private final ApiClientService apiClientService;
    private final UniversalMapperService universalMapperService;
    private final ProductSupplierService productSupplierService;
    private final SyncStateService syncStateService;
    private final NotificationService notificationService;
    private final DeadLetterQueueService deadLetterQueueService;
    private final ObjectMapper objectMapper;

    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    @Value("${sync.pagination.page-size:500}")
    private int pageSize;

    public StockCatalogSyncService(MasterProductRepository masterProductRepository,
                                   SupplierRepository supplierRepository,
                                   ApiClientService apiClientService,
                                   UniversalMapperService universalMapperService,
                                   ProductSupplierService productSupplierService,
                                   SyncStateService syncStateService,
                                   NotificationService notificationService,
                                   DeadLetterQueueService deadLetterQueueService,
                                   ObjectMapper objectMapper) {
        this.masterProductRepository = masterProductRepository;
        this.supplierRepository = supplierRepository;
        this.apiClientService = apiClientService;
        this.universalMapperService = universalMapperService;
        this.productSupplierService = productSupplierService;
        this.syncStateService = syncStateService;
        this.notificationService = notificationService;
        this.deadLetterQueueService = deadLetterQueueService;
        this.objectMapper = objectMapper;
    }

    public void syncAllProducts() {
        if (!syncStateService.tryAcquireLock()) {
            logger.warn("La sincronización ya está en progreso en otra réplica. Se omite esta solicitud.");
            return;
        }
        logger.info("=== INICIANDO SINCRONIZACIÓN TOTAL DE CATÁLOGOS ===");
        try {
            List<Supplier> suppliers = supplierRepository.findAllByActiveTrueWithMappings();
            List<CompletableFuture<Void>> futures = suppliers.stream()
                    .map(supplier -> CompletableFuture.runAsync(() -> syncProviderCatalog(supplier), executorService))
                    .toList();
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            logger.info("=== SINCRONIZACIÓN TOTAL DE CATÁLOGOS COMPLETADA ===");
            syncStateService.setStatus(SyncStatus.COMPLETED);
        } catch (Exception e) {
            logger.error("!!! ERROR CRÍTICO durante la sincronización total de catálogos", e);
            syncStateService.setStatus(SyncStatus.ERROR);
        }
    }

    /**
     * Sincroniza el catálogo de un proveedor con soporte de:
     * - Paginación (para catálogos de 100K+ productos)
     * - Sincronización parcial (cada producto se procesa en su propia transacción)
     * - Dead Letter Queue (productos que fallan se registran para reprocesar)
     */
    void syncProviderCatalog(Supplier supplier) {
        int page = 0;
        int totalProcessed = 0;
        int totalErrors = 0;
        boolean hasMore = true;

        while (hasMore) {
            try {
                String rawJson = apiClientService.fetchCatalog(supplier);
                List<UniversalMapperService.NormalizedData> normalizedList =
                        universalMapperService.normalizeResponse(rawJson, supplier);

                if (normalizedList.isEmpty()) {
                    logger.info("Catálogo vacío para proveedor {}.", supplier.getName());
                    break;
                }

                // Aplicar paginación en memoria sobre la lista normalizada
                int totalProducts = normalizedList.size();
                int fromIndex = page * pageSize;
                int toIndex = Math.min(fromIndex + pageSize, totalProducts);

                if (fromIndex >= totalProducts) {
                    hasMore = false;
                    continue;
                }

                List<UniversalMapperService.NormalizedData> pageData = normalizedList.subList(fromIndex, toIndex);
                logger.info("Procesando página {} de {} para {} (productos {}-{} de {})",
                        page + 1, (totalProducts / pageSize) + 1, supplier.getName(),
                        fromIndex + 1, toIndex, totalProducts);

                // Sincronización parcial: cada producto se procesa individualmente
                for (UniversalMapperService.NormalizedData data : pageData) {
                    try {
                        processSingleProduct(supplier, data);
                        totalProcessed++;
                    } catch (Exception e) {
                        totalErrors++;
                        logger.error("Error procesando producto {} de {}: {}",
                                data.mpn(), supplier.getName(), e.getMessage());
                    }
                }

                page++;
                if (toIndex >= totalProducts) {
                    hasMore = false;
                }

            } catch (Exception e) {
                logger.error("Error obteniendo catálogo del proveedor {} (página {}): {}",
                        supplier.getName(), page, e.getMessage(), e);
                // Si falla la obtención del catálogo completo, no podemos seguir paginando
                hasMore = false;
            }
        }

        logger.info("Proveedor {} sincronizado. Procesados: {}, Errores: {}",
                supplier.getName(), totalProcessed, totalErrors);
    }

    /**
     * Procesa un único producto normalizado con try-catch individual (partial sync).
     * Si falla, lo registra en la Dead Letter Queue.
     */
    private void processSingleProduct(Supplier supplier, UniversalMapperService.NormalizedData data) {
        if (data.mpn() == null || data.mpn().isBlank()) {
            return;
        }

        try {
            Optional<MasterProduct> existingMaster = masterProductRepository.findByMpn(data.mpn());
            boolean isNewMaster = existingMaster.isEmpty();

            MasterProduct master = existingMaster.orElseGet(() -> {
                MasterProduct mp = new MasterProduct();
                mp.setMpn(data.mpn());
                mp.setBrand(data.brand());
                mp.setModel(data.model());
                mp.setCategory(data.category());
                mp.setTechSpecs(data.techSpecs());
                return mp;
            });

            if (data.brand() != null) {
                master.setBrand(data.brand());
            }
            if (data.model() != null) {
                master.setModel(data.model());
            }
            if (data.category() != null && !data.category().isBlank()) {
                master.setCategory(data.category());
            }

            if (data.techSpecs() != null && !data.techSpecs().isBlank()) {
                try {
                    Map<String, String> existingSpecs = master.getTechSpecs() != null && !master.getTechSpecs().isBlank()
                            ? objectMapper.readValue(master.getTechSpecs(), new TypeReference<Map<String, String>>() {})
                            : Map.of();

                    Map<String, String> newSpecs = objectMapper.readValue(data.techSpecs(), new TypeReference<Map<String, String>>() {});

                    Map<String, String> merged = existingSpecs.entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    merged.putAll(newSpecs);
                    master.setTechSpecs(objectMapper.writeValueAsString(merged));
                } catch (IOException e) {

                    master.setTechSpecs(data.techSpecs());
                }
            }

            MasterProduct savedMaster = masterProductRepository.save(master);
            if (isNewMaster) {
                notificationService.createNewProductGlobalNotification(savedMaster, supplier);
            }
            productSupplierService.upsertProductSupplier(savedMaster, supplier, data);

        } catch (Exception e) {
            // Registrar en Dead Letter Queue para reprocesamiento posterior
            String errorType = classifyError(e);
            String rawPayload = serializeRawPayload(data);
            deadLetterQueueService.recordFailure(
                    supplier,
                    data.mpn(),
                    rawPayload,
                    e.getMessage(),
                    errorType
            );
            logger.warn("📥 Producto {} enviado a DLQ (tipo: {})", data.mpn(), errorType);
            // NO relanzamos la excepción — esto permite que el resto del catálogo se siga procesando
        }
    }

    /**
     * Clasifica el tipo de error para la Dead Letter Queue.
     */
    private String classifyError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        if (msg.contains("timeout") || msg.contains("timed out")) {
            return "TIMEOUT";
        } else if (msg.contains("parse") || msg.contains("unexpected character") || msg.contains("json")) {
            return "PARSING";
        } else if (msg.contains("mapping") || msg.contains("field") || msg.contains("transform")) {
            return "MAPPING";
        } else {
            return "UNKNOWN";
        }
    }

    /**
     * Serializa los datos normalizados a JSON para almacenarlos en la DLQ.
     */
    private String serializeRawPayload(UniversalMapperService.NormalizedData data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return "{\"mpn\":\"" + (data.mpn() != null ? data.mpn() : "unknown") + "\"}";
        }
    }
}

