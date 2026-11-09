package com.mcchopik.jinfo.text;

import com.mcchopik.jinfo.config.JInfoConfig;
import java.util.HashMap;
import java.util.Map;

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

        put("templates", "Шаблоны", "Шаблони", "Templates");
        put("photo", "Фото", "Фото", "Photo");
        put("photo_invalid", "Нужна прямая https-ссылка на картинку.",
                "Потрібне пряме https-посилання на картинку.",
                "A direct https image link is required.");
        put("photo_set", "Фото профиля обновлено.", "Фото профілю оновлено.",
                "Profile photo updated.");
        put("photo_removed", "Фото профиля убрано.", "Фото профілю прибрано.",
                "Profile photo removed.");
        put("template_hint", "Выбери шаблон — он заменит текст редактора.",
                "Обери шаблон — він замінить текст редактора.",
                "Pick a template — it will replace the editor text.");
        put("template.streamer", "Стример", "Стрімер", "Streamer");
        put("template.developer", "Разработчик", "Розробник", "Developer");
        put("template.gamer", "Геймер", "Геймер", "Gamer");
        put("template.simple", "Простой", "Простий", "Simple");
        put("template_applied", "Шаблон применён. Не забудь заменить ники.",
                "Шаблон застосовано. Не забудь замінити ніки.",
                "Template applied. Don't forget to replace the nicknames.");
        put("template_unknown", "Неизвестный шаблон.", "Невідомий шаблон.", "Unknown template.");

        put("editor_title", "JInfo — редактор профиля", "JInfo — редактор профілю", "JInfo — profile editor");
        put("editor_line", "Строка ", "Рядок ", "Line ");
        put("editor_add_line", "Добавить строку", "Додати рядок", "Add line");
        put("editor_save", "Сохранить", "Зберегти", "Save");
        put("editor_clear", "Очистить", "Очистити", "Clear");
        put("editor_cancel", "Отмена", "Скасувати", "Cancel");
        put("editor_templates", "Шаблоны", "Шаблони", "Templates");
        put("editor_hex_invalid", "Неверный hex-цвет. Формат: #RRGGBB",
                "Невірний hex-колір. Формат: #RRGGBB", "Invalid hex colour. Format: #RRGGBB");
        put("editor_limit_lines", "Достигнут лимит строк: ", "Досягнуто ліміт рядків: ", "Line limit reached: ");
        put("editor_text_cleared", "Текст очищен.", "Текст очищено.", "Text cleared.");
        put("editor_limit_chars", "Достигнут лимит символов.", "Досягнуто ліміт символів.", "Character limit reached.");
        put("editor_over_limits", "Превышены лимиты: 500 символов / 10 строк / 6 ссылок.",
                "Перевищено ліміти: 500 символів / 10 рядків / 6 посилань.",
                "Limits exceeded: 500 characters / 10 lines / 6 links.");
        put("editor_nick_unknown", "Не удалось определить ник игрока.",
                "Не вдалося визначити нік гравця.", "Could not determine the player nickname.");
        put("editor_saving", "Сохранение...", "Збереження...", "Saving...");
        put("editor_saved", "Профиль сохранён.", "Профіль збережено.", "Profile saved.");
        put("editor_error", "Ошибка: ", "Помилка: ", "Error: ");
        put("editor_preview", "Предпросмотр", "Попередній перегляд", "Preview");
        put("editor_counter_chars", "символов", "символів", "characters");
        put("editor_counter_lines", "строк", "рядків", "lines");

        put("open_editor", "Открыть редактор", "Відкрити редактор", "Open editor");
        put("open_web", "Открыть веб-редактор", "Відкрити веб-редактор", "Open web editor");
        put("my_profile", "Мой профиль", "Мій профіль", "My profile");
        put("preview_opening", "Открываю профиль...", "Відкриваю профіль...", "Opening profile...");

        put("err_rate", "Слишком часто. Подожди немного.",
                "Занадто часто. Зачекай трохи.", "Too many requests. Wait a bit.");
        put("err_auth", "Не удалось авторизоваться.", "Не вдалося авторизуватися.", "Authorization failed.");
        put("err_not_filled", "Профиль не заполнен.", "Профіль не заповнено.", "Profile is empty.");
        put("err_generic", "Ошибка", "Помилка", "Error");

        put("help",
                "=== JInfo — профиль игрока ===\n"
                        + "/jinfo help — эта справка\n"
                        + "/jinfo edit — редактор в игре\n"
                        + "/jinfo web — редактор в браузере\n"
                        + "/jinfo template <id> — готовый шаблон\n"
                        + "/jinfo photo <url> — фото профиля (прямая ссылка)\n"
                        + "/jinfo preview [ник] — как выглядит профиль\n"
                        + "/jinfo set <текст> — быстро задать текст\n"
                        + "/jinfo me — мой профиль\n"
                        + "/jinfo view <ник> — профиль игрока\n"
                        + "/jinfo clear — удалить профиль\n"
                        + "/jinfo settings — язык и жест Shift+ПКМ\n"
                        + "/jinfo update — проверить обновления\n"
                        + "--- Плейсхолдеры ---\n"
                        + "{youtube}, {twitch}, {discord}, {telegram}, {tiktok}, {instagram}, {x}, {github}, {steam}, {vk}, {reddit}, {kick}, {boosty}, {donationalerts}, {modrinth}\n"
                        + "После плейсхолдера — @ник или ссылка. Пример: {youtube} @nick\n"
                        + "Универсально: {link:Название} https://example.com\n"
                        + "Фото: {avatar} https://example.com/photo.png\n"
                        + "Цвета: &0-&f, &l &o &n &m &r, hex &#RRGGBB; &k запрещён.\n"
                        + "Лимиты: 500 символов, 10 строк, 6 ссылок.",
                "=== JInfo — профіль гравця ===\n"
                        + "/jinfo help — ця довідка\n"
                        + "/jinfo edit — редактор у грі\n"
                        + "/jinfo web — редактор у браузері\n"
                        + "/jinfo template <id> — готовий шаблон\n"
                        + "/jinfo photo <url> — фото профілю (пряме посилання)\n"
                        + "/jinfo preview [нік] — як виглядає профіль\n"
                        + "/jinfo set <текст> — швидко задати текст\n"
                        + "/jinfo me — мій профіль\n"
                        + "/jinfo view <нік> — профіль гравця\n"
                        + "/jinfo clear — видалити профіль\n"
                        + "/jinfo settings — мова і жест Shift+ПКМ\n"
                        + "/jinfo update — перевірити оновлення\n"
                        + "--- Плейсхолдери ---\n"
                        + "{youtube}, {twitch}, {discord}, {telegram}, {tiktok}, {instagram}, {x}, {github}, {steam}, {vk}, {reddit}, {kick}, {boosty}, {donationalerts}, {modrinth}\n"
                        + "Після плейсхолдера — @нік або посилання. Приклад: {youtube} @nick\n"
                        + "Універсально: {link:Назва} https://example.com\n"
                        + "Фото: {avatar} https://example.com/photo.png\n"
                        + "Кольори: &0-&f, &l &o &n &m &r, hex &#RRGGBB; &k заборонено.\n"
                        + "Ліміти: 500 символів, 10 рядків, 6 посилань.",
                "=== JInfo — player profile ===\n"
                        + "/jinfo help — this help\n"
                        + "/jinfo edit — in-game editor\n"
                        + "/jinfo web — browser editor\n"
                        + "/jinfo template <id> — ready-made template\n"
                        + "/jinfo photo <url> — profile photo (direct link)\n"
                        + "/jinfo preview [nick] — how the profile looks\n"
                        + "/jinfo set <text> — set the text quickly\n"
                        + "/jinfo me — my profile\n"
                        + "/jinfo view <nick> — someone's profile\n"
                        + "/jinfo clear — delete my profile\n"
                        + "/jinfo settings — language and Shift+Right-click\n"
                        + "/jinfo update — check for updates\n"
                        + "--- Placeholders ---\n"
                        + "{youtube}, {twitch}, {discord}, {telegram}, {tiktok}, {instagram}, {x}, {github}, {steam}, {vk}, {reddit}, {kick}, {boosty}, {donationalerts}, {modrinth}\n"
                        + "After a placeholder put @nick or a link. Example: {youtube} @nick\n"
                        + "Generic: {link:Title} https://example.com\n"
                        + "Photo: {avatar} https://example.com/photo.png\n"
                        + "Colours: &0-&f, &l &o &n &m &r, hex &#RRGGBB; &k is not allowed.\n"
                        + "Limits: 500 characters, 10 lines, 6 links.");
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
