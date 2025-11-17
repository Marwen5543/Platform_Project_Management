package company.user_management_service.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import company.user_management_service.DTO.UserDTO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
@Slf4j
@Service
public class KeycloakService {

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;
    @Value("${keycloak.provisioning.enabled:false}") // Add this new property, default to false
    private boolean provisioningEnabled;
    private Keycloak keycloak;

    @PostConstruct
    public void init() {
        try {
            if (provisioningEnabled) {
                // This code is for initial, one-time setup (e.g., a developer's machine)
                log.warn("Keycloak provisioning is ENABLED. Connecting to 'master' realm with admin credentials.");
                keycloak = KeycloakBuilder.builder()
                        .serverUrl(authServerUrl)
                        .realm("master")
                        .username("admin")
                        .password("admin") // Use environment variables for these too!
                        .clientId("admin-cli")
                        .build();

                log.info("Provisioning: Checking if realm '{}' exists...", realm);
                if (!realmExists()) {
                    createRealm();
                }

                log.info("Provisioning: Ensuring client '{}' exists...", clientId);
                ensureClient();

                // After provisioning, we MUST re-initialize the client to be the service itself!
                log.info("Provisioning complete. Re-initializing Keycloak client for runtime operations.");
                keycloak = KeycloakBuilder.builder()
                        .serverUrl(authServerUrl)
                        .realm(realm)
                        .grantType("client_credentials")
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .build();

            } else {
                // This is the standard code for Kubernetes/Production
                log.info("Keycloak provisioning is DISABLED. Initializing standard admin client for realm '{}'.", realm);
                keycloak = KeycloakBuilder.builder()
                        .serverUrl(authServerUrl)
                        .realm(realm) // Uses tunisys-realm
                        .grantType("client_credentials")
                        .clientId(clientId) // Uses user-service-client
                        .clientSecret(clientSecret) // Uses its own secret
                        .build();
            }
            log.info("Keycloak client initialized successfully for runtime operations.");
        } catch (Exception e) {
            log.error("Keycloak initialization failed: {}", e.getMessage());
            throw new RuntimeException("Keycloak configuration error", e);
        }
    }

   /* @PostConstruct
    public void init() {
        try {
            keycloak = KeycloakBuilder.builder()
                    .serverUrl(authServerUrl)
                    .realm("master")
                    .username("admin")
                    .password("admin")
                    .clientId("admin-cli")
                    .build();

            if (!realmExists()) {
                createRealm();
            }

            ensureClient();
        } catch (Exception e) {
            log.error("Keycloak initialization failed: {}", e.getMessage());
            throw new RuntimeException("Keycloak configuration error", e);
        }
    }*/
    private boolean realmExists() {
        return keycloak.realms().findAll().stream()
                .anyMatch(r -> r.getRealm().equals(realm));
    }

    private void createRealm() {
        RealmRepresentation realmRep = new RealmRepresentation();
        realmRep.setRealm(realm);
        realmRep.setEnabled(true);
        realmRep.setSslRequired("none");
        realmRep.setRegistrationAllowed(false);
        keycloak.realms().create(realmRep);
        log.info("Created realm: {}", realm);
    }

