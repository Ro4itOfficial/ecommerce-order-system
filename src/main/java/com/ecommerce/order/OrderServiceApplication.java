package com.ecommerce.order;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableJpaAuditing
@ConfigurationPropertiesScan
@OpenAPIDefinition(
    info = @Info(
        title = "E-Commerce Order Processing API",
        version = "1.0.0",
        description = "RESTful API for managing e-commerce orders with JWT authentication, rate limiting, and caching",
        contact = @Contact(
            name = "API Support",
            email = "api-support@ecommerce.com",
            url = "https://ecommerce.com/support"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    )
)
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
        log.info("""
            
            ====================================
            Order Service Started Successfully!
            ====================================
            - Swagger UI: http://localhost:8080/swagger-ui.html
            - Health: http://localhost:8080/actuator/health
            - Metrics: http://localhost:8080/actuator/prometheus
            ====================================
            """);
    }
}