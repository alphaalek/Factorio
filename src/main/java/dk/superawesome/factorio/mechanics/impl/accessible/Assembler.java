package dk.superawesome.factorio.mechanics.impl.accessible;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.api.events.AssemblerTypeChangeEvent;
import dk.superawesome.factorio.api.events.AssemblerTypeRequestEvent;
import dk.superawesome.factorio.gui.impl.AssemblerGui;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.routes.events.pipe.PipePutEvent;
import dk.superawesome.factorio.mechanics.transfer.ItemCollection;
import dk.superawesome.factorio.mechanics.transfer.ItemContainer;
import dk.superawesome.factorio.mechanics.transfer.MoneyCollection;
import dk.superawesome.factorio.util.statics.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class Assembler extends AbstractMechanic<Assembler> implements AccessibleMechanic, ThinkingMechanic, ItemContainer, MoneyCollection {

    private final DelayHandler thinkDelayHandler = new DelayHandler(20);
    private final DelayHandler transferDelayHandler = new DelayHandler(10);
    private Type type;
    private int ingredientAmount;
    private double moneyAmount;

    public Assembler(Location loc, BlockFace rotation, MechanicStorageContext context) {
        super(loc, rotation, context);
        loadFromStorage();
    }

    @Override
    public void load(MechanicStorageContext context) throws SQLException, IOException, ClassNotFoundException {
        ByteArrayInputStream data = context.getData();
        ItemStack item = context.getSerializer().readItemStack(data);
        if (item != null) {
            this.type = Types.getLoadedType(Types.getTypeFromMaterial(item.getType()).orElseThrow(IllegalArgumentException::new));
        }
        this.ingredientAmount = context.getSerializer().readInt(data);
        this.moneyAmount = context.getSerializer().readDouble(data);
    }

    public void save(MechanicStorageContext context) throws SQLException, IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        context.getSerializer().writeItemStack(stream,
                Optional.ofNullable(this.type)
                        .map(Type::getMat)
                        .map(ItemStack::new)
                        .orElse(null));
        context.getSerializer().writeInt(stream, this.ingredientAmount);
        context.getSerializer().writeDouble(stream, this.moneyAmount);

        context.uploadData(stream);
    }

    @Override
    public MechanicProfile<Assembler> getProfile() {
        return Profiles.ASSEMBLER;
    }

    @Override
    public DelayHandler getThinkDelayHandler() {
        return thinkDelayHandler;
    }

    @Override
    public void think() {
        // check if an assembler type is chosen, if not, don't continue
        if (type == null
                // check if the assembler has enough ingredients to assemble, if not, don't continue
                || ingredientAmount < type.getRequires()
                // check if the assembler has enough space for money, if not, don't continue
                || moneyAmount + type.getProduces() > getMoneyCapacity()) {
            return;
        }

        // do the assembling
        ingredientAmount -= type.getRequires();
        moneyAmount += type.getProduces();

        AssemblerGui gui = this.<AssemblerGui>getGuiInUse().get();
        if (gui != null) {
            gui.updateRemovedIngredients(type.getRequires());
            gui.setDisplayedMoney(moneyAmount);

            for (HumanEntity player : gui.getInventory().getViewers()) {
                ((Player)player).playSound(getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.25f, 1f);
            }
        }
    }

    @Override
    public boolean isContainerEmpty() {
        return ingredientAmount == 0;
    }

    @Override
    public void pipePut(ItemCollection collection, PipePutEvent event) {
        ItemStack item = Optional.ofNullable(type)
                .map(Type::getMat)
                .map(ItemStack::new)
                .orElse(null);
        if ((item == null && collection.has(i -> Types.getTypeFromMaterial(i.getType()).isPresent()) || item != null && collection.has(item)) && ingredientAmount < getCapacity()) {
            int add = this.<AssemblerGui>put(collection, getCapacity() - ingredientAmount, getGuiInUse(), AssemblerGui::updateAddedIngredients, new HeapToStackAccess<ItemStack>() {
                @Override
                public ItemStack get() {
                    return item;
                }

                @Override
                public void set(ItemStack val) {
                    Types oldTypes = type != null ? type.getType() : null;
                    Types newTypes = Types.getTypeFromMaterial(val.getType()).orElseThrow(IllegalArgumentException::new);
                    type = Types.getLoadedType(newTypes);
                    AssemblerTypeChangeEvent assemblerTypeChangeEvent = new AssemblerTypeChangeEvent(Assembler.this, oldTypes, newTypes);
                    Bukkit.getPluginManager().callEvent(assemblerTypeChangeEvent);
                }
            });

            if (add > 0) {
                ingredientAmount += add;
                event.setTransferred(true);
            }
        }
    }

    @Override
    public int getCapacity() {
        return level.getInt(ItemCollection.CAPACITY_MARK) *
                Optional.ofNullable(type)
                        .map(Type::getMat)
                        .map(Material::getMaxStackSize)
                        .orElse(64);
    }

    public double getMoneyCapacity() {
        return level.getDouble(MoneyCollection.CAPACITY_MARK);
    }

    public Type getType() {
        return this.type;
    }

    public void setType(Types type) {
        Types oldTypes = this.type != null ? this.type.getType() : null;
        this.type = Types.getLoadedType(type);
        AssemblerTypeChangeEvent event = new AssemblerTypeChangeEvent(this, oldTypes, type);
        Bukkit.getPluginManager().callEvent(event);

        AssemblerGui gui = this.<AssemblerGui>getGuiInUse().get();
        if (gui != null) {
            gui.loadAssemblerType();
        }
    }

    public int getIngredientAmount() {
        return this.ingredientAmount;
    }

    public void setIngredientAmount(int amount) {
        this.ingredientAmount = amount;
    }

    public double getMoneyAmount() {
        return this.moneyAmount;
    }

    public void setMoneyAmount(double amount) {
        this.moneyAmount = amount;
    }

    @Override
    public boolean isTransferEmpty() {
        return ((int)moneyAmount) == 0;
    }

    @Override
    public DelayHandler getTransferDelayHandler() {
        return transferDelayHandler;
    }

    @Override
    public int getMaxTransfer() {
        return type.getType().getMat().getMaxStackSize();
    }

    @Override
    public int getTransferAmount() {
        return ingredientAmount;
    }

    @Override
    public double getTransferEnergyCost() {
        return 2d / 3d;
    }

    @Override
    public double take(double amount) {
        double take = Math.min(amount, moneyAmount);
        moneyAmount -= take;

        AssemblerGui gui = this.<AssemblerGui>getGuiInUse().get();
        if (gui != null) {
            gui.setDisplayedMoney(moneyAmount);
        }

        return take;
    }

    @Override
    public boolean canBeDeleted() {
        return isContainerEmpty();
    }

    public static class Type {

        private final Types type;
        private double produces;
        private int requires;

        public Type(Types type, int requires, double produces) {
            this.type = type;
            this.requires = requires;
            this.produces = produces;
        }

        public double getProduces() {
            return produces;
        }

        public int getRequires() {
            return requires;
        }

        public Types getType() {
            return type;
        }

        public Material getMat() {
            return type.getMat();
        }

        public boolean isTypesEquals(Types type) {
            return this.type == type;
        }

        public double getPricePerItem() {
            return produces / requires;
        }

        public void setProduces(double produces) {
            this.produces = produces;
        }

        public void setRequires(int requires) {
            this.requires = requires;
        }
    }

    public enum Types {
        DIODE(Material.REPEATER, 9, 9),
        LAMP(Material.REDSTONE_LAMP, 9, 9),
        RAILS(Material.POWERED_RAIL, 9, 9),
        PISTON(Material.PISTON, 9, 9),
        RABBIT_STEW(Material.RABBIT_STEW, 8, 64),
        TARGET(Material.TARGET, 9, 7.25),
        COOKIE(Material.COOKIE, 32, 2),
        DAYLIGHT(Material.DAYLIGHT_DETECTOR, 9, 9),
        HOPPER(Material.HOPPER, 4, 9),
        FISH(Material.COOKED_SALMON, 5, 6.65),
        BOOKSHELF(Material.BOOKSHELF, 9, 9),
        BEETROOT_SOUP(Material.BEETROOT_SOUP, 8, 5.25),
        TNT(Material.TNT, 9, 9),
        DISPENSER(Material.DISPENSER, 4, 9),
        ENDER_CHEST(Material.ENDER_CHEST, 3, 30),
        BREWING_STAND(Material.BREWING_STAND, 9, 9),
        SMOKER(Material.SMOKER, 5, 7.25),
        CAKE(Material.CAKE, 4, 8),
        ENCHANTING_TABLE(Material.ENCHANTING_TABLE, 3, 30),
        SEA_LANTERN(Material.SEA_LANTERN, 4, 6.85),
        JUKEBOX(Material.JUKEBOX, 9, 18),
        GOLDEN_HOE(Material.GOLDEN_HOE, 4, 7.05),
        SUSPICIOUS_STEW(Material.SUSPICIOUS_STEW, 8, 22.5),
        FLINT_AND_STEAL(Material.FLINT_AND_STEEL, 5, 2.65),
        BOOK_AND_QUILL(Material.WRITABLE_BOOK, 9, 9.55),
        CROSSBOW(Material.CROSSBOW, 9, 9),
        PUMPKIN_PIE(Material.PUMPKIN_PIE, 13, 7.5),
        WHITE_GLASS(Material.WHITE_STAINED_GLASS, 8, 1),
        BLUE_GLASS(Material.BLUE_STAINED_GLASS, 8, 1),
        BROWN_GLASS(Material.BROWN_STAINED_GLASS, 8, 1),
        BEACON(Material.BEACON, 2, 640),
        COMPASS(Material.COMPASS, 7, 11.35),
        SOUL_CAMPFIRE(Material.SOUL_CAMPFIRE, 13, 4),
        MILK_BUCKET(Material.MILK_BUCKET, 4, 5.5),
        ;

        private static final long ONE_HOUR_DELAY_TICKS = (long) 20 * 60 * 60;

        public static long LAST_UPDATE = System.currentTimeMillis();

        private static final List<Type> types = new ArrayList<>();

        static {
            addDefaultTypes();
            requestTypes();
            Bukkit.getScheduler().runTaskTimer(Factorio.get(), Assembler.Types::requestTypes, ONE_HOUR_DELAY_TICKS, ONE_HOUR_DELAY_TICKS);
        }

        private static void addDefaultTypes() {
            for (Types type : Types.values()) {
                types.add(new Type(type, type.getRequires(), type.getProduces()));
            }
        }

        private static void requestTypes() {
            LAST_UPDATE = System.currentTimeMillis();
            for (Types type : Types.values()) {
                AssemblerTypeRequestEvent requestEvent = new AssemblerTypeRequestEvent(type);
                Bukkit.getPluginManager().callEvent(requestEvent);

                Type loadedType = getLoadedType(type);
                loadedType.setRequires(requestEvent.getRequires());
                loadedType.setProduces(requestEvent.getProduces());
            }
        }

        public static List<Type> getTypes() {
            return types;
        }

        public static Type getLoadedType(Types type) {
            return types.stream().filter(t -> t.getType() == type).findFirst().orElseThrow(IllegalArgumentException::new);
        }

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

        public static Optional<Types> getTypeFromMaterial(Material mat) {
            return Arrays.stream(values()).filter(t -> t.getMat().equals(mat)).findFirst();
        }

        @Override
        public String toString() {
            return StringUtil.capitalize(this.getMat());
        }
    }
}