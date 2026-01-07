package com.sms.dashboard.util;

import javax.swing.*;
import java.awt.Component;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Centralized error handling for dashboard operations
 */
public class DashboardErrorHandler {
    
    private static final Logger logger = Logger.getLogger(DashboardErrorHandler.class.getName());
    
    /**
     * Handles database operation errors with user feedback
     */
    public static void handleDatabaseError(Component parent, String operation, Exception e) {
        String message = String.format("Failed to %s. Please try again.", operation);
        logger.log(Level.SEVERE, "Database error during " + operation, e);
        
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(parent, message, "Database Error", JOptionPane.ERROR_MESSAGE);
        });
    }
    
    /**
     * Handles general application errors with user feedback
     */
    public static void handleApplicationError(Component parent, String operation, Exception e) {
        String message = String.format("Error during %s: %s", operation, e.getMessage());
        logger.log(Level.WARNING, "Application error during " + operation, e);
        
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(parent, message, "Application Error", JOptionPane.WARNING_MESSAGE);
        });
    }
    
    /**
     * Shows success message to user
     */
    public static void showSuccess(Component parent, String message) {
        logger.info("Success: " + message);
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(parent, message, "Success", JOptionPane.INFORMATION_MESSAGE);
        });
    }
    
    /**
     * Logs error without showing dialog (for background operations)
     */
    public static void logError(String operation, Exception e) {
        logger.log(Level.SEVERE, "Error during " + operation, e);
    }
    
    /**
     * Handles refresh operation errors
     */
    public static void handleRefreshError(Component parent, Exception e) {
        String message = "Error refreshing dashboard: " + e.getMessage();
        logger.log(Level.WARNING, "Refresh error", e);
        
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(parent, message, "Refresh Error", JOptionPane.ERROR_MESSAGE);
        });
    }
    
    /**
     * Generic error handler with message and exception
     */
    public static void handleError(String message, Exception e) {
        logger.log(Level.SEVERE, message, e);
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, message + ": " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        });
    }
}