package com.sms.dashboard.dialogs;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.AbstractCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
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
 * 
 * WEIGHTED CALCULATION SYSTEM (Current Implementation):
 * ======================================================
 * This panel allows creating sections with subjects using a weighted grading system.
 * 
 * KEY PRINCIPLES:
 * - Each exam component has a weightage % (contribution to subject total of 100)
 * - Marks entered DIRECTLY out of weightage (no scaling: 20% = enter 0-20)
 * - Subject Total = Σ(all component marks) out of 100
 * - All weightages MUST sum to exactly 100%
 * 
 * DUAL PASSING REQUIREMENT:
 * - Component Pass: Score >= component passing marks (e.g., 8 out of 20)
 * - Subject Pass: Total >= subject passing marks (typically 40 out of 100)
 * - Student FAILS if ANY component is failed (even with passing total)
 * 
 * EXAMPLE (CLOUD COMPUTING):
 * - Internal 1: 20%, Pass 8  → Enter 0-20 (fail if < 8)
 * - Internal 2: 25%, Pass 10 → Enter 0-25 (fail if < 10)
 * - Internal 3: 15%, Pass 6  → Enter 0-15 (fail if < 6)
 * - Final Exam: 40%, Pass 16 → Enter 0-40 (fail if < 16)
 * - Total: 100%, typically need 40+ to pass subject
 * 
 * @see com.sms.dao.AnalyzerDAO#calculateWeightedSubjectTotal
 * @see WEIGHTED_CALCULATION_SYSTEM.md for full documentation
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
        subjectTableModel = new DefaultTableModel(new String[]{"Subject Name", "Total Marks", "Credit", "Pass Marks", "Actions"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4; // Only Actions column is editable
            }
        };
        subjectTable = new JTable(subjectTableModel);
        
        subjectNameField = new JTextField(15);
        subjectMarksField = new JTextField(10);
        
        // Initialize exam pattern components
        examPatternsSubjectCombo = new JComboBox<>();
        patternValidationLabel = new JLabel("Select a subject to configure exam patterns");
        
        currentPatternTableModel = new DefaultTableModel(
            new String[]{"Component", "Max Marks", "Weightage (%)", "Passing Marks", "Actions"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4; // Only Actions column is editable
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
        
        // Set row height and column widths
        subjectTable.setRowHeight(35);
        System.out.println("[DEBUG] Table has " + subjectTable.getColumnCount() + " columns");
        
        if (subjectTable.getColumnCount() >= 5) {
            subjectTable.getColumnModel().getColumn(0).setPreferredWidth(250); // Subject Name
            subjectTable.getColumnModel().getColumn(1).setPreferredWidth(120); // Total Marks
            subjectTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // Credit
            subjectTable.getColumnModel().getColumn(3).setPreferredWidth(120); // Pass Marks
            subjectTable.getColumnModel().getColumn(4).setPreferredWidth(200); // Actions - wider for buttons
            subjectTable.getColumnModel().getColumn(4).setMinWidth(200);
            
            // Add cell renderer and editor for the actions column
            subjectTable.getColumnModel().getColumn(4).setCellRenderer(new SubjectButtonRenderer());
            subjectTable.getColumnModel().getColumn(4).setCellEditor(new SubjectButtonEditor());
        }
        
        // Disable auto resize to allow horizontal scrolling
        subjectTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane scrollPane = new JScrollPane(subjectTable);
        scrollPane.setPreferredSize(new Dimension(0, 200));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Context menu on right-click: Edit / Remove
        JPopupMenu subjectMenu = new JPopupMenu();
        JMenuItem editItem = new JMenuItem("Edit Subject");
        JMenuItem removeItem = new JMenuItem("Remove Subject");
        subjectMenu.add(editItem);
        subjectMenu.add(removeItem);
        // Attach menu to table
        subjectTable.setComponentPopupMenu(subjectMenu);
        // Ensure right-click selects the row under cursor
        subjectTable.addMouseListener(new java.awt.event.MouseAdapter() {
            private void selectRow(java.awt.event.MouseEvent e) {
                int row = subjectTable.rowAtPoint(e.getPoint());
                if (row != -1) {
                    subjectTable.setRowSelectionInterval(row, row);
                }
            }
            
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                System.out.println("[DEBUG] Mouse pressed - isPopupTrigger: " + e.isPopupTrigger());
                if (e.isPopupTrigger()) selectRow(e);
            }
            
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                System.out.println("[DEBUG] Mouse released - isPopupTrigger: " + e.isPopupTrigger());
                if (e.isPopupTrigger()) selectRow(e);
            }
        });
        // Hook menu actions
        editItem.addActionListener(e -> {
            int row = subjectTable.getSelectedRow();
            if (row != -1) {
                showEditSubjectDialog(row);
            } else {
                showError("Please select a subject to edit");
            }
        });
        
        removeItem.addActionListener(e -> {
            int row = subjectTable.getSelectedRow();
            if (row != -1) {
                removeSubjectAtRow(row);
            } else {
                showError("Please select a subject to remove");
            }
        });
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
        // Set up table with custom renderer and editor for Actions column
        currentPatternTable.setRowHeight(35);
        
        if (currentPatternTable.getColumnCount() >= 5) {
            currentPatternTable.getColumnModel().getColumn(0).setPreferredWidth(250); // Component
            currentPatternTable.getColumnModel().getColumn(1).setPreferredWidth(120); // Max Marks
            currentPatternTable.getColumnModel().getColumn(2).setPreferredWidth(120); // Weightage
            currentPatternTable.getColumnModel().getColumn(3).setPreferredWidth(120); // Passing Marks
            currentPatternTable.getColumnModel().getColumn(4).setPreferredWidth(200); // Actions - wider for buttons
            currentPatternTable.getColumnModel().getColumn(4).setMinWidth(200);
            
            // Add cell renderer and editor for the actions column
            currentPatternTable.getColumnModel().getColumn(4).setCellRenderer(new ComponentButtonRenderer());
            currentPatternTable.getColumnModel().getColumn(4).setCellEditor(new ComponentButtonEditor());
        }
        
        // Disable auto-resize for horizontal scrolling
        currentPatternTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        // Pattern table
        JScrollPane scrollPane = new JScrollPane(currentPatternTable);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(0, 200));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Add right-click context menu
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem editMenuItem = new JMenuItem("Edit Component");
        JMenuItem deleteMenuItem = new JMenuItem("Delete Component");
        
        editMenuItem.addActionListener(e -> {
            int row = currentPatternTable.getSelectedRow();
            if (row >= 0) {
                showEditComponentDialog(row);
            }
        });
        
        deleteMenuItem.addActionListener(e -> {
            int row = currentPatternTable.getSelectedRow();
            if (row >= 0) {
                removeComponentAtRow(row);
            }
        });
        
        contextMenu.add(editMenuItem);
        contextMenu.add(deleteMenuItem);
        
        currentPatternTable.setComponentPopupMenu(contextMenu);
        
        currentPatternTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = currentPatternTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        currentPatternTable.setRowSelectionInterval(row, row);
                    }
                }
            }
        });
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
        if (editSectionId == null) {
            System.out.println("[DEBUG] editSectionId is null - skipping load (CREATE mode)");
            return;
        }
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
            sql = "SELECT s.id, s.subject_name, ss.max_marks, ss.credit, ss.passing_marks " +
                  "FROM subjects s " +
                  "JOIN section_subjects ss ON s.id = ss.subject_id " +
                  "WHERE ss.section_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, editSectionId);
                ResultSet rs = pstmt.executeQuery();
                
                int subjectCount = 0;
                while (rs.next()) {
                    subjectCount++;
                    int subjectId = rs.getInt("id");
                    String subjectName = rs.getString("subject_name");
                    System.out.println("[DEBUG] Loading subject #" + subjectCount + ": " + subjectName + " (ID: " + subjectId + ")");
                    
                    subjectTableModel.addRow(new Object[]{
                        subjectName,
                        String.valueOf(rs.getInt("max_marks")),
                        String.valueOf(rs.getInt("credit")),
                        String.valueOf(rs.getInt("passing_marks")),
                        "Edit/Remove"
                    });
                    // Also add to exam patterns combo
                    examPatternsSubjectCombo.addItem(subjectName);
                    
                    // Load exam components for this subject
                    try {
                        loadExamComponentsForSubject(conn, subjectId, subjectName);
                    } catch (SQLException e) {
                        System.err.println("[ERROR] Failed to load components for subject: " + subjectName);
                        e.printStackTrace();
                    }
                }
                System.out.println("[DEBUG] subjectExamPatterns map now has " + subjectExamPatterns.size() + " entries");
            }
            
            // Update save button text
            if (saveButton != null) {
                saveButton.setText("Update Section");
            }
            
            System.out.println("[DEBUG] Finished loadSectionDataForEdit - Final map size: " + subjectExamPatterns.size());
            System.out.println("[DEBUG] Map keys: " + subjectExamPatterns.keySet());
            
        } catch (SQLException e) {
            System.err.println("[ERROR] SQL Exception in loadSectionDataForEdit");
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error loading section data: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadExamComponentsForSubject(Connection conn, int subjectId, String subjectName) throws SQLException {
        System.out.println("[DEBUG] Loading exam components for subject: " + subjectName + " (ID: " + subjectId + ")");
        
        // Load from subject_exam_types table which stores the configuration
        // This ensures we see exam types even before marks are entered
        // Try with max_marks first (new schema), fallback to weightage only (old schema)
        String sql = "SELECT DISTINCT et.id, et.exam_name, et.weightage, et.passing_marks " +
                     "FROM exam_types et " +
                     "INNER JOIN subject_exam_types sext ON et.id = sext.exam_type_id " +
                     "WHERE sext.section_id = ? AND sext.subject_id = ? " +
                     "ORDER BY et.id";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, editSectionId);
            pstmt.setInt(2, subjectId);
            ResultSet rs = pstmt.executeQuery();
            
            List<ExamComponent> components = new ArrayList<>();
            int componentCount = 0;
            
            // Check if max_marks column exists in the database table (not ResultSet)
            boolean hasMaxMarksColumn = false;
            try {
                DatabaseMetaData dbMetaData = conn.getMetaData();
                ResultSet columns = dbMetaData.getColumns(null, null, "exam_types", "max_marks");
                hasMaxMarksColumn = columns.next();
                columns.close();
            } catch (SQLException e) {
                System.out.println("[DEBUG] Could not check for max_marks column: " + e.getMessage());
            }
            
            while (rs.next()) {
                componentCount++;
                String componentName = rs.getString("exam_name");
                int weightage = rs.getInt("weightage");
                int passingMarks = rs.getInt("passing_marks");
                
                // Get max_marks from database if column exists
                int maxMarks = weightage;  // Default: backward compatibility (Option A)
                if (hasMaxMarksColumn) {
                    try {
                        // Re-query with max_marks for this specific exam
                        String maxMarksQuery = "SELECT et.max_marks FROM exam_types et " +
                                             "INNER JOIN subject_exam_types sext ON et.id = sext.exam_type_id " +
                                             "WHERE sext.section_id = ? AND sext.subject_id = ? AND et.exam_name = ?";
                        try (PreparedStatement ps2 = conn.prepareStatement(maxMarksQuery)) {
                            ps2.setInt(1, editSectionId);
                            ps2.setInt(2, subjectId);
                            ps2.setString(3, componentName);
                            try (ResultSet rs2 = ps2.executeQuery()) {
                                if (rs2.next()) {
                                    int dbMaxMarks = rs2.getInt("max_marks");
                                    if (dbMaxMarks > 0) {
                                        maxMarks = dbMaxMarks;
                                    } else {
                                    }
                                } else {
                                }
                            }
                        }
                    } catch (SQLException e) {
                        System.out.println("[DEBUG] ❌ Error fetching max_marks for " + componentName + ": " + e.getMessage());
                    }
                } else {
                }
                
                // Create component with max_marks and weightage
                ExamComponent component = new ExamComponent(componentName, maxMarks, weightage, passingMarks);
                components.add(component);
                System.out.println("[DEBUG] Loaded component #" + componentCount + ": " + componentName + 
                                 " (Max: " + maxMarks + ", Weightage: " + weightage + "%, Passing: " + passingMarks + ")");
            }
            if (!components.isEmpty()) {
                subjectExamPatterns.put(subjectName, components);
                System.out.println("[DEBUG] ✓ Stored " + components.size() + " components for subject: " + subjectName);
            } else {
                System.out.println("[DEBUG] ✗ No components found for subject: " + subjectName + " (section: " + editSectionId + ")");
            }
        }
    }
    
    private void setupEventHandlers() {
        // Subject selection change handler
        examPatternsSubjectCombo.addActionListener(e -> {
            String selectedSubject = getCurrentSelectedSubject();
            if (selectedSubject != null) {
                displayPatternForSubject(selectedSubject);
                validatePattern(selectedSubject);
            } else {
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
                
                subjectTableModel.addRow(new Object[]{name, marks, credit, passMarks, "Edit/Remove"});
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
        
        // In weighted system, total marks is always 100 (sum of weightages)
        // Templates define weightage percentages, not absolute marks
        List<ExamComponent> components = new ArrayList<>();
        
        switch (templateName) {
            case "3 Internal + Final (100M)":
                // Weightages are fixed percentages that sum to 100%
                components.add(new ExamComponent("Internal 1", 20, 20)); // 20% weightage
                components.add(new ExamComponent("Internal 2", 25, 25)); // 25% weightage
                components.add(new ExamComponent("Internal 3", 15, 15)); // 15% weightage
                components.add(new ExamComponent("Final Exam", 40, 40)); // 40% weightage
                break;
                
            case "2 Internal + Final (100M)":
                components.add(new ExamComponent("Internal 1", 25, 25)); // 25% weightage
                components.add(new ExamComponent("Internal 2", 25, 25)); // 25% weightage
                components.add(new ExamComponent("Final Exam", 50, 50)); // 50% weightage
                break;
                
            case "Theory + Lab (100M)":
                components.add(new ExamComponent("Theory Internal", 20, 20)); // 20% weightage
                components.add(new ExamComponent("Theory Final", 50, 50));    // 50% weightage
                components.add(new ExamComponent("Lab Internal", 10, 10));    // 10% weightage
                components.add(new ExamComponent("Lab Final", 20, 20));       // 20% weightage
                break;
                
            case "Practical Only (100M)":
                components.add(new ExamComponent("Lab Work", 40, 40));         // 40% weightage
                components.add(new ExamComponent("Practical Exam", 60, 60));   // 60% weightage
                break;
        }
        
        System.out.println("[DEBUG] Applying template '" + templateName + "' with " + components.size() + " components");
        subjectExamPatterns.put(selectedSubject, components);
        System.out.println("[DEBUG] Map now contains " + subjectExamPatterns.size() + " subjects with patterns");
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
        System.out.println("[DEBUG] Map contains keys: " + subjectExamPatterns.keySet());
        List<ExamComponent> components = subjectExamPatterns.get(subjectName);
        System.out.println("[DEBUG] Retrieved " + (components == null ? "null" : components.size()) + " components");
        displayPattern(components);
    }
    
    private void displayPattern(List<ExamComponent> components) {
        if (currentPatternTableModel == null) {
            return;
        }
        
        currentPatternTableModel.setRowCount(0);
        if (components == null || components.isEmpty()) {
            System.out.println("[DEBUG] No components to display (null or empty)");
            return;
        }
        
        System.out.println("[DEBUG] Adding " + components.size() + " components to table");
        for (ExamComponent component : components) {
            Object[] row = {
                component.componentName,
                component.maxMarks,
                component.weightage,
                component.passingMarks,
                "Edit/Remove"
            };
            currentPatternTableModel.addRow(row);
        }
        System.out.println("[DEBUG] Table now has " + currentPatternTableModel.getRowCount() + " rows");
        
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
        
        // Sum weightages (should equal 100 for weighted system)
        int totalWeightage = components.stream().mapToInt(c -> c.weightage).sum();
        
        if (totalWeightage == 100) {
            patternValidationLabel.setText("✅ Pattern valid (" + totalWeightage + "% total weightage)");
            patternValidationLabel.setForeground(new Color(34, 197, 94)); // green
        } else {
            patternValidationLabel.setText("❌ Invalid total: " + totalWeightage + "% (must equal 100%)");
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
        dialog.setSize(450, 380);
        dialog.setLocationRelativeTo(this);
        
        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Component name
        gbc.gridx = 0; gbc.gridy = 0;
        content.add(new JLabel("Component Name:"), gbc);
        gbc.gridx = 1;
        JTextField nameField = new JTextField(15);
        styleTextField(nameField);
        content.add(nameField, gbc);
        
        // Max Marks
        gbc.gridx = 0; gbc.gridy = 1;
        content.add(new JLabel("Max Marks:"), gbc);
        gbc.gridx = 1;
        JTextField marksField = new JTextField(10);
        styleTextField(marksField);
        content.add(marksField, gbc);
        
        // Weightage
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel weightageLabel = new JLabel("<html>Weightage (%):<br><small>Contribution to subject total</small></html>");
        content.add(weightageLabel, gbc);
        gbc.gridx = 1;
        JTextField weightageField = new JTextField(10);
        styleTextField(weightageField);
        content.add(weightageField, gbc);
        
        // Passing Marks
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel passingLabel = new JLabel("<html>Passing Marks:<br><small>Minimum marks to pass</small></html>");
        content.add(passingLabel, gbc);
        gbc.gridx = 1;
        JTextField passingField = new JTextField(10);
        styleTextField(passingField);
        content.add(passingField, gbc);
        
        // Buttons
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addBtn = createStyledButton("Add", SUCCESS_COLOR);
        JButton cancelBtn = createStyledButton("Cancel", ERROR_COLOR);
        
        addBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String marksText = marksField.getText().trim();
            String weightageText = weightageField.getText().trim();
            String passingText = passingField.getText().trim();
            
            if (name.isEmpty() || marksText.isEmpty() || weightageText.isEmpty() || passingText.isEmpty()) {
                showError("Please fill in all fields");
                return;
            }
            
            try {
                int marks = Integer.parseInt(marksText);
                int weightage = Integer.parseInt(weightageText);
                int passing = Integer.parseInt(passingText);
                
                if (marks <= 0 || weightage <= 0 || passing < 0) {
                    showError("Marks and weightage must be positive");
                    return;
                }
                
                if (passing > marks) {
                    showError("Passing marks cannot exceed max marks");
                    return;
                }
                
                if (weightage > 100) {
                    showError("Weightage cannot exceed 100%");
                    return;
                }
                
                List<ExamComponent> components = subjectExamPatterns.get(subjectName);
                if (components == null) {
                    components = new ArrayList<>();
                    subjectExamPatterns.put(subjectName, components);
                }
                
                components.add(new ExamComponent(name, marks, weightage, passing));
                displayPattern(components);
                validatePattern(subjectName);
                
                dialog.dispose();
                showSuccess("Component added successfully");
                
            } catch (NumberFormatException ex) {
                showError("Please enter valid numbers");
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
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                try {
                    conn.close(); // CRITICAL: Return connection to pool!
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
                        // Check if max_marks column exists (backward compatibility)
                        DatabaseMetaData metaData = conn.getMetaData();
                        ResultSet columns = metaData.getColumns(null, null, "exam_types", "max_marks");
                        boolean hasMaxMarksColumn = columns.next();
                        columns.close();
                        
                        // Insert exam types and link them to subject
                        for (ExamComponent component : components) {
                            String insertExamType;
                            if (hasMaxMarksColumn) {
                                // Option B: Scaled system with separate max_marks and weightage
                                insertExamType = "INSERT INTO exam_types (section_id, exam_name, max_marks, weightage, passing_marks, created_by) VALUES (?, ?, ?, ?, ?, ?)";
                            } else {
                                // Option A: Direct entry system (weightage = max_marks)
                                insertExamType = "INSERT INTO exam_types (section_id, exam_name, weightage, passing_marks, created_by) VALUES (?, ?, ?, ?, ?)";
                            }
                            
                            try (PreparedStatement ps = conn.prepareStatement(insertExamType, PreparedStatement.RETURN_GENERATED_KEYS)) {
                                ps.setInt(1, sectionId);
                                ps.setString(2, component.componentName);
                                
                                if (hasMaxMarksColumn) {
                                    ps.setInt(3, component.maxMarks);   // Exam paper max marks
                                    ps.setInt(4, component.weightage);  // Contribution to 100
                                    ps.setInt(5, component.passingMarks);
                                    ps.setInt(6, userId);
                                } else {
                                    ps.setInt(3, component.weightage);  // Use weightage only
                                    ps.setInt(4, component.passingMarks);
                                    ps.setInt(5, userId);
                                }
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
    
    // Method to show edit subject dialog with pre-filled values
    private void showEditSubjectDialog(int row) {
        String subjectName = (String) subjectTableModel.getValueAt(row, 0);
        String totalMarksStr = subjectTableModel.getValueAt(row, 1).toString();
        String creditStr = subjectTableModel.getValueAt(row, 2).toString();
        String passMarksStr = subjectTableModel.getValueAt(row, 3).toString();
        
        // Create dialog
        JDialog editDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Subject", true);
        editDialog.setLayout(new BorderLayout(10, 10));
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        formPanel.setBackground(themeManager.getCardColor());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Subject Name (pre-filled)
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel subjectLabel = new JLabel("Subject Name:");
        formPanel.add(subjectLabel, gbc);
        
        gbc.gridx = 1;
        JTextField subjectField = new JTextField(subjectName, 20);
        formPanel.add(subjectField, gbc);
        
        // Total Marks (pre-filled)
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel marksLabel = new JLabel("Total Marks:");
        formPanel.add(marksLabel, gbc);
        
        gbc.gridx = 1;
        JTextField marksField = new JTextField(totalMarksStr, 20);
        formPanel.add(marksField, gbc);
        
        // Credit (pre-filled)
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel creditLabel = new JLabel("Credit:");
        formPanel.add(creditLabel, gbc);
        
        gbc.gridx = 1;
        JTextField creditField = new JTextField(creditStr, 20);
        formPanel.add(creditField, gbc);
        
        // Pass Marks (pre-filled)
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel passLabel = new JLabel("Pass Marks:");
        formPanel.add(passLabel, gbc);
        
        gbc.gridx = 1;
        JTextField passField = new JTextField(passMarksStr, 20);
        formPanel.add(passField, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(themeManager.getCardColor());
        
        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");
        
        saveBtn.addActionListener(e -> {
            String newName = subjectField.getText().trim();
            if (newName.isEmpty()) {
                JOptionPane.showMessageDialog(editDialog, "Subject name cannot be empty!");
                return;
            }
            
            try {
                String newTotalMarks = marksField.getText().trim();
                String newCredit = creditField.getText().trim();
                String newPassMarks = passField.getText().trim();
                
                // Update table model
                subjectTableModel.setValueAt(newName, row, 0);
                subjectTableModel.setValueAt(newTotalMarks, row, 1);
                subjectTableModel.setValueAt(newCredit, row, 2);
                subjectTableModel.setValueAt(newPassMarks, row, 3);
                editDialog.dispose();
                JOptionPane.showMessageDialog(this, "Subject updated successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(editDialog, "Please enter valid data!");
            }
        });
        
        cancelBtn.addActionListener(e -> editDialog.dispose());
        
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        
        editDialog.add(formPanel, BorderLayout.CENTER);
        editDialog.add(buttonPanel, BorderLayout.SOUTH);
        editDialog.pack();
        editDialog.setLocationRelativeTo(this);
        editDialog.setVisible(true);
    }
    
    // Method to remove subject from table
    private void removeSubjectAtRow(int row) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to remove this subject?",
                "Confirm Removal",
                JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            String subjectName = (String) subjectTableModel.getValueAt(row, 0);
            
            // Remove from table
            subjectTableModel.removeRow(row);
            
            // Remove from subject combo
            examPatternsSubjectCombo.removeItem(subjectName);
            JOptionPane.showMessageDialog(this, "Subject removed successfully!");
        }
    }
    
    // Method to show edit component dialog with pre-filled values
    private void showEditComponentDialog(int row) {
        String subjectName = getCurrentSelectedSubject();
        if (subjectName == null) {
            showError("Please select a subject first");
            return;
        }
        String componentName = (String) currentPatternTableModel.getValueAt(row, 0);
        String maxMarksStr = currentPatternTableModel.getValueAt(row, 1).toString();
        String weightageStr = currentPatternTableModel.getValueAt(row, 2).toString();
        
        // Get passing marks from table or in-memory structure
        int passingMarks = 0;
        if (currentPatternTableModel.getColumnCount() >= 4) {
            Object passingValue = currentPatternTableModel.getValueAt(row, 3);
            if (passingValue != null) {
                passingMarks = Integer.parseInt(passingValue.toString());
            }
        } else {
            // Fallback to in-memory structure
            List<ExamComponent> components = subjectExamPatterns.get(subjectName);
            if (components != null && row < components.size()) {
                passingMarks = components.get(row).passingMarks;
            }
        }
        
        JDialog editDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Component", true);
        editDialog.setLayout(new BorderLayout(10, 10));
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        formPanel.setBackground(themeManager.getCardColor());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Component Name
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel nameLabel = new JLabel("Component Name:");
        formPanel.add(nameLabel, gbc);
        
        gbc.gridx = 1;
        JTextField nameField = new JTextField(componentName, 20);
        formPanel.add(nameField, gbc);
        
        // Max Marks
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel marksLabel = new JLabel("Max Marks:");
        formPanel.add(marksLabel, gbc);
        
        gbc.gridx = 1;
        JTextField marksField = new JTextField(maxMarksStr, 20);
        formPanel.add(marksField, gbc);
        
        // Weightage
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel weightageLabel = new JLabel("<html>Weightage (%):<br><small>Contribution to subject total</small></html>");
        formPanel.add(weightageLabel, gbc);
        
        gbc.gridx = 1;
        JTextField weightageField = new JTextField(weightageStr, 20);
        formPanel.add(weightageField, gbc);
        
        // Passing Marks
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel passingLabel = new JLabel("<html>Passing Marks:<br><small>Minimum marks to pass</small></html>");
        formPanel.add(passingLabel, gbc);
        
        gbc.gridx = 1;
        JTextField passingField = new JTextField(String.valueOf(passingMarks), 20);
        formPanel.add(passingField, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(themeManager.getCardColor());
        
        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");
        
        saveBtn.addActionListener(e -> {
            String newName = nameField.getText().trim();
            if (newName.isEmpty()) {
                JOptionPane.showMessageDialog(editDialog, "Component name cannot be empty!");
                return;
            }
            
            try {
                int newMarks = Integer.parseInt(marksField.getText().trim());
                int newWeightage = Integer.parseInt(weightageField.getText().trim());
                int newPassing = Integer.parseInt(passingField.getText().trim());
                
                if (newMarks <= 0 || newWeightage <= 0 || newPassing < 0) {
                    JOptionPane.showMessageDialog(editDialog, "Marks and weightage must be positive!");
                    return;
                }
                
                if (newPassing > newMarks) {
                    JOptionPane.showMessageDialog(editDialog, "Passing marks cannot exceed max marks!");
                    return;
                }
                
                if (newWeightage > 100) {
                    JOptionPane.showMessageDialog(editDialog, "Weightage cannot exceed 100%!");
                    return;
                }
                
                // Update table model
                currentPatternTableModel.setValueAt(newName, row, 0);
                currentPatternTableModel.setValueAt(newMarks, row, 1);
                currentPatternTableModel.setValueAt(newWeightage, row, 2);
                currentPatternTableModel.setValueAt(newPassing, row, 3);
                
                // Update in-memory structure
                List<ExamComponent> compList = subjectExamPatterns.get(subjectName);
                if (compList != null && row < compList.size()) {
                    ExamComponent comp = compList.get(row);
                    comp.componentName = newName;
                    comp.maxMarks = newMarks;
                    comp.weightage = newWeightage;
                    comp.passingMarks = newPassing;
                }
                
                // *** UPDATE DATABASE IF SECTION IS ALREADY SAVED ***
                if (editSectionId != null && editSectionId > 0) {  // Only update DB if this is an existing section
                    updateExamTypeInDatabase(componentName, newName, newMarks, newWeightage, newPassing, subjectName);
                }
                
                validatePattern(subjectName);
                editDialog.dispose();
                JOptionPane.showMessageDialog(this, "Component updated successfully!");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(editDialog, "Please enter valid numbers!");
            }
        });
        
        cancelBtn.addActionListener(e -> editDialog.dispose());
        
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        
        editDialog.add(formPanel, BorderLayout.CENTER);
        editDialog.add(buttonPanel, BorderLayout.SOUTH);
        editDialog.pack();
        editDialog.setLocationRelativeTo(this);
        editDialog.setVisible(true);
    }
    
    // Method to remove component from table
    private void removeComponentAtRow(int row) {
        String subjectName = getCurrentSelectedSubject();
        if (subjectName == null) {
            showError("Please select a subject first");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to remove this component?",
                "Confirm Removal",
                JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            // Remove from in-memory structure
            List<ExamComponent> components = subjectExamPatterns.get(subjectName);
            if (components != null && row < components.size()) {
                ExamComponent removed = components.remove(row);
                // Update display
                displayPattern(components);
                validatePattern(subjectName);
            }
        }
    }
    
    /**
     * Updates an exam type in the database when editing an existing section
     * @param oldName Old component name (to identify which record to update)
     * @param newName New component name
     * @param newMaxMarks New max marks
     * @param newWeightage New weightage percentage
     * @param newPassingMarks New passing marks
     * @param subjectName Subject this component belongs to
     */
    private void updateExamTypeInDatabase(String oldName, String newName, int newMaxMarks, 
                                         int newWeightage, int newPassingMarks, String subjectName) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Get subject_id
            int subjectId = getSubjectId(conn, subjectName);
            if (subjectId <= 0) {
                System.err.println("Could not find subject: " + subjectName);
                return;
            }
            
            // Find the exam_type_id through subject_exam_types table
            String findQuery = "SELECT et.id FROM exam_types et " +
                             "JOIN subject_exam_types sext ON et.id = sext.exam_type_id " +
                             "WHERE et.section_id = ? AND sext.subject_id = ? AND et.exam_name = ?";
            
            int examTypeId = -1;
            try (PreparedStatement ps = conn.prepareStatement(findQuery)) {
                ps.setInt(1, editSectionId);
                ps.setInt(2, subjectId);
                ps.setString(3, oldName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        examTypeId = rs.getInt("id");
                    }
                }
            }
            
            if (examTypeId > 0) {
                // Check if max_marks column exists
                DatabaseMetaData dbMetaData = conn.getMetaData();
                ResultSet rsColumns = dbMetaData.getColumns(null, null, "exam_types", "max_marks");
                boolean hasMaxMarksColumn = rsColumns.next();
                rsColumns.close();
                // Update the exam_type record - with or without max_marks column
                String updateQuery;
                if (hasMaxMarksColumn) {
                    updateQuery = "UPDATE exam_types SET exam_name = ?, max_marks = ?, weightage = ?, passing_marks = ? WHERE id = ?";
                } else {
                    // Backward compatibility: update without max_marks
                    updateQuery = "UPDATE exam_types SET exam_name = ?, weightage = ?, passing_marks = ? WHERE id = ?";
                }
                
                try (PreparedStatement ps = conn.prepareStatement(updateQuery)) {
                    ps.setString(1, newName);
                    if (hasMaxMarksColumn) {
                        ps.setInt(2, newMaxMarks);
                        ps.setInt(3, newWeightage);
                        ps.setInt(4, newPassingMarks);
                        ps.setInt(5, examTypeId);
                    } else {
                        ps.setInt(2, newWeightage);
                        ps.setInt(3, newPassingMarks);
                        ps.setInt(4, examTypeId);
                    }
                    int rowsUpdated = ps.executeUpdate();
                    
                    if (rowsUpdated > 0) {
                        if (hasMaxMarksColumn) {
                            System.out.println("✅ Database updated: " + oldName + " -> " + newName + 
                                             " (max: " + newMaxMarks + ", weightage: " + newWeightage + "%, passing: " + newPassingMarks + ")");
                        } else {
                            System.out.println("✅ Database updated (no max_marks column): " + oldName + " -> " + newName + 
                                             " (weightage: " + newWeightage + "%, passing: " + newPassingMarks + ")");
                        }
                    } else {
                        System.err.println("❌ No rows updated for exam_type_id: " + examTypeId);
                    }
                }
            } else {
                System.err.println("❌ Could not find exam type with name: " + oldName + " for subject: " + subjectName);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error updating exam type in database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Inner class for rendering action buttons in exam pattern table
    private class ComponentButtonRenderer extends JPanel implements TableCellRenderer {
        private JButton editButton;
        private JButton removeButton;
        
        public ComponentButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
            setOpaque(true);
            
            editButton = new JButton("Edit");
            editButton.setPreferredSize(new Dimension(70, 25));
            editButton.setBackground(new Color(52, 152, 219));
            editButton.setForeground(Color.WHITE);
            editButton.setFocusPainted(false);
            editButton.setBorderPainted(false);
            
            removeButton = new JButton("Remove");
            removeButton.setPreferredSize(new Dimension(80, 25));
            removeButton.setBackground(new Color(231, 76, 60));
            removeButton.setForeground(Color.WHITE);
            removeButton.setFocusPainted(false);
            removeButton.setBorderPainted(false);
            
            add(editButton);
            add(removeButton);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setBackground(table.getSelectionBackground());
            } else {
                setBackground(table.getBackground());
            }
            return this;
        }
    }
    
    // Inner class for editing action buttons in exam pattern table
    private class ComponentButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private JPanel panel;
        private JButton editButton;
        private JButton removeButton;
        private int currentRow;
        
        public ComponentButtonEditor() {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
            panel.setOpaque(true);
            
            editButton = new JButton("Edit");
            editButton.setPreferredSize(new Dimension(70, 25));
            editButton.setBackground(new Color(52, 152, 219));
            editButton.setForeground(Color.WHITE);
            editButton.setFocusPainted(false);
            editButton.setBorderPainted(false);
            
            removeButton = new JButton("Remove");
            removeButton.setPreferredSize(new Dimension(80, 25));
            removeButton.setBackground(new Color(231, 76, 60));
            removeButton.setForeground(Color.WHITE);
            removeButton.setFocusPainted(false);
            removeButton.setBorderPainted(false);
            
            editButton.addActionListener(e -> {
                fireEditingStopped();
                showEditComponentDialog(currentRow);
            });
            
            removeButton.addActionListener(e -> {
                fireEditingStopped();
                removeComponentAtRow(currentRow);
            });
            
            panel.add(editButton);
            panel.add(removeButton);
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            currentRow = row;
            panel.setBackground(table.getSelectionBackground());
            return panel;
        }
        
        @Override
        public Object getCellEditorValue() {
            return "Edit/Remove";
        }
    }
    
    // Inner class for rendering action buttons in subject table
    private class SubjectButtonRenderer extends JPanel implements TableCellRenderer {
        private JButton editButton;
        private JButton removeButton;
        
        public SubjectButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
            setOpaque(true);
            
            editButton = new JButton("Edit");
            editButton.setPreferredSize(new Dimension(70, 25));
            editButton.setBackground(new Color(52, 152, 219));
            editButton.setForeground(Color.WHITE);
            editButton.setFocusPainted(false);
            editButton.setBorderPainted(false);
            
            removeButton = new JButton("Remove");
            removeButton.setPreferredSize(new Dimension(80, 25));
            removeButton.setBackground(new Color(231, 76, 60));
            removeButton.setForeground(Color.WHITE);
            removeButton.setFocusPainted(false);
            removeButton.setBorderPainted(false);
            
            add(editButton);
            add(removeButton);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setBackground(table.getSelectionBackground());
            } else {
                setBackground(table.getBackground());
            }
            return this;
        }
    }
    
    // Inner class for editing action buttons in table
    private class SubjectButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private JPanel panel;
        private JButton editButton;
        private JButton removeButton;
        private int currentRow;
        
        public SubjectButtonEditor() {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
            panel.setOpaque(true);
            
            editButton = new JButton("Edit");
            editButton.setPreferredSize(new Dimension(70, 25));
            editButton.setBackground(new Color(52, 152, 219));
            editButton.setForeground(Color.WHITE);
            editButton.setFocusPainted(false);
            editButton.setBorderPainted(false);
            
            removeButton = new JButton("Remove");
            removeButton.setPreferredSize(new Dimension(80, 25));
            removeButton.setBackground(new Color(231, 76, 60));
            removeButton.setForeground(Color.WHITE);
            removeButton.setFocusPainted(false);
            removeButton.setBorderPainted(false);
            
            editButton.addActionListener(e -> {
                fireEditingStopped();
                showEditSubjectDialog(currentRow);
            });
            
            removeButton.addActionListener(e -> {
                fireEditingStopped();
                removeSubjectAtRow(currentRow);
            });
            
            panel.add(editButton);
            panel.add(removeButton);
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            currentRow = row;
            panel.setBackground(table.getSelectionBackground());
            return panel;
        }
        
        @Override
        public Object getCellEditorValue() {
            return "Edit/Remove";
        }
    }
    
    // Inner classes for exam structure (same as original dialog)
    /**
     * ExamComponent represents an exam component in the SCALED grading system (Option B).
     * 
     * SCALED GRADING SYSTEM (Current Implementation):
     * - componentName: Component name (e.g., "Internal 1", "Final Exam")
     * - maxMarks: EXAM PAPER max marks (e.g., 50 marks on physical exam paper)
     * - weightage: Contribution % to subject total (must sum to 100% across all components)
     * - passingMarks: Minimum SCALED marks to pass THIS component
     * 
     * SCALING FORMULA:
     * - Scaled Contribution = (marks_obtained / maxMarks) × weightage
     * - Enter marks out of maxMarks, system auto-scales to weightage
     * 
     * CRITICAL RULES:
     * - Subject Total = Σ(all scaled contributions) out of 100
     * - DUAL PASSING: Must pass EACH component AND overall subject
     * - Failing ANY single component = SUBJECT FAIL (even if total passes)
     * 
     * EXAMPLE (CLOUD COMPUTING with 50-mark internal exams):
     * - Internal 1: maxMarks=50, weightage=20%, passingMarks=20 (40% of 50)
     *   → Student scores 40/50 → Scaled: (40/50)×20 = 16/20 ✅ PASS
     * - Internal 2: maxMarks=50, weightage=25%, passingMarks=25 (50% of 50)
     *   → Student scores 15/50 → Scaled: (15/50)×25 = 7.5/25 ❌ FAIL (< 25)
     * - Final: maxMarks=100, weightage=40%, passingMarks=40
     *   → Student scores 80/100 → Scaled: (80/100)×40 = 32/40 ✅ PASS
     * 
     * Formula: Subject Total = Σ(marks_obtained/maxMarks × weightage) out of 100
     */
    private static class ExamComponent {
        String componentName;
        int maxMarks;      // Exam paper max marks (e.g., 50, 100)
        int weightage;     // Contribution % to subject total (out of 100)
        int passingMarks;  // Minimum marks to pass this component (on original scale)
        
        ExamComponent(String componentName, int maxMarks, int weightage) {
            this.componentName = componentName;
            this.maxMarks = maxMarks;
            this.weightage = weightage;
            this.passingMarks = (int)(maxMarks * 0.4); // Default 40%
        }
        
        ExamComponent(String componentName, int maxMarks, int weightage, int passingMarks) {
            this.componentName = componentName;
            this.maxMarks = maxMarks;
            this.weightage = weightage;
            this.passingMarks = passingMarks;
        }
        
        @Override
        public String toString() {
            return componentName + " (" + maxMarks + "M, " + weightage + "%, Pass: " + passingMarks + ")";  
        }
    }
}