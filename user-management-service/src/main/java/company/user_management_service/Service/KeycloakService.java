package company.user_management_service.Service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.OAuth2Constants;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@Slf4j
public class KeycloakService {
    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    // Keep the admin credentials separate - we need them for creating users
    private static final String ADMIN_CLIENT_ID = "admin-cli";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";
    private static final String ADMIN_REALM = "master";

    public String getToken(String username, String password) {
        log.info("Starting token request for user: {}", username);

        try {
            Keycloak keycloak = KeycloakBuilder.builder()
                    .serverUrl(authServerUrl)
                    .realm(realm)
                    .grantType(OAuth2Constants.PASSWORD)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .username(username)
                    .password(password)
                    .build();

            log.info("Attempting to get token from Keycloak...");
            String token = keycloak.tokenManager().getAccessToken().getToken();
            log.info("Successfully obtained token");
            return token;
        } catch (Exception e) {
            log.error("Token request failed. Error: {}", e.getMessage());
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }

    public void createUser(String username, String email, String password) {
        try {
            // Use admin credentials to create users
            Keycloak adminKeycloak = KeycloakBuilder.builder()
                    .serverUrl(authServerUrl)
                    .realm(ADMIN_REALM)
                    .clientId(ADMIN_CLIENT_ID)
                    .username(ADMIN_USERNAME)
                    .password(ADMIN_PASSWORD)
                    .build();

            UserRepresentation user = new UserRepresentation();
            user.setEnabled(true);
            user.setUsername(username);
            user.setEmail(email);

            // Create the user
            jakarta.ws.rs.core.Response response = adminKeycloak.realm(realm).users().create(user);

            if (response.getStatus() == 201) {
                String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

                // Set password
                CredentialRepresentation credential = new CredentialRepresentation();
                credential.setType(CredentialRepresentation.PASSWORD);
                credential.setValue(password);
                credential.setTemporary(false);

                adminKeycloak.realm(realm).users().get(userId).resetPassword(credential);
                log.info("Successfully created user in Keycloak: {}", username);
            } else {
                log.error("Failed to create user in Keycloak. Status: {}", response.getStatus());
                throw new RuntimeException("Failed to create user in Keycloak. Status: " + response.getStatus());
            }
        } catch (Exception e) {
            log.error("Failed to create user in Keycloak: {}", e.getMessage());
            throw e;
        }
    }

    @PostConstruct
    public void init() {
        log.info("Initializing KeycloakService with configuration:");
        log.info("Auth Server URL: {}", authServerUrl);
        log.info("Realm: {}", realm);
        log.info("Client ID: {}", clientId);
        log.info("Client Secret exists: {}", clientSecret != null && !clientSecret.isEmpty());
    }
}