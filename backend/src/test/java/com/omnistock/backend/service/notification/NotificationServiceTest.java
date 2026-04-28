package com.omnistock.backend.service.notification;

import com.omnistock.backend.dtos.notification.NotificationCountsDto;
import com.omnistock.backend.dtos.notification.NotificationItemDto;
import com.omnistock.backend.dtos.notification.NotificationTab;
import com.omnistock.backend.entity.*;
import com.omnistock.backend.repository.AppNotificationRepository;
import com.omnistock.backend.repository.UserNotificationStatusRepository;
import com.omnistock.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private AppNotificationRepository appNotificationRepository;

    @Mock
    private UserNotificationStatusRepository userNotificationStatusRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private NotificationService notificationService;

    private User testUser;
    private Supplier testSupplier;
    private MasterProduct testProduct;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(appNotificationRepository,
                userNotificationStatusRepository, userRepository);

        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");

        testSupplier = new Supplier();
        testSupplier.setId(1);
        testSupplier.setName("TestSupplier");

        testProduct = new MasterProduct();
        testProduct.setId(1);
        testProduct.setBrand("TestBrand");
        testProduct.setModel("TestModel");
        testProduct.setMpn("MPN-001");

        // Setup SecurityContext
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("testuser");
        lenient().when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    }

    @Nested
    @DisplayName("createErrorNotification()")
    class CreateErrorNotification {

        @Test
        @DisplayName("Debe crear notificación de error")
        void shouldCreateErrorNotification() {
            notificationService.createErrorNotification(testSupplier, "Sync", "Error de conexión");

            verify(appNotificationRepository).save(any(AppNotification.class));
        }

        @Test
        @DisplayName("Debe crear notificación de error con valores por defecto")
        void shouldCreateErrorWithDefaults() {
            notificationService.createErrorNotification(null, null, null);

            verify(appNotificationRepository).save(any(AppNotification.class));
        }
    }

    @Nested
    @DisplayName("createOfferNotification()")
    class CreateOfferNotification {

        @Test
        @DisplayName("Debe crear notificación de oferta cuando el precio baja")
        void shouldCreateOfferNotification() {
            notificationService.createOfferNotification(testProduct, testSupplier,
                    new BigDecimal("100.00"), new BigDecimal("80.00"));

            verify(appNotificationRepository).save(any(AppNotification.class));
        }

        @Test
        @DisplayName("No debe crear notificación si el precio no baja")
        void shouldNotCreateWhenPriceDoesNotDrop() {
            notificationService.createOfferNotification(testProduct, testSupplier,
                    new BigDecimal("80.00"), new BigDecimal("100.00"));

            verify(appNotificationRepository, never()).save(any(AppNotification.class));
        }

        @Test
        @DisplayName("No debe crear notificación si algún parámetro es nulo")
        void shouldNotCreateWhenParamsAreNull() {
            notificationService.createOfferNotification(null, testSupplier,
                    new BigDecimal("100.00"), new BigDecimal("80.00"));

            verify(appNotificationRepository, never()).save(any(AppNotification.class));
        }
    }

    @Nested
    @DisplayName("createNewProductGlobalNotification()")
    class CreateNewProductGlobalNotification {

        @Test
        @DisplayName("Debe crear notificación de nuevo producto global")
        void shouldCreateNewProductGlobalNotification() {
            notificationService.createNewProductGlobalNotification(testProduct, testSupplier);

            verify(appNotificationRepository).save(any(AppNotification.class));
        }

        @Test
        @DisplayName("No debe crear si el producto es nulo")
        void shouldNotCreateWhenProductIsNull() {
            notificationService.createNewProductGlobalNotification(null, testSupplier);

            verify(appNotificationRepository, never()).save(any(AppNotification.class));
        }
    }

    @Nested
    @DisplayName("createNewProductSupplierNotification()")
    class CreateNewProductSupplierNotification {

        @Test
        @DisplayName("Debe crear notificación de nuevo producto en proveedor")
        void shouldCreateNewProductSupplierNotification() {
            notificationService.createNewProductSupplierNotification(testProduct, testSupplier);

            verify(appNotificationRepository).save(any(AppNotification.class));
        }

        @Test
        @DisplayName("No debe crear si producto o proveedor es nulo")
        void shouldNotCreateWhenProductOrSupplierIsNull() {
            notificationService.createNewProductSupplierNotification(null, testSupplier);

            verify(appNotificationRepository, never()).save(any(AppNotification.class));
        }
    }

    @Nested
    @DisplayName("createGeneralNotification()")
    class CreateGeneralNotification {

        @Test
        @DisplayName("Debe crear notificación general")
        void shouldCreateGeneralNotification() {
            notificationService.createGeneralNotification("Título", "Mensaje", "dedup-base");

            verify(appNotificationRepository).save(any(AppNotification.class));
        }
    }

    @Nested
    @DisplayName("getNotifications()")
    class GetNotifications {

        @Test
        @DisplayName("Debe devolver lista de notificaciones")
        void shouldReturnNotifications() {
            AppNotification notification = new AppNotification();
            notification.setId(1L);
            notification.setType(AppNotificationType.GENERAL);
            notification.setTitle("Test");
            notification.setMessage("Message");
            notification.setCreatedAt(LocalDateTime.now());

            when(appNotificationRepository.findActiveByType(any(), any(), any()))
                    .thenReturn(List.of(notification));
            when(userNotificationStatusRepository.findReadNotificationIds(anyInt(), anyList()))
                    .thenReturn(List.of());

            List<NotificationItemDto> result = notificationService.getNotifications(NotificationTab.GENERAL, 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).title()).isEqualTo("Test");
        }

        @Test
        @DisplayName("Debe devolver lista vacía si no hay notificaciones")
        void shouldReturnEmptyList() {
            when(appNotificationRepository.findActiveByType(any(), any(), any()))
                    .thenReturn(List.of());

            List<NotificationItemDto> result = notificationService.getNotifications(NotificationTab.GENERAL, 10);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUnreadCounts()")
    class GetUnreadCounts {

        @Test
        @DisplayName("Debe devolver conteos de no leídas")
        void shouldReturnUnreadCounts() {
            when(appNotificationRepository.findAllActiveIdsByType(any(), any()))
                    .thenReturn(List.of(1L, 2L));
            when(userNotificationStatusRepository.findAllReadActiveIds(anyInt(), any()))
                    .thenReturn(List.of());
            when(appNotificationRepository.findAllById(List.of(1L, 2L)))
                    .thenReturn(List.of(
                            createNotification(1L, AppNotificationType.ERROR),
                            createNotification(2L, AppNotificationType.OFERTA)
                    ));

            NotificationCountsDto counts = notificationService.getUnreadCounts();

            assertThat(counts.errores()).isEqualTo(1);
            assertThat(counts.ofertas()).isEqualTo(1);
            assertThat(counts.general()).isEqualTo(2);
            assertThat(counts.nuevoProducto()).isEqualTo(0);
        }

        @Test
        @DisplayName("Debe devolver ceros si no hay notificaciones activas")
        void shouldReturnZerosWhenNoActiveNotifications() {
            when(appNotificationRepository.findAllActiveIdsByType(any(), any()))
                    .thenReturn(List.of());

            NotificationCountsDto counts = notificationService.getUnreadCounts();

            assertThat(counts.errores()).isEqualTo(0);
            assertThat(counts.ofertas()).isEqualTo(0);
            assertThat(counts.general()).isEqualTo(0);
            assertThat(counts.nuevoProducto()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("markAsRead()")
    class MarkAsRead {

        @Test
        @DisplayName("Debe marcar notificación como leída")
        void shouldMarkAsRead() {
            AppNotification notification = new AppNotification();
            notification.setId(1L);

            when(appNotificationRepository.findById(1L)).thenReturn(Optional.of(notification));
            when(userNotificationStatusRepository.findByUserIdAndNotificationId(1, 1L))
                    .thenReturn(Optional.empty());

            notificationService.markAsRead(1L);

            verify(userNotificationStatusRepository).save(any(UserNotificationStatus.class));
        }

        @Test
        @DisplayName("Debe lanzar excepción si la notificación no existe")
        void shouldThrowWhenNotFound() {
            when(appNotificationRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.markAsRead(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Notificación no encontrada");
        }
    }

    @Nested
    @DisplayName("markAllAsRead()")
    class MarkAllAsRead {

        @Test
        @DisplayName("Debe marcar todas como leídas")
        void shouldMarkAllAsRead() {
            when(appNotificationRepository.findAllActiveIdsByType(any(), any()))
                    .thenReturn(List.of(1L));
            when(userNotificationStatusRepository.findReadNotificationIds(anyInt(), anyList()))
                    .thenReturn(List.of());
            when(appNotificationRepository.findAllById(List.of(1L)))
                    .thenReturn(List.of(createNotification(1L, AppNotificationType.GENERAL)));

            notificationService.markAllAsRead(NotificationTab.GENERAL);

            verify(userNotificationStatusRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("No debe hacer nada si no hay notificaciones activas")
        void shouldDoNothingWhenNoActiveNotifications() {
            when(appNotificationRepository.findAllActiveIdsByType(any(), any()))
                    .thenReturn(List.of());

            notificationService.markAllAsRead(NotificationTab.GENERAL);

            verify(userNotificationStatusRepository, never()).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("cleanupExpiredNotifications()")
    class CleanupExpiredNotifications {

        @Test
        @DisplayName("Debe limpiar notificaciones expiradas")
        void shouldCleanupExpiredNotifications() {
            when(appNotificationRepository.findExpiredIds(any())).thenReturn(List.of(1L, 2L));

            notificationService.cleanupExpiredNotifications();

            verify(userNotificationStatusRepository).deleteByNotificationIds(List.of(1L, 2L));
            verify(appNotificationRepository).deleteAllByIdInBatch(List.of(1L, 2L));
        }

        @Test
        @DisplayName("No debe hacer nada si no hay expiradas")
        void shouldDoNothingWhenNoExpired() {
            when(appNotificationRepository.findExpiredIds(any())).thenReturn(List.of());

            notificationService.cleanupExpiredNotifications();

            verify(userNotificationStatusRepository, never()).deleteByNotificationIds(anyList());
            verify(appNotificationRepository, never()).deleteAllByIdInBatch(anyList());
        }
    }

    private AppNotification createNotification(Long id, AppNotificationType type) {
        AppNotification n = new AppNotification();
        n.setId(id);
        n.setType(type);
        n.setTitle("Test");
        n.setMessage("Message");
        n.setCreatedAt(LocalDateTime.now());
        return n;
    }
}
