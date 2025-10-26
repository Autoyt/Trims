package dev.auto.trims.effectHandlers;


import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
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

public class NightVisionHandler implements Listener, IBaseEffectHandler {
    private final Main instance;
    private final TrimPattern defaultPattern = TrimPattern.DUNE;
    private final Set<UUID> lv4Players = new HashSet<>();
    private final Map<UUID, Set<UUID>> glowingTargets = new HashMap<>();
    
    private final double HORIZ_SIGHT_DISTANCE = 32.0;

    private Team playerGlowTeam;
    private Team hideNametagTeam;

    public NightVisionHandler(Main instance) {
        this.instance = instance;
        TrimManager.handlers.add(this);

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        
        playerGlowTeam = scoreboard.getTeam("playerGlow");
        if (playerGlowTeam == null) {
            playerGlowTeam = scoreboard.registerNewTeam("playerGlow");
            playerGlowTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            playerGlowTeam.color(NamedTextColor.DARK_PURPLE);
        }

        hideNametagTeam = scoreboard.getTeam("hideNametag");
        if (hideNametagTeam == null) {
            hideNametagTeam = scoreboard.registerNewTeam("hideNametag");
            hideNametagTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS);
        }
    }

    @Override
    public void onTick() {
        for (Player player : instance.getServer().getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            PlayerArmorSlots slots = TrimManager.getSlots(id);
            int instanceCount = slots.instancesOfTrim(this.defaultPattern);

            // Maintain LV4 membership and per-tick behavior
            if (instanceCount >= 4) {
                lv4Players.add(id);
                handleLV4(player);
            } else {
                // Player no longer has LV4 - clean up any glow/team state once
                if (lv4Players.remove(id)) {
                    hideNametagTeam.removeEntry(player.getName());

                    // Turn off glow for all targets this viewer was tracking, but only if no other viewer still tracks them
                    Set<UUID> targets = glowingTargets.remove(id);
                    if (targets != null) {
                        for (UUID targetId : targets) {
                            boolean stillNeeded = glowingTargets.values().stream().anyMatch(set -> set.contains(targetId));
                            if (!stillNeeded) {
                                Player target = Bukkit.getPlayer(targetId);
                                if (target != null) {
                                    target.setGlowing(false);
                                    playerGlowTeam.removeEntry(target.getName());
                                }
                            }
                        }
                    }
                }
            }


            // Apply night vision potion if needed (any number of DUNE pieces)
            if (instanceCount > 0) {
                // Request via coordinator; it will add/refresh and handle removals when not desired
                TrimManager.wantEffect(id, new PotionEffect(PotionEffectType.NIGHT_VISION, 2400, 0, false, false));
            }
        }
    }

    @EventHandler
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        handleEquip(event, defaultPattern);
    }

    private void handleLV4(Player viewer) {
        if (!lv4Players.contains(viewer.getUniqueId())) return;
        
        // Hide viewer's nametag using team
        hideNametagTeam.addEntry(viewer.getName());

        double verticalRange = viewer.getWorld().getMaxHeight() - viewer.getWorld().getMinHeight();
        Location viewerLoc = viewer.getLocation();
        
        Set<UUID> previousTargets = glowingTargets.computeIfAbsent(viewer.getUniqueId(), k -> new HashSet<>());
        Set<UUID> currentTargets = new HashSet<>();

        // Find all players currently in range
        Collection<Player> nearbyPlayers = viewer.getWorld().getNearbyPlayers(
            viewerLoc, 
            HORIZ_SIGHT_DISTANCE, 
            verticalRange, 
            p -> p != viewer
        );

        for (Player target : nearbyPlayers) {
            UUID targetId = target.getUniqueId();
            currentTargets.add(targetId);
            
            // If this is a new target, turn on glow
            if (!previousTargets.contains(targetId)) {
                target.setGlowing(true);
                playerGlowTeam.addEntry(target.getName());
            }
        }

        // Turn off glow for players who left range
        for (UUID targetId : previousTargets) {
            if (!currentTargets.contains(targetId)) {
                Player target = Bukkit.getPlayer(targetId);
                if (target != null) {
                    target.setGlowing(false);
                    playerGlowTeam.removeEntry(target.getName());
                }
            }
        }

        // Update tracking
        glowingTargets.put(viewer.getUniqueId(), currentTargets);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        UUID leaverId = event.getPlayer().getUniqueId();

        // Remove viewer status and nametag
        lv4Players.remove(leaverId);
        hideNametagTeam.removeEntry(event.getPlayer().getName());

        // Turn off glow for targets that were glowing only because of this viewer
        Set<UUID> targets = glowingTargets.remove(leaverId);
        if (targets != null) {
            for (UUID targetId : targets) {
                boolean stillNeeded = glowingTargets.values().stream().anyMatch(set -> set.contains(targetId));
                if (!stillNeeded) {
                    Player target = Bukkit.getPlayer(targetId);
                    if (target != null) {
                        target.setGlowing(false);
                        playerGlowTeam.removeEntry(target.getName());
                    }
                }
            }
        }

        // If the leaver themselves were glowing (e.g., added as a target elsewhere), remove their entry
        playerGlowTeam.removeEntry(event.getPlayer().getName());

        // Optional: explicitly clear the glowing flag on the leaver
        event.getPlayer().setGlowing(false);
    }
}
