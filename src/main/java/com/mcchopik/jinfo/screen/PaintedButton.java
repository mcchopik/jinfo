package com.mcchopik.jinfo.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

public class PaintedButton extends AbstractButton {

    public interface Painter {
        void paint(GuiGraphicsExtractor graphics, PaintedButton button, boolean hovered);
    }

    private final Painter painter;
    private final Runnable action;

    public PaintedButton(int x, int y, int w, int h, Component message,
                         Painter painter, Runnable action) {
        super(x, y, w, h, message);
        this.painter = painter;
        this.action = action;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        action.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        boolean hovered = mouseX >= getX() && mouseX <= getRight()
                && mouseY >= getY() && mouseY <= getBottom();
        painter.paint(graphics, this, hovered);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
