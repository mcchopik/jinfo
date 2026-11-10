package com.mcchopik.jinfo.server.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mcchopik.jinfo.server.JInfoServerMod;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProfileStorage {

    private final Path profilesDir;
    private final Map<String, JsonObject> profilesByUuid = new ConcurrentHashMap<>();
    private final Map<String, String> nameToUuid = new ConcurrentHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ProfileStorage(String profilesDir) {
        this.profilesDir = Paths.get(profilesDir);
        try {
            Files.createDirectories(this.profilesDir);
            loadAll();
        } catch (IOException e) {
            JInfoServerMod.LOGGER.error("Failed to create profiles directory: {}", e.getMessage());
        }
    }

    private void loadAll() {
        try (var stream = Files.list(profilesDir)) {
            stream.filter(p -> p.toString().endsWith(".json"))
                .forEach(this::loadProfile);
        } catch (IOException e) {
            JInfoServerMod.LOGGER.error("Failed to load profiles: {}", e.getMessage());
        }
    }

    private void loadProfile(Path file) {
        try {
            String json = Files.readString(file);
            JsonObject obj = gson.fromJson(json, JsonObject.class);
            if (obj != null && obj.has("uuid")) {
                String uuid = obj.get("uuid").getAsString();
                profilesByUuid.put(uuid, obj);
                if (obj.has("name")) {
                    nameToUuid.put(obj.get("name").getAsString().toLowerCase(), uuid);
                }
            }
        } catch (IOException e) {
            JInfoServerMod.LOGGER.warn("Failed to load profile {}: {}", file, e.getMessage());
        }
    }

    public JsonObject getProfile(String uuid) {
        return profilesByUuid.get(uuid);
    }

    public String getUuidByName(String name) {
        return nameToUuid.get(name.toLowerCase());
    }

    public Collection<JsonObject> getAllProfiles() {
        return profilesByUuid.values();
    }

    public void saveProfile(JsonObject profile) {
        String uuid = profile.get("uuid").getAsString();
        profilesByUuid.put(uuid, profile);
        if (profile.has("name")) {
            nameToUuid.put(profile.get("name").getAsString().toLowerCase(), uuid);
        }
        Path file = profilesDir.resolve(uuid + ".json");
        try {
            Files.writeString(file, gson.toJson(profile));
        } catch (IOException e) {
            JInfoServerMod.LOGGER.error("Failed to save profile {}: {}", uuid, e.getMessage());
        }
    }

    public void deleteProfile(String uuid) {
        JsonObject removed = profilesByUuid.remove(uuid);
        if (removed != null && removed.has("name")) {
            nameToUuid.remove(removed.get("name").getAsString().toLowerCase());
        }
        try {
            Files.deleteIfExists(profilesDir.resolve(uuid + ".json"));
        } catch (IOException e) {
            JInfoServerMod.LOGGER.warn("Failed to delete profile file {}: {}", uuid, e.getMessage());
        }
    }
}