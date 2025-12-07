package com.sms.login;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.border.*;
import com.formdev.flatlaf.FlatLightLaf;
import java.sql.*;

public class ResetPasswordScreen extends JFrame {
    
    // Reuse custom components (same as above)
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
            
            for (int i = 0; i < shadowSize; i++) {
                g2.setColor(new Color(0, 0, 0, 20 - (i * 4)));
                g2.fill(new RoundRectangle2D.Float(i, i, getWidth() - (i * 2), getHeight() - (i * 2), cornerRadius, cornerRadius));
            }
            
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Float(shadowSize, shadowSize, getWidth() - (shadowSize * 2), getHeight() - (shadowSize * 2), cornerRadius, cornerRadius));
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
            
            setText(placeholder);
            setForeground(new Color(150, 150, 150));
            setEchoChar((char) 0);
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
    
    private RoundedPasswordField newPasswordField;
    private RoundedPasswordField confirmPasswordField;
    private String email;

    public ResetPasswordScreen(String email) {
        this.email = email;
        
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("Reset Password - Academic Analyzer");
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
        JLabel titleLabel = new JLabel("Reset Password");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(30, 30, 30));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Description
        JLabel descLabel = new JLabel("<html><center>Enter your new password below</center></html>");
        descLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        descLabel.setForeground(new Color(100, 100, 100));
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        descLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // New password field
        newPasswordField = new RoundedPasswordField(25, "Enter new password");
        newPasswordField.setMaximumSize(new Dimension(340, 50));
        newPasswordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        addPasswordPlaceholderBehavior(newPasswordField, "Enter new password");
        
        // Confirm password field
        confirmPasswordField = new RoundedPasswordField(25, "Enter password again");
        confirmPasswordField.setMaximumSize(new Dimension(340, 50));
        confirmPasswordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        addPasswordPlaceholderBehavior(confirmPasswordField, "Enter password again");
        
        // Reset button
        RoundedButton resetButton = new RoundedButton("Reset Password", 25, 
            new Color(100, 120, 200), new Color(80, 100, 180));
        resetButton.setFont(new Font("Arial", Font.BOLD, 16));
        resetButton.setForeground(Color.WHITE);
        resetButton.setMaximumSize(new Dimension(340, 50));
        resetButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Password requirements
        JLabel requirementsLabel = new JLabel("<html><center>Password must be at least 6 characters long</center></html>");
        requirementsLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        requirementsLabel.setForeground(new Color(120, 120, 120));
        requirementsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Add components
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(descLabel);
        contentPanel.add(Box.createVerticalStrut(30));
        contentPanel.add(newPasswordField);
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(confirmPasswordField);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(requirementsLabel);
        contentPanel.add(Box.createVerticalStrut(25));
        contentPanel.add(resetButton);
        
        cardPanel.add(contentPanel);
        mainContainer.add(cardPanel);
        
        // Reset action
        resetButton.addActionListener(e -> resetPassword());
        
        add(mainContainer);
        setVisible(true);
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
    
    private void resetPassword() {
        String newPassword = new String(newPasswordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        
        // Validate inputs
        if (newPassword.equals("Enter new password") || confirmPassword.equals("Enter password again")) {
            JOptionPane.showMessageDialog(this, 
                "Please enter your new password", 
                "Password Required", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please fill in both password fields", 
                "Incomplete Form", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, 
                "Passwords do not match. Please try again.", 
                "Password Mismatch", 
                JOptionPane.ERROR_MESSAGE);
            // Clear fields
            newPasswordField.setText("");
            confirmPasswordField.setText("");
            newPasswordField.setShowingPlaceholder(true);
            confirmPasswordField.setShowingPlaceholder(true);
            newPasswordField.setText("Enter new password");
            confirmPasswordField.setText("Enter password again");
            newPasswordField.setForeground(new Color(150, 150, 150));
            confirmPasswordField.setForeground(new Color(150, 150, 150));
            newPasswordField.setEchoChar((char) 0);
            confirmPasswordField.setEchoChar((char) 0);
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
        if (updatePasswordInDatabase(email, newPassword)) {
            JOptionPane.showMessageDialog(this, 
                "Password reset successfully! Please login with your new password.", 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
            
            // Go back to login screen
            dispose();
            new LoginScreen();
        }
    }
    
    private boolean updatePasswordInDatabase(String email, String newPassword) {
        String url = "jdbc:mysql://localhost:3306/academic_analyzer";
        String dbUsername = "root";
        String dbPassword = "mk0492"; // Your MySQL password
        
        String sql = "UPDATE users SET password = ? WHERE email = ?";
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            try (Connection conn = DriverManager.getConnection(url, dbUsername, dbPassword);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setString(1, newPassword); // In production, hash the password!
                pstmt.setString(2, email);
                
                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
                
            }
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this, 
                "MySQL driver not found", 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, 
                "Database error: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
    }
}