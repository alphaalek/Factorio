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

    private static final Base64.Decoder DECODER = Base64.getDecoder();
    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    public static String encode(ByteArrayOutputStream stream) {
        byte[] bytes = stream.toByteArray();
        return ENCODER.encodeToString(bytes);
    }

    public static ByteArrayInputStream decode(String data) {
        return new ByteArrayInputStream(DECODER.decode(data));
    }

    private final MechanicController controller;
    private final Management fallbackManagement;

    private Location loc;

    public MechanicStorageContext(MechanicController controller, Location location, Management fallbackManagement) {
        this.controller = controller;
        this.loc = location;
        this.fallbackManagement = fallbackManagement;
    }

    public Snapshot load() throws SQLException, IOException {
        return this.controller.load(this.loc);
    }

    public void save(Snapshot snapshot) throws SQLException, IOException {
        this.controller.save(this.loc, snapshot);
    }

    public boolean hasContext() throws SQLException {
        return this.controller.exists(this.loc);
    }

    public void move(Location loc, BlockFace rot) throws SQLException {
        this.controller.move(this.loc, loc, rot);
        this.loc = loc;
    }

    public MechanicController getController() {
        return this.controller;
    }

    public MechanicSerializer getSerializer() {
        return getController().getMechanicSerializer();
    }

    public Management getFallbackManagement() {
        return this.fallbackManagement;
    }

    public Location getLocation() {
        return this.loc;
    }
}
