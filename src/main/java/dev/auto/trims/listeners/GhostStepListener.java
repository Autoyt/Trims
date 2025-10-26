package dev.auto.trims.listeners;

import dev.auto.trims.Main;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;
import dev.auto.trims.particles.GhostStepFX;

import java.util.UUID;

public class GhostStepListener implements Listener {

    public double MIN_FALLING_VEL = -0.08;
    public double UP_BOOST_Y = 0.9;
    public double HORIZ_KEEP = 1.0;
    public boolean TOGGLE = true;

    public GhostStepListener() {
        // Setup
        for (Player p : Bukkit.getOnlinePlayers()) {
            this.armIfGrounded(p);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!TOGGLE) return;
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        if (!player.isOnGround()) return;

        armIfGrounded(player);
    }

    public void armIfGrounded(Player p) {
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        p.setAllowFlight(true);
        p.setFlying(false);
    }

    // Add no horizontal speed limiters, Make you have a power bar and moving gives more energy + base regen rate

    @EventHandler
    public void onGhostStep(PlayerToggleFlightEvent event) {
        if (!TOGGLE) return;
        final Player p = event.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        if (p.isGliding()) return;

        event.setCancelled(true);
        p.setFlying(false);
        if (p.isOnGround()) return;

        p.setAllowFlight(false);
        p.setFallDistance(0f);

        final Vector v  = p.getVelocity();
        final double vy = v.getY();
        final double fallSpeed = Math.max(0.0, -vy);

        // --- UPWARD CONFIG ---
        final double baseUp   = 0.22;
        final double upScale  = 0.50;
        final double ascendUp = 0.25; // small boost even if rising

        final double up = baseUp + upScale * fallSpeed + (vy >= 0 ? ascendUp : 0.0);

        // --- HORIZONTAL CONFIG ---
        double minMult    = 1.20;   // base carry (fast fall → ~this)
        double maxMult    = 1.55;   // slow fall → up to this carry
        double k          = 0.65;   // decay rate (smaller = keeps more speed)

        double baseLook   = 0.25;   // always add this much forward bias
        double maxLook    = 0.45;   // extra bias when falling slowly (decays with speed)
        double lookK      = 0.65;   // decay rate for look bias

        double speedFloor = 0.60;   // guarantee at least this horizontal speed
        double speedCap   = 1.50;   // hard cap (anticheat-friendly)

        // --- CARRY & LOOK ---
        double carryMult = minMult + (maxMult - minMult) * Math.exp(-k * fallSpeed);

        Vector horiz = new Vector(v.getX(), 0, v.getZ()).multiply(carryMult);

        // look influence = base + extra that decays with fall speed
        double lookBias = baseLook + (maxLook - baseLook) * Math.exp(-lookK * fallSpeed);
        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        horiz.add(dir.multiply(lookBias));

        // enforce floor/cap
        double len = horiz.length();
        if (len < speedFloor && len > 1e-6) horiz.multiply(speedFloor / len);
        else if (len > speedCap)            horiz.multiply(speedCap / len);

        // set final velocity
        p.setVelocity(new Vector(horiz.getX(), up, horiz.getZ()));

        GhostStepFX fx = new GhostStepFX();
        fx.run(Main.getInstance(), p);
    }

}
