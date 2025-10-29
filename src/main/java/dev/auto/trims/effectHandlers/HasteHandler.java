package dev.auto.trims.effectHandlers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import dev.auto.trims.Main;
import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class HasteHandler implements IBaseEffectHandler, Listener, Runnable {
    private final Main instance;
    private final TrimPattern defaultPattern = TrimPattern.FLOW;
    private final NamespacedKey DROP_KEY;
    private final Set<UUID> lv4Players = new HashSet<>();
    private final Map<UUID, Long> fxCoolDown = new HashMap<>();
    private AtomicInteger fxCounter = new AtomicInteger(0);

    public HasteHandler(Main instance) {
        this.instance = instance;
        DROP_KEY = new NamespacedKey(instance, "drop");

        TrimManager.handlers.add(this);
        Bukkit.getScheduler().runTaskTimer(instance, this, 1, 5);
    }

    @Override
    public void onlinePlayerTick(Player player) {
        UUID id = player.getUniqueId();
        int instanceCount = getTrimCount(id, defaultPattern);

        if (instanceCount >= 4) {
            lv4Players.add(id);
        }
        else {
            lv4Players.remove(id);
        }

        if (instanceCount > 0) {
            int amplifier = Math.min(instanceCount, 4) - 1;
            TrimManager.wantEffect(id, new PotionEffect(PotionEffectType.HASTE, 3600, amplifier, false, false));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        event.getItemDrop().getPersistentDataContainer().set(DROP_KEY, PersistentDataType.STRING, player.getUniqueId().toString());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDrop(ItemSpawnEvent event) {
        if (lv4Players.isEmpty()) return;
        Item item = event.getEntity();
        Location loc = item.getLocation().clone().add(0, 0.25, 0);

        Collection<Player> nearbyPlayers = loc.getWorld().getNearbyPlayers(
                loc,
                10,
                p -> lv4Players.contains(p.getUniqueId())
        );

        if (nearbyPlayers.isEmpty()) return;

        Player nearest = null;
        double best = Double.POSITIVE_INFINITY;
        for (Player p : nearbyPlayers) {
            double d2 = p.getLocation().distanceSquared(loc);
            if (d2 < best) {
                best = d2; nearest = p;
            }
        }

        if (nearest == null) return;
        UUID nearestId = nearest.getUniqueId();

        PersistentDataContainer pdc = item.getPersistentDataContainer();
        if (pdc.has(DROP_KEY, PersistentDataType.STRING)) {
            String uuidStr = pdc.get(DROP_KEY, PersistentDataType.STRING);
            if (uuidStr == null) throw new IllegalStateException("UUID drop key string is null");

            UUID uuid = UUID.fromString(uuidStr);
            if (uuid.equals(nearestId)) return;
        }

        if (fxCounter.incrementAndGet() < 100) {
            final long currentTime = System.currentTimeMillis();
            final long lastTime = fxCoolDown.getOrDefault(nearestId, 0L);

            if (currentTime - lastTime >= 200) {
                if (currentTime - lastTime >= 600) {
                    World w = loc.getWorld();
                    w.playSound(nearest.getLocation().clone().add(0, 0.5, 0), Sound.BLOCK_TRIAL_SPAWNER_EJECT_ITEM, 1, 1);
                }

                animate(loc, nearest.getLocation().clone().add(0, 0.5, 0));
                fxCoolDown.put(nearestId, currentTime);
            }


        }

        ItemStack originalStack = item.getItemStack();
        Map<Integer, ItemStack> leftover = nearest.getInventory().addItem(originalStack.clone());

        int remaining = 0;
        for (ItemStack it : leftover.values()) {
            if (it != null) remaining += it.getAmount();
        }

        if (remaining <= 0) {
            event.setCancelled(true);
            item.remove();
        }
        else {
            ItemStack newStack = originalStack.clone();
            newStack.setAmount(remaining);
            item.setItemStack(newStack);
            item.teleport(nearest.getLocation());
        }
    }

    @EventHandler
    public void onMerge(ItemMergeEvent e) {
        PersistentDataContainer from = e.getEntity().getPersistentDataContainer();
        PersistentDataContainer to = e.getTarget().getPersistentDataContainer();

        if (from.has(DROP_KEY, PersistentDataType.STRING) && !to.has(DROP_KEY, PersistentDataType.STRING)) {
            to.set(DROP_KEY, PersistentDataType.STRING, Objects.requireNonNull(from.get(DROP_KEY, PersistentDataType.STRING)));
        }
    }

    private void animate(Location from, Location to) {
        World w = from.getWorld();
        new BukkitRunnable() {
            double d = 0, step = 0.35;
            final Vector dir = to.clone().subtract(from).toVector().normalize();
            final double len = from.distance(to);

            @Override public void run() {
                if (len <= 0.01) { cancel(); return; }
                d = Math.min(d + step, len);
                Location spot = from.clone().add(dir.clone().multiply(d));
                var dustX = new Particle.DustTransition(
                    Color.fromRGB(0xffd54f),
                    Color.fromRGB(0x0fa6ec),
                    1.0f
                );
                w.spawnParticle(Particle.DUST_COLOR_TRANSITION, spot, 1, 0, 0, 0, 0, dustX);
                if (d >= len) cancel();
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        lv4Players.remove(event.getPlayer().getUniqueId());
        fxCoolDown.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onArmorEquip(PlayerArmorChangeEvent event) {
        handleEquip(event, defaultPattern);
        UUID id = event.getPlayer().getUniqueId();
        int instanceCount = getTrimCount(id, defaultPattern);

        if (instanceCount < 4) {
            lv4Players.remove(id);
            fxCoolDown.remove(id);
        }
    }

    @Override
    public void run() {
        fxCounter.set(0);
    }
}
