package com.mcchopik.jinfo.text;

public enum Lang {
    RU("ru"),
    UK("uk"),
    EN("en");

    private final String code;

    Lang(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Lang fromCode(String code) {
        if (code == null) {
            return RU;
        }
        String c = code.trim().toLowerCase();
        if (c.startsWith("uk") || c.startsWith("ua")) {
            return UK;
        }
        if (c.startsWith("en")) {
            return EN;
        }
        return RU;
    }
}
