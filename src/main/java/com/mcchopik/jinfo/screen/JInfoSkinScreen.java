package com.mcchopik.jinfo.screen;

import com.mcchopik.jinfo.JInfoClient;
import com.mcchopik.jinfo.config.JInfoConfig;
import com.mcchopik.jinfo.net.ApiClient;
import com.mcchopik.jinfo.net.SkinTextures;
import com.mcchopik.jinfo.text.Messages;
import java.io.File;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;

public class JInfoSkinScreen extends Screen {

    private static final int PANEL_W = 360;
    private static final int PANEL_H = 300;
    private static final int PREVIEW_SIZE = 96;

    private final Screen parent;
    private final ApiClient api;

    private EditBox sourceField;
    private Button modeAutoBtn;
    private Button modeUrlBtn;
    private Button modeFileBtn;
    private Button modelBtn;
    private Button browseBtn;
    private Button loadBtn;
    private Button clearBtn;
    private SkinViewer previewViewer;
    private String statusText = "";
    private int statusColor = 0xFFAAAAAA;
    private boolean loading = false;

    private int panelX;
    private int panelY;
    private int previewX;
    private int previewY;

    public JInfoSkinScreen(Screen parent) {
        super(Component.literal("JInfo"));
        this.parent = parent;
        this.api = JInfoClient.api();
    }

    @Override
    protected void init() {
        panelX = (width - PANEL_W) / 2;
        panelY = (height - PANEL_H) / 2;

        previewX = panelX + PANEL_W - PREVIEW_SIZE - 16;
        previewY = panelY + 36;

        buildWidgets();
        updatePreview();
    }

    private void buildWidgets() {
        clearWidgets();

        int x = panelX + 16;
        int y = panelY + 36;
        int w = 220;
        int bw = 100;
        int gap = 6;

        addRenderableWidget(Button.builder(
                        Component.literal(Messages.s("skin_mode_auto")),
                        b -> setMode(JInfoConfig.SkinMode.AUTO))
                .bounds(x, y, w, 20)
                .build());
        y += 26;

        addRenderableWidget(Button.builder(
                        Component.literal(Messages.s("skin_mode_url")),
                        b -> setMode(JInfoConfig.SkinMode.URL))
                .bounds(x, y, bw, 20)
                .build());
        addRenderableWidget(Button.builder(
                        Component.literal(Messages.s("skin_mode_file")),
                        b -> setMode(JInfoConfig.SkinMode.FILE))
                .bounds(x + bw + gap, y, bw, 20)
                .build());
        y += 26;

        sourceField = new EditBox(font, x, y, w, 20, Component.literal(""));
        sourceField.setMaxLength(512);
        sourceField.setValue(getSourceValue());
        sourceField.setResponder(s -> {});
        addRenderableWidget(sourceField);
        y += 26;

        browseBtn = Button.builder(
                        Component.literal(Messages.s("skin_browse")),
                        b -> browseFile())
                .bounds(x, y, w, 20)
                .build();
        addRenderableWidget(browseBtn);
        y += 26;

        modelBtn = Button.builder(
                        Component.literal(JInfoConfig.skinSlim() ? Messages.s("skin_model_slim") : Messages.s("skin_model_classic")),
                        b -> {
                            JInfoConfig.setSkinSlim(!JInfoConfig.skinSlim());
                            modelBtn.setMessage(Component.literal(JInfoConfig.skinSlim() ? Messages.s("skin_model_slim") : Messages.s("skin_model_classic")));
                            updatePreview();
                        })
                .bounds(x, y, w, 20)
                .build();
        addRenderableWidget(modelBtn);
        y += 26;

        loadBtn = Button.builder(
                        Component.literal(Messages.s("skin_load")),
                        b -> loadSkin())
                .bounds(x, y, (w - gap) / 2, 20)
                .build();
        addRenderableWidget(loadBtn);

        clearBtn = Button.builder(
                        Component.literal(Messages.s("skin_clear")),
                        b -> clearSkin())
                .bounds(x + (w - gap) / 2 + gap, y, (w - gap) / 2, 20)
                .build();
        addRenderableWidget(clearBtn);

        addRenderableWidget(Button.builder(
                        Component.literal(Messages.s("back")),
                        b -> onClose())
                .bounds(panelX + (PANEL_W - 90) / 2, panelY + PANEL_H - 28, 90, 20)
                .build());

        updateModeButtons();
    }

    private void setMode(JInfoConfig.SkinMode mode) {
        JInfoConfig.setSkinMode(mode);
        if (mode == JInfoConfig.SkinMode.URL) {
            sourceField.setValue(JInfoConfig.skinUrl());
        } else if (mode == JInfoConfig.SkinMode.FILE) {
            sourceField.setValue(JInfoConfig.skinFile());
        } else {
            sourceField.setValue("");
        }
        updateModeButtons();
        updatePreview();
    }

    private String getSourceValue() {
        return switch (JInfoConfig.skinMode()) {
            case URL -> JInfoConfig.skinUrl();
            case FILE -> JInfoConfig.skinFile();
            default -> "";
        };
    }

    private void updateModeButtons() {
        boolean isAuto = JInfoConfig.skinMode() == JInfoConfig.SkinMode.AUTO;
        boolean isUrl = JInfoConfig.skinMode() == JInfoConfig.SkinMode.URL;
        boolean isFile = JInfoConfig.skinMode() == JInfoConfig.SkinMode.FILE;
        sourceField.setVisible(!isAuto);
        browseBtn.visible = isFile;
        modelBtn.visible = !isAuto;
        loadBtn.active = !isAuto && !getSourceValue().isBlank() && !loading;
        clearBtn.active = !isAuto && !loading;
    }

