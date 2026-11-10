package com.mcchopik.jinfo.paper;

import com.mcchopik.jinfo.paper.command.JInfoCommand;
import com.mcchopik.jinfo.paper.profile.ProfileStorage;
import java.nio.file.Path;
import org.bukkit.plugin.java.JavaPlugin;

public class JInfoPaperPlugin extends JavaPlugin {

    private ProfileStorage profileStorage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Path dir = getDataFolder().toPath().resolve(getConfig().getString("profiles-dir", "profiles"));
        profileStorage = new ProfileStorage(dir, getLogger());
        JInfoCommand command = new JInfoCommand(this, profileStorage);
        if (getCommand("jinfo") != null) {
            getCommand("jinfo").setExecutor(command);
            getCommand("jinfo").setTabCompleter(command);
        }
        getLogger().info("JInfo Paper enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("JInfo Paper disabled");
    }

    public ProfileStorage storage() {
        return profileStorage;
    }

    public String centralApiBase() {
        return getConfig().getString("central-api-base", "https://mcprofiles.mcchopik.workers.dev");
    }
}
