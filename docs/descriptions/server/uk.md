# JInfo Server

[English](en.md) | [Русский](ru.md) | [Беларуская](be.md) | **Українська**

JInfo Server додає профілі гравців на сервер Minecraft **26.2** через команди в чаті. Серверна частина **не відкриває веб-сторінку** і **не потребує клієнтського мода**. Гравці створюють і переглядають профілі прямо в чаті; посилання клікабельні.

Доступні дві збірки:

- **Fabric server mod** — гілка `server`
- **Paper plugin** — гілка `paper`

Обидві збірки використовують ті самі команди, права, локальне JSON-сховище та хмарний fallback JInfo.

## Можливості

- `/jinfo me` — показати свій профіль у чаті.
- `/jinfo view <нік|uuid>` — переглянути локальний або хмарний профіль.
- `/jinfo set <текст>` — зберегти свій серверний профіль.
- `/jinfo photo <url|off>` — встановити або прибрати фото профілю.
- `/jinfo clear` — видалити свій серверний профіль.
- Адмін-команди: `list`, `import`, `remove`, `reload`.
- Підтримка кольорів, стилів, плейсхолдерів сервісів і клікабельних посилань.
- Локальні профілі зберігаються в JSON-файлах на сервері.
- Якщо профілю немає локально, він завантажується з хмарного API JInfo.
- Без веб-сервера, без відкритих портів і без клієнтського мода.

## Вимоги

- Сервер Minecraft 26.2
- Java 25

Fabric: Fabric Loader 0.20.1+ і Fabric API.
Paper: Paper 26.2.

## Встановлення

### Fabric

1. Встановіть Fabric Loader на сервер.
2. Покладіть Fabric API і `jinfo-server-<version>.jar` у `mods/`.
3. Запустіть сервер.
4. За потреби відредагуйте `jinfo-server.json`.

### Paper

1. Покладіть `jinfo-paper-<version>.jar` у `plugins/`.
2. Запустіть сервер.
3. За потреби відредагуйте `plugins/JInfoPaper/config.yml`.

## Команди

| Команда | Право | Опис |
| --- | --- | --- |
| `/jinfo help` | `jinfo.use` | Показати довідку |
| `/jinfo me` | `jinfo.use` | Показати свій профіль |
| `/jinfo view <нік\|uuid>` | `jinfo.use` | Переглянути профіль |
| `/jinfo set <текст>` | `jinfo.use` | Зберегти свій профіль |
| `/jinfo photo <url\|off>` | `jinfo.use` | Встановити або прибрати фото |
| `/jinfo clear` | `jinfo.use` | Видалити свій профіль |
| `/jinfo list` | `jinfo.admin` | Список локальних профілів |
| `/jinfo import <uuid> [нік]` | `jinfo.admin` | Імпорт із хмари |
| `/jinfo remove <нік\|uuid>` | `jinfo.admin` | Видалити локальний профіль |
| `/jinfo reload` | `jinfo.admin` | Перезавантажити конфіг |

## Конфігурація

Fabric (`jinfo-server.json`):

```json
{
  "centralApiBase": "https://mcprofiles.mcchopik.workers.dev",
  "profilesDir": "profiles"
}
```

Paper (`plugins/JInfoPaper/config.yml`):

```yaml
central-api-base: https://mcprofiles.mcchopik.workers.dev
profiles-dir: profiles
```

## Формат профілю

До 500 символів, 10 рядків і 6 посилань.

`{youtube}`, `{twitch}`, `{discord}`, `{telegram}`, `{tiktok}`, `{instagram}`, `{x}`, `{github}`, `{steam}`, `{vk}`, `{reddit}`, `{kick}`, `{boosty}`, `{donationalerts}`, `{modrinth}` — далі `@нік` або HTTPS-посилання.

Універсальне посилання: `{link:Назва} https://example.com`. Фото: `{photo} https://example.com/photo.png`.

Кольори: `&0`–`&f`, `&l`, `&o`, `&n`, `&m`, `&r`, hex `&#RRGGBB`.

Дозволені лише HTTPS-посилання. Небезпечні схеми, localhost, IP-адреси та посилання з логіном відхиляються.

## Приватність

Сервер зберігає лише текст профілю, нік, UUID і час оновлення. Запити до хмари виконуються лише для `view` і `import`. Паролі та токени не зберігаються.

## Посилання

- [Репозиторій](https://github.com/mcchopik/jinfo)
- [Релізи](https://github.com/mcchopik/jinfo/releases)
- [Проблеми](https://github.com/mcchopik/jinfo/issues)
- [Клієнтська частина](../client/uk.md)
