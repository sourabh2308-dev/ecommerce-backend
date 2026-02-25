package com.sourabh.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;

@SpringBootApplication(
		exclude = {
				ReactiveUserDetailsServiceAutoConfiguration.class
		}
)
/**
 * MAIN APPLICATION ENTRY POINT
 * 
 * This is the Spring Boot application entry point for this microservice.
 * Enables component scanning, sets up Spring contexts, and initializes all beans.
 */
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}
}
