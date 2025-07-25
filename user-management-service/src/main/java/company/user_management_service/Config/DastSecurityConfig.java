package company.user_management_service.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("dast") // This configuration only activates when the 'dast' profile is active
public class DastSecurityConfig {

    @Bean
    public SecurityFilterChain dastFilterChain(HttpSecurity http) throws Exception {
        http
                // Allow all requests without authentication.
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                // Disable CSRF protection, which can interfere with scanners.
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}