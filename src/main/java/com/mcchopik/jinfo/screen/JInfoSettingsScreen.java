package com.mcchopik.jinfo.screen;

import com.mcchopik.jinfo.config.JInfoConfig;
import com.mcchopik.jinfo.text.Lang;
import com.mcchopik.jinfo.text.Messages;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Настройки мода: включение жеста Shift+ПКМ и язык интерфейса (ru/uk/en).
 */
public class JInfoSettingsScreen extends Screen {

    private final Screen parent;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public JInfoSettingsScreen(Screen parent) {
        super(Component.literal("JInfo"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 20, 320);
        panelH = Math.min(height - 20, 180);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        int x = panelX + 16;
        int w = panelW - 32;
        int y = panelY + 34;

        String gestureState = JInfoConfig.gestureEnabled() ? Messages.s("on") : Messages.s("off");
        addRenderableWidget(Button.builder(
                        Component.literal(Messages.s("gesture") + ": " + gestureState),
                        b -> {
                            JInfoConfig.setGestureEnabled(!JInfoConfig.gestureEnabled());
                            rebuild();
                        })
                .bounds(x, y, w, 20)
                .build());

        y += 26;
        addRenderableWidget(Button.builder(
                        Component.literal(Messages.s("language") + ": " + JInfoConfig.language().name()),
                        b -> {
                            Lang[] values = Lang.values();
                            Lang next = values[(JInfoConfig.language().ordinal() + 1) % values.length];
                            JInfoConfig.setLanguage(next);
                            rebuild();
                        })
                .bounds(x, y, w, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal(Messages.s("back")), b -> onClose())
                .bounds(panelX + (panelW - 90) / 2, panelY + panelH - 28, 90, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0x99000000);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF171B21);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 2, 0xFF2F74C9);

        int tw = font.width(Messages.s("settings"));
        graphics.text(font, Messages.s("settings"), panelX + (panelW - tw) / 2, panelY + 10,
                0xFFFFFFFF, true);

        int y = panelY + 80;
        for (String line : wrap(Messages.s("key_hint"), panelW - 32)) {
            graphics.text(font, line, panelX + 16, y, 0xFFB8C0CC, false);
            y += 10;
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (font.width(line.toString() + c) > maxWidth) {
                out.add(line.toString());
                line.setLength(0);
            }
            line.append(c);
        }
        out.add(line.toString());
        return out;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
        }
    }
}