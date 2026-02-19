package com.sourabh.user_service.util;

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
