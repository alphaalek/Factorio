package dk.superawesome.factorio.util;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuard {

    public static boolean canBuild(Player player, Location loc) {
        LocalPlayer lp = WorldGuardPlugin.inst().wrapPlayer(player);
        com.sk89q.worldguard.WorldGuard wg = com.sk89q.worldguard.WorldGuard.getInstance();
        if (wg.getPlatform().getSessionManager().hasBypass(lp, lp.getWorld())) {
            return true;
        }

        RegionQuery query = wg.getPlatform().getRegionContainer().createQuery();
        return query.testBuild(BukkitAdapter.adapt(loc), lp, Flags.BUILD);
    }
}
