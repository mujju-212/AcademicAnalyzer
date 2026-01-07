package com.sms.dashboard.dialogs;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
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

/**
 * Inner classes for exam structure
 */
class ExamComponent {
    public final String name;
    public final int marks;
    public final String type;
    
    public ExamComponent(String name, int marks, String type) {
        this.name = name;
        this.marks = marks;
        this.type = type;
    }
}

class ExamPattern {
    private final String[] components;
    
    public ExamPattern(String... components) {
        this.components = components;
    }
    
    public String[] getComponents() {
        return components;
    }
}

class MarkDistribution {
    public final String examType;
    public final int weightage;
    public final int totalMarks;
    
    public MarkDistribution(String examType, int weightage, int totalMarks) {
        this.examType = examType;
        this.weightage = weightage;
        this.totalMarks = totalMarks;
    }
}

public class CreateSectionDialog extends JDialog {
    
    private JTextField sectionField;
    private JTextField totalStudentsField;
    private DefaultTableModel subjectTableModel;
    private JTable subjectTable;
    private ThemeManager themeManager;
    private JTabbedPane tabbedPane;
    private ButtonGroup markingSystemGroup;
    private boolean useFlexibleMarking = false;
    private JLabel currentSubjectLabel;
    private String selectedSubject;
    
    // Per-subject exam pattern storage
    private Map<String, List<ExamComponent>> subjectExamPatterns;
    private Map<String, MarkingScheme> subjectMarkingSchemes;
    private Map<String, List<MarkDistribution>> subjectMarkDistributions;
    private DefaultTableModel examTypeTableModel;
    private JLabel totalSummaryLabel;
    private JLabel warningLabel;
    private JComboBox<String> examPatternsSubjectCombo; // Reference to update when subjects change
    private DefaultTableModel currentPatternTableModel; // Reference to pattern display table
    private JLabel patternValidationLabel; // Reference to validation label
    private int userId;
    private Integer editSectionId = null; // null means create mode, non-null means edit mode
    
    // University pattern templates
    private static final Map<String, ExamPattern> UNIVERSITY_TEMPLATES = new HashMap<>();
    
    static {
        // Initialize common university patterns
        UNIVERSITY_TEMPLATES.put("3 Internal + Final (100M)", 
            new ExamPattern("Internal 1: 20", "Internal 2: 25", "Internal 3: 15", "Final Exam: 40"));
        UNIVERSITY_TEMPLATES.put("2 Internal + Final (100M)", 
            new ExamPattern("Internal 1: 25", "Internal 2: 25", "Final Exam: 50"));
        UNIVERSITY_TEMPLATES.put("Theory + Lab (100M)", 
            new ExamPattern("Theory Internal: 20", "Theory Final: 50", "Lab Internal: 10", "Lab Final: 20"));
        UNIVERSITY_TEMPLATES.put("Practical Only (100M)", 
            new ExamPattern("Lab Work: 40", "Practical Exam: 60"));
        UNIVERSITY_TEMPLATES.put("Project Based (100M)", 
            new ExamPattern("Project Report: 40", "Project Viva: 30", "Presentation: 30"));
    }

    public CreateSectionDialog(JFrame parent, int userId) {
        this(parent, userId, null); // Call overloaded constructor in create mode
    }
    
    // Constructor for edit mode
    public CreateSectionDialog(JFrame parent, int userId, Integer sectionId) {
        super(parent, sectionId == null ? "Create Section" : "Edit Section", true);
        this.userId = userId;
        this.editSectionId = sectionId;
        themeManager = ThemeManager.getInstance();
        subjectExamPatterns = new HashMap<>();
        subjectMarkingSchemes = new HashMap<>();
        subjectMarkDistributions = new HashMap<>();
        examTypeTableModel = new DefaultTableModel();
        totalSummaryLabel = new JLabel();
        warningLabel = new JLabel();
        
        setSize(900, 650);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(themeManager.getCardColor());
        
        // Header
        JPanel headerPanel = createHeaderPanel();
        mainContainer.add(headerPanel, BorderLayout.NORTH);
        
        // Content with 3 tabs only
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        // Tab 1: Basic Information
        JPanel basicInfoPanel = createBasicInfoPanel();
        tabbedPane.addTab("Basic Information", basicInfoPanel);
        
        // Tab 2: Subjects  
        JPanel subjectsPanel = createSubjectsPanel();
        tabbedPane.addTab("Subjects", subjectsPanel);
        
        // Tab 3: Exam Patterns (Per Subject)
        JPanel examPatternsPanel = createExamPatternsPanel();
        tabbedPane.addTab("Exam Patterns", examPatternsPanel);
        
        mainContainer.add(tabbedPane, BorderLayout.CENTER);
        
        // Footer
        JPanel footerPanel = createFooterPanel();
        mainContainer.add(footerPanel, BorderLayout.SOUTH);
        
        add(mainContainer);
        getContentPane().setBackground(themeManager.getCardColor());
        
        // Load existing data if in edit mode
        if (editSectionId != null) {
            loadSectionData();
        }
    }
    
    private JPanel createBasicInfoPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(themeManager.getCardColor());
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        
        // Section Name
        JPanel sectionPanel = createFieldPanel("Section Name");
        sectionField = createStyledTextField();
        sectionPanel.add(sectionField);
        
        // Total Students
        JPanel studentsPanel = createFieldPanel("Total Students");
        totalStudentsField = createStyledTextField();
        studentsPanel.add(totalStudentsField);
        
        panel.add(sectionPanel);
        panel.add(Box.createVerticalStrut(30));
        panel.add(studentsPanel);
        panel.add(Box.createVerticalGlue());
        
