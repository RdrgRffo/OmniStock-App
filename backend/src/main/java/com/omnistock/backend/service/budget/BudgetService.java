package com.omnistock.backend.service.budget;

import com.omnistock.backend.dtos.budget.BudgetItemDto;
import com.omnistock.backend.dtos.budget.BudgetLineDto;
import com.omnistock.backend.dtos.budget.BudgetRequestDto;
import com.omnistock.backend.dtos.budget.BudgetResponseDto;
import com.omnistock.backend.entity.Budget;
import com.omnistock.backend.entity.BudgetLine;
import com.omnistock.backend.entity.MasterProduct;
import com.omnistock.backend.entity.ProductSupplier;
import com.omnistock.backend.entity.Supplier;
import com.omnistock.backend.repository.BudgetLineRepository;
import com.omnistock.backend.repository.BudgetRepository;
import com.omnistock.backend.repository.MasterProductRepository;
import com.omnistock.backend.repository.ProductSupplierRepository;
import com.omnistock.backend.repository.SupplierRepository;
import com.omnistock.backend.service.notification.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Servicio para simular y gestionar presupuestos.
 * Ahora con persistencia en BD (entidades Budget + BudgetLine) y notificaciones.
 */
@Service
public class BudgetService {

    private static final Logger log = LoggerFactory.getLogger(BudgetService.class);

    private final MasterProductRepository productRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final SupplierRepository supplierRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetLineRepository budgetLineRepository;
    private final NotificationService notificationService;

    public BudgetService(MasterProductRepository productRepository,
                         ProductSupplierRepository productSupplierRepository,
                         SupplierRepository supplierRepository,
                         BudgetRepository budgetRepository,
                         BudgetLineRepository budgetLineRepository,
                         NotificationService notificationService) {
        this.productRepository = productRepository;
        this.productSupplierRepository = productSupplierRepository;
        this.supplierRepository = supplierRepository;
        this.budgetRepository = budgetRepository;
        this.budgetLineRepository = budgetLineRepository;
        this.notificationService = notificationService;
    }

