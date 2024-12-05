package dk.superawesome.factorio.util.db;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Optional;

public class Types {

    public static Type<Location> LOCATION = new Type<>() {
        @Override
        public Location from(String text) {
            String[] split = text.split(";");

            String world = split[0];
            Optional<World> worldOptional = Bukkit.getWorlds()
                    .stream().filter(w -> w.getName().equals(world)).findAny();
            if (!worldOptional.isPresent()) {
                throw new RuntimeException("Failed to find world named " + world);
            }

            double x = Double.parseDouble(split[1]);
            double y = Double.parseDouble(split[2]);
            double z = Double.parseDouble(split[3]);

            if (split.length == 4) {
                return new Location(worldOptional.get(), x, y, z);
            } else {
                float yaw = Float.parseFloat(split[4]);
                float pitch = Float.parseFloat(split[5]);

                return new Location(worldOptional.get(), x, y, z, yaw, pitch);
            }
        }

        @Override
        public String convert(Location val) {
            if (val.getWorld() == null) {
                throw new RuntimeException("Unknown world!");
            }

            StringBuilder text = new StringBuilder();
            text.append(val.getWorld().getName());
            text.append(";").append(val.getX());
            text.append(";").append(val.getY());
            text.append(";").append(val.getZ());

            if (val.getPitch() > 0 || val.getYaw() > 0) {
                text.append(";").append(val.getYaw());
                text.append(";").append(val.getPitch());
            }

            return text.toString();
        }
    };
}
