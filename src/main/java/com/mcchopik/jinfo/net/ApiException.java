package com.mcchopik.jinfo.net;

/**
 * Ошибка обращения к Worker или к серверу Mojang.
 */
public class ApiException extends Exception {

    private final int statusCode;

    public ApiException(String message) {
        this(0, message);
    }

    public ApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }

    public boolean isRateLimited() {
        return statusCode == 429;
    }

    public boolean isUnauthorized() {
        return statusCode == 401;
    }
}