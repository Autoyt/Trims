package dev.auto.trims.effectHandlers.helpers;

import dev.auto.trims.Main;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;
import java.util.function.BiConsumer;

public class StatusBar {
    private float progress;
    private boolean shown;
    private final UUID playerId;
    private final BossBar bossBar = BossBar.bossBar(Component.text("precompute"), 0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
    @Setter
    private BiConsumer<UUID, StatusBar> consumer;
    @Setter
    private int hideTicks = 5;

    public StatusBar(UUID player) {
        this.playerId = player;

        show();
    }

    public void hide() {
        Player p = Bukkit.getPlayer(playerId);
        if (p != null && p.isOnline()) p.hideBossBar(bossBar);
        shown = false;
    }

    public void show() {
        Player p = Bukkit.getPlayer(playerId);
        if (p != null && p.isOnline()) p.showBossBar(bossBar);
        shown = true;
    }

    public void setProgress(float preprogress) {
        float postprogress = clamp(preprogress);
        if (postprogress == this.progress) return;

        bossBar.progress(postprogress);
        if (!shown) show();

        if (postprogress >= 1f) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (getProgress() >= 1f) hide();
                }
            }.runTaskLater(Main.getInstance(), hideTicks);
        }

        this.progress = postprogress;
        if (consumer != null) consumer.accept(playerId, this);
    }

    public void incrementProgress(float delta) {
        setProgress(this.progress + delta);
    }

    public void removeProgress(float delta) {
        setProgress(this.progress - delta);
    }

    public float getProgress() {
        return clamp(progress);
    }

    private float clamp(float x) {
        return Math.max(0f, Math.min(1f, x));
    }

    public StatusBar setTitle(String title) {
        bossBar.name(Component.text(title));
        return this;
    }

    public StatusBar setColor(BossBar.Color color) {
        bossBar.color(color);
        return this;
    }

    public StatusBar setOverlay(BossBar.Overlay overlay) {
        bossBar.overlay(overlay);
        return this;
    }
}
