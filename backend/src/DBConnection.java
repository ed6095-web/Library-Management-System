package backend.src;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database connection utility class for Library Management System
 * Handles MySQL database connection using JDBC
 */
public class DBConnection {
    
    // Database configuration - Load from environment variables
    private static final String DB_HOST = getEnv("DB_HOST", "localhost");
    private static final String DB_PORT = getEnv("DB_PORT", "3306");
    private static final String DB_NAME = getEnv("DB_NAME", "library_management");
    private static final String DB_USERNAME = getEnv("DB_USER", "root");
    private static final String DB_PASSWORD = getEnv("DB_PASSWORD", "");
    private static final String DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME + "?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false";
    private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";
    
    // Singleton connection instance
    private static Connection connection = null;
    
    /**
     * Private constructor to prevent instantiation
     */
    private DBConnection() {}
    
    /**
     * Get database connection instance (Singleton pattern)
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Load MySQL JDBC driver
                Class.forName(DB_DRIVER);
                
                // Create connection
                connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                
                // Set auto-commit to false for transaction management
                connection.setAutoCommit(false);
                
                System.out.println("Database connection established successfully.");
                
            } catch (ClassNotFoundException e) {
                System.err.println("MySQL JDBC Driver not found: " + e.getMessage());
                throw new SQLException("Database driver not found", e);
            } catch (SQLException e) {
                System.err.println("Failed to connect to database: " + e.getMessage());
                throw e;
            }
        }
        return connection;
    }
    
    /**
     * Test database connection
     * @return true if connection is successful, false otherwise
     */
    public static boolean testConnection() {
        try {
            Connection conn = getConnection();
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Database connection test failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Close database connection
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Database connection closed.");
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            }
        }
    }
    
    /**
     * Commit current transaction
     */
    public static void commitTransaction() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.commit();
            }
        } catch (SQLException e) {
            System.err.println("Error committing transaction: " + e.getMessage());
        }
    }
    
    /**
     * Rollback current transaction
     */
    public static void rollbackTransaction() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.rollback();
            }
        } catch (SQLException e) {
            System.err.println("Error rolling back transaction: " + e.getMessage());
        }
    }
    
    /**
     * Get database URL for reference
     * @return database URL string
     */
    public static String getDatabaseUrl() {
        return DB_URL;
    }
    
    /**
     * Get database username for reference
     * @return database username
     */
    public static String getDatabaseUsername() {
        return DB_USERNAME;
    }
    
    /**
     * Helper method to get environment variable with default value
     * @param key environment variable name
     * @param defaultValue default value if not found
     * @return environment variable value or default
     */
    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }
}
