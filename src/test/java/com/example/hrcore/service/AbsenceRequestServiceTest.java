package com.example.hrcore.service;

import com.example.hrcore.dto.AbsenceRequestDto;
import com.example.hrcore.dto.AbsenceRequestFilterDto;
import com.example.hrcore.dto.AbsenceRequestOperationContext;
import com.example.hrcore.dto.NamedUserDto;
import com.example.hrcore.dto.PageResponse;
import com.example.hrcore.entity.AbsenceRequest;
import com.example.hrcore.entity.User;
import com.example.hrcore.entity.enums.AbsenceRequestStatus;
import com.example.hrcore.entity.enums.AbsenceRequestType;
import com.example.hrcore.entity.enums.UserRole;
import com.example.hrcore.exception.InvalidOperationException;
import com.example.hrcore.exception.UnauthorizedException;
import com.example.hrcore.exception.UserNotFoundException;
import com.example.hrcore.mapper.AbsenceRequestMapper;
import com.example.hrcore.repository.AbsenceRequestRepository;
import com.example.hrcore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AbsenceRequestService Tests")
class AbsenceRequestServiceTest {

    @Mock
    private AbsenceRequestRepository absenceRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AbsenceRequestMapper mapper;

    @InjectMocks
    private AbsenceRequestService absenceRequestService;

    private UUID employeeId;
    private UUID managerId;
    private UUID superAdminId;
    private UUID anotherEmployeeId;
    private User employee;
    private User manager;
    private User superAdmin;
    private AbsenceRequest pendingRequest;
    private AbsenceRequest approvedRequest;
    private AbsenceRequestDto requestDto;
    private PageResponse<AbsenceRequestDto> pageResponse;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        managerId = UUID.randomUUID();
        superAdminId = UUID.randomUUID();
        anotherEmployeeId = UUID.randomUUID();

        manager = User.builder()
                .id(managerId)
                .email("manager@test.com")
                .role(UserRole.MANAGER)
                .build();

        employee = User.builder()
                .id(employeeId)
                .email("employee@test.com")
                .role(UserRole.EMPLOYEE)
                .manager(manager)
                .build();

        superAdmin = User.builder()
                .id(superAdminId)
                .email("admin@test.com")
                .role(UserRole.SUPER_ADMIN)
                .build();

