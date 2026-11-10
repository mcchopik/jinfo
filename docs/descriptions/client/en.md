# JInfo Client

**English** | [Русский](ru.md) | [Беларуская](be.md) | [Українська](uk.md)

JInfo Client is a Fabric client mod for Minecraft **26.2** that adds short player profiles: up to 500 characters, 10 lines and 6 links. Profiles work for both premium and offline accounts.

## Features

- **Shift + Right-click a player** — compact card with the player's skin head and nickname. Hovering the head shows the formatted profile.
- **Click the head** — full profile screen with a rotating 3D skin, layers, status, text, photo and service buttons.
- **Key `P`** — opens your own profile (remappable in Controls).
- **Profile skin** — automatic, direct HTTPS URL or a local PNG (classic or slim), with a 3D preview.
- **In-game editor** — colours, styles, placeholders, service links and live preview.
- **Browser editor** — local web editor with `/jinfo web` and `/jinfo web stop`.
- **Mod Menu** — the JInfo settings screen is available from the Mod Menu list.
- **Languages** — Russian, Belarusian, Ukrainian and English.

## Requirements

- Minecraft 26.2
- Java 25
- Fabric Loader 0.20.1 or newer
- Fabric API
- Mod Menu is optional

## Install

1. Install Fabric Loader for Minecraft 26.2.
2. Put `jinfo-<version>.jar` and Fabric API into `.minecraft/mods`.
3. Optionally install Mod Menu.
4. Launch the game and press `P` or run `/jinfo`.

## Commands

| Command | Description |
| --- | --- |
| `/jinfo help` | Show help |
| `/jinfo edit` | Open the in-game editor |
| `/jinfo web` | Start the local browser editor |
| `/jinfo web stop` | Stop the local browser editor and close the tunnel |
| `/jinfo template <id>` | Apply a ready-made template |
| `/jinfo photo <url>` | Set a profile photo |
| `/jinfo preview [nick]` | Preview a profile |
| `/jinfo set <text>` | Set profile text |
| `/jinfo me` | Show your profile |
| `/jinfo view <nick>` | Show another player's profile |
| `/jinfo clear` | Delete your profile |
| `/jinfo settings` | Open settings |
| `/jinfo update` | Check for updates |

## Profile placeholders

`{youtube}`, `{twitch}`, `{discord}`, `{telegram}`, `{tiktok}`, `{instagram}`, `{x}`, `{github}`, `{steam}`, `{vk}`, `{reddit}`, `{kick}`, `{boosty}`, `{donationalerts}`, `{modrinth}` — followed by `@nick` or an HTTPS link.

Generic link: `{link:Title} https://example.com`. Photo: `{photo} https://example.com/photo.png`.

Colours: `&0`–`&f`, `&l`, `&o`, `&n`, `&m`, `&r`, hex `&#RRGGBB`.

## Privacy and security

Only HTTPS links are allowed. `javascript:`, `file:`, localhost, IP addresses and links with user info are rejected. Suspicious addresses are marked in the confirmation screen.

## Links

- [Repository](https://github.com/mcchopik/jinfo)
- [Releases](https://github.com/mcchopik/jinfo/releases)
- [Issues](https://github.com/mcchopik/jinfo/issues)
- [Server component](../server/en.md)
