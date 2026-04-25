package com.omnistock.backend.service.auth;

import com.omnistock.backend.dtos.auth.RegisterRequestDto;
import com.omnistock.backend.dtos.auth.UserDto;
import com.omnistock.backend.dtos.auth.UserResponseDto;
import com.omnistock.backend.exception.RegistrationConflictException;
import com.omnistock.backend.entity.Role;
import com.omnistock.backend.entity.User;
import com.omnistock.backend.repository.RoleRepository;
import com.omnistock.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RoleService roleService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, roleRepository, passwordEncoder, roleService);
    }

    private User createUser(Integer id, String username, String email, String fullName, String roleName) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPassword("encoded-pass");
        user.setActive(true);
        if (roleName != null) {
            Role role = new Role();
            role.setName(roleName);
            user.setRoles(new HashSet<>(Set.of(role)));
        }
        return user;
    }

    @Nested
    @DisplayName("loadUserByUsername")
    class LoadUserByUsername {

        @Test
        @DisplayName("Debe cargar usuario por username")
        void shouldLoadUser() {
            User user = createUser(1, "testuser", "test@test.com", "Test User", "ROLE_USER");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            var result = userService.loadUserByUsername("testuser");

            assertEquals("testuser", result.getUsername());
        }

        @Test
        @DisplayName("Debe lanzar excepción si no existe")
        void shouldThrowWhenNotFound() {
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername("unknown"));
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("Debe devolver todos los usuarios")
        void shouldReturnAllUsers() {
            when(userRepository.findAll()).thenReturn(List.of(
                    createUser(1, "user1", "u1@test.com", "User 1", "ROLE_USER"),
                    createUser(2, "user2", "u2@test.com", "User 2", "ROLE_ADMIN")
            ));

            List<UserDto> result = userService.findAll();

            assertEquals(2, result.size());
            assertEquals("user1", result.get(0).username());
            assertEquals("user2", result.get(1).username());
        }

        @Test
        @DisplayName("Debe devolver lista vacía si no hay usuarios")
        void shouldReturnEmptyList() {
            when(userRepository.findAll()).thenReturn(List.of());

            List<UserDto> result = userService.findAll();

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("Debe devolver usuario por ID")
        void shouldReturnUserById() {
            when(userRepository.findById(1)).thenReturn(Optional.of(
                    createUser(1, "testuser", "test@test.com", "Test User", "ROLE_USER")
            ));

            Optional<UserDto> result = userService.findById(1);

            assertTrue(result.isPresent());
            assertEquals("testuser", result.get().username());
        }

        @Test
        @DisplayName("Debe devolver empty si no existe")
        void shouldReturnEmptyWhenNotFound() {
            when(userRepository.findById(99)).thenReturn(Optional.empty());

            Optional<UserDto> result = userService.findById(99);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("Debe registrar usuario exitosamente")
        void shouldRegisterUser() {
            RegisterRequestDto request = new RegisterRequestDto(
                    "newuser", "password123", "New User", "new@test.com"
            );
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
            Role role = new Role();
            role.setName("ROLE_CLIENTE");
            when(roleService.getOrCreateByName("ROLE_CLIENTE")).thenReturn(role);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1);
                return u;
            });

            UserResponseDto result = userService.register(request);

            assertEquals("newuser", result.username());
            assertEquals("new@test.com", result.email());
            assertEquals("New User", result.fullName());
        }

        @Test
        @DisplayName("Debe lanzar excepción si username ya existe")
        void shouldThrowWhenUsernameExists() {
            RegisterRequestDto request = new RegisterRequestDto(
                    "existing", "pass", "Existing", "test@test.com"
            );
            when(userRepository.existsByUsername("existing")).thenReturn(true);

            assertThrows(RegistrationConflictException.class, () -> userService.register(request));
        }

        @Test
        @DisplayName("Debe lanzar excepción si email ya existe")
        void shouldThrowWhenEmailExists() {
            RegisterRequestDto request = new RegisterRequestDto(
                    "newuser", "pass", "New User", "existing@test.com"
            );
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

            assertThrows(RegistrationConflictException.class, () -> userService.register(request));
        }

        @Test
        @DisplayName("Debe registrar sin verificar email si es null")
        void shouldRegisterWhenEmailIsNull() {
            RegisterRequestDto request = new RegisterRequestDto(
                    "nouser", "pass", "No Email", null
            );
            when(userRepository.existsByUsername("nouser")).thenReturn(false);
            Role role = new Role();
            role.setName("ROLE_CLIENTE");
            when(roleService.getOrCreateByName("ROLE_CLIENTE")).thenReturn(role);
            when(passwordEncoder.encode("pass")).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(2);
                return u;
            });

            UserResponseDto result = userService.register(request);

            assertEquals("nouser", result.username());
            verify(userRepository, never()).existsByEmail(anyString());
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("Debe guardar usuario con contraseña")
        void shouldSaveUserWithPassword() {
            UserDto dto = new UserDto(null, "newuser", "new@test.com", "New User",
                    "raw-pass", Set.of("ROLE_USER"));
            Role role = new Role();
            role.setName("ROLE_USER");
            when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role));
            when(passwordEncoder.encode("raw-pass")).thenReturn("encoded-pass");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1);
                return u;
            });

            UserDto result = userService.save(dto);

            assertEquals("newuser", result.username());
            verify(passwordEncoder).encode("raw-pass");
        }

        @Test
        @DisplayName("Debe guardar usuario sin contraseña")
        void shouldSaveUserWithoutPassword() {
            UserDto dto = new UserDto(null, "nopass", "nopass@test.com", "No Pass",
                    null, Set.of("ROLE_USER"));
            Role role = new Role();
            role.setName("ROLE_USER");
            when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(2);
                return u;
            });

            UserDto result = userService.save(dto);

            assertEquals("nopass", result.username());
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("Debe lanzar excepción si roles están vacíos")
        void shouldThrowWhenRolesEmpty() {
            UserDto dto = new UserDto(null, "nouser", null, null, null, Set.of());

            assertThrows(IllegalArgumentException.class, () -> userService.save(dto));
        }

        @Test
        @DisplayName("Debe lanzar excepción si tiene más de un rol")
        void shouldThrowWhenMultipleRoles() {
            UserDto dto = new UserDto(null, "multi", null, null, null, Set.of("ROLE_USER", "ROLE_ADMIN"));

            assertThrows(IllegalArgumentException.class, () -> userService.save(dto));
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("Debe actualizar usuario exitosamente")
        void shouldUpdateUser() {
            User existing = createUser(1, "olduser", "old@test.com", "Old User", "ROLE_USER");
            when(userRepository.findById(1)).thenReturn(Optional.of(existing));
            when(userRepository.save(any(User.class))).thenReturn(existing);

            UserDto dto = new UserDto(1, "newuser", "new@test.com", "New User", null, null);
            UserDto result = userService.update(1, dto);

            assertEquals("newuser", result.username());
            assertEquals("new@test.com", result.email());
        }

        @Test
        @DisplayName("Debe lanzar excepción si no existe")
        void shouldThrowWhenNotFound() {
            when(userRepository.findById(99)).thenReturn(Optional.empty());

            UserDto dto = new UserDto(99, "test", null, null, null, null);
            assertThrows(EntityNotFoundException.class, () -> userService.update(99, dto));
        }

        @Test
        @DisplayName("Debe actualizar contraseña si se proporciona")
        void shouldUpdatePassword() {
            User existing = createUser(1, "user", "user@test.com", "User", "ROLE_USER");
            when(userRepository.findById(1)).thenReturn(Optional.of(existing));
            when(passwordEncoder.encode("new-pass")).thenReturn("new-encoded");
            when(userRepository.save(any(User.class))).thenReturn(existing);

            UserDto dto = new UserDto(1, null, null, null, "new-pass", null);
            userService.update(1, dto);

            verify(passwordEncoder).encode("new-pass");
        }

        @Test
        @DisplayName("Debe actualizar roles si se proporcionan")
        void shouldUpdateRoles() {
            User existing = createUser(1, "user", "user@test.com", "User", "ROLE_USER");
            when(userRepository.findById(1)).thenReturn(Optional.of(existing));
            Role role = new Role();
            role.setName("ROLE_ADMIN");
            when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(role));
            when(userRepository.save(any(User.class))).thenReturn(existing);

            UserDto dto = new UserDto(1, null, null, null, null, Set.of("ROLE_ADMIN"));
            UserDto result = userService.update(1, dto);

            assertNotNull(result);
            verify(roleRepository).findByName("ROLE_ADMIN");
        }
    }

    @Nested
    @DisplayName("deleteById")
    class DeleteById {

        @Test
        @DisplayName("Debe eliminar usuario por ID")
        void shouldDeleteUser() {
            userService.deleteById(1);

            verify(userRepository).deleteById(1);
        }
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        @Test
        @DisplayName("Debe actualizar perfil del usuario autenticado")
        void shouldUpdateProfile() {
            User existing = createUser(1, "currentuser", "old@test.com", "Old Name", "ROLE_USER");
            when(userRepository.findByUsername("currentuser")).thenReturn(Optional.of(existing));
            when(userRepository.save(any(User.class))).thenReturn(existing);

            UserDto dto = new UserDto(null, null, "new@test.com", "New Name", null, null);
            UserDto result = userService.updateProfile("currentuser", dto);

            assertEquals("new@test.com", result.email());
            assertEquals("New Name", result.fullName());
        }

        @Test
        @DisplayName("Debe actualizar contraseña en perfil")
        void shouldUpdatePasswordInProfile() {
            User existing = createUser(1, "currentuser", "test@test.com", "User", "ROLE_USER");
            when(userRepository.findByUsername("currentuser")).thenReturn(Optional.of(existing));
            when(passwordEncoder.encode("new-pass")).thenReturn("new-encoded");
            when(userRepository.save(any(User.class))).thenReturn(existing);

            UserDto dto = new UserDto(null, null, null, null, "new-pass", null);
            userService.updateProfile("currentuser", dto);

            verify(passwordEncoder).encode("new-pass");
        }

        @Test
        @DisplayName("Debe lanzar excepción si usuario no existe")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            UserDto dto = new UserDto(null, null, null, null, null, null);
            assertThrows(UsernameNotFoundException.class, () -> userService.updateProfile("unknown", dto));
        }
    }
}
