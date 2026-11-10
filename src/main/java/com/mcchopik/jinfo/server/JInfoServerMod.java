package com.mcchopik.jinfo.server;

import com.mcchopik.jinfo.server.command.JInfoCommand;
import com.mcchopik.jinfo.server.config.ServerConfig;
import com.mcchopik.jinfo.server.profile.ProfileStorage;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JInfoServerMod implements ModInitializer {

    public static final String MOD_ID = "jinfo-server";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ProfileStorage profileStorage;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing JInfo Server mod...");
        ServerConfig.load();
        profileStorage = new ProfileStorage(ServerConfig.getProfilesDir());
        JInfoCommand.register(profileStorage);
        LOGGER.info("JInfo Server mod initialized ({} local profiles)", profileStorage.getAllProfiles().size());
    }

    public static ProfileStorage getProfileStorage() {
        return profileStorage;
    }
}
