package com.mcchopik.jinfo.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Простой конфиг мода. На шаге 10 будет расширен настройками.
 */
public final class JInfoConfig {

    private static Path file;
    private static boolean gestureEnabled = true;

    private JInfoConfig() {
    }

    public static synchronized void init(Path dir) {
        file = dir.resolve("config.json");
        load();
    }

    public static synchronized boolean gestureEnabled() {
        return gestureEnabled;
    }

    public static synchronized void setGestureEnabled(boolean value) {
        gestureEnabled = value;
        save();
    }

    private static void load() {
        try {
            if (file != null && Files.exists(file)) {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                if (obj.has("gestureEnabled")) {
                    gestureEnabled = obj.get("gestureEnabled").getAsBoolean();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void save() {
        try {
            if (file == null) {
                return;
            }
            Files.createDirectories(file.getParent());
            JsonObject obj = new JsonObject();
            obj.addProperty("gestureEnabled", gestureEnabled);
            Files.writeString(file, obj.toString(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }
}