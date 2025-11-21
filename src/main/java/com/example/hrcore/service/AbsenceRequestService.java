package com.example.hrcore.service;

import com.example.hrcore.dto.AbsenceRequestDto;
import com.example.hrcore.dto.AbsenceRequestFilterDto;
import com.example.hrcore.dto.AbsenceRequestOperationContext;
import com.example.hrcore.dto.ManagerAbsenceUpdateDto;
import com.example.hrcore.dto.PageResponse;
import com.example.hrcore.entity.AbsenceRequest;
import com.example.hrcore.entity.enums.AbsenceRequestStatus;
import com.example.hrcore.entity.enums.AbsenceRequestType;
import com.example.hrcore.entity.enums.UserRole;
import com.example.hrcore.exception.InvalidOperationException;
import com.example.hrcore.exception.UnauthorizedException;
import com.example.hrcore.exception.UserNotFoundException;
import com.example.hrcore.mapper.AbsenceRequestMapper;
import com.example.hrcore.repository.AbsenceRequestRepository;
import com.example.hrcore.repository.UserRepository;
import com.example.hrcore.specification.AbsenceRequestSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AbsenceRequestService {

    private final AbsenceRequestRepository absenceRequestRepository;
    private final UserRepository userRepository;
    private final AbsenceRequestMapper mapper;

    /**
     * Search absence requests with filtering and pagination
     */
    public PageResponse<AbsenceRequestDto> searchAbsenceRequests(
            AbsenceRequestFilterDto filters,
            int page,
            int size,
            AbsenceRequestOperationContext context) {
        
        log.debug("Searching absence requests with filters: {}, page: {}, size: {}", filters, page, size);
        
        // Role-based filtering is applied at query level in specification
        Specification<AbsenceRequest> spec = AbsenceRequestSpecification.withFilters(
            filters,
            context.getCurrentUserId(),
            context.getCurrentUserRole()
        );
        
        // Build pageable with sorting from context
        Sort sort = Sort.by(
            "DESC".equalsIgnoreCase(context.getSortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC,
            context.getSortBy() != null ? context.getSortBy() : "startDate"
        );
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<AbsenceRequest> resultPage = absenceRequestRepository.findAll(spec, pageable);
        PageResponse<AbsenceRequestDto> response = mapper.toPageResponse(resultPage);
        
        // Set canApprove flag for each request
        response.getContent().forEach(dto -> 
            dto.setCanApprove(canApproveOrReject(dto.getUser().getId(), context))
        );
        
        return response;
    }

    /**
     * Submit a new absence request
     */
    @Transactional
    public AbsenceRequestDto submitRequest(
            UUID userId, 
            LocalDate startDate, 
            LocalDate endDate, 
            String typeStr, 
            String reason,
            AbsenceRequestOperationContext context) {
        
        AbsenceRequestType type = AbsenceRequestType.fromString(typeStr);
        
        // Validate dates
        if (startDate.isAfter(endDate)) {
            throw new InvalidOperationException("submit absence request", 
                "Start date cannot be after end date");
        }
        
        if (startDate.isBefore(LocalDate.now())) {
            throw new InvalidOperationException("submit absence request", 
                "Cannot request absence for past dates");
        }
        
        // Authorization: Can only submit for self unless manager+
        if (!Objects.equals(userId, context.getCurrentUserId()) &&
            !context.getCurrentUserRole().isManagerOrAbove()) {
            throw new UnauthorizedException("submit absence request", "another user");
        }
        
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        
        AbsenceRequest request = AbsenceRequest.builder()
                .userId(userId)
                .startDate(startDate)
                .endDate(endDate)
                .type(type)
                .reason(reason)
                .status(AbsenceRequestStatus.PENDING)
                .createdById(context.getCurrentUserId())
                .build();

        AbsenceRequest saved = absenceRequestRepository.save(request);
        log.info("Absence request created - User: {}, CreatedBy: {}, Dates: {} to {}, Type: {}", 
            userId, context.getCurrentUserId(), startDate, endDate, type);
        return mapper.toDto(saved);
    }

    /**
     * Get user's absence requests with pagination
     */
    public PageResponse<AbsenceRequestDto> getUserRequests(
            UUID userId,
            int page,
            int size,
            AbsenceRequestOperationContext context) {
        
        // Authorization: Can only view own requests unless manager+
        if (!Objects.equals(userId, context.getCurrentUserId()) &&
            !context.getCurrentUserRole().isManagerOrAbove()) {
            throw new UnauthorizedException("view absence requests", "another user");
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startDate"));
        Page<AbsenceRequest> resultPage = absenceRequestRepository.findByUserIdOrderByStartDateDesc(userId, pageable);
        PageResponse<AbsenceRequestDto> response = mapper.toPageResponse(resultPage);
        
        // Set canApprove flag for each request
        response.getContent().forEach(dto -> 
            dto.setCanApprove(canApproveOrReject(dto.getUser().getId(), context))
        );
        
        return response;
    }

    /**
     * Get pending requests with pagination
     */
    public PageResponse<AbsenceRequestDto> getPendingRequests(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startDate"));
        Page<AbsenceRequest> resultPage = absenceRequestRepository.findByStatusOrderByStartDateDesc(
            AbsenceRequestStatus.PENDING, pageable);
        return mapper.toPageResponse(resultPage);
    }

    /**
     * Approve an absence request
     */
    @Transactional
    public AbsenceRequestDto approveRequest(Long requestId, AbsenceRequestOperationContext context) {
        
        AbsenceRequest request = absenceRequestRepository.findById(requestId)
                .orElseThrow(() -> new InvalidOperationException("approve absence request", 
                    "Request not found with ID: " + requestId));

        if (request.getStatus() != AbsenceRequestStatus.PENDING) {
            throw new InvalidOperationException("approve absence request", 
                "Request has already been " + request.getStatus().name().toLowerCase());
        }

        // Authorization: Only direct manager or super admin can approve
        if (!canApproveOrReject(request.getUserId(), context)) {
            throw new UnauthorizedException("approve absence request", "only direct manager or super admin");
        }

        request.setStatus(AbsenceRequestStatus.APPROVED);
        request.setApproverId(context.getCurrentUserId());
        AbsenceRequest updated = absenceRequestRepository.save(request);
        
        log.info("Absence request approved - ID: {}, Approver: {}", requestId, context.getCurrentUserId());
        return mapper.toDto(updated);
    }

    /**
     * Reject an absence request
     */
    @Transactional
    public AbsenceRequestDto rejectRequest(Long requestId, String reason, AbsenceRequestOperationContext context) {
        
        AbsenceRequest request = absenceRequestRepository.findById(requestId)
                .orElseThrow(() -> new InvalidOperationException("reject absence request", 
                    "Request not found with ID: " + requestId));

        if (request.getStatus() != AbsenceRequestStatus.PENDING) {
            throw new InvalidOperationException("reject absence request", 
                "Request has already been " + request.getStatus().name().toLowerCase());
        }

        // Authorization: Only direct manager or super admin can reject
        if (!canApproveOrReject(request.getUserId(), context)) {
            throw new UnauthorizedException("reject absence request", "only direct manager or super admin");
        }

        request.setStatus(AbsenceRequestStatus.REJECTED);
        request.setApproverId(context.getCurrentUserId());
        request.setRejectionReason(reason);
        AbsenceRequest updated = absenceRequestRepository.save(request);
        
        log.info("Absence request rejected - ID: {}, Approver: {}, Reason: {}", 
            requestId, context.getCurrentUserId(), reason);
        return mapper.toDto(updated);
    }

    /**
     * Check if the current user can approve/reject for the given employee
     * Only direct manager or super admin can approve/reject
     */
    private boolean canApproveOrReject(UUID employeeId, AbsenceRequestOperationContext context) {
        // Super admin can do anything
        if (context.getCurrentUserRole() == UserRole.SUPER_ADMIN) {
            return true;
        }
        
        // Must be at least a manager
        if (!context.getCurrentUserRole().isManagerOrAbove()) {
            return false;
        }
        
        // Check if current user is the direct manager of the employee
        return userRepository.findById(employeeId)
                .map(employee -> employee.getManager() != null && 
                               employee.getManager().getId().equals(context.getCurrentUserId()))
                .orElse(false);
    }

    /**
     * Check for conflicting absence requests
     */
    public PageResponse<AbsenceRequestDto> checkConflicts(
            UUID userId,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size,
            AbsenceRequestOperationContext context) {
        
        // Authorization: Can only check conflicts for self unless manager+
        if (!Objects.equals(userId, context.getCurrentUserId()) && 
            !context.getCurrentUserRole().isManagerOrAbove()) {
            throw new UnauthorizedException("check conflicts", "another user");
        }
        
        // Use specification for more complex conflict detection
        AbsenceRequestFilterDto filters = AbsenceRequestFilterDto.builder()
                .userId(userId)
                .startDateFrom(startDate)
                .startDateTo(endDate)
                .build();
        
        Specification<AbsenceRequest> spec = AbsenceRequestSpecification.withFilters(
            filters,
            context.getCurrentUserId(),
            context.getCurrentUserRole()
        );
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "startDate"));
        
        Page<AbsenceRequest> resultPage = absenceRequestRepository.findAll(spec, pageable);
        return mapper.toPageResponse(resultPage);
    }

    /**
     * Manager update of absence request
     */
    @Transactional
    public AbsenceRequestDto managerUpdateAbsenceRequest(
            Long requestId,
            ManagerAbsenceUpdateDto updateDto,
            AbsenceRequestOperationContext context) {
        
        AbsenceRequest request = absenceRequestRepository.findById(requestId)
                .orElseThrow(() -> new InvalidOperationException("update absence request", 
                    "Request not found with ID: " + requestId));

        // Authorization: Only direct manager or super admin can update
        if (!canApproveOrReject(request.getUserId(), context)) {
            throw new UnauthorizedException("update absence request", "only direct manager or super admin");
        }

        if (updateDto.getStatus() != null) {
            AbsenceRequestStatus newStatus = AbsenceRequestStatus.fromString(updateDto.getStatus());
            request.setStatus(newStatus);
            request.setApproverId(context.getCurrentUserId());
        }
        
        if (updateDto.getManagerComment() != null) {
            request.setRejectionReason(updateDto.getManagerComment());
        }
        
        AbsenceRequest updated = absenceRequestRepository.save(request);
        log.info("Absence request updated - ID: {}, Manager: {}", requestId, context.getCurrentUserId());
        return mapper.toDto(updated);
    }
}
