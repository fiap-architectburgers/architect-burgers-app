package com.example.gomesrodris.archburgers.adapters.database;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Supplier;

@Component
@Scope("singleton")
public class DatabaseConnection implements TransactionManager, AutoCloseable {
    private final ComboPooledDataSource cpds;

    private final ThreadLocal<ConnectionInstance> inTransactionConnection = new ThreadLocal<>();

    public DatabaseConnection(String driverClass, String dbUrl, String dbUser, String dbPass) {
        cpds = buildDataSource(driverClass, dbUrl, dbUser, dbPass);
    }

    @Autowired
    public DatabaseConnection(Environment environment) {
        String driverClassEnv = environment.getProperty("archburgers.datasource.driverClass");
        String dbUrlEnv = environment.getProperty("archburgers.datasource.dbUrl");
        String dbUserEnv = environment.getProperty("archburgers.datasource.dbUser");
        String dbPassEnv = environment.getProperty("archburgers.datasource.dbPass");

        if (driverClassEnv == null) {
            throw new IllegalStateException("driverClass env is missing");
        }
        if (dbUrlEnv == null) {
            throw new IllegalStateException("dbUrl env is missing");
        }
        if (dbUserEnv == null) {
            throw new IllegalStateException("dbUser env is missing");
        }
        if (dbPassEnv == null) {
            throw new IllegalStateException("dbPass env is missing");
        }

        cpds = buildDataSource(driverClassEnv, dbUrlEnv, dbUserEnv, dbPassEnv);
    }

    @Override
    public <T> T runInTransaction(Supplier<T> task) throws Exception {
        Connection conn = cpds.getConnection();
        try {
            conn.setAutoCommit(false);
            inTransactionConnection.set(new ConnectionInstance(conn));

            T result = task.get();
            conn.commit();

            return result;
        } catch (Throwable t) {
            conn.rollback();
            throw t;
        } finally {
            inTransactionConnection.remove();
            conn.close();
        }
    }

    public boolean isInTransaction() {
        return inTransactionConnection.get() != null;
    }

    public ConnectionInstance getConnection() {
        var transactionConnection = inTransactionConnection.get();
        if (transactionConnection != null) {
            return transactionConnection;
        }

        try {
            return new ConnectionInstance(cpds.getConnection());
        } catch (SQLException e) {
            throw new RuntimeException("Could not get DB connection: " + e.getMessage(), e);
        }
    }

    /**
     * Para uso em casos especiais como execução de Migration. Não utilize o acesso direto
     * nos métodos da aplicação!
     */
    public Connection jdbcConnection() throws SQLException {
        return cpds.getConnection();
    }

    private ComboPooledDataSource buildDataSource(String driverClass, String dbUrl, String dbUser, String dbPass) {
        ComboPooledDataSource cpds = new ComboPooledDataSource();
        try {
            cpds.setDriverClass(driverClass);
        } catch (PropertyVetoException e) {
            throw new RuntimeException(e);
        }
        cpds.setJdbcUrl(dbUrl);
        cpds.setUser(dbUser);
        cpds.setPassword(dbPass);

        cpds.setMinPoolSize(1);
        cpds.setMaxPoolSize(10);

        return cpds;
    }

    @Override
    public void close() {
        cpds.close();
    }

    public class ConnectionInstance implements AutoCloseable {
        private final Connection conn;

        public ConnectionInstance(Connection conn) {
            this.conn = conn;
        }

        public PreparedStatement prepareStatement(@Language("SQL") String sql) throws SQLException {
            return conn.prepareStatement(sql);
        }

        @Override
        public void close() {
            // Se está em transação ignorar, será fechada no final
            if (!isInTransaction()) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    throw new RuntimeException("Error closing the connection: " + e, e);
                }
            }
        }

        @VisibleForTesting
        public boolean isClosed() {
            try {
                return conn.isClosed();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
