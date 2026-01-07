package com.sms.login;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.border.*;
import com.formdev.flatlaf.FlatLightLaf;
import java.sql.*;
import java.util.Random;
import java.net.http.*;
import java.net.URI;
import com.sms.util.ConfigLoader;
import com.sms.database.DatabaseConnection;

public class CreateAccountScreen extends JFrame {
    
    // EmailJS Configuration loaded from environment
    private static final String EMAILJS_SERVICE_ID = ConfigLoader.getEmailJsServiceId();
    private static final String EMAILJS_TEMPLATE_ID = ConfigLoader.getEmailJsTemplateId();
    private static final String EMAILJS_PUBLIC_KEY = ConfigLoader.getEmailJsPublicKey();
    
    // Reuse the custom components from LoginScreen
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
            
            // Initialize with placeholder
            setText(placeholder);
            setForeground(new Color(150, 150, 150));
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw light gray background
            g2.setColor(new Color(248, 248, 248));
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
            
            // Draw border
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
    
    static class RoundedPasswordField extends JPasswordField {
        private int radius;
        private String placeholder;
        private boolean showingPlaceholder = true;
        
        public RoundedPasswordField(int radius, String placeholder) {
            this.radius = radius;
            this.placeholder = placeholder;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
            setFont(new Font("Arial", Font.PLAIN, 15));
            setBackground(new Color(248, 248, 248));
            
            // Initialize with placeholder
            setText(placeholder);
            setForeground(new Color(150, 150, 150));
            setEchoChar((char) 0);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw light gray background
            g2.setColor(new Color(248, 248, 248));
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
            
            // Draw border
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
    
    static class RoundedComboBox extends JComboBox<String> {
        private int radius;
        
        public RoundedComboBox(String[] items, int radius) {
            super(items);
            this.radius = radius;
            setOpaque(false);
            setBackground(new Color(248, 248, 248));
            setFont(new Font("Arial", Font.PLAIN, 15));
            setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));
            
            // Custom UI for rounded corners
            setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
                @Override
                protected JButton createArrowButton() {
                    JButton button = new JButton("▼");
                    button.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));
                    button.setContentAreaFilled(false);
                    button.setFocusable(false);
                    return button;
                }
            });
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw background
            g2.setColor(new Color(248, 248, 248));
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
            
            // Draw border
            g2.setColor(new Color(230, 230, 230));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, radius, radius));
            
            super.paintComponent(g);
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
            
            // Draw button background
            g2.setColor(isHovered ? hoverColor : backgroundColor);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
            
            // Draw text
            FontMetrics fm = g2.getFontMetrics();
            Rectangle2D r = fm.getStringBounds(getText(), g2);
            int x = (getWidth() - (int) r.getWidth()) / 2;
            int y = (getHeight() - (int) r.getHeight()) / 2 + fm.getAscent();
            
            g2.setColor(getForeground());
            g2.drawString(getText(), x, y);
        }
    }
    
    // Form fields
    private RoundedTextField fullNameField;
    private RoundedTextField usernameField;
    private RoundedTextField emailField;
    private RoundedPasswordField passwordField;
    private RoundedPasswordField confirmPasswordField;
    private RoundedComboBox roleComboBox;
    
    // OTP related fields
    private RoundedTextField otpField;
    private RoundedButton sendOTPButton;
    private RoundedButton verifyButton;
    private JPanel otpPanel;
    private String generatedOTP;
    private boolean emailVerified = false;
    private JPanel contentPanel;
    private RoundedButton signUpButton;

    public CreateAccountScreen() {
        // Set Look and Feel
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("Create Account - Academic Analyzer");
        setSize(500, 800); // Increased height to accommodate OTP fields
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
                
                // Create gradient background
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
        cardPanel.setPreferredSize(new Dimension(420, 700));
        
        // Inner content panel
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        // Title
        JLabel titleLabel = new JLabel("Create Account");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setForeground(new Color(30, 30, 30));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Create form fields
        fullNameField = new RoundedTextField(25, "Full Name");
        fullNameField.setMaximumSize(new Dimension(340, 50));
        fullNameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        addPlaceholderBehavior(fullNameField, "Full Name");
        
        usernameField = new RoundedTextField(25, "Username");
        usernameField.setMaximumSize(new Dimension(340, 50));
        usernameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        addPlaceholderBehavior(usernameField, "Username");
        
        emailField = new RoundedTextField(25, "Email");
        emailField.setMaximumSize(new Dimension(340, 50));
        emailField.setAlignmentX(Component.CENTER_ALIGNMENT);
        addPlaceholderBehavior(emailField, "Email");
        
        // Send OTP button
        sendOTPButton = new RoundedButton("Send OTP", 25, 
            new Color(100, 120, 200), new Color(80, 100, 180));
        sendOTPButton.setFont(new Font("Arial", Font.BOLD, 14));
        sendOTPButton.setForeground(Color.WHITE);
        sendOTPButton.setMaximumSize(new Dimension(340, 40));
        sendOTPButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        sendOTPButton.addActionListener(e -> sendOTP());
        
        // OTP Panel (initially hidden)
        otpPanel = new JPanel();
        otpPanel.setLayout(new BoxLayout(otpPanel, BoxLayout.Y_AXIS));
        otpPanel.setOpaque(false);
        otpPanel.setVisible(false);
        
        otpField = new RoundedTextField(25, "Enter OTP");
        otpField.setMaximumSize(new Dimension(340, 50));
        otpField.setAlignmentX(Component.CENTER_ALIGNMENT);
        addPlaceholderBehavior(otpField, "Enter OTP");
        
        verifyButton = new RoundedButton("Verify OTP", 25, 
            new Color(34, 197, 94), new Color(22, 163, 74));
        verifyButton.setFont(new Font("Arial", Font.BOLD, 14));
        verifyButton.setForeground(Color.WHITE);
        verifyButton.setMaximumSize(new Dimension(340, 40));
        verifyButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        verifyButton.addActionListener(e -> verifyOTP());
        
        otpPanel.add(otpField);
        otpPanel.add(Box.createVerticalStrut(10));
        otpPanel.add(verifyButton);
        
        passwordField = new RoundedPasswordField(25, "Password");
        passwordField.setMaximumSize(new Dimension(340, 50));
        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        addPasswordPlaceholderBehavior(passwordField, "Password");
        
        confirmPasswordField = new RoundedPasswordField(25, "Confirm Password");
        confirmPasswordField.setMaximumSize(new Dimension(340, 50));
        confirmPasswordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        addPasswordPlaceholderBehavior(confirmPasswordField, "Confirm Password");
        
        // Role dropdown with label
        JPanel rolePanel = new JPanel();
        rolePanel.setLayout(new BoxLayout(rolePanel, BoxLayout.X_AXIS));
        rolePanel.setOpaque(false);
        rolePanel.setMaximumSize(new Dimension(340, 50));
        rolePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel roleLabel = new JLabel("Role");
        roleLabel.setFont(new Font("Arial", Font.PLAIN, 15));
        roleLabel.setForeground(new Color(100, 100, 100));
        roleLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        
        String[] roles = {"Student", "Teacher", "Admin"};
        roleComboBox = new RoundedComboBox(roles, 25);
        roleComboBox.setMaximumSize(new Dimension(200, 50));
        
        rolePanel.add(roleLabel);
        rolePanel.add(Box.createHorizontalGlue());
        rolePanel.add(roleComboBox);
        
        // Sign Up button (initially disabled)
        signUpButton = new RoundedButton("Sign Up", 25, 
            new Color(100, 120, 200), new Color(80, 100, 180));
        signUpButton.setFont(new Font("Arial", Font.BOLD, 16));
        signUpButton.setForeground(Color.WHITE);
        signUpButton.setMaximumSize(new Dimension(340, 50));
        signUpButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        signUpButton.setEnabled(false); // Disabled until email is verified
        
        // Add components to content panel
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(30));
        contentPanel.add(fullNameField);
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(usernameField);
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(emailField);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(sendOTPButton);
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(otpPanel);
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(passwordField);
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(confirmPasswordField);
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(rolePanel);
        contentPanel.add(Box.createVerticalStrut(30));
        contentPanel.add(signUpButton);
        
        // Cancel link at bottom
        JLabel cancelLabel = new JLabel("Cancel");
        cancelLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        cancelLabel.setForeground(new Color(100, 100, 100));
        cancelLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancelLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                dispose();
                new LoginScreen();
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                cancelLabel.setText("<html><u>Cancel</u></html>");
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                cancelLabel.setText("Cancel");
            }
        });
        
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(cancelLabel);
        
        cardPanel.add(contentPanel);
        
        // Add card panel to main container
        mainContainer.add(cardPanel);
        
        // Sign up button action
        signUpButton.addActionListener(e -> handleSignUp());
        
        add(mainContainer);
        setVisible(true);
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
        
        // Check if email already exists
        if (checkEmailExists(email)) {
            JOptionPane.showMessageDialog(this, 
                "An account with this email already exists", 
                "Email Already Registered", 
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
            
            // Show OTP panel
            otpPanel.setVisible(true);
            sendOTPButton.setText("Resend OTP");
            emailField.setEditable(false); // Lock email field after sending OTP
            
            // Refresh the layout
            contentPanel.revalidate();
            contentPanel.repaint();
        } else {
            JOptionPane.showMessageDialog(this, 
                "Failed to generate OTP. Please try again.", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void verifyOTP() {
        String enteredOTP = otpField.getText();
        
        if (enteredOTP.equals("Enter OTP") || enteredOTP.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please enter the OTP", 
                "OTP Required", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (enteredOTP.equals(generatedOTP)) {
            emailVerified = true;
            signUpButton.setEnabled(true);
            
            // Update UI to show verification success
            verifyButton.setText("✓ Verified");
            verifyButton.setBackground(new Color(34, 197, 94));
            verifyButton.setEnabled(false);
            otpField.setEditable(false);
            sendOTPButton.setEnabled(false);
            
            JOptionPane.showMessageDialog(this, 
                "Email verified successfully! You can now complete your registration.", 
                "Verification Successful", 
                JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, 
                "Invalid OTP. Please try again.", 
                "Invalid OTP", 
                JOptionPane.ERROR_MESSAGE);
            otpField.setText("");
            otpField.setShowingPlaceholder(true);
            otpField.setText("Enter OTP");
            otpField.setForeground(new Color(150, 150, 150));
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
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.emailjs.com/api/v1.0/email/send"))
                .header("Content-Type", "application/json")
                .header("origin", "http://localhost")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
            
            HttpResponse<String> response = client.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() == 200;
            
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
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    private boolean storeOTPInDatabase(String email, String otp) {
        try (Connection conn = DatabaseConnection.getConnection()) {
        
            // Create table if not exists
            String createTableSql = "CREATE TABLE IF NOT EXISTS registration_otps (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "email VARCHAR(100) NOT NULL, " +
                "otp VARCHAR(6) NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "expires_at TIMESTAMP NULL, " +
                "used BOOLEAN DEFAULT FALSE, " +
                "INDEX idx_email_otp (email, otp))";
            
            // Invalidate old OTPs and insert new one
            String invalidateSql = "UPDATE registration_otps SET used = TRUE WHERE email = ? AND used = FALSE";
            String insertSql = "INSERT INTO registration_otps (email, otp, expires_at) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 10 MINUTE))";
            
            // Create table if needed
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createTableSql);
            }
            
            // Invalidate old OTPs
            try (PreparedStatement pstmt = conn.prepareStatement(invalidateSql)) {
                pstmt.setString(1, email);
                pstmt.executeUpdate();
            }
            
            // Insert new OTP
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setString(1, email);
                pstmt.setString(2, otp);
                return pstmt.executeUpdate() > 0;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
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
    
    private void addPasswordPlaceholderBehavior(RoundedPasswordField field, String placeholder) {
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent evt) {
                if (field.isShowingPlaceholder() && new String(field.getPassword()).equals(placeholder)) {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                    field.setEchoChar('•');
                    field.setShowingPlaceholder(false);
                }
            }
            
            @Override
            public void focusLost(FocusEvent evt) {
                if (field.getPassword().length == 0) {
                    field.setText(placeholder);
                    field.setForeground(new Color(150, 150, 150));
                    field.setEchoChar((char) 0);
                    field.setShowingPlaceholder(true);
                }
            }
        });
    }
    
    private void handleSignUp() {
        // Check if email is verified
        if (!emailVerified) {
            JOptionPane.showMessageDialog(this, 
                "Please verify your email before creating an account", 
                "Email Not Verified", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Get form values
        String fullName = fullNameField.getText();
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        String role = (String) roleComboBox.getSelectedItem();
        
        // Validate inputs
        if (fullName.equals("Full Name") || username.equals("Username") || 
            email.equals("Email") || password.equals("Password") || 
            confirmPassword.equals("Confirm Password")) {
            JOptionPane.showMessageDialog(this, 
                "Please fill in all fields", 
                "Incomplete Form", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || 
            password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please fill in all fields", 
                "Incomplete Form", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Check if passwords match
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, 
                    "Passwords do not match. Please enter the correct password.", 
                    "Password Mismatch", 
                    JOptionPane.ERROR_MESSAGE);
                passwordField.setText("");
                confirmPasswordField.setText("");
                passwordField.setShowingPlaceholder(true);
                confirmPasswordField.setShowingPlaceholder(true);
                passwordField.setText("Password");
                confirmPasswordField.setText("Confirm Password");
                passwordField.setForeground(new Color(150, 150, 150));
                confirmPasswordField.setForeground(new Color(150, 150, 150));
                passwordField.setEchoChar((char) 0);
                confirmPasswordField.setEchoChar((char) 0);
                return;
            }
            
            // Password strength check
            if (password.length() < 6) {
                JOptionPane.showMessageDialog(this, 
                    "Password must be at least 6 characters long", 
                    "Weak Password", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Save to database
            if (saveUserToDatabase(fullName, username, email, password, role)) {
                JOptionPane.showMessageDialog(this, 
                    "Account created successfully! Please login with your credentials.", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
                
                // Go back to login screen
                dispose();
                new LoginScreen();
            }
        }
        
        private boolean saveUserToDatabase(String fullName, String username, String email, String password, String role) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "INSERT INTO users (full_name, username, email, password, role) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                
                // Insert new user
                pstmt.setString(1, fullName);
                pstmt.setString(2, username);
                pstmt.setString(3, email);
                pstmt.setString(4, password); // In production, hash the password!
                pstmt.setString(5, role.toLowerCase());
                
                int rowsAffected = pstmt.executeUpdate();
                
                // Mark OTP as used after successful registration
                if (rowsAffected > 0) {
                    markOTPAsUsed(email);
                }
                
                return rowsAffected > 0;
                
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, 
                    "Database error: " + e.getMessage(), 
                    "Database Error", 
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                return false;
            }
        }
        
        private void markOTPAsUsed(String email) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "UPDATE registration_otps SET used = TRUE WHERE email = ? AND used = FALSE";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                
                pstmt.setString(1, email);
                pstmt.executeUpdate();
                
            } catch (SQLException e) {
                // Log error but don't show to user as registration was successful
                e.printStackTrace();
            }
        }
        
        public static void main(String[] args) {
            SwingUtilities.invokeLater(() -> {
                // Enable anti-aliasing
                System.setProperty("awt.useSystemAAFontSettings", "on");
                System.setProperty("swing.aatext", "true");
                
                new CreateAccountScreen();
            });
        }
    }