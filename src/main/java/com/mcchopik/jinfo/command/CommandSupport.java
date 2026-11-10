package com.mcchopik.jinfo.command;

import com.mcchopik.jinfo.JInfoClient;
import com.mcchopik.jinfo.net.ApiException;
import com.mcchopik.jinfo.text.Messages;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.Locale;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class CommandSupport {

    private static final Pattern UUID_DASHED = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern UUID_PLAIN = Pattern.compile("[0-9a-fA-F]{32}");

    private CommandSupport() {
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> lit(String name) {
        return LiteralArgumentBuilder.<FabricClientCommandSource>literal(name);
    }

    public static RequiredArgumentBuilder<FabricClientCommandSource, String> arg(
            String name, StringArgumentType type) {
        return RequiredArgumentBuilder.<FabricClientCommandSource, String>argument(name, type);
    }

    public static void reply(FabricClientCommandSource src, String text) {
        Minecraft client = src.getClient();
        client.execute(() -> src.sendFeedback(Component.literal(text)));
    }

    public static String describe(ApiException e) {
        return switch (e.statusCode()) {
            case 429 -> Messages.s("err_rate");
            case 401 -> Messages.s("err_auth");
            case 404 -> Messages.s("err_not_filled");
            default -> Messages.s("err_generic") + " (" + e.statusCode() + "): " + e.getMessage();
        };
    }

    public static String selfName() {
        return JInfoClient.selfName();
    }

    public static boolean looksLikeUuid(String value) {
        return UUID_DASHED.matcher(value).matches() || UUID_PLAIN.matcher(value).matches();
    }

    public static String normalizeUuid(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains("-")) {
            return lower;
        }
        return lower.substring(0, 8) + "-" + lower.substring(8, 12) + "-" + lower.substring(12, 16)
                + "-" + lower.substring(16, 20) + "-" + lower.substring(20, 32);
    }
}
