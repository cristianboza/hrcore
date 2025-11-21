package com.example.hrcore.controller;

import com.example.hrcore.dto.AbsenceRequestDto;
import com.example.hrcore.dto.AbsenceRequestFilterDto;
import com.example.hrcore.dto.AbsenceRequestOperationContext;
import com.example.hrcore.dto.ManagerAbsenceUpdateDto;
import com.example.hrcore.dto.PageResponse;
import com.example.hrcore.entity.User;
import com.example.hrcore.entity.enums.AbsenceRequestStatus;
import com.example.hrcore.entity.enums.AbsenceRequestType;
import com.example.hrcore.security.annotation.RequireAuthenticated;
import com.example.hrcore.security.annotation.RequireManagerOrAbove;
import com.example.hrcore.service.AbsenceRequestService;
import com.example.hrcore.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/absence-requests")
@RequiredArgsConstructor
@Tag(name = "Absence Requests", description = "Absence request management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class AbsenceRequestController {

    private final AbsenceRequestService absenceRequestService;
    private final AuthenticationService authenticationService;

    /**
     * Search absence requests with advanced filtering and pagination
     */
    @RequireAuthenticated
    @PostMapping("/search")
    @Operation(
        summary = "Search absence requests",
        description = "Search and filter absence requests with pagination. Employees see their own requests, managers see their team's requests, admins see all."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved absence requests"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    public ResponseEntity<PageResponse<AbsenceRequestDto>> searchRequests(
            @Parameter(description = "Search filters and pagination") @RequestBody com.example.hrcore.dto.AbsenceRequestSearchRequest searchRequest,
            Authentication authentication) {
        
        User currentUser = authenticationService.getCurrentUser(authentication);
        log.info("Searching absence requests by {}, search: {}, userId: {}, status: {}", 
            currentUser.getEmail(), searchRequest.getSearch(), searchRequest.getUserId(), searchRequest.getStatus());
        
        AbsenceRequestFilterDto filters = AbsenceRequestFilterDto.builder()
                .search(searchRequest.getSearch())
                .userId(searchRequest.getUserId())
                .status(searchRequest.getStatus() != null ? AbsenceRequestStatus.valueOf(searchRequest.getStatus().toUpperCase()) : null)
                .type(searchRequest.getType() != null ? AbsenceRequestType.valueOf(searchRequest.getType().toUpperCase()) : null)
                .startDateFrom(searchRequest.getStartDateFrom())
                .startDateTo(searchRequest.getStartDateTo())
                .endDateFrom(searchRequest.getEndDateFrom())
                .endDateTo(searchRequest.getEndDateTo())
                .approverId(searchRequest.getApproverId())
                .managerId(searchRequest.getManagerId())
                .hasRejectionReason(searchRequest.getHasRejectionReason())
                .build();
        
        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(currentUser.getId())
                .currentUserRole(currentUser.getRole())
                .sortBy(searchRequest.getSortBy())
                .sortDirection(searchRequest.getSortDirection())
                .build();
        
        PageResponse<AbsenceRequestDto> results = absenceRequestService.searchAbsenceRequests(
                filters, searchRequest.getPage(), searchRequest.getSize(), context);
        
        log.debug("Found {} requests", results.getTotalElements());
        return ResponseEntity.ok(results);
    }

    /**
     * Submit a new absence request
     */
    @RequireAuthenticated
    @PostMapping
    @Operation(
        summary = "Submit new absence request",
        description = "Create a new absence request. Employees can create for themselves, managers can create for their team."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Absence request created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    public ResponseEntity<AbsenceRequestDto> submitRequest(
            @Parameter(description = "User ID") @RequestParam UUID userId,
            @Parameter(description = "Start date") @RequestParam LocalDate startDate,
            @Parameter(description = "End date") @RequestParam LocalDate endDate,
            @Parameter(description = "Request type (VACATION, SICK_LEAVE, etc)") @RequestParam String type,
            @Parameter(description = "Optional reason") @RequestParam(required = false) String reason,
            Authentication authentication) {
        
        User currentUser = authenticationService.getCurrentUser(authentication);
        log.info("Creating absence request for user {} by {}", userId, currentUser.getEmail());
        
        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(currentUser.getId())
                .currentUserRole(currentUser.getRole())
                .build();
        
        AbsenceRequestDto request = absenceRequestService.submitRequest(
            userId, startDate, endDate, type, reason, context);
        
        log.info("Absence request created successfully with ID: {}", request.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    /**
     * Approve an absence request (Manager+ only)
     */
    @RequireManagerOrAbove
    @PutMapping("/{requestId}/approve")
    @Operation(
        summary = "Approve absence request",
        description = "Approve a pending absence request. Requires MANAGER role or above."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Request approved successfully"),
        @ApiResponse(responseCode = "404", description = "Request not found", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires MANAGER role", content = @Content)
    })
    public ResponseEntity<AbsenceRequestDto> approveRequest(
            @Parameter(description = "Request ID") @PathVariable Long requestId,
            Authentication authentication) {
        
        User currentUser = authenticationService.getCurrentUser(authentication);
        log.info("Approving absence request {} by {}", requestId, currentUser.getEmail());
        
        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(currentUser.getId())
                .currentUserRole(currentUser.getRole())
                .build();
        
        AbsenceRequestDto request = absenceRequestService.approveRequest(requestId, context);
        log.info("Absence request {} approved successfully", requestId);
        return ResponseEntity.ok(request);
    }

    /**
     * Reject an absence request (Manager+ only)
     */
    @RequireManagerOrAbove
    @PutMapping("/{requestId}/reject")
    public ResponseEntity<AbsenceRequestDto> rejectRequest(
            @PathVariable Long requestId,
            @RequestParam String reason,
            Authentication authentication) {
        
        User currentUser = authenticationService.getCurrentUser(authentication);
        log.info("Rejecting absence request {} by {}", requestId, currentUser.getEmail());
        
        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(currentUser.getId())
                .currentUserRole(currentUser.getRole())
                .build();
        
        AbsenceRequestDto request = absenceRequestService.rejectRequest(requestId, reason, context);
        log.info("Absence request {} rejected successfully", requestId);
        return ResponseEntity.ok(request);
    }

    @RequireAuthenticated
    @PostMapping("/conflicts")
    @Operation(
        summary = "Check for conflicts",
        description = "Check for conflicting absence requests for a user within a date range"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully checked for conflicts"),
        @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    public ResponseEntity<PageResponse<AbsenceRequestDto>> checkConflicts(
            @Parameter(description = "Conflict check request") @Valid @RequestBody com.example.hrcore.dto.ConflictCheckRequest request,
            Authentication authentication) {
        
        User currentUser = authenticationService.getCurrentUser(authentication);
        log.info("Checking conflicts for user {} between {} and {}", 
            request.getUserId(), request.getStartDate(), request.getEndDate());
        
        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(currentUser.getId())
                .currentUserRole(currentUser.getRole())
                .build();
        
        PageResponse<AbsenceRequestDto> conflicts = absenceRequestService.checkConflicts(
            request.getUserId(), request.getStartDate(), request.getEndDate(), 
            request.getPage(), request.getSize(), context);
        
        log.debug("Found {} conflicts", conflicts.getTotalElements());
        return ResponseEntity.ok(conflicts);
    }

    /**
     * Manager update of absence request (Manager+ only)
     */
    @RequireManagerOrAbove
    @PatchMapping("/{requestId}/manager-update")
    public ResponseEntity<AbsenceRequestDto> managerUpdateAbsenceRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody ManagerAbsenceUpdateDto updateDto,
            Authentication authentication) {
        
        User currentUser = authenticationService.getCurrentUser(authentication);
        log.info("Manager updating absence request {} by {}", requestId, currentUser.getEmail());
        
        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(currentUser.getId())
                .currentUserRole(currentUser.getRole())
                .build();
        
        AbsenceRequestDto request = absenceRequestService.managerUpdateAbsenceRequest(
            requestId, updateDto, context);
        
        log.info("Absence request {} updated successfully", requestId);
        return ResponseEntity.ok(request);
    }
}
