package dev.auto.trims.world;

import dev.auto.trims.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.bukkit.generator.structure.Structure;
import org.bukkit.util.StructureSearchResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class WorldManager implements Listener {
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


    private UUID createWorld() {
        UUID worldID = UUID.randomUUID();

        if (Bukkit.getWorld(worldID) != null) {
            throw new IllegalStateException("World already exists");
        }

        WorldCreator creator = new WorldCreator(worldID.toString());
        creator.environment(World.Environment.NORMAL);
        creator.seed(UUID.randomUUID().getLeastSignificantBits());

        ConfigurationSection config = Main.getInstance().getConfig().getConfigurationSection("trims.world-options");
        assert config != null;
        ConfigurationSection spawnToObjective = config.getConfigurationSection("s-o");
        assert spawnToObjective != null;
        ConfigurationSection objectiveToExtraction = config.getConfigurationSection("o-e");
        assert objectiveToExtraction != null;

        double spawnMin = spawnToObjective.getDouble("min");
        double spawnMax = spawnToObjective.getDouble("max");
        double exitMin = objectiveToExtraction.getDouble("min");
        double exitMax = objectiveToExtraction.getDouble("max");

        World world = creator.createWorld();
        assert world != null;

        Location origin = new Location(world, 0, world.getMaxHeight(), 0);

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Map<Structure, WorldObjective> worldObjectives = new HashMap<>();

        for (Structure structure : structures) {
            StructureSearchResult result = world.locateNearestStructure(origin, structure, 20000, false);
            if (result == null) {
                Main.getInstance().getLogger().warning("Cant find structure " + structure);
                continue;
            }

            Location resultLoc = result.getLocation();
            int baseX = resultLoc.getBlockX();
            int baseZ = resultLoc.getBlockZ();

            Location objective = null;
            for (int y = world.getMaxHeight() - 1; y >= world.getMinHeight(); y--) {
                var block = world.getBlockAt(baseX, y, baseZ);
                if (!block.getType().isAir()) {
                    objective = new Location(world, baseX + 0.5, y + 1, baseZ + 0.5);
                    break;
                }
            }
            if (objective == null) {
                objective = world.getSpawnLocation(); // fallback
            }

            double angleSpawn = rnd.nextDouble(0, Math.PI * 2);
            double radiusSpawn = rnd.nextDouble(spawnMin, spawnMax);

            double spawnX = objective.getX() + Math.cos(angleSpawn) * radiusSpawn;
            double spawnZ = objective.getZ() + Math.sin(angleSpawn) * radiusSpawn;

            Location spawn = null;
            int spawnBX = (int) Math.floor(spawnX);
            int spawnBZ = (int) Math.floor(spawnZ);

            for (int y = world.getMaxHeight() - 1; y >= world.getMinHeight(); y--) {
                var block = world.getBlockAt(spawnBX, y, spawnBZ);
                if (!block.getType().isAir()) {
                    spawn = new Location(world, spawnBX + 0.5, y + 1, spawnBZ + 0.5);
                    break;
                }
            }

            if (spawn == null) {
                spawn = objective.clone().add(0, 5, 0);
            }

            double angleExit = rnd.nextDouble(0, Math.PI * 2);
            double radiusExit = rnd.nextDouble(exitMin, exitMax);

            double exitX = objective.getX() + Math.cos(angleExit) * radiusExit;
            double exitZ = objective.getZ() + Math.sin(angleExit) * radiusExit;

            Location exit = null;
            int exitBX = (int) Math.floor(exitX);
            int exitBZ = (int) Math.floor(exitZ);

            for (int y = world.getMaxHeight() - 1; y >= world.getMinHeight(); y--) {
                var block = world.getBlockAt(exitBX, y, exitBZ);
                if (!block.getType().isAir()) {
                    exit = new Location(world, exitBX + 0.5, y + 1, exitBZ + 0.5);
                    break;
                }
            }
            if (exit == null) {
                exit = objective.clone().add(0, 5, 0);
            }

            WorldObjective worldObjective = new WorldObjective(
                    structure,
                    spawn,
                    objective,
                    exit
            );

            worldObjectives.put(structure, worldObjective);
        }

        // TODO save world objectives
        return worldID;
    }
}
