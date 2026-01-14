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

/**
 * Mark Entry Dialog for entering student marks using WEIGHTED GRADING SYSTEM
 * 
 * WEIGHTED MARKS SYSTEM (Current Implementation):
 * ================================================
 * Each exam component has a WEIGHTAGE that determines its contribution to the subject total of 100.
 * Marks are entered DIRECTLY out of weightage (no scaling).
 * 
 * HOW IT WORKS:
 * - Weightage % = Max marks for that component (e.g., 20% = enter 0-20 marks)
 * - Subject Total = Σ(marks obtained in each component) out of 100
 * - All component weightages MUST sum to 100%
 * 
 * PASSING CRITERIA (DUAL REQUIREMENT):
 * - Student must score >= passing marks in EACH component (component-level pass)
 * - Student must score >= subject passing total (subject-level pass)
 * - Failing ANY component = FAIL (even if total is passing)
 * 
 * REALISTIC EXAMPLE (CLOUD COMPUTING):
 * Components:
 *   - Internal 1 (Weightage: 20%, Pass: 8)  → Enter 0-20 marks
 *   - Internal 2 (Weightage: 25%, Pass: 10) → Enter 0-25 marks
 *   - Internal 3 (Weightage: 15%, Pass: 6)  → Enter 0-15 marks
 *   - Final Exam (Weightage: 40%, Pass: 16) → Enter 0-40 marks
 * 
 * Student scores: 16, 20, 12, 32
 * Subject Total = 16 + 20 + 12 + 32 = 80/100 ✅ PASS
 * (All components passed: 16>=8, 20>=10, 12>=6, 32>=16)
 * 
 * Student scores: 5, 20, 12, 35
 * Subject Total = 5 + 20 + 12 + 35 = 72/100 ❌ FAIL
 * (Failed Internal 1: 5 < 8, even though total is 72)
 * 
 * See WEIGHTED_CALCULATION_SYSTEM.md for complete documentation.
 */
public class MarkEntryDialog extends JPanel {
    private JFrame parentFrame;
    private Runnable onCloseCallback;
    
    private JComboBox<String> sectionDropdown;
    private JComboBox<String> subjectDropdown;
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
    private Map<String, Integer> studentIdMap;
    
    // Store exam types with their max marks
    private List<ExamTypeInfo> examTypes;
    private boolean isCalculating = false; // Flag to prevent infinite recursion in calculateRowTotal
    private boolean isLoadingData = false; // Flag to prevent auto-save during initial data load
    
    // Inner class to store exam type information
    private static class ExamTypeInfo {
        int id;
        String name;
        int maxMarks;
        int weightage;
        int passingMarks;
        
