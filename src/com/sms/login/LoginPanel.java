package com.sms.login;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import com.sms.database.DatabaseConnection;

/**
 * Modern login panel with white/purple theme, glassmorphism look
 */
public class LoginPanel extends JPanel {
    private AuthenticationFrame parentFrame;
    private JTextField emailField;
    private JPasswordField passwordField;
    
    // Dashboard colors
    private static final Color BACKGROUND_COLOR = new Color(248, 250, 252);
    private static final Color CARD_BACKGROUND = Color.WHITE;
    private static final Color PRIMARY_COLOR = new Color(99, 102, 241);
    private static final Color PRIMARY_HOVER = new Color(79, 82, 221);
    private static final Color TEXT_PRIMARY = new Color(17, 24, 39);
    private static final Color TEXT_SECONDARY = new Color(107, 114, 128);
    private static final Color BORDER_COLOR = new Color(229, 231, 235);
    
    public LoginPanel(AuthenticationFrame parent) {
        this.parentFrame = parent;
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);
        initializeComponents();
    }
    
    private void initializeComponents() {
        // Center container
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(BACKGROUND_COLOR);
        
        // Main card
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout());
        card.setBackground(CARD_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(50, 50, 50, 50)
        ));
        card.setPreferredSize(new Dimension(500, 600));
        
        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        
        // Logo
        try {
            java.net.URL logoURL = getClass().getClassLoader().getResource("resources/images/AA LOGO.png");
            if (logoURL != null) {
                ImageIcon logoIcon = new ImageIcon(logoURL);
                Image scaledImage = logoIcon.getImage().getScaledInstance(150, 90, Image.SCALE_SMOOTH);
                JLabel logoLabel = new JLabel(new ImageIcon(scaledImage));
                logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                contentPanel.add(logoLabel);
                contentPanel.add(Box.createVerticalStrut(18));
            }
        } catch (Exception e) {
            // Continue without logo
        }
        
        // Title
        JLabel titleLabel = new JLabel("Academic Analyzer");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(6));
        
        JLabel subtitleLabel = new JLabel("Sign in to your account");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(subtitleLabel);
        contentPanel.add(Box.createVerticalStrut(30));
        
        // Email field with icon
        emailField = createStyledTextField("📧", "Email");
        emailField.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(emailField);
        contentPanel.add(Box.createVerticalStrut(15));
        
        // Password field with icon
        passwordField = createStyledPasswordField("🔒", "Password");
        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(passwordField);
        contentPanel.add(Box.createVerticalStrut(12));
        
        // Forgot password link
        JPanel forgotPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        forgotPanel.setOpaque(false);
        forgotPanel.setMaximumSize(new Dimension(400, 25));
        
        JLabel forgotLink = new JLabel("Forgot password?");
        forgotLink.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        forgotLink.setForeground(TEXT_SECONDARY);
        forgotLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        forgotLink.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                parentFrame.showForgotPasswordView();
            }
            public void mouseEntered(MouseEvent e) {
                forgotLink.setForeground(PRIMARY_COLOR);
            }
            public void mouseExited(MouseEvent e) {
                forgotLink.setForeground(TEXT_SECONDARY);
            }
        });
        
        forgotPanel.add(forgotLink);
        contentPanel.add(forgotPanel);
        contentPanel.add(Box.createVerticalStrut(25));
        
        // Sign in button
        JButton loginButton = createStyledButton("Sign in", true);
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.addActionListener(e -> handleLogin());
        contentPanel.add(loginButton);
        contentPanel.add(Box.createVerticalStrut(20));
        
        // Divider
        JPanel divider = new JPanel();
        divider.setLayout(new BoxLayout(divider, BoxLayout.X_AXIS));
        divider.setOpaque(false);
        divider.setMaximumSize(new Dimension(400, 20));
        
        JSeparator sep1 = new JSeparator();
        sep1.setForeground(BORDER_COLOR);
        JLabel orLabel = new JLabel("  or  ");
        orLabel.setForeground(TEXT_SECONDARY);
        orLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JSeparator sep2 = new JSeparator();
        sep2.setForeground(BORDER_COLOR);
        
        divider.add(sep1);
        divider.add(orLabel);
        divider.add(sep2);
        contentPanel.add(divider);
        contentPanel.add(Box.createVerticalStrut(20));
        
        // Create account button
        JButton createButton = createStyledButton("Create new account", false);
        createButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        createButton.addActionListener(e -> parentFrame.showSignUpView());
        contentPanel.add(createButton);
        
        card.add(contentPanel, BorderLayout.CENTER);
        centerPanel.add(card);
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
        
        field.addActionListener(e -> handleLogin());
        
        wrapper.add(iconLabel, BorderLayout.WEST);
        wrapper.add(field, BorderLayout.CENTER);
        
        // Return wrapper as JTextField
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
        
        field.addActionListener(e -> handleLogin());
        
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
        button.setForeground(isPrimary ? Color.WHITE : PRIMARY_COLOR);
        button.setBackground(isPrimary ? PRIMARY_COLOR : Color.WHITE);
        button.setBorder(isPrimary ? null : BorderFactory.createLineBorder(PRIMARY_COLOR, 1));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(400, 50));
        button.setMaximumSize(new Dimension(400, 50));
        
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (isPrimary) {
                    button.setBackground(PRIMARY_HOVER);
                } else {
                    button.setBackground(new Color(249, 250, 251));
                }
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(isPrimary ? PRIMARY_COLOR : Color.WHITE);
            }
        });
        
        return button;
    }
    
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter your email and password",
                "Login Failed",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String sql = "SELECT id, username FROM users WHERE email = ? AND password = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int userId = rs.getInt("id");
                parentFrame.onLoginSuccess(userId);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Invalid email or password",
                    "Login Failed",
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
}
