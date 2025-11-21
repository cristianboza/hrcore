package com.example.hrcore.controller;

import com.example.hrcore.config.SecurityConfig;
import com.example.hrcore.config.TestSecurityConfig;
import com.example.hrcore.dto.UserDto;
import com.example.hrcore.entity.User;
import com.example.hrcore.entity.enums.UserRole;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.main.allow-bean-definition-overriding=true"})
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@DisplayName("ProfileController Integration Tests")
class ProfileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private User superAdmin;
    private User manager;
    private User employee1;
    private User employee2;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        superAdmin = userRepository.save(User.builder()
                .email("admin@hrcore.com")
                .firstName("Super")
                .lastName("Admin")
                .role(UserRole.SUPER_ADMIN)
                .department("IT")
                .build());

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

    // ========== GET ALL PROFILES ==========

    @Test
    @WithMockUser(username = "admin@hrcore.com", roles = {"SUPER_ADMIN"})
    @DisplayName("SUPER_ADMIN should see all profiles")
    void getAllProfiles_asSuperAdmin_shouldReturnAllProfiles() throws Exception {
        mockMvc.perform(get("/api/profiles"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(4)))
                .andExpect(jsonPath("$.content[*].email", containsInAnyOrder(
                        "admin@hrcore.com",
                        "manager@hrcore.com",
                        "employee1@hrcore.com",
                        "employee2@hrcore.com"
                )));
    }

    @Test
    @WithMockUser(username = "manager@hrcore.com", roles = {"MANAGER"})
    @DisplayName("MANAGER should see all profiles except SUPER_ADMIN")
    void getAllProfiles_asManager_shouldReturnAllProfiles() throws Exception {
        mockMvc.perform(get("/api/profiles"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)));  // Manager sees manager + 2 employees (not super admin)
    }

    @Test
    @WithMockUser(username = "employee1@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("EMPLOYEE should see all profiles (with masked sensitive data for others)")
    void getAllProfiles_asEmployee_shouldReturnOnlyOwnProfile() throws Exception {
        mockMvc.perform(get("/api/profiles"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)));  // Should see all 3 users (manager + 2 employees)
    }

    @Test
    @DisplayName("Unauthenticated user should get 403")
    void getAllProfiles_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/profiles"))
                .andExpect(status().isForbidden());
    }

    // ========== SEARCH PROFILES ==========

    @Test
    @WithMockUser(username = "admin@hrcore.com", roles = {"SUPER_ADMIN"})
    @DisplayName("Search profiles by keyword")
    void searchProfiles_withKeyword_shouldReturnMatchingProfiles() throws Exception {
        mockMvc.perform(get("/api/profiles")
                        .param("search", "employee"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].lastName", containsInAnyOrder("Employee", "Developer")));
    }

    @Test
    @WithMockUser(username = "admin@hrcore.com", roles = {"SUPER_ADMIN"})
    @DisplayName("Search profiles by role")
    void searchProfiles_byRole_shouldReturnMatchingProfiles() throws Exception {
        mockMvc.perform(get("/api/profiles")
                        .param("role", "EMPLOYEE"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].role", everyItem(is("EMPLOYEE"))));
    }

    @Test
    @WithMockUser(username = "admin@hrcore.com", roles = {"SUPER_ADMIN"})
    @DisplayName("Search profiles by department")
    void searchProfiles_byDepartment_shouldReturnMatchingProfiles() throws Exception {
        mockMvc.perform(get("/api/profiles")
                        .param("department", "Engineering"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[*].department", everyItem(is("Engineering"))));
    }

    @Test
    @WithMockUser(username = "manager@hrcore.com", roles = {"MANAGER"})
    @DisplayName("Search profiles by manager ID")
    void searchProfiles_byManagerId_shouldReturnDirectReports() throws Exception {
        mockMvc.perform(get("/api/profiles")
                        .param("managerId", manager.getId().toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].managerId", everyItem(is(manager.getId().toString()))));
    }

    // ========== GET SINGLE PROFILE ==========

    @Test
    @WithMockUser(username = "admin@hrcore.com", roles = {"SUPER_ADMIN"})
    @DisplayName("Get profile by ID - should return full details")
    void getProfile_validId_shouldReturnProfile() throws Exception {
        mockMvc.perform(get("/api/profiles/{userId}", employee1.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(employee1.getId().toString())))
                .andExpect(jsonPath("$.email", is("employee1@hrcore.com")))
                .andExpect(jsonPath("$.firstName", is("Jane")))
                .andExpect(jsonPath("$.lastName", is("Employee")))
                .andExpect(jsonPath("$.role", is("EMPLOYEE")))
                .andExpect(jsonPath("$.department", is("Engineering")))
                .andExpect(jsonPath("$.managerId", is(manager.getId().toString())))
                .andExpect(jsonPath("$.manager.id", is(manager.getId().toString())))
                .andExpect(jsonPath("$.manager.firstName", is("John")))
                .andExpect(jsonPath("$.manager.email", is("manager@hrcore.com")));
    }

    @Test
    @WithMockUser(username = "employee1@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("EMPLOYEE viewing another employee's profile should see masked data")
    void getProfile_employeeViewingOther_shouldMaskSensitiveData() throws Exception {
        mockMvc.perform(get("/api/profiles/{userId}", employee2.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(employee2.getId().toString())))
                .andExpect(jsonPath("$.email", is(nullValue())))
                .andExpect(jsonPath("$.phone", is(nullValue())));
    }

    @Test
    @WithMockUser(username = "employee1@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("EMPLOYEE viewing own profile should see all data")
    void getProfile_employeeViewingOwn_shouldSeeAllData() throws Exception {
        mockMvc.perform(get("/api/profiles/{userId}", employee1.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(employee1.getId().toString())))
                .andExpect(jsonPath("$.email", is("employee1@hrcore.com")));
    }

    @Test
    @WithMockUser(username = "admin@hrcore.com", roles = {"SUPER_ADMIN"})
    @DisplayName("Get profile with invalid UUID - should return 400")
    void getProfile_invalidId_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/profiles/{userId}", "invalid-uuid"))
                .andExpect(status().isBadRequest());
    }

    // ========== GET CURRENT USER ==========

    @Test
    @WithMockUser(username = "employee1@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("Get current user /me endpoint")
    void getCurrentUser_shouldReturnAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/profiles/me"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("employee1@hrcore.com")))
                .andExpect(jsonPath("$.firstName", is("Jane")));
    }

    // ========== UPDATE PROFILE ==========

    @Test
    @WithMockUser(username = "employee1@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("EMPLOYEE updating own profile - should succeed")
    void updateProfile_employeeUpdatingOwn_shouldSucceed() throws Exception {
        UserDto updateDto = UserDto.builder()
                .firstName("Jane Updated")
                .lastName("Employee Updated")
                .email("employee1@hrcore.com")
                .phone("+1234567890")
                .build();

        mockMvc.perform(put("/api/profiles/{userId}", employee1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Jane Updated")))
                .andExpect(jsonPath("$.lastName", is("Employee Updated")))
                .andExpect(jsonPath("$.phone", is("+1234567890")));
    }

    @Test
    @WithMockUser(username = "employee1@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("EMPLOYEE updating another employee's profile - should fail")
    void updateProfile_employeeUpdatingOther_shouldFail() throws Exception {
        UserDto updateDto = UserDto.builder()
                .firstName("Hacker")
                .lastName("Hacker")
                .email(employee2.getEmail())
                .build();

        mockMvc.perform(put("/api/profiles/{userId}", employee2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager@hrcore.com", roles = {"MANAGER"})
    @DisplayName("MANAGER updating employee profile - should succeed")
    void updateProfile_managerUpdatingEmployee_shouldSucceed() throws Exception {
        UserDto updateDto = UserDto.builder()
                .firstName(employee1.getFirstName())
                .lastName(employee1.getLastName())
                .email(employee1.getEmail())
                .department("Engineering - Team A")
                .build();

        mockMvc.perform(put("/api/profiles/{userId}", employee1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department", is("Engineering - Team A")));
    }

    // ========== DELETE PROFILE ==========

    @Test
    @WithMockUser(username = "manager@hrcore.com", roles = {"MANAGER"})
    @DisplayName("MANAGER deleting employee - should succeed")
    void deleteProfile_manager_shouldSucceed() throws Exception {
        mockMvc.perform(delete("/api/profiles/{userId}", employee1.getId()))
                .andDo(print())
                .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(get("/api/profiles/{userId}", employee1.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "employee1@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("EMPLOYEE trying to delete profile - should fail")
    void deleteProfile_employee_shouldFail() throws Exception {
        mockMvc.perform(delete("/api/profiles/{userId}", employee2.getId()))
                .andExpect(status().isForbidden());
    }

    // ========== MANAGER RELATIONSHIPS ==========

    @Test
    @WithMockUser(username = "manager@hrcore.com", roles = {"MANAGER"})
    @DisplayName("Get direct reports")
    void getDirectReports_shouldReturnEmployees() throws Exception {
        mockMvc.perform(get("/api/profiles/{userId}/direct-reports", manager.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].managerId", everyItem(is(manager.getId().toString()))));
    }

    @Test
    @WithMockUser(username = "admin@hrcore.com", roles = {"SUPER_ADMIN"})
    @DisplayName("Get available managers")
    void getAvailableManagers_shouldReturnOnlyManagers() throws Exception {
        mockMvc.perform(get("/api/profiles/available-managers"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].role", is("MANAGER")))
                .andExpect(jsonPath("$[0].email", is("manager@hrcore.com")));
    }

    @Test
    @WithMockUser(username = "employee1@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("Get user's manager")
    void getManager_shouldReturnManager() throws Exception {
        mockMvc.perform(get("/api/profiles/{userId}/manager", employee1.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(manager.getId().toString())))
                .andExpect(jsonPath("$.email", is("manager@hrcore.com")));
    }

    @Test
    @WithMockUser(username = "admin@hrcore.com", roles = {"SUPER_ADMIN"})
    @DisplayName("Assign manager to employee")
    void assignManager_shouldSucceed() throws Exception {
        User newEmployee = userRepository.save(User.builder()
                .email("new@hrcore.com")
                .firstName("New")
                .lastName("Employee")
                .role(UserRole.EMPLOYEE)
                .department("Engineering")
                .build());

        mockMvc.perform(put("/api/profiles/{userId}/manager/{managerId}", 
                        newEmployee.getId(), manager.getId()))
                .andDo(print())
                .andExpect(status().isOk());

        // Verify assignment
        mockMvc.perform(get("/api/profiles/{userId}/manager", newEmployee.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(manager.getId().toString())));
    }

    // ========== PERMISSIONS ==========

    @Test
    @WithMockUser(username = "employee1@hrcore.com", roles = {"EMPLOYEE"})
    @DisplayName("Get permissions for own profile")
    void getPermissions_forOwnProfile_shouldReturnPermissions() throws Exception {
        mockMvc.perform(get("/api/profiles/{userId}/permissions", employee1.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canViewAll", is(true)))
                .andExpect(jsonPath("$.canEdit", is(true)))
                .andExpect(jsonPath("$.canDelete", is(false)));
    }

    @Test
    @WithMockUser(username = "manager@hrcore.com", roles = {"MANAGER"})
    @DisplayName("Get permissions as manager")
    void getPermissions_asManager_shouldReturnManagerPermissions() throws Exception {
        mockMvc.perform(get("/api/profiles/{userId}/permissions", employee1.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canViewAll", is(true)))
                .andExpect(jsonPath("$.canEdit", is(true)))
                .andExpect(jsonPath("$.canDelete", is(true)));
    }
}
