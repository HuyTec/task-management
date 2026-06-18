package com.taskmanagement.dto;

public record Response<T>(boolean success, T data, String message) {
    public static <T> Response<T> ok(T data, String message) {
        return new Response<>(true, data, message);
    }

    public static <T> Response<T> error(String message) {
        return new Response<>(false, null, message);
    }
}
