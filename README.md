# JInfo Paper

Серверный плагин для Paper 26.2. Показывает профили JInfo игрокам **без клиентского мода** через браузер.

## Возможности

- HTTP-сервер со списком профилей и страницей каждого игрока
- Просмотр скина и текста профиля в браузере
- Импорт профиля из центрального JInfo API по UUID
- Команды `/jinfo` для администраторов

## Установка

1. Положите `jinfo-paper-0.2.0.jar` в папку `plugins`.
2. Запустите сервер — создастся `plugins/JInfoPaper/config.yml`.
3. При необходимости поменяйте `port`, `host`, `central-api-base`, `public-url`.
4. Перезапустите или выполните `/jinfo reload`.

## Команды

- `/jinfo reload` — перезагрузить конфиг и веб-сервер
- `/jinfo list` — список профилей
- `/jinfo profile <ник>` — ссылка на профиль
- `/jinfo import <uuid> [ник]` — импорт из `https://mcprofiles.mcchopik.workers.dev/public/profile/<uuid>`
- `/jinfo remove <uuid>` — удалить локальный профиль

Право: `jinfo.admin` (по умолчанию OP).

## Ссылки

- Клиентский мод: https://github.com/mcchopik/jinfo (ветка `master`)
- Fabric-сервер: https://github.com/mcchopik/jinfo (ветка `server`)
- Этот плагин: https://github.com/mcchopik/jinfo (ветка `paper`)

## Сборка

GitHub Actions: workflow `build-paper` собирает jar на каждый push в ветку `paper` и публикует релиз на тег `paper-v*`.
