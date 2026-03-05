package com.sourabh.user_service;

import org.springframework.boot.SpringApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Entry point for the User Service microservice.
 *
 * <p>Bootstraps the Spring Boot application context with the following capabilities:</p>
 * <ul>
 *   <li>{@code @SpringBootApplication} &ndash; enables auto-configuration, component scanning,
 *       and Spring Boot property support.</li>
 *   <li>{@code @EnableFeignClients} &ndash; activates OpenFeign declarative HTTP clients used
 *       for inter-service communication (e.g. order-service invoice retrieval).</li>
 *   <li>{@code @EnableJpaAuditing} &ndash; enables automatic population of {@code @CreatedDate}
 *       and {@code @LastModifiedDate} fields on JPA entities.</li>
 *   <li>{@code @EnableAsync} &ndash; enables asynchronous method execution (used by
 *       {@link com.sourabh.user_service.service.impl.EmailServiceImpl} to send emails
 *       without blocking the request thread).</li>
 *   <li>{@code @EnableScheduling} &ndash; enables scheduled tasks such as loyalty-point
 *       expiration cron jobs.</li>
 * </ul>
 */
@SpringBootApplication
@EnableFeignClients
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class UserServiceApplication {

	/**
	 * JVM entry point &ndash; launches the embedded Tomcat server and initialises
	 * all Spring beans.
	 *
	 * @param args command-line arguments (passed through to Spring Boot)
	 */
	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}

}
