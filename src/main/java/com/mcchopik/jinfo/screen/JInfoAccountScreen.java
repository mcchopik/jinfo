package com.mcchopik.jinfo.screen;

import com.mcchopik.jinfo.JInfoClient;
import com.mcchopik.jinfo.config.JInfoConfig;
import com.mcchopik.jinfo.net.ApiException;
import com.mcchopik.jinfo.text.Messages;
import com.mcchopik.jinfo.util.TextLayout;
import com.mcchopik.jinfo.util.UiColors;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public class JInfoAccountScreen extends Screen {

    private final Screen parent;
    private EditBox userField;
    private EditBox passField;
    private String note = "";
    private boolean noteError;
    private boolean busy;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public JInfoAccountScreen(Screen parent) {
        super(Component.literal("JInfo"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 20, 320);
        panelH = Math.min(height - 20, 280);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        int x = panelX + 16;
        int w = panelW - 32;
        boolean account = JInfoClient.api().isAccount();
        boolean guestMode = JInfoClient.api().isGuest();

        if (account) {
            addRenderableWidget(Button.builder(Component.literal(Messages.s("account_logout")), b -> logout())
                    .bounds(x, panelY + 100, w, 20)
                    .build());
            addRenderableWidget(Button.builder(Component.literal(Messages.s("back")), b -> onClose())
                    .bounds(panelX + (panelW - 90) / 2, panelY + panelH - 28, 90, 20)
                    .build());
            return;
        }
        if (guestMode) {
            Button disable = Button.builder(Component.literal(Messages.s("account_disable_guest")),
                            b -> disableGuest())
                    .bounds(x, panelY + 100, w, 20)
                    .build();
            disable.active = !busy;
            addRenderableWidget(disable);
            addRenderableWidget(Button.builder(Component.literal(Messages.s("back")), b -> onClose())
                    .bounds(panelX + (panelW - 90) / 2, panelY + panelH - 28, 90, 20)
                    .build());
            return;
        }

        userField = new EditBox(font, x, panelY + 75, w, 20, Component.literal("nick"));
        userField.setMaxLength(16);
        String initial = JInfoClient.selfName();
        userField.setValue(initial == null ? "" : initial);
        userField.setEditable(!busy);
        userField.setResponder(v -> note = "");
        addRenderableWidget(userField);

        passField = new EditBox(font, x, panelY + 109, w, 20, Component.literal("password"));
        passField.setMaxLength(128);
        passField.addFormatter((value, offset) ->
                FormattedCharSequence.forward("*".repeat(value.length()), Style.EMPTY));
        passField.setEditable(!busy);
        passField.setResponder(v -> note = "");
        addRenderableWidget(passField);

        int half = (w - 8) / 2;
        Button login = Button.builder(Component.literal(Messages.s("account_login")), b -> submit(false))
                .bounds(x, panelY + 141, half, 20)
                .build();
        login.active = !busy;
        addRenderableWidget(login);
        Button register = Button.builder(Component.literal(Messages.s("account_register")), b -> submit(true))
                .bounds(x + half + 8, panelY + 141, w - half - 8, 20)
                .build();
        register.active = !busy;
        addRenderableWidget(register);
        Button guest = Button.builder(Component.literal(Messages.s("account_guest")), b -> guestLogin())
                .bounds(x, panelY + 167, w, 20)
                .build();
        guest.active = !busy;
        addRenderableWidget(guest);
        addRenderableWidget(Button.builder(Component.literal(Messages.s("back")), b -> onClose())
                .bounds(panelX + (panelW - 90) / 2, panelY + panelH - 28, 90, 20)
                .build());
        setInitialFocus(userField);
    }

    private void submit(boolean register) {
        if (busy) {
            return;
        }
        String username = userField.getValue().trim();
        String password = passField.getValue();
        if (username.isEmpty()) {
            setNote(Messages.s("nick_invalid"), true);
            return;
        }
        if (register && (password.length() < 8 || password.length() > 128)) {
            setNote(Messages.s("account_bad_pass"), true);
            return;
        }
        if (password.isEmpty()) {
            setNote(Messages.s("account_bad_login"), true);
            return;
        }
        busy = true;
        setNote(Messages.s("account_busy"), false);
        rebuild();
        CompletableFuture.runAsync(() -> {
            try {
                if (register) {
                    JInfoClient.api().registerAccount(username, password);
                } else {
                    JInfoClient.api().loginAccount(username, password);
                }
                JInfoConfig.setNickname(username);
                onClient(() -> {
                    busy = false;
                    noteError = false;
                    note = Messages.s(register ? "account_created" : "account_ok");
                    rebuild();
                });
            } catch (Exception e) {
                onClient(() -> {
                    busy = false;
                    setNote(accountError(e), true);
                    rebuild();
                });
            }
        });
    }

    private void guestLogin() {
        if (busy) {
            return;
        }
        String typed = userField == null ? "" : userField.getValue().trim();
        if (!typed.isEmpty()) {
            loginGuest(typed);
            return;
        }
        JInfoClient.requireSelfName(this, this::loginGuest);
    }

    private void loginGuest(String username) {
        if (busy) {
            return;
        }
        busy = true;
        setNote(Messages.s("account_busy"), false);
        rebuild();
        CompletableFuture.runAsync(() -> {
            try {
                JInfoClient.api().loginGuest(username);
                JInfoConfig.setNickname(username);
                onClient(() -> {
                    busy = false;
                    noteError = false;
                    note = Messages.s("account_ok");
                    rebuild();
                });
            } catch (Exception e) {
                onClient(() -> {
                    busy = false;
                    setNote(accountError(e), true);
                    rebuild();
                });
            }
        });
    }

    private void logout() {
        JInfoClient.api().logout();
        setNote("", false);
        rebuild();
    }

    private void disableGuest() {
        if (busy) {
            return;
        }
        busy = true;
        setNote(Messages.s("account_busy"), false);
        rebuild();
        CompletableFuture.runAsync(() -> {
            try {
                JInfoClient.api().stopGuest();
                JInfoClient.api().logout();
                onClient(() -> {
                    busy = false;
                    noteError = false;
                    note = Messages.s("account_guest_off");
                    rebuild();
                });
            } catch (Exception e) {
                onClient(() -> {
                    busy = false;
                    setNote(accountError(e), true);
                    rebuild();
                });
            }
        });
    }

    private String accountError(Exception e) {
        if (e instanceof ApiException api) {
            return switch (api.statusCode()) {
                case 401 -> Messages.s("account_bad_login");
                case 409 -> api.getMessage() != null && api.getMessage().contains("Guest mode")
                        ? Messages.s("account_guest_locked")
                        : Messages.s("account_exists");
                case 403 -> Messages.s("account_registered_guest");
                case 429 -> Messages.s("err_rate");
                default -> Messages.s("err_generic") + ": " + api.getMessage();
            };
        }
        return Messages.s("err_generic") + ": " + e.getMessage();
    }

    private void setNote(String text, boolean error) {
        note = text;
        noteError = error;
    }

    private void onClient(Runnable action) {
        if (minecraft != null) {
            minecraft.execute(action);
        } else {
            action.run();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, UiColors.OVERLAY);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, UiColors.PANEL);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 2, UiColors.BORDER);
        graphics.text(font, Messages.s("account_title"), panelX + 16, panelY + 10, UiColors.TEXT, true);

        int y = panelY + 30;
        String hint = JInfoClient.api().isGuest()
                ? Messages.s("account_guest_hint")
                : Messages.s("account_hint");
        for (String line : TextLayout.wrap(font, hint, panelW - 32)) {
            graphics.text(font, line, panelX + 16, y, UiColors.TEXT_LABEL, false);
            y += 10;
        }

        if (JInfoClient.api().isAccount()) {
            graphics.text(font, Messages.s("account_logged") + (JInfoClient.api().accountName() == null ? "" : JInfoClient.api().accountName()),
                    panelX + 16, panelY + 70, UiColors.OK, false);
            String uuid = JInfoClient.api().currentUuid();
            if (uuid != null) {
                graphics.text(font, uuid, panelX + 16, panelY + 86, UiColors.TEXT_LABEL, false);
            }
        } else if (JInfoClient.api().isGuest()) {
            String name = JInfoClient.api().currentName();
            graphics.text(font, Messages.s("account_guest_mode") + (name == null ? "" : ": " + name),
                    panelX + 16, panelY + 70, UiColors.OK, false);
            String uuid = JInfoClient.api().currentUuid();
            if (uuid != null) {
                graphics.text(font, uuid, panelX + 16, panelY + 86, UiColors.TEXT_LABEL, false);
            }
        } else {
            graphics.text(font, Messages.s("account_user"), panelX + 16, panelY + 64,
                    UiColors.TEXT_LABEL, false);
            graphics.text(font, Messages.s("account_pass"), panelX + 16, panelY + 98,
                    UiColors.TEXT_LABEL, false);
        }

        if (!note.isBlank()) {
            int color = noteError ? UiColors.WARN : UiColors.OK;
            graphics.text(font, TextLayout.fit(font, note, panelW - 32),
                    panelX + 16, panelY + panelH - 46, color, false);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
        }
    }
}
