package dk.superawesome.factorio.mechanics.db;

import dk.superawesome.factorio.Factorio;
import org.bukkit.configuration.ConfigurationSection;
import org.mariadb.jdbc.MariaDbPoolDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

public class DatabaseConnection {

    private final MariaDbPoolDataSource dataSource = new MariaDbPoolDataSource();

    public DatabaseConnection(ConfigurationSection section) {
        String host = section.getString("host");
        int port = section.getInt("port");
        String database = section.getString("database");
        String username = section.getString("username");
        String password = section.getString("password");

        try {
            Factorio.get().getLogger().info("Connecting to database " + host + ":" + port + "/" + database + " with user \"" + username + "\" and password \"" + "*".repeat(password.length()) + "\"");

            this.dataSource.setUser(username);
            this.dataSource.setPassword(password);
            this.dataSource.setUrl("jdbc:mariadb://" + host + ":" + port + "/" + database + "?maxPoolSize=10");
        } catch (SQLException ex) {
            Factorio.get().getLogger().log(Level.SEVERE, "Failed to connect to database!", ex);
        }
    }

    public Connection getConnection() throws SQLException {
        return this.dataSource.getConnection();
    }
}
