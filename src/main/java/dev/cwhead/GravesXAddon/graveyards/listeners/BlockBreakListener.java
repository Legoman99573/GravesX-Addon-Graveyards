package dev.cwhead.GravesXAddon.graveyards.listeners;

import dev.cwhead.GravesXAddon.graveyards.Graveyards;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class BlockBreakListener implements Listener {

    private final Graveyards plugin;
    private final NamespacedKey graveHeadKey;

    public BlockBreakListener(Graveyards plugin) {
        this.plugin = plugin;
        this.graveHeadKey = new NamespacedKey(plugin, "GraveyardHead");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        BlockState state = block.getState();

        if (!(state instanceof Skull skull)) return;

        PersistentDataContainer pdc = skull.getPersistentDataContainer();
        Byte marker = pdc.get(graveHeadKey, PersistentDataType.BYTE);

        if (marker == null || marker != (byte) 1) return;

        event.setCancelled(true);

        event.getPlayer().sendMessage("§7☠ §cYou cannot break a grave head.");

        plugin.getGravesX().debugMessage(
                "Cancelled break of protected grave head at [x=" + block.getX() +
                        ", y=" + block.getY() + ", z=" + block.getZ() + ", world=" +
                        block.getWorld().getName() + "] by " + event.getPlayer().getName(), 1
        );
    }
}
