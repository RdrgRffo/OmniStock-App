package com.omnistock.backend.controller;

import com.omnistock.backend.dtos.pricing.PriceHistoryRequestDto;
import com.omnistock.backend.dtos.pricing.PriceHistoryResponseDto;
import com.omnistock.backend.dtos.pricing.PriceStatisticsDto;
import com.omnistock.backend.entity.MasterProduct;
import com.omnistock.backend.entity.PriceHistory;
import com.omnistock.backend.entity.Supplier;
import com.omnistock.backend.service.product.MasterProductService;
import com.omnistock.backend.service.pricing.PriceHistoryService;
import com.omnistock.backend.service.supplier.SupplierService;
import com.omnistock.backend.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controlador REST para el historial de precios y análisis BI.
 * Rutas bajo /api/v1/historial-precios por compatibilidad con el frontend.
 * Mensajes de error en español.
 */
@RestController
@RequestMapping("/api/v1/historial-precios")
@Tag(name = "Historial de Precios - BI", description = "Análisis de precios y Business Intelligence")
public class PriceHistoryController {

    private final PriceHistoryService priceHistoryService;
    private final MasterProductService masterProductService;
    private final SupplierService supplierService;

    public PriceHistoryController(PriceHistoryService priceHistoryService,
                                  MasterProductService masterProductService,
                                  SupplierService supplierService) {
        this.priceHistoryService = priceHistoryService;
        this.masterProductService = masterProductService;
        this.supplierService = supplierService;
    }

    @GetMapping("/producto/{productoId}/proveedor/{proveedorId}")
    @Operation(summary = "Obtener historial de precios")
    public ResponseEntity<ApiResponse<List<PriceHistoryResponseDto>>> getHistoryByProductAndSupplier(
            @Parameter(description = "ID del producto maestro") @PathVariable Integer productoId,
            @Parameter(description = "ID del proveedor") @PathVariable Integer proveedorId) {
        MasterProduct product = masterProductService.getById(productoId);
        Supplier supplier = supplierService.getById(proveedorId);
        List<PriceHistory> history = priceHistoryService.getHistoryForProduct(product, supplier);
        List<PriceHistoryResponseDto> response = history.stream().map(this::toResponseDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(response, "Historial obtenido exitosamente"));
    }

    @GetMapping("/producto/{productoId}")
    @Operation(summary = "Obtener historial de precios de un producto")
    public ResponseEntity<ApiResponse<List<PriceHistoryResponseDto>>> getHistoryByProduct(
            @Parameter(description = "ID del producto maestro") @PathVariable Integer productoId) {
        MasterProduct product = masterProductService.getById(productoId);
        List<PriceHistory> history = priceHistoryService.getHistoryForProduct(product, null);
        List<PriceHistoryResponseDto> response = history.stream().map(this::toResponseDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(response, "Historial obtenido exitosamente"));
    }

    @GetMapping("/producto/{productoId}/proveedor/{proveedorId}/analytics")
    @Operation(summary = "Obtener análisis de precios (BI)")
    public ResponseEntity<ApiResponse<PriceStatisticsDto>> getAnalytics(
            @Parameter(description = "ID del producto maestro") @PathVariable Integer productoId,
            @Parameter(description = "ID del proveedor") @PathVariable Integer proveedorId) {
        MasterProduct product = masterProductService.getById(productoId);
        Supplier supplier = supplierService.getById(proveedorId);
        List<PriceHistory> history = priceHistoryService.getHistoryForProduct(product, supplier);
        PriceStatisticsDto stats = priceHistoryService.computeStatistics(history, product, supplier);
        return ResponseEntity.ok(ApiResponse.success(stats, "Análisis obtenido exitosamente"));
    }

