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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;


import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private KeycloakService keycloakService;

    @InjectMocks
    private UserService userService;

    private CreateUserRequest validCreateRequest;
    private LoginRequest validLoginRequest;
    private LoginRequest invalidLoginRequest;

    @BeforeEach
    void setUp() {
        validCreateRequest = new CreateUserRequest();
        validCreateRequest.setUsername("testuser");
        validCreateRequest.setEmail("test@example.com");
        validCreateRequest.setPassword("password123");
        validCreateRequest.setFirstName("Test");
        validCreateRequest.setLastName("User");
        validCreateRequest.setRole("EMPLOYEE");

        validLoginRequest = new LoginRequest();
        validLoginRequest.setUsername("testuser");
        validLoginRequest.setPassword("correct_password");

        invalidLoginRequest = new LoginRequest();
        invalidLoginRequest.setUsername("testuser");
        invalidLoginRequest.setPassword("wrong_password");
    }

    @Test
    void whenCreateUser_withValidRequest_thenKeycloakServiceIsCalledCorrectly() {
        // --- ARRANGE ---
        // No stubbing needed for void method createUser

        // --- ACT ---
        userService.createUser(validCreateRequest);

        // --- ASSERT ---
        verify(keycloakService, times(1)).createUser(
                eq("testuser"),
                eq("test@example.com"),
                eq("password123"),
                eq("EMPLOYEE"),
                eq("Test"),
                eq("User"),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull()
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    void whenCreateUser_withInvalidEmail_thenKeycloakServiceIsCalled(String email) {
        // --- ARRANGE ---
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setEmail(email);
        request.setPassword("password123");

        // --- ACT ---
        userService.createUser(request);

        // --- ASSERT ---
        verify(keycloakService, times(1)).createUser(
                eq("testuser"),
                eq(email),
                eq("password123"),
                eq("EMPLOYEE"),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    void whenCreateUser_withInvalidPassword_thenKeycloakServiceIsCalled(String password) {
        // --- ARRANGE ---
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword(password);

        // --- ACT ---
        userService.createUser(request);

        // --- ASSERT ---
        verify(keycloakService, times(1)).createUser(
                eq("testuser"),
                eq("test@example.com"),
                eq(password),
                eq("EMPLOYEE"),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    void whenCreateUser_withInvalidUsername_thenKeycloakServiceIsCalled(String username) {
        // --- ARRANGE ---
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername(username);
        request.setEmail("test@example.com");
        request.setPassword("password123");

        // --- ACT ---
        userService.createUser(request);

        // --- ASSERT ---
        verify(keycloakService, times(1)).createUser(
                eq(username),
                eq("test@example.com"),
                eq("password123"),
                eq("EMPLOYEE"),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()
        );
    }

    @Test
    void whenCreateUser_withKeycloakException_thenThrowsRuntimeException() {
        // --- ARRANGE ---
        doThrow(new RuntimeException("Keycloak error")).when(keycloakService).createUser(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                any(), any(), any(), any(), any(), any()
        );

        // --- ACT & ASSERT ---
        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.createUser(validCreateRequest));
        assertEquals("Failed to create user in Keycloak: Keycloak error", exception.getMessage());
        verify(keycloakService, times(1)).createUser(
                eq("testuser"),
                eq("test@example.com"),
                eq("password123"),
                eq("EMPLOYEE"),
                eq("Test"),
                eq("User"),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull()
        );
    }

    @Test
    void whenLogin_withValidCredentials_thenReturnsLoginResponse() {
        // --- ARRANGE ---
        String fakeToken = "a-very-long-fake-jwt-token";
        Map<String, Object> fakeUserDetails = new HashMap<>();
        fakeUserDetails.put("username", "testuser");
        fakeUserDetails.put("role", "EMPLOYEE");
        fakeUserDetails.put("firstName", "Test");
        fakeUserDetails.put("lastName", "User");
        fakeUserDetails.put("forcePasswordReset", "false");

        when(keycloakService.getToken(validLoginRequest.getUsername(), validLoginRequest.getPassword())).thenReturn(fakeToken);
        when(keycloakService.getUserDetails(validLoginRequest.getUsername())).thenReturn(fakeUserDetails);

        // --- ACT ---
        LoginResponse response = userService.login(validLoginRequest);

        // --- ASSERT ---
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
        // --- ARRANGE ---
        LoginRequest request = new LoginRequest();
        request.setUsername(null);
        request.setPassword("password123");

        // --- ACT & ASSERT ---
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> userService.login(request));
        assertEquals("Username and password are required", exception.getMessage());
        verify(keycloakService, never()).getToken(anyString(), anyString());
    }

    @Test
    void whenLogin_withNullPassword_thenThrowsBadCredentialsException() {
        // --- ARRANGE ---
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword(null);

        // --- ACT & ASSERT ---
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> userService.login(request));
        assertEquals("Username and password are required", exception.getMessage());
        verify(keycloakService, never()).getToken(anyString(), anyString());
    }

    @Test
    void whenLogin_withInvalidCredentials_thenThrowsBadCredentialsException() {
        // --- ARRANGE ---
        when(keycloakService.getToken(invalidLoginRequest.getUsername(), invalidLoginRequest.getPassword()))
                .thenThrow(new RuntimeException("Keycloak authentication failed"));

        // --- ACT & ASSERT ---
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> userService.login(invalidLoginRequest));
        assertEquals("Invalid credentials", exception.getMessage());
        verify(keycloakService, never()).getUserDetails(anyString());
    }

    @Test
    void whenLogin_andGetUserDetailsReturnsNull_thenThrowsBadCredentialsException() {
        // --- ARRANGE ---
        when(keycloakService.getToken(validLoginRequest.getUsername(), validLoginRequest.getPassword())).thenReturn("fake-token");
        when(keycloakService.getUserDetails(validLoginRequest.getUsername())).thenReturn(null);

        // --- ACT & ASSERT ---
        assertThrows(BadCredentialsException.class, () -> userService.login(validLoginRequest));
    }

    @Test
    void whenGetUserById_withExistingUser_thenReturnsUserDTO() {
        // --- ARRANGE ---
        Map<String, Object> fakeUserDetails = new HashMap<>();
        fakeUserDetails.put("userId", "123");
        fakeUserDetails.put("username", "testuser");
        fakeUserDetails.put("email", "test@example.com");
        fakeUserDetails.put("roles", List.of("EMPLOYEE"));
        fakeUserDetails.put("status", "ACTIVE");

        when(keycloakService.getUserDetailsById("123")).thenReturn(fakeUserDetails);

        // --- ACT ---
        UserDTO userDTO = userService.getUserById("123");

        // --- ASSERT ---
        assertNotNull(userDTO);
        assertEquals("123", userDTO.getUserId());
        assertEquals("testuser", userDTO.getUsername());
        assertEquals("test@example.com", userDTO.getEmail());
        assertEquals(UserDTO.UserRole.EMPLOYEE, userDTO.getRole());
        assertEquals(UserDTO.UserStatus.ACTIVE, userDTO.getStatus());
    }

/* ------------------------*/
    @Test
    void whenConvertToDTO_withCompleteDetails_thenDTOIsCorrect() {
        // --- ARRANGE ---
        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("userId", "123");
        userDetails.put("username", "testuser");
        userDetails.put("email", "test@example.com");
        userDetails.put("firstName", "Test");
        userDetails.put("lastName", "User");
        userDetails.put("roles", List.of("ADMIN"));
        userDetails.put("status", "ACTIVE");

        when(keycloakService.getUserDetailsById("123")).thenReturn(userDetails);

        // --- ACT ---
        UserDTO dto = userService.getUserById("123");

        // --- ASSERT ---
        assertEquals("123", dto.getUserId());
        assertEquals("testuser", dto.getUsername());
        assertEquals("test@example.com", dto.getEmail());
        assertEquals("Test", dto.getFirstName());
        assertEquals("User", dto.getLastName());
        assertEquals(UserDTO.UserRole.ADMIN, dto.getRole());
        assertEquals(UserDTO.UserStatus.ACTIVE, dto.getStatus());
    }

    @Test
    void whenConvertToDTO_withMissingRoles_thenDefaultsToEmployee() {
        // --- ARRANGE ---
        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("userId", "123");
        userDetails.put("username", "testuser");
        userDetails.put("roles", Collections.emptyList());

        when(keycloakService.getUserDetailsById("123")).thenReturn(userDetails);

        // --- ACT ---
        UserDTO dto = userService.getUserById("123");

        // --- ASSERT ---
        assertEquals(UserDTO.UserRole.EMPLOYEE, dto.getRole());
    }

    @Test
    void whenConvertToDTO_withInvalidStatus_thenDefaultsToActive() {
        // --- ARRANGE ---
        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("userId", "123");
        userDetails.put("username", "testuser");
        userDetails.put("status", "INVALID_STATUS");

        when(keycloakService.getUserDetailsById("123")).thenReturn(userDetails);

        // --- ACT ---
        UserDTO dto = userService.getUserById("123");

        // --- ASSERT ---
        assertEquals(UserDTO.UserStatus.ACTIVE, dto.getStatus());
    }

    @Test
    void whenAssignProject_withValidInput_thenProjectIsAssigned() {
        // --- ARRANGE ---
        String userId = "123";
        String projectTitle = "ProjectX";
        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("userId", userId);
        userDetails.put("username", "testuser");
        userDetails.put("projectTitles", Collections.emptyList());

        Map<String, Object> updatedDetails = new HashMap<>(userDetails);
        updatedDetails.put("projectTitles", List.of(projectTitle));

        when(keycloakService.getUserDetailsById(userId)).thenReturn(userDetails, updatedDetails);
        doNothing().when(keycloakService).updateUserProjects(eq(userId), eq(List.of(projectTitle)));

        // --- ACT ---
        UserDTO result = userService.assignProject(userId, projectTitle);

        // --- ASSERT ---
        assertNotNull(result);
        assertEquals(List.of(projectTitle), result.getProjectTitles());
        verify(keycloakService, times(2)).getUserDetailsById(userId);
        verify(keycloakService, times(1)).updateUserProjects(eq(userId), eq(List.of(projectTitle)));
    }






    @Test
    void whenAssignProject_alreadyAssigned_thenNoUpdate() {
        // --- ARRANGE ---
        String userId = "123";
        String projectTitle = "ProjectX";
        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("userId", userId);
        userDetails.put("username", "testuser");
        userDetails.put("projectTitles", List.of(projectTitle));

        when(keycloakService.getUserDetailsById(userId)).thenReturn(userDetails);

        // --- ACT ---
        UserDTO result = userService.assignProject(userId, projectTitle);

        // --- ASSERT ---
        assertNotNull(result);
        assertEquals(List.of(projectTitle), result.getProjectTitles());
        verify(keycloakService, times(1)).getUserDetailsById(userId);
        verify(keycloakService, never()).updateUserProjects(anyString(), anyList());
    }

    @Test
    void whenDeassignProject_withValidInput_thenProjectIsRemoved() {
        // --- ARRANGE ---
        String userId = "123";
        String projectTitle = "ProjectX";
        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("userId", userId);
        userDetails.put("username", "testuser");
        userDetails.put("projectTitles", List.of(projectTitle));

        Map<String, Object> updatedDetails = new HashMap<>(userDetails);
        updatedDetails.put("projectTitles", Collections.emptyList());

        when(keycloakService.getUserDetailsById(userId)).thenReturn(userDetails, updatedDetails);
        doNothing().when(keycloakService).updateUserProjects(eq(userId), eq(Collections.emptyList()));

        // --- ACT ---
        UserDTO result = userService.deassignProject(userId, projectTitle);

        // --- ASSERT ---
        assertNotNull(result);
        assertEquals(Collections.emptyList(), result.getProjectTitles());
        verify(keycloakService, times(2)).getUserDetailsById(userId);
        verify(keycloakService, times(1)).updateUserProjects(eq(userId), eq(Collections.emptyList()));
    }

    @Test
    void whenChangePassword_withShortPassword_thenThrowsIllegalArgumentException() {
        // --- ACT & ASSERT ---
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.changePassword("testuser", "currentPassword", "short"));
        assertEquals("Password must be at least 8 characters", exception.getMessage());
        verify(keycloakService, never()).updatePassword(anyString(), anyString(), anyString());
    }

    @Test
    void whenChangePassword_withValidPassword_thenUpdatesPassword() {
        // --- ARRANGE ---
        doNothing().when(keycloakService).updatePassword(eq("testuser"), eq("currentPassword"), eq("newPassword123"));

        // --- ACT ---
        userService.changePassword("testuser", "currentPassword", "newPassword123");

        // --- ASSERT ---
        verify(keycloakService, times(1)).updatePassword(eq("testuser"), eq("currentPassword"), eq("newPassword123"));
    }
}