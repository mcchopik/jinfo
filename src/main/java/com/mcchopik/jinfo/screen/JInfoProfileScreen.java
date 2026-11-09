package com.mcchopik.jinfo.screen;

import com.mcchopik.jinfo.JInfoClient;
import com.mcchopik.jinfo.config.JInfoConfig;
import com.mcchopik.jinfo.net.ApiClient;
import com.mcchopik.jinfo.net.ApiException;
import com.mcchopik.jinfo.net.PhotoTextures;
import com.mcchopik.jinfo.net.ProfileData;
import com.mcchopik.jinfo.text.Messages;
import com.mcchopik.jinfo.text.ParsedProfile;
import com.mcchopik.jinfo.text.Part;
import com.mcchopik.jinfo.text.Service;
import com.mcchopik.jinfo.text.TextParser;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;

public class JInfoProfileScreen extends Screen {

    private static final int GAP = 12;
    private static final int PHOTO = 56;
    private static final int LAYER_H = 15;

    private final ApiClient api;
    private final String uuid;
    private final String playerName;
    private final Object skinSource;
    private final Screen parent;
    private final TextParser parser = new TextParser();

    private ParsedProfile parsed;
    private PhotoTextures.Photo photo;
    private SkinViewer skinViewer;
    private String statusText = Messages.s("loading");
    private int statusColor = 0xFFAAAAAA;
    private boolean verified = false;
    private boolean loading = true;

    private boolean hat = true;
    private boolean jacket = true;
    private boolean sleeves = true;
    private boolean pants = true;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int viewerX;
    private int viewerY;
    private int viewerW;
    private int viewerH;
    private int infoX;
    private int infoW;
    private int linksX;
    private int linksY;
    private int linksMaxW;

    public JInfoProfileScreen(ApiClient api, String uuid, String playerName,
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
        panelW = Math.min(width - 20, 460);
        panelH = Math.min(height - 20, 300);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        viewerW = Math.min(120, Math.max(90, panelW / 3));
        int layersHeight = 4 * LAYER_H + 3 * 4;
        viewerH = Math.min(170, Math.max(80, panelH - 24 - layersHeight - 20));
        viewerX = panelX + GAP;
        viewerY = panelY + GAP;
        infoX = viewerX + viewerW + GAP;
        infoW = Math.max(80, panelX + panelW - GAP - infoX);
        linksX = infoX;
        linksMaxW = infoW;
        linksY = panelY + 136;

        skinViewer = createSkinViewer();
        setWidgets();

        if (loading) {
            loadProfile();
        }
    }

