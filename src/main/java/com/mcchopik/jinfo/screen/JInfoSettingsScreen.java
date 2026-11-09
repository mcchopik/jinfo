package com.mcchopik.jinfo.screen;

import com.mcchopik.jinfo.JInfoClient;
import com.mcchopik.jinfo.config.JInfoConfig;
import com.mcchopik.jinfo.text.Lang;
import com.mcchopik.jinfo.text.Messages;
import com.mcchopik.jinfo.web.WebServer;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

public class JInfoSettingsScreen extends Screen {

    private final Screen parent;
    private String note = "";

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
        panelW = Math.min(width - 20, 340);
        panelH = Math.min(height - 20, 300);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        int x = panelX + 16;
        int w = panelW - 32;

        String gestureState = JInfoConfig.gestureEnabled() ? Messages.s("on") : Messages.s("off");
        addRenderableWidget(Button.builder(
                        Component.literal(Messages.s("gesture") + ": " + gestureState),
                        b -> {
                            JInfoConfig.setGestureEnabled(!JInfoConfig.gestureEnabled());
                            rebuild();
                        })
                .bounds(x, panelY + 34, w, 20)
                .build());

        int langY = panelY + 64;
        int gap = 6;
        int lw = (w - 2 * gap) / 3;
        Lang current = JInfoConfig.language();
        Lang[] langs = Lang.values();
        for (int i = 0; i < langs.length; i++) {
            final Lang lang = langs[i];
            String label = lang.name() + (lang == current ? " ✓" : "");
            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                        JInfoConfig.setLanguage(lang);
                        rebuild();
                    })
                    .bounds(x + i * (lw + gap), langY, lw, 20)
                    .build());
        }

        int actionY = panelY + 96;
        addRenderableWidget(Button.builder(Component.literal(Messages.s("open_editor")), b -> openEditor())
                .bounds(x, actionY, w, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal(Messages.s("open_web")), b -> openWeb())
                .bounds(x, actionY + 26, w, 20)
                .build());
        int half = (w - 8) / 2;
        addRenderableWidget(Button.builder(Component.literal(Messages.s("my_profile")), b -> openMyProfile())
                .bounds(x, actionY + 52, half, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal(Messages.s("change_nick")), b -> openNickEditor())
                .bounds(x + half + 8, actionY + 52, w - half - 8, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal(Messages.s("back")), b -> onClose())
                .bounds(panelX + (panelW - 90) / 2, panelY + panelH - 28, 90, 20)
                .build());
    }

    private void openEditor() {
        if (minecraft != null) {
            minecraft.gui.setScreen(new JInfoEditorScreen(JInfoClient.api(), this));
        }
    }

    private void openMyProfile() {
        JInfoClient.requireSelfName(this, name -> {
            if (minecraft != null) {
                minecraft.gui.setScreen(new JInfoProfileScreen(
                        JInfoClient.api(), JInfoClient.selfUuid(name), name, null, this));
            }
        });
    }

    private void openNickEditor() {
        if (minecraft != null) {
            minecraft.gui.setScreen(new JInfoNickScreen(this, JInfoClient.selfName(), nick -> rebuild()));
        }
    }

    private void openWeb() {
        JInfoClient.requireSelfName(this, this::startWeb);
    }

    private void startWeb(String name) {
        note = Messages.s("loading");
        CompletableFuture.runAsync(() -> {
            try {
                String url = WebServer.start(JInfoClient.api(), name);
                if (minecraft != null) {
                    minecraft.execute(() -> {
                        try {
                            Util.getPlatform().openUri(URI.create(url));
                        } catch (Throwable ignored) {
                        }
                    });
                }
                note = url;
            } catch (Exception e) {
                note = Messages.s("err_generic") + ": " + e.getMessage();
            }
        });
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0x99000000);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF171B21);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 2, 0xFF2F74C9);

        graphics.text(font, Messages.s("settings"), panelX + 16, panelY + 10, 0xFFFFFFFF, true);
        graphics.text(font, Messages.s("language"), panelX + 16, panelY + 52, 0xFFB8C0CC, false);

        int y = panelY + 180;
        for (String line : wrap(Messages.s("key_hint"), panelW - 32)) {
            graphics.text(font, line, panelX + 16, y, 0xFFB8C0CC, false);
            y += 10;
        }
        if (!note.isBlank()) {
            graphics.text(font, fit(note, panelW - 32), panelX + 16, panelY + panelH - 44,
                    0xFFFFAA00, false);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private String fit(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (font.width(sb.toString() + text.charAt(i)) > maxWidth - 6) {
                break;
            }
            sb.append(text.charAt(i));
        }
        return sb + "…";
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
