package com.omnistock.backend.service.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
        // Set a valid 32-character key for AES
        ReflectionTestUtils.setField(encryptionService, "encryptionSecret", "0123456789abcdef0123456789abcdef");
    }

    @Nested
    @DisplayName("encrypt")
    class Encrypt {

        @Test
        @DisplayName("Debe encriptar texto correctamente")
        void shouldEncryptText() {
            String plaintext = "test-api-key-123";
            String encrypted = encryptionService.encrypt(plaintext);

            assertNotNull(encrypted);
            assertFalse(encrypted.isEmpty());
            assertNotEquals(plaintext, encrypted);
        }

        @Test
        @DisplayName("Debe encriptar texto vacío")
        void shouldEncryptEmptyText() {
            String encrypted = encryptionService.encrypt("");
            assertNotNull(encrypted);
        }

        @Test
        @DisplayName("Debe producir diferentes resultados para diferentes entradas")
        void shouldProduceDifferentResults() {
            String encrypted1 = encryptionService.encrypt("key1");
            String encrypted2 = encryptionService.encrypt("key2");
            assertNotEquals(encrypted1, encrypted2);
        }
    }

    @Nested
    @DisplayName("decrypt")
    class Decrypt {

        @Test
        @DisplayName("Debe desencriptar texto previamente encriptado")
        void shouldDecryptEncryptedText() {
            String plaintext = "my-secret-api-key";
            String encrypted = encryptionService.encrypt(plaintext);
            String decrypted = encryptionService.decrypt(encrypted);

            assertEquals(plaintext, decrypted);
        }

        @Test
        @DisplayName("Debe manejar caracteres especiales")
        void shouldHandleSpecialCharacters() {
            String plaintext = "abc123!@#$%^&*()_+-=[]{}|;':\",./<>?`~";
            String encrypted = encryptionService.encrypt(plaintext);
            String decrypted = encryptionService.decrypt(encrypted);

            assertEquals(plaintext, decrypted);
        }

        @Test
        @DisplayName("Debe lanzar excepción para texto inválido")
        void shouldThrowForInvalidText() {
            assertThrows(RuntimeException.class, () -> encryptionService.decrypt("invalid-base64!"));
        }

        @Test
        @DisplayName("Debe lanzar excepción para Base64 inválido")
        void shouldThrowForInvalidBase64() {
            assertThrows(RuntimeException.class, () -> encryptionService.decrypt("not-valid-base64-!!!"));
        }
    }

    @Nested
    @DisplayName("encrypt/decrypt roundtrip")
    class RoundTrip {

        @Test
        @DisplayName("Debe mantener integridad en roundtrip completo")
        void shouldMaintainIntegrity() {
            String[] testCases = {
                "",
                "a",
                "hello",
                "1234567890",
                "a".repeat(100),
                "special_chars_ñ_á_é_í_ó_ú",
                "  spaces  ",
                "line\nbreak",
                "tab\tcharacter"
            };

            for (String plaintext : testCases) {
                String encrypted = encryptionService.encrypt(plaintext);
                String decrypted = encryptionService.decrypt(encrypted);
                assertEquals(plaintext, decrypted, "Roundtrip failed for: " + plaintext);
            }
        }

        @Test
        @DisplayName("Debe funcionar con diferentes longitudes de clave")
        void shouldWorkWithDifferentKeyLengths() {
            // Test with 16-char key
            EncryptionService service16 = new EncryptionService();
            ReflectionTestUtils.setField(service16, "encryptionSecret", "0123456789abcdef");
            String encrypted = service16.encrypt("test");
            assertEquals("test", service16.decrypt(encrypted));

            // Test with 24-char key
            EncryptionService service24 = new EncryptionService();
            ReflectionTestUtils.setField(service24, "encryptionSecret", "0123456789abcdef01234567");
            encrypted = service24.encrypt("test");
            assertEquals("test", service24.decrypt(encrypted));
        }
    }
}
