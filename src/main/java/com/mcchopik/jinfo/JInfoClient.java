package com.mcchopik.jinfo;

import com.mcchopik.jinfo.command.JInfoCommand;
import com.mcchopik.jinfo.config.JInfoConfig;
import com.mcchopik.jinfo.net.ApiClient;
import com.mcchopik.jinfo.net.MinecraftSession;
import com.mcchopik.jinfo.net.PlayerResolver;
import com.mcchopik.jinfo.screen.JInfoCardScreen;
import com.mcchopik.jinfo.screen.JInfoProfileScreen;
import com.mcchopik.jinfo.web.WebServer;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

public class JInfoClient implements ClientModInitializer {

    private static ApiClient api;
    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("jinfo", "main"));
    private static KeyMapping openProfileKey;

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
        registerKeybind();
        JInfoMod.LOGGER.info("JInfo client initialized (config: {})", configDir);
    }

    private void registerKeybind() {
        openProfileKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.jinfo.profile", GLFW.GLFW_KEY_P, KEY_CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openProfileKey.consumeClick()) {
                openSelf(client);
            }
        });
    }

    private void openSelf(Minecraft client) {
        String name = MinecraftSession.currentUsername().orElse(null);
        if (name == null || name.isEmpty()) {
            return;
        }
        String uuid = MinecraftSession.current()
                .map(MinecraftSession.SessionInfo::uuid)
                .map(JInfoClient::dashed)
                .orElseGet(() -> PlayerResolver.offlineUuid(name));
        Screen parent = client.gui.screen();
        client.gui.setScreen(new JInfoProfileScreen(api, uuid, name, null, parent));
    }

    private static String dashed(String uuid) {
        if (uuid == null || uuid.contains("-") || uuid.length() != 32) {
            return uuid;
        }
        String h = uuid.toLowerCase();
        return h.substring(0, 8) + "-" + h.substring(8, 12) + "-" + h.substring(12, 16)
                + "-" + h.substring(16, 20) + "-" + h.substring(20, 32);
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