package com.mcchopik.jinfo.screen;

import com.mcchopik.jinfo.net.ApiClient;
import com.mcchopik.jinfo.net.ApiException;
import com.mcchopik.jinfo.net.ProfileData;
import com.mcchopik.jinfo.net.SkinHelper;
import com.mcchopik.jinfo.text.Lang;
import com.mcchopik.jinfo.text.ParsedProfile;
import com.mcchopik.jinfo.text.Part;
import com.mcchopik.jinfo.text.Service;
import com.mcchopik.jinfo.text.TextParser;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Полный профиль игрока: голова, ник, текст и кнопки сервисов.
 * Ссылки никогда не открываются автоматически — сначала диалог подтверждения.
 */
public class JInfoProfileScreen extends Screen {

    private static final int HEAD = 32;

    private final ApiClient api;
    private final String uuid;
    private final String playerName;
    private final Object skinSource;
    private final Screen parent;
    private final TextParser parser = new TextParser();

    private Identifier skin;
    private ParsedProfile parsed;
    private String statusText = "Загрузка...";
    private int statusColor = 0xFFAAAAAA;
    private boolean verified = false;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int headX;
    private int headY;
    private int linksX;
    private int linksY;
    private int linksMaxW;

    private boolean loading = true;

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
        panelW = Math.min(width - 20, 400);
        panelH = Math.min(height - 20, 240);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        headX = panelX + (panelW - HEAD) / 2;
        headY = panelY + 10;
        linksX = panelX + 12;
        linksMaxW = panelW - 24;
        skin = skinSource == null ? null : SkinHelper.texture(skinSource);

        setWidgets();

        if (loading) {
            loadProfile();
        }
    }

    private void setWidgets() {
        clearWidgets();
        int by = panelY + panelH - 24;
        addRenderableWidget(Button.builder(Component.literal("Закрыть"), b -> onClose())
                .bounds(panelX + panelW - 90, by, 78, 18)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Пожаловаться"), b ->
                        setStatus("Жалоба будет доступна на следующем шаге.", 0xFFFFAA00))
                .bounds(panelX + 12, by, 100, 18)
                .build());

        if (parsed != null) {
            buildLinkButtons();
        }
    }

    private void buildLinkButtons() {
        List<Part.Link> links = new ArrayList<>();
        for (Part p : parsed.parts()) {
            if (p instanceof Part.Link link) {
                links.add(link);
            }
        }
        int rx = linksX;
        int ry = panelY + 138;
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
        return s == null ? key : s.label(Lang.RU);
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
                ProfileData data = api.getProfile(uuid);
                ParsedProfile p = parser.parse(data.data(), Lang.RU);
                onClient(() -> {
                    parsed = p;
                    loading = false;
                    verified = data.verified();
                    statusText = verified ? "Профиль подтверждён" : "Профиль (не подтверждён)";
                    statusColor = verified ? 0xFF55FF55 : 0xFFFFAA00;
                    setWidgets();
                });
            } catch (ApiException e) {
                onClient(() -> {
                    loading = false;
                    if (e.statusCode() == 404) {
                        statusText = "Профиль не заполнен";
                        statusColor = 0xFFAAAAAA;
                    } else if (e.statusCode() == 429) {
                        statusText = "Слишком часто, подожди";
                        statusColor = 0xFFFFAA00;
                    } else {
                        statusText = "Ошибка сети";
                        statusColor = 0xFFFF5555;
                    }
                    setWidgets();
                });
            } catch (Exception e) {
                onClient(() -> {
                    loading = false;
                    statusText = "Ошибка сети";
                    statusColor = 0xFFFF5555;
                    setWidgets();
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
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF171B21);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 2, 0xFF2F74C9);

        drawHead(graphics);

        int nameW = font.width(playerName);
        graphics.text(font, playerName, panelX + (panelW - nameW) / 2, headY + HEAD + 6,
                0xFFFFFFFF, true);

        String icon = parsed == null ? "" : (verified ? "[✓]" : "[?]");
        if (!icon.isEmpty()) {
            graphics.text(font, icon, panelX + (panelW + nameW) / 2 + 6, headY + HEAD + 6,
                    verified ? 0xFF55FF55 : 0xFFFFAA00, false);
        }

        int statusW = font.width(statusText);
        graphics.text(font, statusText, panelX + (panelW - statusW) / 2, headY + HEAD + 20,
                statusColor, false);

        if (parsed != null) {
            int y = panelY + 74;
            int maxLines = 5;
            List<String> lines = new ArrayList<>();
            for (String raw : parsed.plainText().split("\n", -1)) {
                lines.addAll(wrap(raw, panelW - 24));
            }
            for (int i = 0; i < lines.size() && i < maxLines; i++) {
                String line = lines.get(i);
                if (i == maxLines - 1 && lines.size() > maxLines) {
                    line = line + "…";
                }
                graphics.text(font, line, panelX + 12, y, 0xFFE6E8EC, false);
                y += 10;
            }
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