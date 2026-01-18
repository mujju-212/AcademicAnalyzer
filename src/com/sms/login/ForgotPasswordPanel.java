package com.sms.login;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.sql.*;
import java.util.Random;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import com.sms.database.DatabaseConnection;
import com.sms.util.ConfigLoader;

/**
 * Forgot password panel with clean white/purple theme, same look as glassmorphism
 */
public class ForgotPasswordPanel extends JPanel {
    private AuthenticationFrame parentFrame;
    private JTextField emailField;
    private JTextField otpField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;
    private JButton sendOTPButton;
    private JButton resetPasswordButton;
    
    private String generatedOTP = "";
    private boolean otpSent = false;
    
    // Dashboard colors
    private static final Color BACKGROUND_COLOR = new Color(248, 250, 252);
    private static final Color CARD_BACKGROUND = Color.WHITE;
    private static final Color PRIMARY_COLOR = new Color(99, 102, 241);
    private static final Color PRIMARY_HOVER = new Color(79, 82, 221);
    private static final Color TEXT_PRIMARY = new Color(17, 24, 39);
    private static final Color TEXT_SECONDARY = new Color(107, 114, 128);
    private static final Color BORDER_COLOR = new Color(229, 231, 235);
    
    // MailerSend Configuration
    private static final String MAILERSEND_API_KEY = ConfigLoader.getMailerSendApiKey();
    private static final String MAILERSEND_FROM_EMAIL = ConfigLoader.getMailerSendFromEmail();
    private static final String MAILERSEND_FROM_NAME = ConfigLoader.getMailerSendFromName();
    
    public ForgotPasswordPanel(AuthenticationFrame parentFrame) {
        this.parentFrame = parentFrame;
        initializeUI();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);
        
        // Center container
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(BACKGROUND_COLOR);
        
