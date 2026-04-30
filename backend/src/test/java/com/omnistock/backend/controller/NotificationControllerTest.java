package com.omnistock.backend.controller;

import com.omnistock.backend.configuration.SecurityConfig;
import com.omnistock.backend.dtos.notification.NotificationCountsDto;
import com.omnistock.backend.dtos.notification.NotificationItemDto;
import com.omnistock.backend.dtos.notification.NotificationTab;
import com.omnistock.backend.entity.AppNotificationType;
import com.omnistock.backend.security.JwtService;
import com.omnistock.backend.service.notification.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Nested
    @DisplayName("GET /api/v1/notificaciones")
    class GetNotifications {

        @Test
        @DisplayName("Debe obtener notificaciones con CLIENTE autenticado")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldGetNotifications() throws Exception {
            NotificationItemDto item = new NotificationItemDto(
                    1L, AppNotificationType.GENERAL, "Test", "Mensaje",
                    null, null, null, null, null, null,
                    LocalDateTime.now(), false);
            when(notificationService.getNotifications(NotificationTab.GENERAL, 50)).thenReturn(List.of(item));

            mockMvc.perform(get("/api/v1/notificaciones"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].title").value("Test"));
        }

        @Test
        @DisplayName("Debe devolver 401 sin autenticación")
        void shouldReturn401WithoutAuth() throws Exception {
            mockMvc.perform(get("/api/v1/notificaciones"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/notificaciones/counts")
    class GetCounts {

        @Test
        @DisplayName("Debe obtener conteos de notificaciones")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldGetCounts() throws Exception {
            NotificationCountsDto counts = new NotificationCountsDto(5L, 3L, 2L, 1L);
            when(notificationService.getUnreadCounts()).thenReturn(counts);

            mockMvc.perform(get("/api/v1/notificaciones/counts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.general").value(5));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/notificaciones/{notificationId}/read")
    class MarkAsRead {

        @Test
        @DisplayName("Debe marcar notificación como leída")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldMarkAsRead() throws Exception {
            doNothing().when(notificationService).markAsRead(1L);

            mockMvc.perform(put("/api/v1/notificaciones/1/read"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Notificación marcada como leída"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/notificaciones/read-all")
    class MarkAllAsRead {

        @Test
        @DisplayName("Debe marcar todas como leídas")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldMarkAllAsRead() throws Exception {
            doNothing().when(notificationService).markAllAsRead(NotificationTab.GENERAL);

            mockMvc.perform(put("/api/v1/notificaciones/read-all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Notificaciones marcadas como leídas"));
        }
    }
}
