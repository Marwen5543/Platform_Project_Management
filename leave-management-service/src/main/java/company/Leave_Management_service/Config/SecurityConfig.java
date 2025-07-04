package company.Leave_Management_service.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.SecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${keycloak.auth-server-url}")
    private String keycloakAuthServerUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, RoleHierarchy roleHierarchy) throws Exception {
        return http
                .csrf(Customizer.withDefaults()) // Disable CSRF for stateless APIs
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/leaves/request").access(new RoleHierarchyAuthoritiesMapper(roleHierarchy, "ROLE_EMPLOYEE"))
                        .requestMatchers("/api/leaves/history").access(new RoleHierarchyAuthoritiesMapper(roleHierarchy, "ROLE_EMPLOYEE", "ROLE_MANAGER", "ROLE_HR"))
                        .requestMatchers("/api/leaves/team").access(new RoleHierarchyAuthoritiesMapper(roleHierarchy, "ROLE_MANAGER", "ROLE_HR"))
                        .requestMatchers("/api/leaves/*/status").access(new RoleHierarchyAuthoritiesMapper(roleHierarchy, "ROLE_MANAGER", "ROLE_HR"))
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> realmRoles = jwt.getClaimAsMap("realm_access") != null &&
                    jwt.getClaimAsMap("realm_access").get("roles") != null
                    ? (List<String>) jwt.getClaimAsMap("realm_access").get("roles")
                    : Collections.emptyList();
            System.out.println("JWT Roles: " + realmRoles);
            return realmRoles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        });
        return converter;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        String jwkSetUri = keycloakAuthServerUrl + "/realms/Tunisys/protocol/openid-connect/certs";
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();
        roleHierarchy.setHierarchy("ROLE_SUPER_ADMIN > ROLE_ADMIN\nROLE_ADMIN > ROLE_MANAGER\nROLE_MANAGER > ROLE_HR\nROLE_HR > ROLE_EMPLOYEE");
        return roleHierarchy;
    }

    @Bean
    public SecurityExpressionHandler<FilterInvocation> expressionHandler(RoleHierarchy roleHierarchy) {
        DefaultWebSecurityExpressionHandler handler = new DefaultWebSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200")); // Add Angular app URL
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    static class RoleHierarchyAuthoritiesMapper implements AuthorizationManager<RequestAuthorizationContext> {
        private final RoleHierarchy roleHierarchy;
        private final String[] requiredRoles;

        RoleHierarchyAuthoritiesMapper(RoleHierarchy roleHierarchy, String... requiredRoles) {
            this.roleHierarchy = roleHierarchy;
            this.requiredRoles = requiredRoles;
        }

        @Override
        public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
            Authentication auth = authentication.get();
            List<GrantedAuthority> authorities = auth.getAuthorities().stream().collect(Collectors.toList());
            for (String role : requiredRoles) {
                if (roleHierarchy.getReachableGrantedAuthorities(authorities).stream()
                        .anyMatch(a -> a.getAuthority().equals(role))) {
                    return new AuthorizationDecision(true);
                }
            }
            return new AuthorizationDecision(false);
        }
    }
}