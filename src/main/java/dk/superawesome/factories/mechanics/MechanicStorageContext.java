package dk.superawesome.factories.mechanics;


import org.bukkit.Location;

public class MechanicStorageContext {

    public static MechanicStorageContext DEFAULT = new MechanicStorageContext();

    public static MechanicStorageContext findAt(Location loc) {
        return DEFAULT;
    }

    public int getLevel() {
        return 1;
    }
}
