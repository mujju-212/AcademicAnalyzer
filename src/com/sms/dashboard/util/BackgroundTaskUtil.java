package com.sms.dashboard.util;

import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import java.awt.Cursor;
import java.awt.Component;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utility class for consistent background task execution with proper UI threading
 */
public class BackgroundTaskUtil {
    
    /**
     * Executes a background task with loading state and proper error handling
     */
    public static <T> void executeTask(
            Component component,
            Supplier<T> backgroundTask,
            Consumer<T> onSuccess,
            Consumer<Exception> onError) {
        
        executeTask(component, backgroundTask, onSuccess, onError, true);
    }
    
    /**
     * Executes a background task with optional loading cursor
     */
    public static <T> void executeTask(
            Component component,
            Supplier<T> backgroundTask,
            Consumer<T> onSuccess,
            Consumer<Exception> onError,
            boolean showLoadingCursor) {
        
        if (showLoadingCursor && component != null) {
            component.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
        
        SwingWorker<T, Void> worker = new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return backgroundTask.get();
            }
            
            @Override
            protected void done() {
                try {
                    T result = get();
                    SwingUtilities.invokeLater(() -> {
                        if (onSuccess != null) {
                            onSuccess.accept(result);
                        }
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        if (onError != null) {
                            onError.accept(e);
                        }
                    });
                } finally {
                    if (showLoadingCursor && component != null) {
                        SwingUtilities.invokeLater(() -> {
                            component.setCursor(Cursor.getDefaultCursor());
                        });
                    }
                }
            }
        };
        
        worker.execute();
    }
    
    /**
     * Executes a simple background task without return value
     */
    public static void executeVoidTask(
            Component component,
            Runnable backgroundTask,
            Runnable onSuccess,
            Consumer<Exception> onError) {
        
        executeTask(component, 
            () -> {
                backgroundTask.run();
                return null;
            },
            result -> {
                if (onSuccess != null) {
                    onSuccess.run();
                }
            },
            onError);
    }
    
    /**
     * Executes a background refresh operation with notification
     */
    public static void executeRefreshTask(
            Component component,
            Runnable refreshTask,
            Consumer<String> showNotification) {
        
        executeVoidTask(component,
            refreshTask,
            () -> {
                if (showNotification != null) {
                    showNotification.accept("Dashboard refreshed successfully");
                }
            },
            exception -> DashboardErrorHandler.handleRefreshError(component, exception));
    }
    
    /**
     * Simple async execution without UI components
     */
    public static void executeAsync(Runnable task) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                task.run();
                return null;
            }
        };
        worker.execute();
    }
}