    private SkinViewer createSkinViewer() {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.getEntityModels() == null) {
                return null;
            }
            SkinViewer viewer = new SkinViewer(viewerW, viewerH, playerName,
                    skinSupplier(), client.getEntityModels());
            viewer.setX(viewerX);
            viewer.setY(viewerY);
            return viewer;
        } catch (Throwable t) {
            return null;
        }
    }

    private Supplier<PlayerSkin> skinSupplier() {
        if (skinSource instanceof AbstractClientPlayer player) {
            return player::getSkin;
        }
        try {
            Minecraft client = Minecraft.getInstance();
            if (client == null) {
                return () -> null;
            }
            String id = uuid == null ? "" : uuid.replace("-", "");
            if (id.length() != 32) {
                return () -> null;
            }
            UUID real = new UUID(Long.parseUnsignedLong(id.substring(0, 16), 16),
                    Long.parseUnsignedLong(id.substring(16), 16));
            GameProfile profile = new GameProfile(real, playerName);
            return client.getSkinManager().createLookup(profile, true);
        } catch (Throwable t) {
            return () -> null;
        }
    }

    private void setWidgets() {
        clearWidgets();

        if (skinViewer != null) {
            addRenderableWidget(skinViewer);
            int lw = viewerW;
            int lx = viewerX;
            int ly = viewerY + viewerH + 4;

            addRenderableWidget(Button.builder(layerText("layer_hat", hat), b -> {
                hat = !hat;
                skinViewer.setLayer(hat, "hat");
                b.setMessage(layerText("layer_hat", hat));
            }).bounds(lx, ly, lw, LAYER_H).build());

            addRenderableWidget(Button.builder(layerText("layer_jacket", jacket), b -> {
                jacket = !jacket;
                skinViewer.setLayer(jacket, "jacket");
                b.setMessage(layerText("layer_jacket", jacket));
            }).bounds(lx, ly + LAYER_H + 4, lw, LAYER_H).build());

            addRenderableWidget(Button.builder(layerText("layer_sleeves", sleeves), b -> {
                sleeves = !sleeves;
                skinViewer.setLayer(sleeves, "right_sleeve", "left_sleeve");
                b.setMessage(layerText("layer_sleeves", sleeves));
            }).bounds(lx, ly + 2 * (LAYER_H + 4), lw, LAYER_H).build());

            addRenderableWidget(Button.builder(layerText("layer_pants", pants), b -> {
                pants = !pants;
                skinViewer.setLayer(pants, "right_pants", "left_pants");
                b.setMessage(layerText("layer_pants", pants));
            }).bounds(lx, ly + 3 * (LAYER_H + 4), lw, LAYER_H).build());
        }

        int by = panelY + panelH - 24;
        addRenderableWidget(Button.builder(Component.literal(Messages.s("close")), b -> onClose())
                .bounds(panelX + panelW - 90, by, 78, 18)
                .build());
        addRenderableWidget(Button.builder(Component.literal(Messages.s("report")), b -> report())
                .bounds(panelX + GAP, by, 100, 18)
                .build());

        if (parsed != null) {
            buildLinkButtons();
        }
    }

    private Component layerText(String key, boolean enabled) {
        return Component.literal(Messages.s(key) + (enabled ? " ✓" : " ✗"));
    }

    private void buildLinkButtons() {
        List<Part.Link> links = new ArrayList<>();
        for (Part p : parsed.parts()) {
            if (p instanceof Part.Link link) {
                links.add(link);
            }
        }
        int rx = linksX;
        int ry = linksY;
        int bh = 18;
        for (Part.Link link : links) {
            String label = link.label() == null || link.label().isBlank()
                    ? serviceLabel(link.serviceKey()) : link.label();
            int bw = Math.max(64, font.width(label) + 14);
            if (rx + bw > linksX + linksMaxW) {
                rx = linksX;
                ry += bh + 4;
            }
            int color = parseColor(link.colorHex());
            addRenderableWidget(new PaintedButton(rx, ry, bw, bh, Component.literal(label),
                    (graphics, button, hovered) -> paintLink(graphics, button, hovered, color, label),
                    () -> openLink(link)));
            rx += bw + 4;
        }
    }

    private void paintLink(GuiGraphicsExtractor graphics, PaintedButton button, boolean hovered,
                           int color, String label) {
        int bg = hovered ? 0xFF232A33 : 0xFF1B2027;
        graphics.fill(button.getX(), button.getY(), button.getRight(), button.getBottom(), bg);
        graphics.fill(button.getX(), button.getY(), button.getRight(), button.getY() + 2, color);
        int tw = font.width(label);
        graphics.text(font, label, button.getX() + (button.getWidth() - tw) / 2,
                button.getY() + (button.getHeight() - 8) / 2, 0xFFE6E8EC, false);
    }

    private String serviceLabel(String key) {
        Service s = Service.byKey(key);
        return s == null ? key : s.label(JInfoConfig.language());
    }

    private void openLink(Part.Link link) {
        boolean suspicious = link.warnings() != null && !link.warnings().isEmpty()
                || !link.verified();
        if (minecraft != null) {
            minecraft.gui.setScreen(new LinkConfirmScreen(
                    link.url(), serviceLabel(link.serviceKey()), this, suspicious));
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
                onClient(() -> {
                    parsed = p;
                    photo = PhotoTextures.get(p.avatarUrl());
                    loading = false;
                    verified = data.verified();
                    statusText = verified ? Messages.s("verified") : Messages.s("unverified");
                    statusColor = verified ? 0xFF55FF55 : 0xFFFFAA00;
                    setWidgets();
                });
            } catch (ApiException e) {
                onClient(() -> {
                    loading = false;
                    if (e.statusCode() == 404) {
                        statusText = Messages.s("not_filled");
                        statusColor = 0xFFAAAAAA;
                    } else if (e.statusCode() == 429) {
                        statusText = Messages.s("rate_limited");
                        statusColor = 0xFFFFAA00;
                    } else {
                        statusText = Messages.s("net_error");
                        statusColor = 0xFFFF5555;
                    }
                    setWidgets();
                });
            } catch (Exception e) {
                onClient(() -> {
                    loading = false;
                    statusText = Messages.s("net_error");
                    statusColor = 0xFFFF5555;
                    setWidgets();
                });
            }
        });
    }

    private void report() {
        setStatus(Messages.s("loading"), 0xFFAAAAAA);
        CompletableFuture.runAsync(() -> {
            try {
                if (!api.isLoggedIn()) {
                    String self = JInfoClient.selfName();
                    if (self != null) {
                        api.login(self);
                    }
                }
                api.report(uuid);
                onClient(() -> setStatus(Messages.s("reported"), 0xFF55FF55));
            } catch (ApiException e) {
                onClient(() -> {
                    if (e.statusCode() == 429) {
                        setStatus(Messages.s("already_reported"), 0xFFFFAA00);
                    } else if (e.statusCode() == 400) {
                        setStatus(Messages.s("report_self"), 0xFFFF5555);
                    } else {
                        setStatus(Messages.s("report_failed"), 0xFFFF5555);
                    }
                });
            } catch (Exception e) {
                onClient(() -> setStatus(Messages.s("report_failed"), 0xFFFF5555));
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
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF171B21);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 2, 0xFF2F74C9);

        if (photo == null && parsed != null) {
            photo = PhotoTextures.get(parsed.avatarUrl());
        }
        if (photo != null) {
            int px = panelX + panelW - GAP - PHOTO;
            int py = panelY + GAP;
            graphics.blit(photo.id(), px, py, px + PHOTO, py + PHOTO,
                    photo.u0(), photo.u1(), photo.v0(), photo.v1());
        }

        int textW = photo != null ? Math.max(60, infoW - PHOTO - 8) : infoW;
        String name = fit(playerName, textW);
        graphics.text(font, name, infoX, panelY + 12, 0xFFFFFFFF, true);

        String icon = parsed == null ? "" : (verified ? "[✓]" : "[?]");
        if (!icon.isEmpty()) {
            int nameW = font.width(name);
            if (nameW + 12 <= textW) {
                graphics.text(font, icon, infoX + nameW + 4, panelY + 12,
                        verified ? 0xFF55FF55 : 0xFFFFAA00, false);
            }
        }

        graphics.text(font, fit(statusText, textW), infoX, panelY + 26, statusColor, false);

        if (parsed != null) {
            int y = panelY + 44;
            int maxLines = 8;
            List<String> lines = new ArrayList<>();
            for (String raw : parsed.plainText().split("\n", -1)) {
                lines.addAll(wrap(raw, textW));
            }
            for (int i = 0; i < lines.size() && i < maxLines; i++) {
                String line = lines.get(i);
                if (i == maxLines - 1 && lines.size() > maxLines) {
                    line = line + "…";
                }
                graphics.text(font, line, infoX, y, 0xFFE6E8EC, false);
                y += 10;
            }
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

    private int parseColor(String hex) {
        try {
            if (hex == null || hex.isBlank()) {
                return 0xFF8899AA;
            }
            String s = hex.startsWith("#") ? hex.substring(1) : hex;
            return 0xFF000000 | Integer.parseInt(s, 16);
        } catch (Throwable t) {
            return 0xFF8899AA;
        }
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> out = new ArrayList<>();
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