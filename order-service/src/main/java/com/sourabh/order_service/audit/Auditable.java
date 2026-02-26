package com.sourabh.order_service.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark controller/service methods to be audited.
 * Aspect will automatically log the action.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    AuditAction action();
    String resourceType();
    /** SpEL expression to extract resource ID from method parameters or return value */
    String resourceId() default "";
}
