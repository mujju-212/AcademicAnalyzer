package com.sms.marking.dialogs;

import javax.swing.*;
import java.awt.*;
import com.sms.marking.models.*;
import com.sms.theme.ThemeManager;

public class ComponentGroupDialog extends JDialog {
    private ThemeManager themeManager;
    private String groupType;
    private ComponentGroup componentGroup;
    private boolean confirmed = false;
    
    // UI Components
    private JTextField groupNameField;
    private JSpinner totalMarksSpinner;
    private JComboBox<String> selectionTypeCombo;
    private JSpinner selectionCountSpinner;
    private JLabel selectionCountLabel;
    
    public ComponentGroupDialog(Dialog parent, String groupType) {
        super(parent, "Add " + (groupType.equals("internal") ? "Internal" : "External") + " Group", true);
        this.themeManager = ThemeManager.getInstance();
        this.groupType = groupType;
        
        initializeUI();
        
        setSize(500, 400);
        setLocationRelativeTo(parent);
    }
    
    private void initializeUI() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(themeManager.getCardColor());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Group name
        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(new JLabel("Group Name:"), gbc);
        
        gbc.gridx = 1;
        groupNameField = createStyledTextField();
        mainPanel.add(groupNameField, gbc);
        
        // Total marks
        gbc.gridx = 0; gbc.gridy = 1;
        mainPanel.add(new JLabel("Total Marks:"), gbc);
        
        gbc.gridx = 1;
        totalMarksSpinner = new JSpinner(new SpinnerNumberModel(25, 0, 100, 5));
        totalMarksSpinner.setPreferredSize(new Dimension(100, 35));
        mainPanel.add(totalMarksSpinner, gbc);
        
        // Selection type
        gbc.gridx = 0; gbc.gridy = 2;
        mainPanel.add(new JLabel("Selection Type:"), gbc);
        
        gbc.gridx = 1;
        selectionTypeCombo = new JComboBox<>(new String[]{"All Components", "Best Of", "Average Of"});
        selectionTypeCombo.setPreferredSize(new Dimension(200, 35));
        mainPanel.add(selectionTypeCombo, gbc);
        
        // Selection count (for best of)
        gbc.gridx = 0; gbc.gridy = 3;
        selectionCountLabel = new JLabel("Best Of Count:");
        selectionCountLabel.setVisible(false);
        mainPanel.add(selectionCountLabel, gbc);
        
        gbc.gridx = 1;
        selectionCountSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 10, 1));
        selectionCountSpinner.setPreferredSize(new Dimension(100, 35));
        selectionCountSpinner.setVisible(false);
        mainPanel.add(selectionCountSpinner, gbc);
        
        // Add listener for selection type
        selectionTypeCombo.addActionListener(e -> {
            boolean isBestOf = "Best Of".equals(selectionTypeCombo.getSelectedItem());
            selectionCountLabel.setVisible(isBestOf);
            selectionCountSpinner.setVisible(isBestOf);
        });
        
        // Info panel
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        
        JTextArea infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        infoArea.setBackground(new Color(239, 246, 255));
        infoArea.setForeground(new Color(30, 58, 138));
        infoArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        
        String infoText = groupType.equals("internal") ?
            "Internal components are assessments conducted by the institution throughout the semester.\n\n" +
            "Examples:\n" +
            "• Internal Assessments (IAs)\n" +
            "• Assignments\n" +
            "• Lab Tests\n" +
            "• Projects\n" +
            "• Class Participation" :
            "External components are assessments conducted by external examiners.\n\n" +
            "Examples:\n" +
            "• Final Theory Exam\n" +
            "• Final Lab Exam\n" +
            "• External Viva\n" +
            "• External Project Evaluation";
        
        infoArea.setText(infoText);
        
        JScrollPane infoScrollPane = new JScrollPane(infoArea);
        infoScrollPane.setBorder(BorderFactory.createTitledBorder("Information"));
        mainPanel.add(infoScrollPane, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(themeManager.getCardColor());
        
        JButton cancelBtn = createSecondaryButton("Cancel");
        JButton createBtn = createPrimaryButton("Create Group");
        
        cancelBtn.addActionListener(e -> dispose());
        
        createBtn.addActionListener(e -> {
            if (validateInput()) {
                createComponentGroup();
                confirmed = true;
                dispose();
            }
        });
        
        buttonPanel.add(cancelBtn);
        buttonPanel.add(createBtn);
        
        // Assemble
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private boolean validateInput() {
        String groupName = groupNameField.getText().trim();
        if (groupName.isEmpty()) {
            showError("Please enter a group name");
            return false;
        }
        
        int totalMarks = (Integer) totalMarksSpinner.getValue();
        if (totalMarks <= 0) {
            showError("Total marks must be greater than 0");
            return false;
        }
        
        return true;
    }
    
    private void createComponentGroup() {
        componentGroup = new ComponentGroup();
        componentGroup.setGroupName(groupNameField.getText().trim());
        componentGroup.setGroupType(groupType);
        componentGroup.setTotalGroupMarks((Integer) totalMarksSpinner.getValue());
        
        String selectionType = (String) selectionTypeCombo.getSelectedItem();
        if ("All Components".equals(selectionType)) {
            componentGroup.setSelectionType("all");
        } else if ("Best Of".equals(selectionType)) {
            componentGroup.setSelectionType("best_of");
            componentGroup.setSelectionCount((Integer) selectionCountSpinner.getValue());
        } else {
            componentGroup.setSelectionType("average_of");
        }
    }
    
    public ComponentGroup getComponentGroup() {
        return confirmed ? componentGroup : null;
    }
    
    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(250, 35));
        field.setBackground(themeManager.isDarkMode() ? 
            themeManager.getBackgroundColor() : Color.WHITE);
        field.setForeground(themeManager.getTextPrimaryColor());
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(themeManager.getBorderColor(), 1, true),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        return field;
    }
    
    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setBackground(themeManager.getPrimaryColor());
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }
    
    private JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setBackground(themeManager.getBackgroundColor());
        button.setForeground(themeManager.getTextPrimaryColor());
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(themeManager.getBorderColor(), 1));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Validation Error", JOptionPane.ERROR_MESSAGE);
    }
}