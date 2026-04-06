package com.omnistock.backend.repository;

import com.omnistock.backend.entity.DeadLetterQueue;
import com.omnistock.backend.entity.DeadLetterQueue.DlqStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeadLetterQueueRepository extends JpaRepository<DeadLetterQueue, Long> {

    List<DeadLetterQueue> findByStatus(DlqStatus status);

    List<DeadLetterQueue> findBySupplierIdAndStatus(Long supplierId, DlqStatus status);

    long countByStatus(DlqStatus status);
}
