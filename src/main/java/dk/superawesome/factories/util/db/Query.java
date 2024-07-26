package dk.superawesome.factories.util.db;

import dk.superawesome.factories.mechanics.db.DatabaseConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Query {

    private final String query;
    private final List<Object> values = new ArrayList<>();

    public Query(String query) {
        this.query = query;
    }

    public Query add(Object val) {
        this.values.add(val);
        return this;
    }

    private <T> T create(DatabaseConnection connection, CheckedFunction<PreparedStatement, T> apply) throws SQLException {
        if (connection.hasConnection()) {
            try (PreparedStatement statement = connection.getConnection().prepareStatement(this.query)) {
                for (int i = 0; i < values.size(); i++) {
                    statement.setObject(i, values.get(i));
                }

                return apply.sneaky(statement);
            }
        }

        return null;
    }

    public ResultSet executeQuery(DatabaseConnection connection) throws SQLException {
        return create(connection, PreparedStatement::executeQuery);
    }

    public boolean execute(DatabaseConnection connection) throws SQLException {
        return Boolean.TRUE.equals(create(connection, PreparedStatement::execute));
    }

    public int executeUpdate(DatabaseConnection connection) throws SQLException {
        return Optional.ofNullable(create(connection, PreparedStatement::executeUpdate))
                .orElse(0);
    }

    @SuppressWarnings("unchecked")
    static <T, E extends Exception> T sneakyThrow(Exception e) throws E {
        throw (E) e;
    }

    @FunctionalInterface
    public interface CheckedSupplier<T> {

        T get() throws Exception;

        default <E extends Exception> T sneaky() throws E {
            try {
                return get();
            } catch (Exception ex) {
                return sneakyThrow(ex);
            }
        }
    }

    @FunctionalInterface
    public interface CheckedConsumer<T> {

        void accept(T val) throws Exception;

        default <E extends Exception> void sneaky(T val) throws E {
            try {
                accept(val);
            } catch (Exception ex) {
                sneakyThrow(ex);
            }
        }
    }

    @FunctionalInterface
    public interface CheckedFunction<T, R> {

        R apply(T val) throws Exception;

        default <E extends Exception> R sneaky(T val) throws E {
            try {
                return apply(val);
            } catch (Exception ex) {
                return sneakyThrow(ex);
            }
        }
    }
}
