package com.sms.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import com.sms.util.ConfigLoader;

/**
 * Database Connection Manager
 * 
 * UPDATED: Now uses HikariCP connection pooling for production-grade performance
 * - Supports 50+ concurrent users
 * - Automatic connection management
 * - Connection leak detection
 * - Backward compatible with existing code
 * 
 * @version 2.0 (Connection Pool)
 * @since 2026-01-25
 */
public class DatabaseConnection {
    private static final String URL = ConfigLoader.getDatabaseUrl();
    private static final String USERNAME = ConfigLoader.getDatabaseUsername();
    private static final String PASSWORD = ConfigLoader.getDatabasePassword();
    
    // Deprecated - no longer used with connection pooling
    @Deprecated
    private static Connection connection = null;
    
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL Driver loaded successfully");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Driver not found: " + e.getMessage());
        }
    }
    
    /**
     * Get a database connection from the pool
     * 
     * IMPORTANT: Always use try-with-resources to prevent connection leaks:
     * <pre>
     * try (Connection conn = DatabaseConnection.getConnection()) {
     *     // Use connection
     * } // Automatically returned to pool
     * </pre>
     * 
     * @return Database connection from pool
     * @throws SQLException if connection cannot be obtained
     */
    public static Connection getConnection() throws SQLException {
        // Delegate to connection pool manager
        return ConnectionPoolManager.getConnection();
    }
    
    public static void closeConnection() {
        // No-op with connection pooling - connections auto-return to pool
        // Kept for backward compatibility only
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    // Test method
    public static void testConnection() {
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("Database connection test successful!");
                System.out.println(ConnectionPoolManager.getPoolStats());
                
                // Test if tables exist
                var meta = conn.getMetaData();
                var tables = meta.getTables(null, null, "sections", null);
                if (tables.next()) {
                    System.out.println("'sections' table exists");
                } else {
                    System.err.println("'sections' table does not exist!");
                }
                tables.close();
            }
        } catch (SQLException e) {
            System.err.println("Database connection test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public static void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Shutdown connection pool gracefully
     * Call when application is closing
     */
    public static void shutdown() {
        ConnectionPoolManager.shutdown();
    }
}