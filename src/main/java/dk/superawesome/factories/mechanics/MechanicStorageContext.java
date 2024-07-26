package dk.superawesome.factories.mechanics;


import dk.superawesome.factories.mechanics.db.MechanicController;
import dk.superawesome.factories.util.db.Query;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Base64;
import java.util.UUID;

public class MechanicStorageContext {

    public static class Provider {

        private final MechanicController controller;

        public Provider(MechanicController controller) {
            this.controller = controller;
        }

        public MechanicStorageContext findAt(Location loc) {
            return controller.findAt(loc);
        }

        public MechanicStorageContext create(Location loc, BlockFace rot, String type, UUID owner) throws SQLException, IOException {
            return controller.create(loc, rot, type, owner);
        }
    }

    private final MechanicController controller;
    private final Location location;

    public MechanicStorageContext(MechanicController controller, Location location) {
        this.controller = controller;
        this.location = location;
    }

    public MechanicController getController() {
        return this.controller;
    }

    public MechanicSerializer getSerializer() {
        return getController().getMechanicSerializer();
    }

    public ByteArrayInputStream getData(Query.CheckedSupplier<String> data) throws SQLException {
        byte[] bytes = Base64.getDecoder().decode(data.<SQLException>sneaky());
        return new ByteArrayInputStream(bytes);
    }

    public ByteArrayInputStream getData() throws SQLException {
        return getData(() -> this.controller.getData(this.location));
    }

    public Management getManagement() throws SQLException, IOException {
        ByteArrayInputStream stream = getData(() -> this.controller.getManagement(this.location));
        return this.controller.getManagementSerializer().deserialize(stream);
    }

    public String encode(ByteArrayOutputStream stream) {
        byte[] bytes = stream.toByteArray();
        return Base64.getEncoder().encodeToString(bytes);
    }

    public void upload(ByteArrayOutputStream stream, Query.CheckedConsumer<String> data) throws SQLException {
        data.<SQLException>sneaky(encode(stream));
    }

    public void upload(ByteArrayOutputStream stream) throws SQLException {
        upload(stream, base64 -> this.controller.setData(this.location, base64));
    }

    public void uploadManagement(Management management) throws SQLException, IOException {
        ByteArrayOutputStream stream = this.controller.getManagementSerializer().serialize(management);
        upload(stream, base64 -> this.controller.setManagement(this.location, base64));
    }

    public boolean hasContext() throws SQLException {
        return this.controller.exists(this.location);
    }

    public int getLevel() throws SQLException {
        return this.controller.getLevel(this.location);
    }
}
