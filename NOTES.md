# JInfo — статус и заметки

## Краткий итог (сверху)
- **Шаг 1 (Worker)** — готово, задеплоено на `mcprofiles.mcchopik.workers.dev`. Проверка: `GET /profile/test` без токена → 401.
- **Шаг 2 (каркас мода + CI)** — готово. Сборка в GitHub Actions зелёная (MC 26.2, Java 25, Loom 1.19).
- **Шаг 3 (парсер текста)** — готово: классы `com.mcchopik.jinfo.text` (Service, Lang, Style,
  Part, ParsedProfile, LinkValidator, TextParser, HtmlRenderer) + JUnit-тесты.
- **Шаг 4 (сетевой клиент)** — готово: `com.mcchopik.jinfo.net` (ApiClient, TokenStore,
  OfflineKeyStore, ProfileCache, MinecraftSession) + тесты. Online-авторизация берёт токен
  сессии через рефлексию (best-effort); если недоступно — используется offline Ed25519.
- **Шаг 5 (команда /jinfo)** — готово: help/edit/web/set/me/view/clear. web пока заглушка.
  Имена API 26.2 взяты из рабочего шаблона: `net.minecraft.client.Minecraft`,
  `net.minecraft.network.chat.Component`. Команды регистрируются через Brigadier
  (`ClientCommandManager` в этой версии Fabric API отсутствует).
- **Шаг 6 (экран редактора)** — готово: `JInfoEditorScreen` (стек EditBox = многострочность,
  счётчик, предпросмотр, цвета/стили/сервисы/hex, сохранить/очистить/отмена). GUI-API 26.2:
  `extractRenderState(GuiGraphicsExtractor,…)`, `graphics.text/fill`. Открывается `/jinfo edit`.
- **Шаг 7 (веб-редактор)** — готово: `com.mcchopik.jinfo.web.WebServer` (JDK HttpServer,
  127.0.0.1, порт 0, токен в пути, проверка Host/Origin, CSP+nosniff). Ресурсы:
  `assets/jinfo/web/{editor.html,app.css,app.js}`. `/jinfo web` открывает браузер,
  `/jinfo web stop` останавливает; авто-стоп через 30 мин простоя, при выходе из мира/игры.
  Предпросмотр и сохранение идут через Java-парсер; секреты странице не отдаются.
- **Шаг 8 (Shift+ПКМ, карточка)** — готово: `UseEntityCallback` + `JInfoCardScreen`
  (голова из скина best-effort через рефлексию, иконка статуса, подсказка при наведении).
  Галка выключения жеста — `JInfoConfig` (config/jinfo/config.json).
- **Шаг 9 (полный профиль)** — `JInfoProfileScreen` (голова, ник, статус, текст, кнопки сервисов)
  + `PaintedButton` (цвет по сервису) + `LinkConfirmScreen` (домен, полный URL, «Открыть/Отмена»;
  ссылки никогда не открываются сами). Клик по голове/кнопке в карточке ведёт сюда.
- **Шаг 10 (жалобы, настройки, локализация)** — «Пожаловаться» шлёт `POST /report`
  (429 = уже жаловались сегодня, 400 = нельзя на себя); `JInfoSettingsScreen` (жест, язык
  ru/uk/en, `/jinfo settings`); `Messages` (ru/uk/en) для карточки/профиля/подтверждения/
  настроек; клавиша `P` (`KeyMapping`, категория JInfo) открывает свой профиль.
  Язык сохраняется в config.json и влияет на подписи сервисов. TODO: локализовать
  `/jinfo help` и экран редактора (пока ru); аватар в экране по клавише P — заглушка
  (нет доступа к скину локального игрока без сущности).

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
