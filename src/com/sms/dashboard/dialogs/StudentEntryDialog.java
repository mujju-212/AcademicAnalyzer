package com.sms.dashboard.dialogs;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import com.sms.dashboard.data.DashboardDataManager;
import com.sms.dao.SectionDAO;
import com.sms.dao.StudentDAO;

public class StudentEntryDialog extends JPanel {
    private JPanel studentListPanel;
    private JTextField nameField;
    private JTextField rollField;
    private JTextField emailField;
    private JTextField phoneField;
    private List<StudentEntry> studentEntries;
    private JComboBox<String> sectionDropdown;
    private DashboardDataManager dataManager;
    private int editingIndex = -1;
    private JButton addButton;
    private Map<String, Integer> sectionIdMap;
    private JPanel totalStudentsLabel;
    private JPanel sectionStudentsLabel;
    private Runnable onCloseCallback;
    private JFrame parentFrame;
    
    // Clean theme colors matching Dashboard Constants
    private Color backgroundColor = new Color(248, 250, 252);  // DashboardConstants.BACKGROUND_COLOR
    private Color cardBackground = Color.WHITE;
    private Color primaryBlue = new Color(99, 102, 241);      // DashboardConstants.PRIMARY_COLOR
    private Color primaryGreen = new Color(34, 197, 94);       // DashboardConstants.SUCCESS_COLOR
    private Color textPrimary = new Color(17, 24, 39);         // DashboardConstants.TEXT_PRIMARY
    private Color textSecondary = new Color(75, 85, 99);       // DashboardConstants.TEXT_SECONDARY
    private Color borderColor = new Color(229, 231, 235);      // Gray-200
    private Color hoverColor = new Color(250, 250, 250);       // DashboardConstants.CARD_HOVER_BACKGROUND
    private Color errorColor = new Color(220, 53, 69);         // DashboardConstants.ERROR_COLOR

    // Inner class to store student data
    private static class StudentEntry {
        String name;
        String rollNumber;
        String email;
        String phone;
        
        StudentEntry(String name, String rollNumber, String email, String phone) {
            this.name = name;
            this.rollNumber = rollNumber;
            this.email = email;
            this.phone = phone;
        }
    }

    public StudentEntryDialog(JFrame parent, DashboardDataManager dataManager) {
        this(parent, dataManager, null);
    }
    
