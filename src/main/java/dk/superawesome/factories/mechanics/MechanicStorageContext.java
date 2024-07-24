package dk.superawesome.factories.mechanics;


import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import sun.misc.IOUtils;

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
        String mat = new String(IOUtils.readNBytes(stream, stream.read()));
        if (!mat.isEmpty()) {
            return new ItemStack(Material.valueOf(mat));
        } else {
            return null;
        }
    }

    public int getLevel() {
        return 1;
    }
}
