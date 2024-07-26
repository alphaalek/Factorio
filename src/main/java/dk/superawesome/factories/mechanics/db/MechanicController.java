package dk.superawesome.factories.mechanics.db;

import dk.superawesome.factories.mechanics.Management;
import dk.superawesome.factories.mechanics.MechanicSerializer;
import dk.superawesome.factories.mechanics.MechanicStorageContext;
import dk.superawesome.factories.util.Serializer;
import dk.superawesome.factories.util.db.Query;
import dk.superawesome.factories.util.db.Types;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
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
                "rotation ENUM(NORTH, EAST, SOUTH, WEST) NOT NULL, " +
                "level INT DEFAULT 1, " +
                "management TEXT, " +
                "data TEXT)"
        );

        try {
            query.execute(this.connection);
        } catch (SQLException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to create table!", ex);
        }
    }

    public Serializer<Management> getManagementSerializer() {
        return this.managementSerializer;
    }

    public MechanicSerializer getMechanicSerializer() {
        return this.mechanicSerializer;
    }

    public MechanicStorageContext findAt(Location loc) {
        return new MechanicStorageContext(this, loc);
    }

    public MechanicStorageContext create(Location loc, BlockFace rot, String type, UUID owner) throws SQLException, IOException {
        MechanicStorageContext context = new MechanicStorageContext(this, loc);

        Management management = new Management(owner);
        String managementData = context.encode(managementSerializer.serialize(management));

        Query query = new Query(
                "INSERT INTO mechanics (type, location, rotation, management) " +
                "VALUES (?, ?, ?, ?)"
                )
                .add(type)
                .add(Types.LOCATION.convert(loc))
                .add(rot.name())
                .add(managementData);
        query.execute(this.connection);

        return context;
    }

    public boolean exists(Location loc) throws SQLException {
        Query query = new Query(
                "SELECT * from mechanics " +
                "WHERE location = ? " +
                "LIMIT BY 1"
                )
                .add(Types.LOCATION.convert(loc));

        try (ResultSet result = query.executeQuery(this.connection)) {
            return result != null && result.next();
        }
    }

    public <T> T get(Location loc, String column, Query.CheckedFunction<ResultSet, T> function) throws SQLException {
        Query query = new Query(
                "SELECT ? from mechanics " +
                "WHERE location = ? " +
                "LIMIT BY 1"
                )
                .add(column)
                .add(Types.LOCATION.convert(loc));

        try (ResultSet result = query.executeQuery(this.connection)) {
            return function.sneaky(result);
        }
    }

    public String getData(Location loc) throws SQLException {
        return get(loc, "data",
                result -> Optional.ofNullable(result.getString("data"))
                        .orElse("")
        );
    }

    public String getManagement(Location loc) throws SQLException {
        return get(loc, "management",
                result -> Optional.ofNullable(result.getString("management"))
                        .orElse("")
        );
    }

    public int getLevel(Location loc) throws SQLException {
        return get(loc, "level", result -> result.getInt("level"));
    }

    public void set(Location loc, String column, Object val) throws SQLException {
        Query query = new Query(
                "UPDATE mechanics " +
                "SET ? = ? " +
                "WHERE location = ? " +
                "LIMIT BY 1"
                )
                .add(column)
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
