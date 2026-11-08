package com.mcchopik.jinfo;

import com.mcchopik.jinfo.command.JInfoCommand;
import com.mcchopik.jinfo.net.ApiClient;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class JInfoClient implements ClientModInitializer {

    private static ApiClient api;

    public static ApiClient api() {
        return api;
    }

    @Override
    public void onInitializeClient() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("jinfo");
        api = new ApiClient(configDir);
        JInfoCommand.register(api);
        JInfoMod.LOGGER.info("JInfo client initialized (config: {})", configDir);
    }
}