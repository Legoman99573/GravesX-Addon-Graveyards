package dev.cwhead.GravesXAddon.graveyards.util;

import com.ranull.graves.integration.MiniMessage;
import dev.cwhead.GravesXAddon.graveyards.Graveyards;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for configuration handling in the GravesX addon.
 * Manages:
 *  - Static folder /plugins/GravesX/addon/Graveyards/graveyards-data (per-graveyard data)
 *  - Global config at /plugins/GravesX/addon/Graveyards/config.yml (holograms, messages)
 *  - Helper methods for graveyard and gravesite CRUD.
 */
public final class ConfigUtil {
    private final Graveyards plugin;

    private final File addonFolder;
    private final File graveyardFolder;
    private final File globalConfigFile;

    private YamlConfiguration globalConfig;

    public ConfigUtil(Graveyards plugin) {
        this.plugin = plugin;

        this.addonFolder = new File("plugins/GravesX/addon/Graveyards");
        if (!addonFolder.exists() && !addonFolder.mkdirs()) {
            plugin.getLogger().warning("Failed to create addon folder at: " + addonFolder.getAbsolutePath());
        }

        this.graveyardFolder = new File(addonFolder, "graveyards-data");
        if (!graveyardFolder.exists() && !graveyardFolder.mkdirs()) {
            plugin.getLogger().warning("Failed to create graveyard folder at: " + graveyardFolder.getAbsolutePath());
        }

        this.globalConfigFile = new File(addonFolder, "config.yml");
        ensureGlobalConfigExists();
        reloadGlobalConfig();
    }

    /**
     * Re-reads /plugins/GravesX/addon/config.yml.
     * Call this if you edit the file at runtime.
     */
    public void reloadGlobalConfig() {
        this.globalConfig = YamlConfiguration.loadConfiguration(globalConfigFile);
    }

    /**
     * Reads the config-version from the global config. Defaults to 1 if absent.
     */
    public int getConfigVersion() {
        return globalConfig.getInt("config-version", 1);
    }

    /** Hologram base offset (relative to head block). */
    public Vector getHologramBaseOffset() {
        return readVector("graveyard-holograms.base-location-offset", new Vector(0, 0.5, 0));
    }

    /** Hologram new-line offset. */
    public Vector getHologramNewLineOffset() {
        return readVector("graveyard-holograms.new-line-offset", new Vector(0, 0.25, 0));
    }

    /** Hologram lines (colorized). */
    public List<String> getHologramLines() {
        List<String> raw = globalConfig.getStringList("graveyard-holograms.lines");
        if (raw.isEmpty()) {
            raw = Arrays.asList("&7Here Lies", "&6%player%");
            warnMissing("graveyard-holograms.lines");
        }
        return colorize(raw);
    }

    /**
     * Returns a colorized message from messages.* with the prefix applied.
     * Example keys:
     *  - "graveyard-place-successful"
     *  - "graveyard-place-failed"
     */
    public String getMessage(String key) {
        String prefix = globalConfig.getString("messages.prefix", "&7☠ ");
        String body = globalConfig.getString("messages." + key, "");
        if (body.isEmpty()) {
            warnMissing("messages." + key);
        }
        return colorize(prefix + body);
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

    /** Deletes the graveyard YAML. */
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

    /** Creates or updates a gravesite in the specified graveyard file. */
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

    /** Removes a gravesite from the specified graveyard. */
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
     * If some numbers are missing in the middle, this returns (max + 1).
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

    /** Lists all graveyard names (filenames without .yml) found in the folder. */
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

    /** Reads all graveyards into a map: graveyard name -> list of GraveSite. */
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

    private void ensureGlobalConfigExists() {
        try {
            Files.createDirectories(addonFolder.toPath());

            if (globalConfigFile.exists()) return;

            File legacy = new File(plugin.getDataFolder(), "config.yml");
            if (legacy.exists()) {
                try {
                    Files.createDirectories(globalConfigFile.toPath().getParent());
                    Files.copy(legacy.toPath(), globalConfigFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    plugin.getLogger().info("Migrated legacy config.yml to " + globalConfigFile.getAbsolutePath());
                    return;
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to migrate legacy config.yml: " + e.getMessage());
                    // fall through to copy from resource
                }
            }

            try (InputStream in = plugin.getResource("config.yml")) {
                if (in == null) {
                    plugin.getLogger().severe("Embedded config.yml not found in plugin JAR!");
                    return;
                }
                Files.copy(in, globalConfigFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("Wrote default config.yml to: " + globalConfigFile.getAbsolutePath());
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Unable to ensure global config.yml: " + e.getMessage());
        }
    }

    private Vector readVector(String basePath, Vector def) {
        double x = globalConfig.getDouble(basePath + ".x", def.getX());
        double y = globalConfig.getDouble(basePath + ".y", def.getY());
        double z = globalConfig.getDouble(basePath + ".z", def.getZ());
        if (!globalConfig.contains(basePath)) {
            warnMissing(basePath);
        }
        return new Vector(x, y, z);
    }

    private List<String> colorize(List<String> lines) {
        return lines.stream().map(this::colorize).collect(Collectors.toList());
    }

    private String colorize(String s) {
        String mainMessage = ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
        if (hasMiniMessage()) {
            mainMessage = MiniMessage.convertLegacyToMiniMessage(mainMessage);
            return MiniMessage.parseString(mainMessage);
        }
        return mainMessage;
    }

    private void warnMissing(String path) {
        plugin.getLogger().warning("Global config missing key: " + path + " (using default).");
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

    public boolean hasMiniMessage() {
        try {
            return plugin.getGravesX().getIntegrationManager().hasMiniMessage();
        } catch (Throwable t) {
            return false;
        }
    }
}