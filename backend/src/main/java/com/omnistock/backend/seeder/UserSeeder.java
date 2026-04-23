package com.omnistock.backend.seeder;

import com.omnistock.backend.entity.Role;
import com.omnistock.backend.entity.User;
import com.omnistock.backend.repository.RoleRepository;
import com.omnistock.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

@Component
@Order(2)
@Profile("!test")
@ConditionalOnProperty(name = "omnistock.seed.enabled", havingValue = "true", matchIfMissing = true)
public class UserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserSeeder(UserRepository userRepository,
                      RoleRepository roleRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUser("admin", "admin123", "admin@omnistock.local", "Administrador del Sistema", "ROLE_ADMIN");
        seedUser("cliente", "cliente123", "cliente@omnistock.local", "Cliente de Prueba", "ROLE_CLIENTE");
        // Compatibilidad: credenciales antiguas renombradas a cliente
        migrateLegacyUsername("usuario", "cliente", "ROLE_CLIENTE");
    }

    private void seedUser(String username, String password, String email, String fullName, String roleName) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        Optional<Role> roleOpt = roleRepository.findByName(roleName);
        if (roleOpt.isEmpty()) {
            System.out.println("Seeder: Rol " + roleName + " no encontrado; omitiendo usuario " + username);
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setFullName(fullName);
        user.setActive(true);
        user.setRoles(new HashSet<>(Collections.singletonList(roleOpt.get())));
        userRepository.save(user);
        System.out.println("Seeder: Usuario creado (" + username + " / " + password + ") -> " + roleName);
    }

    private void migrateLegacyUsername(String oldUsername, String newUsername, String roleName) {
        if (!userRepository.existsByUsername(oldUsername)) {
            return;
        }
        if (userRepository.existsByUsername(newUsername)) {
            userRepository.findByUsername(oldUsername).ifPresent(userRepository::delete);
            System.out.println("Seeder: Eliminado usuario legacy '" + oldUsername + "' (ya existe '" + newUsername + "')");
            return;
        }
        userRepository.findByUsername(oldUsername).ifPresent(user -> {
            user.setUsername(newUsername);
            roleRepository.findByName(roleName).ifPresent(role -> {
                user.setRoles(new HashSet<>(Collections.singletonList(role)));
            });
            userRepository.save(user);
            System.out.println("Seeder: Usuario renombrado " + oldUsername + " -> " + newUsername);
        });
    }
}
