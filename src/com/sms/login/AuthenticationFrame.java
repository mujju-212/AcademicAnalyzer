package com.sms.login;

import java.awt.*;
import javax.swing.*;
import com.formdev.flatlaf.FlatLightLaf;
import com.sms.dashboard.DashboardScreen;

/**
 * Main application frame that handles both authentication and dashboard views
 * Uses CardLayout to smoothly transition between login and dashboard without closing the window
 */
public class AuthenticationFrame extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    
    // View names
    private static final String LOGIN_VIEW = "login";
    private static final String SIGNUP_VIEW = "signup";
    private static final String FORGOT_PASSWORD_VIEW = "forgotPassword";
    private static final String DASHBOARD_VIEW = "dashboard";
    
    // Panels
    private LoginPanel loginPanel;
    private SignUpPanel signUpPanel;
    private ForgotPasswordPanel forgotPasswordPanel;
    
    public AuthenticationFrame() {
        // Set FlatLaf Look and Feel
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        initializeUI();
    }
    
    private void initializeUI() {
        setTitle("Academic Analyzer - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(false); // Keep window decorations
        
        // Create CardLayout for switching between views
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(Color.WHITE);
        
        // Initialize panels
        loginPanel = new LoginPanel(this);
        signUpPanel = new SignUpPanel(this);
        forgotPasswordPanel = new ForgotPasswordPanel(this);
        
        // Add panels to CardLayout
        mainPanel.add(loginPanel, LOGIN_VIEW);
        mainPanel.add(signUpPanel, SIGNUP_VIEW);
        mainPanel.add(forgotPasswordPanel, FORGOT_PASSWORD_VIEW);
        
        add(mainPanel);
        setVisible(true);
        
        // Show login by default
        showLoginView();
    }
    
    // Navigation methods
    public void showLoginView() {
        cardLayout.show(mainPanel, LOGIN_VIEW);
        setTitle("Academic Analyzer - Login");
    }
    
    public void showSignUpView() {
        cardLayout.show(mainPanel, SIGNUP_VIEW);
        setTitle("Academic Analyzer - Create Account");
    }
    
    public void showForgotPasswordView() {
        cardLayout.show(mainPanel, FORGOT_PASSWORD_VIEW);
        setTitle("Academic Analyzer - Reset Password");
    }
    
    // Success handlers
    public void onLoginSuccess(int userId) {
        LoginScreen.currentUserId = userId;
        
        // Show loading indicator in same window
        JPanel loadingPanel = createLoadingPanel();
        getContentPane().removeAll();
        getContentPane().add(loadingPanel);
        revalidate();
        repaint();
        setTitle("Academic Analyzer - Loading...");
        
        // Prepare for dashboard - maximize window
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setResizable(true);
        
        // Load dashboard in background thread
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Dashboard initialization happens here
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    // Clear current content
                    getContentPane().removeAll();
                    
                    // Create dashboard screen without showing it
                    DashboardScreen dashboard = new DashboardScreen(userId, false);
                    
                    // Set BorderLayout for this frame (same as dashboard)
                    getContentPane().setLayout(new BorderLayout());
                    
                    // Transfer content from dashboard with proper layout constraints
                    Container dashboardContent = dashboard.getContentPane();
                    LayoutManager layout = dashboardContent.getLayout();
                    
                    if (layout instanceof BorderLayout) {
                        BorderLayout borderLayout = (BorderLayout) layout;
                        Component west = borderLayout.getLayoutComponent(BorderLayout.WEST);
                        Component center = borderLayout.getLayoutComponent(BorderLayout.CENTER);
                        Component north = borderLayout.getLayoutComponent(BorderLayout.NORTH);
                        Component south = borderLayout.getLayoutComponent(BorderLayout.SOUTH);
                        Component east = borderLayout.getLayoutComponent(BorderLayout.EAST);
                        
                        if (west != null) getContentPane().add(west, BorderLayout.WEST);
                        if (center != null) getContentPane().add(center, BorderLayout.CENTER);
                        if (north != null) getContentPane().add(north, BorderLayout.NORTH);
                        if (south != null) getContentPane().add(south, BorderLayout.SOUTH);
                        if (east != null) getContentPane().add(east, BorderLayout.EAST);
                    }
                    
                    // Dispose the dashboard's frame (we only needed its content)
                    dashboard.dispose();
                    
                    // Update this frame
                    setTitle("Academic Analyzer - Dashboard");
                    revalidate();
                    repaint();
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    // Restore login view on error
                    getContentPane().removeAll();
                    getContentPane().add(mainPanel);
                    setExtendedState(JFrame.NORMAL);
                    setSize(1000, 700);
                    setLocationRelativeTo(null);
                    setResizable(false);
                    revalidate();
                    repaint();
                    JOptionPane.showMessageDialog(AuthenticationFrame.this,
                        "Failed to load dashboard: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                    showLoginView();
                }
            }
        };
        worker.execute();
    }
    
    private JPanel createLoadingPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Color.WHITE);
        
        // Loading spinner
        JLabel spinner = new JLabel("⟳");
        spinner.setFont(new Font("SansSerif", Font.PLAIN, 48));
        spinner.setForeground(new Color(0, 120, 215));
        spinner.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Loading text
        JLabel loadingText = new JLabel("Loading Dashboard...");
        loadingText.setFont(new Font("SansSerif", Font.PLAIN, 18));
        loadingText.setForeground(new Color(100, 100, 100));
        loadingText.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        content.add(spinner);
        content.add(Box.createVerticalStrut(20));
        content.add(loadingText);
        
        // Animate spinner
        Timer timer = new Timer(100, null);
        timer.addActionListener(e -> {
            spinner.setText(spinner.getText().equals("⟳") ? "⟲" : "⟳");
        });
        timer.start();
        
        panel.add(content);
        return panel;
    }
    
    public void onSignUpSuccess() {
        showLoginView();
        JOptionPane.showMessageDialog(this,
            "Account created successfully! Please login.",
            "Success",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    public void onPasswordResetSuccess() {
        showLoginView();
        JOptionPane.showMessageDialog(this,
            "Password reset successful! Please login with your new password.",
            "Success",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
            new AuthenticationFrame();
        });
    }
}
