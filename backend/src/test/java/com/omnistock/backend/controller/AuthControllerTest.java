package com.omnistock.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnistock.backend.configuration.SecurityConfig;
import com.omnistock.backend.exception.GlobalExceptionHandler;
import com.omnistock.backend.dtos.auth.AuthResponse;
import com.omnistock.backend.dtos.auth.LoginRequestDto;
import com.omnistock.backend.dtos.auth.RegisterRequestDto;
import com.omnistock.backend.dtos.auth.UserResponseDto;
import com.omnistock.backend.exception.RegistrationConflictException;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserService userService;

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("Debe iniciar sesión correctamente")
        void shouldLoginSuccessfully() throws Exception {
            LoginRequestDto request = new LoginRequestDto("testuser", "password");

            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username("testuser")
                    .password("password")
                    .roles("CLIENTE")
                    .build();

            Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(auth);
            when(jwtService.generateToken(userDetails)).thenReturn("test-token");
            when(jwtService.getExpirationTime()).thenReturn(9000000L);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Sesión iniciada correctamente"))
                    .andExpect(jsonPath("$.data.token").value("test-token"));
        }

        @Test
        @DisplayName("Debe devolver 401 con credenciales inválidas")
        void shouldReturn401WithInvalidCredentials() throws Exception {
            LoginRequestDto request = new LoginRequestDto("testuser", "wrongpassword");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
        }

        @Test
        @DisplayName("Debe devolver 400 si el username está vacío")
        void shouldReturn400WhenUsernameIsBlank() throws Exception {
            LoginRequestDto request = new LoginRequestDto("", "password");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class Register {

        @Test
        @DisplayName("Debe registrar un nuevo usuario")
        void shouldRegisterUser() throws Exception {
            RegisterRequestDto request = new RegisterRequestDto("newuser", "password123", "New User", "new@test.com");

            when(userService.register(any(RegisterRequestDto.class)))
                    .thenReturn(new UserResponseDto(1, "newuser", "new@test.com", "New User"));

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Usuario registrado con éxito"));
        }

        @Test
        @DisplayName("Debe devolver 400 si el username ya existe")
        void shouldReturn400WhenUsernameExists() throws Exception {
            RegisterRequestDto request = new RegisterRequestDto("existing", "password123", "Existing User", "email@test.com");

            when(userService.register(any(RegisterRequestDto.class)))
                    .thenThrow(new RegistrationConflictException("El username ya existe"));

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("El username ya existe"));
        }

        @Test
        @DisplayName("Debe devolver 400 si el email ya existe")
        void shouldReturn400WhenEmailExists() throws Exception {
            RegisterRequestDto request = new RegisterRequestDto("newuser", "password123", "New User", "existing@test.com");

            when(userService.register(any(RegisterRequestDto.class)))
                    .thenThrow(new RegistrationConflictException("El email ya está registrado"));

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("El email ya está registrado"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/validate")
    class ValidateToken {

        @Test
        @DisplayName("Debe validar token correctamente")
        void shouldValidateToken() throws Exception {
            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username("testuser")
                    .password("password")
                    .roles("CLIENTE")
                    .build();

            when(jwtService.extractUsername("valid-token")).thenReturn("testuser");
            when(userService.loadUserByUsername("testuser")).thenReturn(userDetails);
            when(jwtService.isTokenValid("valid-token", userDetails)).thenReturn(true);

            mockMvc.perform(get("/api/v1/auth/validate")
                            .param("token", "valid-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(true));
        }

        @Test
        @DisplayName("Debe devolver false para token inválido")
        void shouldReturnFalseForInvalidToken() throws Exception {
            when(jwtService.extractUsername("invalid-token")).thenThrow(new RuntimeException("Invalid token"));

            mockMvc.perform(get("/api/v1/auth/validate")
                            .param("token", "invalid-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshToken {

        @Test
        @DisplayName("Debe renovar el token")
        void shouldRefreshToken() throws Exception {
            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username("testuser")
                    .password("password")
                    .roles("CLIENTE")
                    .build();

            when(jwtService.extractUsername("old-token")).thenReturn("testuser");
            when(userService.loadUserByUsername("testuser")).thenReturn(userDetails);
            when(jwtService.generateToken(userDetails)).thenReturn("new-token");
            when(jwtService.getExpirationTime()).thenReturn(9000000L);

            String json = "{\"refreshToken\": \"old-token\"}";

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.token").value("new-token"));
        }

        @Test
        @DisplayName("Debe devolver 403 si falla la renovación")
        void shouldReturn403WhenRefreshFails() throws Exception {
            when(jwtService.extractUsername("bad-token")).thenThrow(new RuntimeException("Invalid token"));

            String json = "{\"refreshToken\": \"bad-token\"}";

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
