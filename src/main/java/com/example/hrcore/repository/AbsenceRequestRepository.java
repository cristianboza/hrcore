package com.example.hrcore.repository;

import com.example.hrcore.entity.AbsenceRequest;
import com.example.hrcore.entity.enums.AbsenceRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for AbsenceRequest entities.
 * Supports both simple queries and complex filtering via Specification API.
 */
@Repository
public interface AbsenceRequestRepository extends 
        JpaRepository<AbsenceRequest, Long>,
        JpaSpecificationExecutor<AbsenceRequest> {
    
    Page<AbsenceRequest> findByUserIdOrderByStartDateDesc(UUID userId, Pageable pageable);
    Page<AbsenceRequest> findByStatusOrderByStartDateDesc(AbsenceRequestStatus status, Pageable pageable);
}

