package com.sourabh.order_service.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation used to mark controller or service methods for automatic audit logging.
 *
 * <p>When a method annotated with {@code @Auditable} completes successfully,
 * the {@link AuditAspect} AOP aspect intercepts the invocation and persists an
 * {@link AuditLog} record containing the action performed, the actor, the target
 * resource, and the originating IP address.</p>
 *
 * <p>This annotation is retained at runtime so that Spring AOP can detect it
 * via reflection. It may only be applied to methods.</p>
 *
 * @see AuditAspect
 * @see AuditAction
 * @see AuditLog
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * The audit action being performed (e.g., {@code CREATE}, {@code UPDATE}, {@code DELETE}).
     *
     * @return the {@link AuditAction} enum value representing the operation
     */
    AuditAction action();

    /**
     * A logical name for the type of resource being acted upon (e.g., {@code "ORDER"}, {@code "COUPON"}).
     *
     * @return the resource type identifier
     */
    String resourceType();

    /**
     * An optional SpEL expression used to extract the resource identifier from
     * the method parameters or return value. When left empty, the aspect falls
     * back to using the first {@link String} argument of the annotated method.
     *
     * @return a SpEL expression string, or an empty string if not specified
     */
    String resourceId() default "";
}
