package com.mcchopik.jinfo.screen;

import com.mcchopik.jinfo.config.JInfoConfig;
import com.mcchopik.jinfo.text.Messages;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class JInfoNickScreen extends Screen {

    private final Screen parent;
    private final String initial;
    private final Consumer<String> onAccept;

    private EditBox nickField;
    private boolean remember = true;
    private String error = "";

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public JInfoNickScreen(Screen parent, String initial, Consumer<String> onAccept) {
        super(Component.literal("JInfo"));
        this.parent = parent;
        this.initial = initial == null ? "" : initial;
        this.onAccept = onAccept;
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 20, 300);
        panelH = Math.min(height - 20, 170);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        int x = panelX + 16;
        int w = panelW - 32;

        nickField = new EditBox(font, x, panelY + 58, w, 20, Component.literal("nick"));
        nickField.setMaxLength(16);
        nickField.setValue(initial);
        nickField.setResponder(v -> error = "");
        addRenderableWidget(nickField);

        addRenderableWidget(Checkbox.builder(Component.literal(Messages.s("nick_remember")), font)
                .pos(x, panelY + 84)
                .selected(remember)
                .onValueChange((box, value) -> remember = value)
                .build());

        int by = panelY + panelH - 30;
        int bw = (w - 8) / 2;
        addRenderableWidget(Button.builder(Component.literal(Messages.s("cancel")), b -> onClose())
                .bounds(x, by, bw, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal(Messages.s("nick_continue")), b -> confirm())
                .bounds(x + bw + 8, by, w - bw - 8, 20)
                .build());
        setInitialFocus(nickField);
    }

    private void confirm() {
        String nick = nickField.getValue().trim();
        if (nick.isEmpty()) {
            error = Messages.s("nick_invalid");
            return;
        }
        JInfoConfig.setNickname(remember ? nick : "");
        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
        }
        if (onAccept != null) {
            onAccept.accept(nick);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0x99000000);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF171B21);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 2, 0xFF2F74C9);
        graphics.text(font, Messages.s("nick_title"), panelX + 16, panelY + 10, 0xFFFFFFFF, true);

        int y = panelY + 30;
        for (String line : wrap(Messages.s("nick_hint"), panelW - 32)) {
            graphics.text(font, line, panelX + 16, y, 0xFFB8C0CC, false);
            y += 10;
        }
        if (!error.isBlank()) {
            graphics.text(font, error, panelX + 16, panelY + panelH - 50, 0xFFFF5555, false);
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
        }
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
}
