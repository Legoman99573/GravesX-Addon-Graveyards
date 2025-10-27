package dev.cwhead.GravesXAddon.graveyards.util;

import dev.cwhead.GravesXAddon.graveyards.Graveyards;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Utility class for configuration handling in the GravesX addon.
 * Manages the static folder /plugins/GravesX/addon/Graveyards and
 * provides helper methods for graveyard and gravesite CRUD.
 */
public final class ConfigUtil {
    private final Graveyards plugin;
    private final File graveyardFolder;

    public ConfigUtil(Graveyards plugin) {
        this.plugin = plugin;

        this.graveyardFolder = new File("plugins/GravesX/addon/Graveyards");

        if (!graveyardFolder.exists() && !graveyardFolder.mkdirs()) {
            plugin.getLogger().warning("Failed to create graveyard folder at: " + graveyardFolder.getAbsolutePath());
        }
    }

    public File getGraveyardFolder() {
        return graveyardFolder;
    }

    public File getGraveyardFile(String name) {
        return new File(graveyardFolder, name + ".yml");
    }

    public boolean graveyardExists(String name) {
        return getGraveyardFile(name).exists();
    }

    /**
     * Creates a graveyard YAML with pos1/pos2 corners.
     * Returns false if file already exists or IO fails.
     */
    public boolean createGraveyard(String name, Location pos1, Location pos2) {
        File file = getGraveyardFile(name);
        if (file.exists()) {
            plugin.getLogger().warning("Cannot create graveyard '" + name + "': file already exists.");
            return false;
        }

        YamlConfiguration config = new YamlConfiguration();
        config.set("name", name);

        config.set("pos1.world", pos1.getWorld().getName());
        config.set("pos1.x", pos1.getBlockX());
        config.set("pos1.y", pos1.getBlockY());
        config.set("pos1.z", pos1.getBlockZ());

        config.set("pos2.world", pos2.getWorld().getName());
        config.set("pos2.x", pos2.getBlockX());
        config.set("pos2.y", pos2.getBlockY());
        config.set("pos2.z", pos2.getBlockZ());

        return saveConfig(config, file, "create graveyard '" + name + "'");
    }

    /**
     * Deletes the graveyard YAML.
     */
    public boolean deleteGraveyard(String name) {
        File file = getGraveyardFile(name);
        if (!file.exists()) {
            plugin.getLogger().warning("Cannot delete graveyard '" + name + "': file not found.");
            return false;
        }
        boolean ok = file.delete();
        if (!ok) {
            plugin.getLogger().severe("Failed to delete graveyard '" + name + "' at " + file.getAbsolutePath());
        }
        return ok;
    }

