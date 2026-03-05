package com.sourabh.user_service.util;

/**
 * General-purpose utility class for safe enum parsing.
 * <p>
 * Provides a single static helper that converts a string value into
 * the corresponding enum constant, throwing a descriptive
 * {@link IllegalArgumentException} when the value is invalid.
 * </p>
 */
public class EnumUtil {

    /**
     * Parses a string into the specified enum type (case-insensitive).
     *
     * @param <T>          the enum type
     * @param enumClass    the {@link Class} of the target enum
     * @param value        the string value to convert (converted to upper-case)
     * @param errorMessage the message used if parsing fails
     * @return the matching enum constant
     * @throws IllegalArgumentException if the value does not match any constant
     */
    public static <T extends Enum<T>> T parseEnum(
            Class<T> enumClass,
            String value,
            String errorMessage) {

        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
