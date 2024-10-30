package dev.cwhead.GravesXAddon;

import com.ranull.graves.Graves;
import dev.cwhead.GravesX.GravesXAPI;
import dev.cwhead.GravesXAddon.commands.GraveyardCommand;
import dev.cwhead.GravesXAddon.commands.GraveyardInfoCommand;
import dev.cwhead.GravesXAddon.events.EntityDeathListener;
import dev.cwhead.GravesXAddon.managers.CacheManager;
import dev.cwhead.GravesXAddon.tabcomplete.GraveyardInfoTabCompleter;
import dev.cwhead.GravesXAddon.tabcomplete.GraveyardTabCompleter;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The main class for the GravesX Addon: Graveyards.
 * This plugin integrates with the GravesX plugin to manage graveyards and related functionalities.
 */
public final class Graveyards extends JavaPlugin {

    private static Graveyards instance;
    private GravesXAPI gravesXAPI;
    private Graves graves;
    private CacheManager cacheManager;

    /**
     * Called when the plugin is enabled.
     * This method checks for the GravesX plugin, sets up commands,
     * initializes the cache manager, and registers event listeners.
     */
    @Override
    public void onEnable() {
        Plugin gravesX = getServer().getPluginManager().getPlugin("GravesX");
        if (gravesX != null && gravesX.isEnabled()) {
            instance = this;
            this.cacheManager = new CacheManager(this);  // Set the instance
            getCommand("graveyards").setExecutor(new GraveyardCommand(this));
            getCommand("graveyardinfo").setExecutor(new GraveyardInfoCommand(this));
            getCommand("graveyardinfo").setTabCompleter(new GraveyardInfoTabCompleter(this));
            getCommand("graveyards").setTabCompleter(new GraveyardTabCompleter(this));

            gravesXAPI = new GravesXAPI((Graves) gravesX);
            graves = (Graves) getServer().getPluginManager().getPlugin("GravesX");
            getLogger().info("Hooked into GravesX. Deaths in graveyards will be handled by this plugin.");
            getServer().getPluginManager().registerEvents(new EntityDeathListener(this), this);

            getCacheManager().loadAllGraveyards();
            getLogger().info("Loaded GravesX Addon: Graveyards");
        } else {
            getLogger().severe("Plugin GravesX is either missing or not enabled. Disabling Plugin.");
        }
    }

    /**
     * Called when the plugin is disabled.
     * This method performs any necessary cleanup when the plugin is unloaded.
     */
    @Override
    public void onDisable() {
        getLogger().info("Graveyards Addon Disabled.");
    }

    /**
     * Retrieves the GravesXAPI instance for interacting with the GravesX plugin.
     *
     * @return The GravesXAPI instance.
     */
    public GravesXAPI getGravesXAPI() {
        return gravesXAPI;
    }

    /**
     * Retrieves the Graves plugin instance that this addon is hooked into.
     *
     * @return The Graves plugin instance.
     */
    public Graves getGravesX() {
        return gravesXAPI.getGravesX();
    }

    /**
     * Retrieves the singleton instance of the Graveyards plugin.
     *
     * @return The instance of the Graveyards plugin.
     */
    public static Graveyards getInstance() {
        return instance;
    }

    /**
     * Retrieves the CacheManager instance used for managing graveyard data.
     *
     * @return The CacheManager instance.
     */
    public CacheManager getCacheManager() {
        return cacheManager;
    }
}
