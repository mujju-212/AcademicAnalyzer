import javax.swing.UIManager;
import javax.swing.JOptionPane;
import com.formdev.flatlaf.FlatLightLaf;
import com.sms.login.AuthenticationFrame;
import com.sms.util.ConfigLoader;
import com.sms.database.DatabaseConnection;
import com.sms.util.BackgroundTask;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf");
        }

        // Validate database configuration
        if (!ConfigLoader.isDatabaseConfigValid()) {
            System.err.println("========================================");
            System.err.println("DATABASE CONFIGURATION ERROR");
            System.err.println("========================================");
            System.err.println("Required database credentials not found!");
            System.err.println("Working directory: " + System.getProperty("user.dir"));
            System.err.println("Please ensure .env file is present with:");
            System.err.println("  - DB_HOST");
            System.err.println("  - DB_USERNAME");
            System.err.println("  - DB_PASSWORD");
            System.err.println("========================================");
            
            JOptionPane.showMessageDialog(null,
                "Database configuration error!\n\n" +
                "The application requires a .env file with Azure database credentials.\n" +
                "Please contact support if this error persists.\n\n" +
                "Working directory: " + System.getProperty("user.dir"),
                "Configuration Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Add shutdown hook to close connection pool gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down application...");
            BackgroundTask.shutdown();
            DatabaseConnection.shutdown();
            System.out.println("✓ Cleanup complete");
        }));

        // Show login screen
        new AuthenticationFrame();
    }
}

