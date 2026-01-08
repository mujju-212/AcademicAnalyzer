package com.sms.analyzer;

import com.sms.analyzer.SectionAnalyzer;
import com.sms.dao.AnalyzerDAO;
import com.sms.dao.SectionDAO;
import com.sms.database.DatabaseConnection;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import org.jfree.chart.*;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.ui.TextAnchor;
import com.formdev.flatlaf.FlatLightLaf;

public class StudentAnalyzer extends JPanel {

    private JFrame parentFrame;
    private Runnable onCloseCallback;
    
    // Modern color scheme
    private static final Color BACKGROUND_COLOR = new Color(245, 247, 250);
    private static final Color CARD_COLOR = Color.WHITE;
    private static final Color PRIMARY_COLOR = new Color(99, 102, 241);
    private static final Color PRIMARY_DARK = new Color(79, 70, 229);
    private static final Color TEXT_PRIMARY = new Color(17, 24, 39);
    private static final Color TEXT_SECONDARY = new Color(107, 114, 128);
    private static final Color BORDER_COLOR = new Color(229, 231, 235);
    private static final Color SUCCESS_COLOR = new Color(34, 197, 94);
    private static final Color DANGER_COLOR = new Color(239, 68, 68);
    private static final Color WARNING_COLOR = new Color(251, 146, 60);
    private static final Color INFO_COLOR = new Color(59, 130, 246);
    
    private JComboBox<String> sectionDropdown;
    private JTextField studentNameField;
    private JTextField rollNumberField;
    private JPanel inputCard; // Reference to hide after analysis
    private JPanel filterCard; // Tree selector card
    private JTree filterTree; // Tree with checkboxes
    private JPanel resultsPanel;
    private JPanel analysisPanel;
    private JPanel chartPanel;
    private JRadioButton studentRadio;
    private JRadioButton sectionRadio;
    private JPanel subjectPerformancePanel;
    private HashMap<String, List<Student>> sectionStudents;
    private Student currentStudent;
    private int currentUserId;
    private Map<String, Set<String>> selectedFilters; // Track selected subject-exam combinations

    public StudentAnalyzer(JFrame parent, HashMap<String, List<Student>> sectionStudents) {
        this(parent, sectionStudents, null);
    }
    
    public StudentAnalyzer(JFrame parent, HashMap<String, List<Student>> sectionStudents, Runnable onCloseCallback) {
        this.parentFrame = parent;
        this.onCloseCallback = onCloseCallback;
        this.sectionStudents = sectionStudents != null ? sectionStudents : new HashMap<>();
        this.currentUserId = com.sms.login.LoginScreen.currentUserId;

        // Set light theme for modern look
        FlatLightLaf.setup();
        
        // Configure UI settings for a clean modern look
        UIManager.put("Button.arc", 15);
        UIManager.put("Component.arc", 15);
        UIManager.put("TextField.arc", 15);
        UIManager.put("Panel.arc", 15);
        
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);
        
