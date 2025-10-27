package dev.cwhead.GravesXAddon.graveyards.listeners;

import dev.cwhead.GravesX.event.GraveCreateEvent;
import dev.cwhead.GravesXAddon.graveyards.Graveyards;
import dev.cwhead.GravesXAddon.graveyards.managers.GraveyardHologramManager;
import dev.cwhead.GravesXAddon.graveyards.util.GraveSite;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.*;

public class EntityDeathListener implements Listener {

    private final Graveyards plugin;
    private final NamespacedKey graveHeadKey;

    public EntityDeathListener(Graveyards plugin) {
        this.plugin = plugin;
        graveHeadKey = new NamespacedKey(plugin, "GraveyardHead");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGraveCreate(GraveCreateEvent event) throws MalformedURLException {
        if (event.getEntity() instanceof Player player) {
            String graveyardName = getAnyAvailableGraveyard();
            if (graveyardName != null) {
                List<GraveSite> graveSites = plugin.getCacheManager().getGraveSites(graveyardName);
                GraveSite selectedGraveSite = null;

                List<GraveSite> availableGraveSites = graveSites.stream()
                        .filter(graveSite -> !graveSite.isOccupied())
                        .toList();

                if (!availableGraveSites.isEmpty()) {
                    selectedGraveSite = availableGraveSites.get(new Random().nextInt(availableGraveSites.size()));
                } else {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "No available grave sites found in the graveyard " + ChatColor.GOLD + graveyardName);
                    plugin.getGravesX().debugMessage("No available grave sites for player " + player.getName(), 2);
                }
                if (selectedGraveSite != null) {
                    Entity killer = player.getKiller();
                    EntityType killerEntityType = killer != null ? killer.getType() : null;

                    Location holoLoc = selectedGraveSite.getLocation().clone().add(0.5, 0.5, 0.5);
                    new GraveyardHologramManager(plugin).createHologram(holoLoc, player.getName());
                    Block skullBlock = selectedGraveSite.getLocation().getBlock();
                    skullBlock.setType(Material.PLAYER_HEAD);

                    BlockState state = skullBlock.getState();
                    if (state instanceof Skull skull) {
                        URI url = URI.create("http://textures.minecraft.net/texture/b7cab56c82cb81bdb9979a464bc9d3ba3e6722ba122cf6c52873010a2b59aefe");

                        // Create a blank profile (can use random UUID)
                        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());

                        // Set the texture
                        PlayerTextures textures = profile.getTextures();
                        textures.setSkin(url.toURL());
                        profile.setTextures(textures);

                        skull.setOwnerProfile(profile);
                        skull.getPersistentDataContainer().set(graveHeadKey, PersistentDataType.BYTE, (byte) 1);
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

    private String getAnyAvailableGraveyard() {
        for (Map.Entry<String, List<GraveSite>> entry : plugin.getCacheManager().getAllGraveyards().entrySet()) {
            boolean hasAvailable = entry.getValue().stream().anyMatch(site -> !site.isOccupied());
            if (hasAvailable) return entry.getKey();
        }
        return null;
    }
}