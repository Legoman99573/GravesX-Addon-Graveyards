package dev.cwhead.GravesXAddon.graveyards.listeners;

import dev.cwhead.GravesXAddon.graveyards.Graveyards;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Iterator;

public class ExplosionListener implements Listener {


    private final Graveyards plugin;
    private final NamespacedKey graveHeadKey;

    public ExplosionListener(Graveyards plugin) {
        this.plugin = plugin;
        this.graveHeadKey = new NamespacedKey(plugin, "GraveyardHead");
    }

    /**
     * Protects tagged grave heads from entity/mob-caused explosions.
     * Covers creepers, withers, primed TNT, TNT minecarts, wither skulls, etc.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        int protectedCount = stripProtectedHeads(event.blockList().iterator());
        if (protectedCount > 0) {
            plugin.getGravesX().debugMessage(
                    "Protected " + protectedCount + " grave head block(s) from entity explosion at " +
                            event.getLocation(), 1
            );
        }
    }

    /**
     * OPTIONAL: Protects tagged grave heads from block-sourced explosions
     * like beds and respawn anchors.
     * Remove this handler if you ONLY want entity/mob protection.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        int protectedCount = stripProtectedHeads(event.blockList().iterator());
        if (protectedCount > 0) {
            plugin.getGravesX().debugMessage(
                    "Protected " + protectedCount + " grave head block(s) from block explosion at " +
                            event.getBlock().getLocation(), 1
            );
        }
    }

    /**
     * Removes protected skull blocks from the explosion's destruction list.
     * Returns how many were removed, for debug logging.
     */
    private int stripProtectedHeads(Iterator<Block> it) {
        int protectedCount = 0;
        while (it.hasNext()) {
            Block b = it.next();
            BlockState state = b.getState();
            if (!(state instanceof Skull skull)) continue;

            PersistentDataContainer pdc = skull.getPersistentDataContainer();
            Byte marker = pdc.get(graveHeadKey, PersistentDataType.BYTE);
            if (marker != null && marker == (byte) 1) {
                it.remove();
                protectedCount++;

                plugin.getGravesX().debugMessage(
                        "Explosion protection: preserved grave head at [x=" + b.getX() +
                                ", y=" + b.getY() + ", z=" + b.getZ() + ", world=" + b.getWorld().getName() + "]",
                        2
                );
            }
        }
        return protectedCount;
    }

}
