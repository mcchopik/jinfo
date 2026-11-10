# JInfo Server

**English** | [Русский](ru.md) | [Беларуская](be.md) | [Українська](uk.md)

JInfo Server adds player profiles to a Minecraft **26.2** server through chat commands. The server component does **not** open a web page and does **not** require the client mod. Players can create and view profiles directly in chat; links are clickable.

Two builds are available:

- **Fabric server mod** — branch `server`
- **Paper plugin** — branch `paper`

Both builds use the same commands, permissions, local JSON storage and JInfo cloud fallback.

## Features

- `/jinfo me` — show your profile in chat.
- `/jinfo view <name|uuid>` — view a local or cloud profile.
- `/jinfo set <text>` — save your server profile.
- `/jinfo photo <url|off>` — set or remove a profile photo.
- `/jinfo clear` — remove your server profile.
- Admin commands: `list`, `import`, `remove`, `reload`.
- Profiles support colours, styles, service placeholders and clickable links.
- Local profiles are stored as JSON files on the server.
- If a profile is not local, it is loaded from the JInfo cloud API.
- No web server, no open ports and no client mod required.

## Requirements

- Minecraft 26.2 server
- Java 25

Fabric build: Fabric Loader 0.20.1+ and Fabric API.
Paper build: Paper 26.2.

## Install

### Fabric

1. Install Fabric Loader on the server.
2. Put Fabric API and `JInfo-Server-Fabric-<version>.jar` into `mods/`.
3. Start the server.
4. Edit `jinfo-server.json` if needed.

### Paper

1. Put `JInfo-Server-Paper-<version>.jar` into `plugins/`.
2. Start the server.
3. Edit `plugins/JInfoPaper/config.yml` if needed.

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/jinfo help` | `jinfo.use` | Show help |
| `/jinfo me` | `jinfo.use` | Show your profile |
| `/jinfo view <name\|uuid>` | `jinfo.use` | View a profile |
| `/jinfo set <text>` | `jinfo.use` | Save your profile |
| `/jinfo photo <url\|off>` | `jinfo.use` | Set or remove a photo |
| `/jinfo clear` | `jinfo.use` | Remove your profile |
| `/jinfo list` | `jinfo.admin` | List local profiles |
| `/jinfo import <uuid> [name]` | `jinfo.admin` | Import from cloud |
| `/jinfo remove <name\|uuid>` | `jinfo.admin` | Remove a local profile |
| `/jinfo reload` | `jinfo.admin` | Reload config |

## Configuration

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

## Profile format

Up to 500 characters, 10 lines and 6 links.

`{youtube}`, `{twitch}`, `{discord}`, `{telegram}`, `{tiktok}`, `{instagram}`, `{x}`, `{github}`, `{steam}`, `{vk}`, `{reddit}`, `{kick}`, `{boosty}`, `{donationalerts}`, `{modrinth}` — followed by `@nick` or an HTTPS link.

Generic link: `{link:Title} https://example.com`. Photo: `{photo} https://example.com/photo.png`.

Colours: `&0`–`&f`, `&l`, `&o`, `&n`, `&m`, `&r`, hex `&#RRGGBB`.

Only HTTPS links are allowed. Unsafe schemes, localhost, IP addresses and links with user info are rejected.

## Privacy

The server stores only profile text, nickname, UUID and update time. Cloud requests are sent only for `view` and `import`. No passwords or tokens are stored.

## Links

- [Repository](https://github.com/mcchopik/jinfo)
- [Releases](https://github.com/mcchopik/jinfo/releases)
- [Issues](https://github.com/mcchopik/jinfo/issues)
- [Client component](../client/en.md)
