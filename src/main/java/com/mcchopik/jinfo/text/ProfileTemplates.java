package com.mcchopik.jinfo.text;

public enum ProfileTemplates {

    STREAMER("streamer",
            "&lСтример&r — стримлю и общаюсь с чатом.\n"
                    + "{youtube} @mynick\n"
                    + "{twitch} @mynick\n"
                    + "{discord} https://discord.gg/shyminers\n"
                    + "{telegram} @mynick",
            "&lСтрэмер&r — стрымлю і размаўляю з чатам.\n"
                    + "{youtube} @mynick\n"
                    + "{twitch} @mynick\n"
                    + "{discord} https://discord.gg/shyminers\n"
                    + "{telegram} @mynick",
            "&lСтрімер&r — стрімлю та спілкуюся з чатом.\n"
                    + "{youtube} @mynick\n"
                    + "{twitch} @mynick\n"
                    + "{discord} https://discord.gg/shyminers\n"
                    + "{telegram} @mynick",
            "&lStreamer&r — I stream and chat with viewers.\n"
                    + "{youtube} @mynick\n"
                    + "{twitch} @mynick\n"
                    + "{discord} https://discord.gg/shyminers\n"
                    + "{telegram} @mynick"),

    DEVELOPER("developer",
            "&lРазработчик&r — пишу моды на Fabric.\n"
                    + "{github} @mynick\n"
                    + "{modrinth} @mynick\n"
                    + "{discord} https://discord.gg/shyminers\n"
                    + "{link:Сайт} https://example.com",
            "&lРаспрацоўшчык&r — пішу моды на Fabric.\n"
                    + "{github} @mynick\n"
                    + "{modrinth} @mynick\n"
                    + "{discord} https://discord.gg/shyminers\n"
                    + "{link:Сайт} https://example.com",
            "&lРозробник&r — пишу моди на Fabric.\n"
                    + "{github} @mynick\n"
                    + "{modrinth} @mynick\n"
                    + "{discord} https://discord.gg/shyminers\n"
                    + "{link:Сайт} https://example.com",
            "&lDeveloper&r — I write Fabric mods.\n"
                    + "{github} @mynick\n"
                    + "{modrinth} @mynick\n"
                    + "{discord} https://discord.gg/shyminers\n"
                    + "{link:Website} https://example.com"),

    GAMER("gamer",
            "&lГеймер&r — Minecraft и не только.\n"
                    + "{steam} @mynick\n"
                    + "{discord} https://discord.gg/shyminers\n"
                    + "{youtube} @mynick",
            "&lГеймер&r — Minecraft і не толькі.\n"
                    + "{steam} @mynick\n"
                    + "{discord} https://discord.gg/shyminers\n"
                    + "{youtube} @mynick",
            "&lГеймер&r — Minecraft і не лише.\n"
                    + "{steam} @mynick\n"
                    + "{discord} https://discord.gg/shyminers\n"
                    + "{youtube} @mynick",
            "&lGamer&r — Minecraft and more.\n"
                    + "{steam} @mynick\n"
                    + "{discord} https://discord.gg/shyminers\n"
                    + "{youtube} @mynick"),

    SIMPLE("simple",
            "&lПривет!&r Это мой профиль.\n"
                    + "{discord} https://discord.gg/shyminers\n"
                    + "{telegram} @mynick",
            "&lПрывітанне!&r Гэта мой профіль.\n"
                    + "{discord} https://discord.gg/shyminers\n"
                    + "{telegram} @mynick",
            "&lПривіт!&r Це мій профіль.\n"
                    + "{discord} https://discord.gg/shyminers\n"
                    + "{telegram} @mynick",
            "&lHi!&r This is my profile.\n"
                    + "{discord} https://discord.gg/shyminers\n"
                    + "{telegram} @mynick");

    private final String id;
    private final String ru;
    private final String be;
    private final String uk;
    private final String en;

    ProfileTemplates(String id, String ru, String be, String uk, String en) {
        this.id = id;
        this.ru = ru;
        this.be = be;
        this.uk = uk;
        this.en = en;
    }

    public String id() {
        return id;
    }

    public String label(Lang lang) {
        return Messages.s("template." + id);
    }

    public String text(Lang lang) {
        return switch (lang) {
            case RU -> ru;
            case BE -> be;
            case UK -> uk;
            case EN -> en;
        };
    }

    public static ProfileTemplates byId(String id) {
        if (id == null) {
            return null;
        }
        for (ProfileTemplates t : values()) {
            if (t.id.equalsIgnoreCase(id)) {
                return t;
            }
        }
        return null;
    }
}
