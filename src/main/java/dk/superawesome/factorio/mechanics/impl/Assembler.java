package dk.superawesome.factorio.mechanics.impl;

import dk.superawesome.factorio.gui.impl.AssemblerGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.transfer.Container;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.mechanics.transfer.ItemContainer;
import dk.superawesome.factorio.mechanics.transfer.MoneyCollection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;

public class Assembler extends AbstractMechanic<Assembler, AssemblerGui> implements ThinkingMechanic, ItemContainer, MoneyCollection {

    private final ThinkDelayHandler thinkDelayHandler = new ThinkDelayHandler(20);
    private Types type;
    private int ingredientAmount;
    private int moneyAmount;

    public Assembler(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
        loadFromStorage();
    }

    @Override
    public void load(MechanicStorageContext context) throws SQLException {
        ByteArrayInputStream data = context.getData();
        ItemStack item = context.getSerializer().readItemStack(data);
        if (item != null) {
            this.type = Types.getType(item.getType()).orElse(null);
        }
    }

    public void save(MechanicStorageContext context) throws SQLException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        context.getSerializer().writeItemStack(stream,
                Optional.ofNullable(this.type)
                        .map(Types::getMat)
                        .map(ItemStack::new)
                        .orElse(null));

        context.uploadData(stream);
    }

    @Override
    public MechanicProfile<Assembler, AssemblerGui> getProfile() {
        return Profiles.ASSEMBLER;
    }

    @Override
    public ThinkDelayHandler getDelayHandler() {
        return thinkDelayHandler;
    }

    @Override
    public void think() {

    }

    @Override
    public boolean isContainerEmpty() {
        return ingredientAmount == 0;
    }

    @Override
    public void pipePut(ItemCollection collection) {
        ItemStack item = Optional.ofNullable(type).map(Types::getMat).map(ItemStack::new).orElse(null);
        if (item == null || collection.has(item)) {
            ingredientAmount += put(collection, Math.min(64, getCapacity() - ingredientAmount), inUse, AssemblerGui::updateAddedIngredients, new Updater<ItemStack>() {
                @Override
                public ItemStack get() {
                    return item;
                }

                @Override
                public void set(ItemStack val) {
                    type = Types.getType(val.getType()).orElse(null);
                }
            });
        }
    }

    @Override
    public int getCapacity() {
        return level.getInt(ItemCollection.CAPACITY_MARK);
    }

    public Types getType() {
        return this.type;
    }

    public void setType(Types type) {
        this.type = type;
    }

    public int getIngredientAmount() {
        return this.ingredientAmount;
    }

    public void setIngredientAmount(int amount) {
        this.ingredientAmount = amount;
    }

    public int getMoneyAmount() {
        return this.moneyAmount;
    }

    public void setMoneyAmount(int amount) {
        this.moneyAmount = amount;
    }

    @Override
    public boolean isTransferEmpty() {
        return moneyAmount == 0;
    }

    @Override
    public double getTransferEnergyCost() {
        return 0;
    }

    public enum Types {
        DIODE(Material.REPEATER, 9, 9),
        LAMP(Material.REDSTONE_LAMP, 9, 9),
        RAILS(Material.POWERED_RAIL, 9, 9),
        PISTON(Material.PISTON, 9, 9),
        RABBIT_STEW(Material.RABBIT_STEW, 8, 64),
        TARGET(Material.TARGET, 9, 9),
        COOKIE(Material.COOKIE, 32, 1),
        DAYLIGHT(Material.DAYLIGHT_DETECTOR, 9, 9),
        HOPPER(Material.HOPPER, 4, 9),
        FISH(Material.COOKED_SALMON, 9, 12),
        BOOKSHELF(Material.BOOKSHELF, 9, 9),
        BEETROOT_SOUP(Material.BEETROOT_SOUP, 9, 5),
        TNT(Material.TNT, 9, 9),
        ENDER_CHEST(Material.ENDER_CHEST, 3, 30),
        BREWING_STAND(Material.BREWING_STAND, 9, 9),
        DISPENSER(Material.DISPENSER, 4, 9),
        SMOKER(Material.SMOKER, 9, 6),
        CAKE(Material.CAKE, 8, 16),
        ENCHANTING_TABLE(Material.ENCHANTING_TABLE, 3, 30),
        COMPASS(Material.COMPASS, 5, 9),
        JUKEBOX(Material.JUKEBOX, 9, 18),
        GOLDEN_HOE(Material.GOLDEN_HOE, 8, 15),
        SUSPICIOUS_STEW(Material.SUSPICIOUS_STEW, 8, 5),
        FLINT_AND_STEAL(Material.FLINT_AND_STEEL, 8, 6),
        BOOK_AND_QUILL(Material.WRITABLE_BOOK, 9, 9),
        CROSSBOW(Material.CROSSBOW, 9, 9),
        PUMPKIN_PIE(Material.PUMPKIN_PIE, 18, 9),
        ;

        private final Material mat;
        private final int requires;
        private final double produces;

        Types (Material mat, int requires, double produces) {
            this.mat = mat;
            this.requires = requires;
            this.produces = produces;
        }

        public Material getMat() {
            return mat;
        }

        public int getRequires() {
            return requires;
        }

        public double getProduces() {
            return produces;
        }

        public static Optional<Types> getType(Material mat) {
            return Arrays.stream(values()).filter(t -> t.getMat().equals(mat)).findFirst();
        }
    }
}
