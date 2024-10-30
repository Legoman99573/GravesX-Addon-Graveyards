package dev.cwhead.GravesXAddon.commands;

import dev.cwhead.GravesXAddon.Graveyards;
import dev.cwhead.GravesXAddon.util.GraveSite;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Command executor for retrieving information about grave sites within a specified graveyard.
 * This command provides players with details on the locations and occupancy of grave sites.
 */
public class GraveyardInfoCommand implements CommandExecutor {

    private final Graveyards plugin;

    /**
     * Constructs a GraveyardInfoCommand for the specified plugin instance.
     *
     * @param plugin the main plugin class instance used to access plugin resources.
     */
    public GraveyardInfoCommand(Graveyards plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes the command to retrieve grave site information for a specified graveyard.
     * Sends details such as the world, coordinates, and occupancy status of each grave site to the player.
     *
     * @param sender the entity that issued the command (should be a player).
     * @param command the command that was executed.
     * @param label the alias used for the command.
     * @param args the arguments provided with the command.
     * @return true if the command was executed successfully, false otherwise.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // Check if the correct number of arguments is provided
            if (args.length != 1) {
                player.sendMessage("Usage: /graveyardinfo <graveyard-name>");
                return true;
            }

            String graveyardName = args[0];
            List<GraveSite> graveSites = plugin.getCacheManager().getGraveSites(graveyardName);

            // Check if any grave sites exist for the specified graveyard
            if (graveSites.isEmpty()) {
                player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "No grave sites found for " + ChatColor.GOLD + graveyardName);
            } else {
                player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Grave sites for " + ChatColor.GOLD + graveyardName);
                for (GraveSite graveSite : graveSites) {
                    Location location = graveSite.getLocation();
                    if (location.getWorld() != null) {
                        // Send grave site details to the player
                        player.sendMessage(ChatColor.RED + "World: " + ChatColor.GOLD + location.getWorld().getName() +
                                ChatColor.RED + ", X: " + ChatColor.GOLD + location.getBlockX() +
                                ChatColor.RED + ", Y: " + ChatColor.GOLD + location.getBlockY() +
                                ChatColor.RED + ", Z: " + ChatColor.GOLD + location.getBlockZ() +
                                ChatColor.RED + "\n Occupied: " + ChatColor.GOLD + graveSite.isOccupied());
                    } else {
                        // Handle unknown world case
                        player.sendMessage(ChatColor.RED + "World: " + ChatColor.GOLD + "Unknown" +
                                ChatColor.RED + ", X: " + ChatColor.GOLD + location.getBlockX() +
                                ChatColor.RED + ", Y: " + ChatColor.GOLD + location.getBlockY() +
                                ChatColor.RED + ", Z: " + ChatColor.GOLD + location.getBlockZ() +
                                ChatColor.RED + "\n Occupied: " + ChatColor.GOLD + graveSite.isOccupied());
                    }
                }
            }
        } else {
            sender.sendMessage("This command can only be run by a player.");
        }
        return true;
    }
}