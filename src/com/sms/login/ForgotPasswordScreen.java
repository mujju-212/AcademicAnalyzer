package com.sms.login;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.border.*;
import com.formdev.flatlaf.FlatLightLaf;
import java.util.Random;
import java.sql.*;
import java.net.http.*;
import java.net.URI;
import com.sms.util.ConfigLoader;
import com.sms.database.DatabaseConnection;

public class ForgotPasswordScreen extends JFrame {
    
    // EmailJS Configuration loaded from environment
    private static final String EMAILJS_SERVICE_ID = ConfigLoader.getEmailJsServiceId();
    private static final String EMAILJS_TEMPLATE_ID = ConfigLoader.getEmailJsTemplateId();
    private static final String EMAILJS_PUBLIC_KEY = ConfigLoader.getEmailJsPublicKey();
    
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
            HttpClient client = HttpClient.newHttpClient();
            
            // Create JSON payload
            String jsonPayload = String.format(
                "{\"service_id\":\"%s\"," +
                "\"template_id\":\"%s\"," +
                "\"user_id\":\"%s\"," +
                "\"template_params\":{" +
                    "\"to_email\":\"%s\"," +
                    "\"otp\":\"%s\"" +
                "}}",
                EMAILJS_SERVICE_ID,
                EMAILJS_TEMPLATE_ID,
                EMAILJS_PUBLIC_KEY,
                toEmail,
                otp
            );
            
            System.out.println("Sending email to: " + toEmail);
            System.out.println("OTP: " + otp);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.emailjs.com/api/v1.0/email/send"))
                .header("Content-Type", "application/json")
                .header("origin", "http://localhost")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
            
            HttpResponse<String> response = client.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            System.out.println("Response status: " + response.statusCode());
            System.out.println("Response body: " + response.body());
            
            if (response.statusCode() == 200) {
                System.out.println("Email sent successfully via EmailJS!");
                return true;
            } else {
                System.err.println("EmailJS failed with status: " + response.statusCode());
                System.err.println("Response: " + response.body());
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("Error sending email via EmailJS: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
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