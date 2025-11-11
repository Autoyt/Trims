package dev.auto.trims.world;

import dev.auto.trims.Main;
import dev.auto.trims.customEvents.NewBorderlandGenerationEvent;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.generator.structure.Structure;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StructureSearchResult;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

public class WorldGenerator {
    public static final List<Structure> structures = List.of(
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

    @Getter
    public final UUID worldID = UUID.randomUUID();
    private World world;
    private Location origin;
    private final ConfigurationSection worldConfig = Main.getInstance().getConfig().getConfigurationSection("trims.world-options");
    private long startTime;
    @Getter
    private long endTime;

    private final double spawnMin;
    private final double spawnMax;
    private final double exitMin;
    private final double exitMax;

    private int bufferChunks;
    private int chunksPerTick;
    public WorldGenerator() {
        if (worldConfig == null) {
            throw new IllegalStateException("World config is null");
        }

        ConfigurationSection spawnToObjective = worldConfig.getConfigurationSection("s-o");
        if (spawnToObjective == null) {
            throw new IllegalStateException("Spawn to objective config is null");
        }

        ConfigurationSection objectiveToExtraction = worldConfig.getConfigurationSection("o-e");
        if (objectiveToExtraction == null) {
            throw new IllegalStateException("Objective to extraction config is null");
        }

        ConfigurationSection generation = worldConfig.getConfigurationSection("generation");
        if (generation == null) {
            throw new IllegalStateException("Generation config is null");
        }

        spawnMin = spawnToObjective.getDouble("min");
        spawnMax = spawnToObjective.getDouble("max");
        exitMin = objectiveToExtraction.getDouble("min");
        exitMax = objectiveToExtraction.getDouble("max");

        bufferChunks = generation.getInt("buffer-chunks");
        chunksPerTick = generation.getInt("cpt");

        generate();
    }

    public void generate() {
        startTime = System.currentTimeMillis();
        WorldCreator creator = new WorldCreator(worldID.toString());
        creator.environment(World.Environment.NORMAL);
        creator.seed(UUID.randomUUID().getLeastSignificantBits());

        world = creator.createWorld();
        if (world == null) {
            throw new IllegalStateException("Failed to create world");
        }

        origin = world.getSpawnLocation();
        Map<Structure, WorldObjective> worldObjectives = new HashMap<>();
        List<CompletableFuture<Void>> preloadFutures = new ArrayList<>();

        Iterator<Structure> iterator = structures.iterator();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!iterator.hasNext()) {
                    CompletableFuture<Void> all = CompletableFuture.allOf(preloadFutures.toArray(new CompletableFuture[0]));
                    all.whenComplete((v, e) -> {
                        if (e != null) {
                            Main.getInstance().getLogger().warning("Chunk preloading encountered an error for world " + worldID + ": " + e.getMessage());
                        }
                        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                            flushFile(worldObjectives);
                            endTime = System.currentTimeMillis() - startTime;

                            Bukkit.unloadWorld(world, true);
                            world = null;
                            Main.getInstance().getServer().getPluginManager().callEvent(new NewBorderlandGenerationEvent(worldID, endTime));
                        });
                    });
                    cancel();
                    return;
                }

                Structure structure = iterator.next();
                Location loc = locateStructure(structure, Optional.empty());
                if (loc == null) {
                    Main.getInstance().getLogger().warning("Cant find structure " + structure + " in world " + worldID);
                }
                else {
                    WorldObjective objective = generateWorldObjective(structure, loc);
                    CompletableFuture<Void> f = preloadObjectives(world, objective);
                    preloadFutures.add(f);
                    worldObjectives.put(structure, objective);
                }
            }
        }.runTaskTimer(Main.getInstance(), 0, 2);
    }

    private void flushFile(Map<Structure, WorldObjective> worldObjectives) {
        File datafile = new File(Main.getInstance().getDataFolder(), "data/%world%/world-objectives.yml".replace("%world%", worldID.toString()));
        File parent = datafile.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                Main.getInstance().getLogger().warning("Failed to create directories for objectives file: " + parent.getAbsolutePath());
            }
        }

        YamlConfiguration dataConfig = YamlConfiguration.loadConfiguration(datafile);

        for (Map.Entry<Structure, WorldObjective> entry : worldObjectives.entrySet()) {
            Structure struc = entry.getKey();
            WorldObjective objective = entry.getValue();

            String idKey = WorldManager.getIdFromStructure(struc).toString();
            dataConfig.set(idKey, objective);
        }

        try {
            dataConfig.save(datafile);
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("Failed to save world data for world %worldid%: ".replace("%worldid%", worldID.toString()) + e.getMessage());
        }
    }

    private CompletableFuture<Void> preloadObjectives(World world, WorldObjective objective) {
        Location spawnLoc = objective.spawn();
        Location objectiveLoc = objective.objective();
        Location exitLoc = objective.exit();

        List<int[]> chunks = new ArrayList<>();

        List<int[]> finalChunks = chunks;
        BiConsumer<Location, Location> collect = (a, b) -> {
            int minX = Math.min(a.getBlockX(), b.getBlockX());
            int maxX = Math.max(a.getBlockX(), b.getBlockX());
            int minZ = Math.min(a.getBlockZ(), b.getBlockZ());
            int maxZ = Math.max(a.getBlockZ(), b.getBlockZ());

            int minChunkX = (minX >> 4) - bufferChunks;
            int maxChunkX = (maxX >> 4) + bufferChunks;
            int minChunkZ = (minZ >> 4) - bufferChunks;
            int maxChunkZ = (maxZ >> 4) + bufferChunks;

            for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                    finalChunks.add(new int[]{cx, cz});
                }
            }
        };

        collect.accept(spawnLoc, objectiveLoc);
        collect.accept(objectiveLoc, exitLoc);
        collect.accept(spawnLoc, exitLoc);

        Set<Long> seen = new HashSet<>();
        List<int[]> unique = new ArrayList<>();
        for (int[] c : chunks) {
            long key = (((long) c[0]) << 32) ^ (c[1] & 0xffffffffL);
            if (seen.add(key)) {
                unique.add(c);
            }
        }
        chunks = unique;

        int totalChunks = chunks.size();
        Main.getInstance().getLogger().info("Preloading " + totalChunks + " chunks for objective " + objective.type());

        CompletableFuture<Void> future = new CompletableFuture<>();
        Iterator<int[]> it = chunks.iterator();
        final int[] loaded = {0};

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!it.hasNext()) {
                    Main.getInstance().getLogger().info("Finished preloading chunks (" + loaded[0] + "/" + totalChunks + ")");
                    future.complete(null);
                    cancel();
                    return;
                }

                for (int i = 0; i < chunksPerTick && it.hasNext(); i++) {
                    int[] c = it.next();
                    try {
                        Chunk chunk = world.getChunkAt(c[0], c[1]);
                        if (!chunk.isLoaded()) {
                            chunk.load(true);
                        }
                        loaded[0]++;
                    }
                    catch (Throwable t) {
                        if (!future.isDone()) future.completeExceptionally(t);
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(Main.getInstance(), 1L, 1L);

        return future;
    }

    private Location locateStructure(Structure structure, Optional<Integer> distance) {
        int d = distance.orElse(1000);
        while (d < 20000) {
            StructureSearchResult result = world.locateNearestStructure(origin, structure, d, false);
            if (result != null) {
                return result.getLocation();
            }
            d *= 2;
        }
        Main.getInstance().getLogger().warning("Cant find structure " + structure + " within max distance in world " + worldID);
        return null;
    }

    private WorldObjective generateWorldObjective(Structure structure, Location structLoc) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        int baseX = structLoc.getBlockX();
        int baseZ = structLoc.getBlockZ();

        Block highestObjective = world.getHighestBlockAt(baseX, baseZ);
        Location objective = highestObjective.getLocation().add(0.5, 1, 0.5);

        double angleSpawn = rnd.nextDouble(0, Math.PI * 2);
        double radiusSpawn = rnd.nextDouble(spawnMin, spawnMax);
        double spawnX = objective.getX() + Math.cos(angleSpawn) * radiusSpawn;
        double spawnZ = objective.getZ() + Math.sin(angleSpawn) * radiusSpawn;

        int spawnBX = (int) Math.floor(spawnX);
        int spawnBZ = (int) Math.floor(spawnZ);
        Block highestSpawn = world.getHighestBlockAt(spawnBX, spawnBZ);
        Location spawn = highestSpawn.getLocation().add(0.5, 1, 0.5);

        double angleExit = rnd.nextDouble(0, Math.PI * 2);
        double radiusExit = rnd.nextDouble(exitMin, exitMax);
        double exitX = objective.getX() + Math.cos(angleExit) * radiusExit;
        double exitZ = objective.getZ() + Math.sin(angleExit) * radiusExit;

        int exitBX = (int) Math.floor(exitX);
        int exitBZ = (int) Math.floor(exitZ);
        Block highestExit = world.getHighestBlockAt(exitBX, exitBZ);
        Location exit = highestExit.getLocation().add(0.5, 1, 0.5);

        return new WorldObjective(
                WorldManager.getIdFromStructure(structure),
                spawn,
                objective,
                exit
        );
    }

}
