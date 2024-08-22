package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.mechanics.impl.behaviour.*;
import dk.superawesome.factorio.mechanics.impl.circuits.*;
import dk.superawesome.factorio.mechanics.profiles.behaviour.*;
import dk.superawesome.factorio.mechanics.profiles.behaviour.LiquidTankProfile;
import dk.superawesome.factorio.mechanics.profiles.circuits.*;
import dk.superawesome.factorio.util.Array;

public class Profiles {

    public static final MechanicProfile<Assembler> ASSEMBLER;
    public static final MechanicProfile<AssemblerTrigger> ASSEMBLER_TRIGGER;
    public static final MechanicProfile<Cauldron> CAULDRON;
    public static final MechanicProfile<Collector> COLLECTOR;
    public static final MechanicProfile<Comparator> COMPARATOR;
    public static final MechanicProfile<Constructor> CONSTRUCTOR;
    public static final MechanicProfile<EmeraldForge> EMERALD_FORGE;
    public static final MechanicProfile<Filter> FILTER;
    public static final MechanicProfile<Gate> GATE;
    public static final MechanicProfile<Generator> GENERATOR;
    public static final MechanicProfile<Hopper> HOPPER;
    public static final MechanicProfile<LiquidTank> LIQUID_TANK;
    public static final MechanicProfile<Smelter> SMELTER;
    public static final MechanicProfile<Splitter> SPLITTER;
    public static final MechanicProfile<StorageBox> STORAGE_BOX;
    public static final MechanicProfile<PowerCentral> POWER_CENTRAL;
    public static final MechanicProfile<Refinery> REFINERY;

    static {
        profiles = new Array<>();

        ASSEMBLER         = loadProfile(new AssemblerProfile());            /* ID: 4 */
        ASSEMBLER_TRIGGER = loadProfile(new AssemblerTriggerProfile());     /* ID: 15 */
        CAULDRON          = loadProfile(new CauldronProfile());             /* ID: 14 */
        COLLECTOR         = loadProfile(new CollectorProfile());            /* ID: 7 */
        COMPARATOR        = loadProfile(new ComparatorProfile());           /* ID: 12 */
        CONSTRUCTOR       = loadProfile(new ConstructorProfile());          /* ID: 0 */
        EMERALD_FORGE     = loadProfile(new EmeraldForgeProfile());         /* ID: 5 */
        FILTER            = loadProfile(new FilterProfile());               /* ID: 11 */
        GATE              = loadProfile(new GateProfile());                 /* ID: 9 */
        GENERATOR         = loadProfile(new GeneratorProfile());            /* ID: 6 */
        HOPPER            = loadProfile(new HopperProfile());               /* ID: 8 */
        LIQUID_TANK       = loadProfile(new LiquidTankProfile());           /* ID: 16 */
        SMELTER           = loadProfile(new SmelterProfile());              /* ID: 1 */
        SPLITTER          = loadProfile(new SplitterProfile());             /* ID: 10 */
        STORAGE_BOX       = loadProfile(new StorageBoxProfile());           /* ID: 2 */
        POWER_CENTRAL     = loadProfile(new PowerCentralProfile());         /* ID: 3 */
        REFINERY          = loadProfile(new RefineryProfile());             /* ID: 13 */
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
