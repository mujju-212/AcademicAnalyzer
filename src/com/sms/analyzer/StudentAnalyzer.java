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
    private static final Color SELECTION_COLOR = new Color(99, 102, 241, 30);
    
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
    private JPanel mainContentPanel; // Main content area
    private JPanel analysisLayout; // Analysis layout panel
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
    private void initializeUI() {
        setBackground(BACKGROUND_COLOR);
        
        // Main panel with padding - matching SectionAnalyzer
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Header Card - matching SectionAnalyzer structure
        // Header Card - properly sized to contain title and radio buttons
        JPanel headerCard = createModernCard();
        headerCard.setLayout(new BorderLayout(10, 15));
        headerCard.setPreferredSize(new Dimension(0, 140));
        headerCard.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));
        
        // Top section with back button and title
        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setOpaque(false);
        
        // Add back button if callback present at the top
        if (onCloseCallback != null) {
            JButton backButton = new JButton("← Back");
            backButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            backButton.setForeground(PRIMARY_COLOR);
            backButton.setBackground(CARD_COLOR);
            backButton.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
            backButton.setFocusPainted(false);
            backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            backButton.addActionListener(e -> closePanel());
            
            JPanel backPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            backPanel.setOpaque(false);
            backPanel.add(backButton);
            topSection.add(backPanel, BorderLayout.NORTH);
        }
        
        // Title
        JLabel titleLabel = new JLabel("STUDENT PERFORMANCE ANALYZER");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setForeground(PRIMARY_COLOR);
        topSection.add(titleLabel, BorderLayout.CENTER);
        
        headerCard.add(topSection, BorderLayout.NORTH);
        
        // Bottom section - Radio buttons (clean design without purple background)
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        radioPanel.setOpaque(false);
        radioPanel.setBorder(BorderFactory.createEmptyBorder(30, 0, 5, 0));
        
        ButtonGroup group = new ButtonGroup();
        
        studentRadio = createModernRadioButton("Student", true);
        sectionRadio = createModernRadioButton("Section", false);
        
        group.add(studentRadio);
        group.add(sectionRadio);
        
        radioPanel.add(studentRadio);
        radioPanel.add(sectionRadio);
        
        headerCard.add(radioPanel, BorderLayout.SOUTH);
        
        sectionRadio.addActionListener(e -> {
            // Show custom section selection dialog with dropdown
            showSectionSelectionDialog();
        });
        
        mainPanel.add(headerCard, BorderLayout.NORTH);
        
        // Main content panel - matching SectionAnalyzer
        mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setOpaque(false);
        
        createMainContent();
        
        mainPanel.add(mainContentPanel, BorderLayout.CENTER);
        add(mainPanel);
    }
    
    private void createMainContent() {
        // Clear existing content
        mainContentPanel.removeAll();
        
        // Student selection area (top of main content)
        JPanel studentSelectionCard = createModernCard();
        studentSelectionCard.setLayout(new BorderLayout(15, 10));
        studentSelectionCard.setPreferredSize(new Dimension(0, 100));
        studentSelectionCard.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        
        JLabel selectionTitle = new JLabel("Select Student for Analysis");
        selectionTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        selectionTitle.setForeground(TEXT_PRIMARY);
        
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        inputPanel.setOpaque(false);
        
        // Section Selection
        JLabel sectionLabel = new JLabel("Section:");
        sectionLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        sectionLabel.setForeground(TEXT_PRIMARY);
        
        sectionDropdown = new JComboBox<>();
        sectionDropdown.setFont(new Font("SansSerif", Font.PLAIN, 12));
        sectionDropdown.setBackground(Color.WHITE);
        sectionDropdown.setPreferredSize(new Dimension(200, 30));
        sectionDropdown.addItem("-- Select Section --");
        for (String section : sectionStudents.keySet()) {
            sectionDropdown.addItem(section);
        }
        
        // Roll Number Field
        JLabel rollLabel = new JLabel("Roll Number:");
        rollLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        rollLabel.setForeground(TEXT_PRIMARY);
        
        rollNumberField = new JTextField();
        rollNumberField.setFont(new Font("SansSerif", Font.PLAIN, 12));
        rollNumberField.setPreferredSize(new Dimension(150, 30));
        rollNumberField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        
        // Analyze Button
        JButton analyzeButton = new JButton("📊 Analyze Student");
        analyzeButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        analyzeButton.setBackground(PRIMARY_COLOR);
        analyzeButton.setForeground(Color.WHITE);
        analyzeButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        analyzeButton.setFocusPainted(false);
        analyzeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        analyzeButton.addActionListener(e -> analyzeStudent());
        
        inputPanel.add(sectionLabel);
        inputPanel.add(sectionDropdown);
        inputPanel.add(Box.createHorizontalStrut(10));
        inputPanel.add(rollLabel);
        inputPanel.add(rollNumberField);
        inputPanel.add(Box.createHorizontalStrut(10));
        inputPanel.add(analyzeButton);
        
        studentSelectionCard.add(selectionTitle, BorderLayout.NORTH);
        studentSelectionCard.add(inputPanel, BorderLayout.CENTER);
        
        mainContentPanel.add(studentSelectionCard, BorderLayout.NORTH);
        
        // Main analysis layout (hidden until student is selected)
        analysisLayout = new JPanel(new BorderLayout(0, 0));
        analysisLayout.setOpaque(false);
        analysisLayout.setVisible(false);
        
        mainContentPanel.add(analysisLayout, BorderLayout.CENTER);
        
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }
    
    private void createStudentAnalysisLayout() {
        if (analysisLayout == null) return;
        
        analysisLayout.removeAll();
        
        // Left side: Filter Panel (matching SectionAnalyzer)
        JPanel filterPanel = createFilterPanel();
        analysisLayout.add(filterPanel, BorderLayout.WEST);
        
        // Right side: Main content with tabs
        JPanel contentWrapper = createTabbedContent();
        analysisLayout.add(contentWrapper, BorderLayout.CENTER);
        
        analysisLayout.setVisible(true);
        analysisLayout.revalidate();
        analysisLayout.repaint();
    }

    // Update the updateAnalysisPanel method for tabbed layout
    private void updateAnalysisPanel(double sgpa, double percentage, String letterGrade, int subjectCount, int examCount, int studentRank) {
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
        
        // Calculate grade color
        Color gradeColor = getGradeColor(letterGrade);
        
        // Calculate total out of (subjects × 100)
        double actualTotal = (percentage / 100.0) * (subjectCount * 100);
        
        // Row 1: Main metrics
        metricsGrid.add(createLargeMetricCard("📊", "Total", String.format("%.0f", actualTotal) + "/" + (subjectCount * 100), PRIMARY_COLOR));
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
        
        JButton exportPDFBtn = createModernButton("📄 Export PDF Report");
        exportPDFBtn.addActionListener(e -> exportToPDF(sgpa, percentage, letterGrade, 0, subjectCount * 100, subjectCount, examCount));
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
        // For performance, return simplified rank based on ID order
        // Full rank calculation with weighted totals is too slow for large sections
        try {
            String query = "SELECT COUNT(*) as rank FROM students WHERE section_id = (SELECT section_id FROM students WHERE id = ?) AND id < ?";
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, studentId);
            stmt.setInt(2, studentId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("rank") + 1;
            }
            return 1;
        } catch (Exception e) {
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
     * Export student analysis to PDF with new weighted calculations and visual elements
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
                com.itextpdf.text.Document document = new com.itextpdf.text.Document(com.itextpdf.text.PageSize.A4, 40, 40, 50, 50);
                com.itextpdf.text.pdf.PdfWriter writer = com.itextpdf.text.pdf.PdfWriter.getInstance(document, new java.io.FileOutputStream(filePath));
                document.open();
                
                // Define colors
                com.itextpdf.text.BaseColor primaryColor = new com.itextpdf.text.BaseColor(59, 130, 246);
                com.itextpdf.text.BaseColor secondaryColor = new com.itextpdf.text.BaseColor(148, 163, 184);
                com.itextpdf.text.BaseColor successColor = new com.itextpdf.text.BaseColor(34, 197, 94);
                com.itextpdf.text.BaseColor headerBg = new com.itextpdf.text.BaseColor(239, 246, 255);
                
                // Title with colored background
                com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 24, com.itextpdf.text.Font.BOLD, primaryColor);
                com.itextpdf.text.Paragraph title = new com.itextpdf.text.Paragraph("📊 Academic Performance Report", titleFont);
                title.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                title.setSpacingAfter(25);
                document.add(title);
                
                // Student Information Section with modern styling
                com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 16, com.itextpdf.text.Font.BOLD, primaryColor);
                com.itextpdf.text.Font normalFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 11, com.itextpdf.text.Font.NORMAL);
                com.itextpdf.text.Font boldFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 11, com.itextpdf.text.Font.BOLD);
                
                com.itextpdf.text.Paragraph studentHeader = new com.itextpdf.text.Paragraph("👤 Student Information", headerFont);
                studentHeader.setSpacingAfter(12);
                document.add(studentHeader);
                
                com.itextpdf.text.pdf.PdfPTable infoTable = new com.itextpdf.text.pdf.PdfPTable(2);
                infoTable.setWidthPercentage(100);
                infoTable.setWidths(new float[]{1.2f, 2f});
                infoTable.setSpacingAfter(25);
                
                addModernPdfTableRow(infoTable, "Name:", currentStudent.getName(), boldFont, normalFont, headerBg);
                addModernPdfTableRow(infoTable, "Roll Number:", currentStudent.getRollNumber(), boldFont, normalFont, headerBg);
                addModernPdfTableRow(infoTable, "Section:", currentStudent.getSection() != null ? currentStudent.getSection() : "N/A", boldFont, normalFont, headerBg);
                int rank = calculateStudentRank(currentStudent.getId());
                addModernPdfTableRow(infoTable, "Rank:", String.valueOf(rank), boldFont, normalFont, headerBg);
                addModernPdfTableRow(infoTable, "Report Date:", new java.text.SimpleDateFormat("dd MMMM yyyy, HH:mm").format(new java.util.Date()), boldFont, normalFont, headerBg);
                
                document.add(infoTable);
                
                // Performance Metrics Cards Section
                com.itextpdf.text.Paragraph metricsHeader = new com.itextpdf.text.Paragraph("📈 Performance Metrics", headerFont);
                metricsHeader.setSpacingAfter(12);
                document.add(metricsHeader);
                
                // Calculate actual total out of (subjects × 100)
                double actualTotal = (percentage / 100.0) * (subjectCount * 100);
                
                // Create 4-column metrics table
                com.itextpdf.text.pdf.PdfPTable metricsTable = new com.itextpdf.text.pdf.PdfPTable(4);
                metricsTable.setWidthPercentage(100);
                metricsTable.setSpacingAfter(25);
                
                addMetricCard(metricsTable, "Total", String.format("%.0f / %d", actualTotal, subjectCount * 100), primaryColor);
                addMetricCard(metricsTable, "SGPA", String.format("%.2f / 10.00", sgpa), successColor);
                addMetricCard(metricsTable, "Percentage", String.format("%.2f%%", percentage), new com.itextpdf.text.BaseColor(139, 92, 246));
                addMetricCard(metricsTable, "Grade", grade, new com.itextpdf.text.BaseColor(249, 115, 22));
                
                document.add(metricsTable);
                
                // Detailed Marks Breakdown with weighted calculations
                com.itextpdf.text.Paragraph marksHeader = new com.itextpdf.text.Paragraph("📋 Detailed Marks Breakdown (Weighted)", headerFont);
                marksHeader.setSpacingAfter(12);
                document.add(marksHeader);
                
                AnalyzerDAO analyzerDAO = new AnalyzerDAO();
                Map<String, Map<String, Integer>> marks = currentStudent.getMarks();
                Set<String> allExamTypes = new LinkedHashSet<>();
                
                // Collect all exam types from selected subjects
                for (String subject : marks.keySet()) {
                    if (selectedFilters.containsKey(subject)) {
                        Set<String> selectedExams = selectedFilters.get(subject);
                        for (String examType : marks.get(subject).keySet()) {
                            if (selectedExams.contains(examType)) {
                                allExamTypes.add(examType);
                            }
                        }
                    }
                }
                
                // Create marks table with modern styling
                int columnCount = 2 + allExamTypes.size() + 1; // Subject + exam types + Weighted Total + Grade
                com.itextpdf.text.pdf.PdfPTable marksTable = new com.itextpdf.text.pdf.PdfPTable(columnCount);
                marksTable.setWidthPercentage(100);
                marksTable.setSpacingAfter(25);
                
                // Header row
                com.itextpdf.text.Font tableHeaderFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD, com.itextpdf.text.BaseColor.WHITE);
                addModernPdfTableCell(marksTable, "Subject", tableHeaderFont, primaryColor, true);
                for (String examType : allExamTypes) {
                    addModernPdfTableCell(marksTable, examType, tableHeaderFont, primaryColor, true);
                }
                addModernPdfTableCell(marksTable, "Weighted Total", tableHeaderFont, primaryColor, true);
                addModernPdfTableCell(marksTable, "Grade", tableHeaderFont, primaryColor, true);
                
                // Data rows with alternating colors
                com.itextpdf.text.Font tableCellFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.NORMAL);
                com.itextpdf.text.Font tableCellBoldFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.BOLD);
                
                int rowIndex = 0;
                double grandWeightedTotal = 0.0;
                
                for (Map.Entry<String, Map<String, Integer>> entry : marks.entrySet()) {
                    String subject = entry.getKey();
                    if (!selectedFilters.containsKey(subject)) continue;
                    
                    Set<String> selectedExams = selectedFilters.get(subject);
                    com.itextpdf.text.BaseColor rowBg = (rowIndex % 2 == 0) ? com.itextpdf.text.BaseColor.WHITE : new com.itextpdf.text.BaseColor(249, 250, 251);
                    
                    addModernPdfTableCell(marksTable, subject, tableCellBoldFont, rowBg, false);
                    
                    // Add exam marks
                    for (String examType : allExamTypes) {
                        Integer mark = entry.getValue().get(examType);
                        if (mark != null && selectedExams.contains(examType)) {
                            addModernPdfTableCell(marksTable, String.valueOf(mark), tableCellFont, rowBg, false);
                        } else {
                            addModernPdfTableCell(marksTable, "-", tableCellFont, rowBg, false);
                        }
                    }
                    
                    // Calculate weighted total using new calculation method
                    int studentSectionId = getStudentSectionId(currentStudent.getId());
                    double weightedTotal = analyzerDAO.calculateWeightedSubjectTotal(
                        currentStudent.getId(), studentSectionId, subject, selectedExams
                    );
                    grandWeightedTotal += weightedTotal;
                    
                    String subjectGrade = getLetterGrade(weightedTotal);
                    
                    addModernPdfTableCell(marksTable, String.format("%.2f / 100", weightedTotal), tableCellBoldFont, rowBg, false);
                    addModernPdfTableCell(marksTable, subjectGrade, tableCellBoldFont, rowBg, false);
                    
                    rowIndex++;
                }
                
                // Add TOTAL row
                com.itextpdf.text.BaseColor totalRowBg = new com.itextpdf.text.BaseColor(229, 231, 235);
                com.itextpdf.text.Font totalFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD);
                
                addModernPdfTableCell(marksTable, "TOTAL", totalFont, totalRowBg, false);
                
                // Sum each exam column
                for (String examType : allExamTypes) {
                    int columnSum = 0;
                    for (String subject : marks.keySet()) {
                        if (!selectedFilters.containsKey(subject)) continue;
                        Set<String> selectedExams = selectedFilters.get(subject);
                        Integer mark = marks.get(subject).get(examType);
                        if (mark != null && selectedExams.contains(examType)) {
                            columnSum += mark;
                        }
                    }
                    addModernPdfTableCell(marksTable, String.valueOf(columnSum), totalFont, totalRowBg, false);
                }
                
                // Grand weighted total
                addModernPdfTableCell(marksTable, String.format("%.0f / %d", grandWeightedTotal, subjectCount * 100), totalFont, totalRowBg, false);
                addModernPdfTableCell(marksTable, grade, totalFont, totalRowBg, false);
                
                document.add(marksTable);
                
                // Performance Summary Section
                com.itextpdf.text.Paragraph summaryHeader = new com.itextpdf.text.Paragraph("📊 Performance Summary", headerFont);
                summaryHeader.setSpacingAfter(12);
                document.add(summaryHeader);
                
                com.itextpdf.text.Paragraph summaryText = new com.itextpdf.text.Paragraph();
                summaryText.setFont(normalFont);
                summaryText.setAlignment(com.itextpdf.text.Element.ALIGN_JUSTIFIED);
                
                String performanceText = String.format(
                    "The student has achieved an overall weighted total of %.0f out of %d marks across %d subject(s), " +
                    "resulting in a percentage of %.2f%% and a SGPA of %.2f. The student's performance is graded as '%s'. " +
                    "This report uses weighted calculation where each exam type contributes its actual weightage percentage " +
                    "to the final score (no normalization). The rank is %d in the section.",
                    grandWeightedTotal, subjectCount * 100, subjectCount, percentage, sgpa, grade, rank
                );
                
                summaryText.add(performanceText);
                summaryText.setSpacingAfter(25);
                document.add(summaryText);
                
                // Grade Legend with modern styling
                com.itextpdf.text.Paragraph legendHeader = new com.itextpdf.text.Paragraph("📌 Grading System", headerFont);
                legendHeader.setSpacingAfter(12);
                document.add(legendHeader);
                
                com.itextpdf.text.pdf.PdfPTable legendTable = new com.itextpdf.text.pdf.PdfPTable(8);
                legendTable.setWidthPercentage(100);
                legendTable.setSpacingAfter(20);
                
                String[] grades = {"A+", "A", "B+", "B", "C+", "C", "D", "F"};
                String[] ranges = {"90-100", "85-89", "80-84", "75-79", "70-74", "65-69", "50-64", "< 50"};
                com.itextpdf.text.BaseColor[] gradeColors = {
                    new com.itextpdf.text.BaseColor(34, 197, 94),
                    new com.itextpdf.text.BaseColor(59, 130, 246),
                    new com.itextpdf.text.BaseColor(139, 92, 246),
                    new com.itextpdf.text.BaseColor(249, 115, 22),
                    new com.itextpdf.text.BaseColor(236, 72, 153),
                    new com.itextpdf.text.BaseColor(251, 191, 36),
                    new com.itextpdf.text.BaseColor(148, 163, 184),
                    new com.itextpdf.text.BaseColor(239, 68, 68)
                };
                
                com.itextpdf.text.Font gradeFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.BOLD, com.itextpdf.text.BaseColor.WHITE);
                com.itextpdf.text.Font rangeFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8, com.itextpdf.text.Font.NORMAL);
                
                for (int i = 0; i < grades.length; i++) {
                    com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell();
                    cell.setBackgroundColor(gradeColors[i]);
                    cell.setPadding(8);
                    cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                    cell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
                    
                    com.itextpdf.text.Phrase phrase = new com.itextpdf.text.Phrase();
                    phrase.add(new com.itextpdf.text.Chunk(grades[i] + "\n", gradeFont));
                    phrase.add(new com.itextpdf.text.Chunk(ranges[i] + "%", rangeFont));
                    
                    cell.setPhrase(phrase);
                    legendTable.addCell(cell);
                }
                
                document.add(legendTable);
                
                // Footer
                com.itextpdf.text.Font footerFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8, com.itextpdf.text.Font.ITALIC, secondaryColor);
                com.itextpdf.text.Paragraph footer = new com.itextpdf.text.Paragraph(
                    "Note: This report uses weighted calculation system where marks represent actual contribution (no normalization). " +
                    "Selecting only some exam types will show their actual weighted contribution, not normalized to 100%.",
                    footerFont
                );
                footer.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                document.add(footer);
                
                document.close();
                
                JOptionPane.showMessageDialog(this, 
                    "✅ Report exported successfully!\n\n" + filePath,
                    "Export Success", 
                    JOptionPane.INFORMATION_MESSAGE);
                
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "❌ Failed to export report: " + e.getMessage(),
                "Export Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Helper method to add modern styled rows to PDF table
    private void addModernPdfTableRow(com.itextpdf.text.pdf.PdfPTable table, String label, String value, 
                                     com.itextpdf.text.Font labelFont, com.itextpdf.text.Font valueFont,
                                     com.itextpdf.text.BaseColor bgColor) {
        com.itextpdf.text.pdf.PdfPCell labelCell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(label, labelFont));
        labelCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
        labelCell.setPadding(10);
        labelCell.setBackgroundColor(bgColor);
        
        com.itextpdf.text.pdf.PdfPCell valueCell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(value, valueFont));
        valueCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
        valueCell.setPadding(10);
        valueCell.setBackgroundColor(com.itextpdf.text.BaseColor.WHITE);
        
        table.addCell(labelCell);
        table.addCell(valueCell);
    }
    
    // Helper method to create metric cards in PDF
    private void addMetricCard(com.itextpdf.text.pdf.PdfPTable table, String title, String value, com.itextpdf.text.BaseColor color) {
        com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell();
        cell.setPadding(15);
        cell.setBackgroundColor(new com.itextpdf.text.BaseColor(249, 250, 251));
        cell.setBorderColor(color);
        cell.setBorderWidth(2);
        cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        
        com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.NORMAL, new com.itextpdf.text.BaseColor(100, 116, 139));
        com.itextpdf.text.Font valueFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 14, com.itextpdf.text.Font.BOLD, color);
        
        com.itextpdf.text.Phrase phrase = new com.itextpdf.text.Phrase();
        phrase.add(new com.itextpdf.text.Chunk(title + "\n", titleFont));
        phrase.add(new com.itextpdf.text.Chunk(value, valueFont));
        
        cell.setPhrase(phrase);
        table.addCell(cell);
    }
    
    // Helper method to add modern styled cells to marks table
    private void addModernPdfTableCell(com.itextpdf.text.pdf.PdfPTable table, String text, com.itextpdf.text.Font font, 
                                      com.itextpdf.text.BaseColor bgColor, boolean isHeader) {
        com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(text, font));
        cell.setPadding(8);
        cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        cell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(bgColor);
        
        if (isHeader) {
            cell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
        } else {
            cell.setBorderColor(new com.itextpdf.text.BaseColor(229, 231, 235));
            cell.setBorderWidth(0.5f);
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
        radio.setFont(new Font("SansSerif", Font.BOLD, 16));
        radio.setForeground(PRIMARY_COLOR); // Match title color
        radio.setBackground(Color.WHITE);
        radio.setOpaque(false);
        radio.setFocusPainted(false);
        radio.setCursor(new Cursor(Cursor.HAND_CURSOR));
        radio.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        
        // Enhanced radio button icons with purple colors
        radio.setIcon(new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setStroke(new BasicStroke(2.5f));
                g2.setColor(new Color(147, 51, 234, 120)); // Purple border
                g2.drawOval(x + 1, y + 1, 18, 18);
                g2.dispose();
            }
            @Override
            public int getIconWidth() { return 22; }
            @Override
            public int getIconHeight() { return 22; }
        });
        
        radio.setSelectedIcon(new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setStroke(new BasicStroke(2.5f));
                // Outer circle - purple border
                g2.setColor(new Color(147, 51, 234)); // Strong purple
                g2.drawOval(x + 1, y + 1, 18, 18);
                // Inner filled circle - purple
                g2.setColor(new Color(147, 51, 234)); // Strong purple
                g2.fillOval(x + 6, y + 6, 10, 10);
                g2.dispose();
            }
            @Override
            public int getIconWidth() { return 22; }
            @Override
            public int getIconHeight() { return 22; }
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
        
        // Create and show the analysis layout
        createStudentAnalysisLayout();
        
        // Initial analysis update
        updateAnalysisWithCurrentData();
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
        
        // Build columns: Subject | ExamType1 | ExamType2 | ... | Total | Grade
        List<String> columns = new ArrayList<>();
        columns.add("Subject");
        columns.addAll(allExamTypes);
        columns.add("Total");
        columns.add("Grade");
        
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
        int studentSectionId = getStudentSectionId(currentStudent.getId());
        
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
            
            // Calculate subject percentage using WEIGHTED FORMULA
            double subjectWeightedTotal = analyzerDAO.calculateWeightedSubjectTotal(
                currentStudent.getId(),
                studentSectionId,
                subject,
                selectedFilters.get(subject)
            );
            
            // Weighted total is already a percentage out of 100
            double subjectPercentage = subjectWeightedTotal >= 0 ? subjectWeightedTotal : 0.0;
            subjectPercentages.put(subject, subjectPercentage);
            
            // Add performance icon and subject name with HTML for color
            String performanceIcon = getPerformanceIcon(subjectPercentage);
            Color iconColor = getPerformanceColor(subjectPercentage);
            String colorHex = String.format("#%02x%02x%02x", iconColor.getRed(), iconColor.getGreen(), iconColor.getBlue());
            String cellText = String.format("<html><span style='color:%s; font-size:10px;'>●</span> <span style='color:#1f2937;'>%s</span></html>", colorHex, subject);
            rowData.add(0, cellText);
            
            // Add weighted total (out of 100) instead of raw sum
            rowData.add(String.format("%.0f", subjectPercentage));
            
            // Add letter grade
            String grade = getLetterGrade(subjectPercentage);
            rowData.add(grade);
            
            grandTotal += rowTotal;
            tableModel.addRow(rowData.toArray());
        }
        
        // Add column totals row (show raw sum for reference)
        List<Object> columnTotalsRow = new ArrayList<>();
        columnTotalsRow.add("<html><b>Raw Sum</b></html>");
        
        // Add empty cells for exam type columns
        for (int i = 0; i < allExamTypes.size(); i++) {
            columnTotalsRow.add("");
        }
        
        // Add grand total in the Total column (this is raw sum for reference)
        columnTotalsRow.add(grandTotal);
        
        // Empty cell for Grade column
        columnTotalsRow.add("");
        
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
                
                // Total column (second to last) - bold with light background
                if (column == table.getColumnCount() - 2) {
                    label.setFont(new Font("SansSerif", Font.BOLD, 14));
                    label.setBackground(new Color(243, 244, 246));
                }
                
                // Grade column (last) - bold with green background
                if (column == table.getColumnCount() - 1 && row != table.getRowCount() - 1) {
                    label.setFont(new Font("SansSerif", Font.BOLD, 14));
                    label.setBackground(new Color(220, 252, 231)); // Light green
                    label.setForeground(new Color(22, 163, 74)); // Dark green
                }
                
                // Bottom Total cell (Grand Total) - extra bold with accent color
                if (row == table.getRowCount() - 1 && column == table.getColumnCount() - 2) {
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
        
        // Update analysis panel with calculated totals - pass calculated metrics
        // Calculate metrics here to pass to updateAnalysisPanel
        AnalyzerDAO dao = new AnalyzerDAO();
        double totalWeightedScore = 0.0;
        int examCount = 0;
        
        for (Map.Entry<String, Set<String>> entry : selectedFilters.entrySet()) {
            double subjectWeightedTotal = dao.calculateWeightedSubjectTotal(
                currentStudent.getId(),
                studentSectionId,
                entry.getKey(),
                entry.getValue()
            );
            if (subjectWeightedTotal >= 0) {
                totalWeightedScore += subjectWeightedTotal;
            }
            examCount += entry.getValue().size();
        }
        
        double percentage = selectedFilters.size() > 0 ? totalWeightedScore / selectedFilters.size() : 0.0;
        double sgpa = percentage / 10.0;
        String letterGrade = getLetterGrade(percentage);
        int studentRank = calculateStudentRank(currentStudent.getId());
        
        updateAnalysisPanel(sgpa, percentage, letterGrade, selectedFilters.size(), examCount, studentRank);
        
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
        // Calculate CGPA as percentage / 10 (e.g., 94.14% = 9.41 CGPA)
        return percentage / 10.0;
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
    
    private void showSectionSelectionDialog() {
        // Create custom dialog
        JDialog dialog = new JDialog(parentFrame, "Section Selection", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout(10, 15));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        contentPanel.setBackground(Color.WHITE);
        
        // Label
        JLabel label = new JLabel("Select Section:");
        label.setFont(new Font("SansSerif", Font.BOLD, 16));
        label.setForeground(TEXT_PRIMARY);
        contentPanel.add(label, BorderLayout.NORTH);
        
        // Dropdown with scrollbar
        String[] sections = sectionStudents.keySet().toArray(new String[0]);
        JComboBox<String> sectionCombo = new JComboBox<>(sections);
        sectionCombo.setFont(new Font("SansSerif", Font.PLAIN, 16));
        sectionCombo.setPreferredSize(new Dimension(300, 45));
        // Show max 6 items at a time, scrollbar appears if more sections exist
        sectionCombo.setMaximumRowCount(6);
        sectionCombo.setBackground(Color.WHITE);
        sectionCombo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 2),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        
        // Make dropdown items larger and more visible
        sectionCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setFont(new Font("SansSerif", Font.PLAIN, 16));
                setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
                if (isSelected) {
                    setBackground(SELECTION_COLOR);
                    setForeground(PRIMARY_COLOR);
                } else {
                    setBackground(Color.WHITE);
                    setForeground(TEXT_PRIMARY);
                }
                return this;
            }
        });
        
        contentPanel.add(sectionCombo, BorderLayout.CENTER);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBackground(Color.WHITE);
        
        JButton okButton = new JButton("OK");
        okButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        okButton.setForeground(Color.WHITE);
        okButton.setBackground(PRIMARY_COLOR);
        okButton.setPreferredSize(new Dimension(100, 35));
        okButton.setBorderPainted(false);
        okButton.setFocusPainted(false);
        okButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        okButton.addActionListener(ev -> {
            selectedSection = (String) sectionCombo.getSelectedItem();
            dialog.dispose();
            
            if (selectedSection != null) {
                HashMap<String, ArrayList<Student>> sectionMap = new HashMap<>();
                sectionMap.put(selectedSection, new ArrayList<>(sectionStudents.get(selectedSection)));
                showSectionAnalyzer(sectionMap);
            }
        });
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("SansSerif", Font.PLAIN, 13));
        cancelButton.setForeground(TEXT_PRIMARY);
        cancelButton.setBackground(Color.WHITE);
        cancelButton.setPreferredSize(new Dimension(100, 35));
        cancelButton.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        cancelButton.setFocusPainted(false);
        cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelButton.addActionListener(ev -> {
            dialog.dispose();
            studentRadio.setSelected(true);
        });
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(contentPanel);
        dialog.setVisible(true);
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
        
        // Overall checkbox - initial state based on whether all are selected
        JCheckBox overallCheck = new JCheckBox("Overall (All)", isAllSelected());
        overallCheck.setFont(new Font("SansSerif", Font.BOLD, 14));
        overallCheck.setBackground(Color.WHITE);
        
        overallCheck.addActionListener(e -> {
            boolean selected = overallCheck.isSelected();
            if (selected) {
                // Select all subjects and exams
                initializeFilters();
            } else {
                // Deselect all
                selectedFilters.clear();
            }
            // Immediately recreate filter tree to update checkbox visual state
            createFilterTree();
            // Then update results in background
            SwingUtilities.invokeLater(() -> displayResults());
        });
        
        JPanel overallPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        overallPanel.setBackground(Color.WHITE);
        overallPanel.add(overallCheck);
        
        // Subject and exam type checkboxes
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));
        checkboxPanel.setBackground(Color.WHITE);
        
        for (String subject : currentStudent.getMarks().keySet()) {
            // Subject checkbox - check if this subject is fully selected
            boolean isSubjectFullySelected = selectedFilters.containsKey(subject) && 
                selectedFilters.get(subject).containsAll(currentStudent.getMarks().get(subject).keySet());
            JCheckBox subjectCheck = new JCheckBox("📁 " + subject, isSubjectFullySelected);
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
                boolean isExamSelected = selectedFilters.containsKey(subject) && selectedFilters.get(subject).contains(examType);
                JCheckBox examCheck = new JCheckBox("   └─ " + examType, isExamSelected);
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
    
    /**
     * Get section ID for a student
     */
    private int getStudentSectionId(int studentId) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT section_id FROM students WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int sectionId = rs.getInt("section_id");
                rs.close();
                ps.close();
                return sectionId;
            }
            
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    // ==================== New UI Methods ====================
    
    private JPanel createFilterPanel() {
        filterCard = createModernCard();
        filterCard.setLayout(new BorderLayout(10, 10));
        filterCard.setPreferredSize(new Dimension(280, 0));
        
        JLabel filterLabel = new JLabel("Filter Analysis");
        filterLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        filterLabel.setForeground(TEXT_PRIMARY);
        filterLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
        
        // Create tree structure with scroll pane
        JPanel treePanel = new JPanel();
        treePanel.setLayout(new BoxLayout(treePanel, BoxLayout.Y_AXIS));
        treePanel.setBackground(Color.WHITE);
        treePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        if (currentStudent != null && selectedFilters != null) {
            // Overall checkbox
            JCheckBox overallCheck = new JCheckBox("Overall (All)", true);
            overallCheck.setFont(new Font("SansSerif", Font.BOLD, 14));
            overallCheck.setBackground(Color.WHITE);
            overallCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
            overallCheck.addActionListener(e -> {
                if (overallCheck.isSelected()) {
                    // Select all subjects and their exams
                    for (String subject : currentStudent.getMarks().keySet()) {
                        Set<String> allExams = new HashSet<>(currentStudent.getMarks().get(subject).keySet());
                        selectedFilters.put(subject, allExams);
                    }
                    refreshFilterCheckboxes(treePanel, overallCheck);
                } else {
                    selectedFilters.clear();
                    refreshFilterCheckboxes(treePanel, overallCheck);
                }
                updateAnalysisWithCurrentData();
            });
            
            treePanel.add(overallCheck);
            treePanel.add(Box.createVerticalStrut(10));
            treePanel.add(new JSeparator());
            treePanel.add(Box.createVerticalStrut(10));
            
            // Subject and exam type checkboxes
            for (String subject : currentStudent.getMarks().keySet()) {
                // Subject checkbox
                JCheckBox subjectCheck = new JCheckBox("+ " + subject, selectedFilters.containsKey(subject));
                subjectCheck.setFont(new Font("SansSerif", Font.BOLD, 13));
                subjectCheck.setForeground(TEXT_PRIMARY);
                subjectCheck.setBackground(Color.WHITE);
                subjectCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
                
                java.util.List<JCheckBox> examChecks = new java.util.ArrayList<>();
                
                final String currentSubject = subject;
                subjectCheck.addActionListener(e -> {
                    boolean selected = subjectCheck.isSelected();
                    for (JCheckBox examCheck : examChecks) {
                        examCheck.setSelected(selected);
                    }
                    
                    if (selected) {
                        Set<String> allExams = new HashSet<>(currentStudent.getMarks().get(currentSubject).keySet());
                        selectedFilters.put(currentSubject, allExams);
                    } else {
                        selectedFilters.remove(currentSubject);
                    }
                    overallCheck.setSelected(isAllSelected());
                    updateAnalysisWithCurrentData();
                });
                
                treePanel.add(subjectCheck);
                
                // Exam type checkboxes (indented)
                for (String examType : currentStudent.getMarks().get(subject).keySet()) {
                    boolean examSelected = selectedFilters.containsKey(subject) && 
                                         selectedFilters.get(subject).contains(examType);
                    
                    JCheckBox examCheck = new JCheckBox("   - " + examType, examSelected);
                    examCheck.setFont(new Font("SansSerif", Font.PLAIN, 12));
                    examCheck.setBackground(Color.WHITE);
                    examCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
                    examChecks.add(examCheck);
                    
                    final String currentExam = examType;
                    examCheck.addActionListener(e -> {
                        if (examCheck.isSelected()) {
                            selectedFilters.putIfAbsent(currentSubject, new HashSet<>());
                            selectedFilters.get(currentSubject).add(currentExam);
                        } else {
                            if (selectedFilters.containsKey(currentSubject)) {
                                selectedFilters.get(currentSubject).remove(currentExam);
                                if (selectedFilters.get(currentSubject).isEmpty()) {
                                    selectedFilters.remove(currentSubject);
                                }
                            }
                        }
                        
                        // Update subject checkbox
                        boolean allSelected = examChecks.stream().allMatch(JCheckBox::isSelected);
                        subjectCheck.setSelected(allSelected);
                        overallCheck.setSelected(isAllSelected());
                        updateAnalysisWithCurrentData();
                    });
                    
                    treePanel.add(examCheck);
                }
                
                treePanel.add(Box.createVerticalStrut(5));
            }
        }
        
        JScrollPane scrollPane = new JScrollPane(treePanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        scrollPane.setBackground(Color.WHITE);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        filterCard.add(filterLabel, BorderLayout.NORTH);
        filterCard.add(scrollPane, BorderLayout.CENTER);
        
        return filterCard;
    }
    
    private void refreshFilterCheckboxes(JPanel treePanel, JCheckBox overallCheck) {
        // This method would refresh all checkboxes - simplified implementation
        // In a full implementation, you'd iterate through components and update their states
    }
    
    private JPanel createTabbedContent() {
        JPanel contentWrapper = new JPanel();
        contentWrapper.setLayout(new BoxLayout(contentWrapper, BoxLayout.Y_AXIS));
        contentWrapper.setOpaque(false);
        contentWrapper.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));
        
        // Create modern tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 12));
        tabbedPane.setBackground(BACKGROUND_COLOR);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        // Tab 1: Overview & Metrics
        JPanel overviewPanel = createOverviewTab();
        JScrollPane overviewScroll = new JScrollPane(overviewPanel);
        overviewScroll.setBorder(null);
        overviewScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        overviewScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        overviewScroll.getVerticalScrollBar().setUnitIncrement(16);
        tabbedPane.addTab("📊 Overview", overviewScroll);
        
        // Tab 2: Marks Table
        JPanel tablePanel = createMarksTable();
        JScrollPane tableScroll = new JScrollPane(tablePanel);
        tableScroll.setBorder(null);
        tableScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScroll.getVerticalScrollBar().setUnitIncrement(16);
        tableScroll.getHorizontalScrollBar().setUnitIncrement(16);
        tabbedPane.addTab("📋 Marks Table", tableScroll);
        
        // Tab 3: Performance Chart
        subjectPerformancePanel = createModernCard();
        subjectPerformancePanel.setLayout(new BorderLayout(0, 15));
        createModernBarChartFromFiltered(); // Initialize chart
        JScrollPane chartScroll = new JScrollPane(subjectPerformancePanel);
        chartScroll.setBorder(null);
        chartScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        chartScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        chartScroll.getVerticalScrollBar().setUnitIncrement(16);
        chartScroll.getHorizontalScrollBar().setUnitIncrement(16);
        tabbedPane.addTab("📈 Performance Chart", chartScroll);
        
        // Tab 4: Analysis Summary
        JPanel summaryPanel = createAnalysisSummary();
        JScrollPane summaryScroll = new JScrollPane(summaryPanel);
        summaryScroll.setBorder(null);
        summaryScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        summaryScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        summaryScroll.getVerticalScrollBar().setUnitIncrement(16);
        tabbedPane.addTab("📑 Summary", summaryScroll);
        
        contentWrapper.add(tabbedPane);
        
        // Store reference for updates
        this.putClientProperty("tabbedPane", tabbedPane);
        
        return contentWrapper;
    }
    
    private JPanel createOverviewTab() {
        analysisPanel = createModernCard();
        analysisPanel.setLayout(new BorderLayout());
        
        // This will be populated by updateAnalysisPanel method
        return analysisPanel;
    }
    
    private JPanel createMarksTable() {
        JPanel tablePanel = createModernCard();
        tablePanel.setLayout(new BorderLayout(10, 10));
        
        // Title
        JLabel tableTitle = new JLabel("Detailed Marks Table");
        tableTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        tableTitle.setForeground(TEXT_PRIMARY);
        tableTitle.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        // This will be populated by updateMarksTable method
        tablePanel.add(tableTitle, BorderLayout.NORTH);
        
        // Store reference
        this.putClientProperty("marksTablePanel", tablePanel);
        
        return tablePanel;
    }
    
    private JPanel createAnalysisSummary() {
        JPanel summaryPanel = createModernCard();
        summaryPanel.setLayout(new BorderLayout(10, 10));
        
        // Title
        JLabel summaryTitle = new JLabel("Performance Analysis Summary");
        summaryTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        summaryTitle.setForeground(TEXT_PRIMARY);
        summaryTitle.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        summaryPanel.add(summaryTitle, BorderLayout.NORTH);
        
        // This will be populated with subject-wise analysis
        this.putClientProperty("summaryPanel", summaryPanel);
        
        return summaryPanel;
    }
    
    private void updateAnalysisWithCurrentData() {
        if (currentStudent != null && selectedFilters != null) {
            // Calculate metrics using WEIGHTED formula
            AnalyzerDAO dao = new AnalyzerDAO();
            int studentSectionId = getStudentSectionId(currentStudent.getId());
            
            double totalWeightedScore = 0.0;
            int examCount = 0;
            
            for (Map.Entry<String, Set<String>> entry : selectedFilters.entrySet()) {
                double subjectWeightedTotal = dao.calculateWeightedSubjectTotal(
                    currentStudent.getId(),
                    studentSectionId,
                    entry.getKey(),
                    entry.getValue()
                );
                if (subjectWeightedTotal >= 0) {
                    totalWeightedScore += subjectWeightedTotal;
                }
                examCount += entry.getValue().size();
            }
            
            double percentage = selectedFilters.size() > 0 ? totalWeightedScore / selectedFilters.size() : 0.0;
            double sgpa = percentage / 10.0;
            String letterGrade = getLetterGrade(percentage);
            int studentRank = calculateStudentRank(currentStudent.getId());
            
            // Update all tabs
            updateAnalysisPanel(sgpa, percentage, letterGrade, selectedFilters.size(), examCount, studentRank);
            updateMarksTable();
            createModernBarChartFromFiltered();
            updateAnalysisSummary();
        }
    }
    
    private void updateMarksTable() {
        JPanel tablePanel = (JPanel) this.getClientProperty("marksTablePanel");
        if (tablePanel == null || currentStudent == null) return;
        
        // Remove existing table content
        Component[] components = tablePanel.getComponents();
        for (Component c : components) {
            if (c instanceof JScrollPane) {
                tablePanel.remove(c);
            }
        }
        
        // Create table data
        java.util.List<String> columnList = new java.util.ArrayList<>();
        columnList.add("Subject");
        
        // Get all unique exam types from selected filters
        Set<String> allExamTypes = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : selectedFilters.entrySet()) {
            allExamTypes.addAll(entry.getValue());
        }
        java.util.List<String> examTypeList = new java.util.ArrayList<>(allExamTypes);
        Collections.sort(examTypeList);
        columnList.addAll(examTypeList);
        columnList.add("Total");
        columnList.add("Grade");
        
        String[] columns = columnList.toArray(new String[0]);
        
        java.util.List<String[]> rows = new java.util.ArrayList<>();
        
        // Get AnalyzerDAO for weighted calculations
        AnalyzerDAO dao = new AnalyzerDAO();
        int studentSectionId = getStudentSectionId(currentStudent.getId());
        
        for (Map.Entry<String, Set<String>> entry : selectedFilters.entrySet()) {
            String subject = entry.getKey();
            Map<String, Integer> subjectMarks = currentStudent.getMarks().get(subject);
            
            String[] row = new String[columns.length];
            row[0] = subject;
            
            int subjectTotal = 0;
            int examCount = 0;
            
            for (int i = 1; i < columns.length - 2; i++) {
                String examType = columns[i];
                Integer mark = subjectMarks.get(examType);
                if (mark != null && entry.getValue().contains(examType)) {
                    row[i] = String.valueOf(mark);
                    subjectTotal += mark;
                    examCount++;
                } else {
                    row[i] = "-";
                }
            }
            
            // Calculate WEIGHTED subject total using DUAL PASSING method
            AnalyzerDAO.SubjectPassResult result = dao.calculateWeightedSubjectTotalWithPass(
                currentStudent.getId(),
                studentSectionId,
                subject,
                entry.getValue()
            );
            
            // Use absolute percentage (negative indicates failure)
            double subjectPercentage = Math.abs(result.percentage);
            row[columns.length - 2] = String.format("%.0f", subjectPercentage);
            
            // Calculate grade from weighted percentage
            row[columns.length - 1] = getLetterGrade(subjectPercentage);
            
            rows.add(row);
        }
        
        // Add total row at bottom
        String[] totalRow = new String[columns.length];
        totalRow[0] = "<html><b>TOTAL</b></html>";
        
        // Sum each exam column
        for (int col = 1; col < columns.length - 2; col++) {
            int columnSum = 0;
            int validCount = 0;
            for (String[] row : rows) {
                if (row[col] != null && !row[col].equals("-")) {
                    try {
                        columnSum += Integer.parseInt(row[col]);
                        validCount++;
                    } catch (NumberFormatException e) {
                        // Skip non-numeric values
                    }
                }
            }
            totalRow[col] = validCount > 0 ? String.valueOf(columnSum) : "-";
        }
        
        // Calculate grand total for Total column (sum of all subject totals)
        double grandTotal = 0.0;
        int subjectCount = 0;
        for (String[] row : rows) {
            try {
                double subjectTotal = Double.parseDouble(row[columns.length - 2]);
                grandTotal += subjectTotal;
                subjectCount++;
            } catch (NumberFormatException e) {
                // Skip
            }
        }
        totalRow[columns.length - 2] = String.format("<html><b>%.0f / %d</b></html>", grandTotal, subjectCount * 100);
        totalRow[columns.length - 1] = ""; // No grade for total row
        
        rows.add(totalRow);
        
        String[][] data = rows.toArray(new String[0][]);
        
        JTable table = new JTable(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
            
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component component = super.prepareRenderer(renderer, row, column);
                
                // Check if this is the total row (last row)
                boolean isTotalRow = (row == getRowCount() - 1);
                
                if (isTotalRow) {
                    // Total row styling
                    component.setBackground(new Color(229, 231, 235));
                    component.setFont(new Font("SansSerif", Font.BOLD, 15));
                    if (component instanceof JLabel) {
                        ((JLabel) component).setForeground(new Color(30, 41, 59));
                    }
                } else {
                    // Alternate row colors for normal rows
                    if (!isRowSelected(row)) {
                        if (row % 2 == 0) {
                            component.setBackground(Color.WHITE);
                        } else {
                            component.setBackground(new Color(248, 249, 250));
                        }
                    }
                }
                
                // Color coding for different columns (skip for total row)
                if (!isTotalRow && column == 0) { // Subject column
                    component.setBackground(new Color(59, 130, 246, 30));
                    component.setFont(new Font("SansSerif", Font.BOLD, 14));
                } else if (column == getColumnCount() - 2) { // Total column
                    component.setBackground(new Color(34, 197, 94, 30));
                    component.setFont(new Font("SansSerif", Font.BOLD, 14));
                } else if (column == getColumnCount() - 1) { // Grade column
                    component.setBackground(new Color(168, 85, 247, 30));
                    component.setFont(new Font("SansSerif", Font.BOLD, 14));
                    
                    // Grade-specific colors
                    String grade = (String) getValueAt(row, column);
                    if ("A".equals(grade) || "A+".equals(grade)) {
                        component.setBackground(new Color(34, 197, 94, 50));
                    } else if ("B".equals(grade) || "B+".equals(grade)) {
                        component.setBackground(new Color(59, 130, 246, 50));
                    } else if ("C".equals(grade) || "C+".equals(grade)) {
                        component.setBackground(new Color(245, 158, 11, 50));
                    } else if ("D".equals(grade) || "F".equals(grade)) {
                        component.setBackground(new Color(239, 68, 68, 50));
                    }
                } else { // Exam score columns
                    component.setFont(new Font("SansSerif", Font.PLAIN, 14));
                }
                
                return component;
            }
        };
        
        // Enhanced table styling
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 15));
        table.getTableHeader().setBackground(new Color(59, 130, 246));
        table.getTableHeader().setForeground(Color.WHITE);
        table.setRowHeight(30);
        table.setGridColor(new Color(229, 231, 235));
        table.setSelectionBackground(new Color(99, 102, 241, 100));
        table.setSelectionForeground(Color.BLACK);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        
        // Enhanced table with proper auto-resize and scrolling
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // Allow horizontal scrolling
        table.setFillsViewportHeight(true);
        
        // Set optimal column widths
        for (int i = 0; i < table.getColumnCount(); i++) {
            javax.swing.table.TableColumn column = table.getColumnModel().getColumn(i);
            if (i == 0) { // Subject column
                column.setPreferredWidth(150);
                column.setMinWidth(120);
            } else if (i == table.getColumnCount() - 2) { // Total column
                column.setPreferredWidth(80);
                column.setMinWidth(60);
            } else if (i == table.getColumnCount() - 1) { // Grade column
                column.setPreferredWidth(60);
                column.setMinWidth(50);
            } else { // Exam columns
                column.setPreferredWidth(100);
                column.setMinWidth(80);
            }
        }
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(59, 130, 246), 2),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setBackground(Color.WHITE);
        // Enable both horizontal and vertical scrolling
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        tablePanel.revalidate();
        tablePanel.repaint();
    }
    
    private void updateAnalysisSummary() {
        JPanel summaryPanel = (JPanel) this.getClientProperty("summaryPanel");
        if (summaryPanel == null || currentStudent == null) return;
        
        // Remove existing content
        Component[] components = summaryPanel.getComponents();
        for (Component c : components) {
            if (!(c instanceof JLabel)) { // Keep the title
                summaryPanel.remove(c);
            }
        }
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Subject-wise performance analysis using WEIGHTED CALCULATION
        AnalyzerDAO dao = new AnalyzerDAO();
        int studentSectionId = getStudentSectionId(currentStudent.getId());
        java.util.List<SubjectPerformance> subjectPerformances = new java.util.ArrayList<>();
        
        for (Map.Entry<String, Set<String>> entry : selectedFilters.entrySet()) {
            String subject = entry.getKey();
            
            // Calculate WEIGHTED subject total (out of 100)
            double subjectWeightedTotal = dao.calculateWeightedSubjectTotal(
                currentStudent.getId(),
                studentSectionId,
                subject,
                entry.getValue()
            );
            
            if (subjectWeightedTotal >= 0) {
                // Use weighted percentage
                double percentage = subjectWeightedTotal;
                subjectPerformances.add(new SubjectPerformance(subject, (int)percentage, percentage, percentage));
            }
        }
        
        // Sort by percentage (lowest first for improvement suggestions)
        subjectPerformances.sort((a, b) -> Double.compare(a.percentage, b.percentage));
        
        // Low scoring subjects
        JLabel lowScoreTitle = new JLabel("🔻 Areas for Improvement");
        lowScoreTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        lowScoreTitle.setForeground(DANGER_COLOR);
        lowScoreTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        contentPanel.add(lowScoreTitle);
        contentPanel.add(Box.createVerticalStrut(10));
        
        boolean hasLowScores = false;
        for (SubjectPerformance perf : subjectPerformances) {
            if (perf.percentage < 75) { // Below 75% needs improvement
                hasLowScores = true;
                JPanel subjectCard = createSubjectSummaryCard(perf, DANGER_COLOR);
                subjectCard.setAlignmentX(Component.LEFT_ALIGNMENT);
                contentPanel.add(subjectCard);
                contentPanel.add(Box.createVerticalStrut(8));
            }
        }
        
        if (!hasLowScores) {
            JLabel noIssues = new JLabel("✅ Great! All subjects performing well");
            noIssues.setFont(new Font("SansSerif", Font.ITALIC, 14));
            noIssues.setForeground(SUCCESS_COLOR);
            noIssues.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(noIssues);
        }
        
        contentPanel.add(Box.createVerticalStrut(20));
        
        // High scoring subjects
        JLabel highScoreTitle = new JLabel("🔺 Strengths");
        highScoreTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        highScoreTitle.setForeground(SUCCESS_COLOR);
        highScoreTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        contentPanel.add(highScoreTitle);
        contentPanel.add(Box.createVerticalStrut(10));
        
        // Reverse list for high scores
        for (int i = subjectPerformances.size() - 1; i >= 0; i--) {
            SubjectPerformance perf = subjectPerformances.get(i);
            if (perf.percentage >= 80) { // Above 80% is a strength
                JPanel subjectCard = createSubjectSummaryCard(perf, SUCCESS_COLOR);
                subjectCard.setAlignmentX(Component.LEFT_ALIGNMENT);
                contentPanel.add(subjectCard);
                contentPanel.add(Box.createVerticalStrut(8));
            }
        }
        
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        summaryPanel.add(scrollPane, BorderLayout.CENTER);
        summaryPanel.revalidate();
        summaryPanel.repaint();
    }
    
    private JPanel createSubjectSummaryCard(SubjectPerformance perf, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(10, 5));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 2),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        card.setMaximumSize(new Dimension(500, 60));
        
        JLabel subjectLabel = new JLabel(perf.subject);
        subjectLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        subjectLabel.setForeground(TEXT_PRIMARY);
        
        JLabel statsLabel = new JLabel(String.format("%.1f%% | Grade: %s | Total: %d", 
            perf.percentage, getLetterGrade(perf.percentage), perf.total));
        statsLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statsLabel.setForeground(TEXT_SECONDARY);
        
        card.add(subjectLabel, BorderLayout.NORTH);
        card.add(statsLabel, BorderLayout.CENTER);
        
        return card;
    }
    
    // Helper class for subject performance
    private static class SubjectPerformance {
        String subject;
        int total;
        double average;
        double percentage;
        
        SubjectPerformance(String subject, int total, double average, double percentage) {
            this.subject = subject;
            this.total = total;
            this.average = average;
            this.percentage = percentage;
        }
    }
    
    private void closePanel() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }
}
