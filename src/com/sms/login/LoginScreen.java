package com.sms.login;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.*;
import javax.swing.border.*;
import com.formdev.flatlaf.FlatLightLaf;
import com.sms.dashboard.DashboardScreen;

public class LoginScreen extends JFrame {
	public static int currentUserId; // 🔹 This stores the logged-in user's ID

    // Custom panel with rounded corners and shadow
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
    
    // Custom rounded text field with better styling
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
    
    // Custom rounded password field
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
    
    // Custom rounded button
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
    
    // Custom outlined button
    static class OutlinedButton extends JButton {
        private int radius;
        
        public OutlinedButton(String text, int radius) {
            super(text);
            this.radius = radius;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw border
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, radius, radius));
            
            // Draw text
            FontMetrics fm = g2.getFontMetrics();
            Rectangle2D r = fm.getStringBounds(getText(), g2);
            int x = (getWidth() - (int) r.getWidth()) / 2;
            int y = (getHeight() - (int) r.getHeight()) / 2 + fm.getAscent();
            
            g2.setColor(Color.BLACK);
            g2.drawString(getText(), x, y);
        }
    }

    public LoginScreen() {
        // Set Look and Feel
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }
        

        setTitle("Academic Analyzer - Login");
        setSize(500, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Main container with beige background
        JPanel mainContainer = new JPanel(new GridBagLayout());
        mainContainer.setBackground(new Color(225, 220, 215));
        
        // White card panel with shadow
        RoundedPanel cardPanel = new RoundedPanel(30);
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setPreferredSize(new Dimension(400, 550));
        
        // Inner content panel with padding
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        // Logo and Title Panel
        JPanel logoTitlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        logoTitlePanel.setOpaque(false);
        logoTitlePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Create logo icon
        JPanel iconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw rounded rectangle background
                g2.setColor(new Color(25, 45, 85));
                g2.fill(new RoundRectangle2D.Float(0, 0, 60, 60, 15, 15));
                
                // Draw chart bars
                g2.setColor(Color.WHITE);
                // Bar 1
                g2.fill(new RoundRectangle2D.Float(12, 35, 10, 18, 2, 2));
                // Bar 2
                g2.fill(new RoundRectangle2D.Float(25, 25, 10, 28, 2, 2));
                // Bar 3
                g2.fill(new RoundRectangle2D.Float(38, 15, 10, 38, 2, 2));
            }
            
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(60, 60);
            }
        };
        iconPanel.setOpaque(false);
        
        // Title with better typography
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("Academic");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(new Color(30, 30, 30));
        
        JLabel subtitleLabel = new JLabel("Analyzer");
        subtitleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        subtitleLabel.setForeground(new Color(30, 30, 30));
        
        titlePanel.add(titleLabel);
        titlePanel.add(subtitleLabel);
        
        logoTitlePanel.add(iconPanel);
        logoTitlePanel.add(titlePanel);
        
        // Email field
        RoundedTextField emailField = new RoundedTextField(25, "Email");
        emailField.setMaximumSize(new Dimension(300, 50));
        emailField.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Add focus listener for email field
        emailField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent evt) {
                if (emailField.isShowingPlaceholder() && emailField.getText().equals("Email")) {
                    emailField.setText("");
                    emailField.setForeground(Color.BLACK);
                    emailField.setShowingPlaceholder(false);
                }
            }
            
            @Override
            public void focusLost(FocusEvent evt) {
                if (emailField.getText().isEmpty()) {
                    emailField.setText("Email");
                    emailField.setForeground(new Color(150, 150, 150));
                    emailField.setShowingPlaceholder(true);
                }
            }
        });
        
        // Password field
        RoundedPasswordField passwordField = new RoundedPasswordField(25, "Password");
        passwordField.setMaximumSize(new Dimension(300, 50));
        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Add focus listener for password field
        passwordField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent evt) {
                if (passwordField.isShowingPlaceholder() && new String(passwordField.getPassword()).equals("Password")) {
                    passwordField.setText("");
                    passwordField.setForeground(Color.BLACK);
                    passwordField.setEchoChar('•');
                    passwordField.setShowingPlaceholder(false);
                }
            }
            
            @Override
            public void focusLost(FocusEvent evt) {
                if (passwordField.getPassword().length == 0) {
                    passwordField.setText("Password");
                    passwordField.setForeground(new Color(150, 150, 150));
                    passwordField.setEchoChar((char) 0);
                    passwordField.setShowingPlaceholder(true);
                }
            }
        });
        
        // Forgot password link
        JPanel forgotPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        forgotPanel.setOpaque(false);
        forgotPanel.setMaximumSize(new Dimension(300, 25));
        forgotPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel forgotPassword = new JLabel("Forgot password?");
        forgotPassword.setFont(new Font("Arial", Font.PLAIN, 13));
        forgotPassword.setForeground(new Color(30, 120, 200));
        forgotPassword.setCursor(new Cursor(Cursor.HAND_CURSOR));
        forgotPassword.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                dispose();
                new ForgotPasswordScreen();
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                forgotPassword.setText("<html><u>Forgot password?</u></html>");
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                forgotPassword.setText("Forgot password?");
            }
        });
            forgotPanel.add(forgotPassword);
            
            // Login button - reduced size
            RoundedButton loginButton = new RoundedButton("Log in", 25, 
                new Color(30, 120, 200), new Color(25, 100, 180));
            loginButton.setFont(new Font("Arial", Font.BOLD, 15));
            loginButton.setForeground(Color.WHITE);
            loginButton.setMaximumSize(new Dimension(300, 45));
            loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            // OR separator
            JPanel orPanel = new JPanel();
            orPanel.setOpaque(false);
            orPanel.setMaximumSize(new Dimension(300, 25));
            orPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
            orPanel.setLayout(new BoxLayout(orPanel, BoxLayout.X_AXIS));
            
            JSeparator leftLine = new JSeparator();
            leftLine.setMaximumSize(new Dimension(120, 1));
            
            JLabel orLabel = new JLabel(" or ");
            orLabel.setFont(new Font("Arial", Font.PLAIN, 13));
            orLabel.setForeground(new Color(150, 150, 150));
            
            JSeparator rightLine = new JSeparator();
            rightLine.setMaximumSize(new Dimension(120, 1));
            
            orPanel.add(leftLine);
            orPanel.add(orLabel);
            orPanel.add(rightLine);
            
            // Create account button - reduced size
            OutlinedButton createAccountButton = new OutlinedButton("Create new account", 25);
            createAccountButton.setFont(new Font("Arial", Font.BOLD, 15));
            createAccountButton.setMaximumSize(new Dimension(300, 45));
            createAccountButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            // Add components to content panel with adjusted spacing
            contentPanel.add(logoTitlePanel);
            contentPanel.add(Box.createVerticalStrut(40));
            contentPanel.add(emailField);
            contentPanel.add(Box.createVerticalStrut(12));
            contentPanel.add(passwordField);
            contentPanel.add(Box.createVerticalStrut(8));
            contentPanel.add(forgotPanel);
            contentPanel.add(Box.createVerticalStrut(20));
            contentPanel.add(loginButton);
            contentPanel.add(Box.createVerticalStrut(15));
            contentPanel.add(orPanel);
            contentPanel.add(Box.createVerticalStrut(15));
            contentPanel.add(createAccountButton);
            
            cardPanel.add(contentPanel);
            
            // Add card panel to main container
            mainContainer.add(cardPanel);
            
            // Login action
         // In LoginScreen.java, replace the login button action with:
            loginButton.addActionListener(e -> {
                String email = emailField.getText();
                String password = new String(passwordField.getPassword());
                
                if (email.equals("Email") || password.equals("Password") || 
                    email.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(this, 
                        "Please enter your email and password", 
                        "Login Failed", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Check credentials in database
                if (validateLogin(email, password)) {
                    dispose();
                    new DashboardScreen();
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Invalid email or password", 
                        "Login Failed", 
                        JOptionPane.ERROR_MESSAGE);
                }
            });
            

         // Create account action
            createAccountButton.addActionListener(e -> {
                dispose(); // Close login screen
                new CreateAccountScreen(); // Open create account screen
            });
            
            
            add(mainContainer);
            setVisible(true);
        }
    private boolean validateLogin(String email, String password) {
        String url = "jdbc:mysql://localhost:3306/academic_analyzer";
        String dbUsername = "root";
        String dbPassword = "mk0492";
        
        String sql = "SELECT id, username FROM users WHERE email = ? AND password = ?";
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            try (Connection conn = DriverManager.getConnection(url, dbUsername, dbPassword);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setString(1, email);
                pstmt.setString(2, password);
                
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    // Store the logged-in user's ID
                    currentUserId = rs.getInt("id");
                    System.out.println("User logged in with ID: " + currentUserId); // Add this line
                    return true;
                }
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
   

    
        
        public static void main(String[] args) {
            SwingUtilities.invokeLater(() -> {
                // Enable anti-aliasing for smoother text
                System.setProperty("awt.useSystemAAFontSettings", "on");
                System.setProperty("swing.aatext", "true");
                
                new LoginScreen();
            });
        }
    }