package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.gui.BaseGui;
import dk.superawesome.factorio.gui.impl.ConstructorGui;
import dk.superawesome.factorio.gui.impl.PowerCentralGui;
import dk.superawesome.factorio.gui.impl.SmelterGui;
import dk.superawesome.factorio.gui.impl.StorageBoxGui;
import dk.superawesome.factorio.mechanics.impl.Constructor;
import dk.superawesome.factorio.mechanics.impl.PowerCentral;
import dk.superawesome.factorio.mechanics.impl.Smelter;
import dk.superawesome.factorio.mechanics.impl.StorageBox;
import dk.superawesome.factorio.mechanics.profiles.ConstructorProfile;
import dk.superawesome.factorio.mechanics.profiles.PowerCentralProfile;
import dk.superawesome.factorio.mechanics.profiles.SmelterProfile;
import dk.superawesome.factorio.mechanics.profiles.StorageBoxProfile;
import dk.superawesome.factorio.util.Array;

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
