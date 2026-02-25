package com.sourabh.config_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer
/**
 * MAIN APPLICATION ENTRY POINT
 * 
 * This is the Spring Boot application entry point for this microservice.
 * Enables component scanning, sets up Spring contexts, and initializes all beans.
 */
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
