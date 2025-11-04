package dev.auto.trims.world;

import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@SerializableAs("WorldObjective")
public record WorldObjective(
        Integer type,      // your structure id
        Location spawn,
        Location objective,
        Location exit
) implements ConfigurationSerializable {

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("spawn", spawn);
        map.put("objective", objective);
        map.put("exit", exit);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static WorldObjective deserialize(Map<String, Object> map) {
        Object rawType = map.get("type");
        if (rawType == null) {
            throw new IllegalStateException("Missing 'type' in WorldObjective config");
        }

        Integer type;
        if (rawType instanceof Number n) {
            type = n.intValue();
        } else {
            type = Integer.valueOf(rawType.toString());
        }

        Location spawn = (Location) map.get("spawn");
        Location objective = (Location) map.get("objective");
        Location exit = (Location) map.get("exit");

        return new WorldObjective(type, spawn, objective, exit);
    }
}
