package com.omnistock.backend.service.auth;

import com.omnistock.backend.dtos.auth.RegisterRequestDto;
import com.omnistock.backend.dtos.auth.UserDto;
import com.omnistock.backend.dtos.auth.UserResponseDto;
import com.omnistock.backend.exception.RegistrationConflictException;
import com.omnistock.backend.entity.Role;
import com.omnistock.backend.entity.User;
import com.omnistock.backend.repository.BudgetRepository;
import com.omnistock.backend.repository.RoleRepository;
import com.omnistock.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio de lógica de negocio para usuarios del sistema.
 * Gestiona CRUD de usuarios, carga por username para Spring Security y conversión a DTOs.
 * Usado por {@link com.omnistock.backend.controller.UserController} y {@link com.omnistock.backend.controller.AuthController}.
 */
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;
    private final BudgetRepository budgetRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder, RoleService roleService,
                       BudgetRepository budgetRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleService = roleService;
        this.budgetRepository = budgetRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con username: " + username));
        if (!user.isEnabled()) {
            throw new UsernameNotFoundException("Usuario desactivado: " + username);
        }
        return user;
    }

    /**
     * Obtiene todos los usuarios mapeados a DTO.
     */
    public List<UserDto> findAll() {
        return userRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Busca un usuario por ID y lo devuelve como DTO.
     */
    public Optional<UserDto> findById(Integer id) {
        return userRepository.findById(id).map(this::convertToDto);
    }

    /**
     * Crea un nuevo usuario a partir del DTO. Codifica la contraseña si se proporciona.
     */
    /**
     * Registro público de un nuevo usuario con rol {@code ROLE_CLIENTE}.
     */
    public UserResponseDto register(RegisterRequestDto request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new RegistrationConflictException("El username ya existe");
        }
        if (request.email() != null && !request.email().isBlank()
                && userRepository.existsByEmail(request.email())) {
            throw new RegistrationConflictException("El email ya está registrado");
        }

        Role role = roleService.getOrCreateByName("ROLE_CLIENTE");
        User newUser = new User();
        newUser.setUsername(request.username());
        newUser.setEmail(request.email());
        newUser.setPassword(passwordEncoder.encode(request.password()));
        newUser.setFullName(request.fullName());
        newUser.setRoles(new HashSet<>(java.util.Collections.singletonList(role)));
        newUser.setActive(true);

        User saved = userRepository.save(newUser);
        return new UserResponseDto(saved.getId(), saved.getUsername(), saved.getEmail(), saved.getFullName());
    }

    public UserDto save(UserDto userDto) {
        validateRoles(userDto.roles(), true);
        User user = convertToEntity(userDto);
        if (userDto.password() != null && !userDto.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(userDto.password()));
        }
        User saved = userRepository.save(user);
        return convertToDto(saved);
    }

    /**
     * Actualiza un usuario existente. Solo modifica los campos no nulos del DTO.
     */
    public UserDto update(Integer id, UserDto userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + id));

        if (userDto.username() != null) user.setUsername(userDto.username());
        if (userDto.email() != null) user.setEmail(userDto.email());
        if (userDto.fullName() != null) user.setFullName(userDto.fullName());

        if (userDto.password() != null && !userDto.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(userDto.password()));
        }

        if (userDto.roles() != null) {
            validateRoles(userDto.roles(), false);
            user.setRoles(userDto.roles().stream()
                    .map(roleName -> roleRepository.findByName(roleName)
                            .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado: " + roleName)))
                    .collect(Collectors.toSet()));
        }

        User updated = userRepository.save(user);
        return convertToDto(updated);
    }

    /**
     * Desactiva (soft delete) un usuario por ID.
     * Verifica que no tenga presupuestos activos y que no sea el único administrador.
     */
    public void deleteById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + id));

        // Verificar que no sea el último administrador activo
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
        if (isAdmin) {
            long adminCount = userRepository.countActiveByRoleName("ROLE_ADMIN");
            if (adminCount <= 1) {
                throw new IllegalStateException("No se puede desactivar el último administrador del sistema.");
            }
        }

        // Verificar que no tenga presupuestos activos (DRAFT o FINALIZED)
        long activeBudgets = budgetRepository.countByCreatedByAndStatus(user.getUsername(), "DRAFT")
                + budgetRepository.countByCreatedByAndStatus(user.getUsername(), "FINALIZED");
        if (activeBudgets > 0) {
            throw new IllegalStateException(
                    "No se puede desactivar el usuario porque tiene " + activeBudgets + " presupuesto(s) activo(s).");
        }

        // Soft delete: marcar como inactivo
        user.setActive(false);
        userRepository.save(user);
    }

    /**
     * Actualiza el perfil del usuario autenticado (email, nombre completo, contraseña).
     */
    public UserDto updateProfile(String username, UserDto userDto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con username: " + username));

        if (userDto.email() != null) user.setEmail(userDto.email());
        if (userDto.fullName() != null) user.setFullName(userDto.fullName());
        if (userDto.password() != null && !userDto.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(userDto.password()));
        }

        User updated = userRepository.save(user);
        return convertToDto(updated);
    }

    private UserDto convertToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                null,
                user.getRoles() != null
                        ? user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
                        : new HashSet<>()
        );
    }

    private User convertToEntity(UserDto userDto) {
        User user = new User();
        user.setUsername(userDto.username());
        user.setEmail(userDto.email());
        user.setFullName(userDto.fullName());

        if (userDto.roles() != null) {
            user.setRoles(userDto.roles().stream()
                    .map(roleName -> roleRepository.findByName(roleName)
                            .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado: " + roleName)))
                    .collect(Collectors.toSet()));
        } else {
            user.setRoles(new HashSet<>());
        }
        return user;
    }

    private void validateRoles(java.util.Set<String> roles, boolean required) {
        if (required && (roles == null || roles.isEmpty())) {
            throw new IllegalArgumentException("El usuario debe tener al menos un rol.");
        }
        if (roles != null && roles.size() > 1) {
            throw new IllegalArgumentException("El usuario solo puede tener un rol.");
        }
    }
}

