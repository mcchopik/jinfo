package com.mcchopik.jinfo.net;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

/**
 * Best-effort доступ к текущей сессии Minecraft через рефлексию.
 * Так мы не завязываемся на конкретные имена API (которые могут отличаться
 * между версиями) и не выдумываем методы. Если что-то не найдено — вернём пусто,
 * и мод использует offline-авторизацию.
 */
public final class MinecraftSession {

    public record SessionInfo(String accessToken, String uuid) {
    }

    private MinecraftSession() {
    }

    public static Optional<SessionInfo> current() {
        try {
            Class<?> clientClass = Class.forName("net.minecraft.client.MinecraftClient");
            Object client = clientClass.getMethod("getInstance").invoke(null);
            if (client == null) {
                return Optional.empty();
            }
            Object session = invoke(client, "getSession");
            if (session == null) {
                return Optional.empty();
            }
            String accessToken = asString(invoke(session, "getAccessToken"));
            Object uuidObj = invoke(session, "getUuidOrNull");
            String uuid = uuidObj == null ? null : uuidObj.toString();
            if (uuid == null) {
                uuid = asString(invoke(session, "getUuid"));
            }
            if (uuid != null) {
                uuid = uuid.replace("-", "");
            }
            if (accessToken == null || accessToken.isEmpty() || uuid == null || uuid.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new SessionInfo(accessToken, uuid));
        } catch (Throwable t) {
            return Optional.empty();
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