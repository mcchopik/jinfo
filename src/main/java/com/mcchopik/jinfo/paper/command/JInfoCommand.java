package com.mcchopik.jinfo.paper.command;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mcchopik.jinfo.paper.JInfoPaperPlugin;
import com.mcchopik.jinfo.paper.http.HttpServerManager;
import com.mcchopik.jinfo.paper.profile.ProfileStorage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class JInfoCommand implements CommandExecutor, TabCompleter {

    private final JInfoPaperPlugin plugin;
    private final ProfileStorage storage;
    private final HttpServerManager http;
    private final Gson gson = new Gson();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public JInfoCommand(JInfoPaperPlugin plugin, ProfileStorage storage, HttpServerManager http) {
        this.plugin = plugin;
        this.storage = storage;
        this.http = http;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("jinfo.admin")) {
            sender.sendMessage("§cНет прав.");
            return true;
        }
        if (args.length == 0) {
            help(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadConfig();
                String host = plugin.getConfig().getString("host", "0.0.0.0");
                int port = plugin.getConfig().getInt("port", 8765);
                try {
                    http.start(host, port);
                    sender.sendMessage("§aJInfo перезагружен: §e" + host + ":" + port);
                } catch (Exception e) {
                    sender.sendMessage("§cНе удалось запустить веб-сервер: " + e.getMessage());
                }
            }
            case "list" -> {
                if (storage.all().isEmpty()) {
                    sender.sendMessage("§7Профилей нет.");
                    return true;
                }
                sender.sendMessage("§bПрофили JInfo:");
                storage.all().forEach(p -> sender.sendMessage("§7- §f"
                        + (p.has("name") ? p.get("name").getAsString() : "?")
                        + " §8(" + p.get("uuid").getAsString() + ")"));
            }
            case "profile" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cИспользование: /jinfo profile <ник>");
                    return true;
                }
                String name = args[1];
                String uuid = storage.uuidByName(name);
                if (uuid == null) {
                    uuid = offlineUuid(name);
                    if (storage.get(uuid) == null) {
                        sender.sendMessage("§cПрофиль не найден: " + name);
                        return true;
                    }
                }
                sender.sendMessage("§aПрофиль " + name + ": §b" + baseUrl() + "/p/" + uuid);
            }
            case "import" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cИспользование: /jinfo import <uuid> [ник]");
                    return true;
                }
                importProfile(sender, args[1], args.length > 2 ? args[2] : null);
            }
            case "remove" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cИспользование: /jinfo remove <uuid>");
                    return true;
                }
                boolean removed = storage.remove(args[1]);
                sender.sendMessage(removed ? "§aПрофиль удалён." : "§cПрофиль не найден.");
            }
            default -> help(sender);
        }
        return true;
    }

    private void importProfile(CommandSender sender, String uuid, String name) {
        sender.sendMessage("§7Загрузка профиля " + uuid + "...");
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String base = plugin.centralApiBase();
                if (base.endsWith("/")) {
                    base = base.substring(0, base.length() - 1);
                }
                HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/public/profile/" + uuid))
                        .timeout(Duration.ofSeconds(10))
                        .header("User-Agent", "jinfo-paper/0.2.0")
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> sender.sendMessage(
                            "§cОшибка загрузки: HTTP " + response.statusCode()));
                    return;
                }
                JsonObject source = gson.fromJson(response.body(), JsonObject.class);
                JsonObject profile = new JsonObject();
                profile.addProperty("uuid", uuid);
                profile.addProperty("name", name != null ? name : (source.has("name") ? source.get("name").getAsString() : "Unknown"));
                profile.addProperty("data", source.has("data") ? source.get("data").getAsString() : "");
                if (source.has("skin") && !source.get("skin").isJsonNull()) {
                    profile.addProperty("skin", source.get("skin").getAsString());
                }
                profile.addProperty("updated", source.has("updated") ? source.get("updated").getAsLong() : System.currentTimeMillis() / 1000L);
                storage.save(profile);
                String url = baseUrl() + "/p/" + uuid;
                plugin.getServer().getScheduler().runTask(plugin, () -> sender.sendMessage("§aИмпортировано: §b" + url));
            } catch (Exception e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> sender.sendMessage(
                        "§cОшибка: " + e.getMessage()));
            }
        });
    }

    private String baseUrl() {
        String host = plugin.getConfig().getString("public-url", "");
        if (host != null && !host.isBlank()) {
            return host.endsWith("/") ? host.substring(0, host.length() - 1) : host;
        }
        return "http://" + plugin.getConfig().getString("host", "localhost") + ":" + http.port();
    }

    private String offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private void help(CommandSender sender) {
        sender.sendMessage("§bJInfo Server §7— профили игроков без мода");
        sender.sendMessage("§f/jinfo reload §7— перезагрузить конфиг");
        sender.sendMessage("§f/jinfo list §7— список профилей");
        sender.sendMessage("§f/jinfo profile <ник> §7— ссылка на профиль");
        sender.sendMessage("§f/jinfo import <uuid> [ник] §7— импорт из облака");
        sender.sendMessage("§f/jinfo remove <uuid> §7— удалить профиль");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("jinfo.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            for (String option : List.of("reload", "list", "profile", "import", "remove")) {
                if (option.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(option);
                }
            }
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("profile")) {
            return storage.all().stream()
                    .filter(p -> p.has("name"))
                    .map(p -> p.get("name").getAsString())
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return List.of();
    }
}