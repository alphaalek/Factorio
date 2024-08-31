package dk.superawesome.factorio.mechanics.db;

import dk.superawesome.factorio.Factorio;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;

public class DatabaseConnection {

    private final String host;
    private final int port;

    private final String database;
    private final String username;
    private final String password;

    private Connection connection;

    public DatabaseConnection(ConfigurationSection section) {
        this.host = section.getString("host");
        this.port = section.getInt("port");

        this.database = section.getString("database");

        this.username = section.getString("username");
        this.password = section.getString("password");

        if (this.host != null && !this.host.isEmpty()) {
            try {
                tryConnect();
            } catch (Exception ex) {
                Factorio.get().getLogger().log(Level.SEVERE, "Failed to connect to database!", ex);
            }
        }
    }

    public boolean validConnection() throws SQLException {
        return connection != null && !connection.isClosed();
    }

    private boolean loadDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return true;
        } catch (Exception ex) {
            Factorio.get().getLogger().log(Level.SEVERE, "Failed to load jdbc driver", ex);
        }

        return false;
    }

    public void tryConnect() throws SQLException {
        if (!loadDriver()) {
            return;
        }

        Properties properties = new Properties();
        properties.setProperty("user", this.username);
        properties.setProperty("password", this.password);
        properties.setProperty("autoReconnect", "true");

        this.connection = DriverManager.getConnection("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database, properties);
        Factorio.get().getLogger().info("Successfully connected to database!");
    }

    public boolean hasConnection() throws SQLException {
        return this.connection != null && !this.connection.isClosed();
    }

    public Connection getConnection() {
        return this.connection;
    }
}
