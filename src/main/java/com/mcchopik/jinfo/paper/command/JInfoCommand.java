package com.mcchopik.jinfo.paper.command;

import com.google.gson.JsonObject;
import com.mcchopik.jinfo.paper.JInfoPaperPlugin;
import com.mcchopik.jinfo.paper.api.CentralApi;
import com.mcchopik.jinfo.paper.profile.ProfileStorage;
import com.mcchopik.jinfo.paper.text.ProfileText;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class JInfoCommand implements CommandExecutor, TabCompleter {

    private final JInfoPaperPlugin plugin;
    private final ProfileStorage storage;

    public JInfoCommand(JInfoPaperPlugin plugin, ProfileStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("jinfo.use")) {
            fail(sender, "No permission.");
            return true;
        }
        if (args.length == 0) {
            help(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help" -> help(sender);
            case "me" -> me(sender);
            case "view" -> {
                if (args.length < 2) {
                    fail(sender, "Usage: /jinfo view <name|uuid>");
                    return true;
                }
                view(sender, args[1]);
            }
            case "set" -> {
                if (args.length < 2) {
                    fail(sender, "Usage: /jinfo set <text>");
                    return true;
                }
                set(sender, join(args, 1));
            }
            case "clear" -> clear(sender);
            case "photo" -> {
                if (args.length < 2) {
                    fail(sender, "Usage: /jinfo photo <url|off>");
                    return true;
                }
                photo(sender, join(args, 1));
            }
            case "list" -> {
                if (admin(sender)) {
                    list(sender);
                }
            }
            case "import" -> {
                if (admin(sender)) {
                    if (args.length < 2) {
                        fail(sender, "Usage: /jinfo import <uuid> [name]");
                    } else {
                        importProfile(sender, args[1], args.length > 2 ? args[2] : null);
                    }
                }
            }
            case "remove" -> {
                if (admin(sender)) {
                    if (args.length < 2) {
                        fail(sender, "Usage: /jinfo remove <name|uuid>");
                    } else {
                        remove(sender, args[1]);
                    }
                }
            }
            case "reload" -> {
                if (admin(sender)) {
                    plugin.reloadConfig();
                    success(sender, "JInfo config reloaded.");
                }
            }
            default -> help(sender);
        }
        return true;
    }

    private boolean admin(CommandSender sender) {
        if (!sender.hasPermission("jinfo.admin")) {
            fail(sender, "No permission.");
            return false;
        }
        return true;
    }

    private void me(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            fail(sender, "This command can only be used by a player.");
            return;
        }
        String uuid = player.getUniqueId().toString();
        String name = player.getName();
        JsonObject local = storage.get(uuid);
        if (local != null) {
            show(sender, name, local, "server");
            return;
        }
        fetchAndShow(sender, uuid, name);
    }

    private void view(CommandSender sender, String target) {
        String localUuid = storage.uuidByName(target);
        if (localUuid != null) {
            JsonObject local = storage.get(localUuid);
            if (local != null) {
                show(sender, target, local, "server");
                return;
            }
        }
        UUID uuid = resolve(target);
        if (uuid == null) {
            fail(sender, "Player not found: " + target);
            return;
        }
        JsonObject cached = storage.get(uuid.toString());
        if (cached != null) {
            show(sender, target, cached, "server");
            return;
        }
        fetchAndShow(sender, uuid.toString(), target);
    }

    private void set(CommandSender sender, String text) {
        if (!(sender instanceof Player player)) {
            fail(sender, "This command can only be used by a player.");
            return;
        }
        String error = ProfileText.validate(text);
        if (error != null) {
            fail(sender, error);
            return;
        }
        JsonObject profile = new JsonObject();
        profile.addProperty("uuid", player.getUniqueId().toString());
        profile.addProperty("name", player.getName());
        profile.addProperty("data", text);
        profile.addProperty("updated", Instant.now().getEpochSecond());
        storage.save(profile);
        success(sender, "Profile saved. Others can view it with /jinfo view " + player.getName());
    }

    private void clear(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            fail(sender, "This command can only be used by a player.");
            return;
        }
        storage.remove(player.getUniqueId().toString());
        success(sender, "Server profile removed.");
    }

    private void photo(CommandSender sender, String raw) {
        if (!(sender instanceof Player player)) {
            fail(sender, "This command can only be used by a player.");
            return;
        }
        String url = raw == null ? "" : raw.trim();
        boolean remove = url.isEmpty() || url.equalsIgnoreCase("off")
                || url.equalsIgnoreCase("none") || url.equalsIgnoreCase("remove") || url.equals("-");
        if (!remove && (!url.toLowerCase(Locale.ROOT).startsWith("https://") || !validUrl(url))) {
            fail(sender, "A direct https image link is required.");
            return;
        }
        String uuid = player.getUniqueId().toString();
        JsonObject profile = storage.get(uuid);
        if (profile == null) {
            profile = new JsonObject();
            profile.addProperty("uuid", uuid);
            profile.addProperty("name", player.getName());
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
            fail(sender, error);
            return;
        }
        profile.addProperty("data", text);
        profile.addProperty("updated", Instant.now().getEpochSecond());
        storage.save(profile);
        success(sender, remove ? "Profile photo removed." : "Profile photo updated.");
    }

    private void list(CommandSender sender) {
        if (storage.all().isEmpty()) {
            sender.sendMessage(Component.text("No local profiles.").color(NamedTextColor.GRAY));
            return;
        }
        sender.sendMessage(Component.text("Local JInfo profiles:").color(NamedTextColor.AQUA));
        for (JsonObject profile : storage.all()) {
            String name = profile.has("name") ? profile.get("name").getAsString() : "?";
            String uuid = profile.has("uuid") ? profile.get("uuid").getAsString() : "?";
            sender.sendMessage(Component.text("- " + name + " (" + uuid + ")").color(NamedTextColor.GRAY));
        }
    }

    private void importProfile(CommandSender sender, String rawUuid, String name) {
        String uuid = normalizeUuid(rawUuid);
        if (uuid == null) {
            fail(sender, "Invalid UUID: " + rawUuid);
            return;
        }
        sender.sendMessage(Component.text("Importing profile " + uuid + "...").color(NamedTextColor.GRAY));
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Optional<JsonObject> remote = CentralApi.fetch(plugin.centralApiBase(), uuid);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (remote.isEmpty()) {
                    fail(sender, "Profile not found in JInfo cloud.");
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
                storage.save(profile);
                success(sender, "Imported profile " + profile.get("name").getAsString());
            });
        });
    }

    private void remove(CommandSender sender, String target) {
        String uuid = storage.uuidByName(target);
        if (uuid == null) {
            uuid = normalizeUuid(target);
        }
        if (uuid == null || storage.get(uuid) == null) {
            fail(sender, "Local profile not found: " + target);
            return;
        }
        storage.remove(uuid);
        success(sender, "Local profile removed.");
    }

    private void fetchAndShow(CommandSender sender, String uuid, String name) {
        sender.sendMessage(Component.text("Loading profile " + name + "...").color(NamedTextColor.GRAY));
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Optional<JsonObject> remote = CentralApi.fetch(plugin.centralApiBase(), uuid);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (remote.isEmpty()) {
                    fail(sender, "Profile not found: " + name);
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
                show(sender, profile.get("name").getAsString(), profile, "JInfo cloud");
            });
        });
    }

    private void show(CommandSender sender, String name, JsonObject profile, String source) {
        String data = profile.has("data") ? profile.get("data").getAsString() : "";
        sender.sendMessage(Component.text("JInfo — ").color(NamedTextColor.AQUA)
                .append(Component.text(name).color(NamedTextColor.WHITE))
                .append(Component.text(" (" + source + ")").color(NamedTextColor.DARK_GRAY)));
        for (Component line : ProfileText.lines(data)) {
            sender.sendMessage(line);
        }
    }

    private UUID resolve(String target) {
        Player online = plugin.getServer().getPlayerExact(target);
        if (online != null) {
            return online.getUniqueId();
        }
        UUID parsed = parseUuid(target);
        if (parsed != null) {
            return parsed;
        }
        return offlineUuid(target);
    }

    private UUID parseUuid(String value) {
        try {
            String normalized = normalizeUuid(value);
            return normalized == null ? null : UUID.fromString(normalized);
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeUuid(String value) {
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

    private UUID offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    private boolean validUrl(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            return host != null && !host.isBlank() && uri.getUserInfo() == null;
        } catch (Exception e) {
            return false;
        }
    }

    private String join(String[] args, int start) {
        StringBuilder out = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(args[i]);
        }
        return out.toString();
    }

    private void success(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message).color(NamedTextColor.GREEN));
    }

    private void fail(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message).color(NamedTextColor.RED));
    }

    private void help(CommandSender sender) {
        sender.sendMessage(Component.text("JInfo").color(NamedTextColor.AQUA));
        sender.sendMessage(Component.text("/jinfo me — my profile"));
        sender.sendMessage(Component.text("/jinfo view <name|uuid> — view a profile"));
        sender.sendMessage(Component.text("/jinfo set <text> — save my server profile"));
        sender.sendMessage(Component.text("/jinfo photo <url|off> — set or remove my photo"));
        sender.sendMessage(Component.text("/jinfo clear — remove my server profile"));
        if (sender.hasPermission("jinfo.admin")) {
            sender.sendMessage(Component.text("/jinfo list — local profiles"));
            sender.sendMessage(Component.text("/jinfo import <uuid> [name] — import from cloud"));
            sender.sendMessage(Component.text("/jinfo remove <name|uuid> — remove local profile"));
            sender.sendMessage(Component.text("/jinfo reload — reload config"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("jinfo.use")) {
            return List.of();
        }
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            List<String> base = sender.hasPermission("jinfo.admin")
                    ? List.of("help", "me", "view", "set", "photo", "clear", "list", "import", "remove", "reload")
                    : List.of("help", "me", "view", "set", "photo", "clear");
            for (String option : base) {
                if (option.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(option);
                }
            }
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("view")) {
            return storage.all().stream()
                    .filter(p -> p.has("name"))
                    .map(p -> p.get("name").getAsString())
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return List.of();
    }
}
