package com.mcchopik.jinfo.net;

/**
 * Результат авторизации: UUID и токен вида {@code uuid.expiry.hmac}.
 */
public record AuthResult(String uuid, String token) {
}