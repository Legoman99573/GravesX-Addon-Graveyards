package dev.cwhead.GravesXAddon.events;

import com.ranull.graves.event.GraveCreateEvent;
import dev.cwhead.GravesXAddon.Graveyards;
import dev.cwhead.GravesXAddon.managers.GraveyardHologramManager;
import dev.cwhead.GravesXAddon.util.GraveSite;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class EntityDeathListener implements Listener {

    private final Graveyards plugin;

    public EntityDeathListener(Graveyards plugin) {
        this.plugin = plugin;
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGraveCreate(GraveCreateEvent event) throws MalformedURLException {
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
                    Entity killer = player.getKiller();
                    EntityType killerEntityType = killer != null ? killer.getType() : null;

                    // Create decorative empty grave at symbolic graveyard site
                    Location holoLoc = selectedGraveSite.getLocation().clone().add(0.5, 2.25, 0.5);
                    new GraveyardHologramManager(plugin).createHologram(holoLoc, player.getName());
                    Block skullBlock = selectedGraveSite.getLocation().getBlock();
                    skullBlock.setType(Material.PLAYER_HEAD);

                    BlockState state = skullBlock.getState();
                    if (state instanceof Skull) {
                        Skull skull = (Skull) state;
                        URI url = URI.create("http://textures.minecraft.net/texture/b7cab56c82cb81bdb9979a464bc9d3ba3e6722ba122cf6c52873010a2b59aefe");

                        // Create a blank profile (can use random UUID)
                        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());

                        // Set the texture
                        PlayerTextures textures = profile.getTextures();
                        textures.setSkin(url.toURL());
                        profile.setTextures(textures);

                        skull.setOwnerProfile(profile);
                        skull.update(false, false);
                    }

                    // Mark decorative site as occupied
                    plugin.getCacheManager().updateGraveSiteOccupancy(graveyardName, selectedGraveSite.getLocation(), true);
                    selectedGraveSite.setOccupied(true);

                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "A symbolic grave was created in " + ChatColor.GOLD + graveyardName);
                    plugin.getGravesX().debugMessage("Decorative grave created for " + player.getName() + " at " + selectedGraveSite.getLocation(), 2);
                }
            }
        }
    }

//    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
//    public void onEntityDeath(EntityDeathEvent event) {
//        if (event.getEntity() instanceof Player) {
//            Player player = (Player) event.getEntity();
//            String graveyardName = getAnyAvailableGraveyard();
//
//            if (graveyardName != null) {
//                List<GraveSite> graveSites = plugin.getCacheManager().getGraveSites(graveyardName);
//                GraveSite selectedGraveSite = null;
//
//                List<GraveSite> availableGraveSites = graveSites.stream()
//                        .filter(graveSite -> !graveSite.isOccupied())
//                        .collect(Collectors.toList());
//
//                if (!availableGraveSites.isEmpty()) {
//                    selectedGraveSite = availableGraveSites.get(new Random().nextInt(availableGraveSites.size()));
//                } else {
//                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "No available grave sites found in the graveyard " + ChatColor.GOLD + graveyardName);
//                    plugin.getGravesX().debugMessage("No available grave sites for player " + player.getName(), 2);
//                }
//
//                if (selectedGraveSite != null) {
//                    player.getInventory().clear();
//                    Entity killer = player.getKiller();
//                    EntityType killerEntityType = killer != null ? killer.getType() : null;
//                    int experience = player.getTotalExperience();
//                    long timeAliveRemaining = 10000;
//                    Map<EquipmentSlot, ItemStack> equipmentMap = new EnumMap<>(EquipmentSlot.class);
//                    List<ItemStack> itemStackList = event.getDrops();
//                    EntityDamageEvent.DamageCause damageCause = (player.getLastDamageCause() != null) ?
//                            player.getLastDamageCause().getCause() : EntityDamageEvent.DamageCause.CUSTOM;
//                    boolean graveProtection = true;
//                    long graveProtectionTime = 10000;
//
//                    // Create real grave at death location
//                    Location deathLoc = player.getLocation();
//                    plugin.getGravesXAPI().createGrave(
//                            player, killer, killerEntityType, deathLoc,
//                            equipmentMap, itemStackList, experience, timeAliveRemaining,
//                            damageCause, graveProtection, graveProtectionTime
//                    );
//
//                    // Create decorative empty grave at symbolic graveyard site
//                    Location holoLoc = selectedGraveSite.getLocation().clone().add(0.5, 2.25, 0.5);
//                    new GraveyardHologramManager(plugin).createHologram(holoLoc, player.getName());
//                    selectedGraveSite.getLocation().getBlock().setType(Material.SKELETON_SKULL);
//
//                    plugin.getCacheManager().updateGraveSiteOccupancy(graveyardName, selectedGraveSite.getLocation(), true);
//                    selectedGraveSite.setOccupied(true);
//
//                    // Mark decorative site as occupied
//                    plugin.getCacheManager().updateGraveSiteOccupancy(graveyardName, selectedGraveSite.getLocation(), true);
//                    selectedGraveSite.setOccupied(true);
//
//                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "A symbolic grave was created in " + ChatColor.GOLD + graveyardName);
//                    plugin.getGravesX().debugMessage("Decorative grave created for " + player.getName() + " at " + selectedGraveSite.getLocation(), 2);
//                    event.getDrops().clear();
//                } else {
//                    for (ItemStack item : event.getDrops()) {
//                        player.getWorld().dropItemNaturally(player.getLocation(), item);
//                    }
//                    event.getDrops().clear();
//                }
//            }
//        }
//    }

    private String getAnyAvailableGraveyard() {
        for (Map.Entry<String, List<GraveSite>> entry : plugin.getCacheManager().getAllGraveyards().entrySet()) {
            boolean hasAvailable = entry.getValue().stream().anyMatch(site -> !site.isOccupied());
            if (hasAvailable) return entry.getKey();
        }
        return null;
    }
}