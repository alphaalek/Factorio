package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.mechanics.impl.behaviour.*;
import dk.superawesome.factorio.mechanics.impl.circuits.*;
import dk.superawesome.factorio.mechanics.profiles.behaviour.*;
import dk.superawesome.factorio.mechanics.profiles.circuits.*;
import dk.superawesome.factorio.util.Array;

public class Profiles {

    public static final MechanicProfile<Assembler> ASSEMBLER;
    public static final MechanicProfile<Collector> COLLECTOR;
    public static final MechanicProfile<Comparator> COMPARATOR;
    public static final MechanicProfile<Constructor> CONSTRUCTOR;
    public static final MechanicProfile<EmeraldForge> EMERALD_FORGE;
    public static final MechanicProfile<Filter> FILTER;
    public static final MechanicProfile<Gate> GATE;
    public static final MechanicProfile<Generator> GENERATOR;
    public static final MechanicProfile<Hopper> HOPPER;
    public static final MechanicProfile<Smelter> SMELTER;
    public static final MechanicProfile<Splitter> SPLITTER;
    public static final MechanicProfile<StorageBox> STORAGE_BOX;
    public static final MechanicProfile<PowerCentral> POWER_CENTRAL;
    public static final MechanicProfile<Refinery> REFINERY;
    public static final MechanicProfile<Cauldron> CAULDRON;
    public static final MechanicProfile<AssemblerTrigger> ASSEMBLER_TRIGGER;

    static {
        profiles = new Array<>();

        ASSEMBLER         = loadProfile(new AssemblerProfile());
        COLLECTOR         = loadProfile(new CollectorProfile());
        COMPARATOR        = loadProfile(new ComparatorProfile());
        CONSTRUCTOR       = loadProfile(new ConstructorProfile());
        EMERALD_FORGE     = loadProfile(new EmeraldForgeProfile());
        FILTER            = loadProfile(new FilterProfile());
        GATE              = loadProfile(new GateProfile());
        GENERATOR         = loadProfile(new GeneratorProfile());
        HOPPER            = loadProfile(new HopperProfile());
        SMELTER           = loadProfile(new SmelterProfile());
        SPLITTER          = loadProfile(new SplitterProfile());
        STORAGE_BOX       = loadProfile(new StorageBoxProfile());
        POWER_CENTRAL     = loadProfile(new PowerCentralProfile());
        REFINERY          = loadProfile(new RefineryProfile());
        CAULDRON          = loadProfile(new CauldronProfile());
        ASSEMBLER_TRIGGER = loadProfile(new AssemblerTriggerProfile());
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
