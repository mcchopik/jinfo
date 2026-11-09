package com.mcchopik.jinfo.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public final class ApiClient {

    public static final String DEFAULT_BASE_URL = "https://mcprofiles.mcchopik.workers.dev";
    private static final String MOJANG_JOIN_URL =
            "https://sessionserver.mojang.com/session/minecraft/join";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    public record ProfileRef(long updated, boolean verified) {
    }

    private final String baseUrl;
    private final HttpClient http;
    private final TokenStore tokenStore;
    private final OfflineKeyStore keyStore;
    private final ProfileCache cache = new ProfileCache();
    private final SecureRandom random = new SecureRandom();

    public ApiClient(Path configDir) {
        this(DEFAULT_BASE_URL, configDir);
    }

    public ApiClient(String baseUrl, Path configDir) {
        String b = baseUrl.trim();
        this.baseUrl = b.endsWith("/") ? b.substring(0, b.length() - 1) : b;
        this.http = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.tokenStore = new TokenStore(configDir.resolve("token.json"));
        this.keyStore = new OfflineKeyStore(configDir.resolve("keys.json"));
        this.keyStore.loadOrCreate();
    }

    public boolean isLoggedIn() {
        return tokenStore.validToken() != null;
    }

    public String currentUuid() {
        return tokenStore.uuid();
    }

    public ProfileCache cache() {
        return cache;
    }

    public AuthResult login(String username) throws ApiException {
        Optional<MinecraftSession.SessionInfo> session = MinecraftSession.current();
        if (session.isPresent()) {
            try {
                return loginOnline(session.get(), username);
            } catch (ApiException e) {
                if (e.isRateLimited()) {
                    throw e;
                }

            }
        }
        return loginOffline(username);
    }

    public AuthResult loginOnline(MinecraftSession.SessionInfo session, String username)
            throws ApiException {
        String serverId = "jinfo-" + randomHex(16);
        joinMojang(session.accessToken(), session.uuid(), serverId);
        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("serverId", serverId);
        JsonObject resp = requestJson("POST", baseUrl + "/auth", body, null);
        return saveAuth(resp);
    }

    public AuthResult loginOffline(String username) throws ApiException {
        long timestamp = System.currentTimeMillis();
        String message = username + ":" + timestamp;
        String signature = keyStore.signBase64(message);
        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("publicKey", keyStore.publicKeyBase64());
        body.addProperty("timestamp", timestamp);
        body.addProperty("signature", signature);
        JsonObject resp = requestJson("POST", baseUrl + "/auth/offline", body, null);
        return saveAuth(resp);
    }

    private AuthResult saveAuth(JsonObject resp) throws ApiException {
        String uuid = stringOrNull(resp, "uuid");
        String token = stringOrNull(resp, "token");
        if (uuid == null || token == null) {
            throw new ApiException("Malformed auth response");
        }
        tokenStore.save(uuid, token);
        return new AuthResult(uuid, token);
    }

    private void joinMojang(String accessToken, String uuidNoDashes, String serverId)
            throws ApiException {
        JsonObject body = new JsonObject();
        body.addProperty("accessToken", accessToken);
        body.addProperty("selectedProfile", uuidNoDashes);
        body.addProperty("serverId", serverId);
        HttpResult r = send(jsonRequest(MOJANG_JOIN_URL, body, null));
        if (r.status() != 204) {
            throw new ApiException(r.status(), "Mojang join failed: " + r.status());
        }
    }

    public void putProfile(String data) throws ApiException {
        String token = requireToken();
        JsonObject body = new JsonObject();
        body.addProperty("data", data);
        String skin = com.mcchopik.jinfo.config.JInfoConfig.skinPayload();
        if (skin != null) {
            body.addProperty("skin", skin);
        }
        requestJson("PUT", baseUrl + "/profile", body, token);
        String uuid = tokenStore.uuid();
        if (uuid != null) {
            cache.remove(uuid);
        }
    }

    public ProfileData getProfile(String uuid) throws ApiException {
        String token = requireToken();
        JsonObject resp = requestJson("GET", baseUrl + "/profile/" + uuid, null, token);
        ProfileData data = new ProfileData(
                uuid,
                stringOrNull(resp, "data"),
                longOrZero(resp, "updated"),
                boolOrFalse(resp, "verified"),
                stringOrNull(resp, "skin"));
        cache.put(data);
        return data;
    }

    public Optional<ProfileData> getCached(String uuid) {
        return cache.get(uuid);
    }

    public Map<String, ProfileRef> batchProfiles(List<String> uuids) throws ApiException {
        String token = requireToken();
        if (uuids == null || uuids.isEmpty()) {
            return Map.of();
        }
        JsonArray arr = new JsonArray();
        int limit = Math.min(uuids.size(), 50);
        for (int i = 0; i < limit; i++) {
            arr.add(uuids.get(i));
        }
        JsonObject body = new JsonObject();
        body.add("uuids", arr);
        JsonObject resp = requestJson("POST", baseUrl + "/profiles/batch", body, token);
        Map<String, ProfileRef> out = new TreeMap<>();
        if (resp.has("profiles") && resp.get("profiles").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : resp.getAsJsonObject("profiles").entrySet()) {
                JsonObject p = e.getValue().getAsJsonObject();
                out.put(e.getKey(), new ProfileRef(longOrZero(p, "updated"), boolOrFalse(p, "verified")));
            }
        }
        return out;
    }

    public void deleteProfile() throws ApiException {
        String token = requireToken();
        requestJson("DELETE", baseUrl + "/profile", null, token);
        String uuid = tokenStore.uuid();
        if (uuid != null) {
            cache.remove(uuid);
        }
    }

    public void report(String profileUuid) throws ApiException {
        String token = requireToken();
        JsonObject body = new JsonObject();
        body.addProperty("profileUuid", profileUuid);
        requestJson("POST", baseUrl + "/report", body, token);
    }

    public TokenStore tokenStore() {
        return tokenStore;
    }

    public OfflineKeyStore keyStore() {
        return keyStore;
    }

    private record HttpResult(int status, String body) {
    }

    private String requireToken() throws ApiException {
        String token = tokenStore.validToken();
        if (token == null) {
            throw new ApiException(401, "Not logged in");
        }
        return token;
    }

    private JsonObject requestJson(String method, String url, JsonObject body, String bearer)
            throws ApiException {
        HttpResult r;
        try {
            r = send(jsonRequest(method, url, body, bearer));
        } catch (ApiException e) {
            throw e;
        }
        if (r.status() < 200 || r.status() >= 300) {
            throw new ApiException(r.status(), extractError(r.body(), r.status()));
        }
        if (r.body() == null || r.body().isBlank()) {
            return new JsonObject();
        }
        try {
            return JsonParser.parseString(r.body()).getAsJsonObject();
        } catch (Exception e) {
            throw new ApiException(r.status(), "Invalid JSON response");
        }
    }

    private HttpRequest jsonRequest(String url, JsonObject body, String bearer) {
        return jsonRequest("POST", url, body, bearer);
    }

    private HttpRequest jsonRequest(String method, String url, JsonObject body, String bearer) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json");
        if (bearer != null) {
            b.header("Authorization", "Bearer " + bearer);
        }
        String payload = body == null ? "" : body.toString();
        switch (method) {
            case "GET" -> b.GET();
            case "DELETE" -> b.DELETE();
            case "PUT" -> b.PUT(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
            default -> b.POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
        }
        return b.build();
    }

    private HttpResult send(HttpRequest request) throws ApiException {
        try {
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
            return new HttpResult(resp.statusCode(), resp.body());
        } catch (IOException e) {
            throw new ApiException("Network error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("Request interrupted");
        }
    }

    private static String extractError(String body, int status) {
        if (body != null && !body.isBlank()) {
            try {
                JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
                if (obj.has("error")) {
                    return obj.get("error").getAsString();
                }
            } catch (Exception ignored) {
            }
        }
        return "HTTP " + status;
    }

    private static String stringOrNull(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }

    private static long longOrZero(JsonObject obj, String key) {
        try {
            return obj.has(key) ? obj.get(key).getAsLong() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private static boolean boolOrFalse(JsonObject obj, String key) {
        try {
            return obj.has(key) && obj.get(key).getAsBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    private String randomHex(int bytes) {
        byte[] buf = new byte[bytes];
        random.nextBytes(buf);
        StringBuilder sb = new StringBuilder(bytes * 2);
        for (byte x : buf) {
            sb.append(Character.forDigit((x >> 4) & 0xF, 16));
            sb.append(Character.forDigit(x & 0xF, 16));
        }
        return sb.toString();
    }

    public List<String> knownUuids() {
        List<String> list = new ArrayList<>();
        String uuid = tokenStore.uuid();
        if (uuid != null) {
            list.add(uuid);
        }
        return list;
    }
}
