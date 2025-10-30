package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import dev.auto.trims.particles.FXUtilities;
import dev.auto.trims.particles.utils.CircleFX;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class AbsorptionListener implements IBaseEffectHandler, Listener, Runnable {
    private final Main instance;
    public final TrimPattern defaultPattern = TrimPattern.VEX;
    private final Set<UUID> lv4Players = new HashSet<>();
    private final Map<UUID, CircleFX> fx = new HashMap<>();
    private final Map<UUID, Integer> countdown = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    public AbsorptionListener(Main instance) {
        this.instance = instance;
        TrimManager.handlers.add(this);

        instance.getServer().getScheduler().runTaskTimer(instance, this, 1, 1);

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team regenGlow = scoreboard.getTeam("regenGlow");
        if (regenGlow == null) {
            regenGlow = scoreboard.registerNewTeam("regenGlow");
            regenGlow.color(NamedTextColor.DARK_RED);
        }
    }

    @Override
    public void onlinePlayerTick(Player player) {
        UUID id = player.getUniqueId();
        int instanceCount = getTrimCount(id, defaultPattern);

        if (instanceCount >= 4) {
            if (!fx.containsKey(id)) {
                fx.put(id, FXUtilities.AbsorptionFX(player));
            }
            lv4Players.add(id);
        }
        else {
            CircleFX circleFX = fx.remove(id);
            if (circleFX != null) {
                circleFX.cancel();
            }
            lv4Players.remove(id);
        }

        if (instanceCount > 0) {
            showBossBar(id);
        }
        else {
            BossBar bar = bossBars.remove(id);
            if (bar != null) {
                player.hideBossBar(bar);
            }
        }
    }

    private void showBossBar(UUID id) {
        int instances = getTrimCount(id, defaultPattern);
        if (instances < 0) return;

        Player player = instance.getServer().getPlayer(id);
        if (player == null) {
            bossBars.remove(id);
            return;
        }

        if (!bossBars.containsKey(id) && player.isOnline()) {
            BossBar bar = BossBar.bossBar(Component.text("Charging 0%"), 0, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
            bossBars.put(id, bar);
        }

        BossBar bar = bossBars.get(id);
        player.showBossBar(bar);
    }

    @EventHandler
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        handleEquip(event, defaultPattern);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        lv4Players.remove(id);
        countdown.remove(id);

        CircleFX circleFX = fx.remove(id);
        if (circleFX != null) {
            circleFX.cancel();
        }

        BossBar bar = bossBars.remove(id);
        if (bar != null) {
            event.getPlayer().hideBossBar(bar);
        }

    }
    @Override
    public void run() {
        List<Player> sources = new ArrayList<>(lv4Players.size());
        for (UUID id : lv4Players) {
            Player s = Bukkit.getPlayer(id);
            if (s != null && s.isOnline()) sources.add(s);
        }

        final int PERIOD = 20 * 15;
        final double R2 = 12.0 * 12.0;
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team regenGlow = scoreboard.getTeam("regenGlow");

        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID uid = p.getUniqueId();
            boolean playable = p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE;
            if (!playable) continue;
            int instances = getTrimCount(uid, defaultPattern);

            if (instances > 0) {
                AttributeInstance maxAbsHealth = p.getAttribute(Attribute.MAX_ABSORPTION);
                if (maxAbsHealth != null) {
                    double desired = Math.min(40.0, instances * 10.0);
                    if (maxAbsHealth.getBaseValue() != desired) {
                        maxAbsHealth.setBaseValue(desired);
                    }
                }

                int time = countdown.getOrDefault(uid, 0) + 1;
                if (time >= PERIOD) {
                    countdown.put(uid, 0);
                    double cap = instances * 10.0;
                    double cur = p.getAbsorptionAmount();
                    
                    if (cur < cap) {
                        p.setAbsorptionAmount(Math.min(cap, cur + 10.0));

                        Location loc = p.getLocation();
                        Color color = Color.fromRGB(0xFBD716);
                        Particle.DustOptions options = new Particle.DustOptions(color, 1.25f);
                        loc.getWorld().spawnParticle(Particle.DUST, loc, 10, 0.3, 0.3, 0.3, 1.5, options);

                        p.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1f);
                    }

                    BossBar bar = bossBars.get(uid);
                    if (bar != null) {
                        bar.progress(0f);
                        bar.name(Component.text("Charging 0%"));
                        p.showBossBar(bar);
                    }
                } 
                else {
                    countdown.put(uid, time);
                    BossBar bar = bossBars.get(uid);
                    if (bar != null) {
                        float progress = Math.min(1f, (float) time / (float) PERIOD);
                        int percent = Math.min(100, Math.max(0, Math.round(progress * 100f)));
                        bar.progress(progress);
                        bar.name(Component.text("Charging " + percent + "%"));
                        p.showBossBar(bar);
                    }
                }
            }
            else {
                countdown.remove(uid);
                AttributeInstance maxAbsHealth = p.getAttribute(Attribute.MAX_ABSORPTION);
                if (maxAbsHealth != null && maxAbsHealth.getBaseValue() != 0.0) {
                    maxAbsHealth.setBaseValue(0.0);
                }
            }

            boolean near = false;
            if (!sources.isEmpty()) {
                double px = p.getX(), py = p.getY(), pz = p.getZ();
                for (Player s : sources) {
                    if (s == p) continue;
                    double dx = s.getX() - px, dy = s.getY() - py, dz = s.getZ() - pz;
                    if (dx*dx + dy*dy + dz*dz <= R2) { near = true; break; }
                }
            }

            if (regenGlow != null) {
                boolean inTeam = regenGlow.hasEntry(p.getName());
                if (near) {
                    if (!inTeam) regenGlow.addEntry(p.getName());
                    if (!p.isGlowing()) p.setGlowing(true);
                } else {
                    if (inTeam) regenGlow.removeEntry(p.getName());
                    if (p.isGlowing()) p.setGlowing(false);
                }
            }

            if (near) {
                PotionEffect active = p.getPotionEffect(PotionEffectType.REGENERATION);
                if (active == null || active.getDuration() < 40 || active.getAmplifier() < 0) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 15, 0, false, false));
                }
            }
        }
    }
}
