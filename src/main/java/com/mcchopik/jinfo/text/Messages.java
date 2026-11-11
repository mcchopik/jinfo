package com.mcchopik.jinfo.text;

import com.mcchopik.jinfo.config.JInfoConfig;
import java.util.HashMap;
import java.util.Map;

public final class Messages {

    private static final Map<String, String[]> DICT = new HashMap<>();
    private static final Map<String, String> BE = new HashMap<>();

    static {
        put("loading", "Загрузка...", "Завантаження...", "Loading...");
        put("not_filled", "Профиль не заполнен", "Профіль не заповнено", "Profile is empty");
        put("net_error", "Ошибка сети", "Помилка мережі", "Network error");
        put("rate_limited", "Слишком часто, подожди", "Занадто часто, зачекай", "Too many requests, wait");
        put("verified", "Профиль подтверждён", "Профіль підтверджено", "Profile verified");

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
        put("layer_hat", "Шляпа", "Капелюх", "Hat");
        put("layer_jacket", "Куртка", "Куртка", "Jacket");
        put("layer_sleeves", "Рукава", "Рукави", "Sleeves");
        put("layer_pants", "Штаны", "Штани", "Pants");
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
        put("stop_web", "Остановить веб-сервер", "Зупинити веб-сервер", "Stop web server");
        put("web_stopped", "Веб-сервер остановлен.", "Веб-сервер зупинено.", "Web server stopped.");
        put("my_profile", "Мой профиль", "Мій профіль", "My profile");
        put("change_nick", "Свой ник", "Свій нік", "My nickname");
        put("nick_title", "Ник игрока", "Нік гравця", "Player nickname");
        put("nick_hint", "Не удалось определить ник автоматически. Введите его:",
                "Не вдалося визначити нік автоматично. Введіть його:",
                "Could not detect your nickname automatically. Enter it:");
        put("nick_remember", "Запомнить ник", "Запам'ятати нік", "Remember nickname");
        put("nick_continue", "Продолжить", "Продовжити", "Continue");
        put("nick_invalid", "Введите ник (до 16 символов).", "Введіть нік (до 16 символів).",
                "Enter a nickname (up to 16 characters).");
        put("account_title", "Аккаунт JInfo", "Акаунт JInfo", "JInfo account");
        put("account_hint", "Ник и пароль защищают профиль. Пароль не сохраняется на устройстве.",
                "Нік і пароль захищають профіль. Пароль не зберігається на пристрої.",
                "Nickname and password protect the profile. The password is not stored on this device.");
        put("account_user", "Ник", "Нік", "Nickname");
        put("account_pass", "Пароль (8+ символов)", "Пароль (8+ символів)", "Password (8+ characters)");
        put("account_login", "Войти", "Увійти", "Sign in");
        put("account_register", "Создать аккаунт", "Створити акаунт", "Create account");
        put("account_guest", "Войти как гость", "Увійти як гість", "Continue as guest");
        put("account_logout", "Выйти из аккаунта", "Вийти з акаунта", "Sign out");
        put("account_logged", "Вы вошли как ", "Ви увійшли як ", "Signed in as ");
        put("account_busy", "Подождите...", "Зачекайте...", "Please wait...");
        put("account_bad_pass", "Пароль должен быть от 8 до 128 символов.",
                "Пароль має бути від 8 до 128 символів.", "Password must be 8-128 characters.");
        put("account_registered_guest", "Этот ник зарегистрирован. Войдите с паролем.",
                "Цей нік зареєстрований. Увійдіть із паролем.",
                "This nickname is registered. Sign in with the password.");
        put("account_exists", "Этот ник уже зарегистрирован.", "Цей нік уже зареєстрований.",
                "This nickname is already registered.");
        put("account_bad_login", "Неверный ник или пароль.", "Невірний нік або пароль.",
                "Incorrect nickname or password.");
        put("account_created", "Аккаунт создан.", "Акаунт створено.", "Account created.");
        put("account_ok", "Вход выполнен.", "Вхід виконано.", "Signed in.");
        put("account_guest_mode", "Гостевой режим активен", "Гостьовий режим активний", "Guest mode is active");
        put("account_guest_hint", "Пока режим включён, этот ник нельзя зарегистрировать другим игрокам.",
                "Поки режим увімкнено, цей нік не можна зареєструвати іншим гравцям.",
                "While this mode is on, other players cannot register this nickname.");
        put("account_disable_guest", "Выключить гостевой режим", "Вимкнути гостьовий режим", "Disable guest mode");
        put("account_guest_off", "Гостевой режим выключен.", "Гостьовий режим вимкнено.", "Guest mode disabled.");
        put("account_guest_locked", "Этот ник сейчас в гостевом режиме. Сначала выключите его на устройстве, где он включён.",
                "Цей нік зараз у гостьовому режимі. Спершу вимкніть його на пристрої, де він увімкнений.",
                "This nickname is in guest mode. Disable it on the device where it was enabled first.");
        put("preview_opening", "Открываю профиль...", "Відкриваю профіль...", "Opening profile...");

        put("skin_title", "Настройки скина", "Налаштування скіна", "Skin settings");
        put("skin_mode", "Режим", "Режим", "Mode");
        put("skin_mode_auto", "Авто (свой скин)", "Авто (свій скін)", "Auto (your skin)");
        put("skin_mode_url", "По ссылке", "По посиланню", "By URL");
        put("skin_mode_file", "Из файла", "З файлу", "From file");
        put("skin_model", "Модель", "Модель", "Model");
        put("skin_model_classic", "Классика", "Класика", "Classic");
        put("skin_model_slim", "Тощая (Slim)", "Тонка (Slim)", "Slim");
        put("skin_url", "URL скина (https://...)", "URL скіна (https://...)", "Skin URL (https://...)");
        put("skin_file", "Путь к PNG файлу", "Шлях до PNG файлу", "Path to PNG file");
        put("skin_browse", "Выбрать файл...", "Вибрати файл...", "Choose file...");
        put("skin_load", "Загрузить", "Завантажити", "Load");
        put("skin_clear", "Сбросить", "Скинути", "Clear");
        put("skin_preview", "Предпросмотр", "Попередній перегляд", "Preview");
        put("skin_loading", "Загрузка...", "Завантаження...", "Loading...");
        put("skin_loaded", "Скин загружен.", "Скін завантажено.", "Skin loaded.");
        put("skin_error", "Ошибка загрузки: ", "Помилка завантаження: ", "Load error: ");
        put("skin_invalid_dims", "Скин должен быть 64x64 или 64x32 пикселей.",
                "Скін має бути 64x64 або 64x32 пікселі.",
                "Skin must be 64x64 or 64x32 pixels.");
        put("skin_invalid_url", "Нужна прямая https-ссылка на PNG.",
                "Потрібне пряме https-посилання на PNG.",
                "A direct https PNG link is required.");
        put("skin_file_not_found", "Файл не найден.", "Файл не знайдено.", "File not found.");
        put("skin_applied", "Скин применён. Сохранится при следующем сохранении профиля.",
                "Скін застосовано. Збережеться при наступному збереженні профілю.",
                "Skin applied. Will be saved on next profile save.");
        put("skin_cleared", "Скин сброшен к автоматическому.", "Скін скинуто до автоматичного.", "Skin reset to automatic.");
        put("skin_local_note", "Локальный файл не публикуется в профиль.", "Локальний файл не публікується у профіль.", "Local file is not published to profile.");

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
                        + "Фото: {photo} https://example.com/photo.png\n"
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
                        + "Фото: {photo} https://example.com/photo.png\n"
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
                        + "Photo: {photo} https://example.com/photo.png\n"
                        + "Colours: &0-&f, &l &o &n &m &r, hex &#RRGGBB; &k is not allowed.\n"
                        + "Limits: 500 characters, 10 lines, 6 links.");

