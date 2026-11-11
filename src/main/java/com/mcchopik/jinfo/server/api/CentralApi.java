package com.mcchopik.jinfo.server.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcchopik.jinfo.server.JInfoServerMod;
import com.mcchopik.jinfo.server.config.ServerConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

public final class CentralApi {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private CentralApi() {
    }

    public static Optional<JsonObject> fetch(String uuid) {
        try {
            String base = ServerConfig.getCentralApiBase();
            if (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
            HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/public/profile/" + uuid))
                    .timeout(TIMEOUT)
                    .header("User-Agent", "jinfo-server/1.3")
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            return Optional.of(JsonParser.parseString(response.body()).getAsJsonObject());
        } catch (Exception e) {
            JInfoServerMod.LOGGER.warn("Central API request failed: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