        ExamTypeInfo(int id, String name, int maxMarks, int weightage, int passingMarks) {
            this.id = id;
            this.name = name;
            this.maxMarks = maxMarks;
            this.weightage = weightage;
            this.passingMarks = passingMarks;
        }
    }
    
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
        this(parent, null);
    }
    
    public MarkEntryDialog(JFrame parent, Runnable onCloseCallback) {
        this.parentFrame = parent;
        this.onCloseCallback = onCloseCallback;
        this.currentUserId = com.sms.login.LoginScreen.currentUserId;
        this.themeManager = ThemeManager.getInstance();
        
        setLayout(new BorderLayout());
        setBackground(backgroundColor);
        
        initializeMaps();
        initializeUI();
        loadSections();
    }
    
    private void initializeMaps() {
        sectionIdMap = new HashMap<>();
        subjectIdMap = new HashMap<>();
        studentIdMap = new HashMap<>();
        examTypes = new ArrayList<>();
    }
    
    private void initializeUI() {
        setBackground(backgroundColor);
        
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
        
        // Add back button if callback present
        if (onCloseCallback != null) {
            JButton backButton = new JButton("← Back");
            backButton.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
            backButton.setForeground(primaryBlue);
            backButton.setBackground(cardBackground);
            backButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
            backButton.setFocusPainted(false);
            backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            backButton.addActionListener(e -> closePanel());
            titleSection.add(backButton, BorderLayout.WEST);
        }
        
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
        
        // Subject Selection
        JPanel subjectPanel = createSelectionGroup("Subject", createSubjectDropdown());
        
        selectionPanel.add(sectionPanel);
        selectionPanel.add(subjectPanel);
        
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
                
                // Color code marks columns (column 2 onwards for exam types)
                if (column >= 2 && column < 2 + examTypes.size() && value != null && !value.toString().isEmpty()) {
                    try {
                        int mark = Integer.parseInt(value.toString());
                        int examIndex = column - 2;
                        ExamTypeInfo examInfo = examTypes.get(examIndex);
                        int passingMark = examInfo.passingMarks;
                        int maxMark = examInfo.maxMarks;
                        
                        // Color based on component passing marks
                        if (mark < passingMark) {
                            c.setBackground(new Color(254, 226, 226)); // Light red - FAIL
                            setForeground(new Color(153, 27, 27));
                        } else if (mark >= (maxMark * 0.8)) {  // 80% or above
                            c.setBackground(new Color(220, 252, 231)); // Light green - Excellent
                            setForeground(new Color(22, 101, 52));
                        } else {
                            c.setBackground(cardBackground);
                            setForeground(textPrimary);
                        }
                    } catch (NumberFormatException e) {
                        if (value.toString().equalsIgnoreCase("ABS")) {
                            c.setBackground(new Color(254, 243, 199)); // Light yellow - Absent
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
        
        // Custom cell editor will be set in buildDynamicTable() method
        // marksTable.setDefaultEditor(Object.class, createMarksEditor());  // OLD - commented out
        
        JScrollPane scrollPane = new JScrollPane(marksTable);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(cardBackground);
        
        // Enable horizontal and vertical scrolling
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        marksTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // Allow horizontal scrolling
        
        // Status bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(hoverColor);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));
        
        statusLabel = new JLabel("Select section and subject, then click Load Marks Grid");
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
    
    // OLD EDITOR - No longer used with grid design (custom editor created in buildDynamicTable)
    /*
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
    */
    
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
        cancelButton.addActionListener(e -> closePanel());
        
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
    // OLD DEBUG METHOD - Commented out after grid redesign
    /*
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
    */


    // OLD METHOD - Commented out after grid redesign
    /*
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
    */
    // OLD METHOD - Commented out after grid redesign
    /*
    private void loadExamTypes() {
        System.out.println("\n=== loadExamTypes called ===");
        System.out.println("currentSectionId: " + currentSectionId);
        System.out.println("currentSubjectId: " + currentSubjectId);
        
        examTypeDropdown.removeAllItems();
        examTypeIdMap.clear();
        examTypeDropdown.addItem("Select Exam Type");
        
        if (currentSectionId == null || currentSubjectId == null) {
            System.out.println("Section or Subject ID is null, keeping dropdown disabled");
            examTypeDropdown.setEnabled(false);
            return;
        }
        
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
            
            System.out.println("Is flexible marking: " + isFlexible);
            
            if (isFlexible) {
                // Load components from marking scheme
                loadFlexibleComponents(conn);
            } else {
                // Load traditional exam types
                loadTraditionalExamTypes(conn);
            }
            
        } catch (SQLException e) {
            System.err.println("Error loading exam types: " + e.getMessage());
            e.printStackTrace();
            examTypeDropdown.setEnabled(false);
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
                    System.out.println("Added flexible component: " + componentName + " (ID: " + componentId + ")");
                }
                
                System.out.println("Total flexible components loaded: " + count);
                boolean shouldEnable = count > 0;
                examTypeDropdown.setEnabled(shouldEnable);
                System.out.println("Flexible exam type dropdown enabled: " + shouldEnable);
                
                if (shouldEnable) {
                    // Force UI update
                    examTypeDropdown.revalidate();
                    examTypeDropdown.repaint();
                }
            }
        }
    }

    private void loadTraditionalExamTypes(Connection conn) throws SQLException {
        // Load exam types for the specific section and subject combination
        String query = "SELECT DISTINCT et.id, et.exam_name " +
                       "FROM exam_types et " +
                       "INNER JOIN subject_exam_types set_table ON et.id = set_table.exam_type_id " +
                       "WHERE set_table.section_id = ? AND set_table.subject_id = ? " +
                       "ORDER BY et.id";
        
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, currentSectionId);
            ps.setInt(2, currentSubjectId);
            
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    int examId = rs.getInt("id");
                    String examName = rs.getString("exam_name");
                    
                    examTypeDropdown.addItem(examName);
                    examTypeIdMap.put(examName, examId);
                    System.out.println("Added exam type: " + examName + " (ID: " + examId + ")");
                }
                
                System.out.println("Total exam types loaded: " + count);
                boolean shouldEnable = count > 0;
                examTypeDropdown.setEnabled(shouldEnable);
                System.out.println("Exam type dropdown enabled: " + shouldEnable);
                
                if (shouldEnable) {
                    // Force UI update
                    examTypeDropdown.revalidate();
                    examTypeDropdown.repaint();
                }
            }
        }
    }
    */



// Subject dropdown now only loads grid data when button clicked
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
            
            // Auto-load marks grid when subject is selected
            loadMarksGrid();
        }
    });
    
    subjectDropdown.setRenderer(createComboBoxRenderer());
    
    return subjectDropdown;
}

