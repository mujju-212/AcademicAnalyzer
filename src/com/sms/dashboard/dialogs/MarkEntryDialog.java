package com.sms.dashboard.dialogs;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.*;
import java.awt.*;
import java.awt.Color;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import com.sms.database.DatabaseConnection;
import com.sms.theme.ThemeManager;
import com.sms.dao.SectionDAO;
import com.sms.dao.StudentDAO;

import javax.swing.border.*;

// Import/Export related imports
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Element;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MarkEntryDialog extends JDialog {
    private JComboBox<String> sectionDropdown;
    private JComboBox<String> subjectDropdown;
    private JComboBox<String> examTypeDropdown;
    private JTable marksTable;
    private DefaultTableModel tableModel;
    private JButton saveButton;
    private JButton importButton;
    private JButton exportButton;
    private JButton helpButton;
    private JLabel statusLabel;
    private JLabel totalStudentsLabel;
    private JLabel subjectsCountLabel;
    private JLabel lastSavedLabel;
    
    private Map<String, Integer> sectionIdMap;
    private Map<String, Integer> subjectIdMap;
    private Map<String, Integer> examTypeIdMap;
    private Map<String, Integer> studentIdMap;
    
    private ThemeManager themeManager;
    private int currentUserId;
    
    // Track current selections
    private Integer currentSectionId;
    private Integer currentSubjectId;
    private boolean isFlexibleMarking = false;
    
    // Modern theme colors matching dashboard
    private Color backgroundColor = new Color(248, 249, 250);
    private Color cardBackground = Color.WHITE;
    private Color primaryBlue = new Color(99, 102, 241);
    private Color primaryGreen = new Color(34, 197, 94);
    private Color primaryRed = new Color(239, 68, 68);
    private Color primaryOrange = new Color(251, 146, 60);
    private Color textPrimary = new Color(17, 24, 39);
    private Color textSecondary = new Color(107, 114, 128);
    private Color borderColor = new Color(229, 231, 235);
    private Color hoverColor = new Color(243, 244, 246);
    
    public MarkEntryDialog(JFrame parent) {
        super(parent, "Mark Entry", true);
        this.currentUserId = com.sms.login.LoginScreen.currentUserId;
        this.themeManager = ThemeManager.getInstance();
        
        setSize(1200, 800);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        
        initializeMaps();
        initializeUI();
        loadSections();
    }
    
    private void initializeMaps() {
        sectionIdMap = new HashMap<>();
        subjectIdMap = new HashMap<>();
        examTypeIdMap = new HashMap<>();
        studentIdMap = new HashMap<>();
    }
    
    private void initializeUI() {
        getContentPane().setBackground(backgroundColor);
        
        // Main container
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(backgroundColor);
        
        // Header Panel
        mainPanel.add(createHeaderPanel(), BorderLayout.NORTH);
        
        // Content Panel
        mainPanel.add(createContentPanel(), BorderLayout.CENTER);
        
        add(mainPanel);
    }
    
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(cardBackground);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor),
            BorderFactory.createEmptyBorder(32, 40, 32, 40)
        ));
        
        // Title Section
        JPanel titleSection = new JPanel(new BorderLayout());
        titleSection.setBackground(cardBackground);
        
        JLabel titleLabel = new JLabel("Mark Entry");
        titleLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 32));
        titleLabel.setForeground(textPrimary);
        
        JLabel subtitleLabel = new JLabel("Enter and manage student marks");
        subtitleLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        subtitleLabel.setForeground(textSecondary);
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBackground(cardBackground);
        titlePanel.add(titleLabel);
        titlePanel.add(subtitleLabel);
        
        titleSection.add(titlePanel, BorderLayout.WEST);
        
        // Stats Panel
        JPanel statsPanel = createStatsPanel();
        titleSection.add(statsPanel, BorderLayout.EAST);
        
        headerPanel.add(titleSection, BorderLayout.NORTH);
        headerPanel.add(createSelectionPanel(), BorderLayout.CENTER);
        
        return headerPanel;
    }
    
    private JPanel createStatsPanel() {
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        statsPanel.setBackground(cardBackground);
        
        // Total Students Card
        JPanel studentsCard = createStatCard("0", "Students", primaryBlue);
        totalStudentsLabel = (JLabel) ((JPanel) studentsCard.getComponent(0)).getComponent(0);
        
        // Subjects Count Card
        JPanel subjectsCard = createStatCard("0", "Subjects", primaryGreen);
        subjectsCountLabel = (JLabel) ((JPanel) subjectsCard.getComponent(0)).getComponent(0);
        
        // Last Saved Card
        JPanel savedCard = createStatCard("Never", "Last Saved", primaryOrange);
        lastSavedLabel = (JLabel) ((JPanel) savedCard.getComponent(0)).getComponent(0);
        
        statsPanel.add(studentsCard);
        statsPanel.add(subjectsCard);
        statsPanel.add(savedCard);
        
        return statsPanel;
    }
    
    private JPanel createStatCard(String value, String label, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(cardBackground);
        card.setPreferredSize(new Dimension(120, 80));
        
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(cardBackground);
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 24));
        valueLabel.setForeground(accentColor);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel labelText = new JLabel(label);
        labelText.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
        labelText.setForeground(textSecondary);
        labelText.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        content.add(valueLabel);
        content.add(Box.createVerticalStrut(4));
        content.add(labelText);
        
        card.add(content, BorderLayout.CENTER);
        
        return card;
    }
    
    private JPanel createSelectionPanel() {
        JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        selectionPanel.setBackground(cardBackground);
        selectionPanel.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));
        
        // Section Selection
        JPanel sectionPanel = createSelectionGroup("Section", createSectionDropdown());
        
        // Subject Selection (NEW)
        JPanel subjectPanel = createSelectionGroup("Subject", createSubjectDropdown());
        
        // Exam Type Selection
        JPanel examPanel = createSelectionGroup("Exam Type / Component", createExamTypeDropdown());
        
        // Load Button
        JButton loadButton = createPrimaryButton("Load Students", primaryBlue);
        loadButton.addActionListener(e -> loadStudentsAndMarks());
        
        selectionPanel.add(sectionPanel);
        selectionPanel.add(subjectPanel);
        selectionPanel.add(examPanel);
        selectionPanel.add(Box.createHorizontalStrut(20));
        selectionPanel.add(loadButton);
        
        return selectionPanel;
    }
    
    private JPanel createSelectionGroup(String label, JComponent component) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(cardBackground);
        
        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        labelComponent.setForeground(textSecondary);
        labelComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        panel.add(labelComponent);
        panel.add(Box.createVerticalStrut(8));
        panel.add(component);
        
        return panel;
    }
    
    private JComboBox<String> createSectionDropdown() {
        sectionDropdown = new JComboBox<>();
        sectionDropdown.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 15));
        sectionDropdown.setPreferredSize(new Dimension(200, 45));
        sectionDropdown.setBackground(cardBackground);
        sectionDropdown.setForeground(textPrimary);
        sectionDropdown.setBorder(BorderFactory.createLineBorder(borderColor, 1));
        
        sectionDropdown.addActionListener(e -> {
            String selected = (String) sectionDropdown.getSelectedItem();
            if (selected != null && !selected.equals("Select Section")) {
                currentSectionId = sectionIdMap.get(selected);
                loadSubjects();
                clearTable();
                updateSectionStats();
            }
        });
        
        // Custom renderer
        sectionDropdown.setRenderer(createComboBoxRenderer());
        
        return sectionDropdown;
    }
  
    private JComboBox<String> createExamTypeDropdown() {
        examTypeDropdown = new JComboBox<>();
        examTypeDropdown.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 15));
        examTypeDropdown.setPreferredSize(new Dimension(300, 45));
        examTypeDropdown.setBackground(cardBackground);
        examTypeDropdown.setForeground(textPrimary);
        examTypeDropdown.setBorder(BorderFactory.createLineBorder(borderColor, 1));
        examTypeDropdown.setEnabled(false); // Initially disabled
        
        // Make sure it's focusable and clickable
        examTypeDropdown.setFocusable(true);
        examTypeDropdown.setRequestFocusEnabled(true);
        
        // Add initial item
        examTypeDropdown.addItem("Select Exam Type");
        
        // Custom renderer
        examTypeDropdown.setRenderer(createComboBoxRenderer());
        
        return examTypeDropdown;
    }
      
   
    
    private DefaultListCellRenderer createComboBoxRenderer() {
        return new DefaultListCellRenderer() {
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
        };
    }
    
    private JPanel createContentPanel() {
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(backgroundColor);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        
        // Main content card
        JPanel contentCard = new JPanel(new BorderLayout());
        contentCard.setBackground(cardBackground);
        contentCard.setBorder(BorderFactory.createLineBorder(borderColor, 1));
        
        // Table Panel
        contentCard.add(createTablePanel(), BorderLayout.CENTER);
        
        // Bottom Panel
        contentCard.add(createBottomPanel(), BorderLayout.SOUTH);
        
        contentPanel.add(contentCard);
        
        return contentPanel;
    }
    
    private JPanel createTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(cardBackground);
        
        // Create table
        String[] columnNames = {"Roll No", "Student Name", "Marks"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2; // Only marks column is editable
            }
        };
        
        marksTable = new JTable(tableModel);
        marksTable.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        marksTable.setRowHeight(45);
        marksTable.setShowGrid(true);
        marksTable.setGridColor(borderColor);
        marksTable.setBackground(cardBackground);
        marksTable.setSelectionBackground(new Color(99, 102, 241, 30));
        marksTable.setSelectionForeground(textPrimary);
        
        // Header styling
        JTableHeader header = marksTable.getTableHeader();
        header.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
        header.setBackground(hoverColor);
        header.setForeground(textPrimary);
        header.setPreferredSize(new Dimension(0, 50));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, borderColor));
        
        // Column widths
        marksTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        marksTable.getColumnModel().getColumn(1).setPreferredWidth(350);
        marksTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        
        // Custom cell renderer
        marksTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
                
                if (column == 2 && value != null && !value.toString().isEmpty()) {
                    try {
                        int mark = Integer.parseInt(value.toString());
                        if (mark < 40) {
                            c.setBackground(new Color(254, 226, 226)); // Light red
                            setForeground(new Color(153, 27, 27));
                        } else if (mark >= 80) {
                            c.setBackground(new Color(220, 252, 231)); // Light green
                            setForeground(new Color(22, 101, 52));
                        } else {
                            c.setBackground(cardBackground);
                            setForeground(textPrimary);
                        }
                    } catch (NumberFormatException e) {
                        if (value.toString().equalsIgnoreCase("ABS")) {
                            c.setBackground(new Color(254, 243, 199)); // Light yellow
                            setForeground(new Color(146, 64, 14));
                        } else {
                            c.setBackground(cardBackground);
                            setForeground(textPrimary);
                        }
                    }
                } else {
                    c.setBackground(cardBackground);
                    setForeground(textPrimary);
                }
                
                if (isSelected && !hasFocus) {
                    c.setBackground(new Color(99, 102, 241, 30));
                    if (column < 2) {
                        setForeground(textPrimary);
                    }
                }
                
                return c;
            }
        });
        
        // Custom cell editor for marks
        marksTable.setDefaultEditor(Object.class, createMarksEditor());
        
        JScrollPane scrollPane = new JScrollPane(marksTable);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(cardBackground);
        
        // Status bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(hoverColor);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));
        
        statusLabel = new JLabel("Select section, subject and exam type to load students");
        statusLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        statusLabel.setForeground(textSecondary);
        
        // Quick actions
        JPanel quickActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        quickActions.setBackground(hoverColor);
        
        JButton markAbsentButton = createSmallButton("Mark Selected Absent", primaryOrange);
        markAbsentButton.addActionListener(e -> markSelectedAbsent());
        
        JButton clearMarksButton = createSmallButton("Clear Selected", primaryRed);
        clearMarksButton.addActionListener(e -> clearSelectedMarks());
        
        quickActions.add(markAbsentButton);
        quickActions.add(clearMarksButton);
        
        statusBar.add(statusLabel, BorderLayout.WEST);
        statusBar.add(quickActions, BorderLayout.EAST);
        
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        tablePanel.add(statusBar, BorderLayout.SOUTH);
        
        return tablePanel;
    }
    
    private TableCellEditor createMarksEditor() {
        JTextField textField = new JTextField();
        textField.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(primaryBlue, 2),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        
        DefaultCellEditor editor = new DefaultCellEditor(textField) {
            @Override
            public boolean stopCellEditing() {
                String value = textField.getText().trim();
                
                if (value.isEmpty()) {
                    return super.stopCellEditing();
                }
                
                if (value.equalsIgnoreCase("ABS")) {
                    return super.stopCellEditing();
                }
                
                try {
                    int mark = Integer.parseInt(value);
                    
                    // Get max marks from current exam type
                    String examType = (String) examTypeDropdown.getSelectedItem();
                    int maxMarks = extractMaxMarks(examType);
                    
                    if (mark < 0 || mark > maxMarks) {
                        JOptionPane.showMessageDialog(marksTable,
                            "Mark must be between 0 and " + maxMarks,
                            "Invalid Mark",
                            JOptionPane.ERROR_MESSAGE);
                        textField.selectAll();
                        textField.requestFocus();
                        return false;
                    }
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(marksTable,
                        "Please enter a valid number or 'ABS'",
                        "Invalid Format",
                        JOptionPane.ERROR_MESSAGE);
                    textField.selectAll();
                    textField.requestFocus();
                    return false;
                }
                
                return super.stopCellEditing();
            }
        };
        
        editor.setClickCountToStart(1);
        return editor;
    }
    
    private int extractMaxMarks(String examType) {
        if (examType == null) return 100;
        
        // Extract marks from format "Component Name (XX marks)"
        int start = examType.lastIndexOf("(");
        int end = examType.lastIndexOf(" marks)");
        
        if (start != -1 && end != -1 && start < end) {
            try {
                return Integer.parseInt(examType.substring(start + 1, end).trim());
            } catch (NumberFormatException e) {
                return 100;
            }
        }
        return 100;
    }
    
    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(cardBackground);
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor),
            BorderFactory.createEmptyBorder(20, 30, 20, 30)
        ));
        
        // Import/Export Panel
        JPanel importExportPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        importExportPanel.setBackground(cardBackground);
        
        importButton = createIconButton("📥 Import", "Import marks from Excel");
        importButton.setEnabled(false);
        importButton.addActionListener(e -> showImportDialog());
        
        exportButton = createIconButton("📤 Export", "Export marks to Excel");
        exportButton.setEnabled(false);
        exportButton.addActionListener(e -> showExportDialog());
        
        helpButton = createIconButton("❓ Help", "View import/export format help");
        helpButton.addActionListener(e -> showFormatHelp());
        
        importExportPanel.add(importButton);
        importExportPanel.add(exportButton);
        importExportPanel.add(helpButton);
        
        // Action buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actionPanel.setBackground(cardBackground);
        
        JButton cancelButton = createSecondaryButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        
        saveButton = createPrimaryButton("Save Marks", primaryGreen);
        saveButton.setEnabled(false);
        saveButton.addActionListener(e -> saveMarks());
        
        actionPanel.add(cancelButton);
        actionPanel.add(saveButton);
        
        bottomPanel.add(importExportPanel, BorderLayout.WEST);
        bottomPanel.add(actionPanel, BorderLayout.EAST);
        
        return bottomPanel;
    }
    
    // Data Loading Methods
    private void loadSections() {
        sectionDropdown.removeAllItems();
        sectionIdMap.clear();
        
        sectionDropdown.addItem("Select Section");
        
        SectionDAO sectionDAO = new SectionDAO();
        List<SectionDAO.SectionInfo> sections = sectionDAO.getSectionsByUser(currentUserId);
        
        for (SectionDAO.SectionInfo section : sections) {
            sectionDropdown.addItem(section.sectionName);
            sectionIdMap.put(section.sectionName, section.id);
        }
    }
    
  
  

    // Let's also add a method to manually check what's happening
    private void debugDropdownState() {
        System.out.println("\n=== Dropdown State Debug ===");
        System.out.println("Section dropdown enabled: " + sectionDropdown.isEnabled());
        System.out.println("Section dropdown items: " + sectionDropdown.getItemCount());
        System.out.println("Subject dropdown enabled: " + subjectDropdown.isEnabled());
        System.out.println("Subject dropdown items: " + subjectDropdown.getItemCount());
        System.out.println("Exam type dropdown enabled: " + examTypeDropdown.isEnabled());
        System.out.println("Exam type dropdown items: " + examTypeDropdown.getItemCount());
        
        if (examTypeDropdown.getItemCount() > 0) {
            System.out.println("Exam type items:");
            for (int i = 0; i < examTypeDropdown.getItemCount(); i++) {
                System.out.println("  " + i + ": " + examTypeDropdown.getItemAt(i));
            }
        }
        System.out.println("=== End Debug ===\n");
    }


    private void loadTraditionalExamTypes() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // For traditional marking, load exam types from exam_types table
            String query = "SELECT DISTINCT id, exam_name FROM exam_types " +
                          "WHERE section_id = ? " +
                          "ORDER BY id";
            
            boolean foundExamTypes = false;
            
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, currentSectionId);
                
                System.out.println("Loading exam types for section: " + currentSectionId);
                
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        foundExamTypes = true;
                        int examId = rs.getInt("id");
                        String examName = rs.getString("exam_name");
                        
                        System.out.println("Found exam type: " + examName + " (ID: " + examId + ")");
                        
                        examTypeDropdown.addItem(examName);
                        examTypeIdMap.put(examName, examId);
                    }
                }
            }
            
            if (!foundExamTypes) {
                System.out.println("No exam types found for section " + currentSectionId);
                statusLabel.setText("No exam types configured for this section");
                statusLabel.setForeground(primaryOrange);
            }
        }
    }

    private void loadExamTypes() {
        examTypeDropdown.removeAllItems();
        examTypeIdMap.clear();
        examTypeDropdown.addItem("Select Exam Type");
        examTypeDropdown.setEnabled(false);
        
        if (currentSectionId == null || currentSubjectId == null) return;
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // First check if section uses flexible marking
            String checkQuery = "SELECT marking_system FROM sections WHERE id = ?";
            boolean isFlexible = false;
            
            try (PreparedStatement ps = conn.prepareStatement(checkQuery)) {
                ps.setInt(1, currentSectionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        isFlexible = "flexible".equals(rs.getString("marking_system"));
                    }
                }
            }
            
            if (isFlexible) {
                // Load components from marking scheme
                loadFlexibleComponents(conn);
            } else {
                // Load traditional exam types
                loadTraditionalExamTypes(conn);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadFlexibleComponents(Connection conn) throws SQLException {
        // Load components from the marking scheme table based on your database structure
        String query = "SELECT c.component_name, c.id FROM marking_scheme_components c " +
                       "JOIN section_marking_schemes sms ON c.scheme_id = sms.scheme_id " +
                       "WHERE sms.section_id = ? AND sms.subject_id = ? " +
                       "ORDER BY c.sequence_order";
        
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, currentSectionId);
            ps.setInt(2, currentSubjectId);
            
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    String componentName = rs.getString("component_name");
                    int componentId = rs.getInt("id");
                    
                    examTypeDropdown.addItem(componentName);
                    examTypeIdMap.put(componentName, componentId);
                }
                examTypeDropdown.setEnabled(count > 0);
            }
        }
    }

    private void loadTraditionalExamTypes(Connection conn) throws SQLException {
        // Your existing exam types loading logic
        String query = "SELECT id, exam_name FROM exam_types " +
                       "WHERE section_id = ? ORDER BY id";
        
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, currentSectionId);
            
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    int examId = rs.getInt("id");
                    String examName = rs.getString("exam_name");
                    
                    examTypeDropdown.addItem(examName);
                    examTypeIdMap.put(examName, examId);
                }
                examTypeDropdown.setEnabled(count > 0);
            }
        }
    }



