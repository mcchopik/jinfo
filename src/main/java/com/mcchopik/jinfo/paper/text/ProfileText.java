package com.mcchopik.jinfo.paper.text;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class ProfileText {

    private static final int MAX_CHARS = 500;
    private static final int MAX_LINES = 10;
    private static final int MAX_LINKS = 6;
    private static final int LINK_COLOR = 0x4FC3F7;
    private static final Map<String, String> SERVICES = Map.ofEntries(
            Map.entry("youtube", "https://youtube.com/@%s"),
            Map.entry("twitch", "https://twitch.tv/%s"),
            Map.entry("discord", "https://discord.gg/%s"),
            Map.entry("telegram", "https://t.me/%s"),
            Map.entry("tiktok", "https://tiktok.com/@%s"),
            Map.entry("instagram", "https://instagram.com/%s"),
            Map.entry("x", "https://x.com/%s"),
            Map.entry("github", "https://github.com/%s"),
            Map.entry("steam", "https://steamcommunity.com/id/%s"),
            Map.entry("vk", "https://vk.com/%s"),
            Map.entry("reddit", "https://reddit.com/user/%s"),
            Map.entry("kick", "https://kick.com/%s"),
            Map.entry("boosty", "https://boosty.to/%s"),
            Map.entry("donationalerts", "https://www.donationalerts.com/r/%s"),
            Map.entry("modrinth", "https://modrinth.com/user/%s"));

    private ProfileText() {
    }

    public static List<Component> lines(String raw) {
        List<Component> out = new ArrayList<>();
        String safe = sanitize(raw);
        for (String line : safe.split("\n", -1)) {
            out.add(renderLine(line));
        }
        return out;
    }

    public static String validate(String raw) {
        String safe = sanitize(raw);
        if (safe.length() > MAX_CHARS) {
            return "Profile is too long (max " + MAX_CHARS + " characters).";
        }
        if (safe.split("\n", -1).length > MAX_LINES) {
            return "Profile has too many lines (max " + MAX_LINES + ").";
        }
        int links = 0;
        int i = 0;
        while (i < safe.length()) {
            int open = safe.indexOf('{', i);
            if (open < 0) {
                break;
            }
            int close = safe.indexOf('}', open + 1);
            if (close < 0) {
                break;
            }
            String key = safe.substring(open + 1, close).trim().toLowerCase(Locale.ROOT);
            if (key.startsWith("link:") || SERVICES.containsKey(key)
                    || key.equals("photo") || key.equals("avatar")) {
                links++;
            }
            i = close + 1;
        }
        if (links > MAX_LINKS) {
            return "Profile has too many links (max " + MAX_LINKS + ").";
        }
        return null;
    }

    private static Component renderLine(String line) {
        Component out = Component.empty();
        StringBuilder buffer = new StringBuilder();
        Style style = new Style();
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if ((c == '&' || c == '\u00A7') && i + 1 < line.length()) {
                Format format = format(line, i, style);
                if (format != null) {
                    out = out.append(flush(buffer, style));
                    style = format.style();
                    i += format.consumed();
                    continue;
                }
            }
            if (c == '{') {
                Placeholder placeholder = placeholder(line, i, style);
                if (placeholder != null) {
                    out = out.append(flush(buffer, style));
                    out = out.append(placeholder.component());
                    i = placeholder.nextIndex();
                    continue;
                }
            }
            buffer.append(c);
            i++;
        }
        return out.append(flush(buffer, style));
    }

    private static Component flush(StringBuilder buffer, Style style) {
        if (buffer.length() == 0) {
            return Component.empty();
        }
        Component component = Component.text(buffer.toString());
        if (style.color != null) {
            component = component.color(style.color);
        }
        if (style.bold) {
            component = component.decorate(TextDecoration.BOLD);
        }
        if (style.italic) {
            component = component.decorate(TextDecoration.ITALIC);
        }
        if (style.underline) {
            component = component.decorate(TextDecoration.UNDERLINED);
        }
        if (style.strike) {
            component = component.decorate(TextDecoration.STRIKETHROUGH);
        }
        buffer.setLength(0);
        return component;
    }

    private static final class Style {
        TextColor color;
        boolean bold;
        boolean italic;
        boolean underline;
        boolean strike;

        Style copy() {
            Style copy = new Style();
            copy.color = color;
            copy.bold = bold;
            copy.italic = italic;
            copy.underline = underline;
            copy.strike = strike;
            return copy;
        }
    }

    private record Format(int consumed, Style style) {
    }

    private static Format format(String text, int i, Style current) {
        char n = Character.toLowerCase(text.charAt(i + 1));
        if (n == '#' && i + 7 < text.length()) {
            String hex = text.substring(i + 2, i + 8);
            if (isHex(hex)) {
                Style style = current.copy();
                style.color = TextColor.color(Integer.parseInt(hex, 16));
                return new Format(8, style);
            }
        }
        if ((n >= '0' && n <= '9') || (n >= 'a' && n <= 'f')) {
            Style style = current.copy();
            style.color = legacy(n);
            return new Format(2, style);
        }
        Style style = current.copy();
        switch (n) {
            case 'l' -> style.bold = true;
            case 'o' -> style.italic = true;
            case 'n' -> style.underline = true;
            case 'm' -> style.strike = true;
            case 'r' -> style = new Style();
            case 'k' -> {
            }
            default -> {
                return null;
            }
        }
        return new Format(2, style);
    }

    private static TextColor legacy(char c) {
        return switch (c) {
            case '0' -> TextColor.color(0x000000);
            case '1' -> TextColor.color(0x0000AA);
            case '2' -> TextColor.color(0x00AA00);
            case '3' -> TextColor.color(0x00AAAA);
            case '4' -> TextColor.color(0xAA0000);
            case '5' -> TextColor.color(0xAA00AA);
            case '6' -> TextColor.color(0xFFAA00);
            case '7' -> TextColor.color(0xAAAAAA);
            case '8' -> TextColor.color(0x555555);
            case '9' -> TextColor.color(0x5555FF);
            case 'a' -> TextColor.color(0x55FF55);
            case 'b' -> TextColor.color(0x55FFFF);
            case 'c' -> TextColor.color(0xFF5555);
            case 'd' -> TextColor.color(0xFF55FF);
            case 'e' -> TextColor.color(0xFFFF55);
            default -> TextColor.color(0xFFFFFF);
        };
    }

    private record Placeholder(Component component, int nextIndex) {
    }

    private static Placeholder placeholder(String text, int i, Style style) {
        int close = text.indexOf('}', i + 1);
        if (close < 0 || close - i > 120) {
            return null;
        }
        String content = text.substring(i + 1, close).trim();
        if (content.isEmpty()) {
            return null;
        }
        String lower = content.toLowerCase(Locale.ROOT);
        boolean universal = lower.startsWith("link:");
        boolean photo = lower.equals("photo") || lower.equals("avatar");
        String service = SERVICES.containsKey(lower) ? lower : null;
        if (!universal && !photo && service == null) {
            return null;
        }
        int p = close + 1;
        while (p < text.length() && (text.charAt(p) == ' ' || text.charAt(p) == '\t')) {
            p++;
        }
        int start = p;
        while (p < text.length() && !Character.isWhitespace(text.charAt(p))) {
            p++;
        }
        String arg = text.substring(start, p);
        if (arg.isEmpty()) {
            return null;
        }
        String url;
        String label;
        if (universal) {
            label = content.substring(5).trim();
            if (label.isEmpty()) {
                label = "Link";
            }
            url = arg;
        } else if (photo) {
            label = "Photo";
            url = arg;
        } else {
            label = service;
            if (arg.startsWith("http://") || arg.startsWith("https://")) {
                url = arg;
            } else {
                String handle = arg.startsWith("@") ? arg.substring(1) : arg;
                if (!isHandle(handle)) {
                    return null;
                }
                url = String.format(SERVICES.get(service), handle);
            }
        }
        if (!url.toLowerCase(Locale.ROOT).startsWith("https://")) {
            return null;
        }
        Component link = Component.text("[" + label + "]")
                .color(TextColor.color(LINK_COLOR))
                .decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.openUrl(URI.create(url)))
                .hoverEvent(HoverEvent.showText(Component.text(url)));
        return new Placeholder(link, p);
    }

    private static boolean isHex(String value) {
        if (value.length() != 6) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = Character.toLowerCase(value.charAt(i));
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHandle(String handle) {
        if (handle.isEmpty() || handle.length() > 64) {
            return false;
        }
        for (int i = 0; i < handle.length(); i++) {
            char c = handle.charAt(i);
            if (!((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9') || c == '.' || c == '_' || c == '-')) {
                return false;
            }
        }
        return true;
    }

    public static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.replace("\r\n", "\n").replace('\r', '\n');
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n') {
                out.append(c);
            } else if (c == '\t') {
                out.append(' ');
            } else if (c < 0x20 || c == 0x7F) {
                continue;
            } else if (c >= 0x200B && c <= 0x200D) {
                continue;
            } else if (c == 0xFEFF || c == 0x2060) {
                continue;
            } else if (c >= 0x202A && c <= 0x202E) {
                continue;
            } else if (c >= 0x2066 && c <= 0x2069) {
                continue;
            } else if (c == 0x200E || c == 0x200F) {
                continue;
            } else {
                out.append(c);
            }
        }
        StringBuilder collapsed = new StringBuilder(out.length());
        char last = 0;
        int run = 0;
        for (int i = 0; i < out.length(); i++) {
            char c = out.charAt(i);
            if (c == last) {
                run++;
            } else {
                last = c;
                run = 1;
            }
            if (run <= 4) {
                collapsed.append(c);
            }
        }
        return collapsed.toString();
    }
}
