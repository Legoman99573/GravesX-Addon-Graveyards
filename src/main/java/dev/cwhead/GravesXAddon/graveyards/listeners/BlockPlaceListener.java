package dev.cwhead.GravesXAddon.graveyards.listeners;

import dev.cwhead.GravesXAddon.graveyards.Graveyards;
import dev.cwhead.GravesXAddon.graveyards.util.GraveSite;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.List;
import java.util.Map;

public class BlockPlaceListener implements Listener {

    private final Graveyards plugin;


    public BlockPlaceListener(Graveyards plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block placed = event.getBlockPlaced();
        Location loc = placed.getLocation();

        if (isGraveSiteBlock(loc)) {
            event.setCancelled(true);

            event.getPlayer().sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "You cannot place blocks on a grave site.");

            plugin.getGravesX().debugMessage(
                    "Cancelled block placement on grave site at [x=" + loc.getBlockX() +
                            ", y=" + loc.getBlockY() + ", z=" + loc.getBlockZ() +
                            ", world=" + loc.getWorld().getName() + "] by " + event.getPlayer().getName(), 1
            );
        }
    }

    /**
     * Covers multi-place cases (e.g., tall plants) that place multiple blocks at once.
     * We cancel if ANY target block location is a grave site.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockMultiPlace(BlockMultiPlaceEvent event) {
        boolean shouldCancel = event.getReplacedBlockStates().stream().anyMatch(state ->
                isGraveSiteBlock(state.getLocation())
        );

        if (shouldCancel) {
            event.setCancelled(true);

            event.getPlayer().sendMessage(ChatColor.GRAY + "☠ " + ChatColor.RED + "You cannot place blocks on a grave site.");

            Location first = event.getReplacedBlockStates().stream()
                    .map(BlockState::getLocation)
                    .filter(this::isGraveSiteBlock)
                    .findFirst()
                    .orElse(null);

            if (first != null) {
                plugin.getGravesX().debugMessage(
                        "Cancelled multi-block placement on grave site at [x=" + first.getBlockX() +
                                ", y=" + first.getBlockY() + ", z=" + first.getBlockZ() +
                                ", world=" + first.getWorld().getName() + "] by " + event.getPlayer().getName(), 1
                );
            } else {
                plugin.getGravesX().debugMessage("Cancelled multi-block placement on a grave site (no loc extracted).", 1);
            }
        }
    }

    /**
     * Returns true iff the given block location is exactly one of the known grave site blocks.
     * No radius checks; world + block coords must match a configured GraveSite.
     */
    private boolean isGraveSiteBlock(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;

        final String worldName = loc.getWorld().getName();
        final int x = loc.getBlockX();
        final int y = loc.getBlockY();
        final int z = loc.getBlockZ();

        Map<String, List<GraveSite>> all = plugin.getCacheManager().getAllGraveyards();
        if (all == null || all.isEmpty()) return false;

        for (Map.Entry<String, List<GraveSite>> e : all.entrySet()) {
            List<GraveSite> sites = e.getValue();
            if (sites == null || sites.isEmpty()) continue;

            for (GraveSite site : sites) {
                Location sLoc = site.getLocation();
                if (sLoc == null || sLoc.getWorld() == null) continue;

                if (sLoc.getWorld().getName().equals(worldName)
                        && sLoc.getBlockX() == x
                        && sLoc.getBlockY() == y
                        && sLoc.getBlockZ() == z) {
                    return true;
                }
            }
        }
        return false;
    }
}
