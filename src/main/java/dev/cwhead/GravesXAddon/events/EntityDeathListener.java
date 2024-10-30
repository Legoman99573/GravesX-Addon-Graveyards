package dev.cwhead.GravesXAddon.events;

import com.ranull.graves.event.GraveAutoLootEvent;
import com.ranull.graves.event.GraveCreateEvent;
import com.ranull.graves.event.GraveExplodeEvent;
import com.ranull.graves.event.GraveLootedEvent;
import dev.cwhead.GravesXAddon.Graveyards;
import dev.cwhead.GravesXAddon.util.GraveSite;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Listens for entity death events and handles the creation, looting,
 * and management of graves in the game.
 */
public class EntityDeathListener implements Listener {

    private final Graveyards plugin;

    /**
     * Constructs an EntityDeathListener for the given Graveyards plugin instance.
     *
     * @param plugin The Graveyards plugin instance.
     */
    public EntityDeathListener(Graveyards plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the event when a grave is created.
     * Sets the addon flag to true if the grave is created in a graveyard.
     *
     * @param event The GraveCreateEvent.
     */
    @EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveCreate(GraveCreateEvent event) {
        Player player = (Player) event.getEntity();
        Location deathLocation = player != null ? player.getLocation() : null;
        String graveyardName = getGraveyardNameAtLocation(deathLocation);

        if (graveyardName != null) {
            event.setAddon(true);
            plugin.getGravesX().debugMessage("Grave created at " + deathLocation + " in graveyard " + graveyardName, 2);
        }
    }

    /**
     * Handles the event when a grave is auto-looted.
     * Updates the grave site's occupancy status.
     *
     * @param event The GraveAutoLootEvent.
     */
    @EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveAutoLooted(GraveAutoLootEvent event) {
        Location lootedLocation = event.getLocation();
        String graveyardName = getGraveyardNameAtLocation(lootedLocation);

        if (graveyardName != null) {
            GraveSite lootedGraveSite = plugin.getCacheManager().getGraveSiteByLocation(graveyardName, lootedLocation);

            if (lootedGraveSite != null) {
                plugin.getCacheManager().updateGraveSiteOccupancy(graveyardName, lootedLocation, false);
                lootedGraveSite.setOccupied(false);
                plugin.getGravesX().debugMessage("Grave auto-looted at " + lootedLocation + " in graveyard " + graveyardName, 2);
            }
        }
    }

    /**
     * Handles the event when a grave is looted.
     * Updates the grave site's occupancy status.
     *
     * @param event The GraveLootedEvent.
     */
    @EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveLooted(GraveLootedEvent event) {
        Location lootedLocation = event.getLocation();
        String graveyardName = getGraveyardNameAtLocation(lootedLocation);

        if (graveyardName != null) {
            GraveSite lootedGraveSite = plugin.getCacheManager().getGraveSiteByLocation(graveyardName, lootedLocation);

            if (lootedGraveSite != null) {
                plugin.getCacheManager().updateGraveSiteOccupancy(graveyardName, lootedLocation, false);
                lootedGraveSite.setOccupied(false);
                plugin.getGravesX().debugMessage("Grave looted at " + lootedLocation + " in graveyard " + graveyardName, 2);
            }
        }
    }

    /**
     * Handles the event when a grave explodes.
     * Updates the grave site's occupancy status.
     *
     * @param event The GraveExplodeEvent.
     */
    @EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveExploded(GraveExplodeEvent event) {
        Location lootedLocation = event.getGrave().getLocationDeath();
        String graveyardName = getGraveyardNameAtLocation(lootedLocation);

        if (graveyardName != null) {
            GraveSite lootedGraveSite = plugin.getCacheManager().getGraveSiteByLocation(graveyardName, lootedLocation);

            if (lootedGraveSite != null) {
                plugin.getCacheManager().updateGraveSiteOccupancy(graveyardName, lootedLocation, false);
                lootedGraveSite.setOccupied(false);
                plugin.getGravesX().debugMessage("Grave exploded at " + lootedLocation + " in graveyard " + graveyardName, 2);
            }
        }
    }

    /**
     * Handles the event when an entity dies.
     * Creates a grave for the player if they die in a graveyard.
     *
     * @param event The EntityDeathEvent.
     */
    @EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Location deathLocation = player.getLocation();
            String graveyardName = getGraveyardNameAtLocation(deathLocation);

            if (graveyardName != null) {
                List<GraveSite> graveSites = plugin.getCacheManager().getGraveSites(graveyardName);
                GraveSite selectedGraveSite = null;

                List<GraveSite> availableGraveSites = graveSites.stream()
                        .filter(graveSite -> !graveSite.isOccupied())
                        .collect(Collectors.toList());

                if (!availableGraveSites.isEmpty()) {
                    Random random = new Random();
                    selectedGraveSite = availableGraveSites.get(random.nextInt(availableGraveSites.size()));
                } else {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "No available grave sites found in the graveyard " + ChatColor.GOLD + graveyardName);
                    plugin.getGravesX().debugMessage("No available grave sites for player " + player.getName() + " in graveyard " + graveyardName, 2);
                }

                if (selectedGraveSite != null) {
                    player.getInventory().clear();
                    Entity killer = player.getKiller();
                    EntityType killerEntityType = killer != null ? killer.getType() : null;
                    int experience = player.getTotalExperience();
                    long timeAliveRemaining = -1;
                    Map<EquipmentSlot, ItemStack> equipmentMap = new EnumMap<>(EquipmentSlot.class);
                    List<ItemStack> itemStackList = event.getDrops();
                    EntityDamageEvent.DamageCause damageCause = (player.getLastDamageCause() != null) ?
                            player.getLastDamageCause().getCause() : EntityDamageEvent.DamageCause.CUSTOM;
                    boolean graveProtection = true;
                    long graveProtectionTime = -1;
                    plugin.getCacheManager().updateGraveSiteOccupancy(graveyardName, selectedGraveSite.getLocation(), true);
                    selectedGraveSite.setOccupied(true);

                    plugin.getGravesXAPI().createGrave(player, killer, killerEntityType, selectedGraveSite.getLocation(), equipmentMap, itemStackList, experience, timeAliveRemaining, damageCause, graveProtection, graveProtectionTime);
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Your grave has been created in the graveyard " + ChatColor.GOLD + graveyardName);
                    plugin.getGravesX().debugMessage("Grave created for player " + player.getName() + " in graveyard " + graveyardName + " at location " + selectedGraveSite.getLocation(), 2);
                    event.getDrops().clear();
                } else {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "No available grave sites found in the graveyard " + ChatColor.GOLD + graveyardName);
                    plugin.getGravesX().debugMessage("Grave not created for player " + player.getName() + " in graveyard " + graveyardName + " at location " + event.getEntity().getLocation(), 2);
                    for (ItemStack item : event.getDrops()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    }
                    event.getDrops().clear();
                }
            }
        }
    }

    /**
     * Retrieves the name of the graveyard at the specified location.
     *
     * @param location The location to check.
     * @return The name of the graveyard, or null if no graveyard is found.
     */
    private String getGraveyardNameAtLocation(Location location) {
        for (Map.Entry<String, List<GraveSite>> entry : plugin.getCacheManager().getAllGraveyards().entrySet()) {
            String graveyardName = entry.getKey();
            for (GraveSite graveSite : entry.getValue()) {
                if (isLocationInGraveSite(location, graveSite.getLocation())) {
                    return graveyardName;
                }
            }
        }
        return null;
    }

    /**
     * Checks if the given location is within the bounds of the specified grave site.
     *
     * @param location          The location to check.
     * @param graveSiteLocation The location of the grave site.
     * @return true if the location is within the grave site, false otherwise.
     */
    private boolean isLocationInGraveSite(Location location, Location graveSiteLocation) {
        double radius = 5.0;
        return location.getWorld().equals(graveSiteLocation.getWorld())
                && location.distance(graveSiteLocation) <= radius;
    }
}