package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import dev.auto.trims.effectHandlers.helpers.IBaseEffectHandler;
import dev.auto.trims.managers.TrimManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SuicideVestHandler implements IBaseEffectHandler, Listener {
    private final Main instance;
    private final TrimPattern defaultPattern = TrimPattern.SHAPER;

    public SuicideVestHandler(Main instance) {
        this.instance = instance;
        TrimManager.handlers.add(this);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID id = player.getUniqueId();
        int instanceCount = getTrimCount(id, defaultPattern);

        if (!(instanceCount > 0)) return;

        List<ItemStack> drops = new ArrayList<>(event.getDrops());
        int xp = event.getDroppedExp();

        event.getDrops().clear();
        event.setDroppedExp(0);

        Location loc = player.getLocation().clone();
        World world = loc.getWorld();
        if (world == null) return;

        switch (instanceCount) {
            case 1 -> {
                loc.getWorld().createExplosion(loc, 2.0f);
            }

            case 2 -> {
                loc.getWorld().createExplosion(loc, 2.5f);
            }

            case 3 -> {
                loc.getWorld().createExplosion(loc, 4.0f);
            }

            case 4 -> {
                loc.getWorld().createExplosion(loc, 5.0f);
            }
        }

        Bukkit.getScheduler().runTaskLater(instance, () -> {
            if (!loc.isChunkLoaded()) loc.getChunk().load();
            for (ItemStack it : drops) {
                if (it != null && it.getType().isItem()) world.dropItem(loc, it);
            }

            ExperienceOrb orb = world.spawn(loc, ExperienceOrb.class);
            orb.setExperience(xp);

        }, 5);
    }

    @EventHandler
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        handleEquip(event, defaultPattern);
    }

    @EventHandler
    public void onCreeperExplode(ExplosionPrimeEvent event) {
        if (!(event.getEntity() instanceof Creeper creeper)) return;
        for (Player player : creeper.getWorld().getNearbyPlayers(creeper.getLocation(), 10)) {
            if (getTrimCount(player.getUniqueId(), defaultPattern) > 0) {
                event.setCancelled(true);
                creeper.setAI(false);
                return;
            }
        }
    }
}
