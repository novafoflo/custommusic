package com.custommusic;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.List;

public class MusicScreen extends Screen {
    private static final int PER_PAGE = 8;
    private final MusicPlayer player;
    private List<Path> files;
    private int page = 0;

    public MusicScreen(MusicPlayer player) {
        super(Component.literal("Custom Music"));
        this.player = player;
    }

    @Override
    protected void init() {
        files = player.list();
        int pages = Math.max(1, (files.size() + PER_PAGE - 1) / PER_PAGE);
        if (page >= pages) page = pages - 1;
        int x = this.width / 2 - 150;
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < files.size(); i++) {
            Path f = files.get(start + i);
            String name = f.getFileName().toString().replaceFirst("(?i)\\.mp3$", "");
            name = this.font.plainSubstrByWidth(name, 280);
            this.addRenderableWidget(Button.builder(Component.literal(name), b -> player.playNow(f))
                    .bounds(x, 50 + i * 24, 300, 20).build());
        }
        int y = this.height - 30;
        int bx = this.width / 2 - 147;
        this.addRenderableWidget(Button.builder(Component.literal("<"), b -> { if (page > 0) { page--; rebuildWidgets(); } })
                .bounds(bx, y, 40, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), b -> { if (page < pages - 1) { page++; rebuildWidgets(); } })
                .bounds(bx + 45, y, 40, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Pause / Reprendre"), b -> player.togglePause())
                .bounds(bx + 90, y, 110, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Suivant"), b -> player.next())
                .bounds(bx + 205, y, 90, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        String cur = player.currentName();
        String text = files.isEmpty()
                ? "Aucun MP3 dans le dossier custom_music"
                : (cur.isEmpty() ? "Clique sur une musique" : "En cours : " + cur);
        g.drawCenteredString(this.font, Component.literal(this.font.plainSubstrByWidth(text, this.width - 20)),
                this.width / 2, 30, 0xAAAAAA);
    }
}
