package com.mcchopik.jinfo.command;

import static com.mcchopik.jinfo.command.CommandSupport.arg;
import static com.mcchopik.jinfo.command.CommandSupport.lit;

import com.mcchopik.jinfo.net.ApiClient;
import com.mcchopik.jinfo.screen.JInfoEditorScreen;
import com.mcchopik.jinfo.screen.JInfoSettingsScreen;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class JInfoCommand {

    private JInfoCommand() {
    }

    public static void register(ApiClient api) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(lit("jinfo")
                        .executes(ctx -> JInfoActions.help(ctx.getSource()))
                        .then(lit("help").executes(ctx -> JInfoActions.help(ctx.getSource())))
                        .then(lit("edit").executes(ctx -> {
                            Minecraft client = ctx.getSource().getClient();
                            client.execute(() -> {
                                Screen parent = client.gui.screen();
                                client.gui.setScreen(new JInfoEditorScreen(api, parent));
                            });
                            return 1;
                        }))
                        .then(lit("web")
                                .executes(ctx -> JInfoActions.webStart(ctx.getSource(), api))
                                .then(lit("stop").executes(ctx -> JInfoActions.webStop(ctx.getSource()))))
                        .then(lit("settings").executes(ctx -> {
                            Minecraft client = ctx.getSource().getClient();
                            client.execute(() -> {
                                Screen parent = client.gui.screen();
                                client.gui.setScreen(new JInfoSettingsScreen(parent));
                            });
                            return 1;
                        }))
                        .then(lit("update").executes(ctx -> JInfoActions.update(ctx.getSource())))
                        .then(lit("template")
                                .executes(ctx -> JInfoActions.templates(ctx.getSource()))
                                .then(arg("id", StringArgumentType.word())
                                        .executes(ctx -> JInfoActions.applyTemplate(ctx.getSource(), api,
                                                StringArgumentType.getString(ctx, "id")))))
                        .then(lit("photo")
                                .then(arg("url", StringArgumentType.greedyString())
                                        .executes(ctx -> JInfoActions.photo(ctx.getSource(), api,
                                                StringArgumentType.getString(ctx, "url")))))
                        .then(lit("preview")
                                .executes(ctx -> JInfoActions.previewSelf(ctx.getSource(), api))
                                .then(arg("name", StringArgumentType.word())
                                        .executes(ctx -> JInfoActions.preview(ctx.getSource(), api,
                                                StringArgumentType.getString(ctx, "name")))))
                        .then(lit("me").executes(ctx -> JInfoActions.me(ctx.getSource(), api)))
                        .then(lit("clear").executes(ctx -> JInfoActions.clear(ctx.getSource(), api)))
                        .then(lit("view").then(arg("name", StringArgumentType.word())
                                .executes(ctx -> JInfoActions.view(ctx.getSource(), api,
                                        StringArgumentType.getString(ctx, "name")))))
                        .then(lit("set").then(arg("text", StringArgumentType.greedyString())
                                .executes(ctx -> JInfoActions.set(ctx.getSource(), api,
                                        StringArgumentType.getString(ctx, "text")))))
                )
        );
    }
}
