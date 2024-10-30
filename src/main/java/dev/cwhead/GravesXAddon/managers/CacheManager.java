package dev.cwhead.GravesXAddon.managers;

import dev.cwhead.GravesXAddon.Graveyards;
import dev.cwhead.GravesXAddon.util.GraveSite;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the caching and loading of graveyard data from YAML configuration files.
 * This includes handling grave sites and their occupancy status.
 */
public class CacheManager {

    private final Map<String, List<GraveSite>> graveyardCache = new HashMap<>();
    private final File graveyardFolder;
    private final Graveyards plugin;

    /**
     * Constructs a CacheManager for the specified plugin instance, initializing
     * the folder for storing graveyard configuration files if it does not exist.
     *
     * @param plugin the main plugin class instance used to access plugin resources.
     */
    public CacheManager(Graveyards plugin) {
        this.plugin = plugin;
        this.graveyardFolder = new File(plugin.getDataFolder(), "Graveyards");
        if (!graveyardFolder.exists()) {
            graveyardFolder.mkdirs();
        }
    }

    /**
     * Loads all graveyards asynchronously from YAML configuration files into the cache.
     * This method logs the loading process and populates the graveyard cache with
     * grave sites and their occupancy status.
     */
    public void loadAllGraveyards() {
        Bukkit.getScheduler().runTaskAsynchronously(Graveyards.getInstance(), () -> {
            plugin.getLogger().info("Loading Graveyards...");
            File[] graveyardFiles = graveyardFolder.listFiles((dir, name) -> name.endsWith(".yml"));

            if (graveyardFiles == null) {
                plugin.getLogger().warning("No graveyard files found.");
                return;
            }

            for (File graveyardFile : graveyardFiles) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(graveyardFile);

                String graveyardName = config.getString("name");
                if (graveyardName == null) {
                    plugin.getLogger().warning("Graveyard name not found in file: " + graveyardFile.getName());
                    continue;
                }

                List<GraveSite> graveSites = new ArrayList<>();
                if (config.isConfigurationSection("gravesite")) {
                    for (String key : config.getConfigurationSection("gravesite").getKeys(false)) {
                        String worldName = config.getString("gravesite." + key + ".world");
                        double x = config.getDouble("gravesite." + key + ".x");
                        double y = config.getDouble("gravesite." + key + ".y");
                        double z = config.getDouble("gravesite." + key + ".z");
                        boolean occupied = config.getBoolean("gravesite." + key + ".occupied", false);

                        if (worldName != null) {
                            Location location = new Location(Bukkit.getWorld(worldName), x, y, z);
                            graveSites.add(new GraveSite(location, occupied));
                            plugin.getLogger().info("Loaded grave site: " + key + " at " + location + " (occupied: " + occupied + ")");
                        } else {
                            plugin.getLogger().warning("World not found for grave site: " + key);
                        }
                    }
                }
                graveyardCache.put(graveyardName, graveSites);
            }
            Bukkit.getScheduler().runTask(Graveyards.getInstance(), () -> {
                plugin.getLogger().info("Loaded " + getGraveyardCacheSize() + " graveyards.");
            });
        });
    }

    /**
     * Updates the occupancy status of a grave site in the specified graveyard.
     * This method also updates the corresponding YAML configuration file to reflect the change.
     *
     * @param graveyardName the name of the graveyard containing the grave site.
     * @param location the location of the grave site to be updated.
     * @param occupied the new occupancy status of the grave site.
     */
    public void updateGraveSiteOccupancy(String graveyardName, Location location, boolean occupied) {
        List<GraveSite> graveSites = getGraveSites(graveyardName);

        for (GraveSite graveSite : graveSites) {
            plugin.getGravesX().debugMessage("Looking for grave site in " + graveyardName + " for location " + location.toString(),2);
            if (graveSite.getLocation().equals(location)) {
                graveSite.setOccupied(occupied);

                File graveyardFile = new File(plugin.getDataFolder(), "Graveyards/" + graveyardName + ".yml");
                YamlConfiguration config = YamlConfiguration.loadConfiguration(graveyardFile);

                for (String key : config.getConfigurationSection("gravesite").getKeys(false)) {
                    Location configLocation = new Location(
                            Bukkit.getWorld(config.getString("gravesite." + key + ".world")),
                            config.getLong("gravesite." + key + ".x"),
                            config.getLong("gravesite." + key + ".y"),
                            config.getLong("gravesite." + key + ".z")
                    );

                    if (configLocation.equals(location)) {
                        config.set("gravesite." + key + ".occupied", occupied);
                        break;
                    }
                }

                try {
                    config.save(graveyardFile);
                    plugin.getGravesX().debugMessage("Grave site location " + location + " found and updated in " + graveyardName, 1);
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not save updated graveyard file for " + graveyardName);
                    e.printStackTrace();
                }
                break;
            } else {
                plugin.getGravesX().debugMessage("Grave site location " + location + " not found in " + graveyardName, 2);
            }
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
     *
     * @param graveyardName the name of the graveyard.
     * @param location the location of the grave site.
     * @return the {@link GraveSite} if found, or null if not found.
     */
    public GraveSite getGraveSiteByLocation(String graveyardName, Location location) {
        List<GraveSite> graveSites = getGraveSites(graveyardName);
        for (GraveSite graveSite : graveSites) {
            if (graveSite.getLocation().equals(location)) {
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
        } catch (Exception ignored) {
            // Ignore exceptions during cache clearing
        }
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
}