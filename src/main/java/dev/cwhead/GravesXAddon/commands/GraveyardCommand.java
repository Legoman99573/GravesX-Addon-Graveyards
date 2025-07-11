package dev.cwhead.GravesXAddon.commands;

import dev.cwhead.GravesXAddon.Graveyards;
import dev.cwhead.GravesXAddon.type.Graveyard;
import dev.cwhead.GravesXAddon.util.GraveSite;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Command executor for managing graveyards in the GravesX addon.
 * This command allows players to set positions, create graveyards,
 * add or remove grave sites, and delete graveyards.
 */
public class GraveyardCommand implements CommandExecutor {

    private Location pos1;
    private Location pos2;
    private final Graveyards plugin;
    private final File graveyardFolder;

    /**
     * Constructs a GraveyardCommand for the specified plugin instance.
     * Initializes the directory for storing graveyard data.
     *
     * @param plugin the main plugin class instance used to access plugin resources.
     */
    public GraveyardCommand(Graveyards plugin) {
        this.plugin = plugin;
        this.graveyardFolder = new File(plugin.getDataFolder(), "Graveyards");
        if (!graveyardFolder.exists()) {
            graveyardFolder.mkdirs();
        }
    }

    /**
     * Executes the command for managing graveyards.
     * Supports subcommands for setting positions, creating graveyards,
     * adding and removing grave sites, and deleting graveyards.
     *
     * @param sender the entity that issued the command (should be a player).
     * @param command the command that was executed.
     * @param label the alias used for the command.
     * @param args the arguments provided with the command.
     * @return true if the command was executed successfully, false otherwise.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Usage: /graveyards <pos1|pos2|create|addSite>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "pos1":
                pos1 = player.getLocation().subtract(0, -1, 0);
                player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Position 1 set to x:" + ChatColor.GOLD  + pos1.getBlockX() + ChatColor.RED + " y:" + ChatColor.GOLD  + pos1.getBlockY() + ChatColor.RED + " z:" + ChatColor.GOLD  + pos1.getBlockZ());
                break;

            case "pos2":
                pos2 = player.getLocation().subtract(0, -1, 0);
                player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Position 2 set to x:" + ChatColor.GOLD + pos2.getBlockX() + ChatColor.RED + " y:" + ChatColor.GOLD  + pos2.getBlockY() + ChatColor.RED + " z:" + ChatColor.GOLD  + pos2.getBlockZ());
                break;

            case "create":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Usage: /graveyards create <name>");
                    return true;
                }
                if (pos1 == null || pos2 == null) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Please set both positions (pos1 and pos2) before creating a graveyard.");
                    return true;
                }

                String name = args[1];
                File graveyardFile = new File(graveyardFolder, name + ".yml");

                if (graveyardFile.exists()) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "A graveyard with the name " + ChatColor.GOLD  + name + ChatColor.RED  + " already exists.");
                    return true;
                }

                Graveyard graveyard = new Graveyard(name, pos1, pos2);
                if (saveGraveyardToFile(graveyard)) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Graveyard " + ChatColor.GOLD  + name + ChatColor.RED  + " created and saved successfully!");
                    plugin.getCacheManager().reloadCache();  // Reload cache to reflect new graveyard
                } else {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Failed to save graveyard " + ChatColor.GOLD  + name + ChatColor.RED  + ". Please check the server logs.");
                }

                pos1 = null;
                pos2 = null;

                break;

            case "addsite":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Usage: /graveyards addSite <graveyard-name>");
                    return true;
                }

                String graveyardName = args[1];
                File graveyardYmlFile = new File(graveyardFolder, graveyardName + ".yml");

                if (!graveyardYmlFile.exists()) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Graveyard " + ChatColor.GOLD + graveyardName + ChatColor.RED + " does not exist.");
                    return true;
                }

                Location playerLocation = player.getLocation();
                Location gravesiteLocation = playerLocation.clone().subtract(0, 1, 0); // Position 1 block above the ground

                if (gravesiteLocation.getBlock().getType() == Material.AIR) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Please stand on solid ground to add a grave site.");
                    return true;
                }

                List<GraveSite> graveSites = plugin.getCacheManager().getGraveSites(graveyardName);

                int nextSiteNumber = graveSites.size() + 1;

                GraveSite newGraveSite = new GraveSite(playerLocation.clone().add(0, 1, 0), false);
                graveSites.add(newGraveSite);

                if (addGraveSiteToFile(graveyardYmlFile, nextSiteNumber, newGraveSite)) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Added grave site " + ChatColor.GOLD + nextSiteNumber + ChatColor.RED + " to graveyard " + ChatColor.GOLD + graveyardName + ChatColor.RED + ".");
                } else {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Failed to save grave site. Check the server logs for details.");
                }

                break;

            case "delete":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Usage: /graveyards delete <graveyard-name>");
                    return true;
                }

                String graveyardToDelete = args[1];
                File graveyardFileToDelete = new File(graveyardFolder, graveyardToDelete + ".yml");

                if (!graveyardFileToDelete.exists()) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Graveyard " + ChatColor.GOLD + graveyardToDelete + ChatColor.RED + " does not exist.");
                    return true;
                }

                if (graveyardFileToDelete.delete()) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Graveyard " + ChatColor.GOLD + graveyardToDelete + ChatColor.RED + " deleted successfully!");
                    plugin.getCacheManager().reloadCache();  // Reload cache to reflect changes
                } else {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Failed to delete graveyard " + ChatColor.GOLD + graveyardToDelete + ChatColor.RED + ". Please check the server logs.");
                }
                break;

            case "removesite":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Usage: /graveyards removeSite <graveyard-name> <site-number>");
                    return true;
                }

                String graveyardNameToRemoveFrom = args[1];
                File graveyardYmlFileToRemoveFrom = new File(graveyardFolder, graveyardNameToRemoveFrom + ".yml");

                if (!graveyardYmlFileToRemoveFrom.exists()) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Graveyard " + ChatColor.GOLD + graveyardNameToRemoveFrom + ChatColor.RED + " does not exist.");
                    return true;
                }

                int siteNumber;
                try {
                    siteNumber = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Site number must be an integer.");
                    return true;
                }

                YamlConfiguration configToRemoveFrom = YamlConfiguration.loadConfiguration(graveyardYmlFileToRemoveFrom);
                String siteKeyToRemove = "gravesite." + siteNumber;

                if (configToRemoveFrom.contains(siteKeyToRemove)) {
                    configToRemoveFrom.set(siteKeyToRemove, null); // Remove the grave site
                    try {
                        configToRemoveFrom.save(graveyardYmlFileToRemoveFrom);
                        player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Removed grave site " + ChatColor.GOLD + siteNumber + ChatColor.RED + " from graveyard " + ChatColor.GOLD + graveyardNameToRemoveFrom + ChatColor.RED + ".");
                        plugin.getCacheManager().reloadCache();  // Reload cache to reflect changes
                    } catch (IOException e) {
                        player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Failed to save changes to graveyard. Check the server logs.");
                    }
                } else {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "No grave site found with number " + ChatColor.GOLD + siteNumber + ChatColor.RED + " in graveyard " + ChatColor.GOLD + graveyardNameToRemoveFrom + ChatColor.RED + ".");
                }
                break;

            default:
                player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Unknown subcommand. Use /graveyards <pos1|pos2|create|addSite>");
                break;
        }
        return true;
    }

    /**
     * Saves the graveyard data to a YAML file.
     *
     * @param graveyard the graveyard object containing data to save.
     * @return true if the save operation was successful, false otherwise.
     */
    private boolean saveGraveyardToFile(Graveyard graveyard) {
        File file = new File(graveyardFolder, graveyard.getName() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set("name", graveyard.getName());
        config.set("pos1.world", graveyard.getPos1().getWorld().getName());
        config.set("pos1.x", graveyard.getPos1().getBlockX());
        config.set("pos1.y", graveyard.getPos1().getBlockY());
        config.set("pos1.z", graveyard.getPos1().getBlockZ());

        config.set("pos2.world", graveyard.getPos2().getWorld().getName());
        config.set("pos2.x", graveyard.getPos2().getBlockX());
        config.set("pos2.y", graveyard.getPos2().getBlockY());
        config.set("pos2.z", graveyard.getPos2().getBlockZ());

        try {
            config.save(file);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Adds a grave site to the graveyard's YAML file.
     *
     * @param graveyardFile the graveyard file to update.
     * @param siteNumber the site number to assign.
     * @param graveSite the grave site object containing data to save.
     * @return true if the operation was successful, false otherwise.
     */
    private boolean addGraveSiteToFile(File graveyardFile, int siteNumber, GraveSite graveSite) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(graveyardFile);

        String siteKey = "gravesite." + siteNumber;
        config.set(siteKey + ".world", graveSite.getLocation().getWorld().getName());
        config.set(siteKey + ".x", graveSite.getLocation().getBlockX());
        config.set(siteKey + ".y", graveSite.getLocation().getBlockY());
        config.set(siteKey + ".z", graveSite.getLocation().getBlockZ());
        config.set(siteKey + ".occupied", false);

        try {
            config.save(graveyardFile);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}