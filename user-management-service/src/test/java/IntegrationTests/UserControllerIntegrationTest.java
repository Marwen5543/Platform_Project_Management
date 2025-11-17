package IntegrationTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import company.user_management_service.DTO.CreateUserRequest;
import company.user_management_service.DTO.LoginRequest;
import company.user_management_service.Service.KeycloakService;
import company.user_management_service.UserApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.mockito.ArgumentMatchers.any;
import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = UserApplication.class
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "keycloak.provisioning.enabled=false",
        "eureka.client.enabled=false",
        "management.endpoints.web.exposure.include=health,info"
})
public class UserControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KeycloakService keycloakService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void contextLoads() {}

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllUsers() throws Exception {
        Mockito.when(keycloakService.getAllUsers()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void testRegisterUser() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser" + System.currentTimeMillis());
        request.setEmail(System.currentTimeMillis() + "@example.com");
        request.setPassword("password123");
        request.setRole("EMPLOYEE");

        Mockito.doNothing().when(keycloakService).createUser(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any(), any(), anyString());

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testLoginEndpoint() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        // ARRANGE: Your UserService first calls getToken, then getUserDetails. We must mock both.
        Mockito.when(keycloakService.getToken(anyString(), anyString())).thenReturn("mock-jwt-token");
        Mockito.when(keycloakService.getUserDetails(anyString()))
                .thenReturn(Map.of(
                        "username", "testuser",
                        "role", "EMPLOYEE",
                        "firstName", "Test",
                        "lastName", "User"
                        // Add other fields required by your LoginResponse.builder()
                ));

        // ACT & ASSERT
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "EMPLOYEE")
    void testGetCurrentUser() throws Exception {
        Mockito.when(keycloakService.getUserDetails("testuser"))
                .thenReturn(Map.of("username", "testuser", "email", "test@example.com"));

        mockMvc.perform(get("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username="superadmin", roles = "SUPER_ADMIN") // Added username to the mock user
    void testChangeUserRole() throws Exception {
        String roleChangeRequest = "{\"role\":\"ADMIN\"}";
        String userIdToUpdate = "test-user-id";
        String usernameToUpdate = "someuser";

        // ARRANGE: Your UserService first calls getUserDetailsById. Mock this.
        Mockito.when(keycloakService.getUserDetailsById(userIdToUpdate))
                .thenReturn(Map.of("id", userIdToUpdate, "username", usernameToUpdate));

        // ARRANGE: Then, it calls updateUserRole with the username. Mock this void method.
        Mockito.doNothing().when(keycloakService).updateUserRole(usernameToUpdate, "ADMIN");

        // ACT & ASSERT
        mockMvc.perform(put("/api/users/" + userIdToUpdate + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roleChangeRequest))
                .andExpect(status().isOk());
    }
}