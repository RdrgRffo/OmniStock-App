package com.omnistock.backend.security;

import com.omnistock.backend.entity.Role;
import com.omnistock.backend.entity.User;
import com.omnistock.backend.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private UserRepository userRepository;

    private JwtService jwtService;

    private User testUser;
    private UserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        // Use reflection to set the secret key since @Value won't work in tests
        jwtService = new JwtService(userRepository);

        // Set the secret key via reflection
        try {
            var field = JwtService.class.getDeclaredField("jwtSecretKey");
            field.setAccessible(true);
            field.set(jwtService, "claveJWT32bytes!!!!!!!!!!!!!!!!!!!!!!!!");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set secret key", e);
        }

        Role role = new Role();
        role.setId(1);
        role.setName("ROLE_CLIENTE");

        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setFullName("Test User");
        testUser.setRoles(Set.of(role));

        testUserDetails = org.springframework.security.core.userdetails.User.builder()
                .username("testuser")
                .password("password")
                .roles("CLIENTE")
                .build();
    }

    @Nested
    @DisplayName("generateToken()")
    class GenerateToken {

        @Test
        @DisplayName("Debe generar un token JWT válido")
        void shouldGenerateValidToken() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            String token = jwtService.generateToken(testUserDetails);

            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
        }

        @Test
        @DisplayName("Debe lanzar excepción si el usuario no existe en BD")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> jwtService.generateToken(testUserDetails))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Usuario no encontrado");
        }
    }

    @Nested
    @DisplayName("extractUsername()")
    class ExtractUsername {

        @Test
        @DisplayName("Debe extraer el username del token")
        void shouldExtractUsername() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            String token = jwtService.generateToken(testUserDetails);
            String username = jwtService.extractUsername(token);

            assertThat(username).isEqualTo("testuser");
        }
    }

    @Nested
    @DisplayName("extractAllClaims()")
    class ExtractAllClaims {

        @Test
        @DisplayName("Debe extraer todos los claims del token")
        void shouldExtractAllClaims() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            String token = jwtService.generateToken(testUserDetails);
            Claims claims = jwtService.extractAllClaims(token);

            assertThat(claims.getSubject()).isEqualTo("testuser");
            assertThat(claims.getIssuer()).isEqualTo("OmniStock");
            assertThat(claims.get("fullName")).isEqualTo("Test User");
            assertThat(claims.get("roles")).isInstanceOf(List.class);
        }
    }

    @Nested
    @DisplayName("isTokenValid()")
    class IsTokenValid {

        @Test
        @DisplayName("Debe devolver true para token válido")
        void shouldReturnTrueForValidToken() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            String token = jwtService.generateToken(testUserDetails);
            boolean valid = jwtService.isTokenValid(token, testUserDetails);

            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("Debe devolver false para token con CLIENTE diferente")
        void shouldReturnFalseForDifferentUser() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            String token = jwtService.generateToken(testUserDetails);

            UserDetails otherUser = org.springframework.security.core.userdetails.User.builder()
                    .username("otheruser")
                    .password("password")
                    .roles("CLIENTE")
                    .build();

            boolean valid = jwtService.isTokenValid(token, otherUser);

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("Debe devolver false para token malformado")
        void shouldReturnFalseForMalformedToken() {
            boolean valid = jwtService.isTokenValid("invalid-token", testUserDetails);

            assertThat(valid).isFalse();
        }
    }

    @Nested
    @DisplayName("getExpirationTime()")
    class GetExpirationTime {

        @Test
        @DisplayName("Debe devolver el tiempo de expiración configurado")
        void shouldReturnExpirationTime() {
            Long expiration = jwtService.getExpirationTime();

            assertThat(expiration).isEqualTo(1000L * 60 * 150); // 150 minutes
        }
    }
}
