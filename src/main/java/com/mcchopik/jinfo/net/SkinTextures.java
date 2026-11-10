package com.mcchopik.jinfo.net;

import com.mcchopik.jinfo.JInfoMeta;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

public final class SkinTextures {

    private static final int MAX_ENTRIES = 64;
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private record Loaded(PlayerSkin skin, Identifier id, long createdAt) {
    }

    private static final Map<String, Loaded> READY = new ConcurrentHashMap<>();
    private static final Map<String, List<Consumer<PlayerSkin>>> WAITERS = new ConcurrentHashMap<>();
    private static final Set<String> LOADING = ConcurrentHashMap.newKeySet();
    private static final AtomicLong GENERATION = new AtomicLong();

    private SkinTextures() {
    }

    public static PlayerSkin get(String key) {
        Loaded loaded = key == null ? null : READY.get(key);
        return loaded == null ? null : loaded.skin();
    }

    public static boolean isLoading(String key) {
        return key != null && LOADING.contains(key);
    }

    public static void loadAsync(String key, String url, String file, boolean slim) {
        loadAsync(key, url, file, slim, null);
    }

    public static void loadAsync(String key, String url, String file, boolean slim,
                                 Consumer<PlayerSkin> callback) {
        if (key == null || key.isBlank()) {
            return;
        }
        Loaded ready = READY.get(key);
        if (ready != null) {
            notifyLater(callback, ready.skin());
            return;
        }
        if (callback != null) {
            WAITERS.computeIfAbsent(key, ignored -> new ArrayList<>()).add(callback);
        }
        if (!LOADING.add(key)) {
            return;
        }
        long generation = GENERATION.get();
        if (url != null && !url.isBlank()) {
            if (!url.toLowerCase(Locale.ROOT).startsWith("https://")) {
                fail(key, generation);
                return;
            }
            downloadFromUrl(key, url, slim, generation);
        } else if (file != null && !file.isBlank()) {
            loadFromFile(key, file, slim, generation);
        } else {
            fail(key, generation);
        }
    }

    private static void notifyLater(Consumer<PlayerSkin> callback, PlayerSkin skin) {
        if (callback == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            callback.accept(skin);
        } else {
            client.execute(() -> callback.accept(skin));
        }
    }

    private static void downloadFromUrl(String key, String url, boolean slim, long generation) {
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(REQUEST_TIMEOUT)
                        .header("User-Agent", JInfoMeta.USER_AGENT)
                        .header("Accept", "image/*")
                        .GET()
                        .build();
                HttpResponse<byte[]> response = HTTP.send(request,
                        HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() != 200) {
                    fail(key, generation);
                    return;
                }
                byte[] bytes = response.body();
                if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES) {
                    fail(key, generation);
                    return;
                }
                String type = response.headers().firstValue("Content-Type").orElse("")
                        .toLowerCase(Locale.ROOT);
                if (!type.isEmpty() && !type.startsWith("image/")) {
                    fail(key, generation);
                    return;
                }
                registerOnClient(key, bytes, slim, generation);
            } catch (Throwable ignored) {
                fail(key, generation);
            }
        });
    }

    private static void loadFromFile(String key, String filePath, boolean slim, long generation) {
        CompletableFuture.runAsync(() -> {
            try {
                Path path = Path.of(filePath);
                if (!Files.exists(path) || !Files.isRegularFile(path)) {
                    fail(key, generation);
                    return;
                }
                long size = Files.size(path);
                if (size == 0 || size > MAX_BYTES) {
                    fail(key, generation);
                    return;
                }
                byte[] bytes = Files.readAllBytes(path);
                registerOnClient(key, bytes, slim, generation);
            } catch (Throwable ignored) {
                fail(key, generation);
            }
        });
    }

    private static void registerOnClient(String key, byte[] bytes, boolean slim, long generation) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            fail(key, generation);
            return;
        }
        client.execute(() -> register(key, bytes, slim, generation));
    }

    private static void register(String key, byte[] bytes, boolean slim, long generation) {
        if (generation != GENERATION.get()) {
            return;
        }
        NativeImage image = null;
        try {
            image = NativeImage.read(bytes);
            int width = image.getWidth();
            int height = image.getHeight();
            if (!isValidSize(width, height)) {
                image.close();
                fail(key, generation);
                return;
            }
            Identifier id = Identifier.fromNamespaceAndPath("jinfo",
                    "skin/" + Integer.toHexString(key.hashCode()) + "_" + width + "x" + height);
            DynamicTexture texture = new DynamicTexture(() -> "jinfo skin", image);
            TextureManager textures = Minecraft.getInstance().getTextureManager();
            textures.register(id, texture);
            ClientAsset.Texture asset = new ClientAsset.Texture() {
                @Override
                public Identifier texturePath() {
                    return id;
                }

                @Override
                public Identifier id() {
                    return id;
                }
            };
            PlayerModelType model = slim ? PlayerModelType.SLIM : PlayerModelType.WIDE;
            PlayerSkin skin = PlayerSkin.insecure(asset, null, null, model);
            complete(key, new Loaded(skin, id, System.currentTimeMillis()));
        } catch (Throwable ignored) {
            if (image != null) {
                image.close();
            }
            fail(key, generation);
        }
    }

    private static boolean isValidSize(int width, int height) {
        return (width == 64 && (height == 64 || height == 32))
                || (width == 128 && (height == 128 || height == 64));
    }

    private static void complete(String key, Loaded loaded) {
        LOADING.remove(key);
        READY.put(key, loaded);
        trim();
        notifyWaiters(key, loaded.skin());
    }

    private static void fail(String key, long generation) {
        if (generation != GENERATION.get()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            finishFailure(key);
        } else {
            client.execute(() -> finishFailure(key));
        }
    }

    private static void finishFailure(String key) {
        LOADING.remove(key);
        notifyWaiters(key, null);
    }

    private static void notifyWaiters(String key, PlayerSkin skin) {
        List<Consumer<PlayerSkin>> callbacks = WAITERS.remove(key);
        if (callbacks == null) {
            return;
        }
        for (Consumer<PlayerSkin> callback : callbacks) {
            callback.accept(skin);
        }
    }

    private static void trim() {
        while (READY.size() > MAX_ENTRIES) {
            Map.Entry<String, Loaded> oldest = null;
            for (Map.Entry<String, Loaded> entry : READY.entrySet()) {
                if (oldest == null || entry.getValue().createdAt() < oldest.getValue().createdAt()) {
                    oldest = entry;
                }
            }
            if (oldest == null) {
                return;
            }
            READY.remove(oldest.getKey());
            release(oldest.getValue().id());
        }
    }

    private static void release(Identifier id) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        client.execute(() -> client.getTextureManager().release(id));
    }

    public static String cacheKey(String url, String file, boolean slim) {
        String base = url != null && !url.isBlank() ? "url:" + url : "file:" + file;
        return base + ":" + (slim ? "slim" : "classic");
    }

    public static void clear() {
        GENERATION.incrementAndGet();
        List<Loaded> loaded = new ArrayList<>(READY.values());
        READY.clear();
        WAITERS.clear();
        LOADING.clear();
        for (Loaded entry : loaded) {
            release(entry.id());
        }
    }
}
