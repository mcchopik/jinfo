package com.mcchopik.jinfo.screen;

import com.mcchopik.jinfo.net.ApiClient;
import com.mcchopik.jinfo.net.ApiException;
import com.mcchopik.jinfo.net.MinecraftSession;
import com.mcchopik.jinfo.net.ProfileData;
import com.mcchopik.jinfo.text.Lang;
import com.mcchopik.jinfo.text.ParsedProfile;
import com.mcchopik.jinfo.text.Service;
import com.mcchopik.jinfo.text.TextParser;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Экран редактора профиля в игре.
 * Многострочность реализована стеком однострочных EditBox (по одной на строку),
 * чтобы не зависеть от нестабильных внутренних API. Сетевые операции — в фоне.
 */
public class JInfoEditorScreen extends Screen {

    private static final int MAX_LINES = 10;
    private static final int MAX_CHARS = 500;
    private static final int LINE_H = 12;
    private static final int LINE_GAP = 2;
    private static final int MARGIN = 12;

    private static final String[] COLOR_CODES = {
            "0", "1", "2", "3", "4", "5", "6", "7",
            "8", "9", "a", "b", "c", "d", "e", "f"
    };
    private static final int[] COLOR_HEX = {
            0xFF000000, 0xFF0000AA, 0xFF00AA00, 0xFF00AAAA,
            0xFFAA0000, 0xFFAA00AA, 0xFFFFAA00, 0xFFAAAAAA,
            0xFF555555, 0xFF5555FF, 0xFF55FF55, 0xFF55FFFF,
            0xFFFF5555, 0xFFFF55FF, 0xFFFFFF55, 0xFFFFFFFF
    };

    private final ApiClient api;
    private final Screen parent;
    private final TextParser parser = new TextParser();

    private final List<String> lines = new ArrayList<>();
    private final List<EditBox> lineBoxes = new ArrayList<>();

    private EditBox hexField;
    private String status = "";
    private int statusColor = 0xFFAAAAAA;

    private int textTop;
    private int previewX;
    private int previewY;
    private int previewW;
    private int previewBottom;

    public JInfoEditorScreen(ApiClient api, Screen parent) {
        super(Component.literal("JInfo — редактор"));
        this.api = api;
        this.parent = parent;
        this.lines.add("");
    }

    @Override
    protected void init() {
        rebuild();
        loadCurrentProfile();
    }

    // ------------------------------------------------------------------
    // Построение виджетов
    // ------------------------------------------------------------------

    private void capture() {
        for (int i = 0; i < lineBoxes.size() && i < lines.size(); i++) {
            lines.set(i, lineBoxes.get(i).getValue());
        }
    }

    private void rebuild() {
        capture();
        clearWidgets();
        lineBoxes.clear();

        int areaLeft = MARGIN;
        int areaWidth = Math.max(180, (int) (width * 0.52) - MARGIN);
        textTop = 40;

        int y = textTop;
        for (int i = 0; i < lines.size(); i++) {
            EditBox box = new EditBox(font, areaLeft, y, areaWidth, LINE_H,
                    Component.literal("Строка " + (i + 1)));
            box.setMaxLength(MAX_CHARS);
            box.setValue(lines.get(i));
            final int idx = i;
            box.setResponder(v -> {
                if (idx < lines.size()) {
                    lines.set(idx, v);
                }
            });
            lineBoxes.add(box);
            addRenderableWidget(box);
            y += LINE_H + LINE_GAP;
        }

        int controlsTop = textTop + MAX_LINES * (LINE_H + LINE_GAP) + 10;
        layoutButtons(areaLeft, controlsTop);

        previewX = areaLeft + areaWidth + 10;
        previewY = textTop - 2;
        previewW = Math.max(60, width - MARGIN - previewX);
        previewBottom = controlsTop - 6;
    }

