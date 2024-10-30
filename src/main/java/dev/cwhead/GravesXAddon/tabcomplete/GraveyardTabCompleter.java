package dev.cwhead.GravesXAddon.tabcomplete;

import dev.cwhead.GravesXAddon.Graveyards;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Provides tab completion for graveyard-related commands, supporting operations
 * like creating, deleting, and managing graveyard sites.
 */
public class GraveyardTabCompleter implements TabCompleter {

    private final File graveyardFolder;

    /**
     * Constructs a GraveyardTabCompleter for a given plugin instance, initializing
     * the folder to store graveyard configuration files if it does not already exist.
     *
     * @param plugin the main plugin class instance, used to locate the graveyard folder.
     */
    public GraveyardTabCompleter(Graveyards plugin) {
        this.graveyardFolder = new File(plugin.getDataFolder(), "Graveyards");
        if (!graveyardFolder.exists()) {
            graveyardFolder.mkdirs();
        }
    }

    /**
     * Handles tab completion for graveyard commands, suggesting possible arguments
     * based on the input length and command context.
     *
     * @param sender the command sender, typically a {@link Player}.
     * @param command the command being executed.
     * @param label the alias of the command.
     * @param args the arguments provided so far.
     * @return a list of suggested completions based on the command and argument context.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return completions;
        }

        if (args.length == 1) {
            completions.add("pos1");
            completions.add("pos2");
            completions.add("create");
            completions.add("addsite");
            completions.add("delete");
            completions.add("removesite");
        } else if (args.length == 2) {
            if ("create".equalsIgnoreCase(args[0]) || "delete".equalsIgnoreCase(args[0]) || "removesite".equalsIgnoreCase(args[0])) {
                completions = Arrays.stream(Objects.requireNonNull(graveyardFolder.listFiles()))
                        .filter(file -> file.isFile() && file.getName().endsWith(".yml"))
                        .map(file -> file.getName().replace(".yml", ""))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 3 && "removesite".equalsIgnoreCase(args[0])) {
            String graveyardName = args[1];
            File graveyardFile = new File(graveyardFolder, graveyardName + ".yml");
            if (graveyardFile.exists()) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(graveyardFile);
                int siteCount = Objects.requireNonNull(config.getConfigurationSection("gravesite")).getKeys(false).size();
                for (int i = 1; i <= siteCount; i++) {
                    completions.add(String.valueOf(i));
                }
            }
        }

        return completions;
    }
}