        initializeUI();
    }

    public static void openAnalyzer(JFrame parent, HashMap<String, List<Student>> sectionStudents) {
        SwingUtilities.invokeLater(() -> new StudentAnalyzer(parent, sectionStudents));
    }
    
    private String selectedSection;
 // Update the initializeUI method - modify the input card section
    private void initializeUI() {
        setBackground(BACKGROUND_COLOR);
        
        // Main panel with padding
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(0, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        mainPanel.setBackground(BACKGROUND_COLOR);
        
        // Header Card
        JPanel headerCard = createModernCard();
        headerCard.setLayout(new BorderLayout(20, 10));
        headerCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        
        // Add back button if callback present
        if (onCloseCallback != null) {
            JButton backButton = new JButton("← Back");
            backButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            backButton.setForeground(PRIMARY_COLOR);
            backButton.setBackground(CARD_COLOR);
            backButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
            backButton.setFocusPainted(false);
            backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            backButton.addActionListener(e -> closePanel());
            
            JPanel backPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            backPanel.setOpaque(false);
            backPanel.add(backButton);
            headerCard.add(backPanel, BorderLayout.NORTH);
        }
        
        // Title
        JLabel titleLabel = new JLabel("STUDENT PERFORMANCE ANALYZER");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        titleLabel.setForeground(PRIMARY_COLOR);
        
        // Radio button panel
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        radioPanel.setOpaque(false);
        
        ButtonGroup group = new ButtonGroup();
        
        studentRadio = createModernRadioButton("Student", true);
        sectionRadio = createModernRadioButton("Section", false);
        
        group.add(studentRadio);
        group.add(sectionRadio);
        
        radioPanel.add(studentRadio);
        radioPanel.add(sectionRadio);
        
        sectionRadio.addActionListener(e -> {
            selectedSection = (String) JOptionPane.showInputDialog(
                this, "Select Section:", "Section Selection",
                JOptionPane.PLAIN_MESSAGE, null,
                sectionStudents.keySet().toArray(), null
            );

            if (selectedSection != null) {
                HashMap<String, ArrayList<Student>> sectionMap = new HashMap<>();
                sectionMap.put(selectedSection, new ArrayList<>(sectionStudents.get(selectedSection)));
                showSectionAnalyzer(sectionMap);
            } else {
                studentRadio.setSelected(true);
            }
        });
        
        headerCard.add(titleLabel, BorderLayout.NORTH);
        headerCard.add(radioPanel, BorderLayout.CENTER);
        
        mainPanel.add(headerCard, BorderLayout.NORTH);
        
        // Main content area with custom layout
        JPanel contentArea = new JPanel(new BorderLayout(20, 0));
        contentArea.setOpaque(false);
        
        // Left side panel (Input + Results)
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        leftPanel.setPreferredSize(new Dimension(550, 0));
        
        // Input Card - INCREASED SIZE for section dropdown
        inputCard = createModernCard();
        inputCard.setLayout(new BoxLayout(inputCard, BoxLayout.Y_AXIS));
        inputCard.setMaximumSize(new Dimension(550, 280));
        inputCard.setPreferredSize(new Dimension(550, 280));
        inputCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Section Selection
        JLabel sectionLabel = new JLabel("Select Section");
        sectionLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        sectionLabel.setForeground(TEXT_PRIMARY);
        sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        sectionDropdown = new JComboBox<>();
        sectionDropdown.setFont(new Font("SansSerif", Font.PLAIN, 14));
        sectionDropdown.setBackground(Color.WHITE);
        sectionDropdown.setAlignmentX(Component.LEFT_ALIGNMENT);
        sectionDropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        // Populate section dropdown
        sectionDropdown.addItem("-- Select Section --");
        for (String section : sectionStudents.keySet()) {
            sectionDropdown.addItem(section);
        }
        
        // Roll Number Field (primary search field)
        JLabel rollLabel = new JLabel("Roll Number");
        rollLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        rollLabel.setForeground(TEXT_PRIMARY);
        rollLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        rollNumberField = createModernTextField();
        rollNumberField.setAlignmentX(Component.LEFT_ALIGNMENT);
        rollNumberField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); // Smaller height
        
        // Analyze Button
        JButton analyzeButton = createModernButton("Analyze");
        analyzeButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        analyzeButton.addActionListener(e -> analyzeStudent());
        
        inputCard.add(sectionLabel);
        inputCard.add(Box.createVerticalStrut(5));
        inputCard.add(sectionDropdown);
        inputCard.add(Box.createVerticalStrut(15));
        inputCard.add(rollLabel);
        inputCard.add(Box.createVerticalStrut(5));
        inputCard.add(rollNumberField);
        inputCard.add(Box.createVerticalStrut(20));
        inputCard.add(analyzeButton);
        
        // Filter Card (Tree selector - initially hidden)
        filterCard = createModernCard();
        filterCard.setLayout(new BorderLayout(10, 10));
        filterCard.setMaximumSize(new Dimension(550, 450));
        filterCard.setPreferredSize(new Dimension(550, 450));
        filterCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterCard.setVisible(false);
        
        // Results Card
        resultsPanel = createModernCard();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setVisible(false);
        resultsPanel.setMaximumSize(new Dimension(550, 1100));
        resultsPanel.setPreferredSize(new Dimension(550, 1100));
        resultsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        leftPanel.add(inputCard);
        leftPanel.add(Box.createVerticalStrut(20));
        leftPanel.add(filterCard);
        leftPanel.add(Box.createVerticalStrut(20));
        leftPanel.add(resultsPanel);
        leftPanel.add(Box.createVerticalGlue());
        
        // Right side panel with Tabbed Interface
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);
        
        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 14));
        tabbedPane.setVisible(false);
        
        // Tab 1: Analysis Summary
        analysisPanel = createModernCard();
        analysisPanel.setLayout(new BorderLayout());
        JScrollPane analysisScrollPane = new JScrollPane(analysisPanel);
        analysisScrollPane.setBorder(null);
        analysisScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        analysisScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        tabbedPane.addTab("📊 Analysis Summary", analysisScrollPane);
        
        // Tab 2: Subject Performance Chart
        subjectPerformancePanel = createModernCard();
        subjectPerformancePanel.setLayout(new BorderLayout(0, 15));
        JScrollPane chartScrollPane = new JScrollPane(subjectPerformancePanel);
        chartScrollPane.setBorder(null);
        chartScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        chartScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        chartScrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        tabbedPane.addTab("📈 Performance Chart", chartScrollPane);
        
        rightPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // Store reference to tabbedPane for visibility control
        this.putClientProperty("tabbedPane", tabbedPane);
        
        // Add panels to content area
        contentArea.add(leftPanel, BorderLayout.WEST);
        contentArea.add(rightPanel, BorderLayout.CENTER);
        
        mainPanel.add(contentArea, BorderLayout.CENTER);
        add(mainPanel);
    }

    // Update the updateAnalysisPanel method for tabbed layout
    private void updateAnalysisPanel(int totalMarks) {
        analysisPanel.removeAll();
        analysisPanel.setLayout(new BorderLayout());
        
        // Main panel for analysis with better spacing
        JPanel mainAnalysisPanel = new JPanel();
        mainAnalysisPanel.setLayout(new BoxLayout(mainAnalysisPanel, BoxLayout.Y_AXIS));
        mainAnalysisPanel.setOpaque(false);
        mainAnalysisPanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        
        // Analysis header
        JLabel analysisHeader = new JLabel("Performance Metrics");
        analysisHeader.setFont(new Font("SansSerif", Font.BOLD, 24));
        analysisHeader.setForeground(TEXT_PRIMARY);
        analysisHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        mainAnalysisPanel.add(analysisHeader);
        mainAnalysisPanel.add(Box.createVerticalStrut(30));
        
        // Create grid for metric cards with proper spacing
        JPanel metricsGrid = new JPanel(new GridLayout(2, 4, 10, 10));
        metricsGrid.setOpaque(false);
        metricsGrid.setMaximumSize(new Dimension(950, 200));
        metricsGrid.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Calculate proper metrics using subject-wise analysis
        AnalyzerDAO dao = new AnalyzerDAO();
        double totalWeightedPoints = 0;
        int totalCredits = 0;
        int totalMaxMarks = 0;
        int totalObtainedMarks = 0;
        int subjectCount = selectedFilters.size();
        
        System.out.println("\n=== Credit-Based SGPA Calculation ===");
        
        // Calculate per subject
        for (Map.Entry<String, Set<String>> entry : selectedFilters.entrySet()) {
            String subject = entry.getKey();
            
            // Get subject info (max_marks and credit)
            AnalyzerDAO.SubjectConfig info = dao.getSubjectInfo(currentStudent.getId(), subject);
            
            // Calculate marks obtained for this subject (sum of all selected exam types)
            int subjectMarks = 0;
            Map<String, Integer> examMarks = currentStudent.getMarks().get(subject);
            if (examMarks != null) {
                for (String examType : entry.getValue()) {
                    Integer marks = examMarks.get(examType);
                    if (marks != null) {
                        subjectMarks += marks;
                    }
                }
            }
            
            // Multiply max_marks by number of selected exam types for this subject
            int selectedExamCount = entry.getValue().size();
            int subjectMaxMarks = info.maxMarks * selectedExamCount;
            
            totalObtainedMarks += subjectMarks;
            totalMaxMarks += subjectMaxMarks;
            
            // Calculate grade points (out of 10) for this subject
            double subjectPercentage = subjectMaxMarks > 0 ? (subjectMarks * 100.0) / subjectMaxMarks : 0.0;
            double gradePoints = subjectPercentage / 10.0; // Convert percentage to 10-point scale
            
            // Weighted points = grade_points × credit
            double weightedPoints = gradePoints * info.credit;
            totalWeightedPoints += weightedPoints;
            totalCredits += info.credit;
            
            System.out.println("Subject: " + subject);
            System.out.println("  Marks: " + subjectMarks + "/" + subjectMaxMarks + " (" + String.format("%.1f%%", subjectPercentage) + ")");
            System.out.println("  Grade Points: " + String.format("%.2f", gradePoints) + " × Credit: " + info.credit + " = " + String.format("%.2f", weightedPoints));
        }
        
        // Calculate final SGPA and percentage
        double sgpa = totalCredits > 0 ? totalWeightedPoints / totalCredits : 0.0;
        double percentage = totalMaxMarks > 0 ? (totalObtainedMarks * 100.0) / totalMaxMarks : 0.0;
        
        System.out.println("\nTotal Weighted Points: " + String.format("%.2f", totalWeightedPoints));
        System.out.println("Total Credits: " + totalCredits);
        System.out.println("SGPA: " + String.format("%.2f", sgpa) + " / 10.00");
        System.out.println("Overall: " + totalObtainedMarks + "/" + totalMaxMarks + " (" + String.format("%.2f%%", percentage) + ")");
        System.out.println("====================================\n");
        
        // Count total exam components selected
        int examCount = 0;
        for (Set<String> exams : selectedFilters.values()) {
            examCount += exams.size();
        }
        
        // Calculate letter grade and rank
        String letterGrade = getLetterGrade(percentage);
        Color gradeColor = getGradeColor(letterGrade);
        int studentRank = calculateStudentRank(currentStudent.getId());
        
        // Row 1: Main metrics
        metricsGrid.add(createLargeMetricCard("📊", "Total Marks", totalObtainedMarks + "/" + totalMaxMarks, WARNING_COLOR));
        metricsGrid.add(createLargeMetricCard("🎯", "SGPA", String.format("%.2f", sgpa), SUCCESS_COLOR));
        metricsGrid.add(createLargeMetricCard("📈", "Percentage", String.format("%.1f%%", percentage), PRIMARY_COLOR));
        metricsGrid.add(createLargeMetricCard("🏆", "Rank", String.valueOf(studentRank), new Color(234, 179, 8)));
        
        // Row 2: Secondary metrics
        metricsGrid.add(createLargeMetricCard("📚", "Subjects", String.valueOf(subjectCount), INFO_COLOR));
        metricsGrid.add(createLargeMetricCard("📝", "Exams", String.valueOf(examCount), new Color(147, 51, 234)));
        metricsGrid.add(createLargeMetricCard("🎓", "Grade", letterGrade, gradeColor));
        metricsGrid.add(createLargeMetricCard("✅", "Status", percentage >= 50 ? "Pass" : "Fail", percentage >= 50 ? SUCCESS_COLOR : DANGER_COLOR));
        
        mainAnalysisPanel.add(metricsGrid);
        mainAnalysisPanel.add(Box.createVerticalStrut(20));
        
        // Add Export Button
        JPanel exportPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        exportPanel.setOpaque(false);
        exportPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Make variables effectively final for lambda
        final double finalSgpa = sgpa;
        final double finalPercentage = percentage;
        final String finalGrade = letterGrade;
        final int finalObtained = totalObtainedMarks;
        final int finalMax = totalMaxMarks;
        final int finalSubjects = subjectCount;
        final int finalExams = examCount;
        
        JButton exportPDFBtn = createModernButton("📄 Export PDF Report");
        exportPDFBtn.addActionListener(e -> exportToPDF(finalSgpa, finalPercentage, finalGrade, finalObtained, finalMax, finalSubjects, finalExams));
        exportPanel.add(exportPDFBtn);
        
        mainAnalysisPanel.add(exportPanel);
        
        analysisPanel.add(mainAnalysisPanel, BorderLayout.CENTER);
        analysisPanel.setVisible(true);
        analysisPanel.revalidate();
        analysisPanel.repaint();
        
        // Force parent containers to update
        SwingUtilities.invokeLater(() -> {
            Component parent = analysisPanel.getParent();
            if (parent != null) {
                parent.revalidate();
                parent.repaint();
            }
        });
    }

    // Calculate letter grade from percentage
    private String getLetterGrade(double percentage) {
        if (percentage >= 90) return "A+";
        else if (percentage >= 85) return "A";
        else if (percentage >= 80) return "B+";
        else if (percentage >= 75) return "B";
        else if (percentage >= 70) return "C+";
        else if (percentage >= 65) return "C";
        else if (percentage >= 60) return "D+";
        else if (percentage >= 50) return "D";
        else return "F";
    }
    
    // Get grade color
    private Color getGradeColor(String grade) {
        switch(grade) {
            case "A+": case "A": return SUCCESS_COLOR;
            case "B+": case "B": return new Color(34, 197, 94);
            case "C+": case "C": return WARNING_COLOR;
            case "D+": case "D": return new Color(251, 146, 60);
            default: return DANGER_COLOR;
        }
    }
    
    // Calculate student rank within section
    private int calculateStudentRank(int studentId) {
        try {
            // Get all students in the same section
            String query = "SELECT id FROM students WHERE section_id = (SELECT section_id FROM students WHERE id = ?) ORDER BY id";
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            
            List<Integer> studentIds = new ArrayList<>();
            while (rs.next()) {
                studentIds.add(rs.getInt("id"));
            }
            
            // For now, return position in list (can be enhanced with actual SGPA comparison)
            int rank = studentIds.indexOf(studentId) + 1;
            return rank > 0 ? rank : 1;
            
        } catch (Exception e) {
            System.err.println("Error calculating rank: " + e.getMessage());
            return 1;
        }
    }
    
    // Get performance icon based on percentage
    private String getPerformanceIcon(double percentage) {
        if (percentage >= 75) return "● ";
        else if (percentage >= 50) return "● ";
        else return "● ";
    }
    
    // Get color for performance indicator
    private Color getPerformanceColor(double percentage) {
        if (percentage >= 75) return new Color(34, 197, 94);  // Green
        else if (percentage >= 50) return new Color(234, 179, 8);  // Yellow/Orange
        else return new Color(239, 68, 68);  // Red
    }
    
    /**
     * Export student analysis to PDF
     */
    private void exportToPDF(double sgpa, double percentage, String grade, int totalObtained, int totalMax, 
                             int subjectCount, int examCount) {
        try {
            // Create file chooser
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Student Report");
            fileChooser.setSelectedFile(new java.io.File(currentStudent.getName().replaceAll("\\s+", "_") + "_Report.pdf"));
            
            int userSelection = fileChooser.showSaveDialog(this);
            
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                java.io.File fileToSave = fileChooser.getSelectedFile();
                String filePath = fileToSave.getAbsolutePath();
                if (!filePath.toLowerCase().endsWith(".pdf")) {
                    filePath += ".pdf";
                }
                
                // Create PDF document using iText
                com.itextpdf.text.Document document = new com.itextpdf.text.Document(com.itextpdf.text.PageSize.A4);
                com.itextpdf.text.pdf.PdfWriter.getInstance(document, new java.io.FileOutputStream(filePath));
                document.open();
                
                // Title
                com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 20, com.itextpdf.text.Font.BOLD);
                com.itextpdf.text.Paragraph title = new com.itextpdf.text.Paragraph("Student Academic Analysis Report", titleFont);
                title.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                title.setSpacingAfter(20);
                document.add(title);
                
                // Student Information Section
                com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 14, com.itextpdf.text.Font.BOLD);
                com.itextpdf.text.Font normalFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 11, com.itextpdf.text.Font.NORMAL);
                
                com.itextpdf.text.Paragraph studentHeader = new com.itextpdf.text.Paragraph("Student Information", headerFont);
                studentHeader.setSpacingAfter(10);
                document.add(studentHeader);
                
                com.itextpdf.text.pdf.PdfPTable infoTable = new com.itextpdf.text.pdf.PdfPTable(2);
                infoTable.setWidthPercentage(100);
                infoTable.setSpacingAfter(20);
                
                addPdfTableRow(infoTable, "Name:", currentStudent.getName(), normalFont);
                addPdfTableRow(infoTable, "Roll Number:", currentStudent.getRollNumber(), normalFont);
                addPdfTableRow(infoTable, "Section:", currentStudent.getSection() != null ? currentStudent.getSection() : "N/A", normalFont);
                addPdfTableRow(infoTable, "Report Generated:", new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new java.util.Date()), normalFont);
                
                document.add(infoTable);
                
                // Performance Summary Section
                com.itextpdf.text.Paragraph summaryHeader = new com.itextpdf.text.Paragraph("Performance Summary", headerFont);
                summaryHeader.setSpacingAfter(10);
                document.add(summaryHeader);
                
                com.itextpdf.text.pdf.PdfPTable summaryTable = new com.itextpdf.text.pdf.PdfPTable(2);
                summaryTable.setWidthPercentage(100);
                summaryTable.setSpacingAfter(20);
                
                addPdfTableRow(summaryTable, "Overall Percentage:", String.format("%.2f%%", percentage), normalFont);
                addPdfTableRow(summaryTable, "Letter Grade:", grade, normalFont);
                addPdfTableRow(summaryTable, "SGPA:", String.format("%.2f / 10.00", sgpa), normalFont);
                addPdfTableRow(summaryTable, "Total Marks:", String.format("%d / %d", totalObtained, totalMax), normalFont);
                addPdfTableRow(summaryTable, "Subjects Analyzed:", String.valueOf(subjectCount), normalFont);
                addPdfTableRow(summaryTable, "Exam Components:", String.valueOf(examCount), normalFont);
                
                document.add(summaryTable);
                
                // Marks Breakdown Section
                com.itextpdf.text.Paragraph marksHeader = new com.itextpdf.text.Paragraph("Detailed Marks Breakdown", headerFont);
                marksHeader.setSpacingAfter(10);
                document.add(marksHeader);
                
                AnalyzerDAO analyzerDAO = new AnalyzerDAO();
                Map<String, Map<String, Integer>> marks = currentStudent.getMarks();
                Set<String> allExamTypes = new LinkedHashSet<>();
                
                // Collect all exam types from selected subjects
                for (String subject : marks.keySet()) {
                    if (selectedFilters.containsKey(subject)) {
                        allExamTypes.addAll(marks.get(subject).keySet());
                    }
                }
                
                // Create marks table
                int columnCount = 2 + allExamTypes.size() + 1; // Subject + exam types + Total + Grade
                com.itextpdf.text.pdf.PdfPTable marksTable = new com.itextpdf.text.pdf.PdfPTable(columnCount);
                marksTable.setWidthPercentage(100);
                marksTable.setSpacingAfter(20);
                
                // Header row
                com.itextpdf.text.Font tableHeaderFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD);
                addPdfTableCell(marksTable, "Subject", tableHeaderFont, true);
                for (String examType : allExamTypes) {
                    addPdfTableCell(marksTable, examType, tableHeaderFont, true);
                }
                addPdfTableCell(marksTable, "Total", tableHeaderFont, true);
                addPdfTableCell(marksTable, "Grade", tableHeaderFont, true);
                
                // Data rows
                com.itextpdf.text.Font tableCellFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.NORMAL);
                for (Map.Entry<String, Map<String, Integer>> entry : marks.entrySet()) {
                    String subject = entry.getKey();
                    if (!selectedFilters.containsKey(subject)) continue;
                    
                    Map<String, Integer> examMarks = entry.getValue();
                    int rowTotal = 0;
                    int examCountForSubject = 0;
                    
                    addPdfTableCell(marksTable, subject, tableCellFont, false);
                    
                    for (String examType : allExamTypes) {
                        Integer mark = examMarks.get(examType);
                        if (mark != null && selectedFilters.get(subject).contains(examType)) {
                            addPdfTableCell(marksTable, String.valueOf(mark), tableCellFont, false);
                            rowTotal += mark;
                            examCountForSubject++;
                        } else {
                            addPdfTableCell(marksTable, "-", tableCellFont, false);
                        }
                    }
                    
                    // Calculate grade
                    AnalyzerDAO.SubjectConfig info = analyzerDAO.getSubjectInfo(currentStudent.getId(), subject);
                    int subjectMaxMarks = info.maxMarks * examCountForSubject;
                    double subjectPercentage = subjectMaxMarks > 0 ? (rowTotal * 100.0) / subjectMaxMarks : 0.0;
                    String subjectGrade = getLetterGrade(subjectPercentage);
                    
                    addPdfTableCell(marksTable, String.valueOf(rowTotal), tableCellFont, false);
                    addPdfTableCell(marksTable, subjectGrade, tableCellFont, false);
                }
                
                document.add(marksTable);
                
                // Grade Legend
                com.itextpdf.text.Paragraph legendHeader = new com.itextpdf.text.Paragraph("Grade Legend", headerFont);
                legendHeader.setSpacingAfter(10);
                document.add(legendHeader);
                
                com.itextpdf.text.Font legendFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.NORMAL);
                com.itextpdf.text.Paragraph legend = new com.itextpdf.text.Paragraph(
                    "A+ (90-100%) | A (85-89%) | B+ (80-84%) | B (75-79%) | C+ (70-74%) | C (65-69%) | D+ (60-64%) | D (50-59%) | F (<50%)", 
                    legendFont);
                document.add(legend);
                
                document.close();
                
                JOptionPane.showMessageDialog(this, 
                    "Report exported successfully!\n" + filePath,
                    "Export Success", 
                    JOptionPane.INFORMATION_MESSAGE);
                
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Failed to export report: " + e.getMessage(),
                "Export Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Helper method to add rows to PDF table
    private void addPdfTableRow(com.itextpdf.text.pdf.PdfPTable table, String label, String value, com.itextpdf.text.Font font) {
        com.itextpdf.text.pdf.PdfPCell labelCell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(label, font));
        labelCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        
        com.itextpdf.text.pdf.PdfPCell valueCell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(value, font));
        valueCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
        valueCell.setPadding(5);
        
        table.addCell(labelCell);
        table.addCell(valueCell);
    }
    
    // Helper method to add cells to marks table
    private void addPdfTableCell(com.itextpdf.text.pdf.PdfPTable table, String text, com.itextpdf.text.Font font, boolean isHeader) {
        com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(text, font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        
        if (isHeader) {
            cell.setBackgroundColor(new com.itextpdf.text.BaseColor(229, 231, 235));
        }
        
        table.addCell(cell);
    }

    // Create a more compact metric card for single row layout
    private JPanel createCompactMetricCard(String icon, String title, String value, Color color) {
        System.out.println("Creating metric card: " + title + " = " + value + " (color: " + color + ")");
        
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, color),
                BorderFactory.createLineBorder(new Color(229, 231, 235), 1)
            ),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        card.setPreferredSize(new Dimension(140, 100));
        card.setMinimumSize(new Dimension(130, 100));
        card.setMaximumSize(new Dimension(160, 100));
        
        // Icon and title row
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        titleLabel.setForeground(TEXT_SECONDARY);
        
        headerPanel.add(iconLabel);
        headerPanel.add(titleLabel);
        
        // Value label
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        valueLabel.setForeground(color);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        
        System.out.println("  Value label: text='" + valueLabel.getText() + "', visible=" + valueLabel.isVisible());
        
        card.add(headerPanel);
        card.add(valueLabel);
        
        return card;
    }
    
    // Create larger metric card for tabbed view with better visibility
    private JPanel createLargeMetricCard(String icon, String title, String value, Color color) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, color),
                BorderFactory.createLineBorder(new Color(229, 231, 235), 1)
            ),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        card.setPreferredSize(new Dimension(110, 80));
        card.setMaximumSize(new Dimension(140, 80));
        
        // Icon with reduced size
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        iconLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Title label
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        titleLabel.setForeground(TEXT_SECONDARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Value label with reduced font
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        valueLabel.setForeground(color);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        card.add(iconLabel);
        card.add(Box.createVerticalStrut(3));
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(3));
        card.add(valueLabel);
        card.add(Box.createVerticalGlue());
        
        return card;
    }
    
    // Create metric card with subtext (for grade display)
    private JPanel createMetricCardWithSubtext(String icon, String title, String value, String subtext, Color color, Color subtextColor) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, color),
                BorderFactory.createLineBorder(new Color(229, 231, 235), 1)
            ),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        card.setPreferredSize(new Dimension(110, 80));
        card.setMaximumSize(new Dimension(140, 80));
        
        // Icon with reduced size
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        iconLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Title label
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        titleLabel.setForeground(TEXT_SECONDARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Value label with reduced font
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        valueLabel.setForeground(color);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Subtext label (grade) - reduced size
        JLabel subtextLabel = new JLabel(subtext);
        subtextLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        subtextLabel.setForeground(subtextColor);
        subtextLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        card.add(iconLabel);
        card.add(Box.createVerticalStrut(3));
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(valueLabel);
        card.add(Box.createVerticalStrut(3));
        card.add(subtextLabel);
        card.add(Box.createVerticalGlue());
        
        return card;
    }

    // Update createModernTextField to be more compact
    private JTextField createModernTextField() {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Background
                g2.setColor(new Color(249, 250, 251));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                
                // Border
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                
                g2.dispose();
                
                // Paint text
                super.paintComponent(g);
            }
        };
        field.setOpaque(false);
        field.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15)); // Reduced padding
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        field.setPreferredSize(new Dimension(300, 40));
        return field;
    }
    
    private JPanel createModernCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw shadow
                g2.setColor(new Color(0, 0, 0, 10));
                g2.fillRoundRect(2, 2, getWidth() - 2, getHeight() - 2, 20, 20);
                
                // Draw card
                g2.setColor(CARD_COLOR);
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 20, 20);
                
                // Draw border
                g2.setColor(new Color(0, 0, 0, 30));
                g2.drawRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 20, 20);
                
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        return card;
    }
 
    
    private JButton createModernButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Gradient background
                if (getModel().isPressed()) {
                    g2.setPaint(new GradientPaint(0, 0, PRIMARY_DARK, 0, getHeight(), PRIMARY_COLOR));
                } else {
                    g2.setPaint(new GradientPaint(0, 0, PRIMARY_COLOR, 0, getHeight(), PRIMARY_DARK));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                
                // Text
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                
                g2.dispose();
            }
        };
        button.setFont(new Font("SansSerif", Font.BOLD, 16));
        button.setForeground(Color.WHITE);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        button.setPreferredSize(new Dimension(300, 45));
        return button;
    }
    
    private JRadioButton createModernRadioButton(String text, boolean selected) {
        JRadioButton radio = new JRadioButton(text, selected);
        radio.setFont(new Font("SansSerif", Font.PLAIN, 16));
        radio.setOpaque(false);
        radio.setForeground(TEXT_PRIMARY);
        radio.setIcon(new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BORDER_COLOR);
                g2.drawOval(x, y, 18, 18);
                g2.dispose();
            }
            @Override
            public int getIconWidth() { return 20; }
            @Override
            public int getIconHeight() { return 20; }
        });
        radio.setSelectedIcon(new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PRIMARY_COLOR);
                g2.fillOval(x + 4, y + 4, 10, 10);
                g2.setColor(PRIMARY_COLOR);
                g2.drawOval(x, y, 18, 18);
                g2.dispose();
            }
            @Override
            public int getIconWidth() { return 20; }
            @Override
            public int getIconHeight() { return 20; }
        });
        return radio;
    }
    
    private void analyzeStudent() {
        // Validate section selection
        if (sectionDropdown.getSelectedIndex() == 0) {
            JOptionPane.showMessageDialog(this, 
                "Please select a section first", 
                "Section Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String selectedSection = (String) sectionDropdown.getSelectedItem();
        String rollNumber = rollNumberField.getText().trim();
        
        if (rollNumber.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please enter roll number", 
                "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get student from database with section filter
        AnalyzerDAO analyzerDAO = new AnalyzerDAO();
        currentStudent = analyzerDAO.getStudentByRollAndSection(
            rollNumber,
            selectedSection,
            currentUserId
        );
        
        if (currentStudent == null) {
            JOptionPane.showMessageDialog(this, 
                "Student with Roll Number '" + rollNumber + "' not found in section '" + selectedSection + "'.\nPlease check the section and roll number.", 
                "Student Not Found", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Initialize selectedFilters with all subjects and exam types (default = "Overall")
        initializeFilters();
        
        // Hide input card and show filter card
        inputCard.setVisible(false);
        filterCard.setVisible(true);
        createFilterTree();
        
        displayResults();
    }
    
    private void displayResults() {
        resultsPanel.removeAll();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        
        // Results header with icon
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        headerPanel.setOpaque(false);
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel iconLabel = new JLabel("👤");
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 24));
        
        JLabel resultsHeader = new JLabel("Results");
        resultsHeader.setFont(new Font("SansSerif", Font.BOLD, 20));
        resultsHeader.setForeground(TEXT_PRIMARY);
        
        headerPanel.add(iconLabel);
        headerPanel.add(resultsHeader);
        
        // Student info
        JLabel nameLabel = new JLabel(currentStudent.getName());
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        nameLabel.setForeground(TEXT_PRIMARY);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel rollLabel = new JLabel("Roll Number: " + currentStudent.getRollNumber());
        rollLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        rollLabel.setForeground(TEXT_SECONDARY);
        rollLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        String section = currentStudent.getSection() != null ? currentStudent.getSection() : "Unknown";
        JLabel sectionLabel = new JLabel("Section: " + section);
        sectionLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        sectionLabel.setForeground(TEXT_SECONDARY);
        sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Create pivot table (subjects as rows, exam types as columns)
        Set<String> allExamTypes = new TreeSet<>();
        for (String subject : currentStudent.getMarks().keySet()) {
            allExamTypes.addAll(currentStudent.getMarks().get(subject).keySet());
        }
        
        // Build columns: Subject | ExamType1 | ExamType2 | ... | Total
        List<String> columns = new ArrayList<>();
        columns.add("Subject");
        columns.addAll(allExamTypes);
        columns.add("Total");
        
        DefaultTableModel tableModel = new DefaultTableModel(columns.toArray(new String[0]), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        // Add rows for each subject (only if in selectedFilters)
        int grandTotal = 0;
        Map<String, Double> subjectPercentages = new HashMap<>();
        AnalyzerDAO analyzerDAO = new AnalyzerDAO();
        
        for (String subject : currentStudent.getMarks().keySet()) {
            if (!selectedFilters.containsKey(subject)) continue;
            
            List<Object> rowData = new ArrayList<>();
            
            int rowTotal = 0;
            int examCount = 0;
            for (String examType : allExamTypes) {
                Integer marks = currentStudent.getMarks().get(subject).get(examType);
                
                // Check if this exam type is selected
                boolean isSelected = selectedFilters.get(subject).contains(examType);
                
                if (marks != null && isSelected) {
                    rowData.add(marks);
                    rowTotal += marks;
                    examCount++;
                } else if (marks != null && !isSelected) {
                    rowData.add("-"); // Not selected
                } else {
                    rowData.add("-"); // Subject doesn't have this exam type
                }
            }
            
            // Calculate subject percentage for performance indicator
            AnalyzerDAO.SubjectConfig info = analyzerDAO.getSubjectInfo(currentStudent.getId(), subject);
            int subjectMaxMarks = info.maxMarks * examCount;
            double subjectPercentage = subjectMaxMarks > 0 ? (rowTotal * 100.0) / subjectMaxMarks : 0.0;
            subjectPercentages.put(subject, subjectPercentage);
            
            // Add performance icon and subject name with HTML for color
            String performanceIcon = getPerformanceIcon(subjectPercentage);
            Color iconColor = getPerformanceColor(subjectPercentage);
            String colorHex = String.format("#%02x%02x%02x", iconColor.getRed(), iconColor.getGreen(), iconColor.getBlue());
            String cellText = String.format("<html><span style='color:%s; font-size:10px;'>●</span> <span style='color:#1f2937;'>%s</span></html>", colorHex, subject);
            rowData.add(0, cellText);
            
            rowData.add(rowTotal);
            grandTotal += rowTotal;
            tableModel.addRow(rowData.toArray());
        }
        
        // Add column totals row (only for Total column)
        List<Object> columnTotalsRow = new ArrayList<>();
        columnTotalsRow.add("<html><b>Column Total</b></html>");
        
        // Add empty cells for exam type columns
        for (int i = 0; i < allExamTypes.size(); i++) {
            columnTotalsRow.add("");
        }
        
        // Add grand total in the last column
        columnTotalsRow.add(grandTotal);
        tableModel.addRow(columnTotalsRow.toArray());
        
        JTable marksTable = new JTable(tableModel);
        marksTable.setRowHeight(40);
        marksTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
        marksTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        marksTable.getTableHeader().setBackground(new Color(249, 250, 251));
        marksTable.getTableHeader().setForeground(TEXT_PRIMARY);
        marksTable.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_COLOR));
        marksTable.setShowGrid(true);
        marksTable.setGridColor(BORDER_COLOR);
        marksTable.setIntercellSpacing(new Dimension(1, 1));
        marksTable.setBackground(Color.WHITE);
        
        // Custom cell renderer
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
                
                label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
                label.setBackground(Color.WHITE);
                label.setForeground(TEXT_PRIMARY);
                label.setFont(new Font("SansSerif", Font.PLAIN, 13));
                
                // First column (Subject) - left aligned
                if (column == 0) {
                    label.setHorizontalAlignment(JLabel.LEFT);
                    label.setFont(new Font("SansSerif", Font.BOLD, 13));
                } else {
                    label.setHorizontalAlignment(JLabel.CENTER);
                }
                
                // Last row (Column Total row) - bold with darker background
                if (row == table.getRowCount() - 1) {
                    label.setFont(new Font("SansSerif", Font.BOLD, 14));
                    label.setBackground(new Color(229, 231, 235));
                }
                
                // Last column (Row Total column) - bold with light background
                if (column == table.getColumnCount() - 1) {
                    label.setFont(new Font("SansSerif", Font.BOLD, 14));
                    label.setBackground(new Color(243, 244, 246));
                }
                
                // Bottom-right cell (Grand Total) - extra bold with accent color
                if (row == table.getRowCount() - 1 && column == table.getColumnCount() - 1) {
                    label.setFont(new Font("SansSerif", Font.BOLD, 15));
                    label.setBackground(new Color(219, 234, 254)); // Light blue
                    label.setForeground(new Color(29, 78, 216)); // Dark blue
                }
                
                return label;
            }
        };
        
        for (int i = 0; i < marksTable.getColumnCount(); i++) {
            marksTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
        
        // Auto-adjust column widths based on content
        marksTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int column = 0; column < marksTable.getColumnCount(); column++) {
            TableColumn tableColumn = marksTable.getColumnModel().getColumn(column);
            int preferredWidth = tableColumn.getMinWidth();
            int maxWidth = 300; // Maximum column width
            
            // Check header width
            TableCellRenderer headerRenderer = marksTable.getTableHeader().getDefaultRenderer();
            Component headerComp = headerRenderer.getTableCellRendererComponent(
                marksTable, tableColumn.getHeaderValue(), false, false, 0, column);
            preferredWidth = Math.max(preferredWidth, headerComp.getPreferredSize().width + 20);
            
            // Check all rows for this column
            for (int row = 0; row < marksTable.getRowCount(); row++) {
                TableCellRenderer cellRenderer2 = marksTable.getCellRenderer(row, column);
                Component comp = marksTable.prepareRenderer(cellRenderer2, row, column);
                int cellWidth = comp.getPreferredSize().width + 20; // Add padding
                preferredWidth = Math.max(preferredWidth, cellWidth);
            }
            
            // Apply width constraints
            preferredWidth = Math.min(preferredWidth, maxWidth);
            tableColumn.setPreferredWidth(preferredWidth);
        }
        
        JScrollPane scrollPane = new JScrollPane(marksTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true));
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setPreferredSize(new Dimension(520, 700));
        scrollPane.setMaximumSize(new Dimension(550, 800));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        // Total marks label
        JLabel totalMarksLabel = new JLabel("Grand Total: " + grandTotal);
        totalMarksLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        totalMarksLabel.setForeground(TEXT_PRIMARY);
        totalMarksLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        resultsPanel.add(headerPanel);
        resultsPanel.add(Box.createVerticalStrut(20));
        resultsPanel.add(nameLabel);
        resultsPanel.add(Box.createVerticalStrut(5));
        resultsPanel.add(rollLabel);
        resultsPanel.add(Box.createVerticalStrut(5));
        resultsPanel.add(sectionLabel);
        resultsPanel.add(Box.createVerticalStrut(20));
        resultsPanel.add(scrollPane);
        resultsPanel.add(Box.createVerticalStrut(15));
        resultsPanel.add(totalMarksLabel);
        
        // Update analysis panel with calculated totals
        updateAnalysisPanel(grandTotal);
        
        // Create bar chart with filtered data
        createModernBarChartFromFiltered();
        
        // Update subject performance panel
        updateSubjectPerformancePanel(grandTotal);
        
        // Make panels visible
        resultsPanel.setVisible(true);
        
        // Show the tabbed pane
        JTabbedPane tabbedPane = (JTabbedPane) this.getClientProperty("tabbedPane");
        if (tabbedPane != null) {
            tabbedPane.setVisible(true);
        }
        
        // Refresh UI
        revalidate();
        repaint();
    }
 
    
    private JPanel createMetricCard(String icon, String title, String value, Color color) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Background
                g2.setColor(new Color(249, 250, 251));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                
                // Left border accent
                g2.setColor(color);
                g2.fillRoundRect(0, 0, 4, getHeight(), 2, 2);
                
                // Border
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
                
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(8, 2));
        card.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
        card.setPreferredSize(new Dimension(180, 80));
        
        // Top panel with icon and title
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        topPanel.setOpaque(false);
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 18));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        titleLabel.setForeground(TEXT_SECONDARY);
        
        topPanel.add(iconLabel);
        topPanel.add(titleLabel);
        
        // Value
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        valueLabel.setForeground(color);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        card.add(topPanel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        
        return card;
    }
    
    private void updateSubjectPerformancePanel(int totalMarks) {
        // This method is called to update total marks if needed
    }
    
    private String getGrade(int marks) {
        if (marks >= 90) return "A";
        if (marks >= 80) return "B";
        if (marks >= 70) return "C";
        if (marks >= 60) return "D";
        return "F";
    }
    
    private int calculateMaxPossibleMarks() {
        // Calculate max possible marks based on section's subject configuration
        int maxTotal = 0;
        
        System.out.println("\n=== Calculating Max Possible Marks ===");
        System.out.println("Student ID: " + currentStudent.getId());
        System.out.println("Selected Filters: " + selectedFilters.keySet());
        
        try {
            AnalyzerDAO dao = new AnalyzerDAO();
            for (String subject : selectedFilters.keySet()) {
                // Get max marks for this subject from section_subjects table
                int subjectMax = dao.getMaxMarksForSubject(currentStudent.getId(), subject);
                System.out.println("Subject: " + subject + " -> Max Marks from DB: " + subjectMax + ", Selected exams: " + selectedFilters.get(subject).size());
                
                if (subjectMax > 0) {
                    // Use the subject's max marks directly (already represents total for subject)
                    maxTotal += subjectMax;
                } else {
                    // Fallback: assume 100 marks total for subject if not configured
                    System.out.println("  WARNING: No max marks found, using fallback 100");
                    maxTotal += 100;
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR in calculateMaxPossibleMarks: " + e.getMessage());
            e.printStackTrace();
            // Fallback: assume 100 marks per subject
            maxTotal = selectedFilters.size() * 100;
        }
        
        System.out.println("Total Max Possible Marks: " + maxTotal);
        System.out.println("====================================\n");
        return maxTotal;
    }
    
    private double calculatePercentage(Map<String, Map<String, Integer>> marks) {
        if (marks.isEmpty()) return 0.0;
        
        int total = 0;
        int count = 0;
        for (Map<String, Integer> examTypes : marks.values()) {
            for (Integer mark : examTypes.values()) {
                total += mark;
                count++;
            }
        }
        
        return count > 0 ? (double) total / count : 0.0;
    }
    
    private double calculateSGPA(double percentage) {
        if (percentage >= 90) return 10.0;
        if (percentage >= 80) return 9.0;
        if (percentage >= 70) return 8.0;
        if (percentage >= 60) return 7.0;
        if (percentage >= 50) return 6.0;
        return 0.0;
    }
    
    private void createModernBarChart(Map<String, Map<String, Integer>> marks) {
        subjectPerformancePanel.removeAll();
        subjectPerformancePanel.setLayout(new BorderLayout(0, 15));
        
        // Chart header
        JLabel chartTitle = new JLabel("📊 Subject Performance");
        chartTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        chartTitle.setForeground(TEXT_PRIMARY);
        chartTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        // Find max marks for scaling and calculate total (aggregate across exam types)
        int maxMark = 0;
        int totalMarks = 0;
        for (Map.Entry<String, Map<String, Integer>> subjectEntry : marks.entrySet()) {
            String subject = subjectEntry.getKey();
            int subjectTotal = 0;
            for (Integer mark : subjectEntry.getValue().values()) {
                subjectTotal += mark;
            }
            dataset.addValue(subjectTotal, "Marks", subject);
            if (subjectTotal > maxMark) {
            	maxMark = subjectTotal;
            }
            totalMarks += subjectTotal;
        }
        
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12));
        xAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        xAxis.setTickLabelPaint(TEXT_PRIMARY);
        
        NumberAxis yAxis = new NumberAxis("Marks");
        yAxis.setRange(0, Math.max(100, maxMark + 10));
        yAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        yAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12));
        yAxis.setTickLabelPaint(TEXT_PRIMARY);
        yAxis.setLabelPaint(TEXT_PRIMARY);
        
        BarRenderer renderer = new BarRenderer() {
            @Override
            public Paint getItemPaint(int row, int column) {
                // Create gradient for bars
                return new GradientPaint(
                    0, 0, PRIMARY_COLOR,
                    0, 300, PRIMARY_DARK
                );
            }
        };
        renderer.setShadowVisible(false);
        renderer.setDrawBarOutline(false);
        renderer.setMaximumBarWidth(0.08);
        renderer.setItemMargin(0.1);
        renderer.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());
        
        // Add value labels on top of bars
        renderer.setDefaultItemLabelGenerator(new org.jfree.chart.labels.StandardCategoryItemLabelGenerator());
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelFont(new Font("SansSerif", Font.BOLD, 11));
        renderer.setDefaultItemLabelPaint(TEXT_PRIMARY);
        renderer.setDefaultPositiveItemLabelPosition(new org.jfree.chart.labels.ItemLabelPosition(
            org.jfree.chart.labels.ItemLabelAnchor.OUTSIDE12,
            TextAnchor.BOTTOM_CENTER
        ));
        
        CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(new Color(240, 240, 240));
        plot.setOutlineVisible(false);
        
        JFreeChart chart = new JFreeChart(plot);
        chart.setBackgroundPaint(Color.WHITE);
        chart.removeLegend();
        chart.setBorderVisible(false);
        
        ChartPanel chartComponent = new ChartPanel(chart);
        chartComponent.setPreferredSize(new Dimension(700, 400));
        chartComponent.setBackground(Color.WHITE);
        chartComponent.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        // Total marks label
        JLabel totalMarksLabel = new JLabel("Total Marks: " + totalMarks);
        totalMarksLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        totalMarksLabel.setForeground(TEXT_PRIMARY);
        totalMarksLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        totalMarksLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        subjectPerformancePanel.add(chartTitle, BorderLayout.NORTH);
        subjectPerformancePanel.add(chartComponent, BorderLayout.CENTER);
        subjectPerformancePanel.add(totalMarksLabel, BorderLayout.SOUTH);
    }
    
    private void showSectionAnalyzer(HashMap<String, ArrayList<Student>> sectionMap) {
        removeAll();
        
        SectionAnalyzer sectionPanel = new SectionAnalyzer(parentFrame, sectionMap, () -> {
            removeAll();
            initializeUI();
            revalidate();
            repaint();
            studentRadio.setSelected(true);
        });
        
        setLayout(new BorderLayout());
        add(sectionPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
    
    private void initializeFilters() {
        selectedFilters = new HashMap<>();
        // Select all by default (Overall)
        for (String subject : currentStudent.getMarks().keySet()) {
            Set<String> examTypes = new HashSet<>(currentStudent.getMarks().get(subject).keySet());
            selectedFilters.put(subject, examTypes);
        }
    }
    
    private void createFilterTree() {
        filterCard.removeAll();
        filterCard.setLayout(new BorderLayout(10, 10));
        
        JLabel filterLabel = new JLabel("Filter Analysis");
        filterLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        filterLabel.setForeground(TEXT_PRIMARY);
        
        // Create tree structure
        JPanel treePanel = new JPanel(new BorderLayout());
        treePanel.setBackground(Color.WHITE);
        treePanel.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true));
        
        // Overall checkbox
        JCheckBox overallCheck = new JCheckBox("Overall (All)", true);
        overallCheck.setFont(new Font("SansSerif", Font.BOLD, 14));
        overallCheck.setBackground(Color.WHITE);
        overallCheck.addActionListener(e -> {
            if (overallCheck.isSelected()) {
                initializeFilters(); // Select all
            } else {
                selectedFilters.clear(); // Deselect all
            }
            refreshFilterCheckboxes(treePanel);
            displayResults();
        });
        
        JPanel overallPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        overallPanel.setBackground(Color.WHITE);
        overallPanel.add(overallCheck);
        
        // Subject and exam type checkboxes
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));
        checkboxPanel.setBackground(Color.WHITE);
        
        for (String subject : currentStudent.getMarks().keySet()) {
            // Subject checkbox (select all exam types for this subject)
            JCheckBox subjectCheck = new JCheckBox("📁 " + subject, true);
            subjectCheck.setFont(new Font("SansSerif", Font.BOLD, 13));
            subjectCheck.setForeground(TEXT_PRIMARY);
            subjectCheck.setBackground(Color.WHITE);
            final String subj = subject;
            
            // Store exam checkboxes for this subject to update them together
            java.util.List<JCheckBox> examChecks = new java.util.ArrayList<>();
            
            subjectCheck.addActionListener(e -> {
                boolean selected = subjectCheck.isSelected();
                // Select/deselect all exam types for this subject
                for (JCheckBox examCheck : examChecks) {
                    examCheck.setSelected(selected);
                }
                
                if (selected) {
                    Set<String> examTypes = new HashSet<>(currentStudent.getMarks().get(subj).keySet());
                    selectedFilters.put(subj, examTypes);
                } else {
                    selectedFilters.remove(subj);
                }
                overallCheck.setSelected(isAllSelected());
                displayResults();
            });
            
            checkboxPanel.add(subjectCheck);
            
            // Exam type checkboxes (indented)
            for (String examType : currentStudent.getMarks().get(subject).keySet()) {
                JCheckBox examCheck = new JCheckBox("   └─ " + examType, true);
                examCheck.setFont(new Font("SansSerif", Font.PLAIN, 12));
                examCheck.setBackground(Color.WHITE);
                final String exam = examType;
                examChecks.add(examCheck);
                
                examCheck.addActionListener(e -> {
                    if (examCheck.isSelected()) {
                        selectedFilters.putIfAbsent(subj, new HashSet<>());
                        selectedFilters.get(subj).add(exam);
                    } else {
                        if (selectedFilters.containsKey(subj)) {
                            selectedFilters.get(subj).remove(exam);
                            if (selectedFilters.get(subj).isEmpty()) {
                                selectedFilters.remove(subj);
                            }
                        }
                    }
                    // Update subject checkbox based on exam selections
                    boolean allSelected = examChecks.stream().allMatch(JCheckBox::isSelected);
                    subjectCheck.setSelected(allSelected);
                    overallCheck.setSelected(isAllSelected());
                    displayResults();
                });
                
                checkboxPanel.add(examCheck);
            }
            
            checkboxPanel.add(Box.createVerticalStrut(10));
        }
        
        JScrollPane scrollPane = new JScrollPane(checkboxPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setPreferredSize(new Dimension(500, 250));
        
        treePanel.add(overallPanel, BorderLayout.NORTH);
        treePanel.add(scrollPane, BorderLayout.CENTER);
        
        filterCard.add(filterLabel, BorderLayout.NORTH);
        filterCard.add(treePanel, BorderLayout.CENTER);
        
        filterCard.revalidate();
        filterCard.repaint();
    }
    
    private void refreshFilterCheckboxes(JPanel treePanel) {
        // Recreate filter tree if needed
        createFilterTree();
    }
    
    private boolean isAllSelected() {
        for (String subject : currentStudent.getMarks().keySet()) {
            if (!selectedFilters.containsKey(subject)) return false;
            if (!selectedFilters.get(subject).containsAll(currentStudent.getMarks().get(subject).keySet())) {
                return false;
            }
        }
        return true;
    }
    
    private void createModernBarChartFromFiltered() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        int totalCategories = 0;
        for (String subject : selectedFilters.keySet()) {
            for (String examType : selectedFilters.get(subject)) {
                Integer marks = currentStudent.getMarks().get(subject).get(examType);
                if (marks != null) {
                    dataset.addValue(marks, examType, subject);
                    totalCategories++;
                }
            }
        }
        
        JFreeChart chart = ChartFactory.createBarChart(
            "Subject Performance",
            "Subject - Exam Type",
            "Marks",
            dataset
        );
        
        chart.setBackgroundPaint(Color.WHITE);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(BORDER_COLOR);
        
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, PRIMARY_COLOR);
        
        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(
            org.jfree.chart.axis.CategoryLabelPositions.UP_45
        );
        
        // Dynamic width: minimum 800px, add 60px per category for better spacing
        int chartWidth = Math.max(800, totalCategories * 60);
        
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(chartWidth, 500));
        
        subjectPerformancePanel.removeAll();
        subjectPerformancePanel.add(chartPanel, BorderLayout.CENTER);
        subjectPerformancePanel.revalidate();
        subjectPerformancePanel.repaint();
    }
    
    private void closePanel() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }
}
