# JInfo Paper

Paper plugin for Minecraft 26.2. It provides JInfo profiles through chat commands. It does **not** open a web page and does **not** require the client mod.

## Features

- `/jinfo me`, `view`, `set`, `photo`, `clear` for players
- `/jinfo list`, `import`, `remove`, `reload` for admins
- Local JSON profiles and JInfo cloud fallback
- Clickable HTTPS links and formatted profile text

## Install

1. Put `jinfo-paper-0.3.0.jar` into `plugins/`.
2. Start the server. `plugins/JInfoPaper/config.yml` is created automatically.
3. Edit `central-api-base` or `profiles-dir` if needed.
4. Restart the server.

## Permissions

- `jinfo.use` — default true
- `jinfo.admin` — default operator

## Commands

| Command | Description |
| --- | --- |
| `/jinfo help` | Show help |
| `/jinfo me` | Show your profile |
| `/jinfo view <name\|uuid>` | View a profile |
| `/jinfo set <text>` | Save your profile |
| `/jinfo photo <url\|off>` | Set or remove a photo |
| `/jinfo clear` | Remove your profile |
| `/jinfo list` | List local profiles (admin) |
| `/jinfo import <uuid> [name]` | Import from cloud (admin) |
| `/jinfo remove <name\|uuid>` | Remove a local profile (admin) |
| `/jinfo reload` | Reload config (admin) |

## Links

- [English description](https://github.com/mcchopik/jinfo/blob/master/docs/descriptions/server/en.md)
- [Русское описание](https://github.com/mcchopik/jinfo/blob/master/docs/descriptions/server/ru.md)
- [Беларускае апісанне](https://github.com/mcchopik/jinfo/blob/master/docs/descriptions/server/be.md)
- [Український опис](https://github.com/mcchopik/jinfo/blob/master/docs/descriptions/server/uk.md)
- [Client mod](https://github.com/mcchopik/jinfo)
- [Fabric server mod](https://github.com/mcchopik/jinfo/tree/server)

## Build

GitHub Actions workflow `build-paper` builds the jar on every push to `paper` and publishes a release for `paper-v*` tags.