    public StudentEntryDialog(JFrame parent, DashboardDataManager dataManager, Runnable onCloseCallback) {
        this.parentFrame = parent;
        this.dataManager = dataManager;
        this.onCloseCallback = onCloseCallback;
        
        studentEntries = new ArrayList<>();
        sectionIdMap = new HashMap<>();
        
        initializeUI();
        loadSectionsFromDatabase();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(backgroundColor);
        
        // Main container
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(backgroundColor);
        
        // Header with back button
        mainContainer.add(createHeaderPanel(), BorderLayout.NORTH);
        
        // Content area
        mainContainer.add(createContentArea(), BorderLayout.CENTER);
        
        add(mainContainer);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(cardBackground);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor),
            BorderFactory.createEmptyBorder(32, 40, 32, 40)
        ));

        // Left side with back button and title
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        leftPanel.setBackground(cardBackground);
        
        if (onCloseCallback != null) {
            JButton backButton = new JButton("← Back");
            backButton.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            backButton.setForeground(primaryBlue);
            backButton.setBackground(cardBackground);
            backButton.setBorderPainted(false);
            backButton.setFocusPainted(false);
            backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            backButton.addActionListener(e -> closePanel());
            leftPanel.add(backButton);
        }
        
        // Title
        JLabel titleLabel = new JLabel("Student Management");
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 32));
        titleLabel.setForeground(textPrimary);
        leftPanel.add(titleLabel);

        // Stats
        JPanel statsPanel = createStatsPanel();

        header.add(leftPanel, BorderLayout.WEST);
        header.add(statsPanel, BorderLayout.EAST);

        return header;
    }

    private JPanel createStatsPanel() {
        JPanel statsContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        statsContainer.setBackground(cardBackground);

        totalStudentsLabel = createStatCard("0", "Total Students", primaryBlue);
        sectionStudentsLabel = createStatCard("0", "In Section", primaryGreen);

        statsContainer.add(totalStudentsLabel);
        statsContainer.add(sectionStudentsLabel);

        return statsContainer;
    }

    private JPanel createStatCard(String value, String label, Color accentColor) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(cardBackground);
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(accentColor);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel labelText = new JLabel(label);
        labelText.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        labelText.setForeground(textSecondary);
        labelText.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(valueLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(labelText);

        return card;
    }

    private JPanel createContentArea() {
        JPanel content = new JPanel(new BorderLayout(40, 0));
        content.setBackground(backgroundColor);
        content.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        // Left panel - Form
        JPanel leftPanel = createFormPanel();
        leftPanel.setPreferredSize(new Dimension(550, 0));

        // Right panel - Student list
        JPanel rightPanel = createStudentListPanel();

        content.add(leftPanel, BorderLayout.WEST);
        content.add(rightPanel, BorderLayout.CENTER);

        return content;
    }

    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new BorderLayout());
        formPanel.setBackground(cardBackground);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 1),
            BorderFactory.createEmptyBorder(30, 30, 30, 30)
        ));

        // Scrollable form content
        JPanel formContent = new JPanel();
        formContent.setLayout(new BoxLayout(formContent, BoxLayout.Y_AXIS));
        formContent.setBackground(cardBackground);

        // Section selection
        formContent.add(createSectionSelectionPanel());
        formContent.add(Box.createVerticalStrut(30));

        // Student form
        formContent.add(createStudentFormPanel());
        formContent.add(Box.createVerticalStrut(30));

        // Bottom buttons panel
        formContent.add(createBottomButtonsPanel());

        // Make form scrollable
        JScrollPane scrollPane = new JScrollPane(formContent);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(cardBackground);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        formPanel.add(scrollPane, BorderLayout.CENTER);

        return formPanel;
    }

    private JPanel createSectionSelectionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(cardBackground);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel("Select Section");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(textPrimary);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(12));

        // Dropdown container
        JPanel dropdownContainer = new JPanel(new BorderLayout(12, 0));
        dropdownContainer.setBackground(cardBackground);
        dropdownContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        dropdownContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        sectionDropdown = createStyledComboBox();
        JButton refreshButton = createRefreshButton();

        dropdownContainer.add(sectionDropdown, BorderLayout.CENTER);
        dropdownContainer.add(refreshButton, BorderLayout.EAST);

        panel.add(dropdownContainer);

        // Add section change listener
        sectionDropdown.addActionListener(e -> {
            String selected = (String) sectionDropdown.getSelectedItem();
            if (selected != null && !selected.equals("Select Section")) {
                loadExistingStudents();
            }
        });

        return panel;
    }

    private JPanel createStudentFormPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(cardBackground);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel("Student Information");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(textPrimary);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(20));

        // Create form fields
        nameField = createStyledTextField();
        rollField = createStyledTextField();
        emailField = createStyledTextField();
        phoneField = createStyledTextField();

        // Add all form fields
        panel.add(createFormField("Student Name", nameField, true));
        panel.add(Box.createVerticalStrut(16));
        panel.add(createFormField("Roll Number", rollField, true));
        panel.add(Box.createVerticalStrut(16));
        panel.add(createFormField("Email Address", emailField, false));
        panel.add(Box.createVerticalStrut(16));
        panel.add(createFormField("Phone Number", phoneField, false));

        return panel;
    }

    private JPanel createFormField(String labelText, JTextField field, boolean required) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(cardBackground);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));

        JLabel label = new JLabel(labelText + (required ? " *" : ""));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(textSecondary);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

        panel.add(label);
        panel.add(Box.createVerticalStrut(8));
        panel.add(field);

        return panel;
    }

    private JPanel createBottomButtonsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setBackground(cardBackground);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        addButton = createPrimaryButton("Add Student");
        JButton clearButton = createSecondaryButton("Clear");

        addButton.addActionListener(e -> addOrUpdateStudent());
        clearButton.addActionListener(e -> {
            clearForm();
            editingIndex = -1;
            addButton.setText("Add Student");
        });

        panel.add(addButton);
        panel.add(Box.createHorizontalStrut(12));
        panel.add(clearButton);

        return panel;
    }

    private JPanel createStudentListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(cardBackground);
        panel.setBorder(BorderFactory.createLineBorder(borderColor, 1));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(cardBackground);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor),
            BorderFactory.createEmptyBorder(30, 30, 20, 30)
        ));

        JLabel titleLabel = new JLabel("Students List");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(textPrimary);

        JButton saveAllButton = createSuccessButton("Save All Students");
        saveAllButton.addActionListener(e -> submitStudentData());

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(saveAllButton, BorderLayout.EAST);

        // Student list
        studentListPanel = new JPanel();
        studentListPanel.setLayout(new BoxLayout(studentListPanel, BoxLayout.Y_AXIS));
        studentListPanel.setBackground(cardBackground);
        studentListPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 30, 30));

        JScrollPane scrollPane = new JScrollPane(studentListPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(cardBackground);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Initial empty state
        showEmptyState();

        return panel;
    }

    // UI Component Creation Methods
    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        field.setPreferredSize(new Dimension(420, 45));
        field.setMaximumSize(new Dimension(420, 45));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 1),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        field.setBackground(cardBackground);
        field.setForeground(textPrimary);

        // Focus effects
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(primaryBlue, 2),
                    BorderFactory.createEmptyBorder(9, 13, 9, 13)
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(borderColor, 1),
                    BorderFactory.createEmptyBorder(10, 14, 10, 14)
                ));
            }
        });

        return field;
    }

    private JComboBox<String> createStyledComboBox() {
        JComboBox<String> comboBox = new JComboBox<>();
        comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        comboBox.setPreferredSize(new Dimension(420, 45));
        comboBox.setMaximumSize(new Dimension(420, 45));
        comboBox.setBackground(cardBackground);
        comboBox.setForeground(textPrimary);
        comboBox.setBorder(BorderFactory.createLineBorder(borderColor, 1));

        // Custom renderer for better appearance
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
                if (isSelected) {
                    setBackground(primaryBlue);
                    setForeground(Color.WHITE);
                } else {
                    setBackground(cardBackground);
                    setForeground(textPrimary);
                }
                return this;
            }
        });

        return comboBox;
    }

    private JButton createRefreshButton() {
        JButton button = new JButton("↻");
        button.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        button.setPreferredSize(new Dimension(45, 45));
        button.setBackground(cardBackground);
        button.setForeground(textSecondary);
        button.setBorder(BorderFactory.createLineBorder(borderColor, 1));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);

        button.addActionListener(e -> loadSectionsFromDatabase());

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(cardBackground);
            }
        });

        return button;
    }

    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        button.setForeground(Color.WHITE);
        button.setBackground(primaryBlue);
        button.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(140, 45));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(primaryBlue.darker());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(primaryBlue);
            }
        });

        return button;
    }

    private JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        button.setForeground(textSecondary);
        button.setBackground(cardBackground);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 1),
            BorderFactory.createEmptyBorder(11, 23, 11, 23)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(80, 45));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(cardBackground);
            }
        });

        return button;
    }

    private JButton createSuccessButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        button.setForeground(Color.WHITE);
        button.setBackground(primaryGreen);
        button.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(primaryGreen.darker());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(primaryGreen);
            }
        });

        return button;
    }

    // Student Management Methods
    private void addOrUpdateStudent() {
        String name = nameField.getText() != null ? nameField.getText().trim() : "";
        String rollNumber = rollField.getText() != null ? rollField.getText().trim() : "";
        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        String phone = phoneField.getText() != null ? phoneField.getText().trim() : "";

        // Validate all required fields
        if (name.isEmpty() || rollNumber.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            String missingFields = "";
            
            if (name.isEmpty()) missingFields += "Name, ";
            if (rollNumber.isEmpty()) missingFields += "Roll Number, ";
            if (email.isEmpty()) missingFields += "Email, ";
            if (phone.isEmpty()) missingFields += "Phone, ";
            
            // Remove trailing comma and space
            missingFields = missingFields.substring(0, missingFields.length() - 2);
            
            JOptionPane.showMessageDialog(this, 
                "Please fill in all required fields: " + missingFields,
                "Missing Information",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check for duplicate roll numbers
        for (int i = 0; i < studentEntries.size(); i++) {
            if (i != editingIndex && studentEntries.get(i).rollNumber.equals(rollNumber)) {
                JOptionPane.showMessageDialog(this, "Roll Number already exists");
                return;
            }
        }

        if (editingIndex >= 0) {
            StudentEntry student = studentEntries.get(editingIndex);
            student.name = name;
            student.rollNumber = rollNumber;
            student.email = email;
            student.phone = phone;
            editingIndex = -1;
            addButton.setText("Add Student");
        } else {
            StudentEntry student = new StudentEntry(name, rollNumber, email, phone);
            studentEntries.add(student);
        }

        clearForm();
        refreshStudentList();
    }

    private boolean validateStudentInput(String name, String rollNumber, String email, String phone) {
        if (name.isEmpty() || rollNumber.isEmpty()) {
            showError("Name and Roll Number are required fields.");
            return false;
        }

        // Validate email format if provided
        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Please enter a valid email address.");
            return false;
        }

        // Validate phone format if provided
        if (!phone.isEmpty() && !phone.matches("^[0-9]{10,15}$")) {
            showError("Please enter a valid phone number (10-15 digits).");
            return false;
        }

        // Check for duplicate roll number
        for (int i = 0; i < studentEntries.size(); i++) {
            if (i != editingIndex && studentEntries.get(i).rollNumber.equals(rollNumber)) {
                showError("Roll Number already exists.");
                return false;
            }
        }

        return true;
    }

    private void clearForm() {
        nameField.setText("");
        rollField.setText("");
        emailField.setText("");
        phoneField.setText("");
        nameField.requestFocus();
    }

    private void refreshStudentList() {
        studentListPanel.removeAll();

        if (studentEntries.isEmpty()) {
            showEmptyState();
        } else {
            for (int i = 0; i < studentEntries.size(); i++) {
                StudentEntry student = studentEntries.get(i);
                JPanel studentCard = createStudentCard(student, i);
                studentListPanel.add(studentCard);
                if (i < studentEntries.size() - 1) {
                    studentListPanel.add(Box.createVerticalStrut(12));
                }
            }
        }

        studentListPanel.revalidate();
        studentListPanel.repaint();
    }

    private void showEmptyState() {
        JPanel emptyPanel = new JPanel(new GridBagLayout());
        emptyPanel.setBackground(cardBackground);
        emptyPanel.setPreferredSize(new Dimension(0, 300));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(cardBackground);

        // Icon or illustration placeholder
        JLabel iconLabel = new JLabel("📚");
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 48));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel emptyLabel = new JLabel("No students added yet");
        emptyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        emptyLabel.setForeground(textSecondary);
        emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel descLabel = new JLabel("Add students using the form on the left");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        descLabel.setForeground(textSecondary);
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(iconLabel);
        content.add(Box.createVerticalStrut(16));
        content.add(emptyLabel);
        content.add(Box.createVerticalStrut(8));
        content.add(descLabel);

        emptyPanel.add(content);
        studentListPanel.add(emptyPanel);
    }

    private JPanel createStudentCard(StudentEntry student, int index) {
        JPanel card = new JPanel(new BorderLayout(16, 0));
        card.setBackground(cardBackground);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 1),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        // Student info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(cardBackground);

        JLabel nameLabel = new JLabel(student.name);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        nameLabel.setForeground(textPrimary);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel rollLabel = new JLabel("Roll: " + student.rollNumber);
        rollLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        rollLabel.setForeground(textSecondary);
        rollLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        String contactInfo = buildContactInfo(student);
        if (!contactInfo.equals("No contact information")) {
            JLabel contactLabel = new JLabel(contactInfo);
            contactLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            contactLabel.setForeground(textSecondary);
            contactLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            infoPanel.add(nameLabel);
            infoPanel.add(Box.createVerticalStrut(4));
            infoPanel.add(rollLabel);
            infoPanel.add(Box.createVerticalStrut(4));
            infoPanel.add(contactLabel);
        } else {
            infoPanel.add(nameLabel);
            infoPanel.add(Box.createVerticalStrut(4));
            infoPanel.add(rollLabel);
        }

        // Action buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.setBackground(cardBackground);

        JButton editButton = createActionButton("Edit", primaryBlue);
        JButton deleteButton = createActionButton("Delete", errorColor);

        editButton.addActionListener(e -> editStudent(index));
        deleteButton.addActionListener(e -> deleteStudent(index));

        actionPanel.add(editButton);
        actionPanel.add(deleteButton);

        card.add(infoPanel, BorderLayout.CENTER);
        card.add(actionPanel, BorderLayout.EAST);

        // Hover effect
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(hoverColor);
                infoPanel.setBackground(hoverColor);
                actionPanel.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(cardBackground);
                infoPanel.setBackground(cardBackground);
                actionPanel.setBackground(cardBackground);
            }
        });

        return card;
    }

    private String buildContactInfo(StudentEntry student) {
        List<String> contacts = new ArrayList<>();
        if (!student.email.isEmpty()) {
            contacts.add(student.email);
        }
        if (!student.phone.isEmpty()) {
            contacts.add("📞 " + student.phone);
        }
        return contacts.isEmpty() ? "No contact information" : String.join(" | ", contacts);
    }

    private JButton createActionButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.setForeground(color);
        button.setBackground(cardBackground);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(color);
                button.setForeground(Color.WHITE);
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(cardBackground);
                button.setForeground(color);
            }
        });

        return button;
    }

    // Database Operations
    private void loadSectionsFromDatabase() {
        try {
            sectionDropdown.removeAllItems();
            sectionIdMap.clear();
            
            SectionDAO sectionDAO = new SectionDAO();
            List<SectionDAO.SectionInfo> sections = sectionDAO.getSectionsByUser(
                com.sms.login.LoginScreen.currentUserId);
            
            sectionDropdown.addItem("Select Section");
            for (SectionDAO.SectionInfo section : sections) {
                sectionDropdown.addItem(section.sectionName);
                sectionIdMap.put(section.sectionName, section.id);
            }
        } catch (Exception e) {
            showError("Failed to load sections: " + e.getMessage());
        }
    }

    private void loadExistingStudents() {
        String selectedSection = (String) sectionDropdown.getSelectedItem();
        if (selectedSection == null || selectedSection.equals("Select Section")) return;
        
        Integer sectionId = sectionIdMap.get(selectedSection);
        if (sectionId == null) return;
        
        try {
            studentEntries.clear();
            StudentDAO dao = new StudentDAO();
            List<StudentDAO.StudentInfo> students = dao.getStudentsBySection(
                sectionId, com.sms.login.LoginScreen.currentUserId);
            
            for (StudentDAO.StudentInfo student : students) {
                studentEntries.add(new StudentEntry(
                    student.name,
                    student.rollNumber,
                    student.email != null ? student.email : "",
                    student.phone != null ? student.phone : ""
                ));
            }
            
            refreshStudentList();
            updateStatistics();
        } catch (Exception e) {
            showError("Failed to load students: " + e.getMessage());
        }
    }

    private void submitStudentData() {
        if (studentEntries.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No students to save");
            return;
        }

        String selectedSection = (String) sectionDropdown.getSelectedItem();
        if (selectedSection == null || selectedSection.equals("Select Section")) {
            JOptionPane.showMessageDialog(this, "Please select a section");
            return;
        }

        Integer sectionId = sectionIdMap.get(selectedSection);
        if (sectionId == null) {
            JOptionPane.showMessageDialog(this, "Invalid section selected");
            return;
        }

        try {
            StudentDAO dao = new StudentDAO();
            
            // Get existing students to filter out
            List<StudentDAO.StudentInfo> existingStudents = dao.getStudentsBySection(
                sectionId, com.sms.login.LoginScreen.currentUserId);
            
            Set<String> existingRollNumbers = new HashSet<>();
            for (StudentDAO.StudentInfo existing : existingStudents) {
                existingRollNumbers.add(existing.rollNumber);
            }
            
            int successCount = 0;
            int skippedCount = 0;
            
            for (StudentEntry student : studentEntries) {
                if (existingRollNumbers.contains(student.rollNumber)) {
                    // Skip existing students
                    skippedCount++;
                    continue;
                }
                
                // Safe null checks for email and phone
                String emailValue = (student.email != null && !student.email.trim().isEmpty()) ? student.email.trim() : null;
                String phoneValue = (student.phone != null && !student.phone.trim().isEmpty()) ? student.phone.trim() : null;
                
                // Only save new students
                boolean success = dao.addStudent(
                    student.rollNumber,
                    student.name,
                    sectionId,
                    emailValue,
                    phoneValue,
                    com.sms.login.LoginScreen.currentUserId
                );
                
                if (success) successCount++;
            }
            
            String message = String.format(
                "New students added: %d\nExisting students skipped: %d",
                successCount, skippedCount
            );
            
            JOptionPane.showMessageDialog(this, message);
            
            if (successCount > 0 || skippedCount == studentEntries.size()) {
                closePanel();
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error saving students: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private JDialog createProgressDialog() {
        JDialog dialog = new JDialog(parentFrame, "Saving Students", true);
        dialog.setSize(400, 150);
        dialog.setLocationRelativeTo(parentFrame);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(cardBackground);

        JLabel titleLabel = new JLabel("Saving Students...");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressBar.setPreferredSize(new Dimension(350, 25));

        JLabel statusLabel = new JLabel("Initializing...");
        statusLabel.setName("statusLabel");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        statusLabel.setForeground(textSecondary);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(15));
        panel.add(progressBar);
        panel.add(Box.createVerticalStrut(10));
        panel.add(statusLabel);

        dialog.add(panel);
        return dialog;
    }

    // Helper methods to find components in progress dialog
    private JProgressBar findProgressBar(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JProgressBar) {
                return (JProgressBar) comp;
            } else if (comp instanceof Container) {
                JProgressBar bar = findProgressBar((Container) comp);
                if (bar != null) return bar;
            }
        }
        return null;
    }

    private JLabel findStatusLabel(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel && "statusLabel".equals(comp.getName())) {
                return (JLabel) comp;
            } else if (comp instanceof Container) {
                JLabel label = findStatusLabel((Container) comp);
                if (label != null) return label;
            }
        }
        return null;
    }

    // Student Actions
    private void editStudent(int index) {
        StudentEntry student = studentEntries.get(index);
        
        nameField.setText(student.name);
        rollField.setText(student.rollNumber);
        emailField.setText(student.email);
        phoneField.setText(student.phone);
        
        editingIndex = index;
        addButton.setText("Update Student");
        nameField.requestFocus();
        
        // Scroll to top of form
        SwingUtilities.invokeLater(() -> {
            Container parent = nameField.getParent();
            while (parent != null && !(parent instanceof JScrollPane)) {
                parent = parent.getParent();
            }
            if (parent instanceof JScrollPane) {
                ((JScrollPane) parent).getVerticalScrollBar().setValue(0);
            }
        });
    }

    private void deleteStudent(int index) {
        StudentEntry student = studentEntries.get(index);
        
        // Custom confirmation dialog
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to remove \"" + student.name + "\" (Roll: " + student.rollNumber + ")?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            studentEntries.remove(index);
            
            if (editingIndex == index) {
                editingIndex = -1;
                addButton.setText("Add Student");
                clearForm();
            } else if (editingIndex > index) {
                editingIndex--;
            }
            
            refreshStudentList();
            updateStatistics();
            
            // Show brief confirmation
            showTemporaryMessage("Student removed successfully");
        }
    }

    // Statistics Update
    private void updateStatistics() {
        // Update total students label
        if (totalStudentsLabel != null && totalStudentsLabel.getComponentCount() > 0) {
            JLabel valueLabel = (JLabel) totalStudentsLabel.getComponent(0);
            valueLabel.setText(String.valueOf(studentEntries.size()));
        }
        
        // Update section students label  
        if (sectionStudentsLabel != null && sectionStudentsLabel.getComponentCount() > 0) {
            JLabel sectionValueLabel = (JLabel) sectionStudentsLabel.getComponent(0);
            sectionValueLabel.setText(String.valueOf(studentEntries.size()));
        }
        
        if (totalStudentsLabel != null) totalStudentsLabel.repaint();
        if (sectionStudentsLabel != null) sectionStudentsLabel.repaint();
    }

    // Utility Methods
    private void showError(String message) {
        JOptionPane.showMessageDialog(this,
            message,
            "Error",
            JOptionPane.ERROR_MESSAGE);
    }

    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this,
            message,
            "Success",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void showTemporaryMessage(String message) {
        // Create a temporary notification
        JWindow notification = new JWindow(parentFrame);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(primaryGreen);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        JLabel label = new JLabel(message);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        panel.add(label);
        notification.add(panel);
        notification.pack();
        
        // Position at top center of parent frame
        Point frameLocation = parentFrame.getLocationOnScreen();
        int x = frameLocation.x + (parentFrame.getWidth() - notification.getWidth()) / 2;
        int y = frameLocation.y + 50;
        notification.setLocation(x, y);
        
        notification.setVisible(true);
        
        // Auto-hide after 2 seconds
        Timer timer = new Timer(2000, e -> notification.dispose());
        timer.setRepeats(false);
        timer.start();
    }
    
    private void closePanel() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    // Main method for testing
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            
            StudentEntryDialog dialog = new StudentEntryDialog(frame, null);
            dialog.setVisible(true);
        });
    }
}