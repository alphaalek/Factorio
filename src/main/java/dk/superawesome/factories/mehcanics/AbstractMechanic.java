package dk.superawesome.factories.mehcanics;

import dk.superawesome.factories.gui.BaseGui;
import dk.superawesome.factories.items.ItemCollection;
import dk.superawesome.factories.util.TickThrottle;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractMechanic<M extends Mechanic<M, G>, G extends BaseGui<G>> implements Mechanic<M, G>, ItemCollection {

    protected final AtomicReference<G> inUse = new AtomicReference<>();
    protected final TickThrottle tickThrottle = new TickThrottle();
    protected final Location loc;

    public AbstractMechanic(Location loc) {
        this.loc = loc;
    }

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


    @Override
    public boolean has(ItemStack stack) {
        return false;
    }

    @Override
    public List<ItemStack> take(int amount) {
        return Collections.EMPTY_LIST;
    }
}
