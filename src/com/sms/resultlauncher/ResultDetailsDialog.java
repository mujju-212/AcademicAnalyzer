package com.sms.resultlauncher;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.format.DateTimeFormatter;

public class ResultDetailsDialog extends JDialog {
    
    private LaunchedResult launchedResult;
    
    public ResultDetailsDialog(Window parent, LaunchedResult result) {
        super(parent, "Result Details", ModalityType.APPLICATION_MODAL);
        this.launchedResult = result;
        
        initializeUI();
        setSize(600, 500);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(ResultLauncherUtils.BACKGROUND_COLOR);
        
        JPanel mainPanel = ResultLauncherUtils.createModernCard();
        mainPanel.setLayout(new BorderLayout());
        
        // Header
        JPanel headerPanel = createHeaderPanel();
        
        // Details
        JPanel detailsPanel = createDetailsPanel();
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setOpaque(false);
        
        JButton closeButton = ResultLauncherUtils.createSecondaryButton("Close");
        closeButton.addActionListener(e -> dispose());
        
        JButton refreshButton = ResultLauncherUtils.createModernButton("Refresh");
        refreshButton.addActionListener(e -> refreshDetails());
        
        buttonPanel.add(closeButton);
        buttonPanel.add(refreshButton);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(detailsPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        JLabel titleLabel = new JLabel("📊 " + launchedResult.getLaunchName());
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(ResultLauncherUtils.PRIMARY_COLOR);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel statusLabel = new JLabel("Status: " + (launchedResult.isActive() ? "🟢 Active" : "🔴 Inactive"));
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        statusLabel.setForeground(launchedResult.isActive() ? 
            ResultLauncherUtils.SUCCESS_COLOR : ResultLauncherUtils.DANGER_COLOR);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(statusLabel);
        
        return panel;
    }
    
    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        
        // Basic info
        JPanel infoPanel = createInfoSection();
        
        // Statistics (placeholder for now)
        JPanel statsPanel = createStatsSection();
        
        panel.add(infoPanel);
        panel.add(Box.createVerticalStrut(20));
        panel.add(statsPanel);
        
        return panel;
    }
    
    private JPanel createInfoSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        
        JLabel sectionLabel = new JLabel("📚 Section Information");
        sectionLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        sectionLabel.setForeground(ResultLauncherUtils.PRIMARY_COLOR);
        sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel infoGrid = new JPanel(new GridLayout(6, 2, 10, 8));
        infoGrid.setOpaque(false);
        
        addInfoRow(infoGrid, "Section:", launchedResult.getSectionName());
        addInfoRow(infoGrid, "Launch Date:", 
            launchedResult.getLaunchDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        addInfoRow(infoGrid, "Launched By:", launchedResult.getLaunchedByName());
        addInfoRow(infoGrid, "Students:", String.valueOf(launchedResult.getStudentCount()));
        addInfoRow(infoGrid, "Components:", String.valueOf(launchedResult.getComponentCount()));
        addInfoRow(infoGrid, "Email Sent:", launchedResult.isEmailSent() ? "✅ Yes" : "❌ No");
        
        panel.add(sectionLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(infoGrid);
        
        return panel;
    }
    
    private JPanel createStatsSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        
        JLabel statsLabel = new JLabel("📈 Statistics");
        statsLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        statsLabel.setForeground(ResultLauncherUtils.PRIMARY_COLOR);
        statsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel comingSoonLabel = new JLabel("Detailed statistics coming soon...");
        comingSoonLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        comingSoonLabel.setForeground(ResultLauncherUtils.TEXT_SECONDARY);
        comingSoonLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Placeholder for future statistics like:
        // - Pass/Fail ratio
        // - Average percentage
        // - Grade distribution
        // - Component-wise performance
        
        panel.add(statsLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(comingSoonLabel);
        
        return panel;
    }
    
    private void addInfoRow(JPanel parent, String label, String value) {
        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("SansSerif", Font.BOLD, 12));
        labelComp.setForeground(ResultLauncherUtils.TEXT_PRIMARY);
        
        JLabel valueComp = new JLabel(value);
        valueComp.setFont(new Font("SansSerif", Font.PLAIN, 12));
        valueComp.setForeground(ResultLauncherUtils.TEXT_SECONDARY);
        
        parent.add(labelComp);
        parent.add(valueComp);
    }
    
    private void refreshDetails() {
        // TODO: Reload details from database
        JOptionPane.showMessageDialog(this,
            "Refresh functionality will be implemented soon!",
            "Feature Coming Soon", JOptionPane.INFORMATION_MESSAGE);
    }
}