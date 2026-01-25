package com.sms.resultlauncher;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.sms.calculation.models.Component;
import com.sms.database.DatabaseConnection;
import com.sms.calculation.models.CalculationResult;
import com.sms.calculation.StudentCalculator;
import com.sms.dao.AnalyzerDAO;
import com.sms.analyzer.Student;
import com.sms.login.LoginScreen;

public class ResultPreviewDialog extends JDialog {
    
    private int sectionId;
    private List<Integer> studentIds;
    private List<Component> components;
    private JTable previewTable;
    private DefaultTableModel tableModel;
    private AnalyzerDAO.DetailedRankingData storedRankingData; // Store for hierarchical table
    private List<String> columnList; // Shared column list
    private List<Integer> columnWidths; // Shared column widths
    
    // OPTIMIZATION: Cache for batch-loaded data
    private Map<Integer, Map<String, Map<String, Double>>> allStudentMarksCache; // studentId -> subject -> exam -> marks
    private Map<Integer, List<Component>> allStudentComponentsCache; // studentId -> components
    private String markingSystem; // Cache marking system
    
    public ResultPreviewDialog(Window parent, int sectionId, List<Integer> studentIds, List<Component> components) {
        super(parent, "Result Preview", ModalityType.APPLICATION_MODAL);
        this.sectionId = sectionId;
        this.studentIds = studentIds;
        this.components = components;
        
        // Load detailed ranking data for hierarchical headers
        try {
            AnalyzerDAO dao = new AnalyzerDAO();
            this.storedRankingData = dao.getDetailedStudentRanking(sectionId, null);
        } catch (Exception e) {
            System.err.println("Error loading ranking data: " + e.getMessage());
            e.printStackTrace();
        }
        
        initializeUI();
        setSize(800, 600);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        loadPreviewData();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(ResultLauncherUtils.BACKGROUND_COLOR);
        
        // Header
        JPanel headerPanel = ResultLauncherUtils.createModernCard();
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        JLabel titleLabel = new JLabel("👁️ Result Preview");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLabel.setForeground(ResultLauncherUtils.PRIMARY_COLOR);
        
        JLabel descLabel = new JLabel("Preview of results that will be launched for students");
        descLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        descLabel.setForeground(ResultLauncherUtils.TEXT_SECONDARY);
        
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        titlePanel.add(titleLabel);
        titlePanel.add(descLabel);
        
        headerPanel.add(titlePanel, BorderLayout.WEST);
        
        // Summary info
        JPanel summaryPanel = new JPanel();
        summaryPanel.setLayout(new BoxLayout(summaryPanel, BoxLayout.Y_AXIS));
        summaryPanel.setOpaque(false);
        
        JLabel studentsLabel = new JLabel("Students: " + studentIds.size());
        studentsLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        studentsLabel.setForeground(ResultLauncherUtils.TEXT_PRIMARY);
        
        JLabel componentsLabel = new JLabel("Components: " + components.size());
        componentsLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        componentsLabel.setForeground(ResultLauncherUtils.TEXT_PRIMARY);
        
        summaryPanel.add(studentsLabel);
        summaryPanel.add(componentsLabel);
        
        headerPanel.add(summaryPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);
        
        // Build column structure FIRST (shared by both header and table)
        buildColumnStructure();
        
        // Create hierarchical header panel (uses columnWidths)
        JPanel hierarchicalHeaderPanel = createHierarchicalHeader();
        
        // Table (uses columnWidths)
        createPreviewTable();
        // Hide default table header - we'll use custom hierarchical header
        previewTable.setTableHeader(null);
        
        // Create wrapper panel to hold both custom header and table (like Section Analyzer)
        JPanel tableWithHeaderPanel = new JPanel(new BorderLayout());
        tableWithHeaderPanel.setBackground(Color.WHITE);
        
        if (hierarchicalHeaderPanel != null) {
            // Force header panel to match table width
            int totalTableWidth = columnWidths.stream().mapToInt(Integer::intValue).sum();
            hierarchicalHeaderPanel.setPreferredSize(new Dimension(totalTableWidth, hierarchicalHeaderPanel.getPreferredSize().height));
            hierarchicalHeaderPanel.setMaximumSize(new Dimension(totalTableWidth, hierarchicalHeaderPanel.getPreferredSize().height));
            tableWithHeaderPanel.add(hierarchicalHeaderPanel, BorderLayout.NORTH);
        } else {
        }
        
        tableWithHeaderPanel.add(previewTable, BorderLayout.CENTER);
        
        // Wrap the entire table+header in scroll pane
        JScrollPane scrollPane = new JScrollPane(tableWithHeaderPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(ResultLauncherUtils.CARD_COLOR);
        JPanel tablePanel = ResultLauncherUtils.createModernCard();
        tablePanel.setLayout(new BorderLayout());
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        add(tablePanel, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setOpaque(false);
        
        JButton closeButton = ResultLauncherUtils.createSecondaryButton("Close");
        closeButton.addActionListener(e -> dispose());
        
        JButton refreshButton = ResultLauncherUtils.createModernButton("Refresh Preview");
        refreshButton.addActionListener(e -> loadPreviewData());
        
        buttonPanel.add(closeButton);
        buttonPanel.add(refreshButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void buildColumnStructure() {
        // Build column list and widths shared by both header and table
        columnList = new ArrayList<>();
        columnWidths = new ArrayList<>();
        
        if (storedRankingData == null || storedRankingData.subjects.isEmpty()) {
            return;
        }
        
        // Fixed columns
        columnList.add("Rank");
        columnList.add("Name");
        columnList.add("Roll");
        columnList.add("Section");
        columnWidths.add(50);
        columnWidths.add(180);
        columnWidths.add(80);
        columnWidths.add(70);
        
        // Add columns for each exam type under each subject
        for (AnalyzerDAO.SubjectInfoDetailed subject : storedRankingData.subjects) {
            for (String examType : subject.examTypes) {
                Integer maxMarks = subject.examTypeMaxMarks.get(examType);
                String header = examType + (maxMarks != null && maxMarks > 0 ? " (" + maxMarks + ")" : "");
                columnList.add(header);
                int width = Math.max(calculateTextWidth(header, 10) + 20, 85);
                columnWidths.add(width);
            }
            columnList.add(subject.subjectName + " Total");
            columnWidths.add(85);
        }
        
        // Overall metrics
        columnList.add("Total");
        columnList.add("%");
        columnList.add("Grade");
        columnList.add("CGPA");
        columnList.add("Status");
        columnWidths.add(85);
        columnWidths.add(60);
        columnWidths.add(60);
        columnWidths.add(60);
        columnWidths.add(80);
    }
    
    private void createPreviewTable() {
        // Use pre-loaded storedRankingData from constructor
        if (storedRankingData == null || storedRankingData.subjects.isEmpty() || columnList == null) {
            // Fallback to simple table
            String[] columns = {"Rank", "Name", "Roll", "Section", "Total", "%", "Grade", "CGPA", "Status"};
            tableModel = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            previewTable = new JTable(tableModel);
            previewTable.setRowHeight(30);
            return;
        }
        
        // Use pre-built columnList
        String[] columns = columnList.toArray(new String[0]);
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        previewTable = new JTable(tableModel);
        previewTable.setRowHeight(30);
        previewTable.setFont(new Font("SansSerif", Font.PLAIN, 11));
        
        // Style the default table header
        if (previewTable.getTableHeader() != null) {
            previewTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
            previewTable.getTableHeader().setBackground(ResultLauncherUtils.PRIMARY_COLOR);
            previewTable.getTableHeader().setForeground(Color.WHITE);
            previewTable.getTableHeader().setPreferredSize(new Dimension(0, 35));
        }
        
        previewTable.setShowGrid(true);
        previewTable.setGridColor(new Color(229, 231, 235));
        previewTable.setIntercellSpacing(new Dimension(1, 1));
        previewTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        previewTable.setSelectionBackground(new Color(99, 102, 241, 30));
        previewTable.setSelectionForeground(ResultLauncherUtils.TEXT_PRIMARY);
        previewTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // Enable horizontal scrolling
        
        // Set column widths EXACTLY to match header (use min/max/preferred for precise alignment)
        for (int i = 0; i < columnWidths.size(); i++) {
            int width = columnWidths.get(i);
            previewTable.getColumnModel().getColumn(i).setPreferredWidth(width);
            previewTable.getColumnModel().getColumn(i).setMinWidth(width);
            previewTable.getColumnModel().getColumn(i).setMaxWidth(width);
        }
        
        // Custom cell renderer
     // In ResultPreviewDialog.java, fix the renderer method:
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
                
                // Alternating row colors
                if (isSelected) {
                    label.setBackground(new Color(232, 240, 254));
                } else if (row % 2 == 0) {
                    label.setBackground(Color.WHITE);
                } else {
                    label.setBackground(new Color(248, 249, 250));
                }
                
                label.setForeground(ResultLauncherUtils.TEXT_PRIMARY);
                label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                
                int totalCols = table.getColumnCount();
                int statusCol = totalCols - 1;
                int cgpaCol = totalCols - 2;
                int gradeCol = totalCols - 3;
                int percentCol = totalCols - 4;
                int totalCol = totalCols - 5;
                
                if (column == statusCol) { // Status column
                    String status = value.toString();
                    label.setFont(new Font("SansSerif", Font.BOLD, 12));
                    if (status.contains("PASS")) {
                        label.setForeground(ResultLauncherUtils.SUCCESS_COLOR);
                        label.setText("✅ PASS");
                    } else {
                        label.setForeground(ResultLauncherUtils.DANGER_COLOR);
                        label.setText("❌ FAIL");
                    }
                    label.setHorizontalAlignment(JLabel.CENTER);
                } else if (column == gradeCol) { // Grade column
                    label.setFont(new Font("SansSerif", Font.BOLD, 13));
                    label.setHorizontalAlignment(JLabel.CENTER);
                } else if (column >= 4 && column < totalCol) { // Subject columns
                    label.setHorizontalAlignment(JLabel.CENTER);
                    label.setFont(new Font("SansSerif", Font.PLAIN, 11));
                } else if (column >= totalCol && column <= cgpaCol) { // Total, %, Grade, CGPA
                    label.setHorizontalAlignment(JLabel.CENTER);
                } else if (column == 0) { // Rank column
                    label.setFont(new Font("SansSerif", Font.BOLD, 12));
                    label.setHorizontalAlignment(JLabel.CENTER);
                } else {
                    label.setHorizontalAlignment(JLabel.LEFT);
                }
                
                return label;
            }
        };
        
        for (int i = 0; i < previewTable.getColumnCount(); i++) {
            previewTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
        
        // Set column widths dynamically
        int colCount = previewTable.getColumnCount();
        previewTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // Rank
        previewTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Name
        previewTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Roll
        previewTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // Section
        
        // Subject columns (4 to colCount-5)
        for (int i = 4; i < colCount - 5; i++) {
            previewTable.getColumnModel().getColumn(i).setPreferredWidth(70);
        }
        
        // Last 5 columns: Total, %, Grade, CGPA, Status
        previewTable.getColumnModel().getColumn(colCount - 5).setPreferredWidth(100); // Total
        previewTable.getColumnModel().getColumn(colCount - 4).setPreferredWidth(80);  // Percentage
        previewTable.getColumnModel().getColumn(colCount - 3).setPreferredWidth(50);  // Grade
        previewTable.getColumnModel().getColumn(colCount - 2).setPreferredWidth(50);  // CGPA
        previewTable.getColumnModel().getColumn(colCount - 1).setPreferredWidth(80);  // Status
    }
    
    private void loadPreviewData() {
        // Show loading message
        tableModel.setRowCount(0);
        tableModel.addRow(new Object[]{"Loading...", "", "", "", "", "", ""});
        
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                calculatePreviewResults();
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                } catch (Exception e) {
                    System.err.println("Error loading preview data: " + e.getMessage());
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(ResultPreviewDialog.this,
                        "Error loading preview data: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private void calculatePreviewResults() {
        try {
            tableModel.setRowCount(0);
            
            // OPTIMIZATION: Batch load ALL data upfront
            batchLoadAllStudentMarks();
            batchLoadMarkingSystem();
            
            AnalyzerDAO dao = new AnalyzerDAO();
            StudentCalculator calculator = new StudentCalculator(40.0);
            
            // Get students by section
            List<Student> allStudents = dao.getStudentsBySection(sectionId, LoginScreen.currentUserId);
            
            // Filter to selected students
            List<Student> selectedStudents = allStudents.stream()
                .filter(student -> studentIds.contains(student.getId()))
                .toList();
            
            // Check if we have detailed ranking data with exam types
            if (storedRankingData != null && !storedRankingData.subjects.isEmpty()) {
                calculateDetailedPreview(selectedStudents, dao, calculator);
            } else {
                calculateSimplePreview(selectedStudents, dao, calculator);
            }
            
        } catch (Exception e) {
            System.err.println("Error in preview calculation: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void calculateDetailedPreview(List<Student> selectedStudents, AnalyzerDAO dao, StudentCalculator calculator) {
        try {
            // Store results for ranking
            List<StudentResultDetailed> results = new ArrayList<>();
            
            for (Student student : selectedStudents) {
                try {
                    
                    // Get marks for each exam type under each subject
                    java.util.Map<String, java.util.Map<String, Double>> subjectExamMarks = new java.util.LinkedHashMap<>();
                    java.util.Map<String, Double> subjectTotals = new java.util.LinkedHashMap<>();
                    java.util.Map<String, Boolean> subjectPassStatus = new java.util.LinkedHashMap<>();
                    boolean allSubjectsPassed = true;
                    
                    for (AnalyzerDAO.SubjectInfoDetailed subject : storedRankingData.subjects) {
                        java.util.Map<String, Double> examMarks = new java.util.LinkedHashMap<>();
                        
                        // Get marks for each exam type
                        for (String examType : subject.examTypes) {
                            Double marks = getStudentExamMarks(student.getId(), subject.subjectName, examType);
                            examMarks.put(examType, marks);
                        }
                        
                        subjectExamMarks.put(subject.subjectName, examMarks);
                        
                        // Calculate weighted total for subject WITH DUAL PASSING CHECK
                        AnalyzerDAO.SubjectPassResult subResult = dao.calculateWeightedSubjectTotalWithPass(
                            student.getId(), sectionId, subject.subjectName, null);
                        subjectTotals.put(subject.subjectName, subResult.percentage);
                        subjectPassStatus.put(subject.subjectName, subResult.passed);
                        
                        // Student fails overall if ANY subject fails
                        if (!subResult.passed) {
                            allSubjectsPassed = false;
                        }
                    }
                    
                    // Load student component marks for overall calculation
                    List<Component> studentComponents = loadStudentComponentMarks(student.getId());
                    
                    // Calculate results
                    CalculationResult result = calculator.calculateStudentMarks(
                        student.getId(), student.getName(), studentComponents);
                    
                    // Calculate CGPA based on weighted percentage (matching SectionAnalyzer logic)
                    double cgpa = 0.0;
                    if (allSubjectsPassed) {
                        // Student passed all subjects - CGPA = percentage / 10.0
                        cgpa = result.getFinalPercentage() / 10.0;
                    } else {
                        // Student failed at least one subject - CGPA = 0.0
                        cgpa = 0.0;
                    }
                    
                    // Store result for ranking
                    StudentResultDetailed sr = new StudentResultDetailed();
                    sr.student = student;
                    sr.result = result;
                    sr.subjectExamMarks = subjectExamMarks;
                    sr.subjectTotals = subjectTotals;
                    sr.subjectPassStatus = subjectPassStatus;
                    sr.overallPassed = allSubjectsPassed;
                    sr.cgpa = cgpa;
                    results.add(sr);
                    
                } catch (Exception e) {
                    System.err.println("Error calculating for student " + student.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // Sort by percentage descending for ranking
            results.sort((a, b) -> Double.compare(b.result.getFinalPercentage(), a.result.getFinalPercentage()));
            
            // Assign ranks
            int rank = 1;
            for (StudentResultDetailed sr : results) {
                sr.rank = rank++;
            }
            
            // Add to table with detailed subject breakdown
            for (StudentResultDetailed sr : results) {
                SwingUtilities.invokeLater(() -> {
                    List<Object> rowData = new ArrayList<>();
                    rowData.add(String.valueOf(sr.rank));
                    rowData.add(sr.student.getName());
                    rowData.add(sr.student.getRollNumber());
                    rowData.add(sr.student.getSection());
                    
                    // Add exam marks for each subject
                    for (AnalyzerDAO.SubjectInfoDetailed subject : storedRankingData.subjects) {
                        java.util.Map<String, Double> examMarks = sr.subjectExamMarks.get(subject.subjectName);
                        
                        // Add individual exam type marks
                        for (String examType : subject.examTypes) {
                            Double marks = examMarks != null ? examMarks.get(examType) : null;
                            rowData.add(marks != null && marks >= 0 ? String.format("%.0f", marks) : "-");
                        }
                        
                        // Add subject total
                        Double subjectTotal = sr.subjectTotals.get(subject.subjectName);
                        rowData.add(subjectTotal != null && subjectTotal >= 0 ? String.format("%.2f", subjectTotal) : "-");
                    }
                    
                    // Add overall columns
                    rowData.add(String.format("%.0f/%.0f", sr.result.getTotalObtained(), sr.result.getTotalPossible()));
                    rowData.add(String.format("%.2f%%", sr.result.getFinalPercentage()));
                    rowData.add(sr.result.getGrade());
                    rowData.add(String.format("%.2f", sr.cgpa)); // Use calculated CGPA (percentage/10.0)
                    // Use dual passing requirement: ALL subjects must pass
                    rowData.add(sr.overallPassed ? "PASS" : "FAIL");
                    
                    tableModel.addRow(rowData.toArray());
                });
            }
        } catch (Exception e) {
            System.err.println("Error in detailed preview: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void calculateSimplePreview(List<Student> selectedStudents, AnalyzerDAO dao, StudentCalculator calculator) {
        try {
            // Store results for ranking
            List<StudentResult> results = new ArrayList<>();
            List<String> subjectNames = getSubjectsForSection();
            
            for (Student student : selectedStudents) {
                try {
                    
                    // Calculate subject-wise percentages WITH DUAL PASSING CHECK
                    java.util.Map<String, Double> subjectPercentages = new java.util.LinkedHashMap<>();
                    boolean allSubjectsPassed = true;
                    
                    for (String subjectName : subjectNames) {
                        AnalyzerDAO.SubjectPassResult subResult = dao.calculateWeightedSubjectTotalWithPass(
                            student.getId(), sectionId, subjectName, null);
                        subjectPercentages.put(subjectName, subResult.percentage);
                        
                        // Student fails overall if ANY subject fails
                        if (!subResult.passed) {
                            allSubjectsPassed = false;
                        }
                    }
                    
                    // Load student component marks for overall calculation
                    List<Component> studentComponents = loadStudentComponentMarks(student.getId());
                    
                    // Calculate results
                    CalculationResult result = calculator.calculateStudentMarks(
                        student.getId(), student.getName(), studentComponents);
                    
                    // Calculate CGPA based on weighted percentage (matching SectionAnalyzer logic)
                    double cgpa = 0.0;
                    if (allSubjectsPassed) {
                        // Student passed all subjects - CGPA = percentage / 10.0
                        cgpa = result.getFinalPercentage() / 10.0;
                    } else {
                        // Student failed at least one subject - CGPA = 0.0
                        cgpa = 0.0;
                    }
                    
                    // Store result for ranking
                    StudentResult sr = new StudentResult();
                    sr.student = student;
                    sr.result = result;
                    sr.subjectPercentages = subjectPercentages;
                    sr.overallPassed = allSubjectsPassed;
                    sr.cgpa = cgpa;
                    results.add(sr);
                    
                } catch (Exception e) {
                    System.err.println("Error calculating for student " + student.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // Sort by percentage descending for ranking
            results.sort((a, b) -> Double.compare(b.result.getFinalPercentage(), a.result.getFinalPercentage()));
            
            // Assign ranks
            int rank = 1;
            for (StudentResult sr : results) {
                sr.rank = rank++;
            }
            
            // Add to table
            for (StudentResult sr : results) {
                SwingUtilities.invokeLater(() -> {
                    Object[] rowData = {
                        String.valueOf(sr.rank),
                        sr.student.getName(),
                        sr.student.getRollNumber(),
                        sr.student.getSection(),
                        String.format("%.0f/%.0f", sr.result.getTotalObtained(), sr.result.getTotalPossible()),
                        String.format("%.2f%%", sr.result.getFinalPercentage()),
                        sr.result.getGrade(),
                        String.format("%.2f", sr.cgpa), // Use calculated CGPA (percentage/10.0)
                        sr.overallPassed ? "PASS" : "FAIL" // Use dual passing requirement
                    };
                    tableModel.addRow(rowData);
                });
            }
            
        } catch (Exception e) {
            System.err.println("Error in calculatePreviewResults: " + e.getMessage());
            e.printStackTrace();
            
            SwingUtilities.invokeLater(() -> {
                tableModel.setRowCount(0);
                tableModel.addRow(new Object[]{"Error loading data", "", "", "", "", "", "", "", ""});
            });
        }
    }
    
    // Helper class to store student results for ranking
    private static class StudentResult {
        Student student;
        CalculationResult result;
        int rank;
        java.util.Map<String, Double> subjectPercentages;
        boolean overallPassed; // True only if ALL subjects passed
        double cgpa; // Calculated as percentage/10.0 (matching SectionAnalyzer)
    }
    
    // Helper class for detailed results with exam-level breakdown
    private static class StudentResultDetailed {
        Student student;
        CalculationResult result;
        int rank;
        java.util.Map<String, java.util.Map<String, Double>> subjectExamMarks; // subject -> exam -> marks
        java.util.Map<String, Double> subjectTotals; // subject -> weighted percentage
        java.util.Map<String, Boolean> subjectPassStatus; // subject -> pass/fail
        boolean overallPassed; // True only if ALL subjects passed
        double cgpa; // Calculated as percentage/10.0 (matching SectionAnalyzer)
    }
    
    /**
     * OPTIMIZED: Batch load ALL student marks in ONE query instead of N queries per student.
     * Called once before processing students to eliminate N+1 performance problem.
     */
    private void batchLoadAllStudentMarks() {
        allStudentMarksCache = new HashMap<>();
        
        if (studentIds.isEmpty()) return;
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            
            // Build IN clause for batch query
            StringBuilder idList = new StringBuilder();
            for (int i = 0; i < studentIds.size(); i++) {
                if (i > 0) idList.append(",");
                idList.append(studentIds.get(i));
            }
            
            // SINGLE query fetches ALL marks for ALL students
            String query = "SELECT sm.student_id, sub.subject_name, et.exam_name, sm.marks_obtained " +
                          "FROM entered_exam_marks sm " +
                          "JOIN subjects sub ON sm.subject_id = sub.id " +
                          "JOIN exam_types et ON sm.exam_type_id = et.id " +
                          "WHERE sm.student_id IN (" + idList + ") " +
                          "AND sub.id IN (SELECT subject_id FROM section_subjects WHERE section_id = ?)";
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, sectionId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                int studentId = rs.getInt("student_id");
                String subjectName = rs.getString("subject_name");
                String examName = rs.getString("exam_name");
                double marks = rs.getDouble("marks_obtained");
                
                allStudentMarksCache.putIfAbsent(studentId, new HashMap<>());
                allStudentMarksCache.get(studentId).putIfAbsent(subjectName, new HashMap<>());
                allStudentMarksCache.get(studentId).get(subjectName).put(examName, marks);
            }
            rs.close();
            ps.close();
            
        } catch (Exception e) {
            System.err.println("Error batch loading marks: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
    }
    
    /**
     * Fast O(1) lookup from cache instead of database query.
     */
    private Double getStudentExamMarks(int studentId, String subjectName, String examTypeName) {
        if (allStudentMarksCache == null) return null;
        
        Map<String, Map<String, Double>> studentMarks = allStudentMarksCache.get(studentId);
        if (studentMarks == null) return null;
        
        Map<String, Double> subjectMarks = studentMarks.get(subjectName);
        if (subjectMarks == null) return null;
        
        return subjectMarks.get(examTypeName);
    }
    
    /**
     * OPTIMIZED: Load marking system once at start, not per student.
     */
    private void batchLoadMarkingSystem() {
        markingSystem = "old"; // default
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT marking_system FROM sections WHERE id = ?");
            ps.setInt(1, sectionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                markingSystem = rs.getString("marking_system");
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            System.err.println("Error loading marking system: " + e.getMessage());
        } finally {
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
    }
    
    private List<Component> loadStudentComponentMarks(int studentId) {
        try {
            AnalyzerDAO dao = new AnalyzerDAO();
            
            List<Component> studentComponents = new java.util.ArrayList<>();
            
            if ("flexible".equals(markingSystem)) {
                // New system - load from student_component_marks
                List<Integer> componentIds = components.stream()
                    .map(Component::getId)
                    .toList();
                
                java.util.Map<Integer, AnalyzerDAO.StudentComponentMark> studentMarks = 
                    dao.getStudentComponentMarks(studentId, componentIds);
                
                for (Component comp : components) {
                    AnalyzerDAO.StudentComponentMark mark = studentMarks.get(comp.getId());
                    
                    double obtainedMarks = 0;
                    if (mark != null && mark.marksObtained > 0) {
                        obtainedMarks = mark.marksObtained;
                    }
                    
                    Component studentComp = new Component(
                        comp.getId(),
                        comp.getName(),
                        comp.getType(),
                        obtainedMarks,
                        mark != null ? mark.maxMarks : comp.getMaxMarks(),
                        mark != null ? mark.scaledToMarks : comp.getMaxMarks()
                    );
                    
                    studentComp.setCounted(true);
                    studentComp.setGroupName(comp.getGroupName());
                    studentComp.setSequenceOrder(comp.getSequenceOrder());
                    studentComponents.add(studentComp);
                }
            } else {
                // Old system - Use AnalyzerDAO's weighted calculation method PER SUBJECT
                // This matches SectionAnalyzer's calculation logic
                java.sql.Connection conn = null;
                try {
                    conn = com.sms.database.DatabaseConnection.getConnection();
                    
                    // Get all subjects for this section
                    String subjectQuery = "SELECT DISTINCT sub.id, sub.subject_name FROM section_subjects ss " +
                                         "JOIN subjects sub ON ss.subject_id = sub.id " +
                                         "WHERE ss.section_id = ?";
                    java.sql.PreparedStatement ps = conn.prepareStatement(subjectQuery);
                    ps.setInt(1, sectionId);
                    java.sql.ResultSet rs = ps.executeQuery();
                    
                    int subjectCount = 0;
                    double totalObtained = 0.0;
                    
                    while (rs.next()) {
                        int subjectId = rs.getInt("id");
                        String subjectName = rs.getString("subject_name");
                        
                        // Use AnalyzerDAO method to calculate weighted percentage for this subject
                        AnalyzerDAO.SubjectPassResult result = dao.calculateWeightedSubjectTotalWithPass(
                            studentId, sectionId, subjectName, null); // null = include all exam types
                        
                        double subjectPercentage = result.percentage; // This is 0-100 per subject
                        totalObtained += subjectPercentage;
                        subjectCount++;
                    }
                    rs.close();
                    ps.close();
                    
                    // Create a single "pseudo-component" representing the total
                    // Total obtained = sum of all subject percentages
                    // Total possible = number of subjects × 100
                    Component totalComp = new Component(
                        0,
                        "Overall Total",
                        "exam",
                        totalObtained,
                        subjectCount * 100.0,
                        subjectCount * 100.0
                    );
                    totalComp.setCounted(true);
                    studentComponents.add(totalComp);
                    
                } catch (Exception e) {
                    System.err.println("Error loading marks from old system: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    try { if (conn != null) conn.close(); } catch (Exception e) {}
                }
            }
            
            return studentComponents;
            
        } catch (Exception e) {
            System.err.println("Error loading student component marks: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    private Component findMatchingComponent(List<Component> components, int componentId) {
        return components.stream()
            .filter(comp -> comp.getId() == componentId)
            .findFirst()
            .orElse(null);
    }
    
    private List<String> getSubjectsForSection() {
        List<String> subjects = new ArrayList<>();
        java.sql.Connection conn = null;
        try {
            conn = com.sms.database.DatabaseConnection.getConnection();
            String query = "SELECT DISTINCT sub.subject_name FROM section_subjects ss " +
                          "JOIN subjects sub ON ss.subject_id = sub.id " +
                          "WHERE ss.section_id = ? " +
                          "ORDER BY sub.subject_name";
            java.sql.PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, sectionId);
            java.sql.ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                subjects.add(rs.getString("subject_name"));
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            System.err.println("Error loading subjects: " + e.getMessage());
        } finally {
            try { if (conn != null) conn.close(); } catch (Exception e) {}
        }
        return subjects;
    }
    
    private void storeRankingData(AnalyzerDAO.DetailedRankingData data) {
        this.storedRankingData = data;
    }
    
    private JPanel createHierarchicalHeader() {
        if (storedRankingData == null || storedRankingData.subjects.isEmpty()) {
            return null;
        }
        
        // Use BoxLayout.Y_AXIS for hierarchical container (like Section Analyzer)
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(ResultLauncherUtils.PRIMARY_COLOR);
        container.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, ResultLauncherUtils.PRIMARY_COLOR));
        
        // ROW 1: Subject Names
        JPanel subjectRow = new JPanel();
        subjectRow.setLayout(new BoxLayout(subjectRow, BoxLayout.X_AXIS));
        subjectRow.setBackground(ResultLauncherUtils.PRIMARY_COLOR);
        subjectRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        
        // Student Info = Rank + Name + Roll + Section
        int studentInfoWidth = columnWidths.get(0) + columnWidths.get(1) + columnWidths.get(2) + columnWidths.get(3);
        addHeaderCell(subjectRow, "Student Info", studentInfoWidth, ResultLauncherUtils.PRIMARY_COLOR, Color.WHITE, Font.BOLD, 12);
        
        // Calculate subject widths from actual column widths
        int widthIndex = 4; // Start after fixed columns
        for (AnalyzerDAO.SubjectInfoDetailed subject : storedRankingData.subjects) {
            int examCount = subject.examTypes.size();
            int totalCols = examCount + 1; // exam types + total
            String header = subject.subjectName + " (" + subject.maxMarks + ")";
            // Sum up widths for this subject's columns
            int subjectWidth = 0;
            for (int i = 0; i < totalCols; i++) {
                subjectWidth += columnWidths.get(widthIndex++);
            }
            addHeaderCell(subjectRow, header, subjectWidth, ResultLauncherUtils.PRIMARY_COLOR, Color.WHITE, Font.BOLD, 12);
        }
        
        // Overall Metrics = Total + % + Grade + CGPA + Status (last 5 columns)
        int overallWidth = 0;
        for (int i = columnWidths.size() - 5; i < columnWidths.size(); i++) {
            overallWidth += columnWidths.get(i);
        }
        addHeaderCell(subjectRow, "Overall Metrics", overallWidth, ResultLauncherUtils.PRIMARY_COLOR, Color.WHITE, Font.BOLD, 12);
        
        // ROW 2: Exam Types - use columnWidths directly
        JPanel examTypeRow = new JPanel();
        examTypeRow.setLayout(new BoxLayout(examTypeRow, BoxLayout.X_AXIS));
        examTypeRow.setBackground(ResultLauncherUtils.PRIMARY_COLOR);
        examTypeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        
        // Fixed columns with exact widths from columnWidths
        addHeaderCell(examTypeRow, "Rank", columnWidths.get(0), ResultLauncherUtils.PRIMARY_COLOR, Color.WHITE, Font.BOLD, 10);
        addHeaderCell(examTypeRow, "Name", columnWidths.get(1), ResultLauncherUtils.PRIMARY_COLOR, Color.WHITE, Font.BOLD, 10);
        addHeaderCell(examTypeRow, "Roll", columnWidths.get(2), ResultLauncherUtils.PRIMARY_COLOR, Color.WHITE, Font.BOLD, 10);
        addHeaderCell(examTypeRow, "Section", columnWidths.get(3), ResultLauncherUtils.PRIMARY_COLOR, Color.WHITE, Font.BOLD, 10);
        
        // Exam types and totals using pre-calculated widths
        int colIdx = 4; // Start after fixed columns
        for (AnalyzerDAO.SubjectInfoDetailed subject : storedRankingData.subjects) {
            for (String examType : subject.examTypes) {
                Integer maxMarks = subject.examTypeMaxMarks.get(examType);
                String header = examType + (maxMarks != null && maxMarks > 0 ? " (" + maxMarks + ")" : "");
                addHeaderCell(examTypeRow, header, columnWidths.get(colIdx++), ResultLauncherUtils.PRIMARY_COLOR, Color.WHITE, Font.PLAIN, 10);
            }
            addHeaderCell(examTypeRow, "Total", columnWidths.get(colIdx++), ResultLauncherUtils.PRIMARY_COLOR, Color.WHITE, Font.BOLD, 10);
        }
        
        // Overall metrics - use columnWidths for all columns
        addHeaderCell(examTypeRow, "Total", columnWidths.get(colIdx++), ResultLauncherUtils.PRIMARY_COLOR, Color.WHITE, Font.BOLD, 10);
        addHeaderCell(examTypeRow, "%", columnWidths.get(colIdx++), ResultLauncherUtils.PRIMARY_COLOR, Color.WHITE, Font.BOLD, 10);
        addHeaderCell(examTypeRow, "Grade", columnWidths.get(colIdx++), ResultLauncherUtils.PRIMARY_COLOR, Color.WHITE, Font.BOLD, 10);
        addHeaderCell(examTypeRow, "CGPA", columnWidths.get(colIdx++), ResultLauncherUtils.PRIMARY_COLOR, Color.WHITE, Font.BOLD, 10);
        addHeaderCell(examTypeRow, "Status", columnWidths.get(colIdx++), ResultLauncherUtils.PRIMARY_COLOR, Color.WHITE, Font.BOLD, 10);
        
        // Add both rows to container
        container.add(subjectRow);
        container.add(examTypeRow);
        
        return container;
    }
    
    private void addHeaderCell(JPanel row, String text, int width, Color bg, Color fg, int fontStyle, int fontSize) {
        JLabel cell = new JLabel(text, SwingConstants.CENTER);
        cell.setFont(new Font("SansSerif", fontStyle, fontSize));
        cell.setForeground(fg);
        cell.setBackground(bg);
        cell.setOpaque(true);
        cell.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 1, 1, 1, new Color(255, 255, 255, 50)),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        cell.setPreferredSize(new Dimension(width, 30));
        cell.setMinimumSize(new Dimension(width, 30));
        cell.setMaximumSize(new Dimension(width, 30));
        row.add(cell);
    }
    
    private int calculateTextWidth(String text, int fontSize) {
        JLabel dummyLabel = new JLabel(text);
        dummyLabel.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
        FontMetrics metrics = dummyLabel.getFontMetrics(dummyLabel.getFont());
        return metrics.stringWidth(text);
    }
    
    // Helper class to hold marks information with weightage
    private static class MarksInfo {
        double obtained;
        double max;
        double weightage;
        
        MarksInfo(double obtained, double max, double weightage) {
            this.obtained = obtained;
            this.max = max;
            this.weightage = weightage;
        }
    }
}