package com.mcchopik.jinfo.text;

import com.mcchopik.jinfo.config.JInfoConfig;
import java.util.HashMap;
import java.util.Map;

/**
 * Строки интерфейса на ru/uk/en. Язык берётся из конфига мода.
 * Команда /jinfo и часть старых экранов пока остаются на русском.
 */
public final class Messages {

    private static final Map<String, String[]> DICT = new HashMap<>();

    static {
        put("loading", "Загрузка...", "Завантаження...", "Loading...");
        put("not_filled", "Профиль не заполнен", "Профіль не заповнено", "Profile is empty");
        put("net_error", "Ошибка сети", "Помилка мережі", "Network error");
        put("rate_limited", "Слишком часто, подожди", "Занадто часто, зачекай", "Too many requests, wait");
        put("verified", "Профиль подтверждён", "Профіль підтверджено", "Profile verified");
        put("unverified", "Профиль (не подтверждён)", "Профіль (не підтверджено)", "Profile (unverified)");

        put("open_profile", "Открыть профиль", "Відкрити профіль", "Open profile");
        put("close", "Закрыть", "Закрити", "Close");
        put("report", "Пожаловаться", "Поскаржитися", "Report");
        put("reported", "Жалоба отправлена", "Скаргу надіслано", "Report sent");
        put("already_reported", "Жалоба уже отправлена сегодня", "Скаргу вже надіслано сьогодні",
                "Already reported today");
        put("report_self", "Нельзя пожаловаться на себя", "Не можна поскаржитися на себе",
                "You cannot report yourself");
        put("report_failed", "Не удалось отправить жалобу", "Не вдалося надіслати скаргу",
                "Failed to send report");

        put("open_title", "Открыть ссылку?", "Відкрити посилання?", "Open link?");
        put("service", "Сервис", "Сервіс", "Service");
        put("domain", "Домен", "Домен", "Domain");
        put("open", "Открыть", "Відкрити", "Open");
        put("cancel", "Отмена", "Скасувати", "Cancel");
        put("suspicious", "Внимание: адрес выглядит подозрительно.",
                "Увага: адреса виглядає підозріло.",
                "Warning: this address looks suspicious.");

        put("settings", "Настройки JInfo", "Налаштування JInfo", "JInfo settings");
        put("gesture", "Жест Shift+ПКМ", "Жест Shift+ПКМ", "Shift+Right-click gesture");
        put("on", "Вкл", "Увімк", "On");
        put("off", "Выкл", "Вимк", "Off");
        put("language", "Язык", "Мова", "Language");
        put("key_hint", "Клавиша «P» — открыть мой профиль (меняется в управлении).",
                "Клавіша «P» — відкрити мій профіль (змінюється в керуванні).",
                "Key «P» opens my profile (remappable in Controls).");
        put("back", "Назад", "Назад", "Back");
    }

    private Messages() {
    }

    private static void put(String key, String ru, String uk, String en) {
        DICT.put(key, new String[]{ru, uk, en});
    }

    public static String s(String key) {
        String[] values = DICT.get(key);
        if (values == null) {
            return key;
        }
        Lang lang = language();
        return values[Math.min(lang.ordinal(), values.length - 1)];
    }

    private static Lang language() {
        try {
            return JInfoConfig.language();
        } catch (Throwable t) {
            return Lang.RU;
        }
    }
}