package com.sourabh.ecommerce_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
/**
 * MAIN APPLICATION ENTRY POINT
 * 
 * This is the Spring Boot application entry point for this microservice.
 * Enables component scanning, sets up Spring contexts, and initializes all beans.
 */
public class EcommerceBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(EcommerceBackendApplication.class, args);
	}

}
