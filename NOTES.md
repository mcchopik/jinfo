# JInfo — статус и заметки

## Краткий итог (сверху)
- **Своё фото** — `{photo} https://...` (поддерживается и `{avatar}`) в тексте профиля;
  парсер складывает ссылку в `ParsedProfile.avatarUrl()`, игра скачивает картинку
  (`PhotoTextures`, NativeImage + DynamicTexture, центр-кроп) и показывает её как
  дополнение к информации в полном профиле; голова в карточке всегда из скина.
  Веб-редактор показывает `<img>` (CSP img-src https:), кнопка «Фото» в редакторе
  и `/jinfo photo <url>` / `photo off` в командах.
- **Ник игрока** — `MinecraftSession` в 26.2 читает `Minecraft.getUser()`
  (`getName`, `getAccessToken`, `getProfileId`) и `getGameProfile()`; если ник
  определить не удалось, `JInfoNickScreen` спрашивает его, галочка «Запомнить ник»
  сохраняет значение в `config/jinfo/config.json` (`nickname`). Отдельная кнопка
  «Свой ник» есть в настройках и Mod Menu.
- **3D-скин в полном профиле** — `SkinViewer` (`AbstractWidget`) на `Model.Simple` +
  `ModelLayers.PLAYER/PLAYER_SLIM`, `GuiGraphicsExtractor.skin`: вращение мышью,
  кнопки включения слоёв шляпы, куртки, рукавов и штанов; скин всегда соответствует
  текущему скину игрока.
- **Шаблоны/предпросмотр/Mod Menu** — `ProfileTemplates` (streamer/developer/gamer/simple,
  ru/uk/en), `/jinfo template <id>`, `/jinfo preview [ник]` (открывает полный экран),
  кнопка «Шаблоны» в редакторе (`JInfoTemplateScreen`), локализация редактора и `/jinfo help`
  (ru/uk/en), Mod Menu entrypoint + `JInfoSettingsScreen` с кнопками (жест, 3 языка,
  редактор/веб/мой профиль). В профиль/карточку добавлен авто-логин перед запросом.
- **Фикс ника на пиратке** — `MinecraftSession.currentUsername()` теперь берёт ник
  локального игрока (`getName` → `getGameProfile` → `getScoreboardName`), затем сессию,
  затем пользователя лаунчера; `/jinfo web` и `/jinfo me` больше не падают с
  «не удалось определить ник». Открытие профиля по `P` использует v4-сессию, иначе offline UUID.
- **Версия и апдейты** — `mod_version=0.2beta`; workflow по тегу `v*` собирает эту версию,
  создаёт GitHub Release с jar и `update.json` (version/file/sha256). В моде: проверка
  последнего релиза при входе (чат) и `/jinfo update`; сравнение версий, без автоустановки.
  Релиз-ноты: `.github/release-notes/<версия>.md`. Тег не создавался (нужно разрешение).
- **Шаг 1 (Worker)** — готово, задеплоено на `mcprofiles.mcchopik.workers.dev`. Проверка: `GET /profile/test` без токена → 401.
- **Шаг 2 (каркас мода + CI)** — готово. Сборка в GitHub Actions зелёная (MC 26.2, Java 25, Loom 1.19).
- **Шаг 3 (парсер текста)** — готово: классы `com.mcchopik.jinfo.text` (Service, Lang, Style,
  Part, ParsedProfile, LinkValidator, TextParser, HtmlRenderer) + JUnit-тесты.
- **Шаг 4 (сетевой клиент)** — готово: `com.mcchopik.jinfo.net` (ApiClient, TokenStore,
  OfflineKeyStore, ProfileCache, MinecraftSession) + тесты. Online-авторизация берёт токен
  сессии через рефлексию (best-effort); если недоступно — используется offline Ed25519.
- **Шаг 5 (команда /jinfo)** — готово: help/edit/web/set/me/view/clear (+settings на шаге 10).
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
- **Шаг 11 (тесты/релиз)** — добавлены `README.md` (описание, установка, команды,
  плейсхолдеры, сборка, приватность) и `TESTING.md` (чек-лист на лицензионном и offline
  клиентах). Релиз/тег НЕ создавались: действует правило не публиковать без явного
  разрешения. Для релиза: `git tag v1.0.0 && git push origin v1.0.0` (workflow уже
  умеет делать Release по тегу) — нужно подтверждение.

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
