package com.mcchopik.jinfo.screen;

import com.mcchopik.jinfo.JInfoClient;
import com.mcchopik.jinfo.config.JInfoConfig;
import com.mcchopik.jinfo.text.Lang;
import com.mcchopik.jinfo.text.Messages;
import com.mcchopik.jinfo.util.TextLayout;
import com.mcchopik.jinfo.util.UiColors;
import com.mcchopik.jinfo.web.WebServer;
import java.net.URI;
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
        panelH = Math.min(height - 20, 370);
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
        int lw = (w - 3 * gap) / 4;
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
        Button stopButton = Button.builder(Component.literal(Messages.s("stop_web")), b -> stopWeb())
                .bounds(x, actionY + 52, w, 20)
                .build();
        stopButton.active = WebServer.isRunning();
        addRenderableWidget(stopButton);
        addRenderableWidget(Button.builder(Component.literal(Messages.s("skin_title")), b -> openSkin())
                .bounds(x, actionY + 78, w, 20)
                .build());
        int half = (w - 8) / 2;
        addRenderableWidget(Button.builder(Component.literal(Messages.s("my_profile")), b -> openMyProfile())
                .bounds(x, actionY + 104, half, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal(Messages.s("change_nick")), b -> openNickEditor())
                .bounds(x + half + 8, actionY + 104, w - half - 8, 20)
                .build());
        String accountName = JInfoClient.api().currentName();
        String accountLabel;
        if (JInfoClient.api().isAccount() && accountName != null) {
            accountLabel = Messages.s("account_logged") + accountName;
        } else if (JInfoClient.api().isGuest()) {
            accountLabel = Messages.s("account_guest_mode")
                    + (accountName == null ? "" : ": " + accountName);
        } else {
            accountLabel = Messages.s("account_title");
        }
        addRenderableWidget(Button.builder(Component.literal(accountLabel), b -> openAccount())
                .bounds(x, actionY + 130, w, 20)
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

    private void openSkin() {
        if (minecraft != null) {
            minecraft.gui.setScreen(new JInfoSkinScreen(this));
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

    private void openAccount() {
        if (minecraft != null) {
            minecraft.gui.setScreen(new JInfoAccountScreen(this));
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
                onClient(() -> {
                    note = url;
                    rebuild();
                    try {
                        Util.getPlatform().openUri(URI.create(url));
                    } catch (Throwable ignored) {
                    }
                });
            } catch (Exception e) {
                onClient(() -> note = Messages.s("err_generic") + ": " + e.getMessage());
            }
        });
    }

    private void stopWeb() {
        WebServer.stop("settings");
        note = Messages.s("web_stopped");
        rebuild();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, UiColors.OVERLAY);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, UiColors.PANEL);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 2, UiColors.BORDER);

        graphics.text(font, Messages.s("settings"), panelX + 16, panelY + 10, UiColors.TEXT, true);
        graphics.text(font, Messages.s("language"), panelX + 16, panelY + 52, UiColors.TEXT_LABEL, false);

        int y = panelY + 262;
        for (String line : TextLayout.wrap(font, Messages.s("key_hint"), panelW - 32)) {
            graphics.text(font, line, panelX + 16, y, UiColors.TEXT_LABEL, false);
            y += 10;
        }
        if (!note.isBlank()) {
            graphics.text(font, TextLayout.fit(font, note, panelW - 32), panelX + 16, panelY + panelH - 44,
                    UiColors.WARN, false);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void onClient(Runnable action) {
        if (minecraft != null) {
            minecraft.execute(action);
        } else {
            action.run();
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
        }
    }
}
