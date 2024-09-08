package dk.superawesome.factorio.mechanics.impl.circuits;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.building.Buildings;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.accessible.Smelter;
import dk.superawesome.factorio.mechanics.routes.Routes;
import dk.superawesome.factorio.mechanics.stackregistry.Fuel;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.mechanics.transfer.ItemContainer;
import dk.superawesome.factorio.util.MaterialTags;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Predicate;

public class Filter extends Circuit<Filter, ItemCollection> implements ItemContainer {

    private final List<Predicate<ItemStack>> filter = new ArrayList<>();

    public Filter(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
    }

    @Override
    public void onBlocksLoaded(Player by) {
        loadItems(filter, (Sign) this.loc.getBlock().getRelative(this.rot).getState(), by, this);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (event.getBlock().equals(this.loc.getBlock().getRelative(this.rot))) {
            Bukkit.getScheduler().runTask(Factorio.get(), () -> loadItems(filter, (Sign) event.getBlock().getState(), event.getPlayer(), this));
        }
    }

    public static void loadItems(List<Predicate<ItemStack>> filter, Sign sign, Player by, Mechanic<?> mechanic) {
        // remove previous filter items if any present
        filter.clear();

        // get all lines except the first
        for (String line : Arrays.copyOfRange(sign.getSide(Side.FRONT).getLines(), 1, 4)) {
            Arrays.stream(line.split(","))
                    .map(String::trim)
                    .map(s -> findFilter(new Stack<>(), s))
                    .forEach(filter::add);
        }
        filter.removeAll(Collections.singletonList(null));

        if (filter.isEmpty()) {
            Factorio.get().getMechanicManagerFor(mechanic).unload(mechanic);
            Buildings.remove(mechanic, mechanic.getLocation(), mechanic.getRotation(), true);

            if (by != null) {
                by.sendMessage("§cUgyldig item valgt!");
            }
        } else {
            // get all lines except the first
            for (int i = 1; i < 4; i++) {
                String line = sign.getSide(Side.FRONT).getLine(i);
                StringBuilder builder = new StringBuilder();
                Stack<String> stack = new Stack<>();
                Arrays.stream(line.split(","))
                        .map(String::trim)
                        .map(s -> findFilter(stack, s))
                        .filter(Objects::nonNull)
                        .peek(__ -> builder.append(","))
                        .forEach(item -> builder.append(stack.pop()));

                if (!builder.isEmpty()) {
                    sign.getSide(Side.FRONT).setLine(i, builder.substring(1));
                } else if (!line.isEmpty())  {
                    sign.getSide(Side.FRONT).setLine(i, "");
                }
            }

            sign.update();
        }
    }

    public static Predicate<ItemStack> findFilter(Stack<String> stack, String name) {
        stack.push(name.toLowerCase());
        if (name.equalsIgnoreCase("fuel")) {
            return i -> Fuel.getType(i.getType()).isPresent();
        } else if (name.equalsIgnoreCase("smeltable")) {
            return Smelter::canSmeltStatic;
        }

        return findItem(stack, name).map(i -> (Predicate<ItemStack>) i::isSimilar)
                .orElse(findTag(stack, name).map(tag -> (Predicate<ItemStack>) item -> tag.isTagged(item.getType()))
                        .orElse(null)
                );
    }

    public static Optional<ItemStack> findItem(Stack<String> stack, String name) {
        return Arrays.stream(Material.values())
                .filter(m -> m.name().equalsIgnoreCase(name))
                .findFirst()
                .map(ItemStack::new);
    }

    public static Optional<Tag<Material>> findTag(Stack<String> stack, String name) {
        if (name.startsWith("tag:")) {
            return MaterialTags.LIST.entrySet().stream()
                    .filter(t -> t.getKey().equalsIgnoreCase(name.substring(4)))
                    .peek(t -> stack.push("tag:" + t.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst();
        } else {
            return Optional.empty();
        }
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
    public boolean pipePut(ItemCollection collection) {
        for (Predicate<ItemStack> filter : this.filter) {
            if (collection.has(filter)) {
                return Routes.startTransferRoute(loc.getBlock(), collection, this, false);
            }
        }

        return false;
    }

    @Override
    public int getCapacity() {
        return -1;
    }
}
