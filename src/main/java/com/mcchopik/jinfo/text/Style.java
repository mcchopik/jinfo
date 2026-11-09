package com.mcchopik.jinfo.text;

public record Style(String colorHex, boolean bold, boolean italic, boolean underline, boolean strikethrough) {

    public static final Style DEFAULT = new Style(null, false, false, false, false);

    public Style withColor(String hex) {
        return new Style(hex, bold, italic, underline, strikethrough);
    }

    public Style withBold(boolean v) {
        return new Style(colorHex, v, italic, underline, strikethrough);
    }

    public Style withItalic(boolean v) {
        return new Style(colorHex, bold, v, underline, strikethrough);
    }

    public Style withUnderline(boolean v) {
        return new Style(colorHex, bold, italic, v, strikethrough);
    }

    public Style withStrikethrough(boolean v) {
        return new Style(colorHex, bold, italic, underline, v);
    }

    public Style reset() {
        return DEFAULT;
    }
}
