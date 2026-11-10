package com.mcchopik.jinfo.server.command;

import com.google.gson.JsonObject;
import com.mcchopik.jinfo.server.JInfoServerMod;
import com.mcchopik.jinfo.server.api.CentralApi;
import com.mcchopik.jinfo.server.config.ServerConfig;
import com.mcchopik.jinfo.server.profile.ProfileStorage;
import com.mcchopik.jinfo.server.text.ProfileText;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public final class JInfoCommand {

    private static final String PREFIX = "JInfo";

    private JInfoCommand() {
    }

    public static void register(ProfileStorage storage) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("jinfo")
                        .executes(ctx -> help(ctx.getSource()))
                        .then(Commands.literal("help").executes(ctx -> help(ctx.getSource())))
                        .then(Commands.literal("me").executes(ctx -> me(ctx, storage)))
                        .then(Commands.literal("view")
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .executes(ctx -> view(ctx, storage,
                                                StringArgumentType.getString(ctx, "target")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                        .executes(ctx -> set(ctx, storage,
                                                StringArgumentType.getString(ctx, "text")))))
                        .then(Commands.literal("clear").executes(ctx -> clear(ctx, storage)))
                        .then(Commands.literal("photo")
                                .then(Commands.argument("url", StringArgumentType.greedyString())
                                        .executes(ctx -> photo(ctx, storage,
                                                StringArgumentType.getString(ctx, "url")))))
                        .then(Commands.literal("list")
                                .requires(source -> admin(source))
                                .executes(ctx -> list(ctx.getSource(), storage)))
                        .then(Commands.literal("import")
                                .requires(source -> admin(source))
                                .then(Commands.argument("uuid", StringArgumentType.word())
                                        .executes(ctx -> importProfile(ctx, storage,
                                                StringArgumentType.getString(ctx, "uuid"), null))
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .executes(ctx -> importProfile(ctx, storage,
                                                        StringArgumentType.getString(ctx, "uuid"),
                                                        StringArgumentType.getString(ctx, "name"))))))
                        .then(Commands.literal("remove")
                                .requires(source -> admin(source))
                                .then(Commands.argument("target", StringArgumentType.word())
                                        .executes(ctx -> remove(ctx.getSource(), storage,
                                                StringArgumentType.getString(ctx, "target")))))
                        .then(Commands.literal("reload")
                                .requires(source -> admin(source))
                                .executes(ctx -> reload(ctx.getSource())))
                ));
    }

    private static boolean admin(CommandSourceStack source) {
        return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    private static int help(CommandSourceStack src) {
        send(src, Component.literal("JInfo").withStyle(ChatFormatting.AQUA), false);
        send(src, Component.literal("/jinfo me — my profile"), false);
        send(src, Component.literal("/jinfo view <name|uuid> — view a profile"), false);
        send(src, Component.literal("/jinfo set <text> — save my server profile"), false);
        send(src, Component.literal("/jinfo photo <url|off> — set or remove my photo"), false);
        send(src, Component.literal("/jinfo clear — remove my server profile"), false);
        send(src, Component.literal("/jinfo list — local profiles (admin)"), false);
        send(src, Component.literal("/jinfo import <uuid> [name] — import from cloud (admin)"), false);
        send(src, Component.literal("/jinfo remove <name|uuid> — remove local profile (admin)"), false);
        send(src, Component.literal("/jinfo reload — reload config (admin)"), false);
        return 1;
    }

    private static int me(CommandContext<CommandSourceStack> ctx, ProfileStorage storage) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            return fail(src, "This command can only be used by a player.");
        }
        String uuid = player.getUUID().toString();
        String name = player.getName().getString();
        JsonObject local = storage.getProfile(uuid);
        if (local != null) {
            show(src, name, local, "server");
            return 1;
        }
        fetchAndShow(src, storage, uuid, name);
        return 1;
    }

    private static int view(CommandContext<CommandSourceStack> ctx, ProfileStorage storage, String target) {
        CommandSourceStack src = ctx.getSource();
        String localUuid = storage.getUuidByName(target);
        if (localUuid != null) {
            JsonObject local = storage.getProfile(localUuid);
            if (local != null) {
                show(src, target, local, "server");
                return 1;
            }
        }
        UUID uuid = resolve(src, target);
        if (uuid == null) {
            return fail(src, "Player not found: " + target);
        }
        JsonObject cached = storage.getProfile(uuid.toString());
        if (cached != null) {
            show(src, target, cached, "server");
            return 1;
        }
        fetchAndShow(src, storage, uuid.toString(), target);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> ctx, ProfileStorage storage, String text) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            return fail(src, "This command can only be used by a player.");
        }
        String error = ProfileText.validate(text);
        if (error != null) {
            return fail(src, error);
        }
        JsonObject profile = new JsonObject();
        profile.addProperty("uuid", player.getUUID().toString());
        profile.addProperty("name", player.getName().getString());
        profile.addProperty("data", text);
        profile.addProperty("updated", Instant.now().getEpochSecond());
        storage.saveProfile(profile);
        send(src, Component.literal("Profile saved. Others can view it with /jinfo view " + player.getName().getString())
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int photo(CommandContext<CommandSourceStack> ctx, ProfileStorage storage, String raw) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            return fail(src, "This command can only be used by a player.");
        }
        String url = raw == null ? "" : raw.trim();
        boolean remove = url.isEmpty() || url.equalsIgnoreCase("off")
                || url.equalsIgnoreCase("none") || url.equalsIgnoreCase("remove") || url.equals("-");
        if (!remove && (!url.toLowerCase(Locale.ROOT).startsWith("https://") || !validUrl(url))) {
            return fail(src, "A direct https image link is required.");
        }
        String uuid = player.getUUID().toString();
        JsonObject profile = storage.getProfile(uuid);
        if (profile == null) {
            profile = new JsonObject();
            profile.addProperty("uuid", uuid);
            profile.addProperty("name", player.getName().getString());
            profile.addProperty("data", "");
        }
        String current = profile.has("data") ? profile.get("data").getAsString() : "";
        StringBuilder out = new StringBuilder();
        for (String line : current.split("\n", -1)) {
            String trimmed = line.trim().toLowerCase(Locale.ROOT);
            if (trimmed.startsWith("{photo}") || trimmed.startsWith("{avatar}")) {
                continue;
            }
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(line);
        }
        String text = out.toString().strip();
        if (!remove) {
            text = "{photo} " + url + (text.isEmpty() ? "" : "\n" + text);
        }
        String error = ProfileText.validate(text);
        if (error != null) {
            return fail(src, error);
        }
        profile.addProperty("data", text);
        profile.addProperty("updated", Instant.now().getEpochSecond());
        storage.saveProfile(profile);
        send(src, Component.literal(remove ? "Profile photo removed." : "Profile photo updated.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx, ProfileStorage storage) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            return fail(src, "This command can only be used by a player.");
        }
        storage.deleteProfile(player.getUUID().toString());
        send(src, Component.literal("Server profile removed.").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int list(CommandSourceStack src, ProfileStorage storage) {
        if (storage.getAllProfiles().isEmpty()) {
            send(src, Component.literal("No local profiles.").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }
        send(src, Component.literal("Local JInfo profiles:").withStyle(ChatFormatting.AQUA), false);
        for (JsonObject profile : storage.getAllProfiles()) {
            String name = profile.has("name") ? profile.get("name").getAsString() : "?";
            String uuid = profile.has("uuid") ? profile.get("uuid").getAsString() : "?";
            send(src, Component.literal("- " + name + " (" + uuid + ")").withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int importProfile(CommandContext<CommandSourceStack> ctx, ProfileStorage storage,
                                     String rawUuid, String name) {
        CommandSourceStack src = ctx.getSource();
        String uuid = normalizeUuid(rawUuid);
        if (uuid == null) {
            return fail(src, "Invalid UUID: " + rawUuid);
        }
        send(src, Component.literal("Importing profile " + uuid + "...").withStyle(ChatFormatting.GRAY), false);
        CompletableFuture.runAsync(() -> {
            var remote = CentralApi.fetch(uuid);
            src.getServer().executeIfPossible(() -> {
                if (remote.isEmpty()) {
                    fail(src, "Profile not found in JInfo cloud.");
                    return;
                }
                JsonObject source = remote.get();
                JsonObject profile = new JsonObject();
                profile.addProperty("uuid", uuid);
                profile.addProperty("name", name != null ? name
                        : source.has("name") ? source.get("name").getAsString() : "Unknown");
                profile.addProperty("data", source.has("data") ? source.get("data").getAsString() : "");
                if (source.has("skin") && !source.get("skin").isJsonNull()) {
                    profile.addProperty("skin", source.get("skin").getAsString());
                }
                profile.addProperty("updated", Instant.now().getEpochSecond());
                storage.saveProfile(profile);
                send(src, Component.literal("Imported profile " + profile.get("name").getAsString())
                        .withStyle(ChatFormatting.GREEN), true);
            });
        });
        return 1;
    }

    private static int remove(CommandSourceStack src, ProfileStorage storage, String target) {
        String uuid = storage.getUuidByName(target);
        if (uuid == null) {
            uuid = normalizeUuid(target);
        }
        if (uuid == null || storage.getProfile(uuid) == null) {
            return fail(src, "Local profile not found: " + target);
        }
        storage.deleteProfile(uuid);
        send(src, Component.literal("Local profile removed.").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int reload(CommandSourceStack src) {
        ServerConfig.load();
        JInfoServerMod.LOGGER.info("JInfo server config reloaded");
        send(src, Component.literal("JInfo config reloaded.").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static void fetchAndShow(CommandSourceStack src, ProfileStorage storage, String uuid, String name) {
        send(src, Component.literal("Loading profile " + name + "...").withStyle(ChatFormatting.GRAY), false);
        CompletableFuture.runAsync(() -> {
            var remote = CentralApi.fetch(uuid);
            src.getServer().executeIfPossible(() -> {
                if (remote.isEmpty()) {
                    fail(src, "Profile not found: " + name);
                    return;
                }
                JsonObject source = remote.get();
                JsonObject profile = new JsonObject();
                profile.addProperty("uuid", uuid);
                profile.addProperty("name", source.has("name") ? source.get("name").getAsString() : name);
                profile.addProperty("data", source.has("data") ? source.get("data").getAsString() : "");
                if (source.has("skin") && !source.get("skin").isJsonNull()) {
                    profile.addProperty("skin", source.get("skin").getAsString());
                }
                show(src, profile.get("name").getAsString(), profile, "JInfo cloud");
            });
        });
    }

    private static void show(CommandSourceStack src, String name, JsonObject profile, String source) {
        String data = profile.has("data") ? profile.get("data").getAsString() : "";
        send(src, Component.literal(PREFIX + " — ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" (" + source + ")").withStyle(ChatFormatting.DARK_GRAY)), false);
        for (Component line : ProfileText.lines(data)) {
            send(src, line, false);
        }
    }

    private static UUID resolve(CommandSourceStack src, String target) {
        ServerPlayer online = src.getServer().getPlayerList().getPlayerByName(target);
        if (online != null) {
            return online.getUUID();
        }
        UUID parsed = parseUuid(target);
        if (parsed != null) {
            return parsed;
        }
        return offlineUuid(target);
    }

    private static UUID parseUuid(String value) {
        try {
            String normalized = normalizeUuid(value);
            return normalized == null ? null : UUID.fromString(normalized);
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalizeUuid(String value) {
        if (value == null) {
            return null;
        }
        String clean = value.replace("-", "");
        if (clean.length() != 32) {
            return null;
        }
        for (int i = 0; i < clean.length(); i++) {
            if (Character.digit(clean.charAt(i), 16) < 0) {
                return null;
            }
        }
        String lower = clean.toLowerCase(Locale.ROOT);
        return lower.substring(0, 8) + "-" + lower.substring(8, 12) + "-" + lower.substring(12, 16)
                + "-" + lower.substring(16, 20) + "-" + lower.substring(20, 32);
    }

    private static UUID offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static boolean validUrl(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            return host != null && !host.isBlank() && uri.getUserInfo() == null;
        } catch (Exception e) {
            return false;
        }
    }

    private static void send(CommandSourceStack src, Component component, boolean broadcast) {
        src.sendSuccess(() -> component, broadcast);
    }

    private static int fail(CommandSourceStack src, String message) {
        src.sendFailure(Component.literal(message).withStyle(ChatFormatting.RED));
        return 0;
    }
}
