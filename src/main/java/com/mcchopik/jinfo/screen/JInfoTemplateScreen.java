package com.mcchopik.jinfo.screen;

import com.mcchopik.jinfo.config.JInfoConfig;
import com.mcchopik.jinfo.text.Messages;
import com.mcchopik.jinfo.text.ProfileTemplates;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class JInfoTemplateScreen extends Screen {

    private final JInfoEditorScreen editor;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public JInfoTemplateScreen(JInfoEditorScreen editor) {
        super(Component.literal("JInfo"));
        this.editor = editor;
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 20, 300);
        panelH = Math.min(height - 20, 60 + ProfileTemplates.values().length * 26 + 28);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        int x = panelX + 16;
        int w = panelW - 32;
        int y = panelY + 40;
        for (ProfileTemplates template : ProfileTemplates.values()) {
            final ProfileTemplates picked = template;
            addRenderableWidget(Button.builder(
                            Component.literal(template.label(JInfoConfig.language())),
                            b -> pick(picked))
                    .bounds(x, y, w, 20)
                    .build());
            y += 26;
        }

        addRenderableWidget(Button.builder(Component.literal(Messages.s("back")), b -> onClose())
                .bounds(panelX + (panelW - 90) / 2, panelY + panelH - 26, 90, 20)
                .build());
    }

    private void pick(ProfileTemplates template) {
        editor.applyTemplate(template.text(JInfoConfig.language()));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0x99000000);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF171B21);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 2, 0xFF2F74C9);

        graphics.text(font, Messages.s("templates"), panelX + 16, panelY + 10, 0xFFFFFFFF, true);
        graphics.text(font, Messages.s("template_hint"), panelX + 16, panelY + 24, 0xFFB8C0CC, false);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.gui.setScreen(editor);
        }
    }
}
