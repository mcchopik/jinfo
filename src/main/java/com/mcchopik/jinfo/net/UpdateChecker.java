package com.mcchopik.jinfo.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;

public final class UpdateChecker {

    private static final String LATEST_RELEASE =
            "https://api.github.com/repos/mcchopik/jinfo/releases/latest";
    private static final Pattern NUMBERS = Pattern.compile("\\d+(?:\\.\\d+)*");

    private UpdateChecker() {
    }

    public record UpdateInfo(String version, String file, String sha256, String pageUrl) {
    }

    public static Optional<UpdateInfo> check() {
        try {
            HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpResponse<String> res = http.send(get(LATEST_RELEASE, true),
                    HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                return Optional.empty();
            }

            JsonObject release = JsonParser.parseString(res.body()).getAsJsonObject();
            String tag = stringOrNull(release, "tag_name");
            String pageUrl = stringOrNull(release, "html_url");
            String version = tag == null ? null : tag.replaceFirst("^[vV]", "");
            if (version == null || version.isEmpty()) {
                return Optional.empty();
            }

            String file = null;
            String sha = null;
            String updateUrl = null;
            if (release.has("assets") && release.get("assets").isJsonArray()) {
                JsonArray assets = release.getAsJsonArray("assets");
                for (JsonElement el : assets) {
                    if (!el.isJsonObject()) {
                        continue;
                    }
                    JsonObject asset = el.getAsJsonObject();
                    if ("update.json".equals(stringOrNull(asset, "name"))) {
                        updateUrl = stringOrNull(asset, "browser_download_url");
                        break;
                    }
                }
            }

            if (updateUrl != null) {
                HttpResponse<String> u = http.send(get(updateUrl, false),
                        HttpResponse.BodyHandlers.ofString());
                if (u.statusCode() == 200) {
                    JsonObject json = JsonParser.parseString(u.body()).getAsJsonObject();
                    version = valueOrDefault(stringOrNull(json, "version"), version);
                    file = stringOrNull(json, "file");
                    sha = stringOrNull(json, "sha256");
                }
            }

            return Optional.of(new UpdateInfo(version, file, sha, pageUrl));
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    public static String currentVersion() {
        try {
            return FabricLoader.getInstance().getModContainer("jinfo")
                    .map(c -> c.getMetadata().getVersion().getFriendlyString())
                    .orElse("0.0.0");
        } catch (Throwable t) {
            return "0.0.0";
        }
    }

    public static boolean isNewer(String remote, String current) {
        int[] r = numbers(remote);
        int[] c = numbers(current);
        if (r.length == 0) {
            return false;
        }
        int n = Math.max(r.length, c.length);
        for (int i = 0; i < n; i++) {
            int a = i < r.length ? r[i] : 0;
            int b = i < c.length ? c[i] : 0;
            if (a != b) {
                return a > b;
            }
        }
        return false;
    }

    private static int[] numbers(String version) {
        if (version == null) {
            return new int[0];
        }
        Matcher m = NUMBERS.matcher(version);
        if (!m.find()) {
            return new int[0];
        }
        String[] parts = m.group().split("\\.");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                out[i] = 0;
            }
        }
        return out;
    }

    private static HttpRequest get(String url, boolean api) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("User-Agent", "jinfo-mod")
                .GET();
        if (api) {
            b.header("Accept", "application/vnd.github+json");
        }
        return b.build();
    }

    private static String stringOrNull(JsonObject obj, String key) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsString() : null;
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }
}
