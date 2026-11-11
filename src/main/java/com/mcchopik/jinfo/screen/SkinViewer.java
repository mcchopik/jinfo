package com.mcchopik.jinfo.screen;

import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

public class SkinViewer extends AbstractWidget {

    private static final float ROTATION_SENSITIVITY = 2.5f;
    private static final float DEFAULT_ROTATION_X = -5f;
    private static final float DEFAULT_ROTATION_Y = 30f;
    private static final float ROTATION_X_LIMIT = 50f;
    private static final float MODEL_HEIGHT = 2.125f;
    private static final float PIVOT_Y = -1.0625f;

    private final String playerName;
    private final Supplier<PlayerSkin> skin;
    private final Model.Simple wideModel;
    private final Model.Simple slimModel;
    private final Function<String, ModelPart> wideParts;
    private final Function<String, ModelPart> slimParts;

    private float rotationX = DEFAULT_ROTATION_X;
    private float rotationY = DEFAULT_ROTATION_Y;

    public SkinViewer(int width, int height, String playerName,
                      Supplier<PlayerSkin> skin, EntityModelSet models) {
        super(0, 0, width, height, Component.empty());
        this.playerName = playerName == null ? "?" : playerName;
        this.skin = skin;
        this.wideModel = new Model.Simple(models.bakeLayer(ModelLayers.PLAYER),
                RenderTypes::entityCutout);
        this.slimModel = new Model.Simple(models.bakeLayer(ModelLayers.PLAYER_SLIM),
                RenderTypes::entityCutout);
        this.wideParts = wideModel.root().createPartLookup();
        this.slimParts = slimModel.root().createPartLookup();
    }

    public void setLayer(boolean visible, String... parts) {
        applyLayer(wideParts, visible, parts);
        applyLayer(slimParts, visible, parts);
    }

    private static void applyLayer(Function<String, ModelPart> lookup, boolean visible,
                                   String... parts) {
        for (String part : parts) {
            ModelPart modelPart = lookup.apply(part);
            if (modelPart != null) {
                modelPart.visible = visible;
            }
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics,
                                            int mouseX, int mouseY, float delta) {
        PlayerSkin current = skin == null ? null : skin.get();
        if (current == null) {
            graphics.fill(getX(), getY(), getRight(), getBottom(), 0xFF1B2027);
            Font font = Minecraft.getInstance().font;
            String initial = playerName.isEmpty() ? "?" : playerName.substring(0, 1).toUpperCase();
            int textWidth = font.width(initial);
            graphics.text(font, initial, getX() + (getWidth() - textWidth) / 2,
                    getY() + (getHeight() - 8) / 2, 0xFFFFFFFF, true);
            return;
        }
        Model.Simple model = current.model() == PlayerModelType.SLIM ? slimModel : wideModel;
        float scale = 0.97f * getHeight() / MODEL_HEIGHT;
        graphics.skin(model, current.body().texturePath(), scale, rotationX, rotationY,
                PIVOT_Y, getX(), getY(), getRight(), getBottom());
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        rotationX = Mth.clamp(rotationX - (float) dragY * ROTATION_SENSITIVITY,
                -ROTATION_X_LIMIT, ROTATION_X_LIMIT);
        rotationY += (float) dragX * ROTATION_SENSITIVITY;
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}