package com.mcchopik.jinfo.compat;

import com.mcchopik.jinfo.screen.JInfoSettingsScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Точка входа Mod Menu. Загружается только если установлен Mod Menu,
 * поэтому зависимость compileOnly.
 */
public final class JInfoModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return JInfoSettingsScreen::new;
    }
}