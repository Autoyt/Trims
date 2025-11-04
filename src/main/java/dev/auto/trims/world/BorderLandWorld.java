package dev.auto.trims.world;

import dev.auto.trims.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.generator.structure.Structure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BorderLand {
    private final Map<Structure, WorldObjective> worldObjectives = new HashMap<>();
    private ArmorStand waypoint;
    private final UUID worldID;
    private final World world;

    private static final List<Structure> structures = List.of(
            Structure.DESERT_PYRAMID,
            Structure.JUNGLE_PYRAMID,
            Structure.ANCIENT_CITY,
            Structure.TRIAL_CHAMBERS,
            Structure.TRAIL_RUINS,
            Structure.STRONGHOLD,
            Structure.MANSION,
            Structure.MONUMENT,
            Structure.PILLAGER_OUTPOST,
            Structure.SHIPWRECK
    );

    public BorderLand(UUID worldID) {
        this.worldID = worldID;

        try {
            world = Bukkit.getWorld(worldID);
        }
        catch (Exception e) {
            throw new IllegalStateException("World not found");
        }
    }

    private void load() {

    }


    public void createWayPoint(WorldObjective objective, Integer index) {
        if (index > 1 || index < 0) throw new IllegalArgumentException("Index must be between 0 and 1");

        Location loc = switch (index) {
            case 0 -> objective.objective();
            case 1 -> objective.exit();
            default -> throw new IllegalArgumentException("Index must be between 0 and 1");
        };

        UUID id = UUID.randomUUID();
        ArmorStand stand = loc.getWorld().spawn(loc, ArmorStand.class);
        stand.setInvisible(true);
        stand.setInvulnerable(true);
        stand.setMarker(true);
        stand.setCanMove(false);
        stand.addScoreboardTag(id.toString());
        AttributeInstance waypointRange = stand.getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE);
        if (waypointRange != null) waypointRange.setBaseValue(600000);

        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "waypoint modify @e[type=armor_stand,tag=%id%,limit=1] color hex 43E866".replace("%id%", id.toString()));
        }, 5);

        if (waypoint != null) waypoint.remove();
        waypoint = stand;
    }
}
