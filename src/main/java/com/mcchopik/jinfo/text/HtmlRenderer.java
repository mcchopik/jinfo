package com.mcchopik.jinfo.text;

import java.util.List;

public final class HtmlRenderer {

    public enum Mode {

        TOOLTIP,

        FULL
    }

    private HtmlRenderer() {
    }

    public static String render(ParsedProfile profile, Mode mode) {
        StringBuilder sb = new StringBuilder();
        for (Part part : profile.parts()) {
            renderPart(sb, part, mode);
        }
        return sb.toString();
    }

    public static String renderParts(List<Part> parts, Mode mode) {
        StringBuilder sb = new StringBuilder();
        for (Part part : parts) {
            renderPart(sb, part, mode);
        }
        return sb.toString();
    }

    private static void renderPart(StringBuilder sb, Part part, Mode mode) {
        if (part instanceof Part.Text t) {
            renderStyledText(sb, t.text(), t.style());
        } else if (part instanceof Part.Raw r) {
            sb.append("<span class=\"raw\">").append(escape(r.text())).append("</span>");
        } else if (part instanceof Part.Link l) {
            renderLink(sb, l, mode);
        }
    }

    private static void renderStyledText(StringBuilder sb, String text, Style style) {
        boolean openFont = style.colorHex() != null;
        java.util.List<String> opened = new java.util.ArrayList<>();
        if (openFont) {
            sb.append("<font color=\"").append(escapeAttr(style.colorHex())).append("\">");
        }
        if (style.bold()) {
            sb.append("<b>");
            opened.add("b");
        }
        if (style.italic()) {
            sb.append("<i>");
            opened.add("i");
        }
        if (style.underline()) {
            sb.append("<u>");
            opened.add("u");
        }
        if (style.strikethrough()) {
            sb.append("<s>");
            opened.add("s");
        }
        sb.append(escape(text));
        for (int i = opened.size() - 1; i >= 0; i--) {
            sb.append("</").append(opened.get(i)).append(">");
        }
        if (openFont) {
            sb.append("</font>");
        }
    }

    private static void renderLink(StringBuilder sb, Part.Link l, Mode mode) {
        String classes = "svc svc-" + safeClass(l.serviceKey());
        if (l.verified()) {
            classes += " verified";
        } else {
            classes += " generic";
        }
        if (!l.warnings().isEmpty()) {
            classes += " warn";
        }
        sb.append("<span class=\"").append(classes).append("\"")
                .append(" data-url=\"").append(escapeAttr(l.url())).append("\"")
                .append(" title=\"").append(escapeAttr(l.url())).append("\"");
        if (mode == Mode.FULL) {
            sb.append(" role=\"button\" tabindex=\"0\"");
        }
        sb.append(">");
        sb.append(escape(l.label())).append(": ").append(escape(l.displayText()));
        sb.append("</span>");
    }

    private static String safeClass(String key) {
        return key.replaceAll("[^a-z0-9_-]", "");
    }

    public static String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    public static String escapeAttr(String s) {
        return escape(s);
    }
}
