package com.sms.dashboard.dialogs;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import com.sms.theme.ThemeManager;
import com.sms.dao.SectionDAO;
import com.sms.database.DatabaseConnection;
import com.sms.marking.models.MarkingScheme;
import com.sms.marking.models.ComponentGroup;
import com.sms.marking.models.MarkingComponent;
import com.sms.marking.models.ValidationResult;
import com.sms.dashboard.constants.DashboardConstants;
import com.sms.dashboard.util.UIComponentFactory;
import static com.sms.dashboard.constants.DashboardConstants.*;

/**
 * Panel for creating new sections - embedded in dashboard instead of separate dialog
 */
public class CreateSectionPanel extends JPanel {
    private JFrame parentFrame;
    private int userId;
    private ThemeManager themeManager;
    
    // UI Components
    private JTextField sectionNameField;
    private JTextField semesterField;
    private JTextField studentCountField;
    private JSpinner yearSpinner;
    private JTextArea descriptionArea;
    
    // Subject components
    private DefaultTableModel subjectTableModel;
    private JTable subjectTable;
    private JTextField subjectNameField;
    private JTextField subjectMarksField;
    
    // Exam pattern components  
    private JComboBox<String> examPatternsSubjectCombo;
    private JLabel patternValidationLabel;
    private DefaultTableModel currentPatternTableModel;
    private JTable currentPatternTable;
    
    // Data storage
    private Map<String, List<ExamComponent>> subjectExamPatterns;
    private Map<String, MarkingScheme> subjectMarkingSchemes;
    
    // Action buttons
    private JButton saveButton;
    private JButton cancelButton;
    
    // Close callback
    private Runnable onCloseCallback;
    private Integer editSectionId = null; // null = create mode, non-null = edit mode
    
    public CreateSectionPanel(JFrame parent, int userId) {
        this(parent, userId, null, null);
    }
    
    public CreateSectionPanel(JFrame parent, int userId, Runnable onCloseCallback) {
        this(parent, userId, null, onCloseCallback);
    }
    
    public CreateSectionPanel(JFrame parent, int userId, Integer sectionId, Runnable onCloseCallback) {
        this.parentFrame = parent;
        this.userId = userId;
        this.editSectionId = sectionId;
        this.onCloseCallback = onCloseCallback;
        this.themeManager = ThemeManager.getInstance();
        this.subjectExamPatterns = new HashMap<>();
        this.subjectMarkingSchemes = new HashMap<>();
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        applyStyling();
        
        // Load existing data if in edit mode
        if (editSectionId != null) {
            loadSectionDataForEdit();
        }
    }
    
    private void initializeComponents() {
        // Initialize basic info components
        sectionNameField = new JTextField(20);
        
        semesterField = new JTextField(10);
        studentCountField = new JTextField(10);
        
        Calendar currentCalendar = Calendar.getInstance();
        int currentYear = currentCalendar.get(Calendar.YEAR);
        yearSpinner = new JSpinner(new SpinnerNumberModel(currentYear, 2020, 2030, 1));
        
        descriptionArea = new JTextArea(3, 20);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        
        // Initialize subject components
        subjectTableModel = new DefaultTableModel(new String[]{"Subject Name", "Total Marks", "Credit", "Pass Marks"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        subjectTable = new JTable(subjectTableModel);
        
        subjectNameField = new JTextField(15);
        subjectMarksField = new JTextField(10);
        
        // Initialize exam pattern components
        examPatternsSubjectCombo = new JComboBox<>();
        patternValidationLabel = new JLabel("Select a subject to configure exam patterns");
        
        currentPatternTableModel = new DefaultTableModel(
            new String[]{"Component", "Max Marks", "Weightage", "Actions"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3; // Only Actions column is editable
            }
        };
        currentPatternTable = new JTable(currentPatternTableModel);
        
        // Action buttons
        saveButton = new JButton("Create Section");
        cancelButton = new JButton("Cancel");
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        // Header with title and close button
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        
        // Main content with tabs
        JTabbedPane tabbedPane = createStyledTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);
        
        // Button panel at bottom
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));
        
        // Title
        JLabel titleLabel = new JLabel("Create New Section");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        titleLabel.setForeground(themeManager.getTextPrimaryColor());
        
        // Close button
        JButton closeBtn = new JButton("✕");
        closeBtn.setFont(new Font("SansSerif", Font.PLAIN, 18));
        closeBtn.setForeground(themeManager.getTextSecondaryColor());
        closeBtn.setBackground(new Color(0, 0, 0, 0));
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.setPreferredSize(new Dimension(30, 30));
        
