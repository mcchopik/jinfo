package com.mcchopik.jinfo.text;

import java.util.List;

public sealed interface Part {

    record Text(String text, Style style) implements Part {
    }

    record Link(String serviceKey, String label, String colorHex, String url,
                String displayText, boolean verified, List<String> warnings) implements Part {
    }

    record Raw(String text) implements Part {
    }
}
