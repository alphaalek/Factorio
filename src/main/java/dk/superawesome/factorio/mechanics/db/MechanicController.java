package dk.superawesome.factorio.mechanics.db;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.Management;
import dk.superawesome.factorio.mechanics.MechanicSerializer;
import dk.superawesome.factorio.mechanics.MechanicStorageContext;
import dk.superawesome.factorio.util.Serializer;
import dk.superawesome.factorio.util.db.Query;
import dk.superawesome.factorio.util.db.Types;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public class MechanicController {

    private final DatabaseConnection connection;
    private final Serializer<Management> managementSerializer;
    private final MechanicSerializer mechanicSerializer;

    public MechanicController(DatabaseConnection connection, MechanicSerializer mechanicSerializer, Serializer<Management> managementSerializer) {
        this.connection = connection;
        this.mechanicSerializer = mechanicSerializer;
        this.managementSerializer = managementSerializer;

        Query query = new Query(
                "CREATE TABLE IF NOT EXISTS mechanics (" +
                "id INT PRIMARY KEY AUTO_INCREMENT NOT NULL, " +
                "type VARCHAR(16) NOT NULL, " +
                "location VARCHAR(64) NOT NULL, " +
                "rotation ENUM('NORTH', 'EAST', 'SOUTH', 'WEST') NOT NULL, " +
                "level INT DEFAULT 1, " +
                "management TEXT, " +
                "data TEXT)"
        );

        try {
            query.execute(this.connection);
        } catch (SQLException ex) {
            Factorio.get().getLogger().log(Level.SEVERE, "Failed to create table!", ex);
        }
    }

    public Serializer<Management> getManagementSerializer() {
        return this.managementSerializer;
    }

    public MechanicSerializer getMechanicSerializer() {
        return this.mechanicSerializer;
    }

    public void deleteAt(Location location) throws SQLException {
        Query query = new Query(
                "DELETE FROM mechanics " +
                "WHERE location = ?"
                ).add(Types.LOCATION.convert(location));

        query.execute(this.connection);
    }

    public MechanicStorageContext findAt(Location loc) throws SQLException, IOException {
        Management management = managementSerializer.deserialize(MechanicStorageContext.decode(getManagement(loc)));
        if (management == null) {
            throw new IOException("Failed to get management");
        }

        return new MechanicStorageContext(this, loc, management);
    }

    public MechanicStorageContext create(Location loc, BlockFace rot, String type, UUID owner) throws SQLException, IOException {
        Management management = new Management(owner);
        Query query = new Query(
                "INSERT INTO mechanics (type, location, rotation, management) " +
                "VALUES (?, ?, ?, ?)"
                )
                .add(type)
                .add(Types.LOCATION.convert(loc))
                .add(rot.name())
                .add(MechanicStorageContext.encode(managementSerializer.serialize(management)));
        query.execute(this.connection);

        return new MechanicStorageContext(this, loc, management);
    }

    public boolean exists(Location loc) throws SQLException {
        Query query = new Query(
                "SELECT * from mechanics " +
                "WHERE location = ? " +
                "LIMIT 1"
                )
                .add(Types.LOCATION.convert(loc));

        return Boolean.TRUE.equals(
                query.<Boolean>executeQuery(this.connection, __ -> true));
    }

    public <T> T get(Location loc, String column, Query.CheckedFunction<ResultSet, T> function) throws SQLException {
        Query query = new Query(
                "SELECT " + column + " from mechanics " +
                "WHERE location = ? " +
                "LIMIT 1"
                )
                .add(Types.LOCATION.convert(loc));

        return query.<T>executeQuery(this.connection, function::sneaky);
    }

    public String getData(Location loc) throws SQLException {
        return Optional.ofNullable(get(loc, "data", result -> result.getString("data")))
                .orElse("");
    }

    public String getManagement(Location loc) throws SQLException {
        return Optional.ofNullable(get(loc, "management", result -> result.getString("management")))
                .orElse("");
    }

    public int getLevel(Location loc) throws SQLException {
        return Optional.ofNullable(get(loc, "level", result -> result.getInt("level")))
                .orElse(1);
    }

    public void set(Location loc, String column, Object val) throws SQLException {
        Query query = new Query(
                "UPDATE mechanics " +
                "SET " + column + " = ? " +
                "WHERE location = ? " +
                "LIMIT 1"
                )
                .add(val)
                .add(Types.LOCATION.convert(loc));

        query.executeUpdate(this.connection);
    }

    public void setData(Location loc, String data) throws SQLException {
        set(loc, "data", data);
    }

    public void setLevel(Location loc, int level) throws SQLException {
        set(loc, "level", level);
    }

    public void setManagement(Location loc, String data) throws SQLException {
        set(loc, "management", data);
    }
}
