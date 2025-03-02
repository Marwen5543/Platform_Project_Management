package company.user_management_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;

@SpringBootApplication
public class UserApplication {
    public static void main(String[] args) {
        System.out.println("Raw SPRING_DATASOURCE_URL: " + System.getenv("SPRING_DATASOURCE_URL"));
        System.out.println("Raw SPRING_DATASOURCE_USERNAME: " + System.getenv("SPRING_DATASOURCE_USERNAME"));
        System.out.println("Raw SPRING_DATASOURCE_PASSWORD: " + System.getenv("SPRING_DATASOURCE_PASSWORD"));
        SpringApplication app = new SpringApplication(UserApplication.class);
        ConfigurableEnvironment env = app.run(args).getEnvironment();
        System.out.println("Effective SPRING_DATASOURCE_URL: " + env.getProperty("spring.datasource.url"));
        System.out.println("Effective SPRING_DATASOURCE_USERNAME: " + env.getProperty("spring.datasource.username"));
        System.out.println("Effective SPRING_DATASOURCE_PASSWORD: " + env.getProperty("spring.datasource.password"));
    }
}