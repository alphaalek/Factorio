package dk.superawesome.factorio.gui;

import dk.superawesome.factorio.util.Callback;
import dk.superawesome.factorio.util.helper.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public abstract class PaginatedGui<G extends BaseGui<G>, T> extends BaseGuiAdapter<G> {

    private int currentPage = 1;
    private final int pageSize;
    private final int pageRows;

    public PaginatedGui(Supplier<Callback> initCallback, AtomicReference<G> inUseReference, int size, String title, boolean cancel, int pageSize) {
        super(initCallback, inUseReference, size, title, cancel);
        this.pageSize = pageSize;

        double ratio = pageSize / 9d;
        this.pageRows = (int) (9 * (ratio % 1 == 0 ? ratio : Math.ceil(ratio)));

        if (pageSize > pageRows) {
            throw new IllegalArgumentException("Page size greater than page rows");
        }
    }

    @Override
    public void loadItems() {
        IntStream.range(pageRows, getInventory().getSize()).forEach(i -> getInventory().setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE)));

        loadView();
        super.loadItems();
    }

    public abstract List<T> getValues();

    public abstract ItemStack getItemFrom(T val);

    protected void loadView() {
        // clear inventory view for new items
        IntStream.range(0, pageRows).forEach(i -> getInventory().setItem(i, null));

        // evaluate pages
        int skip = Math.max(0, currentPage - 1) * pageSize;
        int i = 0;
        int displayIndex = 0;
        List<T> values = getValues();
        while (i < values.size()) {
            T val = values.get(i);
            // skip first pages
            if (i++ < skip || val == null) {
                continue;
            }

            // show until page ends
            if (displayIndex++ > pageSize - 1) {
                break;
            }

            getInventory().setItem(displayIndex - 1, getItemFrom(val));
        }

        // set arrows for previous/next page buttons
        getInventory().setItem(pageRows, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        getInventory().setItem(pageRows + 8, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));

        if (values.size() > i) {
            getInventory().setItem(pageRows + 8, new ItemBuilder(Material.ARROW).setName("§eNæste side").build());
        }
        if (currentPage > 1) {
            getInventory().setItem(pageRows, new ItemBuilder(Material.ARROW).setName("§eForrige side").build());
        }
    }

    @Override
    public boolean onClickIn(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        // scroll pages
        if (item != null && item.getType() == Material.ARROW) {
            if (event.getSlot() % 9 == 0) {
                currentPage--;
            } else if (event.getSlot() % 9 == 8) {
                currentPage++;
            }

            loadView();
            return true;
        }

        return false;
    }
}
