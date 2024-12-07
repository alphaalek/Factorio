package dk.superawesome.factorio.commands;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.db.DatabaseConnection;
import dk.superawesome.factorio.util.db.Query;
import dk.superawesome.factorio.util.db.Types;
import dk.superawesome.factorio.util.statics.BlockUtil;
import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.regions.GeneralRegion;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Debug extends AbstractCommand {

    public static List<GeneralRegion> getRegions(Location location) {
        List<GeneralRegion> regionsList = new ArrayList<>();

        try {
            Set<ProtectedRegion> regions = AreaShop.getInstance().getWorldGuardHandler().getApplicableRegionsSet(location);
            for (ProtectedRegion region : regions) {
                if (!region.getId().equals("__global__")) {
                    GeneralRegion generalRegion = AreaShop.getInstance().getFileManager().getRegion(region.getId());
                    if (generalRegion != null) {
                        regionsList.add(generalRegion);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return regionsList;
    }

    @Override
    public void execute(Player player, String[] args) {
        if (!player.isOp()) {
            player.sendMessage("§cDu har ikke adgang til dette!");
            return;
        }

        if (args.length > 0) {
            DatabaseConnection connection = Factorio.get().getMechanicController().getConnection();
            MechanicManager manager = Factorio.get().getMechanicManager(player.getWorld());

            if (args[0].equalsIgnoreCase("delete")) {
                int id = Integer.parseInt(args[1]);

                Bukkit.getScheduler().runTaskAsynchronously(Factorio.get(), () -> {
                    Query query = new Query("DELETE FROM mechanics WHERE id = ?").add(id);
                    try {
                        query.executeUpdate(connection);
                        player.sendMessage("§cFjernede maskine " + id);

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        player.sendMessage("§cDer skete en fejl");
                    }
                });

                return;
            }
            if (args[0].equalsIgnoreCase("tp")) {
                Location loc = stringToLocation(args[1]);
                player.teleport(loc);
                return;
            }
            if (args[0].equalsIgnoreCase("find")) {
                Query query = new Query("SELECT * FROM mechanics");

                AtomicInteger called = new AtomicInteger(0);
                int max = Integer.parseInt(args[1]);

                Bukkit.getScheduler().runTaskAsynchronously(Factorio.get(), () -> {
                    try {
                        query.executeQuery(connection, r -> {
                            do {
                                if (called.get() > max) {
                                    return;
                                }

                                try {
                                    String strLoc = r.getString("location");
                                    Location loc = stringToLocation(strLoc);
                                    if (loc == null) {
                                        continue;
                                    }

                                    if (manager.getMechanicAt(loc) != null
                                            || manager.getLoadingMechanic(loc) != null) {
                                        continue;
                                    }

                                    List<GeneralRegion> regions = getRegions(loc);
                                    if (loc.getBlock().getType() == Material.AIR
                                            || regions.stream().allMatch(GeneralRegion::isAvailable)) {
                                        int id = r.getInt("id");
                                        Query delete = new Query("DELETE FROM mechanics WHERE id = ?").add(id);
                                        try {
                                            delete.executeUpdate(connection);
                                            player.sendMessage("§cFjernede maskine " + id);

                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                            player.sendMessage("§cDer skete en fejl");
                                        }

                                        called.incrementAndGet();

                                        continue;
                                    }

                                    TextComponent remove = new TextComponent("§cSlet");
                                    remove.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/debug delete " + r.getInt("id")));

                                    TextComponent tp = new TextComponent("§aTp");
                                    tp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/debug tp " + strLoc));

                                    player.spigot().sendMessage(new TextComponent("§eMechanic " + Types.LOCATION.convert(loc) + " "), remove, new TextComponent(" "), tp);

                                    called.incrementAndGet();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            } while (r.next());
                        });
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                });

                return;
            }
        }

        Block block = player.getTargetBlockExact(5);
        if (block == null) {
            player.sendMessage("§cDu kigger ikke på en block!");
            return;
        }

        Mechanic<?> mechanic = Factorio.get().getMechanicManager(player.getWorld()).getMechanicPartially(block.getLocation());
        if (mechanic == null) {
            Bukkit.getScheduler().runTaskAsynchronously(Factorio.get(), () -> {
                try {
                    MechanicStorageContext context = Factorio.get().getContextProvider().findAt(block.getLocation());

                    if (context.hasContext()) {
                        String type = context.get("type");
                        MechanicProfile<?> profile = Profiles.getProfiles().stream().filter(p -> p.getName().equals(type)).findFirst().orElseThrow(Exception::new);
                        BlockFace rotation = BlockFace.valueOf(context.get("rotation"));

                        Bukkit.getScheduler().runTask(Factorio.get(), () -> {
                            Block signBlock = block.getRelative(rotation);
                            signBlock.setType(Material.OAK_WALL_SIGN, false);
                            BlockUtil.rotate(signBlock, rotation);

                            Sign sign = (Sign) signBlock.getState();
                            sign.getSide(Side.FRONT).setLine(0, "[" + profile.getSignName() + "]");
                            sign.update();

                            Factorio.get().getMechanicManager(player.getWorld()).loadMechanic(sign, aBoolean -> {});
                        });

                        DatabaseConnection connection = Factorio.get().getMechanicController().getConnection();
                        Query has = new Query(
                                "SELECT * from block_rent_prices " +
                                "WHERE block = ? AND lobby = ? AND mechanic = 1"
                                )
                                .add(profile.getName().toLowerCase().replace(" ", "_"))
                                .add("MASKINRUMMET");

                        has.executeQuery(connection, __ -> {
                            Query query = new Query(
                                    "INSERT INTO block_rents (playerid, block, location, lobby) " +
                                    "VALUES ((SELECT p.id FROM players p WHERE p.uuid = ?), ?, ?, ?)"
                                    )
                                    .add(context.getManagement().getOwner().toString())
                                    .add(profile.getName().toLowerCase().replace(" ", "_"))
                                    .add(locationToString(block.getLocation()))
                                    .add("MASKINRUMMET");

                            int updated = query.executeUpdate(connection);
                            if (updated == 0) {
                                player.sendMessage("§cKunne ikke oprette blockrent");
                            } else {
                                player.sendMessage("§aOprettede blockrent");
                            }
                        });

                        player.sendMessage("§eFandt " + profile.getName());
                    } else {
                        player.sendMessage("§cDu skal kigge på en maskine!");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            return;
        }

        player.sendMessage("§e" + mechanic.getProfile().getName() + " Lvl " + mechanic.getLevel());
        Class<?> clazz = mechanic.getClass();
        try {
            while (clazz != Object.class) {
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    Object val = field.get(mechanic);
                    player.sendMessage(field.getName() + ": §e" + (val instanceof Object[] ? Arrays.toString((Object[]) val) : val));
                }

                clazz = clazz.getSuperclass();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String locationToString(Location loc) {
        return loc.getWorld().getName() + ";" + Math.round(loc.getX()) + ";" + Math.round(loc.getY()) + ";" + Math.round(loc.getZ()) + ";" + loc.getYaw() + ";" + loc.getPitch();
    }

    public static Location stringToLocation(String string) {
        String[] parts = string.split(";");
        if (parts.length < 4) {
            return new Location(Bukkit.getWorld("world"), 0, 0, 0);
        }

        World world = Bukkit.getWorld(parts[0]);
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        if (parts.length == 4) {
            return new Location(world, x, y, z);
        } else if (parts.length == 6) {
            float yaw = Float.parseFloat(parts[4]);
            float pitch = Float.parseFloat(parts[5]);

            return new Location(world, x, y, z, yaw, pitch);
        } else {
            return null;
        }
    }
}
