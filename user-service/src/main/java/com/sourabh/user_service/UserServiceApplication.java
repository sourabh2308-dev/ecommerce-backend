package com.sourabh.user_service;

import org.springframework.boot.SpringApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
/**
 * MAIN APPLICATION ENTRY POINT
 * 
 * This is the Spring Boot application entry point for this microservice.
 * Enables component scanning, sets up Spring contexts, and initializes all beans.
 */
public class UserServiceApplication {

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
		SpringApplication.run(UserServiceApplication.class, args);
	}

}
