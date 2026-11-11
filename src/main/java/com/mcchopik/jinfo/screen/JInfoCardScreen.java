package com.mcchopik.jinfo.screen;

import com.mcchopik.jinfo.JInfoClient;
import com.mcchopik.jinfo.config.JInfoConfig;
import com.mcchopik.jinfo.net.ApiClient;
import com.mcchopik.jinfo.net.ApiException;
import com.mcchopik.jinfo.net.ProfileData;
import com.mcchopik.jinfo.net.SkinHelper;
import com.mcchopik.jinfo.net.SkinSpec;
import com.mcchopik.jinfo.net.SkinTextures;
import com.mcchopik.jinfo.text.Messages;
import com.mcchopik.jinfo.text.ParsedProfile;
import com.mcchopik.jinfo.text.TextParser;
import com.mcchopik.jinfo.util.TextLayout;
import com.mcchopik.jinfo.util.UiColors;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

public class JInfoCardScreen extends Screen {

    private static final int PANEL_W = 200;
    private static final int PANEL_H = 150;
    private static final int HEAD = 40;
    private static final int TOOLTIP_MAX_WIDTH = 220;
    private static final int TOOLTIP_LINE_HEIGHT = 10;

    private final ApiClient api;
    private final String uuid;
    private final String playerName;
    private final Object skinSource;
    private final Screen parent;
    private final TextParser parser = new TextParser();

    private Identifier skin;
    private ParsedProfile parsed;
    private String statusText = Messages.s("loading");
    private int statusColor = UiColors.TEXT_MUTED;
    private boolean hasProfile = false;
    private boolean verified = false;

    private int panelX;
    private int panelY;
    private int headX;
    private int headY;

    public JInfoCardScreen(ApiClient api, String uuid, String playerName,
                           Object skinSource, Screen parent) {
        super(Component.literal("JInfo"));
        this.api = api;
        this.uuid = uuid;
        this.playerName = playerName == null ? "?" : playerName;
        this.skinSource = skinSource;
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelX = (width - PANEL_W) / 2;
        panelY = (height - PANEL_H) / 2;
        headX = panelX + (PANEL_W - HEAD) / 2;
        headY = panelY + 10;
        skin = SkinHelper.texture(skinSource);

        int bw = 80;
        int by = panelY + PANEL_H - 26;
        addRenderableWidget(new PaintedButton(headX, headY, HEAD, HEAD,
                Component.literal(Messages.s("open_profile")), (g, b, hovered) -> {
                }, this::openProfile));
        addRenderableWidget(Button.builder(Component.literal(Messages.s("open_profile")), b -> openProfile())
                .bounds(panelX + (PANEL_W - bw) / 2, by, bw, 18)
                .build());

        loadProfile();
    }

    private void openProfile() {
        if (minecraft != null) {
            minecraft.gui.setScreen(new JInfoProfileScreen(api, uuid, playerName, skinSource, parent));
        }
    }

    private void loadProfile() {
        CompletableFuture.runAsync(() -> {
            try {
                if (!api.isLoggedIn()) {
                    String self = JInfoClient.selfName();
                    if (self != null) {
                        api.login(self);
                    }
                }
                ProfileData data = api.getProfile(uuid);
                ParsedProfile p = parser.parse(data.data(), JInfoConfig.language());
                String skinJson = data.skin();
                onClient(() -> {
                    parsed = p;
                    hasProfile = true;
                    verified = data.verified();
                    statusText = verified ? Messages.s("verified") : "";
                    statusColor = UiColors.OK;
                    SkinSpec spec = SkinSpec.parse(skinJson);
                    if (spec != null) {
                        SkinTextures.loadAsync(spec.cacheKey(), spec.url(), null, spec.slim(),
                                loaded -> applySkin(loaded));
                    }
                });
            } catch (ApiException e) {
                onClient(() -> {
                    if (e.statusCode() == 404) {
                        hasProfile = false;
                        statusText = Messages.s("not_filled");
                        statusColor = UiColors.TEXT_MUTED;
                    } else if (e.statusCode() == 429) {
                        statusText = Messages.s("rate_limited");
                        statusColor = UiColors.WARN;
                    } else {
                        statusText = Messages.s("net_error");
                        statusColor = UiColors.ERROR;
                    }
                });
            } catch (Exception e) {
                onClient(() -> {
                    statusText = Messages.s("net_error");
                    statusColor = UiColors.ERROR;
                });
            }
        });
    }

