package effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.util.Vector;

import java.util.*;

public class SpeedHandler implements Listener, IBaseEffectHandler {
    private final Main instance;
    private final TrimPattern defaultPattern = TrimPattern.EYE;
    private final Set<UUID> lv4Players = new HashSet<>();
    private final Map<UUID, Long> lastDash = new HashMap<>();

    public SpeedHandler(Main instance) {
        this.instance = instance;
    }

    @Override
    public void onTick() {
        for (Player player : instance.getServer().getOnlinePlayers()) {
            int instanceCount = TrimManager.getSlots(player.getUniqueId()).instancesOfTrim(this.defaultPattern);

            if (instanceCount >= 4) {
                lv4Players.add(player.getUniqueId());
            }
            else {
                lv4Players.remove(player.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        handleEquip(event, defaultPattern);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        lv4Players.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onGroundRearm(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;

        // simple grounded check
        if (p.isOnGround() || p.getLocation().subtract(0, 0.1, 0).getBlock().getType() != Material.AIR) {
            if (!p.getAllowFlight()) p.setAllowFlight(true);
            p.setFlying(false);
        }
    }

    @EventHandler
    public void onDoubleJump(PlayerToggleFlightEvent event) {
        Player p = event.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;

        event.setCancelled(true);
        p.setAllowFlight(false);

        long now = System.currentTimeMillis();
        long last = lastDash.getOrDefault(p.getUniqueId(), 0L);
        if (now - last < 3000) return;
        lastDash.put(p.getUniqueId(), now);

        Vector v = p.getLocation().getDirection().setY(0).normalize().multiply(1.8);
        v.setY(p.getVelocity().getY() + 0.2);
        p.setVelocity(v);

        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 1.4f);
        p.setFallDistance(0f);
    }
}
