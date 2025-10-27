package dev.cwhead.GravesXAddon.tabcomplete;

import dev.cwhead.GravesXAddon.Graveyards;
import dev.cwhead.GravesXAddon.util.ConfigUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides tab completion for graveyard-related commands, supporting operations
 * like creating, deleting, and managing graveyard sites.
 */
public class GraveyardTabCompleter implements TabCompleter {

    private final Graveyards plugin;
    private final ConfigUtil configUtil;
    private final File graveyardFolder;

    /**
     * Constructs a GraveyardTabCompleter for a given plugin instance, initializing
     * the folder to store graveyard configuration files if it does not already exist.
     *
     * @param plugin the main plugin class instance, used to locate the graveyard folder.
     */
    public GraveyardTabCompleter(Graveyards plugin) {
        this.plugin = plugin;
        this.configUtil = plugin.getConfigUtil();
        this.graveyardFolder = configUtil.getGraveyardFolder();

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

            return filterPrefix(completions, args[0]);
        }

        if (args.length == 2) {
            if ("addsite".equalsIgnoreCase(args[0]) ||
                    "delete".equalsIgnoreCase(args[0]) ||
                    "removesite".equalsIgnoreCase(args[0])) {

                completions = listGraveyardNames();
                return filterPrefix(completions, args[1]);
            }
        }

        if (args.length == 3 && "removesite".equalsIgnoreCase(args[0])) {
            String graveyardName = args[1];
            File graveyardFile = configUtil.getGraveyardFile(graveyardName);
            if (graveyardFile.exists()) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(graveyardFile);
                if (config.isConfigurationSection("gravesite")) {
                    List<String> siteKeys = new ArrayList<>(config.getConfigurationSection("gravesite").getKeys(false));
                    siteKeys.sort(Comparator.comparingInt(Integer::parseInt));
                    completions.addAll(siteKeys);
                }
            }
            return filterPrefix(completions, args[2]);
        }

        return completions;
    }

    /**
     * Returns a list of all existing graveyard names (without the .yml extension)
     * from the configured graveyard folder.
     *
     * @return a list of graveyard file names without extensions.
     */
    private List<String> listGraveyardNames() {
        File[] files = graveyardFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) return Collections.emptyList();

        return Arrays.stream(files)
                .filter(File::isFile)
                .map(f -> {
                    String name = f.getName();
                    int i = name.lastIndexOf('.');
                    return (i > 0) ? name.substring(0, i) : name;
                })
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    /**
     * Filters a list of strings by case-insensitive prefix matching against
     * the provided partial input.
     *
     * @param candidates the list of possible completions.
     * @param partial the partial input typed by the user.
     * @return a filtered list of completions that match the input prefix.
     */
    private static List<String> filterPrefix(List<String> candidates, String partial) {
        if (partial == null || partial.isEmpty()) return new ArrayList<>(candidates);
        String lower = partial.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(lower))
                .collect(Collectors.toList());
    }
}