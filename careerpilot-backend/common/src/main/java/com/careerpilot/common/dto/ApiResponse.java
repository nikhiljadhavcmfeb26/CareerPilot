package com.careerpilot.common.dto;

/**
 * Mirrors CareerPilot.Shared.Models.ApiResponse<T> from the original .NET API
 * so every service returns the exact same response envelope the React
 * client already knows how to parse ({ success, message, data }).
 */
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    public ApiResponse() {
    }

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, message, data);
    }

    /**
     * Success envelope for endpoints that have nothing to return.
     *
     * Command endpoints used to answer with {@code ok(new Object(), message)}.
     * Jackson cannot serialise a bare {@link Object} - it has no properties -
     * so with the default FAIL_ON_EMPTY_BEANS setting the response writer blew
     * up *after* the transaction had already committed. The work succeeded, the
     * client got a 500 rendered by GlobalExceptionHandler, and the UI showed
     * "An unexpected error occurred." Returning a null payload keeps the
     * { success, message, data } contract the client parses while removing the
     * unserialisable value entirely.
     */
    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, null);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
