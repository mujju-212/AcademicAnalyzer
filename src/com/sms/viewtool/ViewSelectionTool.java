package com.sms.viewtool;

import com.sms.analyzer.Student;
import com.sms.dao.AnalyzerDAO;
import com.sms.dao.SectionDAO;
import com.sms.dao.StudentDAO;
import com.sms.database.DatabaseConnection;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import java.awt.*;
import java.awt.Color;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.awt.Desktop;
import java.awt.Toolkit;

import org.apache.poi.hpsf.Date;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

public class ViewSelectionTool extends JPanel {

    private JFrame parentFrame;
    private Runnable onCloseCallback;
    
    // Custom rounded panel class
    static class RoundedPanel extends JPanel {
        private int cornerRadius;
        private int shadowSize = 5;
        
        public RoundedPanel(int radius) {
            this.cornerRadius = radius;
            setOpaque(false);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw shadow
            for (int i = 0; i < shadowSize; i++) {
                g2.setColor(new Color(0, 0, 0, 20 - (i * 4)));
                g2.fill(new RoundRectangle2D.Float(i, i, getWidth() - (i * 2), getHeight() - (i * 2), cornerRadius, cornerRadius));
            }
            
            // Draw white background
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Float(shadowSize, shadowSize, getWidth() - (shadowSize * 2), getHeight() - (shadowSize * 2), cornerRadius, cornerRadius));
        }
    }
    
    // Custom rounded button
    static class RoundedButton extends JButton {
        private int radius;
        private Color backgroundColor;
        private Color hoverColor;
        private boolean isHovered = false;
        
        public RoundedButton(String text, int radius, Color bgColor, Color hoverColor) {
            super(text);
            this.radius = radius;
            this.backgroundColor = bgColor;
            this.hoverColor = hoverColor;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2.setColor(isHovered ? hoverColor : backgroundColor);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
            
            FontMetrics fm = g2.getFontMetrics();
            Rectangle2D r = fm.getStringBounds(getText(), g2);
            int x = (getWidth() - (int) r.getWidth()) / 2;
            int y = (getHeight() - (int) r.getHeight()) / 2 + fm.getAscent();
            
            g2.setColor(getForeground());
            g2.drawString(getText(), x, y);
        }
    }
    
    // Custom styled checkbox
    static class StyledCheckBox extends JCheckBox {
        public StyledCheckBox(String text, boolean selected) {
            super(text, selected);
            setOpaque(false);
            setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
            setForeground(new Color(17, 24, 39)); // TEXT_PRIMARY
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
    }

    private HashMap<String, List<Student>> sectionStudents;
    private JComboBox<String> sectionDropdown;
    private JPanel subjectCheckboxPanel;
    private JCheckBox nameCheckBox, rollNumberCheckBox, emailCheckBox, phoneCheckBox, 
                      sectionCheckBox, yearCheckBox, semesterCheckBox, totalMarksCheckBox, percentageCheckBox, 
                      sgpaCheckBox, gradeCheckBox, statusCheckBox, rankCheckBox,
                      failedSubjectsCheckBox;
    // Launched Results components
    private JComboBox<String> launchedResultsDropdown;
    private JCheckBox showStudentsCheckBox, showSubjectsCheckBox, showComponentsCheckBox;
    private Map<String, Set<String>> selectedFilters; // Subject -> Set of exam types
    private RoundedButton showButton, exportExcelButton, exportPdfButton, printButton;
    private JTable resultTable;
    private JScrollPane tableScrollPane;
    private Map<Integer, SectionInfo> sectionInfoMap;
    private List<ColumnGroup> columnGroups; // For 2-row headers
    
    // Inner class for column grouping
    private static class ColumnGroup {
        String groupName;
        int startColumn;
        int columnCount;
        
        ColumnGroup(String groupName, int startColumn, int columnCount) {
            this.groupName = groupName;
            this.startColumn = startColumn;
            this.columnCount = columnCount;
        }
    }

    public ViewSelectionTool(JFrame parent, HashMap<String, List<Student>> sectionStudents) {
        this(parent, sectionStudents, null);
    }
    
    public ViewSelectionTool(JFrame parent, HashMap<String, List<Student>> sectionStudents, Runnable onCloseCallback) {
        this.parentFrame = parent;
        this.onCloseCallback = onCloseCallback;
        this.sectionStudents = (sectionStudents != null) ? sectionStudents : new HashMap<>();
        this.selectedFilters = new HashMap<>();
        this.sectionInfoMap = new HashMap<>();
        this.columnGroups = new ArrayList<>();
        
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));
        
        initializeUI();
        
        // Load launched results when tool opens
        loadLaunchedResultsDropdown();
    }
    

    private void initializeUI() {
        // Main container with desktop app background (no gradient)
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(new Color(248, 250, 252)); // BACKGROUND_COLOR from desktop
        mainContainer.setOpaque(true);
        
        // TOP HEADER with title and back button
        JPanel headerPanel = createHeaderPanel();
        mainContainer.add(headerPanel, BorderLayout.NORTH);
        
        // LEFT SIDEBAR for filters (like StudentAnalyzer)
        JPanel leftSidebar = createFilterSidebar();
        JScrollPane sidebarScrollPane = new JScrollPane(leftSidebar);
        sidebarScrollPane.setBorder(null);
        sidebarScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sidebarScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sidebarScrollPane.setPreferredSize(new Dimension(320, 0));
        mainContainer.add(sidebarScrollPane, BorderLayout.WEST);
        
        // CENTER PANEL for table only
        JPanel centerPanel = createTablePanel();
        mainContainer.add(centerPanel, BorderLayout.CENTER);
        
        add(mainContainer, BorderLayout.CENTER);
        
        // Initialize subject checkboxes
        updateSubjectCheckboxes();
    }
    
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(229, 231, 235)),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        
        // Left side - Back button
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);
        
        if (onCloseCallback != null) {
            JButton backButton = new JButton("← Back");
            backButton.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 14));
            backButton.setForeground(new Color(99, 102, 241));
            backButton.setBorderPainted(false);
            backButton.setContentAreaFilled(false);
            backButton.setFocusPainted(false);
            backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            backButton.addActionListener(e -> closePanel());
            leftPanel.add(backButton);
        }
        
        // Center - Title
        JLabel titleLabel = new JLabel("View Student Data");
        titleLabel.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 20));
        titleLabel.setForeground(new Color(17, 24, 39));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Right side - Close button
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(false);
        
        JButton closeButton = new JButton("×");
        closeButton.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 24));
        closeButton.setForeground(new Color(75, 85, 99));
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setFocusPainted(false);
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> closePanel());
        rightPanel.add(closeButton);
        
        headerPanel.add(leftPanel, BorderLayout.WEST);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.add(rightPanel, BorderLayout.EAST);
        
        return headerPanel;
    }
    
    private JPanel createFilterSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Color.WHITE);
        sidebar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(229, 231, 235)),
            BorderFactory.createEmptyBorder(20, 10, 20, 10)
        ));
        
        // Title
        JLabel filterTitle = new JLabel("Filters");
        filterTitle.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 18));
        filterTitle.setForeground(new Color(17, 24, 39));
        filterTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(filterTitle);
        sidebar.add(Box.createVerticalStrut(15));
        
        // Section selector
        JLabel sectionLabel = new JLabel("Select Section");
        sectionLabel.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 13));
        sectionLabel.setForeground(new Color(17, 24, 39));
        sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(sectionLabel);
        sidebar.add(Box.createVerticalStrut(8));
        
        Vector<String> sections = new Vector<>();
        sections.add("All Sections");
        loadSectionInfo();
        sections.addAll(sectionStudents.keySet());
        
        sectionDropdown = new JComboBox<>(sections);
        sectionDropdown.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 13));
        sectionDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        sectionDropdown.setBackground(Color.WHITE);
        sectionDropdown.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        sectionDropdown.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(sectionDropdown);
        sidebar.add(Box.createVerticalStrut(20));
        
        // Subject & Exam Type Filter
        JLabel filterLabel = new JLabel("Subject & Exam Type Filter");
        filterLabel.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 13));
        filterLabel.setForeground(new Color(17, 24, 39));
        filterLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(filterLabel);
        sidebar.add(Box.createVerticalStrut(8));
        
        // Subject checkbox panel (hierarchical tree)
        subjectCheckboxPanel = new JPanel(new BorderLayout());
        subjectCheckboxPanel.setBackground(Color.WHITE);
        subjectCheckboxPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subjectCheckboxPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        subjectCheckboxPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        sidebar.add(subjectCheckboxPanel);
        
        // Add listener to update filter when section changes
        sectionDropdown.addActionListener(e -> {
            updateSubjectCheckboxes();
        });
        
        sidebar.add(Box.createVerticalStrut(20));
        
        // Student Information Checkboxes
        JPanel studentInfoHeader = new JPanel(new BorderLayout());
        studentInfoHeader.setBackground(Color.WHITE);
        studentInfoHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        studentInfoHeader.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        studentInfoHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel studentInfoLabel = new JLabel("Student Information");
        studentInfoLabel.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 13));
        studentInfoLabel.setForeground(new Color(17, 24, 39));
        
        JCheckBox selectAllStudentInfo = new JCheckBox("All", true);
        selectAllStudentInfo.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 11));
        selectAllStudentInfo.setBackground(Color.WHITE);
        selectAllStudentInfo.setForeground(new Color(99, 102, 241));
        selectAllStudentInfo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        studentInfoHeader.add(studentInfoLabel, BorderLayout.WEST);
        studentInfoHeader.add(selectAllStudentInfo, BorderLayout.EAST);
        sidebar.add(studentInfoHeader);
        sidebar.add(Box.createVerticalStrut(8));
        
        JPanel studentInfoPanel = new JPanel();
        studentInfoPanel.setLayout(new BoxLayout(studentInfoPanel, BoxLayout.Y_AXIS));
        studentInfoPanel.setBackground(Color.WHITE);
        studentInfoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        studentInfoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        
        nameCheckBox = new StyledCheckBox("Name", true);
        rollNumberCheckBox = new StyledCheckBox("Roll Number", true);
        emailCheckBox = new StyledCheckBox("Email", false);
        phoneCheckBox = new StyledCheckBox("Phone", false);
        sectionCheckBox = new StyledCheckBox("Section", true);
        yearCheckBox = new StyledCheckBox("Year", true);
        semesterCheckBox = new StyledCheckBox("Semester", true);
        
        nameCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        rollNumberCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        emailCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        phoneCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        sectionCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        yearCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        semesterCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        studentInfoPanel.add(nameCheckBox);
        studentInfoPanel.add(rollNumberCheckBox);
        studentInfoPanel.add(emailCheckBox);
        studentInfoPanel.add(phoneCheckBox);
        studentInfoPanel.add(sectionCheckBox);
        studentInfoPanel.add(yearCheckBox);
        studentInfoPanel.add(semesterCheckBox);
        
        // Add Select All listener
        selectAllStudentInfo.addActionListener(e -> {
            boolean selected = selectAllStudentInfo.isSelected();
            nameCheckBox.setSelected(selected);
            rollNumberCheckBox.setSelected(selected);
            emailCheckBox.setSelected(selected);
            phoneCheckBox.setSelected(selected);
            sectionCheckBox.setSelected(selected);
            yearCheckBox.setSelected(selected);
            semesterCheckBox.setSelected(selected);
        });
        
        studentInfoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        sidebar.add(studentInfoPanel);
        sidebar.add(Box.createVerticalStrut(15));
        
        // Academic Information Checkboxes
        JPanel academicInfoHeader = new JPanel(new BorderLayout());
        academicInfoHeader.setBackground(Color.WHITE);
        academicInfoHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        academicInfoHeader.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        academicInfoHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel academicInfoLabel = new JLabel("Academic Information");
        academicInfoLabel.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 13));
        academicInfoLabel.setForeground(new Color(17, 24, 39));
        
        JCheckBox selectAllAcademicInfo = new JCheckBox("All", true);
        selectAllAcademicInfo.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 11));
        selectAllAcademicInfo.setBackground(Color.WHITE);
        selectAllAcademicInfo.setForeground(new Color(99, 102, 241));
        selectAllAcademicInfo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        academicInfoHeader.add(academicInfoLabel, BorderLayout.WEST);
        academicInfoHeader.add(selectAllAcademicInfo, BorderLayout.EAST);
        sidebar.add(academicInfoHeader);
        sidebar.add(Box.createVerticalStrut(8));
        
        JPanel academicInfoPanel = new JPanel();
        academicInfoPanel.setLayout(new BoxLayout(academicInfoPanel, BoxLayout.Y_AXIS));
        academicInfoPanel.setBackground(Color.WHITE);
        academicInfoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        academicInfoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        
        totalMarksCheckBox = new StyledCheckBox("Total Marks", true);
        percentageCheckBox = new StyledCheckBox("Percentage", true);
        sgpaCheckBox = new StyledCheckBox("SGPA", true);
        gradeCheckBox = new StyledCheckBox("Grade", true);
        statusCheckBox = new StyledCheckBox("Pass/Fail", true);
        rankCheckBox = new StyledCheckBox("Rank", false);
        failedSubjectsCheckBox = new StyledCheckBox("Failed Subjects", false);
        
        totalMarksCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        percentageCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        sgpaCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        gradeCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        rankCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        failedSubjectsCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        academicInfoPanel.add(totalMarksCheckBox);
        academicInfoPanel.add(percentageCheckBox);
        academicInfoPanel.add(sgpaCheckBox);
        academicInfoPanel.add(gradeCheckBox);
        academicInfoPanel.add(statusCheckBox);
        academicInfoPanel.add(rankCheckBox);
        academicInfoPanel.add(failedSubjectsCheckBox);
        
        // Add Select All listener
        selectAllAcademicInfo.addActionListener(e -> {
            boolean selected = selectAllAcademicInfo.isSelected();
            totalMarksCheckBox.setSelected(selected);
            percentageCheckBox.setSelected(selected);
            sgpaCheckBox.setSelected(selected);
            gradeCheckBox.setSelected(selected);
            statusCheckBox.setSelected(selected);
            rankCheckBox.setSelected(selected);
            failedSubjectsCheckBox.setSelected(selected);
        });
        
        academicInfoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        sidebar.add(academicInfoPanel);
        sidebar.add(Box.createVerticalStrut(15));
        
        // Launched Results Section
        JLabel launchedResultsLabel = new JLabel("Launched Results");
        launchedResultsLabel.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 13));
        launchedResultsLabel.setForeground(new Color(17, 24, 39));
        launchedResultsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(launchedResultsLabel);
        sidebar.add(Box.createVerticalStrut(8));
        
        JPanel launchedResultsPanel = new JPanel();
        launchedResultsPanel.setLayout(new BoxLayout(launchedResultsPanel, BoxLayout.Y_AXIS));
        launchedResultsPanel.setBackground(Color.WHITE);
        launchedResultsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        launchedResultsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        
        // Dropdown to select launched result
        JLabel selectLaunchLabel = new JLabel("Select Launch:");
        selectLaunchLabel.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 11));
        selectLaunchLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        launchedResultsPanel.add(selectLaunchLabel);
        launchedResultsPanel.add(Box.createVerticalStrut(4));
        
        launchedResultsDropdown = new JComboBox<>();
        launchedResultsDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        launchedResultsDropdown.setAlignmentX(Component.LEFT_ALIGNMENT);
        launchedResultsDropdown.setBackground(Color.WHITE);
        launchedResultsPanel.add(launchedResultsDropdown);
        launchedResultsPanel.add(Box.createVerticalStrut(8));
        
        // Add listener to deselect Student/Academic info when launched result is selected
        launchedResultsDropdown.addActionListener(e -> {
            if (launchedResultsDropdown.getSelectedIndex() > 0) {
                // Deselect all Student Information checkboxes
                nameCheckBox.setSelected(false);
                rollNumberCheckBox.setSelected(false);
                emailCheckBox.setSelected(false);
                phoneCheckBox.setSelected(false);
                sectionCheckBox.setSelected(false);
                yearCheckBox.setSelected(false);
                semesterCheckBox.setSelected(false);
                
                // Deselect all Academic Information checkboxes
                totalMarksCheckBox.setSelected(false);
                percentageCheckBox.setSelected(false);
                sgpaCheckBox.setSelected(false);
                gradeCheckBox.setSelected(false);
                statusCheckBox.setSelected(false);
                rankCheckBox.setSelected(false);
                failedSubjectsCheckBox.setSelected(false);
            }
        });
        
        // Load launched results into dropdown
        loadLaunchedResultsDropdown();
        
        JLabel includeLabel = new JLabel("Include in Export:");
        includeLabel.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 11));
        includeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        launchedResultsPanel.add(includeLabel);
        launchedResultsPanel.add(Box.createVerticalStrut(4));
        
        showStudentsCheckBox = new StyledCheckBox("Student Details", true);
        showSubjectsCheckBox = new StyledCheckBox("Subject Marks", true);
        showComponentsCheckBox = new StyledCheckBox("Component Breakdown", false);
        
        showStudentsCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        showSubjectsCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        showComponentsCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        launchedResultsPanel.add(showStudentsCheckBox);
        launchedResultsPanel.add(showSubjectsCheckBox);
        launchedResultsPanel.add(showComponentsCheckBox);
        
        launchedResultsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        sidebar.add(launchedResultsPanel);
        sidebar.add(Box.createVerticalStrut(20));
        
        // Show Data Button at bottom of sidebar
        showButton = new RoundedButton("Show Data", 12,
            new Color(99, 102, 241), new Color(79, 82, 221));
        showButton.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14));
        showButton.setForeground(Color.WHITE);
        showButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        showButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        showButton.addActionListener(e -> displaySelectedData());
        sidebar.add(showButton);
        
        sidebar.add(Box.createVerticalGlue());
        
        return sidebar;
    }
    
    private JPanel createTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(Color.WHITE);
        tablePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Create table container
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setBackground(Color.WHITE);
        tableContainer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        // Create table
        resultTable = new JTable();
        resultTable.setRowHeight(30);
        resultTable.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
        resultTable.setGridColor(new Color(229, 231, 235));
        resultTable.setShowGrid(true);
        resultTable.setIntercellSpacing(new Dimension(1, 1));
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // Enable horizontal scrolling
        
        // Style table header
        JTableHeader header = resultTable.getTableHeader();
        header.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
        header.setBackground(new Color(248, 250, 252));
        header.setForeground(new Color(17, 24, 39));
        header.setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235)));
        
        tableScrollPane = new JScrollPane(resultTable);
        tableScrollPane.setBorder(null);
        tableScrollPane.getViewport().setBackground(Color.WHITE);
        tableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tableContainer.add(tableScrollPane, BorderLayout.CENTER);
        
        tablePanel.add(tableContainer, BorderLayout.CENTER);
        
        // Export buttons at bottom
        JPanel exportPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        exportPanel.setBackground(Color.WHITE);
        
        exportExcelButton = new RoundedButton("Export Excel", 12,
            new Color(34, 197, 94), new Color(22, 163, 74)); // SUCCESS_COLOR from desktop
        exportExcelButton.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
        exportExcelButton.setForeground(Color.WHITE);
        exportExcelButton.setPreferredSize(new Dimension(110, 35)); // Reduced
        
        exportPdfButton = new RoundedButton("Export PDF", 12,
            new Color(220, 53, 69), new Color(185, 28, 28)); // ERROR_COLOR from desktop
        exportPdfButton.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
        exportPdfButton.setForeground(Color.WHITE);
        exportPdfButton.setPreferredSize(new Dimension(110, 35)); // Reduced
        
        printButton = new RoundedButton("Print", 12,
            new Color(251, 191, 36), new Color(245, 158, 11)); // WARNING_COLOR from desktop
        printButton.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
        printButton.setForeground(Color.WHITE);
        printButton.setPreferredSize(new Dimension(80, 35)); // Reduced
        
        exportPanel.add(exportExcelButton);
        exportPanel.add(exportPdfButton);
        exportPanel.add(printButton);
        
        tablePanel.add(exportPanel, BorderLayout.SOUTH);
        
        // Add listeners for export buttons
        exportExcelButton.addActionListener(e -> exportToExcel());
        exportPdfButton.addActionListener(e -> exportToPdf());
        printButton.addActionListener(e -> printTable());
        
        return tablePanel;
    }
    
    
    private void loadSectionInfo() {
        try {
            SectionDAO sectionDAO = new SectionDAO();
            List<SectionDAO.SectionInfo> sections = sectionDAO.getSectionsByUser(
                com.sms.login.LoginScreen.currentUserId);
            
            for (SectionDAO.SectionInfo section : sections) {
                sectionInfoMap.put(section.id, new SectionInfo(section));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadLaunchedResultsDropdown() {
        launchedResultsDropdown.removeAllItems();
        launchedResultsDropdown.addItem("-- Select Launched Result --");
        
        // Query ALL launched results (independent of section selection)
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT launch_id, MIN(created_at) as launch_date, COUNT(DISTINCT student_id) as student_count " +
                          "FROM launched_student_results " +
                          "GROUP BY launch_id " +
                          "ORDER BY launch_date DESC";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                ResultSet rs = stmt.executeQuery();
                
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd-MMM-yyyy hh:mm a");
                
                while (rs.next()) {
                    int launchId = rs.getInt("launch_id");
                    java.sql.Timestamp launchDate = rs.getTimestamp("launch_date");
                    int studentCount = rs.getInt("student_count");
                    
                    String displayText = String.format("#%d - %s (%d students)", 
                        launchId, 
                        dateFormat.format(launchDate),
                        studentCount);
                    
                    launchedResultsDropdown.addItem(displayText);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Error loading launched results: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void addDragFunctionality(JPanel panel) {
        final Point[] mouseClickPoint = {null};
        
        panel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                mouseClickPoint[0] = e.getPoint();
            }
        });
        
        panel.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (mouseClickPoint[0] != null) {
                    Point newPoint = e.getLocationOnScreen();
                    newPoint.translate(-mouseClickPoint[0].x, -mouseClickPoint[0].y);
                    setLocation(newPoint);
                }
            }
        });
    }
    
    
    private void updateSubjectCheckboxes() {
        subjectCheckboxPanel.removeAll();
        selectedFilters.clear();
        
        String selectedSection = (String) sectionDropdown.getSelectedItem();
        System.out.println("Selected section: " + selectedSection);
        
        if (selectedSection == null || selectedSection.equals("All Sections")) {
            JLabel infoLabel = new JLabel("Please select a specific section to view exam filters");
            infoLabel.setFont(new java.awt.Font("SansSerif", java.awt.Font.ITALIC, 12));
            infoLabel.setForeground(Color.GRAY);
            subjectCheckboxPanel.add(infoLabel, BorderLayout.CENTER);
            subjectCheckboxPanel.revalidate();
            subjectCheckboxPanel.repaint();
            return;
        }
        
        // Get section ID
        int sectionId = getSectionIdByName(selectedSection);
        if (sectionId <= 0) {
            JLabel errorLabel = new JLabel("Section not found in database");
            errorLabel.setForeground(Color.RED);
            subjectCheckboxPanel.add(errorLabel, BorderLayout.CENTER);
            subjectCheckboxPanel.revalidate();
            subjectCheckboxPanel.repaint();
            return;
        }
        
        // Create hierarchical filter tree
        JPanel treePanel = new JPanel();
        treePanel.setLayout(new BoxLayout(treePanel, BoxLayout.Y_AXIS));
        treePanel.setBackground(Color.WHITE);
        treePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Query database for subjects and exam types
        Map<String, List<String>> subjectExamTypes = getSubjectExamTypesForSection(sectionId);
        
        if (subjectExamTypes.isEmpty()) {
            JLabel noDataLabel = new JLabel("No exam patterns configured for this section");
            noDataLabel.setFont(new java.awt.Font("SansSerif", java.awt.Font.ITALIC, 12));
            noDataLabel.setForeground(Color.GRAY);
            treePanel.add(noDataLabel);
        } else {
            // Overall checkbox
            JCheckBox overallCheck = new JCheckBox("Overall (All)", true);
            overallCheck.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14));
            overallCheck.setBackground(Color.WHITE);
            overallCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            java.util.List<JCheckBox> allCheckboxes = new java.util.ArrayList<>();
            
            treePanel.add(overallCheck);
            treePanel.add(Box.createVerticalStrut(10));
            treePanel.add(new JSeparator());
            treePanel.add(Box.createVerticalStrut(10));
            
            // Subject and exam type checkboxes
            for (Map.Entry<String, List<String>> entry : subjectExamTypes.entrySet()) {
                String subject = entry.getKey();
                List<String> examTypes = entry.getValue();
                
                // Subject checkbox
                JCheckBox subjectCheck = new JCheckBox("+ " + subject, selectedFilters.containsKey(subject));
                subjectCheck.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 13));
                subjectCheck.setForeground(new Color(17, 24, 39)); // TEXT_PRIMARY
                subjectCheck.setBackground(Color.WHITE);
                subjectCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
                allCheckboxes.add(subjectCheck);
                
                java.util.List<JCheckBox> examChecks = new java.util.ArrayList<>();
                
                subjectCheck.addActionListener(e -> {
                    boolean selected = subjectCheck.isSelected();
                    for (JCheckBox examCheck : examChecks) {
                        examCheck.setSelected(selected);
                    }
                    
                    if (selected) {
                        selectedFilters.put(subject, new HashSet<>(examTypes));
                    } else {
                        selectedFilters.remove(subject);
                    }
                    overallCheck.setSelected(isAllSelected(subjectExamTypes));
                });
                
                treePanel.add(subjectCheck);
                
                // Exam type checkboxes (indented)
                for (String examType : examTypes) {
                    boolean examSelected = selectedFilters.containsKey(subject) && 
                                         selectedFilters.get(subject).contains(examType);
                    
                    JCheckBox examCheck = new JCheckBox("   - " + examType, examSelected);
                    examCheck.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
                    examCheck.setBackground(Color.WHITE);
                    examCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
                    examChecks.add(examCheck);
                    allCheckboxes.add(examCheck);
                    
                    examCheck.addActionListener(e -> {
                        if (examCheck.isSelected()) {
                            selectedFilters.putIfAbsent(subject, new HashSet<>());
                            selectedFilters.get(subject).add(examType);
                        } else {
                            if (selectedFilters.containsKey(subject)) {
                                selectedFilters.get(subject).remove(examType);
                                if (selectedFilters.get(subject).isEmpty()) {
                                    selectedFilters.remove(subject);
                                }
                            }
                        }
                        
                        // Update subject checkbox
                        boolean allSelected = examChecks.stream().allMatch(JCheckBox::isSelected);
                        subjectCheck.setSelected(allSelected);
                        overallCheck.setSelected(isAllSelected(subjectExamTypes));
                    });
                    
                    treePanel.add(examCheck);
                }
                
                treePanel.add(Box.createVerticalStrut(5));
            }
            
            // Set up overall checkbox listener after all checkboxes are created
            overallCheck.addActionListener(e -> {
                boolean selected = overallCheck.isSelected();
                for (JCheckBox checkbox : allCheckboxes) {
                    checkbox.setSelected(selected);
                }
                
                if (selected) {
                    for (Map.Entry<String, List<String>> entry : subjectExamTypes.entrySet()) {
                        selectedFilters.put(entry.getKey(), new HashSet<>(entry.getValue()));
                    }
                } else {
                    selectedFilters.clear();
                }
            });
            
            // Select all by default on first load
            if (selectedFilters.isEmpty()) {
                for (Map.Entry<String, List<String>> entry : subjectExamTypes.entrySet()) {
                    selectedFilters.put(entry.getKey(), new HashSet<>(entry.getValue()));
                }
            }
        }
        
        JScrollPane scrollPane = new JScrollPane(treePanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.setBackground(Color.WHITE);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setPreferredSize(new Dimension(0, 150)); // Fixed height
        
        subjectCheckboxPanel.add(scrollPane, BorderLayout.CENTER);
        subjectCheckboxPanel.revalidate();
        subjectCheckboxPanel.repaint();
    }
    
    private Map<String, List<String>> getSubjectExamTypesForSection(int sectionId) {
        Map<String, List<String>> subjectExamTypes = new LinkedHashMap<>();
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            String query = "SELECT DISTINCT s.subject_name, et.exam_name " +
                          "FROM section_subjects ss " +
                          "JOIN subjects s ON ss.subject_id = s.id " +
                          "JOIN subject_exam_types set_tbl ON ss.section_id = set_tbl.section_id AND ss.subject_id = set_tbl.subject_id " +
                          "JOIN exam_types et ON set_tbl.exam_type_id = et.id " +
                          "WHERE ss.section_id = ? " +
                          "ORDER BY s.subject_name, et.exam_name";
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, sectionId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                String subjectName = rs.getString("subject_name");
                String examName = rs.getString("exam_name");
                
                subjectExamTypes.putIfAbsent(subjectName, new ArrayList<>());
                subjectExamTypes.get(subjectName).add(examName);
            }
            
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return subjectExamTypes;
    }
    
    private Map<String, Map<String, Integer>> getMaxMarksForSection(int sectionId) {
        Map<String, Map<String, Integer>> maxMarksMap = new LinkedHashMap<>();
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            // Step 1: Get subjects and their max marks for this section (same as Section Analyzer)
            String subjectQuery = 
                "SELECT DISTINCT sub.subject_name, sub.id, ss.max_marks " +
                "FROM section_subjects ss " +
                "JOIN subjects sub ON ss.subject_id = sub.id " +
                "WHERE ss.section_id = ? " +
                "ORDER BY sub.subject_name";
            
            PreparedStatement ps1 = conn.prepareStatement(subjectQuery);
            ps1.setInt(1, sectionId);
            ResultSet rs1 = ps1.executeQuery();
            
            Map<String, Integer> subjectIds = new HashMap<>();
            
            while (rs1.next()) {
                String subjectName = rs1.getString("subject_name");
                int subjectId = rs1.getInt("id");
                subjectIds.put(subjectName, subjectId);
            }
            rs1.close();
            ps1.close();
            
            // Step 2: Get exam types and their actual max marks for each subject (same as Section Analyzer)
            for (Map.Entry<String, Integer> entry : subjectIds.entrySet()) {
                String subjectName = entry.getKey();
                int subjectId = entry.getValue();
                
                Map<String, Integer> examTypeMaxMarks = new LinkedHashMap<>();
                
                // Get exam types from entered_exam_marks table (same approach as Section Analyzer)
                String examTypeQuery1 = 
                    "SELECT DISTINCT et.exam_name, et.max_marks " +
                    "FROM entered_exam_marks sm " +
                    "JOIN exam_types et ON sm.exam_type_id = et.id " +
                    "JOIN students s ON sm.student_id = s.id " +
                    "WHERE sm.subject_id = ? AND s.section_id = ? " +
                    "ORDER BY et.exam_name";
                
                PreparedStatement ps3 = conn.prepareStatement(examTypeQuery1);
                ps3.setInt(1, subjectId);
                ps3.setInt(2, sectionId);
                ResultSet rs3 = ps3.executeQuery();
                
                while (rs3.next()) {
                    String examType = rs3.getString("exam_name");
                    int maxMarks = rs3.getInt("max_marks");
                    if (examType != null && !examType.trim().isEmpty()) {
                        examTypeMaxMarks.put(examType, maxMarks);
                    }
                }
                rs3.close();
                ps3.close();
                
                if (!examTypeMaxMarks.isEmpty()) {
                    maxMarksMap.put(subjectName, examTypeMaxMarks);
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return maxMarksMap;
    }
    
    private boolean isAllSelected(Map<String, List<String>> subjectExamTypes) {
        for (Map.Entry<String, List<String>> entry : subjectExamTypes.entrySet()) {
            if (!selectedFilters.containsKey(entry.getKey())) return false;
            if (!selectedFilters.get(entry.getKey()).containsAll(entry.getValue())) {
                return false;
            }
        }
        return true;
    }
    

    private void displaySelectedData() {
        String selectedSection = (String) sectionDropdown.getSelectedItem();
        System.out.println("\n=== USER SELECTED SECTION ===");
        System.out.println("Selected: '" + selectedSection + "'");
        System.out.println("Available keys: " + sectionStudents.keySet());
        
        if (selectedSection == null) {
            showStyledMessage("Please select a section!", "No Section Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Create column headers based on selected checkboxes
        ArrayList<String> columnNames = new ArrayList<>();
        columnGroups.clear();
        int columnIndex = 0;
        
        if (nameCheckBox.isSelected()) {
            columnNames.add("Name");
            columnIndex++;
        }
        if (rollNumberCheckBox.isSelected()) {
            columnNames.add("Roll Number");
            columnIndex++;
        }
        if (emailCheckBox.isSelected()) {
            columnNames.add("Email");
            columnIndex++;
        }
        if (phoneCheckBox.isSelected()) {
            columnNames.add("Phone");
            columnIndex++;
        }
        if (sectionCheckBox.isSelected()) {
            columnNames.add("Section");
            columnIndex++;
        }
        if (yearCheckBox.isSelected()) {
            columnNames.add("Year");
            columnIndex++;
        }
        if (semesterCheckBox.isSelected()) {
            columnNames.add("Semester");
            columnIndex++;
        }
        
        // Add selected subject columns with exam types
        List<String> selectedSubjects = new ArrayList<>();
        Map<String, List<String>> subjectExamTypesMap = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> maxMarksMap = new HashMap<>();
        
        // Get section ID to fetch max marks
        int sectionId = getSectionIdByName(selectedSection);
        if (sectionId > 0) {
            maxMarksMap = getMaxMarksForSection(sectionId);
        }
        
        for (Map.Entry<String, Set<String>> entry : selectedFilters.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                String subject = entry.getKey();
                selectedSubjects.add(subject);
                List<String> examTypes = new ArrayList<>(entry.getValue());
                subjectExamTypesMap.put(subject, examTypes);
                
                // Create column group for this subject
                int startCol = columnIndex;
                
                // Add columns for each exam type
                for (String examType : examTypes) {
                    columnNames.add(subject + " - " + examType);
                    columnIndex++;
                }
                
                // Add subject total column
                columnNames.add(subject + " - Total");
                columnIndex++;
                
                // Store group info (including total column)
                columnGroups.add(new ColumnGroup(subject, startCol, examTypes.size() + 1));
            }
        }
        
        // Store max marks map for header display
        final Map<String, Map<String, Integer>> finalMaxMarksMap = maxMarksMap;
        
        if (totalMarksCheckBox.isSelected()) {
            // Show max marks in header
            int maxMarks = selectedSubjects.size() * 100;
            columnNames.add("Total Marks (" + maxMarks + ")");
        }
        if (percentageCheckBox.isSelected()) columnNames.add("Percentage");
        if (sgpaCheckBox.isSelected()) columnNames.add("SGPA");
        if (gradeCheckBox.isSelected()) columnNames.add("Grade");
        if (statusCheckBox.isSelected()) columnNames.add("Status");
        if (rankCheckBox.isSelected()) columnNames.add("Rank");
        if (failedSubjectsCheckBox.isSelected()) columnNames.add("Failed Subjects");
        // Check if a launched result is selected
        boolean showLaunchedResults = launchedResultsDropdown.getSelectedIndex() > 0;
        Integer selectedLaunchId = null;
        
        if (showLaunchedResults) {
            // Extract launch_id from dropdown selection (format: "#9 - 16-Jan-2026 11:50 pm (1 students)")
            String selectedItem = (String) launchedResultsDropdown.getSelectedItem();
            if (selectedItem != null && selectedItem.startsWith("#")) {
                try {
                    String launchIdStr = selectedItem.substring(1, selectedItem.indexOf(" -"));
                    selectedLaunchId = Integer.parseInt(launchIdStr);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        
        // Collect all students for ranking
        List<ExtendedStudentData> allStudentData = new ArrayList<>();
        
        // Maps to store exam types and max marks for launched results
        Map<String, List<String>> launchedResultsExamTypesMap = new HashMap<>();
        Map<String, Map<String, Integer>> launchedResultsMaxMarksMap = new HashMap<>();
        
        // If launched result is selected, fetch students from that launch first to get subject names
        if (showLaunchedResults && selectedLaunchId != null) {
            allStudentData = getStudentsFromLaunchedResult(selectedLaunchId, selectedSubjects);
            
            // For launched results, always show basic student info
            columnNames.add("Name");
            columnNames.add("Roll Number");
            columnNames.add("Section");
            columnNames.add("Year");
            columnNames.add("Semester");
            
            // Add subject columns with exam types from the launched result data
            if (!allStudentData.isEmpty()) {
                ExtendedStudentData firstStudent = allStudentData.get(0);
                // Get subject names in sorted order for consistency - UPDATE selectedSubjects to use these
                selectedSubjects = new ArrayList<>(firstStudent.subjectWeightedTotals.keySet());
                java.util.Collections.sort(selectedSubjects);
                
                // Build exam types and max marks maps for multi-row headers
                if (firstStudent.subjectExamTypes != null) {
                    for (String subject : selectedSubjects) {
                        List<String> examTypes = firstStudent.subjectExamTypes.get(subject);
                        Map<String, Integer> subjectMaxMarks = firstStudent.subjectMaxMarks != null ? 
                            firstStudent.subjectMaxMarks.get(subject) : null;
                        
                        if (examTypes != null && !examTypes.isEmpty()) {
                            // Store for header rendering
                            launchedResultsExamTypesMap.put(subject, examTypes);
                            
                            // Store max marks if available
                            if (subjectMaxMarks != null && !subjectMaxMarks.isEmpty()) {
                                launchedResultsMaxMarksMap.put(subject, subjectMaxMarks);
                            }
                            
                            // Add individual exam type columns
                            for (String examType : examTypes) {
                                columnNames.add(subject + " - " + examType);
                            }
                            // Add total column
                            columnNames.add(subject + " - Total");
                        } else {
                            // No exam types - just add subject name
                            columnNames.add(subject);
                        }
                    }
                }
            }
            
            // Add overall stats
            columnNames.add("Total Marks");
            columnNames.add("Percentage");
            columnNames.add("CGPA");
            columnNames.add("Grade");
            columnNames.add("Status");
            columnNames.add("Launch Date");
        }
        
        if (columnNames.isEmpty()) {
            showStyledMessage("Please select at least one field to display!", "No Fields Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Store subject exam types map for later use
        final Map<String, List<String>> finalSubjectExamTypesMap = subjectExamTypesMap;
        
        DefaultTableModel model = new DefaultTableModel(columnNames.toArray(new String[0]), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        
        if (!showLaunchedResults) {
            // Normal mode: fetch from selected section
            // Add data rows
            if (selectedSection.equals("All Sections")) {
                System.out.println("DEBUG: Total sections: " + sectionStudents.size());
                
                // Display data from all sections
                for (Map.Entry<String, List<Student>> entry : sectionStudents.entrySet()) {
                    String section = entry.getKey();
                    List<Student> students = entry.getValue();
                    System.out.println("DEBUG: Section " + section + " has " + students.size() + " students");
                    
                    for (Student student : students) {
                        ExtendedStudentData data = getExtendedStudentData(student, section, selectedSubjects);
                        if (data != null) {
                            allStudentData.add(data);
                        }
                    }
                }
            } else {
                // Display data from selected section
                List<Student> students = sectionStudents.get(selectedSection);
                System.out.println("DEBUG: Students in section: " + (students != null ? students.size() : "null"));
                
                if (students != null) {
                    for (Student student : students) {
                        ExtendedStudentData data = getExtendedStudentData(student, selectedSection, selectedSubjects);
                        if (data != null) {
                            allStudentData.add(data);
                        }
                    }
                } else {
                    showStyledMessage("No students found in section: " + selectedSection, "No Data", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
            }
        }
        
        if (allStudentData.isEmpty()) {
            showStyledMessage("No student data found for the selected section(s)!", "No Data", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Calculate ranks
        if (rankCheckBox.isSelected()) {
            calculateRanks(allStudentData);
        }
        
        // Add rows to table
        for (ExtendedStudentData data : allStudentData) {
            ArrayList<Object> rowData = new ArrayList<>();
            
            if (showLaunchedResults) {
                // For launched results, always add basic student info
                rowData.add(data.name);
                rowData.add(data.rollNumber);
                rowData.add(data.section);
                rowData.add(String.valueOf(data.year));
                rowData.add(String.valueOf(data.semester));
            } else {
                // For normal mode, use checkbox selections
                if (nameCheckBox.isSelected()) rowData.add(data.name);
                if (rollNumberCheckBox.isSelected()) rowData.add(data.rollNumber);
                if (emailCheckBox.isSelected()) rowData.add(data.email != null ? data.email : "");
                if (phoneCheckBox.isSelected()) rowData.add(data.phone != null ? data.phone : "");
                if (sectionCheckBox.isSelected()) rowData.add(data.section);
                if (yearCheckBox.isSelected()) rowData.add(String.valueOf(data.year));
                if (semesterCheckBox.isSelected()) rowData.add(String.valueOf(data.semester));
            }
            
            // Add exam type marks for each subject
            for (String subject : selectedSubjects) {
                // Use the correct exam types map based on mode
                List<String> examTypes = showLaunchedResults ? 
                    launchedResultsExamTypesMap.get(subject) : 
                    finalSubjectExamTypesMap.get(subject);
                
                if (examTypes != null && !examTypes.isEmpty()) {
                    // Add individual exam type marks
                    for (String examType : examTypes) {
                        Map<String, Integer> subjectMarks = data.subjectMarks.get(subject);
                        if (subjectMarks != null && subjectMarks.containsKey(examType)) {
                            Integer mark = subjectMarks.get(examType);
                            int markValue = (mark != null) ? mark : 0;
                            rowData.add(String.valueOf(markValue));
                        } else {
                            rowData.add("0");
                        }
                    }
                    
                    // Add subject weighted total WITHOUT % symbol (just number out of 100)
                    if (data.subjectWeightedTotals != null && data.subjectWeightedTotals.containsKey(subject)) {
                        double weightedPercentage = data.subjectWeightedTotals.get(subject);
                        String totalDisplay = String.format("%.0f", weightedPercentage); // No % symbol
                        
                        // Mark as failed if subject failed
                        if (data.subjectPassStatus != null && data.subjectPassStatus.containsKey(subject) 
                            && !data.subjectPassStatus.get(subject)) {
                            totalDisplay += " (F)";
                        }
                        rowData.add(totalDisplay);
                    } else {
                        rowData.add("N/A");
                    }
                } else {
                    // No exam types - shouldn't happen but handle gracefully
                    if (data.subjectWeightedTotals != null && data.subjectWeightedTotals.containsKey(subject)) {
                        rowData.add(String.format("%.0f", data.subjectWeightedTotals.get(subject)));
                    } else {
                        rowData.add("N/A");
                    }
                }
            }
            
            if (showLaunchedResults) {
                // For launched results, always add overall stats
                rowData.add(String.valueOf(data.totalMarks));
                rowData.add(String.format("%.2f%%", data.percentage));
                rowData.add(String.format("%.2f", data.sgpa));
                rowData.add(data.grade);
                rowData.add(data.status);
                rowData.add(data.launchDate);
            } else {
                // For normal mode, use checkbox selections
                // Format: Just show totalMarks without (max)
                if (totalMarksCheckBox.isSelected()) {
                    rowData.add(String.format("%.0f", data.totalMarks));
                }
                if (percentageCheckBox.isSelected()) rowData.add(String.format("%.2f%%", data.percentage));
                if (sgpaCheckBox.isSelected()) rowData.add(String.format("%.2f", data.sgpa));
                if (gradeCheckBox.isSelected()) rowData.add(data.grade);
                if (statusCheckBox.isSelected()) rowData.add(data.status);
                if (rankCheckBox.isSelected()) rowData.add(data.rank);
                if (failedSubjectsCheckBox.isSelected()) rowData.add(data.failedSubjectsCount);
            }
            
            model.addRow(rowData.toArray());
        }
        
        resultTable.setModel(model);
        
        // Apply custom 2-row header if we have subjects selected
        if (showLaunchedResults && !launchedResultsExamTypesMap.isEmpty()) {
            // For launched results, use the maps built from JSON data
            setupMultiRowHeader(launchedResultsExamTypesMap, launchedResultsMaxMarksMap);
        } else if (!finalSubjectExamTypesMap.isEmpty()) {
            setupMultiRowHeader(finalSubjectExamTypesMap, finalMaxMarksMap);
        }
        
        // Auto-resize columns to fit content
        autoResizeTableColumns(resultTable);
        
        // Apply alternating row colors and conditional formatting
        resultTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                // Handle HTML content for multi-line text
                if (value != null && value.toString().contains("\\n")) {
                    String htmlText = "<html>" + value.toString().replace("\\n", "<br>") + "</html>";
                    ((JLabel) c).setText(htmlText);
                }
                
                if (!isSelected) {
                    if (row % 2 == 0) {
                        c.setBackground(Color.WHITE);
                    } else {
                        c.setBackground(new Color(248, 248, 248));
                    }
                }
                
                // Reset foreground color first
                c.setForeground(Color.BLACK);
                
                // Color coding for status
                String columnName = table.getColumnName(column);
                if (columnName.equals("Status")) {
                    String cellValue = value != null ? value.toString() : "";
                    if ("Pass".equals(cellValue)) {
                        c.setForeground(new Color(52, 168, 83));
                    } else if ("Fail".equals(cellValue)) {
                        c.setForeground(new Color(220, 53, 69));
                    }
                }
                
                // Center align numeric columns
                if (value instanceof Number || columnName.equals("Percentage") || 
                    columnName.equals("SGPA") || columnName.equals("Grade") || 
                    columnName.equals("Rank") || columnName.equals("Failed Subjects")) {
                    ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                }
                
                return c;
            }
        });
        
        // Auto-resize columns
        autoResizeColumns();
        
        // Force table to repaint
        resultTable.revalidate();
        resultTable.repaint();
    }
    
    private ExtendedStudentData getExtendedStudentData(Student student, String section, List<String> selectedSubjects) {
        if (student == null) {
            return null;
        }
        
        ExtendedStudentData data = new ExtendedStudentData();
        data.name = student.getName();
        data.rollNumber = student.getRollNumber();
        data.section = section;
        data.subjectMarks = student.getMarks() != null ? student.getMarks() : new HashMap<String, Map<String, Integer>>();
        
        System.out.println("DEBUG: Processing student: " + data.name + " (" + data.rollNumber + ")");
        // Get section ID for this section
        int sectionId = getSectionIdByName(section);
        // Get additional info from database
        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT s.email, s.phone, sec.academic_year, sec.semester " +
                          "FROM students s " +
                          "JOIN sections sec ON s.section_id = sec.id " +
                          "WHERE s.roll_number = ? AND s.created_by = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, student.getRollNumber());
            ps.setInt(2, com.sms.login.LoginScreen.currentUserId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                data.email = rs.getString("email");
                data.phone = rs.getString("phone");
                data.year = rs.getInt("academic_year");
                data.semester = rs.getInt("semester");
            }
            
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.out.println("DEBUG: SQL Error getting student info: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Calculate academic metrics
        if (sectionId > 0) {
            calculateAcademicMetrics(data, sectionId);
        } else {
            // Use default calculations if section ID not found
            data.totalMarks = 0;
            for (Map<String, Integer> examTypes : data.subjectMarks.values()) {
                if (examTypes != null) {
                    for (Integer mark : examTypes.values()) {
                        if (mark != null) {
                            data.totalMarks += mark;
                        }
                    }
                }
            }
            data.percentage = 0.0;
            data.sgpa = 0.0;
            data.grade = "N/A";
            data.status = "N/A";
            data.failedSubjectsCount = 0;
        }
        
        return data;
    }
    private void calculateAcademicMetrics(ExtendedStudentData data, int sectionId) {
        data.totalMarks = 0;
        data.totalMaxMarks = 0;
        data.totalCredits = 0;
        data.totalGradePoints = 0;
        data.failedSubjectsCount = 0;
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            // Get student ID from roll number
            String studentIdQuery = "SELECT id FROM students WHERE roll_number = ? AND section_id = ?";
            PreparedStatement psStudent = conn.prepareStatement(studentIdQuery);
            psStudent.setString(1, data.rollNumber);
            psStudent.setInt(2, sectionId);
            ResultSet rsStudent = psStudent.executeQuery();
            
            int studentId = 0;
            if (rsStudent.next()) {
                studentId = rsStudent.getInt("id");
            }
            rsStudent.close();
            psStudent.close();
            
            if (studentId == 0) {
                System.out.println("ERROR: Student ID not found for roll number: " + data.rollNumber);
                return;
            }
            
            // Use AnalyzerDAO to get student marks the same way Section Analyzer does
            AnalyzerDAO analyzerDAO = new AnalyzerDAO();
            
            // Get marks for this student (same approach as Section Analyzer)
            Map<String, Map<String, Integer>> studentMarks = getStudentMarksFromDB(studentId, sectionId);
            data.subjectMarks = studentMarks;
            
            // Get subject details with credits  
            String query = """
                SELECT sub.id, sub.subject_name, ss.max_marks, ss.passing_marks, ss.credit
                FROM section_subjects ss
                JOIN subjects sub ON ss.subject_id = sub.id
                WHERE ss.section_id = ?
            """;
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, sectionId);
            ResultSet rs = ps.executeQuery();
            
            double totalWeightedPercentage = 0;
            int subjectCount = 0;
            
            while (rs.next()) {
                int subjectId = rs.getInt("id");
                String subjectName = rs.getString("subject_name");
                int maxMarks = rs.getInt("max_marks");
                int credit = rs.getInt("credit");
                
                // Only process if this subject was in the marks data
                if (!data.subjectMarks.containsKey(subjectName)) {
                    continue;
                }
                
                // Get selected filters for this subject (or null for all)
                Set<String> examFilter = selectedFilters.get(subjectName);
                
                // Calculate weighted total using AnalyzerDAO with DUAL PASSING logic (same as Section Analyzer)
                AnalyzerDAO.SubjectPassResult result = analyzerDAO.calculateWeightedSubjectTotalWithPass(
                    studentId, sectionId, subjectName, examFilter);
                
                double weightedPercentage = Math.abs(result.percentage);
                boolean passed = result.passed;
                
                // Store subject-level details (same as Section Analyzer)
                data.subjectWeightedTotals.put(subjectName, weightedPercentage);
                data.subjectPassStatus.put(subjectName, passed);
                data.subjectFailedComponents.put(subjectName, result.failedComponents);
                
                // Aggregate for overall calculation
                totalWeightedPercentage += weightedPercentage;
                subjectCount++;
                data.totalCredits += credit;
                
                // Calculate grade points for SGPA (based on weighted percentage)
                double gradePoint = calculateGradePoint((int)weightedPercentage, 100);
                data.totalGradePoints += (gradePoint * credit);
                
                // Check if failed (Component-level passing only - ignore weighted %)
                // Student fails if ANY component didn't meet its passing threshold
                if (!result.allComponentsPassed) {
                    data.failedSubjectsCount++;
                    System.out.println("FAILED Subject: " + subjectName + 
                                     " | Weighted%: " + String.format("%.2f", weightedPercentage) +
                                     " | Failed Components: " + result.failedComponents);
                }
            }
            
            rs.close();
            ps.close();
            
            // Calculate total marks as sum of weighted percentages
            if (subjectCount > 0) {
                data.totalMarks = totalWeightedPercentage; // Sum of all subject weighted %
                data.totalMaxMarks = subjectCount * 100; // Max is 100 per subject
                data.percentage = (data.totalMarks / data.totalMaxMarks) * 100; // Percentage for 100 scale
            } else {
                data.totalMarks = 0.0;
                data.totalMaxMarks = 0;
                data.percentage = 0.0;
            }
            
            // Calculate CGPA using proper formula:
            // CGPA = Σ[(subjectPercentage/10) × credit] / Σ[credit]
            if (data.totalCredits > 0) {
                double totalWeightedPoints = 0.0;
                for (Map.Entry<String, Double> entry : data.subjectWeightedTotals.entrySet()) {
                    // Get credit for this subject from database
                    String subjectName = entry.getKey();
                    double percentage = entry.getValue();
                    
                    // Find credit for this subject
                    try {
                        String creditQuery = "SELECT ss.credit FROM section_subjects ss " +
                                           "JOIN subjects sub ON ss.subject_id = sub.id " +
                                           "WHERE ss.section_id = ? AND sub.subject_name = ?";
                        PreparedStatement psCredit = conn.prepareStatement(creditQuery);
                        psCredit.setInt(1, sectionId);
                        psCredit.setString(2, subjectName);
                        ResultSet rsCredit = psCredit.executeQuery();
                        if (rsCredit.next()) {
                            int credit = rsCredit.getInt("credit");
                            totalWeightedPoints += (percentage / 10.0) * credit;
                        }
                        rsCredit.close();
                        psCredit.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                data.sgpa = totalWeightedPoints / data.totalCredits;
            } else {
                data.sgpa = 0.0;
            }
            
            // Calculate grade
            data.grade = calculateGrade(data.percentage);
            
            // Determine status (fail if ANY subject failed)
            data.status = (data.failedSubjectsCount == 0) ? "Pass" : "Fail";
            
            // Get launched results info
            data.launchedResultsInfo = getLaunchedResultsInfo(studentId);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Helper method to get student marks from database (same approach as Section Analyzer)
    private Map<String, Map<String, Integer>> getStudentMarksFromDB(int studentId, int sectionId) {
        Map<String, Map<String, Integer>> marks = new HashMap<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            // Same query as Section Analyzer uses
            String query = "SELECT sub.subject_name, et.exam_name, sm.marks_obtained " +
                          "FROM entered_exam_marks sm " +
                          "JOIN subjects sub ON sm.subject_id = sub.id " +
                          "JOIN exam_types et ON sm.exam_type_id = et.id " +
                          "WHERE sm.student_id = ? " +
                          "ORDER BY sub.subject_name, et.exam_name";
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                String subjectName = rs.getString("subject_name");
                String examName = rs.getString("exam_name");
                int marksObtained = rs.getInt("marks_obtained");
                
                marks.computeIfAbsent(subjectName, k -> new HashMap<>()).put(examName, marksObtained);
            }
            
            rs.close();
            ps.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return marks;
    }
    
    private String getLaunchedResultsInfo(int studentId) {
        StringBuilder info = new StringBuilder();
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            String query = "SELECT launch_id, created_at " +
                          "FROM launched_student_results " +
                          "WHERE student_id = ? " +
                          "ORDER BY created_at DESC";
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd-MMM-yyyy");
            
            while (rs.next()) {
                if (info.length() > 0) {
                    info.append("; ");
                }
                info.append("Launch #")
                    .append(rs.getInt("launch_id"))
                    .append(" (")
                    .append(dateFormat.format(rs.getTimestamp("created_at")))
                    .append(")");
            }
            
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error fetching launched results";
        }
        
        return info.length() > 0 ? info.toString() : "None";
    }
    
    private List<ExtendedStudentData> getStudentsFromLaunchedResult(int launchId, List<String> selectedSubjects) {
        List<ExtendedStudentData> studentDataList = new ArrayList<>();
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            // Get all students from this launch with their result data
            String query = "SELECT lsr.student_id, lsr.created_at, lsr.result_data, " +
                          "s.student_name, s.roll_number, s.email, s.phone, sec.section_name, sec.academic_year, sec.semester " +
                          "FROM launched_student_results lsr " +
                          "JOIN students s ON lsr.student_id = s.id " +
                          "JOIN sections sec ON s.section_id = sec.id " +
                          "WHERE lsr.launch_id = ? " +
                          "ORDER BY s.roll_number";
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, launchId);
            ResultSet rs = ps.executeQuery();
            
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd-MMM-yyyy hh:mm a");
            
            while (rs.next()) {
                ExtendedStudentData data = new ExtendedStudentData();
                data.studentId = rs.getInt("student_id");
                data.name = rs.getString("student_name");
                data.rollNumber = rs.getString("roll_number");
                data.email = rs.getString("email");
                data.phone = rs.getString("phone");
                data.section = rs.getString("section_name");
                data.year = rs.getInt("academic_year");
                data.semester = rs.getInt("semester");
                data.launchDate = dateFormat.format(rs.getTimestamp("created_at"));
                
                // Parse result_data JSON to get subject marks
                String resultData = rs.getString("result_data");
                if (resultData != null && !resultData.isEmpty()) {
                    parseResultData(data, resultData, selectedSubjects);
                }
                
                studentDataList.add(data);
            }
            
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return studentDataList;
    }
    
    private void parseResultData(ExtendedStudentData data, String jsonData, List<String> selectedSubjects) {
        // Parse nested JSON structure with "subjects" array
        try {
            // First, extract subject names and their exam_types for display
            Map<String, List<String>> subjectExamTypes = new HashMap<>();
            
            // Use regex to find each subject block with its exam_types array
            java.util.regex.Pattern subjectPattern = java.util.regex.Pattern.compile(
                "\"subject_name\":\"([^\"]+)\".*?\"exam_types\":\\[(.*?)\\].*?\"weighted_total\":(\\d+(?:\\.\\d+)?).*?\"passed\":(true|false)", 
                java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher subjectMatcher = subjectPattern.matcher(jsonData);
            
            double totalPercentage = 0;
            int subjectCount = 0;
            int failedCount = 0;
            
            while (subjectMatcher.find()) {
                String subjectName = subjectMatcher.group(1);
                String examTypesArray = subjectMatcher.group(2);
                double weightedTotal = Double.parseDouble(subjectMatcher.group(3));
                boolean passed = "true".equals(subjectMatcher.group(4));
                
                // Extract exam type names, obtained marks, AND max marks from the array
                List<String> examTypesList = new ArrayList<>();
                Map<String, Integer> examMarksMap = new HashMap<>();
                Map<String, Integer> examMaxMarksMap = new HashMap<>();
                
                // Pattern to match: "exam_name":"Internal","obtained":18,"max":20,"weightage":20
                java.util.regex.Pattern examTypePattern = java.util.regex.Pattern.compile(
                    "\"exam_name\":\"([^\"]+)\".*?\"obtained\":(\\d+).*?\"max\":(\\d+)");
                java.util.regex.Matcher examTypeMatcher = examTypePattern.matcher(examTypesArray);
                
                while (examTypeMatcher.find()) {
                    String examName = examTypeMatcher.group(1);
                    int obtainedMarks = Integer.parseInt(examTypeMatcher.group(2));
                    int maxMarks = Integer.parseInt(examTypeMatcher.group(3));
                    
                    examTypesList.add(examName);
                    examMarksMap.put(examName, obtainedMarks);
                    examMaxMarksMap.put(examName, maxMarks);
                }
                
                subjectExamTypes.put(subjectName, examTypesList);
                data.subjectMarks.put(subjectName, examMarksMap);
                
                // Store max marks for header display
                if (!examMaxMarksMap.isEmpty()) {
                    if (data.subjectMaxMarks == null) {
                        data.subjectMaxMarks = new HashMap<>();
                    }
                    data.subjectMaxMarks.put(subjectName, examMaxMarksMap);
                }
                
                System.out.println("DEBUG: Found subject: " + subjectName + " = " + weightedTotal + " (" + (passed ? "passed" : "failed") + ")");
                data.subjectWeightedTotals.put(subjectName, weightedTotal);
                data.subjectPassStatus.put(subjectName, passed);
                
                totalPercentage += weightedTotal;
                subjectCount++;
                
                if (!passed) {
                    failedCount++;
                }
            }
            
            // Store exam types information for display
            data.subjectExamTypes = subjectExamTypes;
            // Extract overall data
            if (subjectCount > 0) {
                String overallPercentageStr = extractJsonValue(jsonData, "percentage");
                String cgpaStr = extractJsonValue(jsonData, "cgpa");
                String gradeStr = extractJsonValue(jsonData, "grade");
                String isPassingStr = extractJsonValue(jsonData, "is_passing");
                
                if (overallPercentageStr != null) {
                    data.percentage = Double.parseDouble(overallPercentageStr);
                } else {
                    data.percentage = totalPercentage / subjectCount;
                }
                
                if (cgpaStr != null) {
                    data.sgpa = Double.parseDouble(cgpaStr);
                } else {
                    data.sgpa = data.percentage / 10.0;
                }
                
                data.grade = gradeStr != null ? gradeStr : calculateGrade(data.percentage);
                data.status = "true".equals(isPassingStr) ? "Pass" : "Fail";
                data.failedSubjectsCount = failedCount;
                // Total marks should be sum of all subject weighted totals
                data.totalMarks = totalPercentage;
            }
        } catch (Exception e) {
            System.err.println("ERROR parsing result data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String extractJsonValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\":";
            int startIndex = json.indexOf(searchKey);
            if (startIndex == -1) return null;
            
            startIndex += searchKey.length();
            
            // Skip whitespace
            while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
                startIndex++;
            }
            
            // Check if value is a string (starts with ")
            if (json.charAt(startIndex) == '"') {
                startIndex++; // Skip opening quote
                int endIndex = json.indexOf('"', startIndex);
                if (endIndex == -1) return null;
                return json.substring(startIndex, endIndex);
            } else {
                // Value is a number or boolean
                int endIndex = startIndex;
                while (endIndex < json.length() && 
                       (Character.isDigit(json.charAt(endIndex)) || 
                        json.charAt(endIndex) == '.' || 
                        json.charAt(endIndex) == '-' ||
                        Character.isLetter(json.charAt(endIndex)))) {
                    endIndex++;
                }
                return json.substring(startIndex, endIndex);
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    
    private double calculateGradePoint(int marks, int maxMarks) {
        double percentage = (marks * 100.0) / maxMarks;
        
        if (percentage >= 90) return 10.0;
        if (percentage >= 80) return 9.0;
        if (percentage >= 70) return 8.0;
        if (percentage >= 60) return 7.0;
        if (percentage >= 50) return 6.0;
        if (percentage >= 40) return 5.0;
        return 0.0;
    }
    
    private String calculateGrade(double percentage) {
        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B+";
        if (percentage >= 60) return "B";
        if (percentage >= 50) return "C";
        if (percentage >= 40) return "D";
        return "F";
    }
    
    private void calculateRanks(List<ExtendedStudentData> students) {
        // Sort by total marks in descending order
        students.sort((s1, s2) -> Double.compare(s2.totalMarks, s1.totalMarks));
        
        // Assign ranks
        int currentRank = 1;
        for (int i = 0; i < students.size(); i++) {
            if (i > 0 && students.get(i).totalMarks < students.get(i-1).totalMarks) {
                currentRank = i + 1;
            }
            students.get(i).rank = currentRank;
        }
    }
    
    private int getSectionIdByName(String sectionName) {
        for (Map.Entry<Integer, SectionInfo> entry : sectionInfoMap.entrySet()) {
            if (entry.getValue().sectionName.equals(sectionName)) {
                return entry.getKey();
            }
        }
        return -1;
    }
    
    private void autoResizeColumns() {
        TableColumnModel columnModel = resultTable.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            TableColumn column = columnModel.getColumn(i);
            String columnName = column.getHeaderValue().toString();
            
            // Set preferred widths based on column type
            if (columnName.equals("Name")) {
                column.setPreferredWidth(150);
            } else if (columnName.equals("Email")) {
                column.setPreferredWidth(180);
            } else if (columnName.equals("Roll Number") || columnName.equals("Phone")) {
                column.setPreferredWidth(100);
            } else if (columnName.equals("Section") || columnName.equals("Grade") || 
                      columnName.equals("Status") || columnName.equals("Rank")) {
                column.setPreferredWidth(60);
            } else if (columnName.equals("Percentage") || columnName.equals("SGPA")) {
                column.setPreferredWidth(80);
            } else if (columnName.equals("Failed Subjects")) {
                column.setPreferredWidth(100);
            } else {
                column.setPreferredWidth(80);
            }
        }
    }

    private void exportToExcel() {
        if (resultTable.getRowCount() == 0) {
            showStyledMessage("No data to export! Please click 'Show Data' first.", "No Data", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        FileOutputStream fileOut = null;
        Workbook workbook = null;
        
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Excel File");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Files (*.xlsx)", "xlsx"));
            
            String defaultFileName = "Student_Data_" + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            fileChooser.setSelectedFile(new File(defaultFileName + ".xlsx"));
            
            int userSelection = fileChooser.showSaveDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                String filePath = fileToSave.getAbsolutePath();
                if (!filePath.endsWith(".xlsx")) {
                    filePath += ".xlsx";
                }
                
                System.out.println("Saving Excel file to: " + filePath); // Debug
                
                workbook = new XSSFWorkbook();
                Sheet sheet = workbook.createSheet("Student Data");
                
                // Create column header row at row 4 with modern styling
                Row headerRow = sheet.createRow(4);
                headerRow.setHeightInPoints(25);
                CellStyle headerStyle = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerFont.setFontHeightInPoints((short) 11);
                headerFont.setColor(IndexedColors.WHITE.getIndex());
                headerStyle.setFont(headerFont);
                headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                headerStyle.setBorderBottom(BorderStyle.MEDIUM);
                headerStyle.setBorderTop(BorderStyle.MEDIUM);
                headerStyle.setBorderLeft(BorderStyle.THIN);
                headerStyle.setBorderRight(BorderStyle.THIN);
                headerStyle.setAlignment(HorizontalAlignment.CENTER);
                headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                
                // ============ MODERN HEADER SECTION ============
                // Create title rows
                Row titleRow = sheet.createRow(0);
                titleRow.setHeightInPoints(30);
                Cell titleCell = titleRow.createCell(0);
                titleCell.setCellValue("ACADEMIC MANAGEMENT SYSTEM");
                
                CellStyle titleStyle = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
                titleFont.setFontHeightInPoints((short) 18);
                titleFont.setBold(true);
                titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
                titleStyle.setFont(titleFont);
                titleStyle.setAlignment(HorizontalAlignment.CENTER);
                titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                titleCell.setCellStyle(titleStyle);
                
                // Subtitle
                Row subtitleRow = sheet.createRow(1);
                subtitleRow.setHeightInPoints(25);
                Cell subtitleCell = subtitleRow.createCell(0);
                subtitleCell.setCellValue("Student Performance Report");
                
                CellStyle subtitleStyle = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font subtitleFont = workbook.createFont();
                subtitleFont.setFontHeightInPoints((short) 14);
                subtitleFont.setBold(true);
                subtitleFont.setColor(IndexedColors.GREY_80_PERCENT.getIndex());
                subtitleStyle.setFont(subtitleFont);
                subtitleStyle.setAlignment(HorizontalAlignment.CENTER);
                subtitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                subtitleCell.setCellStyle(subtitleStyle);
                
                // Info row
                Row infoRow = sheet.createRow(2);
                infoRow.setHeightInPoints(18);
                Cell infoCell = infoRow.createCell(0);
                String selectedSection = (String) sectionDropdown.getSelectedItem();
                String sectionInfo = selectedSection != null ? selectedSection : "All Sections";
                infoCell.setCellValue("Section: " + sectionInfo + " | Generated: " + 
                    new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a").format(new java.util.Date()));
                
                CellStyle infoStyle = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font infoFont = workbook.createFont();
                infoFont.setFontHeightInPoints((short) 10);
                infoFont.setItalic(true);
                infoFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
                infoStyle.setFont(infoFont);
                infoStyle.setAlignment(HorizontalAlignment.CENTER);
                infoCell.setCellStyle(infoStyle);
                
                // Empty row for spacing
                sheet.createRow(3);
                
                // Add column headers with modern styling
                for (int i = 0; i < resultTable.getColumnCount(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(resultTable.getColumnName(i));
                    cell.setCellStyle(headerStyle);
                }
                
                // Create modern cell styles for data
                CellStyle dataStyle = workbook.createCellStyle();
                dataStyle.setBorderBottom(BorderStyle.THIN);
                dataStyle.setBorderTop(BorderStyle.THIN);
                dataStyle.setBorderLeft(BorderStyle.THIN);
                dataStyle.setBorderRight(BorderStyle.THIN);
                dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                
                // Alternating row style
                CellStyle altRowStyle = workbook.createCellStyle();
                altRowStyle.cloneStyleFrom(dataStyle);
                altRowStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                altRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                
                // Pass style - modern green
                CellStyle passStyle = workbook.createCellStyle();
                passStyle.cloneStyleFrom(dataStyle);
                passStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
                passStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                org.apache.poi.ss.usermodel.Font passFont = workbook.createFont();
                passFont.setColor(IndexedColors.DARK_GREEN.getIndex());
                passFont.setBold(true);
                passStyle.setFont(passFont);
                
                // Fail style - modern red
                CellStyle failStyle = workbook.createCellStyle();
                failStyle.cloneStyleFrom(dataStyle);
                failStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
                failStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                org.apache.poi.ss.usermodel.Font failFont = workbook.createFont();
                failFont.setColor(IndexedColors.RED.getIndex());
                failFont.setBold(true);
                failStyle.setFont(failFont);
                
                // Bold style for important columns
                CellStyle boldStyle = workbook.createCellStyle();
                boldStyle.cloneStyleFrom(dataStyle);
                org.apache.poi.ss.usermodel.Font boldFont = workbook.createFont();
                boldFont.setBold(true);
                boldStyle.setFont(boldFont);
                
                CellStyle boldAltStyle = workbook.createCellStyle();
                boldAltStyle.cloneStyleFrom(altRowStyle);
                boldAltStyle.setFont(boldFont);
                
                // Create data rows with modern styling
                for (int i = 0; i < resultTable.getRowCount(); i++) {
                    Row row = sheet.createRow(i + 5); // Start after header section
                    row.setHeightInPoints(20); // Taller rows for better readability
                    
                    boolean isAltRow = (i % 2 == 1);
                    
                    for (int j = 0; j < resultTable.getColumnCount(); j++) {
                        Cell cell = row.createCell(j);
                        Object value = resultTable.getValueAt(i, j);
                        String colName = resultTable.getColumnName(j);
                        
                        // Determine if this is an important column (for bold)
                        boolean isImportant = colName.contains("Name") || colName.contains("Total") ||
                                            colName.contains("CGPA") || colName.contains("Grade") ||
                                            colName.contains("Rank");
                        
                        // Set cell value based on type
                        if (value == null) {
                            cell.setCellValue("");
                        } else if (value instanceof Integer) {
                            cell.setCellValue((Integer) value);
                        } else if (value instanceof Double) {
                            cell.setCellValue((Double) value);
                        } else if (value instanceof String) {
                            String strValue = (String) value;
                            // Handle percentage and other formatted values
                            if (strValue.endsWith("%")) {
                                try {
                                    double percentValue = Double.parseDouble(strValue.replace("%", ""));
                                    cell.setCellValue(percentValue / 100.0);
                                    CellStyle percentStyle = workbook.createCellStyle();
                                    percentStyle.cloneStyleFrom(isAltRow ? altRowStyle : dataStyle);
                                    if (isImportant) percentStyle.setFont(boldFont);
                                    percentStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));
                                    cell.setCellStyle(percentStyle);
                                } catch (NumberFormatException e) {
                                    cell.setCellValue(strValue);
                                    cell.setCellStyle(isImportant ? (isAltRow ? boldAltStyle : boldStyle) : (isAltRow ? altRowStyle : dataStyle));
                                }
                            } else {
                                cell.setCellValue(strValue);
                                cell.setCellStyle(isImportant ? (isAltRow ? boldAltStyle : boldStyle) : (isAltRow ? altRowStyle : dataStyle));
                            }
                        } else {
                            cell.setCellValue(value.toString());
                            cell.setCellStyle(isImportant ? (isAltRow ? boldAltStyle : boldStyle) : (isAltRow ? altRowStyle : dataStyle));
                        }
                        
                        // Apply conditional formatting for status
                        if (colName.equals("Status")) {
                            String cellValue = value != null ? value.toString() : "";
                            if ("Pass".equals(cellValue)) {
                                cell.setCellStyle(passStyle);
                            } else if ("Fail".equals(cellValue)) {
                                cell.setCellStyle(failStyle);
                            }
                        }
                        
                        // Center align for numeric columns
                        if (!colName.contains("Name") && !colName.contains("Email") && 
                            !colName.contains("Phone") && !colName.contains("Section")) {
                            CellStyle currentStyle = cell.getCellStyle();
                            if (currentStyle != passStyle && currentStyle != failStyle) {
                                CellStyle centeredStyle = workbook.createCellStyle();
                                centeredStyle.cloneStyleFrom(currentStyle);
                                centeredStyle.setAlignment(HorizontalAlignment.CENTER);
                                cell.setCellStyle(centeredStyle);
                            }
                        }
                    }
                }
                
                // Merge cells for title section
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, resultTable.getColumnCount() - 1));
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, resultTable.getColumnCount() - 1));
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 2, 0, resultTable.getColumnCount() - 1));
                
                // Auto-size columns with better spacing
                for (int i = 0; i < resultTable.getColumnCount(); i++) {
                    sheet.autoSizeColumn(i);
                    // Add generous extra space for readability
                    int currentWidth = sheet.getColumnWidth(i);
                    sheet.setColumnWidth(i, currentWidth + 1200);
                }
                
                // Set print settings for professional output
                sheet.setFitToPage(true);
                sheet.getPrintSetup().setFitWidth((short) 1);
                sheet.getPrintSetup().setFitHeight((short) 0);
                sheet.getPrintSetup().setLandscape(true);
                sheet.setHorizontallyCenter(true);
                
                // Write to file
                fileOut = new FileOutputStream(filePath);
                workbook.write(fileOut);
                fileOut.flush();
                
                System.out.println("Excel file saved successfully!"); // Debug
                
                showStyledMessage("Data exported successfully to:\n" + filePath, "Export Successful", JOptionPane.INFORMATION_MESSAGE);
                
                // Optionally open the file
                if (Desktop.isDesktopSupported()) {
                    int result = JOptionPane.showConfirmDialog(this, 
                        "Do you want to open the exported file?", 
                        "Open File", 
                        JOptionPane.YES_NO_OPTION);
                    if (result == JOptionPane.YES_OPTION) {
                        Desktop.getDesktop().open(new File(filePath));
                    }
                }
                
            }
        } catch (Exception e) {
            showStyledMessage("Error exporting to Excel:\n" + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            // Ensure resources are closed
            try {
                if (fileOut != null) {
                    fileOut.close();
                }
                if (workbook != null) {
                    workbook.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private void exportToPdf() {
        if (resultTable.getRowCount() == 0) {
            showStyledMessage("No data to export! Please click 'Show Data' first.", "No Data", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save PDF File");
            fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Files (*.pdf)", "pdf"));
            
            // Fix: Use fully qualified Date class
            String defaultFileName = "Student_Data_" + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            fileChooser.setSelectedFile(new File(defaultFileName + ".pdf"));
            
            int userSelection = fileChooser.showSaveDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                String filePath = fileToSave.getAbsolutePath();
                if (!filePath.endsWith(".pdf")) {
                    filePath += ".pdf";
                }
                
                // Use A4 landscape for professional report
                Document document = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
                PdfWriter.getInstance(document, new FileOutputStream(filePath));
                document.open();
                
                // Add Logo
                try {
                    java.io.InputStream logoStream = null;
                    // Try multiple paths
                    logoStream = getClass().getClassLoader().getResourceAsStream("resources/images/AA LOGO.png");
                    if (logoStream == null) {
                        // Try from working directory
                        java.io.File logoFile = new java.io.File("resources/images/AA LOGO.png");
                        if (logoFile.exists()) {
                            logoStream = new java.io.FileInputStream(logoFile);
                        }
                    }
                    if (logoStream == null) {
                        // Try from installed app directory
                        java.io.File appLogoFile = new java.io.File(System.getProperty("user.dir") + "/resources/images/AA LOGO.png");
                        if (appLogoFile.exists()) {
                            logoStream = new java.io.FileInputStream(appLogoFile);
                        }
                    }
                    if (logoStream != null) {
                        byte[] logoBytes = logoStream.readAllBytes();
                        logoStream.close();
                        com.itextpdf.text.Image logo = com.itextpdf.text.Image.getInstance(logoBytes);
                        logo.scaleToFit(120, 72);
                        logo.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                        document.add(logo);
                        document.add(new Paragraph(" "));
                    }
                } catch (Exception ex) {
                    // If logo not found, continue without it
                    System.err.println("Warning: Could not load logo: " + ex.getMessage());
                }
                
                // ============ MODERN HEADER SECTION ============
                // Institution name
                com.itextpdf.text.Font institutionFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 20, com.itextpdf.text.Font.BOLD,
                    new com.itextpdf.text.BaseColor(33, 37, 41));
                Paragraph institution = new Paragraph("Academic Management System", institutionFont);
                institution.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                institution.setSpacingAfter(4);
                document.add(institution);
                
                // Report title
                com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 16, com.itextpdf.text.Font.BOLD,
                    new com.itextpdf.text.BaseColor(52, 58, 64));
                Paragraph title = new Paragraph("Student Performance Report", titleFont);
                title.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                title.setSpacingAfter(2);
                document.add(title);
                
                // Get section info
                String selectedSection = (String) sectionDropdown.getSelectedItem();
                String sectionInfo = selectedSection != null ? selectedSection : "All Sections";
                
                // Section and date info
                com.itextpdf.text.Font infoFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.NORMAL,
                    new com.itextpdf.text.BaseColor(108, 117, 125));
                Paragraph info = new Paragraph(
                    "Section: " + sectionInfo + "  |  Generated: " + 
                    new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a").format(new java.util.Date()),
                    infoFont
                );
                info.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                info.setSpacingAfter(12);
                document.add(info);
                
                // Decorative line
                com.itextpdf.text.pdf.draw.LineSeparator line = new com.itextpdf.text.pdf.draw.LineSeparator();
                line.setLineColor(new com.itextpdf.text.BaseColor(52, 143, 226));
                line.setLineWidth(2);
                document.add(new com.itextpdf.text.Chunk(line));
                document.add(new Paragraph("\n"));
                
                // ============ DATA TABLE ============
                int columnCount = resultTable.getColumnCount();
                PdfPTable pdfTable = new PdfPTable(columnCount);
                pdfTable.setWidthPercentage(100);
                pdfTable.setSpacingBefore(5);
                
                // Calculate font sizes based on column count for better fitting
                int headerFontSize = columnCount > 35 ? 5 : (columnCount > 30 ? 5 : (columnCount > 25 ? 6 : (columnCount > 20 ? 7 : (columnCount > 15 ? 8 : 9))));
                int dataFontSize = columnCount > 35 ? 4 : (columnCount > 30 ? 5 : (columnCount > 25 ? 5 : (columnCount > 20 ? 6 : (columnCount > 15 ? 7 : 8))));
                
                // Set relative column widths based on content type - very aggressive for many columns
                float[] columnWidths = new float[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    String colName = resultTable.getColumnName(i);
                    if (colName.contains("Name")) {
                        columnWidths[i] = columnCount > 35 ? 2.0f : (columnCount > 25 ? 2.2f : (columnCount > 20 ? 2.5f : 3.5f));
                    } else if (colName.contains("Roll")) {
                        columnWidths[i] = columnCount > 35 ? 1.2f : (columnCount > 25 ? 1.3f : (columnCount > 20 ? 1.5f : 2.0f));
                    } else if (colName.contains("Email")) {
                        columnWidths[i] = columnCount > 35 ? 1.5f : (columnCount > 25 ? 1.7f : (columnCount > 20 ? 2.0f : 2.8f));
                    } else if (colName.contains("Section") || colName.contains("Year") || colName.contains("Semester")) {
                        columnWidths[i] = columnCount > 35 ? 0.7f : (columnCount > 25 ? 0.8f : (columnCount > 20 ? 1.0f : 1.3f));
                    } else if (colName.contains("Total Marks") || colName.contains("Percentage")) {
                        columnWidths[i] = columnCount > 35 ? 1.0f : (columnCount > 25 ? 1.2f : (columnCount > 20 ? 1.5f : 2.0f));
                    } else if (colName.contains("CGPA") || colName.contains("Grade")) {
                        columnWidths[i] = columnCount > 35 ? 0.6f : (columnCount > 25 ? 0.7f : (columnCount > 20 ? 1.0f : 1.3f));
                    } else if (colName.contains("Status") || colName.contains("Rank")) {
                        columnWidths[i] = columnCount > 35 ? 0.7f : (columnCount > 25 ? 0.8f : (columnCount > 20 ? 1.0f : 1.3f));
                    } else if (colName.contains("Phone")) {
                        columnWidths[i] = columnCount > 35 ? 1.2f : (columnCount > 25 ? 1.3f : (columnCount > 20 ? 1.5f : 2.0f));
                    } else {
                        // Exam marks and subject columns - very compact
                        columnWidths[i] = columnCount > 35 ? 0.6f : (columnCount > 25 ? 0.65f : (columnCount > 20 ? 0.8f : 1.2f));
                    }
                }
                pdfTable.setWidths(columnWidths);
                
                // Add headers with modern styling and better text wrapping
                com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, headerFontSize, com.itextpdf.text.Font.BOLD,
                    new com.itextpdf.text.BaseColor(255, 255, 255));
                    
                for (int i = 0; i < columnCount; i++) {
                    String colName = resultTable.getColumnName(i);
                    
                    // Abbreviate long column names for better fitting
                    String displayName = colName;
                    if (columnCount > 20) {
                        displayName = displayName.replace("Internal", "Int")
                                                 .replace("External", "Ext")
                                                 .replace("Assignment", "Assgn")
                                                 .replace("Examination", "Exam")
                                                 .replace("Practical", "Pract")
                                                 .replace("Theory", "Th")
                                                 .replace("Final", "Fin");
                    }
                    
                    com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(
                        new com.itextpdf.text.Phrase(displayName, headerFont));
                    cell.setBackgroundColor(new com.itextpdf.text.BaseColor(52, 143, 226));
                    cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                    cell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
                    cell.setRotation(0); // Ensure text is NOT rotated
                    
                    // Adjust padding based on column count - very tight for many columns
                    int padding = columnCount > 35 ? 1 : (columnCount > 25 ? 1 : (columnCount > 20 ? 2 : (columnCount > 15 ? 3 : 4)));
                    cell.setPadding(padding);
                    cell.setPaddingTop(padding + 1);
                    cell.setPaddingBottom(padding + 1);
                    cell.setBorderWidth(0.3f);
                    cell.setBorderColor(new com.itextpdf.text.BaseColor(255, 255, 255));
                    
                    // Enable text wrapping for long headers
                    cell.setNoWrap(false);
                    
                    pdfTable.addCell(cell);
                }
                
                // Add data with professional formatting
                com.itextpdf.text.Font dataFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, dataFontSize, com.itextpdf.text.Font.NORMAL,
                    new com.itextpdf.text.BaseColor(33, 37, 41));
                com.itextpdf.text.Font boldDataFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, dataFontSize, com.itextpdf.text.Font.BOLD,
                    new com.itextpdf.text.BaseColor(33, 37, 41));
                    
                for (int i = 0; i < resultTable.getRowCount(); i++) {
                    for (int j = 0; j < columnCount; j++) {
                        Object value = resultTable.getValueAt(i, j);
                        String cellValue = value != null ? value.toString() : "";
                        String colName = resultTable.getColumnName(j);
                        
                        // Use bold font for important columns
                        com.itextpdf.text.Font currentFont = 
                            (colName.contains("Name") || colName.contains("Total") || 
                             colName.contains("CGPA") || colName.contains("Grade") || 
                             colName.contains("Rank")) ? boldDataFont : dataFont;
                        
                        com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(
                            new com.itextpdf.text.Phrase(cellValue, currentFont));
                        
                        // Alignment based on content
                        if (colName.contains("Name") || colName.contains("Email") || 
                            colName.contains("Section") || colName.contains("Phone")) {
                            cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_LEFT);
                        } else {
                            cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                        }
                        cell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
                        
                        // Very compact padding for many columns
                        int padding = columnCount > 35 ? 1 : (columnCount > 25 ? 1 : (columnCount > 20 ? 2 : (columnCount > 15 ? 3 : 4)));
                        cell.setPadding(padding);
                        
                        // Modern alternating row colors
                        if (i % 2 == 0) {
                            cell.setBackgroundColor(new com.itextpdf.text.BaseColor(248, 249, 250));
                        } else {
                            cell.setBackgroundColor(new com.itextpdf.text.BaseColor(255, 255, 255));
                        }
                        
                        // Professional color coding for status
                        if (colName.equals("Status")) {
                            if ("Pass".equals(cellValue)) {
                                cell.setBackgroundColor(new com.itextpdf.text.BaseColor(212, 237, 218));
                                com.itextpdf.text.Font passFont = new com.itextpdf.text.Font(
                                    com.itextpdf.text.Font.FontFamily.HELVETICA, dataFontSize, com.itextpdf.text.Font.BOLD,
                                    new com.itextpdf.text.BaseColor(25, 135, 84));
                                cell.setPhrase(new com.itextpdf.text.Phrase(cellValue, passFont));
                            } else if ("Fail".equals(cellValue)) {
                                cell.setBackgroundColor(new com.itextpdf.text.BaseColor(248, 215, 218));
                                com.itextpdf.text.Font failFont = new com.itextpdf.text.Font(
                                    com.itextpdf.text.Font.FontFamily.HELVETICA, dataFontSize, com.itextpdf.text.Font.BOLD,
                                    new com.itextpdf.text.BaseColor(220, 53, 69));
                                cell.setPhrase(new com.itextpdf.text.Phrase(cellValue, failFont));
                            }
                        }
                        
                        // Subtle borders
                        cell.setBorderWidth(0.3f);
                        cell.setBorderColor(new com.itextpdf.text.BaseColor(222, 226, 230));
                        
                        pdfTable.addCell(cell);
                    }
                }
                
                document.add(pdfTable);
                
                // ============ FOOTER SECTION ============
                document.add(new Paragraph("\n"));
                com.itextpdf.text.Font footerFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 8, com.itextpdf.text.Font.ITALIC,
                    new com.itextpdf.text.BaseColor(108, 117, 125));
                Paragraph footer = new Paragraph(
                    "This is a computer-generated report | Total Records: " + resultTable.getRowCount(),
                    footerFont
                );
                footer.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                document.add(footer);
                
                document.close();
                
                showStyledMessage("Data exported successfully to:\n" + filePath, "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            showStyledMessage("Error exporting to PDF:\n" + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    private void printTable() {
        if (resultTable.getRowCount() == 0) {
            showStyledMessage("No data to print! Please click 'Show Data' first.", "No Data", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            boolean complete = resultTable.print(JTable.PrintMode.FIT_WIDTH, 
                new java.text.MessageFormat("Student Data Report"), 
                new java.text.MessageFormat("Page {0}"));
            
            if (complete) {
                showStyledMessage("Printing completed successfully!", "Print Successful", JOptionPane.INFORMATION_MESSAGE);
            } else {
                showStyledMessage("Printing cancelled by user.", "Print Cancelled", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (PrinterException e) {
            showStyledMessage("Error printing table:\n" + e.getMessage(), "Print Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void autoResizeTableColumns(JTable table) {
        TableColumnModel columnModel = table.getColumnModel();
        
        for (int column = 0; column < table.getColumnCount(); column++) {
            TableColumn tableColumn = columnModel.getColumn(column);
            int preferredWidth = 100; // Minimum width
            int maxWidth = 400; // Maximum width
            
            // Get header width
            TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
            Component headerComp = headerRenderer.getTableCellRendererComponent(
                table, tableColumn.getHeaderValue(), false, false, 0, column);
            preferredWidth = Math.max(preferredWidth, headerComp.getPreferredSize().width + 20);
            
            // Get max cell width from first 100 rows (for performance)
            int rowsToCheck = Math.min(100, table.getRowCount());
            for (int row = 0; row < rowsToCheck; row++) {
                TableCellRenderer cellRenderer = table.getCellRenderer(row, column);
                Component comp = table.prepareRenderer(cellRenderer, row, column);
                int width = comp.getPreferredSize().width + 20;
                preferredWidth = Math.max(preferredWidth, width);
                
                if (preferredWidth >= maxWidth) {
                    preferredWidth = maxWidth;
                    break;
                }
            }
            
            tableColumn.setPreferredWidth(preferredWidth);
        }
    }

    private void showStyledMessage(String message, String title, int messageType) {
        JOptionPane optionPane = new JOptionPane(message, messageType);
        JDialog dialog = optionPane.createDialog(this, title);
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    public static void openViewTool(JFrame parent, HashMap<String, List<Student>> sectionStudents) {
        ViewSelectionTool dialog = new ViewSelectionTool(parent, sectionStudents);
        dialog.setVisible(true);
    }
    
    // Inner class to hold extended student data
    private static class ExtendedStudentData {
        int studentId;
        String name;
        String rollNumber;
        String email;
        String phone;
        String section;
        int year;
        int semester;
        Map<String, Map<String, Integer>> subjectMarks;
        double totalMarks; // Changed to double for weighted sum
        int totalMaxMarks;
        double percentage;
        double sgpa;
        int totalCredits;
        double totalGradePoints;
        String grade;
        String status;
        int rank;
        int failedSubjectsCount;
        
        // New fields for weighted calculation system
        Map<String, Double> subjectWeightedTotals; // Subject -> weighted percentage
        Map<String, List<String>> subjectFailedComponents; // Subject -> list of failed exam types
        Map<String, Boolean> subjectPassStatus; // Subject -> pass/fail
        Map<String, List<String>> subjectExamTypes; // Subject -> list of exam type names
        Map<String, Map<String, Integer>> subjectMaxMarks; // Subject -> (ExamType -> max marks)
        String launchedResultsInfo; // Comma-separated list of launched result names
        String launchDate; // Date when result was launched
        
        ExtendedStudentData() {
            subjectMarks = new HashMap<>();
            subjectWeightedTotals = new HashMap<>();
            subjectFailedComponents = new HashMap<>();
            subjectPassStatus = new HashMap<>();
            subjectExamTypes = new HashMap<>();
            subjectMaxMarks = new HashMap<>();
            launchedResultsInfo = "";
        }
    }
    
    private void setupMultiRowHeader(Map<String, List<String>> subjectExamTypesMap, Map<String, Map<String, Integer>> maxMarksMap) {
        JTableHeader header = resultTable.getTableHeader();
        header.setPreferredSize(new Dimension(header.getWidth(), 60)); // Double height for 2 rows
        
        TableCellRenderer headerRenderer = new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                
                JPanel panel = new JPanel(new BorderLayout());
                panel.setBackground(new Color(248, 250, 252));
                panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(229, 231, 235)),
                    BorderFactory.createEmptyBorder(2, 5, 2, 5)
                ));
                
                String columnName = value.toString();
                
                // Check if this column is part of a subject group
                String subjectName = null;
                String examTypeName = null;
                boolean isTotal = false;
                
                if (columnName.contains(" - ")) {
                    String[] parts = columnName.split(" - ", 2);
                    subjectName = parts[0];
                    examTypeName = parts[1];
                    isTotal = examTypeName.equals("Total");
                }
                
                if (subjectName != null && examTypeName != null) {
                    // Two-row header: subject on top, exam type on bottom
                    JLabel topLabel = new JLabel(subjectName, SwingConstants.CENTER);
                    topLabel.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 11));
                    topLabel.setForeground(new Color(79, 70, 229));
                    
                    String bottomText = examTypeName;
                    
                    if (isTotal) {
                        // For total column, show max marks (100)
                        bottomText = "Total (100)";
                    } else {
                        // Show max marks for individual exam type
                        if (maxMarksMap.containsKey(subjectName) && 
                            maxMarksMap.get(subjectName).containsKey(examTypeName)) {
                            int maxMarks = maxMarksMap.get(subjectName).get(examTypeName);
                            bottomText = examTypeName + " (" + maxMarks + ")";
                        } else {
                            bottomText = examTypeName + " (N/A)";
                        }
                    }
                    
                    JLabel bottomLabel = new JLabel(bottomText, SwingConstants.CENTER);
                    bottomLabel.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));
                    bottomLabel.setForeground(isTotal ? new Color(34, 197, 94) : new Color(17, 24, 39));
                    
                    panel.add(topLabel, BorderLayout.NORTH);
                    panel.add(bottomLabel, BorderLayout.CENTER);
                } else {
                    // Single-row header for non-subject columns
                    JLabel label = new JLabel(columnName, SwingConstants.CENTER);
                    label.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
                    label.setForeground(new Color(17, 24, 39));
                    panel.add(label, BorderLayout.CENTER);
                }
                
                return panel;
            }
        };
        
        // Apply renderer to all columns
        for (int i = 0; i < resultTable.getColumnCount(); i++) {
            resultTable.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }
    }
    
    private void closePanel() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }
    
    // Inner class to hold section information
    private static class SectionInfo {
        int id;
        String sectionName;
        
        SectionInfo(SectionDAO.SectionInfo info) {
            this.id = info.id;
            this.sectionName = info.sectionName;
        }
    }
}