package dev.cwhead.GravesXAddon.type;

import org.bukkit.Location;

/**
 * Represents a graveyard area defined by two corner positions.
 * Each graveyard has a unique name and a defined area within two {@link Location} points.
 */
public class Graveyard {

    private final String name;
    private final Location pos1;
    private final Location pos2;

    /**
     * Constructs a Graveyard with the specified name and corner positions.
     *
     * @param name the unique name of the graveyard.
     * @param pos1 the first corner {@link Location} of the graveyard.
     * @param pos2 the second corner {@link Location} of the graveyard.
     */
    public Graveyard(String name, Location pos1, Location pos2) {
        this.name = name;
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    /**
     * Gets the name of the graveyard.
     *
     * @return the name of the graveyard.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the first corner location of the graveyard area.
     *
     * @return the first {@link Location} corner.
     */
    public Location getPos1() {
        return pos1;
    }

    /**
     * Gets the second corner location of the graveyard area.
     *
     * @return the second {@link Location} corner.
     */
    public Location getPos2() {
        return pos2;
    }
}