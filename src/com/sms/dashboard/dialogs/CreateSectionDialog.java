package com.sms.dashboard.dialogs;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import com.sms.theme.ThemeManager;
import com.sms.dao.SectionDAO;
import com.sms.marking.models.*;
import com.sms.marking.dao.*;

public class CreateSectionDialog extends JDialog {
    
    private JTextField sectionField;
    private JTextField totalStudentsField;
    private DefaultTableModel subjectTableModel;
    private DefaultTableModel examTypeTableModel;
    private JTable subjectTable;
    private JTable examTypeTable;
    private ThemeManager themeManager;
    private Map<String, List<MarkDistribution>> subjectMarkDistributions;
    private JLabel currentSubjectLabel;
    private String selectedSubject = null;
    private JLabel totalSummaryLabel;
    private JLabel warningLabel;
    private int userId;
    
    // New fields for flexible marking system
    private Map<String, MarkingScheme> subjectMarkingSchemes;
    private boolean useFlexibleMarking = false;
    private ButtonGroup markingSystemGroup;

    public CreateSectionDialog(JFrame parent, int userId) {
        super(parent, "Create Section", true);
        this.userId = userId;
        themeManager = ThemeManager.getInstance();
        subjectMarkDistributions = new HashMap<>();
        subjectMarkingSchemes = new HashMap<>();
        
        setSize(900, 750);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(themeManager.getCardColor());
        
        // Header
        JPanel headerPanel = createHeaderPanel();
        mainContainer.add(headerPanel, BorderLayout.NORTH);
        
        // Content with tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        // Tab 1: Basic Info
        JPanel basicInfoPanel = createBasicInfoPanel();
        tabbedPane.addTab("Basic Information", basicInfoPanel);
        
        // Tab 2: Subjects
        JPanel subjectsPanel = createSubjectsPanel();
        tabbedPane.addTab("Subjects", subjectsPanel);
        
        // Tab 3: Exam Structure
        JPanel examStructurePanel = createExamStructurePanel();
        tabbedPane.addTab("Exam Structure", examStructurePanel);
        
        // Tab 4: Mark Distribution
        JPanel markDistributionPanel = createMarkDistributionPanel();
        tabbedPane.addTab("Mark Distribution", markDistributionPanel);
        
        mainContainer.add(tabbedPane, BorderLayout.CENTER);
        
        // Footer
        JPanel footerPanel = createFooterPanel();
        mainContainer.add(footerPanel, BorderLayout.SOUTH);
        
        add(mainContainer);
        getContentPane().setBackground(themeManager.getCardColor());
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
            }
        });
        
        return panel;
    }
    
    private JPanel createExamStructurePanel() {
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
        
        JLabel infoIcon = new JLabel("ℹ️");
        infoIcon.setFont(new Font("SansSerif", Font.PLAIN, 20));
        
        JLabel infoText = new JLabel("<html>Define the exam types that will be conducted in this section.<br>" +
                                    "Examples: Internal Exam 1, Internal Exam 2, Final Exam, Assignment, Project, etc.</html>");
        infoText.setFont(new Font("SansSerif", Font.PLAIN, 13));
        
        infoPanel.add(infoIcon, BorderLayout.WEST);
        infoPanel.add(Box.createHorizontalStrut(10), BorderLayout.CENTER);
        infoPanel.add(infoText, BorderLayout.EAST);
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(themeManager.getCardColor());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        
        JLabel titleLabel = new JLabel("Exam Types");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(themeManager.getTextPrimaryColor());
        
        JButton addButton = createCompactPrimaryButton("+ Add Exam Type");
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(addButton, BorderLayout.EAST);
        
        // Table
        String[] columns = {"Exam Name", "Type", "Weightage (%)"};
        examTypeTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        examTypeTable = createStyledTable(examTypeTableModel);
        
        JScrollPane scrollPane = new JScrollPane(examTypeTable);
        scrollPane.setPreferredSize(new Dimension(0, 250));
        
        // Actions
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionPanel.setBackground(themeManager.getCardColor());
        
        JButton removeBtn = createCompactDangerButton("Remove");
        actionPanel.add(removeBtn);
        
        // Assemble
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(themeManager.getCardColor());
        contentPanel.add(infoPanel, BorderLayout.NORTH);
        contentPanel.add(headerPanel, BorderLayout.CENTER);
        
        panel.add(contentPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(actionPanel, BorderLayout.SOUTH);
        
        // Listeners
        addButton.addActionListener(e -> showAddExamTypeDialog());
        
        removeBtn.addActionListener(e -> {
            int row = examTypeTable.getSelectedRow();
            if (row != -1) {
                examTypeTableModel.removeRow(row);
            }
        });
        
        return panel;
    }
    
    private JPanel createMarkDistributionPanel() {
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
                showError("Please enter group name");
                return;
            }
            
            if (components.isEmpty()) {
                showError("Please add at least one component");
                return;
            }
            
            ComponentGroup group = new ComponentGroup();
            group.setGroupName(groupName);
            group.setGroupType(groupType);
            group.setTotalGroupMarks((Integer) marksSpinner.getValue());
            
            String selectionType = (String) selectionCombo.getSelectedItem();
            if ("All".equals(selectionType)) {
                group.setSelectionType("all");
            } else if ("Best Of".equals(selectionType)) {
                group.setSelectionType("best_of");
                group.setSelectionCount((Integer) bestOfSpinner.getValue());
            } else {
                group.setSelectionType("average_of");
            }
            
            for (MarkingComponent comp : components) {
                group.addComponent(comp);
            }
            
            scheme.addComponentGroup(group);
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
            
            // Create section with flexible marking
            createSectionWithFlexibleMarking(sectionName, totalStudents);
            
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
            
            // Create section with traditional marking
            createSectionWithTraditionalMarking(sectionName, totalStudents);
        }
    }
    
 // In CreateSectionDialog.java, update the createSectionWithFlexibleMarking method:

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
    
    // Helper class for mark distribution
    private static class MarkDistribution {
        String examType;
        int maxMarks;
        int weightage;
        
        MarkDistribution(String examType, int maxMarks, int weightage) {
            this.examType = examType;
            this.maxMarks = maxMarks;
            this.weightage = weightage;
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
        
        JButton createBtn = createPrimaryButton("Create Section");
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
                dialog.dispose();
                
            } catch (NumberFormatException ex) {
                showError("Please enter valid numbers");
            }
        });
        
        dialog.setVisible(true);
    }
    
    // UI Helper methods
    private JPanel createFieldPanel(String labelText) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(themeManager.getCardColor());
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        label.setForeground(themeManager.getTextSecondaryColor());
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        
        panel.add(label, BorderLayout.NORTH);
        
        return panel;
    }
    
    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(0, 45));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        field.setBackground(themeManager.isDarkMode() ? 
            themeManager.getBackgroundColor() : Color.WHITE);
        field.setForeground(themeManager.getTextPrimaryColor());
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(themeManager.getBorderColor(), 1, true),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        return field;
    }
    
    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(40);
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setBackground(themeManager.getCardColor());
        table.setForeground(themeManager.getTextPrimaryColor());
        table.setSelectionBackground(new Color(99, 102, 241, 50));
        table.setSelectionForeground(themeManager.getTextPrimaryColor());
        table.setGridColor(themeManager.getBorderColor());
        table.setShowVerticalLines(false);
        
        // Style header
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("SansSerif", Font.BOLD, 14));
        header.setBackground(themeManager.getCardColor());
        header.setForeground(themeManager.getTextPrimaryColor());
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, themeManager.getBorderColor()));
        
        return table;
    }
    
    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setBackground(themeManager.getPrimaryColor());
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
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
        button.setBackground(themeManager.getBackgroundColor());
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
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}