package org.example.leavemanagement;

import company.Leave_Management_service.DTO.LeaveRequestDto;
import company.Leave_Management_service.DTO.UserDTO;
import company.Leave_Management_service.Models.LeaveRequest;
import company.Leave_Management_service.Models.LeaveStatus;
import company.Leave_Management_service.Models.LeaveType;
import company.Leave_Management_service.Repository.LeaveRequestRepository;
import company.Leave_Management_service.Services.LeaveService;
import company.Leave_Management_service.Services.UserClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock
    private LeaveRequestRepository repository;

    @Mock
    private UserClient userClient;

    @Mock
    private RoleHierarchy roleHierarchy;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private LeaveService leaveService;

    private static final String TEST_USER_ID = "user123";
    private static final String TEST_USERNAME = "testuser";
    private static final Long TEST_DEPARTMENT_ID = 1L;

    @BeforeEach
    void setUp() {
        reset(userClient, repository, authentication, securityContext, roleHierarchy);
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn(TEST_USER_ID);
    }

    @Test
    void createLeaveRequest_Success() {
        // Arrange
        LeaveRequestDto dto = createLeaveRequestDto();
        UserDTO userDTO = createUserDTO();
        LeaveRequest savedLeave = createLeaveRequest();

        when(userClient.getUserById(TEST_USER_ID)).thenReturn(userDTO);
        when(repository.save(any(LeaveRequest.class))).thenReturn(savedLeave);

        // Act
        LeaveRequest result = leaveService.createLeaveRequest(dto);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getEmployeeId());
        assertEquals(TEST_USERNAME, result.getUsername());
        assertEquals(dto.getStartDate(), result.getStartDate());
        assertEquals(dto.getEndDate(), result.getEndDate());
        assertEquals(dto.getType(), result.getType());
        assertEquals(LeaveStatus.PENDING, result.getStatus());
        assertEquals(dto.getReason(), result.getReason());

        verify(userClient).getUserById(TEST_USER_ID);
        verify(repository).save(any(LeaveRequest.class));
    }

    @Test
    void createLeaveRequest_UserNotFound_ThrowsException() {
        // Arrange
        LeaveRequestDto dto = createLeaveRequestDto();
        when(userClient.getUserById(TEST_USER_ID)).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> leaveService.createLeaveRequest(dto));

        assertEquals("User not found or username missing in user-management-service",
                exception.getMessage());
        verify(userClient).getUserById(TEST_USER_ID);
        verify(repository, never()).save(any(LeaveRequest.class));
    }

    @Test
    void createLeaveRequest_UserWithoutUsername_ThrowsException() {
        // Arrange
        LeaveRequestDto dto = createLeaveRequestDto();
        UserDTO userDTO = createUserDTO();
        userDTO.setUsername(null);

        when(userClient.getUserById(TEST_USER_ID)).thenReturn(userDTO);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> leaveService.createLeaveRequest(dto));

        assertEquals("User not found or username missing in user-management-service",
                exception.getMessage());
    }

    @Test
    void createLeaveRequest_UserClientException_ThrowsException() {
        // Arrange
        LeaveRequestDto dto = createLeaveRequestDto();
        when(userClient.getUserById(TEST_USER_ID)).thenThrow(new RuntimeException("Service unavailable"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> leaveService.createLeaveRequest(dto));

        assertEquals("User not found or username missing in user-management-service",
                exception.getMessage());
    }



    @Test
    void getLeaveHistory_EmptyList_ReturnsEmptyList() {
        // Arrange
        List<GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"));
        doReturn(authorities).when(authentication).getAuthorities();
        doReturn(authorities).when(roleHierarchy).getReachableGrantedAuthorities(any());
        when(repository.findByEmployeeId(TEST_USER_ID)).thenReturn(Collections.emptyList());

        // Act
        List<LeaveRequest> result = leaveService.getLeaveHistory();

        // Assert
        assertTrue(result.isEmpty());
        verify(userClient, never()).getAllUsers();
    }

    @Test
    void getTeamLeaves_Success() {
        // Arrange
        UserDTO manager = createUserDTO();
        List<UserDTO> departmentUsers = Arrays.asList(
                createUserDTO(),
                createUserDTO("user456", "testuser2")
        );
        List<LeaveRequest> teamRequests = Arrays.asList(createLeaveRequest());

        when(userClient.getUserById(TEST_USER_ID)).thenReturn(manager);
        when(userClient.getAllUsers()).thenReturn(departmentUsers);
        when(repository.findByEmployeeIdIn(any())).thenReturn(teamRequests);

        // Act
        List<LeaveRequest> result = leaveService.getTeamLeaves();

        // Assert
        assertEquals(1, result.size());
        verify(userClient).getUserById(TEST_USER_ID);
        verify(userClient).getAllUsers();
        verify(repository).findByEmployeeIdIn(any());
    }

    @Test
    void getTeamLeaves_ManagerNotFound_ReturnsEmptyList() {
        // Arrange
        when(userClient.getUserById(TEST_USER_ID)).thenReturn(null);

        // Act
        List<LeaveRequest> result = leaveService.getTeamLeaves();

        // Assert
        assertTrue(result.isEmpty());
        verify(userClient).getUserById(TEST_USER_ID);
        verify(repository, never()).findByEmployeeIdIn(any());
    }

    @Test
    void getTeamLeaves_ManagerWithoutDepartment_ReturnsEmptyList() {
        // Arrange
        UserDTO manager = createUserDTO();
        manager.setDepartmentId(null);

        when(userClient.getUserById(TEST_USER_ID)).thenReturn(manager);

        // Act
        List<LeaveRequest> result = leaveService.getTeamLeaves();

        // Assert
        assertTrue(result.isEmpty());
        verify(userClient).getUserById(TEST_USER_ID);
        verify(repository, never()).findByEmployeeIdIn(any());
    }

    @Test
    void getTeamLeaves_UserClientException_ReturnsOwnRequests() {
        // Arrange
        UserDTO manager = createUserDTO();
        List<LeaveRequest> ownRequests = Arrays.asList(createLeaveRequest());

        when(userClient.getUserById(TEST_USER_ID)).thenReturn(manager);
        when(userClient.getAllUsers()).thenThrow(new RuntimeException("Service unavailable"));
        when(repository.findByEmployeeId(TEST_USER_ID)).thenReturn(ownRequests);

        // Act
        List<LeaveRequest> result = leaveService.getTeamLeaves();

        // Assert
        assertEquals(1, result.size());
        verify(repository).findByEmployeeId(TEST_USER_ID);
    }

    @Test
    void getTeamLeaves_NoEmployeesInDepartment_ReturnsEmptyList() {
        // Arrange
        UserDTO manager = createUserDTO();
        manager.setDepartmentId(999L); // Different department
        List<UserDTO> departmentUsers = Arrays.asList(createUserDTO());

        when(userClient.getUserById(TEST_USER_ID)).thenReturn(manager);
        when(userClient.getAllUsers()).thenReturn(departmentUsers);

        // Act
        List<LeaveRequest> result = leaveService.getTeamLeaves();

        // Assert
        assertTrue(result.isEmpty());
        verify(repository, never()).findByEmployeeIdIn(any());
    }

    @Test
    void updateLeaveStatus_Success() {
        // Arrange
        UUID leaveId = UUID.randomUUID();
        LeaveRequest existingLeave = createLeaveRequest();
        existingLeave.setId(leaveId);
        existingLeave.setStatus(LeaveStatus.PENDING);

        when(repository.findById(leaveId)).thenReturn(Optional.of(existingLeave));
        when(repository.save(any(LeaveRequest.class))).thenReturn(existingLeave);

        // Act
        LeaveRequest result = leaveService.updateLeaveStatus(leaveId, LeaveStatus.APPROVED);

        // Assert
        assertNotNull(result);
        assertEquals(LeaveStatus.APPROVED, result.getStatus());
        // The updatedAt should be set by the service method
        verify(repository).findById(leaveId);
        verify(repository).save(existingLeave);
    }

    @Test
    void updateLeaveStatus_LeaveNotFound_ThrowsException() {
        // Arrange
        UUID leaveId = UUID.randomUUID();
        when(repository.findById(leaveId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> leaveService.updateLeaveStatus(leaveId, LeaveStatus.APPROVED));

        assertEquals("Leave not found", exception.getMessage());
        verify(repository).findById(leaveId);
        verify(repository, never()).save(any());
    }

    @Test
    void populateUsernames_Success() {
        // Arrange
        LeaveRequest request1 = createLeaveRequest();
        request1.setUsername(null);
        request1.setEmployeeId("user123");

        LeaveRequest request2 = createLeaveRequest();
        request2.setUsername("Utilisateur_456");
        request2.setEmployeeId("user456");

        List<LeaveRequest> requests = Arrays.asList(request1, request2);

        List<UserDTO> users = Arrays.asList(
                createUserDTO("user123", "testuser1"),
                createUserDTO("user456", "testuser2")
        );

        // Fixed: Use doReturn() instead of when().thenReturn() to handle wildcard types
        List<GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"));
        doReturn(authorities).when(authentication).getAuthorities();
        doReturn(authorities).when(roleHierarchy).getReachableGrantedAuthorities(any());
        when(repository.findByEmployeeId(TEST_USER_ID)).thenReturn(requests);
        when(userClient.getAllUsers()).thenReturn(users);

        // Act
        List<LeaveRequest> result = leaveService.getLeaveHistory();

        // Assert
        assertEquals("testuser1", result.get(0).getUsername());
        assertEquals("testuser2", result.get(1).getUsername());
        verify(userClient).getAllUsers();
    }

    @Test
    void populateUsernames_UserClientException_SetsUnknownUser() {
        // Arrange
        LeaveRequest request = createLeaveRequest();
        request.setUsername(null);
        List<LeaveRequest> requests = Arrays.asList(request);

        // Fixed: Use doReturn() instead of when().thenReturn() to handle wildcard types
        List<GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"));
        doReturn(authorities).when(authentication).getAuthorities();
        doReturn(authorities).when(roleHierarchy).getReachableGrantedAuthorities(any());
        when(repository.findByEmployeeId(TEST_USER_ID)).thenReturn(requests);
        when(userClient.getAllUsers()).thenThrow(new RuntimeException("Service unavailable"));

        // Act
        List<LeaveRequest> result = leaveService.getLeaveHistory();

        // Assert
        assertEquals("Unknown User", result.get(0).getUsername());
        verify(userClient).getAllUsers();
    }

    // Helper methods

    private LeaveRequestDto createLeaveRequestDto() {
        LeaveRequestDto dto = new LeaveRequestDto();
        dto.setStartDate(LocalDate.now().plusDays(1));
        dto.setEndDate(LocalDate.now().plusDays(5));
        dto.setType(LeaveType.VACATION);
        dto.setReason("Family vacation");
        return dto;
    }

    private UserDTO createUserDTO() {
        return createUserDTO(TEST_USER_ID, TEST_USERNAME);
    }

    private UserDTO createUserDTO(String userId, String username) {
        UserDTO dto = new UserDTO();
        dto.setUserId(userId);
        dto.setUsername(username);
        dto.setDepartmentId(TEST_DEPARTMENT_ID);
        return dto;
    }

    private LeaveRequest createLeaveRequest() {
        LeaveRequest request = new LeaveRequest();
        request.setId(UUID.randomUUID());
        request.setEmployeeId(TEST_USER_ID);
        request.setUsername(TEST_USERNAME);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(5));
        request.setType(LeaveType.VACATION);
        request.setStatus(LeaveStatus.PENDING);
        request.setReason("Family vacation");
        request.setCreatedAt(LocalDateTime.now());
        return request;
    }
}