package com.mcchopik.jinfo.compat;

import com.mcchopik.jinfo.screen.JInfoSettingsScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class JInfoModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new JInfoSettingsScreen(parent);
    }
}
