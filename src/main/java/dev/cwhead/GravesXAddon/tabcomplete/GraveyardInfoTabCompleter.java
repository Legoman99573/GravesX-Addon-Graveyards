package dev.cwhead.GravesXAddon.tabcomplete;

import dev.cwhead.GravesXAddon.Graveyards;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides tab completion for graveyard information commands, allowing players
 * to easily complete graveyard names based on partial input.
 */
public class GraveyardInfoTabCompleter implements TabCompleter {

    private final Graveyards plugin;

    /**
     * Constructs a GraveyardInfoTabCompleter for a given plugin instance.
     *
     * @param plugin the main plugin class instance, used to access the cache manager.
     */
    public GraveyardInfoTabCompleter(Graveyards plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles tab completion for graveyard information commands, suggesting
     * graveyard names based on the user's input.
     *
     * @param sender the command sender, typically a {@link Player}.
     * @param command the command being executed.
     * @param alias the alias of the command.
     * @param args the arguments provided so far.
     * @return a list of suggested completions based on the graveyard names
     *         that start with the given input.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (sender instanceof Player && args.length == 1) {
            String partialInput = args[0].toLowerCase();

            for (String graveyardName : plugin.getCacheManager().getAllGraveyards().keySet()) {
                if (graveyardName.toLowerCase().startsWith(partialInput)) {
                    completions.add(graveyardName);
                }
            }
        }

        return completions;
    }
}