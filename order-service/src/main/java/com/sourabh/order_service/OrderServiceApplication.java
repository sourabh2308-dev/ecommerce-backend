package com.sourabh.order_service;

import org.springframework.boot.SpringApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableFeignClients
@EnableScheduling
/**
 * MAIN APPLICATION ENTRY POINT
 * 
 * This is the Spring Boot application entry point for this microservice.
 * Enables component scanning, sets up Spring contexts, and initializes all beans.
 */
public class OrderServiceApplication {

    /**
     * MAIN - Method Documentation
     *
     * PURPOSE:
     * This method handles the main operation.
     *
     * PARAMETERS:
     * @param args - String[] value
     *
     * RETURN VALUE:
     * @return static void - Result of the operation
     *
     * ANNOTATIONS USED:
     * @EnableJpaAuditing - Applied to this method
     * @EnableFeignClients - Applied to this method
     *
     */
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
