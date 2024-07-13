package dk.superawesome.factories.mehcanics.profiles;

import dk.superawesome.factories.mehcanics.MechanicFactory;
import dk.superawesome.factories.mehcanics.MechanicProfile;
import dk.superawesome.factories.mehcanics.impl.Constructor;
import org.bukkit.Location;

public class ConstructorProfile implements MechanicProfile<Constructor> {

    private final MechanicFactory<Constructor> factory = new Factory();

    @Override
    public int getID() {
        return 0;
    }

    @Override
    public String getName() {
        return "Constructor";
    }

    @Override
    public MechanicFactory<Constructor> getFactory() {
        return factory;
    }

    public static class Factory implements MechanicFactory<Constructor> {

        @Override
        public Constructor create(Location loc) {
            return null;
        }
    }
}
