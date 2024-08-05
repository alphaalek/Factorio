package dk.superawesome.factorio.mechanics;

import dk.superawesome.factorio.util.db.Query;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

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

    public void writeItemStack(ByteArrayOutputStream stream, ItemStack item) throws IOException {
        if (item == null) {
            stream.write(0);
            writeInt(stream, 0);
            return;
        }

        String mat = item.getType().name();
        byte[] bytes = mat.getBytes(StandardCharsets.UTF_8);
        stream.write(bytes.length);
        stream.write(bytes, 0, bytes.length);
        writeInt(stream, item.getAmount());
    }

    public ItemStack readItemStack(ByteArrayInputStream stream) throws IOException {
        int l = stream.read();
        int a = readInt(stream);
        if (l > 0) {
            byte[] buf = new byte[l];
            int len = stream.read(buf, 0, l);
            if (len == l) {
                String mat = new String(buf);
                return new ItemStack(Material.valueOf(mat), a);
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

    public void writeData(ByteArrayOutputStream stream, Query.CheckedConsumer<DataOutputStream> function) throws IOException {
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
}
