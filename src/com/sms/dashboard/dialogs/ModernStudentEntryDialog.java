package com.sms.dashboard.dialogs;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import com.sms.dao.StudentDAO;
import com.sms.dashboard.constants.DashboardConstants;

public class ModernStudentEntryDialog extends JDialog {
    private JComboBox<String> sectionDropdown;
    private JTextField nameField;
    private JTextField rollNumberField;
    private JTextField emailField;
    private JTextField phoneField;
    private JButton saveButton;
    private JButton clearButton;
    
    private Map<String, Integer> sectionIdMap;
    private int currentUserId;
    private Runnable onSaveCallback;
    
    public ModernStudentEntryDialog(JFrame parent, int userId, Runnable onSaveCallback) {
        super(parent, "Add New Student", true);
        this.currentUserId = userId;
        this.onSaveCallback = onSaveCallback;
        this.sectionIdMap = new HashMap<>();
        
        initializeUI();
        loadSections();
        setLocationRelativeTo(parent);
    }
    
    private void initializeUI() {
        setSize(650, 700);
        setLayout(new BorderLayout());
        getContentPane().setBackground(DashboardConstants.BACKGROUND_COLOR);
        
        // Main container with padding
        JPanel mainPanel = new JPanel(new BorderLayout(0, 25));
        mainPanel.setBackground(DashboardConstants.BACKGROUND_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        
        // Header
        mainPanel.add(createHeader(), BorderLayout.NORTH);
        
        // Form
        mainPanel.add(createFormPanel(), BorderLayout.CENTER);
        
        // Buttons
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private JPanel createHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(DashboardConstants.BACKGROUND_COLOR);
        
        JLabel titleLabel = new JLabel("Add New Student");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(DashboardConstants.TEXT_PRIMARY);
        
        JLabel subtitleLabel = new JLabel("Enter student information below");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(DashboardConstants.TEXT_SECONDARY);
        
        JPanel titleContainer = new JPanel();
        titleContainer.setLayout(new BoxLayout(titleContainer, BoxLayout.Y_AXIS));
        titleContainer.setBackground(DashboardConstants.BACKGROUND_COLOR);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleContainer.add(titleLabel);
        titleContainer.add(Box.createVerticalStrut(8));
        titleContainer.add(subtitleLabel);
        
        headerPanel.add(titleContainer, BorderLayout.WEST);
        
        return headerPanel;
    }
    
    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(DashboardConstants.CARD_BACKGROUND);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1, true),
            BorderFactory.createEmptyBorder(35, 35, 35, 35)
        ));
        
        // Section Selection
        formPanel.add(createFieldGroup("Select Section", 
            createSectionDropdown(), true));
        formPanel.add(Box.createVerticalStrut(25));
        
        // Student Name
        nameField = createModernTextField();
        formPanel.add(createFieldGroup("Student Name", nameField, true));
        formPanel.add(Box.createVerticalStrut(25));
        
        // Roll Number
        rollNumberField = createModernTextField();
        formPanel.add(createFieldGroup("Roll Number", rollNumberField, true));
        formPanel.add(Box.createVerticalStrut(25));
        
        // Email
        emailField = createModernTextField();
        formPanel.add(createFieldGroup("Email Address", emailField, false));
        formPanel.add(Box.createVerticalStrut(25));
        
        // Phone
        phoneField = createModernTextField();
        formPanel.add(createFieldGroup("Phone Number", phoneField, false));
        
        return formPanel;
    }
    
    private JPanel createFieldGroup(String labelText, JComponent field, boolean required) {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setBackground(DashboardConstants.CARD_BACKGROUND);
        group.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel label = new JLabel(labelText + (required ? " *" : ""));
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(DashboardConstants.TEXT_PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        
        group.add(label);
        group.add(Box.createVerticalStrut(10));
        group.add(field);
        
        return group;
    }
    
    private JComboBox<String> createSectionDropdown() {
        sectionDropdown = new JComboBox<>();
        sectionDropdown.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        sectionDropdown.setBackground(DashboardConstants.CARD_BACKGROUND);
        sectionDropdown.setForeground(DashboardConstants.TEXT_PRIMARY);
        sectionDropdown.setPreferredSize(new Dimension(0, 50));
        sectionDropdown.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1, true),
            BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        
        // Custom renderer for better appearance
        sectionDropdown.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
                setFont(new Font("Segoe UI", Font.PLAIN, 15));
                
                if (isSelected) {
                    setBackground(DashboardConstants.PRIMARY_COLOR);
                    setForeground(Color.WHITE);
                } else {
                    setBackground(DashboardConstants.CARD_BACKGROUND);
                    setForeground(DashboardConstants.TEXT_PRIMARY);
                }
                return this;
            }
        });
        
        return sectionDropdown;
    }
    
    private JTextField createModernTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        field.setBackground(DashboardConstants.CARD_BACKGROUND);
        field.setForeground(DashboardConstants.TEXT_PRIMARY);
        field.setCaretColor(DashboardConstants.PRIMARY_COLOR);
        field.setPreferredSize(new Dimension(0, 50));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1, true),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        // Focus effects
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(DashboardConstants.PRIMARY_COLOR, 2, true),
                    BorderFactory.createEmptyBorder(9, 14, 9, 14)
                ));
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(229, 231, 235), 1, true),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)
                ));
            }
        });
        
        return field;
    }
    
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        buttonPanel.setBackground(DashboardConstants.BACKGROUND_COLOR);
        
        // Clear Button
        clearButton = createSecondaryButton("Clear Form");
        clearButton.addActionListener(e -> clearForm());
        
        // Save Button
        saveButton = createPrimaryButton("Save Student");
        saveButton.addActionListener(e -> saveStudent());
        
        buttonPanel.add(clearButton);
        buttonPanel.add(saveButton);
        
        return buttonPanel;
    }
    
    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setPreferredSize(new Dimension(160, 48));
        button.setBackground(DashboardConstants.PRIMARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(79, 82, 221));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(DashboardConstants.PRIMARY_COLOR);
            }
        });
        
        return button;
    }
    
    private JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setPreferredSize(new Dimension(140, 48));
        button.setBackground(DashboardConstants.CARD_BACKGROUND);
        button.setForeground(DashboardConstants.TEXT_SECONDARY);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1, true),
            BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(DashboardConstants.CARD_HOVER_BACKGROUND);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(DashboardConstants.CARD_BACKGROUND);
            }
        });
        
        return button;
    }
    
    private void loadSections() {
        sectionDropdown.removeAllItems();
        sectionDropdown.addItem("Select Section");
        sectionIdMap.clear();
        
        try {
            com.sms.dao.SectionDAO sectionDAO = new com.sms.dao.SectionDAO();
            List<com.sms.dao.SectionDAO.SectionInfo> sections = sectionDAO.getSectionsByUser(currentUserId);
            
            for (com.sms.dao.SectionDAO.SectionInfo section : sections) {
                String displayName = section.sectionName;
                sectionDropdown.addItem(displayName);
                sectionIdMap.put(displayName, section.id);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error loading sections: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void saveStudent() {
        // Validate inputs
        String selectedSection = (String) sectionDropdown.getSelectedItem();
        if (selectedSection == null || selectedSection.equals("Select Section")) {
            showError("Please select a section");
            return;
        }
        
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showError("Please enter student name");
            nameField.requestFocus();
            return;
        }
        
        String rollNumber = rollNumberField.getText().trim();
        if (rollNumber.isEmpty()) {
            showError("Please enter roll number");
            rollNumberField.requestFocus();
            return;
        }
        
        String email = emailField.getText().trim();
        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Please enter a valid email address");
            emailField.requestFocus();
            return;
        }
        
        String phone = phoneField.getText().trim();
        if (!phone.isEmpty() && !phone.matches("^[0-9]{10,15}$")) {
            showError("Please enter a valid phone number (10-15 digits)");
            phoneField.requestFocus();
            return;
        }
        
        // Get section ID
        Integer sectionId = sectionIdMap.get(selectedSection);
        if (sectionId == null) {
            showError("Invalid section selected");
            return;
        }
        
        // Save to database
        try {
            StudentDAO dao = new StudentDAO();
            boolean success = dao.addStudent(
                rollNumber,
                name,
                sectionId,
                email.isEmpty() ? null : email,
                phone.isEmpty() ? null : phone,
                currentUserId
            );
            
            if (success) {
                JOptionPane.showMessageDialog(this,
                    "Student added successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
                
                clearForm();
                
                if (onSaveCallback != null) {
                    onSaveCallback.run();
                }
            } else {
                showError("Failed to add student. Please check if roll number already exists.");
            }
        } catch (Exception e) {
            showError("Error saving student: " + e.getMessage());
        }
    }
    
    private void clearForm() {
        sectionDropdown.setSelectedIndex(0);
        nameField.setText("");
        rollNumberField.setText("");
        emailField.setText("");
        phoneField.setText("");
        nameField.requestFocus();
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this,
            message,
            "Validation Error",
            JOptionPane.WARNING_MESSAGE);
    }
}
