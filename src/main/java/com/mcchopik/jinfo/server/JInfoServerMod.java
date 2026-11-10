package com.mcchopik.jinfo.server;

import com.mcchopik.jinfo.server.config.ServerConfig;
import com.mcchopik.jinfo.server.http.HttpServerManager;
import com.mcchopik.jinfo.server.profile.ProfileStorage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
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

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerCommands(dispatcher);
        });

        LOGGER.info("JInfo Server mod initialized on port {}", ServerConfig.getPort());
    }

    private void registerCommands(net.minecraft.command.CommandDispatcher<ServerCommandSource> dispatcher) {
        net.minecraft.command.argument.serialize.ArgumentSerializer.register();

        dispatcher.register(
            net.minecraft.server.command.CommandManager.literal("jinfo")
                .requires(source -> source.hasPermissionLevel(2))
                .then(net.minecraft.server.command.CommandManager.literal("reload")
                    .executes(ctx -> {
                        ServerConfig.load();
                        httpServer.reload(ServerConfig.getPort());
                        ctx.getSource().sendFeedback(() -> Text.literal("§aJInfo Server config reloaded"), true);
                        return 1;
                    }))
                .then(net.minecraft.server.command.CommandManager.literal("profile")
                    .then(net.minecraft.server.command.CommandManager.argument("name", net.minecraft.command.argument.StringArgumentType.string())
                        .executes(ctx -> {
                            String name = net.minecraft.command.argument.StringArgumentType.getString(ctx, "name");
                            String uuid = profileStorage.getUuidByName(name);
                            if (uuid == null) {
                                ctx.getSource().sendError(Text.literal("§cProfile not found for " + name));
                                return 0;
                            }
                            ctx.getSource().sendFeedback(() -> Text.literal("§aProfile for " + name + ": §e" + uuid + " §7- §bhttp://" + ServerConfig.getHost() + ":" + ServerConfig.getPort() + "/p/" + uuid), true);
                            return 1;
                        }))
                )
                .then(net.minecraft.server.command.CommandManager.literal("sync")
                    .executes(ctx -> {
                        ctx.getSource().sendFeedback(() -> Text.literal("§aSyncing with central JInfo API..."), true);
                        // TODO: implement sync
                        return 1;
                    }))
        );
    }

    public static ProfileStorage getProfileStorage() {
        return profileStorage;
    }

    public static HttpServerManager getHttpServer() {
        return httpServer;
    }
}