package com.mcchopik.jinfo.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcchopik.jinfo.text.Lang;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JInfoConfig {

    private static Path file;
    private static boolean gestureEnabled = true;
    private static Lang language = Lang.RU;

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

    public static synchronized Lang language() {
        return language;
    }

    public static synchronized void setLanguage(Lang value) {
        language = value;
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
                if (obj.has("language")) {
                    try {
                        language = Lang.valueOf(obj.get("language").getAsString());
                    } catch (Exception ignored) {
                    }
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
            obj.addProperty("language", language.name());
            Files.writeString(file, obj.toString(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }
}
