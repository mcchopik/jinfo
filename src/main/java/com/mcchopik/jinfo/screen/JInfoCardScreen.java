package com.mcchopik.jinfo.screen;

import com.mcchopik.jinfo.net.ApiClient;
import com.mcchopik.jinfo.net.MinecraftSession;
import com.mcchopik.jinfo.net.ApiException;
import com.mcchopik.jinfo.net.ProfileData;
import com.mcchopik.jinfo.net.SkinHelper;
import com.mcchopik.jinfo.text.Lang;
import com.mcchopik.jinfo.text.Messages;
import com.mcchopik.jinfo.text.ParsedProfile;
import com.mcchopik.jinfo.text.TextParser;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class JInfoCardScreen extends Screen {

    private static final int PANEL_W = 200;
    private static final int PANEL_H = 150;
    private static final int HEAD = 40;

    private final ApiClient api;
    private final String uuid;
    private final String playerName;
    private final Object skinSource;
    private final Screen parent;
    private final TextParser parser = new TextParser();

    private Identifier skin;
    private ParsedProfile parsed;
    private String statusText = Messages.s("loading");
    private int statusColor = 0xFFAAAAAA;
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
                    String self = MinecraftSession.currentUsername().orElse(null);
                    if (self != null) {
                        api.login(self);
                    }
                }
                ProfileData data = api.getProfile(uuid);
                ParsedProfile p = parser.parse(data.data(), Lang.RU);
                onClient(() -> {
                    parsed = p;
                    hasProfile = true;
                    verified = data.verified();
                    statusText = verified ? Messages.s("verified") : Messages.s("unverified");
                    statusColor = verified ? 0xFF55FF55 : 0xFFFFAA00;
                });
            } catch (ApiException e) {
                onClient(() -> {
                    if (e.statusCode() == 404) {
                        hasProfile = false;
                        statusText = Messages.s("not_filled");
                        statusColor = 0xFFAAAAAA;
                    } else if (e.statusCode() == 429) {
                        statusText = Messages.s("rate_limited");
                        statusColor = 0xFFFFAA00;
                    } else {
                        statusText = Messages.s("net_error");
                        statusColor = 0xFFFF5555;
                    }
                });
            } catch (Exception e) {
                onClient(() -> {
                    statusText = Messages.s("net_error");
                    statusColor = 0xFFFF5555;
                });
            }
        });
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
        graphics.fill(0, 0, width, height, 0x99000000);
        graphics.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xFF171B21);
        graphics.fill(panelX, panelY, panelX + PANEL_W, panelY + 2, 0xFF2F74C9);

        drawHead(graphics);

        int nameW = font.width(playerName);
        graphics.text(font, playerName, panelX + (PANEL_W - nameW) / 2, headY + HEAD + 6,
                0xFFFFFFFF, true);

        String icon = hasProfile ? (verified ? "[✓]" : "[?]") : "";
        if (!icon.isEmpty()) {
            graphics.text(font, icon, panelX + (PANEL_W + nameW) / 2 + 6, headY + HEAD + 6,
                    verified ? 0xFF55FF55 : 0xFFFFAA00, false);
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
            graphics.fill(headX, headY, headX + HEAD, headY + HEAD, 0xFF2F74C9);
            String initial = playerName.isEmpty() ? "?"
                    : playerName.substring(0, 1).toUpperCase();
            int iw = font.width(initial);
            graphics.text(font, initial, headX + (HEAD - iw) / 2, headY + (HEAD - 8) / 2,
                    0xFFFFFFFF, true);
        }
    }

    private boolean overHead(int mx, int my) {
        return mx >= headX && mx <= headX + HEAD && my >= headY && my <= headY + HEAD;
    }

    private void drawTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int maxWidth = 220;
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (String raw : parsed.plainText().split("\n", -1)) {
            lines.addAll(wrap(raw, maxWidth));
        }
        int w = 0;
        for (String line : lines) {
            w = Math.max(w, font.width(line));
        }
        w = Math.min(maxWidth, w) + 8;
        int h = lines.size() * 10 + 6;
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
            graphics.text(font, line, x + 2, ty, 0xFFE6E8EC, false);
            ty += 10;
        }
    }

    private java.util.List<String> wrap(String text, int maxWidth) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (text.isEmpty()) {
            out.add("");
            return out;
        }
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
