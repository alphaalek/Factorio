package dk.superawesome.factorio.mechanics.impl.circuits;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.Circuit;
import dk.superawesome.factorio.mechanics.MechanicProfile;
import dk.superawesome.factorio.mechanics.MechanicStorageContext;
import dk.superawesome.factorio.mechanics.Profiles;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.mechanics.transfer.ItemContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Excluder extends Circuit<Excluder, ItemCollection> implements ItemContainer {

    private final List<ItemStack> filter = new ArrayList<>();

    public Excluder(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    @Override
    public void onBlocksLoaded(Player by) {
        Filter.loadItems(filter, (Sign) loc.getBlock().getRelative(rot).getState(), by, loc, this);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (event.getBlock().equals(loc.getBlock().getRelative(rot))) {
            Bukkit.getScheduler().runTask(Factorio.get(), () -> Filter.loadItems(filter, (Sign) event.getBlock().getState(), event.getPlayer(), loc, this));
        }
    }

    @Override
    public MechanicProfile<Excluder> getProfile() {
        return Profiles.EXCLUDER;
    }

    @Override
    public boolean pipePut(ItemCollection collection) {
        for (ItemStack filter : this.filter) {
            if (collection.has(filter)) {
                return false;
            }
        }

        return Routes.startTransferRoute(loc.getBlock(), collection, this, false);
    }

    @Override
    public boolean isContainerEmpty() {
        return true;
    }

    @Override
    public int getCapacity() {
        return -1;
    }
}
