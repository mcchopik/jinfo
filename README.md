# JInfo

JInfo adds short player profiles to Minecraft **26.2**: up to 500 characters, 10 lines and 6 links. Profiles work for both premium and offline accounts.

## Components

- **Client mod** — Fabric client mod with profile cards, a 3D profile screen, an in-game editor and a local browser editor.
- **Server component** — Fabric server mod and Paper plugin that provide JInfo profiles through chat commands. The server component does **not** open a web page and does **not** require the client mod.

## Languages

English is the default language.

| Language | Client | Server |
| --- | --- | --- |
| English | [client/en.md](docs/descriptions/client/en.md) | [server/en.md](docs/descriptions/server/en.md) |
| Русский | [client/ru.md](docs/descriptions/client/ru.md) | [server/ru.md](docs/descriptions/server/ru.md) |
| Беларуская | [client/be.md](docs/descriptions/client/be.md) | [server/be.md](docs/descriptions/server/be.md) |
| Українська | [client/uk.md](docs/descriptions/client/uk.md) | [server/uk.md](docs/descriptions/server/uk.md) |

## Requirements

- Minecraft 26.2
- Java 25
- Client: Fabric Loader 0.20.1+, Fabric API; Mod Menu optional
- Server: Fabric Loader 0.20.1+ with Fabric API, or Paper 26.2

## Install

1. Download the jar for your component from [Releases](https://github.com/mcchopik/jinfo/releases).
2. Client: put the jar and Fabric API into `.minecraft/mods`.
3. Fabric server: put the server jar and Fabric API into `mods/`.
4. Paper server: put the plugin jar into `plugins/`.
5. Start the game or server.

## Quick commands

Client: `/jinfo edit`, `/jinfo web`, `/jinfo settings`.

Server: `/jinfo me`, `/jinfo view <name>`, `/jinfo set <text>`, `/jinfo photo <url|off>`, `/jinfo clear`.

## Links

- [All descriptions](docs/descriptions/README.md)
- [Releases](https://github.com/mcchopik/jinfo/releases)
- [Issues](https://github.com/mcchopik/jinfo/issues)
