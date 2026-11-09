package com.mcchopik.jinfo.net;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.network.chat.Component;

/**
 * Отправка сообщения в чат клиента без завязки на конкретные имена методов
 * (они отличаются между маппингами). Если ничего не нашлось — тихо пропускаем.
 */
public final class ClientChat {

    private static final String[] PLAYER_FIELDS = {"player"};
    private static final String[] PLAYER_METHODS = {"getPlayer", "player"};
    private static final String[] MESSAGE_METHODS = {
            "displayClientMessage", "sendSystemMessage", "sendMessage"
    };
    private static final String[] GUI_FIELDS = {"gui"};
    private static final String[] GUI_METHODS = {"getGui"};
    private static final String[] CHAT_METHODS = {"getChat", "chat"};
    private static final String[] ADD_METHODS = {"addMessage"};

    private ClientChat() {
    }

    public static void send(String text) {
        try {
            Object client = MinecraftSession.clientInstance();
            if (client == null) {
                return;
            }
            Component message = Component.literal(text);

            Object player = member(client, PLAYER_FIELDS, PLAYER_METHODS);
            if (player != null && call(player, MESSAGE_METHODS, message)) {
                return;
            }

            Object gui = member(client, GUI_FIELDS, GUI_METHODS);
            if (gui != null) {
                Object chat = member(gui, new String[0], CHAT_METHODS);
                if (chat != null) {
                    call(chat, ADD_METHODS, message);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean call(Object target, String[] names, Component msg) {
        for (Method m : target.getClass().getMethods()) {
            if (!contains(names, m.getName())) {
                continue;
            }
            Class<?>[] p = m.getParameterTypes();
            try {
                if (p.length == 1 && p[0].isAssignableFrom(msg.getClass())) {
                    m.invoke(target, msg);
                    return true;
                }
                if (p.length == 2 && p[0].isAssignableFrom(msg.getClass())) {
                    if (p[1] == boolean.class || p[1] == Boolean.class) {
                        m.invoke(target, msg, false);
                        return true;
                    }
                    if (p[1] == int.class || p[1] == Integer.class) {
                        m.invoke(target, msg, 0);
                        return true;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private static Object member(Object target, String[] fields, String[] methods) {
        for (String name : fields) {
            try {
                Field f = findField(target.getClass(), name);
                if (f != null) {
                    f.setAccessible(true);
                    return f.get(target);
                }
            } catch (Throwable ignored) {
            }
        }
        for (String name : methods) {
            try {
                Method m = target.getClass().getMethod(name);
                m.setAccessible(true);
                return m.invoke(target);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> c = type;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    private static boolean contains(String[] values, String key) {
        for (String v : values) {
            if (v.equals(key)) {
                return true;
            }
        }
        return false;
    }
}