        // Main glass-style panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(CARD_BACKGROUND);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(50, 50, 50, 50)
        ));
        mainPanel.setPreferredSize(new Dimension(500, 600));
        
        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        
        // Title
        JLabel titleLabel = new JLabel("Reset Password");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel subtitleLabel = new JLabel("Enter your email to receive OTP");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(subtitleLabel);
        contentPanel.add(Box.createVerticalStrut(40));
        
        // Email field with icon
        emailField = createStyledTextField("📧", "Email");
        emailField.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(emailField);
        contentPanel.add(Box.createVerticalStrut(15));
        
        // Send OTP button
        sendOTPButton = createStyledButton("Send OTP", true);
        sendOTPButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        sendOTPButton.addActionListener(e -> handleSendOTP());
        contentPanel.add(sendOTPButton);
        contentPanel.add(Box.createVerticalStrut(15));
        
        // OTP field with icon
        otpField = createStyledTextField("#️⃣", "Enter OTP");
        otpField.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(otpField);
        contentPanel.add(Box.createVerticalStrut(15));
        
        // New password field with icon
        newPasswordField = createStyledPasswordField("🔒", "New Password");
        newPasswordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(newPasswordField);
        contentPanel.add(Box.createVerticalStrut(15));
        
        // Confirm password field with icon
        confirmPasswordField = createStyledPasswordField("🔒", "Confirm Password");
        confirmPasswordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(confirmPasswordField);
        contentPanel.add(Box.createVerticalStrut(25));
        
        // Reset password button
        resetPasswordButton = createStyledButton("Reset Password", true);
        resetPasswordButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        resetPasswordButton.addActionListener(e -> handleResetPassword());
        contentPanel.add(resetPasswordButton);
        contentPanel.add(Box.createVerticalStrut(15));
        
        // Back to login link
        JLabel backLink = new JLabel("Back to Login");
        backLink.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        backLink.setForeground(TEXT_SECONDARY);
        backLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backLink.setAlignmentX(Component.CENTER_ALIGNMENT);
        backLink.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                parentFrame.showLoginView();
            }
            public void mouseEntered(MouseEvent e) {
                backLink.setForeground(PRIMARY_COLOR);
            }
            public void mouseExited(MouseEvent e) {
                backLink.setForeground(TEXT_SECONDARY);
            }
        });
        contentPanel.add(backLink);
        
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        centerPanel.add(mainPanel);
        add(centerPanel, BorderLayout.CENTER);
    }
    
    private JTextField createStyledTextField(String icon, String placeholder) {
        JPanel wrapper = new JPanel(new BorderLayout(10, 0));
        wrapper.setOpaque(true);
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        wrapper.setMaximumSize(new Dimension(400, 50));
        
        // Icon label
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        iconLabel.setForeground(TEXT_SECONDARY);
        
        // Text field
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setForeground(TEXT_PRIMARY);
        field.setBorder(null);
        field.setOpaque(false);
        
        // Placeholder functionality
        field.setText(placeholder);
        field.setForeground(TEXT_SECONDARY);
        
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(TEXT_PRIMARY);
                }
                wrapper.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(PRIMARY_COLOR, 1),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)
                ));
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(TEXT_SECONDARY);
                }
                wrapper.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR, 1),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)
                ));
            }
        });
        
        wrapper.add(iconLabel, BorderLayout.WEST);
        wrapper.add(field, BorderLayout.CENTER);
        
        // Return wrapper as JTextField for easy text access
        JTextField result = new JTextField() {
            @Override
            public String getText() {
                String text = field.getText();
                return text.equals(placeholder) ? "" : text;
            }
            
            @Override
            public void setText(String text) {
                field.setText(text);
            }
        };
        result.setPreferredSize(new Dimension(400, 50));
        result.setMaximumSize(new Dimension(400, 50));
        result.setLayout(new BorderLayout());
        result.add(wrapper);
        result.setOpaque(false);
        result.setBorder(null);
        
        return result;
    }
    
    private JPasswordField createStyledPasswordField(String icon, String placeholder) {
        JPanel wrapper = new JPanel(new BorderLayout(10, 0));
        wrapper.setOpaque(true);
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        wrapper.setMaximumSize(new Dimension(400, 50));
        
        // Icon label
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        iconLabel.setForeground(TEXT_SECONDARY);
        
        // Password field
        JPasswordField field = new JPasswordField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setForeground(TEXT_PRIMARY);
        field.setBorder(null);
        field.setOpaque(false);
        field.setEchoChar((char) 0);
        field.setText(placeholder);
        field.setForeground(TEXT_SECONDARY);
        
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (String.valueOf(field.getPassword()).equals(placeholder)) {
                    field.setText("");
                    field.setEchoChar('●');
                    field.setForeground(TEXT_PRIMARY);
                }
                wrapper.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(PRIMARY_COLOR, 1),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)
                ));
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                if (field.getPassword().length == 0) {
                    field.setEchoChar((char) 0);
                    field.setText(placeholder);
                    field.setForeground(TEXT_SECONDARY);
                }
                wrapper.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR, 1),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)
                ));
            }
        });
        
        wrapper.add(iconLabel, BorderLayout.WEST);
        wrapper.add(field, BorderLayout.CENTER);
        
        // Return wrapper as JPasswordField
        JPasswordField result = new JPasswordField() {
            @Override
            public char[] getPassword() {
                String text = String.valueOf(field.getPassword());
                return text.equals(placeholder) ? new char[0] : text.toCharArray();
            }
        };
        result.setPreferredSize(new Dimension(400, 50));
        result.setMaximumSize(new Dimension(400, 50));
        result.setLayout(new BorderLayout());
        result.add(wrapper);
        result.setOpaque(false);
        result.setBorder(null);
        
        return result;
    }
    
    private JButton createStyledButton(String text, boolean isPrimary) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(PRIMARY_COLOR);
        button.setBorder(null);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(400, 50));
        button.setMaximumSize(new Dimension(400, 50));
        
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(PRIMARY_HOVER);
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(PRIMARY_COLOR);
            }
        });
        
        return button;
    }
    
    private void handleSendOTP() {
        String email = emailField.getText().trim();
        
        if (email.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter your email address",
                "Email Required",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Check if email exists
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM users WHERE email = ?")) {
            
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            
            if (!rs.next()) {
                JOptionPane.showMessageDialog(this,
                    "Email not found",
                    "Invalid Email",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Database error: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Generate and send OTP
        generatedOTP = generateOTP();
        
        if (sendOTPEmail(email, generatedOTP)) {
            otpSent = true;
            JOptionPane.showMessageDialog(this,
                "OTP sent to your email!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
            sendOTPButton.setEnabled(false);
        } else {
            JOptionPane.showMessageDialog(this,
                "Failed to send OTP. Please try again.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void handleResetPassword() {
        String email = emailField.getText().trim();
        String otp = otpField.getText().trim();
        String newPassword = new String(newPasswordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        
        // Validation
        if (email.isEmpty() || otp.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please fill in all fields",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!otpSent) {
            JOptionPane.showMessageDialog(this,
                "Please request OTP first",
                "OTP Required",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (!otp.equals(generatedOTP)) {
            JOptionPane.showMessageDialog(this,
                "Invalid OTP. Please check and try again.",
                "Invalid OTP",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this,
                "Passwords do not match",
                "Password Mismatch",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (newPassword.length() < 6) {
            JOptionPane.showMessageDialog(this,
                "Password must be at least 6 characters long",
                "Weak Password",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Update password in database
        String sql = "UPDATE users SET password = ? WHERE email = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, newPassword);
            pstmt.setString(2, email);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this,
                    "Password reset successful!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
                parentFrame.showLoginView();
            } else {
                JOptionPane.showMessageDialog(this,
                    "Failed to reset password",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Database error: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
    
    private boolean sendOTPEmail(String toEmail, String otp) {
        try {
            URL url = new URL("https://api.mailersend.com/v1/email");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + MAILERSEND_API_KEY);
            conn.setDoOutput(true);
            
            String jsonBody = "{"
                + "\"from\":{\"email\":\"" + escapeJson(MAILERSEND_FROM_EMAIL) + "\",\"name\":\"" + escapeJson(MAILERSEND_FROM_NAME) + "\"},"
                + "\"to\":[{\"email\":\"" + escapeJson(toEmail) + "\"}],"
                + "\"subject\":\"Password Reset OTP - Academic Analyzer\","
                + "\"text\":\"Your password reset OTP is: " + otp + "\","
                + "\"html\":\"" + escapeJson(createOTPEmailHTML(otp)) + "\""
                + "}";
            
            try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(jsonBody);
                writer.flush();
            }
            
            int responseCode = conn.getResponseCode();
            return responseCode == 200 || responseCode == 202;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private String createOTPEmailHTML(String otp) {
        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%); padding: 40px; border-radius: 10px;'>"
            + "<div style='background: white; padding: 40px; border-radius: 10px; text-align: center;'>"
            + "<h1 style='color: #6366f1; margin-bottom: 20px;'>🔐 Password Reset</h1>"
            + "<p style='color: #666; font-size: 16px; margin-bottom: 30px;'>Your password reset OTP is:</p>"
            + "<div style='background: #f5f3ff; border: 2px solid #6366f1; padding: 20px; border-radius: 10px; font-size: 36px; font-weight: bold; letter-spacing: 8px; color: #6366f1; margin-bottom: 30px;'>"
            + otp + "</div>"
            + "<p style='color: #999; font-size: 14px;'>This code will expire in 10 minutes.</p>"
            + "<p style='color: #dc2626; font-size: 14px; font-weight: bold;'>If you didn't request this, please secure your account immediately!</p>"
            + "</div></div>";
    }
    
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r");
    }
}
