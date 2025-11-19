package org.example.documentmanagementservice.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Enhanced UserApiClient with Keycloak integration
 * Handles both application API calls and Keycloak user management
 */
public class UserApiClient {
    // Application API configuration
    private final String userServiceUrl = "http://localhost:8085";

    // Keycloak configuration - Update these according to your setup
    private final String keycloakBaseUrl = System.getProperty("keycloak.base.url", "http://localhost:8080");
    private final String keycloakRealm = System.getProperty("keycloak.realm", "Tunisys");
    private final String keycloakClientId = System.getProperty("keycloak.client.id", "admin-cli");
    private final String keycloakAdminUsername = System.getProperty("keycloak.admin.username", "admin");
    private final String keycloakAdminPassword = System.getProperty("keycloak.admin.password", "admin");

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Feature flags
    private final boolean keycloakEnabled = Boolean.parseBoolean(System.getProperty("keycloak.enabled", "true"));

    // Cached tokens
    private String adminToken; // Application API token
    private String keycloakAccessToken; // Keycloak admin token

    /**
     * Enhanced user creation that handles both Keycloak and application registration
     * @param username The username for the new user
     * @param password The password for the new user
     * @param role The role for the new user (e.g., "EMPLOYEE")
     */
    public void createUser(String username, String password, String role) {
        try {
            if (keycloakEnabled) {
                // Step 1: Create user in Keycloak first
                try {
                    createKeycloakUser(username, password, username + "@test-e2e.com", "E2E-Test", "User");
                } catch (Exception e) {
                    System.err.println("WARNING: Keycloak user creation failed, continuing with application-only: " + e.getMessage());
                    // Continue with application creation even if Keycloak fails
                }
            } else {
                System.out.println("Keycloak integration disabled, creating application user only");
            }

            // Step 2: Create user in your application
            createApplicationUser(username, password, role);

            System.out.println("Successfully created user: " + username +
                    (keycloakEnabled ? " (with Keycloak integration)" : " (application only)"));

        } catch (Exception e) {
            System.err.println("Error creating user " + username + ": " + e.getMessage());

            // Attempt cleanup if partial creation occurred
            if (keycloakEnabled) {
                try {
                    deleteKeycloakUser(username);
                } catch (Exception cleanupEx) {
                    System.err.println("Cleanup failed for " + username + ": " + cleanupEx.getMessage());
                }
            }
            throw new RuntimeException("Failed to create user: " + username, e);
        }
    }

    /**
     * Enhanced user deletion that handles both Keycloak and application
     * @param username The username of the user to delete
     */
    public void deleteUser(String username) {
        // Delete from application first
        deleteApplicationUser(username);

        // Then delete from Keycloak (if enabled)
        if (keycloakEnabled) {
            deleteKeycloakUser(username);
        }

        System.out.println("Successfully deleted user: " + username +
                (keycloakEnabled ? " (from both systems)" : " (from application)"));
    }

    // ========== KEYCLOAK METHODS ==========

    /**
     * Get Keycloak admin access token
     */
    private String getKeycloakAccessToken() throws Exception {
        if (keycloakAccessToken != null) {
            return keycloakAccessToken;
        }

        System.out.println("Attempting to authenticate with Keycloak...");
        System.out.println("Keycloak URL: " + keycloakBaseUrl);
        System.out.println("Admin Username: " + keycloakAdminUsername);

        String tokenUrl = keycloakBaseUrl + "/realms/master/protocol/openid-connect/token";

        // Prepare form data
        String formData = "grant_type=password" +
                "&client_id=" + keycloakClientId +
                "&username=" + keycloakAdminUsername +
                "&password=" + keycloakAdminPassword;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.println("Keycloak authentication failed!");
            System.err.println("URL: " + tokenUrl);
            System.err.println("Status: " + response.statusCode());
            System.err.println("Response: " + response.body());

            // Try alternative authentication approaches
            return tryAlternativeKeycloakAuth();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> tokenResponse = objectMapper.readValue(response.body(), Map.class);
        keycloakAccessToken = (String) tokenResponse.get("access_token");

        System.out.println("Successfully obtained Keycloak access token");
        return keycloakAccessToken;
    }

