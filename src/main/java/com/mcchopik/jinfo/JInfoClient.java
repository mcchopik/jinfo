package com.mcchopik.jinfo;

import net.fabricmc.api.ClientModInitializer;

public class JInfoClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        JInfoMod.LOGGER.info("JInfo client initialized");
    }
}