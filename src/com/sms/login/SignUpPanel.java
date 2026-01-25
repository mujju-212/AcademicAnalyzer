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
 * Modern sign up panel with white/purple theme, glassmorphism look
 */
public class SignUpPanel extends JPanel {
    private AuthenticationFrame parentFrame;
    private JTextField usernameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JComboBox<String> roleComboBox;
    private JTextField otpField;
    private JButton sendOTPButton;
    
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
    
    public SignUpPanel(AuthenticationFrame parentFrame) {
        this.parentFrame = parentFrame;
        initializeUI();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);
        
        // Create scrollable content
        JPanel contentPanel = createContentPanel();
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR);
        
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private JPanel createContentPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(BACKGROUND_COLOR);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(30, 20, 30, 20);
        
        // White card panel
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(40, 50, 40, 50)
        ));
        card.setPreferredSize(new Dimension(520, 950));
        
        // Logo - load from resources folder directly
        try {
            java.io.File logoFile = new java.io.File("resources/images/AA LOGO.png");
            if (logoFile.exists()) {
                ImageIcon logoIcon = new ImageIcon(logoFile.getAbsolutePath());
                Image scaledImage = logoIcon.getImage().getScaledInstance(150, 90, Image.SCALE_SMOOTH);
                JLabel logoLabel = new JLabel(new ImageIcon(scaledImage));
                logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                card.add(logoLabel);
                card.add(Box.createVerticalStrut(18));
            }
        } catch (Exception e) {
            // Continue without logo
        }
        
        // Title
        JLabel titleLabel = new JLabel("Create Account");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(6));
        
        JLabel subtitleLabel = new JLabel("Join Academic Analyzer");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(subtitleLabel);
        card.add(Box.createVerticalStrut(30));
        
        // Username with icon
        usernameField = createStyledTextField("👤", "Username");
        usernameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(usernameField);
        card.add(Box.createVerticalStrut(15));
        
        // Email with icon
        emailField = createStyledTextField("📧", "Email Address");
        emailField.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(emailField);
        card.add(Box.createVerticalStrut(15));
        
        // Send OTP Button
        sendOTPButton = createStyledButton("Send OTP", true);
        sendOTPButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        sendOTPButton.addActionListener(e -> sendOTP());
        card.add(sendOTPButton);
        card.add(Box.createVerticalStrut(15));
        
        // OTP Field with icon
        otpField = createStyledTextField("#️⃣", "Enter OTP");
        otpField.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(otpField);
        card.add(Box.createVerticalStrut(15));
        
        // Password with icon
        passwordField = createStyledPasswordField("🔒", "Password");
        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(passwordField);
        card.add(Box.createVerticalStrut(15));
        
        // Confirm Password with icon
        confirmPasswordField = createStyledPasswordField("🔒", "Confirm Password");
        confirmPasswordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(confirmPasswordField);
        card.add(Box.createVerticalStrut(15));
        
        // Role with icon
        roleComboBox = createStyledComboBox("🎭", new String[]{"Teacher", "Student", "Admin"});
        roleComboBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(roleComboBox);
        card.add(Box.createVerticalStrut(25));
        
        // Sign Up Button
        JButton signUpButton = createStyledButton("Create Account", true);
        signUpButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        signUpButton.addActionListener(e -> handleSignUp());
        card.add(signUpButton);
        card.add(Box.createVerticalStrut(20));
        
        // Already have account link
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        linkPanel.setOpaque(false);
        linkPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        JLabel haveAccountLabel = new JLabel("Already have an account?");
        haveAccountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        haveAccountLabel.setForeground(TEXT_SECONDARY);
        
        JLabel loginLink = new JLabel("Sign in");
        loginLink.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        loginLink.setForeground(TEXT_SECONDARY);
        loginLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginLink.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                parentFrame.showLoginView();
            }
            public void mouseEntered(MouseEvent e) {
                loginLink.setForeground(PRIMARY_COLOR);
            }
            public void mouseExited(MouseEvent e) {
                loginLink.setForeground(TEXT_SECONDARY);
            }
        });
        
        linkPanel.add(haveAccountLabel);
        linkPanel.add(loginLink);
        card.add(linkPanel);
        
        wrapper.add(card, gbc);
        return wrapper;
    }
    
    private JTextField createStyledTextField(String icon, String placeholder) {
        JPanel wrapper = new JPanel(new BorderLayout(10, 0));
        wrapper.setOpaque(true);
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        wrapper.setMaximumSize(new Dimension(420, 50));
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        iconLabel.setForeground(TEXT_SECONDARY);
        
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setForeground(TEXT_PRIMARY);
        field.setBorder(null);
        field.setOpaque(false);
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
        result.setPreferredSize(new Dimension(420, 50));
        result.setMaximumSize(new Dimension(420, 50));
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
        wrapper.setMaximumSize(new Dimension(420, 50));
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        iconLabel.setForeground(TEXT_SECONDARY);
        
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
        
        JPasswordField result = new JPasswordField() {
            @Override
            public char[] getPassword() {
                String text = String.valueOf(field.getPassword());
                return text.equals(placeholder) ? new char[0] : text.toCharArray();
            }
        };
        result.setPreferredSize(new Dimension(420, 50));
        result.setMaximumSize(new Dimension(420, 50));
        result.setLayout(new BorderLayout());
        result.add(wrapper);
        result.setOpaque(false);
        result.setBorder(null);
        
        return result;
    }
    
    private JComboBox<String> createStyledComboBox(String icon, String[] items) {
        JPanel wrapper = new JPanel(new BorderLayout(10, 0));
        wrapper.setOpaque(true);
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        wrapper.setMaximumSize(new Dimension(420, 50));
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        iconLabel.setForeground(TEXT_SECONDARY);
        
        JComboBox<String> combo = new JComboBox<>(items);
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        combo.setBackground(Color.WHITE);
        combo.setBorder(null);
        combo.setFocusable(false);
        
        wrapper.add(iconLabel, BorderLayout.WEST);
        wrapper.add(combo, BorderLayout.CENTER);
        
        JComboBox<String> result = new JComboBox<>(items);
        result.setPreferredSize(new Dimension(420, 50));
        result.setMaximumSize(new Dimension(420, 50));
        result.setLayout(new BorderLayout());
        result.add(wrapper);
        result.setOpaque(false);
        result.setBorder(null);
        result.setRenderer(new DefaultListCellRenderer());
        
        combo.addActionListener(e -> result.setSelectedIndex(combo.getSelectedIndex()));
        
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
        button.setPreferredSize(new Dimension(420, 50));
        button.setMaximumSize(new Dimension(420, 50));
        
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
    
    private void sendOTP() {
        String email = emailField.getText().trim();
        
        if (email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter email address", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!isValidEmail(email)) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        Random random = new Random();
        generatedOTP = String.format("%06d", random.nextInt(1000000));
        
        boolean emailSent = sendEmailViaMailerSend(email, generatedOTP);
        
        if (emailSent) {
            JOptionPane.showMessageDialog(this, "OTP has been sent to your email", "Success", JOptionPane.INFORMATION_MESSAGE);
            otpSent = true;
            // Enable OTP field
            sendOTPButton.setEnabled(false);
        } else {
            JOptionPane.showMessageDialog(this, "Failed to send OTP. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private boolean sendEmailViaMailerSend(String toEmail, String otp) {
        try {
            URL url = new URL("https://api.mailersend.com/v1/email");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + MAILERSEND_API_KEY);
            conn.setDoOutput(true);
            
            String jsonPayload = String.format(
                "{\"from\":{\"email\":\"%s\",\"name\":\"%s\"}," +
                "\"to\":[{\"email\":\"%s\"}]," +
                "\"subject\":\"Your OTP for Academic Analyzer\"," +
                "\"text\":\"Your OTP is: %s. This code is valid for 10 minutes.\"," +
                "\"html\":\"<p>Your OTP is: <strong>%s</strong></p><p>This code is valid for 10 minutes.</p>\"}",
                MAILERSEND_FROM_EMAIL, MAILERSEND_FROM_NAME, toEmail, otp, otp
            );
            
            try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(jsonPayload);
                writer.flush();
            }
            
            int responseCode = conn.getResponseCode();
            return responseCode == 200 || responseCode == 202;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private void handleSignUp() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String otp = otpField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        String role = (String) roleComboBox.getSelectedItem();
        
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!otpSent) {
            JOptionPane.showMessageDialog(this, "Please send OTP first", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!otp.equals(generatedOTP)) {
            JOptionPane.showMessageDialog(this, "Invalid OTP", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String checkEmailSQL = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(checkEmailSQL)) {
            
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next() && rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this, "Email already registered", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String insertSQL = "INSERT INTO users (username, email, password, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            pstmt.setString(4, role);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "Account created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                parentFrame.showLoginView();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to create account", "Error", JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
}
