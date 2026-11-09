package com.mcchopik.jinfo.net;

import java.lang.reflect.Method;
import net.minecraft.resources.Identifier;

/**
 * Best-effort получение текстуры скина через рефлексию.
 * Возвращает Identifier или null, если текстуру получить не удалось.
 */
public final class SkinHelper {

    private SkinHelper() {
    }

    public static Identifier texture(Object player) {
        try {
            Object skin = invoke(player, "getSkin");
            Object body = skin != null ? invoke(skin, "body") : null;
            if (body instanceof Identifier id) {
                return id;
            }
            Object fallback = invoke(player, "getSkinTextureLocation");
            if (fallback instanceof Identifier id) {
                return id;
            }
            Object loc = invoke(player, "getTextureLocation");
            if (loc instanceof Identifier id) {
                return id;
            }
        } catch (Throwable ignored) {
        }
        return null;
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
}