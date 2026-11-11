package com.mcchopik.jinfo.paper.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

    public static Optional<JsonObject> fetch(String base, String uuid) {
        try {
            String url = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
            HttpRequest request = HttpRequest.newBuilder(URI.create(url + "/public/profile/" + uuid))
                    .timeout(TIMEOUT)
                    .header("User-Agent", "jinfo-paper/1.3")
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            return Optional.of(JsonParser.parseString(response.body()).getAsJsonObject());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
