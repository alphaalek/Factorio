package dk.superawesome.factories.mechanics;

import dk.superawesome.factories.gui.BaseGui;
import dk.superawesome.factories.gui.impl.ConstructorGui;
import dk.superawesome.factories.gui.impl.PowerCentralGui;
import dk.superawesome.factories.gui.impl.SmelterGui;
import dk.superawesome.factories.gui.impl.StorageBoxGui;
import dk.superawesome.factories.mechanics.impl.Constructor;
import dk.superawesome.factories.mechanics.impl.PowerCentral;
import dk.superawesome.factories.mechanics.impl.Smelter;
import dk.superawesome.factories.mechanics.impl.StorageBox;
import dk.superawesome.factories.mechanics.profiles.ConstructorProfile;
import dk.superawesome.factories.mechanics.profiles.PowerCentralProfile;
import dk.superawesome.factories.mechanics.profiles.SmelterProfile;
import dk.superawesome.factories.mechanics.profiles.StorageBoxProfile;
import dk.superawesome.factories.util.Array;

public class Profiles {

    public static MechanicProfile<Constructor, ConstructorGui> CONSTRUCTOR;
    public static MechanicProfile<Smelter, SmelterGui> SMELTER;
    public static MechanicProfile<StorageBox, StorageBoxGui> STORAGE_BOX;
    public static MechanicProfile<PowerCentral, PowerCentralGui> POWER_CENTRAL;

    static {
        profiles = new Array<>();

        CONSTRUCTOR   = loadProfile(new ConstructorProfile());
        SMELTER       = loadProfile(new SmelterProfile());
        STORAGE_BOX   = loadProfile(new StorageBoxProfile());
        POWER_CENTRAL = loadProfile(new PowerCentralProfile());
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
