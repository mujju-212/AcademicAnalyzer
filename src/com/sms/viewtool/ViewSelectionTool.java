package com.sms.viewtool;

import com.sms.analyzer.Student;
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

public class ViewSelectionTool extends JDialog {

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
            setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
            setForeground(new Color(50, 50, 50));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
    }

    private HashMap<String, List<Student>> sectionStudents;
    private JComboBox<String> sectionDropdown;
    private JPanel subjectCheckboxPanel;
    private JCheckBox nameCheckBox, rollNumberCheckBox, emailCheckBox, phoneCheckBox, 
                      sectionCheckBox, totalMarksCheckBox, percentageCheckBox, 
                      sgpaCheckBox, gradeCheckBox, statusCheckBox, rankCheckBox,
                      failedSubjectsCheckBox;
    private Map<String, JCheckBox> subjectCheckBoxes;
    private RoundedButton showButton, exportExcelButton, exportPdfButton, printButton;
    private JTable resultTable;
    private JScrollPane tableScrollPane;
    private Map<Integer, SectionInfo> sectionInfoMap;

    public ViewSelectionTool(JFrame parent, HashMap<String, List<Student>> sectionStudents) {
        super(parent, "View Student Data", true);
        this.sectionStudents = (sectionStudents != null) ? sectionStudents : new HashMap<>();
        this.subjectCheckBoxes = new HashMap<>();
        this.sectionInfoMap = new HashMap<>();
        
        // Get screen size and adjust dialog size accordingly
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int dialogHeight = Math.min(850, (int)(screenSize.height * 0.85)); // 85% of screen height max
        int dialogWidth = Math.min(850, (int)(screenSize.width * 0.7)); // 70% of screen width max
        
        setSize(dialogWidth, dialogHeight);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        setUndecorated(true);
        
        // Make it resizable by removing this line or setting to false
        // setUndecorated(false); // This will show title bar with min/max buttons
        
        initializeUI();
    }
    

    private void initializeUI() {
        // Main container with gradient background
        JPanel mainContainer = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(200, 190, 230),
                    getWidth(), getHeight(), new Color(150, 180, 220)
                );
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        
        // White card panel - reduce size
        RoundedPanel cardPanel = new RoundedPanel(30);
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setLayout(new BorderLayout());
        cardPanel.setPreferredSize(new Dimension(800, 650)); // Reduced from 850
        
        // Inner content panel with proper layout
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new GridBagLayout());
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30)); // Reduced padding
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 15, 0); // Reduced from 20
        
        // Title panel with close button
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("View Student Data");
        titleLabel.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 24)); // Reduced from 28
        titleLabel.setForeground(new Color(30, 30, 30));
        
        JButton closeButton = new JButton("✕");
        closeButton.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 20));
        closeButton.setForeground(new Color(100, 100, 100));
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setFocusPainted(false);
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> dispose());
        
        titlePanel.add(titleLabel, BorderLayout.WEST);
        titlePanel.add(closeButton, BorderLayout.EAST);
        contentPanel.add(titlePanel, gbc);
        
        // Section selection panel
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 8, 0); // Reduced
        JLabel sectionLabel = new JLabel("Select Section");
        sectionLabel.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14)); // Reduced from 16
        sectionLabel.setForeground(new Color(50, 50, 50));
        contentPanel.add(sectionLabel, gbc);
        
        // Section dropdown
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 15, 0); // Reduced
        Vector<String> sections = new Vector<>();
        sections.add("All Sections");
        
        // Load sections from database
        loadSectionInfo();
        sections.addAll(sectionStudents.keySet());
        
        sectionDropdown = new JComboBox<>(sections);
        sectionDropdown.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 13)); // Reduced
        sectionDropdown.setPreferredSize(new Dimension(0, 40)); // Reduced from 45
        sectionDropdown.setBackground(Color.WHITE);
        sectionDropdown.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10) // Reduced padding
        ));
        contentPanel.add(sectionDropdown, gbc);
        
        // Fields selection label
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 8, 0);
        JLabel fieldsLabel = new JLabel("Select Fields");
        fieldsLabel.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14)); // Reduced
        fieldsLabel.setForeground(new Color(50, 50, 50));
        contentPanel.add(fieldsLabel, gbc);
        
        // Create a scrollable panel for all checkboxes
        JPanel allFieldsPanel = new JPanel();
        allFieldsPanel.setLayout(new BoxLayout(allFieldsPanel, BoxLayout.Y_AXIS));
        allFieldsPanel.setOpaque(false);
        
        // Student Info checkboxes panel
        JPanel studentInfoPanel = new JPanel(new GridLayout(2, 3, 8, 8)); // Reduced spacing
        studentInfoPanel.setOpaque(false);
        studentInfoPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)), 
            "Student Information",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new java.awt.Font("Arial", java.awt.Font.BOLD, 11), // Reduced
            new Color(100, 100, 100)
        ));
        
        nameCheckBox = new StyledCheckBox("Name", true);
        rollNumberCheckBox = new StyledCheckBox("Roll Number", true);
        emailCheckBox = new StyledCheckBox("Email", false);
        phoneCheckBox = new StyledCheckBox("Phone", false);
        sectionCheckBox = new StyledCheckBox("Section", true);
        
        studentInfoPanel.add(nameCheckBox);
        studentInfoPanel.add(rollNumberCheckBox);
        studentInfoPanel.add(emailCheckBox);
        studentInfoPanel.add(phoneCheckBox);
        studentInfoPanel.add(sectionCheckBox);
        
        allFieldsPanel.add(studentInfoPanel);
        allFieldsPanel.add(Box.createVerticalStrut(10));
        
        // Academic fields checkboxes panel
        JPanel academicPanel = new JPanel(new GridLayout(2, 4, 8, 8)); // Reduced spacing
        academicPanel.setOpaque(false);
        academicPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)), 
            "Academic Information",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new java.awt.Font("Arial", java.awt.Font.BOLD, 11), // Reduced
            new Color(100, 100, 100)
        ));
        
        totalMarksCheckBox = new StyledCheckBox("Total Marks", true);
        percentageCheckBox = new StyledCheckBox("Percentage", true);
        sgpaCheckBox = new StyledCheckBox("SGPA", true);
        gradeCheckBox = new StyledCheckBox("Grade", true);
        statusCheckBox = new StyledCheckBox("Pass/Fail", true);
        rankCheckBox = new StyledCheckBox("Rank", false);
        failedSubjectsCheckBox = new StyledCheckBox("Failed Subjects", false);
        
        academicPanel.add(totalMarksCheckBox);
        academicPanel.add(percentageCheckBox);
        academicPanel.add(sgpaCheckBox);
        academicPanel.add(gradeCheckBox);
        academicPanel.add(statusCheckBox);
        academicPanel.add(rankCheckBox);
        academicPanel.add(failedSubjectsCheckBox);
        
        allFieldsPanel.add(academicPanel);
        allFieldsPanel.add(Box.createVerticalStrut(10));
        
        // Subject checkboxes panel
        subjectCheckboxPanel = new JPanel(new GridBagLayout());
        subjectCheckboxPanel.setOpaque(false);
        subjectCheckboxPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)), 
            "Subject Marks",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new java.awt.Font("Arial", java.awt.Font.BOLD, 11), // Reduced
            new Color(100, 100, 100)
        ));
        allFieldsPanel.add(subjectCheckboxPanel);
        
        // Add scrollable fields panel
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 15, 0);
        gbc.weighty = 0.3; // Give some weight for scrolling
        gbc.fill = GridBagConstraints.BOTH;
        
        JScrollPane fieldsScrollPane = new JScrollPane(allFieldsPanel);
        fieldsScrollPane.setBorder(null);
        fieldsScrollPane.setPreferredSize(new Dimension(0, 180)); // Fixed height
        fieldsScrollPane.getViewport().setBackground(Color.WHITE);
        contentPanel.add(fieldsScrollPane, gbc);
        
        // Show button - centered
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 15, 0);
        showButton = new RoundedButton("Show Data", 20, // Reduced radius
            new Color(66, 133, 244), new Color(50, 110, 220));
        showButton.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14)); // Reduced
        showButton.setForeground(Color.WHITE);
        showButton.setPreferredSize(new Dimension(130, 40)); // Reduced
        contentPanel.add(showButton, gbc);
        
        // Table panel
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 15, 0);
        
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setOpaque(false);
        tablePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(1, 1, 1, 1)
        ));
        
        // Create table with custom styling
        resultTable = new JTable();
        resultTable.setRowHeight(30); // Reduced from 35
        resultTable.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12)); // Reduced
        resultTable.setGridColor(new Color(230, 230, 230));
        resultTable.setShowGrid(true);
        resultTable.setIntercellSpacing(new Dimension(1, 1));
        
        // Style table header
        JTableHeader header = resultTable.getTableHeader();
        header.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12)); // Reduced
        header.setBackground(new Color(245, 245, 245));
        header.setForeground(new Color(50, 50, 50));
        header.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        tableScrollPane = new JScrollPane(resultTable);
        tableScrollPane.setBorder(null);
        tablePanel.add(tableScrollPane, BorderLayout.CENTER);
        contentPanel.add(tablePanel, gbc);
        
        // Export buttons panel - centered
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 0, 0);
        
        JPanel exportPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        exportPanel.setOpaque(false);
        
        exportExcelButton = new RoundedButton("Export Excel", 20,
            new Color(52, 168, 83), new Color(40, 140, 70));
        exportExcelButton.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12)); // Reduced
        exportExcelButton.setForeground(Color.WHITE);
        exportExcelButton.setPreferredSize(new Dimension(110, 35)); // Reduced
        
        exportPdfButton = new RoundedButton("Export PDF", 20,
            new Color(220, 53, 69), new Color(200, 35, 51));
        exportPdfButton.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12)); // Reduced
        exportPdfButton.setForeground(Color.WHITE);
        exportPdfButton.setPreferredSize(new Dimension(110, 35)); // Reduced
        
        printButton = new RoundedButton("Print", 20,
            new Color(255, 193, 7), new Color(235, 173, 0));
        printButton.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12)); // Reduced
        printButton.setForeground(Color.WHITE);
        printButton.setPreferredSize(new Dimension(80, 35)); // Reduced
        
        exportPanel.add(exportExcelButton);
        exportPanel.add(exportPdfButton);
        exportPanel.add(printButton);
        
        contentPanel.add(exportPanel, gbc);
        
        // Make the content scrollable
        JScrollPane contentScrollPane = new JScrollPane(contentPanel);
        contentScrollPane.setBorder(null);
        contentScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        cardPanel.add(contentScrollPane, BorderLayout.CENTER);
        
        // Add card panel to main container
        GridBagConstraints mainGbc = new GridBagConstraints();
        mainGbc.gridx = 0;
        mainGbc.gridy = 0;
        mainContainer.add(cardPanel, mainGbc);
        
        add(mainContainer, BorderLayout.CENTER);
        
        // Add listeners
        sectionDropdown.addActionListener(e -> updateSubjectCheckboxes());
        showButton.addActionListener(e -> displaySelectedData());
        exportExcelButton.addActionListener(e -> exportToExcel());
        exportPdfButton.addActionListener(e -> exportToPdf());
        printButton.addActionListener(e -> printTable());
        
        // Initialize subject checkboxes
        updateSubjectCheckboxes();
        
        // Make dialog draggable
        addDragFunctionality(cardPanel);
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
        subjectCheckBoxes.clear();
        
        String selectedSection = (String) sectionDropdown.getSelectedItem();
        System.out.println("Selected section: " + selectedSection); // Debug
        
        if (selectedSection != null) {
            Set<String> allSubjects = new HashSet<>();
            
            if (selectedSection.equals("All Sections")) {
                // Get all unique subjects from all sections
                for (Map.Entry<String, List<Student>> entry : sectionStudents.entrySet()) {
                    System.out.println("Section: " + entry.getKey() + ", Students: " + entry.getValue().size()); // Debug
                    List<Student> students = entry.getValue();
                    if (!students.isEmpty()) {
                        Student firstStudent = students.get(0);
                        System.out.println("First student marks: " + firstStudent.getMarks()); // Debug
                        allSubjects.addAll(firstStudent.getMarks().keySet());
                    }
                }
            } else {
                // Get subjects for the selected section
                List<Student> students = sectionStudents.get(selectedSection);
                System.out.println("Students in section: " + (students != null ? students.size() : "null")); // Debug
                if (students != null && !students.isEmpty()) {
                    Student firstStudent = students.get(0);
                    System.out.println("First student marks: " + firstStudent.getMarks()); // Debug
                    allSubjects.addAll(firstStudent.getMarks().keySet());
                }
            }
            
            System.out.println("All subjects found: " + allSubjects); // Debug
            
            if (!allSubjects.isEmpty()) {
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.insets = new Insets(5, 5, 5, 10);
                
                for (String subject : allSubjects) {
                    StyledCheckBox subjectCheckBox = new StyledCheckBox(subject, true);
                    subjectCheckBoxes.put(subject, subjectCheckBox);
                    subjectCheckboxPanel.add(subjectCheckBox, gbc);
                    gbc.gridx++;
                    if (gbc.gridx > 3) {
                        gbc.gridx = 0;
                        gbc.gridy++;
                    }
                }
            } else {
                // Add a label when no subjects are found
                JLabel noSubjectsLabel = new JLabel("No subjects found for this section");
                noSubjectsLabel.setFont(new java.awt.Font("Arial", java.awt.Font.ITALIC, 12));
                noSubjectsLabel.setForeground(Color.GRAY);
                subjectCheckboxPanel.add(noSubjectsLabel);
            }
        }
        
        subjectCheckboxPanel.revalidate();
        subjectCheckboxPanel.repaint();
    }
    

    private void displaySelectedData() {
        String selectedSection = (String) sectionDropdown.getSelectedItem();
        System.out.println("DEBUG: Selected section: " + selectedSection);
        
        if (selectedSection == null) {
            showStyledMessage("Please select a section!", "No Section Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Create column headers based on selected checkboxes
        ArrayList<String> columnNames = new ArrayList<>();
        if (nameCheckBox.isSelected()) columnNames.add("Name");
        if (rollNumberCheckBox.isSelected()) columnNames.add("Roll Number");
        if (emailCheckBox.isSelected()) columnNames.add("Email");
        if (phoneCheckBox.isSelected()) columnNames.add("Phone");
        if (sectionCheckBox.isSelected()) columnNames.add("Section");
        
        // Add selected subject columns
        List<String> selectedSubjects = new ArrayList<>();
        System.out.println("DEBUG: Subject checkboxes count: " + subjectCheckBoxes.size());
        for (Map.Entry<String, JCheckBox> entry : subjectCheckBoxes.entrySet()) {
            System.out.println("DEBUG: Subject " + entry.getKey() + " is " + (entry.getValue().isSelected() ? "selected" : "not selected"));
            if (entry.getValue().isSelected()) {
                columnNames.add(entry.getKey());
                selectedSubjects.add(entry.getKey());
            }
        }
        
        if (totalMarksCheckBox.isSelected()) columnNames.add("Total Marks");
        if (percentageCheckBox.isSelected()) columnNames.add("Percentage");
        if (sgpaCheckBox.isSelected()) columnNames.add("SGPA");
        if (gradeCheckBox.isSelected()) columnNames.add("Grade");
        if (statusCheckBox.isSelected()) columnNames.add("Status");
        if (rankCheckBox.isSelected()) columnNames.add("Rank");
        if (failedSubjectsCheckBox.isSelected()) columnNames.add("Failed Subjects");
        
        System.out.println("DEBUG: Column names: " + columnNames);
        
        if (columnNames.isEmpty()) {
            showStyledMessage("Please select at least one field to display!", "No Fields Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        DefaultTableModel model = new DefaultTableModel(columnNames.toArray(new String[0]), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        
        // Collect all students for ranking
        List<ExtendedStudentData> allStudentData = new ArrayList<>();
        
        // Add data rows
        if (selectedSection.equals("All Sections")) {
            System.out.println("DEBUG: Processing all sections");
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
            System.out.println("DEBUG: Processing single section: " + selectedSection);
            
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
        
        System.out.println("DEBUG: Total students to display: " + allStudentData.size());
        
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
            if (nameCheckBox.isSelected()) rowData.add(data.name);
            if (rollNumberCheckBox.isSelected()) rowData.add(data.rollNumber);
            if (emailCheckBox.isSelected()) rowData.add(data.email != null ? data.email : "");
            if (phoneCheckBox.isSelected()) rowData.add(data.phone != null ? data.phone : "");
            if (sectionCheckBox.isSelected()) rowData.add(data.section);
            
            // Add subject marks
            for (String subject : selectedSubjects) {
                Integer mark = data.subjectMarks.get(subject);
                rowData.add(mark != null ? mark : "-");
            }
            
            if (totalMarksCheckBox.isSelected()) rowData.add(data.totalMarks);
            if (percentageCheckBox.isSelected()) rowData.add(String.format("%.2f%%", data.percentage));
            if (sgpaCheckBox.isSelected()) rowData.add(String.format("%.2f", data.sgpa));
            if (gradeCheckBox.isSelected()) rowData.add(data.grade);
            if (statusCheckBox.isSelected()) rowData.add(data.status);
            if (rankCheckBox.isSelected()) rowData.add(data.rank);
            if (failedSubjectsCheckBox.isSelected()) rowData.add(data.failedSubjectsCount);
            
            model.addRow(rowData.toArray());
        }
        
        System.out.println("DEBUG: Added " + model.getRowCount() + " rows to table");
        
        resultTable.setModel(model);
        
        // Apply alternating row colors and conditional formatting
        resultTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
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
            System.out.println("DEBUG: Student is null");
            return null;
        }
        
        ExtendedStudentData data = new ExtendedStudentData();
        data.name = student.getName();
        data.rollNumber = student.getRollNumber();
        data.section = section;
        data.subjectMarks = student.getMarks() != null ? student.getMarks() : new HashMap<>();
        
        System.out.println("DEBUG: Processing student: " + data.name + " (" + data.rollNumber + ")");
        System.out.println("DEBUG: Student marks: " + data.subjectMarks);
        
        // Get section ID for this section
        int sectionId = getSectionIdByName(section);
        System.out.println("DEBUG: Section ID for " + section + ": " + sectionId);
        
        // Get additional info from database
        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT email, phone FROM students WHERE roll_number = ? AND created_by = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, student.getRollNumber());
            ps.setInt(2, com.sms.login.LoginScreen.currentUserId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                data.email = rs.getString("email");
                data.phone = rs.getString("phone");
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
            for (Integer mark : data.subjectMarks.values()) {
                if (mark != null) {
                    data.totalMarks += mark;
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
            
            // Get subject details with max marks, passing marks, and credits
            String query = """
                SELECT sub.subject_name, ss.max_marks, ss.passing_marks, ss.credit
                FROM section_subjects ss
                JOIN subjects sub ON ss.subject_id = sub.id
                WHERE ss.section_id = ?
            """;
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, sectionId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                String subjectName = rs.getString("subject_name");
                int maxMarks = rs.getInt("max_marks");
                int passingMarks = rs.getInt("passing_marks");
                int credit = rs.getInt("credit");
                
                Integer marks = data.subjectMarks.get(subjectName);
                if (marks != null) {
                    data.totalMarks += marks;
                    data.totalMaxMarks += maxMarks;
                    data.totalCredits += credit;
                    
                    // Calculate grade points for SGPA
                    double gradePoint = calculateGradePoint(marks, maxMarks);
                    data.totalGradePoints += (gradePoint * credit);
                    
                    // Check if failed
                    if (marks < passingMarks) {
                        data.failedSubjectsCount++;
                    }
                }
            }
            
            rs.close();
            ps.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // Calculate percentage
        if (data.totalMaxMarks > 0) {
            data.percentage = (data.totalMarks * 100.0) / data.totalMaxMarks;
        } else {
            data.percentage = 0.0;
        }
        
        // Calculate SGPA
        if (data.totalCredits > 0) {
            data.sgpa = data.totalGradePoints / data.totalCredits;
        } else {
            data.sgpa = 0.0;
        }
        
        // Calculate grade
        data.grade = calculateGrade(data.percentage);
        
        // Determine status
        data.status = (data.failedSubjectsCount == 0) ? "Pass" : "Fail";
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
        students.sort((s1, s2) -> Integer.compare(s2.totalMarks, s1.totalMarks));
        
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
                
                // Create header row with styling
                Row headerRow = sheet.createRow(0);
                CellStyle headerStyle = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);
                headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                headerStyle.setBorderBottom(BorderStyle.THIN);
                headerStyle.setBorderTop(BorderStyle.THIN);
                headerStyle.setBorderLeft(BorderStyle.THIN);
                headerStyle.setBorderRight(BorderStyle.THIN);
                
                // Add headers
                for (int i = 0; i < resultTable.getColumnCount(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(resultTable.getColumnName(i));
                    cell.setCellStyle(headerStyle);
                }
                
                // Create cell styles for data
                CellStyle dataStyle = workbook.createCellStyle();
                dataStyle.setBorderBottom(BorderStyle.THIN);
                dataStyle.setBorderTop(BorderStyle.THIN);
                dataStyle.setBorderLeft(BorderStyle.THIN);
                dataStyle.setBorderRight(BorderStyle.THIN);
                
                CellStyle passStyle = workbook.createCellStyle();
                passStyle.cloneStyleFrom(dataStyle);
                org.apache.poi.ss.usermodel.Font passFont = workbook.createFont();
                passFont.setColor(IndexedColors.GREEN.getIndex());
                passStyle.setFont(passFont);
                
                CellStyle failStyle = workbook.createCellStyle();
                failStyle.cloneStyleFrom(dataStyle);
                org.apache.poi.ss.usermodel.Font failFont = workbook.createFont();
                failFont.setColor(IndexedColors.RED.getIndex());
                failStyle.setFont(failFont);
                
                // Create data rows
                for (int i = 0; i < resultTable.getRowCount(); i++) {
                    Row row = sheet.createRow(i + 1);
                    for (int j = 0; j < resultTable.getColumnCount(); j++) {
                        Cell cell = row.createCell(j);
                        Object value = resultTable.getValueAt(i, j);
                        
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
                                    percentStyle.cloneStyleFrom(dataStyle);
                                    percentStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));
                                    cell.setCellStyle(percentStyle);
                                } catch (NumberFormatException e) {
                                    cell.setCellValue(strValue);
                                    cell.setCellStyle(dataStyle);
                                }
                            } else {
                                cell.setCellValue(strValue);
                                cell.setCellStyle(dataStyle);
                            }
                        } else {
                            cell.setCellValue(value.toString());
                            cell.setCellStyle(dataStyle);
                        }
                        
                        // Apply conditional formatting for status
                        if (resultTable.getColumnName(j).equals("Status")) {
                            String cellValue = value != null ? value.toString() : "";
                            if ("Pass".equals(cellValue)) {
                                cell.setCellStyle(passStyle);
                            } else if ("Fail".equals(cellValue)) {
                                cell.setCellStyle(failStyle);
                            }
                        }
                    }
                }
                
                // Auto-size columns
                for (int i = 0; i < resultTable.getColumnCount(); i++) {
                    sheet.autoSizeColumn(i);
                    // Add some extra space
                    int currentWidth = sheet.getColumnWidth(i);
                    sheet.setColumnWidth(i, currentWidth + 500);
                }
                
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
                
                Document document = new Document(PageSize.A4.rotate());
                PdfWriter.getInstance(document, new FileOutputStream(filePath));
                document.open();
                
                // Add title
                com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD);
                Paragraph title = new Paragraph("Student Data Report", titleFont);
                title.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                document.add(title);
                document.add(new Paragraph("\n"));
                
                // Create table
                PdfPTable pdfTable = new PdfPTable(resultTable.getColumnCount());
                pdfTable.setWidthPercentage(100);
                
                // Add headers
                com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD);
                for (int i = 0; i < resultTable.getColumnCount(); i++) {
                    com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(
                        new com.itextpdf.text.Phrase(resultTable.getColumnName(i), headerFont));
                    cell.setBackgroundColor(com.itextpdf.text.BaseColor.LIGHT_GRAY);
                    cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                    pdfTable.addCell(cell);
                }
                
                // Add data
                com.itextpdf.text.Font dataFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 9);
                for (int i = 0; i < resultTable.getRowCount(); i++) {
                    for (int j = 0; j < resultTable.getColumnCount(); j++) {
                        Object value = resultTable.getValueAt(i, j);
                        String cellValue = value != null ? value.toString() : "";
                        
                        com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(
                            new com.itextpdf.text.Phrase(cellValue, dataFont));
                        
                        // Color coding for status
                        if (resultTable.getColumnName(j).equals("Status")) {
                            if ("Pass".equals(cellValue)) {
                                cell.setBackgroundColor(new com.itextpdf.text.BaseColor(220, 255, 220));
                            } else if ("Fail".equals(cellValue)) {
                                cell.setBackgroundColor(new com.itextpdf.text.BaseColor(255, 220, 220));
                            }
                        }
                        
                        pdfTable.addCell(cell);
                    }
                }
                
                document.add(pdfTable);
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
        String name;
        String rollNumber;
        String email;
        String phone;
        String section;
        Map<String, Integer> subjectMarks;
        int totalMarks;
        int totalMaxMarks;
        double percentage;
        double sgpa;
        int totalCredits;
        double totalGradePoints;
        String grade;
        String status;
        int rank;
        int failedSubjectsCount;
        
        ExtendedStudentData() {
            subjectMarks = new HashMap<>();
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