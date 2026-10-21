package com.custommusic;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;

@Mod(value = "custommusic", dist = Dist.CLIENT)
public class CustomMusic {
    private static final KeyMapping NEXT = new KeyMapping("key.custommusic.next", GLFW.GLFW_KEY_N, "key.categories.custommusic");
    private static final KeyMapping PAUSE = new KeyMapping("key.custommusic.pause", GLFW.GLFW_KEY_M, "key.categories.custommusic");
    private final MusicPlayer player;

    public CustomMusic(IEventBus modBus) {
        Path dir = FMLPaths.GAMEDIR.get().resolve("custom_music");
        try { Files.createDirectories(dir); } catch (Exception ignored) {}
        player = new MusicPlayer(dir);

        modBus.addListener((RegisterKeyMappingsEvent e) -> {
            e.register(NEXT);
            e.register(PAUSE);
        });
        NeoForge.EVENT_BUS.addListener(this::onTick);

        Runtime.getRuntime().addShutdownHook(new Thread(player::shutdown));
        player.start();
    }

    private void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        mc.getMusicManager().stopPlaying();

        double master = mc.options.getSoundSourceVolume(SoundSource.MASTER);
        double music = mc.options.getSoundSourceVolume(SoundSource.MUSIC);
        player.setVolume((float) (master * music));

        while (NEXT.consumeClick()) {
            player.next();
            msg(mc, "§a♪ " + player.currentName());
        }
        while (PAUSE.consumeClick()) {
            player.togglePause();
            msg(mc, player.isPaused() ? "§e♪ Pause" : "§a♪ " + player.currentName());
        }
    }

    private static void msg(Minecraft mc, String s) {
        if (mc.player != null) mc.player.displayClientMessage(Component.literal(s), true);
    }
}