// Also check the subject dropdown listener to ensure it's not disabling the exam type dropdown:
private JComboBox<String> createSubjectDropdown() {
    subjectDropdown = new JComboBox<>();
    subjectDropdown.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 15));
    subjectDropdown.setPreferredSize(new Dimension(250, 45));
    subjectDropdown.setBackground(cardBackground);
    subjectDropdown.setForeground(textPrimary);
    subjectDropdown.setBorder(BorderFactory.createLineBorder(borderColor, 1));
    subjectDropdown.setEnabled(false);
    
    subjectDropdown.addActionListener(e -> {
        String selected = (String) subjectDropdown.getSelectedItem();
        System.out.println("Subject selected: " + selected);
        
        if (selected != null && !selected.equals("Select Subject")) {
            currentSubjectId = subjectIdMap.get(selected);
            System.out.println("Current Subject ID set to: " + currentSubjectId);
            
            // Don't disable the exam type dropdown here
            // Just load the exam types
            loadExamTypes();
            clearTable();
        } else {
            // Only disable if no subject is selected
            examTypeDropdown.setEnabled(false);
            examTypeDropdown.removeAllItems();
            examTypeDropdown.addItem("Select Exam Type");
        }
    });
    
    subjectDropdown.setRenderer(createComboBoxRenderer());
    
    return subjectDropdown;
}

