import javax.swing.UIManager;
import com.formdev.flatlaf.FlatLightLaf;
import com.sms.login.LoginScreen;
import com.sms.dashboard.DashboardScreen;
import com.sms.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf");
        }

        // Auto-login for development with mujju718263@gmail.com
        int userId = autoLogin("mujju718263@gmail.com");
        if (userId > 0) {
            System.out.println("Auto-login successful. Opening dashboard...");
            new DashboardScreen(userId);
        } else {
            System.out.println("Auto-login failed. Opening login screen...");
            new LoginScreen();
        }
    }
    
    private static int autoLogin(String email) {
        String sql = "SELECT id, username FROM users WHERE email = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int userId = rs.getInt("id");
                // Store the logged-in user's ID in LoginScreen for compatibility
                LoginScreen.currentUserId = userId;
                System.out.println("Auto-login: User logged in with ID: " + userId);
                return userId;
            }
            return 0;
        } catch (Exception e) {
            System.err.println("Auto-login error: " + e.getMessage());
            return 0;
        }
    }
}

