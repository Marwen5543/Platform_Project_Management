package org.example.documentmanagementservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DocumentManagementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentManagementServiceApplication.class, args);
    }

}
