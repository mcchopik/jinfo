package com.mcchopik.jinfo.net;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Хранит токен доступа (uuid.expiry.hmac) и UUID на диске.
 * Срок действия берётся из самого токена.
 */
public final class TokenStore {

    private final Path file;
    private String uuid;
    private String token;

    public TokenStore(Path file) {
        this.file = file;
        load();
    }

    private void load() {
        try {
            if (Files.exists(file)) {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                if (obj.has("uuid")) {
                    uuid = obj.get("uuid").getAsString();
                }
                if (obj.has("token")) {
                    token = obj.get("token").getAsString();
                }
            }
        } catch (Exception e) {
            uuid = null;
            token = null;
        }
    }

    public synchronized void save(String uuid, String token) {
        this.uuid = uuid;
        this.token = token;
        try {
            Files.createDirectories(file.getParent());
            JsonObject obj = new JsonObject();
            obj.addProperty("uuid", uuid);
            obj.addProperty("token", token);
            Files.writeString(file, obj.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Не критично: токен просто не переживёт перезапуск.
        }
    }

    public synchronized void clear() {
        uuid = null;
        token = null;
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
    }

    public synchronized String uuid() {
        return uuid;
    }

    public synchronized String token() {
        return token;
    }

    /**
     * Возвращает токен, если он есть и ещё не истёк, иначе null.
     */
    public synchronized String validToken() {
        if (token == null) {
            return null;
        }
        long expiry = expiryOf(token);
        if (expiry > 0 && expiry > System.currentTimeMillis() / 1000L + 5) {
            return token;
        }
        return null;
    }

    public static long expiryOf(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return 0;
            }
            return Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}