# JInfo — статус и заметки

## Краткий итог (сверху)
- **Шаг 1 (Worker)** — готово, задеплоено на `mcprofiles.mcchopik.workers.dev`. Проверка: `GET /profile/test` без токена → 401.
- **Шаг 2 (каркас мода + CI)** — готово. Сборка в GitHub Actions зелёная (MC 26.2, Java 25, Loom 1.19).
- **Шаг 3 (парсер текста)** — готово: классы `com.mcchopik.jinfo.text` (Service, Lang, Style,
  Part, ParsedProfile, LinkValidator, TextParser, HtmlRenderer) + JUnit-тесты.
- **Шаг 4 (сетевой клиент)** — готово: `com.mcchopik.jinfo.net` (ApiClient, TokenStore,
  OfflineKeyStore, ProfileCache, MinecraftSession) + тесты. Online-авторизация берёт токен
  сессии через рефлексию (best-effort); если недоступно — используется offline Ed25519.
- **Шаг 5 (команда /jinfo)** — готово: help/edit/web/set/me/view/clear. edit и web пока
  выводят заглушку (реализуются на шагах 6–7). Имена API 26.2 взяты из рабочего шаблона:
  `net.minecraft.client.Minecraft`, `net.minecraft.network.chat.Component`.

## Принятые решения
- Версии взяты из рабочего шаблона (noverplay-26.2): Minecraft `26.2`, Fabric Loader `0.20.1`,
  Loom `1.19-SNAPSHOT`, Fabric API `0.161.0+26.2`, Java `25`, Gradle `9.8.2`.
- Сборка только в GitHub Actions (локальный Gradle на телефоне не запускаем).
- Один source set (`src/main`), без `splitEnvironmentSourceSets()` — проще и надёжнее для старта.
  Клиентский код лежит в том же source set (пакет `com.mcchopik.jinfo`).
- `promp.txt` и `opencode.json` — локальные файлы, в репозиторий не попадают (в `.gitignore`).

## Что нужно сделать вручную («TODO для меня»)
- (пока нет)

## Заблокировано
- (пока нет)