    /**
     * Creates or updates a gravesite in the specified graveyard file.
     */
    public boolean saveGraveSite(String graveyardName, int siteNumber, Location location, boolean occupied) {
        File file = getGraveyardFile(graveyardName);
        if (!file.exists()) {
            plugin.getLogger().warning("Cannot save gravesite: Graveyard '" + graveyardName + "' does not exist.");
            return false;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String key = "gravesite." + siteNumber;

        config.set(key + ".world", location.getWorld().getName());
        config.set(key + ".x", location.getBlockX());
        config.set(key + ".y", location.getBlockY());
        config.set(key + ".z", location.getBlockZ());
        config.set(key + ".occupied", occupied);

        return saveConfig(config, file, "save gravesite " + siteNumber + " in " + graveyardName);
    }

    /**
     * Edits an existing gravesite's position or occupied state.
     * Pass null for fields you don't want to change.
     */
    public boolean editGraveSite(String graveyardName, int siteNumber, Location newLocation, Boolean newOccupied) {
        File file = getGraveyardFile(graveyardName);
        if (!file.exists()) {
            plugin.getLogger().warning("Cannot edit gravesite: Graveyard '" + graveyardName + "' not found.");
            return false;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String key = "gravesite." + siteNumber;

        if (!config.contains(key)) {
            plugin.getLogger().warning("No gravesite " + siteNumber + " found in " + graveyardName);
            return false;
        }

        if (newLocation != null) {
            config.set(key + ".world", newLocation.getWorld().getName());
            config.set(key + ".x", newLocation.getBlockX());
            config.set(key + ".y", newLocation.getBlockY());
            config.set(key + ".z", newLocation.getBlockZ());
        }

        if (newOccupied != null) {
            config.set(key + ".occupied", newOccupied);
        }

        return saveConfig(config, file, "edit gravesite " + siteNumber + " in " + graveyardName);
    }

    /**
     * Removes a gravesite from the specified graveyard.
     */
    public boolean removeGraveSite(String graveyardName, int siteNumber) {
        File file = getGraveyardFile(graveyardName);
        if (!file.exists()) {
            plugin.getLogger().warning("Cannot remove gravesite: Graveyard '" + graveyardName + "' not found.");
            return false;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String key = "gravesite." + siteNumber;

        if (!config.contains(key)) {
            plugin.getLogger().warning("No gravesite " + siteNumber + " found in " + graveyardName);
            return false;
        }

        config.set(key, null);
        return saveConfig(config, file, "remove gravesite " + siteNumber + " in " + graveyardName);
    }

    /**
     * Scans the graveyard YAML and returns the next available site number.
     * If there are no sites yet, returns 1.
     * If some numbers are missing in the middle, this returns (max + 1) for simplicity.
     */
    public int getNextSiteNumber(String graveyardName) {
        File file = getGraveyardFile(graveyardName);
        if (!file.exists()) {
            plugin.getLogger().warning("getNextSiteNumber: Graveyard '" + graveyardName + "' does not exist.");
            return 1;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.isConfigurationSection("gravesite")) {
            return 1;
        }

        int max = 0;
        for (String key : config.getConfigurationSection("gravesite").getKeys(false)) {
            try {
                int n = Integer.parseInt(key);
                if (n > max) max = n;
            } catch (NumberFormatException ignored) {
                // ignore non-numeric keys under gravesite.*
            }
        }
        return max + 1;
    }

    /**
     * Lists all graveyard names (filenames without .yml) found in the folder.
     */
    public List<String> listGraveyardNames() {
        File[] files = graveyardFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) return Collections.emptyList();

        List<String> names = new ArrayList<>();
        for (File f : files) {
            if (!f.isFile()) continue;
            String n = f.getName();
            int i = n.lastIndexOf('.');
            names.add(i > 0 ? n.substring(0, i) : n);
        }
        return names;
    }

    /**
     * Reads and returns all gravesites from a single graveyard YAML.
     * Returns an empty list if file does not exist or has no sites.
     */
    public List<GraveSite> readGraveyardSites(String graveyardName) {
        File file = getGraveyardFile(graveyardName);
        List<GraveSite> result = new ArrayList<>();
        if (!file.exists()) {
            plugin.getLogger().warning("readGraveyardSites: file not found for '" + graveyardName + "'");
            return result;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.isConfigurationSection("gravesite")) {
            return result;
        }

        for (String key : config.getConfigurationSection("gravesite").getKeys(false)) {
            String base = "gravesite." + key;
            String worldName = config.getString(base + ".world");
            int x = config.getInt(base + ".x");
            int y = config.getInt(base + ".y");
            int z = config.getInt(base + ".z");
            boolean occupied = config.getBoolean(base + ".occupied", false);

            World world = (worldName == null) ? null : Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("readGraveyardSites: world '" + worldName +
                        "' not found for " + graveyardName + " site " + key);
                continue;
            }

            result.add(new GraveSite(new Location(world, x, y, z), occupied));
        }

        return result;
    }

    /**
     * Reads all graveyards into a map: graveyard name -> list of GraveSite.
     */
    public Map<String, List<GraveSite>> readAllGraveyards() {
        Map<String, List<GraveSite>> map = new HashMap<>();
        for (String name : listGraveyardNames()) {
            map.put(name, readGraveyardSites(name));
        }
        return map;
    }

    /**
     * Sets the occupied flag for the gravesite that matches the given block location
     * in the specified graveyard YAML. Matches by world + block x/y/z.
     *
     * @param graveyardName name of the graveyard (file without .yml)
     * @param location any Location; block coords are used for matching
     * @param occupied new occupied state
     * @return true if a matching site was found and updated, false otherwise
     */
    public boolean setGraveSiteOccupiedByLocation(String graveyardName, Location location, boolean occupied) {
        if (location == null || location.getWorld() == null) return false;

        File file = getGraveyardFile(graveyardName);
        if (!file.exists()) {
            plugin.getLogger().warning("setGraveSiteOccupiedByLocation: file not found for '" + graveyardName + "'");
            return false;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.isConfigurationSection("gravesite")) {
            return false;
        }

        final String targetWorld = location.getWorld().getName();
        final int tx = location.getBlockX();
        final int ty = location.getBlockY();
        final int tz = location.getBlockZ();

        boolean updated = false;

        for (String key : config.getConfigurationSection("gravesite").getKeys(false)) {
            String base = "gravesite." + key;

            String wName = config.getString(base + ".world");
            int x = config.getInt(base + ".x");
            int y = config.getInt(base + ".y");
            int z = config.getInt(base + ".z");

            if (wName != null
                    && wName.equals(targetWorld)
                    && x == tx && y == ty && z == tz) {
                config.set(base + ".occupied", occupied);
                updated = true;
                break;
            }
        }

        if (!updated) return false;

        try {
            config.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to update occupied flag for " + graveyardName +
                    " at [" + tx + "," + ty + "," + tz + "]: " + e.getMessage());
            return false;
        }
    }

    private boolean saveConfig(YamlConfiguration config, File file, String actionDescription) {
        try {
            config.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to " + actionDescription + ": " + e.getMessage());
            return false;
        }
    }
}