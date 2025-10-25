package effectHandlers;


import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
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
            int instanceCount = TrimManager.getSlots(player.getUniqueId()).instancesOfTrim(this.defaultPattern);
            System.out.println(instanceCount);
            if (instanceCount >= 4) {
                lv4Players.add(player.getUniqueId());
                handleLV4(player);
            }
            else {
                // Player no longer has LV4 - clean up
                if (lv4Players.remove(player.getUniqueId())) {
                    hideNametagTeam.removeEntry(player.getName());
                    
                    // Turn off glow for all targets this viewer was tracking
                    Set<UUID> targets = glowingTargets.remove(player.getUniqueId());
                    if (targets != null) {
                        for (UUID targetId : targets) {
                            Player target = Bukkit.getPlayer(targetId);
                            if (target != null) {
                                target.setGlowing(false);
                                playerGlowTeam.removeEntry(target.getName());
                            }
                        }
                    }
                }
            }

            // Apply night vision potion if needed
            if (instanceCount > 0) {
                PotionEffect current = player.getPotionEffect(PotionEffectType.NIGHT_VISION);
                if (current == null || current.getDuration() <= 60) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 200, 0, false, false));
                }
            }
        }
    }

    @EventHandler
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        System.out.println("Armor change in night vision handler");
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
        lv4Players.remove(event.getPlayer().getUniqueId());
        glowingTargets.remove(event.getPlayer().getUniqueId());
        hideNametagTeam.removeEntry(event.getPlayer().getName());
        playerGlowTeam.removeEntry(event.getPlayer().getName());
        event.getPlayer().setGlowing(false);
    }
}
