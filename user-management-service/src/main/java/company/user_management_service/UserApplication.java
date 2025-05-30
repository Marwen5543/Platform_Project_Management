package company.user_management_service;

import company.user_management_service.Service.KeycloakService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Map;

@SpringBootApplication
public class UserApplication {
    public static void main(String[] args) {
        // Log Keycloak-related environment variables instead of database variables
        System.out.println("Raw KEYCLOAK_AUTH_SERVER_URL: " + System.getenv("KEYCLOAK_AUTH_SERVER_URL"));
        System.out.println("Raw KEYCLOAK_REALM: " + System.getenv("KEYCLOAK_REALM"));
        System.out.println("Raw KEYCLOAK_RESOURCE: " + System.getenv("KEYCLOAK_RESOURCE"));

        SpringApplication app = new SpringApplication(UserApplication.class);
        ConfigurableEnvironment env = app.run(args).getEnvironment();

        // Log effective Keycloak properties
        System.out.println("Effective KEYCLOAK_AUTH_SERVER_URL: " + env.getProperty("keycloak.auth-server-url"));
        System.out.println("Effective KEYCLOAK_REALM: " + env.getProperty("keycloak.realm"));
        System.out.println("Effective KEYCLOAK_RESOURCE: " + env.getProperty("keycloak.resource"));
    }

    /*@Bean
    CommandLineRunner initSuperAdmin(KeycloakService keycloakService) {
        return args -> {
            String superAdminUsername = "superadmin";
            try {
                // Check if the super admin user already exists in Keycloak
                Map<String, Object> existingUser = keycloakService.getUserDetails(superAdminUsername);
                if (existingUser != null && existingUser.get("username") != null) {
                    System.out.println("Super Admin already exists in Keycloak: username=superadmin");
                    return;
                }
            } catch (Exception e) {
                // If the user is not found, proceed to create it
                System.out.println("Super Admin not found in Keycloak, creating new user...");
            }

            // Create the super admin user in Keycloak
            keycloakService.createUser(
                    superAdminUsername,
                    "superadmin@yourcompany.com",
                    "ChangeMe123!", // Initial password, user will be forced to reset
                    "SUPER_ADMIN",
                    "System",
                    "Admin",
                    null, // phone
                    null, // address
                    null, // hireDate
                    null, // departmentId
                    null, // managerId
                    null  // position
            );
            System.out.println("Super Admin created in Keycloak: username=superadmin, password=ChangeMe123!");
        };
    }*/
}