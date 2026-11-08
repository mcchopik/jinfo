package com.mcchopik.jinfo.text;

import java.util.List;
import java.util.Map;

/**
 * Реестр поддерживаемых сервисов в одном месте: ключ, цвет, шаблон ссылки,
 * разрешённые домены и подписи для ru/uk/en.
 * Чтобы добавить сервис — добавь одну константу сюда.
 */
public enum Service {
    YOUTUBE(
            "youtube", "#FF0000", "https://youtube.com/@%s",
            List.of("youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be"),
            "Ютуб", "Ютуб", "YouTube"),
    TWITCH(
            "twitch", "#9146FF", "https://twitch.tv/%s",
            List.of("twitch.tv", "www.twitch.tv"),
            "Твич", "Твіч", "Twitch"),
    DISCORD(
            "discord", "#5865F2", "https://discord.gg/%s",
            List.of("discord.gg", "discord.com", "www.discord.com", "discordapp.com"),
            "Дискорд", "Діскорд", "Discord"),
    TELEGRAM(
            "telegram", "#229ED9", "https://t.me/%s",
            List.of("t.me", "telegram.me", "telegram.org", "www.telegram.org"),
            "Телеграм", "Телеграм", "Telegram"),
    TIKTOK(
            "tiktok", "#FE2C55", "https://tiktok.com/@%s",
            List.of("tiktok.com", "www.tiktok.com", "vm.tiktok.com"),
            "ТикТок", "ТікТок", "TikTok"),
    INSTAGRAM(
            "instagram", "#E1306C", "https://instagram.com/%s",
            List.of("instagram.com", "www.instagram.com"),
            "Инстаграм", "Інстаграм", "Instagram"),
    X(
            "x", "#000000", "https://x.com/%s",
            List.of("x.com", "www.x.com", "twitter.com", "www.twitter.com"),
            "X (Твиттер)", "X (Твіттер)", "X (Twitter)"),
    GITHUB(
            "github", "#6e5494", "https://github.com/%s",
            List.of("github.com", "www.github.com"),
            "GitHub", "GitHub", "GitHub"),
    STEAM(
            "steam", "#66C0F4", "https://steamcommunity.com/id/%s",
            List.of("steamcommunity.com", "www.steamcommunity.com", "store.steampowered.com"),
            "Steam", "Steam", "Steam"),
    VK(
            "vk", "#0077FF", "https://vk.com/%s",
            List.of("vk.com", "www.vk.com"),
            "ВКонтакте", "ВКонтакте", "VK"),
    REDDIT(
            "reddit", "#FF4500", "https://reddit.com/user/%s",
            List.of("reddit.com", "www.reddit.com", "old.reddit.com"),
            "Реддит", "Реддіт", "Reddit"),
    KICK(
            "kick", "#53FC18", "https://kick.com/%s",
            List.of("kick.com", "www.kick.com"),
            "Kick", "Kick", "Kick"),
    BOOSTY(
            "boosty", "#F15F2C", "https://boosty.to/%s",
            List.of("boosty.to", "www.boosty.to"),
            "Boosty", "Boosty", "Boosty"),
    DONATIONALERTS(
            "donationalerts", "#FF6B00", "https://www.donationalerts.com/r/%s",
            List.of("donationalerts.com", "www.donationalerts.com"),
            "DonationAlerts", "DonationAlerts", "DonationAlerts"),
    MODRINTH(
            "modrinth", "#1bd96a", "https://modrinth.com/user/%s",
            List.of("modrinth.com", "www.modrinth.com"),
            "Modrinth", "Modrinth", "Modrinth");

    private static final Map<String, Service> BY_KEY = buildIndex();

    private final String key;
    private final String colorHex;
    private final String urlTemplate;
    private final List<String> allowedDomains;
    private final String ru;
    private final String uk;
    private final String en;

    Service(String key, String colorHex, String urlTemplate, List<String> allowedDomains,
            String ru, String uk, String en) {
        this.key = key;
        this.colorHex = colorHex;
        this.urlTemplate = urlTemplate;
        this.allowedDomains = allowedDomains;
        this.ru = ru;
        this.uk = uk;
        this.en = en;
    }

    private static Map<String, Service> buildIndex() {
        Map<String, Service> map = new java.util.HashMap<>();
        for (Service s : values()) {
            map.put(s.key, s);
        }
        return Map.copyOf(map);
    }

    public String key() {
        return key;
    }

    public String colorHex() {
        return colorHex;
    }

    public String urlTemplate() {
        return urlTemplate;
    }

    public List<String> allowedDomains() {
        return allowedDomains;
    }

    public String label(Lang lang) {
        return switch (lang) {
            case RU -> ru;
            case UK -> uk;
            case EN -> en;
        };
    }

    public String buildUrl(String handle) {
        return String.format(urlTemplate, handle);
    }

    public static Service byKey(String key) {
        return BY_KEY.get(key == null ? null : key.toLowerCase());
    }
}