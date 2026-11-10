# JInfo Server

[English](en.md) | [Русский](ru.md) | **Беларуская** | [Українська](uk.md)

JInfo Server дадае профілі гульцоў на сервер Minecraft **26.2** праз каманды ў чаце. Серверная частка **не адкрывае вэб-старонку** і **не патрабуе кліенцкага мода**. Гульцы ствараюць і глядзяць профілі прама ў чаце; спасылкі клікабельныя.

Даступныя дзве зборкі:

- **Fabric server mod** — галіна `server`
- **Paper plugin** — галіна `paper`

Абедзве зборкі выкарыстоўваюць тыя ж каманды, правы, лакальнае JSON-сховішча і воблачны fallback JInfo.

## Магчымасці

- `/jinfo me` — паказаць свой профіль у чаце.
- `/jinfo view <нік|uuid>` — паглядзець лакальны або воблачны профіль.
- `/jinfo set <тэкст>` — захаваць свой серверны профіль.
- `/jinfo photo <url|off>` — устанавіць або прыбраць фота профілю.
- `/jinfo clear` — выдаліць свой серверны профіль.
- Адмін-каманды: `list`, `import`, `remove`, `reload`.
- Падтрымка колераў, стыляў, плэйсхолдараў сэрвісаў і клікабельных спасылак.
- Лакальныя профілі захоўваюцца ў JSON-файлах на серверы.
- Калі профілю няма лакальна, ён загружаецца з воблачнага API JInfo.
- Без вэб-сервера, без адкрытых партоў і без кліенцкага мода.

## Патрабаванні

- Сервер Minecraft 26.2
- Java 25

Fabric: Fabric Loader 0.20.1+ і Fabric API.
Paper: Paper 26.2.

## Усталёўка

### Fabric

1. Усталюйце Fabric Loader на сервер.
2. Пакладзіце Fabric API і `JInfo-Server-Fabric-<version>.jar` у `mods/`.
3. Запусціце сервер.
4. Пры неабходнасці адрэдагуйце `jinfo-server.json`.

### Paper

1. Пакладзіце `JInfo-Server-Paper-<version>.jar` у `plugins/`.
2. Запусціце сервер.
3. Пры неабходнасці адрэдагуйце `plugins/JInfoPaper/config.yml`.

## Каманды

| Каманда | Права | Апісанне |
| --- | --- | --- |
| `/jinfo help` | `jinfo.use` | Паказаць даведку |
| `/jinfo me` | `jinfo.use` | Паказаць свой профіль |
| `/jinfo view <нік\|uuid>` | `jinfo.use` | Паглядзець профіль |
| `/jinfo set <тэкст>` | `jinfo.use` | Захаваць свой профіль |
| `/jinfo photo <url\|off>` | `jinfo.use` | Устанавіць або прыбраць фота |
| `/jinfo clear` | `jinfo.use` | Выдаліць свой профіль |
| `/jinfo list` | `jinfo.admin` | Спіс лакальных профіляў |
| `/jinfo import <uuid> [нік]` | `jinfo.admin` | Імпарт з воблака |
| `/jinfo remove <нік\|uuid>` | `jinfo.admin` | Выдаліць лакальны профіль |
| `/jinfo reload` | `jinfo.admin` | Перазагрузіць канфіг |

## Канфігурацыя

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

## Фармат профілю

Да 500 сімвалаў, 10 радкоў і 6 спасылак.

`{youtube}`, `{twitch}`, `{discord}`, `{telegram}`, `{tiktok}`, `{instagram}`, `{x}`, `{github}`, `{steam}`, `{vk}`, `{reddit}`, `{kick}`, `{boosty}`, `{donationalerts}`, `{modrinth}` — далей `@нік` або HTTPS-спасылка.

Універсальная спасылка: `{link:Назва} https://example.com`. Фота: `{photo} https://example.com/photo.png`.

Колеры: `&0`–`&f`, `&l`, `&o`, `&n`, `&m`, `&r`, hex `&#RRGGBB`.

Дазволены толькі HTTPS-спасылкі. Небяспечныя схемы, localhost, IP-адрасы і спасылкі з лагінам адхіляюцца.

## Прыватнасць

Сервер захоўвае толькі тэкст профілю, нік, UUID і час абнаўлення. Запыты ў воблака выконваюцца толькі для `view` і `import`. Паролі і токены не захоўваюцца.

## Спасылкі

- [Рэпазіторый](https://github.com/mcchopik/jinfo)
- [Рэлізы](https://github.com/mcchopik/jinfo/releases)
- [Праблемы](https://github.com/mcchopik/jinfo/issues)
- [Кліенцкая частка](../client/be.md)
