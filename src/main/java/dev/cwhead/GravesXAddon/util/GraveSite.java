package dev.cwhead.GravesXAddon.util;

import org.bukkit.Location;
import org.bukkit.block.Block;

/**
 * Represents a single grave site within a graveyard.
 * Provides location information and occupancy status of the grave site.
 */
public class GraveSite {
    private final Location location;
    private boolean occupied;

    /**
     * Constructs a GraveSite at the specified location and occupancy status.
     *
     * @param location the {@link Location} of the grave site.
     * @param occupied the occupancy status of the grave site.
     */
    public GraveSite(Location location, boolean occupied) {
        this.location = location;
        this.occupied = occupied;
    }

    /**
     * Gets the {@link Location} of this grave site.
     *
     * @return the location of the grave site.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Gets the {@link Block} at this grave site's location.
     *
     * @return the block at the grave site's location.
     */
    public Block getBlock() {
        return location.getBlock();
    }

    /**
     * Gets the X-coordinate of the block at this grave site's location.
     *
     * @return the block X-coordinate.
     */
    public int getBlockX() {
        return location.getBlockX();
    }

    /**
     * Gets the Y-coordinate of the block at this grave site's location.
     *
     * @return the block Y-coordinate.
     */
    public int getBlockY() {
        return location.getBlockY();
    }

    /**
     * Gets the Z-coordinate of the block at this grave site's location.
     *
     * @return the block Z-coordinate.
     */
    public int getBlockZ() {
        return location.getBlockZ();
    }

    /**
     * Gets the X-coordinate of this grave site's location in double precision.
     *
     * @return the X-coordinate in double precision.
     * @deprecated Use {@link #getBlockX()} instead for block-level precision.
     */
    @Deprecated
    public double getX() {
        return location.getBlock().getX();
    }

    /**
     * Gets the Y-coordinate of this grave site's location in double precision.
     *
     * @return the Y-coordinate in double precision.
     * @deprecated Use {@link #getBlockY()} instead for block-level precision.
     */
    @Deprecated
    public double getY() {
        return location.getBlock().getY();
    }

    /**
     * Gets the Z-coordinate of this grave site's location in double precision.
     *
     * @return the Z-coordinate in double precision.
     * @deprecated Use {@link #getBlockZ()} instead for block-level precision.
     */
    @Deprecated
    public double getZ() {
        return location.getBlock().getZ();
    }

    /**
     * Checks if the grave site is currently occupied.
     *
     * @return true if the grave site is occupied; false otherwise.
     */
    public boolean isOccupied() {
        return occupied;
    }

    /**
     * Sets the occupancy status of the grave site.
     *
     * @param occupied true to mark the site as occupied; false to mark as unoccupied.
     */
    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }
}