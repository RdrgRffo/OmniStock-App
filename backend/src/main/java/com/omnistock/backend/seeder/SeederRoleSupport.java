package com.omnistock.backend.seeder;

import com.omnistock.backend.entity.Role;
import com.omnistock.backend.repository.RoleRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Utilidades compartidas para seeders de roles (arranque paralelo backend1/backend2).
 */
@Component
public class SeederRoleSupport {

    private final RoleRepository roleRepository;

    public SeederRoleSupport(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
    public void saveRoleIfAbsent(String roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            try {
                roleRepository.save(new Role(roleName));
                System.out.println("Seeder: Rol creado -> " + roleName);
            } catch (DataIntegrityViolationException e) {
                System.out.println("Seeder: Rol ya existente (insertado por otra instancia) -> " + roleName);
            }
        }
    }
}
