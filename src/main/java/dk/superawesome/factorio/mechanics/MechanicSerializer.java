package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.util.db.Query;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MechanicSerializer {

    public UUID readUUID(ByteArrayInputStream stream) throws IOException {
        long l1 = readLong(stream);
        long l2 = readLong(stream);

        if (l1 != 0 || l2 != 0) {
            return new UUID(l1, l2);
        } else {
            return null;
        }
    }

    public void writeUUID(ByteArrayOutputStream stream, UUID uuid) throws IOException {
        writeLong(stream, uuid.getMostSignificantBits());
        writeLong(stream, uuid.getLeastSignificantBits());
    }

    public void writeItemStack(ByteArrayOutputStream stream, ItemStack stack) throws IOException {
        if (stack == null || stack.getType() == Material.AIR) {
            stream.write(0);
            return;
        }

        String mat = stack.getType().name();
        byte[] bytes = mat.getBytes(StandardCharsets.UTF_8);
        stream.write(bytes.length);
        stream.write(bytes, 0, bytes.length);
        writeInt(stream, stack.getAmount());

        if (stack.hasItemMeta()) {
            stream.write(1);
            ObjectOutputStream output = new BukkitObjectOutputStream(stream);
            output.writeObject(stack.getItemMeta());
        } else {
            stream.write(0);
        }
    }

    public ItemStack readItemStack(ByteArrayInputStream stream) throws IOException, ClassNotFoundException {
        int l = stream.read();
        if (l > 0) {
            byte[] buf = new byte[l];
            int len = stream.read(buf, 0, l);

            int a = readInt(stream);
            boolean hasMeta = stream.read() == 1;
            if (len == l && a > 0) {
                String mat = new String(buf);
                ItemStack stack = new ItemStack(Material.valueOf(mat), a);

                if (hasMeta) {
                    ObjectInputStream input = new BukkitObjectInputStream(stream);
                    ItemMeta meta = (ItemMeta) input.readObject();
                    stack.setItemMeta(meta);
                }

                if (stack.getType() != Material.AIR) {
                    return stack;
                }
            }
        }

        return null;
    }

    public <T> T readData(ByteArrayInputStream stream, int bytesRequired, T or, Query.CheckedFunction<DataInputStream, T> function) throws IOException {
        if (stream.available() >= bytesRequired) {
            DataInputStream dataStream = new DataInputStream(stream);
            return function.sneaky(dataStream);
        }

        return or;
    }

    public void writeData(ByteArrayOutputStream stream, Query.CheckedConsumer<DataOutputStream> function) {
        DataOutputStream dataStream = new DataOutputStream(stream);
        function.sneaky(dataStream);
    }

    public int readInt(ByteArrayInputStream stream) throws IOException {
        return readData(stream, 4, 0, DataInputStream::readInt);
    }

    public void writeInt(ByteArrayOutputStream stream, int val) throws IOException {
        writeData(stream, data -> data.writeInt(val));
    }

    public long readLong(ByteArrayInputStream stream) throws IOException {
        return readData(stream, 8, 0L, DataInputStream::readLong);
    }

    public void writeLong(ByteArrayOutputStream stream, long val) throws IOException {
        writeData(stream, data -> data.writeLong(val));
    }

    public double readDouble(ByteArrayInputStream stream) throws IOException {
        return readData(stream, 8, 0d, DataInputStream::readDouble);
    }

    public void writeDouble(ByteArrayOutputStream stream, double val) throws IOException {
        writeData(stream, data -> data.writeDouble(val));
    }

    public boolean readBoolean(ByteArrayInputStream stream) throws IOException {
        return readData(stream, 1, false, DataInputStream::readBoolean);
    }

    public void writeBoolean(ByteArrayOutputStream stream, boolean val) throws IOException {
        writeData(stream, data -> data.writeBoolean(val));
    }
}
