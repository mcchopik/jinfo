package com.mcchopik.jinfo.screen;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import com.mcchopik.jinfo.text.Messages;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

public class LinkConfirmScreen extends Screen {

    private final String url;
    private final String serviceLabel;
    private final Screen parent;
    private final boolean suspicious;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public LinkConfirmScreen(String url, String serviceLabel, Screen parent, boolean suspicious) {
        super(Component.literal(Messages.s("open_title")));
        this.url = url;
        this.serviceLabel = serviceLabel;
        this.parent = parent;
        this.suspicious = suspicious;
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 20, 340);
        panelH = 130;
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        int by = panelY + panelH - 26;
        int bw = 100;
        addRenderableWidget(Button.builder(Component.literal(Messages.s("open")), b -> open())
                .bounds(panelX + panelW / 2 - bw - 4, by, bw, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal(Messages.s("cancel")), b -> onClose())
                .bounds(panelX + panelW / 2 + 4, by, bw, 20)
                .build());
    }

    private void open() {
        try {
            Util.getPlatform().openUri(URI.create(url));
        } catch (Throwable ignored) {
        }
        onClose();
    }

    private String host() {
        try {
            return URI.create(url).getHost();
        } catch (Throwable t) {
            return "?";
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0x99000000);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF171B21);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 2, 0xFF2F74C9);

        int x = panelX + 12;
        int y = panelY + 12;
        graphics.text(font, Messages.s("open_title"), x, y, 0xFFFFFFFF, true);
        y += 14;
        graphics.text(font, Messages.s("service") + ": " + serviceLabel, x, y, 0xFFB8C0CC, false);
        y += 12;
        graphics.text(font, Messages.s("domain") + ": " + host(), x, y, 0xFFFFCC55, false);
        y += 16;

        for (String line : wrap(url, panelW - 24)) {
            graphics.text(font, line, x, y, 0xFFE6E8EC, false);
            y += 10;
        }

        if (suspicious) {
            graphics.text(font, Messages.s("suspicious"), x, y + 4,
                    0xFFFF5555, false);
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
