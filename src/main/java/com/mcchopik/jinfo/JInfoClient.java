package com.mcchopik.jinfo;

import com.mcchopik.jinfo.command.JInfoCommand;
import com.mcchopik.jinfo.config.JInfoConfig;
import com.mcchopik.jinfo.net.ApiClient;
import com.mcchopik.jinfo.net.ClientChat;
import com.mcchopik.jinfo.net.MinecraftSession;
import com.mcchopik.jinfo.net.PlayerResolver;
import com.mcchopik.jinfo.net.UpdateChecker;
import com.mcchopik.jinfo.screen.JInfoCardScreen;
import com.mcchopik.jinfo.screen.JInfoNickScreen;
import com.mcchopik.jinfo.screen.JInfoProfileScreen;
import com.mcchopik.jinfo.web.WebServer;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
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
    private static boolean updatesChecked;

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
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> checkUpdates());
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

    private static void checkUpdates() {
        if (updatesChecked) {
            return;
        }
        updatesChecked = true;
        CompletableFuture.runAsync(() -> UpdateChecker.check().ifPresent(info -> {
            if (UpdateChecker.isNewer(info.version(), UpdateChecker.currentVersion())) {
                String suffix = info.pageUrl() == null ? "" : " — " + info.pageUrl();
                ClientChat.send("JInfo: доступна версия " + info.version() + suffix);
            }
        }));
    }

    private void openSelf(Minecraft client) {
        Screen parent = client.gui.screen();
        requireSelfName(parent, name -> client.gui.setScreen(
                new JInfoProfileScreen(api, selfUuid(name), name, null, parent)));
    }

    public static String selfName() {
        return MinecraftSession.currentUsername()
                .or(() -> Optional.ofNullable(JInfoConfig.nickname())
                        .filter(name -> !name.isBlank()))
                .orElse(null);
    }

    public static void requireSelfName(Screen parent, Consumer<String> action) {
        String name = selfName();
        if (name != null) {
            action.accept(name);
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.gui.setScreen(new JInfoNickScreen(parent, JInfoConfig.nickname(), action));
        }
    }

    public static String selfUuid(String name) {
        return MinecraftSession.current()
                .map(MinecraftSession.SessionInfo::uuid)
                .filter(JInfoClient::isUuidV4)
                .map(JInfoClient::dashed)
                .orElseGet(() -> PlayerResolver.offlineUuid(name));
    }

    private static boolean isUuidV4(String uuid) {
        if (uuid == null) {
            return false;
        }
        if (uuid.length() == 32) {
            return uuid.charAt(12) == '4';
        }
        if (uuid.length() == 36) {
            return uuid.charAt(14) == '4';
        }
        return false;
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
