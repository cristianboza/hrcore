package com.example.hrcore.service;

import com.example.hrcore.config.FeatureFlags;
import com.example.hrcore.dto.FeedbackDto;
import com.example.hrcore.dto.FeedbackFilterDto;
import com.example.hrcore.dto.FeedbackOperationContext;
import com.example.hrcore.dto.PageResponse;
import com.example.hrcore.entity.Feedback;
import com.example.hrcore.entity.User;
import com.example.hrcore.entity.enums.FeedbackStatus;
import com.example.hrcore.entity.enums.UserRole;
import com.example.hrcore.exception.InvalidOperationException;
import com.example.hrcore.exception.UnauthorizedException;
import com.example.hrcore.exception.UserNotFoundException;
import com.example.hrcore.mapper.FeedbackMapper;
import com.example.hrcore.repository.FeedbackRepository;
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
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackService Tests")
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FeedbackMapper feedbackMapper;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private FeatureFlags featureFlags;

    @InjectMocks
    private FeedbackService feedbackService;

    private UUID employeeId;
    private UUID managerId;
    private UUID superAdminId;
    private UUID anotherEmployeeId;
    private User employee;
    private User manager;
    private User superAdmin;
    private Feedback pendingFeedback;
    private Feedback approvedFeedback;
    private FeedbackDto feedbackDto;

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

        pendingFeedback = Feedback.builder()
                .id(1L)
                .fromUserId(managerId)
                .toUserId(employeeId)
                .content("Great work!")
                .status(FeedbackStatus.PENDING)
                .build();

        approvedFeedback = Feedback.builder()
                .id(2L)
                .fromUserId(managerId)
                .toUserId(employeeId)
                .content("Excellent performance!")
                .status(FeedbackStatus.APPROVED)
                .build();

        feedbackDto = FeedbackDto.builder()
                .id(1L)
                .fromUser(com.example.hrcore.dto.NamedUserDto.builder()
                        .id(managerId)
                        .firstName("Manager")
                        .lastName("User")
                        .email("manager@example.com")
                        .build())
                .toUser(com.example.hrcore.dto.NamedUserDto.builder()
                        .id(employeeId)
                        .firstName("Employee")
                        .lastName("User")
                        .email("employee@example.com")
                        .build())
                .content("Great work!")
                .status(FeedbackStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("Employee can submit feedback")
    void testEmployeeCanSubmitFeedback() {
        when(userRepository.existsById(employeeId)).thenReturn(true);
        when(userRepository.existsById(managerId)).thenReturn(true);
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(pendingFeedback);
        when(feedbackMapper.toDto(pendingFeedback)).thenReturn(feedbackDto);

        FeedbackOperationContext context = FeedbackOperationContext.builder()
                .currentUserId(employeeId)
                .currentUserRole(UserRole.EMPLOYEE)
                .build();

        FeedbackDto result = feedbackService.submitFeedback(employeeId, managerId, "Great work!", context);

        assertThat(result).isNotNull();
        verify(feedbackRepository).save(any(Feedback.class));
    }

    @Test
    @DisplayName("Employee cannot submit feedback as another user")
    void testEmployeeCannotSubmitFeedbackAsAnotherUser() {
        FeedbackOperationContext context = FeedbackOperationContext.builder()
                .currentUserId(employeeId)
                .currentUserRole(UserRole.EMPLOYEE)
                .build();

        assertThatThrownBy(() -> 
            feedbackService.submitFeedback(anotherEmployeeId, managerId, "Great work!", context))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Manager can submit feedback as another user")
    void testManagerCanSubmitFeedbackAsAnotherUser() {
        when(userRepository.existsById(any(UUID.class))).thenReturn(true);
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(pendingFeedback);
        when(feedbackMapper.toDto(pendingFeedback)).thenReturn(feedbackDto);

        FeedbackOperationContext context = FeedbackOperationContext.builder()
                .currentUserId(managerId)
                .currentUserRole(UserRole.MANAGER)
                .build();

        FeedbackDto result = feedbackService.submitFeedback(employeeId, anotherEmployeeId, "Great work!", context);

        assertThat(result).isNotNull();
        verify(feedbackRepository).save(any(Feedback.class));
    }

    @Test
    @DisplayName("Cannot submit feedback with empty content")
    void testCannotSubmitEmptyFeedback() {
        when(userRepository.existsById(any(UUID.class))).thenReturn(true);

        FeedbackOperationContext context = FeedbackOperationContext.builder()
                .currentUserId(employeeId)
                .currentUserRole(UserRole.EMPLOYEE)
                .build();

        assertThatThrownBy(() -> 
            feedbackService.submitFeedback(employeeId, managerId, "", context))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Content cannot be empty");
    }

    @Test
    @DisplayName("Cannot submit feedback to non-existent user")
    void testCannotSubmitFeedbackToNonExistentUser() {
        when(userRepository.existsById(employeeId)).thenReturn(true);
        when(userRepository.existsById(managerId)).thenReturn(false);

        FeedbackOperationContext context = FeedbackOperationContext.builder()
                .currentUserId(employeeId)
                .currentUserRole(UserRole.EMPLOYEE)
                .build();

        assertThatThrownBy(() -> 
            feedbackService.submitFeedback(employeeId, managerId, "Great work!", context))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("Direct manager can approve pending feedback")
    void testDirectManagerCanApproveFeedback() {
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(pendingFeedback));
        when(userRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(approvedFeedback);
        when(feedbackMapper.toDto(approvedFeedback)).thenReturn(feedbackDto);

        FeedbackOperationContext context = FeedbackOperationContext.builder()
                .currentUserId(managerId)
                .currentUserRole(UserRole.MANAGER)
                .build();

        FeedbackDto result = feedbackService.approveFeedback(1L, context);

        assertThat(result).isNotNull();
        verify(feedbackRepository).save(argThat(feedback -> 
            feedback.getStatus() == FeedbackStatus.APPROVED));
    }

    @Test
    @DisplayName("Super admin can approve pending feedback")
    void testSuperAdminCanApproveFeedback() {
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(pendingFeedback));
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(approvedFeedback);
        when(feedbackMapper.toDto(approvedFeedback)).thenReturn(feedbackDto);

        FeedbackOperationContext context = FeedbackOperationContext.builder()
                .currentUserId(superAdminId)
                .currentUserRole(UserRole.SUPER_ADMIN)
                .build();

        FeedbackDto result = feedbackService.approveFeedback(1L, context);

        assertThat(result).isNotNull();
        verify(feedbackRepository).save(argThat(feedback -> 
            feedback.getStatus() == FeedbackStatus.APPROVED));
    }

    @Test
    @DisplayName("Non-direct manager cannot approve feedback")
    void testNonDirectManagerCannotApproveFeedback() {
        UUID anotherManagerId = UUID.randomUUID();
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(pendingFeedback));
        when(userRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        FeedbackOperationContext context = FeedbackOperationContext.builder()
                .currentUserId(anotherManagerId)
                .currentUserRole(UserRole.MANAGER)
                .build();

        assertThatThrownBy(() -> feedbackService.approveFeedback(1L, context))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Employee cannot approve feedback")
    void testEmployeeCannotApproveFeedback() {
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(pendingFeedback));

        FeedbackOperationContext context = FeedbackOperationContext.builder()
                .currentUserId(employeeId)
                .currentUserRole(UserRole.EMPLOYEE)
                .build();

        assertThatThrownBy(() -> feedbackService.approveFeedback(1L, context))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Cannot approve already approved feedback")
    void testCannotApproveAlreadyApprovedFeedback() {
        when(feedbackRepository.findById(2L)).thenReturn(Optional.of(approvedFeedback));

        FeedbackOperationContext context = FeedbackOperationContext.builder()
                .currentUserId(managerId)
                .currentUserRole(UserRole.MANAGER)
                .build();

        assertThatThrownBy(() -> feedbackService.approveFeedback(2L, context))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already been");
    }

    @Test
    @DisplayName("Direct manager can reject pending feedback")
    void testDirectManagerCanRejectFeedback() {
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(pendingFeedback));
        when(userRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        Feedback rejectedFeedback = Feedback.builder()
                .id(1L)
                .status(FeedbackStatus.REJECTED)
                .build();
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(rejectedFeedback);
        when(feedbackMapper.toDto(rejectedFeedback)).thenReturn(feedbackDto);

        FeedbackOperationContext context = FeedbackOperationContext.builder()
                .currentUserId(managerId)
                .currentUserRole(UserRole.MANAGER)
                .build();

        FeedbackDto result = feedbackService.rejectFeedback(1L, context);

        assertThat(result).isNotNull();
        verify(feedbackRepository).save(argThat(feedback -> 
            feedback.getStatus() == FeedbackStatus.REJECTED));
    }

    @Test
    @DisplayName("Employee can only see approved feedback they received")
    void testEmployeeCanOnlySeeApprovedReceivedFeedback() {
        Page<Feedback> page = new PageImpl<>(List.of(approvedFeedback));
        when(feedbackRepository.findByToUserIdAndStatusOrderByCreatedAtDesc(
                eq(employeeId), eq(FeedbackStatus.APPROVED), any(Pageable.class)))
                .thenReturn(page);
        when(feedbackMapper.toDto(any(Feedback.class))).thenReturn(feedbackDto);

        FeedbackOperationContext context = FeedbackOperationContext.builder()
                .currentUserId(employeeId)
                .currentUserRole(UserRole.EMPLOYEE)
                .build();

        PageResponse<FeedbackDto> result = feedbackService.getReceivedFeedback(employeeId, context);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(feedbackRepository).findByToUserIdAndStatusOrderByCreatedAtDesc(
                eq(employeeId), eq(FeedbackStatus.APPROVED), any(Pageable.class));
    }

    @Test
    @DisplayName("Manager can see all feedback for their direct reports via search")
    void testManagerCanSeeDirectReportsFeedback() {
        Page<Feedback> page = new PageImpl<>(List.of(pendingFeedback, approvedFeedback));
        when(feedbackRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(feedbackMapper.toDto(any(Feedback.class))).thenReturn(feedbackDto);

        FeedbackOperationContext context = FeedbackOperationContext.builder()
                .currentUserId(managerId)
                .currentUserRole(UserRole.MANAGER)
                .build();

        FeedbackFilterDto filters = FeedbackFilterDto.builder()
                .toUserId(employeeId)
                .build();

        PageResponse<FeedbackDto> result = feedbackService.searchFeedback(filters, context);

        assertThat(result).isNotNull();
        verify(feedbackRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Super admin can see all feedback via search")
    void testSuperAdminCanSeeAllFeedback() {
        Page<Feedback> page = new PageImpl<>(List.of(pendingFeedback, approvedFeedback));
        when(feedbackRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(feedbackMapper.toDto(any(Feedback.class))).thenReturn(feedbackDto);

        FeedbackOperationContext context = FeedbackOperationContext.builder()
                .currentUserId(superAdminId)
                .currentUserRole(UserRole.SUPER_ADMIN)
                .build();

        PageResponse<FeedbackDto> result = feedbackService.searchFeedback(new FeedbackFilterDto(), context);

        assertThat(result).isNotNull();
        verify(feedbackRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Employee cannot view another employee's received feedback")
    void testEmployeeCannotViewOtherReceivedFeedback() {
        FeedbackOperationContext context = FeedbackOperationContext.builder()
                .currentUserId(employeeId)
                .currentUserRole(UserRole.EMPLOYEE)
                .build();

        assertThatThrownBy(() -> 
            feedbackService.getReceivedFeedback(anotherEmployeeId, context))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Manager can view pending feedback")
    void testManagerCanViewPendingFeedback() {
        Page<Feedback> page = new PageImpl<>(List.of(pendingFeedback));
        when(feedbackRepository.findByStatusOrderByCreatedAtDesc(
                eq(FeedbackStatus.PENDING), any(Pageable.class)))
                .thenReturn(page);
        when(feedbackMapper.toDto(any(Feedback.class))).thenReturn(feedbackDto);

        FeedbackOperationContext context = FeedbackOperationContext.builder()
                .currentUserId(managerId)
                .currentUserRole(UserRole.MANAGER)
                .build();

        PageResponse<FeedbackDto> result = feedbackService.getPendingFeedback(context);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Employee cannot view pending feedback")
    void testEmployeeCannotViewPendingFeedback() {
        FeedbackOperationContext context = FeedbackOperationContext.builder()
                .currentUserId(employeeId)
                .currentUserRole(UserRole.EMPLOYEE)
                .build();

        assertThatThrownBy(() -> feedbackService.getPendingFeedback(context))
                .isInstanceOf(UnauthorizedException.class);
    }
}
