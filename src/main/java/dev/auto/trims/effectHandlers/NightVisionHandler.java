package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import dev.auto.trims.effectHandlers.helpers.IBaseEffectHandler;
import dev.auto.trims.effectHandlers.helpers.OptimizedHandler;
import dev.auto.trims.managers.EffectManager;
import dev.auto.trims.managers.TrimManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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

public class NightVisionHandler extends OptimizedHandler implements Listener, IBaseEffectHandler {
    private final Main instance;
    private static final TrimPattern defaultPattern = TrimPattern.HOST;

    private final Set<UUID> lv4Viewers = new HashSet<>();
    private final Map<UUID, Set<UUID>> perViewerTargets = new HashMap<>();
    private final Map<UUID, Integer> glowRefCounts = new HashMap<>();

    private static final double HORIZ_SIGHT_DISTANCE = 32.0;

    private final Team glowTeam;

    public NightVisionHandler(Main instance) {
        super(defaultPattern);
        this.instance = instance;
        TrimManager.handlers.add(this);

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam("playerGlow");
        if (team == null) {
            team = scoreboard.registerNewTeam("playerGlow");
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            team.color(NamedTextColor.DARK_PURPLE);
        }
        this.glowTeam = team;

        // Schedule periodic proximity processing separate from OptimizedHandler.run()
        instance.getServer().getScheduler().runTaskTimer(instance, this::processGlowTick, 1L, 5L);
    }

    @Override
    public void onlinePlayerTick(Player player) {
        UUID id = player.getUniqueId();
        int instanceCount = getTrimCount(id);

        if (instanceCount >= 4) {
            lv4Viewers.add(id);
        } else {
            if (lv4Viewers.remove(id)) {
                Set<UUID> prev = perViewerTargets.remove(id);
                if (prev != null) {
                    for (UUID targetId : prev) {
                        decrementGlow(targetId);
                    }
                }
            }
        }

        // Apply night vision for any number of DUNE pieces
        if (instanceCount > 0) {
            EffectManager.wantEffect(id, new PotionEffect(PotionEffectType.NIGHT_VISION, 3600, 0, false, false));
        }
    }

    private void processGlowTick() {
        if (lv4Viewers.isEmpty()) return;

        for (UUID viewerId : new HashSet<>(lv4Viewers)) {
            Player viewer = instance.getServer().getPlayer(viewerId);
            if (viewer == null) {
                // Viewer is offline, clean up
                lv4Viewers.remove(viewerId);
                Set<UUID> prev = perViewerTargets.remove(viewerId);
                if (prev != null) prev.forEach(this::decrementGlow);
                continue;
            }

            double verticalRange = viewer.getWorld().getMaxHeight() - viewer.getWorld().getMinHeight();
            Location viewerLoc = viewer.getLocation();

            Set<UUID> previousTargets = perViewerTargets.computeIfAbsent(viewerId, k -> new HashSet<>());
            Set<UUID> currentTargets = new HashSet<>();

            Collection<Player> nearbyPlayers = viewer.getWorld().getNearbyPlayers(
                    viewerLoc,
                    HORIZ_SIGHT_DISTANCE,
                    verticalRange,
                    p -> !p.equals(viewer)
            );

            for (Player target : nearbyPlayers) {
                UUID targetId = target.getUniqueId();
                currentTargets.add(targetId);
                if (!previousTargets.contains(targetId)) {
                    incrementGlow(target);
                }
            }

            // Remove glow for players who left this viewer's range
            for (UUID targetId : previousTargets) {
                if (!currentTargets.contains(targetId)) {
                    decrementGlow(targetId);
                }
            }

            perViewerTargets.put(viewerId, currentTargets);
        }
    }

    private void incrementGlow(Player target) {
        UUID targetId = target.getUniqueId();
        int count = glowRefCounts.getOrDefault(targetId, 0) + 1;
        glowRefCounts.put(targetId, count);
        if (count == 1) {
            target.setGlowing(true);
            glowTeam.addEntry(target.getName());
        }
    }

    private void decrementGlow(UUID targetId) {
        Integer count = glowRefCounts.get(targetId);
        if (count == null || count <= 0) return;
        count -= 1;
        if (count <= 0) {
            glowRefCounts.remove(targetId);
            Player target = Bukkit.getPlayer(targetId);
            if (target != null) {
                target.setGlowing(false);
                glowTeam.removeEntry(target.getName());
            }
        } else {
            glowRefCounts.put(targetId, count);
        }
    }

    @EventHandler
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        onArmorChange(event);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        UUID leaverId = event.getPlayer().getUniqueId();

        // Remove viewer status
        if (lv4Viewers.remove(leaverId)) {
            Set<UUID> targets = perViewerTargets.remove(leaverId);
            if (targets != null) {
                for (UUID targetId : targets) {
                    decrementGlow(targetId);
                }
            }
        }

        glowTeam.removeEntry(event.getPlayer().getName());
        event.getPlayer().setGlowing(false);
    }
}
