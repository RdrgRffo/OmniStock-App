package com.omnistock.backend.repository;

import com.omnistock.backend.entity.BudgetLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad BudgetLine.
 */
@Repository
public interface BudgetLineRepository extends JpaRepository<BudgetLine, Long> {

    /**
     * Busca todas las líneas de un presupuesto.
     */
    List<BudgetLine> findByBudgetId(Long budgetId);
}
