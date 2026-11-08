package com.mcchopik.jinfo.net;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Определение UUID по нику.
 * Сначала пытаемся найти игрока в списке подключённых (рефлексия, best-effort),
 * иначе считаем offline-UUID (UUID v3 от "OfflinePlayer:<ник>") — так же, как сервер.
 */
public final class PlayerResolver {

    private PlayerResolver() {
    }

    public static String resolveUuid(String name, Object client) {
        Optional<UUID> online = findOnline(name, client);
        return online.map(UUID::toString).orElseGet(() -> offlineUuid(name));
    }

    public static String offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8))
                .toString();
    }

    private static Optional<UUID> findOnline(String name, Object client) {
        try {
            if (client == null) {
                return Optional.empty();
            }
            Object conn = invoke(client, "getConnection");
            if (conn == null) {
                conn = invoke(client, "getNetworkHandler");
            }
            if (conn == null) {
                return Optional.empty();
            }
            Object players = invoke(conn, "getOnlinePlayers");
            if (players == null) {
                players = invoke(conn, "getPlayerList");
            }
            if (!(players instanceof Iterable<?> it)) {
                return Optional.empty();
            }
            for (Object info : it) {
                if (info == null) {
                    continue;
                }
                Object profile = invoke(info, "getProfile");
                String pname = profile != null ? str(invoke(profile, "getName"))
                        : str(invoke(info, "getName"));
                if (pname == null || !pname.equalsIgnoreCase(name)) {
                    continue;
                }
                Object id = profile != null ? invoke(profile, "getId") : invoke(info, "getUuid");
                if (id != null) {
                    return Optional.of(UUID.fromString(id.toString()));
                }
            }
        } catch (Throwable ignored) {
        }
        return Optional.empty();
    }

    private static Object invoke(Object target, String method) {
        try {
            Method m = target.getClass().getMethod(method);
            m.setAccessible(true);
            return m.invoke(target);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}