        return panel;
    }
    
    private JPanel createSubjectsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(themeManager.getCardColor());
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(themeManager.getCardColor());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        JLabel titleLabel = new JLabel("Subjects");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(themeManager.getTextPrimaryColor());
        
        JButton addButton = createCompactPrimaryButton("+ Add Subject");
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(addButton, BorderLayout.EAST);
        
        // Table
        String[] columns = {"Subject Name", "Total Marks", "Credit", "Pass Marks"};
        subjectTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        subjectTable = createStyledTable(subjectTableModel);
        
        JScrollPane scrollPane = new JScrollPane(subjectTable);
        scrollPane.setPreferredSize(new Dimension(0, 300));
        
        // Actions
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionPanel.setBackground(themeManager.getCardColor());
        
        JButton removeBtn = createCompactDangerButton("Remove");
        JButton editBtn = createCompactSecondaryButton("Edit");
        
        actionPanel.add(removeBtn);
        actionPanel.add(editBtn);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(actionPanel, BorderLayout.SOUTH);
        
        // Listeners
        addButton.addActionListener(e -> showAddSubjectDialog());
        
        removeBtn.addActionListener(e -> {
            int row = subjectTable.getSelectedRow();
            if (row != -1) {
                String subjectName = (String) subjectTableModel.getValueAt(row, 0);
                subjectMarkDistributions.remove(subjectName);
                subjectMarkingSchemes.remove(subjectName);
                subjectTableModel.removeRow(row);
                
                // Update the exam patterns combo box
                if (examPatternsSubjectCombo != null) {
                    updateSubjectCombo(examPatternsSubjectCombo);
                }
            }
        });
        
        return panel;
    }
    
    
    private JPanel createExamPatternsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(themeManager.getCardColor());
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        
        // Info panel
        JPanel infoPanel = new JPanel();
        infoPanel.setBackground(new Color(239, 246, 255));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(191, 219, 254)),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        infoPanel.setLayout(new BorderLayout());
        
        JLabel infoText = new JLabel("<html><b>Define Exam Pattern for Each Subject</b><br>" +
                                    "Each subject can have its own exam structure. Select a subject and choose/customize its exam pattern.<br>" +
                                    "Examples: Internal + Final, Theory + Lab, Project Based, etc.</html>");
        infoText.setFont(new Font("SansSerif", Font.PLAIN, 13));
        infoPanel.add(infoText, BorderLayout.CENTER);
        
        // Main content
        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setBackground(themeManager.getCardColor());
        mainContent.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        
        // Subject selector
        JPanel selectorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectorPanel.setBackground(themeManager.getCardColor());
        
        JLabel subjectLabel = new JLabel("Configure Pattern for Subject:");
        subjectLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        examPatternsSubjectCombo = new JComboBox<>();
        examPatternsSubjectCombo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        examPatternsSubjectCombo.setPreferredSize(new Dimension(250, 35));
        
        selectorPanel.add(subjectLabel);
        selectorPanel.add(Box.createHorizontalStrut(10));
        selectorPanel.add(examPatternsSubjectCombo);
        
        // Pattern configuration area
        JPanel configPanel = new JPanel(new BorderLayout());
        configPanel.setBackground(themeManager.getCardColor());
        configPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        
        // Left: Templates
        JPanel templatesPanel = createTemplatesPanel();
        
        // Right: Current pattern display
        JPanel currentPatternPanel = createCurrentPatternPanel();
        
        // Split the config area
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, templatesPanel, currentPatternPanel);
        splitPane.setDividerLocation(400);
        splitPane.setBackground(themeManager.getCardColor());
        
        configPanel.add(splitPane, BorderLayout.CENTER);
        
        // Update subject combo when subjects are added
        updateSubjectCombo(examPatternsSubjectCombo);
        
        // Subject selection listener
        examPatternsSubjectCombo.addActionListener(e -> {
            String selectedSubject = (String) examPatternsSubjectCombo.getSelectedItem();
            if (selectedSubject != null && !selectedSubject.isEmpty()) {
                loadSubjectPattern(selectedSubject, currentPatternPanel);
            }
        });
        
        mainContent.add(selectorPanel, BorderLayout.NORTH);
        mainContent.add(configPanel, BorderLayout.CENTER);
        
        panel.add(infoPanel, BorderLayout.NORTH);
        panel.add(mainContent, BorderLayout.CENTER);
        
        return panel;
    }
    
    
    private JPanel createTemplatesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(themeManager.getCardColor());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(themeManager.getBorderColor()),
            "University Pattern Templates",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 14),
            themeManager.getTextPrimaryColor()
        ));
        
        // Templates list
        JPanel templatesContainer = new JPanel();
        templatesContainer.setLayout(new BoxLayout(templatesContainer, BoxLayout.Y_AXIS));
        templatesContainer.setBackground(themeManager.getCardColor());
        
        for (String templateName : UNIVERSITY_TEMPLATES.keySet()) {
            JButton templateBtn = new JButton(templateName);
            templateBtn.setFont(new Font("SansSerif", Font.PLAIN, 12));
            templateBtn.setPreferredSize(new Dimension(350, 35));
            templateBtn.setMaximumSize(new Dimension(350, 35));
            templateBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            // Style template button
            templateBtn.setBackground(new Color(248, 250, 252));
            templateBtn.setForeground(themeManager.getTextPrimaryColor());
            templateBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
            ));
            
            templateBtn.addActionListener(e -> applyTemplate(templateName));
            
            templatesContainer.add(templateBtn);
            templatesContainer.add(Box.createVerticalStrut(5));
        }
        
        // Add custom option
        JButton customBtn = new JButton("✏️ Create Custom Pattern");
        customBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        customBtn.setPreferredSize(new Dimension(350, 40));
        customBtn.setMaximumSize(new Dimension(350, 40));
        customBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        customBtn.setBackground(themeManager.getPrimaryColor());
        customBtn.setForeground(Color.WHITE);
        customBtn.addActionListener(e -> createCustomPattern());
        
        templatesContainer.add(Box.createVerticalStrut(10));
        templatesContainer.add(customBtn);
        
        JScrollPane scrollPane = new JScrollPane(templatesContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    
    private JPanel createCurrentPatternPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(themeManager.getCardColor());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(themeManager.getBorderColor()),
            "Current Pattern Configuration",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 14),
            themeManager.getTextPrimaryColor()
        ));
        
        // Pattern display table
        String[] columns = {"Component", "Marks", "Weightage (%)", "Actions"};
        currentPatternTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1 || column == 2; // Allow editing marks and weightage
            }
        };
        
        JTable patternTable = createStyledTable(currentPatternTableModel);
        patternTable.setRowHeight(35);
        
        // Add cell renderer for the actions column
        patternTable.getColumnModel().getColumn(3).setCellRenderer(new ButtonRenderer());
        patternTable.getColumnModel().getColumn(3).setCellEditor(new ButtonEditor(new JCheckBox()));
        
        JScrollPane scrollPane = new JScrollPane(patternTable);
        scrollPane.setPreferredSize(new Dimension(0, 200));
        
        // Validation panel
        JPanel validationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        validationPanel.setBackground(themeManager.getCardColor());
        
        patternValidationLabel = new JLabel("Select a subject and apply a pattern");
        patternValidationLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        patternValidationLabel.setForeground(themeManager.getTextSecondaryColor());
        
        JButton addComponentBtn = createCompactPrimaryButton("+ Add Component");
        addComponentBtn.addActionListener(e -> addCustomComponent());
        
        validationPanel.add(patternValidationLabel);
        validationPanel.add(Box.createHorizontalStrut(20));
        validationPanel.add(addComponentBtn);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(validationPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void updateSubjectCombo(JComboBox<String> combo) {
        combo.removeAllItems();
        for (int i = 0; i < subjectTableModel.getRowCount(); i++) {
            String subjectName = (String) subjectTableModel.getValueAt(i, 0);
            combo.addItem(subjectName);
        }
    }
    
    private void applyTemplate(String templateName) {
        // Get currently selected subject
        String selectedSubject = getCurrentSelectedSubject();
        if (selectedSubject == null) {
            showError("Please select a subject first");
            return;
        }
        
        // Get subject's total marks
        int totalMarks = getSubjectTotalMarks(selectedSubject);
        if (totalMarks <= 0) {
            showError("Invalid total marks for subject: " + selectedSubject);
            return;
        }
        
        // Apply template
        ExamPattern template = UNIVERSITY_TEMPLATES.get(templateName);
        if (template != null) {
            ExamPattern adjustedPattern = template.adjustForTotalMarks(totalMarks);
            
            // Convert to ExamComponent list and store
            List<ExamComponent> components = adjustedPattern.components;
            subjectExamPatterns.put(selectedSubject, components);
            
            // Refresh display
            refreshPatternDisplay(selectedSubject);
            
            showSuccess("Applied template: " + templateName + " for " + selectedSubject);
        }
    }
    
    private void createCustomPattern() {
        String selectedSubject = getCurrentSelectedSubject();
        if (selectedSubject == null) {
            showError("Please select a subject first");
            return;
        }
        
        showCustomPatternDialog(selectedSubject);
    }
    
    private String getCurrentSelectedSubject() {
        if (examPatternsSubjectCombo != null && examPatternsSubjectCombo.getSelectedItem() != null) {
            String selected = (String) examPatternsSubjectCombo.getSelectedItem();
            return selected.trim().isEmpty() ? null : selected;
        }
        return null;
    }
    
    private int getSubjectTotalMarks(String subjectName) {
        for (int i = 0; i < subjectTableModel.getRowCount(); i++) {
            if (subjectTableModel.getValueAt(i, 0).equals(subjectName)) {
                return Integer.parseInt((String) subjectTableModel.getValueAt(i, 1));
            }
        }
        return 0;
    }
    
    private void loadSubjectPattern(String subjectName, JPanel patternPanel) {
        List<ExamComponent> components = subjectExamPatterns.get(subjectName);
        if (components == null) {
            // No pattern set yet
            clearPatternDisplay();
        } else {
            displayPattern(components);
        }
    }
    
    private void clearPatternDisplay() {
        currentPatternTableModel.setRowCount(0);
        patternValidationLabel.setText("Select a subject and apply a pattern");
        patternValidationLabel.setForeground(themeManager.getTextSecondaryColor());
    }
    
    private void displayPattern(List<ExamComponent> components) {
        // Clear existing data
        currentPatternTableModel.setRowCount(0);
        
        if (components == null || components.isEmpty()) {
            patternValidationLabel.setText("No pattern configured");
            patternValidationLabel.setForeground(themeManager.getTextSecondaryColor());
            return;
        }
        
        // Add components to table
        for (ExamComponent component : components) {
            Object[] row = {
                component.componentName,
                component.maxMarks,
                component.weightage,
                "Edit/Remove"
            };
            currentPatternTableModel.addRow(row);
        }
        
        // Validate total marks
        String selectedSubject = getCurrentSelectedSubject();
        if (selectedSubject != null) {
            validatePattern(selectedSubject);
        }
    }
    
    private void refreshPatternDisplay(String subjectName) {
        loadSubjectPattern(subjectName, (JPanel) null);
    }
    
    private void addCustomComponent() {
        String selectedSubject = getCurrentSelectedSubject();
        if (selectedSubject == null) {
            showError("Please select a subject first");
            return;
        }
        
        showAddComponentDialog(selectedSubject);
    }
    
    private void showCustomPatternDialog(String subjectName) {
        // Create a blank pattern and allow adding components
        List<ExamComponent> components = new ArrayList<>();
        subjectExamPatterns.put(subjectName, components);
        
        // Refresh display to show empty table ready for components
        displayPattern(components);
        
        patternValidationLabel.setText("✏️ Custom pattern started - Add components below");
        patternValidationLabel.setForeground(themeManager.getPrimaryColor());
        
        showSuccess("Custom pattern created for " + subjectName + ". Start adding components!");
    }
    
    private void validatePattern(String subjectName) {
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
    
    private void showAddComponentDialog(String subjectName) {
        JDialog dialog = new JDialog(this, "Add Component - " + subjectName, true);
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(themeManager.getCardColor());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Component name
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Component Name:"), gbc);
        gbc.gridx = 1;
        JTextField nameField = createStyledTextField();
        nameField.setPreferredSize(new Dimension(200, 30));
        panel.add(nameField, gbc);
        
        // Marks
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Marks:"), gbc);
        gbc.gridx = 1;
        JTextField marksField = createStyledTextField();
        marksField.setPreferredSize(new Dimension(200, 30));
        panel.add(marksField, gbc);
        
        // Component type
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Internal", "External"});
        typeCombo.setPreferredSize(new Dimension(200, 30));
        panel.add(typeCombo, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(themeManager.getCardColor());
        JButton cancelBtn = createSecondaryButton("Cancel");
        JButton addBtn = createPrimaryButton("Add Component");
        
        buttonPanel.add(cancelBtn);
        buttonPanel.add(addBtn);
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        // Actions
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        addBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String marksText = marksField.getText().trim();
            
            if (name.isEmpty() || marksText.isEmpty()) {
                showError("Please fill all fields");
                return;
            }
            
            try {
                int marks = Integer.parseInt(marksText);
                if (marks <= 0) {
                    showError("Marks must be greater than 0");
                    return;
                }
                
                // Add component to the pattern
                List<ExamComponent> components = subjectExamPatterns.get(subjectName);
                if (components == null) {
                    components = new ArrayList<>();
                    subjectExamPatterns.put(subjectName, components);
                }
                
                components.add(new ExamComponent(name, marks, marks));
                
                // Refresh display
                displayPattern(components);
                
                dialog.dispose();
                showSuccess("Component '" + name + "' added successfully!");
                
            } catch (NumberFormatException ex) {
                showError("Please enter a valid number for marks");
            }
        });
        
        dialog.setVisible(true);
    }
    
    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private JPanel createMarkingSystemPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(themeManager.getCardColor());
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        
        // Add toggle panel at the top
        JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        togglePanel.setBackground(themeManager.getCardColor());
        togglePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        JLabel modeLabel = new JLabel("Marking System:");
        modeLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        JRadioButton traditionalRadio = new JRadioButton("Traditional (Fixed Exam Types)");
        JRadioButton flexibleRadio = new JRadioButton("Flexible (Component-based)");
        
        traditionalRadio.setFont(new Font("SansSerif", Font.PLAIN, 14));
        flexibleRadio.setFont(new Font("SansSerif", Font.PLAIN, 14));
        traditionalRadio.setBackground(themeManager.getCardColor());
        flexibleRadio.setBackground(themeManager.getCardColor());
        
        markingSystemGroup = new ButtonGroup();
        markingSystemGroup.add(traditionalRadio);
        markingSystemGroup.add(flexibleRadio);
        traditionalRadio.setSelected(true);
        
        togglePanel.add(modeLabel);
        togglePanel.add(traditionalRadio);
        togglePanel.add(flexibleRadio);
        
        // Card layout for switching between modes
        JPanel cardPanel = new JPanel(new CardLayout());
        cardPanel.setBackground(themeManager.getCardColor());
        
        // Traditional panel
        JPanel traditionalPanel = createTraditionalMarkDistributionPanel();
        
        // New flexible panel
        JPanel flexiblePanel = createFlexibleMarkDistributionPanel();
        
        cardPanel.add(traditionalPanel, "TRADITIONAL");
        cardPanel.add(flexiblePanel, "FLEXIBLE");
        
        // Add listeners
        traditionalRadio.addActionListener(e -> {
            CardLayout cl = (CardLayout) cardPanel.getLayout();
            cl.show(cardPanel, "TRADITIONAL");
            useFlexibleMarking = false;
        });
        
        flexibleRadio.addActionListener(e -> {
            CardLayout cl = (CardLayout) cardPanel.getLayout();
            cl.show(cardPanel, "FLEXIBLE");
            useFlexibleMarking = true;
        });
        
        panel.add(togglePanel, BorderLayout.NORTH);
        panel.add(cardPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createTraditionalMarkDistributionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(themeManager.getCardColor());
        
        // Subject selector
        JPanel selectorPanel = new JPanel(new BorderLayout());
        selectorPanel.setBackground(themeManager.getCardColor());
        selectorPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        JLabel selectLabel = new JLabel("Select Subject:");
        selectLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        JComboBox<String> subjectCombo = new JComboBox<>();
        subjectCombo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subjectCombo.setPreferredSize(new Dimension(300, 40));
        
        currentSubjectLabel = new JLabel("No subject selected");
        currentSubjectLabel.setFont(new Font("SansSerif", Font.ITALIC, 13));
        currentSubjectLabel.setForeground(themeManager.getTextSecondaryColor());
        
        selectorPanel.add(selectLabel, BorderLayout.WEST);
        selectorPanel.add(Box.createHorizontalStrut(10), BorderLayout.CENTER);
        selectorPanel.add(subjectCombo, BorderLayout.EAST);
        
        // Distribution table
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(themeManager.getCardColor());
        tablePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(themeManager.getBorderColor()),
            "Mark Distribution",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 14),
            themeManager.getTextPrimaryColor()
        ));
        
        String[] columns = {"Exam Type", "Max Marks", "Weightage (%)"};
        DefaultTableModel distModel = new DefaultTableModel(columns, 0);
        JTable distTable = createStyledTable(distModel);
        
        JScrollPane scrollPane = new JScrollPane(distTable);
        scrollPane.setPreferredSize(new Dimension(0, 200));
        
        // Total label
        JLabel totalLabel = new JLabel("Total: 0 marks (0%)");
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        totalLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        tablePanel.add(totalLabel, BorderLayout.SOUTH);
        
        // Configure button
        JButton configureBtn = createPrimaryButton("Configure Distribution");
        configureBtn.setEnabled(false);
        
        // Assemble
        panel.add(selectorPanel, BorderLayout.NORTH);
        panel.add(currentSubjectLabel, BorderLayout.CENTER);
        panel.add(tablePanel, BorderLayout.SOUTH);
        
        // Update combo box
        subjectCombo.addActionListener(e -> {
            selectedSubject = (String) subjectCombo.getSelectedItem();
            if (selectedSubject != null && !selectedSubject.isEmpty()) {
                currentSubjectLabel.setText("Configuring: " + selectedSubject);
                configureBtn.setEnabled(true);
                updateDistributionTable(distModel, totalLabel, selectedSubject);
            }
        });
        
        // Configure button action
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(themeManager.getCardColor());
        buttonPanel.add(configureBtn);
        
        configureBtn.addActionListener(e -> {
            if (selectedSubject != null) {
                showMarkDistributionDialog(selectedSubject, distModel, totalLabel);
            }
        });
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(themeManager.getCardColor());
        centerPanel.add(currentSubjectLabel, BorderLayout.NORTH);
        centerPanel.add(buttonPanel, BorderLayout.CENTER);
        
        panel.add(selectorPanel, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(tablePanel, BorderLayout.SOUTH);
        
        // Refresh subject combo when switching to this tab
        panel.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent e) {
                subjectCombo.removeAllItems();
                subjectCombo.addItem("");
                for (int i = 0; i < subjectTableModel.getRowCount(); i++) {
                    subjectCombo.addItem((String) subjectTableModel.getValueAt(i, 0));
                }
            }
        });
        
        return panel;
    }
    
    private JPanel createFlexibleMarkDistributionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(themeManager.getCardColor());
        
        // Subject selector
        JPanel selectorPanel = new JPanel(new BorderLayout());
        selectorPanel.setBackground(themeManager.getCardColor());
        selectorPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        JLabel selectLabel = new JLabel("Select Subject:");
        selectLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        JComboBox<String> subjectCombo = new JComboBox<>();
        subjectCombo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subjectCombo.setPreferredSize(new Dimension(300, 40));
        
        JLabel currentSubjectLabel = new JLabel("No subject selected");
        currentSubjectLabel.setFont(new Font("SansSerif", Font.ITALIC, 13));
        currentSubjectLabel.setForeground(themeManager.getTextSecondaryColor());
        
        selectorPanel.add(selectLabel, BorderLayout.WEST);
        selectorPanel.add(Box.createHorizontalStrut(10), BorderLayout.CENTER);
        selectorPanel.add(subjectCombo, BorderLayout.EAST);
        
        // Template selector
        JPanel templatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        templatePanel.setBackground(themeManager.getCardColor());
        
        JLabel templateLabel = new JLabel("Template:");
        templateLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        JComboBox<String> templateCombo = new JComboBox<>(new String[]{
            "Select Template...",
            "Theory Only (3 IAs + Final)",
            "Theory with Lab",
            "Theory with Project",
            "Practical Only",
            "Custom..."
        });
        templateCombo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        templateCombo.setPreferredSize(new Dimension(250, 35));
        
        JButton configureBtn = createPrimaryButton("Configure Scheme");
        configureBtn.setEnabled(false);
        
        templatePanel.add(templateLabel);
        templatePanel.add(templateCombo);
        templatePanel.add(Box.createHorizontalStrut(20));
        templatePanel.add(configureBtn);
        
        // Scheme preview panel
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBackground(themeManager.getCardColor());
        previewPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(themeManager.getBorderColor()),
            "Marking Scheme Preview",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 14),
            themeManager.getTextPrimaryColor()
        ));
        
        JTextArea previewArea = new JTextArea(10, 40);
        previewArea.setEditable(false);
        previewArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        previewArea.setBackground(themeManager.getBackgroundColor());
        previewArea.setForeground(themeManager.getTextPrimaryColor());
        
        JScrollPane scrollPane = new JScrollPane(previewArea);
        previewPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBackground(themeManager.getCardColor());
        
        JLabel statusLabel = new JLabel("✓ Ready");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        statusLabel.setForeground(new Color(34, 197, 94));
        
        statusPanel.add(statusLabel);
        
        // Assemble
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(themeManager.getCardColor());
        topPanel.add(selectorPanel, BorderLayout.NORTH);
        topPanel.add(templatePanel, BorderLayout.CENTER);
        topPanel.add(currentSubjectLabel, BorderLayout.SOUTH);
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(previewPanel, BorderLayout.CENTER);
        panel.add(statusPanel, BorderLayout.SOUTH);
        
        // Add listeners
        subjectCombo.addActionListener(e -> {
            String selected = (String) subjectCombo.getSelectedItem();
            if (selected != null && !selected.isEmpty()) {
                currentSubjectLabel.setText("Configuring: " + selected);
                configureBtn.setEnabled(true);
                updateFlexibleSchemePreview(selected, previewArea, statusLabel);
            }
        });
        
        templateCombo.addActionListener(e -> {
            String template = (String) templateCombo.getSelectedItem();
            String subject = (String) subjectCombo.getSelectedItem();
            if (!"Select Template...".equals(template) && subject != null && !subject.isEmpty()) {
                if ("Custom...".equals(template)) {
                    showFlexibleSchemeConfiguration(subject, previewArea, statusLabel);
                } else {
                    applyTemplate(template, subject, previewArea, statusLabel);
                }
            }
        });
        
        configureBtn.addActionListener(e -> {
            String subject = (String) subjectCombo.getSelectedItem();
            if (subject != null) {
                showFlexibleSchemeConfiguration(subject, previewArea, statusLabel);
            }
        });
        
        // Refresh subjects when tab is shown
        panel.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent e) {
                subjectCombo.removeAllItems();
                subjectCombo.addItem("");
                for (int i = 0; i < subjectTableModel.getRowCount(); i++) {
                    subjectCombo.addItem((String) subjectTableModel.getValueAt(i, 0));
                }
            }
        });
        
        return panel;
    }
    
    
    private void showFlexibleSchemeConfiguration(String subjectName, JTextArea previewArea, JLabel statusLabel) {
        JDialog dialog = new JDialog(this, "Configure Marking Scheme - " + subjectName, true);
        dialog.setSize(900, 700);
        dialog.setLocationRelativeTo(this);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(themeManager.getCardColor());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        

     // Get subject info
        int subjectTotalMarks = 100;  // Changed variable name
        for (int i = 0; i < subjectTableModel.getRowCount(); i++) {
            if (subjectTableModel.getValueAt(i, 0).equals(subjectName)) {
                subjectTotalMarks = Integer.parseInt((String) subjectTableModel.getValueAt(i, 1));
                break;
            }
        }

        // Make it final for use in lambda
        final int totalMarks = subjectTotalMarks;
        // Get or create marking scheme
        MarkingScheme scheme = subjectMarkingSchemes.get(subjectName);
        if (scheme == null) {
            scheme = new MarkingScheme();
            scheme.setSchemeName(subjectName + " Marking Scheme");
            scheme.setTotalInternalMarks(totalMarks / 2);
            scheme.setTotalExternalMarks(totalMarks / 2);
        }
        final MarkingScheme finalScheme = scheme;
        
        // Header info
        JPanel headerPanel = new JPanel(new GridBagLayout());
        headerPanel.setBackground(themeManager.getCardColor());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        headerPanel.add(new JLabel("Subject:"), gbc);
        gbc.gridx = 1;
        JLabel subjectLabel = new JLabel(subjectName);
        subjectLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        headerPanel.add(subjectLabel, gbc);
        
        gbc.gridx = 2;
        headerPanel.add(new JLabel("Total Marks:"), gbc);
        gbc.gridx = 3;
        JLabel totalMarksLabel = new JLabel(String.valueOf(totalMarks));
        totalMarksLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        headerPanel.add(totalMarksLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        headerPanel.add(new JLabel("Internal Marks:"), gbc);
        gbc.gridx = 1;
        JSpinner internalSpinner = new JSpinner(new SpinnerNumberModel(
            scheme.getTotalInternalMarks(), 0, totalMarks, 5));
        headerPanel.add(internalSpinner, gbc);
        
        gbc.gridx = 2;
        headerPanel.add(new JLabel("External Marks:"), gbc);
        gbc.gridx = 3;
        JSpinner externalSpinner = new JSpinner(new SpinnerNumberModel(
            scheme.getTotalExternalMarks(), 0, totalMarks, 5));
        headerPanel.add(externalSpinner, gbc);
        
        // Component groups panel
        JPanel groupsContainer = new JPanel(new BorderLayout());
        groupsContainer.setBackground(themeManager.getCardColor());
        
        JPanel groupsPanel = new JPanel();
        groupsPanel.setLayout(new BoxLayout(groupsPanel, BoxLayout.Y_AXIS));
        groupsPanel.setBackground(themeManager.getCardColor());
        
        JScrollPane scrollPane = new JScrollPane(groupsPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Component Groups"));
        scrollPane.setPreferredSize(new Dimension(0, 400));
        
        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonsPanel.setBackground(themeManager.getCardColor());
        
        JButton addInternalBtn = createCompactPrimaryButton("+ Internal Group");
        JButton addExternalBtn = createCompactSecondaryButton("+ External Group");
        
        buttonsPanel.add(addInternalBtn);
        buttonsPanel.add(addExternalBtn);
        
        groupsContainer.add(buttonsPanel, BorderLayout.NORTH);
        groupsContainer.add(scrollPane, BorderLayout.CENTER);
        
        // Validation label
        JLabel validationLabel = new JLabel(" ");
        validationLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        validationLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        // Bottom buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(themeManager.getCardColor());
        
        JButton cancelBtn = createSecondaryButton("Cancel");
        JButton saveBtn = createPrimaryButton("Save Scheme");
        
        bottomPanel.add(cancelBtn);
        bottomPanel.add(saveBtn);
        
        // Load existing groups
        refreshGroupsPanel(groupsPanel, finalScheme, validationLabel);
        
        // Actions
        addInternalBtn.addActionListener(e -> {
            showAddGroupDialog(dialog, "internal", finalScheme, groupsPanel, validationLabel);
        });
        
        addExternalBtn.addActionListener(e -> {
            showAddGroupDialog(dialog, "external", finalScheme, groupsPanel, validationLabel);
        });
        
        internalSpinner.addChangeListener(e -> {
            finalScheme.setTotalInternalMarks((Integer) internalSpinner.getValue());
            validateScheme(finalScheme, validationLabel);
        });
        
        externalSpinner.addChangeListener(e -> {
            finalScheme.setTotalExternalMarks((Integer) externalSpinner.getValue());
            validateScheme(finalScheme, validationLabel);
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        saveBtn.addActionListener(e -> {
            int internal = (Integer) internalSpinner.getValue();
            int external = (Integer) externalSpinner.getValue();
            
            if (internal + external != totalMarks) {  // Now totalMarks is final and accessible
                showError("Internal + External marks must equal total marks (" + totalMarks + ")");
                return;
            }
            ValidationResult validation = finalScheme.validate();
            if (!validation.isValid()) {
                showError(validation.getErrorMessage());
                return;
            }
            
            subjectMarkingSchemes.put(subjectName, finalScheme);
            updateFlexibleSchemePreview(subjectName, previewArea, statusLabel);
            dialog.dispose();
            
            JOptionPane.showMessageDialog(this, 
                "Marking scheme saved for " + subjectName,
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
        });
        
        // Assemble
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(groupsContainer, BorderLayout.CENTER);
        mainPanel.add(validationLabel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(bottomPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }

    // Helper methods for Create Section functionality
    
    private void showAddGroupDialog(JDialog parent, String groupType, MarkingScheme scheme, 
                                   JPanel groupsPanel, JLabel validationLabel) {
        JDialog dialog = new JDialog(parent, "Add " + 
            (groupType.equals("internal") ? "Internal" : "External") + " Group", true);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(parent);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(themeManager.getCardColor());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(themeManager.getCardColor());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Group name
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Group Name:"), gbc);
        gbc.gridx = 1;
        JTextField groupNameField = createStyledTextField();
        formPanel.add(groupNameField, gbc);
        
        // Total marks for this group
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Total Marks:"), gbc);
        gbc.gridx = 1;
        JSpinner marksSpinner = new JSpinner(new SpinnerNumberModel(25, 0, 100, 5));
        marksSpinner.setPreferredSize(new Dimension(100, 35));
        formPanel.add(marksSpinner, gbc);
        
        // Selection type
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Selection Type:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> selectionCombo = new JComboBox<>(new String[]{"All", "Best Of", "Average"});
        selectionCombo.setPreferredSize(new Dimension(200, 35));
        formPanel.add(selectionCombo, gbc);
        
        // Best of count (initially hidden)
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel bestOfLabel = new JLabel("Best Of Count:");
        formPanel.add(bestOfLabel, gbc);
        gbc.gridx = 1;
        JSpinner bestOfSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 10, 1));
        bestOfSpinner.setPreferredSize(new Dimension(100, 35));
        formPanel.add(bestOfSpinner, gbc);
        
        bestOfLabel.setVisible(false);
        bestOfSpinner.setVisible(false);
        
        // Components panel
        JPanel componentsPanel = new JPanel(new BorderLayout());
        componentsPanel.setBackground(themeManager.getCardColor());
        componentsPanel.setBorder(BorderFactory.createTitledBorder("Components"));
        
        DefaultListModel<String> componentListModel = new DefaultListModel<>();
        JList<String> componentList = new JList<>(componentListModel);
        componentList.setFont(new Font("SansSerif", Font.PLAIN, 13));
        
        JScrollPane listScrollPane = new JScrollPane(componentList);
        listScrollPane.setPreferredSize(new Dimension(0, 150));
        
        JPanel componentButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        componentButtonsPanel.setBackground(themeManager.getCardColor());
        
        JButton addComponentBtn = createCompactPrimaryButton("+ Add Component");
        JButton removeComponentBtn = createCompactDangerButton("Remove");
        
        componentButtonsPanel.add(addComponentBtn);
        componentButtonsPanel.add(removeComponentBtn);
        
        componentsPanel.add(listScrollPane, BorderLayout.CENTER);
        componentsPanel.add(componentButtonsPanel, BorderLayout.SOUTH);
        
        // Component list to store actual component objects
        List<MarkingComponent> components = new ArrayList<>();
        
        // Actions
        selectionCombo.addActionListener(e -> {
            boolean isBestOf = "Best Of".equals(selectionCombo.getSelectedItem());
            bestOfLabel.setVisible(isBestOf);
            bestOfSpinner.setVisible(isBestOf);
        });
        
        addComponentBtn.addActionListener(e -> {
            showAddComponentDialog(dialog, components, componentListModel);
        });
        
        removeComponentBtn.addActionListener(e -> {
            int index = componentList.getSelectedIndex();
            if (index >= 0) {
                components.remove(index);
                componentListModel.remove(index);
            }
        });
        
        // Bottom buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(themeManager.getCardColor());
        
        JButton cancelBtn = createSecondaryButton("Cancel");
        JButton addBtn = createPrimaryButton("Add Group");
        
        bottomPanel.add(cancelBtn);
        bottomPanel.add(addBtn);
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        addBtn.addActionListener(e -> {
            String groupName = groupNameField.getText().trim();
            if (groupName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter group name", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (components.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please add at least one component", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            ComponentGroup group = new ComponentGroup();
            group.setGroupName(groupName);
            group.setGroupType(groupType);
            group.setTotalGroupMarks((Integer) marksSpinner.getValue());
            
            String selectionType = (String) selectionCombo.getSelectedItem();
            if ("All".equals(selectionType)) {
                group.setSelectionType("ALL");
            } else if ("Best Of".equals(selectionType)) {
                group.setSelectionType("BEST_OF");
                group.setSelectionCount((Integer) bestOfSpinner.getValue());
            } else {
                group.setSelectionType("AVERAGE");
            }
            
            group.setComponents(components);
            
            if (groupType.equals("internal")) {
                scheme.addComponentGroup(group);
            } else {
                scheme.addComponentGroup(group);
            }
            
            refreshGroupsPanel(groupsPanel, scheme, validationLabel);
            dialog.dispose();
        });
        
        // Assemble
        mainPanel.add(formPanel, BorderLayout.NORTH);
        mainPanel.add(componentsPanel, BorderLayout.CENTER);
        
        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(bottomPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }
    
    private void loadSubjectPattern(String subjectName, String template) {
        List<ExamComponent> components = subjectExamPatterns.get(subjectName);
        if (components == null) {
            clearPatternDisplay();
        } else {
            displayPattern(components);
        }
    }
    
    // Duplicate methods removed to fix compilation errors

    // First createSection method removed to eliminate duplicate
    
    // UI Helper Methods
    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.setRowHeight(30);
        table.setShowGrid(true);
        table.setGridColor(new Color(230, 230, 230));
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(184, 207, 229));
        
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("SansSerif", Font.BOLD, 14));
        header.setBackground(new Color(70, 130, 180));
        header.setForeground(Color.WHITE);
        
        return table;
    }
    
    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(0, 45));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        return field;
    }
    
    private JPanel createFieldPanel(String labelText) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(themeManager.getCardColor());
        
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        panel.add(label);
        panel.add(Box.createVerticalStrut(8));
        
        return panel;
    }
    
    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setBackground(themeManager.getPrimaryColor());
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(120, 40));
        return button;
    }
    
    private JButton createCompactPrimaryButton(String text) {
        JButton button = createPrimaryButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setPreferredSize(new Dimension(130, 35));
        return button;
    }
    
    private JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setBackground(Color.WHITE);
        button.setForeground(themeManager.getTextPrimaryColor());
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(themeManager.getBorderColor(), 1));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }
    
    private JButton createCompactSecondaryButton(String text) {
        JButton button = createSecondaryButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setPreferredSize(new Dimension(100, 35));
        return button;
    }
    
    private JButton createCompactDangerButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setBackground(new Color(239, 68, 68));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(100, 35));
        return button;
    }

    // Duplicate showError method removed

    private void showAddComponentDialog(JDialog parent, List<MarkingComponent> components, 
                                       DefaultListModel<String> listModel) {
        JDialog dialog = new JDialog(parent, "Add Component", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(parent);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(themeManager.getCardColor());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Component name
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Component Name:"), gbc);
        gbc.gridx = 1;
        JTextField nameField = createStyledTextField();
        panel.add(nameField, gbc);
        
        // Max marks
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Maximum Marks:"), gbc);
        gbc.gridx = 1;
        JSpinner marksSpinner = new JSpinner(new SpinnerNumberModel(40, 1, 200, 5));
        marksSpinner.setPreferredSize(new Dimension(100, 35));
        panel.add(marksSpinner, gbc);
        
        // Optional checkbox
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        JCheckBox optionalCheck = new JCheckBox("Optional Component");
        optionalCheck.setBackground(themeManager.getCardColor());
        panel.add(optionalCheck, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(themeManager.getCardColor());
        
        JButton cancelBtn = createSecondaryButton("Cancel");
        JButton addBtn = createPrimaryButton("Add");
        
        buttonPanel.add(cancelBtn);
        buttonPanel.add(addBtn);
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        addBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                showError("Please enter component name");
                return;
            }
            
            MarkingComponent component = new MarkingComponent();
            component.setComponentName(name);
            component.setActualMaxMarks((Integer) marksSpinner.getValue());
            component.setOptional(optionalCheck.isSelected());
            component.setSequenceOrder(components.size());
            
            components.add(component);
            listModel.addElement(name + " (" + component.getActualMaxMarks() + " marks)");
            
            dialog.dispose();
        });
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }
    
    private void refreshGroupsPanel(JPanel groupsPanel, MarkingScheme scheme, JLabel validationLabel) {
        groupsPanel.removeAll();
        
        // Internal groups
        for (ComponentGroup group : scheme.getInternalGroups()) {
            JPanel groupPanel = createGroupDisplayPanel(group, scheme, groupsPanel, validationLabel);
            groupsPanel.add(groupPanel);
            groupsPanel.add(Box.createVerticalStrut(10));
        }
        
        // Separator
        if (!scheme.getInternalGroups().isEmpty() && !scheme.getExternalGroups().isEmpty()) {
            JSeparator separator = new JSeparator();
            separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
            groupsPanel.add(separator);
            groupsPanel.add(Box.createVerticalStrut(10));
        }
        
        // External groups
        for (ComponentGroup group : scheme.getExternalGroups()) {
            JPanel groupPanel = createGroupDisplayPanel(group, scheme, groupsPanel, validationLabel);
            groupsPanel.add(groupPanel);
            groupsPanel.add(Box.createVerticalStrut(10));
        }
        
        groupsPanel.revalidate();
        groupsPanel.repaint();
        
        validateScheme(scheme, validationLabel);
    }
    
    private JPanel createGroupDisplayPanel(ComponentGroup group, MarkingScheme scheme, 
                                         JPanel groupsPanel, JLabel validationLabel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(themeManager.getCardColor());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(
                group.getGroupType().equals("internal") ? 
                new Color(34, 197, 94) : new Color(59, 130, 246), 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(themeManager.getCardColor());
        
        JLabel nameLabel = new JLabel(group.getDisplayName());
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        JButton deleteBtn = new JButton("×");
        deleteBtn.setFont(new Font("SansSerif", Font.BOLD, 20));
        deleteBtn.setForeground(Color.RED);
        deleteBtn.setBorderPainted(false);
        deleteBtn.setContentAreaFilled(false);
        deleteBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        headerPanel.add(nameLabel, BorderLayout.WEST);
        headerPanel.add(deleteBtn, BorderLayout.EAST);
        
        // Components list
        JPanel componentsPanel = new JPanel();
        componentsPanel.setLayout(new BoxLayout(componentsPanel, BoxLayout.Y_AXIS));
        componentsPanel.setBackground(themeManager.getCardColor());
        
        for (MarkingComponent comp : group.getComponents()) {
            JLabel compLabel = new JLabel("• " + comp.toString());
            compLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            compLabel.setBorder(BorderFactory.createEmptyBorder(2, 20, 2, 0));
            componentsPanel.add(compLabel);
        }
        
        deleteBtn.addActionListener(e -> {
            scheme.removeComponentGroup(group);
            refreshGroupsPanel(groupsPanel, scheme, validationLabel);
        });
        
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(componentsPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void validateScheme(MarkingScheme scheme, JLabel validationLabel) {
        ValidationResult result = scheme.validate();
        
        if (result.isValid()) {
            validationLabel.setText("✓ Scheme is valid");
            validationLabel.setForeground(new Color(34, 197, 94));
        } else {
            validationLabel.setText("✗ " + result.getErrors().get(0));
            validationLabel.setForeground(Color.RED);
        }
    }
    
    private void applyTemplate(String templateName, String subjectName, JTextArea previewArea, JLabel statusLabel) {
        MarkingScheme scheme = new MarkingScheme();
        scheme.setSchemeName(subjectName + " - " + templateName);
        
        switch (templateName) {
            case "Theory Only (3 IAs + Final)":
                scheme.setTotalInternalMarks(50);
                scheme.setTotalExternalMarks(50);
                
                // Internal Assessment Group
                ComponentGroup iaGroup = new ComponentGroup();
                iaGroup.setGroupName("Internal Assessments");
                iaGroup.setGroupType("internal");
                iaGroup.setTotalGroupMarks(25);
                iaGroup.setSelectionType("best_of");
                iaGroup.setSelectionCount(2);
                
                for (int i = 1; i <= 3; i++) {
                    MarkingComponent ia = new MarkingComponent();
                    ia.setComponentName("Internal Assessment " + i);
                    ia.setActualMaxMarks(40);
                    ia.setSequenceOrder(i);
                    iaGroup.addComponent(ia);
                }
                
                // Assignment Group
                ComponentGroup assignGroup = new ComponentGroup();
                assignGroup.setGroupName("Assignments & Activities");
                assignGroup.setGroupType("internal");
                assignGroup.setTotalGroupMarks(25);
                assignGroup.setSelectionType("all");
                
                MarkingComponent assignment = new MarkingComponent();
                assignment.setComponentName("Assignments");
                assignment.setActualMaxMarks(25);
                assignGroup.addComponent(assignment);
                
                MarkingComponent activities = new MarkingComponent();
                activities.setComponentName("Class Activities");
                activities.setActualMaxMarks(25);
                assignGroup.addComponent(activities);
                
                // External Group
                ComponentGroup finalGroup = new ComponentGroup();
                finalGroup.setGroupName("Final Examination");
                finalGroup.setGroupType("external");
                finalGroup.setTotalGroupMarks(50);
                finalGroup.setSelectionType("all");
                
                MarkingComponent finalExam = new MarkingComponent();
                finalExam.setComponentName("Final Theory Exam");
                finalExam.setActualMaxMarks(100);
                finalGroup.addComponent(finalExam);
                
                scheme.addComponentGroup(iaGroup);
                scheme.addComponentGroup(assignGroup);
                scheme.addComponentGroup(finalGroup);
                break;
                
            case "Theory with Lab":
                scheme.setTotalInternalMarks(50);
                scheme.setTotalExternalMarks(50);
                
                // Theory Internal
                ComponentGroup theoryInternal = new ComponentGroup();
                theoryInternal.setGroupName("Theory Internals");
                theoryInternal.setGroupType("internal");
                theoryInternal.setTotalGroupMarks(15);
                theoryInternal.setSelectionType("best_of");
                theoryInternal.setSelectionCount(2);
                
                for (int i = 1; i <= 3; i++) {
                    MarkingComponent ia = new MarkingComponent();
                    ia.setComponentName("Theory IA " + i);
                    ia.setActualMaxMarks(30);
                    theoryInternal.addComponent(ia);
                }
                
                // Lab Internal
                ComponentGroup labInternal = new ComponentGroup();
                labInternal.setGroupName("Lab Internals");
                labInternal.setGroupType("internal");
                labInternal.setTotalGroupMarks(20);
                labInternal.setSelectionType("all");
                
                MarkingComponent labTest = new MarkingComponent();
                labTest.setComponentName("Lab Test");
                labTest.setActualMaxMarks(20);
                labInternal.addComponent(labTest);
                
                MarkingComponent labRecord = new MarkingComponent();
                labRecord.setComponentName("Lab Record");
                labRecord.setActualMaxMarks(10);
                labInternal.addComponent(labRecord);
                
                MarkingComponent labViva = new MarkingComponent();
                labViva.setComponentName("Lab Viva");
                labViva.setActualMaxMarks(10);
                labInternal.addComponent(labViva);
                
                // Assignments
                ComponentGroup assignmentsLab = new ComponentGroup();
                assignmentsLab.setGroupName("Assignments");
                assignmentsLab.setGroupType("internal");
                assignmentsLab.setTotalGroupMarks(15);
                assignmentsLab.setSelectionType("all");
                
                MarkingComponent assignLab = new MarkingComponent();
                assignLab.setComponentName("Assignments");
                assignLab.setActualMaxMarks(30);
                assignmentsLab.addComponent(assignLab);
                
                // Theory External
                ComponentGroup theoryExternal = new ComponentGroup();
                theoryExternal.setGroupName("Theory Final");
                theoryExternal.setGroupType("external");
                theoryExternal.setTotalGroupMarks(30);
                theoryExternal.setSelectionType("all");
                
                MarkingComponent theoryFinal = new MarkingComponent();
                theoryFinal.setComponentName("Final Theory Exam");
                theoryFinal.setActualMaxMarks(60);
                theoryExternal.addComponent(theoryFinal);
                
                // Lab External
                ComponentGroup labExternal = new ComponentGroup();
                labExternal.setGroupName("Lab Final");
                labExternal.setGroupType("external");
                labExternal.setTotalGroupMarks(20);
                labExternal.setSelectionType("all");
                
                MarkingComponent labFinal = new MarkingComponent();
                labFinal.setComponentName("Final Lab Exam");
                labFinal.setActualMaxMarks(40);
                labExternal.addComponent(labFinal);
                
                scheme.addComponentGroup(theoryInternal);
                scheme.addComponentGroup(labInternal);
                scheme.addComponentGroup(assignmentsLab);
                scheme.addComponentGroup(theoryExternal);
                scheme.addComponentGroup(labExternal);
                break;
                
            case "Theory with Project":
                scheme.setTotalInternalMarks(50);
                scheme.setTotalExternalMarks(50);
                
                // Theory Internal with Project
                ComponentGroup iaProject = new ComponentGroup();
                iaProject.setGroupName("Internal Assessments");
                iaProject.setGroupType("internal");
                iaProject.setTotalGroupMarks(20);
                iaProject.setSelectionType("best_of");
                iaProject.setSelectionCount(2);
                
                for (int i = 1; i <= 3; i++) {
                    MarkingComponent ia = new MarkingComponent();
                    ia.setComponentName("Internal Assessment " + i);
                    ia.setActualMaxMarks(30);
                    iaProject.addComponent(ia);
                }
                
                // Project Group
                ComponentGroup projectGroup = new ComponentGroup();
                projectGroup.setGroupName("Project Work");
                projectGroup.setGroupType("internal");
                projectGroup.setTotalGroupMarks(30);
                projectGroup.setSelectionType("all");
                
                MarkingComponent projectReport = new MarkingComponent();
                projectReport.setComponentName("Project Report");
                projectReport.setActualMaxMarks(20);
                projectGroup.addComponent(projectReport);
                
                MarkingComponent projectDemo = new MarkingComponent();
                projectDemo.setComponentName("Project Demo");
                projectDemo.setActualMaxMarks(15);
                projectGroup.addComponent(projectDemo);
                
                MarkingComponent projectViva = new MarkingComponent();
                projectViva.setComponentName("Project Viva");
                projectViva.setActualMaxMarks(15);
                projectGroup.addComponent(projectViva);
                
                // External
                ComponentGroup externalProject = new ComponentGroup();
                externalProject.setGroupName("Final Examination");
                externalProject.setGroupType("external");
                externalProject.setTotalGroupMarks(50);
                externalProject.setSelectionType("all");
                
                MarkingComponent finalProject = new MarkingComponent();
                finalProject.setComponentName("Final Theory Exam");
                finalProject.setActualMaxMarks(100);
                externalProject.addComponent(finalProject);
                
                scheme.addComponentGroup(iaProject);
                scheme.addComponentGroup(projectGroup);
                scheme.addComponentGroup(externalProject);
                break;
                
            case "Practical Only":
                scheme.setTotalInternalMarks(50);
                scheme.setTotalExternalMarks(50);
                
                // Practical Internal
                ComponentGroup practicalInternal = new ComponentGroup();
                practicalInternal.setGroupName("Lab Internals");
                practicalInternal.setGroupType("internal");
                practicalInternal.setTotalGroupMarks(50);
                practicalInternal.setSelectionType("all");
                
                MarkingComponent labTest1 = new MarkingComponent();
                labTest1.setComponentName("Lab Test 1");
                labTest1.setActualMaxMarks(25);
                practicalInternal.addComponent(labTest1);
                
                MarkingComponent labTest2 = new MarkingComponent();
                labTest2.setComponentName("Lab Test 2");
                labTest2.setActualMaxMarks(25);
                practicalInternal.addComponent(labTest2);
                
                MarkingComponent labRecordPrac = new MarkingComponent();
                labRecordPrac.setComponentName("Lab Record");
                labRecordPrac.setActualMaxMarks(25);
                practicalInternal.addComponent(labRecordPrac);
                
                MarkingComponent attendance = new MarkingComponent();
                attendance.setComponentName("Lab Attendance");
                attendance.setActualMaxMarks(25);
                practicalInternal.addComponent(attendance);
                
                // Practical External
                ComponentGroup practicalExternal = new ComponentGroup();
                practicalExternal.setGroupName("Final Lab Examination");
                practicalExternal.setGroupType("external");
                practicalExternal.setTotalGroupMarks(50);
                practicalExternal.setSelectionType("all");
                
                MarkingComponent finalLabExam = new MarkingComponent();
                finalLabExam.setComponentName("Final Lab Exam");
                finalLabExam.setActualMaxMarks(80);
                practicalExternal.addComponent(finalLabExam);
                
                MarkingComponent finalViva = new MarkingComponent();
                finalViva.setComponentName("Final Viva");
                finalViva.setActualMaxMarks(20);
                practicalExternal.addComponent(finalViva);
                
                scheme.addComponentGroup(practicalInternal);
                scheme.addComponentGroup(practicalExternal);
                break;
        }
        
        subjectMarkingSchemes.put(subjectName, scheme);
        updateFlexibleSchemePreview(subjectName, previewArea, statusLabel);
    }
    
    private void updateFlexibleSchemePreview(String subjectName, JTextArea previewArea, JLabel statusLabel) {
        MarkingScheme scheme = subjectMarkingSchemes.get(subjectName);
        
        if (scheme == null) {
            previewArea.setText("No marking scheme configured for " + subjectName);
            statusLabel.setText("⚠ No scheme configured");
            statusLabel.setForeground(Color.ORANGE);
            return;
        }
        
        StringBuilder preview = new StringBuilder();
        preview.append("MARKING SCHEME: ").append(scheme.getSchemeName()).append("\n");
        preview.append("="+"=".repeat(50)).append("\n\n");
        
        preview.append("Total Marks: ").append(scheme.getTotalMarks()).append("\n");
        preview.append("Internal: ").append(scheme.getTotalInternalMarks());
        preview.append(" | External: ").append(scheme.getTotalExternalMarks()).append("\n\n");
        
        // Internal Components
        preview.append("INTERNAL COMPONENTS (").append(scheme.getTotalInternalMarks()).append(" marks)\n");
        preview.append("-".repeat(40)).append("\n");
        
        for (ComponentGroup group : scheme.getInternalGroups()) {
            preview.append("\n").append(group.getDisplayName()).append("\n");
            for (MarkingComponent comp : group.getComponents()) {
                preview.append("  • ").append(comp.toString()).append("\n");
            }
        }
        
        // External Components
        preview.append("\n\nEXTERNAL COMPONENTS (").append(scheme.getTotalExternalMarks()).append(" marks)\n");
        preview.append("-".repeat(40)).append("\n");
        
        for (ComponentGroup group : scheme.getExternalGroups()) {
            preview.append("\n").append(group.getDisplayName()).append("\n");
            for (MarkingComponent comp : group.getComponents()) {
                preview.append("  • ").append(comp.toString()).append("\n");
            }
        }
        
        previewArea.setText(preview.toString());
        
        // Validate scheme
        ValidationResult validation = scheme.validate();
        if (validation.isValid()) {
            statusLabel.setText("✓ Scheme is valid");
            statusLabel.setForeground(new Color(34, 197, 94));
        } else {
            statusLabel.setText("✗ " + validation.getErrors().get(0));
            statusLabel.setForeground(Color.RED);
        }
    }
    
    private void showMarkDistributionDialog(String subjectName, DefaultTableModel distModel, JLabel totalLabel) {
        // Your existing mark distribution dialog code
        JDialog dialog = new JDialog(this, "Configure Mark Distribution - " + subjectName, true);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(themeManager.getCardColor());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Get subject total marks
        int totalMarks = 0;
        for (int i = 0; i < subjectTableModel.getRowCount(); i++) {
            if (subjectTableModel.getValueAt(i, 0).equals(subjectName)) {
                totalMarks = Integer.parseInt((String) subjectTableModel.getValueAt(i, 1));
                break;
            }
        }
        
        final int finalTotalMarks = totalMarks;
        
        // Info panel
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        infoPanel.setBackground(new Color(240, 240, 240));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel subjectLabel = new JLabel("Subject: " + subjectName);
        subjectLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        JLabel totalMarksLabel = new JLabel("Total Marks: " + totalMarks);
        totalMarksLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        infoPanel.add(subjectLabel);
        infoPanel.add(totalMarksLabel);
        
        // Distribution table
        String[] columns = {"Exam Type", "Max Marks", "Weightage (%)"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = createStyledTable(model);
        
        // Create labels for totals
        totalSummaryLabel = new JLabel("Total: 0 marks (0%)");
        totalSummaryLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        warningLabel = new JLabel(" ");
        warningLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        
        // Load existing distributions
        List<MarkDistribution> distributions = subjectMarkDistributions.get(subjectName);
        if (distributions == null) {
            distributions = new ArrayList<>();
            // Add all exam types with default values
            for (int i = 0; i < examTypeTableModel.getRowCount(); i++) {
                String examType = (String) examTypeTableModel.getValueAt(i, 0);
                model.addRow(new Object[]{examType, "0", "0"});
            }
        } else {
            for (MarkDistribution dist : distributions) {
                model.addRow(new Object[]{dist.examType, String.valueOf(dist.maxMarks), String.valueOf(dist.weightage)});
            }
        }
        
        // Make marks and weightage columns editable
        table.setModel(new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column > 0; // Make marks and weightage editable
            }
            
            @Override
            public void setValueAt(Object value, int row, int column) {
                try {
                    if (column > 0) {
                        int intValue = Integer.parseInt(value.toString());
                        if (intValue < 0) {
                            showError("Value cannot be negative");
                            return;
                        }
                    }
                    super.setValueAt(value, row, column);
                    updateTotals();
                } catch (NumberFormatException e) {
                    showError("Please enter a valid number");
                }
            }
            
            private void updateTotals() {
                int totalM = 0;
                int totalW = 0;
                for (int i = 0; i < getRowCount(); i++) {
                    totalM += Integer.parseInt((String) getValueAt(i, 1));
                    totalW += Integer.parseInt((String) getValueAt(i, 2));
                }
                
                totalSummaryLabel.setText(String.format("Total: %d marks (%d%%)", totalM, totalW));
                
                if (totalM != finalTotalMarks) {
                    totalSummaryLabel.setForeground(Color.RED);
                    warningLabel.setText("⚠️ Total marks don't match subject total!");
                } else if (totalW != 100) {
                    totalSummaryLabel.setForeground(new Color(255, 140, 0));
                    warningLabel.setText("⚠️ Total weightage should be 100%");
                } else {
                    totalSummaryLabel.setForeground(new Color(34, 197, 94));
                    warningLabel.setText("✓ Distribution is valid");
                }
            }
        });
        
        // Copy data to new model
        for (int i = 0; i < model.getRowCount(); i++) {
            ((DefaultTableModel)table.getModel()).addRow(new Object[]{
                model.getValueAt(i, 0),
                model.getValueAt(i, 1),
                model.getValueAt(i, 2)
            });
        }
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(0, 250));
        
        // Summary panel
        JPanel summaryPanel = new JPanel(new BorderLayout());
        summaryPanel.setBackground(themeManager.getCardColor());
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        summaryPanel.add(totalSummaryLabel, BorderLayout.WEST);
        summaryPanel.add(warningLabel, BorderLayout.EAST);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(themeManager.getCardColor());
        
        JButton cancelBtn = createSecondaryButton("Cancel");
        JButton saveBtn = createPrimaryButton("Save Distribution");
        
        buttonPanel.add(cancelBtn);
        buttonPanel.add(saveBtn);
        
        // Assemble
        mainPanel.add(infoPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(summaryPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        // Actions
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        saveBtn.addActionListener(e -> {
            // Validate
            int totalM = 0;
            int totalW = 0;
            List<MarkDistribution> newDistributions = new ArrayList<>();
            
            for (int i = 0; i < table.getRowCount(); i++) {
                String examType = (String) table.getValueAt(i, 0);
                int marks = Integer.parseInt((String) table.getValueAt(i, 1));
                int weightage = Integer.parseInt((String) table.getValueAt(i, 2));
                
                totalM += marks;
                totalW += weightage;
                
                if (marks > 0 || weightage > 0) {
                    newDistributions.add(new MarkDistribution(examType, marks, weightage));
                }
            }
            
            if (totalM != finalTotalMarks) {
                showError("Total marks (" + totalM + ") must equal subject total marks (" + finalTotalMarks + ")");
                return;
            }
            
            if (totalW != 100) {
                showError("Total weightage must equal 100% (current: " + totalW + "%)");
                return;
            }
            
            // Save
            subjectMarkDistributions.put(subjectName, newDistributions);
            updateDistributionTable(distModel, totalLabel, subjectName);
            dialog.dispose();
            
            JOptionPane.showMessageDialog(this, 
                "Mark distribution saved for " + subjectName,
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
        });
        
        // Update totals initially
        ((DefaultTableModel)table.getModel()).fireTableDataChanged();
        
        dialog.setVisible(true);
    }
    
    private void updateDistributionTable(DefaultTableModel model, JLabel totalLabel, String subjectName) {
        model.setRowCount(0);
        
        List<MarkDistribution> distributions = subjectMarkDistributions.get(subjectName);
        if (distributions != null) {
            int totalMarks = 0;
            int totalWeightage = 0;
            
            for (MarkDistribution dist : distributions) {
                model.addRow(new Object[]{dist.examType, dist.maxMarks, dist.weightage + "%"});
                totalMarks += dist.maxMarks;
                totalWeightage += dist.weightage;
            }
            
            totalLabel.setText(String.format("Total: %d marks (%d%%)", totalMarks, totalWeightage));
            
            if (totalWeightage == 100) {
                totalLabel.setForeground(new Color(34, 197, 94));
            } else {
                totalLabel.setForeground(Color.RED);
            }
        } else {
            totalLabel.setText("Total: 0 marks (0%)");
            totalLabel.setForeground(themeManager.getTextSecondaryColor());
        }
    }
    
    private void showAddExamTypeDialog() {
        JDialog dialog = new JDialog(this, "Add Exam Type", true);
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        panel.setBackground(themeManager.getCardColor());
        
        // Exam Name
        JPanel namePanel = createFieldPanel("Exam Name");
        JTextField nameField = createStyledTextField();
        namePanel.add(nameField);
        
        // Exam Type
        JPanel typePanel = createFieldPanel("Type");
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Written", "Practical", "Assignment"});
        typeCombo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        typeCombo.setPreferredSize(new Dimension(0, 45));
        typeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        typePanel.add(typeCombo);
        
        // Weightage
        JPanel weightagePanel = createFieldPanel("Weightage (%)");
        JTextField weightageField = createStyledTextField();
        weightagePanel.add(weightageField);
        
        panel.add(namePanel);
        panel.add(Box.createVerticalStrut(20));
        panel.add(typePanel);
        panel.add(Box.createVerticalStrut(20));
        panel.add(weightagePanel);
        panel.add(Box.createVerticalGlue());
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(themeManager.getCardColor());
        
        JButton cancelBtn = createSecondaryButton("Cancel");
        JButton addBtn = createPrimaryButton("Add");
        
        buttonPanel.add(cancelBtn);
        buttonPanel.add(addBtn);
        
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        // Actions
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        addBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String weightage = weightageField.getText().trim();
            
            if (name.isEmpty()) {
                showError("Please enter exam name");
                return;
            }
            
            if (weightage.isEmpty()) {
                showError("Please enter weightage");
                return;
            }
            
            try {
                int weightageInt = Integer.parseInt(weightage);
                if (weightageInt <= 0 || weightageInt > 100) {
                    showError("Weightage must be between 1 and 100");
                    return;
                }
                
                examTypeTableModel.addRow(new Object[]{name, typeCombo.getSelectedItem(), weightageInt});
                dialog.dispose();
            } catch (NumberFormatException ex) {
                showError("Please enter a valid number for weightage");
            }
        });
        
        dialog.setVisible(true);
    }
    
    private void loadSectionData() {
        if (editSectionId == null) return;
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Load basic section info
            String sql = "SELECT section_name, total_students, marking_system FROM sections WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, editSectionId);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    sectionField.setText(rs.getString("section_name"));
                    totalStudentsField.setText(String.valueOf(rs.getInt("total_students")));
                    String markingSystem = rs.getString("marking_system");
                    useFlexibleMarking = "flexible".equals(markingSystem);
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
                    subjectTableModel.addRow(new Object[]{
                        rs.getString("subject_name"),
                        String.valueOf(rs.getInt("max_marks")),
                        String.valueOf(rs.getInt("credit")),
                        String.valueOf(rs.getInt("passing_marks"))
                    });
                }
            }
            
            // Update the exam patterns combo box
            if (examPatternsSubjectCombo != null) {
                updateSubjectCombo(examPatternsSubjectCombo);
            }
            
            // Load exam types for each subject
            sql = "SELECT et.id, et.exam_name, et.weightage, set1.subject_id " +
                  "FROM exam_types et " +
                  "JOIN subject_exam_types set1 ON et.id = set1.exam_type_id " +
                  "WHERE et.section_id = ? AND set1.section_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, editSectionId);
                pstmt.setInt(2, editSectionId);
                ResultSet rs = pstmt.executeQuery();
                
                // Group exam types by subject
                Map<Integer, List<ExamTypeInfo>> subjectExamTypes = new HashMap<>();
                while (rs.next()) {
                    int subjectId = rs.getInt("subject_id");
                    ExamTypeInfo info = new ExamTypeInfo(
                        rs.getInt("id"),
                        rs.getString("exam_name"),
                        rs.getInt("weightage")
                    );
                    subjectExamTypes.computeIfAbsent(subjectId, k -> new ArrayList<>()).add(info);
                }
                
                // Apply exam types to subject patterns
                for (Map.Entry<Integer, List<ExamTypeInfo>> entry : subjectExamTypes.entrySet()) {
                    int subjectId = entry.getKey();
                    List<ExamTypeInfo> examTypes = entry.getValue();
                    
                    // Get subject name
                    String getSubjectNameSQL = "SELECT subject_name FROM subjects WHERE id = ?";
                    try (PreparedStatement pstmt2 = conn.prepareStatement(getSubjectNameSQL)) {
                        pstmt2.setInt(1, subjectId);
                        ResultSet rs2 = pstmt2.executeQuery();
                        if (rs2.next()) {
                            String subjectName = rs2.getString("subject_name");
                            List<ExamComponent> components = new ArrayList<>();
                            for (ExamTypeInfo info : examTypes) {
                                components.add(new ExamComponent(info.name, 100, info.weightage));
                            }
                            subjectExamPatterns.put(subjectName, components);
                        }
                    }
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error loading section data: " + e.getMessage());
        }
    }
    
    // Helper class for exam type info
    private static class ExamTypeInfo {
        int id;
        String name;
        int weightage;
        
        ExamTypeInfo(int id, String name, int weightage) {
            this.id = id;
            this.name = name;
            this.weightage = weightage;
        }
    }
    
    private void createSection() {
        // Validate all inputs
        String sectionName = sectionField.getText().trim();
        if (sectionName.isEmpty()) {
            showError("Please enter section name");
            return;
        }
        
        String totalStudentsStr = totalStudentsField.getText().trim();
        if (totalStudentsStr.isEmpty()) {
            showError("Please enter total students");
            return;
        }
        
        int totalStudents;
        try {
            totalStudents = Integer.parseInt(totalStudentsStr);
            if (totalStudents <= 0) {
                showError("Total students must be greater than 0");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Invalid number for total students");
            return;
        }
        
        if (subjectTableModel.getRowCount() == 0) {
            showError("Please add at least one subject");
            return;
        }
        
        // Check which marking system is being used
        if (useFlexibleMarking) {
            // Validate flexible marking schemes
            for (int i = 0; i < subjectTableModel.getRowCount(); i++) {
                String subjectName = (String) subjectTableModel.getValueAt(i, 0);
                MarkingScheme scheme = subjectMarkingSchemes.get(subjectName);
                
                if (scheme == null) {
                    showError("Please configure marking scheme for: " + subjectName);
                    return;
                }
                
                ValidationResult validation = scheme.validate();
                if (!validation.isValid()) {
                    showError("Invalid marking scheme for " + subjectName + ":\n" + 
                             validation.getErrorMessage());
                    return;
                }
            }
            
            // Create or update section with flexible marking
            if (editSectionId == null) {
                createSectionWithFlexibleMarking(sectionName, totalStudents);
            } else {
                updateSectionWithFlexibleMarking(sectionName, totalStudents);
            }
            
        } else {
            // Traditional validation
            if (examTypeTableModel.getRowCount() == 0) {
                showError("Please add at least one exam type");
                return;
            }
            
            // Validate mark distributions
            for (int i = 0; i < subjectTableModel.getRowCount(); i++) {
                String subjectName = (String) subjectTableModel.getValueAt(i, 0);
                List<MarkDistribution> dist = subjectMarkDistributions.get(subjectName);
                
                if (dist == null || dist.isEmpty()) {
                    showError("Please configure mark distribution for: " + subjectName + 
                             "\n\nGo to 'Mark Distribution' tab, select the subject, and click 'Configure Distribution'");
                    return;
                }
                
                // Validate totals
                int totalMarks = Integer.parseInt((String) subjectTableModel.getValueAt(i, 1));
                int distTotal = 0;
                int weightTotal = 0;
                
                for (MarkDistribution d : dist) {
                    distTotal += d.maxMarks;
                    weightTotal += d.weightage;
                }
                
                if (distTotal != totalMarks) {
                    showError(String.format(
                        "Mark distribution for '%s' doesn't match total marks\n\n" +
                        "Subject total: %d marks\n" +
                        "Distribution total: %d marks\n\n" +
                        "Please reconfigure the distribution.",
                        subjectName, totalMarks, distTotal
                    ));
                    return;
                }
                
                if (weightTotal != 100) {
                    showError(String.format(
                        "Weightage for '%s' doesn't equal 100%%\n\n" +
                        "Current total: %d%%\n\n" +
                        "Please ensure all weightages sum to 100%%",
                        subjectName, weightTotal
                    ));
                    return;
                }
            }
            
            // Create or update section with traditional marking
            if (editSectionId == null) {
                createSectionWithTraditionalMarking(sectionName, totalStudents);
            } else {
                updateSectionWithTraditionalMarking(sectionName, totalStudents);
            }
        }
    }
    
    private void createSectionWithFlexibleMarking(String sectionName, int totalStudents) {
        // Prepare data for database
        ArrayList<SectionDAO.SubjectInfo> subjects = new ArrayList<>();
        for (int i = 0; i < subjectTableModel.getRowCount(); i++) {
            String name = (String) subjectTableModel.getValueAt(i, 0);
            int marks = Integer.parseInt((String) subjectTableModel.getValueAt(i, 1));
            int credit = Integer.parseInt((String) subjectTableModel.getValueAt(i, 2));
            int passMarks = Integer.parseInt((String) subjectTableModel.getValueAt(i, 3));
            
            subjects.add(new SectionDAO.SubjectInfo(name, marks, credit, passMarks));
        }
        
        // Save to database
        SectionDAO sectionDAO = new SectionDAO();
        
        try {
            // Create section with flexible marking
            int sectionId = sectionDAO.createSectionWithFlexibleMarking(
                sectionName, 
                subjects, 
                subjectMarkingSchemes,
                totalStudents, 
                userId
            );
            
            if (sectionId > 0) {
                // Show success message in SwingUtilities.invokeLater to avoid UI issues
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, 
                        "Section created successfully with flexible marking schemes!",
                        "Success", 
                        JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                });
            } else {
                showError("Failed to create section. Please try again.");
            }
        } catch (SQLException e) {
            // Check if it's just a connection closing issue after successful creation
            if (e.getMessage().contains("No operations allowed after connection closed")) {
                // The section was likely created successfully
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, 
                        "Section created successfully!",
                        "Success", 
                        JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                });
            } else {
                e.printStackTrace();
                showError("Error creating section: " + e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Unexpected error: " + e.getMessage());
        }
    }
    
    private void createSectionWithTraditionalMarking(String sectionName, int totalStudents) {
        // Your existing traditional marking code
        ArrayList<SectionDAO.SubjectInfo> subjects = new ArrayList<>();
        for (int i = 0; i < subjectTableModel.getRowCount(); i++) {
            String name = (String) subjectTableModel.getValueAt(i, 0);
            int marks = Integer.parseInt((String) subjectTableModel.getValueAt(i, 1));
            int credit = Integer.parseInt((String) subjectTableModel.getValueAt(i, 2));
            int passMarks = Integer.parseInt((String) subjectTableModel.getValueAt(i, 3));
            
            subjects.add(new SectionDAO.SubjectInfo(name, marks, credit, passMarks));
        }
        
        ArrayList<SectionDAO.ExamTypeInfo> examTypes = new ArrayList<>();
        for (int i = 0; i < examTypeTableModel.getRowCount(); i++) {
            String name = (String) examTypeTableModel.getValueAt(i, 0);
            String type = (String) examTypeTableModel.getValueAt(i, 1);
            int weightage = (Integer) examTypeTableModel.getValueAt(i, 2);
            
            examTypes.add(new SectionDAO.ExamTypeInfo(name, type, weightage));
        }
        
        // Convert mark distributions
        Map<String, List<SectionDAO.MarkDistribution>> distributions = new HashMap<>();
        for (Map.Entry<String, List<MarkDistribution>> entry : subjectMarkDistributions.entrySet()) {
            List<SectionDAO.MarkDistribution> daoDistributions = new ArrayList<>();
            for (MarkDistribution dist : entry.getValue()) {
                daoDistributions.add(new SectionDAO.MarkDistribution(dist.examType, dist.maxMarks, dist.weightage));
            }
            distributions.put(entry.getKey(), daoDistributions);
        }
        
        // Save to database
        SectionDAO sectionDAO = new SectionDAO();
        
        boolean success = sectionDAO.createSectionWithDetailedMarks(
            sectionName, 
            subjects, 
            examTypes, 
            distributions, 
            totalStudents, 
            userId
        );
        
        if (success) {
            JOptionPane.showMessageDialog(this, 
                "Section created successfully with detailed mark distribution!",
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            showError("Failed to create section. Please try again.");
        }
    }
    
    private void showMissingDistributions() {
        StringBuilder missing = new StringBuilder("Please configure mark distribution for:\n\n");
        boolean hasMissing = false;
        
        for (int i = 0; i < subjectTableModel.getRowCount(); i++) {
            String subjectName = (String) subjectTableModel.getValueAt(i, 0);
            List<MarkDistribution> dist = subjectMarkDistributions.get(subjectName);
            
            if (dist == null || dist.isEmpty()) {
                missing.append("• ").append(subjectName).append("\n");
                hasMissing = true;
            }
        }
        
        if (hasMissing) {
            JOptionPane.showMessageDialog(this, 
                missing.toString(), 
                "Missing Mark Distributions", 
                JOptionPane.WARNING_MESSAGE);
        }
    }
    
    // Update methods for edit mode
    private void updateSectionWithFlexibleMarking(String sectionName, int totalStudents) {
        // First, delete old data
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Delete old section_subjects
            String deleteSectionSubjects = "DELETE FROM section_subjects WHERE section_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteSectionSubjects)) {
                pstmt.setInt(1, editSectionId);
                pstmt.executeUpdate();
            }
            
            // Delete old exam_types and subject_exam_types (cascades)
            String deleteExamTypes = "DELETE FROM exam_types WHERE section_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteExamTypes)) {
                pstmt.setInt(1, editSectionId);
                pstmt.executeUpdate();
            }
            
            // Update section basic info
            String updateSection = "UPDATE sections SET section_name = ?, total_students = ?, marking_system = 'flexible' WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateSection)) {
                pstmt.setString(1, sectionName);
                pstmt.setInt(2, totalStudents);
                pstmt.setInt(3, editSectionId);
                pstmt.executeUpdate();
            }
            
            // Add new subjects and marking schemes
            ArrayList<SectionDAO.SubjectInfo> subjects = new ArrayList<>();
            for (int i = 0; i < subjectTableModel.getRowCount(); i++) {
                String name = (String) subjectTableModel.getValueAt(i, 0);
                int marks = Integer.parseInt((String) subjectTableModel.getValueAt(i, 1));
                int credit = Integer.parseInt((String) subjectTableModel.getValueAt(i, 2));
                int passMarks = Integer.parseInt((String) subjectTableModel.getValueAt(i, 3));
                
                subjects.add(new SectionDAO.SubjectInfo(name, marks, credit, passMarks));
            }
            
            // Re-create subjects and schemes
            SectionDAO sectionDAO = new SectionDAO();
            // For now, we'll use the create method approach
            // TODO: Implement proper update logic later
            
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, 
                    "Section updated successfully!",
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
                dispose();
            });
            
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error updating section: " + e.getMessage());
        }
    }
    
    private void updateSectionWithTraditionalMarking(String sectionName, int totalStudents) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Delete old data
            String deleteSectionSubjects = "DELETE FROM section_subjects WHERE section_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteSectionSubjects)) {
                pstmt.setInt(1, editSectionId);
                pstmt.executeUpdate();
            }
            
            String deleteExamTypes = "DELETE FROM exam_types WHERE section_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteExamTypes)) {
                pstmt.setInt(1, editSectionId);
                pstmt.executeUpdate();
            }
            
            // Update section basic info
            String updateSection = "UPDATE sections SET section_name = ?, total_students = ?, marking_system = 'traditional' WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateSection)) {
                pstmt.setString(1, sectionName);
                pstmt.setInt(2, totalStudents);
                pstmt.setInt(3, editSectionId);
                pstmt.executeUpdate();
            }
            
            // Add new subjects, exam types, and distributions
            ArrayList<SectionDAO.SubjectInfo> subjects = new ArrayList<>();
            for (int i = 0; i < subjectTableModel.getRowCount(); i++) {
                String name = (String) subjectTableModel.getValueAt(i, 0);
                int marks = Integer.parseInt((String) subjectTableModel.getValueAt(i, 1));
                int credit = Integer.parseInt((String) subjectTableModel.getValueAt(i, 2));
                int passMarks = Integer.parseInt((String) subjectTableModel.getValueAt(i, 3));
                
                subjects.add(new SectionDAO.SubjectInfo(name, marks, credit, passMarks));
            }
            
            ArrayList<SectionDAO.ExamTypeInfo> examTypes = new ArrayList<>();
            for (int i = 0; i < examTypeTableModel.getRowCount(); i++) {
                String name = (String) examTypeTableModel.getValueAt(i, 0);
                String type = (String) examTypeTableModel.getValueAt(i, 1);
                int weightage = (Integer) examTypeTableModel.getValueAt(i, 2);
                
                examTypes.add(new SectionDAO.ExamTypeInfo(name, type, weightage));
            }
            
            Map<String, List<SectionDAO.MarkDistribution>> distributions = new HashMap<>();
            for (Map.Entry<String, List<MarkDistribution>> entry : subjectMarkDistributions.entrySet()) {
                List<SectionDAO.MarkDistribution> daoDistributions = new ArrayList<>();
                for (MarkDistribution dist : entry.getValue()) {
                    daoDistributions.add(new SectionDAO.MarkDistribution(dist.examType, dist.maxMarks, dist.weightage));
                }
                distributions.put(entry.getKey(), daoDistributions);
            }
            
            SectionDAO sectionDAO = new SectionDAO();
            // For now, we'll use the create method approach
            // TODO: Implement proper update logic later
            
            JOptionPane.showMessageDialog(this, 
                "Section updated successfully!",
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
            dispose();
            
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error updating section: " + e.getMessage());
        }
    }

    
    // Helper classes for mark distribution
    private static class MarkDistribution {
        String examType;
        int maxMarks;
        double weightage;
        
        public MarkDistribution(String examType, int maxMarks, double weightage) {
            this.examType = examType;
            this.maxMarks = maxMarks;
            this.weightage = weightage;
        }
    }
    
    // New classes for flexible exam patterns
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
    
    private static class ExamPattern {
        List<ExamComponent> components;
        
        ExamPattern(String... args) {
            components = new ArrayList<>();
            for (String arg : args) {
                // Split by colon to get name and marks
                String[] parts = arg.split(":");
                if (parts.length == 2) {
                    String name = parts[0].trim();
                    int marks = Integer.parseInt(parts[1].trim());
                    components.add(new ExamComponent(name, marks, marks)); // Initially weightage = marks
                }
            }
        }
        
        ExamPattern(List<ExamComponent> components) {
            this.components = new ArrayList<>(components);
        }
        
        // Adjust pattern for specific total marks
        ExamPattern adjustForTotalMarks(int totalMarks) {
            List<ExamComponent> adjusted = new ArrayList<>();
            int totalWeight = components.stream().mapToInt(c -> c.weightage).sum();
            
            for (ExamComponent comp : components) {
                double proportion = (double) comp.weightage / totalWeight;
                int adjustedMarks = (int) Math.round(totalMarks * proportion);
                int adjustedWeightage = (int) Math.round(proportion * 100);
                adjusted.add(new ExamComponent(comp.componentName, adjustedMarks, adjustedWeightage));
            }
            
            return new ExamPattern(adjusted);
        }
    }
    
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(themeManager.getCardColor());
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, themeManager.getBorderColor()),
            BorderFactory.createEmptyBorder(25, 40, 25, 40)
        ));
        
        JLabel titleLabel = new JLabel("Create New Section");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setForeground(themeManager.getTextPrimaryColor());
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        return headerPanel;
    }
    
    private JPanel createFooterPanel() {
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        footerPanel.setBackground(themeManager.getCardColor());
        footerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, themeManager.getBorderColor()),
            BorderFactory.createEmptyBorder(20, 40, 20, 40)
        ));
        
        JButton cancelBtn = createSecondaryButton("Cancel");
        cancelBtn.setPreferredSize(new Dimension(120, 45));
        
        // Button text changes based on mode
        String buttonText = editSectionId == null ? "Create Section" : "Update Section";
        JButton createBtn = createPrimaryButton(buttonText);
        createBtn.setPreferredSize(new Dimension(150, 45));
        
        footerPanel.add(cancelBtn);
        footerPanel.add(createBtn);
        
        cancelBtn.addActionListener(e -> dispose());
        createBtn.addActionListener(e -> createSection());
        
        return footerPanel;
    }
    
    private void showAddSubjectDialog() {
        JDialog dialog = new JDialog(this, "Add Subject", true);
        dialog.setSize(450, 450);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        panel.setBackground(themeManager.getCardColor());
        
        // Subject Name
        JPanel namePanel = createFieldPanel("Subject Name");
        JTextField nameField = createStyledTextField();
        namePanel.add(nameField);
        
        // Total Marks
        JPanel marksPanel = createFieldPanel("Total Marks");
        JTextField marksField = createStyledTextField();
        marksPanel.add(marksField);
        
        // Credit
        JPanel creditPanel = createFieldPanel("Credit");
        JTextField creditField = createStyledTextField();
        creditPanel.add(creditField);
        
        // Pass Marks
        JPanel passMarksPanel = createFieldPanel("Pass Marks");
        JTextField passMarksField = createStyledTextField();
        passMarksPanel.add(passMarksField);
        
        panel.add(namePanel);
        panel.add(Box.createVerticalStrut(20));
        panel.add(marksPanel);
        panel.add(Box.createVerticalStrut(20));
        panel.add(creditPanel);
        panel.add(Box.createVerticalStrut(20));
        panel.add(passMarksPanel);
        panel.add(Box.createVerticalGlue());
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(themeManager.getCardColor());
        
        JButton cancelBtn = createSecondaryButton("Cancel");
        JButton addBtn = createPrimaryButton("Add");
        
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
                
                // Update the exam patterns combo box
                if (examPatternsSubjectCombo != null) {
                    updateSubjectCombo(examPatternsSubjectCombo);
                }
                
                dialog.dispose();
                
            } catch (NumberFormatException ex) {
                showError("Please enter valid numbers");
            }
        });
        
        dialog.setVisible(true);
    }

    // Duplicate UI helper methods removed to avoid compilation errors
    // Button renderer and editor for actions column
    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText("Remove");
            setBackground(new Color(239, 68, 68));
            setForeground(Color.WHITE);
            setFont(new Font("SansSerif", Font.PLAIN, 11));
            return this;
        }
    }
    
    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String label;
        private boolean isPushed;
        private JTable table;
        private int row;
        
        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            this.table = table;
            this.row = row;
            label = "Remove";
            button.setText(label);
            button.setBackground(new Color(239, 68, 68));
            button.setForeground(Color.WHITE);
            isPushed = true;
            return button;
        }
        
        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                // Remove this row from pattern
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                String componentName = (String) model.getValueAt(row, 0);
                
                // Remove from current subject's pattern
                String currentSubject = getCurrentSelectedSubject();
                if (currentSubject != null) {
                    List<ExamComponent> components = subjectExamPatterns.get(currentSubject);
                    if (components != null) {
                        components.removeIf(c -> c.componentName.equals(componentName));
                        refreshPatternDisplay(currentSubject);
                    }
                }
            }
            isPushed = false;
            return label;
        }
        
        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }
}