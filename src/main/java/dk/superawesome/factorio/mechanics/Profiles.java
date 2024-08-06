package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.mechanics.impl.*;
import dk.superawesome.factorio.mechanics.profiles.*;
import dk.superawesome.factorio.util.Array;

public class Profiles {

    public static MechanicProfile<Assembler> ASSEMBLER;
    public static MechanicProfile<Collector> COLLECTOR;
    public static MechanicProfile<Constructor> CONSTRUCTOR;
    public static MechanicProfile<EmeraldForge> EMERALD_FORGE;
    public static MechanicProfile<Generator> GENERATOR;
    public static MechanicProfile<Hopper> HOPPER;
    public static MechanicProfile<Smelter> SMELTER;
    public static MechanicProfile<StorageBox> STORAGE_BOX;
    public static MechanicProfile<PowerCentral> POWER_CENTRAL;

    static {
        profiles = new Array<>();

        ASSEMBLER     = loadProfile(new AssemblerProfile());
        COLLECTOR     = loadProfile(new CollectorProfile());
        CONSTRUCTOR   = loadProfile(new ConstructorProfile());
        EMERALD_FORGE = loadProfile(new EmeraldForgeProfile());
        GENERATOR     = loadProfile(new GeneratorProfile());
        HOPPER        = loadProfile(new HopperProfile());
        SMELTER       = loadProfile(new SmelterProfile());
        STORAGE_BOX   = loadProfile(new StorageBoxProfile());
        POWER_CENTRAL = loadProfile(new PowerCentralProfile());
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
