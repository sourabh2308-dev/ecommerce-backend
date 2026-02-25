package com.sourabh.user_service.util;

/**
 * UTILITY CLASS - Helper Functions
 * 
 * Provides reusable helper methods for:
 *   - Token generation and validation (JWT utils)
 *   - Date/time conversions
 *   - Data transformations
 *   - Common business calculations
 */
public class EnumUtil {

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
