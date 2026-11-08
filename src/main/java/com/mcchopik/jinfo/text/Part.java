package com.mcchopik.jinfo.text;

import java.util.List;

/**
 * Часть разобранного профиля.
 */
public sealed interface Part {

    /** Обычный текст со стилем. */
    record Text(String text, Style style) implements Part {
    }

    /**
     * Ссылка-сервис (валидная). verified = внутренний сервис из реестра.
     * warnings — предупреждения (сокращатели, punycode, произвольный домен).
     */
    record Link(String serviceKey, String label, String colorHex, String url,
                String displayText, boolean verified, List<String> warnings) implements Part {
    }

    /**
     * Некорректный плейсхолдер — показывается как обычный текст без кнопки.
     */
    record Raw(String text) implements Part {
    }
}