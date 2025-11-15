package com.example.hrcore.controller;

import com.example.hrcore.dto.AbsenceRequestDto;
import com.example.hrcore.service.AbsenceRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/absence-requests")
@RequiredArgsConstructor
public class AbsenceRequestController {

    private final AbsenceRequestService absenceRequestService;

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<?> submitRequest(
            @RequestParam Long userId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam String type,
            @RequestParam(required = false) String reason) {
        log.info("POST /api/absence-requests - Submit request for user {} from {} to {} (type: {})", userId, startDate, endDate, type);
        try {
            AbsenceRequestDto request = absenceRequestService.submitRequest(userId, startDate, endDate, type, reason);
            log.info("POST /api/absence-requests - Request created successfully with ID: {}", request.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(request);
        } catch (IllegalArgumentException e) {
            log.error("POST /api/absence-requests - Invalid argument: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("POST /api/absence-requests - Error submitting request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<List<AbsenceRequestDto>> getUserRequests(@RequestParam Long userId) {
        log.info("GET /api/absence-requests - Fetching requests for user {}", userId);
        try {
            List<AbsenceRequestDto> requests = absenceRequestService.getUserRequests(userId);
            log.info("GET /api/absence-requests - Found {} requests for user {}", requests.size(), userId);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            log.error("GET /api/absence-requests - Error fetching user requests: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    @GetMapping("/pending")
    public ResponseEntity<List<AbsenceRequestDto>> getPendingRequests() {
        log.info("GET /api/absence-requests/pending - Fetching pending requests");
        try {
            List<AbsenceRequestDto> requests = absenceRequestService.getPendingRequests();
            log.info("GET /api/absence-requests/pending - Found {} pending requests", requests.size());
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            log.error("GET /api/absence-requests/pending - Error fetching pending requests: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    @PutMapping("/{requestId}/approve")
    public ResponseEntity<?> approveRequest(
            @PathVariable Long requestId,
            @RequestParam Long approverId) {
        log.info("PUT /api/absence-requests/{}/approve - Approving by user {}", requestId, approverId);
        try {
            AbsenceRequestDto request = absenceRequestService.approveRequest(requestId, approverId);
            log.info("PUT /api/absence-requests/{}/approve - Request approved successfully", requestId);
            return ResponseEntity.ok(request);
        } catch (IllegalArgumentException e) {
            log.error("PUT /api/absence-requests/{}/approve - Request not found: {}", requestId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("PUT /api/absence-requests/{}/approve - Error approving request: {}", requestId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    @PutMapping("/{requestId}/reject")
    public ResponseEntity<?> rejectRequest(
            @PathVariable Long requestId,
            @RequestParam Long approverId,
            @RequestParam String reason) {
        log.info("PUT /api/absence-requests/{}/reject - Rejecting by user {} with reason: {}", requestId, approverId, reason);
        try {
            AbsenceRequestDto request = absenceRequestService.rejectRequest(requestId, approverId, reason);
            log.info("PUT /api/absence-requests/{}/reject - Request rejected successfully", requestId);
            return ResponseEntity.ok(request);
        } catch (IllegalArgumentException e) {
            log.error("PUT /api/absence-requests/{}/reject - Request not found: {}", requestId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("PUT /api/absence-requests/{}/reject - Error rejecting request: {}", requestId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'SUPER_ADMIN')")
    @GetMapping("/conflicts")
    public ResponseEntity<List<AbsenceRequestDto>> checkConflicts(
            @RequestParam Long userId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        log.info("GET /api/absence-requests/conflicts - Checking conflicts for user {} from {} to {}", userId, startDate, endDate);
        try {
            List<AbsenceRequestDto> conflicts = absenceRequestService.checkConflicts(userId, startDate, endDate);
            log.info("GET /api/absence-requests/conflicts - Found {} conflicts", conflicts.size());
            return ResponseEntity.ok(conflicts);
        } catch (Exception e) {
            log.error("GET /api/absence-requests/conflicts - Error checking conflicts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
    @PatchMapping("/{requestId}/manager-update")
    public ResponseEntity<?> managerUpdateAbsenceRequest(
            @PathVariable Long requestId,
            @RequestParam Long managerId,
            @RequestBody ManagerAbsenceUpdateDto updateDto) {
        log.info("PATCH /api/absence-requests/{}/manager-update - Updating by manager {}", requestId, managerId);
        try {
            AbsenceRequestDto updated = absenceRequestService.managerUpdateAbsenceRequest(requestId, managerId, updateDto);
            log.info("PATCH /api/absence-requests/{}/manager-update - Request updated successfully", requestId);
            return ResponseEntity.ok(updated);
        } catch (SecurityException e) {
            log.warn("PATCH /api/absence-requests/{}/manager-update - Security exception: {}", requestId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Permission denied: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("PATCH /api/absence-requests/{}/manager-update - Invalid argument: {}", requestId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("PATCH /api/absence-requests/{}/manager-update - Error updating request: {}", requestId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @lombok.Data
    public static class ManagerAbsenceUpdateDto {
        public String status;
        public String managerComment;
    }
}
