package dk.superawesome.factorio.mechanics.impl;

import dk.superawesome.factorio.gui.impl.AssemblerGui;
import dk.superawesome.factorio.mechanics.AbstractMechanic;
import dk.superawesome.factorio.mechanics.MechanicProfile;
import dk.superawesome.factorio.mechanics.MechanicStorageContext;
import dk.superawesome.factorio.mechanics.Profiles;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public class Assembler extends AbstractMechanic<Assembler, AssemblerGui> {

    private Types type;

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

    public Types getType() {
        return type;
    }

    public void setType(Types type) {
        this.type = type;
    }

    public enum Types {
        DIODE(Material.REPEATER),
        LAMP(Material.REDSTONE_LAMP),
        RAILS(Material.POWERED_RAIL),
        PISTON(Material.PISTON),
        RABBIT_STEW(Material.RABBIT_STEW),
        TARGET(Material.TARGET),
        COOKIE(Material.COOKIE),
        DAYLIGHT(Material.DAYLIGHT_DETECTOR),
        HOPPER(Material.HOPPER),
        FISH(Material.COOKED_SALMON),
        BOOKSHELF(Material.BOOKSHELF),
        BEETROOT_SOUP(Material.BEETROOT_SOUP),
        TNT(Material.TNT),
        ENDER_CHEST(Material.ENDER_CHEST),
        BREWING_STAND(Material.BREWING_STAND),
        DISPENSER(Material.DISPENSER),
        MINECART(Material.MINECART),
        CAKE(Material.CAKE),
        ENCHANTING_TABLE(Material.ENCHANTING_TABLE),
        COMPASS(Material.COMPASS),
        JUKEBOX(Material.JUKEBOX),
        GOLDEN_HOE(Material.GOLDEN_HOE),
        SUSPICIOUS_STEW(Material.SUSPICIOUS_STEW),
        FLINT_AND_STEAL(Material.FLINT_AND_STEEL),
        BOOK_AND_QUILL(Material.WRITABLE_BOOK),
        CROSSBOW(Material.CROSSBOW),
        PUMPKIN_PIE(Material.PUMPKIN_PIE),
        ;

        private final Material mat;

        Types (Material mat) {
            this.mat = mat;
        }

        public Material getMat() {
            return mat;
        }

        public static Optional<Types> getType(Material mat) {
            return Arrays.stream(values()).filter(t -> t.getMat().equals(mat)).findFirst();
        }
    }
}
