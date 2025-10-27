package dev.cwhead.GravesXAddon.commands;

import dev.cwhead.GravesXAddon.Graveyards;
import dev.cwhead.GravesXAddon.util.ConfigUtil;
import dev.cwhead.GravesXAddon.util.GraveSite;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GraveyardCommand implements CommandExecutor {

    private Location pos1;
    private Location pos2;
    private final Graveyards plugin;
    private final ConfigUtil configUtil;

    public GraveyardCommand(Graveyards plugin) {
        this.plugin = plugin;
        this.configUtil = plugin.getConfigUtil();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED +
                    "Usage: /graveyards <pos1|pos2|create|addSite|removeSite|delete>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "pos1": {
                pos1 = player.getLocation().subtract(0, -1, 0);
                player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Position 1 set to x:" +
                        ChatColor.GOLD + pos1.getBlockX() + ChatColor.RED + " y:" +
                        ChatColor.GOLD + pos1.getBlockY() + ChatColor.RED + " z:" +
                        ChatColor.GOLD + pos1.getBlockZ());
                break;
            }

            case "pos2": {
                pos2 = player.getLocation().subtract(0, -1, 0);
                player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Position 2 set to x:" +
                        ChatColor.GOLD + pos2.getBlockX() + ChatColor.RED + " y:" +
                        ChatColor.GOLD + pos2.getBlockY() + ChatColor.RED + " z:" +
                        ChatColor.GOLD + pos2.getBlockZ());
                break;
            }

            case "create": {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Usage: /graveyards create <name>");
                    return true;
                }
                if (pos1 == null || pos2 == null) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED +
                            "Please set both positions (pos1 and pos2) before creating a graveyard.");
                    return true;
                }

                String name = args[1];

                if (configUtil.graveyardExists(name)) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED +
                            "A graveyard with the name " + ChatColor.GOLD + name + ChatColor.RED + " already exists.");
                    return true;
                }

                boolean ok = configUtil.createGraveyard(name, pos1, pos2);
                if (ok) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Graveyard " +
                            ChatColor.GOLD + name + ChatColor.RED + " created and saved successfully!");
                    plugin.getCacheManager().reloadCache();
                } else {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED +
                            "Failed to save graveyard " + ChatColor.GOLD + name + ChatColor.RED + ". Please check the server logs.");
                }

                pos1 = null;
                pos2 = null;
                break;
            }

            case "addsite": {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED +
                            "Usage: /graveyards addSite <graveyard-name>");
                    return true;
                }

                String graveyardName = args[1];

                if (!configUtil.graveyardExists(graveyardName)) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED +
                            "Graveyard " + ChatColor.GOLD + graveyardName + ChatColor.RED + " does not exist.");
                    return true;
                }

                Location playerLocation = player.getLocation();
                Location groundCheck = playerLocation.clone().subtract(0, 1, 0);
                if (groundCheck.getBlock().getType() == Material.AIR) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED +
                            "Please stand on solid ground to add a grave site.");
                    return true;
                }

                // **New:** Compute next site number from file, not cache
                int nextSiteNumber = configUtil.getNextSiteNumber(graveyardName);

                Location gravesiteLocation = playerLocation.clone().add(0, 1, 0);
                boolean saved = configUtil.saveGraveSite(graveyardName, nextSiteNumber, gravesiteLocation, false);

                if (saved) {
                    // Optional: also push into your cache for immediate use
                    plugin.getCacheManager().getGraveSites(graveyardName)
                            .add(new GraveSite(gravesiteLocation, false));

                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Added grave site " +
                            ChatColor.GOLD + nextSiteNumber + ChatColor.RED + " to graveyard " +
                            ChatColor.GOLD + graveyardName + ChatColor.RED + ".");
                } else {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED +
                            "Failed to save grave site. Check the server logs for details.");
                }
                break;
            }

            case "removesite": {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED +
                            "Usage: /graveyards removeSite <graveyard-name> <site-number>");
                    return true;
                }

                String graveyardNameToRemoveFrom = args[1];
                if (!configUtil.graveyardExists(graveyardNameToRemoveFrom)) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED +
                            "Graveyard " + ChatColor.GOLD + graveyardNameToRemoveFrom + ChatColor.RED + " does not exist.");
                    return true;
                }

                int siteNumber;
                try {
                    siteNumber = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Site number must be an integer.");
                    return true;
                }

                boolean removed = configUtil.removeGraveSite(graveyardNameToRemoveFrom, siteNumber);

                if (removed) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Removed grave site " +
                            ChatColor.GOLD + siteNumber + ChatColor.RED + " from graveyard " +
                            ChatColor.GOLD + graveyardNameToRemoveFrom + ChatColor.RED + ".");
                    plugin.getCacheManager().reloadCache(); // keep cache in sync
                } else {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED +
                            "No grave site found with number " + ChatColor.GOLD + siteNumber + ChatColor.RED +
                            " in graveyard " + ChatColor.GOLD + graveyardNameToRemoveFrom + ChatColor.RED + ".");
                }
                break;
            }

            case "delete": {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED +
                            "Usage: /graveyards delete <graveyard-name>");
                    return true;
                }

                String graveyardToDelete = args[1];

                if (!configUtil.graveyardExists(graveyardToDelete)) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED +
                            "Graveyard " + ChatColor.GOLD + graveyardToDelete + ChatColor.RED + " does not exist.");
                    return true;
                }

                boolean ok = configUtil.deleteGraveyard(graveyardToDelete);
                if (ok) {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "Graveyard " +
                            ChatColor.GOLD + graveyardToDelete + ChatColor.RED + " deleted successfully!");
                    plugin.getCacheManager().reloadCache();
                } else {
                    player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED +
                            "Failed to delete graveyard " + ChatColor.GOLD + graveyardToDelete + ChatColor.RED +
                            ". Please check the server logs.");
                }
                break;
            }

            default: {
                player.sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED +
                        "Unknown subcommand. Use /graveyards <pos1|pos2|create|addSite|removeSite|delete>");
                break;
            }
        }
        return true;
    }
}