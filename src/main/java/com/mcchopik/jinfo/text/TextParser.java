package com.mcchopik.jinfo.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TextParser {

    private static final String[] COLOR_CODES = {
            "#000000", "#0000AA", "#00AA00", "#00AAAA",
            "#AA0000", "#AA00AA", "#FFAA00", "#AAAAAA",
            "#555555", "#5555FF", "#55FF55", "#55FFFF",
            "#FF5555", "#FF55FF", "#FFFF55", "#FFFFFF"
    };

    public ParsedProfile parse(String raw, Lang lang) {
        Lang language = lang == null ? Lang.RU : lang;
        String text = sanitize(raw);

        List<Part> parts = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        Style style = Style.DEFAULT;
        int linkCount = 0;
        String avatarUrl = null;
        int i = 0;
        int len = text.length();

        while (i < len) {
            char c = text.charAt(i);

            if ((c == '&' || c == '\u00A7') && i + 1 < len) {
                FormatResult fmt = tryApplyFormatting(text, i, style);
                if (fmt != null) {
                    flush(buffer, style, parts);
                    style = fmt.style();
                    i += fmt.consumed();
                    continue;
                }
            }

            if (c == '{') {
                PlaceholderResult ph = tryParsePlaceholder(text, i, language);
                if (ph != null) {
                    flush(buffer, style, parts);
                    if (ph.validLink != null) {
                        parts.add(ph.validLink);
                        linkCount++;
                    } else if (ph.avatarUrl != null) {
                        if (avatarUrl == null) {
                            avatarUrl = ph.avatarUrl;
                        }
                    } else if (ph.literal != null) {
                        parts.add(new Part.Raw(ph.literal));
                    }
                    i = ph.nextIndex;
                    continue;
                }
            }

            buffer.append(c);
            i++;
        }
        flush(buffer, style, parts);

        String plain = plainText(parts);
        int lineCount = 1;
        for (int k = 0; k < text.length(); k++) {
            if (text.charAt(k) == '\n') {
                lineCount++;
            }
        }

        boolean overLimits = text.length() > ParsedProfile.MAX_CHARS
                || lineCount > ParsedProfile.MAX_LINES
                || linkCount > ParsedProfile.MAX_LINKS;
        if (text.length() > ParsedProfile.MAX_CHARS) {
            warnings.add("too_long");
        }
        if (lineCount > ParsedProfile.MAX_LINES) {
            warnings.add("too_many_lines");
        }
        if (linkCount > ParsedProfile.MAX_LINKS) {
            warnings.add("too_many_links");
        }

        return new ParsedProfile(List.copyOf(parts), List.copyOf(warnings),
                linkCount, text.length(), lineCount, plain, overLimits, avatarUrl);
    }

    private record FormatResult(int consumed, Style style) {
    }

    private FormatResult tryApplyFormatting(String text, int i, Style current) {
        char n = Character.toLowerCase(text.charAt(i + 1));

        if (n == '#' && i + 7 < text.length()) {
            String hex = text.substring(i + 2, i + 8);
            if (isHex(hex)) {
                return new FormatResult(8, current.withColor("#" + hex.toUpperCase(Locale.ROOT)));
            }
        }

        if ((n >= '0' && n <= '9') || (n >= 'a' && n <= 'f')) {
            int idx = Character.digit(n, 16);
            return new FormatResult(2, current.withColor(COLOR_CODES[idx]));
        }
        switch (n) {
            case 'l' -> {
                return new FormatResult(2, current.withBold(true));
            }
            case 'o' -> {
                return new FormatResult(2, current.withItalic(true));
            }
            case 'n' -> {
                return new FormatResult(2, current.withUnderline(true));
            }
            case 'm' -> {
                return new FormatResult(2, current.withStrikethrough(true));
            }
            case 'r' -> {
                return new FormatResult(2, Style.DEFAULT);
            }
            case 'k' -> {

                return new FormatResult(2, current);
            }
            default -> {
                return null;
            }
        }
    }

    private record PlaceholderResult(Part.Link validLink, String literal, String avatarUrl, int nextIndex) {
    }

    private PlaceholderResult tryParsePlaceholder(String text, int i, Lang lang) {
        int close = text.indexOf('}', i + 1);
        if (close < 0) {
            return null;
        }

        if (close - i > 80) {
            return null;
        }
        String content = text.substring(i + 1, close).trim();
        if (content.isEmpty()) {
            return null;
        }

        String lower = content.toLowerCase(Locale.ROOT);
        boolean universal = lower.startsWith("link:");
        boolean avatar = lower.equals("avatar") || lower.equals("photo");
        Service service = universal || avatar ? null : Service.byKey(content);
        if (!universal && !avatar && service == null) {
            return null;
        }

        int argStart = close + 1;
        int p = argStart;
        while (p < text.length() && (text.charAt(p) == ' ' || text.charAt(p) == '\t')) {
            p++;
        }
        int tokenStart = p;
        while (p < text.length() && !Character.isWhitespace(text.charAt(p))) {
            p++;
        }
        String arg = text.substring(tokenStart, p);
        if (arg.isEmpty()) {
            return new PlaceholderResult(null, text.substring(i, close + 1), null, close + 1);
        }

        if (avatar) {
            if (!arg.toLowerCase(Locale.ROOT).startsWith("https://")) {
                return new PlaceholderResult(null, text.substring(i, p), null, p);
            }
            LinkValidator.Result res = LinkValidator.validateGeneric(arg);
            if (!res.valid()) {
                return new PlaceholderResult(null, text.substring(i, p), null, p);
            }
            int end = p;
            while (end < text.length() && (text.charAt(end) == ' ' || text.charAt(end) == '\t')) {
                end++;
            }
            if (end < text.length() && text.charAt(end) == '\n') {
                end++;
            }
            return new PlaceholderResult(null, null, arg, end);
        }

        if (universal) {
            String title = content.substring(5).trim();
            if (title.isEmpty()) {
                title = "Link";
            }
            if (!arg.toLowerCase(Locale.ROOT).startsWith("https://")) {
                return new PlaceholderResult(null, text.substring(i, p), null, p);
            }
            LinkValidator.Result res = LinkValidator.validateGeneric(arg);
            if (!res.valid()) {
                return new PlaceholderResult(null, text.substring(i, p), null, p);
            }
            Part.Link link = new Part.Link("link", title, "#4FC3F7", arg, arg, false, res.warnings());
            return new PlaceholderResult(link, null, null, p);
        }

        String url;
        String argLower = arg.toLowerCase(Locale.ROOT);
        if (argLower.startsWith("https://") || argLower.startsWith("http://")) {
            url = arg;
        } else {
            String handle = arg.startsWith("@") ? arg.substring(1) : arg;
            if (!isHandle(handle)) {
                return new PlaceholderResult(null, text.substring(i, p), null, p);
            }
            url = service.buildUrl(handle);
        }
        LinkValidator.Result res = LinkValidator.validateService(url, service);
        if (!res.valid()) {
            return new PlaceholderResult(null, text.substring(i, p), null, p);
        }
        Part.Link link = new Part.Link(service.key(), service.label(lang), service.colorHex(),
                url, arg, true, List.of());
        return new PlaceholderResult(link, null, null, p);
    }

    private static void flush(StringBuilder buffer, Style style, List<Part> parts) {
        if (buffer.length() == 0) {
            return;
        }
        parts.add(new Part.Text(buffer.toString(), style));
        buffer.setLength(0);
    }

    private static String plainText(List<Part> parts) {
        StringBuilder sb = new StringBuilder();
        for (Part part : parts) {
            if (part instanceof Part.Text t) {
                sb.append(t.text());
            } else if (part instanceof Part.Link l) {
                sb.append(l.label()).append(": ").append(l.displayText());
            } else if (part instanceof Part.Raw r) {
                sb.append(r.text());
            }
        }
        return sb.toString();
    }

    public static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.replace("\r\n", "\n").replace('\r', '\n');
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n' || c == '\t') {
                out.append(c == '\t' ? ' ' : c);
                continue;
            }
            if (c < 0x20 || c == 0x7F) {
                continue;
            }
            if (c >= '\u200B' && c <= '\u200D') {
                continue;
            }
            if (c == '\uFEFF' || c == '\u2060') {
                continue;
            }
            if (c >= '\u202A' && c <= '\u202E') {
                continue;
            }
            if (c >= '\u2066' && c <= '\u2069') {
                continue;
            }
            if (c == '\u200E' || c == '\u200F') {
                continue;
            }
            out.append(c);
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
            boolean ok = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9') || c == '.' || c == '_' || c == '-';
            if (!ok) {
                return false;
            }
        }
        return true;
    }
}
