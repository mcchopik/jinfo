package com.mcchopik.jinfo.command;

import com.mcchopik.jinfo.net.ApiClient;
import com.mcchopik.jinfo.net.ApiException;
import com.mcchopik.jinfo.net.MinecraftSession;
import com.mcchopik.jinfo.net.PlayerResolver;
import com.mcchopik.jinfo.net.ProfileData;
import com.mcchopik.jinfo.text.Lang;
import com.mcchopik.jinfo.text.ParsedProfile;
import com.mcchopik.jinfo.text.TextParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Клиентская команда /jinfo со всеми подкомандами и справкой.
 * Сетевые запросы выполняются в фоновом потоке, ответы показываются в чате.
 */
public final class JInfoCommand {

    private static final TextParser PARSER = new TextParser();
    private static final Lang LANG = Lang.RU;

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
                            reply(ctx.getSource(), "Экран редактора появится на следующем шаге.");
                            return 1;
                        }))
                        .then(lit("web").executes(ctx -> {
                            reply(ctx.getSource(), "Веб-редактор появится на следующем шаге.");
                            return 1;
                        }))
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

    // ------------------------------------------------------------------
    // Подкоманды
    // ------------------------------------------------------------------

    private static int help(FabricClientCommandSource src) {
        reply(src, "=== JInfo — профиль игрока ===");
        reply(src, "/jinfo help — эта справка");
        reply(src, "/jinfo edit — открыть редактор в игре");
        reply(src, "/jinfo web — открыть редактор в браузере");
        reply(src, "/jinfo set <текст> — быстро задать текст");
        reply(src, "/jinfo me — показать мой профиль");
        reply(src, "/jinfo view <ник> — профиль игрока");
        reply(src, "/jinfo clear — удалить мой профиль");
        reply(src, "--- Плейсхолдеры сервисов ---");
        reply(src, "{youtube}, {twitch}, {discord}, {telegram}, {tiktok}, {instagram},");
        reply(src, "{x}, {github}, {steam}, {vk}, {reddit}, {kick}, {boosty},");
        reply(src, "{donationalerts}, {modrinth}");
        reply(src, "После плейсхолдера пиши @ник или полную ссылку.");
        reply(src, "Пример: {youtube} @vasya");
        reply(src, "Универсальная ссылка: {link:Название} https://example.com");
        reply(src, "--- Цвета и стили ---");
        reply(src, "&0-&9, &a-&f — цвета; &l жирный; &o курсив; &n подчёркнутый;");
        reply(src, "&m зачёркнутый; &r сброс; hex: &#RRGGBB; &k запрещён.");
        reply(src, "Лимиты: 500 символов, 10 строк, до 6 ссылок.");
        return 1;
    }

    private static int me(FabricClientCommandSource src, ApiClient api) {
        String name = selfName();
        if (name == null) {
            reply(src, "Не удалось определить ник игрока.");
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
            reply(src, "Не удалось определить ник игрока.");
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
            reply(src, "Не удалось определить ник игрока.");
            return 0;
        }
        ParsedProfile parsed = PARSER.parse(text, LANG);
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
            reply(src, "Не удалось определить ник игрока.");
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

    // ------------------------------------------------------------------
    // Вспомогательное
    // ------------------------------------------------------------------

    private static void showProfile(FabricClientCommandSource src, ProfileData data) {
        ParsedProfile parsed = PARSER.parse(data.data(), LANG);
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
            case 429 -> "Слишком часто. Подожди немного.";
            case 401 -> "Не удалось авторизоваться.";
            case 404 -> "Профиль не заполнен.";
            default -> "Ошибка (" + e.statusCode() + "): " + e.getMessage();
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