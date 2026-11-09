package com.mcchopik.jinfo;

import com.mcchopik.jinfo.command.JInfoCommand;
import com.mcchopik.jinfo.config.JInfoConfig;
import com.mcchopik.jinfo.net.ApiClient;
import com.mcchopik.jinfo.screen.JInfoCardScreen;
import com.mcchopik.jinfo.web.WebServer;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

public class JInfoClient implements ClientModInitializer {

    private static ApiClient api;

    public static ApiClient api() {
        return api;
    }

    @Override
    public void onInitializeClient() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("jinfo");
        JInfoConfig.init(configDir);
        api = new ApiClient(configDir);
        JInfoCommand.register(api);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> WebServer.stop("game"));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> WebServer.stop("world"));
        registerGesture();
        JInfoMod.LOGGER.info("JInfo client initialized (config: {})", configDir);
    }

    private void registerGesture() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!JInfoConfig.gestureEnabled()) {
                return InteractionResult.PASS;
            }
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            if (!world.isClientSide()) {
                return InteractionResult.PASS;
            }
            if (!(entity instanceof Player target)) {
                return InteractionResult.PASS;
            }
            if (!player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            Minecraft client = Minecraft.getInstance();
            String uuid = target.getUUID().toString();
            String name = target.getName().getString();
            Screen parent = client.gui.screen();
            client.execute(() -> client.gui.setScreen(
                    new JInfoCardScreen(api, uuid, name, target, parent)));
            return InteractionResult.SUCCESS;
        });
    }
}