    @GetMapping("/producto/{productoId}/comparativa-proveedores")
    @Operation(summary = "Comparativa de precios entre proveedores")
    public ResponseEntity<ApiResponse<Map<String, PriceStatisticsDto>>> getSupplierComparison(
            @Parameter(description = "ID del producto maestro") @PathVariable Integer productoId) {
        MasterProduct product = masterProductService.getById(productoId);
        List<PriceHistory> all = priceHistoryService.getHistoryForProduct(product, null);
        Map<String, PriceStatisticsDto> comparativa = all.stream()
                .collect(Collectors.groupingBy(h -> h.getSupplier().getName(), Collectors.toList()))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            Supplier s = entry.getValue().stream()
                                    .findFirst()
                                    .map(PriceHistory::getSupplier)
                                    .orElse(null);
                            return priceHistoryService.computeStatistics(entry.getValue(), product, s);
                        }));
        return ResponseEntity.ok(ApiResponse.success(comparativa, "Comparativa obtenida exitosamente"));
    }

    @GetMapping("/producto/{productoId}/proveedor/{proveedorId}/por-mes")
    @Operation(summary = "Historial de precios agrupado por mes")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> getHistoryByMonth(
            @Parameter(description = "ID del producto maestro") @PathVariable Integer productoId,
            @Parameter(description = "ID del proveedor") @PathVariable Integer proveedorId) {
        MasterProduct product = masterProductService.getById(productoId);
        Supplier supplier = supplierService.getById(proveedorId);
        List<PriceHistory> history = priceHistoryService.getHistoryForProduct(product, supplier);
        Map<String, BigDecimal> porMes = history.stream()
                .filter(h -> h.getCostPrice() != null)
                .collect(Collectors.groupingBy(
                        h -> YearMonth.from(h.getRegisteredAt()).toString(),
                        Collectors.mapping(
                                PriceHistory::getCostPrice,
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        list -> {
                                            if (list.isEmpty()) return BigDecimal.ZERO;
                                            BigDecimal sum = list.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                                            return sum.divide(BigDecimal.valueOf(list.size()), 2, RoundingMode.HALF_UP);
                                        }))))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
        return ResponseEntity.ok(ApiResponse.success(porMes, "Histórico por mes obtenido exitosamente"));
    }

    @PostMapping("/registrar")
    @Operation(summary = "Registrar cambio de precio manualmente")
    public ResponseEntity<ApiResponse<PriceHistoryResponseDto>> registerPriceChange(
            @Valid @RequestBody PriceHistoryRequestDto request) {
        MasterProduct product = masterProductService.getById(request.masterProductId());
        Supplier supplier = supplierService.getById(request.supplierId());
        priceHistoryService.registerPriceChange(product, supplier, request.costPrice());
        List<PriceHistory> history = priceHistoryService.getHistoryForProduct(product, supplier);
        PriceHistory last = history.get(0);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(toResponseDto(last), "Precio registrado exitosamente"));
    }

    @GetMapping("/producto/{productoId}/proveedor/{proveedorId}/ultimo-precio")
    @Operation(summary = "Obtener último precio registrado")
    public ResponseEntity<ApiResponse<PriceHistoryResponseDto>> getLastPrice(
            @Parameter(description = "ID del producto maestro") @PathVariable Integer productoId,
            @Parameter(description = "ID del proveedor") @PathVariable Integer proveedorId) {
        MasterProduct product = masterProductService.getById(productoId);
        Supplier supplier = supplierService.getById(proveedorId);
        List<PriceHistory> history = priceHistoryService.getHistoryForProduct(product, supplier);
        if (history.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error("No hay histórico de precios para este producto y proveedor"));
        }
        return ResponseEntity.ok(ApiResponse.success(toResponseDto(history.get(0)), "Último precio obtenido"));
    }

    @GetMapping("/producto/{productoId}/proveedor/{proveedorId}/tendencia")
    @Operation(summary = "Obtener tendencia de precios")
    public ResponseEntity<ApiResponse<Map<String, String>>> getTrend(
            @Parameter(description = "ID del producto maestro") @PathVariable Integer productoId,
            @Parameter(description = "ID del proveedor") @PathVariable Integer proveedorId,
            @Parameter(description = "Número de días a analizar (por defecto 30)") @RequestParam(defaultValue = "30") Integer dias) {
        MasterProduct product = masterProductService.getById(productoId);
        Supplier supplier = supplierService.getById(proveedorId);
        List<PriceHistory> history = priceHistoryService.getHistoryForProduct(product, supplier);
        LocalDateTime limit = LocalDateTime.now().minusDays(dias);
        List<PriceHistory> recent = history.stream().filter(h -> h.getRegisteredAt().isAfter(limit)).collect(Collectors.toList());
        String tendencia = "ESTABLE =";
        if (recent.size() >= 2) {
            BigDecimal oldestPrice = recent.get(recent.size() - 1).getCostPrice();
            tendencia = recent.get(0).computeTrend(oldestPrice);
        }
        Map<String, String> response = new HashMap<>();
        response.put("tendencia", tendencia);
        response.put("dias", String.valueOf(dias));
        response.put("registrosAnalizados", String.valueOf(recent.size()));
        return ResponseEntity.ok(ApiResponse.success(response, "Tendencia obtenida exitosamente"));
    }

    private PriceHistoryResponseDto toResponseDto(PriceHistory h) {
        return new PriceHistoryResponseDto(
                h.getHistoryId(),
                h.getMasterProduct().getId(),
                h.getMasterProduct().getModel(),
                h.getSupplier().getId(),
                h.getSupplier().getName(),
                h.getCostPrice(),
                h.getRegisteredAt());
    }

}
