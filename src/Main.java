import javax.swing.UIManager;
import com.formdev.flatlaf.FlatLightLaf;
import com.sms.login.LoginScreen;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf");
        }

        // Launch your login or dashboard here
        new LoginScreen();
    }
}

