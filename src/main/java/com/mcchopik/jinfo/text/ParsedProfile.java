package com.mcchopik.jinfo.text;

import java.util.List;

public record ParsedProfile(
        List<Part> parts,
        List<String> warnings,
        int linkCount,
        int charCount,
        int lineCount,
        String plainText,
        boolean overLimits,
        String avatarUrl) {

    public static final int MAX_CHARS = 500;
    public static final int MAX_LINES = 10;
    public static final int MAX_LINKS = 6;
}
