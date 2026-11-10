# JInfo Server (Fabric)

Серверный мод для Fabric 1.21.4. Позволяет игрокам **без клиентского мода** смотреть профили JInfo через браузер.

## Возможности

- HTTP-сервер со списком профилей и страницей каждого игрока
- Просмотр скина и текста профиля в браузере
- Команды `/jinfo` для администраторов
- Конфиг `config/jinfo-server.json`, профили в `jinfo-profiles/`

## Установка

1. Положите `jinfo-server-0.1.0.jar` в папку `mods` на сервере.
2. Запустите сервер — создастся `config/jinfo-server.json`.
3. Поменяйте `port` / `host` при необходимости.
4. Перезапустите или выполните `/jinfo reload`.

## Команды

- `/jinfo reload` — перечитать конфиг и перезапустить веб-сервер
- `/jinfo list` — список профилей
- `/jinfo profile <ник>` — ссылка на профиль

Право: уровень оператора (2).

## Ссылки

- Клиентский мод: https://github.com/mcchopik/jinfo (ветка `master`)
- Paper-плагин: https://github.com/mcchopik/jinfo (ветка `paper`)
- Этот мод: https://github.com/mcchopik/jinfo (ветка `server`)

## Сборка

GitHub Actions: workflow `build-server` собирает jar на каждый push в ветку `server` и публикует релиз на тег `server-v*`.
