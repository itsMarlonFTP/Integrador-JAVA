package com.axity.curso.retailservice.repository;

import com.axity.curso.retailservice.model.AsyncEventLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsyncEventLogRepository extends JpaRepository<AsyncEventLog, Long> {
    Page<AsyncEventLog> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
