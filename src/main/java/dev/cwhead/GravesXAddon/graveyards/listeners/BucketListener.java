package dev.cwhead.GravesXAddon.graveyards.listeners;

import dev.cwhead.GravesXAddon.graveyards.Graveyards;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class BucketListener implements Listener {


    private final Graveyards plugin;
    private final NamespacedKey graveHeadKey;

    public BucketListener(Graveyards plugin) {
        this.plugin = plugin;
        this.graveHeadKey = new NamespacedKey(plugin, "GraveyardHead");
    }

    /**
     * Prevent players from placing water directly on or next to grave heads.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Material bucket = event.getBucket();
        if (bucket != Material.WATER_BUCKET && bucket != Material.BUCKET) return;

        Block target = event.getBlockClicked().getRelative(event.getBlockFace());
        if (isProtectedGraveHead(target)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§7☠ §cYou cannot pour water on a grave head.");
            plugin.getGravesX().debugMessage(
                    "Cancelled water placement on protected grave head at [x=" + target.getX() +
                            ", y=" + target.getY() + ", z=" + target.getZ() + ", world=" +
                            target.getWorld().getName() + "] by " + event.getPlayer().getName(),
                    1
            );
        }
    }

    /**
     * Prevent flowing water from dislodging grave heads.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        Block toBlock = event.getToBlock();
        if (isProtectedGraveHead(toBlock)) {
            event.setCancelled(true);
            plugin.getGravesX().debugMessage(
                    "Cancelled water flow into protected grave head at [x=" + toBlock.getX() +
                            ", y=" + toBlock.getY() + ", z=" + toBlock.getZ() + ", world=" +
                            toBlock.getWorld().getName() + "]",
                    2
            );
        }
    }

    /**
     * Checks whether a block is a tagged grave head.
     */
    private boolean isProtectedGraveHead(Block block) {
        BlockState state = block.getState();
        if (!(state instanceof Skull skull)) return false;

        PersistentDataContainer pdc = skull.getPersistentDataContainer();
        Byte marker = pdc.get(graveHeadKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

}
