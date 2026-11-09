package com.mcchopik.jinfo.command;

import com.mcchopik.jinfo.JInfoClient;
import com.mcchopik.jinfo.config.JInfoConfig;
import com.mcchopik.jinfo.net.ApiClient;
import com.mcchopik.jinfo.net.ApiException;
import com.mcchopik.jinfo.net.MinecraftSession;
import com.mcchopik.jinfo.net.PlayerResolver;
import com.mcchopik.jinfo.net.ProfileData;
import com.mcchopik.jinfo.net.UpdateChecker;
import com.mcchopik.jinfo.screen.JInfoEditorScreen;
import com.mcchopik.jinfo.screen.JInfoProfileScreen;
import com.mcchopik.jinfo.screen.JInfoSettingsScreen;
import com.mcchopik.jinfo.text.Messages;
import com.mcchopik.jinfo.text.ParsedProfile;
import com.mcchopik.jinfo.text.ProfileTemplates;
import com.mcchopik.jinfo.text.TextParser;
import com.mcchopik.jinfo.web.WebServer;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

public final class JInfoCommand {

    private static final TextParser PARSER = new TextParser();

    private JInfoCommand() {
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> lit(String name) {
        return LiteralArgumentBuilder.<FabricClientCommandSource>literal(name);
    }

    private static RequiredArgumentBuilder<FabricClientCommandSource, String> arg(
            String name, StringArgumentType type) {
        return RequiredArgumentBuilder.<FabricClientCommandSource, String>argument(name, type);
    }

    public static void register(ApiClient api) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(lit("jinfo")
                        .executes(ctx -> help(ctx.getSource()))
                        .then(lit("help").executes(ctx -> help(ctx.getSource())))
                        .then(lit("edit").executes(ctx -> {
                            Minecraft client = ctx.getSource().getClient();
                            client.execute(() -> {
                                Screen parent = client.gui.screen();
                                client.gui.setScreen(new JInfoEditorScreen(api, parent));
                            });
                            return 1;
                        }))
                        .then(lit("web")
                                .executes(ctx -> webStart(ctx.getSource(), api))
                                .then(lit("stop").executes(ctx -> webStop(ctx.getSource()))))
                        .then(lit("settings").executes(ctx -> {
                            Minecraft client = ctx.getSource().getClient();
                            client.execute(() -> {
                                Screen parent = client.gui.screen();
                                client.gui.setScreen(new JInfoSettingsScreen(parent));
                            });
                            return 1;
                        }))
                        .then(lit("update").executes(ctx -> update(ctx.getSource())))
                        .then(lit("template")
                                .executes(ctx -> templates(ctx.getSource()))
                                .then(arg("id", StringArgumentType.word())
                                        .executes(ctx -> applyTemplate(ctx.getSource(), api,
                                                StringArgumentType.getString(ctx, "id")))))
                        .then(lit("preview")
                                .executes(ctx -> previewSelf(ctx.getSource(), api))
                                .then(arg("name", StringArgumentType.word())
                                        .executes(ctx -> preview(ctx.getSource(), api,
                                                StringArgumentType.getString(ctx, "name")))))
                        .then(lit("me").executes(ctx -> me(ctx.getSource(), api)))
                        .then(lit("clear").executes(ctx -> clear(ctx.getSource(), api)))
                        .then(lit("view").then(arg("name", StringArgumentType.word())
                                .executes(ctx -> view(ctx.getSource(), api,
                                        StringArgumentType.getString(ctx, "name")))))
                        .then(lit("set").then(arg("text", StringArgumentType.greedyString())
                                .executes(ctx -> set(ctx.getSource(), api,
                                        StringArgumentType.getString(ctx, "text")))))
                )
        );
    }

    private static int help(FabricClientCommandSource src) {
        for (String line : Messages.s("help").split("\n", -1)) {
            reply(src, line);
        }
        return 1;
    }

    private static int templates(FabricClientCommandSource src) {
        reply(src, "=== " + Messages.s("templates") + " ===");
        for (ProfileTemplates t : ProfileTemplates.values()) {
            reply(src, "/jinfo template " + t.id() + " — " + t.label(JInfoConfig.language()));
        }
        reply(src, Messages.s("template_hint"));
        return 1;
    }

    private static int applyTemplate(FabricClientCommandSource src, ApiClient api, String id) {
        ProfileTemplates template = ProfileTemplates.byId(id);
        if (template == null) {
            reply(src, Messages.s("template_unknown"));
            return 0;
        }
        String name = selfName();
        if (name == null) {
            reply(src, Messages.s("editor_nick_unknown"));
            return 0;
        }
        String text = template.text(JInfoConfig.language());
        reply(src, Messages.s("editor_saving"));
        CompletableFuture.runAsync(() -> {
            try {
                if (!api.isLoggedIn()) {
                    api.login(name);
                }
                api.putProfile(text);
                reply(src, Messages.s("template_applied"));
            } catch (ApiException e) {
                reply(src, describe(e));
            } catch (Exception e) {
                reply(src, Messages.s("editor_error") + e.getMessage());
            }
        });
        return 1;
    }

    private static int previewSelf(FabricClientCommandSource src, ApiClient api) {
        String name = selfName();
        if (name == null) {
            reply(src, Messages.s("editor_nick_unknown"));
            return 0;
        }
        openProfile(src.getClient(), api, JInfoClient.selfUuid(name), name);
        return 1;
    }

    private static int preview(FabricClientCommandSource src, ApiClient api, String target) {
        Minecraft client = src.getClient();
        String uuid = looksLikeUuid(target)
                ? normalizeUuid(target)
                : PlayerResolver.resolveUuid(target, client);
        openProfile(client, api, uuid, target);
        return 1;
    }

    private static void openProfile(Minecraft client, ApiClient api, String uuid, String name) {
        client.execute(() -> {
            Screen parent = client.gui.screen();
            client.gui.setScreen(new JInfoProfileScreen(api, uuid, name, null, parent));
        });
    }

    private static int update(FabricClientCommandSource src) {
        reply(src, "Проверяю обновления...");
        CompletableFuture.runAsync(() -> {
            Optional<UpdateChecker.UpdateInfo> info = UpdateChecker.check();
            if (info.isEmpty()) {
                reply(src, "Релизов пока нет или GitHub недоступен.");
                return;
            }
            UpdateChecker.UpdateInfo u = info.get();
            String current = UpdateChecker.currentVersion();
            if (UpdateChecker.isNewer(u.version(), current)) {
                String suffix = u.pageUrl() == null ? "" : " " + u.pageUrl();
                reply(src, "Доступна версия " + u.version() + " (у вас " + current + ")." + suffix);
            } else {
                reply(src, "У вас последняя версия (" + current + ").");
            }
        });
        return 1;
    }

    private static int me(FabricClientCommandSource src, ApiClient api) {
        String name = selfName();
        if (name == null) {
            reply(src, Messages.s("editor_nick_unknown"));
            return 0;
        }
        reply(src, "Загрузка профиля...");
        CompletableFuture.runAsync(() -> {
            try {
                if (!api.isLoggedIn()) {
                    api.login(name);
                }
                String uuid = api.currentUuid();
                ProfileData data = api.getProfile(uuid);
                reply(src, "=== Мой профиль ===");
                showProfile(src, data);
            } catch (ApiException e) {
                reply(src, describe(e));
            } catch (Exception e) {
                reply(src, "Ошибка: " + e.getMessage());
            }
        });
        return 1;
    }

    private static int view(FabricClientCommandSource src, ApiClient api, String target) {
        String name = selfName();
        if (name == null) {
            reply(src, Messages.s("editor_nick_unknown"));
            return 0;
        }
        String uuid = looksLikeUuid(target)
                ? normalizeUuid(target)
                : PlayerResolver.resolveUuid(target, src.getClient());
        reply(src, "Загрузка профиля " + target + "...");
        CompletableFuture.runAsync(() -> {
            try {
                if (!api.isLoggedIn()) {
                    api.login(name);
                }
                ProfileData data = api.getProfile(uuid);
                reply(src, "=== Профиль " + target + " ===");
                showProfile(src, data);
            } catch (ApiException e) {
                reply(src, describe(e));
            } catch (Exception e) {
                reply(src, "Ошибка: " + e.getMessage());
            }
        });
        return 1;
    }

    private static int set(FabricClientCommandSource src, ApiClient api, String text) {
        String name = selfName();
        if (name == null) {
            reply(src, Messages.s("editor_nick_unknown"));
            return 0;
        }
        ParsedProfile parsed = PARSER.parse(text, JInfoConfig.language());
        if (parsed.overLimits()) {
            reply(src, "Превышены лимиты (500 символов / 10 строк / 6 ссылок).");
            return 0;
        }
        reply(src, "Сохраняю профиль...");
        CompletableFuture.runAsync(() -> {
            try {
                if (!api.isLoggedIn()) {
                    api.login(name);
                }
                api.putProfile(text);
                reply(src, "Профиль сохранён.");
            } catch (ApiException e) {
                reply(src, describe(e));
            } catch (Exception e) {
                reply(src, "Ошибка: " + e.getMessage());
            }
        });
        return 1;
    }

    private static int clear(FabricClientCommandSource src, ApiClient api) {
        String name = selfName();
        if (name == null) {
            reply(src, Messages.s("editor_nick_unknown"));
            return 0;
        }
        reply(src, "Удаляю профиль...");
        CompletableFuture.runAsync(() -> {
            try {
                if (!api.isLoggedIn()) {
                    api.login(name);
                }
                api.deleteProfile();
                reply(src, "Профиль удалён.");
            } catch (ApiException e) {
                if (e.statusCode() == 404) {
                    reply(src, "Профиль и так не был заполнен.");
                } else {
                    reply(src, describe(e));
                }
            } catch (Exception e) {
                reply(src, "Ошибка: " + e.getMessage());
            }
        });
        return 1;
    }

    private static int webStart(FabricClientCommandSource src, ApiClient api) {
        String name = selfName();
        if (name == null) {
            reply(src, Messages.s("editor_nick_unknown"));
            return 0;
        }
        Minecraft client = src.getClient();
        CompletableFuture.runAsync(() -> {
            try {
                String url = WebServer.start(api, name);
                client.execute(() -> {
                    try {
                        Util.getPlatform().openUri(URI.create(url));
                    } catch (Throwable ignored) {

                    }
                });
                reply(src, "Веб-редактор: " + url);
                reply(src, "Остановить: /jinfo web stop");
            } catch (Exception e) {
                reply(src, "Не удалось запустить веб-сервер: " + e.getMessage());
            }
        });
        return 1;
    }

    private static int webStop(FabricClientCommandSource src) {
        WebServer.stop("command");
        reply(src, "Веб-сервер остановлен.");
        return 1;
    }

    private static void showProfile(FabricClientCommandSource src, ProfileData data) {
        ParsedProfile parsed = PARSER.parse(data.data(), JInfoConfig.language());
        String marker = data.verified() ? "" : " [не подтверждён]";
        reply(src, "Статус: " + (data.verified() ? "подтверждён" : "не подтверждён") + marker);
        for (String line : parsed.plainText().split("\n", -1)) {
            reply(src, line);
        }
    }

    private static String selfName() {
        return MinecraftSession.currentUsername().orElse(null);
    }

    private static void reply(FabricClientCommandSource src, String text) {
        Minecraft client = src.getClient();
        client.execute(() -> src.sendFeedback(Component.literal(text)));
    }

    private static String describe(ApiException e) {
        return switch (e.statusCode()) {
            case 429 -> Messages.s("err_rate");
            case 401 -> Messages.s("err_auth");
            case 404 -> Messages.s("err_not_filled");
            default -> Messages.s("err_generic") + " (" + e.statusCode() + "): " + e.getMessage();
        };
    }

    private static boolean looksLikeUuid(String s) {
        return s.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
                || s.matches("[0-9a-fA-F]{32}");
    }

    private static String normalizeUuid(String s) {
        if (s.contains("-")) {
            return s.toLowerCase();
        }
        String h = s.toLowerCase();
        return h.substring(0, 8) + "-" + h.substring(8, 12) + "-" + h.substring(12, 16)
                + "-" + h.substring(16, 20) + "-" + h.substring(20, 32);
    }
}