    private void browseFile() {
        if (JInfoConfig.skinMode() != JInfoConfig.SkinMode.FILE) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                java.awt.FileDialog dialog = new java.awt.FileDialog((java.awt.Frame) null, "Выберите PNG скин", java.awt.FileDialog.LOAD);
                dialog.setFilenameFilter((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".png"));
                dialog.setVisible(true);
                String file = dialog.getFile();
                String dir = dialog.getDirectory();
                if (file != null && dir != null) {
                    String path = new File(dir, file).getAbsolutePath();
                    if (minecraft != null) {
                        minecraft.execute(() -> {
                            sourceField.setValue(path);
                            JInfoConfig.setSkinFile(path);
                            updateModeButtons();
                        });
                    }
                }
            } catch (Throwable ignored) {
            }
        });
    }

    private void loadSkin() {
        String src = sourceField.getValue().trim();
        if (src.isBlank()) {
            setStatus(Messages.s("skin_error") + Messages.s("skin_invalid_url"), 0xFFFF5555);
            return;
        }
        loading = true;
        updateModeButtons();
        setStatus(Messages.s("skin_loading"), 0xFFAAAAAA);

        String key;
        boolean slim = JInfoConfig.skinSlim();
        if (JInfoConfig.skinMode() == JInfoConfig.SkinMode.URL) {
            if (!src.toLowerCase(Locale.ROOT).startsWith("https://")) {
                loading = false;
                updateModeButtons();
                setStatus(Messages.s("skin_error") + Messages.s("skin_invalid_url"), 0xFFFF5555);
                return;
            }
            JInfoConfig.setSkinUrl(src);
            key = SkinTextures.cacheKey(src, null, slim);
        } else {
            JInfoConfig.setSkinFile(src);
            key = SkinTextures.cacheKey(null, src, slim);
        }

        SkinTextures.loadAsync(key,
                JInfoConfig.skinMode() == JInfoConfig.SkinMode.URL ? src : null,
                JInfoConfig.skinMode() == JInfoConfig.SkinMode.FILE ? src : null,
                slim);

        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 200 && loading; i++) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                if (SkinTextures.get(key) != null || !SkinTextures.isLoading(key)) {
                    break;
                }
            }
            if (minecraft != null) {
                minecraft.execute(() -> {
                    loading = false;
                    if (SkinTextures.get(key) != null) {
                        setStatus(Messages.s("skin_loaded"), 0xFF55FF55);
                        updatePreview();
                    } else {
                        setStatus(Messages.s("skin_error") + Messages.s("skin_invalid_dims"), 0xFFFF5555);
                    }
                    updateModeButtons();
                });
            }
        });
    }

    private void clearSkin() {
        JInfoConfig.setSkinMode(JInfoConfig.SkinMode.AUTO);
        SkinTextures.clear();
        sourceField.setValue("");
        setStatus(Messages.s("skin_cleared"), 0xFF55FF55);
        updateModeButtons();
        updatePreview();
    }

    private void setStatus(String text, int color) {
        statusText = text;
        statusColor = color;
    }

    private void updatePreview() {
        PlayerSkin skin = null;
        if (JInfoConfig.skinMode() != JInfoConfig.SkinMode.AUTO) {
            String key = SkinTextures.cacheKey(
                    JInfoConfig.skinMode() == JInfoConfig.SkinMode.URL ? JInfoConfig.skinUrl() : null,
                    JInfoConfig.skinMode() == JInfoConfig.SkinMode.FILE ? JInfoConfig.skinFile() : null,
                    JInfoConfig.skinSlim());
            skin = SkinTextures.get(key);
        }
        if (previewViewer != null) {
            removeWidget(previewViewer);
        }
        previewViewer = new SkinViewer(PREVIEW_SIZE, PREVIEW_SIZE, "Preview",
                () -> skin, Minecraft.getInstance().getEntityModels());
        previewViewer.setX(previewX);
        previewViewer.setY(previewY);
        addRenderableWidget(previewViewer);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0x99000000);
        graphics.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xFF171B21);
        graphics.fill(panelX, panelY, panelX + PANEL_W, panelY + 2, 0xFF2F74C9);

        graphics.text(font, Messages.s("skin_title"), panelX + 16, panelY + 10, 0xFFFFFFFF, true);
        graphics.text(font, Messages.s("skin_mode"), panelX + 16, panelY + 24, 0xFFB8C0CC, false);

        if (JInfoConfig.skinMode() != JInfoConfig.SkinMode.AUTO) {
            graphics.text(font, Messages.s("skin_model"), panelX + 16, panelY + 88, 0xFFB8C0CC, false);
            graphics.text(font, Messages.s("skin_url"), panelX + 16, panelY + 62, 0xFFB8C0CC, false);
        }
        if (JInfoConfig.skinMode() == JInfoConfig.SkinMode.FILE) {
            graphics.text(font, Messages.s("skin_local_note"), panelX + 16, panelY + PANEL_H - 50, 0xFF888888, false);
        }

        if (!statusText.isBlank()) {
            graphics.text(font, statusText, panelX + 16, panelY + PANEL_H - 34, statusColor, false);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
        }
    }
}