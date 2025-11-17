package org.example.user.UnitTests;

import company.user_management_service.DTO.CreateUserRequest;
import company.user_management_service.DTO.LoginRequest;
import company.user_management_service.DTO.LoginResponse;
import company.user_management_service.DTO.UserDTO;
import company.user_management_service.Service.KeycloakService;
import company.user_management_service.Service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private KeycloakService keycloakService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserService userService;

    private CreateUserRequest validCreateRequest;
    private LoginRequest validLoginRequest;
    private LoginRequest invalidLoginRequest;
    private Map<String, Object> sampleUserDetails;

    @BeforeEach
    void setUp() {
        validCreateRequest = createValidUserRequest();
        validLoginRequest = createValidLoginRequest();
        invalidLoginRequest = createInvalidLoginRequest();
        sampleUserDetails = createSampleUserDetails();

        // Reset security context
        SecurityContextHolder.clearContext();
    }

    private CreateUserRequest createValidUserRequest() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setRole("EMPLOYEE");
        return request;
    }

    private LoginRequest createValidLoginRequest() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("correct_password");
        return request;
    }

    private LoginRequest createInvalidLoginRequest() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrong_password");
        return request;
    }

    private Map<String, Object> createSampleUserDetails() {
        Map<String, Object> details = new HashMap<>();
        details.put("userId", "123");
        details.put("username", "testuser");
        details.put("email", "test@example.com");
        details.put("firstName", "Test");
        details.put("lastName", "User");
        details.put("roles", List.of("EMPLOYEE"));
        details.put("status", "ACTIVE");
        details.put("role", "EMPLOYEE");
        details.put("phone", "123-456-7890");
        details.put("address", "123 Test St");
        details.put("hireDate", "2023-01-01");
        details.put("departmentId", "1");
        details.put("managerId", "2");
        details.put("position", "Developer");
        details.put("forcePasswordReset", "false");
        details.put("projectTitles", new ArrayList<>(List.of("Project1")));
        return details;
    }

    // CREATE USER TESTS
    @Test
    void whenCreateUser_withValidRequest_thenKeycloakServiceIsCalledWithCurrentDate() {
        userService.createUser(validCreateRequest);

        verify(keycloakService).createUser(
                eq("testuser"),
                eq("test@example.com"),
                eq("password123"),
                eq("EMPLOYEE"),
                eq("Test"),
                eq("User"),
                isNull(),
                isNull(),
                eq(LocalDate.now().toString()),
                isNull(),
                isNull(),
                isNull()
        );
    }

    @Test
    void whenCreateUser_withProvidedHireDate_thenUsesProvidedDate() {
        validCreateRequest.setHireDate("2023-01-15");

        userService.createUser(validCreateRequest);

        verify(keycloakService).createUser(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                any(), any(),
                eq("2023-01-15"),
                any(), any(), any()
        );
    }

    @Test
    void whenCreateUser_withWhitespaceHireDate_thenUsesCurrentDate() {
        validCreateRequest.setHireDate("   ");

        userService.createUser(validCreateRequest);

        verify(keycloakService).createUser(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                any(), any(),
                eq(LocalDate.now().toString()),
                any(), any(), any()
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    void whenCreateUser_withInvalidEmail_thenKeycloakServiceIsCalled(String email) {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setEmail(email);
        request.setPassword("password123");

        userService.createUser(request);

        verify(keycloakService).createUser(
                eq("testuser"), eq(email), eq("password123"), eq("EMPLOYEE"),
                isNull(), isNull(), isNull(), isNull(),
                eq(LocalDate.now().toString()),
                isNull(), isNull(), isNull()
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    void whenCreateUser_withInvalidPassword_thenKeycloakServiceIsCalled(String password) {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword(password);

        userService.createUser(request);

        verify(keycloakService).createUser(
                eq("testuser"), eq("test@example.com"), eq(password), eq("EMPLOYEE"),
                isNull(), isNull(), isNull(), isNull(),
                eq(LocalDate.now().toString()),
                isNull(), isNull(), isNull()
        );
    }

    @Test
    void whenCreateUser_withKeycloakException_thenThrowsRuntimeException() {
        doThrow(new RuntimeException("Keycloak error")).when(keycloakService).createUser(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                any(), any(), anyString(), any(), any(), any()
        );

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                userService.createUser(validCreateRequest)
        );

        assertEquals("Failed to create user in Keycloak: Keycloak error", exception.getMessage());
    }

    @Test
    void whenCreateUser_withCompleteRequest_thenAllFieldsPassedCorrectly() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setRole("ADMIN");
        request.setPhone("123-456-7890");
        request.setAddress("123 Test St");
        request.setHireDate("2023-01-01");
        request.setDepartmentId(1L);
        request.setManagerId(2L);
        request.setPosition("Developer");

        userService.createUser(request);

        verify(keycloakService).createUser(
                eq("testuser"), eq("test@example.com"), eq("password123"), eq("ADMIN"),
                eq("Test"), eq("User"), eq("123-456-7890"), eq("123 Test St"),
                eq("2023-01-01"), eq(1L), eq(2L), eq("Developer")
        );
    }

    // LOGIN TESTS
    @Test
    void whenLogin_withValidCredentials_thenReturnsLoginResponse() {
        String fakeToken = "fake-jwt-token";
        Map<String, Object> userDetails = createSampleUserDetails();

        when(keycloakService.getToken(validLoginRequest.getUsername(), validLoginRequest.getPassword()))
                .thenReturn(fakeToken);
        when(keycloakService.getUserDetails(validLoginRequest.getUsername()))
                .thenReturn(userDetails);

        LoginResponse response = userService.login(validLoginRequest);

        assertNotNull(response);
        assertEquals(fakeToken, response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("testuser", response.getUsername());
        assertEquals("EMPLOYEE", response.getRole());
        assertEquals("Test", response.getFirstName());
        assertEquals("User", response.getLastName());
        assertFalse(response.isForcePasswordReset());
    }

    @Test
    void whenLogin_withNullUsername_thenThrowsBadCredentialsException() {
        LoginRequest request = new LoginRequest();
        request.setUsername(null);
        request.setPassword("password123");

        BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                () -> userService.login(request));

        assertEquals("Username and password are required", exception.getMessage());
        verify(keycloakService, never()).getToken(anyString(), anyString());
    }

    @Test
    void whenLogin_withNullPassword_thenThrowsBadCredentialsException() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword(null);

        BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                () -> userService.login(request));

        assertEquals("Username and password are required", exception.getMessage());
        verify(keycloakService, never()).getToken(anyString(), anyString());
    }

    @Test
    void whenLogin_withInvalidCredentials_thenThrowsBadCredentialsException() {
        when(keycloakService.getToken(invalidLoginRequest.getUsername(), invalidLoginRequest.getPassword()))
                .thenThrow(new RuntimeException("Keycloak authentication failed"));

        BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                () -> userService.login(invalidLoginRequest));

        assertEquals("Invalid credentials", exception.getMessage());
        verify(keycloakService, never()).getUserDetails(anyString());
    }

    @Test
    void whenLogin_andGetUserDetailsReturnsNull_thenThrowsBadCredentialsException() {
        when(keycloakService.getToken(validLoginRequest.getUsername(), validLoginRequest.getPassword()))
                .thenReturn("fake-token");
        when(keycloakService.getUserDetails(validLoginRequest.getUsername()))
                .thenReturn(null);

        assertThrows(BadCredentialsException.class, () -> userService.login(validLoginRequest));
    }

    // GET USER TESTS
    @Test
    void whenGetUserById_withExistingUser_thenReturnsUserDTO() {
        when(keycloakService.getUserDetailsById("123")).thenReturn(sampleUserDetails);

        UserDTO userDTO = userService.getUserById("123");

        assertNotNull(userDTO);
        assertEquals("123", userDTO.getUserId());
        assertEquals("testuser", userDTO.getUsername());
        assertEquals("test@example.com", userDTO.getEmail());
        assertEquals(UserDTO.UserRole.EMPLOYEE, userDTO.getRole());
        assertEquals(UserDTO.UserStatus.ACTIVE, userDTO.getStatus());
    }

    @Test
    void whenGetUserByUsername_withExistingUser_thenReturnsUserDTO() {
        when(keycloakService.getUserDetails("testuser")).thenReturn(sampleUserDetails);

        UserDTO userDTO = userService.getUserByUsername("testuser");

        assertNotNull(userDTO);
        assertEquals("testuser", userDTO.getUsername());
        assertEquals("test@example.com", userDTO.getEmail());
    }

    @Test
    void whenGetAllUsers_thenReturnsUserDTOList() {
        List<Map<String, Object>> usersList = List.of(sampleUserDetails);
        when(keycloakService.getAllUsers()).thenReturn(usersList);

        List<UserDTO> result = userService.getAllUsers();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).getUsername());
    }

    @Test
    void whenGetUserFullDetails_thenReturnsUserDetails() {
        when(keycloakService.getUserDetails("testuser")).thenReturn(sampleUserDetails);

        Map<String, Object> result = userService.getUserFullDetails("testuser");

        assertNotNull(result);
        assertEquals(sampleUserDetails, result);
    }

    // ROLE MANAGEMENT TESTS
    @Test
    void whenChangeUserRole_withSuperAdminRole_thenRoleIsChanged() {
        setupSecurityContext("admin", List.of("SUPER_ADMIN"));
        when(keycloakService.getUserDetailsById("123")).thenReturn(sampleUserDetails);

        userService.changeUserRole("123", "ADMIN");

        verify(keycloakService).updateUserRole("testuser", "ADMIN");
    }



    @Test
    void whenChangeUserRole_withUnauthenticatedUser_thenThrowsRuntimeException() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.changeUserRole("123", "ADMIN"));

        assertEquals("Failed to change user role: User not authenticated", exception.getMessage());
    }

    // PASSWORD CHANGE TESTS
    @Test
    void whenChangePassword_withShortPassword_thenThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.changePassword("testuser", "currentPassword", "short"));

        assertEquals("Password must be at least 8 characters", exception.getMessage());
        verify(keycloakService, never()).updatePassword(anyString(), anyString(), anyString());
    }

    @Test
    void whenChangePassword_withValidPassword_thenUpdatesPassword() {
        doNothing().when(keycloakService).updatePassword("testuser", "currentPassword", "newPassword123");

        userService.changePassword("testuser", "currentPassword", "newPassword123");

        verify(keycloakService).updatePassword("testuser", "currentPassword", "newPassword123");
    }

    @Test
    void whenChangePassword_withKeycloakException_thenThrowsRuntimeException() {
        doThrow(new RuntimeException("Keycloak error")).when(keycloakService)
                .updatePassword("testuser", "currentPassword", "newPassword123");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.changePassword("testuser", "currentPassword", "newPassword123"));

        assertEquals("Failed to update Keycloak password", exception.getMessage());
    }

    // PROJECT ASSIGNMENT TESTS
    @Test
    void whenAssignProject_withValidInput_thenProjectIsAssigned() {
        String userId = "123";
        String projectTitle = "ProjectX";
        Map<String, Object> userDetails = new HashMap<>(sampleUserDetails);
        userDetails.put("projectTitles", new ArrayList<>());

        Map<String, Object> updatedDetails = new HashMap<>(userDetails);
        updatedDetails.put("projectTitles", List.of(projectTitle));

        when(keycloakService.getUserDetailsById(userId)).thenReturn(userDetails, updatedDetails);
        doNothing().when(keycloakService).updateUserProjects(eq(userId), eq(List.of(projectTitle)));

        UserDTO result = userService.assignProject(userId, projectTitle);

        assertNotNull(result);
        assertTrue(result.getProjectTitles().contains(projectTitle));
        verify(keycloakService, times(2)).getUserDetailsById(userId);
        verify(keycloakService).updateUserProjects(eq(userId), eq(List.of(projectTitle)));
    }

    @Test
    void whenAssignProject_alreadyAssigned_thenNoUpdate() {
        String userId = "123";
        String projectTitle = "ProjectX";
        Map<String, Object> userDetails = new HashMap<>(sampleUserDetails);
        userDetails.put("projectTitles", List.of(projectTitle));

        when(keycloakService.getUserDetailsById(userId)).thenReturn(userDetails);

        UserDTO result = userService.assignProject(userId, projectTitle);

        assertNotNull(result);
        assertTrue(result.getProjectTitles().contains(projectTitle));
        verify(keycloakService, times(1)).getUserDetailsById(userId);
        verify(keycloakService, never()).updateUserProjects(anyString(), anyList());
    }



    @Test
    void whenAssignProject_withUserNotFound_thenThrowsRuntimeException() {
        when(keycloakService.getUserDetailsById("123")).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.assignProject("123", "ProjectX"));

        assertEquals("Failed to assign project: User not found in Keycloak", exception.getMessage());
    }

    @Test
    void whenDeassignProject_withValidInput_thenProjectIsRemoved() {
        String userId = "123";
        String projectTitle = "ProjectX";
        Map<String, Object> userDetails = new HashMap<>(sampleUserDetails);
        userDetails.put("projectTitles", new ArrayList<>(List.of(projectTitle)));

        Map<String, Object> updatedDetails = new HashMap<>(userDetails);
        updatedDetails.put("projectTitles", Collections.emptyList());

        when(keycloakService.getUserDetailsById(userId)).thenReturn(userDetails, updatedDetails);
        doNothing().when(keycloakService).updateUserProjects(eq(userId), anyList());

        UserDTO result = userService.deassignProject(userId, projectTitle);

        assertNotNull(result);
        assertFalse(result.getProjectTitles().contains(projectTitle));
        verify(keycloakService, times(2)).getUserDetailsById(userId);
        verify(keycloakService).updateUserProjects(eq(userId), anyList());
    }

    @Test
    void whenDeassignProject_withProjectNotAssigned_thenNoUpdate() {
        String userId = "123";
        String projectTitle = "ProjectY";
        Map<String, Object> userDetails = new HashMap<>(sampleUserDetails);
        userDetails.put("projectTitles", List.of("ProjectX"));

        when(keycloakService.getUserDetailsById(userId)).thenReturn(userDetails);

        UserDTO result = userService.deassignProject(userId, projectTitle);

        assertNotNull(result);
        assertFalse(result.getProjectTitles().contains(projectTitle));
        verify(keycloakService, times(1)).getUserDetailsById(userId);
        verify(keycloakService, never()).updateUserProjects(anyString(), anyList());
    }

    // PROFILE UPDATE TESTS
    @Test
    void whenUpdateProfile_withValidInput_thenProfileIsUpdated() {
        UserDTO updateRequest = new UserDTO();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Name");
        updateRequest.setEmail("updated@example.com");

        Map<String, Object> updatedDetails = new HashMap<>(sampleUserDetails);
        updatedDetails.put("firstName", "Updated");
        updatedDetails.put("lastName", "Name");
        updatedDetails.put("email", "updated@example.com");

        when(keycloakService.getUserDetailsById("123")).thenReturn(sampleUserDetails, updatedDetails);
        doNothing().when(keycloakService).updateUserAttributes("123", updateRequest);

        UserDTO result = userService.updateProfile("123", updateRequest);

        assertNotNull(result);
        assertEquals("Updated", result.getFirstName());
        assertEquals("Name", result.getLastName());
        verify(keycloakService).updateUserAttributes("123", updateRequest);
    }

    @Test
    void whenUpdateProfile_withUserNotFound_thenThrowsRuntimeException() {
        UserDTO updateRequest = new UserDTO();
        when(keycloakService.getUserDetailsById("123")).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateProfile("123", updateRequest));

        assertEquals("Failed to update user profile", exception.getMessage());
    }

    // USER DELETION TESTS
    @Test
    void whenDeleteUser_withValidUsername_thenUserIsDeleted() {
        doNothing().when(keycloakService).deleteUser("testuser");

        userService.deleteUser("testuser");

        verify(keycloakService).deleteUser("testuser");
    }

    @Test
    void whenDeleteUser_withKeycloakException_thenThrowsRuntimeException() {
        doThrow(new RuntimeException("Keycloak error")).when(keycloakService).deleteUser("testuser");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.deleteUser("testuser"));

        assertEquals("Failed to delete user: Keycloak error", exception.getMessage());
    }

    // UTILITY METHOD TESTS
    @Test
    void whenGetUserNameAndRole_thenReturnsCorrectInfo() {
        when(keycloakService.getUserDetailsById("123")).thenReturn(sampleUserDetails);

        Map<String, String> result = userService.getUserNameAndRole("123");

        assertEquals("Test User", result.get("name"));
        assertEquals("EMPLOYEE", result.get("role"));
    }

    @Test
    void whenGetUserNameAndRoleByUsername_thenReturnsCorrectInfo() {
        when(keycloakService.getUserDetails("testuser")).thenReturn(sampleUserDetails);

        Map<String, String> result = userService.getUserNameAndRoleByUsername("testuser");

        assertEquals("Test User", result.get("name"));
        assertEquals("EMPLOYEE", result.get("role"));
    }

    @Test
    void whenMigrateExistingUsersHireDate_thenMigrationIsTriggered() {
        doNothing().when(keycloakService).migrateExistingUsersWithHireDate();

        userService.migrateExistingUsersHireDate();

        verify(keycloakService).migrateExistingUsersWithHireDate();
    }

    @Test
    void whenMigrateExistingUsersHireDate_withException_thenThrowsRuntimeException() {
        doThrow(new RuntimeException("Migration failed")).when(keycloakService).migrateExistingUsersWithHireDate();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.migrateExistingUsersHireDate());

        assertEquals("Failed to migrate user hireDates", exception.getMessage());
    }

    // CURRENT USER ROLES TESTS
    @Test
    void whenGetCurrentUserRoles_withAuthenticatedUser_thenReturnsRoles() {
        setupSecurityContext("testuser", List.of("ROLE_EMPLOYEE", "default-roles-demo"));
        when(keycloakService.getUserDetails("testuser")).thenReturn(sampleUserDetails);

        Map<String, Object> result = userService.getCurrentUserRoles();

        assertNotNull(result);
        assertEquals("EMPLOYEE", result.get("primaryRole"));
        assertTrue(((List<?>) result.get("keycloakRoles")).contains("ROLE_EMPLOYEE"));
    }

    @Test
    void whenGetCurrentUserRoles_withUnauthenticatedUser_thenThrowsRuntimeException() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.getCurrentUserRoles());

        assertEquals("User is not authenticated", exception.getMessage());
    }

    // CONVERSION TESTS
    @Test
    void whenConvertToDTO_withCompleteDetails_thenDTOIsCorrect() {
        when(keycloakService.getUserDetailsById("123")).thenReturn(sampleUserDetails);

        UserDTO dto = userService.getUserById("123");

        assertEquals("123", dto.getUserId());
        assertEquals("testuser", dto.getUsername());
        assertEquals("test@example.com", dto.getEmail());
        assertEquals("Test", dto.getFirstName());
        assertEquals("User", dto.getLastName());
        assertEquals(UserDTO.UserRole.EMPLOYEE, dto.getRole());
        assertEquals(UserDTO.UserStatus.ACTIVE, dto.getStatus());
        assertEquals("123-456-7890", dto.getPhone());
        assertEquals("123 Test St", dto.getAddress());
        assertEquals("2023-01-01", dto.getHireDate());
    }

    @Test
    void whenConvertToDTO_withMissingRoles_thenDefaultsToEmployee() {
        Map<String, Object> userDetails = new HashMap<>(sampleUserDetails);
        userDetails.put("roles", Collections.emptyList());
        when(keycloakService.getUserDetailsById("123")).thenReturn(userDetails);

        UserDTO dto = userService.getUserById("123");

        assertEquals(UserDTO.UserRole.EMPLOYEE, dto.getRole());
    }

    @Test
    void whenConvertToDTO_withInvalidStatus_thenDefaultsToActive() {
        Map<String, Object> userDetails = new HashMap<>(sampleUserDetails);
        userDetails.put("status", "INVALID_STATUS");
        when(keycloakService.getUserDetailsById("123")).thenReturn(userDetails);

        UserDTO dto = userService.getUserById("123");

        assertEquals(UserDTO.UserStatus.ACTIVE, dto.getStatus());
    }

    @Test
    void whenConvertToDTO_withNullValues_thenHandlesGracefully() {
        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("userId", "123");
        userDetails.put("username", "testuser");
        userDetails.put("roles", List.of("EMPLOYEE"));
        userDetails.put("projectTitles", Collections.emptyList());

        when(keycloakService.getUserDetailsById("123")).thenReturn(userDetails);

        UserDTO dto = userService.getUserById("123");

        assertEquals("123", dto.getUserId());
        assertEquals("testuser", dto.getUsername());
        assertNull(dto.getEmail());
        assertEquals(UserDTO.UserRole.EMPLOYEE, dto.getRole());
        assertEquals(UserDTO.UserStatus.ACTIVE, dto.getStatus());
    }

    // EDGE CASE TESTS
    @Test
    void whenGetUserNameAndRole_withNullNames_thenHandlesGracefully() {
        Map<String, Object> userDetails = new HashMap<>(sampleUserDetails);
        userDetails.put("firstName", null);
        userDetails.put("lastName", null);
        when(keycloakService.getUserDetailsById("123")).thenReturn(userDetails);

        Map<String, String> result = userService.getUserNameAndRole("123");

        assertEquals("", result.get("name"));
        assertEquals("EMPLOYEE", result.get("role"));
    }


    // Helper method to setup security context
    private void setupSecurityContext(String username, List<String> roles) {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(username);

        List<GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        doReturn(authorities).when(authentication).getAuthorities();

    }
}