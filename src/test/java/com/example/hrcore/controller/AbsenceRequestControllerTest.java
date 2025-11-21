package com.example.hrcore.controller;

import com.example.hrcore.config.TestSecurityConfig;
import com.example.hrcore.dto.ManagerAbsenceUpdateDto;
import com.example.hrcore.entity.AbsenceRequest;
import com.example.hrcore.entity.User;
import com.example.hrcore.entity.enums.AbsenceRequestStatus;
import com.example.hrcore.entity.enums.AbsenceRequestType;
import com.example.hrcore.entity.enums.UserRole;
import com.example.hrcore.repository.AbsenceRequestRepository;
import com.example.hrcore.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.main.allow-bean-definition-overriding=true"})
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@DisplayName("AbsenceRequestController Integration Tests")
class AbsenceRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AbsenceRequestRepository absenceRequestRepository;

    private User manager;
    private User employee;

    @BeforeEach
    void setUp() {
        absenceRequestRepository.deleteAll();
        userRepository.deleteAll();

        manager = userRepository.save(User.builder()
                .email("manager@hrcore.com")
                .firstName("John")
                .lastName("Manager")
                .role(UserRole.MANAGER)
                .department("Engineering")
                .build());

        employee = userRepository.save(User.builder()
                .email("employee@hrcore.com")
                .firstName("Jane")
                .lastName("Employee")
                .role(UserRole.EMPLOYEE)
                .department("Engineering")
                .manager(manager)
                .build());
    }

    // ========== SUBMIT ABSENCE REQUEST ==========

    @Test
    @WithMockUser(username = "employee@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("Employee submitting absence request for self - should succeed")
    void submitRequest_employeeForSelf_shouldSucceed() throws Exception {
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(3);

        mockMvc.perform(post("/api/absence-requests")
                        .param("userId", employee.getId().toString())
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .param("type", "VACATION")
                        .param("reason", "Family vacation"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", is(employee.getId().toString())))
                .andExpect(jsonPath("$.type", is("VACATION")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.reason", is("Family vacation")));
    }

    @Test
    @WithMockUser(username = "employee@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("Employee submitting request with past start date - should fail")
    void submitRequest_pastStartDate_shouldFail() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(1);

        mockMvc.perform(post("/api/absence-requests")
                        .param("userId", employee.getId().toString())
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .param("type", "VACATION"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "employee@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("Employee submitting request with end before start - should fail")
    void submitRequest_endBeforeStart_shouldFail() throws Exception {
        LocalDate startDate = LocalDate.now().plusDays(5);
        LocalDate endDate = LocalDate.now().plusDays(2);

        mockMvc.perform(post("/api/absence-requests")
                        .param("userId", employee.getId().toString())
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .param("type", "VACATION"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "employee@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("Employee submitting request for another employee - should fail")
    void submitRequest_employeeForOther_shouldFail() throws Exception {
        User otherEmployee = userRepository.save(User.builder()
                .email("other@hrcore.com")
                .firstName("Other")
                .lastName("Employee")
                .role(UserRole.EMPLOYEE)
                .department("Engineering")
                .build());

        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(3);

        mockMvc.perform(post("/api/absence-requests")
                        .param("userId", otherEmployee.getId().toString())
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .param("type", "VACATION"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager@hrcore.com", roles = {"MANAGER"})
    @DisplayName("Manager submitting request for employee - should succeed")
    void submitRequest_managerForEmployee_shouldSucceed() throws Exception {
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(3);

        mockMvc.perform(post("/api/absence-requests")
                        .param("userId", employee.getId().toString())
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .param("type", "SICK"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("SICK")));
    }

    // ========== GET USER REQUESTS ==========

    @Test
    @WithMockUser(username = "employee@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("Employee getting own requests - should succeed")
    void getUserRequests_employeeOwn_shouldSucceed() throws Exception {
        absenceRequestRepository.save(AbsenceRequest.builder()
                .userId(employee.getId())
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .type(AbsenceRequestType.VACATION)
                .status(AbsenceRequestStatus.PENDING)
                .build());

        mockMvc.perform(get("/api/absence-requests")
                        .param("userId", employee.getId().toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].userId", is(employee.getId().toString())));
    }

    @Test
    @WithMockUser(username = "employee@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("Employee getting another's requests - should fail")
    void getUserRequests_employeeOther_shouldFail() throws Exception {
        mockMvc.perform(get("/api/absence-requests")
                        .param("userId", manager.getId().toString()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager@hrcore.com", roles = {"MANAGER"})
    @DisplayName("Manager getting employee requests - should succeed")
    void getUserRequests_managerForEmployee_shouldSucceed() throws Exception {
        absenceRequestRepository.save(AbsenceRequest.builder()
                .userId(employee.getId())
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .type(AbsenceRequestType.VACATION)
                .status(AbsenceRequestStatus.PENDING)
                .build());

        mockMvc.perform(get("/api/absence-requests")
                        .param("userId", employee.getId().toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    // ========== GET PENDING REQUESTS ==========

    @Test
    @WithMockUser(username = "manager@hrcore.com", roles = {"MANAGER"})
    @DisplayName("Manager getting pending requests - should succeed")
    void getPendingRequests_asManager_shouldSucceed() throws Exception {
        absenceRequestRepository.save(AbsenceRequest.builder()
                .userId(employee.getId())
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .type(AbsenceRequestType.VACATION)
                .status(AbsenceRequestStatus.PENDING)
                .build());

        absenceRequestRepository.save(AbsenceRequest.builder()
                .userId(employee.getId())
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(12))
                .type(AbsenceRequestType.SICK)
                .status(AbsenceRequestStatus.APPROVED)
                .build());

        mockMvc.perform(get("/api/absence-requests/pending"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status", is("PENDING")));
    }

    @Test
    @WithMockUser(username = "employee@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("Employee getting pending requests - should fail")
    void getPendingRequests_asEmployee_shouldFail() throws Exception {
        mockMvc.perform(get("/api/absence-requests/pending"))
                .andExpect(status().isForbidden());
    }

    // ========== APPROVE REQUEST ==========

    @Test
    @WithMockUser(username = "manager@hrcore.com", roles = {"MANAGER"})
    @DisplayName("Manager approving request - should succeed")
    void approveRequest_asManager_shouldSucceed() throws Exception {
        AbsenceRequest request = absenceRequestRepository.save(AbsenceRequest.builder()
                .userId(employee.getId())
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .type(AbsenceRequestType.VACATION)
                .status(AbsenceRequestStatus.PENDING)
                .build());

        mockMvc.perform(put("/api/absence-requests/{requestId}/approve", request.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.approverId", is(manager.getId().toString())));
    }

    @Test
    @WithMockUser(username = "manager@hrcore.com", roles = {"MANAGER"})
    @DisplayName("Manager approving already approved request - should fail")
    void approveRequest_alreadyApproved_shouldFail() throws Exception {
        AbsenceRequest request = absenceRequestRepository.save(AbsenceRequest.builder()
                .userId(employee.getId())
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .type(AbsenceRequestType.VACATION)
                .status(AbsenceRequestStatus.APPROVED)
                .approverId(manager.getId())
                .build());

        mockMvc.perform(put("/api/absence-requests/{requestId}/approve", request.getId()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "employee@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("Employee approving request - should fail")
    void approveRequest_asEmployee_shouldFail() throws Exception {
        AbsenceRequest request = absenceRequestRepository.save(AbsenceRequest.builder()
                .userId(employee.getId())
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .type(AbsenceRequestType.VACATION)
                .status(AbsenceRequestStatus.PENDING)
                .build());

        mockMvc.perform(put("/api/absence-requests/{requestId}/approve", request.getId()))
                .andExpect(status().isForbidden());
    }

    // ========== REJECT REQUEST ==========

    @Test
    @WithMockUser(username = "manager@hrcore.com", roles = {"MANAGER"})
    @DisplayName("Manager rejecting request - should succeed")
    void rejectRequest_asManager_shouldSucceed() throws Exception {
        AbsenceRequest request = absenceRequestRepository.save(AbsenceRequest.builder()
                .userId(employee.getId())
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .type(AbsenceRequestType.VACATION)
                .status(AbsenceRequestStatus.PENDING)
                .build());

        mockMvc.perform(put("/api/absence-requests/{requestId}/reject", request.getId())
                        .param("reason", "Not enough coverage"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")))
                .andExpect(jsonPath("$.rejectionReason", is("Not enough coverage")))
                .andExpect(jsonPath("$.approverId", is(manager.getId().toString())));
    }

    // ========== CHECK CONFLICTS ==========

    @Test
    @WithMockUser(username = "employee@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("Employee checking conflicts for self - should succeed")
    void checkConflicts_employeeOwn_shouldSucceed() throws Exception {
        LocalDate conflictStart = LocalDate.now().plusDays(5);
        LocalDate conflictEnd = LocalDate.now().plusDays(7);

        absenceRequestRepository.save(AbsenceRequest.builder()
                .userId(employee.getId())
                .startDate(conflictStart)
                .endDate(conflictEnd)
                .type(AbsenceRequestType.VACATION)
                .status(AbsenceRequestStatus.APPROVED)
                .build());

        mockMvc.perform(get("/api/absence-requests/conflicts")
                        .param("userId", employee.getId().toString())
                        .param("startDate", conflictStart.toString())
                        .param("endDate", conflictEnd.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    // ========== MANAGER UPDATE ==========

    @Test
    @WithMockUser(username = "manager@hrcore.com", roles = {"MANAGER"})
    @DisplayName("Manager updating absence request - should succeed")
    void managerUpdate_asManager_shouldSucceed() throws Exception {
        AbsenceRequest request = absenceRequestRepository.save(AbsenceRequest.builder()
                .userId(employee.getId())
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .type(AbsenceRequestType.VACATION)
                .status(AbsenceRequestStatus.PENDING)
                .build());

        ManagerAbsenceUpdateDto updateDto = ManagerAbsenceUpdateDto.builder()
                .status("APPROVED")
                .managerComment("Approved with conditions")
                .build();

        mockMvc.perform(patch("/api/absence-requests/{requestId}/manager-update", request.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.rejectionReason", is("Approved with conditions")));
    }

    @Test
    @WithMockUser(username = "employee@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("Employee updating request - should fail")
    void managerUpdate_asEmployee_shouldFail() throws Exception {
        AbsenceRequest request = absenceRequestRepository.save(AbsenceRequest.builder()
                .userId(employee.getId())
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .type(AbsenceRequestType.VACATION)
                .status(AbsenceRequestStatus.PENDING)
                .build());

        ManagerAbsenceUpdateDto updateDto = ManagerAbsenceUpdateDto.builder()
                .status("APPROVED")
                .build();

        mockMvc.perform(patch("/api/absence-requests/{requestId}/manager-update", request.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden());
    }
}
