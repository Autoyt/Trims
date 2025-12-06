package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import dev.auto.trims.effectHandlers.helpers.IBaseEffectHandler;
import dev.auto.trims.effectHandlers.helpers.OptimizedHandler;
import dev.auto.trims.managers.TrimManager;
import dev.auto.trims.managers.EffectManager;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LevitationHandler extends OptimizedHandler implements IBaseEffectHandler, Listener {
    private final Main instance;
    private static final TrimPattern defaultPattern = TrimPattern.DUNE;
    private static final int cooldown = 5 * 20;
    private final Set<UUID> lv4Players = new HashSet<>();
    private final Set<UUID> sneakingPlayers = new HashSet<>();
    private final Set<UUID> onCooldown = new HashSet<>();

    public LevitationHandler(Main instance) {
        super(defaultPattern);
        this.instance = instance;
        TrimManager.handlers.add(this);
    }

    @Override
    public void onlinePlayerTick(Player player) {
        UUID id = player.getUniqueId();
        int instanceCount = getTrimCount(id);

        if (instanceCount >= 4) {
            lv4Players.add(id);
        }
        else {
            lv4Players.remove(id);
        }

        if (instanceCount > 0 && !onCooldown.contains(id)) {
            if (!sneakingPlayers.contains(id)) {
                int amplifier = Math.min(instanceCount, 4) - 1;
                EffectManager.wantEffect(id, new PotionEffect(PotionEffectType.LEVITATION, 3600, amplifier, false, false));
            }

            else {
                if (instanceCount < 4) {
                    int amplifier = Math.min(instanceCount, 4) - 1;
                    EffectManager.wantEffect(id, new PotionEffect(PotionEffectType.LEVITATION, 3600, amplifier, false, false));
                } else {
                    EffectManager.wantEffect(id, new PotionEffect(PotionEffectType.SLOW_FALLING, 3600, 0, false, false));
                }
            }

        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        if (!(lv4Players.contains(id))) {
            sneakingPlayers.remove(id);
            return;
        }

        if (event.isSneaking()) {
            sneakingPlayers.add(id);
        }
        else {
            sneakingPlayers.remove(id);
        }
    }

    @EventHandler
    public void onProjectileHitEvent(ProjectileHitEvent event) {
        if (!(event.getHitEntity() instanceof Player player)) return;
        UUID id = player.getUniqueId();
        int instanceCount = getTrimCount(id);

        if (instanceCount > 0) {
            if (onCooldown.contains(id)) return;
            onCooldown.add(id);
            instance.getServer().getScheduler().runTaskLater(instance, () -> onCooldown.remove(id), cooldown);

            player.getWorld().spawnParticle(Particle.ASH, player.getLocation(), 10);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    @EventHandler
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        super.onArmorChange(event);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        lv4Players.remove(uuid);
        sneakingPlayers.remove(uuid);
        onCooldown.remove(uuid);
    }
}
