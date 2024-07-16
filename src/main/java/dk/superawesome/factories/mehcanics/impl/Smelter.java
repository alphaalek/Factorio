package dk.superawesome.factories.mehcanics.impl;

import dk.superawesome.factories.gui.impl.SmelterGui;
import dk.superawesome.factories.items.ItemCollection;
import dk.superawesome.factories.mehcanics.AbstractMechanic;
import dk.superawesome.factories.mehcanics.MechanicProfile;
import dk.superawesome.factories.mehcanics.Profiles;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class Smelter extends AbstractMechanic<Smelter, SmelterGui> {

    public Smelter(Location loc) {
        super(loc);
    }

    @Override
    public MechanicProfile<Smelter, SmelterGui> getProfile() {
        return Profiles.SMELTER;
    }

    @Override
    public void pipePut(ItemCollection collection) {

    }

    @Override
    public boolean has(ItemStack stack) {
        return false;
    }

    @Override
    public List<ItemStack> take(int amount) {
        return Collections.EMPTY_LIST;
    }
}
