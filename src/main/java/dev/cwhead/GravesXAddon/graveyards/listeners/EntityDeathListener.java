package dev.cwhead.GravesXAddon.graveyards.listeners;

import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.api.skin.SkinAPI;
import dev.cwhead.GravesX.event.GraveCreateEvent;
import dev.cwhead.GravesXAddon.graveyards.Graveyards;
import dev.cwhead.GravesXAddon.graveyards.managers.GraveyardHologramManager;
import dev.cwhead.GravesXAddon.graveyards.util.GraveSite;
import dev.cwhead.GravesXAddon.graveyards.util.ConfigUtil;
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

import java.util.*;

public class EntityDeathListener implements Listener {

    private final Graveyards plugin;
    private final NamespacedKey graveHeadKey;
    private final GraveyardHologramManager holograms;

    public EntityDeathListener(Graveyards plugin) {
        this.plugin = plugin;
        this.graveHeadKey = new NamespacedKey(plugin, "GraveyardHead");
        this.holograms = new GraveyardHologramManager(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGraveCreate(GraveCreateEvent event) {
        if (!event.isEntityActuallyPlayer())
            return; // We are only doing players. Entity Graves do not count.

        Player player = event.getPlayer();

        String graveyardName = getAnyAvailableGraveyard();
        if (graveyardName == null) {
            graveyardName = pickAnyGraveyardName();
            if (graveyardName == null) {
                return;
            }
            resetAllSitesIn(graveyardName);
        }

        List<GraveSite> graveSites = plugin.getCacheManager().getGraveSites(graveyardName);
        if (graveSites == null || graveSites.isEmpty()) {
            return;
        }

        // Build list of available sites
        List<GraveSite> availableGraveSites = graveSites.stream()
                .filter(gs -> !gs.isOccupied())
                .toList();

        ConfigUtil cfg = plugin.getConfigUtil();

        GraveSite selectedGraveSite;
        if (!availableGraveSites.isEmpty()) {
            selectedGraveSite = availableGraveSites.get(new Random().nextInt(availableGraveSites.size()));
        } else {
            plugin.getGravesX().debugMessage("All grave sites occupied in '" + graveyardName + "'. Resetting all to unoccupied.", 1);
            resetAllSitesIn(graveyardName);

            List<GraveSite> nowAvailable = plugin.getCacheManager().getGraveSites(graveyardName).stream()
                    .filter(gs -> !gs.isOccupied())
                    .toList();

            if (nowAvailable.isEmpty()) {
                String failMsg = cfg.getMessage("graveyard-place-failed").replace("%graveyard%", graveyardName);
                player.sendMessage(failMsg);
                return;
            }
            selectedGraveSite = nowAvailable.get(new Random().nextInt(nowAvailable.size()));
        }

        Entity killer = player.getKiller();
        EntityType killerEntityType = killer != null ? killer.getType() : null;

        Location holoLoc = selectedGraveSite.getLocation().clone().add(0.5, 0.5, 0.5);
        holograms.createHologram(holoLoc, player.getName(), killer, killerEntityType);

        Block skullBlock = selectedGraveSite.getLocation().getBlock();
        skullBlock.setType(Material.PLAYER_HEAD);

        BlockState state = skullBlock.getState();
        if (state instanceof Skull skull) {
            Grave grave = event.getGrave();
            String headName = plugin.getGravesX().getConfig("block.head.name", grave).getString("block.head.name");
            String headBase64 = plugin.getGravesX().getConfig("block.head.base64", grave).getString("block.head.base64");
            switch (plugin.getGravesX().getConfig("block.head.type", grave).getInt("block.head.type")) {
                case 1:
                    SkinAPI.setSkullTexture(skull, headName, headBase64);
                    break;
                case 2:
                    if (grave.getOwnerType() == EntityType.PLAYER) {
                        try {
                            skull.setOwningPlayer(plugin.getServer().getOfflinePlayer(grave.getOwnerUUID()));
                        } catch (Exception e) {
                            skull.setOwner(grave.getOwnerName());
                        }
                    }
                    break;
                case 0:
                default:
                    if (grave.getOwnerType() == EntityType.PLAYER) {
                        try {
                            skull.setOwningPlayer(plugin.getServer().getOfflinePlayer(grave.getOwnerUUID()));
                        } catch (Exception e) {
                            skull.setOwner(grave.getOwnerName());
                        }
                    } else if (grave.getOwnerTexture() != null) {
                        SkinAPI.setSkullTexture(skull, grave.getOwnerName(), grave.getOwnerTexture());
                    } else if (headBase64 != null && !headBase64.isEmpty()) {
                        SkinAPI.setSkullTexture(skull, grave.getOwnerName(), headBase64);
                    }

            }

            skull.getPersistentDataContainer().set(graveHeadKey, PersistentDataType.BYTE, (byte) 1);
            skull.update();
        }

        plugin.getCacheManager().updateGraveSiteOccupancy(graveyardName, selectedGraveSite.getLocation(), true);
        selectedGraveSite.setOccupied(true);

        String okMsg = cfg.getMessage("graveyard-place-successful")
                .replace("%graveyard%", graveyardName);
        player.sendMessage(okMsg);

        plugin.getGravesX().debugMessage(
                "Decorative grave created for " + player.getName() + " at " + selectedGraveSite.getLocation(), 2);
    }

    private String getAnyAvailableGraveyard() {
        for (Map.Entry<String, List<GraveSite>> entry : plugin.getCacheManager().getAllGraveyards().entrySet()) {
            boolean hasAvailable = entry.getValue().stream().anyMatch(site -> !site.isOccupied());
            if (hasAvailable) return entry.getKey();
        }
        return null;
    }

    private String pickAnyGraveyardName() {
        for (String name : plugin.getCacheManager().getAllGraveyards().keySet()) {
            return name;
        }
        return null;
    }

    /** Sets ALL gravesites in the given graveyard to unoccupied = false (both cache + in-memory objects). */
    private void resetAllSitesIn(String graveyardName) {
        List<GraveSite> sites = plugin.getCacheManager().getGraveSites(graveyardName);
        if (sites == null || sites.isEmpty()) return;

        int count = 0;
        for (GraveSite site : sites) {
            if (site.isOccupied()) {
                plugin.getCacheManager().updateGraveSiteOccupancy(graveyardName, site.getLocation(), false);
                site.setOccupied(false);
                count++;
            }
        }
        plugin.getGravesX().debugMessage("Reset " + count + " occupied graves in '" + graveyardName + "'.", 1);
    }
}