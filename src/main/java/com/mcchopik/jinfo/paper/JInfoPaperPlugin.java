package com.mcchopik.jinfo.paper;

import com.mcchopik.jinfo.paper.command.JInfoCommand;
import com.mcchopik.jinfo.paper.http.HttpServerManager;
import com.mcchopik.jinfo.paper.profile.ProfileStorage;
import java.nio.file.Path;
import org.bukkit.plugin.java.JavaPlugin;

public class JInfoPaperPlugin extends JavaPlugin {

    private ProfileStorage profileStorage;
    private HttpServerManager httpServer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Path dir = getDataFolder().toPath().resolve("profiles");
        profileStorage = new ProfileStorage(dir, getLogger());
        httpServer = new HttpServerManager(this, profileStorage);

        String host = getConfig().getString("host", "0.0.0.0");
        int port = getConfig().getInt("port", 8765);
        try {
            httpServer.start(host, port);
            getLogger().info("JInfo web viewer started on " + host + ":" + port);
        } catch (Exception e) {
            getLogger().severe("Failed to start JInfo web viewer: " + e.getMessage());
        }

        JInfoCommand command = new JInfoCommand(this, profileStorage, httpServer);
        if (getCommand("jinfo") != null) {
            getCommand("jinfo").setExecutor(command);
            getCommand("jinfo").setTabCompleter(command);
        }
        getLogger().info("JInfo Paper enabled");
    }

    @Override
    public void onDisable() {
        if (httpServer != null) {
            httpServer.stop();
        }
        getLogger().info("JInfo Paper disabled");
    }

    public ProfileStorage storage() {
        return profileStorage;
    }

    public HttpServerManager http() {
        return httpServer;
    }

    public String centralApiBase() {
        return getConfig().getString("central-api-base", "https://mcprofiles.mcchopik.workers.dev");
    }
}