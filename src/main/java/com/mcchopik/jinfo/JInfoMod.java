package com.mcchopik.jinfo;

import net.fabricmc.api.ModInitializer;

public class JInfoMod implements ModInitializer {
    @Override
    public void onInitialize() {
        JInfoMod.LOGGER.info("JInfo mod initialized");
    }

    public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("jinfo");
}