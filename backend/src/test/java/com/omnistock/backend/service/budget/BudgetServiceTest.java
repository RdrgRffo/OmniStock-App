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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private MasterProductRepository productRepository;

    @Mock
    private ProductSupplierRepository productSupplierRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private BudgetLineRepository budgetLineRepository;

    @Mock
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<Budget> budgetCaptor;

    private BudgetService budgetService;

    private MasterProduct testProduct;
    private Supplier testSupplier;
    private ProductSupplier testProductSupplier;

    @BeforeEach
    void setUp() {
        budgetService = new BudgetService(productRepository, productSupplierRepository, supplierRepository,
                budgetRepository, budgetLineRepository, notificationService);

        testProduct = new MasterProduct();
        testProduct.setId(1);
        testProduct.setMpn("MPN-001");
        testProduct.setBrand("TestBrand");
        testProduct.setModel("TestModel");

        testSupplier = new Supplier();
        testSupplier.setId(1);
        testSupplier.setName("TestSupplier");

        testProductSupplier = new ProductSupplier();
        testProductSupplier.setMasterProduct(testProduct);
        testProductSupplier.setSupplier(testSupplier);
        testProductSupplier.setPrice(new BigDecimal("150.00"));
        testProductSupplier.setRetailPrice(new BigDecimal("250.00"));
        testProductSupplier.setStock(10);
    }

    @Nested
    @DisplayName("simulateBudget()")
    class SimulateBudget {

        @Test
        @DisplayName("Debe simular presupuesto correctamente con una línea válida")
        void shouldSimulateBudgetSuccessfully() {
            // Arrange
            BudgetLineDto line = new BudgetLineDto(1, 1, 5, "Nota de prueba");
            BudgetRequestDto request = new BudgetRequestDto("Presupuesto Test", "Notas generales", List.of(line));

            when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
            when(supplierRepository.findById(1)).thenReturn(Optional.of(testSupplier));
            when(productSupplierRepository.findByMasterProductIdAndSupplierId(1, 1))
                    .thenReturn(Optional.of(testProductSupplier));
            when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
                Budget b = invocation.getArgument(0);
                b.setId(1L);
                return b;
            });

            // Act
            BudgetResponseDto response = budgetService.simulateBudget(request, "testuser");

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getBudgetName()).isEqualTo("Presupuesto Test");
            assertThat(response.getNotes()).isEqualTo("Notas generales");
            assertThat(response.getStatus()).isEqualTo("DRAFT");
            assertThat(response.getCreatedBy()).isEqualTo("testuser");
            assertThat(response.getBudgetNumber()).startsWith("PRES-");
            assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("750.00")); // 150 * 5
            assertThat(response.getItems()).hasSize(1);

            BudgetItemDto item = response.getItems().get(0);
            assertThat(item.getProductId()).isEqualTo(1);
            assertThat(item.getMpn()).isEqualTo("MPN-001");
            assertThat(item.getProductName()).isEqualTo("TestBrand TestModel");
            assertThat(item.getBrand()).isEqualTo("TestBrand");
            assertThat(item.getModel()).isEqualTo("TestModel");
            assertThat(item.getSupplierId()).isEqualTo(1);
            assertThat(item.getSupplierName()).isEqualTo("TestSupplier");
            assertThat(item.getQuantity()).isEqualTo(5);
            assertThat(item.getStockAvailable()).isEqualTo(10);
            assertThat(item.getUnitPrice()).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(item.getRetailPrice()).isEqualByComparingTo(new BigDecimal("250.00"));
            assertThat(item.getNotes()).isEqualTo("Nota de prueba");

            verify(productRepository).findById(1);
            verify(supplierRepository).findById(1);
            verify(productSupplierRepository).findByMasterProductIdAndSupplierId(1, 1);
            verify(budgetRepository).save(any(Budget.class));
            verify(notificationService).createGeneralNotification(
                    anyString(), anyString(), anyString()
            );
        }

        @Test
        @DisplayName("Debe limitar cantidad al stock disponible")
        void shouldClampQuantityToStock() {
            // Arrange
            BudgetLineDto line = new BudgetLineDto(1, 1, 100, null); // stock=10, quantity=100
            BudgetRequestDto request = new BudgetRequestDto("Test", null, List.of(line));

            when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
            when(supplierRepository.findById(1)).thenReturn(Optional.of(testSupplier));
            when(productSupplierRepository.findByMasterProductIdAndSupplierId(1, 1))
                    .thenReturn(Optional.of(testProductSupplier));
            when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
                Budget b = invocation.getArgument(0);
                b.setId(1L);
                return b;
            });

            // Act
            BudgetResponseDto response = budgetService.simulateBudget(request, "user");

            // Assert
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getQuantity()).isEqualTo(10); // limitado al stock
            assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1500.00")); // 150 * 10
        }

        @Test
        @DisplayName("Debe saltar líneas con producto no encontrado")
        void shouldSkipLineWhenProductNotFound() {
            // Arrange
            BudgetLineDto line = new BudgetLineDto(999, 1, 5, null);
            BudgetRequestDto request = new BudgetRequestDto("Test", null, List.of(line));

            when(productRepository.findById(999)).thenReturn(Optional.empty());
            when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
                Budget b = invocation.getArgument(0);
                b.setId(1L);
                return b;
            });

            // Act
            BudgetResponseDto response = budgetService.simulateBudget(request, "user");

            // Assert
            assertThat(response.getItems()).isEmpty();
            assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Debe saltar líneas con proveedor no encontrado")
        void shouldSkipLineWhenSupplierNotFound() {
            // Arrange
            BudgetLineDto line = new BudgetLineDto(1, 999, 5, null);
            BudgetRequestDto request = new BudgetRequestDto("Test", null, List.of(line));

            when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
            when(supplierRepository.findById(999)).thenReturn(Optional.empty());
            when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
                Budget b = invocation.getArgument(0);
                b.setId(1L);
                return b;
            });

            // Act
            BudgetResponseDto response = budgetService.simulateBudget(request, "user");

            // Assert
            assertThat(response.getItems()).isEmpty();
            assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Debe manejar líneas sin ProductSupplier (precio cero)")
        void shouldHandleLineWithoutProductSupplier() {
            // Arrange
            BudgetLineDto line = new BudgetLineDto(1, 1, 5, null);
            BudgetRequestDto request = new BudgetRequestDto("Test", null, List.of(line));

            when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
            when(supplierRepository.findById(1)).thenReturn(Optional.of(testSupplier));
            when(productSupplierRepository.findByMasterProductIdAndSupplierId(1, 1))
                    .thenReturn(Optional.empty());
            when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
                Budget b = invocation.getArgument(0);
                b.setId(1L);
                return b;
            });

            // Act
            BudgetResponseDto response = budgetService.simulateBudget(request, "user");

            // Assert
            assertThat(response.getItems()).hasSize(1);
            BudgetItemDto item = response.getItems().get(0);
            assertThat(item.getUnitPrice()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(item.getStockAvailable()).isEqualTo(0);
            assertThat(item.getQuantity()).isEqualTo(0); // stock=0, min(5, 0) = 0
            assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Debe manejar múltiples líneas correctamente")
        void shouldHandleMultipleLines() {
            // Arrange
            ProductSupplier ps2 = new ProductSupplier();
            ps2.setPrice(new BigDecimal("75.00"));
            ps2.setRetailPrice(new BigDecimal("120.00"));
            ps2.setStock(20);

            MasterProduct product2 = new MasterProduct();
            product2.setId(2);
            product2.setMpn("MPN-002");
            product2.setBrand("Brand2");
            product2.setModel("Model2");

            Supplier supplier2 = new Supplier();
            supplier2.setId(2);
            supplier2.setName("Supplier2");

            BudgetLineDto line1 = new BudgetLineDto(1, 1, 3, null);
            BudgetLineDto line2 = new BudgetLineDto(2, 2, 10, null);
            BudgetRequestDto request = new BudgetRequestDto("Multi", null, List.of(line1, line2));

            when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
            when(supplierRepository.findById(1)).thenReturn(Optional.of(testSupplier));
            when(productSupplierRepository.findByMasterProductIdAndSupplierId(1, 1))
                    .thenReturn(Optional.of(testProductSupplier));

            when(productRepository.findById(2)).thenReturn(Optional.of(product2));
            when(supplierRepository.findById(2)).thenReturn(Optional.of(supplier2));
            when(productSupplierRepository.findByMasterProductIdAndSupplierId(2, 2))
                    .thenReturn(Optional.of(ps2));
            when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
                Budget b = invocation.getArgument(0);
                b.setId(1L);
                return b;
            });

            // Act
            BudgetResponseDto response = budgetService.simulateBudget(request, "user");

            // Assert
            assertThat(response.getItems()).hasSize(2);
            // 150*3 + 75*10 = 450 + 750 = 1200
            assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1200.00"));
        }

        @Test
        @DisplayName("Debe generar número de presupuesto con formato PRES-YYYYMMDD-NNN")
        void shouldGenerateBudgetNumberInCorrectFormat() {
            // Arrange
            BudgetLineDto line = new BudgetLineDto(1, 1, 1, null);
            BudgetRequestDto request = new BudgetRequestDto("Format Test", null, List.of(line));

            when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
            when(supplierRepository.findById(1)).thenReturn(Optional.of(testSupplier));
            when(productSupplierRepository.findByMasterProductIdAndSupplierId(1, 1))
                    .thenReturn(Optional.of(testProductSupplier));
            when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
                Budget b = invocation.getArgument(0);
                b.setId(1L);
                return b;
            });

            // Act
            BudgetResponseDto response = budgetService.simulateBudget(request, "user");

            // Assert
            assertThat(response.getBudgetNumber()).matches("PRES-\\d{8}-\\d{3}");
        }

        @Test
        @DisplayName("Debe manejar lista vacía de líneas")
        void shouldHandleEmptyLines() {
            // Arrange
            BudgetRequestDto request = new BudgetRequestDto("Empty", null, List.of());
            when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
                Budget b = invocation.getArgument(0);
                b.setId(1L);
                return b;
            });

            // Act
            BudgetResponseDto response = budgetService.simulateBudget(request, "user");

            // Assert
            assertThat(response.getItems()).isEmpty();
            assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getBudgetName()).isEqualTo("Empty");
            assertThat(response.getCreatedBy()).isEqualTo("user");
        }

        @Test
        @DisplayName("Debe usar precio cero cuando ProductSupplier tiene price nulo")
        void shouldUseZeroPriceWhenProductSupplierPriceIsNull() {
            // Arrange
            testProductSupplier.setPrice(null);
            BudgetLineDto line = new BudgetLineDto(1, 1, 5, null);
            BudgetRequestDto request = new BudgetRequestDto("Null Price", null, List.of(line));

            when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
            when(supplierRepository.findById(1)).thenReturn(Optional.of(testSupplier));
            when(productSupplierRepository.findByMasterProductIdAndSupplierId(1, 1))
                    .thenReturn(Optional.of(testProductSupplier));
            when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
                Budget b = invocation.getArgument(0);
                b.setId(1L);
                return b;
            });

            // Act
            BudgetResponseDto response = budgetService.simulateBudget(request, "user");

            // Assert
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getUnitPrice()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Debe usar stock cero cuando ProductSupplier tiene stock nulo")
        void shouldUseZeroStockWhenProductSupplierStockIsNull() {
            // Arrange
            testProductSupplier.setStock(null);
            BudgetLineDto line = new BudgetLineDto(1, 1, 5, null);
            BudgetRequestDto request = new BudgetRequestDto("Null Stock", null, List.of(line));

            when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
            when(supplierRepository.findById(1)).thenReturn(Optional.of(testSupplier));
            when(productSupplierRepository.findByMasterProductIdAndSupplierId(1, 1))
                    .thenReturn(Optional.of(testProductSupplier));
            when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
                Budget b = invocation.getArgument(0);
                b.setId(1L);
                return b;
            });

            // Act
            BudgetResponseDto response = budgetService.simulateBudget(request, "user");

            // Assert
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getStockAvailable()).isEqualTo(0);
            assertThat(response.getItems().get(0).getQuantity()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getUserBudgets()")
    class GetUserBudgets {

        @Test
        @DisplayName("Debe devolver lista de presupuestos del usuario")
        void shouldReturnUserBudgets() {
            // Arrange
            Budget budget = new Budget("PRES-20260520-001", "Test", null, "DRAFT", "testuser",
                    LocalDateTime.now(), LocalDateTime.now(), new BigDecimal("100.00"));
            budget.setId(1L);
            budget.addLine(new BudgetLine(1, "MPN-001", "Producto Test", "Brand", "Model",
                    1, "Supplier", 5, 10, new BigDecimal("20.00"), null, null, null));

            when(budgetRepository.findByCreatedByOrderByCreatedAtDesc("testuser"))
                    .thenReturn(List.of(budget));

            // Act
            List<BudgetResponseDto> result = budgetService.getUserBudgets("testuser");

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getBudgetName()).isEqualTo("Test");
            assertThat(result.get(0).getBudgetNumber()).isEqualTo("PRES-20260520-001");
            assertThat(result.get(0).getItems()).hasSize(1);
        }

        @Test
        @DisplayName("Debe devolver lista vacía si no hay presupuestos")
        void shouldReturnEmptyListWhenNoBudgets() {
            when(budgetRepository.findByCreatedByOrderByCreatedAtDesc("testuser"))
                    .thenReturn(List.of());

            List<BudgetResponseDto> result = budgetService.getUserBudgets("testuser");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getBudgetById()")
    class GetBudgetById {

        @Test
        @DisplayName("Debe devolver presupuesto por ID")
        void shouldReturnBudgetById() {
            // Arrange
            Budget budget = new Budget("PRES-20260520-001", "Test", null, "DRAFT", "testuser",
                    LocalDateTime.now(), LocalDateTime.now(), new BigDecimal("100.00"));
            budget.setId(1L);

            when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget));

            // Act
            BudgetResponseDto result = budgetService.getBudgetById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getBudgetName()).isEqualTo("Test");
        }

        @Test
        @DisplayName("Debe lanzar excepción si no existe")
        void shouldThrowWhenNotFound() {
            when(budgetRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.getBudgetById(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Presupuesto no encontrado");
        }
    }

    @Nested
    @DisplayName("updateBudgetStatus()")
    class UpdateBudgetStatus {

        @Test
        @DisplayName("Debe actualizar estado correctamente")
        void shouldUpdateStatus() {
            // Arrange
            Budget budget = new Budget("PRES-20260520-001", "Test", null, "DRAFT", "testuser",
                    LocalDateTime.now(), LocalDateTime.now(), new BigDecimal("100.00"));
            budget.setId(1L);

            when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget));
            when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            BudgetResponseDto result = budgetService.updateBudgetStatus(1L, "FINALIZED");

            // Assert
            assertThat(result.getStatus()).isEqualTo("FINALIZED");
            verify(notificationService).createGeneralNotification(
                    anyString(), anyString(), anyString()
            );
        }

        @Test
        @DisplayName("Debe lanzar excepción para estado inválido")
        void shouldThrowForInvalidStatus() {
            assertThatThrownBy(() -> budgetService.updateBudgetStatus(1L, "INVALID"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Estado inválido");
        }

        @Test
        @DisplayName("Debe lanzar excepción si presupuesto no existe")
        void shouldThrowWhenBudgetNotFound() {
            when(budgetRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.updateBudgetStatus(999L, "FINALIZED"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Presupuesto no encontrado");
        }
    }

    @Nested
    @DisplayName("deleteBudget()")
    class DeleteBudget {

        @Test
        @DisplayName("Debe eliminar presupuesto existente")
        void shouldDeleteExistingBudget() {
            // Arrange
            Budget budget = new Budget("PRES-20260520-001", "Test", null, "DRAFT", "testuser",
                    LocalDateTime.now(), LocalDateTime.now(), new BigDecimal("100.00"));
            budget.setId(1L);

            when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget));

            // Act
            budgetService.deleteBudget(1L);

            // Assert
            verify(budgetRepository).delete(budget);
        }

        @Test
        @DisplayName("Debe lanzar excepción si no existe")
        void shouldThrowWhenNotFound() {
            when(budgetRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.deleteBudget(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Presupuesto no encontrado");
        }
    }

    @Nested
    @DisplayName("notifyBudgetExported()")
    class NotifyBudgetExported {

        @Test
        @DisplayName("Debe notificar exportación y cambiar estado a EXPORTED si está en DRAFT")
        void shouldNotifyExportAndChangeStatusFromDraft() {
            // Arrange
            Budget budget = new Budget("PRES-20260520-001", "Test", null, "DRAFT", "testuser",
                    LocalDateTime.now(), LocalDateTime.now(), new BigDecimal("100.00"));
            budget.setId(1L);

            when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget));
            when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            budgetService.notifyBudgetExported(1L, "testuser");

            // Assert
            assertThat(budget.getStatus()).isEqualTo("EXPORTED");
            verify(notificationService).createGeneralNotification(
                    "Presupuesto exportado",
                    "Presupuesto PRES-20260520-001 exportado a Excel por testuser",
                    "BUDGET_EXPORT|PRES-20260520-001"
            );
        }

        @Test
        @DisplayName("Debe notificar exportación sin cambiar estado si ya está EXPORTED")
        void shouldNotifyExportWithoutChangingStatusIfAlreadyExported() {
            // Arrange
            Budget budget = new Budget("PRES-20260520-001", "Test", null, "EXPORTED", "testuser",
                    LocalDateTime.now(), LocalDateTime.now(), new BigDecimal("100.00"));
            budget.setId(1L);

            when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget));

            // Act
            budgetService.notifyBudgetExported(1L, "testuser");

            // Assert
            assertThat(budget.getStatus()).isEqualTo("EXPORTED"); // unchanged
            verify(budgetRepository, never()).save(any(Budget.class));
        }
    }
}
