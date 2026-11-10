# JInfo Server

[English](en.md) | **Русский** | [Беларуская](be.md) | [Українська](uk.md)

JInfo Server добавляет профили игроков на сервер Minecraft **26.2** через команды в чате. Серверная часть **не открывает веб-страницу** и **не требует клиентского мода**. Игроки создают и смотрят профили прямо в чате; ссылки кликабельны.

Доступны две сборки:

- **Fabric server mod** — ветка `server`
- **Paper plugin** — ветка `paper`

Обе сборки используют одни и те же команды, права, локальное JSON-хранилище и облачный fallback JInfo.

## Возможности

- `/jinfo me` — показать свой профиль в чате.
- `/jinfo view <ник|uuid>` — посмотреть локальный или облачный профиль.
- `/jinfo set <текст>` — сохранить свой серверный профиль.
- `/jinfo photo <url|off>` — установить или убрать фото профиля.
- `/jinfo clear` — удалить свой серверный профиль.
- Админ-команды: `list`, `import`, `remove`, `reload`.
- Поддержка цветов, стилей, плейсхолдеров сервисов и кликабельных ссылок.
- Локальные профили хранятся в JSON-файлах на сервере.
- Если профиля нет локально, он загружается из облачного API JInfo.
- Без веб-сервера, без открытых портов и без клиентского мода.

## Требования

- Сервер Minecraft 26.2
- Java 25

Fabric: Fabric Loader 0.20.1+ и Fabric API.
Paper: Paper 26.2.

## Установка

### Fabric

1. Установите Fabric Loader на сервер.
2. Положите Fabric API и `jinfo-server-<version>.jar` в `mods/`.
3. Запустите сервер.
4. При необходимости отредактируйте `jinfo-server.json`.

### Paper

1. Положите `jinfo-paper-<version>.jar` в `plugins/`.
2. Запустите сервер.
3. При необходимости отредактируйте `plugins/JInfoPaper/config.yml`.

## Команды

| Команда | Право | Описание |
| --- | --- | --- |
| `/jinfo help` | `jinfo.use` | Показать справку |
| `/jinfo me` | `jinfo.use` | Показать свой профиль |
| `/jinfo view <ник\|uuid>` | `jinfo.use` | Посмотреть профиль |
| `/jinfo set <текст>` | `jinfo.use` | Сохранить свой профиль |
| `/jinfo photo <url\|off>` | `jinfo.use` | Установить или убрать фото |
| `/jinfo clear` | `jinfo.use` | Удалить свой профиль |
| `/jinfo list` | `jinfo.admin` | Список локальных профилей |
| `/jinfo import <uuid> [ник]` | `jinfo.admin` | Импорт из облака |
| `/jinfo remove <ник\|uuid>` | `jinfo.admin` | Удалить локальный профиль |
| `/jinfo reload` | `jinfo.admin` | Перезагрузить конфиг |

## Конфигурация

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

## Формат профиля

До 500 символов, 10 строк и 6 ссылок.

`{youtube}`, `{twitch}`, `{discord}`, `{telegram}`, `{tiktok}`, `{instagram}`, `{x}`, `{github}`, `{steam}`, `{vk}`, `{reddit}`, `{kick}`, `{boosty}`, `{donationalerts}`, `{modrinth}` — далее `@ник` или HTTPS-ссылка.

Универсальная ссылка: `{link:Название} https://example.com`. Фото: `{photo} https://example.com/photo.png`.

Цвета: `&0`–`&f`, `&l`, `&o`, `&n`, `&m`, `&r`, hex `&#RRGGBB`.

Разрешены только HTTPS-ссылки. Небезопасные схемы, localhost, IP-адреса и ссылки с логином отклоняются.

## Приватность

Сервер хранит только текст профиля, ник, UUID и время обновления. Запросы в облако выполняются только для `view` и `import`. Пароли и токены не хранятся.

## Ссылки

- [Репозиторий](https://github.com/mcchopik/jinfo)
- [Релизы](https://github.com/mcchopik/jinfo/releases)
- [Проблемы](https://github.com/mcchopik/jinfo/issues)
- [Клиентская часть](../client/ru.md)
