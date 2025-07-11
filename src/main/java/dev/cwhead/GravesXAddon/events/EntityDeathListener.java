package dev.cwhead.GravesXAddon.events;

import com.ranull.graves.event.*;
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

public class EntityDeathListener implements Listener {

    private final Graveyards plugin;

    public EntityDeathListener(Graveyards plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveCreate(GraveCreateEvent event) {
        Player player = (Player) event.getEntity();
        Location deathLocation = player != null ? player.getLocation() : null;
        String graveyardName = getAnyAvailableGraveyard();

        if (graveyardName != null) {
            event.setAddon(true);
            plugin.getGravesX().debugMessage("Grave created in graveyard " + graveyardName, 2);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveAutoLooted(GraveAutoLootEvent event) {
        Location lootedLocation = event.getLocation();
        String graveyardName = getAnyAvailableGraveyard();

        if (graveyardName != null) {
            GraveSite lootedGraveSite = plugin.getCacheManager().getGraveSiteByLocation(graveyardName, lootedLocation);
            if (lootedGraveSite != null) {
                plugin.getCacheManager().updateGraveSiteOccupancy(graveyardName, lootedLocation, false);
                lootedGraveSite.setOccupied(false);
                plugin.getGravesX().debugMessage("Grave auto-looted in graveyard " + graveyardName, 2);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveLooted(GraveLootedEvent event) {
        Location lootedLocation = event.getLocation();
        String graveyardName = getAnyAvailableGraveyard();

        if (graveyardName != null) {
            GraveSite lootedGraveSite = plugin.getCacheManager().getGraveSiteByLocation(graveyardName, lootedLocation);
            if (lootedGraveSite != null) {
                plugin.getCacheManager().updateGraveSiteOccupancy(graveyardName, lootedLocation, false);
                lootedGraveSite.setOccupied(false);
                plugin.getGravesX().debugMessage("Grave looted in graveyard " + graveyardName, 2);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveWalkedOver(GraveWalkOverEvent event) {
        Location lootedLocation = event.getLocation();
        String graveyardName = getAnyAvailableGraveyard();

        if (graveyardName != null) {
            GraveSite lootedGraveSite = plugin.getCacheManager().getGraveSiteByLocation(graveyardName, lootedLocation);
            if (lootedGraveSite != null) {
                plugin.getCacheManager().updateGraveSiteOccupancy(graveyardName, lootedLocation, false);
                lootedGraveSite.setOccupied(false);
                plugin.getGravesX().debugMessage("Grave looted via walkover in graveyard " + graveyardName, 2);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveProjectile(GraveProjectileHitEvent event) {
        Location lootedLocation = event.getLocation();
        String graveyardName = getAnyAvailableGraveyard();

        if (graveyardName != null) {
            GraveSite lootedGraveSite = plugin.getCacheManager().getGraveSiteByLocation(graveyardName, lootedLocation);
            if (lootedGraveSite != null) {
                plugin.getCacheManager().updateGraveSiteOccupancy(graveyardName, lootedLocation, false);
                lootedGraveSite.setOccupied(false);
                plugin.getGravesX().debugMessage("Grave projectile hit in graveyard " + graveyardName, 2);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraveExploded(GraveExplodeEvent event) {
        Location lootedLocation = event.getGrave().getLocationDeath();
        String graveyardName = getAnyAvailableGraveyard();

        if (graveyardName != null) {
            GraveSite lootedGraveSite = plugin.getCacheManager().getGraveSiteByLocation(graveyardName, lootedLocation);
            if (lootedGraveSite != null) {
                plugin.getCacheManager().updateGraveSiteOccupancy(graveyardName, lootedLocation, false);
                lootedGraveSite.setOccupied(false);
                plugin.getGravesX().debugMessage("Grave exploded in graveyard " + graveyardName, 2);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            String graveyardName = getAnyAvailableGraveyard();

            if (graveyardName != null) {
                List<GraveSite> graveSites = plugin.getCacheManager().getGraveSites(graveyardName);
                GraveSite selectedGraveSite = null;

                List<GraveSite> availableGraveSites = graveSites.stream()
                        .filter(graveSite -> !graveSite.isOccupied())
                        .collect(Collectors.toList());

                if (!availableGraveSites.isEmpty()) {
                    selectedGraveSite = availableGraveSites.get(new Random().nextInt(availableGraveSites.size()));
                } else {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "No available grave sites found in the graveyard " + ChatColor.GOLD + graveyardName);
                    plugin.getGravesX().debugMessage("No available grave sites for player " + player.getName(), 2);
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
                    plugin.getGravesX().debugMessage("Grave created for player " + player.getName() + " at " + selectedGraveSite.getLocation(), 2);
                    event.getDrops().clear();
                } else {
                    for (ItemStack item : event.getDrops()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    }
                    event.getDrops().clear();
                }
            }
        }
    }

    /**
     * Selects any graveyard that has unoccupied grave sites.
     *
     * @return The name of an available graveyard, or null if none found.
     */
    private String getAnyAvailableGraveyard() {
        for (Map.Entry<String, List<GraveSite>> entry : plugin.getCacheManager().getAllGraveyards().entrySet()) {
            boolean hasAvailable = entry.getValue().stream().anyMatch(site -> !site.isOccupied());
            if (hasAvailable) return entry.getKey();
        }
        return null;
    }
}
