package com.mcchopik.jinfo.server;

import com.mcchopik.jinfo.server.config.ServerConfig;
import com.mcchopik.jinfo.server.http.HttpServerManager;
import com.mcchopik.jinfo.server.profile.ProfileStorage;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JInfoServerMod implements ModInitializer {

    public static final String MOD_ID = "jinfo-server";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static HttpServerManager httpServer;
    private static ProfileStorage profileStorage;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing JInfo Server mod...");

        ServerConfig.load();
        profileStorage = new ProfileStorage(ServerConfig.getProfilesDir());
        httpServer = new HttpServerManager(ServerConfig.getPort(), profileStorage);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        Commands.literal("jinfo")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.literal("reload")
                                        .executes(ctx -> {
                                            ServerConfig.load();
                                            httpServer.reload(ServerConfig.getPort());
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§aJInfo Server config reloaded"), true);
                                            return 1;
                                        }))
                                .then(Commands.literal("profile")
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    String name = StringArgumentType.getString(ctx, "name");
                                                    String uuid = profileStorage.getUuidByName(name);
                                                    if (uuid == null) {
                                                        ctx.getSource().sendFailure(
                                                                Component.literal("§cProfile not found for " + name));
                                                        return 0;
                                                    }
                                                    String url = "http://" + ServerConfig.getHost() + ":"
                                                            + ServerConfig.getPort() + "/p/" + uuid;
                                                    ctx.getSource().sendSuccess(() ->
                                                            Component.literal("§aProfile for " + name + ": §b" + url), true);
                                                    return 1;
                                                })))
                                .then(Commands.literal("list")
                                        .executes(ctx -> {
                                            if (profileStorage.getAllProfiles().isEmpty()) {
                                                ctx.getSource().sendSuccess(() ->
                                                        Component.literal("§7No profiles yet."), false);
                                                return 1;
                                            }
                                            profileStorage.getAllProfiles().forEach(p -> ctx.getSource().sendSuccess(() ->
                                                    Component.literal("§7- §f" + (p.has("name") ? p.get("name").getAsString() : "?")
                                                            + " §8(" + p.get("uuid").getAsString() + ")"), false));
                                            return 1;
                                        }))
                ));

        LOGGER.info("JInfo Server mod initialized on port {}", ServerConfig.getPort());
    }

    public static ProfileStorage getProfileStorage() {
        return profileStorage;
    }

    public static HttpServerManager getHttpServer() {
        return httpServer;
    }
}
