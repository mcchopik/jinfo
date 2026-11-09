package com.mcchopik.jinfo.net;

import com.mojang.blaze3d.platform.NativeImage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class PhotoTextures {

    public record Photo(Identifier id, float u0, float u1, float v0, float v1) {
    }

    private static final int MAX_BYTES = 4 * 1024 * 1024;
    private static final int MAX_DIMENSION = 4096;
    private static final Map<String, Photo> READY = new ConcurrentHashMap<>();
    private static final Set<String> ATTEMPTED = ConcurrentHashMap.newKeySet();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private PhotoTextures() {
    }

    public static Photo get(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        Photo photo = READY.get(url);
        if (photo != null) {
            return photo;
        }
        if (ATTEMPTED.add(url)) {
            download(url);
        }
        return null;
    }

    private static void download(String url) {
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .header("User-Agent", "jinfo/0.1beta")
                        .header("Accept", "image/*")
                        .GET()
                        .build();
                HttpResponse<byte[]> response = HTTP.send(request,
                        HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() != 200) {
                    return;
                }
                byte[] bytes = response.body();
                if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES) {
                    return;
                }
                String type = response.headers().firstValue("Content-Type").orElse("")
                        .toLowerCase(Locale.ROOT);
                if (!type.isEmpty() && !type.startsWith("image/")) {
                    return;
                }
                Minecraft client = Minecraft.getInstance();
                if (client == null) {
                    return;
                }
                client.execute(() -> register(url, bytes));
            } catch (Throwable ignored) {
            }
        });
    }

    private static void register(String url, byte[] bytes) {
        NativeImage image = null;
        try {
            image = NativeImage.read(bytes);
            int w = image.getWidth();
            int h = image.getHeight();
            if (w <= 0 || h <= 0 || w > MAX_DIMENSION || h > MAX_DIMENSION) {
                image.close();
                return;
            }
            Identifier id = Identifier.fromNamespaceAndPath("jinfo",
                    "photo/" + Integer.toHexString(url.hashCode()) + "_" + w + "x" + h);
            DynamicTexture texture = new DynamicTexture(() -> "jinfo photo", image);
            Minecraft.getInstance().getTextureManager().register(id, texture);
            float u0 = 0f;
            float u1 = 1f;
            float v0 = 0f;
            float v1 = 1f;
            if (w > h) {
                float half = h / (2f * w);
                u0 = 0.5f - half;
                u1 = 0.5f + half;
            } else if (h > w) {
                float half = w / (2f * h);
                v0 = 0.5f - half;
                v1 = 0.5f + half;
            }
            READY.put(url, new Photo(id, u0, u1, v0, v1));
        } catch (Throwable ignored) {
            if (image != null) {
                image.close();
            }
        }
    }
}