        putBe("loading", "Загрузка...");
        putBe("not_filled", "Профіль не запоўнены");
        putBe("net_error", "Памылка сеткі");
        putBe("rate_limited", "Занадта часта, пачакай");
        putBe("verified", "Профіль пацверджаны");

        putBe("open_profile", "Адкрыць профіль");
        putBe("close", "Закрыць");
        putBe("report", "Паскардзіцца");
        putBe("reported", "Скарга адпраўлена");
        putBe("already_reported", "Скарга ўжо адпраўлена сёння");
        putBe("report_self", "Нельга паскардзіцца на сябе");
        putBe("report_failed", "Не ўдалося адправіць скаргу");

        putBe("open_title", "Адкрыць спасылку?");
        putBe("service", "Сэрвіс");
        putBe("domain", "Домен");
        putBe("open", "Адкрыць");
        putBe("cancel", "Скасаваць");
        putBe("suspicious", "Увага: адрас выглядае падазрона.");

        putBe("settings", "Налады JInfo");
        putBe("gesture", "Жэст Shift+ПКМ");
        putBe("on", "Укл");
        putBe("off", "Выкл");
        putBe("language", "Мова");
        putBe("key_hint", "Клавіша «P» — адкрыць мой профіль (змяняецца ў кіраванні).");
        putBe("back", "Назад");

