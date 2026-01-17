package com.sms.login;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.border.*;
import com.formdev.flatlaf.FlatLightLaf;
import java.util.Random;
import java.sql.*;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import com.sms.util.ConfigLoader;
import com.sms.database.DatabaseConnection;

public class ForgotPasswordScreen extends JFrame {
    
    // MailerSend Configuration loaded from environment
    private static final String MAILERSEND_API_KEY = ConfigLoader.getMailerSendApiKey();
    private static final String MAILERSEND_FROM_EMAIL = ConfigLoader.getMailerSendFromEmail();
    private static final String MAILERSEND_FROM_NAME = ConfigLoader.getMailerSendFromName();
    
    // Reuse custom components from CreateAccountScreen
    static class RoundedPanel extends JPanel {
        private int cornerRadius;
        private int shadowSize = 5;
        
        public RoundedPanel(int radius) {
            this.cornerRadius = radius;
            setOpaque(false);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw shadow
            for (int i = 0; i < shadowSize; i++) {
                g2.setColor(new Color(0, 0, 0, 20 - (i * 4)));
                g2.fill(new RoundRectangle2D.Float(i, i, getWidth() - (i * 2), getHeight() - (i * 2), cornerRadius, cornerRadius));
            }
            
            // Draw white background
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Float(shadowSize, shadowSize, getWidth() - (shadowSize * 2), getHeight() - (shadowSize * 2), cornerRadius, cornerRadius));
        }
    }
    
    static class RoundedTextField extends JTextField {
        private int radius;
        private String placeholder;
        private boolean showingPlaceholder = true;
        
        public RoundedTextField(int radius, String placeholder) {
            this.radius = radius;
            this.placeholder = placeholder;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
            setFont(new Font("Arial", Font.PLAIN, 15));
            setBackground(new Color(248, 248, 248));
            
            setText(placeholder);
            setForeground(new Color(150, 150, 150));
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2.setColor(new Color(248, 248, 248));
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
            
            g2.setColor(new Color(230, 230, 230));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, radius, radius));
            
            super.paintComponent(g);
        }
        
        public boolean isShowingPlaceholder() {
            return showingPlaceholder;
        }
        
        public void setShowingPlaceholder(boolean showing) {
            this.showingPlaceholder = showing;
        }
    }
    
    static class RoundedButton extends JButton {
        private int radius;
        private Color backgroundColor;
        private Color hoverColor;
        private boolean isHovered = false;
        
        public RoundedButton(String text, int radius, Color bgColor, Color hoverColor) {
            super(text);
            this.radius = radius;
            this.backgroundColor = bgColor;
            this.hoverColor = hoverColor;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2.setColor(isHovered ? hoverColor : backgroundColor);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
            
            FontMetrics fm = g2.getFontMetrics();
            Rectangle2D r = fm.getStringBounds(getText(), g2);
            int x = (getWidth() - (int) r.getWidth()) / 2;
            int y = (getHeight() - (int) r.getHeight()) / 2 + fm.getAscent();
            
            g2.setColor(getForeground());
            g2.drawString(getText(), x, y);
        }
    }
    
    private RoundedTextField emailField;
    private String generatedOTP;

    public ForgotPasswordScreen() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("Forgot Password - Academic Analyzer");
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Main container with gradient background
        JPanel mainContainer = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(200, 190, 230),
                    getWidth(), getHeight(), new Color(150, 180, 220)
                );
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        
        // White card panel
        RoundedPanel cardPanel = new RoundedPanel(30);
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setPreferredSize(new Dimension(420, 400));
        
        // Inner content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        // Title
        JLabel titleLabel = new JLabel("Forgot Password");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(30, 30, 30));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Description
        JLabel descLabel = new JLabel("<html><center>Enter your email address and we'll send you an OTP to reset your password.</center></html>");
        descLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        descLabel.setForeground(new Color(100, 100, 100));
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        descLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Email field
        emailField = new RoundedTextField(25, "Email");
        emailField.setMaximumSize(new Dimension(340, 50));
        emailField.setAlignmentX(Component.CENTER_ALIGNMENT);
        addPlaceholderBehavior(emailField, "Email");
        
        // Send OTP button
        RoundedButton sendOTPButton = new RoundedButton("Send OTP", 25, 
            new Color(100, 120, 200), new Color(80, 100, 180));
        sendOTPButton.setFont(new Font("Arial", Font.BOLD, 16));
        sendOTPButton.setForeground(Color.WHITE);
        sendOTPButton.setMaximumSize(new Dimension(340, 50));
        sendOTPButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Back to login link
        JLabel backToLoginLabel = new JLabel("Back to Login");
        backToLoginLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        backToLoginLabel.setForeground(new Color(100, 120, 200));
        backToLoginLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backToLoginLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        backToLoginLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                dispose();
                new LoginScreen();
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                backToLoginLabel.setText("<html><u>Back to Login</u></html>");
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                backToLoginLabel.setText("Back to Login");
            }
        });
        
        // Add components
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(descLabel);
        contentPanel.add(Box.createVerticalStrut(30));
        contentPanel.add(emailField);
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(sendOTPButton);
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(backToLoginLabel);
        
        cardPanel.add(contentPanel);
        mainContainer.add(cardPanel);
        
        // Send OTP action
        sendOTPButton.addActionListener(e -> sendOTP());
        
        add(mainContainer);
        setVisible(true);
    }
    
    private void addPlaceholderBehavior(RoundedTextField field, String placeholder) {
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent evt) {
                if (field.isShowingPlaceholder() && field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                    field.setShowingPlaceholder(false);
                }
            }
            
            @Override
            public void focusLost(FocusEvent evt) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(new Color(150, 150, 150));
                    field.setShowingPlaceholder(true);
                }
            }
        });
    }
    
    private void sendOTP() {
        String email = emailField.getText();
        
        if (email.equals("Email") || email.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please enter your email address", 
                "Email Required", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            JOptionPane.showMessageDialog(this, 
                "Please enter a valid email address", 
                "Invalid Email", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Check if email exists in database
        if (!checkEmailExists(email)) {
            JOptionPane.showMessageDialog(this, 
                "No account found with this email address", 
                "Email Not Found", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Generate OTP
        generatedOTP = generateOTP();
        
        // Store OTP in database
        if (storeOTPInDatabase(email, generatedOTP)) {
            // Try to send email via EmailJS
            boolean emailSent = sendEmailViaEmailJS(email, generatedOTP);
            
            if (emailSent) {
                JOptionPane.showMessageDialog(this, 
                    "OTP has been sent to " + email + "\nPlease check your email.", 
                    "OTP Sent", 
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                // Fallback: show OTP if email fails
                JOptionPane.showMessageDialog(this, 
                    "Email service temporarily unavailable.\n\n" +
                    "Your OTP is: " + generatedOTP + "\n\n" +
                    "Valid for 10 minutes", 
                    "OTP Generated", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
            
            // Open OTP verification screen
            dispose();
            new OTPVerificationScreen(email, generatedOTP);
        } else {
            JOptionPane.showMessageDialog(this, 
                "Failed to generate OTP. Please try again.", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private boolean sendEmailViaEmailJS(String toEmail, String otp) {
        try {
            URL url = new URL("https://api.mailersend.com/v1/email");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + MAILERSEND_API_KEY);
            conn.setDoOutput(true);
            
            // Build professional HTML email for password reset OTP
            String htmlContent = buildPasswordResetOTPHtml(otp);
            String plainContent = buildPasswordResetOTPPlain(otp);
            
            // Build JSON payload
            String jsonPayload = buildJsonPayload(toEmail, htmlContent, plainContent);
            
            // Send request
            try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(jsonPayload);
                writer.flush();
            }
            
            // Check response
            int responseCode = conn.getResponseCode();
            
            if (responseCode == 202 || responseCode == 200) {
                System.out.println("✅ Password reset OTP sent successfully via MailerSend");
                return true;
            } else {
                System.err.println("❌ MailerSend API error: HTTP " + responseCode);
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error sending password reset OTP: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private String buildPasswordResetOTPHtml(String otp) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body{font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;line-height:1.6;color:#333;background:#f4f4f4;margin:0;padding:0;}");
        html.append(".container{max-width:600px;margin:20px auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,0.1);}");
        html.append(".header{background:linear-gradient(135deg,#ea4335 0%,#fbbc04 100%);color:#fff;padding:40px 30px;text-align:center;}");
        html.append(".header h1{margin:0;font-size:28px;font-weight:600;}");
        html.append(".content{padding:40px 30px;}");
        html.append(".otp-box{background:#f8f9fa;border:2px dashed #ea4335;padding:30px;margin:25px 0;text-align:center;border-radius:8px;}");
        html.append(".otp-code{font-size:36px;font-weight:700;color:#ea4335;letter-spacing:8px;font-family:monospace;margin:10px 0;}");
        html.append(".warning-box{background:#fff3cd;border-left:4px solid #ffc107;padding:15px;margin:20px 0;border-radius:4px;}");
        html.append(".info-box{background:#d1ecf1;border-left:4px solid #0dcaf0;padding:15px;margin:20px 0;border-radius:4px;}");
        html.append(".footer{background:#f8f9fa;padding:30px;text-align:center;border-top:1px solid #dee2e6;}");
        html.append(".footer p{margin:5px 0;color:#6c757d;font-size:13px;}");
        html.append("</style></head><body><div class='container'>");
        html.append("<div class='header'><h1>🔒 Password Reset Request</h1><p>Reset your account password securely</p></div>");
        html.append("<div class='content'>");
        html.append("<p>Dear User,</p>");
        html.append("<p>We received a request to reset the password for your <strong>Academic Analyzer</strong> account. Use the One-Time Password (OTP) below to proceed with resetting your password.</p>");
        html.append("<div class='otp-box'><p style='margin:0;color:#6c757d;font-size:14px;'>Your Password Reset Code</p>");
        html.append("<div class='otp-code'>").append(otp).append("</div>");
        html.append("<p style='margin:0;color:#6c757d;font-size:12px;'>Valid for 10 minutes</p></div>");
        html.append("<div class='warning-box'><p style='margin:0;color:#856404;font-size:13px;'>");
        html.append("<strong>⚠️ Security Alert:</strong> If you didn't request a password reset, please ignore this email and ensure your account is secure. Never share this OTP with anyone.");
        html.append("</p></div>");
        html.append("<div class='info-box'><p style='margin:0;color:#055160;font-size:13px;'>");
        html.append("<strong>🛈 Security Tips:</strong><br>");
        html.append("• Use a strong, unique password<br>");
        html.append("• Never share your password with anyone<br>");
        html.append("• Enable two-factor authentication when available");
        html.append("</p></div>");
        html.append("</div>");
        html.append("<div class='footer'>");
        html.append("<p>This is an automated notification from <span style='color:#4285f4;font-weight:600;'>Academic Analyzer</span></p>");
        html.append("<p>If you have concerns about your account security, please contact support immediately.</p>");
        html.append("<p>&copy; 2026 Academic Analyzer. All rights reserved.</p>");
        html.append("</div></div></body></html>");
        return html.toString();
    }
    
    private String buildPasswordResetOTPPlain(String otp) {
        StringBuilder plain = new StringBuilder();
        plain.append("=============================================\n");
        plain.append("   PASSWORD RESET - ACADEMIC ANALYZER\n");
        plain.append("=============================================\n\n");
        plain.append("Dear User,\n\n");
        plain.append("We received a request to reset the password for your Academic Analyzer account.\n");
        plain.append("Use the OTP below to proceed with resetting your password.\n\n");
        plain.append("YOUR PASSWORD RESET CODE\n");
        plain.append("------------------------------------------\n");
        plain.append("        ").append(otp).append("\n");
        plain.append("------------------------------------------\n");
        plain.append("Valid for 10 minutes\n\n");
        plain.append("SECURITY ALERT\n");
        plain.append("------------------------------------------\n");
        plain.append("If you didn't request a password reset, please ignore this email and ensure\n");
        plain.append("your account is secure. Never share this OTP with anyone.\n\n");
        plain.append("SECURITY TIPS\n");
        plain.append("------------------------------------------\n");
        plain.append("* Use a strong, unique password\n");
        plain.append("* Never share your password with anyone\n");
        plain.append("* Enable two-factor authentication when available\n\n");
        plain.append("--\n");
        plain.append("This is an automated notification from Academic Analyzer.\n");
        plain.append("If you have concerns about your account security, contact support immediately.\n\n");
        plain.append("© 2026 Academic Analyzer. All rights reserved.\n");
        return plain.toString();
    }
    
    private String buildJsonPayload(String toEmail, String htmlContent, String plainContent) {
        return "{" +
            "\"from\":{\"email\":\"" + escapeJson(MAILERSEND_FROM_EMAIL) + "\",\"name\":\"" + escapeJson(MAILERSEND_FROM_NAME) + "\"}," +
            "\"to\":[{\"email\":\"" + escapeJson(toEmail) + "\"}]," +
            "\"subject\":\"Password Reset Request - Academic Analyzer\"," +
            "\"text\":\"" + escapeJson(plainContent) + "\"," +
            "\"html\":\"" + escapeJson(htmlContent) + "\"" +
            "}";
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    private String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
    
    private boolean checkEmailExists(String email) {
        String url = "jdbc:mysql://localhost:3306/academic_analyzer";
        String dbUsername = "root";
        String dbPassword = "mk0492"; // Your MySQL password
        
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        
        try (Connection conn = DriverManager.getConnection(url, dbUsername, dbPassword);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Database error: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
        
        return false;
    }
    
    private boolean storeOTPInDatabase(String email, String otp) {
        String url = "jdbc:mysql://localhost:3306/academic_analyzer";
        String dbUsername = "root";
        String dbPassword = "mk0492"; // Your MySQL password
        
        // Create table with a simpler structure
        String createTableSql = "CREATE TABLE IF NOT EXISTS password_reset_otps (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, " +
            "email VARCHAR(100) NOT NULL, " +
            "otp VARCHAR(6) NOT NULL, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "expires_at TIMESTAMP NULL, " +
            "used BOOLEAN DEFAULT FALSE, " +
            "INDEX idx_email_otp (email, otp))";
        
        // Invalidate old OTPs and insert new one
        String invalidateSql = "UPDATE password_reset_otps SET used = TRUE WHERE email = ? AND used = FALSE";
        // Calculate expiry time in the INSERT statement
        String insertSql = "INSERT INTO password_reset_otps (email, otp, expires_at) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 10 MINUTE))";
        
        try (Connection conn = DriverManager.getConnection(url, dbUsername, dbPassword)) {
            // Create table if needed
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createTableSql);
            }
            
            // Invalidate old OTPs
            try (PreparedStatement pstmt = conn.prepareStatement(invalidateSql)) {
                pstmt.setString(1, email);
                pstmt.executeUpdate();
            }
            
            // Insert new OTP with expiry time
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setString(1, email);
                pstmt.setString(2, otp);
                return pstmt.executeUpdate() > 0;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Database error: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ForgotPasswordScreen());
    }
}