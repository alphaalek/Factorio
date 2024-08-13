package dk.superawesome.factorio.mechanics.impl.circuits;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.AbstractMechanic;
import dk.superawesome.factorio.mechanics.MechanicProfile;
import dk.superawesome.factorio.mechanics.MechanicStorageContext;
import dk.superawesome.factorio.mechanics.Profiles;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.mechanics.transfer.ItemContainer;
import dk.superawesome.factorio.util.Array;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MinecraftFont;

import java.util.*;

public class Filter extends AbstractMechanic<Filter> implements ItemContainer {

    private final List<ItemStack> filter = new ArrayList<>();

    public Filter(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    @Override
    public void onBlocksLoaded() {
        Block block = loc.getBlock().getRelative(getRotation());
        loadItems((Sign) block.getState());
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Bukkit.getScheduler().runTask(Factorio.get(), () -> loadItems((Sign) event.getBlock().getState()));
    }

    private void loadItems(Sign sign) {
        // get all lines except the first
        for (String line : Arrays.copyOfRange(sign.getSide(Side.FRONT).getLines(), 1, 4)) {
            Arrays.stream(line.split(","))
                    .map(String::trim)
                    .map(this::findItem)
                    .forEach(filter::add);
        }
        filter.removeAll(Collections.singletonList(null));

        if (filter.isEmpty()) {
            Factorio.get().getMechanicManager(loc.getWorld()).unload(this);
            Buildings.remove(loc.getWorld(), this);

            Player owner = Bukkit.getPlayer(management.getOwner());
            if (owner != null) {
                owner.sendMessage("§cUgyldig item valgt!");
            }
        } else {
            // get all lines except the first
            for (int i = 1; i < 4; i++) {
                String line = sign.getSide(Side.FRONT).getLine(i);
                StringBuilder builder = new StringBuilder();
                Arrays.stream(line.split(","))
                        .map(String::trim)
                        .map(this::findItem)
                        .filter(Objects::nonNull)
                        .peek(__ -> builder.append(","))
                        .forEach(item -> builder.append(item.getType().name().toLowerCase()));

                if (!builder.isEmpty()) {
                    sign.getSide(Side.FRONT).setLine(i, builder.substring(1));
                } else if (!line.isEmpty())  {
                    sign.getSide(Side.FRONT).setLine(i, "");
                }
            }

            sign.update();
        }
    }


    private ItemStack findItem(String name) {
        return Arrays.stream(Material.values())
                .filter(m -> m.name().equalsIgnoreCase(name))
                .findFirst()
                .map(ItemStack::new)
                .orElse(null);
    }

    @Override
    public MechanicProfile<Filter> getProfile() {
        return Profiles.FILTER;
    }

    @Override
    public boolean isContainerEmpty() {
        return true;
    }

    @Override
    public void pipePut(ItemCollection collection, PipePutEvent event) {
        for (ItemStack filter : this.filter) {
            if (collection.has(filter)) {
                if (Routes.startTransferRoute(loc.getBlock(), collection, false)) {
                    event.setTransferred(true);
                }
                return;
            }
        }
    }

    @Override
    public int getCapacity() {
        return -1;
    }
}
