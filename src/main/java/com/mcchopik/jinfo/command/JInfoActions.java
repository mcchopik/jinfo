package com.mcchopik.jinfo.command;

import static com.mcchopik.jinfo.command.CommandSupport.describe;
import static com.mcchopik.jinfo.command.CommandSupport.looksLikeUuid;
import static com.mcchopik.jinfo.command.CommandSupport.normalizeUuid;
import static com.mcchopik.jinfo.command.CommandSupport.reply;
import static com.mcchopik.jinfo.command.CommandSupport.selfName;

import com.mcchopik.jinfo.JInfoClient;
import com.mcchopik.jinfo.config.JInfoConfig;
import com.mcchopik.jinfo.net.ApiClient;
import com.mcchopik.jinfo.net.ApiException;
import com.mcchopik.jinfo.net.PlayerResolver;
import com.mcchopik.jinfo.net.ProfileData;
import com.mcchopik.jinfo.net.UpdateChecker;
import com.mcchopik.jinfo.screen.JInfoProfileScreen;
import com.mcchopik.jinfo.text.LinkValidator;
import com.mcchopik.jinfo.text.Messages;
import com.mcchopik.jinfo.text.ParsedProfile;
import com.mcchopik.jinfo.text.ProfileTemplates;
import com.mcchopik.jinfo.text.TextParser;
import com.mcchopik.jinfo.web.WebServer;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Util;

public final class JInfoActions {

    private static final TextParser PARSER = new TextParser();

    private JInfoActions() {
    }

    public static int help(FabricClientCommandSource src) {
        for (String line : Messages.s("help").split("\n", -1)) {
            reply(src, line);
        }
        return 1;
    }

    public static int templates(FabricClientCommandSource src) {
        reply(src, "=== " + Messages.s("templates") + " ===");
        for (ProfileTemplates t : ProfileTemplates.values()) {
            reply(src, "/jinfo template " + t.id() + " — " + t.label(JInfoConfig.language()));
        }
        reply(src, Messages.s("template_hint"));
        return 1;
    }

    public static int applyTemplate(FabricClientCommandSource src, ApiClient api, String id) {
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

    public static int photo(FabricClientCommandSource src, ApiClient api, String raw) {
        String name = selfName();
        if (name == null) {
            reply(src, Messages.s("editor_nick_unknown"));
            return 0;
        }
        String url = raw == null ? "" : raw.trim();
        boolean remove = url.isEmpty()
                || url.equalsIgnoreCase("off")
                || url.equalsIgnoreCase("none")
                || url.equalsIgnoreCase("remove")
                || url.equals("-");
        if (!remove) {
            if (!url.toLowerCase(Locale.ROOT).startsWith("https://")
                    || !LinkValidator.validateGeneric(url).valid()) {
                reply(src, Messages.s("photo_invalid"));
                return 0;
            }
        }
        reply(src, Messages.s("editor_saving"));
        CompletableFuture.runAsync(() -> {
            try {
                if (!api.isLoggedIn()) {
                    api.login(name);
                }
                String uuid = api.currentUuid();
                if (uuid == null) {
                    reply(src, Messages.s("editor_error") + "no uuid");
                    return;
                }
                String current = "";
                try {
                    current = api.getProfile(uuid).data();
                } catch (ApiException e) {
                    if (e.statusCode() != 404) {
                        throw e;
                    }
                }
                List<String> lines = new ArrayList<>();
                for (String line : current.split("\n", -1)) {
                    String trimmed = line.trim().toLowerCase(Locale.ROOT);
                    if (trimmed.startsWith("{avatar}") || trimmed.startsWith("{photo}")) {
                        continue;
                    }
                    lines.add(line);
                }
                while (!lines.isEmpty() && lines.get(0).isBlank()) {
                    lines.remove(0);
                }
                while (!lines.isEmpty() && lines.get(lines.size() - 1).isBlank()) {
                    lines.remove(lines.size() - 1);
                }
                if (!remove) {
                    lines.add(0, "{photo} " + url);
                }
                String text = String.join("\n", lines);
                if (text.isBlank()) {
                    try {
                        api.deleteProfile();
                    } catch (ApiException e) {
                        if (e.statusCode() != 404) {
                            throw e;
                        }
                    }
                    reply(src, Messages.s("photo_removed"));
                    return;
                }
                ParsedProfile parsed = PARSER.parse(text, JInfoConfig.language());
                if (parsed.overLimits()) {
                    reply(src, Messages.s("editor_over_limits"));
                    return;
                }
                api.putProfile(text);
                reply(src, remove ? Messages.s("photo_removed") : Messages.s("photo_set"));
            } catch (ApiException e) {
                reply(src, describe(e));
            } catch (Exception e) {
                reply(src, Messages.s("editor_error") + e.getMessage());
            }
        });
        return 1;
    }

    public static int previewSelf(FabricClientCommandSource src, ApiClient api) {
        String name = selfName();
        if (name == null) {
            reply(src, Messages.s("editor_nick_unknown"));
            return 0;
        }
        openProfile(src.getClient(), api, JInfoClient.selfUuid(name), name);
        return 1;
    }

    public static int preview(FabricClientCommandSource src, ApiClient api, String target) {
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

    public static int update(FabricClientCommandSource src) {
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

    public static int me(FabricClientCommandSource src, ApiClient api) {
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

    public static int view(FabricClientCommandSource src, ApiClient api, String target) {
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

    public static int set(FabricClientCommandSource src, ApiClient api, String text) {
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

    public static int clear(FabricClientCommandSource src, ApiClient api) {
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

    public static int webStart(FabricClientCommandSource src, ApiClient api) {
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

    public static int webStop(FabricClientCommandSource src) {
        WebServer.stop("command");
        reply(src, "Веб-сервер остановлен.");
        return 1;
    }

    private static void showProfile(FabricClientCommandSource src, ProfileData data) {
        ParsedProfile parsed = PARSER.parse(data.data(), JInfoConfig.language());
        if (data.verified()) {
            reply(src, "Статус: подтверждён");
        }
        for (String line : parsed.plainText().split("\n", -1)) {
            reply(src, line);
        }
    }
}