    private void ensureClient() {
        RealmResource realmResource = keycloak.realm(realm);
        List<ClientRepresentation> clients = realmResource.clients().findByClientId(clientId);

        if (clients.isEmpty()) {
            ClientRepresentation client = new ClientRepresentation();
            client.setClientId(clientId);
            client.setSecret(clientSecret);
            client.setDirectAccessGrantsEnabled(true);
            client.setPublicClient(false);
            client.setProtocol("openid-connect");
            client.setRedirectUris(Collections.singletonList("*"));

            try (Response response = realmResource.clients().create(client)) {
                if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                    throw new RuntimeException("Client creation failed: " + response.getStatusInfo());
                }
            }
            log.info("Created client: {}", clientId);
        }
    }

    public String getToken(String username, String password) {
        return KeycloakBuilder.builder()
                .serverUrl(authServerUrl)
                .realm(realm)
                .username(username)
                .password(password)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType("password")
                .build()
                .tokenManager()
                .getAccessToken()
                .getToken();
    }

    public void createUser(String username, String email, String password, String role,
                           String firstName, String lastName, String phone, String address,
                           String hireDate, Long departmentId, Long managerId, String position) {
        try {
            UsersResource usersResource = keycloak.realm(realm).users();

            UserRepresentation user = new UserRepresentation();
            user.setUsername(username);
            user.setEmail(email);
            user.setEnabled(true);
            user.setFirstName(firstName);
            user.setLastName(lastName);

            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put("phone", Collections.singletonList(phone != null ? phone : ""));
            attributes.put("address", Collections.singletonList(address != null ? address : ""));

            // CRITICAL FIX: Ensure hireDate is never null or empty
            String finalHireDate;
            if (hireDate != null && !hireDate.trim().isEmpty()) {
                finalHireDate = hireDate.trim();
            } else {
                finalHireDate = LocalDate.now().toString(); // Auto-generate if not provided
            }

            attributes.put("hireDate", Collections.singletonList(finalHireDate));
            log.info("Setting hireDate attribute to: '{}' for user: {}", finalHireDate, username);

            attributes.put("departmentId", Collections.singletonList(departmentId != null ? departmentId.toString() : ""));
            attributes.put("managerId", Collections.singletonList(managerId != null ? managerId.toString() : ""));
            attributes.put("position", Collections.singletonList(position != null ? position : ""));
            attributes.put("status", Collections.singletonList("ACTIVE"));
            attributes.put("forcePasswordReset", Collections.singletonList("true"));
            attributes.put("projectTitles", Collections.emptyList());

            user.setAttributes(attributes);

            Response response = usersResource.create(user);
            int status = response.getStatus();
            String userId = null;

            if (status == Response.Status.CREATED.getStatusCode()) {
                String locationHeader = response.getHeaderString("Location");
                if (locationHeader != null) {
                    String[] parts = locationHeader.split("/");
                    userId = parts[parts.length - 1];
                    log.info("User created successfully with ID: {}", userId);
                }
            } else if (status == Response.Status.CONFLICT.getStatusCode()) {
                List<UserRepresentation> foundUsers = usersResource.search(username, true);
                if (!foundUsers.isEmpty()) {
                    userId = foundUsers.get(0).getId();
                    log.info("User already exists in Keycloak with ID: {}", userId);
                }
            }

            if (userId == null) {
                throw new RuntimeException("Failed to obtain user ID from Keycloak user creation");
            }

            // Set password
            CredentialRepresentation passwordCred = new CredentialRepresentation();
            passwordCred.setType(CredentialRepresentation.PASSWORD);
            passwordCred.setValue(password);
            passwordCred.setTemporary(false);
            keycloak.realm(realm).users().get(userId).resetPassword(passwordCred);

            // Assign role
            UserResource userResource = usersResource.get(userId);
            RoleRepresentation roleRep = keycloak.realm(realm).roles().get(role != null ? role : "EMPLOYEE").toRepresentation();
            userResource.roles().realmLevel().add(Collections.singletonList(roleRep));

            // CRITICAL: Verify the hireDate was set correctly
            UserRepresentation createdUser = userResource.toRepresentation();
            Map<String, List<String>> createdAttributes = createdUser.getAttributes();

            if (createdAttributes != null && createdAttributes.containsKey("hireDate")) {
                List<String> hireDateList = createdAttributes.get("hireDate");
                if (hireDateList != null && !hireDateList.isEmpty()) {
                    String storedHireDate = hireDateList.get(0);
                    log.info("SUCCESS: Verified hireDate attribute for user {}: '{}'", username, storedHireDate);

                    // Additional verification
                    if (storedHireDate == null || storedHireDate.trim().isEmpty()) {
                        log.error("CRITICAL: hireDate was stored as empty/null for user: {}", username);
                    }
                } else {
                    log.error("CRITICAL: hireDate list is empty for user: {}", username);
                }
            } else {
                log.error("CRITICAL: hireDate attribute not found after user creation for: {}", username);
            }

            log.info("User created in Keycloak: {} with final hireDate: {}", username, finalHireDate);

        } catch (Exception e) {
            log.error("Error creating user in Keycloak: {}", e.getMessage());
            throw new RuntimeException("Error creating user in Keycloak", e);
        }
    }

    public void deleteUser(String username) {
        try {
            UsersResource usersResource = keycloak.realm(realm).users();
            List<UserRepresentation> users = usersResource.search(username, true);

            if (!users.isEmpty()) {
                String userId = users.get(0).getId();
                usersResource.delete(userId);
                log.info("Deleted user from Keycloak: {}", username);
            } else {
                log.warn("User not found in Keycloak for deletion: {}", username);
            }
        } catch (Exception e) {
            log.error("Error deleting user from Keycloak: {}", e.getMessage());
            throw new RuntimeException("Error deleting user from Keycloak", e);
        }
    }

    public void updatePassword(String username, String currentPassword, String newPassword) {
        try {
            UsersResource usersResource = keycloak.realm(realm).users();
            List<UserRepresentation> users = usersResource.search(username, true);

            if (users.isEmpty()) {
                throw new RuntimeException("User not found in Keycloak");
            }

            String userId = users.get(0).getId();
            UserResource userResource = usersResource.get(userId);

            try {
                getToken(username, currentPassword);
            } catch (Exception e) {
                throw new RuntimeException("Current password is incorrect");
            }

            CredentialRepresentation passwordCred = new CredentialRepresentation();
            passwordCred.setType(CredentialRepresentation.PASSWORD);
            passwordCred.setValue(newPassword);
            passwordCred.setTemporary(false);
            userResource.resetPassword(passwordCred);

            UserRepresentation user = userResource.toRepresentation();
            Map<String, List<String>> attributes = user.getAttributes();
            if (attributes == null) {
                attributes = new HashMap<>();
            }
            attributes.put("forcePasswordReset", Collections.singletonList("false"));
            user.setAttributes(attributes);
            userResource.update(user);

            log.info("Password updated for user: {}", username);
        } catch (Exception e) {
            log.error("Failed to update password in Keycloak for user {}: {}", username, e.getMessage());
            throw new RuntimeException("Failed to update Keycloak password", e);
        }
    }

    public void updateUserRole(String username, String newRole) {
        try {
            UsersResource usersResource = keycloak.realm(realm).users();
            List<UserRepresentation> userList = usersResource.search(username, true);
            if (userList.isEmpty()) {
                log.error("User '{}' not found in Keycloak realm '{}'", username, realm);
                throw new RuntimeException("User not found in Keycloak");
            }

            String userId = userList.get(0).getId();
            UserResource userResource = usersResource.get(userId);

            List<RoleRepresentation> currentRoles = userResource.roles().realmLevel().listEffective();
            log.debug("Current effective roles for user '{}': {}", username,
                    currentRoles.stream().map(RoleRepresentation::getName).collect(Collectors.toList()));

            List<RoleRepresentation> rolesToRemove = userResource.roles().realmLevel().listAll();
            if (!rolesToRemove.isEmpty()) {
                userResource.roles().realmLevel().remove(rolesToRemove);
                log.debug("Removed existing realm roles for user '{}'", username);
            }

            RoleRepresentation newRoleRep;
            try {
                newRoleRep = keycloak.realm(realm).roles().get(newRole).toRepresentation();
            } catch (Exception e) {
                log.error("Role '{}' does not exist in Keycloak realm '{}'", newRole, realm);
                throw new IllegalArgumentException("Role '" + newRole + "' does not exist in Keycloak");
            }

            userResource.roles().realmLevel().add(Collections.singletonList(newRoleRep));
            log.info("Assigned role '{}' to user '{}'", newRole, username);
        } catch (Exception e) {
            log.error("Failed to update role in Keycloak for user '{}': {}", username, e.getMessage(), e);
            throw new RuntimeException("Failed to update Keycloak role: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getUserDetails(String username) {
        try {
            UsersResource usersResource = keycloak.realm(realm).users();
            List<UserRepresentation> users = usersResource.search(username, true);
            if (users.isEmpty()) {
                throw new RuntimeException("User not found in Keycloak");
            }

            UserRepresentation user = users.get(0);
            UserResource userResource = usersResource.get(user.getId());
            Map<String, Object> userDetails = new HashMap<>();

            userDetails.put("userId", user.getId());
            userDetails.put("username", user.getUsername());
            userDetails.put("email", user.getEmail());
            userDetails.put("firstName", user.getFirstName());
            userDetails.put("lastName", user.getLastName());

            Map<String, List<String>> attributes = user.getAttributes();
            if (attributes != null) {
                userDetails.put("phone", getAttributeValue(attributes, "phone", ""));
                userDetails.put("address", getAttributeValue(attributes, "address", ""));

                // FIXED: Better hireDate handling
                String hireDate = getAttributeValue(attributes, "hireDate", null);
                if (hireDate == null || hireDate.trim().isEmpty()) {
                    log.warn("User {} has missing/empty hireDate, using current date", username);
                    hireDate = LocalDate.now().toString();
                    // Optionally update the user with the default hireDate
                    updateUserHireDate(user.getId(), hireDate);
                }
                userDetails.put("hireDate", hireDate);
                log.debug("Retrieved hireDate for user {}: '{}'", username, hireDate);

                userDetails.put("departmentId", getAttributeValue(attributes, "departmentId", ""));
                userDetails.put("managerId", getAttributeValue(attributes, "managerId", ""));
                userDetails.put("position", getAttributeValue(attributes, "position", ""));
                userDetails.put("status", getAttributeValue(attributes, "status", "ACTIVE"));
                userDetails.put("forcePasswordReset", getAttributeValue(attributes, "forcePasswordReset", "true"));
                userDetails.put("projectTitles", attributes.getOrDefault("projectTitles", Collections.emptyList()));

            } else {
                log.warn("No attributes found for user: {}, initializing defaults", username);
                String defaultHireDate = LocalDate.now().toString();

                userDetails.put("phone", "");
                userDetails.put("address", "");
                userDetails.put("hireDate", defaultHireDate);
                userDetails.put("departmentId", "");
                userDetails.put("managerId", "");
                userDetails.put("position", "");
                userDetails.put("status", "ACTIVE");
                userDetails.put("forcePasswordReset", "true");
                userDetails.put("projectTitles", Collections.emptyList());

                // Create default attributes for this user
                updateUserHireDate(user.getId(), defaultHireDate);
            }

            List<RoleRepresentation> realmRoles = userResource.roles().realmLevel().listEffective();
            List<String> roleNames = realmRoles.stream()
                    .map(RoleRepresentation::getName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            String primaryRole = roleNames.isEmpty() ? "EMPLOYEE" : roleNames.get(0);
            userDetails.put("role", primaryRole);
            userDetails.put("roles", roleNames);

            log.debug("Fetched user details for {}: roles={}, hireDate='{}', projectTitles={}",
                    username, roleNames, userDetails.get("hireDate"), userDetails.get("projectTitles"));
            return userDetails;

        } catch (Exception e) {
            log.error("Failed to fetch user details from Keycloak for user {}: {}", username, e.getMessage());
            throw new RuntimeException("Failed to fetch user details from Keycloak", e);
        }
    }

    private String getAttributeValue(Map<String, List<String>> attributes, String key, String defaultValue) {
        List<String> values = attributes.get(key);
        if (values == null || values.isEmpty()) {
            return defaultValue;
        }
        String value = values.get(0);
        return (value != null && !value.trim().isEmpty()) ? value.trim() : defaultValue;
    }

    // Helper method to update hireDate for a user
    private void updateUserHireDate(String userId, String hireDate) {
        try {
            UserResource userResource = keycloak.realm(realm).users().get(userId);
            UserRepresentation user = userResource.toRepresentation();

            Map<String, List<String>> attributes = user.getAttributes() != null ?
                    new HashMap<>(user.getAttributes()) : new HashMap<>();

            attributes.put("hireDate", Collections.singletonList(hireDate));
            user.setAttributes(attributes);
            userResource.update(user);

            log.info("Updated hireDate to '{}' for userId: {}", hireDate, userId);
        } catch (Exception e) {
            log.error("Failed to update hireDate for userId {}: {}", userId, e.getMessage());
        }
    }

    public Map<String, Object> getUserDetailsById(String userId) {
        try {
            UserResource userResource = keycloak.realm(realm).users().get(userId);
            UserRepresentation user = userResource.toRepresentation();

            Map<String, Object> userDetails = new HashMap<>();
            userDetails.put("userId", user.getId() != null ? user.getId() : "");
            userDetails.put("username", user.getUsername() != null ? user.getUsername() : "");
            userDetails.put("email", user.getEmail() != null ? user.getEmail() : "");
            userDetails.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
            userDetails.put("lastName", user.getLastName() != null ? user.getLastName() : "");

            Map<String, List<String>> attributes = user.getAttributes();
            if (attributes != null) {
                userDetails.put("phone", attributes.getOrDefault("phone", Collections.singletonList("")).get(0));
                userDetails.put("address", attributes.getOrDefault("address", Collections.singletonList("")).get(0));

                // FIXED: Proper hireDate handling
                List<String> hireDateList = attributes.getOrDefault("hireDate", Collections.singletonList(""));
                String hireDate = hireDateList.isEmpty() ? "" : hireDateList.get(0);
                userDetails.put("hireDate", (hireDate != null && !hireDate.trim().isEmpty()) ? hireDate : null);
                log.debug("Retrieved hireDate for userId {}: {}", userId, hireDate);

                userDetails.put("departmentId", attributes.getOrDefault("departmentId", Collections.singletonList("")).get(0));
                userDetails.put("managerId", attributes.getOrDefault("managerId", Collections.singletonList("")).get(0));
                userDetails.put("position", attributes.getOrDefault("position", Collections.singletonList("")).get(0));
                userDetails.put("status", attributes.getOrDefault("status", Collections.singletonList("ACTIVE")).get(0));
                userDetails.put("forcePasswordReset", attributes.getOrDefault("forcePasswordReset", Collections.singletonList("true")).get(0));
                userDetails.put("projectTitles", attributes.getOrDefault("projectTitles", Collections.emptyList()));
            } else {
                log.warn("No attributes found for userId: {}", userId);
                userDetails.put("phone", "");
                userDetails.put("address", "");
                userDetails.put("hireDate", null);
                userDetails.put("departmentId", "");
                userDetails.put("managerId", "");
                userDetails.put("position", "");
                userDetails.put("status", "ACTIVE");
                userDetails.put("forcePasswordReset", "true");
                userDetails.put("projectTitles", Collections.emptyList());
            }

            List<RoleRepresentation> realmRoles = userResource.roles().realmLevel().listEffective();
            List<String> roleNames = realmRoles.stream()
                    .map(RoleRepresentation::getName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            String primaryRole = roleNames.isEmpty() ? "EMPLOYEE" : roleNames.get(0);
            userDetails.put("role", primaryRole);
            userDetails.put("roles", roleNames);

            log.debug("Fetched user details by ID {}: roles={}, hireDate={}, projectTitles={}",
                    userId, roleNames, userDetails.get("hireDate"), userDetails.get("projectTitles"));
            return userDetails;
        } catch (Exception e) {
            log.error("Failed to fetch user details from Keycloak for user ID {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to fetch user details from Keycloak", e);
        }
    }

    public List<Map<String, Object>> getAllUsers() {
        try {
            UsersResource usersResource = keycloak.realm(realm).users();
            List<UserRepresentation> users = usersResource.list();
            List<Map<String, Object>> userDetailsList = new ArrayList<>();

            log.info("Fetching all users from Keycloak realm: {}", realm);
            if (users.isEmpty()) {
                log.warn("No users found in Keycloak realm: {}", realm);
            }

            for (UserRepresentation user : users) {
                UserResource userResource = usersResource.get(user.getId());
                Map<String, Object> userDetails = new HashMap<>();

                userDetails.put("userId", user.getId() != null ? user.getId() : "");
                userDetails.put("username", user.getUsername() != null ? user.getUsername() : "");
                userDetails.put("email", user.getEmail() != null ? user.getEmail() : "");
                userDetails.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
                userDetails.put("lastName", user.getLastName() != null ? user.getLastName() : "");

                Map<String, List<String>> attributes = user.getAttributes();
                if (attributes != null) {
                    userDetails.put("phone", attributes.getOrDefault("phone", Collections.singletonList("")).get(0));
                    userDetails.put("address", attributes.getOrDefault("address", Collections.singletonList("")).get(0));

                    // FIXED: Proper hireDate handling
                    List<String> hireDateList = attributes.getOrDefault("hireDate", Collections.singletonList(""));
                    String hireDate = hireDateList.isEmpty() ? "" : hireDateList.get(0);
                    userDetails.put("hireDate", (hireDate != null && !hireDate.trim().isEmpty()) ? hireDate : null);

                    userDetails.put("departmentId", attributes.getOrDefault("departmentId", Collections.singletonList("")).get(0));
                    userDetails.put("managerId", attributes.getOrDefault("managerId", Collections.singletonList("")).get(0));
                    userDetails.put("position", attributes.getOrDefault("position", Collections.singletonList("")).get(0));
                    userDetails.put("status", attributes.getOrDefault("status", Collections.singletonList("ACTIVE")).get(0));
                    userDetails.put("forcePasswordReset", attributes.getOrDefault("forcePasswordReset", Collections.singletonList("true")).get(0));
                    userDetails.put("projectTitles", attributes.getOrDefault("projectTitles", Collections.emptyList()));
                } else {
                    userDetails.put("phone", "");
                    userDetails.put("address", "");
                    userDetails.put("hireDate", null);
                    userDetails.put("departmentId", "");
                    userDetails.put("managerId", "");
                    userDetails.put("position", "");
                    userDetails.put("status", "ACTIVE");
                    userDetails.put("forcePasswordReset", "true");
                    userDetails.put("projectTitles", Collections.emptyList());
                }

                List<RoleRepresentation> realmRoles = userResource.roles().realmLevel().listEffective();
                List<String> roleNames = realmRoles.stream()
                        .map(RoleRepresentation::getName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                log.debug("User {} (ID: {}) effective roles: {}, hireDate: {}, projectTitles: {}",
                        user.getUsername(), user.getId(), roleNames, userDetails.get("hireDate"), userDetails.get("projectTitles"));

                String primaryRole = roleNames.isEmpty() ? "EMPLOYEE" : roleNames.get(0);
                userDetails.put("role", primaryRole);
                userDetails.put("roles", roleNames);

                userDetailsList.add(userDetails);
            }

            log.info("Successfully fetched {} users from Keycloak", userDetailsList.size());
            return userDetailsList;
        } catch (Exception e) {
            log.error("Failed to fetch all users from Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch all users from Keycloak", e);
        }
    }

    // Add this temporary debug method to your KeycloakService class
    public void debugUserAttributes(String username) {
        try {
            UsersResource usersResource = keycloak.realm(realm).users();
            List<UserRepresentation> users = usersResource.search(username, true);
            if (users.isEmpty()) {
                log.error("DEBUG: User not found: {}", username);
                return;
            }

            UserRepresentation user = users.get(0);
            log.info("DEBUG: User ID: {}", user.getId());
            log.info("DEBUG: Username: {}", user.getUsername());

            Map<String, List<String>> attributes = user.getAttributes();
            if (attributes == null) {
                log.error("DEBUG: No attributes found for user: {}", username);
                return;
            }

            log.info("DEBUG: All attributes for user {}:", username);
            attributes.forEach((key, value) -> {
                log.info("  {} = {} (type: {}, size: {})", key, value,
                        value != null ? value.getClass().getSimpleName() : "null",
                        value != null ? value.size() : "null");
            });

            // Specific hireDate debugging
            if (attributes.containsKey("hireDate")) {
                List<String> hireDateList = attributes.get("hireDate");
                log.info("DEBUG: hireDate attribute exists");
                log.info("  hireDate list: {}", hireDateList);
                log.info("  hireDate list size: {}", hireDateList != null ? hireDateList.size() : "null");
                if (hireDateList != null && !hireDateList.isEmpty()) {
                    String hireDateValue = hireDateList.get(0);
                    log.info("  hireDate value: '{}'", hireDateValue);
                    log.info("  hireDate value is null: {}", hireDateValue == null);
                    log.info("  hireDate value is empty: {}", hireDateValue != null ? hireDateValue.isEmpty() : "N/A");
                    log.info("  hireDate value trimmed is empty: {}", hireDateValue != null ? hireDateValue.trim().isEmpty() : "N/A");
                }
            } else {
                log.error("DEBUG: hireDate attribute NOT found in attributes map");
            }

        } catch (Exception e) {
            log.error("DEBUG: Error during attribute debugging: {}", e.getMessage(), e);
        }
    }

    public void migrateExistingUsersWithHireDate() {
        try {
            log.info("Starting migration: Adding hireDate to existing users without this attribute");
            UsersResource usersResource = keycloak.realm(realm).users();
            List<UserRepresentation> users = usersResource.list();

            int updatedCount = 0;
            int alreadyHasHireDateCount = 0;
            String defaultHireDate = LocalDate.now().toString();

            for (UserRepresentation user : users) {
                try {
                    Map<String, List<String>> attributes = user.getAttributes();
                    boolean needsUpdate = false;

                    if (attributes == null) {
                        attributes = new HashMap<>();
                        needsUpdate = true;
                    } else {
                        String currentHireDate = getAttributeValue(attributes, "hireDate", null);
                        if (currentHireDate == null) {
                            needsUpdate = true;
                        }
                    }

                    if (needsUpdate) {
                        attributes.put("hireDate", Collections.singletonList(defaultHireDate));
                        user.setAttributes(attributes);

                        UserResource userResource = usersResource.get(user.getId());
                        userResource.update(user);

                        log.info("Migration: Added hireDate '{}' to user: {} ({})",
                                defaultHireDate, user.getUsername(), user.getId());
                        updatedCount++;
                    } else {
                        alreadyHasHireDateCount++;
                        log.debug("Migration: User {} already has hireDate", user.getUsername());
                    }

                } catch (Exception e) {
                    log.error("Migration: Failed to update user {} ({}): {}",
                            user.getUsername(), user.getId(), e.getMessage());
                }
            }

            log.info("Migration completed: {} users updated with hireDate, {} users already had hireDate",
                    updatedCount, alreadyHasHireDateCount);

        } catch (Exception e) {
            log.error("Migration failed: Error during hireDate migration: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to migrate existing users with hireDate", e);
        }
    }



    public void updateUserAttributes(String userId, UserDTO updateRequest) {
        try {
            UserResource userResource = keycloak.realm(realm).users().get(userId);
            UserRepresentation user = userResource.toRepresentation();

            // Update basic fields
            Optional.ofNullable(updateRequest.getFirstName()).ifPresent(user::setFirstName);
            Optional.ofNullable(updateRequest.getLastName()).ifPresent(user::setLastName);
            Optional.ofNullable(updateRequest.getEmail()).ifPresent(user::setEmail);

            // Update attributes
            Map<String, List<String>> attributes = user.getAttributes() != null ? new HashMap<>(user.getAttributes()) : new HashMap<>();

            if (updateRequest.getPhone() != null) attributes.put("phone", Collections.singletonList(updateRequest.getPhone()));
            if (updateRequest.getAddress() != null) attributes.put("address", Collections.singletonList(updateRequest.getAddress()));
            if (updateRequest.getPosition() != null) attributes.put("position", Collections.singletonList(updateRequest.getPosition()));
            if (updateRequest.getHireDate() != null) attributes.put("hireDate", Collections.singletonList(updateRequest.getHireDate()));
            if (updateRequest.getDepartmentId() != null) attributes.put("departmentId", Collections.singletonList(updateRequest.getDepartmentId().toString()));
            if (updateRequest.getManagerId() != null) attributes.put("managerId", Collections.singletonList(updateRequest.getManagerId().toString()));
            // Preserve projectTitles if not explicitly updated
            attributes.putIfAbsent("projectTitles", Collections.emptyList());

            user.setAttributes(attributes);

            userResource.update(user);
            log.info("Successfully updated user attributes for userId: {}", userId);
        } catch (Exception e) {
            log.error("Failed to update user attributes in Keycloak for userId {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to update user attributes in Keycloak", e);
        }
    }

    public void updateUserProjects(String userId, List<String> projectTitles) {
        try {
            UserResource userResource = keycloak.realm(realm).users().get(userId);
            UserRepresentation user = userResource.toRepresentation();

            // Get existing attributes or create if null
            Map<String, List<String>> attributes = user.getAttributes() != null ?
                    new HashMap<>(user.getAttributes()) : new HashMap<>();

            // Log current projectTitles
            log.info("Before update - projectTitles for userId {}: {}", userId,
                    attributes.getOrDefault("projectTitles", Collections.emptyList()));

            // Create a new ArrayList from the input list to ensure proper serialization
            List<String> projectTitlesCopy = projectTitles != null ?
                    new ArrayList<>(projectTitles) : new ArrayList<>();

            // Update projectTitles attribute
            attributes.put("projectTitles", projectTitlesCopy);
            user.setAttributes(attributes);

            // Perform the update
            userResource.update(user);

            // Verify update
            UserRepresentation updatedUser = userResource.toRepresentation();
            Map<String, List<String>> updatedAttributes = updatedUser.getAttributes();
            List<String> updatedProjects = updatedAttributes != null ?
                    updatedAttributes.getOrDefault("projectTitles", Collections.emptyList()) :
                    Collections.emptyList();

            log.info("After update - projectTitles for userId {}: {}", userId, updatedProjects);

            if (!new HashSet<>(updatedProjects).equals(new HashSet<>(projectTitlesCopy))) {
                log.warn("Project titles update may not have persisted correctly for userId {}: expected {}, got {}",
                        userId, projectTitlesCopy, updatedProjects);
            }
        } catch (Exception e) {
            log.error("Failed to update user projects in Keycloak for userId {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to update user projects in Keycloak", e);
        }
    }

    public void updateUserPosition(String userId, String position) {
        try {
            UserResource userResource = keycloak.realm(realm).users().get(userId);
            UserRepresentation user = userResource.toRepresentation();
            Map<String, List<String>> attributes = user.getAttributes() != null ?
                    new HashMap<>(user.getAttributes()) : new HashMap<>();
            attributes.put("position", Collections.singletonList(position));
            user.setAttributes(attributes);
            userResource.update(user);
            log.info("Updated position for userId {}: {}", userId, position);
        } catch (Exception e) {
            log.error("Failed to update user position in Keycloak for userId {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to update user position in Keycloak", e);
        }
    }
}