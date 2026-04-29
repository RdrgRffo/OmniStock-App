package com.omnistock.backend.controller;

import com.omnistock.backend.configuration.SecurityConfig;
import com.omnistock.backend.dtos.auth.UserDto;
import com.omnistock.backend.security.JwtService;
import com.omnistock.backend.service.auth.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    private UserDto createSampleUser() {
        return new UserDto(1, "testuser", "test@example.com", "Test User", null, Set.of("ADMIN"));
    }

    @Nested
    @DisplayName("GET /api/v1/admin/usuarios")
    class GetAllUsers {

        @Test
        @DisplayName("Debe listar usuarios con ADMIN")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldListUsers() throws Exception {
            when(userService.findAll()).thenReturn(List.of(createSampleUser()));

            mockMvc.perform(get("/api/v1/admin/usuarios"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].username").value("testuser"));
        }

        @Test
        @DisplayName("Debe devolver 500 si no es ADMIN (AccessDeniedException atrapado por GlobalExceptionHandler)")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldReturn500WhenNotAdmin() throws Exception {
            mockMvc.perform(get("/api/v1/admin/usuarios"))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/usuarios")
    class CreateUser {

        @Test
        @DisplayName("Debe crear usuario con ADMIN")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldCreateUser() throws Exception {
            when(userService.save(any(UserDto.class))).thenReturn(createSampleUser());

            String json = """
                    {
                        "username": "testuser",
                        "email": "test@example.com",
                        "fullName": "Test User",
                        "password": "password123",
                        "roles": ["ADMIN"]
                    }
                    """;

            mockMvc.perform(post("/api/v1/admin/usuarios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.username").value("testuser"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/usuarios/{id}")
    class GetUserById {

        @Test
        @DisplayName("Debe obtener usuario por ID con ADMIN")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldGetUserById() throws Exception {
            when(userService.findById(1)).thenReturn(Optional.of(createSampleUser()));

            mockMvc.perform(get("/api/v1/admin/usuarios/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.username").value("testuser"));
        }

        @Test
        @DisplayName("Debe devolver 404 si el usuario no existe")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldReturn404WhenNotFound() throws Exception {
            when(userService.findById(999)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/admin/usuarios/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/usuarios/{id}")
    class UpdateUser {

        @Test
        @DisplayName("Debe actualizar usuario con ADMIN")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldUpdateUser() throws Exception {
            UserDto updated = new UserDto(1, "updateduser", "updated@example.com", "Updated User", null, Set.of("ADMIN"));
            when(userService.update(anyInt(), any(UserDto.class))).thenReturn(updated);

            String json = """
                    {
                        "username": "updateduser",
                        "email": "updated@example.com"
                    }
                    """;

            mockMvc.perform(put("/api/v1/admin/usuarios/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.username").value("updateduser"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/usuarios/{id}")
    class DeleteUser {

        @Test
        @DisplayName("Debe eliminar usuario con ADMIN")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldDeleteUser() throws Exception {
            doNothing().when(userService).deleteById(1);

            mockMvc.perform(delete("/api/v1/admin/usuarios/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Usuario eliminado con éxito"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/usuarios/perfil")
    class UpdateProfile {

        @Test
        @DisplayName("Debe actualizar perfil del usuario autenticado")
        @WithMockUser(username = "testuser", roles = {"CLIENTE"})
        void shouldUpdateProfile() throws Exception {
            UserDto updated = new UserDto(1, "testuser", "newemail@example.com", "New Name", null, Set.of("CLIENTE"));
            when(userService.updateProfile(anyString(), any(UserDto.class))).thenReturn(updated);

            String json = """
                    {
                        "email": "newemail@example.com",
                        "fullName": "New Name"
                    }
                    """;

            mockMvc.perform(put("/api/v1/usuarios/perfil")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.email").value("newemail@example.com"));
        }
    }
}