    /**
     * Simula un presupuesto a partir de una lista de líneas (producto + proveedor + cantidad).
     * Busca los precios actuales en ProductSupplier, calcula el total y PERSISTE en BD.
     */
    @Transactional
    public BudgetResponseDto simulateBudget(BudgetRequestDto request, String username) {
        log.info("Simulando presupuesto '{}' para usuario '{}' con {} líneas",
                request.getBudgetName(), username, request.getLines().size());

        List<BudgetItemDto> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (BudgetLineDto line : request.getLines()) {
            BudgetItemDto item = buildBudgetItem(line);
            if (item != null) {
                items.add(item);
                BigDecimal lineTotal = item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()));
                totalAmount = totalAmount.add(lineTotal);
            }
        }

        String budgetNumber = generateBudgetNumber();
        LocalDateTime now = LocalDateTime.now();

        // Crear y persistir la entidad Budget
        Budget budget = new Budget(budgetNumber, request.getBudgetName(), request.getNotes(),
                "DRAFT", username, now, now, totalAmount);

        for (BudgetItemDto item : items) {
            BudgetLine line = new BudgetLine(
                    item.getProductId(), item.getMpn(), item.getProductName(),
                    item.getBrand(), item.getModel(),
                    item.getSupplierId(), item.getSupplierName(),
                    item.getQuantity(), item.getStockAvailable(),
                    item.getUnitPrice(), item.getRetailPrice(),
                    item.getProductUrl(), item.getNotes()
            );
            budget.addLine(line);
        }

        budget = budgetRepository.save(budget);

        // Notificar simulación de presupuesto
        notificationService.createGeneralNotification(
                "Presupuesto simulado",
                "Presupuesto " + budgetNumber + " simulado por " + username,
                "BUDGET_SIMULATE|" + username + "|" + budgetNumber
        );

        log.info("Presupuesto '{}' persistido: id={}, número={}, total={}, items={}",
                request.getBudgetName(), budget.getId(), budgetNumber, totalAmount, items.size());

        return toResponseDto(budget, items);
    }

    /**
     * Obtiene todos los presupuestos de un usuario.
     */
    public List<BudgetResponseDto> getUserBudgets(String username) {
        List<Budget> budgets = budgetRepository.findByCreatedByOrderByCreatedAtDesc(username);
        return budgets.stream()
                .map(b -> toResponseDto(b, getItemsForBudget(b)))
                .toList();
    }

    /**
     * Obtiene un presupuesto por su ID.
     */
    public BudgetResponseDto getBudgetById(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Presupuesto no encontrado: " + id));
        return toResponseDto(budget, getItemsForBudget(budget));
    }

    /**
     * Cambia el estado de un presupuesto.
     */
    @Transactional
    public BudgetResponseDto updateBudgetStatus(Long id, String newStatus) {
        if (!List.of("DRAFT", "FINALIZED", "EXPORTED").contains(newStatus)) {
            throw new IllegalArgumentException("Estado inválido: " + newStatus);
        }

        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Presupuesto no encontrado: " + id));

        String oldStatus = budget.getStatus();
        budget.setStatus(newStatus);
        budget.setUpdatedAt(LocalDateTime.now());
        budget = budgetRepository.save(budget);

        // Notificar cambio de estado
        notificationService.createGeneralNotification(
                "Estado de presupuesto actualizado",
                "Presupuesto " + budget.getBudgetNumber() + " cambió de " + oldStatus + " a " + newStatus,
                "BUDGET_STATUS|" + budget.getBudgetNumber() + "|" + newStatus
        );

        log.info("Presupuesto {} cambiado de estado: {} -> {}", budget.getBudgetNumber(), oldStatus, newStatus);
        return toResponseDto(budget, getItemsForBudget(budget));
    }

    /**
     * Elimina un presupuesto.
     */
    @Transactional
    public void deleteBudget(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Presupuesto no encontrado: " + id));
        budgetRepository.delete(budget);
        log.info("Presupuesto {} eliminado", budget.getBudgetNumber());
    }

    /**
     * Notifica que un presupuesto ha sido exportado a Excel.
     */
    @Transactional
    public void notifyBudgetExported(Long id, String username) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Presupuesto no encontrado: " + id));

        notificationService.createGeneralNotification(
                "Presupuesto exportado",
                "Presupuesto " + budget.getBudgetNumber() + " exportado a Excel por " + username,
                "BUDGET_EXPORT|" + budget.getBudgetNumber()
        );

        // Si está en DRAFT, pasar a EXPORTED automáticamente
        if ("DRAFT".equals(budget.getStatus()) || "FINALIZED".equals(budget.getStatus())) {
            budget.setStatus("EXPORTED");
            budget.setUpdatedAt(LocalDateTime.now());
            budgetRepository.save(budget);
        }
    }

    /**
     * Construye un BudgetItemDto buscando producto, proveedor y precio actual.
     */
    private BudgetItemDto buildBudgetItem(BudgetLineDto line) {
        Optional<MasterProduct> productOpt = productRepository.findById(line.getProductId());
        if (productOpt.isEmpty()) {
            log.warn("Producto {} no encontrado, saltando línea", line.getProductId());
            return null;
        }

        Optional<Supplier> supplierOpt = supplierRepository.findById(line.getSupplierId());
        if (supplierOpt.isEmpty()) {
            log.warn("Proveedor {} no encontrado, saltando línea", line.getSupplierId());
            return null;
        }

        MasterProduct product = productOpt.get();
        Supplier supplier = supplierOpt.get();

        Optional<ProductSupplier> psOpt = productSupplierRepository
                .findByMasterProductIdAndSupplierId(product.getId(), supplier.getId());

        BigDecimal unitPrice = BigDecimal.ZERO;
        BigDecimal retailPrice = null;
        Integer stockAvailable = 0;

        if (psOpt.isPresent()) {
            ProductSupplier ps = psOpt.get();
            unitPrice = ps.getPrice() != null ? ps.getPrice() : BigDecimal.ZERO;
            retailPrice = ps.getRetailPrice();
            stockAvailable = ps.getStock() != null ? ps.getStock() : 0;
        }

        int quantity = Math.min(line.getQuantity(), Math.max(0, stockAvailable));

        return new BudgetItemDto(
                product.getId(), product.getMpn(),
                product.getBrand() + " " + product.getModel(),
                product.getBrand(), product.getModel(),
                supplier.getId(), supplier.getName(),
                quantity, stockAvailable, unitPrice, retailPrice,
                null, line.getNotes()
        );
    }

    /**
     * Genera un número de presupuesto con formato PRES-YYYYMMDD-NNN.
     */
    private String generateBudgetNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = ThreadLocalRandom.current().nextInt(1, 1000);
        return String.format("PRES-%s-%03d", datePart, seq);
    }

    /**
     * Obtiene las líneas de un presupuesto como DTOs.
     */
    private List<BudgetItemDto> getItemsForBudget(Budget budget) {
        return budget.getLines().stream()
                .map(this::toItemDto)
                .toList();
    }

    /**
     * Convierte una entidad BudgetLine a BudgetItemDto.
     */
    private BudgetItemDto toItemDto(BudgetLine line) {
        return new BudgetItemDto(
                line.getProductId(), line.getMpn(), line.getProductName(),
                line.getBrand(), line.getModel(),
                line.getSupplierId(), line.getSupplierName(),
                line.getQuantity(), line.getStockAvailable(),
                line.getUnitPrice(), line.getRetailPrice(),
                line.getProductUrl(), line.getNotes()
        );
    }

    /**
     * Convierte una entidad Budget + items a BudgetResponseDto.
     */
    private BudgetResponseDto toResponseDto(Budget budget, List<BudgetItemDto> items) {
        BudgetResponseDto dto = new BudgetResponseDto();
        dto.setId(budget.getId());
        dto.setBudgetNumber(budget.getBudgetNumber());
        dto.setBudgetName(budget.getBudgetName());
        dto.setNotes(budget.getNotes());
        dto.setStatus(budget.getStatus());
        dto.setCreatedBy(budget.getCreatedBy());
        dto.setCreatedAt(budget.getCreatedAt());
        dto.setUpdatedAt(budget.getUpdatedAt());
        dto.setTotalAmount(budget.getTotalAmount());
        dto.setItems(items);
        return dto;
    }
}