// Debug method - no longer needed but kept for reference
    /*
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
    */
    // Also update the loadSubjects method to properly get subject IDs
    private void loadSubjects() {
        subjectDropdown.removeAllItems();
        subjectIdMap.clear();
        examTypes.clear();
        
        if (currentSectionId == null) {
            subjectDropdown.setEnabled(false);
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
    
    // OLD METHOD - Commented out after grid redesign
    /*
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
    */
  
    private void loadMarksGrid() {
        if (currentSectionId == null || currentSubjectId == null) {
            statusLabel.setText("Please select both section and subject");
            statusLabel.setForeground(primaryRed);
            return;
        }
        
        // Disable auto-save during data loading
        isLoadingData = true;
        
        // Load all exam types for this section and subject
        loadExamTypesForGrid();
        
        if (examTypes.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No exam types configured for this section and subject.\nPlease configure exam patterns when creating the section.",
                "No Exam Types", JOptionPane.WARNING_MESSAGE);
            statusLabel.setText("No exam types configured");
            statusLabel.setForeground(primaryOrange);
            isLoadingData = false;
            return;
        }
        
        // Build dynamic table columns
        buildDynamicTable();
        
        // Load students and their marks
        loadStudentsWithAllMarks();
        
        // Re-enable auto-save after loading complete
        isLoadingData = false;
    }
    
    private void loadExamTypesForGrid() {
        examTypes.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check if flexible marking
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
                // Load flexible components
                String query = "SELECT c.id, c.component_name, c.max_marks " +
                             "FROM marking_components c " +
                             "JOIN marking_schemes ms ON c.scheme_id = ms.id " +
                             "WHERE ms.section_id = ? AND ms.subject_id = ? " +
                             "ORDER BY c.sequence_order";
                
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setInt(1, currentSectionId);
                    ps.setInt(2, currentSubjectId);
                    
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            examTypes.add(new ExamTypeInfo(
                                rs.getInt("id"),
                                rs.getString("component_name"),
                                rs.getInt("max_marks"),
                                100,  // weightage (flexible components contribute full marks)
                                0     // passingMarks (flexible system may not have passing marks)
                            ));
                        }
                    }
                }
            } else {
                // Load exam types with SCALED SYSTEM (Option B: max_marks ≠ weightage)
                // Check if max_marks column exists
                boolean hasMaxMarksColumn = false;
                try {
                    DatabaseMetaData dbMetaData = conn.getMetaData();
                    ResultSet columns = dbMetaData.getColumns(null, null, "exam_types", "max_marks");
                    hasMaxMarksColumn = columns.next();
                    columns.close();
                } catch (SQLException e) {
                    System.out.println("[WARN] Could not check for max_marks column: " + e.getMessage());
                }
                
                String query;
                if (hasMaxMarksColumn) {
                    // Option B: Scaled system (max_marks separate from weightage)
                    query = "SELECT DISTINCT et.id, et.exam_name, et.max_marks, et.weightage, et.passing_marks " +
                           "FROM exam_types et " +
                           "INNER JOIN subject_exam_types set_table ON et.id = set_table.exam_type_id " +
                           "WHERE set_table.section_id = ? AND set_table.subject_id = ? " +
                           "ORDER BY et.id";
                } else {
                    // Option A: Direct entry (weightage = max_marks)
                    query = "SELECT DISTINCT et.id, et.exam_name, et.weightage, et.passing_marks " +
                           "FROM exam_types et " +
                           "INNER JOIN subject_exam_types set_table ON et.id = set_table.exam_type_id " +
                           "WHERE set_table.section_id = ? AND set_table.subject_id = ? " +
                           "ORDER BY et.id";
                }
                
                System.out.println("\n=== Loading Exam Types for Mark Entry ===");
                if (hasMaxMarksColumn) {
                    System.out.println("Mode: SCALED SYSTEM (Option B)");
                    System.out.println("Enter marks out of max_marks, system will scale by weightage");
                } else {
                    System.out.println("Mode: DIRECT ENTRY (Option A)");
                    System.out.println("Weightage = Max marks (contribution to 100)");
                }
                
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setInt(1, currentSectionId);
                    ps.setInt(2, currentSubjectId);
                    
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int examId = rs.getInt("id");
                            String examName = rs.getString("exam_name");
                            int maxMarks;
                            int weightage;
                            int passingMarks = rs.getInt("passing_marks");
                            
                            if (hasMaxMarksColumn) {
                                maxMarks = rs.getInt("max_marks");
                                weightage = rs.getInt("weightage");
                                if (maxMarks == 0) maxMarks = weightage; // Fallback
                                System.out.println("  " + examName + ": Enter marks out of " + maxMarks + " (Contributes " + weightage + "%, Pass: " + passingMarks + ")");
                            } else {
                                weightage = rs.getInt("weightage");
                                maxMarks = weightage; // Option A: weightage = max_marks
                                System.out.println("  " + examName + ": Enter marks out of " + maxMarks + " (Pass: " + passingMarks + ")");
                            }
                            
                            examTypes.add(new ExamTypeInfo(
                                examId,
                                examName,
                                maxMarks,
                                weightage,
                                passingMarks
                            ));
                        }
                    }
                }
                System.out.println("==============================\n");
            }
            
            // FALLBACK: If no exam types configured but marks exist, auto-detect from student_marks table
            if (examTypes.isEmpty()) {
                String fallbackQuery = "SELECT DISTINCT et.id, et.exam_name, et.weightage, MAX(sm.marks_obtained) as max_marks_found " +
                                     "FROM student_marks sm " +
                                     "JOIN exam_types et ON sm.exam_type_id = et.id " +
                                     "WHERE sm.subject_id = ? " +
                                     "GROUP BY et.id, et.exam_name, et.weightage " +
                                     "ORDER BY et.id";
                
                try (PreparedStatement ps = conn.prepareStatement(fallbackQuery)) {
                    ps.setInt(1, currentSubjectId);
                    
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int examId = rs.getInt("id");
                            String examTypeName = rs.getString("exam_name");
                            int weightage = rs.getInt("weightage");
                            int maxMarksFound = rs.getInt("max_marks_found");
                            
                            // Get max_marks from section_subjects if available
                            int maxMarks = 100; // Default
                            String maxMarksQuery = "SELECT max_marks FROM section_subjects " +
                                                 "WHERE section_id = ? AND subject_id = ?";
                            try (PreparedStatement psMax = conn.prepareStatement(maxMarksQuery)) {
                                psMax.setInt(1, currentSectionId);
                                psMax.setInt(2, currentSubjectId);
                                try (ResultSet rsMax = psMax.executeQuery()) {
                                    if (rsMax.next()) {
                                        int sectionMaxMarks = rsMax.getInt("max_marks");
                                        if (sectionMaxMarks > 0) {
                                            maxMarks = sectionMaxMarks;
                                        }
                                    }
                                }
                            }
                            
                            examTypes.add(new ExamTypeInfo(
                                examId,  // Use actual ID from exam_types table
                                examTypeName,
                                Math.max(weightage, maxMarks),  // Use weightage or maxMarks, whichever is higher
                                weightage,  // weightage
                                0  // passingMarks (unknown in fallback)
                            ));
                        }
                    }
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading exam types: " + e.getMessage());
        }
    }
    
    private void buildDynamicTable() {
        // Create column names: Roll No, Student Name, ExamType1(max), ExamType2(max), ..., Total, Status
        List<String> columnNames = new ArrayList<>();
        columnNames.add("Roll No");
        columnNames.add("Student Name");
        
        for (ExamTypeInfo exam : examTypes) {
            columnNames.add(exam.name + " (" + exam.maxMarks + ")");
        }
        
        columnNames.add("Total");
        columnNames.add("Status");
        
        // Create new table model
        tableModel = new DefaultTableModel(columnNames.toArray(new String[0]), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Only marks columns are editable (not Roll No, Name, Total, Status)
                return column >= 2 && column < getColumnCount() - 2;
            }
        };
        
        marksTable.setModel(tableModel);
        
        // Set column widths - auto-calculate based on content
        marksTable.getColumnModel().getColumn(0).setPreferredWidth(100);  // Roll No
        marksTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Student Name
        
        // Auto-size exam columns based on header text length
        for (int i = 2; i < examTypes.size() + 2; i++) {
            ExamTypeInfo examInfo = examTypes.get(i - 2);
            String headerText = examInfo.name + " (" + examInfo.maxMarks + ")";
            
            // Calculate width based on text length: ~10 pixels per character, minimum 150px
            int calculatedWidth = Math.max(150, headerText.length() * 10);
            marksTable.getColumnModel().getColumn(i).setPreferredWidth(calculatedWidth);
            marksTable.getColumnModel().getColumn(i).setMinWidth(150);  // Minimum width
        }
        
        marksTable.getColumnModel().getColumn(examTypes.size() + 2).setPreferredWidth(100);  // Total
        marksTable.getColumnModel().getColumn(examTypes.size() + 3).setPreferredWidth(120); // Status
        
        // Add custom cell editor for validation
        for (int i = 0; i < examTypes.size(); i++) {
            final int examIndex = i;
            final int maxMarks = examTypes.get(i).maxMarks;
            
            marksTable.getColumnModel().getColumn(i + 2).setCellEditor(new DefaultCellEditor(new JTextField()) {
                @Override
                public boolean stopCellEditing() {
                    String value = (String) getCellEditorValue();
                    if (value.trim().isEmpty()) {
                        return super.stopCellEditing();
                    }
                    
                    try {
                        double marks = Double.parseDouble(value);
                        if (marks < 0 || marks > maxMarks) {
                            JOptionPane.showMessageDialog(marksTable, 
                                "Marks must be between 0 and " + maxMarks,
                                "Invalid Marks", JOptionPane.ERROR_MESSAGE);
                            return false;
                        }
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(marksTable, 
                            "Please enter a valid number",
                            "Invalid Input", JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                    
                    return super.stopCellEditing();
                }
            });
        }
        
        // Add table model listener to calculate totals and auto-save (only for exam columns)
        tableModel.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int column = e.getColumn();
                
                // Only trigger calculation for exam columns (between Student Name and Total)
                // Skip if updating Total or Status columns to prevent infinite loop
                if (row >= 0 && column >= 2 && column < examTypes.size() + 2) {
                    calculateRowTotal(row);
                    
                    // Auto-save the mark that was just entered
                    autoSaveMark(row, column);
                }
            }
        });
    }
    
    private void loadStudentsWithAllMarks() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT s.id, s.roll_number, s.student_name " +
                          "FROM students s " +
                          "WHERE s.section_id = ? " +
                          "ORDER BY s.roll_number";
            
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setInt(1, currentSectionId);
                
                try (ResultSet rs = ps.executeQuery()) {
                    studentIdMap.clear();
                    
                    while (rs.next()) {
                        int studentId = rs.getInt("id");
                        String rollNumber = rs.getString("roll_number");
                        String studentName = rs.getString("student_name");
                        
                        studentIdMap.put(rollNumber, studentId);
                        
                        // Create row with empty marks
                        Object[] row = new Object[examTypes.size() + 4];
                        row[0] = rollNumber;
                        row[1] = studentName;
                        
                        // Initialize marks columns as empty
                        for (int i = 0; i < examTypes.size(); i++) {
                            row[i + 2] = "";
                        }
                        
                        row[examTypes.size() + 2] = ""; // Total
                        row[examTypes.size() + 3] = "Incomplete"; // Status
                        
                        tableModel.addRow(row);
                    }
                    
                    // Load existing marks
                    loadExistingMarksForAllExams();
                    
                    // Enable buttons
                    saveButton.setEnabled(true);
                    importButton.setEnabled(true);
                    exportButton.setEnabled(true);
                    
                    statusLabel.setText("Loaded " + tableModel.getRowCount() + " students with " + examTypes.size() + " exam types");
                    statusLabel.setForeground(primaryGreen);
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading students: " + e.getMessage());
        }
    }
    
    private void loadExistingMarksForAllExams() {
        // For each exam type, load existing marks
        for (int examIndex = 0; examIndex < examTypes.size(); examIndex++) {
            ExamTypeInfo exam = examTypes.get(examIndex);
            
            try (Connection conn = DatabaseConnection.getConnection()) {
                // Load marks from student_marks table using exam_type_id FK
                String query = "SELECT s.roll_number, sm.marks_obtained " +
                              "FROM student_marks sm " +
                              "JOIN students s ON sm.student_id = s.id " +
                              "WHERE sm.exam_type_id = ? AND sm.subject_id = ?";
                
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setInt(1, exam.id);  // exam_type_id
                    ps.setInt(2, currentSubjectId);
                    
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String rollNumber = rs.getString("roll_number");
                            int marks = rs.getInt("marks_obtained");
                            
                            // Find student row and set marks
                            for (int row = 0; row < tableModel.getRowCount(); row++) {
                                if (tableModel.getValueAt(row, 0).equals(rollNumber)) {
                                    tableModel.setValueAt(String.valueOf(marks), row, examIndex + 2);
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                // Silently handle - no existing marks is OK
                System.err.println("Info: No existing marks found for exam type: " + exam.name);
            }
        }
        
        // Recalculate all totals
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            calculateRowTotal(row);
        }
    }
    
    private void calculateRowTotal(int row) {
        // Prevent re-entry to avoid infinite recursion
        if (isCalculating) {
            return;
        }
        
        try {
            isCalculating = true;
            
            double total = 0;
            int filledCount = 0;
            
            // Apply SCALED CALCULATION: (marks_obtained / max_marks) × weightage
            for (int i = 0; i < examTypes.size(); i++) {
                Object value = tableModel.getValueAt(row, i + 2);
                if (value != null && !value.toString().trim().isEmpty()) {
                    try {
                        double marksObtained = Double.parseDouble(value.toString());
                        ExamTypeInfo examInfo = examTypes.get(i);
                        
                        // Scaled contribution = (marks_obtained / max_marks) × weightage
                        double contribution = (marksObtained / examInfo.maxMarks) * examInfo.weightage;
                        total += contribution;
                        filledCount++;
                    } catch (NumberFormatException e) {
                        // Ignore invalid values
                    }
                }
            }
            
            // Only show total if all exam types are filled
            if (filledCount == examTypes.size()) {
                tableModel.setValueAt(String.format("%.2f", total), row, examTypes.size() + 2);
                tableModel.setValueAt("Complete", row, examTypes.size() + 3);
            } else {
                tableModel.setValueAt("", row, examTypes.size() + 2);
                tableModel.setValueAt("Incomplete (" + filledCount + "/" + examTypes.size() + ")", row, examTypes.size() + 3);
            }
        } finally {
            isCalculating = false;
        }
    }
    
    private void autoSaveMark(int row, int column) {
        // Skip auto-save during initial data loading
        if (isLoadingData) {
            return;
        }
        
        // Get student info
        String rollNumber = (String) tableModel.getValueAt(row, 0);
        Integer studentId = studentIdMap.get(rollNumber);
        
        if (studentId == null) {
            return;
        }
        
        // Get exam type info (column index - 2 because first 2 columns are Roll No and Name)
        int examIndex = column - 2;
        if (examIndex < 0 || examIndex >= examTypes.size()) {
            return;
        }
        
        ExamTypeInfo exam = examTypes.get(examIndex);
        Object valueObj = tableModel.getValueAt(row, column);
        String value = valueObj != null ? valueObj.toString().trim() : "";
        
        // Save in background thread to avoid blocking UI
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    if (value.isEmpty()) {
                        // Delete mark if value is empty
                        String deleteQuery = "DELETE FROM student_marks WHERE student_id = ? AND exam_type_id = ? AND subject_id = ?";
                        try (PreparedStatement ps = conn.prepareStatement(deleteQuery)) {
                            ps.setInt(1, studentId);
                            ps.setInt(2, exam.id);
                            ps.setInt(3, currentSubjectId);
                            ps.executeUpdate();
                        }
                    } else {
                        // Delete existing mark first (to handle updates)
                        String deleteQuery = "DELETE FROM student_marks WHERE student_id = ? AND exam_type_id = ? AND subject_id = ?";
                        try (PreparedStatement ps = conn.prepareStatement(deleteQuery)) {
                            ps.setInt(1, studentId);
                            ps.setInt(2, exam.id);
                            ps.setInt(3, currentSubjectId);
                            ps.executeUpdate();
                        }
                        
                        // Insert new mark using exam_type_id FK
                        String insertQuery = "INSERT INTO student_marks (student_id, exam_type_id, subject_id, marks_obtained, created_by) VALUES (?, ?, ?, ?, ?)";
                        try (PreparedStatement ps = conn.prepareStatement(insertQuery)) {
                            ps.setInt(1, studentId);
                            ps.setInt(2, exam.id);  // exam_type_id FK
                            ps.setInt(3, currentSubjectId);
                            ps.setInt(4, Integer.parseInt(value));
                            ps.setInt(5, currentUserId);
                            ps.executeUpdate();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Auto-save error: " + e.getMessage());
                    e.printStackTrace();
                }
                return null;
            }
            
            @Override
            protected void done() {
                // Update status bar to show auto-saved
                statusLabel.setText("✓ Auto-saved at " + new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()));
                statusLabel.setForeground(primaryGreen);
            }
        };
        
        worker.execute();
    }
    
    // OLD METHODS - Commented out after grid redesign
    /*
    private void loadStudentsAndMarksOLD() {
        // Full method body removed
    }
    
    private void loadExistingComponentMarks(int componentId) {
        // Full method body removed
    }
    
    private void loadExistingTraditionalMarks(String examType) {
        // Full method body removed
    }
    */
    
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
        if (examTypes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No exam types loaded!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        JDialog progressDialog = createProgressDialog("Saving all marks...");
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            private String errorMessage = null;
            private int savedCount = 0;
            
            @Override
            protected Boolean doInBackground() throws Exception {
                Connection conn = null;
                
                try {
                    conn = DatabaseConnection.getConnection();
                    conn.setAutoCommit(false);
                    
                    // For each row (student)
                    for (int row = 0; row < tableModel.getRowCount(); row++) {
                        String rollNumber = (String) tableModel.getValueAt(row, 0);
                        Integer studentId = studentIdMap.get(rollNumber);
                        
                        if (studentId == null) continue;
                        
                        // For each exam type
                        for (int examIndex = 0; examIndex < examTypes.size(); examIndex++) {
                            ExamTypeInfo exam = examTypes.get(examIndex);
                            Object valueObj = tableModel.getValueAt(row, examIndex + 2);
                            String value = valueObj != null ? valueObj.toString().trim() : "";
                            
                            if (!value.isEmpty()) {
                                if (exam.id < 0) {
                                    // Text-based exam type - use student_marks table
                                    // Delete existing mark
                                    String deleteQuery = "DELETE FROM student_marks WHERE student_id = ? AND exam_type = ? AND subject_id = ?";
                                    try (PreparedStatement ps = conn.prepareStatement(deleteQuery)) {
                                        ps.setInt(1, studentId);
                                        ps.setString(2, exam.name);
                                        ps.setInt(3, currentSubjectId);
                                        ps.executeUpdate();
                                    }
                                    
                                    // Insert new mark
                                    String insertQuery = "INSERT INTO student_marks (student_id, exam_type, subject_id, marks_obtained, created_by) VALUES (?, ?, ?, ?, ?)";
                                    try (PreparedStatement ps = conn.prepareStatement(insertQuery)) {
                                        ps.setInt(1, studentId);
                                        ps.setString(2, exam.name);
                                        ps.setInt(3, currentSubjectId);
                                        ps.setInt(4, (int) Double.parseDouble(value));
                                        ps.setInt(5, currentUserId);
                                        ps.executeUpdate();
                                        savedCount++;
                                    }
                                } else {
                                    // ID-based exam type - use marks table
                                    // Delete existing mark
                                    String deleteQuery = "DELETE FROM marks WHERE student_id = ? AND exam_type_id = ? AND subject_id = ?";
                                    try (PreparedStatement ps = conn.prepareStatement(deleteQuery)) {
                                        ps.setInt(1, studentId);
                                        ps.setInt(2, exam.id);
                                        ps.setInt(3, currentSubjectId);
                                        ps.executeUpdate();
                                    }
                                    
                                    // Insert new mark
                                    String insertQuery = "INSERT INTO marks (student_id, exam_type_id, subject_id, marks, created_by) VALUES (?, ?, ?, ?, ?)";
                                    try (PreparedStatement ps = conn.prepareStatement(insertQuery)) {
                                        ps.setInt(1, studentId);
                                        ps.setInt(2, exam.id);
                                        ps.setInt(3, currentSubjectId);
                                        ps.setDouble(4, Double.parseDouble(value));
                                        ps.setInt(5, currentUserId);
                                        ps.executeUpdate();
                                        savedCount++;
                                    }
                                }
                            }
                        }
                    }
                    
                    conn.commit();
                    return true;
                    
                } catch (Exception e) {
                    if (conn != null) {
                        try {
                            conn.rollback();
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    }
                    errorMessage = e.getMessage();
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
            
            @Override
            protected void done() {
                progressDialog.dispose();
                
                try {
                    if (get()) {
                        statusLabel.setText("✓ Bulk save complete: " + savedCount + " marks saved at " + new SimpleDateFormat("HH:mm:ss").format(new Date()));
                        statusLabel.setForeground(primaryGreen);
                        lastSavedLabel.setText("Last saved: " + new SimpleDateFormat("MMM dd, yyyy HH:mm").format(new Date()));
                        
                        JOptionPane.showMessageDialog(MarkEntryDialog.this, 
                            "Bulk save successful!\nSaved " + savedCount + " marks to database.",
                            "Success", 
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        statusLabel.setText("Error saving marks");
                        statusLabel.setForeground(primaryRed);
                        JOptionPane.showMessageDialog(MarkEntryDialog.this, 
                            "Error saving marks: " + errorMessage,
                            "Error", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        
        worker.execute();
        progressDialog.setVisible(true);
    }
    
    // OLD METHODS - Commented out after grid redesign
    /*
    private void saveMarksOLD() {
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
        // ... rest of method
    }
    
    private void saveTraditionalMarks(String examType) {
        // ... rest of method
    }
    */
    
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
        JDialog dialog = new JDialog(parentFrame, "Processing", true);
        dialog.setSize(300, 120);
        dialog.setLocationRelativeTo(parentFrame);
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
        JWindow notification = new JWindow(parentFrame);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(primaryGreen);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        
        JLabel label = new JLabel("✓ " + message);
        label.setForeground(Color.WHITE);
        label.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        
        panel.add(label);
        notification.add(panel);
        notification.pack();
        
        // Position at top center of parent frame
        Point frameLocation = parentFrame.getLocationOnScreen();
        int x = frameLocation.x + (parentFrame.getWidth() - notification.getWidth()) / 2;
        int y = frameLocation.y + 100;
        notification.setLocation(x, y);
        
        notification.setVisible(true);
        
        // Auto-hide after 3 seconds
        Timer timer = new Timer(3000, e -> notification.dispose());
        timer.setRepeats(false);
        timer.start();
    }
    
    // Import/Export Methods
    private void showImportDialog() {
        if (currentSectionId == null || currentSubjectId == null || examTypes.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please select section and subject first", 
                "Selection Required", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Import Marks from Excel");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Files (*.xlsx, *.xls)", "xlsx", "xls"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            importMarksFromExcel(selectedFile);
        }
    }
    
    private void importMarksFromExcel(File file) {
        JDialog progressDialog = createProgressDialog("Importing marks...");
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            private String errorMessage = null;
            private int importedCount = 0;
            
            @Override
            protected Boolean doInBackground() throws Exception {
                try (FileInputStream fis = new FileInputStream(file)) {
                    Workbook workbook = null;
                    
                    // Determine file type
                    String fileName = file.getName().toLowerCase();
                    if (fileName.endsWith(".xlsx")) {
                        workbook = new XSSFWorkbook(fis);
                    } else if (fileName.endsWith(".xls")) {
                        workbook = new HSSFWorkbook(fis);
                    } else {
                        errorMessage = "Unsupported file format";
                        return false;
                    }
                    
                    Sheet sheet = workbook.getSheetAt(0);
                    
                    // Read header row to match exam types
                    Row headerRow = sheet.getRow(0);
                    if (headerRow == null) {
                        errorMessage = "Invalid Excel format: No header row";
                        return false;
                    }
                    
                    // Map column indices to exam types
                    Map<Integer, Integer> columnToExamIndex = new HashMap<>();
                    for (int col = 2; col < headerRow.getLastCellNum(); col++) {
                        Cell cell = headerRow.getCell(col);
                        if (cell != null) {
                            String headerText = cell.getStringCellValue().trim();
                            // Find matching exam type
                            for (int i = 0; i < examTypes.size(); i++) {
                                if (examTypes.get(i).name.equals(headerText)) {
                                    columnToExamIndex.put(col, i);
                                    break;
                                }
                            }
                        }
                    }
                    
                    // Read data rows
                    for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                        Row dataRow = sheet.getRow(rowIndex);
                        if (dataRow == null) continue;
                        
                        // Get roll number from first column
                        Cell rollCell = dataRow.getCell(0);
                        if (rollCell == null) continue;
                        
                        String rollNumber = getCellValueAsString(rollCell).trim();
                        if (rollNumber.isEmpty()) continue;
                        
                        // Find matching row in table
                        int tableRow = -1;
                        for (int r = 0; r < tableModel.getRowCount(); r++) {
                            if (tableModel.getValueAt(r, 0).equals(rollNumber)) {
                                tableRow = r;
                                break;
                            }
                        }
                        
                        if (tableRow == -1) continue; // Student not found
                        
                        // Import marks for each exam type
                        for (Map.Entry<Integer, Integer> entry : columnToExamIndex.entrySet()) {
                            int col = entry.getKey();
                            int examIndex = entry.getValue();
                            
                            Cell markCell = dataRow.getCell(col);
                            if (markCell != null) {
                                String value = getCellValueAsString(markCell).trim();
                                if (!value.isEmpty()) {
                                    tableModel.setValueAt(value, tableRow, examIndex + 2);
                                    importedCount++;
                                }
                            }
                        }
                    }
                    
                    workbook.close();
                    return true;
                    
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    e.printStackTrace();
                    return false;
                }
            }
            
            @Override
            protected void done() {
                progressDialog.dispose();
                
                try {
                    if (get()) {
                        showSuccessNotification("Imported " + importedCount + " marks successfully!");
                        statusLabel.setText("Imported " + importedCount + " marks");
                        statusLabel.setForeground(primaryGreen);
                    } else {
                        JOptionPane.showMessageDialog(MarkEntryDialog.this, 
                            "Import failed: " + errorMessage, 
                            "Import Error", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        
        worker.execute();
        progressDialog.setVisible(true);
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numValue = cell.getNumericCellValue();
                    if (numValue == (long) numValue) {
                        return String.valueOf((long) numValue);
                    } else {
                        return String.valueOf(numValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "";
        }
    }
    
    private void showExportDialog() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, 
                "No data to export", 
                "Export", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String[] options = {"Excel (.xlsx)", "Excel (.xls)", "PDF", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this,
            "Select export format:",
            "Export Marks",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);
        
        if (choice == 3 || choice == JOptionPane.CLOSED_OPTION) {
            return; // User cancelled
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Marks");
        
        String extension = "";
        String description = "";
        
        switch (choice) {
            case 0:
                extension = "xlsx";
                description = "Excel 2007+ (*.xlsx)";
                break;
            case 1:
                extension = "xls";
                description = "Excel 97-2003 (*.xls)";
                break;
            case 2:
                extension = "pdf";
                description = "PDF Document (*.pdf)";
                break;
        }
        
        fileChooser.setFileFilter(new FileNameExtensionFilter(description, extension));
        
        // Suggest filename
        String sectionName = (String) sectionDropdown.getSelectedItem();
        String subjectName = (String) subjectDropdown.getSelectedItem();
        String suggestedName = "Marks_" + sectionName + "_" + subjectName + "_" + 
                               new SimpleDateFormat("yyyyMMdd").format(new Date()) + "." + extension;
        fileChooser.setSelectedFile(new File(suggestedName));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            // Ensure correct extension
            if (!selectedFile.getName().toLowerCase().endsWith("." + extension)) {
                selectedFile = new File(selectedFile.getAbsolutePath() + "." + extension);
            }
            
            if (choice == 2) {
                exportToPDF(selectedFile);
            } else {
                exportToExcel(selectedFile, choice == 0);
            }
        }
    }
    
    private void exportToExcel(File file, boolean isXlsx) {
        JDialog progressDialog = createProgressDialog("Exporting to Excel...");
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            private String errorMessage = null;
            
            @Override
            protected Boolean doInBackground() throws Exception {
                Workbook workbook = null;
                FileOutputStream fos = null;
                
                try {
                    // Create workbook
                    workbook = isXlsx ? new XSSFWorkbook() : new HSSFWorkbook();
                    Sheet sheet = workbook.createSheet("Marks");
                    
                    // Create styles
                    CellStyle headerStyle = workbook.createCellStyle();
                    Font headerFont = workbook.createFont();
                    headerFont.setBold(true);
                    headerFont.setFontHeightInPoints((short) 12);
                    headerStyle.setFont(headerFont);
                    headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    headerStyle.setBorderBottom(BorderStyle.THIN);
                    headerStyle.setBorderTop(BorderStyle.THIN);
                    headerStyle.setBorderLeft(BorderStyle.THIN);
                    headerStyle.setBorderRight(BorderStyle.THIN);
                    headerStyle.setAlignment(HorizontalAlignment.CENTER);
                    
                    CellStyle dataStyle = workbook.createCellStyle();
                    dataStyle.setBorderBottom(BorderStyle.THIN);
                    dataStyle.setBorderTop(BorderStyle.THIN);
                    dataStyle.setBorderLeft(BorderStyle.THIN);
                    dataStyle.setBorderRight(BorderStyle.THIN);
                    
                    // Create title row
                    Row titleRow = sheet.createRow(0);
                    Cell titleCell = titleRow.createCell(0);
                    titleCell.setCellValue("Mark Entry - " + sectionDropdown.getSelectedItem() + 
                                          " - " + subjectDropdown.getSelectedItem());
                    Font titleFont = workbook.createFont();
                    titleFont.setBold(true);
                    titleFont.setFontHeightInPoints((short) 14);
                    CellStyle titleStyle = workbook.createCellStyle();
                    titleStyle.setFont(titleFont);
                    titleCell.setCellStyle(titleStyle);
                    
                    // Merge title cells
                    sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, tableModel.getColumnCount() - 1));
                    
                    // Create header row
                    Row headerRow = sheet.createRow(2);
                    for (int col = 0; col < tableModel.getColumnCount(); col++) {
                        Cell cell = headerRow.createCell(col);
                        cell.setCellValue(tableModel.getColumnName(col));
                        cell.setCellStyle(headerStyle);
                        
                        // Auto-size columns
                        sheet.setColumnWidth(col, 15 * 256);
                    }
                    
                    // Create data rows
                    for (int row = 0; row < tableModel.getRowCount(); row++) {
                        Row dataRow = sheet.createRow(row + 3);
                        
                        for (int col = 0; col < tableModel.getColumnCount(); col++) {
                            Cell cell = dataRow.createCell(col);
                            Object value = tableModel.getValueAt(row, col);
                            
                            if (value != null) {
                                String strValue = value.toString();
                                
                                // Try to parse as number if possible
                                if (col >= 2 && col < tableModel.getColumnCount() - 2) { // Mark columns
                                    try {
                                        if (!strValue.equalsIgnoreCase("ABS") && !strValue.isEmpty()) {
                                            double numValue = Double.parseDouble(strValue);
                                            cell.setCellValue(numValue);
                                        } else {
                                            cell.setCellValue(strValue);
                                        }
                                    } catch (NumberFormatException e) {
                                        cell.setCellValue(strValue);
                                    }
                                } else {
                                    cell.setCellValue(strValue);
                                }
                            }
                            
                            cell.setCellStyle(dataStyle);
                        }
                    }
                    
                    // Auto-size first two columns
                    sheet.autoSizeColumn(0);
                    sheet.autoSizeColumn(1);
                    
                    // Write to file
                    fos = new FileOutputStream(file);
                    workbook.write(fos);
                    
                    return true;
                    
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    e.printStackTrace();
                    return false;
                } finally {
                    if (fos != null) try { fos.close(); } catch (IOException e) { }
                    if (workbook != null) try { workbook.close(); } catch (IOException e) { }
                }
            }
            
            @Override
            protected void done() {
                progressDialog.dispose();
                
                try {
                    if (get()) {
                        showSuccessNotification("Marks exported successfully!");
                        statusLabel.setText("Exported to: " + file.getName());
                        statusLabel.setForeground(primaryGreen);
                    } else {
                        JOptionPane.showMessageDialog(MarkEntryDialog.this, 
                            "Export failed: " + errorMessage, 
                            "Export Error", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        
        worker.execute();
        progressDialog.setVisible(true);
    }
    
    private void exportToPDF(File file) {
        JDialog progressDialog = createProgressDialog("Exporting to PDF...");
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            private String errorMessage = null;
            
            @Override
            protected Boolean doInBackground() throws Exception {
                Document document = new Document();
                
                try {
                    PdfWriter.getInstance(document, new FileOutputStream(file));
                    document.open();
                    
                    // Add title
                    com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(
                        com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD);
                    Paragraph title = new Paragraph("Mark Entry Report", titleFont);
                    title.setAlignment(Element.ALIGN_CENTER);
                    title.setSpacingAfter(10);
                    document.add(title);
                    
                    // Add info
                    com.itextpdf.text.Font infoFont = new com.itextpdf.text.Font(
                        com.itextpdf.text.Font.FontFamily.HELVETICA, 12);
                    Paragraph info = new Paragraph(
                        "Section: " + sectionDropdown.getSelectedItem() + "\n" +
                        "Subject: " + subjectDropdown.getSelectedItem() + "\n" +
                        "Date: " + new SimpleDateFormat("dd-MM-yyyy HH:mm").format(new Date()),
                        infoFont);
                    info.setSpacingAfter(15);
                    document.add(info);
                    
                    // Create table
                    PdfPTable pdfTable = new PdfPTable(tableModel.getColumnCount());
                    pdfTable.setWidthPercentage(100);
                    
                    // Set column widths
                    float[] columnWidths = new float[tableModel.getColumnCount()];
                    columnWidths[0] = 1.5f; // Roll No
                    columnWidths[1] = 3f;   // Name
                    for (int i = 2; i < tableModel.getColumnCount(); i++) {
                        columnWidths[i] = 1.5f;
                    }
                    pdfTable.setWidths(columnWidths);
                    
                    // Add headers
                    com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(
                        com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD);
                    
                    for (int col = 0; col < tableModel.getColumnCount(); col++) {
                        PdfPCell cell = new PdfPCell(new Phrase(tableModel.getColumnName(col), headerFont));
                        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        cell.setPadding(5);
                        pdfTable.addCell(cell);
                    }
                    
                    // Add data
                    com.itextpdf.text.Font dataFont = new com.itextpdf.text.Font(
                        com.itextpdf.text.Font.FontFamily.HELVETICA, 9);
                    
                    for (int row = 0; row < tableModel.getRowCount(); row++) {
                        for (int col = 0; col < tableModel.getColumnCount(); col++) {
                            Object value = tableModel.getValueAt(row, col);
                            String text = value != null ? value.toString() : "";
                            
                            PdfPCell cell = new PdfPCell(new Phrase(text, dataFont));
                            cell.setHorizontalAlignment(col == 1 ? Element.ALIGN_LEFT : Element.ALIGN_CENTER);
                            cell.setPadding(4);
                            pdfTable.addCell(cell);
                        }
                    }
                    
                    document.add(pdfTable);
                    
                    // Add footer
                    Paragraph footer = new Paragraph("\nGenerated by Academic Analyzer on " + 
                        new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date()), 
                        new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8));
                    footer.setAlignment(Element.ALIGN_CENTER);
                    document.add(footer);
                    
                    return true;
                    
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    e.printStackTrace();
                    return false;
                } finally {
                    if (document.isOpen()) {
                        document.close();
                    }
                }
            }
            
            @Override
            protected void done() {
                progressDialog.dispose();
                
                try {
                    if (get()) {
                        showSuccessNotification("Marks exported to PDF successfully!");
                        statusLabel.setText("Exported to: " + file.getName());
                        statusLabel.setForeground(primaryGreen);
                    } else {
                        JOptionPane.showMessageDialog(MarkEntryDialog.this, 
                            "Export failed: " + errorMessage, 
                            "Export Error", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        
        worker.execute();
        progressDialog.setVisible(true);
    }
    
    private void showFormatHelp() {
        JOptionPane.showMessageDialog(this, 
            "Help documentation will be updated for the new structure", 
            "Help", 
            JOptionPane.INFORMATION_MESSAGE);
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
            
            MarkEntryDialog dialog = new MarkEntryDialog(frame);
            dialog.setVisible(true);
        });
    }
}