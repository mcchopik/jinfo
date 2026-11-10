package com.mcchopik.jinfo.server.http;

import com.mcchopik.jinfo.server.JInfoServerMod;
import com.mcchopik.jinfo.server.config.ServerConfig;
import com.mcchopik.jinfo.server.profile.ProfileStorage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class HttpServerManager {

    private final ProfileStorage profileStorage;
    private HttpServer server;
    private final Gson gson = new Gson();
    private int port;

    public HttpServerManager(int port, ProfileStorage profileStorage) {
        this.port = port;
        this.profileStorage = profileStorage;
        start();
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(ServerConfig.getHost(), port), 0);
            server.createContext("/", new RootHandler());
            server.createContext("/api/profiles", new ProfilesApiHandler());
            server.createContext("/api/profile/", new ProfileApiHandler());
            server.createContext("/p/", new ProfileWebHandler());
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            JInfoServerMod.LOGGER.info("JInfo HTTP server started on {}:{}", ServerConfig.getHost(), port);
        } catch (IOException e) {
            JInfoServerMod.LOGGER.error("Failed to start HTTP server: {}", e.getMessage());
        }
    }

    public void reload(int newPort) {
        if (server != null) {
            server.stop(0);
        }
        this.port = newPort;
        start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private void sendJson(HttpExchange exchange, int status, Object obj) throws IOException {
        String json = gson.toJson(obj);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(status, json.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void sendHtml(HttpExchange exchange, String html) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, html.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(html.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void sendNotFound(HttpExchange exchange) throws IOException {
        sendJson(exchange, 404, Map.of("error", "Not found"));
    }

    private String extractUuid(String path) {
        return path.substring("/p/".length()).split("/")[0];
    }

    class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <title>JInfo Server</title>
                    <style>
                        body { font-family: system-ui; max-width: 800px; margin: 2rem auto; padding: 0 1rem; }
                        .profile { border: 1px solid #ddd; border-radius: 8px; padding: 1rem; margin: 1rem 0; }
                        .profile h3 { margin: 0 0 0.5rem; }
                        .profile .meta { color: #666; font-size: 0.9rem; }
                        a { color: #2563eb; }
                        code { background: #f3f4f6; padding: 0.1rem 0.3rem; border-radius: 4px; }
                    </style>
                </head>
                <body>
                    <h1>JInfo Server</h1>
                    <p>Player profiles HTTP API and web viewer.</p>
                    <h2>Profiles</h2>
                    <div id="profiles">Loading...</div>
                    <script>
                        fetch('/api/profiles')
                            .then(r => r.json())
                            .then(data => {
                                const container = document.getElementById('profiles');
                                if (!data.profiles || data.profiles.length === 0) {
                                    container.innerHTML = '<p>No profiles yet.</p>';
                                    return;
                                }
                                container.innerHTML = data.profiles.map(p => `
                                    <div class="profile">
                                        <h3>${p.name || 'Unknown'}</h3>
                                        <div class="meta">UUID: <code>${p.uuid}</code></div>
                                        <div class="meta"><a href="/p/${p.uuid}">View profile</a></div>
                                    </div>
                                `).join('');
                            })
                            .catch(e => {
                                document.getElementById('profiles').innerHTML = '<p style="color:red">Error loading profiles</p>';
                            });
                    </script>
                </body>
                </html>
                """;
            sendHtml(exchange, html);
        }
    }

    class ProfilesApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<Map<String, String>> list = profileStorage.getAllProfiles().stream()
                .map(p -> Map.of(
                    "uuid", p.get("uuid").getAsString(),
                    "name", p.has("name") ? p.get("name").getAsString() : "Unknown"
                ))
                .toList();
            sendJson(exchange, 200, Map.of("profiles", list));
        }
    }

    class ProfileApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String uuid = exchange.getRequestURI().getPath().substring("/api/profile/".length());
            JsonObject profile = profileStorage.getProfile(uuid);
            if (profile == null) {
                sendNotFound(exchange);
                return;
            }
            sendJson(exchange, 200, profile);
        }
    }

    class ProfileWebHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String uuid = extractUuid(exchange.getRequestURI().getPath());
            JsonObject profile = profileStorage.getProfile(uuid);
            if (profile == null) {
                sendNotFound(exchange);
                return;
            }
            String name = profile.has("name") ? profile.get("name").getAsString() : "Unknown";
            String data = profile.has("data") ? profile.get("data").getAsString() : "";
            String skin = profile.has("skin") ? profile.get("skin").getAsString() : "";
            String skinUrl = "";
            String skinModel = "classic";
            if (!skin.isEmpty()) {
                try {
                    JsonObject skinObj = gson.fromJson(skin, JsonObject.class);
                    skinUrl = skinObj.has("url") ? skinObj.get("url").getAsString() : "";
                    skinModel = skinObj.has("model") && "slim".equals(skinObj.get("model").getAsString()) ? "slim" : "classic";
                } catch (Exception ignored) {}
            }
            String html = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <title>%s - JInfo Profile</title>
                    <style>
                        body { font-family: system-ui; max-width: 600px; margin: 2rem auto; padding: 0 1rem; background: #0f1216; color: #e6e8ec; }
                        .panel { background: #171b21; border: 1px solid #2a313a; border-radius: 8px; padding: 1.5rem; }
                        .skin { width: 128px; height: 128px; image-rendering: pixelated; border-radius: 8px; }
                        .skin-container { text-align: center; margin-bottom: 1rem; }
                        h1 { margin: 0 0 0.5rem; }
                        .meta { color: #9aa3ae; font-size: 0.9rem; margin-bottom: 1rem; }
                        .data { white-space: pre-wrap; word-break: break-word; line-height: 1.6; }
                        a { color: #4fc3f7; }
                    </style>
                </head>
                <body>
                    <div class="panel">
                        <div class="skin-container">
                            %s
                        </div>
                        <h1>%s</h1>
                        <div class="meta">UUID: <code>%s</code></div>
                        <div class="data">%s</div>
                    </div>
                </body>
                </html>
                """,
                name,
                skinUrl.isEmpty() ? "<div class='skin' style='background:#2f74c9;display:flex;align-items:center;justify-content:center;'>?</div>" :
                    "<img class='skin' src='" + skinUrl + "' alt='Skin'>",
                name,
                uuid,
                data.isEmpty() ? "No profile data" : data.replace("&", "&").replace("<", "<").replace(">", ">")
            );
            sendHtml(exchange, html);
        }
    }
}