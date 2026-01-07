package com.sms.dashboard.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import com.sms.dashboard.DashboardActions;

public class SidebarPanel extends JPanel {
    private JButton selectedButton = null;
    private Color backgroundColor = new Color(245, 247, 250);
    private Color selectedBgColor = new Color(237, 233, 254);
    private Color selectedTextColor = new Color(99, 102, 241);
    private Color defaultTextColor = new Color(107, 114, 128);
    private Color hoverBgColor = new Color(243, 244, 246);
    private DashboardActions actions; // Add this field to store the actions reference
    
    public SidebarPanel(DashboardActions actions) {
        this.actions = actions; // Store the actions reference
        setPreferredSize(new Dimension(280, getHeight()));
        setMinimumSize(new Dimension(280, 600));
        setMaximumSize(new Dimension(280, Integer.MAX_VALUE));
        setBackground(backgroundColor);
        setOpaque(true);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(229, 231, 235)));
        
        // Add logo/title area
        JPanel logoPanel = new JPanel();
        logoPanel.setBackground(backgroundColor);
        logoPanel.setMaximumSize(new Dimension(280, 100));
        logoPanel.setLayout(new GridBagLayout());

        JPanel logoContent = new JPanel();
        logoContent.setOpaque(false);
        logoContent.setLayout(new BoxLayout(logoContent, BoxLayout.Y_AXIS));

        // Create a single label for "Academic Analyzer" with all bold
        JLabel titleLabel = new JLabel("Academic Analyzer");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        titleLabel.setForeground(new Color(17, 24, 39));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        logoContent.add(titleLabel);
        logoPanel.add(logoContent);

        add(logoPanel);
        add(Box.createVerticalStrut(30));
        
        // Add sidebar buttons
        JButton dashboardBtn = addSidebarButton("Dashboard", "🏠", actions::showDashboard);
        selectButton(dashboardBtn); // Dashboard selected by default
        
        addSidebarButton("Library", "📚", actions::showLibrary);
        addSidebarButton("Create Section", "➕", actions::openCreateSectionDialog);
        addSidebarButton("Add Student", "👤", actions::openStudentEntryDialog);
        addSidebarButton("Mark Entry", "✏️", actions::openMarkEntryDialog);
        addSidebarButton("Result Launcher", "🎯", actions::openStudentAnalyzer);
        addSidebarButton("Analyzer", "📊", actions::openStudentAnalyzerPanel);
        addSidebarButton("View Data", "📄", actions::openAnalysisView);
        
        // Add spacer to push logout to bottom
        add(Box.createVerticalGlue());
        
        // Add refresh button ONLY ONCE

        
        // Add logout button
        addSidebarButton("Logout", "🚪", () -> {
            int result = JOptionPane.showConfirmDialog(
                SwingUtilities.getWindowAncestor(this),
                "Are you sure you want to logout?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            if (result == JOptionPane.YES_OPTION) {
                Window window = SwingUtilities.getWindowAncestor(this);
                if (window != null) {
                    window.dispose();
                }
            }
        });
        
        add(Box.createVerticalStrut(30));
        // REMOVED: addRefreshButton(); // This was causing the duplicate
    }

    private void addRefreshButton() {
        JButton refreshButton = new JButton("🔄 Refresh");
        refreshButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setBackground(new Color(99, 102, 241));
        refreshButton.setBorderPainted(false);
        refreshButton.setFocusPainted(false);
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshButton.setPreferredSize(new Dimension(240, 40));
        refreshButton.setMaximumSize(new Dimension(240, 40));
        refreshButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Use the actions reference to call refreshDashboard
        refreshButton.addActionListener(e -> {
            if (actions instanceof com.sms.dashboard.DashboardScreen) {
                ((com.sms.dashboard.DashboardScreen) actions).refreshDashboard();
            }
        });
        
        // Add hover effect
        refreshButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                refreshButton.setBackground(new Color(99, 102, 241).darker());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                refreshButton.setBackground(new Color(99, 102, 241));
            }
        });
        
        // Add some spacing before the button
        
    }
    
    private JButton addSidebarButton(String label, String emoji, Runnable action) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (this == selectedButton) {
                    g2.setColor(selectedBgColor);
                    g2.fillRoundRect(15, 2, getWidth() - 30, getHeight() - 4, 10, 10);
                } else if (getModel().isRollover()) {
                    g2.setColor(hoverBgColor);
                    g2.fillRoundRect(15, 2, getWidth() - 30, getHeight() - 4, 10, 10);
                }
                
                g2.dispose();
                super.paintComponent(g);
            }
        };
        
        btn.setText(emoji + "  " + label);
        btn.setMaximumSize(new Dimension(280, 48));
        btn.setPreferredSize(new Dimension(280, 48));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setForeground(defaultTextColor);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 15));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(BorderFactory.createEmptyBorder(0, 35, 0, 35));
        
        // Add hover effect
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn != selectedButton) {
                    btn.setForeground(new Color(17, 24, 39));
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (btn != selectedButton) {
                    btn.setForeground(defaultTextColor);
                }
            }
        });
        
        // Add selection effect
        btn.addActionListener(e -> {
            selectButton(btn);
            if (action != null) {
                action.run();
            }
        });
        
        add(Box.createVerticalStrut(5));
        add(btn);
        
        return btn;
    }
    
    private void selectButton(JButton btn) {
        if (selectedButton != null) {
            selectedButton.setForeground(defaultTextColor);
            selectedButton.repaint();
        }
        selectedButton = btn;
        btn.setForeground(selectedTextColor);
        btn.repaint();
    }
}