        header.add(titleLabel, BorderLayout.WEST);
        header.add(closeBtn, BorderLayout.EAST);
        
        return header;
    }
    
    private JTabbedPane createStyledTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(SUBTITLE_FONT);
        tabbedPane.setBackground(themeManager.getBackgroundColor());
        
        // Tab 1: Basic Information
        JPanel basicInfoTab = createBasicInfoTab();
        tabbedPane.addTab("Basic Information", basicInfoTab);
        
        // Tab 2: Subjects
        JPanel subjectsTab = createSubjectsTab();
        tabbedPane.addTab("Subjects", subjectsTab);
        
        // Tab 3: Exam Patterns
        JPanel examPatternsTab = createExamPatternsTab();
        tabbedPane.addTab("Exam Patterns", examPatternsTab);
        
        return tabbedPane;
    }
    
    private JPanel createBasicInfoTab() {
        JPanel tab = new JPanel(new GridBagLayout());
        tab.setOpaque(false);
        tab.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 0, 15, 0);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Section Name
        gbc.gridx = 0; gbc.gridy = 0;
        tab.add(createFieldLabel("Section Name *"), gbc);
        gbc.gridx = 1;
        tab.add(sectionNameField, gbc);
        
        // Semester
        gbc.gridx = 0; gbc.gridy = 1;
        tab.add(createFieldLabel("Semester *"), gbc);
        gbc.gridx = 1;
        tab.add(semesterField, gbc);
        
        // Number of Students
        gbc.gridx = 0; gbc.gridy = 2;
        tab.add(createFieldLabel("Number of Students *"), gbc);
        gbc.gridx = 1;
        tab.add(studentCountField, gbc);
        
        // Year
        gbc.gridx = 0; gbc.gridy = 3;
        tab.add(createFieldLabel("Academic Year *"), gbc);
        gbc.gridx = 1;
        tab.add(yearSpinner, gbc);
        
        // Description
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        tab.add(createFieldLabel("Description"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        
        JScrollPane descScrollPane = new JScrollPane(descriptionArea);
        descScrollPane.setPreferredSize(new Dimension(300, 80));
        tab.add(descScrollPane, gbc);
        
        return tab;
    }
    
    private JPanel createSubjectsTab() {
        JPanel tab = new JPanel(new BorderLayout());
        tab.setOpaque(false);
        tab.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        
        // Add subject form
        JPanel addSubjectPanel = createAddSubjectPanel();
        tab.add(addSubjectPanel, BorderLayout.NORTH);
        
        // Subject table
        JPanel tablePanel = createSubjectTablePanel();
        tab.add(tablePanel, BorderLayout.CENTER);
        
        return tab;
    }
    
    private JPanel createAddSubjectPanel() {
        JPanel panel = UIComponentFactory.createStyledPanel(themeManager.getCardColor(), BORDER_RADIUS);
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JButton addBtn = createStyledButton("+ Add Subject", PRIMARY_COLOR);
        addBtn.addActionListener(e -> showAddSubjectDialog());
        panel.add(addBtn);
        
        return panel;
    }
    
    private JPanel createSubjectTablePanel() {
        JPanel panel = UIComponentFactory.createStyledPanel(themeManager.getCardColor(), BORDER_RADIUS);
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Added Subjects"));
        
        JScrollPane scrollPane = new JScrollPane(subjectTable);
        scrollPane.setPreferredSize(new Dimension(0, 200));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createExamPatternsTab() {
        JPanel tab = new JPanel(new BorderLayout());
        tab.setOpaque(false);
        tab.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        
        // Subject selection and pattern buttons
        JPanel controlPanel = createPatternControlPanel();
        tab.add(controlPanel, BorderLayout.NORTH);
        
        // Pattern display
        JPanel displayPanel = createPatternDisplayPanel();
        tab.add(displayPanel, BorderLayout.CENTER);
        
        return tab;
    }
    
    private JPanel createPatternControlPanel() {
        JPanel panel = UIComponentFactory.createStyledPanel(themeManager.getCardColor(), BORDER_RADIUS);
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Subject selection
        JPanel subjectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        subjectPanel.setOpaque(false);
        subjectPanel.add(createFieldLabel("Subject:"));
        subjectPanel.add(examPatternsSubjectCombo);
        
        // Template buttons panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createTitledBorder("University Pattern Templates"));
        
        String[] templates = {
            "3 Internal + Final (100M)",
            "2 Internal + Final (100M)",
            "Theory + Lab (100M)",
            "Practical Only (100M)"
        };
        
        for (String template : templates) {
            JButton btn = createStyledButton(template, new Color(248, 250, 252));
            btn.setForeground(Color.BLACK);
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.setMaximumSize(new Dimension(350, 35));
            btn.addActionListener(e -> applyTemplate(template));
            buttonPanel.add(btn);
            buttonPanel.add(Box.createVerticalStrut(5));
        }
        
        buttonPanel.add(Box.createVerticalStrut(10));
        JButton customBtn = createStyledButton("✏️ Create Custom Pattern", PRIMARY_COLOR);
        customBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        customBtn.setMaximumSize(new Dimension(350, 40));
        customBtn.addActionListener(e -> createCustomPattern());
        buttonPanel.add(customBtn);
        
        panel.add(subjectPanel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createPatternDisplayPanel() {
        JPanel panel = UIComponentFactory.createStyledPanel(themeManager.getCardColor(), BORDER_RADIUS);
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Exam Pattern Configuration"));
        
        // Pattern table
        JScrollPane scrollPane = new JScrollPane(currentPatternTable);
        scrollPane.setPreferredSize(new Dimension(0, 200));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Validation and add component
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        bottomPanel.add(patternValidationLabel, BorderLayout.WEST);
        
        JButton addComponentBtn = createStyledButton("Add Component", PRIMARY_COLOR);
        addComponentBtn.addActionListener(e -> showAddComponentDialog());
        bottomPanel.add(addComponentBtn, BorderLayout.EAST);
        
        panel.add(bottomPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        
        // Style buttons
        cancelButton.setFont(BUTTON_FONT);
        cancelButton.setPreferredSize(new Dimension(120, 40));
        saveButton.setFont(BUTTON_FONT);
        saveButton.setPreferredSize(new Dimension(150, 40));
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        
        return buttonPanel;
    }
    
    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BODY_FONT);
        label.setForeground(themeManager.getTextPrimaryColor());
        return label;
    }
    
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(150, 35));
        
        // Add hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(bgColor.darker());
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }
    
    private void applyStyling() {
        setBackground(themeManager.getBackgroundColor());
        
        // Style text fields
        JTextField[] textFields = {sectionNameField, semesterField, studentCountField, subjectNameField, subjectMarksField};
        for (JTextField field : textFields) {
            styleTextField(field);
        }
        
        // Style combo boxes
        JComboBox<?>[] comboBoxes = {examPatternsSubjectCombo};
        for (JComboBox<?> combo : comboBoxes) {
            styleComboBox(combo);
        }
        
        // Style tables
        JTable[] tables = {subjectTable, currentPatternTable};
        for (JTable table : tables) {
            styleTable(table);
        }
        
        // Style text area
        styleTextArea(descriptionArea);
        
        // Style buttons
        styleActionButtons();
    }
    
    private void styleTextField(JTextField field) {
        field.setFont(BODY_FONT);
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(themeManager.getBorderColor(), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        field.setPreferredSize(new Dimension(200, 35));
    }
    
    private void styleComboBox(JComboBox<?> combo) {
        combo.setFont(BODY_FONT);
        combo.setBackground(Color.WHITE);
        combo.setPreferredSize(new Dimension(150, 35));
    }
    
    private void styleTable(JTable table) {
        table.setFont(BODY_FONT);
        table.setRowHeight(30);
        table.getTableHeader().setFont(SUBTITLE_FONT);
        table.getTableHeader().setBackground(themeManager.getCardColor());
        table.setGridColor(themeManager.getBorderColor());
        table.setSelectionBackground(themeManager.getPrimaryColor().brighter());
    }
    
    private void styleTextArea(JTextArea area) {
        area.setFont(BODY_FONT);
        area.setBackground(Color.WHITE);
        area.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(themeManager.getBorderColor(), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
    }
    
    private void styleActionButtons() {
        saveButton.setBackground(PRIMARY_COLOR);
        saveButton.setForeground(Color.WHITE);
        saveButton.setBorderPainted(false);
        saveButton.setFocusPainted(false);
        saveButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        cancelButton.setBackground(themeManager.getTextSecondaryColor());
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setBorderPainted(false);
        cancelButton.setFocusPainted(false);
        cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
    
    private void loadSectionDataForEdit() {
        if (editSectionId == null) return;
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Load basic section info
            String sql = "SELECT section_name, total_students, academic_year, semester FROM sections WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, editSectionId);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    sectionNameField.setText(rs.getString("section_name"));
                    studentCountField.setText(String.valueOf(rs.getInt("total_students")));
                    
                    // Load year and semester
                    int academicYear = rs.getInt("academic_year");
                    int semester = rs.getInt("semester");
                    if (yearSpinner != null && academicYear > 0) {
                        yearSpinner.setValue(academicYear);
                    }
                    if (semesterField != null && semester > 0) {
                        semesterField.setText(String.valueOf(semester));
                    }
                }
            }
            
            // Load subjects
            sql = "SELECT s.subject_name, ss.max_marks, ss.credit, ss.passing_marks " +
                  "FROM subjects s " +
                  "JOIN section_subjects ss ON s.id = ss.subject_id " +
                  "WHERE ss.section_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, editSectionId);
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    String subjectName = rs.getString("subject_name");
                    subjectTableModel.addRow(new Object[]{
                        subjectName,
                        String.valueOf(rs.getInt("max_marks")),
                        String.valueOf(rs.getInt("credit")),
                        String.valueOf(rs.getInt("passing_marks"))
                    });
                    // Also add to exam patterns combo
                    examPatternsSubjectCombo.addItem(subjectName);
                }
            }
            
            // Update save button text
            if (saveButton != null) {
                saveButton.setText("Update Section");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error loading section data: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void setupEventHandlers() {
        // Subject selection change handler
        examPatternsSubjectCombo.addActionListener(e -> {
            String selectedSubject = getCurrentSelectedSubject();
            if (selectedSubject != null) {
                displayPatternForSubject(selectedSubject);
                validatePattern(selectedSubject);
            }
        });
        
        // Cancel button
        cancelButton.addActionListener(e -> closePanel());
        
        // Save button  
        saveButton.addActionListener(e -> saveSection());
    }
    
    // Business logic methods (adapted from original dialog)
    
    private void showAddSubjectDialog() {
        JDialog dialog = new JDialog(parentFrame, "Add Subject", true);
        dialog.setSize(450, 400);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        panel.setBackground(themeManager.getCardColor());
        
        // Subject Name
        JPanel namePanel = new JPanel(new BorderLayout());
        namePanel.setBackground(themeManager.getCardColor());
        namePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        JLabel nameLabel = new JLabel("Subject Name:");
        nameLabel.setFont(BODY_FONT);
        JTextField nameField = new JTextField();
        styleTextField(nameField);
        namePanel.add(nameLabel, BorderLayout.NORTH);
        namePanel.add(nameField, BorderLayout.CENTER);
        
        // Total Marks
        JPanel marksPanel = new JPanel(new BorderLayout());
        marksPanel.setBackground(themeManager.getCardColor());
        marksPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        JLabel marksLabel = new JLabel("Total Marks:");
        marksLabel.setFont(BODY_FONT);
        JTextField marksField = new JTextField();
        styleTextField(marksField);
        marksPanel.add(marksLabel, BorderLayout.NORTH);
        marksPanel.add(marksField, BorderLayout.CENTER);
        
        // Credit
        JPanel creditPanel = new JPanel(new BorderLayout());
        creditPanel.setBackground(themeManager.getCardColor());
        creditPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        JLabel creditLabel = new JLabel("Credit:");
        creditLabel.setFont(BODY_FONT);
        JTextField creditField = new JTextField();
        styleTextField(creditField);
        creditPanel.add(creditLabel, BorderLayout.NORTH);
        creditPanel.add(creditField, BorderLayout.CENTER);
        
        // Pass Marks
        JPanel passMarksPanel = new JPanel(new BorderLayout());
        passMarksPanel.setBackground(themeManager.getCardColor());
        passMarksPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        JLabel passMarksLabel = new JLabel("Pass Marks:");
        passMarksLabel.setFont(BODY_FONT);
        JTextField passMarksField = new JTextField();
        styleTextField(passMarksField);
        passMarksPanel.add(passMarksLabel, BorderLayout.NORTH);
        passMarksPanel.add(passMarksField, BorderLayout.CENTER);
        
        panel.add(namePanel);
        panel.add(Box.createVerticalStrut(15));
        panel.add(marksPanel);
        panel.add(Box.createVerticalStrut(15));
        panel.add(creditPanel);
        panel.add(Box.createVerticalStrut(15));
        panel.add(passMarksPanel);
        panel.add(Box.createVerticalGlue());
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(themeManager.getCardColor());
        
        JButton cancelBtn = createStyledButton("Cancel", ERROR_COLOR);
        JButton addBtn = createStyledButton("Add", SUCCESS_COLOR);
        
        buttonPanel.add(cancelBtn);
        buttonPanel.add(addBtn);
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        // Actions
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        addBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String marks = marksField.getText().trim();
            String credit = creditField.getText().trim();
            String passMarks = passMarksField.getText().trim();
            
            if (name.isEmpty() || marks.isEmpty() || credit.isEmpty() || passMarks.isEmpty()) {
                showError("Please fill all fields");
                return;
            }
            
            try {
                int marksInt = Integer.parseInt(marks);
                int creditInt = Integer.parseInt(credit);
                int passMarksInt = Integer.parseInt(passMarks);
                
                if (marksInt <= 0 || creditInt <= 0 || passMarksInt <= 0) {
                    showError("All values must be greater than 0");
                    return;
                }
                
                if (passMarksInt >= marksInt) {
                    showError("Pass marks must be less than total marks");
                    return;
                }
                
                subjectTableModel.addRow(new Object[]{name, marks, credit, passMarks});
                examPatternsSubjectCombo.addItem(name);
                dialog.dispose();
                showSuccess("Subject added successfully");
                
            } catch (NumberFormatException ex) {
                showError("Please enter valid numbers");
            }
        });
        
        dialog.setVisible(true);
    }
    
    private void addSubject() {
        String name = subjectNameField.getText().trim();
        String marksText = subjectMarksField.getText().trim();
        
        if (name.isEmpty() || marksText.isEmpty()) {
            showError("Please fill in both subject name and marks");
            return;
        }
        
        try {
            int marks = Integer.parseInt(marksText);
            if (marks <= 0) {
                showError("Marks must be a positive number");
                return;
            }
            
            // Check for duplicate
            for (int i = 0; i < subjectTableModel.getRowCount(); i++) {
                if (subjectTableModel.getValueAt(i, 0).equals(name)) {
                    showError("Subject already exists");
                    return;
                }
            }
            
            subjectTableModel.addRow(new Object[]{name, marksText});
            examPatternsSubjectCombo.addItem(name);
            
            subjectNameField.setText("");
            subjectMarksField.setText("");
            
            showSuccess("Subject added successfully");
            
        } catch (NumberFormatException e) {
            showError("Please enter a valid number for marks");
        }
    }
    
    private void applyTemplate(String templateName) {
        String selectedSubject = getCurrentSelectedSubject();
        if (selectedSubject == null) {
            showError("Please select a subject first");
            return;
        }
        
        int totalMarks = getSubjectTotalMarks(selectedSubject);
        if (totalMarks <= 0) {
            showError("Invalid total marks for subject: " + selectedSubject);
            return;
        }
        
        List<ExamComponent> components = new ArrayList<>();
        
        switch (templateName) {
            case "3 Internal + Final (100M)":
                // Scale to actual marks
                double scale = totalMarks / 100.0;
                components.add(new ExamComponent("Internal 1", (int)(20 * scale), (int)(20 * scale)));
                components.add(new ExamComponent("Internal 2", (int)(25 * scale), (int)(25 * scale)));
                components.add(new ExamComponent("Internal 3", (int)(15 * scale), (int)(15 * scale)));
                components.add(new ExamComponent("Final Exam", (int)(40 * scale), (int)(40 * scale)));
                break;
                
            case "2 Internal + Final (100M)":
                scale = totalMarks / 100.0;
                components.add(new ExamComponent("Internal 1", (int)(25 * scale), (int)(25 * scale)));
                components.add(new ExamComponent("Internal 2", (int)(25 * scale), (int)(25 * scale)));
                components.add(new ExamComponent("Final Exam", (int)(50 * scale), (int)(50 * scale)));
                break;
                
            case "Theory + Lab (100M)":
                scale = totalMarks / 100.0;
                components.add(new ExamComponent("Theory Internal", (int)(20 * scale), (int)(20 * scale)));
                components.add(new ExamComponent("Theory Final", (int)(50 * scale), (int)(50 * scale)));
                components.add(new ExamComponent("Lab Internal", (int)(10 * scale), (int)(10 * scale)));
                components.add(new ExamComponent("Lab Final", (int)(20 * scale), (int)(20 * scale)));
                break;
                
            case "Practical Only (100M)":
                scale = totalMarks / 100.0;
                components.add(new ExamComponent("Lab Work", (int)(40 * scale), (int)(40 * scale)));
                components.add(new ExamComponent("Practical Exam", (int)(60 * scale), (int)(60 * scale)));
                break;
        }
        
        subjectExamPatterns.put(selectedSubject, components);
        displayPattern(components);
        validatePattern(selectedSubject);
        
        showSuccess("Applied template: " + templateName + " for " + selectedSubject);
    }
    
    private void createCustomPattern() {
        String selectedSubject = getCurrentSelectedSubject();
        if (selectedSubject == null) {
            showError("Please select a subject first");
            return;
        }
        
        List<ExamComponent> components = new ArrayList<>();
        subjectExamPatterns.put(selectedSubject, components);
        displayPattern(components);
        
        patternValidationLabel.setText("✏️ Custom pattern started - Add components below");
        patternValidationLabel.setForeground(themeManager.getPrimaryColor());
        
        showSuccess("Custom pattern created for " + selectedSubject + ". Start adding components!");
    }
    
    private String getCurrentSelectedSubject() {
        if (examPatternsSubjectCombo != null && examPatternsSubjectCombo.getSelectedItem() != null) {
            String selected = (String) examPatternsSubjectCombo.getSelectedItem();
            return selected.trim().isEmpty() ? null : selected;
        }
        return null;
    }
    
    private void displayPatternForSubject(String subjectName) {
        List<ExamComponent> components = subjectExamPatterns.get(subjectName);
        displayPattern(components);
    }
    
    private void displayPattern(List<ExamComponent> components) {
        if (currentPatternTableModel == null) {
            return;
        }
        
        currentPatternTableModel.setRowCount(0);
        
        if (components == null || components.isEmpty()) {
            return;
        }
        
        for (ExamComponent component : components) {
            Object[] row = {
                component.componentName,
                component.maxMarks,
                component.weightage,
                "Edit/Remove"
            };
            currentPatternTableModel.addRow(row);
        }
        
        validatePattern(getCurrentSelectedSubject());
    }
    
    private void validatePattern(String subjectName) {
        if (subjectName == null) return;
        
        List<ExamComponent> components = subjectExamPatterns.get(subjectName);
        int expectedMarks = getSubjectTotalMarks(subjectName);
        
        if (components == null || components.isEmpty()) {
            patternValidationLabel.setText("⚠️ No components defined");
            patternValidationLabel.setForeground(new Color(251, 191, 36)); // amber
            return;
        }
        
        int totalMarks = components.stream().mapToInt(c -> c.maxMarks).sum();
        
        if (totalMarks == expectedMarks) {
            patternValidationLabel.setText("✅ Pattern valid (" + totalMarks + "/" + expectedMarks + " marks)");
            patternValidationLabel.setForeground(new Color(34, 197, 94)); // green
        } else {
            patternValidationLabel.setText("❌ Invalid total: " + totalMarks + "/" + expectedMarks + " marks");
            patternValidationLabel.setForeground(new Color(239, 68, 68)); // red
        }
    }
    
    private int getSubjectTotalMarks(String subjectName) {
        for (int i = 0; i < subjectTableModel.getRowCount(); i++) {
            if (subjectTableModel.getValueAt(i, 0).equals(subjectName)) {
                return Integer.parseInt((String) subjectTableModel.getValueAt(i, 1));
            }
        }
        return 0;
    }
    
    private void showAddComponentDialog() {
        String subjectName = getCurrentSelectedSubject();
        if (subjectName == null) {
            showError("Please select a subject first");
            return;
        }
        
        JDialog dialog = new JDialog(parentFrame, "Add Component - " + subjectName, true);
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(this);
        
        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Component name
        gbc.gridx = 0; gbc.gridy = 0;
        content.add(new JLabel("Component Name:"), gbc);
        gbc.gridx = 1;
        JTextField nameField = new JTextField(15);
        styleTextField(nameField);
        content.add(nameField, gbc);
        
        // Marks
        gbc.gridx = 0; gbc.gridy = 1;
        content.add(new JLabel("Max Marks:"), gbc);
        gbc.gridx = 1;
        JTextField marksField = new JTextField(10);
        styleTextField(marksField);
        content.add(marksField, gbc);
        
        // Buttons
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addBtn = createStyledButton("Add", SUCCESS_COLOR);
        JButton cancelBtn = createStyledButton("Cancel", ERROR_COLOR);
        
        addBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String marksText = marksField.getText().trim();
            
            if (name.isEmpty() || marksText.isEmpty()) {
                showError("Please fill in all fields");
                return;
            }
            
            try {
                int marks = Integer.parseInt(marksText);
                if (marks <= 0) {
                    showError("Marks must be positive");
                    return;
                }
                
                List<ExamComponent> components = subjectExamPatterns.get(subjectName);
                if (components == null) {
                    components = new ArrayList<>();
                    subjectExamPatterns.put(subjectName, components);
                }
                
                components.add(new ExamComponent(name, marks, marks));
                displayPattern(components);
                validatePattern(subjectName);
                
                dialog.dispose();
                showSuccess("Component added successfully");
                
            } catch (NumberFormatException ex) {
                showError("Please enter a valid number for marks");
            }
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(addBtn);
        buttonPanel.add(cancelBtn);
        content.add(buttonPanel, gbc);
        
        dialog.add(content);
        dialog.setVisible(true);
    }
    
    private void saveSection() {
        // Validate basic info
        if (sectionNameField.getText().trim().isEmpty()) {
            showError("Please fill in all required fields");
            return;
        }
        
        if (semesterField.getText().trim().isEmpty()) {
            showError("Please enter semester number");
            return;
        }
        
        if (studentCountField.getText().trim().isEmpty()) {
            showError("Please enter number of students");
            return;
        }
        
        if (subjectTableModel.getRowCount() == 0) {
            showError("Please add at least one subject");
            return;
        }
        
        try {
            // Get form values
            String sectionName = sectionNameField.getText().trim();
            int semester = Integer.parseInt(semesterField.getText().trim());
            int studentCount = Integer.parseInt(studentCountField.getText().trim());
            int year = (Integer) yearSpinner.getValue();
            String description = descriptionArea.getText().trim();
            
            // Create subject info list
            java.util.List<com.sms.dao.SectionDAO.SubjectInfo> subjectInfos = new java.util.ArrayList<>(); 
            for (int i = 0; i < subjectTableModel.getRowCount(); i++) {
                String subjectName = (String) subjectTableModel.getValueAt(i, 0);
                String totalMarks = (String) subjectTableModel.getValueAt(i, 1);
                String credit = (String) subjectTableModel.getValueAt(i, 2);
                String passMarks = (String) subjectTableModel.getValueAt(i, 3);
                subjectInfos.add(new com.sms.dao.SectionDAO.SubjectInfo(subjectName, Integer.parseInt(totalMarks), Integer.parseInt(credit), Integer.parseInt(passMarks)));
            }
            
            boolean success;
            int sectionId;
            
            if (editSectionId != null) {
                // UPDATE MODE - Update existing section
                success = updateSection(editSectionId, sectionName, subjectInfos, studentCount, year, semester);
                sectionId = editSectionId;
                
                if (success) {
                    showSuccess("Section updated successfully!");
                    closePanel();
                } else {
                    showError("Failed to update section");
                }
            } else {
                // CREATE MODE - Create new section
                SectionDAO sectionDAO = new SectionDAO();
                success = sectionDAO.createSection(sectionName, subjectInfos, studentCount, userId, year, semester);
                
                if (success) {
                    // Get the newly created section ID
                    sectionId = sectionDAO.getSectionIdByName(sectionName, userId);
                    
                    // Save exam patterns if they exist
                    if (sectionId > 0 && !subjectExamPatterns.isEmpty()) {
                        saveExamPatterns(sectionId, subjectInfos);
                    }
                    
                    showSuccess("Section created successfully!");
                    closePanel();
                } else {
                    showError("Failed to create section");
                }
            }
            
        } catch (NumberFormatException e) {
            showError("Please enter valid numbers for semester, student count, and total marks");
        } catch (Exception e) {
            showError("Error saving section: " + e.getMessage());
        }
    }
    
    private boolean updateSection(int sectionId, String sectionName, 
                                   java.util.List<com.sms.dao.SectionDAO.SubjectInfo> subjectInfos, 
                                   int studentCount, int year, int semester) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            // Update sections table
            String updateSectionSQL = "UPDATE sections SET section_name = ?, total_students = ?, academic_year = ?, semester = ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateSectionSQL)) {
                pstmt.setString(1, sectionName);
                pstmt.setInt(2, studentCount);
                pstmt.setInt(3, year);
                pstmt.setInt(4, semester);
                pstmt.setInt(5, sectionId);
                pstmt.executeUpdate();
            }
            
            // Delete existing section_subjects entries
            String deleteSubjectsSQL = "DELETE FROM section_subjects WHERE section_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteSubjectsSQL)) {
                pstmt.setInt(1, sectionId);
                pstmt.executeUpdate();
            }
            
            // Insert updated subjects
            String insertSubjectSQL = "INSERT INTO section_subjects (section_id, subject_id, max_marks, passing_marks, credit) " +
                                     "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSubjectSQL)) {
                for (com.sms.dao.SectionDAO.SubjectInfo subjectInfo : subjectInfos) {
                    // Get or create subject
                    int subjectId = getOrCreateSubject(conn, subjectInfo.name);
                    
                    pstmt.setInt(1, sectionId);
                    pstmt.setInt(2, subjectId);
                    pstmt.setInt(3, subjectInfo.totalMarks);
                    pstmt.setInt(4, subjectInfo.passMarks);
                    pstmt.setInt(5, subjectInfo.credit);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
            
            conn.commit();
            return true;
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private int getOrCreateSubject(Connection conn, String subjectName) throws SQLException {
        // First try to find existing subject
        String selectSQL = "SELECT id FROM subjects WHERE subject_name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            pstmt.setString(1, subjectName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        
        // If not found, create new subject
        String insertSQL = "INSERT INTO subjects (subject_name) VALUES (?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL, PreparedStatement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, subjectName);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        
        throw new SQLException("Failed to get or create subject: " + subjectName);
    }
    
    private void closePanel() {
        // Use callback if provided, otherwise fall back to direct cast
        if (onCloseCallback != null) {
            onCloseCallback.run();
        } else if (parentFrame instanceof com.sms.dashboard.DashboardScreen) {
            ((com.sms.dashboard.DashboardScreen) parentFrame).closeSectionCreationPanel();
        }
    }
    
    private void saveExamPatterns(int sectionId, java.util.List<com.sms.dao.SectionDAO.SubjectInfo> subjectInfos) {
        try (Connection conn = com.sms.database.DatabaseConnection.getConnection()) {
            // For each subject that has exam patterns defined
            for (com.sms.dao.SectionDAO.SubjectInfo subjectInfo : subjectInfos) {
                List<ExamComponent> components = subjectExamPatterns.get(subjectInfo.name);
                
                if (components != null && !components.isEmpty()) {
                    // Get subject ID
                    int subjectId = getSubjectId(conn, subjectInfo.name);
                    
                    if (subjectId > 0) {
                        // Insert exam types and link them to subject
                        for (ExamComponent component : components) {
                            // Insert into exam_types table
                            String insertExamType = "INSERT INTO exam_types (section_id, exam_name, weightage, created_by) VALUES (?, ?, ?, ?)";
                            try (PreparedStatement ps = conn.prepareStatement(insertExamType, PreparedStatement.RETURN_GENERATED_KEYS)) {
                                ps.setInt(1, sectionId);
                                ps.setString(2, component.componentName);
                                ps.setInt(3, component.weightage);
                                ps.setInt(4, userId);
                                ps.executeUpdate();
                                
                                // Get the generated exam_type_id
                                try (ResultSet rs = ps.getGeneratedKeys()) {
                                    if (rs.next()) {
                                        int examTypeId = rs.getInt(1);
                                        
                                        // Link exam type with subject in subject_exam_types
                                        String linkQuery = "INSERT INTO subject_exam_types (section_id, subject_id, exam_type_id) VALUES (?, ?, ?)";
                                        try (PreparedStatement linkPs = conn.prepareStatement(linkQuery)) {
                                            linkPs.setInt(1, sectionId);
                                            linkPs.setInt(2, subjectId);
                                            linkPs.setInt(3, examTypeId);
                                            linkPs.executeUpdate();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error saving exam patterns: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private int getSubjectId(Connection conn, String subjectName) {
        try {
            String query = "SELECT id FROM subjects WHERE subject_name = ?";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, subjectName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("id");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting subject ID: " + e.getMessage());
        }
        return -1;
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }
    
    // Inner classes for exam structure (same as original dialog)
    private static class ExamComponent {
        String componentName;
        int maxMarks;
        int weightage;
        
        ExamComponent(String componentName, int maxMarks, int weightage) {
            this.componentName = componentName;
            this.maxMarks = maxMarks;
            this.weightage = weightage;
        }
        
        @Override
        public String toString() {
            return componentName + " (" + maxMarks + "M, " + weightage + "%)";  
        }
    }
}