package com.mcchopik.jinfo.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcchopik.jinfo.net.ApiClient;
import com.mcchopik.jinfo.net.ApiException;
import com.mcchopik.jinfo.net.ProfileData;
import com.mcchopik.jinfo.text.HtmlRenderer;
import com.mcchopik.jinfo.text.Lang;
import com.mcchopik.jinfo.text.ParsedProfile;
import com.mcchopik.jinfo.text.TextParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class WebServer {

    private static final long IDLE_TIMEOUT_MS = 2L * 60L * 1000L;
    private static final int MAX_BODY = 20_000;

    private static WebServer instance;

    private final HttpServer server;
    private final int port;
    private final String token;
    private final String basePath;
    private final ApiClient api;
    private final String username;
    private final TextParser parser = new TextParser();
    private final AtomicLong rev = new AtomicLong();

    private final byte[] html;
    private final byte[] css;
    private final byte[] js;

    private volatile long lastRequest = System.currentTimeMillis();
    private volatile boolean closed;

    private ScheduledExecutorService watchdog;
    private ExecutorService executor;

    private WebServer(HttpServer server, int port, ApiClient api, String username) throws IOException {
        this.server = server;
        this.port = port;
        this.api = api;
        this.username = username;
        this.token = randomToken();
        this.basePath = "/e/" + token;
        this.html = loadResource("/assets/jinfo/web/editor.html");
        this.css = loadResource("/assets/jinfo/web/app.css");
        this.js = loadResource("/assets/jinfo/web/app.js");
    }

    public static synchronized String start(ApiClient api, String username) throws IOException {
        if (instance != null && !instance.closed) {
            return instance.url();
        }
        HttpServer server = bind();
        int port = server.getAddress().getPort();
        WebServer ws = new WebServer(server, port, api, username);
        ws.register();
        ws.startWatchdog();
        server.start();
        instance = ws;
        return ws.url();
    }

    public static synchronized void stop(String reason) {
        if (instance != null) {
            instance.shutdown();
            instance = null;
        }
    }

    public static synchronized boolean isRunning() {
        return instance != null && !instance.closed;
    }

    public String url() {
        return "http://127.0.0.1:" + port + basePath + "/";
    }

    private static HttpServer bind() throws IOException {
        IOException last = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                return HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            } catch (IOException e) {
                last = e;
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw last != null ? last : new IOException("Cannot bind local server");
    }

    private void register() {
        executor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("jinfo-web");
            return t;
        });
        server.setExecutor(executor);
        server.createContext("/", this::handle);
    }

    private void startWatchdog() {
        watchdog = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("jinfo-web-watchdog");
            return t;
        });
        watchdog.scheduleAtFixedRate(() -> {
            if (System.currentTimeMillis() - lastRequest > IDLE_TIMEOUT_MS) {
                shutdown();
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    private void shutdown() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            server.stop(0);
        } catch (Exception ignored) {
        }
        if (watchdog != null) {
            watchdog.shutdownNow();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private void handle(HttpExchange ex) throws IOException {
        lastRequest = System.currentTimeMillis();
        try {
            String host = ex.getRequestHeaders().getFirst("Host");
            if (!validHost(host)) {
                send(ex, 404, "text/plain", new byte[0]);
                return;
            }
            String path = ex.getRequestURI().getPath();
            if (!path.startsWith(basePath + "/")) {
                send(ex, 404, "text/plain", new byte[0]);
                return;
            }
            if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                String origin = ex.getRequestHeaders().getFirst("Origin");
                if (origin != null && !validOrigin(origin)) {
                    send(ex, 403, "text/plain", new byte[0]);
                    return;
                }
            }
            String sub = path.substring(basePath.length());
            switch (sub) {
                case "/", "" -> serve(ex, "text/html", html);
                case "/app.css" -> serve(ex, "text/css", css);
                case "/app.js" -> serve(ex, "text/javascript", js);
                case "/ping" -> json(ex, new JsonObject());
                case "/profile" -> handleProfile(ex);
                case "/preview" -> handlePreview(ex);
                case "/save" -> handleSave(ex);
                case "/close" -> handleClose(ex);
                default -> send(ex, 404, "text/plain", new byte[0]);
            }
        } catch (Exception e) {
            try {
                JsonObject err = new JsonObject();
                err.addProperty("error", "internal");
                json(ex, err);
            } catch (Exception ignored) {
            }
        } finally {
            ex.close();
        }
    }

    private void handleProfile(HttpExchange ex) throws IOException {
        JsonObject out = new JsonObject();
        try {
            if (!api.isLoggedIn()) {
                if (username == null) {
                    out.addProperty("empty", true);
                    json(ex, out);
                    return;
                }
                api.login(username);
            }
            String uuid = api.currentUuid();
            if (uuid == null) {
                out.addProperty("empty", true);
                json(ex, out);
                return;
            }
            ProfileData data = api.getProfile(uuid);
            out.addProperty("empty", false);
            out.addProperty("text", data.data());
            out.addProperty("verified", data.verified());
            out.addProperty("rev", rev.get());
        } catch (ApiException e) {
            out.addProperty("empty", true);
            out.addProperty("error", e.statusCode() == 404 ? "" : e.getMessage());
        }
        json(ex, out);
    }

    private void handlePreview(HttpExchange ex) throws IOException {
        JsonObject body = readJson(ex);
        if (body == null) {
            send(ex, 400, "text/plain", new byte[0]);
            return;
        }
        String text = getString(body, "text");
        Lang lang = Lang.fromCode(getString(body, "lang"));
        ParsedProfile parsed = parser.parse(text, lang);

        JsonObject out = new JsonObject();
        out.addProperty("tooltip", HtmlRenderer.render(parsed, HtmlRenderer.Mode.TOOLTIP));
        out.addProperty("full", HtmlRenderer.render(parsed, HtmlRenderer.Mode.FULL));
        out.addProperty("overLimits", parsed.overLimits());
        out.addProperty("chars", parsed.charCount());
        out.addProperty("lines", parsed.lineCount());
        out.addProperty("links", parsed.linkCount());
        JsonArray warnings = new JsonArray();
        for (String w : parsed.warnings()) {
            warnings.add(w);
        }
        out.add("warnings", warnings);
        json(ex, out);
    }

    private void handleSave(HttpExchange ex) throws IOException {
        JsonObject body = readJson(ex);
        if (body == null) {
            send(ex, 400, "text/plain", new byte[0]);
            return;
        }
        String text = getString(body, "text");
        long clientRev = body.has("rev") ? body.get("rev").getAsLong() : -1L;

        ParsedProfile parsed = parser.parse(text, Lang.RU);
        JsonObject out = new JsonObject();
        if (parsed.overLimits()) {
            out.addProperty("ok", false);
            out.addProperty("error", "limits");
            out.addProperty("rateLimited", false);
            json(ex, out);
            return;
        }
        if (username == null) {
            out.addProperty("ok", false);
            out.addProperty("error", "no_user");
            json(ex, out);
            return;
        }
        try {
            if (!api.isLoggedIn()) {
                api.login(username);
            }
            boolean stale = clientRev >= 0 && clientRev != rev.get();
            api.putProfile(text);
            long r = rev.incrementAndGet();
            out.addProperty("ok", true);
            out.addProperty("rev", r);
            out.addProperty("stale", stale);
        } catch (ApiException e) {
            out.addProperty("ok", false);
            out.addProperty("rateLimited", e.isRateLimited());
            out.addProperty("error", e.statusCode() == 429
                    ? "Слишком часто. Подожди немного."
                    : e.getMessage());
        } catch (Exception e) {
            out.addProperty("ok", false);
            out.addProperty("error", e.getMessage());
        }
        json(ex, out);
    }


    private void handleClose(HttpExchange ex) throws IOException {
        JsonObject out = new JsonObject();
        out.addProperty("ok", true);
        json(ex, out);
        CompletableFuture.runAsync(() -> stop("browser"));
    }

    private boolean validHost(String host) {
        if (host == null) {
            return false;
        }
        String h = host.toLowerCase();
        return h.equals("127.0.0.1:" + port) || h.equals("localhost:" + port);
    }

    private boolean validOrigin(String origin) {
        String o = origin.toLowerCase();
        return o.equals("http://127.0.0.1:" + port) || o.equals("http://localhost:" + port);
    }

    private void serve(HttpExchange ex, String contentType, byte[] body) throws IOException {
        if (body == null) {
            send(ex, 500, "text/plain", "resource missing".getBytes(StandardCharsets.UTF_8));
            return;
        }
        send(ex, 200, contentType, body);
    }

    private void json(HttpExchange ex, JsonObject obj) throws IOException {
        send(ex, 200, "application/json", obj.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void send(HttpExchange ex, int status, String contentType, byte[] body) throws IOException {
        Headers h = ex.getResponseHeaders();
        h.set("Content-Security-Policy",
                "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data: https:; "
                + "connect-src 'self'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'");
        h.set("X-Content-Type-Options", "nosniff");
        h.set("Cache-Control", "no-store");
        h.set("Referrer-Policy", "no-referrer");
        h.set("Content-Type", contentType + "; charset=utf-8");
        ex.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        if (body.length > 0) {
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        }
    }

    private JsonObject readJson(HttpExchange ex) {
        try (InputStream in = ex.getRequestBody()) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[1024];
            int n;
            int total = 0;
            while ((n = in.read(chunk)) > 0) {
                total += n;
                if (total > MAX_BODY) {
                    return null;
                }
                buf.write(chunk, 0, n);
            }
            return JsonParser.parseString(buf.toString(StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    private static String getString(JsonObject obj, String key) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsString() : "";
    }

    private static String randomToken() {
        byte[] buf = new byte[24];
        new SecureRandom().nextBytes(buf);
        StringBuilder sb = new StringBuilder(48);
        for (byte b : buf) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    private static byte[] loadResource(String path) {
        try (InputStream in = WebServer.class.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            return in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }
}
