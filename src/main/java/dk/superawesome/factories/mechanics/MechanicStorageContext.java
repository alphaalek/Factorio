package dk.superawesome.factories.mechanics;


import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class MechanicStorageContext {

    public static MechanicStorageContext DEFAULT = new MechanicStorageContext();

    public static MechanicStorageContext findAt(Location loc) {
        return DEFAULT;
    }

    public ByteArrayInputStream getData() {
        String base64 = ""; // get
        byte[] bytes = Base64.getDecoder().decode(base64);
        return new ByteArrayInputStream(bytes);
    }

    public void upload(ByteArrayOutputStream stream) {
        byte[] bytes = stream.toByteArray();
        String base64 = Base64.getEncoder().encodeToString(bytes);

        // upload
    }

    public ItemStack readItemStack(ByteArrayInputStream stream) throws IOException {
        int l = stream.read();
        if (l > 0) {
            byte[] buf = new byte[l];
            int len = stream.read(buf);
            String mat;
            if (len == buf.length && (mat = new String(buf)).length() == buf.length) {
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
