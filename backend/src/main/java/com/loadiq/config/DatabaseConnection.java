
package com.loadiq.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String DB_URL =
            System.getenv().getOrDefault(
                    "LOADIQ_DB_URL",
                    "jdbc:mysql://localhost:3306/loadiq"
            );

    private static final String DB_USER =
            System.getenv("LOADIQ_DB_USER");

    private static final String DB_PASSWORD =
            System.getenv("LOADIQ_DB_PASSWORD");

    private DatabaseConnection() {
        // Prevent object creation.
    }

    public static Connection getConnection() throws SQLException {

        if (DB_USER == null || DB_PASSWORD == null) {
            throw new SQLException(
                    "MySQL credentials are missing. " +
                    "Set LOADIQ_DB_USER and LOADIQ_DB_PASSWORD."
            );
        }

        return DriverManager.getConnection(
                DB_URL,
                DB_USER,
                DB_PASSWORD
        );
    }

    public static void initializeDatabase() {

        try (Connection connection = getConnection()) {

            System.out.println("================================");
            System.out.println("   LoadIQ Database Connected!");
            System.out.println("   MySQL Connection Successful");
            System.out.println("   Database: "
                    + connection.getCatalog());
            System.out.println("================================");

        } catch (SQLException e) {

            System.out.println("Database initialization failed!");
            e.printStackTrace();
        }
    }

    public static void testConnection() {
        initializeDatabase();
    }
}
