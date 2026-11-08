package com.mcchopik.jinfo.text;

import java.util.List;

/**
 * Результат разбора профиля.
 *
 * @param parts      части документа для рендера (игра, HTML)
 * @param warnings   общие предупреждения (например, превышены лимиты)
 * @param linkCount  количество валидных ссылок
 * @param charCount  длина после очистки
 * @param lineCount  количество строк
 * @param plainText  простой текст без оформления
 * @param overLimits true, если превышены лимиты (500 символов / 10 строк / 6 ссылок)
 */
public record ParsedProfile(
        List<Part> parts,
        List<String> warnings,
        int linkCount,
        int charCount,
        int lineCount,
        String plainText,
        boolean overLimits) {

    public static final int MAX_CHARS = 500;
    public static final int MAX_LINES = 10;
    public static final int MAX_LINKS = 6;
}