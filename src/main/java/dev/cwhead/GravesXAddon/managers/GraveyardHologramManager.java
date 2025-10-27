package dev.cwhead.GravesXAddon.managers;

import dev.cwhead.GravesXAddon.Graveyards;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Manages creation and debugging of holograms used for graveyard headstones.
 * Each hologram is composed of one or more invisible ArmorStands stacked vertically.
 */
public class GraveyardHologramManager {

    private final Graveyards plugin;
    private final NamespacedKey hologramKey;

    public GraveyardHologramManager(Graveyards plugin) {
        this.plugin = plugin;
        this.hologramKey = new NamespacedKey(plugin, "GraveyardHologram");
    }

    /**
     * Creates a hologram above a grave site displaying the given player's name.
     *
     * @param base the base block location of the grave site
     * @param playerName the player's name to display
     */
    public void createHologram(Location base, String playerName) {
        if (base == null || base.getWorld() == null) {
            plugin.getLogger().warning("[GraveyardHologram] Invalid base location for hologram creation.");
            return;
        }

        List<String> lines = Arrays.asList(
                ChatColor.GRAY + "Here lies",
                ChatColor.GOLD + playerName
        );

        Collections.reverse(lines);
        Location current = base.clone().add(0.5, 2.25, 0.5);

        plugin.getGravesX().debugMessage("Creating hologram for '" + playerName +
                "' at " + formatLoc(base), 1);

        for (String line : lines) {
            ArmorStand stand = (ArmorStand) base.getWorld().spawnEntity(current, EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setCustomNameVisible(true);
            stand.setCustomName(line);
            stand.setMarker(true);
            stand.setSmall(true);
            stand.setSilent(true);

            stand.getPersistentDataContainer().set(hologramKey, PersistentDataType.BYTE, (byte) 1);

            plugin.getGravesX().debugMessage("Spawned ArmorStand line '" + ChatColor.stripColor(line) +
                    "' at " + formatLoc(current), 1);

            plugin.getGravesX().debugMessage("Spawned hologram line: '" + ChatColor.stripColor(line) + "' for player " +
                    playerName + " at " + formatLoc(current), 2);

            current.add(0, 0.25, 0);
        }

        plugin.getGravesX().debugMessage("Created hologram for player '" + playerName +
                "' at base " + formatLoc(base), 1);

    }

    private static String formatLoc(Location loc) {
        return String.format("[x=%d, y=%d, z=%d, world=%s]",
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                loc.getWorld() != null ? loc.getWorld().getName() : "null");
    }
}