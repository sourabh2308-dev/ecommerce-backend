package com.sourabh.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;

/**
 * Entry point for the API Gateway microservice.
 *
 * <p>This Spring Boot application serves as the single ingress point for the
 * entire e-commerce microservices platform. All external HTTP traffic is routed
 * through this gateway before reaching any downstream service.
 *
 * <h3>Core Responsibilities</h3>
 * <ul>
 *   <li>JWT authentication &mdash; validates bearer tokens and injects identity
 *       headers ({@code X-User-UUID}, {@code X-User-Role}, {@code X-User-Email})
 *       into forwarded requests.</li>
 *   <li>Route management &mdash; forwards requests to the correct downstream
 *       microservice (auth, user, product, order, review, payment) via
 *       Spring Cloud Gateway route definitions.</li>
 *   <li>Rate limiting &mdash; enforces per-IP request quotas using a
 *       token-bucket algorithm backed by Bucket4j.</li>
 *   <li>Correlation ID propagation &mdash; generates or forwards a unique
 *       {@code X-Correlation-Id} header for distributed tracing across all
 *       services.</li>
 *   <li>Internal secret injection &mdash; attaches an {@code X-Internal-Secret}
 *       header so downstream services can verify the request originated from
 *       the gateway rather than a direct external call.</li>
 *   <li>Swagger UI aggregation &mdash; exposes a unified OpenAPI UI that
 *       consolidates the API docs of every downstream service.</li>
 * </ul>
 *
 * <h3>Annotation Details</h3>
 * <p>{@code @SpringBootApplication} enables auto-configuration and component
 * scanning. {@link ReactiveUserDetailsServiceAutoConfiguration} is explicitly
 * excluded because the gateway does not manage user credentials; it delegates
 * authentication entirely to JWT validation.</p>
 *
 * @see com.sourabh.api_gateway.security.SecurityConfig
 * @see com.sourabh.api_gateway.filter.RateLimitGlobalFilter
 * @see com.sourabh.api_gateway.filter.CorrelationIdGlobalFilter
 */
@SpringBootApplication(
		exclude = {
				ReactiveUserDetailsServiceAutoConfiguration.class
		}
)
public class ApiGatewayApplication {

	/**
	 * Bootstrap method that launches the reactive Netty server and
	 * initialises the Spring application context.
	 *
	 * @param args command-line arguments forwarded to Spring Boot
	 */
	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}
}