    private void applySkin(PlayerSkin loaded) {
        if (loaded != null) {
            skin = loaded.body().texturePath();
        }
    }

    private void setStatus(String text, int color) {
        this.statusText = text;
        this.statusColor = color;
    }

    private void onClient(Runnable r) {
        if (minecraft != null) {
            minecraft.execute(r);
        } else {
            r.run();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, UiColors.OVERLAY);
        graphics.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, UiColors.PANEL);
        graphics.fill(panelX, panelY, panelX + PANEL_W, panelY + 2, UiColors.BORDER);

        drawHead(graphics);

        int nameW = font.width(playerName);
        graphics.text(font, playerName, panelX + (PANEL_W - nameW) / 2, headY + HEAD + 6,
                UiColors.TEXT, true);

        String icon = hasProfile && verified ? "[✓]" : "";
        if (!icon.isEmpty()) {
            graphics.text(font, icon, panelX + (PANEL_W + nameW) / 2 + 6, headY + HEAD + 6,
                    UiColors.OK, false);
        }

        int statusW = font.width(statusText);
        graphics.text(font, statusText, panelX + (PANEL_W - statusW) / 2, headY + HEAD + 20,
                statusColor, false);

        if (parsed != null && overHead(mouseX, mouseY)) {
            drawTooltip(graphics, mouseX, mouseY);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawHead(GuiGraphicsExtractor graphics) {
        if (skin != null) {
            float u0 = 8f / 64f;
            float u1 = 16f / 64f;
            float v0 = 8f / 64f;
            float v1 = 16f / 64f;
            graphics.blit(skin, headX, headY, headX + HEAD, headY + HEAD, u0, u1, v0, v1);
            float hu0 = 40f / 64f;
            float hu1 = 48f / 64f;
            graphics.blit(skin, headX, headY, headX + HEAD, headY + HEAD, hu0, hu1, v0, v1);
        } else {
            graphics.fill(headX, headY, headX + HEAD, headY + HEAD, UiColors.BORDER);
            String initial = playerName.isEmpty() ? "?"
                    : playerName.substring(0, 1).toUpperCase();
            int iw = font.width(initial);
            graphics.text(font, initial, headX + (HEAD - iw) / 2, headY + (HEAD - 8) / 2,
                    UiColors.TEXT, true);
        }
    }

    private boolean overHead(int mx, int my) {
        return mx >= headX && mx <= headX + HEAD && my >= headY && my <= headY + HEAD;
    }

    private void drawTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<String> lines = new ArrayList<>();
        for (String raw : parsed.plainText().split("\n", -1)) {
            lines.addAll(TextLayout.wrap(font, raw, TOOLTIP_MAX_WIDTH));
        }
        int w = 0;
        for (String line : lines) {
            w = Math.max(w, font.width(line));
        }
        w = Math.min(TOOLTIP_MAX_WIDTH, w) + 8;
        int h = lines.size() * TOOLTIP_LINE_HEIGHT + 6;
        int x = mouseX + 8;
        int y = mouseY + 8;
        if (x + w > width - 4) {
            x = width - w - 4;
        }
        if (y + h > height - 4) {
            y = height - h - 4;
        }
        graphics.fill(x - 2, y - 2, x + w, y + h, 0xF0101418);
        int ty = y;
        for (String line : lines) {
            graphics.text(font, line, x + 2, ty, UiColors.TEXT, false);
            ty += TOOLTIP_LINE_HEIGHT;
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
        }
    }
}
