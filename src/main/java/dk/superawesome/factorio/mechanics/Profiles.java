package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.gui.BaseGui;
import dk.superawesome.factorio.gui.impl.*;
import dk.superawesome.factorio.mechanics.impl.*;
import dk.superawesome.factorio.mechanics.profiles.*;
import dk.superawesome.factorio.util.Array;

public class Profiles {

    public static MechanicProfile<Assembler, AssemblerGui> ASSEMBLER;
    public static MechanicProfile<Constructor, ConstructorGui> CONSTRUCTOR;
    public static MechanicProfile<EmeraldForge, EmeraldForgeGui> EMERALD_FORGE;
    public static MechanicProfile<Generator, GeneratorGui> GENERATOR;
    public static MechanicProfile<Smelter, SmelterGui> SMELTER;
    public static MechanicProfile<StorageBox, StorageBoxGui> STORAGE_BOX;
    public static MechanicProfile<PowerCentral, PowerCentralGui> POWER_CENTRAL;

    static {
        profiles = new Array<>();

        ASSEMBLER     = loadProfile(new AssemblerProfile());
        CONSTRUCTOR   = loadProfile(new ConstructorProfile());
        EMERALD_FORGE = loadProfile(new EmeraldForgeProfile());
        GENERATOR     = loadProfile(new GeneratorProfile());
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
