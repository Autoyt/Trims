package dev.auto.trims.world;

import org.bukkit.Location;
import org.bukkit.generator.structure.Structure;

public record WorldObjective(Structure type, Location spawn, Location objective, Location exit) {}
