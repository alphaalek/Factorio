package dk.superawesome.factories.mehcanics;

import dk.superawesome.factories.gui.BaseGui;
import dk.superawesome.factories.gui.MechanicGui;
import dk.superawesome.factories.gui.impl.StorageBoxGui;
import dk.superawesome.factories.items.ItemCollection;
import dk.superawesome.factories.util.TickThrottle;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public abstract class AbstractMechanic<M extends Mechanic<M, G>, G extends BaseGui<G>> implements Mechanic<M, G>, ItemCollection {

    protected final AtomicReference<G> inUse = new AtomicReference<>();
    protected final TickThrottle tickThrottle = new TickThrottle();
    protected final Location loc;

    public AbstractMechanic(Location loc) {
        this.loc = loc;
    }

    @Override
    public TickThrottle getTickThrottle() {
        return tickThrottle;
    }

    @Override
    public int getLevel() {
        return 1;
    }

    @Override
    public Location getLocation() {
        return loc;
    }

    @Override
    public void openInventory(Player player) {
        G inUse = this.inUse.get();
        if (inUse != null) {
            player.openInventory(inUse.getInventory());
            return;
        }

        G gui = getProfile().getGuiFactory().create((M) this, this.inUse);
        player.openInventory(gui.getInventory());
    }

    protected List<ItemStack> take(int amount, ItemStack stored, int storedAmount, AtomicInteger takenUpdate, Consumer<G> doGui) {
        List<ItemStack> items = new ArrayList<>();
        int taken = 0;
        while (taken < amount && taken < storedAmount) {
            ItemStack item = stored.clone();
            int a = Math.min(item.getMaxStackSize(), Math.min(storedAmount, amount) - taken);

            taken += a;
            item.setAmount(a);
            items.add(item);
        }

        takenUpdate.addAndGet(taken);
        G gui = inUse.get();
        if (gui != null) {
            doGui.accept(gui);
        }

        return items;
    }
}
