package com.sourabh.payment_service;

import org.springframework.boot.SpringApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableJpaAuditing
/**
 * MAIN APPLICATION ENTRY POINT
 * 
 * This is the Spring Boot application entry point for this microservice.
 * Enables component scanning, sets up Spring contexts, and initializes all beans.
 */
public class PaymentServiceApplication {

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
  * @SpringBootApplication - Applied to this method
  * @EnableJpaAuditing - Applied to this method
  *
  */
	public static void main(String[] args) {
		SpringApplication.run(PaymentServiceApplication.class, args);
	}

}
