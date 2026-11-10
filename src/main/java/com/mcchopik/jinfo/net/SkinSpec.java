package com.mcchopik.jinfo.net;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public record SkinSpec(String url, boolean slim, String cacheKey) {

    public static SkinSpec parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            String url = obj.has("url") && !obj.get("url").isJsonNull()
                    ? obj.get("url").getAsString().trim() : "";
            if (!url.toLowerCase(java.util.Locale.ROOT).startsWith("https://")) {
                return null;
            }
            boolean slim = obj.has("model")
                    && "slim".equalsIgnoreCase(obj.get("model").getAsString());
            return new SkinSpec(url, slim, SkinTextures.cacheKey(url, null, slim));
        } catch (Exception ignored) {
            return null;
        }
    }
}
