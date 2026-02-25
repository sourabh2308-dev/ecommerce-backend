package com.sourabh.eureka_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
/**
 * MAIN APPLICATION ENTRY POINT
 * 
 * This is the Spring Boot application entry point for this microservice.
 * Enables component scanning, sets up Spring contexts, and initializes all beans.
 */
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
