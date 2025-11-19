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

    /*@PostConstruct
    public void init() {
        try {
            if (provisioningEnabled) {
                // This is the old code, for the developer's local machine ONLY
                log.warn("Keycloak provisioning is ENABLED. This should not be used in production.");
                keycloak = KeycloakBuilder.builder()
                        .serverUrl(authServerUrl)
                        .realm("master")
                        .username("admin")
                        .password("admin")
                        .clientId("admin-cli")
                        .build();
                // ... (keep the realmExists, createRealm logic here) ...

            } else {
                // This is the new, correct code for Kubernetes/Production
                log.info("Keycloak provisioning is DISABLED. Initializing standard admin client.");
                keycloak = KeycloakBuilder.builder()
                        .serverUrl(authServerUrl)
                        .realm(realm)
                        .grantType("client_credentials")
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .build();
            }
            log.info("Keycloak client initialized successfully.");
        } catch (Exception e) {
            log.error("Keycloak initialization failed: {}", e.getMessage());
            throw new RuntimeException("Keycloak configuration error", e);
        }
    }*/

    @PostConstruct
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
    }
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
            attributes.put("hireDate", Collections.singletonList(hireDate != null ? hireDate : ""));
            attributes.put("departmentId", Collections.singletonList(departmentId != null ? departmentId.toString() : ""));
            attributes.put("managerId", Collections.singletonList(managerId != null ? managerId.toString() : ""));
            attributes.put("position", Collections.singletonList(position != null ? position : ""));
            attributes.put("status", Collections.singletonList("ACTIVE"));
            attributes.put("forcePasswordReset", Collections.singletonList("true"));
            attributes.put("projectTitles", Collections.emptyList()); // Initialize projectTitles
            user.setAttributes(attributes);

            Response response = usersResource.create(user);
            int status = response.getStatus();
            String userId = null;

            if (status == Response.Status.CREATED.getStatusCode()) {
                String locationHeader = response.getHeaderString("Location");
                if (locationHeader != null) {
                    String[] parts = locationHeader.split("/");
                    userId = parts[parts.length - 1];
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

            CredentialRepresentation passwordCred = new CredentialRepresentation();
            passwordCred.setType(CredentialRepresentation.PASSWORD);
            passwordCred.setValue(password);
            passwordCred.setTemporary(false);
            keycloak.realm(realm).users().get(userId).resetPassword(passwordCred);

            UserResource userResource = usersResource.get(userId);
            RoleRepresentation roleRep = keycloak.realm(realm).roles().get(role != null ? role : "EMPLOYEE").toRepresentation();
            userResource.roles().realmLevel().add(Collections.singletonList(roleRep));

            log.info("User created in Keycloak: {}", username);
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
                userDetails.put("phone", attributes.getOrDefault("phone", Collections.singletonList("")).get(0));
                userDetails.put("address", attributes.getOrDefault("address", Collections.singletonList("")).get(0));
                userDetails.put("hireDate", attributes.getOrDefault("hireDate", Collections.singletonList("")).get(0));
                userDetails.put("departmentId", attributes.getOrDefault("departmentId", Collections.singletonList("")).get(0));
                userDetails.put("managerId", attributes.getOrDefault("managerId", Collections.singletonList("")).get(0));
                userDetails.put("position", attributes.getOrDefault("position", Collections.singletonList("")).get(0));
                userDetails.put("status", attributes.getOrDefault("status", Collections.singletonList("ACTIVE")).get(0));
                userDetails.put("forcePasswordReset", attributes.getOrDefault("forcePasswordReset", Collections.singletonList("true")).get(0));
                userDetails.put("projectTitles", attributes.getOrDefault("projectTitles", Collections.emptyList()));
            } else {
                userDetails.put("phone", "");
                userDetails.put("address", "");
                userDetails.put("hireDate", "");
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

            log.debug("Fetched user details for {}: roles={}, projectTitles={}", username, roleNames, userDetails.get("projectTitles"));
            return userDetails;
        } catch (Exception e) {
            log.error("Failed to fetch user details from Keycloak for user {}: {}", username, e.getMessage());
            throw new RuntimeException("Failed to fetch user details from Keycloak", e);
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
                userDetails.put("hireDate", attributes.getOrDefault("hireDate", Collections.singletonList("")).get(0));
                userDetails.put("departmentId", attributes.getOrDefault("departmentId", Collections.singletonList("")).get(0));
                userDetails.put("managerId", attributes.getOrDefault("managerId", Collections.singletonList("")).get(0));
                userDetails.put("position", attributes.getOrDefault("position", Collections.singletonList("")).get(0));
                userDetails.put("status", attributes.getOrDefault("status", Collections.singletonList("ACTIVE")).get(0));
                userDetails.put("forcePasswordReset", attributes.getOrDefault("forcePasswordReset", Collections.singletonList("true")).get(0));
                userDetails.put("projectTitles", attributes.getOrDefault("projectTitles", Collections.emptyList()));
            } else {
                userDetails.put("phone", "");
                userDetails.put("address", "");
                userDetails.put("hireDate", "");
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

            log.debug("Fetched user details by ID {}: roles={}, projectTitles={}", userId, roleNames, userDetails.get("projectTitles"));
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
                    userDetails.put("hireDate", attributes.getOrDefault("hireDate", Collections.singletonList("")).get(0));
                    userDetails.put("departmentId", attributes.getOrDefault("departmentId", Collections.singletonList("")).get(0));
                    userDetails.put("managerId", attributes.getOrDefault("managerId", Collections.singletonList("")).get(0));
                    userDetails.put("position", attributes.getOrDefault("position", Collections.singletonList("")).get(0));
                    userDetails.put("status", attributes.getOrDefault("status", Collections.singletonList("ACTIVE")).get(0));
                    userDetails.put("forcePasswordReset", attributes.getOrDefault("forcePasswordReset", Collections.singletonList("true")).get(0));
                    userDetails.put("projectTitles", attributes.getOrDefault("projectTitles", Collections.emptyList()));
                } else {
                    userDetails.put("phone", "");
                    userDetails.put("address", "");
                    userDetails.put("hireDate", "");
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

                log.debug("User {} (ID: {}) effective roles: {}, projectTitles: {}",
                        user.getUsername(), user.getId(), roleNames, userDetails.get("projectTitles"));

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