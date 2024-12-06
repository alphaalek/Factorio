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
import java.util.function.Predicate;

public class Excluder extends Circuit<Excluder, ItemCollection> implements ItemContainer {

    private final List<Predicate<ItemStack>> filter = new ArrayList<>();

    public Excluder(Location loc, BlockFace rotation, MechanicStorageContext context, boolean hasWallSign, boolean isBuild) {
        super(loc, rotation, context, hasWallSign, isBuild);
    }

    @Override
    public void onBlocksLoaded(Player by) {
        Filter.loadItems(filter, getSign(), by, this);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (event.getBlock().equals(getSign().getBlock())) {
            Bukkit.getScheduler().runTask(Factorio.get(), () -> Filter.loadItems(filter, getSign(), event.getPlayer(), this));
        }
    }

    @Override
    public MechanicProfile<Excluder> getProfile() {
        return Profiles.EXCLUDER;
    }

    @Override
    public boolean pipePut(ItemCollection collection) {
        for (Predicate<ItemStack> filter : this.filter) {
            if (collection.has(filter)) {
                return false;
            }
        }

        return Routes.startTransferRoute(this.loc.getBlock(), collection, this, false);
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