        putBe("templates", "Шаблоны");
        putBe("photo", "Фота");
        putBe("photo_invalid", "Патрэбна прамая https-спасылка на выяву.");
        putBe("photo_set", "Фота профілю абноўлена.");
        putBe("photo_removed", "Фота профілю прыбрана.");
        putBe("layer_hat", "Капялюш");
        putBe("layer_jacket", "Куртка");
        putBe("layer_sleeves", "Рукавы");
        putBe("layer_pants", "Штаны");
        putBe("template_hint", "Выберы шаблон — ён заменіць тэкст рэдактара.");
        putBe("template.streamer", "Стрэмер");
        putBe("template.developer", "Распрацоўшчык");
        putBe("template.gamer", "Геймер");
        putBe("template.simple", "Просты");
        putBe("template_applied", "Шаблон ужыты. Не забудзь замяніць нікі.");
        putBe("template_unknown", "Невядомы шаблон.");

        putBe("editor_title", "JInfo — рэдактар профілю");
        putBe("editor_line", "Радок ");
        putBe("editor_add_line", "Дадаць радок");
        putBe("editor_save", "Захаваць");
        putBe("editor_clear", "Ачысціць");
        putBe("editor_cancel", "Скасаваць");
        putBe("editor_templates", "Шаблоны");
        putBe("editor_hex_invalid", "Няверны hex-колер. Фармат: #RRGGBB");
        putBe("editor_limit_lines", "Дасягнуты ліміт радкоў: ");
        putBe("editor_text_cleared", "Тэкст ачышчаны.");
        putBe("editor_limit_chars", "Дасягнуты ліміт сімвалаў.");
        putBe("editor_over_limits", "Перавышаны ліміты: 500 сімвалаў / 10 радкоў / 6 спасылак.");
        putBe("editor_nick_unknown", "Не ўдалося вызначыць нік гульца.");
        putBe("editor_saving", "Захаванне...");
        putBe("editor_saved", "Профіль захаваны.");
        putBe("editor_error", "Памылка: ");
        putBe("editor_preview", "Прадпрагляд");
        putBe("editor_counter_chars", "сімвалаў");
        putBe("editor_counter_lines", "радкоў");

        putBe("open_editor", "Адкрыць рэдактар");
        putBe("open_web", "Адкрыць вэб-рэдактар");
        putBe("my_profile", "Мой профіль");
        putBe("change_nick", "Свой нік");
        putBe("nick_title", "Нік гульца");
        putBe("nick_hint", "Не ўдалося вызначыць нік аўтаматычна. Увядзіце яго:");
        putBe("nick_remember", "Запомніць нік");
        putBe("nick_continue", "Працягнуць");
        putBe("nick_invalid", "Увядзіце нік (да 16 сімвалаў).");
        putBe("account_title", "Акаўнт JInfo");
        putBe("account_hint", "Нік і пароль абараняюць профіль. Пароль не захоўваецца на прыладзе.");
        putBe("account_user", "Нік");
        putBe("account_pass", "Пароль (8+ сімвалаў)");
        putBe("account_login", "Увайсці");
        putBe("account_register", "Стварыць акаўнт");
        putBe("account_guest", "Увайсці як госць");
        putBe("account_logout", "Выйсці з акаўнта");
        putBe("account_logged", "Вы ўвайшлі як ");
        putBe("account_busy", "Пачакайце...");
        putBe("account_bad_pass", "Пароль павінен быць ад 8 да 128 сімвалаў.");
        putBe("account_registered_guest", "Гэты нік зарэгістраваны. Увайдзіце з паролем.");
        putBe("account_exists", "Гэты нік ужо зарэгістраваны.");
        putBe("account_bad_login", "Няверны нік або пароль.");
        putBe("account_created", "Акаўнт створаны.");
        putBe("account_ok", "Уваход выкананы.");
        putBe("account_guest_mode", "Гасьцявы рэжым актыўны");
        putBe("account_guest_hint", "Пакуль рэжым уключаны, гэты нік нельга зарэгістраваць іншым гульцам.");
        putBe("account_disable_guest", "Выключыць гасьцявы рэжым");
        putBe("account_guest_off", "Гасьцявы рэжым выключаны.");
        putBe("account_guest_locked", "Гэты нік зараз у гасьцявым рэжыме. Спачатку выключыце яго на прыладзе, дзе ён уключаны.");
        putBe("preview_opening", "Адкрываю профіль...");

