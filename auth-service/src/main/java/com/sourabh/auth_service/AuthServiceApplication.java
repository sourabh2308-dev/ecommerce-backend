package com.sourabh.auth_service;

import org.springframework.boot.SpringApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@EnableMethodSecurity
@SpringBootApplication
@EnableJpaAuditing
public class AuthServiceApplication {
    public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}
}

