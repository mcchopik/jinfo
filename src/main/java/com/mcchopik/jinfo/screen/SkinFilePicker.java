package com.mcchopik.jinfo.screen;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;

public final class SkinFilePicker {

    private SkinFilePicker() {
    }

    public static void open(Consumer<String> onPicked) {
        CompletableFuture.runAsync(() -> {
            try {
                FileDialog dialog = new FileDialog((Frame) null, "Выберите PNG скин", FileDialog.LOAD);
                dialog.setFilenameFilter((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".png"));
                dialog.setVisible(true);
                String file = dialog.getFile();
                String dir = dialog.getDirectory();
                dialog.dispose();
                if (file == null || dir == null) {
                    return;
                }
                String path = new File(dir, file).getAbsolutePath();
                Minecraft client = Minecraft.getInstance();
                if (client != null) {
                    client.execute(() -> onPicked.accept(path));
                } else {
                    onPicked.accept(path);
                }
            } catch (Throwable ignored) {
            }
        });
    }
}
