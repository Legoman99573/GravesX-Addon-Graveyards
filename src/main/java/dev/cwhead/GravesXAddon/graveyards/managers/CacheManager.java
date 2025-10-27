package dev.cwhead.GravesXAddon.graveyards.managers;

import dev.cwhead.GravesXAddon.graveyards.Graveyards;
import dev.cwhead.GravesXAddon.graveyards.util.ConfigUtil;
import dev.cwhead.GravesXAddon.graveyards.util.GraveSite;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.util.*;

/**
 * Manages the caching and loading of graveyard data from YAML configuration files.
 * This includes handling grave sites and their occupancy status.
 */
public class CacheManager {

    private final Map<String, List<GraveSite>> graveyardCache = new HashMap<>();
    private final File graveyardFolder;
    private final Graveyards plugin;
    private final ConfigUtil configUtil;

    /**
     * Constructs a CacheManager for the specified plugin instance.
     * Uses ConfigUtil for the fixed addon folder (/plugins/GravesX/addon/Graveyards).
     *
     * @param plugin the main plugin class instance used to access plugin resources.
     */
    public CacheManager(Graveyards plugin) {
        this.plugin = plugin;
        this.configUtil = plugin.getConfigUtil();
        this.graveyardFolder = configUtil.getGraveyardFolder();
        if (!graveyardFolder.exists()) {
            graveyardFolder.mkdirs();
        }
    }

    /**
     * Loads all graveyards asynchronously from YAML configuration files into the cache.
     * Populates the graveyard cache with grave sites and their occupancy status.
     */
    public void loadAllGraveyards() {
        plugin.getGravesX().getGravesXScheduler().runTaskAsynchronously(() -> {
            plugin.getLogger().info("Loading Graveyards...");

            Map<String, List<GraveSite>> nextCache = configUtil.readAllGraveyards();

            if (nextCache.isEmpty()) {
                plugin.getLogger().warning("No graveyard files found in: " + graveyardFolder.getAbsolutePath());
            }

            plugin.getGravesX().getGravesXScheduler().runTask(() -> {
                graveyardCache.clear();
                graveyardCache.putAll(nextCache);
                plugin.getLogger().info("Loaded " + getGraveyardCacheSize() + " graveyards.");
            });
        });
    }

    /**
     * Updates the occupancy status of a grave site in the specified graveyard,
     * both in cache and on disk. Compares locations using block coordinates.
     *
     * @param graveyardName the name of the graveyard containing the grave site.
     * @param location the location of the grave site to be updated (any yaw/pitch/decimals allowed).
     * @param occupied the new occupancy status of the grave site.
     */
    public void updateGraveSiteOccupancy(String graveyardName, Location location, boolean occupied) {
        Location target = toBlockLocation(location);

        List<GraveSite> graveSites = getGraveSites(graveyardName);
        boolean foundInCache = false;
        for (GraveSite graveSite : graveSites) {
            if (sameBlock(graveSite.getLocation(), target)) {
                graveSite.setOccupied(occupied);
                foundInCache = true;
                break;
            }
        }
        if (!foundInCache) {
            plugin.getGravesX().debugMessage(
                    "Grave site location " + target + " not found in cache for " + graveyardName, 2);
        }

        boolean updated = configUtil.setGraveSiteOccupiedByLocation(graveyardName, target, occupied);
        if (updated) {
            plugin.getGravesX().debugMessage(
                    "Grave site location " + target + " updated on disk in " + graveyardName, 1);
        } else {
            plugin.getGravesX().debugMessage(
                    "Grave site location " + target + " NOT found on disk for " + graveyardName, 2);
        }
    }

    /**
     * Retrieves the list of grave sites for a specified graveyard.
     *
     * @param graveyardName the name of the graveyard.
     * @return a list of {@link GraveSite} objects for the specified graveyard.
     */
    public List<GraveSite> getGraveSites(String graveyardName) {
        return graveyardCache.getOrDefault(graveyardName, new ArrayList<>());
    }

    /**
     * Retrieves a specific grave site by its location within the specified graveyard.
     * Matches by block coordinates.
     *
     * @param graveyardName the name of the graveyard.
     * @param location the location of the grave site.
     * @return the {@link GraveSite} if found, or null if not found.
     */
    public GraveSite getGraveSiteByLocation(String graveyardName, Location location) {
        Location target = toBlockLocation(location);
        for (GraveSite graveSite : getGraveSites(graveyardName)) {
            if (sameBlock(graveSite.getLocation(), target)) {
                return graveSite;
            }
        }
        return null;
    }

    /**
     * Retrieves a map of all graveyards and their corresponding grave sites.
     *
     * @return a map where keys are graveyard names and values are lists of {@link GraveSite} objects.
     */
    public Map<String, List<GraveSite>> getAllGraveyards() {
        return new HashMap<>(graveyardCache);
    }

    /**
     * Reloads the cache by clearing it and loading all graveyards again.
     */
    public void reloadCache() {
        try {
            graveyardCache.clear();
        } catch (Exception ignored) {}
        loadAllGraveyards();
    }

    /**
     * Gets the number of graveyards currently cached.
     *
     * @return the size of the graveyard cache.
     */
    public int getGraveyardCacheSize() {
        return graveyardCache.size();
    }

    private static boolean sameBlock(Location a, Location b) {
        if (a == null || b == null) return false;
        if (a.getWorld() == null || b.getWorld() == null) return false;
        return Objects.equals(a.getWorld().getName(), b.getWorld().getName())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    private static Location toBlockLocation(Location loc) {
        if (loc == null) return null;
        World w = loc.getWorld();
        return new Location(w, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}