package com.example.hrcore.service;

import com.example.hrcore.controller.AbsenceRequestController;
import com.example.hrcore.dto.AbsenceRequestDto;
import com.example.hrcore.entity.AbsenceRequest;
import com.example.hrcore.repository.AbsenceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AbsenceRequestService {

    private final AbsenceRequestRepository absenceRequestRepository;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    public static final String TYPE_VACATION = "VACATION";
    public static final String TYPE_SICK = "SICK";
    public static final String TYPE_OTHER = "OTHER";

    public AbsenceRequestDto submitRequest(Long userId, LocalDate startDate, LocalDate endDate, String type, String reason) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }

        AbsenceRequest request = AbsenceRequest.builder()
                .userId(userId)
                .startDate(startDate)
                .endDate(endDate)
                .type(type)
                .reason(reason)
                .status(STATUS_PENDING)
                .build();

        AbsenceRequest saved = absenceRequestRepository.save(request);
        return AbsenceRequestDto.from(saved);
    }

    public List<AbsenceRequestDto> getUserRequests(Long userId) {
        return absenceRequestRepository.findByUserIdOrderByStartDateDesc(userId)
                .stream()
                .map(AbsenceRequestDto::from)
                .toList();
    }

    public List<AbsenceRequestDto> getPendingRequests() {
        return absenceRequestRepository.findByStatusOrderByStartDateDesc(STATUS_PENDING)
                .stream()
                .map(AbsenceRequestDto::from)
                .toList();
    }

    public AbsenceRequestDto approveRequest(Long requestId, Long approverId) {
        Optional<AbsenceRequest> request = absenceRequestRepository.findById(requestId);
        if (request.isEmpty()) {
            throw new IllegalArgumentException("Request not found");
        }

        AbsenceRequest r = request.get();
        r.setStatus(STATUS_APPROVED);
        r.setApproverId(approverId);
        AbsenceRequest updated = absenceRequestRepository.save(r);
        return AbsenceRequestDto.from(updated);
    }

    public AbsenceRequestDto rejectRequest(Long requestId, Long approverId, String reason) {
        Optional<AbsenceRequest> request = absenceRequestRepository.findById(requestId);
        if (request.isEmpty()) {
            throw new IllegalArgumentException("Request not found");
        }

        AbsenceRequest r = request.get();
        r.setStatus(STATUS_REJECTED);
        r.setApproverId(approverId);
        r.setRejectionReason(reason);
        AbsenceRequest updated = absenceRequestRepository.save(r);
        return AbsenceRequestDto.from(updated);
    }

    public List<AbsenceRequestDto> checkConflicts(Long userId, LocalDate startDate, LocalDate endDate) {
        return absenceRequestRepository.findByUserIdAndStartDateBetween(userId, startDate, endDate)
                .stream()
                .map(AbsenceRequestDto::from)
                .toList();
    }

    public AbsenceRequestDto managerUpdateAbsenceRequest(Long requestId, Long managerId, AbsenceRequestController.ManagerAbsenceUpdateDto updateDto) {
        Optional<AbsenceRequest> requestOpt = absenceRequestRepository.findById(requestId);
        if (requestOpt.isEmpty()) {
            throw new IllegalArgumentException("Request not found");
        }
        AbsenceRequest request = requestOpt.get();
        // TODO: Validate managerId is allowed to manage this user's request
        // For now, assume manager can update any request
        if (updateDto.status != null) {
            request.setStatus(updateDto.status);
        }
        if (updateDto.managerComment != null) {
            request.setRejectionReason(updateDto.managerComment); // Or add a new field for manager comment
        }
        AbsenceRequest updated = absenceRequestRepository.save(request);
        return AbsenceRequestDto.from(updated);
    }
}
