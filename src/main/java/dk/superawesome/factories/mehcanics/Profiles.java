package dk.superawesome.factories.mehcanics;

import dk.superawesome.factories.mehcanics.impl.Constructor;
import dk.superawesome.factories.mehcanics.impl.StorageBox;
import dk.superawesome.factories.mehcanics.profiles.ConstructorProfile;
import dk.superawesome.factories.mehcanics.profiles.StorageBoxProfile;
import dk.superawesome.factories.util.Array;

public class Profiles {

    public static MechanicProfile<Constructor> CONSTRUCTOR;
    public static MechanicProfile<StorageBox> STORAGE_BOX;

    static {
        profiles = new Array<>();

        CONSTRUCTOR = loadProfile(new ConstructorProfile());
        STORAGE_BOX = loadProfile(new StorageBoxProfile());
    }

    private static final Array<MechanicProfile<?>> profiles;

    public static <M extends Mechanic<M>> MechanicProfile<M> loadProfile(MechanicProfile<M> production) {
        profiles.set(production, production);
        return production;
    }

    public static Array<MechanicProfile<?>> getProfiles() {
        return profiles;
    }
}
