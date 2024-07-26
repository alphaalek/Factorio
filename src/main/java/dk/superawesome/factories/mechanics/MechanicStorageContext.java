package dk.superawesome.factories.mechanics;


import dk.superawesome.factories.mechanics.db.MechanicController;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Base64;

public class MechanicStorageContext {

    public static class Provider {

        private final MechanicController controller;

        public Provider(MechanicController controller) {
            this.controller = controller;
        }

        public MechanicStorageContext findAt(Location loc) {
            return controller.findAt(loc);
        }

        public MechanicStorageContext create(Location loc, BlockFace rot, String type) throws SQLException {
            return controller.create(loc, rot, type);
        }
    }

    private final MechanicController controller;
    private final Location location;

    public MechanicStorageContext(MechanicController controller, Location location) {
        this.controller = controller;
        this.location = location;
    }

    public ByteArrayInputStream getData() throws SQLException {
        String base64 = controller.getData(location);

        byte[] bytes = Base64.getDecoder().decode(base64);
        return new ByteArrayInputStream(bytes);
    }

    public void upload(ByteArrayOutputStream stream) throws SQLException {
        byte[] bytes = stream.toByteArray();
        String base64 = Base64.getEncoder().encodeToString(bytes);

        controller.setData(location, base64);
    }

    public boolean hasContext() throws SQLException {
        return controller.hasData(location);
    }

    public void writeItemStack(ByteArrayOutputStream stream, ItemStack item) {
        if (item == null) {
            stream.write(0);
            return;
        }

        String mat = item.getType().name();
        byte[] bytes = mat.getBytes(StandardCharsets.UTF_8);
        stream.write(bytes.length);
        stream.write(bytes, 0, bytes.length);
    }

    public ItemStack readItemStack(ByteArrayInputStream stream) {
        int l = stream.read();
        if (l > 0) {
            byte[] buf = new byte[l];
            int len = stream.read(buf, 0, l);
            if (len == l) {
                String mat = new String(buf);
                return new ItemStack(Material.valueOf(mat));
            }
        }

        return null;
    }

    public int readInt(ByteArrayInputStream stream) {
        int l = stream.read();
        return l == -1 ? 0 : l;
    }

    public int getLevel() {
        return 1;
    }
}
