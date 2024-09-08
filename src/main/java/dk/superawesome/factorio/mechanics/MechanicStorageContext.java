package dk.superawesome.factorio.mechanics;


import dk.superawesome.factorio.mechanics.db.MechanicController;
import dk.superawesome.factorio.util.db.Query;
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

        public MechanicStorageContext findAt(Location loc) throws SQLException, IOException {
            return controller.findAt(loc);
        }

        public MechanicStorageContext create(Location loc, BlockFace rot, String type, UUID owner) throws SQLException, IOException {
            return controller.create(loc, rot, type, owner);
        }

        public void deleteAt(Location loc) throws SQLException {
            controller.deleteAt(loc);
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

    public static ByteArrayInputStream getData(Query.CheckedSupplier<String> data) throws SQLException {
        return decode(data.<SQLException>sneaky());
    }

    private final MechanicController controller;
    private final Management fallbackManagement;

    private Location location;

    public MechanicStorageContext(MechanicController controller, Location location, Management fallbackManagement) {
        this.controller = controller;
        this.location = location;
        this.fallbackManagement = fallbackManagement;
    }

    public void move(Location loc, BlockFace rot) throws SQLException {
        if (hasContext() && this.controller.validConnection()) {
            this.controller.move(this.location, loc, rot);
        }

        this.location = loc;
    }

    public MechanicController getController() {
        return this.controller;
    }

    public MechanicSerializer getSerializer() {
        return getController().getMechanicSerializer();
    }

    public ByteArrayInputStream getData() throws SQLException {
        return getData(() -> this.controller.getData(this.location));
    }

    public Management getManagement() throws SQLException, IOException {
        ByteArrayInputStream stream = getData(() -> this.controller.getManagement(this.location));
        if (stream.available() == 0) {
            // return fallback management if it failed to poll from db
            return fallbackManagement;
        }

        return this.controller.getManagementSerializer().deserialize(stream);
    }

    public void uploadData(ByteArrayOutputStream stream) throws SQLException {
        upload(stream, base64 -> this.controller.setData(this.location, base64), this.controller.getData(this.location));
    }

    public void uploadManagement(Management management) throws SQLException, IOException {
        ByteArrayOutputStream stream = this.controller.getManagementSerializer().serialize(management);
        upload(stream, base64 -> this.controller.setManagement(this.location, base64), this.controller.getManagement(this.location));
    }

    public boolean hasContext() throws SQLException {
        return this.controller.exists(this.location);
    }

    public int getLevel() throws SQLException {
        return this.controller.getLevel(this.location);
    }

    public void setLevel(int level) throws SQLException {
        this.controller.setLevel(this.location, level);
    }

    public double getXP() throws SQLException {
        return this.controller.getXP(this.location);
    }

    public void setXP(double xp) throws SQLException {
        this.controller.setXP(this.location, xp);
    }
}