// Add a debug method to check dropdown states:
private void debugDropdownStates() {
    System.out.println("\n=== Dropdown States ===");
    System.out.println("Section dropdown:");
    System.out.println("  Enabled: " + sectionDropdown.isEnabled());
    System.out.println("  Items: " + sectionDropdown.getItemCount());
    System.out.println("  Selected: " + sectionDropdown.getSelectedItem());
    
    System.out.println("Subject dropdown:");
    System.out.println("  Enabled: " + subjectDropdown.isEnabled());
    System.out.println("  Items: " + subjectDropdown.getItemCount());
    System.out.println("  Selected: " + subjectDropdown.getSelectedItem());
    
    System.out.println("Exam type dropdown:");
    System.out.println("  Enabled: " + examTypeDropdown.isEnabled());
    System.out.println("  Items: " + examTypeDropdown.getItemCount());
    System.out.println("  Focusable: " + examTypeDropdown.isFocusable());
    System.out.println("  Visible: " + examTypeDropdown.isVisible());
    
    if (examTypeDropdown.getItemCount() > 0) {
        System.out.println("  Items in dropdown:");
        for (int i = 0; i < examTypeDropdown.getItemCount(); i++) {
            System.out.println("    " + i + ": " + examTypeDropdown.getItemAt(i));
        }
    }
    System.out.println("===================\n");
}

    // Also update the loadSubjects method to properly get subject IDs
    private void loadSubjects() {
        subjectDropdown.removeAllItems();
        subjectIdMap.clear();
        examTypeDropdown.removeAllItems();
        examTypeIdMap.clear();
        
        if (currentSectionId == null) {
            subjectDropdown.setEnabled(false);
            examTypeDropdown.setEnabled(false);
            return;
        }
        
        subjectDropdown.addItem("Select Subject");
        
        System.out.println("\n=== Loading Subjects for Section " + currentSectionId + " ===");
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT DISTINCT s.id, s.subject_name, ss.use_new_marking_system " +
                          "FROM section_subjects ss " +
                          "JOIN subjects s ON ss.subject_id = s.id " +
                          "WHERE ss.section_id = ? " +
                          "ORDER BY s.subject_name";
            
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, currentSectionId);
                
                try (ResultSet rs = ps.executeQuery()) {
                    boolean hasSubjects = false;
                    
                    while (rs.next()) {
                        hasSubjects = true;
                        int subjectId = rs.getInt("id");
                        String subjectName = rs.getString("subject_name");
                        boolean usesNewSystem = rs.getBoolean("use_new_marking_system");
                        
                        System.out.println("Found subject: " + subjectName + 
                                         " (ID: " + subjectId + 
                                         ", Uses new system: " + usesNewSystem + ")");
                        
                        subjectDropdown.addItem(subjectName);
                        subjectIdMap.put(subjectName, subjectId);
                    }
                    
                    subjectDropdown.setEnabled(hasSubjects);
                    examTypeDropdown.setEnabled(false);
                    
                    if (!hasSubjects) {
                        System.out.println("No subjects found for section " + currentSectionId);
                        statusLabel.setText("No subjects found for this section");
                        statusLabel.setForeground(primaryRed);
                    } else {
                        System.out.println("Total subjects loaded: " + subjectIdMap.size());
                    }
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading subjects: " + e.getMessage());
            statusLabel.setForeground(primaryRed);
        }
        
        System.out.println("=== End Loading Subjects ===\n");
    }    
    private void loadFlexibleComponents(int markingSchemeId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT mc.id, mc.component_name, mc.actual_max_marks, " +
                          "cg.group_name, cg.group_type " +
                          "FROM marking_components mc " +
                          "JOIN component_groups cg ON mc.group_id = cg.id " +
                          "WHERE cg.scheme_id = ? " +
                          "ORDER BY cg.sequence_order, mc.sequence_order";
            
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, markingSchemeId);
                
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int componentId = rs.getInt("id");
                        String componentName = rs.getString("component_name");
                        int maxMarks = rs.getInt("actual_max_marks");
                        String groupName = rs.getString("group_name");
                        
                        String displayName = componentName + " (" + maxMarks + " marks)";
                        
                        examTypeDropdown.addItem(displayName);
                        examTypeIdMap.put(displayName, -componentId); // Negative for components
                    }
                }
            }
        }
    }
    
  
    private void loadStudentsAndMarks() {
    	debugDropdownState();
        
     
        String selectedSection = (String) sectionDropdown.getSelectedItem();
        String selectedSubject = (String) subjectDropdown.getSelectedItem();
        String selectedExam = (String) examTypeDropdown.getSelectedItem();
        
        if (selectedSection == null || selectedSection.equals("Select Section") ||
            selectedSubject == null || selectedSubject.equals("Select Subject") ||
            selectedExam == null || selectedExam.equals("Select Exam Type")) {
            statusLabel.setText("Please select section, subject and exam type");
            statusLabel.setForeground(primaryRed);
            return;
        }
        
        Integer examTypeId = examTypeIdMap.get(selectedExam);
        
        // Clear table
        tableModel.setRowCount(0);
        
        // Update column header with exam type info
        String marksColumnHeader = "Marks (" + extractMaxMarks(selectedExam) + ")";
        tableModel.setColumnIdentifiers(new String[]{"Roll No", "Student Name", marksColumnHeader});
        
        // Load students
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT s.id, s.roll_number, s.student_name " +
                          "FROM students s " +
                          "JOIN sections sec ON s.section_id = sec.id " +
                          "WHERE s.section_id = ? AND sec.created_by = ? " +
                          "ORDER BY s.roll_number";
            
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, currentSectionId);
                ps.setInt(2, currentUserId);
                
                try (ResultSet rs = ps.executeQuery()) {
                    studentIdMap.clear();
                    List<Object[]> rows = new ArrayList<>();
                    
                    while (rs.next()) {
                        int studentId = rs.getInt("id");
                        String rollNumber = rs.getString("roll_number");
                        String studentName = rs.getString("student_name");
                        
                        studentIdMap.put(rollNumber, studentId);
                        
                        Object[] row = new Object[]{rollNumber, studentName, ""};
                        rows.add(row);
                    }
                    
                    // Add rows to table
                    for (Object[] row : rows) {
                        tableModel.addRow(row);
                    }
                    
                    // Load existing marks
                    if (examTypeId != null) {
                        if (isFlexibleMarking && examTypeId < 0) {
                            loadExistingComponentMarks(Math.abs(examTypeId));
                        } else {
                            loadExistingTraditionalMarks(selectedExam);
                        }
                    }
                    
                    // Update UI
                    updateStatistics(rows.size(), 1); // 1 subject selected
                    
                    // Enable buttons
                    saveButton.setEnabled(true);
                    importButton.setEnabled(true);
                    exportButton.setEnabled(true);
                    
                    statusLabel.setText("Loaded " + rows.size() + " students for " + 
                                      selectedSubject + " - " + selectedExam);
                    statusLabel.setForeground(primaryGreen);
                    
                    // Force table refresh
                    SwingUtilities.invokeLater(() -> {
                        tableModel.fireTableDataChanged();
                        marksTable.revalidate();
                        marksTable.repaint();
                    });
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading students: " + e.getMessage());
            statusLabel.setText("Error loading students: " + e.getMessage());
            statusLabel.setForeground(primaryRed);
        }
    }
    
    private void loadExistingComponentMarks(int componentId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT s.roll_number, cm.marks_obtained " +
                          "FROM component_marks cm " +
                          "JOIN students s ON cm.student_id = s.id " +
                          "WHERE s.section_id = ? AND cm.component_id = ?";
            
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, currentSectionId);
                ps.setInt(2, componentId);
                
                try (ResultSet rs = ps.executeQuery()) {
                    Map<String, String> marksMap = new HashMap<>();
                    
                    while (rs.next()) {
                        String rollNumber = rs.getString("roll_number");
                        int marks = rs.getInt("marks_obtained");
                        marksMap.put(rollNumber, marks == 0 ? "ABS" : String.valueOf(marks));
                    }
                    
                    // Update table with existing marks
                    for (int row = 0; row < tableModel.getRowCount(); row++) {
                        String rollNumber = (String) tableModel.getValueAt(row, 0);
                        String marks = marksMap.get(rollNumber);
                        
                        if (marks != null) {
                            tableModel.setValueAt(marks, row, 2);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void loadExistingTraditionalMarks(String examType) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT s.roll_number, sm.marks_obtained " +
                          "FROM student_marks sm " +
                          "JOIN students s ON sm.student_id = s.id " +
                          "WHERE s.section_id = ? AND sm.subject_id = ? AND sm.exam_type = ?";
            
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, currentSectionId);
                ps.setInt(2, currentSubjectId);
                ps.setString(3, examType);
                
                try (ResultSet rs = ps.executeQuery()) {
                    Map<String, String> marksMap = new HashMap<>();
                    
                    while (rs.next()) {
                        String rollNumber = rs.getString("roll_number");
                        int marks = rs.getInt("marks_obtained");
                        marksMap.put(rollNumber, marks == 0 ? "ABS" : String.valueOf(marks));
                    }
                    
                    // Update table with existing marks
                    for (int row = 0; row < tableModel.getRowCount(); row++) {
                        String rollNumber = (String) tableModel.getValueAt(row, 0);
                        String marks = marksMap.get(rollNumber);
                        
                        if (marks != null) {
                            tableModel.setValueAt(marks, row, 2);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // UI Update Methods
    private void updateStatistics(int studentCount, int subjectCount) {
        totalStudentsLabel.setText(String.valueOf(studentCount));
        subjectsCountLabel.setText(String.valueOf(subjectCount));
    }
    
    private void updateSectionStats() {
        if (currentSectionId != null) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                // Get total subjects
                String subjectQuery = "SELECT COUNT(*) FROM section_subjects WHERE section_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(subjectQuery)) {
                    ps.setInt(1, currentSectionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            subjectsCountLabel.setText(String.valueOf(rs.getInt(1)));
                        }
                    }
                }
                
                // Get total students
                String studentQuery = "SELECT COUNT(*) FROM students WHERE section_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(studentQuery)) {
                    ps.setInt(1, currentSectionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            totalStudentsLabel.setText(String.valueOf(rs.getInt(1)));
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void clearTable() {
        tableModel.setRowCount(0);
        saveButton.setEnabled(false);
        importButton.setEnabled(false);
        exportButton.setEnabled(false);
        statusLabel.setText("Select section, subject and exam type to load students");
        statusLabel.setForeground(textSecondary);
    }
    
    private void markSelectedAbsent() {
        int[] selectedRows = marksTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, 
                "Please select students to mark as absent", 
                "No Selection", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        for (int row : selectedRows) {
            tableModel.setValueAt("ABS", row, 2);
        }
        
        statusLabel.setText("Marked " + selectedRows.length + " students as absent");
        statusLabel.setForeground(primaryOrange);
    }
    
    private void clearSelectedMarks() {
        int[] selectedRows = marksTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, 
                "Please select students to clear marks", 
                "No Selection", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        for (int row : selectedRows) {
            tableModel.setValueAt("", row, 2);
        }
        
        statusLabel.setText("Cleared marks for " + selectedRows.length + " students");
        statusLabel.setForeground(primaryRed);
    }
    
    // Save Methods
    private void saveMarks() {
        String selectedExam = (String) examTypeDropdown.getSelectedItem();
        Integer examTypeId = examTypeIdMap.get(selectedExam);
        
        if (examTypeId == null) return;
        
        // Validate marks before saving
        if (!validateMarks()) {
            return;
        }
        
        // Check if this is a flexible component (negative ID)
        if (isFlexibleMarking && examTypeId < 0) {
            saveFlexibleComponentMarks(Math.abs(examTypeId));
        } else {
            saveTraditionalMarks(selectedExam);
        }
    }
    
    private boolean validateMarks() {
        String selectedExam = (String) examTypeDropdown.getSelectedItem();
        int maxMarks = extractMaxMarks(selectedExam);
        
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            Object valueObj = tableModel.getValueAt(row, 2);
            String value = valueObj != null ? valueObj.toString().trim() : "";
            
            if (!value.isEmpty() && !value.equalsIgnoreCase("ABS")) {
                try {
                    int mark = Integer.parseInt(value);
                    
                    if (mark < 0 || mark > maxMarks) {
                        String studentName = (String) tableModel.getValueAt(row, 1);
                        JOptionPane.showMessageDialog(this, 
                            "Invalid mark for " + studentName + ": " + mark + 
                            "\nMark must be between 0 and " + maxMarks, 
                            "Invalid Mark", 
                            JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                } catch (NumberFormatException e) {
                    String studentName = (String) tableModel.getValueAt(row, 1);
                    JOptionPane.showMessageDialog(this, 
                        "Invalid mark format for " + studentName + ": " + value, 
                        "Invalid Format", 
                        JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }
        
        return true;
    }
    
    private void saveFlexibleComponentMarks(int componentId) {
        JDialog progressDialog = createProgressDialog("Saving component marks...");
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            private String errorMessage = null;
            private int savedCount = 0;
            
            @Override
            protected Boolean doInBackground() throws Exception {
                Connection conn = null;
                PreparedStatement ps = null;
                
                try {
                    conn = DatabaseConnection.getConnection();
                    conn.setAutoCommit(false);
                    
                    // Delete existing marks
                    String deleteQuery = "DELETE cm FROM component_marks cm " +
                                       "JOIN students s ON cm.student_id = s.id " +
                                       "WHERE cm.component_id = ? AND s.section_id = ?";
                    ps = conn.prepareStatement(deleteQuery);
                    ps.setInt(1, componentId);
                    ps.setInt(2, currentSectionId);
                    ps.executeUpdate();
                    ps.close();
                    
                    // Insert new marks
                    String insertQuery = "INSERT INTO component_marks " +
                                       "(student_id, component_id, marks_obtained, created_by) " +
                                       "VALUES (?, ?, ?, ?)";
                    ps = conn.prepareStatement(insertQuery);
                    
                    for (int row = 0; row < tableModel.getRowCount(); row++) {
                        String rollNumber = (String) tableModel.getValueAt(row, 0);
                        Integer studentId = studentIdMap.get(rollNumber);
                        
                        if (studentId == null) continue;
                        
                        Object valueObj = tableModel.getValueAt(row, 2);
                        String value = valueObj != null ? valueObj.toString().trim() : "";
                        
                        if (!value.isEmpty()) {
                            ps.setInt(1, studentId);
                            ps.setInt(2, componentId);
                            
                            if (value.equalsIgnoreCase("ABS")) {
                                ps.setInt(3, 0);
                            } else {
                                ps.setInt(3, Integer.parseInt(value));
                            }
                            
                            ps.setInt(4, currentUserId);
                            ps.addBatch();
                            savedCount++;
                        }
                    }
                    
                    ps.executeBatch();
                    conn.commit();
                    return true;
                    
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    e.printStackTrace();
                    if (conn != null) {
                        try {
                            conn.rollback();
                        } catch (SQLException ex) 
                        {
                            ex.printStackTrace();
                        }
                    }
                    return false;
                } finally {
                    if (ps != null) try { ps.close(); } catch (SQLException e) { }
                    if (conn != null) {
                        try {
                            conn.setAutoCommit(true);
                            conn.close();
                        } catch (SQLException e) { }
                    }
                }
            }
            
            @Override
            protected void done() {
                progressDialog.dispose();
                
                try {
                    boolean success = get();
                    if (success) {
                        String timestamp = new SimpleDateFormat("HH:mm").format(new Date());
                        lastSavedLabel.setText(timestamp);
                        
                        showSuccessNotification("Successfully saved " + savedCount + " component marks!");
                        
                        statusLabel.setText("Component marks saved successfully at " + timestamp);
                        statusLabel.setForeground(primaryGreen);
                    } else {
                        JOptionPane.showMessageDialog(MarkEntryDialog.this, 
                            "Error saving marks: " + (errorMessage != null ? errorMessage : "Unknown error"), 
                            "Error", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(MarkEntryDialog.this, 
                        "Error: " + e.getMessage(), 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        worker.execute();
        progressDialog.setVisible(true);
    }
    
    private void saveTraditionalMarks(String examType) {
        JDialog progressDialog = createProgressDialog("Saving marks...");
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            private String errorMessage = null;
            private int savedCount = 0;
            
            @Override
            protected Boolean doInBackground() throws Exception {
                Connection conn = null;
                PreparedStatement ps = null;
                
                try {
                    conn = DatabaseConnection.getConnection();
                    conn.setAutoCommit(false);
                    
                    // Delete existing marks
                    String deleteQuery = "DELETE sm FROM student_marks sm " +
                                       "JOIN students s ON sm.student_id = s.id " +
                                       "WHERE sm.exam_type = ? AND s.section_id = ? " +
                                       "AND sm.subject_id = ?";
                    ps = conn.prepareStatement(deleteQuery);
                    ps.setString(1, examType);
                    ps.setInt(2, currentSectionId);
                    ps.setInt(3, currentSubjectId);
                    ps.executeUpdate();
                    ps.close();
                    
                    // Insert new marks
                    String insertQuery = "INSERT INTO student_marks " +
                                       "(student_id, subject_id, marks_obtained, exam_type, " +
                                       "academic_year, created_by) VALUES (?, ?, ?, ?, ?, ?)";
                    ps = conn.prepareStatement(insertQuery);
                    
                    String currentYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
                    
                    for (int row = 0; row < tableModel.getRowCount(); row++) {
                        String rollNumber = (String) tableModel.getValueAt(row, 0);
                        Integer studentId = studentIdMap.get(rollNumber);
                        
                        if (studentId == null) continue;
                        
                        Object valueObj = tableModel.getValueAt(row, 2);
                        String value = valueObj != null ? valueObj.toString().trim() : "";
                        
                        if (!value.isEmpty()) {
                            ps.setInt(1, studentId);
                            ps.setInt(2, currentSubjectId);
                            
                            if (value.equalsIgnoreCase("ABS")) {
                                ps.setInt(3, 0);
                            } else {
                                ps.setInt(3, Integer.parseInt(value));
                            }
                            
                            ps.setString(4, examType);
                            ps.setString(5, currentYear);
                            ps.setInt(6, currentUserId);
                            ps.addBatch();
                            savedCount++;
                        }
                    }
                    
                    ps.executeBatch();
                    conn.commit();
                    return true;
                    
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    e.printStackTrace();
                    if (conn != null) {
                        try {
                            conn.rollback();
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    }
                    return false;
                } finally {
                    if (ps != null) try { ps.close(); } catch (SQLException e) { }
                    if (conn != null) {
                        try {
                            conn.setAutoCommit(true);
                            conn.close();
                        } catch (SQLException e) { }
                    }
                }
            }
            
            @Override
            protected void done() {
                progressDialog.dispose();
                
                try {
                    boolean success = get();
                    if (success) {
                        String timestamp = new SimpleDateFormat("HH:mm").format(new Date());
                        lastSavedLabel.setText(timestamp);
                        
                        showSuccessNotification("Successfully saved " + savedCount + " marks!");
                        
                        statusLabel.setText("Marks saved successfully at " + timestamp);
                        statusLabel.setForeground(primaryGreen);
                    } else {
                        JOptionPane.showMessageDialog(MarkEntryDialog.this, 
                            "Error saving marks: " + (errorMessage != null ? errorMessage : "Unknown error"), 
                            "Error", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(MarkEntryDialog.this, 
                        "Error: " + e.getMessage(), 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        worker.execute();
        progressDialog.setVisible(true);
    }
    
    // UI Component Creation Methods
    private JButton createPrimaryButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 15));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.darker());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }
    
    private JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 15));
        button.setForeground(textSecondary);
        button.setBackground(cardBackground);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 1),
            BorderFactory.createEmptyBorder(11, 23, 11, 23)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        
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
    
    private JButton createIconButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        button.setForeground(textPrimary);
        button.setBackground(cardBackground);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 1),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        button.setToolTipText(tooltip);
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverColor);
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(primaryBlue, 1),
                    BorderFactory.createEmptyBorder(8, 16, 8, 16)
                ));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(cardBackground);
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(borderColor, 1),
                    BorderFactory.createEmptyBorder(8, 16, 8, 16)
                ));
            }
        });
        
        return button;
    }
    
    private JButton createSmallButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
        button.setForeground(color);
        button.setBackground(cardBackground);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(color);
                button.setForeground(Color.WHITE);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(cardBackground);
                button.setForeground(color);
            }
        });
        
        return button;
    }
    
    private JDialog createProgressDialog(String message) {
        JDialog dialog = new JDialog(this, "Processing", true);
        dialog.setSize(300, 120);
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(cardBackground);
        
        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        panel.add(messageLabel);
        panel.add(Box.createVerticalStrut(15));
        panel.add(progressBar);
        
        dialog.add(panel);
        return dialog;
    }
    
    private void showSuccessNotification(String message) {
        // Create a temporary notification
        JWindow notification = new JWindow(this);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(primaryGreen);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        
        JLabel label = new JLabel("✓ " + message);
        label.setForeground(Color.WHITE);
        label.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        
        panel.add(label);
        notification.add(panel);
        notification.pack();
        
        // Position at top center of dialog
        Point dialogLocation = getLocationOnScreen();
        int x = dialogLocation.x + (getWidth() - notification.getWidth()) / 2;
        int y = dialogLocation.y + 100;
        notification.setLocation(x, y);
        
        notification.setVisible(true);
        
        // Auto-hide after 3 seconds
        Timer timer = new Timer(3000, e -> notification.dispose());
        timer.setRepeats(false);
        timer.start();
    }
    
    // Import/Export Methods (simplified for brevity)
    private void showImportDialog() {
        JOptionPane.showMessageDialog(this, 
            "Import functionality will be implemented based on the new structure", 
            "Import", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showExportDialog() {
        JOptionPane.showMessageDialog(this, 
            "Export functionality will be implemented based on the new structure", 
            "Export", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showFormatHelp() {
        JOptionPane.showMessageDialog(this, 
            "Help documentation will be updated for the new structure", 
            "Help", 
            JOptionPane.INFORMATION_MESSAGE);
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
            
            MarkEntryDialog dialog = new MarkEntryDialog(frame);
            dialog.setVisible(true);
        });
    }
}