package dev.cwhead.GravesXAddon.graveyards.managers;

import dev.cwhead.GravesXAddon.graveyards.Graveyards;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.persistence.PersistentDataType;

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
            plugin.getGravesX().debugMessage("Invalid base location for hologram creation.", 1);
            return;
        }

        List<String> lines = Arrays.asList(
                ChatColor.GRAY + "Here lies",
                ChatColor.GOLD + playerName
        );

        Collections.reverse(lines);
        Location current = base.clone().add(0, 0.25, 0);

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

            stand.addScoreboardTag("graveyardHologram");
            stand.addScoreboardTag("graveyardHologramLocation" + base.getWorld() + base.getBlockX() + base.getBlockY() + base.getBlockZ());

            plugin.getGravesX().debugMessage("Spawned ArmorStand line '" + ChatColor.stripColor(line) +
                    "' at " + formatLoc(current), 1);

            plugin.getGravesX().debugMessage("Spawned hologram line: '" + ChatColor.stripColor(line) + "' for player " +
                    playerName + " at " + formatLoc(current), 2);

            current.add(0, 0.25, 0);
        }

        plugin.getGravesX().debugMessage("Created hologram for player '" + playerName +
                "' at base " + formatLoc(base), 1);

    }

    /**
     * Removes all holograms associated with a specific graveyard location.
     *
     * @param base the base block location of the grave site
     */
    public void removeHologram(Location base) {
        if (base == null || base.getWorld() == null) {
            plugin.getGravesX().debugMessage("Invalid base location for hologram removal.", 2);
            return;
        }

        String tag = "graveyardHologramLocation" +
                base.getWorld().getName() +
                base.getBlockX() + base.getBlockY() + base.getBlockZ();

        plugin.getGravesX().debugMessage("Attempting to remove holograms for tag '" + tag +
                "' at " + formatLoc(base), 1);

        int removed = 0;
        for (Entity entity : base.getWorld().getEntitiesByClass(ArmorStand.class)) {
            if (entity.getScoreboardTags().contains(tag)) {
                plugin.getGravesX().debugMessage("Found hologram ArmorStand '" +
                        ChatColor.stripColor(entity.getCustomName() != null ? entity.getCustomName() : "Unnamed") +
                        "' at " + formatLoc(entity.getLocation()) + " — removing.", 1);

                entity.remove();
                removed++;
            }
        }

        if (removed == 0) {
            plugin.getGravesX().debugMessage("No hologram entities found for tag '" + tag + "'.", 2);
        } else {
            plugin.getGravesX().debugMessage("Removed " + removed + " hologram entities for tag '" +
                    tag + "' at " + formatLoc(base), 1);
        }
    }

    private static String formatLoc(Location loc) {
        return String.format("[x=%d, y=%d, z=%d, world=%s]",
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                loc.getWorld() != null ? loc.getWorld().getName() : "null");
    }
}