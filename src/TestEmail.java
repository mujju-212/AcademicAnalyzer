import com.sms.resultlauncher.EmailService;

/**
 * Simple test program to verify MailerSend email configuration.
 */
public class TestEmail {
    
    public static void main(String[] args) {
        System.out.println("=== MailerSend Email Test ===\n");
        
        // Check configuration
        String configStatus = EmailService.getConfigStatus();
        System.out.println("Configuration Status: " + configStatus);
        System.out.println();
        
        if (!EmailService.isConfigured()) {
            System.err.println("❌ MailerSend is not configured properly!");
            System.err.println("Please check your .env file");
            System.exit(1);
        }
        
        // Send test email
        System.out.println("📧 Sending test email to: mujju718263@gmail.com");
        System.out.println("Please wait...\n");
        
        boolean success = EmailService.sendTestEmail("mujju718263@gmail.com", "Test Recipient");
        
        System.out.println();
        if (success) {
            System.out.println("✅ SUCCESS! Test email sent successfully!");
            System.out.println("Please check the inbox at mujju718263@gmail.com");
            System.out.println("(Also check spam/junk folder if not in inbox)");
        } else {
            System.out.println("❌ FAILED! Could not send test email");
            System.out.println("Please check:");
            System.out.println("  1. API key is correct");
            System.out.println("  2. Domain is verified in MailerSend");
            System.out.println("  3. From email matches verified domain");
        }
        
        System.out.println("\n=== Test Complete ===");
    }
}
