package dev.cwhead.GravesXAddon.graveyards.managers;

import dev.cwhead.GravesXAddon.graveyards.Graveyards;
import dev.cwhead.GravesXAddon.graveyards.util.ConfigUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Manages creation and debugging of holograms used for graveyard headstones.
 * Each hologram is composed of one or more invisible ArmorStands stacked vertically.
 */
public class GraveyardHologramManager {

    private final Graveyards plugin;
    private final NamespacedKey hologramKey;
    private final ConfigUtil config;

    public GraveyardHologramManager(Graveyards plugin) {
        this.plugin = plugin;
        this.hologramKey = new NamespacedKey(plugin, "GraveyardHologram");
        this.config = plugin.getConfigUtil();
    }

    /**
     * Creates a hologram above a grave site displaying the given player's name.
     *
     * @param base       the base block location of the grave site
     * @param playerName the player's name to display
     * @param killer     the player's killer to display (may be null)
     * @param entityType the player's entity type (not currently used in placeholders)
     */
    public void createHologram(Location base, String playerName, Entity killer, EntityType entityType) {
        if (base == null || base.getWorld() == null) {
            plugin.getGravesX().debugMessage("Invalid base location for hologram creation.", 1);
            return;
        }

        removeHologram(base);

        Vector baseOffset = config.getHologramBaseOffset();
        Vector stepOffset = config.getHologramNewLineOffset();
        List<String> templateLines = config.getHologramLines();

        List<String> lines = new ArrayList<>(templateLines.size());
        for (String line : templateLines) {
            lines.add(resolvePlaceholders(line, playerName, killer, entityType));
        }

        Collections.reverse(lines);

        Location current = base.clone().add(baseOffset);

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
            stand.addScoreboardTag("graveyardHologramLocation" +
                    base.getWorld().getName() + base.getBlockX() + base.getBlockY() + base.getBlockZ());

            plugin.getGravesX().debugMessage("Spawned ArmorStand line '" + ChatColor.stripColor(line) +
                    "' at " + formatLoc(current), 1);
            plugin.getGravesX().debugMessage("Spawned hologram line: '" + ChatColor.stripColor(line) + "' for player " +
                    playerName + " at " + formatLoc(current), 2);

            current.add(stepOffset);
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
        for (ArmorStand entity : base.getWorld().getEntitiesByClass(ArmorStand.class)) {
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

    private String resolvePlaceholders(String line, String playerName, Entity killer, EntityType entityType) {
        String killerName = "Unknown";
        if (killer instanceof Player) {
            killerName = killer.getName();
        } else if (killer != null) {
            killerName = entityType.name().toLowerCase(Locale.ROOT);
        }
        return line
                .replace("%player%", playerName != null ? playerName : "Unknown")
                .replace("%killer%", killerName);
    }

    private static String formatLoc(Location loc) {
        return String.format("[x=%d, y=%d, z=%d, world=%s]",
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                loc.getWorld() != null ? loc.getWorld().getName() : "null");
    }
}