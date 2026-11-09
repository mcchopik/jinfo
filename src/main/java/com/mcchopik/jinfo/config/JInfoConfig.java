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
    private static String nickname = "";

    public enum SkinMode {
        AUTO, URL, FILE
    }

    private static SkinMode skinMode = SkinMode.AUTO;
    private static String skinUrl = "";
    private static String skinFile = "";
    private static boolean skinSlim = false;

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

    public static synchronized String nickname() {
        return nickname;
    }

    public static synchronized void setNickname(String value) {
        nickname = value == null ? "" : value.trim();
        save();
    }

    public static synchronized SkinMode skinMode() {
        return skinMode;
    }

    public static synchronized void setSkinMode(SkinMode value) {
        skinMode = value;
        save();
    }

    public static synchronized String skinUrl() {
        return skinUrl;
    }

    public static synchronized void setSkinUrl(String value) {
        skinUrl = value == null ? "" : value.trim();
        save();
    }

    public static synchronized String skinFile() {
        return skinFile;
    }

    public static synchronized void setSkinFile(String value) {
        skinFile = value == null ? "" : value.trim();
        save();
    }

    public static synchronized boolean skinSlim() {
        return skinSlim;
    }

    public static synchronized void setSkinSlim(boolean value) {
        skinSlim = value;
        save();
    }

    public static String skinPayload() {
        if (skinMode == SkinMode.URL && !skinUrl.isBlank()) {
            return "{\"url\":\"" + skinUrl.replace("\\", "\\\\").replace("\"", "\\\"") + "\",\"model\":\"" + (skinSlim ? "slim" : "classic") + "\"}";
        }
        return null;
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
                if (obj.has("nickname")) {
                    nickname = obj.get("nickname").getAsString();
                }
                if (obj.has("skinMode")) {
                    try {
                        skinMode = SkinMode.valueOf(obj.get("skinMode").getAsString());
                    } catch (Exception ignored) {
                    }
                }
                if (obj.has("skinUrl")) {
                    skinUrl = obj.get("skinUrl").getAsString();
                }
                if (obj.has("skinFile")) {
                    skinFile = obj.get("skinFile").getAsString();
                }
                if (obj.has("skinSlim")) {
                    skinSlim = obj.get("skinSlim").getAsBoolean();
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
            if (!nickname.isBlank()) {
                obj.addProperty("nickname", nickname);
            }
            obj.addProperty("skinMode", skinMode.name());
            if (!skinUrl.isBlank()) {
                obj.addProperty("skinUrl", skinUrl);
            }
            if (!skinFile.isBlank()) {
                obj.addProperty("skinFile", skinFile);
            }
            obj.addProperty("skinSlim", skinSlim);
            Files.writeString(file, obj.toString(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }
}
