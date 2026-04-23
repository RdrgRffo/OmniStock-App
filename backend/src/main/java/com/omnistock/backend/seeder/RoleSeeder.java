package com.omnistock.backend.seeder;

import com.omnistock.backend.entity.Role;
import com.omnistock.backend.entity.User;
import com.omnistock.backend.repository.RoleRepository;
import com.omnistock.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@Order(1)
@Profile("!test")
@ConditionalOnProperty(name = "omnistock.seed.enabled", havingValue = "true", matchIfMissing = true)
public class RoleSeeder implements CommandLineRunner {

    private static final String LEGACY_USUARIO = "ROLE_USUARIO";
    private static final String ROLE_CLIENTE = "ROLE_CLIENTE";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final SeederRoleSupport seederRoleSupport;

    public RoleSeeder(RoleRepository roleRepository,
                      UserRepository userRepository,
                      SeederRoleSupport seederRoleSupport) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.seederRoleSupport = seederRoleSupport;
    }

    @Override
    public void run(String... args) {
        migrateLegacyUsuarioRole();
        seederRoleSupport.saveRoleIfAbsent(ROLE_ADMIN);
        seederRoleSupport.saveRoleIfAbsent(ROLE_CLIENTE);
    }

    @Transactional
    protected void migrateLegacyUsuarioRole() {
        Optional<Role> legacyOpt = roleRepository.findByName(LEGACY_USUARIO);
        if (legacyOpt.isEmpty()) {
            return;
        }
        Role legacy = legacyOpt.get();
        Role cliente = roleRepository.findByName(ROLE_CLIENTE)
                .orElseGet(() -> roleRepository.save(new Role(ROLE_CLIENTE)));

        for (User user : userRepository.findAll()) {
            if (user.getRoles().remove(legacy)) {
                user.getRoles().add(cliente);
                userRepository.save(user);
            }
        }
        roleRepository.delete(legacy);
        System.out.println("Seeder: Migrado " + LEGACY_USUARIO + " -> " + ROLE_CLIENTE);
    }
}