        putBe("skin_title", "Налады скіна");
        putBe("skin_mode", "Рэжым");
        putBe("skin_mode_auto", "Аўта (свой скін)");
        putBe("skin_mode_url", "Па спасылцы");
        putBe("skin_mode_file", "З файла");
        putBe("skin_model", "Мадэль");
        putBe("skin_model_classic", "Класіка");
        putBe("skin_model_slim", "Худая (Slim)");
        putBe("skin_url", "URL скіна (https://...)");
        putBe("skin_file", "Шлях да PNG файла");
        putBe("skin_browse", "Выбраць файл...");
        putBe("skin_load", "Загрузіць");
        putBe("skin_clear", "Скінуць");
        putBe("skin_preview", "Прадпрагляд");
        putBe("skin_loading", "Загрузка...");
        putBe("skin_loaded", "Скін загружаны.");
        putBe("skin_error", "Памылка загрузкі: ");
        putBe("skin_invalid_dims", "Скін павінен быць 64x64 або 64x32 пікселяў.");
        putBe("skin_invalid_url", "Патрэбна прамая https-спасылка на PNG.");
        putBe("skin_file_not_found", "Файл не знойдзены.");
        putBe("skin_applied", "Скін ужыты. Захаваецца пры наступным захаванні профілю.");
        putBe("skin_cleared", "Скін скінуты да аўтаматычнага.");
        putBe("skin_local_note", "Лакальны файл не публікуецца ў профіль.");

        putBe("stop_web", "Спыніць вэб-сервер");
        putBe("web_stopped", "Вэб-сервер спынены.");

        putBe("err_rate", "Занадта часта. Пачакай трохі.");
        putBe("err_auth", "Не ўдалося аўтарызавацца.");
        putBe("err_not_filled", "Профіль не запоўнены.");
        putBe("err_generic", "Памылка");

        putBe("help",
                "=== JInfo — профіль гульца ===\n"
                        + "/jinfo help — гэта даведка\n"
                        + "/jinfo edit — рэдактар у гульні\n"
                        + "/jinfo web — рэдактар у браўзеры\n"
                        + "/jinfo template <id> — гатовы шаблон\n"
                        + "/jinfo photo <url> — фота профілю (прамая спасылка)\n"
                        + "/jinfo preview [нік] — як выглядае профіль\n"
                        + "/jinfo set <тэкст> — хутка задаць тэкст\n"
                        + "/jinfo me — мой профіль\n"
                        + "/jinfo view <нік> — профіль гульца\n"
                        + "/jinfo clear — выдаліць профіль\n"
                        + "/jinfo settings — мова і жэст Shift+ПКМ\n"
                        + "/jinfo update — праверыць абнаўленні\n"
                        + "--- Плэйсхолдары ---\n"
                        + "{youtube}, {twitch}, {discord}, {telegram}, {tiktok}, {instagram}, {x}, {github}, {steam}, {vk}, {reddit}, {kick}, {boosty}, {donationalerts}, {modrinth}\n"
                        + "Пасля плэйсхолдара — @нік або спасылка. Прыклад: {youtube} @nick\n"
                        + "Універсальна: {link:Назва} https://example.com\n"
                        + "Фота: {photo} https://example.com/photo.png\n"
                        + "Колеры: &0-&f, &l &o &n &m &r, hex &#RRGGBB; &k забаронены.\n"
                        + "Ліміты: 500 сімвалаў, 10 радкоў, 6 спасылак.");
    }

    private static void putBe(String key, String be) {
        BE.put(key, be);
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
        return switch (lang) {
            case RU -> values[0];
            case BE -> BE.getOrDefault(key, values[0]);
            case UK -> values[1];
            case EN -> values[2];
        };
    }

    private static Lang language() {
        try {
            return JInfoConfig.language();
        } catch (Throwable t) {
            return Lang.RU;
        }
    }
}
