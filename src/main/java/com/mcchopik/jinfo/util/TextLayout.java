package com.mcchopik.jinfo.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;

public final class TextLayout {

    private static final String ELLIPSIS = "…";

    private TextLayout() {
    }

    public static String fit(Font font, String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }
        int limit = Math.max(0, maxWidth - font.width(ELLIPSIS));
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (font.width(out.toString() + c) > limit) {
                break;
            }
            out.append(c);
        }
        return out.append(ELLIPSIS).toString();
    }

    public static List<String> wrap(Font font, String text, int maxWidth) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            out.add("");
            return out;
        }
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (line.length() > 0 && font.width(line.toString() + c) > maxWidth) {
                out.add(line.toString());
                line.setLength(0);
            }
            line.append(c);
        }
        out.add(line.toString());
        return out;
    }
}
