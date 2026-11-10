package com.mcchopik.jinfo.server.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mcchopik.jinfo.server.JInfoServerMod;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ServerConfig {

    private static final String CONFIG_FILE = "jinfo-server.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static int port = 8765;
    private static String host = "0.0.0.0";
    private static String profilesDir = "jinfo-profiles";
    private static String centralApiBase = "https://mcprofiles.mcchopik.workers.dev";

    public static void load() {
        Path path = Paths.get(CONFIG_FILE);
        if (Files.exists(path)) {
            try {
                String json = Files.readString(path);
                ConfigData data = GSON.fromJson(json, ConfigData.class);
                if (data != null) {
                    port = data.port != 0 ? data.port : port;
                    host = data.host != null ? data.host : host;
                    profilesDir = data.profilesDir != null ? data.profilesDir : profilesDir;
                    centralApiBase = data.centralApiBase != null ? data.centralApiBase : centralApiBase;
                }
            } catch (IOException e) {
                JInfoServerMod.LOGGER.warn("Failed to load config: {}", e.getMessage());
            }
        }
        save();
    }

    public static void save() {
        try {
            ConfigData data = new ConfigData();
            data.port = port;
            data.host = host;
            data.profilesDir = profilesDir;
            data.centralApiBase = centralApiBase;
            Files.writeString(Paths.get(CONFIG_FILE), GSON.toJson(data));
        } catch (IOException e) {
            JInfoServerMod.LOGGER.warn("Failed to save config: {}", e.getMessage());
        }
    }

    public static int getPort() { return port; }
    public static String getHost() { return host; }
    public static String getProfilesDir() { return profilesDir; }
    public static String getCentralApiBase() { return centralApiBase; }

    private static class ConfigData {
        int port;
        String host;
        String profilesDir;
        String centralApiBase;
    }
}