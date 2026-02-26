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
 * AOP Aspect to automatically log auditable actions.
 * Intercepts methods annotated with @Auditable and records them.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

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
                    .build();

            auditLogRepository.save(auditLog);
            log.info("Audit: {} {} {} by {}", auditable.action(), auditable.resourceType(), resourceId, actorUuid);
        } catch (Exception e) {
            log.error("Failed to record audit log: {}", e.getMessage());
        }
    }

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

    private String extractResourceId(Object[] args, Auditable auditable) {
        if (args.length > 0 && args[0] instanceof String) {
            return (String) args[0];
        }
        return "UNKNOWN";
    }
}
