package com.sourabh.order_service.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import com.sourabh.order_service.repository.AuditLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Spring AOP aspect that automatically persists audit trail records whenever a
 * method annotated with {@link Auditable} completes successfully.
 *
 * <p>The aspect captures contextual information from the current HTTP request
 * (actor UUID, role, IP address) and combines it with metadata declared on the
 * annotation (action type, resource type) to build a complete {@link AuditLog}
 * entity that is then saved via {@link com.sourabh.order_service.repository.AuditLogRepository}.</p>
 *
 * <p>Failures during audit recording are logged but never propagated, ensuring
 * that audit instrumentation does not interfere with normal request processing.</p>
 *
 * @see Auditable
 * @see AuditLog
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    /** Repository used to persist audit log entries to the database. */
    private final AuditLogRepository auditLogRepository;

    /**
     * Advice executed after a method annotated with {@link Auditable} returns
     * successfully. Extracts actor and request metadata, builds an
     * {@link AuditLog} entity, and persists it.
     *
     * @param joinPoint the join point providing reflective access to the intercepted method
     * @param auditable the {@link Auditable} annotation instance carrying action metadata
     */
    @AfterReturning("@annotation(auditable)")
    public void auditAfter(JoinPoint joinPoint, Auditable auditable) {
        try {
            String actorUuid = extractActorUuid();
            String actorRole = extractActorRole();
            String ipAddress = extractIpAddress();
            Object[] args = joinPoint.getArgs();
            String resourceId = extractResourceId(args, auditable);

                AuditLog auditLog = AuditLog.builder()
                    .action(auditable.action())
                    .actorUuid(actorUuid)
                    .actorRole(actorRole)
                    .resourceType(auditable.resourceType())
                    .resourceId(resourceId)
                    .ipAddress(ipAddress)
                    .details(joinPoint.getSignature().getName())
                    .changeData(null)
                    .build();

            auditLogRepository.save(auditLog);
            log.info("Audit: {} {} {} by {}", auditable.action(), auditable.resourceType(), resourceId, actorUuid);
        } catch (Exception e) {
            log.error("Failed to record audit log: {}", e.getMessage());
        }
    }

    /**
     * Extracts the authenticated user's UUID from the {@code X-User-UUID}
     * HTTP request header forwarded by the API Gateway.
     *
     * @return the user UUID, or {@code "SYSTEM"} if unavailable
     */
    private String extractActorUuid() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                return request.getHeader("X-User-UUID");
            }
        } catch (Exception e) {
            log.debug("Could not extract actor UUID from request");
        }
        return "SYSTEM";
    }

    /**
     * Extracts the authenticated user's role from the {@code X-User-Role}
     * HTTP request header forwarded by the API Gateway.
     *
     * @return the user role string, or {@code "UNKNOWN"} if unavailable
     */
    private String extractActorRole() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                return request.getHeader("X-User-Role");
            }
        } catch (Exception e) {
            log.debug("Could not extract actor role from request");
        }
        return "UNKNOWN";
    }

    /**
     * Extracts the client IP address from the current HTTP request.
     * Prefers the {@code X-Forwarded-For} header (set by proxies/load balancers)
     * and falls back to {@link HttpServletRequest#getRemoteAddr()}.
     *
     * @return the client IP address, or {@code "UNKNOWN"} if unavailable
     */
    private String extractIpAddress() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty()) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception e) {
            log.debug("Could not extract IP from request");
        }
        return "UNKNOWN";
    }

    /**
     * Attempts to resolve the resource identifier from the method arguments.
     * Uses the first {@link String} argument as a simple fallback strategy.
     *
     * @param args      the arguments passed to the intercepted method
     * @param auditable the annotation instance (reserved for future SpEL evaluation)
     * @return the extracted resource ID, or {@code "UNKNOWN"} if it cannot be determined
     */
    private String extractResourceId(Object[] args, Auditable auditable) {
        if (args.length > 0 && args[0] instanceof String) {
            return (String) args[0];
        }
        return "UNKNOWN";
    }
}