    private void layoutButtons(int left, int top) {
        int bw = 20;
        int bh = 14;
        int gap = 2;

        // 16 цветов в 2 ряда по 8
        int cx = left;
        int cy = top;
        for (int i = 0; i < 16; i++) {
            final String code = COLOR_CODES[i];
            int col = i % 8;
            int row = i / 8;
            Button b = Button.builder(Component.literal("&" + code), btn -> append("&" + code))
                    .bounds(left + col * (bw + gap), top + row * (bh + gap), bw, bh)
                    .build();
            addRenderableWidget(b);
        }
        int afterColors = top + 2 * (bh + gap) + 4;

        // Стили
        String[][] styles = {
                {"&l", "&l"}, {"&o", "&o"}, {"&n", "&n"}, {"&m", "&m"}, {"&r", "&r"}
        };
        int sx = left;
        for (String[] s : styles) {
            Button b = Button.builder(Component.literal(s[0]), btn -> append(s[1]))
                    .bounds(sx, afterColors, bw, bh).build();
            addRenderableWidget(b);
            sx += bw + gap;
        }
        int afterStyles = afterColors + bh + 4;

        // Сервисы: по 5 в ряд
        int perRow = 5;
        int idx = 0;
        for (Service service : Service.values()) {
            String key = service.key();
            int col = idx % perRow;
            int row = idx / perRow;
            Button b = Button.builder(Component.literal(key), btn -> append("{" + key + "} "))
                    .bounds(left + col * 52, afterStyles + row * (bh + gap), 50, bh)
                    .build();
            addRenderableWidget(b);
            idx++;
        }
        int serviceRows = (Service.values().length + perRow - 1) / perRow;
        int afterServices = afterStyles + serviceRows * (bh + gap) + 6;

        // Действия
        int ay = afterServices;
        addRenderableWidget(Button.builder(Component.literal("Добавить строку"), b -> addLine())
                .bounds(left, ay, 100, bh).build());
        addRenderableWidget(Button.builder(Component.literal("Сохранить"), b -> save())
                .bounds(left + 104, ay, 70, bh).build());
        addRenderableWidget(Button.builder(Component.literal("Очистить"), b -> clearText())
                .bounds(left + 178, ay, 70, bh).build());
        addRenderableWidget(Button.builder(Component.literal("Отмена"), b -> onClose())
                .bounds(left + 252, ay, 60, bh).build());
        addRenderableWidget(Button.builder(Component.literal("В браузере"), b ->
                setStatus("Веб-редактор появится на следующем шаге.", 0xFFFFAA00))
                .bounds(left + 316, ay, 90, bh).build());

        int hexY2 = afterServices;
        EditBox hex = new EditBox(font, left + 420, hexY2, 60, bh, Component.literal("#RRGGBB"));
        hex.setMaxLength(7);
        hex.setHint(Component.literal("#RRGGBB"));
        hexField = hex;
        addRenderableWidget(hex);
        addRenderableWidget(Button.builder(Component.literal("Hex"), b -> applyHex())
                .bounds(left + 484, hexY2, 40, bh).build());
    }

    private void applyHex() {
        if (hexField == null) {
            return;
        }
        String v = hexField.getValue().trim();
        if (!v.startsWith("#")) {
            v = "#" + v;
        }
        if (v.matches("#[0-9a-fA-F]{6}")) {
            append("&#" + v.substring(1).toUpperCase());
        } else {
            setStatus("Неверный hex-цвет. Формат: #RRGGBB", 0xFFFF5555);
        }
    }

    // ------------------------------------------------------------------
    // Логика текста
    // ------------------------------------------------------------------

    private void addLine() {
        capture();
        if (lines.size() >= MAX_LINES) {
            setStatus("Достигнут лимит " + MAX_LINES + " строк.", 0xFFFFAA00);
            return;
        }
        lines.add("");
        rebuild();
    }

    private void clearText() {
        capture();
        lines.clear();
        lines.add("");
        rebuild();
        setStatus("Текст очищен.", 0xFFAAAAAA);
    }

    private EditBox focusedLine() {
        for (EditBox b : lineBoxes) {
            if (b.isFocused()) {
                return b;
            }
        }
        return lineBoxes.isEmpty() ? null : lineBoxes.get(lineBoxes.size() - 1);
    }

    private void append(String code) {
        EditBox box = focusedLine();
        if (box == null) {
            return;
        }
        String v = box.getValue() + code;
        if (v.length() <= MAX_CHARS) {
            box.setValue(v);
        } else {
            setStatus("Достигнут лимит символов.", 0xFFFFAA00);
        }
    }

