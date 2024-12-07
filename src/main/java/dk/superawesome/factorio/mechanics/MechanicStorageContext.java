package dk.superawesome.factorio.mechanics;


import dk.superawesome.factorio.mechanics.db.MechanicController;
import dk.superawesome.factorio.mechanics.db.StorageException;
import dk.superawesome.factorio.util.db.Query;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Base64;
import java.util.UUID;
import java.util.stream.IntStream;

public class MechanicStorageContext {

    public static class Provider {

        private final MechanicController controller;

        public Provider(MechanicController controller) {
            this.controller = controller;
        }

        public MechanicStorageContext findAt(Location loc) throws StorageException {
            try {
                return controller.findAt(loc);
            } catch (IOException | SQLException ex) {
                throw new StorageException(ex);
            }
        }

        public MechanicStorageContext create(Location loc, BlockFace rot, String type, UUID owner) throws StorageException {
            try {
                return controller.create(loc, rot, type, owner);
            } catch (SQLException | IOException ex) {
                throw new StorageException(ex);
            }
        }

        public boolean deleteAt(Location loc) throws StorageException {
            try {
                return controller.deleteAt(loc);
            } catch (SQLException ex) {
                throw new StorageException(ex);
            }
        }
    }

    public static String encode(ByteArrayOutputStream stream) {
        byte[] bytes = stream.toByteArray();
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static ByteArrayInputStream decode(String data) {
        return new ByteArrayInputStream(Base64.getDecoder().decode(data));
    }

    public static void upload(ByteArrayOutputStream stream, Query.CheckedConsumer<String> data, String currentData) throws SQLException {
        // check if either new or current data is valid
        // if none of them are, don't allow this upload because it doesn't matter anyway
        if (hasData(stream.toByteArray()) || hasData(decode(currentData).readAllBytes())) {
            data.<SQLException>sneaky(encode(stream));
        }
    }

    public static boolean hasData(byte[] bytes) {
        return IntStream.range(0, bytes.length).map(i -> bytes[i]).anyMatch(b -> b > 0);
    }

    public static <E extends Exception> ByteArrayInputStream getData(Query.CheckedSupplier<String, E> data) throws E {
        return decode(data.sneaky());
    }

    private final MechanicController controller;
    private final Management fallbackManagement;

    private int lastLevel;
    private double lastXP;
    private Management lastManagement;

    private Location loc;

    public MechanicStorageContext(MechanicController controller, Location location, Management fallbackManagement) {
        this.controller = controller;
        this.loc = location;
        this.fallbackManagement = fallbackManagement;
    }

    public Management load(Mechanic<?> mechanic) throws SQLException, IOException {
        return this.controller.load(mechanic);
    }

    public Location getLocation() {
        return this.loc;
    }

    public void move(Location loc, BlockFace rot) throws SQLException {
        if (hasContext()) {
            this.controller.move(this.loc, loc, rot);
        }

        this.loc = loc;
    }

    public MechanicController getController() {
        return this.controller;
    }

    public MechanicSerializer getSerializer() {
        return getController().getMechanicSerializer();
    }

    public ByteArrayInputStream getData() throws SQLException {
        return getData(() -> this.controller.getData(this.loc));
    }

    public Management getFallbackManagement() {
        return this.fallbackManagement;
    }

    public Management getManagement() throws SQLException, IOException {
        ByteArrayInputStream stream = getData(() -> this.controller.getManagementData(this.loc));
        if (stream.available() == 0) {
            // return fallback management if it failed to poll from db
            return this.fallbackManagement;
        }

        return this.lastManagement = this.controller.getManagementSerializer().deserialize(stream);
    }

    public void uploadData(ByteArrayOutputStream stream) throws SQLException {
        upload(stream, base64 -> this.controller.setData(this.loc, base64), this.controller.getData(this.loc));
    }

    public void uploadManagement(Management management) throws SQLException, IOException {
        if (this.lastManagement != null && this.lastManagement.equals(management)) {
            return;
        }

        ByteArrayOutputStream stream = this.controller.getManagementSerializer().serialize(management);
        upload(stream, base64 -> this.controller.setManagement(this.loc, base64), this.controller.getManagementData(this.loc));
    }

    public boolean hasContext() throws SQLException {
        return this.controller.exists(this.loc);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String column) throws SQLException {
        return (T) this.controller.get(this.loc, column, result -> result.getObject(column));
    }

    public int getLevel() throws SQLException {
        return this.lastLevel = this.controller.getLevel(this.loc);
    }

    public void setLevel(int level) throws SQLException {
        if (this.lastLevel == level) {
            return;
        }
        this.controller.setLevel(this.loc, level);
    }

    public double getXP() throws SQLException {
        return this.lastXP = this.controller.getXP(this.loc);
    }

    public void setXP(double xp) throws SQLException {
        if (this.lastXP == xp) {
            return;
        }
        this.controller.setXP(this.loc, xp);
    }
}
