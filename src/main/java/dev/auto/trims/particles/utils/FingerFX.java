package dev.auto.trims.particles.utils;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Particle;

public class FingerFX {
    @Setter
    public Particle particle;
    @Setter
    public int points, octaves, ticksToCompletion;

    @Setter
    public boolean avoidBlocks;
    @Getter
    public final Location from;
    @Getter
    public final Location to;


    public FingerFX(Location from, Location to) {
        this.from = from;
        this.to = to;
    }

    public void run() {

    }
}
