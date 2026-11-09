package com.mcchopik.jinfo.net;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

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
            String accessToken = null;
            String username = null;
            String uuid = null;
            Object user = invoke(client, "getUser");
            if (user != null) {
                accessToken = asString(invoke(user, "getAccessToken"));
                username = asString(invoke(user, "getName"));
                uuid = asString(invoke(user, "getProfileId"));
            }
            if (uuid == null) {
                Object profile = invoke(client, "getGameProfile");
                if (profile != null) {
                    uuid = asString(invoke(profile, "getId"));
                }
            }
            if (accessToken == null || accessToken.isEmpty() || uuid == null) {
                Object session = invoke(client, "getSession");
                if (session != null) {
                    accessToken = asString(invoke(session, "getAccessToken"));
                    username = asString(invoke(session, "getUsername"));
                    uuid = asString(invoke(session, "getUuidOrNull"));
                    if (uuid == null) {
                        uuid = asString(invoke(session, "getProfileId"));
                    }
                    if (uuid == null) {
                        uuid = asString(invoke(session, "getUuid"));
                    }
                }
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

    public static Optional<String> currentUsername() {
        try {
            Object client = clientInstance();
            if (client == null) {
                return Optional.empty();
            }

            Object player = field(client, "player");
            if (player == null) {
                player = invokeAny(client, "getPlayer", "player");
            }
            String fromPlayer = usernameOf(player);
            if (fromPlayer != null) {
                return Optional.of(fromPlayer);
            }

            Object session = invoke(client, "getSession");
            String fromSession = asString(invoke(session, "getUsername"));
            if (valid(fromSession)) {
                return Optional.of(fromSession.trim());
            }

            Object user = invokeAny(client, "getUser");
            String fromUser = usernameOf(user);
            if (fromUser != null) {
                return Optional.of(fromUser);
            }
        } catch (Throwable ignored) {
        }
        return Optional.empty();
    }

    public static Object clientInstance() {
        try {
            Class<?> clientClass = Class.forName("net.minecraft.client.Minecraft");
            return clientClass.getMethod("getInstance").invoke(null);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String usernameOf(Object owner) {
        if (owner == null) {
            return null;
        }
        Object profile = invokeAny(owner, "getGameProfile", "getProfile");
        String name = profile == null ? null : asString(invoke(profile, "getName"));
        if (valid(name)) {
            return name.trim();
        }
        Object component = invoke(owner, "getName");
        if (component instanceof String directName) {
            return valid(directName) ? directName.trim() : null;
        }
        String text = component == null ? null : asString(invoke(component, "getString"));
        if (valid(text)) {
            return text.trim();
        }
        Object direct = invokeAny(owner, "getScoreboardName", "getUsername");
        return valid(asString(direct)) ? asString(direct).trim() : null;
    }

    private static boolean valid(String value) {
        return value != null && !value.isBlank();
    }

    private static Object field(Object target, String name) {
        try {
            Class<?> type = target.getClass();
            while (type != null) {
                try {
                    Field f = type.getDeclaredField(name);
                    f.setAccessible(true);
                    return f.get(target);
                } catch (NoSuchFieldException e) {
                    type = type.getSuperclass();
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object invoke(Object target, String method) {
        if (target == null) {
            return null;
        }
        try {
            Method m = target.getClass().getMethod(method);
            m.setAccessible(true);
            return m.invoke(target);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object invokeAny(Object target, String... methods) {
        for (String method : methods) {
            Object value = invoke(target, method);
            if (value != null) {
                return value;
            }
        }
        return null;
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
