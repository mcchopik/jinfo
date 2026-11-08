package com.mcchopik.jinfo.net;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/**
 * Best-effort доступ к текущей сессии Minecraft через рефлексию.
 * Так мы не завязываемся на конкретные имена API (они отличаются между версиями)
 * и не выдумываем методы. Если что-то не найдено — возвращаем пусто,
 * и мод использует offline-авторизацию.
 */
public final class MinecraftSession {

    public record SessionInfo(String accessToken, String uuid, String username) {
    }

    private MinecraftSession() {
    }

    public static Optional<SessionInfo> current() {
        try {
            Object client = clientInstance();
            if (client == null) {
                return Optional.empty();
            }
            Object session = invoke(client, "getSession");
            if (session == null) {
                return Optional.empty();
            }
            String accessToken = asString(invoke(session, "getAccessToken"));
            String username = asString(invoke(session, "getUsername"));
            String uuid = asString(invoke(session, "getUuidOrNull"));
            if (uuid == null) {
                uuid = asString(invoke(session, "getProfileId"));
            }
            if (uuid == null) {
                uuid = asString(invoke(session, "getUuid"));
            }
            if (uuid != null) {
                uuid = uuid.replace("-", "");
            }
            if (accessToken == null || accessToken.isEmpty() || uuid == null || uuid.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new SessionInfo(accessToken, uuid, username));
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    /**
     * Отдельно достаёт только ник (нужен даже без токена, например в offline).
     */
    public static Optional<String> currentUsername() {
        Optional<SessionInfo> s = current();
        if (s.isPresent() && s.get().username() != null && !s.get().username().isEmpty()) {
            return Optional.of(s.get().username());
        }
        // Попробуем достать ник напрямую из сессии, даже если токен недоступен.
        try {
            Object client = clientInstance();
            if (client == null) {
                return Optional.empty();
            }
            Object session = invoke(client, "getSession");
            if (session == null) {
                return Optional.empty();
            }
            String username = asString(invoke(session, "getUsername"));
            return username == null || username.isEmpty() ? Optional.empty() : Optional.of(username);
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    public static Object clientInstance() {
        try {
            Class<?> clientClass = Class.forName("net.minecraft.client.Minecraft");
            return clientClass.getMethod("getInstance").invoke(null);
        } catch (Throwable t) {
            return null;
        }
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

    private static String asString(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof UUID uuid) {
            return uuid.toString();
        }
        return o.toString();
    }
}