package dev.auto.trims.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class EffectManager {
    private static final Map<UUID, Set<PotionEffectType>> DESIRED = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<PotionEffectType, PotionEffect>> OWNED = new ConcurrentHashMap<>();

    private EffectManager() {}

    public static void beginTick() {
        DESIRED.clear();
    }

    public static void wantEffect(UUID id, PotionEffect want) {
        if (id == null || want == null) return;
        Player p = Bukkit.getPlayer(id);
        if (p == null) return;

        PotionEffectType t = want.getType();
        DESIRED.computeIfAbsent(id, k -> new HashSet<>()).add(t);

        PotionEffect live = p.getPotionEffect(t);
        Map<PotionEffectType, PotionEffect> mine = OWNED.computeIfAbsent(id, k -> new HashMap<>());
        PotionEffect prev = mine.get(t);

        boolean flagsDiff = live != null && (live.isAmbient() != want.isAmbient()
                || live.hasParticles() != want.hasParticles()
                || live.hasIcon() != want.hasIcon());
        int la = live == null ? -1 : live.getAmplifier();
        int wa = want.getAmplifier();
        int ld = live == null ? 0 : live.getDuration();
        int wd = want.getDuration();

        boolean needs = live == null || la != wa || flagsDiff || ld < wd - 2;

        if (!needs) {
            mine.put(t, snap(live));
            return;
        }

        if (la > wa) {
            boolean weOwnLive = prev != null && eqShallow(live, prev);
            if (!weOwnLive) return;
            p.removePotionEffect(t);
            p.addPotionEffect(want);
        } else {
            p.addPotionEffect(want);
        }

        PotionEffect now = p.getPotionEffect(t);
        if (now != null) mine.put(t, snap(now)); else mine.remove(t);
    }

    public static void endTick() {
        Set<UUID> ids = new HashSet<>(OWNED.keySet());
        ids.addAll(DESIRED.keySet());

        for (UUID id : ids) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;

            Map<PotionEffectType, PotionEffect> mine = OWNED.getOrDefault(id, Collections.emptyMap());
            Set<PotionEffectType> wantTypes = DESIRED.getOrDefault(id, Collections.emptySet());

            Iterator<Map.Entry<PotionEffectType, PotionEffect>> it = mine.entrySet().iterator();
            while (it.hasNext()) {
                var e = it.next();
                PotionEffectType t = e.getKey();
                if (wantTypes.contains(t)) continue;

                PotionEffect live = p.getPotionEffect(t);
                PotionEffect prev = e.getValue();
                boolean unchanged = live != null && eqShallow(live, prev);

                if (unchanged) {
                    p.removePotionEffect(t);
                    it.remove();
                } else {
                    it.remove();
                }
            }

            if (OWNED.getOrDefault(id, Collections.emptyMap()).isEmpty()) OWNED.remove(id);
        }

        DESIRED.clear();
    }

    private static PotionEffect snap(PotionEffect e) {
        return new PotionEffect(e.getType(), e.getDuration(), e.getAmplifier(), e.isAmbient(), e.hasParticles(), e.hasIcon());
    }

    private static boolean eqShallow(PotionEffect a, PotionEffect b) {
        return a.getType().equals(b.getType())
                && a.getAmplifier() == b.getAmplifier()
                && a.isAmbient() == b.isAmbient()
                && a.hasParticles() == b.hasParticles()
                && a.hasIcon() == b.hasIcon();
    }
}
