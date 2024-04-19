package com.example.gomesrodris.archburgers.tools.migration;

import com.example.gomesrodris.archburgers.adapters.driven.infra.ConnectionPool;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DatabaseMigration implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseMigration.class);

    private final ConnectionPool connectionPool;
    private final boolean isPoolOwner;

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println("Usage: DatabaseMigration <driverClass> <dbUrl> <dbUser> <dbPass>");
            System.exit(1);
        }

        try (var migration = new DatabaseMigration(args[0], args[1], args[2], args[3])) {
            migration.runMigrations();
        }
    }

    @Autowired
    public DatabaseMigration(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
        this.isPoolOwner = false;
    }

    public DatabaseMigration(String driverClass, String dbUrl, String dbUser, String dbPass) {
        this.connectionPool = new ConnectionPool(driverClass, dbUrl, dbUser, dbPass);
        this.isPoolOwner = true;
    }

    public void runMigrations() throws Exception {
        LOGGER.info("Starting Database migrations");

        try (var connection = connectionPool.getConnection()) {

            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new liquibase.Liquibase("liquibase/dbchangelog.xml", new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts(), new LabelExpression());
        }

        LOGGER.info("Database migration complete");
    }

    @Override
    public void close() {
        if (isPoolOwner)
            connectionPool.close();
    }
}
