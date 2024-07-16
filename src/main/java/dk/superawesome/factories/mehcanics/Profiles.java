package dk.superawesome.factories.mehcanics;

import dk.superawesome.factories.gui.BaseGui;
import dk.superawesome.factories.gui.impl.ConstructorGui;
import dk.superawesome.factories.gui.impl.SmelterGui;
import dk.superawesome.factories.gui.impl.StorageBoxGui;
import dk.superawesome.factories.mehcanics.impl.Constructor;
import dk.superawesome.factories.mehcanics.impl.Smelter;
import dk.superawesome.factories.mehcanics.impl.StorageBox;
import dk.superawesome.factories.mehcanics.profiles.ConstructorProfile;
import dk.superawesome.factories.mehcanics.profiles.SmelterProfile;
import dk.superawesome.factories.mehcanics.profiles.StorageBoxProfile;
import dk.superawesome.factories.util.Array;

public class Profiles {

    public static MechanicProfile<Constructor, ConstructorGui> CONSTRUCTOR;
    public static MechanicProfile<Smelter, SmelterGui> SMELTER;
    public static MechanicProfile<StorageBox, StorageBoxGui> STORAGE_BOX;

    static {
        profiles = new Array<>();

        CONSTRUCTOR = loadProfile(new ConstructorProfile());
        SMELTER     = loadProfile(new SmelterProfile());
        STORAGE_BOX = loadProfile(new StorageBoxProfile());
    }

    private static final Array<MechanicProfile<?, ?>> profiles;

    public static <M extends Mechanic<M, G>, G extends BaseGui<G>> MechanicProfile<M, G> loadProfile(MechanicProfile<M, G> production) {
        profiles.set(production, production);
        return production;
    }

    public static Array<MechanicProfile<?, ?>> getProfiles() {
        return profiles;
    }
}
