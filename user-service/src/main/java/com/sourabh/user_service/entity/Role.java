package com.sourabh.user_service.entity;

/**
 * Defines the access-control roles available within the e-commerce
 * platform.
 * <p>
 * A {@link User} is assigned exactly one role at registration time.
 * The role is embedded in the JWT token and enforced by
 * {@code @PreAuthorize} annotations on controller methods.
 * </p>
 *
 * @see User
 */
public enum Role {

    /** Platform administrator with full management privileges. */
    ADMIN,

    /** Merchant who lists and fulfils products. */
    SELLER,

    /** Consumer who browses, purchases, and reviews products. */
    BUYER
}
