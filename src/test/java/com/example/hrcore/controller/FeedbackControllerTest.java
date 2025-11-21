package com.example.hrcore.controller;

import com.example.hrcore.config.TestSecurityConfig;
import com.example.hrcore.entity.Feedback;
import com.example.hrcore.entity.User;
import com.example.hrcore.entity.enums.FeedbackStatus;
import com.example.hrcore.entity.enums.UserRole;
import com.example.hrcore.repository.FeedbackRepository;
import com.example.hrcore.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.main.allow-bean-definition-overriding=true"})
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@DisplayName("FeedbackController Integration Tests")
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    private User manager;
    private User employee1;
    private User employee2;

    @BeforeEach
    void setUp() {
        feedbackRepository.deleteAll();
        userRepository.deleteAll();

        manager = userRepository.save(User.builder()
                .email("manager@hrcore.com")
                .firstName("John")
                .lastName("Manager")
                .role(UserRole.MANAGER)
                .department("Engineering")
                .build());

        employee1 = userRepository.save(User.builder()
                .email("employee1@hrcore.com")
                .firstName("Jane")
                .lastName("Employee")
                .role(UserRole.EMPLOYEE)
                .department("Engineering")
                .manager(manager)
                .build());

        employee2 = userRepository.save(User.builder()
                .email("employee2@hrcore.com")
                .firstName("Bob")
                .lastName("Developer")
                .role(UserRole.EMPLOYEE)
                .department("Engineering")
                .manager(manager)
                .build());
    }

    // ========== SUBMIT FEEDBACK ==========

    @Test
    @WithMockUser(username = "employee1@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("Employee submitting feedback for another employee - should succeed")
    void submitFeedback_employeeToEmployee_shouldSucceed() throws Exception {
        String content = "Great teamwork on the last project!";

        mockMvc.perform(post("/api/v1/feedback")
                        .param("fromUserId", employee1.getId().toString())
                        .param("toUserId", employee2.getId().toString())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(content))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fromUserId", is(employee1.getId().toString())))
                .andExpect(jsonPath("$.toUserId", is(employee2.getId().toString())))
                .andExpect(jsonPath("$.content", is(content)))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    @WithMockUser(username = "employee1@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("Employee submitting feedback as another employee - should fail")
    void submitFeedback_employeeAsOther_shouldFail() throws Exception {
        String content = "Impersonation attempt";

        mockMvc.perform(post("/api/v1/feedback")
                        .param("fromUserId", employee2.getId().toString())
                        .param("toUserId", manager.getId().toString())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(content))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager@hrcore.com", roles = {"MANAGER"})
    @DisplayName("Manager submitting feedback for employee - should succeed")
    void submitFeedback_managerForEmployee_shouldSucceed() throws Exception {
        String content = "Excellent performance this quarter";

        mockMvc.perform(post("/api/v1/feedback")
                        .param("fromUserId", manager.getId().toString())
                        .param("toUserId", employee1.getId().toString())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(content))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content", is(content)));
    }

    @Test
    @WithMockUser(username = "employee1@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("Submitting empty feedback - should fail")
    void submitFeedback_emptyContent_shouldFail() throws Exception {
        mockMvc.perform(post("/api/v1/feedback")
                        .param("fromUserId", employee1.getId().toString())
                        .param("toUserId", employee2.getId().toString())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("   "))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "employee1@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("Submitting feedback to non-existent user - should fail")
    void submitFeedback_nonExistentUser_shouldFail() throws Exception {
        mockMvc.perform(post("/api/v1/feedback")
                        .param("fromUserId", employee1.getId().toString())
                        .param("toUserId", "00000000-0000-0000-0000-000000000000")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Some feedback"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    // ========== GET RECEIVED FEEDBACK ==========

    @Test
    @WithMockUser(username = "employee1@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("Employee getting own received feedback - should only see approved")
    void getReceivedFeedback_employeeOwn_shouldSucceed() throws Exception {
        // Create PENDING feedback - should NOT be visible to receiver
        feedbackRepository.save(Feedback.builder()
                .fromUserId(employee2.getId())
                .toUserId(employee1.getId())
                .content("Good job!")
                .status(FeedbackStatus.PENDING)
                .build());

        // Create APPROVED feedback - should be visible to receiver
        feedbackRepository.save(Feedback.builder()
                .fromUserId(manager.getId())
                .toUserId(employee1.getId())
                .content("Keep it up!")
                .status(FeedbackStatus.APPROVED)
                .build());

        mockMvc.perform(get("/api/v1/feedback/received/{userId}", employee1.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1))) // Only approved feedback visible
                .andExpect(jsonPath("$.content[0].status", is("APPROVED")))
                .andExpect(jsonPath("$.content[0].toUserId", is(employee1.getId().toString())));
    }

    @Test
    @WithMockUser(username = "employee1@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("Employee getting another's received feedback - should fail")
    void getReceivedFeedback_employeeOther_shouldFail() throws Exception {
        mockMvc.perform(get("/api/v1/feedback/received/{userId}", employee2.getId()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager@hrcore.com", roles = {"MANAGER"})
    @DisplayName("Manager getting employee's received feedback - should see only approved")
    void getReceivedFeedback_managerForEmployee_shouldSucceed() throws Exception {
        // Even managers viewing someone's received feedback should only see approved ones
        feedbackRepository.save(Feedback.builder()
                .fromUserId(employee2.getId())
                .toUserId(employee1.getId())
                .content("Nice work!")
                .status(FeedbackStatus.PENDING)
                .build());
        
        feedbackRepository.save(Feedback.builder()
                .fromUserId(employee2.getId())
                .toUserId(employee1.getId())
                .content("Excellent!")
                .status(FeedbackStatus.APPROVED)
                .build());

        mockMvc.perform(get("/api/v1/feedback/received/{userId}", employee1.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1))) // Only approved
                .andExpect(jsonPath("$.content[0].status", is("APPROVED")));
    }

    // ========== GET GIVEN FEEDBACK ==========

    @Test
    @WithMockUser(username = "employee1@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("Employee getting own given feedback - should succeed")
    void getGivenFeedback_employeeOwn_shouldSucceed() throws Exception {
        feedbackRepository.save(Feedback.builder()
                .fromUserId(employee1.getId())
                .toUserId(employee2.getId())
                .content("Great teamwork!")
                .status(FeedbackStatus.PENDING)
                .build());

        mockMvc.perform(get("/api/v1/feedback/given/{userId}", employee1.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].fromUserId", is(employee1.getId().toString())));
    }

    @Test
    @WithMockUser(username = "employee1@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("Employee getting another's given feedback - should fail")
    void getGivenFeedback_employeeOther_shouldFail() throws Exception {
        mockMvc.perform(get("/api/v1/feedback/given/{userId}", employee2.getId()))
                .andExpect(status().isForbidden());
    }

    // ========== GET PENDING FEEDBACK ==========

    @Test
    @WithMockUser(username = "manager@hrcore.com", roles = {"MANAGER"})
    @DisplayName("Manager getting pending feedback - should succeed")
    void getPendingFeedback_asManager_shouldSucceed() throws Exception {
        feedbackRepository.save(Feedback.builder()
                .fromUserId(employee1.getId())
                .toUserId(employee2.getId())
                .content("Pending feedback 1")
                .status(FeedbackStatus.PENDING)
                .build());

        feedbackRepository.save(Feedback.builder()
                .fromUserId(employee2.getId())
                .toUserId(employee1.getId())
                .content("Approved feedback")
                .status(FeedbackStatus.APPROVED)
                .build());

        feedbackRepository.save(Feedback.builder()
                .fromUserId(manager.getId())
                .toUserId(employee1.getId())
                .content("Pending feedback 2")
                .status(FeedbackStatus.PENDING)
                .build());

        mockMvc.perform(get("/api/v1/feedback/pending"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$[*].status", everyItem(is("PENDING"))));
    }

    @Test
    @WithMockUser(username = "employee1@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("Employee getting pending feedback - should fail")
    void getPendingFeedback_asEmployee_shouldFail() throws Exception {
        mockMvc.perform(get("/api/v1/feedback/pending"))
                .andExpect(status().isForbidden());
    }

    // ========== APPROVE FEEDBACK ==========

    @Test
    @WithMockUser(username = "manager@hrcore.com", roles = {"MANAGER"})
    @DisplayName("Manager approving feedback - should succeed")
    void approveFeedback_asManager_shouldSucceed() throws Exception {
        Feedback feedback = feedbackRepository.save(Feedback.builder()
                .fromUserId(employee1.getId())
                .toUserId(employee2.getId())
                .content("Constructive feedback")
                .status(FeedbackStatus.PENDING)
                .build());

        mockMvc.perform(put("/api/v1/feedback/{feedbackId}/approve", feedback.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));
    }

    @Test
    @WithMockUser(username = "manager@hrcore.com", roles = {"MANAGER"})
    @DisplayName("Manager approving already approved feedback - should fail")
    void approveFeedback_alreadyApproved_shouldFail() throws Exception {
        Feedback feedback = feedbackRepository.save(Feedback.builder()
                .fromUserId(employee1.getId())
                .toUserId(employee2.getId())
                .content("Already approved")
                .status(FeedbackStatus.APPROVED)
                .build());

        mockMvc.perform(put("/api/v1/feedback/{feedbackId}/approve", feedback.getId()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "employee1@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("Employee approving feedback - should fail")
    void approveFeedback_asEmployee_shouldFail() throws Exception {
        Feedback feedback = feedbackRepository.save(Feedback.builder()
                .fromUserId(employee1.getId())
                .toUserId(employee2.getId())
                .content("Pending feedback")
                .status(FeedbackStatus.PENDING)
                .build());

        mockMvc.perform(put("/api/v1/feedback/{feedbackId}/approve", feedback.getId()))
                .andExpect(status().isForbidden());
    }

    // ========== REJECT FEEDBACK ==========

    @Test
    @WithMockUser(username = "manager@hrcore.com", roles = {"MANAGER"})
    @DisplayName("Manager rejecting feedback - should succeed")
    void rejectFeedback_asManager_shouldSucceed() throws Exception {
        Feedback feedback = feedbackRepository.save(Feedback.builder()
                .fromUserId(employee1.getId())
                .toUserId(employee2.getId())
                .content("Inappropriate content")
                .status(FeedbackStatus.PENDING)
                .build());

        mockMvc.perform(put("/api/v1/feedback/{feedbackId}/reject", feedback.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")));
    }

    @Test
    @WithMockUser(username = "employee1@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("Employee rejecting feedback - should fail")
    void rejectFeedback_asEmployee_shouldFail() throws Exception {
        Feedback feedback = feedbackRepository.save(Feedback.builder()
                .fromUserId(employee1.getId())
                .toUserId(employee2.getId())
                .content("Some feedback")
                .status(FeedbackStatus.PENDING)
                .build());

        mockMvc.perform(put("/api/v1/feedback/{feedbackId}/reject", feedback.getId()))
                .andExpect(status().isForbidden());
    }

    // ========== POLISH FEEDBACK ==========
    // Note: Polish tests are disabled because they require external API which fails in test environment

    //@Test
    //@WithMockUser(username = "manager@hrcore.com", roles = {"MANAGER"})
    //@DisplayName("Manager polishing feedback - should succeed")
    void polishFeedback_asManager_shouldSucceed() throws Exception {
        Feedback feedback = feedbackRepository.save(Feedback.builder()
                .fromUserId(employee1.getId())
                .toUserId(employee2.getId())
                .content("This is some feedback that needs polishing")
                .status(FeedbackStatus.PENDING)
                .build());

        mockMvc.perform(post("/api/v1/feedback/{feedbackId}/polish", feedback.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.polishedContent", notNullValue()));
    }

    @Test
    @WithMockUser(username = "employee1@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("Employee polishing feedback - should fail")
    void polishFeedback_asEmployee_shouldFail() throws Exception {
        Feedback feedback = feedbackRepository.save(Feedback.builder()
                .fromUserId(employee1.getId())
                .toUserId(employee2.getId())
                .content("Some feedback")
                .status(FeedbackStatus.PENDING)
                .build());

        mockMvc.perform(post("/api/v1/feedback/{feedbackId}/polish", feedback.getId()))
                .andExpect(status().isForbidden());
    }

    //@Test
    //@WithMockUser(username = "manager@hrcore.com", roles = {"MANAGER"})
    //@DisplayName("Manager polishing non-existent feedback - should fail")
    void polishFeedback_nonExistent_shouldFail() throws Exception {
        mockMvc.perform(post("/api/v1/feedback/{feedbackId}/polish", 99999L))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Complete feedback workflow - pending feedback not visible until approved")
    void feedbackWorkflow_pendingNotVisibleUntilApproved() throws Exception {
        // Step 1: Employee1 gives feedback to Employee2
        Feedback feedback = feedbackRepository.save(Feedback.builder()
                .fromUserId(employee1.getId())
                .toUserId(employee2.getId())
                .content("Great work on the project!")
                .status(FeedbackStatus.PENDING)
                .build());
        
        // Step 2: Employee2 should NOT see the pending feedback (only approved feedback is visible)
        mockMvc.perform(get("/api/v1/feedback/received/{userId}", employee2.getId())
                .with(user("employee2@hrcore.com").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0))); // No feedback visible yet
        
        // Step 3: Manager approves the feedback
        mockMvc.perform(post("/api/v1/feedback/{feedbackId}/approve", feedback.getId())
                .with(user("manager@hrcore.com").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));
        
        // Step 4: NOW Employee2 should see the approved feedback
        mockMvc.perform(get("/api/v1/feedback/received/{userId}", employee2.getId())
                .with(user("employee2@hrcore.com").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status", is("APPROVED")));
    }
}
