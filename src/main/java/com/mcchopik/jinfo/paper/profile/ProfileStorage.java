package com.mcchopik.jinfo.paper.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ProfileStorage {

    private final Path dir;
    private final Logger logger;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, JsonObject> byUuid = new ConcurrentHashMap<>();
    private final Map<String, String> nameToUuid = new ConcurrentHashMap<>();

    public ProfileStorage(Path dir, Logger logger) {
        this.dir = dir;
        this.logger = logger;
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            logger.warning("Could not create profiles directory: " + e.getMessage());
        }
        loadAll();
    }

    private void loadAll() {
        try (var stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(this::load);
        } catch (IOException e) {
            logger.warning("Could not load profiles: " + e.getMessage());
        }
    }

    private void load(Path file) {
        try {
            JsonObject obj = gson.fromJson(Files.readString(file), JsonObject.class);
            if (obj == null || !obj.has("uuid")) {
                return;
            }
            String uuid = obj.get("uuid").getAsString();
            byUuid.put(uuid, obj);
            if (obj.has("name")) {
                nameToUuid.put(obj.get("name").getAsString().toLowerCase(Locale.ROOT), uuid);
            }
        } catch (Exception e) {
            logger.warning("Could not read " + file + ": " + e.getMessage());
        }
    }

    public JsonObject get(String uuid) {
        return byUuid.get(uuid);
    }

    public String uuidByName(String name) {
        if (name == null) {
            return null;
        }
        return nameToUuid.get(name.toLowerCase(Locale.ROOT));
    }

    public Collection<JsonObject> all() {
        return byUuid.values();
    }

    public void save(JsonObject profile) {
        if (profile == null || !profile.has("uuid")) {
            return;
        }
        String uuid = profile.get("uuid").getAsString();
        byUuid.put(uuid, profile);
        if (profile.has("name")) {
            nameToUuid.put(profile.get("name").getAsString().toLowerCase(Locale.ROOT), uuid);
        }
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(uuid + ".json"), gson.toJson(profile));
        } catch (IOException e) {
            logger.warning("Could not save profile " + uuid + ": " + e.getMessage());
        }
    }

    public boolean remove(String uuid) {
        JsonObject removed = byUuid.remove(uuid);
        if (removed != null && removed.has("name")) {
            nameToUuid.remove(removed.get("name").getAsString().toLowerCase(Locale.ROOT));
        }
        try {
            return Files.deleteIfExists(dir.resolve(uuid + ".json"));
        } catch (IOException e) {
            logger.warning("Could not delete profile " + uuid + ": " + e.getMessage());
            return false;
        }
    }
}