        pendingRequest = AbsenceRequest.builder()
                .id(1L)
                .userId(employeeId)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(15))
                .type(AbsenceRequestType.VACATION)
                .reason("Family vacation")
                .status(AbsenceRequestStatus.PENDING)
                .build();

        approvedRequest = AbsenceRequest.builder()
                .id(2L)
                .userId(employeeId)
                .startDate(LocalDate.now().plusDays(20))
                .endDate(LocalDate.now().plusDays(25))
                .type(AbsenceRequestType.VACATION)
                .reason("Summer holiday")
                .status(AbsenceRequestStatus.APPROVED)
                .approverId(managerId)
                .build();

        requestDto = AbsenceRequestDto.builder()
                .id(1L)
                .user(NamedUserDto.builder()
                        .id(employeeId)
                        .firstName("Employee")
                        .lastName("User")
                        .email("employee@example.com")
                        .build())
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(15))
                .type(AbsenceRequestType.VACATION)
                .reason("Family vacation")
                .status(AbsenceRequestStatus.PENDING)
                .build();

        pageResponse = PageResponse.<AbsenceRequestDto>builder()
                .content(List.of(requestDto))
                .totalElements(1L)
                .totalPages(1)
                .page(0)
                .size(10)
                .first(true)
                .last(true)
                .build();
    }

    @Test
    @DisplayName("Employee can submit absence request for future dates")
    void testEmployeeCanSubmitAbsenceRequest() {
        when(userRepository.existsById(employeeId)).thenReturn(true);
        when(absenceRequestRepository.save(any(AbsenceRequest.class))).thenReturn(pendingRequest);
        when(mapper.toDto(pendingRequest)).thenReturn(requestDto);

        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(employeeId)
                .currentUserRole(UserRole.EMPLOYEE)
                .build();

        AbsenceRequestDto result = absenceRequestService.submitRequest(
                employeeId,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15),
                "VACATION",
                "Family vacation",
                context
        );

        assertThat(result).isNotNull();
        verify(absenceRequestRepository).save(any(AbsenceRequest.class));
    }

    @Test
    @DisplayName("Cannot submit absence request with start date after end date")
    void testCannotSubmitRequestWithInvalidDates() {
        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(employeeId)
                .currentUserRole(UserRole.EMPLOYEE)
                .build();

        assertThatThrownBy(() -> absenceRequestService.submitRequest(
                employeeId,
                LocalDate.now().plusDays(15),
                LocalDate.now().plusDays(10),
                "VACATION",
                "Invalid dates",
                context
        ))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Start date cannot be after end date");
    }

    @Test
    @DisplayName("Cannot submit absence request for past dates")
    void testCannotSubmitRequestForPastDates() {
        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(employeeId)
                .currentUserRole(UserRole.EMPLOYEE)
                .build();

        assertThatThrownBy(() -> absenceRequestService.submitRequest(
                employeeId,
                LocalDate.now().minusDays(5),
                LocalDate.now().minusDays(1),
                "VACATION",
                "Past dates",
                context
        ))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot request absence for past dates");
    }

    @Test
    @DisplayName("Employee cannot submit request for another user")
    void testEmployeeCannotSubmitRequestForAnotherUser() {
        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(employeeId)
                .currentUserRole(UserRole.EMPLOYEE)
                .build();

        assertThatThrownBy(() -> absenceRequestService.submitRequest(
                anotherEmployeeId,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15),
                "VACATION",
                "On behalf",
                context
        ))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Manager can submit request for another user")
    void testManagerCanSubmitRequestForAnotherUser() {
        when(userRepository.existsById(employeeId)).thenReturn(true);
        when(absenceRequestRepository.save(any(AbsenceRequest.class))).thenReturn(pendingRequest);
        when(mapper.toDto(pendingRequest)).thenReturn(requestDto);

        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(managerId)
                .currentUserRole(UserRole.MANAGER)
                .build();

        AbsenceRequestDto result = absenceRequestService.submitRequest(
                employeeId,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15),
                "VACATION",
                "On behalf",
                context
        );

        assertThat(result).isNotNull();
        verify(absenceRequestRepository).save(any(AbsenceRequest.class));
    }

    @Test
    @DisplayName("Cannot submit request for non-existent user")
    void testCannotSubmitRequestForNonExistentUser() {
        when(userRepository.existsById(employeeId)).thenReturn(false);

        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(employeeId)
                .currentUserRole(UserRole.EMPLOYEE)
                .build();

        assertThatThrownBy(() -> absenceRequestService.submitRequest(
                employeeId,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15),
                "VACATION",
                "Non-existent",
                context
        ))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("Direct manager can approve pending request")
    void testDirectManagerCanApproveRequest() {
        when(absenceRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(absenceRequestRepository.save(any(AbsenceRequest.class))).thenReturn(approvedRequest);
        when(mapper.toDto(approvedRequest)).thenReturn(requestDto);

        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(managerId)
                .currentUserRole(UserRole.MANAGER)
                .build();

        AbsenceRequestDto result = absenceRequestService.approveRequest(1L, context);

        assertThat(result).isNotNull();
        verify(absenceRequestRepository).save(argThat(request ->
                request.getStatus() == AbsenceRequestStatus.APPROVED &&
                request.getApproverId().equals(managerId)
        ));
    }

    @Test
    @DisplayName("Super admin can approve pending request")
    void testSuperAdminCanApproveRequest() {
        when(absenceRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));
        when(absenceRequestRepository.save(any(AbsenceRequest.class))).thenReturn(approvedRequest);
        when(mapper.toDto(approvedRequest)).thenReturn(requestDto);

        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(superAdminId)
                .currentUserRole(UserRole.SUPER_ADMIN)
                .build();

        AbsenceRequestDto result = absenceRequestService.approveRequest(1L, context);

        assertThat(result).isNotNull();
        verify(absenceRequestRepository).save(argThat(request ->
                request.getStatus() == AbsenceRequestStatus.APPROVED
        ));
    }

    @Test
    @DisplayName("Non-direct manager cannot approve request")
    void testNonDirectManagerCannotApproveRequest() {
        UUID anotherManagerId = UUID.randomUUID();
        when(absenceRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(anotherManagerId)
                .currentUserRole(UserRole.MANAGER)
                .build();

        assertThatThrownBy(() -> absenceRequestService.approveRequest(1L, context))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Employee cannot approve request")
    void testEmployeeCannotApproveRequest() {
        when(absenceRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));

        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(employeeId)
                .currentUserRole(UserRole.EMPLOYEE)
                .build();

        assertThatThrownBy(() -> absenceRequestService.approveRequest(1L, context))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Cannot approve already approved request")
    void testCannotApproveAlreadyApprovedRequest() {
        when(absenceRequestRepository.findById(2L)).thenReturn(Optional.of(approvedRequest));

        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(managerId)
                .currentUserRole(UserRole.MANAGER)
                .build();

        assertThatThrownBy(() -> absenceRequestService.approveRequest(2L, context))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already been");
    }

    @Test
    @DisplayName("Direct manager can reject pending request")
    void testDirectManagerCanRejectRequest() {
        when(absenceRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        AbsenceRequest rejectedRequest = AbsenceRequest.builder()
                .id(1L)
                .status(AbsenceRequestStatus.REJECTED)
                .approverId(managerId)
                .rejectionReason("Not enough coverage")
                .build();
        when(absenceRequestRepository.save(any(AbsenceRequest.class))).thenReturn(rejectedRequest);
        when(mapper.toDto(rejectedRequest)).thenReturn(requestDto);

        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(managerId)
                .currentUserRole(UserRole.MANAGER)
                .build();

        AbsenceRequestDto result = absenceRequestService.rejectRequest(1L, "Not enough coverage", context);

        assertThat(result).isNotNull();
        verify(absenceRequestRepository).save(argThat(request ->
                request.getStatus() == AbsenceRequestStatus.REJECTED &&
                request.getApproverId().equals(managerId) &&
                request.getRejectionReason().equals("Not enough coverage")
        ));
    }

    @Test
    @DisplayName("Super admin can reject pending request")
    void testSuperAdminCanRejectRequest() {
        when(absenceRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));
        AbsenceRequest rejectedRequest = AbsenceRequest.builder()
                .id(1L)
                .status(AbsenceRequestStatus.REJECTED)
                .build();
        when(absenceRequestRepository.save(any(AbsenceRequest.class))).thenReturn(rejectedRequest);
        when(mapper.toDto(rejectedRequest)).thenReturn(requestDto);

        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(superAdminId)
                .currentUserRole(UserRole.SUPER_ADMIN)
                .build();

        AbsenceRequestDto result = absenceRequestService.rejectRequest(1L, "Rejected by admin", context);

        assertThat(result).isNotNull();
        verify(absenceRequestRepository).save(argThat(request ->
                request.getStatus() == AbsenceRequestStatus.REJECTED
        ));
    }

    @Test
    @DisplayName("Employee cannot reject request")
    void testEmployeeCannotRejectRequest() {
        when(absenceRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));

        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(employeeId)
                .currentUserRole(UserRole.EMPLOYEE)
                .build();

        assertThatThrownBy(() -> absenceRequestService.rejectRequest(1L, "Reason", context))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Employee can view their own requests")
    void testEmployeeCanViewOwnRequests() {
        Page<AbsenceRequest> page = new PageImpl<>(List.of(pendingRequest, approvedRequest));
        when(absenceRequestRepository.findByUserIdOrderByStartDateDesc(eq(employeeId), any(Pageable.class)))
                .thenReturn(page);
        when(mapper.toPageResponse(page)).thenReturn(pageResponse);

        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(employeeId)
                .currentUserRole(UserRole.EMPLOYEE)
                .build();

        PageResponse<AbsenceRequestDto> result = absenceRequestService.getUserRequests(employeeId, 0, 10, context);

        assertThat(result).isNotNull();
        verify(absenceRequestRepository).findByUserIdOrderByStartDateDesc(eq(employeeId), any(Pageable.class));
    }

    @Test
    @DisplayName("Employee cannot view another employee's requests")
    void testEmployeeCannotViewOtherRequests() {
        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(employeeId)
                .currentUserRole(UserRole.EMPLOYEE)
                .build();

        assertThatThrownBy(() -> absenceRequestService.getUserRequests(anotherEmployeeId, 0, 10, context))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Manager can view direct reports' requests")
    void testManagerCanViewDirectReportsRequests() {
        Page<AbsenceRequest> page = new PageImpl<>(List.of(pendingRequest));
        when(absenceRequestRepository.findByUserIdOrderByStartDateDesc(eq(employeeId), any(Pageable.class)))
                .thenReturn(page);
        when(mapper.toPageResponse(page)).thenReturn(pageResponse);

        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(managerId)
                .currentUserRole(UserRole.MANAGER)
                .build();

        PageResponse<AbsenceRequestDto> result = absenceRequestService.getUserRequests(employeeId, 0, 10, context);

        assertThat(result).isNotNull();
        verify(absenceRequestRepository).findByUserIdOrderByStartDateDesc(eq(employeeId), any(Pageable.class));
    }

    @Test
    @DisplayName("Super admin can view any user's requests")
    void testSuperAdminCanViewAnyRequests() {
        Page<AbsenceRequest> page = new PageImpl<>(List.of(pendingRequest));
        when(absenceRequestRepository.findByUserIdOrderByStartDateDesc(eq(employeeId), any(Pageable.class)))
                .thenReturn(page);
        when(mapper.toPageResponse(page)).thenReturn(pageResponse);

        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(superAdminId)
                .currentUserRole(UserRole.SUPER_ADMIN)
                .build();

        PageResponse<AbsenceRequestDto> result = absenceRequestService.getUserRequests(employeeId, 0, 10, context);

        assertThat(result).isNotNull();
        verify(absenceRequestRepository).findByUserIdOrderByStartDateDesc(eq(employeeId), any(Pageable.class));
    }

    @Test
    @DisplayName("Search with filters applies role-based filtering")
    void testSearchWithFiltersAppliesRoleBasedFiltering() {
        Page<AbsenceRequest> page = new PageImpl<>(List.of(pendingRequest));
        when(absenceRequestRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(mapper.toPageResponse(page)).thenReturn(pageResponse);

        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(managerId)
                .currentUserRole(UserRole.MANAGER)
                .sortBy("startDate")
                .sortDirection("DESC")
                .build();

        AbsenceRequestFilterDto filters = AbsenceRequestFilterDto.builder()
                .status(AbsenceRequestStatus.PENDING)
                .build();

        PageResponse<AbsenceRequestDto> result = absenceRequestService.searchAbsenceRequests(filters, 0, 10, context);

        assertThat(result).isNotNull();
        verify(absenceRequestRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Check conflicts for overlapping requests")
    void testCheckConflicts() {
        Page<AbsenceRequest> page = new PageImpl<>(List.of(pendingRequest));
        when(absenceRequestRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(mapper.toPageResponse(page)).thenReturn(pageResponse);

        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(employeeId)
                .currentUserRole(UserRole.EMPLOYEE)
                .build();

        PageResponse<AbsenceRequestDto> result = absenceRequestService.checkConflicts(
                employeeId,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15),
                0,
                10,
                context
        );

        assertThat(result).isNotNull();
        verify(absenceRequestRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Employee cannot check conflicts for another user")
    void testEmployeeCannotCheckConflictsForAnotherUser() {
        AbsenceRequestOperationContext context = AbsenceRequestOperationContext.builder()
                .currentUserId(employeeId)
                .currentUserRole(UserRole.EMPLOYEE)
                .build();

        assertThatThrownBy(() -> absenceRequestService.checkConflicts(
                anotherEmployeeId,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15),
                0,
                10,
                context
        ))
                .isInstanceOf(UnauthorizedException.class);
    }
}
