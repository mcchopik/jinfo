package com.mcchopik.jinfo.paper.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mcchopik.jinfo.paper.JInfoPaperPlugin;
import com.mcchopik.jinfo.paper.profile.ProfileStorage;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class HttpServerManager {

    private final JInfoPaperPlugin plugin;
    private final ProfileStorage storage;
    private final Gson gson = new Gson();
    private HttpServer server;

    public HttpServerManager(JInfoPaperPlugin plugin, ProfileStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public synchronized void start(String host, int port) throws IOException {
        stop();
        server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/api/profiles", new ProfilesHandler());
        server.createContext("/api/profile/", new ProfileApiHandler());
        server.createContext("/p/", new ProfilePageHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public int port() {
        return server == null ? -1 : server.getAddress().getPort();
    }

    private void send(HttpExchange exchange, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private void json(HttpExchange exchange, int status, Object value) throws IOException {
        send(exchange, status, "application/json; charset=utf-8", gson.toJson(value));
    }

    private String pathPart(String path, String prefix) {
        String rest = path.substring(prefix.length());
        int slash = rest.indexOf('/');
        return slash < 0 ? rest : rest.substring(0, slash);
    }

    static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String skinData(JsonObject profile) {
        if (profile == null || !profile.has("skin") || profile.get("skin").isJsonNull()) {
            return "";
        }
        try {
            JsonObject skin = gson.fromJson(profile.get("skin").getAsString(), JsonObject.class);
            String url = skin.has("url") ? skin.get("url").getAsString() : "";
            if (!url.startsWith("https://")) {
                return "";
            }
            String model = skin.has("model") ? skin.get("model").getAsString() : "classic";
            return "<img class=\"skin\" src=\"" + escape(url) + "\" alt=\"Skin\">"
                    + "<div class=\"model\">" + escape(model) + "</div>";
        } catch (Exception e) {
            return "";
        }
    }

    private String page(String title, String body) {
        return "<!DOCTYPE html><html lang=\"ru\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>" + escape(title) + "</title><style>"
                + ":root{--bg:#0f1216;--panel:#171b21;--line:#2a313a;--text:#e6e8ec;--muted:#9aa3ae;--accent:#4fc3f7}"
                + "*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif}"
                + "main{max-width:760px;margin:0 auto;padding:24px 16px}h1,h2{margin:0 0 12px}a{color:var(--accent);text-decoration:none}"
                + ".panel{background:var(--panel);border:1px solid var(--line);border-radius:10px;padding:18px;margin:14px 0}"
                + ".row{display:flex;align-items:center;gap:16px}.skin{width:96px;height:96px;object-fit:contain;image-rendering:pixelated;background:#1e242b;border-radius:8px}"
                + ".model{color:var(--muted);font-size:12px;text-align:center;margin-top:4px}.meta{color:var(--muted);font-size:13px;margin:4px 0}"
                + ".data{white-space:pre-wrap;word-break:break-word;line-height:1.6;margin-top:12px}.empty{color:var(--muted)}"
                + "</style></head><body><main>" + body + "</main></body></html>";
    }

    private class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<Map<String, String>> list = new ArrayList<>();
            for (JsonObject profile : storage.all()) {
                Map<String, String> item = new LinkedHashMap<>();
                item.put("uuid", profile.get("uuid").getAsString());
                item.put("name", profile.has("name") ? profile.get("name").getAsString() : "Unknown");
                list.add(item);
            }
            list.sort((a, b) -> a.get("name").compareToIgnoreCase(b.get("name")));
            StringBuilder cards = new StringBuilder();
            if (list.isEmpty()) {
                cards.append("<p class=\"empty\">Профилей пока нет.</p>");
            }
            for (Map<String, String> item : list) {
                String uuid = item.get("uuid");
                cards.append("<div class=\"panel\"><h2>").append(escape(item.get("name"))).append("</h2>")
                        .append("<div class=\"meta\">UUID: ").append(escape(uuid)).append("</div>")
                        .append("<a href=\"/p/").append(escape(uuid)).append("\">Открыть профиль →</a></div>");
            }
            String body = "<h1>JInfo Server</h1><p class=\"meta\">Профили игроков сервера. "
                    + "Открывается без мода, прямо в браузере.</p>" + cards;
            send(exchange, 200, "text/html; charset=utf-8", page("JInfo Server", body));
        }
    }

    private class ProfilesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<Map<String, String>> list = new ArrayList<>();
            for (JsonObject profile : storage.all()) {
                Map<String, String> item = new LinkedHashMap<>();
                item.put("uuid", profile.get("uuid").getAsString());
                item.put("name", profile.has("name") ? profile.get("name").getAsString() : "Unknown");
                item.put("updated", profile.has("updated") ? profile.get("updated").getAsString() : "0");
                list.add(item);
            }
            json(exchange, 200, Map.of("profiles", list));
        }
    }

    private class ProfileApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String uuid = pathPart(exchange.getRequestURI().getPath(), "/api/profile/");
            JsonObject profile = storage.get(uuid);
            if (profile == null) {
                json(exchange, 404, Map.of("error", "Profile not found"));
                return;
            }
            json(exchange, 200, profile);
        }
    }

    private class ProfilePageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String uuid = pathPart(exchange.getRequestURI().getPath(), "/p/");
            JsonObject profile = storage.get(uuid);
            if (profile == null) {
                send(exchange, 404, "text/html; charset=utf-8",
                        page("Not found", "<h1>Профиль не найден</h1><p><a href=\"/\">На главную</a></p>"));
                return;
            }
            String name = profile.has("name") ? profile.get("name").getAsString() : "Unknown";
            String data = profile.has("data") ? profile.get("data").getAsString() : "";
            String skin = skinData(profile);
            StringBuilder body = new StringBuilder();
            body.append("<div class=\"panel\"><div class=\"row\">")
                    .append(skin.isEmpty() ? "" : "<div>" + skin + "</div>")
                    .append("<div><h1>").append(escape(name)).append("</h1>")
                    .append("<div class=\"meta\">UUID: ").append(escape(uuid)).append("</div></div></div>")
                    .append("<div class=\"data\">")
                    .append(data.isBlank() ? "<span class=\"empty\">Профиль пуст.</span>" : escape(data))
                    .append("</div></div><p><a href=\"/\">← Все профили</a></p>");
            send(exchange, 200, "text/html; charset=utf-8", page(name + " — JInfo", body.toString()));
        }
    }
}