package com.mcchopik.jinfo.net;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TokenStore {

    private final Path file;
    private String uuid;
    private String token;
    private boolean account;
    private String accountName;

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
                if (obj.has("account")) {
                    account = obj.get("account").getAsBoolean();
                }
                if (obj.has("accountName")) {
                    accountName = obj.get("accountName").getAsString();
                }
            }
        } catch (Exception e) {
            uuid = null;
            token = null;
            account = false;
            accountName = null;
        }
    }

    public synchronized void save(String uuid, String token) {
        save(uuid, token, false, null);
    }

    public synchronized void save(String uuid, String token, boolean account, String accountName) {
        this.uuid = uuid;
        this.token = token;
        this.account = account;
        this.accountName = accountName;
        try {
            Files.createDirectories(file.getParent());
            JsonObject obj = new JsonObject();
            obj.addProperty("uuid", uuid);
            obj.addProperty("token", token);
            obj.addProperty("account", account);
            if (accountName != null) {
                obj.addProperty("accountName", accountName);
            }
            Files.writeString(file, obj.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {

        }
    }

    public synchronized void clear() {
        uuid = null;
        token = null;
        account = false;
        accountName = null;
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

    public synchronized boolean isAccount() {
        return account;
    }

    public synchronized String accountName() {
        return accountName;
    }

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
