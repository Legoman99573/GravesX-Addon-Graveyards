package dev.cwhead.GravesXAddon.managers;

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

public class GraveyardHologramManager {

    private final Plugin plugin;
    private final NamespacedKey hologramKey;

    public GraveyardHologramManager(Plugin plugin) {
        this.plugin = plugin;
        this.hologramKey = new NamespacedKey(plugin, "GraveyardHologram");
    }

    public void createHologram(Location base, String playerName) {
        List<String> lines = Arrays.asList(
                ChatColor.GRAY + "Here lies",
                ChatColor.GOLD + playerName
        );

        Collections.reverse(lines);
        Location current = base.clone().add(0.5, 2.25, 0.5);

        for (String line : lines) {
            ArmorStand stand = (ArmorStand) base.getWorld().spawnEntity(current, EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setCustomNameVisible(true);
            stand.setCustomName(line);
            stand.setMarker(true);
            stand.setSmall(true);
            stand.setSilent(true);

            // Mark with persistent data so we can identify/remove it later
            stand.getPersistentDataContainer().set(hologramKey, PersistentDataType.BYTE, (byte) 1);

            current.add(0, 0.25, 0);
        }
    }
}