    private String joined() {
        capture();
        return String.join("\n", lines);
    }

    private int charCount() {
        int n = 0;
        for (int i = 0; i < lines.size(); i++) {
            n += lines.get(i).length();
            if (i > 0) {
                n++;
            }
        }
        return n;
    }

    private void save() {
        String text = joined();
        ParsedProfile parsed = parser.parse(text, Lang.RU);
        if (parsed.overLimits()) {
            setStatus("Превышены лимиты: 500 символов / 10 строк / 6 ссылок.", 0xFFFF5555);
            return;
        }
        String name = MinecraftSession.currentUsername().orElse(null);
        if (name == null) {
            setStatus("Не удалось определить ник игрока.", 0xFFFF5555);
            return;
        }
        setStatus("Сохранение...", 0xFFAAAAAA);
        CompletableFuture.runAsync(() -> {
            try {
                if (!api.isLoggedIn()) {
                    api.login(name);
                }
                api.putProfile(text);
                onClient(() -> setStatus("Профиль сохранён.", 0xFF55FF55));
            } catch (ApiException e) {
                onClient(() -> setStatus(describe(e), 0xFFFF5555));
            } catch (Exception e) {
                onClient(() -> setStatus("Ошибка: " + e.getMessage(), 0xFFFF5555));
            }
        });
    }

    private void loadCurrentProfile() {
        String name = MinecraftSession.currentUsername().orElse(null);
        CompletableFuture.runAsync(() -> {
            try {
                if (!api.isLoggedIn()) {
                    if (name == null) {
                        return;
                    }
                    api.login(name);
                }
                String uuid = api.currentUuid();
                if (uuid == null) {
                    return;
                }
                ProfileData data = api.getProfile(uuid);
                onClient(() -> setText(data.data()));
            } catch (Exception ignored) {
                // Профиля ещё нет — начинаем с пустого листа.
            }
        });
    }

    private void setText(String text) {
        String[] parts = text.split("\n", -1);
        lines.clear();
        for (int i = 0; i < parts.length && i < MAX_LINES; i++) {
            lines.add(parts[i]);
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        rebuild();
    }

    private void setStatus(String message, int color) {
        this.status = message;
        this.statusColor = color;
    }

    private void onClient(Runnable r) {
        if (minecraft != null) {
            minecraft.execute(r);
        } else {
            r.run();
        }
    }

    private static String describe(ApiException e) {
        return switch (e.statusCode()) {
            case 429 -> "Слишком часто. Подожди немного.";
            case 401 -> "Не удалось авторизоваться.";
            case 404 -> "Профиль не заполнен.";
            default -> "Ошибка (" + e.statusCode() + "): " + e.getMessage();
        };
    }

    // ------------------------------------------------------------------
    // Рендер
    // ------------------------------------------------------------------

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xE6101418);
        graphics.text(font, "JInfo — редактор профиля", MARGIN, 12, 0xFFFFFFFF, true);
        String counter = charCount() + " / " + MAX_CHARS + " символов, "
                + lines.size() + " / " + MAX_LINES + " строк";
        graphics.text(font, counter, MARGIN, 24, 0xFFAAAAAA, false);

        // Рамка предпросмотра
        if (previewW > 20 && previewBottom > previewY) {
            graphics.fill(previewX - 4, previewY - 14, width - MARGIN, previewBottom, 0xAA1E242B);
            graphics.text(font, "Предпросмотр", previewX, previewY - 12, 0xFFFFFFFF, true);
            drawPreview(graphics);
        }

        if (!status.isBlank()) {
            graphics.text(font, status, MARGIN, height - 14, statusColor, false);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawPreview(GuiGraphicsExtractor graphics) {
        ParsedProfile parsed = parser.parse(joined(), Lang.RU);
        int y = previewY + 2;
        int lineStep = 10;
        for (String rawLine : parsed.plainText().split("\n", -1)) {
            if (y > previewBottom - 10) {
                break;
            }
            String line = fit(rawLine, previewW - 8);
            graphics.text(font, line, previewX, y, 0xFFE6E8EC, false);
            y += lineStep;
        }
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

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
        }
    }
}