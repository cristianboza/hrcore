package com.example.hrcore.repository;

import com.example.hrcore.entity.AbsenceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AbsenceRequestRepository extends JpaRepository<AbsenceRequest, Long> {
    List<AbsenceRequest> findByUserIdOrderByStartDateDesc(Long userId);
    List<AbsenceRequest> findByStatusOrderByStartDateDesc(String status);
    List<AbsenceRequest> findByUserIdAndStartDateBetween(Long userId, LocalDate start, LocalDate end);
}

