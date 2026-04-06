package com.omnistock.backend.repository;

import com.omnistock.backend.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad Budget.
 */
@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    /**
     * Busca todos los presupuestos de un usuario, ordenados por fecha de creación descendente.
     */
    List<Budget> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    /**
     * Busca un presupuesto por su número único.
     */
    Optional<Budget> findByBudgetNumber(String budgetNumber);

    /**
     * Busca presupuestos por estado.
     */
    List<Budget> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * Busca presupuestos de un usuario filtrados por estado.
     */
    List<Budget> findByCreatedByAndStatusOrderByCreatedAtDesc(String createdBy, String status);

    /**
     * Cuenta los presupuestos de un usuario con un estado específico.
     */
    long countByCreatedByAndStatus(String createdBy, String status);
}
