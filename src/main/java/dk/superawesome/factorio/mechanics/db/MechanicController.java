package dk.superawesome.factorio.mechanics.db;

import dk.superawesome.factorio.Factorio;
import dk.superawesome.factorio.mechanics.*;
import dk.superawesome.factorio.mechanics.impl.accessible.Assembler;
import dk.superawesome.factorio.util.Serializer;
import dk.superawesome.factorio.util.db.Query;
import dk.superawesome.factorio.util.db.Types;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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

        Query createMechanics = new Query(
                "CREATE TABLE IF NOT EXISTS mechanics (" +
                "id INT PRIMARY KEY AUTO_INCREMENT NOT NULL, " +
                "type VARCHAR(18) NOT NULL, " +
                "location VARCHAR(64) NOT NULL, " +
                "rotation ENUM('NORTH', 'EAST', 'SOUTH', 'WEST') NOT NULL, " +
                "level INT DEFAULT 1, " +
                "xp DOUBLE(16, 2) DEFAULT 0, " +
                "management TEXT, " +
                "data TEXT)");

        Query createDefaultMembers = new Query(
                "CREATE TABLE IF NOT EXISTS mechanics_defaultMembers (" +
                "playerUUID VARCHAR(36) NOT NULL, " +
                "defaultMemberPlayerUUID VARCHAR(36) NOT NULL, " +
                "PRIMARY KEY (playerUUID, defaultMemberPlayerUUID)" +
                ")");

        Query createAssemblerTransformed = new Query(
                "CREATE TABLE IF NOT EXISTS mechanics_assembler_transformed (" +
                "type VARCHAR(32) PRIMARY KEY NOT NULL, " +
                "transformed DOUBLE(32, 2)" +
                ")"
        );

        try {
            createMechanics.execute(this.connection);
            createDefaultMembers.execute(this.connection);
            createAssemblerTransformed.execute(this.connection);
        } catch (SQLException ex) {
            Factorio.get().getLogger().log(Level.SEVERE, "Failed to create tables!", ex);
        }
    }

    public void close() throws SQLException {
        this.connection.getConnection().close();
    }

    public DatabaseConnection getConnection() {
        return this.connection;
    }

    public Serializer<Management> getManagementSerializer() {
        return this.managementSerializer;
    }

    public MechanicSerializer getMechanicSerializer() {
        return this.mechanicSerializer;
    }

    public Snapshot load(Location loc) throws SQLException, IOException {
        Query query = new Query(
                "SELECT level, xp, management, data " +
                "FROM mechanics " +
                "WHERE location = ?")
                .add(Types.LOCATION.convert(loc));

        return query.executeQueryCall(this.connection, r ->
            new Snapshot(
                r.getInt("level"),
                r.getDouble("xp"),
                this.managementSerializer.deserialize(MechanicStorageContext.decode(r.getString("management"))),
                r.getString("data")
            )
        );
    }

    public void save(Location loc, Snapshot snapshot) throws SQLException, IOException {
        Query query = new Query(
                "UPDATE mechanics " +
                "SET level = ?, xp = ?, management = ?, data = ? " +
                "WHERE location = ?")
                .add(snapshot.level())
                .add(snapshot.xp())
                .add(this.managementSerializer.serialize(snapshot.management()))
                .add(snapshot.strData())
                .add(Types.LOCATION.convert(loc));

        query.execute(this.connection);
    }

    public void registerTransformed(Assembler.Types type, double amount) throws SQLException {
        Query query = new Query(
                "INSERT INTO mechanics_assembler_transformed " +
                "VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE transformed = transformed + ?")
                .add(type.name())
                .add(amount)
                .add(amount);

        query.execute(this.connection);
    }

    public List<UUID> getDefaultMembersFor(UUID uuid) throws SQLException {
        Query query = new Query(
                "SELECT defaultMemberPlayerUUID AS member " +
                "FROM mechanics_defaultMembers " +
                "WHERE playerUUID = ?")
                .add(uuid.toString());

        List<UUID> members = new ArrayList<>();
        return Optional.ofNullable(
                query.executeQueryCall(this.connection, rs -> {
                    do {
                        members.add(UUID.fromString(rs.getString("member")));
                    } while (rs.next());
                    return members;
                }))
                .orElse(members);
    }

    public void addDefaultMemberFor(UUID uuid, UUID member) throws SQLException {
        Query query = new Query(
                "INSERT INTO mechanics_defaultMembers VALUES (?, ?)")
                .add(uuid.toString())
                .add(member.toString());

        query.execute(this.connection);
    }

    public void removeDefaultMemberFor(UUID uuid, UUID member) throws SQLException {
        Query query = new Query(
                "DELETE FROM mechanics_defaultMembers " +
                "WHERE playerUUID = ? AND defaultMemberPlayerUUID = ?")
                .add(uuid.toString())
                .add(member.toString());

        query.executeUpdate(this.connection);
    }

    public void move(Location from, Location to, BlockFace rot) throws SQLException {
        Query query = new Query(
                "UPDATE mechanics " +
                "SET location = ?, rotation = ? " +
                "WHERE location = ?")
                .add(Types.LOCATION.convert(to))
                .add(rot.name())
                .add(Types.LOCATION.convert(from));

        query.executeUpdate(this.connection);
    }

    public boolean deleteAt(Location location) throws SQLException {
        Query query = new Query(
                "DELETE FROM mechanics " +
                "WHERE location = ?")
                .add(Types.LOCATION.convert(location));

        return query.executeUpdate(this.connection) > 0;
    }

    public MechanicStorageContext findAt(Location loc) throws SQLException, IOException {
        return new MechanicStorageContext(this, loc, null);
    }

    public MechanicStorageContext create(Location loc, BlockFace rot, String type, UUID owner) throws SQLException, IOException {
        deleteAt(loc);

        Management management = new Management(owner);

        Query query = new Query(
                "INSERT INTO mechanics (type, location, rotation, management) " +
                "VALUES (?, ?, ?, ?)")
                .add(type)
                .add(Types.LOCATION.convert(loc))
                .add(rot.name())
                .add(MechanicStorageContext.encode(this.managementSerializer.serialize(management)));
        query.execute(this.connection);

        return new MechanicStorageContext(this, loc, management);
    }

    public boolean exists(Location loc) throws SQLException {
        Query query = new Query(
                "SELECT * FROM mechanics " +
                "WHERE location = ? " +
                "LIMIT 1")
                .add(Types.LOCATION.convert(loc));

        return Boolean.TRUE.equals(
                query.<Boolean>executeQueryCall(this.connection, __ -> true));
    }
}
