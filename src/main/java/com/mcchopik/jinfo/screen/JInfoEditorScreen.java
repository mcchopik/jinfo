package com.mcchopik.jinfo.screen;

import com.mcchopik.jinfo.JInfoClient;
import com.mcchopik.jinfo.config.JInfoConfig;
import com.mcchopik.jinfo.net.ApiClient;
import com.mcchopik.jinfo.net.ApiException;
import com.mcchopik.jinfo.net.ProfileData;
import com.mcchopik.jinfo.text.Messages;
import com.mcchopik.jinfo.text.ParsedProfile;
import com.mcchopik.jinfo.text.Service;
import com.mcchopik.jinfo.text.TextParser;
import com.mcchopik.jinfo.util.TextLayout;
import com.mcchopik.jinfo.util.UiColors;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class JInfoEditorScreen extends Screen {

    private static final int LINE_H = 12;
    private static final int LINE_GAP = 2;
    private static final int MARGIN = 12;

    private static final String[] COLOR_CODES = {
            "0", "1", "2", "3", "4", "5", "6", "7",
            "8", "9", "a", "b", "c", "d", "e", "f"
    };

    private final ApiClient api;
    private final Screen parent;
    private final TextParser parser = new TextParser();

    private final List<String> lines = new ArrayList<>();
    private final List<EditBox> lineBoxes = new ArrayList<>();

    private EditBox hexField;
    private String status = "";
    private int statusColor = UiColors.TEXT_MUTED;
    private String pendingText;

    private int textTop;
    private int previewX;
    private int previewY;
    private int previewW;
    private int previewBottom;

    public JInfoEditorScreen(ApiClient api, Screen parent) {
        super(Component.literal("JInfo"));
        this.api = api;
        this.parent = parent;
        this.lines.add("");
    }

    @Override
    protected void init() {
        rebuild();
        if (pendingText != null) {
            String text = pendingText;
            pendingText = null;
            setText(text);
        } else {
            loadCurrentProfile();
        }
    }

    public void applyTemplate(String text) {
        this.pendingText = text;
        if (minecraft != null) {
            minecraft.gui.setScreen(this);
        }
    }

    private void openTemplates() {
        if (minecraft != null) {
            minecraft.gui.setScreen(new JInfoTemplateScreen(this));
        }
    }

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
                    Component.literal(Messages.s("editor_line") + (i + 1)));
            box.setMaxLength(ParsedProfile.MAX_CHARS);
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

        int controlsTop = textTop + ParsedProfile.MAX_LINES * (LINE_H + LINE_GAP) + 10;
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

        addRenderableWidget(Button.builder(Component.literal(Messages.s("photo")),
                        b -> append("{photo} "))
                .bounds(sx, afterColors, 50, bh).build());

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

        int ay = afterServices;
        addRenderableWidget(Button.builder(Component.literal(Messages.s("editor_add_line")), b -> addLine())
                .bounds(left, ay, 100, bh).build());
        addRenderableWidget(Button.builder(Component.literal(Messages.s("editor_save")), b -> save())
                .bounds(left + 104, ay, 70, bh).build());
        addRenderableWidget(Button.builder(Component.literal(Messages.s("editor_clear")), b -> clearText())
                .bounds(left + 178, ay, 70, bh).build());
        addRenderableWidget(Button.builder(Component.literal(Messages.s("editor_cancel")), b -> onClose())
                .bounds(left + 252, ay, 60, bh).build());
        addRenderableWidget(Button.builder(Component.literal(Messages.s("editor_templates")), b -> openTemplates())
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
        if (isHexColor(v)) {
            append("&#" + v.substring(1).toUpperCase());
        } else {
            setStatus(Messages.s("editor_hex_invalid"), UiColors.ERROR);
        }
    }

    private static boolean isHexColor(String value) {
        if (value.length() != 7 || value.charAt(0) != '#') {
            return false;
        }
        for (int i = 1; i < value.length(); i++) {
            char c = Character.toLowerCase(value.charAt(i));
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) {
                return false;
            }
        }
        return true;
    }

    private void addLine() {
        capture();
        if (lines.size() >= ParsedProfile.MAX_LINES) {
            setStatus(Messages.s("editor_limit_lines") + ParsedProfile.MAX_LINES, UiColors.WARN);
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
        setStatus(Messages.s("editor_text_cleared"), UiColors.TEXT_MUTED);
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
        if (v.length() <= ParsedProfile.MAX_CHARS) {
            box.setValue(v);
        } else {
            setStatus(Messages.s("editor_limit_chars"), UiColors.WARN);
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
        ParsedProfile parsed = parser.parse(text, JInfoConfig.language());
        if (parsed.overLimits()) {
            setStatus(Messages.s("editor_over_limits"), UiColors.ERROR);
            return;
        }
        String name = JInfoClient.selfName();
        if (name == null) {
            setStatus(Messages.s("editor_nick_unknown"), UiColors.ERROR);
            return;
        }
        setStatus(Messages.s("editor_saving"), UiColors.TEXT_MUTED);
        CompletableFuture.runAsync(() -> {
            try {
                if (!api.isLoggedIn()) {
                    api.login(name);
                }
                api.putProfile(text);
                onClient(() -> setStatus(Messages.s("editor_saved"), UiColors.OK));
            } catch (ApiException e) {
                onClient(() -> setStatus(describe(e), UiColors.ERROR));
            } catch (Exception e) {
                onClient(() -> setStatus(Messages.s("editor_error") + e.getMessage(), UiColors.ERROR));
            }
        });
    }

    private void loadCurrentProfile() {
        String name = JInfoClient.selfName();
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

            }
        });
    }

    private void setText(String text) {
        String[] parts = text.split("\n", -1);
        lines.clear();
        for (int i = 0; i < parts.length && i < ParsedProfile.MAX_LINES; i++) {
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
            case 429 -> Messages.s("err_rate");
            case 401 -> Messages.s("err_auth");
            case 404 -> Messages.s("err_not_filled");
            default -> Messages.s("err_generic") + " (" + e.statusCode() + "): " + e.getMessage();
        };
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, UiColors.EDITOR_OVERLAY);
        graphics.text(font, Messages.s("editor_title"), MARGIN, 12, UiColors.TEXT, true);
        String counter = charCount() + " / " + ParsedProfile.MAX_CHARS + " " + Messages.s("editor_counter_chars") + ", "
                + lines.size() + " / " + ParsedProfile.MAX_LINES + " " + Messages.s("editor_counter_lines");
        graphics.text(font, counter, MARGIN, 24, UiColors.TEXT_MUTED, false);

        if (previewW > 20 && previewBottom > previewY) {
            graphics.fill(previewX - 4, previewY - 14, width - MARGIN, previewBottom, 0xAA1E242B);
            graphics.text(font, Messages.s("editor_preview"), previewX, previewY - 12, UiColors.TEXT, true);
            drawPreview(graphics);
        }

        if (!status.isBlank()) {
            graphics.text(font, status, MARGIN, height - 14, statusColor, false);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawPreview(GuiGraphicsExtractor graphics) {
        ParsedProfile parsed = parser.parse(joined(), JInfoConfig.language());
        int y = previewY + 2;
        int lineStep = 10;
        for (String rawLine : parsed.plainText().split("\n", -1)) {
            if (y > previewBottom - 10) {
                break;
            }
            String line = TextLayout.fit(font, rawLine, previewW - 8);
            graphics.text(font, line, previewX, y, UiColors.TEXT, false);
            y += lineStep;
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
        }
    }
}
