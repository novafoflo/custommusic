package com.custommusic;

import javazoom.jl.decoder.*;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class MusicPlayer {
    private final Path dir;
    private volatile float volume = 1f;
    private volatile boolean paused = false;
    private volatile boolean skip = false;
    private volatile boolean running = true;
    private volatile String current = "";
    private volatile Path requested = null;
    private Thread thread;

    public MusicPlayer(Path dir) { this.dir = dir; }

    public void setVolume(float v) { volume = v; }
    public void next() { skip = true; paused = false; }
    public void togglePause() { paused = !paused; }
    public boolean isPaused() { return paused; }
    public String currentName() { return current; }
    public void playNow(Path p) { requested = p; skip = true; paused = false; }
    public void shutdown() { running = false; skip = true; }

    public void start() {
        thread = new Thread(this::loop, "CustomMusic-Player");
        thread.setDaemon(true);
        thread.start();
    }

    public List<Path> list() {
        List<Path> l = new ArrayList<>(scan());
        l.sort(Comparator.comparing(x -> x.getFileName().toString().toLowerCase()));
        return l;
    }

    private List<Path> scan() {
        try (Stream<Path> s = Files.list(dir)) {
            List<Path> l = new ArrayList<>();
            s.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".mp3")).forEach(l::add);
            return l;
        } catch (Exception e) { return List.of(); }
    }

    private void loop() {
        Deque<Path> queue = new ArrayDeque<>();
        while (running) {
            Path p = requested;
            if (p != null) {
                requested = null;
            } else {
                if (queue.isEmpty()) {
                    List<Path> files = scan();
                    if (files.isEmpty()) { sleep(3000); continue; }
                    Collections.shuffle(files);
                    queue.addAll(files);
                }
                p = queue.poll();
            }
            current = p.getFileName().toString().replaceFirst("(?i)\\.mp3$", "");
            try { play(p); } catch (Throwable t) { t.printStackTrace(); sleep(500); }
        }
    }

    private void play(Path file) throws Exception {
        skip = false;
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
            Bitstream bs = new Bitstream(in);
            Decoder dec = new Decoder();
            SourceDataLine line = null;
            try {
                Header h;
                while (running && !skip && (h = bs.readFrame()) != null) {
                    SampleBuffer buf = (SampleBuffer) dec.decodeFrame(h, bs);
                    if (line == null) {
                        AudioFormat fmt = new AudioFormat(dec.getOutputFrequency(), 16, dec.getOutputChannels(), true, false);
                        line = AudioSystem.getSourceDataLine(fmt);
                        line.open(fmt);
                        line.start();
                    }
                    while (paused && running && !skip) sleep(100);
                    if (skip) break;
                    short[] s = buf.getBuffer();
                    int len = buf.getBufferLength();
                    byte[] out = new byte[len * 2];
                    float v = volume;
                    for (int i = 0; i < len; i++) {
                        short x = (short) Math.max(-32768, Math.min(32767, s[i] * v));
                        out[i * 2] = (byte) x;
                        out[i * 2 + 1] = (byte) (x >> 8);
                    }
                    line.write(out, 0, out.length);
                    bs.closeFrame();
                }
                if (line != null && !skip) line.drain();
            } finally {
                if (line != null) { line.stop(); line.close(); }
                bs.close();
            }
        }
    }

    private static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }
}
