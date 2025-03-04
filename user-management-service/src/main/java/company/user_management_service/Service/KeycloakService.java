package company.user_management_service.Service;

import jakarta.annotation.PostConstruct;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

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

    private Keycloak keycloak;

    @PostConstruct
    public void init() {
        keycloak = KeycloakBuilder.builder()
                .serverUrl(authServerUrl)
                .realm("master")
                .username("admin")
                .password("admin")
                .clientId("admin-cli")
                .build();

        // Create realm if it doesn’t exist
        if (keycloak.realms().findAll().stream().noneMatch(r -> r.getRealm().equals(realm))) {
            RealmRepresentation realmRep = new RealmRepresentation();
            realmRep.setRealm(realm);
            realmRep.setEnabled(true);
            keycloak.realms().create(realmRep);
        }

        // Create client if it doesn’t exist
        RealmResource realmResource = keycloak.realm(realm);
        if (realmResource.clients().findByClientId(clientId).isEmpty()) {
            ClientRepresentation client = new ClientRepresentation();
            client.setClientId(clientId);
            client.setSecret(clientSecret);
            client.setDirectAccessGrantsEnabled(true);
            client.setPublicClient(false);
            client.setProtocol("openid-connect");
            client.setEnabled(true);
            realmResource.clients().create(client);
        }
    }

    public void createUser(String username, String email, String password) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setEnabled(true);

        CredentialRepresentation creds = new CredentialRepresentation();
        creds.setType(CredentialRepresentation.PASSWORD);
        creds.setValue(password);
        creds.setTemporary(false);
        user.setCredentials(Collections.singletonList(creds));

        keycloak.realm(realm).users().create(user);
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
}