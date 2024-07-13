package dk.superawesome.factories.mehcanics.impl;

import dk.superawesome.factories.items.ItemCollection;
import dk.superawesome.factories.mehcanics.Mechanic;
import dk.superawesome.factories.mehcanics.MechanicProfile;
import dk.superawesome.factories.mehcanics.Profiles;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public class Constructor implements Mechanic<Constructor> {

    private final Location loc;

    public Constructor(Location loc) {
        this.loc = loc;
    }

    @Override
    public Location getLocation() {
        return loc;
    }

    @Override
    public MechanicProfile<Constructor> getProfile() {
        return Profiles.CONSTRUCTOR;
    }

    @Override
    public void pipePut(ItemCollection collection) {
        Bukkit.getLogger().info("Putting items into " + getProfile().getName() + " " + collection);
    }
}
