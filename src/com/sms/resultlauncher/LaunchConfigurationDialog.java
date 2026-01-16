package com.sms.resultlauncher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.sms.resultlauncher.ResultLauncherUtils;
import com.sms.resultlauncher.ResultConfiguration;

public class LaunchConfigurationDialog extends JDialog {
    
    private boolean confirmed = false;
    private ResultConfiguration configuration;
    
    private JTextField launchNameField;
    private JCheckBox emailNotificationCheckbox;
    private JTextField emailSubjectField;
    private JTextArea emailMessageArea;
    private JCheckBox allowPdfCheckbox;
    private JCheckBox showDetailedCheckbox;
    
    // Enhanced visibility controls
    private JCheckBox showComponentMarksCheckbox;
    private JCheckBox showSubjectDetailsCheckbox;
    private JCheckBox showRankCheckbox;
    private JCheckBox showClassStatsCheckbox;
    
 // In LaunchConfigurationDialog.java, change the constructor:
    public LaunchConfigurationDialog(Window parent) {  // Changed from JFrame to Window
        super(parent, "Launch Configuration", ModalityType.APPLICATION_MODAL);
        this.configuration = new ResultConfiguration();
        
        initializeUI();
        setSize(550, 750); // Increased height for additional options
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
    private void initializeUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(ResultLauncherUtils.BACKGROUND_COLOR);
        
        JPanel mainPanel = ResultLauncherUtils.createModernCard();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
        // Title
        JLabel titleLabel = new JLabel("🚀 Configure Result Launch");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(ResultLauncherUtils.PRIMARY_COLOR);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        // Launch Name
        JPanel namePanel = createFieldPanel("Launch Name:", "Enter a name for this result launch");
        launchNameField = new JTextField();
        launchNameField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        launchNameField.setPreferredSize(new Dimension(0, 35));
        launchNameField.setText("Result Launch - " + java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        namePanel.add(launchNameField);
        
        // Email Notification
        emailNotificationCheckbox = new JCheckBox("Send Email Notifications to Students");
        emailNotificationCheckbox.setFont(new Font("SansSerif", Font.BOLD, 14));
        emailNotificationCheckbox.setSelected(true);
        emailNotificationCheckbox.setOpaque(false);
        emailNotificationCheckbox.addActionListener(e -> toggleEmailFields());
        
        // Email Subject
        JPanel subjectPanel = createFieldPanel("Email Subject:", "Subject line for notification emails");
        emailSubjectField = new JTextField();
        emailSubjectField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        emailSubjectField.setPreferredSize(new Dimension(0, 35));
        emailSubjectField.setText(configuration.getEmailSubject());
        subjectPanel.add(emailSubjectField);
        
        // Email Message
        JPanel messagePanel = createFieldPanel("Email Message:", "Message content for notification emails");
        emailMessageArea = new JTextArea(4, 30);
        emailMessageArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        emailMessageArea.setText(configuration.getEmailMessage());
        emailMessageArea.setLineWrap(true);
        emailMessageArea.setWrapStyleWord(true);
        JScrollPane messageScrollPane = new JScrollPane(emailMessageArea);
        messageScrollPane.setPreferredSize(new Dimension(0, 100));
        messagePanel.add(messageScrollPane);
        
        // Options
        JLabel optionsLabel = new JLabel("Display Options:");
        optionsLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        optionsLabel.setForeground(ResultLauncherUtils.TEXT_PRIMARY);
        optionsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        allowPdfCheckbox = new JCheckBox("Allow PDF Download");
        allowPdfCheckbox.setFont(new Font("SansSerif", Font.PLAIN, 14));
        allowPdfCheckbox.setSelected(true);
        allowPdfCheckbox.setOpaque(false);
        
        showDetailedCheckbox = new JCheckBox("Show Detailed Component Results");
        showDetailedCheckbox.setFont(new Font("SansSerif", Font.PLAIN, 14));
        showDetailedCheckbox.setSelected(true);
        showDetailedCheckbox.setOpaque(false);
        
        // Enhanced visibility controls
        JLabel visibilityLabel = new JLabel("Student Visibility Controls:");
        visibilityLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        visibilityLabel.setForeground(ResultLauncherUtils.TEXT_PRIMARY);
        visibilityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel visibilityDescLabel = new JLabel("Control what information students can see in their results");
        visibilityDescLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        visibilityDescLabel.setForeground(ResultLauncherUtils.TEXT_SECONDARY);
        visibilityDescLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        showComponentMarksCheckbox = new JCheckBox("Show Individual Component Marks");
        showComponentMarksCheckbox.setFont(new Font("SansSerif", Font.PLAIN, 13));
        showComponentMarksCheckbox.setSelected(true);
        showComponentMarksCheckbox.setOpaque(false);
        
        showSubjectDetailsCheckbox = new JCheckBox("Show Subject-wise Breakdown (Theory/Practical)");
        showSubjectDetailsCheckbox.setFont(new Font("SansSerif", Font.PLAIN, 13));
        showSubjectDetailsCheckbox.setSelected(true);
        showSubjectDetailsCheckbox.setOpaque(false);
        
        showRankCheckbox = new JCheckBox("Show Student Rank in Class");
        showRankCheckbox.setFont(new Font("SansSerif", Font.PLAIN, 13));
        showRankCheckbox.setSelected(false); // Privacy: default off
        showRankCheckbox.setOpaque(false);
        
        showClassStatsCheckbox = new JCheckBox("Show Class Statistics (Avg, Highest, Lowest)");
        showClassStatsCheckbox.setFont(new Font("SansSerif", Font.PLAIN, 13));
        showClassStatsCheckbox.setSelected(true);
        showClassStatsCheckbox.setOpaque(false);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        
        JButton cancelButton = ResultLauncherUtils.createSecondaryButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        
        JButton launchButton = ResultLauncherUtils.createModernButton("Launch Results");
        launchButton.addActionListener(e -> confirmLaunch());
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(launchButton);
        
        // Add all components
        mainPanel.add(titleLabel);
        mainPanel.add(namePanel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(emailNotificationCheckbox);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(subjectPanel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(messagePanel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(optionsLabel);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(allowPdfCheckbox);
        mainPanel.add(showDetailedCheckbox);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(visibilityLabel);
        mainPanel.add(visibilityDescLabel);
        mainPanel.add(Box.createVerticalStrut(8));
        mainPanel.add(showComponentMarksCheckbox);
        mainPanel.add(showSubjectDetailsCheckbox);
        mainPanel.add(showRankCheckbox);
        mainPanel.add(showClassStatsCheckbox);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(buttonPanel);
        
        add(mainPanel, BorderLayout.CENTER);
        
        toggleEmailFields();
    }
    
    private JPanel createFieldPanel(String labelText, String description) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setForeground(ResultLauncherUtils.TEXT_PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        descLabel.setForeground(ResultLauncherUtils.TEXT_SECONDARY);
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        panel.add(label);
        panel.add(descLabel);
        panel.add(Box.createVerticalStrut(5));
        
        return panel;
    }
    
    private void toggleEmailFields() {
        boolean enabled = emailNotificationCheckbox.isSelected();
        emailSubjectField.setEnabled(enabled);
        emailMessageArea.setEnabled(enabled);
    }
    
    private void confirmLaunch() {
        String launchName = launchNameField.getText().trim();
        if (launchName.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter a launch name.",
                "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Create configuration
        configuration.setLaunchName(launchName);
        configuration.setSendEmailNotification(emailNotificationCheckbox.isSelected());
        configuration.setEmailSubject(emailSubjectField.getText().trim());
        configuration.setEmailMessage(emailMessageArea.getText().trim());
        configuration.setAllowPdfDownload(allowPdfCheckbox.isSelected());
        configuration.setShowDetailedResults(showDetailedCheckbox.isSelected());
        
        // Set visibility controls
        configuration.setShowComponentMarks(showComponentMarksCheckbox.isSelected());
        configuration.setShowSubjectDetails(showSubjectDetailsCheckbox.isSelected());
        configuration.setShowRank(showRankCheckbox.isSelected());
        configuration.setShowClassStats(showClassStatsCheckbox.isSelected());
        
        confirmed = true;
        dispose();
    }
    
    public boolean isConfirmed() {
        return confirmed;
    }
    
    public ResultConfiguration getConfiguration() {
        return configuration;
    }
}