package com.mcchopik.jinfo.text;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Проверка безопасности ссылок. Зависимостей от Minecraft нет.
 */
public final class LinkValidator {

    private static final Pattern IPV4 = Pattern.compile(
            "^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");

    private static final Set<String> SHORTENERS = Set.of(
            "bit.ly", "tinyurl.com", "t.co", "goo.gl", "is.gd", "ow.ly",
            "buff.ly", "cutt.ly", "rb.gy", "shorturl.at", "v.gd", "rebrand.ly",
            "clck.ru", "vk.cc", "u.to");

    private LinkValidator() {
    }

    /**
     * Результат проверки ссылки.
     */
    public record Result(boolean valid, List<String> warnings) {
        public static Result invalid() {
            return new Result(false, List.of());
        }

        public static Result ok(List<String> warnings) {
            return new Result(true, List.copyOf(warnings));
        }
    }

    /**
     * Проверка ссылки для встроенного сервиса: домен обязан совпадать со списком.
     */
    public static Result validateService(String url, Service service) {
        URI uri = parseHttps(url);
        if (uri == null) {
            return Result.invalid();
        }
        String host = normalizeHost(uri.getHost());
        if (host == null || !isAllowedDomain(host, service.allowedDomains())) {
            return Result.invalid();
        }
        return Result.ok(List.of());
    }

    /**
     * Проверка произвольной ссылки {@code {link:...}}: любой https-домен,
     * но с предупреждениями для сокращателей, punycode и IP.
     */
    public static Result validateGeneric(String url) {
        URI uri = parseHttps(url);
        if (uri == null) {
            return Result.invalid();
        }
        String host = normalizeHost(uri.getHost());
        if (host == null) {
            return Result.invalid();
        }
        List<String> warnings = new ArrayList<>();
        if (SHORTENERS.contains(host)) {
            warnings.add("shortener");
        }
        if (host.contains("xn--")) {
            warnings.add("punycode");
        }
        if (isIp(host)) {
            warnings.add("ip");
        }
        return Result.ok(warnings);
    }

    /**
     * Приводит ввод к https URI или возвращает null, если ссылка недопустима:
     * только https, без логина/пароля, без localhost и IP, без опасных схем.
     */
    private static URI parseHttps(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("javascript:") || lower.startsWith("file:")
                || lower.startsWith("data:") || lower.startsWith("ftp:")
                || lower.startsWith("mailto:")) {
            return null;
        }
        if (!lower.startsWith("https://")) {
            return null;
        }
        URI uri;
        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException e) {
            return null;
        }
        if (uri.getScheme() == null || !uri.getScheme().equalsIgnoreCase("https")) {
            return null;
        }
        if (uri.getUserInfo() != null) {
            return null;
        }
        String host = normalizeHost(uri.getHost());
        if (host == null) {
            return null;
        }
        if (isLocalhost(host) || isIp(host)) {
            return null;
        }
        return uri;
    }

    private static String normalizeHost(String host) {
        if (host == null) {
            return null;
        }
        String h = host.toLowerCase(Locale.ROOT);
        while (h.endsWith(".")) {
            h = h.substring(0, h.length() - 1);
        }
        return h.isEmpty() ? null : h;
    }

    private static boolean isAllowedDomain(String host, List<String> allowed) {
        for (String domain : allowed) {
            String d = domain.toLowerCase(Locale.ROOT);
            if (host.equals(d) || host.endsWith("." + d)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLocalhost(String host) {
        return host.equals("localhost") || host.equals("127.0.0.1")
                || host.equals("::1") || host.equals("[::1]") || host.endsWith(".localhost");
    }

    private static boolean isIp(String host) {
        if (IPV4.matcher(host).matches()) {
            return true;
        }
        // IPv6 в квадратных скобках или с двоеточиями
        return host.contains(":") || (host.startsWith("[") && host.endsWith("]"));
    }
}