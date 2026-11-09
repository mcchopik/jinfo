package com.mcchopik.jinfo.net;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class SkinTextures {

    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final int MAX_DIMENSION = 4096;
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final Map<String, PlayerSkin> READY = new ConcurrentHashMap<>();
    private static final Set<String> LOADING = ConcurrentHashMap.newKeySet();

    private SkinTextures() {
    }

    public static PlayerSkin get(String key) {
        return READY.get(key);
    }

    public static boolean isLoading(String key) {
        return LOADING.contains(key);
    }

    public static void loadAsync(String key, String url, String file, boolean slim) {
        if (key == null || key.isBlank()) {
            return;
        }
        if (READY.containsKey(key) || LOADING.contains(key)) {
            return;
        }
        LOADING.add(key);
        if (url != null && !url.isBlank()) {
            downloadFromUrl(key, url, slim);
        } else if (file != null && !file.isBlank()) {
            loadFromFile(key, file, slim);
        } else {
            LOADING.remove(key);
        }
    }

    private static void downloadFromUrl(String key, String url, boolean slim) {
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .header("User-Agent", "jinfo/0.3beta")
                        .header("Accept", "image/*")
                        .GET()
                        .build();
                HttpResponse<byte[]> response = HTTP.send(request,
                        HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() != 200) {
                    LOADING.remove(key);
                    return;
                }
                byte[] bytes = response.body();
                if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES) {
                    LOADING.remove(key);
                    return;
                }
                String type = response.headers().firstValue("Content-Type").orElse("")
                        .toLowerCase(Locale.ROOT);
                if (!type.isEmpty() && !type.startsWith("image/")) {
                    LOADING.remove(key);
                    return;
                }
                Minecraft client = Minecraft.getInstance();
                if (client == null) {
                    LOADING.remove(key);
                    return;
                }
                client.execute(() -> register(key, bytes, slim));
            } catch (Throwable ignored) {
                LOADING.remove(key);
            }
        });
    }

    private static void loadFromFile(String key, String filePath, boolean slim) {
        CompletableFuture.runAsync(() -> {
            try {
                Path path = Path.of(filePath);
                if (!Files.exists(path) || !Files.isRegularFile(path)) {
                    LOADING.remove(key);
                    return;
                }
                long size = Files.size(path);
                if (size == 0 || size > MAX_BYTES) {
                    LOADING.remove(key);
                    return;
                }
                byte[] bytes = Files.readAllBytes(path);
                Minecraft client = Minecraft.getInstance();
                if (client == null) {
                    LOADING.remove(key);
                    return;
                }
                client.execute(() -> register(key, bytes, slim));
            } catch (Throwable ignored) {
                LOADING.remove(key);
            }
        });
    }

    private static void register(String key, byte[] bytes, boolean slim) {
        NativeImage image = null;
        try {
            image = NativeImage.read(bytes);
            int w = image.getWidth();
            int h = image.getHeight();
            if ((w != 64 && w != 128) || (h != 32 && h != 64 && h != 128)) {
                image.close();
                LOADING.remove(key);
                return;
            }
            Identifier id = Identifier.fromNamespaceAndPath("jinfo",
                    "skin/" + Integer.toHexString(key.hashCode()) + "_" + w + "x" + h);
            DynamicTexture texture = new DynamicTexture(() -> "jinfo skin", image);
            TextureManager tm = Minecraft.getInstance().getTextureManager();
            tm.register(id, texture);
            ClientAsset.Texture skinTexture = new ClientAsset.Texture() {
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
            PlayerSkin skin = PlayerSkin.insecure(skinTexture, null, null, model);
            READY.put(key, skin);
            LOADING.remove(key);
        } catch (Throwable ignored) {
            if (image != null) {
                image.close();
            }
            LOADING.remove(key);
        }
    }

    public static String cacheKey(String url, String file, boolean slim) {
        String base = (url != null && !url.isBlank()) ? "url:" + url : "file:" + file;
        return base + ":" + (slim ? "slim" : "classic");
    }

    public static void clear() {
        for (PlayerSkin skin : READY.values()) {
            skin.body().close();
        }
        READY.clear();
        LOADING.clear();
    }
}