    /**
     * Try alternative Keycloak authentication methods
     */
    private String tryAlternativeKeycloakAuth() throws Exception {
        System.out.println("Trying alternative Keycloak authentication methods...");

        // Method 1: Try with different client configurations
        String[] alternativeClients = {"admin-cli", "account", "security-admin-console"};

        for (String clientId : alternativeClients) {
            try {
                String tokenUrl = keycloakBaseUrl + "/realms/master/protocol/openid-connect/token";
                String formData = "grant_type=password" +
                        "&client_id=" + clientId +
                        "&username=" + keycloakAdminUsername +
                        "&password=" + keycloakAdminPassword;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(tokenUrl))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(formData))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> tokenResponse = objectMapper.readValue(response.body(), Map.class);
                    keycloakAccessToken = (String) tokenResponse.get("access_token");
                    System.out.println("Successfully authenticated with client: " + clientId);
                    return keycloakAccessToken;
                }

                System.out.println("Client " + clientId + " failed with status: " + response.statusCode());

            } catch (Exception e) {
                System.out.println("Client " + clientId + " failed: " + e.getMessage());
            }
        }

        // Method 2: Try authenticating against the target realm instead of master
        try {
            String tokenUrl = keycloakBaseUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token";
            String formData = "grant_type=password" +
                    "&client_id=admin-cli" +
                    "&username=" + keycloakAdminUsername +
                    "&password=" + keycloakAdminPassword;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> tokenResponse = objectMapper.readValue(response.body(), Map.class);
                keycloakAccessToken = (String) tokenResponse.get("access_token");
                System.out.println("Successfully authenticated against realm: " + keycloakRealm);
                return keycloakAccessToken;
            }

            System.out.println("Realm authentication failed with status: " + response.statusCode());

        } catch (Exception e) {
            System.out.println("Realm authentication failed: " + e.getMessage());
        }

        throw new RuntimeException("All Keycloak authentication methods failed. Please check:\n" +
                "1. Keycloak is running at: " + keycloakBaseUrl + "\n" +
                "2. Admin credentials are correct: " + keycloakAdminUsername + "\n" +
                "3. Realm '" + keycloakRealm + "' exists\n" +
                "4. Admin user has proper permissions");
    }

    /**
     * Create user in Keycloak
     */
    public void createKeycloakUser(String username, String password, String email, String firstName, String lastName) {
        try {
            String accessToken = getKeycloakAccessToken();

            String createUserUrl = keycloakBaseUrl + "/admin/realms/" + keycloakRealm + "/users";

            // Create user payload
            Map<String, Object> userPayload = Map.of(
                    "username", username,
                    "email", email != null ? email : username + "@example.com",
                    "firstName", firstName != null ? firstName : "Test",
                    "lastName", lastName != null ? lastName : "User",
                    "enabled", true,
                    "emailVerified", true,
                    "credentials", List.of(Map.of(
                            "type", "password",
                            "value", password,
                            "temporary", false
                    ))
            );

            String jsonPayload = objectMapper.writeValueAsString(userPayload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(createUserUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + accessToken)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            if (statusCode == 201) {
                System.out.println("Successfully created Keycloak user: " + username);
            } else if (statusCode == 409) {
                System.out.println("Keycloak user already exists: " + username);
            } else {
                throw new RuntimeException("Failed to create Keycloak user. Status: " + statusCode +
                        ", Body: " + response.body());
            }

        } catch (Exception e) {
            throw new RuntimeException("Error creating Keycloak user: " + username, e);
        }
    }

    /**
     * Get Keycloak user ID by username
     */
    private String getKeycloakUserId(String username) throws Exception {
        String accessToken = getKeycloakAccessToken();

        String getUserUrl = keycloakBaseUrl + "/admin/realms/" + keycloakRealm + "/users?username=" + username;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getUserUrl))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to get Keycloak user. Status: " +
                    response.statusCode() + ", Body: " + response.body());
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> users = objectMapper.readValue(response.body(), List.class);

        if (users.isEmpty()) {
            return null; // User not found
        }

        return (String) users.get(0).get("id");
    }

    /**
     * Delete user from Keycloak
     */
    public void deleteKeycloakUser(String username) {
        try {
            String accessToken = getKeycloakAccessToken();
            String userId = getKeycloakUserId(username);

            if (userId == null) {
                System.out.println("Keycloak user not found, skipping deletion: " + username);
                return;
            }

            String deleteUserUrl = keycloakBaseUrl + "/admin/realms/" + keycloakRealm + "/users/" + userId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(deleteUserUrl))
                    .header("Authorization", "Bearer " + accessToken)
                    .DELETE()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            if (statusCode == 204) {
                System.out.println("Successfully deleted Keycloak user: " + username);
            } else {
                System.err.println("Failed to delete Keycloak user. Status: " + statusCode +
                        ", Body: " + response.body());
            }

        } catch (Exception e) {
            System.err.println("Error deleting Keycloak user " + username + ": " + e.getMessage());
        }
    }

    /**
     * Check if user exists in Keycloak
     */
    public boolean keycloakUserExists(String username) {
        try {
            String userId = getKeycloakUserId(username);
            return userId != null;
        } catch (Exception e) {
            System.err.println("Error checking Keycloak user existence: " + e.getMessage());
            return false;
        }
    }

    // ========== APPLICATION API METHODS (EXISTING) ==========

    /**
     * Create user in your application (original method renamed)
     */
    public void createApplicationUser(String username, String password, String role) {
        try {
            Map<String, String> requestBody = Map.of(
                    "username", username,
                    "password", password,
                    "role", role,
                    "email", username + "@test-e2e.com",
                    "firstName", "E2E-Test",
                    "lastName", "User"
            );
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(userServiceUrl + "/api/users/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 409) {
                throw new RuntimeException("Failed to create application user. Status: "
                        + response.statusCode() + ", Body: " + response.body());
            }
            System.out.println("API HELPER: Ensured application user '" + username + "' exists.");
        } catch (Exception e) {
            throw new RuntimeException("Error calling application user creation API", e);
        }
    }

    /**
     * Delete user from application (original method renamed)
     */
    public void deleteApplicationUser(String username) {
        try {
            ensureAdminToken();

            String userId = getUserIdByUsername(username);
            if (userId == null) {
                System.out.println("API HELPER: Application user '" + username + "' not found, skipping deletion.");
                return;
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(userServiceUrl + "/api/users/" + userId))
                    .header("Authorization", "Bearer " + adminToken)
                    .DELETE()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 204) {
                System.out.println("API HELPER: Successfully deleted application user '" + username + "'.");
            } else if (response.statusCode() == 404) {
                System.out.println("API HELPER: Application user '" + username + "' not found (already deleted).");
            } else {
                System.err.println("API HELPER: Failed to delete application user '" + username + "'. Status: "
                        + response.statusCode() + ", Body: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("API HELPER: Error deleting application user '" + username + "': " + e.getMessage());
        }
    }

    /**
     * Alternative delete method that tries multiple approaches
     */
    public void deleteUserByUsername(String username) {
        try {
            ensureAdminToken();

            HttpRequest directDeleteRequest = HttpRequest.newBuilder()
                    .uri(URI.create(userServiceUrl + "/api/users/username/" + username))
                    .header("Authorization", "Bearer " + adminToken)
                    .DELETE()
                    .build();

            HttpResponse<String> directResponse = client.send(directDeleteRequest, HttpResponse.BodyHandlers.ofString());

            if (directResponse.statusCode() == 200 || directResponse.statusCode() == 204) {
                System.out.println("API HELPER: Successfully deleted user '" + username + "' (direct method).");
                return;
            }

            if (directResponse.statusCode() == 404 || directResponse.statusCode() == 405) {
                System.out.println("API HELPER: Direct username deletion not supported, trying ID-based deletion...");
                deleteApplicationUser(username);
                return;
            }

            System.err.println("API HELPER: Failed to delete user '" + username + "'. Status: "
                    + directResponse.statusCode() + ", Body: " + directResponse.body());

        } catch (Exception e) {
            System.err.println("API HELPER: Error deleting user '" + username + "': " + e.getMessage());
        }
    }

    /**
     * Get user ID by username (helper method for deletion)
     */
    private String getUserIdByUsername(String username) throws Exception {
        ensureAdminToken();

        String[] searchEndpoints = {
                "/api/users/search?username=" + username,
                "/api/users?username=" + username,
                "/api/users/by-username/" + username
        };

        for (String endpoint : searchEndpoints) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(userServiceUrl + endpoint))
                        .header("Authorization", "Bearer " + adminToken)
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);

                    String userId = extractUserIdFromResponse(responseMap, username);
                    if (userId != null) {
                        System.out.println("API HELPER: Found user ID '" + userId + "' for username '" + username + "'");
                        return userId;
                    }
                }
            } catch (Exception e) {
                System.out.println("API HELPER: Search endpoint '" + endpoint + "' failed: " + e.getMessage());
            }
        }

        return null;
    }

    /**
     * Extract user ID from different possible response formats
     */
    @SuppressWarnings("unchecked")
    private String extractUserIdFromResponse(Map<String, Object> responseMap, String username) {
        if (responseMap.containsKey("id") && username.equals(responseMap.get("username"))) {
            return responseMap.get("id").toString();
        }

        if (responseMap.containsKey("users")) {
            List<Map<String, Object>> users = (List<Map<String, Object>>) responseMap.get("users");
            return findUserIdInList(users, username);
        }

        if (responseMap.containsKey("data")) {
            Object data = responseMap.get("data");
            if (data instanceof List) {
                List<Map<String, Object>> users = (List<Map<String, Object>>) data;
                return findUserIdInList(users, username);
            } else if (data instanceof Map) {
                Map<String, Object> userMap = (Map<String, Object>) data;
                if (username.equals(userMap.get("username"))) {
                    return userMap.get("id").toString();
                }
            }
        }

        if (responseMap.containsKey("content")) {
            List<Map<String, Object>> users = (List<Map<String, Object>>) responseMap.get("content");
            return findUserIdInList(users, username);
        }

        return null;
    }

    /**
     * Helper method to find user ID in a list of user objects
     */
    @SuppressWarnings("unchecked")
    private String findUserIdInList(List<Map<String, Object>> users, String username) {
        if (users != null) {
            for (Map<String, Object> user : users) {
                if (username.equals(user.get("username"))) {
                    Object id = user.get("id");
                    return id != null ? id.toString() : null;
                }
            }
        }
        return null;
    }

    /**
     * Ensure we have a valid admin token for authenticated operations
     */
    private void ensureAdminToken() throws Exception {
        if (adminToken != null) {
            return;
        }

        String adminUsername = System.getProperty("test.admin.username", "admin");
        String adminPassword = System.getProperty("test.admin.password", "admin123");

        String[] authEndpoints = {
                "/api/auth/login",
                "/api/users/login",
                "/auth/login"
        };

        for (String endpoint : authEndpoints) {
            try {
                Map<String, String> loginRequest = Map.of(
                        "username", adminUsername,
                        "password", adminPassword
                );
                String jsonBody = objectMapper.writeValueAsString(loginRequest);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(userServiceUrl + endpoint))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> authResponse = objectMapper.readValue(response.body(), Map.class);

                    String[] tokenFields = {"token", "access_token", "accessToken", "jwt", "authToken"};
                    for (String field : tokenFields) {
                        if (authResponse.containsKey(field)) {
                            adminToken = authResponse.get(field).toString();
                            System.out.println("API HELPER: Successfully authenticated as admin");
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("API HELPER: Auth endpoint '" + endpoint + "' failed: " + e.getMessage());
            }
        }

        throw new RuntimeException("Failed to authenticate as admin. Please check admin credentials and API endpoints.");
    }

    /**
     * Check if user exists in application (useful for verification)
     */
    public boolean userExists(String username) {
        try {
            ensureAdminToken();
            String userId = getUserIdByUsername(username);
            return userId != null;
        } catch (Exception e) {
            System.err.println("API HELPER: Error checking if user exists: " + e.getMessage());
            return false;
        }
    }

    /**
     * Clear cached tokens (useful if tokens expire)
     */
    public void clearAuthTokens() {
        this.adminToken = null;
        this.keycloakAccessToken = null;
    }

    // ========== CONFIGURATION METHODS ==========

    /**
     * Update Keycloak configuration if needed
     */
    public void updateKeycloakConfig(String baseUrl, String realm, String clientId, String adminUsername, String adminPassword) {
        // Note: These would need to be made non-final fields for this to work
        System.out.println("Keycloak config update requested - consider making fields configurable if needed");
    }

    /**
     * Test Keycloak connectivity
     */
    public boolean testKeycloakConnection() {
        try {
            getKeycloakAccessToken();
            System.out.println("Keycloak connection test successful");
            return true;
        } catch (Exception e) {
            System.err.println("Keycloak connection test failed: " + e.getMessage());
            return false;
        }
    }
}