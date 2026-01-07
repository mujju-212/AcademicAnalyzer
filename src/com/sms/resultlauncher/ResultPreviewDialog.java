package com.sms.resultlauncher;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;

import com.sms.calculation.models.Component;
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
    
    public ResultPreviewDialog(Window parent, int sectionId, List<Integer> studentIds, List<Component> components) {
        super(parent, "Result Preview", ModalityType.APPLICATION_MODAL);
        this.sectionId = sectionId;
        this.studentIds = studentIds;
        this.components = components;
        
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
        
        // Table
        createPreviewTable();
        JScrollPane scrollPane = new JScrollPane(previewTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(ResultLauncherUtils.CARD_COLOR);
        
        JPanel tablePanel = ResultLauncherUtils.createModernCard();
        tablePanel.setLayout(new BorderLayout());
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setOpaque(false);
        
        JButton closeButton = ResultLauncherUtils.createSecondaryButton("Close");
        closeButton.addActionListener(e -> dispose());
        
        JButton refreshButton = ResultLauncherUtils.createModernButton("Refresh Preview");
        refreshButton.addActionListener(e -> loadPreviewData());
        
        buttonPanel.add(closeButton);
        buttonPanel.add(refreshButton);
        
        // Layout
        add(headerPanel, BorderLayout.NORTH);
        add(tablePanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void createPreviewTable() {
        String[] columns = {"Student Name", "Roll No", "Section", "Total Marks", "Percentage", "Grade", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        previewTable = new JTable(tableModel);
        previewTable.setRowHeight(35);
        previewTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        previewTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        previewTable.getTableHeader().setBackground(new Color(249, 250, 251));
        previewTable.getTableHeader().setReorderingAllowed(false);
        previewTable.setShowGrid(false);
        previewTable.setIntercellSpacing(new Dimension(0, 1));
        previewTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Custom cell renderer
     // In ResultPreviewDialog.java, fix the renderer method:
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value,  // Use full package name
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
                
                label.setBackground(ResultLauncherUtils.CARD_COLOR);
                label.setForeground(ResultLauncherUtils.TEXT_PRIMARY);
                
                if (column == 6) { // Status column
                    String status = value.toString();
                    if ("PASS".equals(status)) {
                        label.setForeground(ResultLauncherUtils.SUCCESS_COLOR);
                        label.setText("✅ PASS");
                    } else {
                        label.setForeground(ResultLauncherUtils.DANGER_COLOR);
                        label.setText("❌ FAIL");
                    }
                    label.setHorizontalAlignment(JLabel.CENTER);
                } else if (column >= 3 && column <= 5) { // Numbers and grade
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
        
        // Set column widths
        previewTable.getColumnModel().getColumn(0).setPreferredWidth(150); // Name
        previewTable.getColumnModel().getColumn(1).setPreferredWidth(80);  // Roll
        previewTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // Section
        previewTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Total
        previewTable.getColumnModel().getColumn(4).setPreferredWidth(80);  // Percentage
        previewTable.getColumnModel().getColumn(5).setPreferredWidth(60);  // Grade
        previewTable.getColumnModel().getColumn(6).setPreferredWidth(80);  // Status
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
            
            AnalyzerDAO dao = new AnalyzerDAO();
            StudentCalculator calculator = new StudentCalculator(40.0);
            
            // Get students by section
            List<Student> allStudents = dao.getStudentsBySection(sectionId, LoginScreen.currentUserId);
            
            // Filter to selected students
            List<Student> selectedStudents = allStudents.stream()
                .filter(student -> studentIds.contains(student.getId()))
                .toList();
            
            for (Student student : selectedStudents) {
                try {
                    // Load student component marks
                    List<Component> studentComponents = loadStudentComponentMarks(student.getId());
                    
                    // Filter to selected components and set marks
                    List<Component> calculationComponents = new java.util.ArrayList<>();
                    for (Component selectedComp : components) {
                        Component studentComp = findMatchingComponent(studentComponents, selectedComp.getId());
                        if (studentComp != null) {
                            calculationComponents.add(studentComp);
                        } else {
                            // Component not found for this student, add with 0 marks
                            Component zeroComp = new Component(
                                selectedComp.getId(),
                                selectedComp.getName(),
                                selectedComp.getType(),
                                0, // obtained marks
                                selectedComp.getMaxMarks(),
                                selectedComp.getWeight()
                            );
                            zeroComp.setCounted(false);
                            calculationComponents.add(zeroComp);
                        }
                    }
                    
                    // Calculate results
                    CalculationResult result = calculator.calculateStudentMarks(
                        student.getId(), student.getName(), calculationComponents);
                    
                    // Add to table
                    SwingUtilities.invokeLater(() -> {
                        Object[] rowData = {
                            student.getName(),
                            student.getRollNumber(),
                            student.getSection(),
                            String.format("%.1f/%.1f", result.getTotalObtained(), result.getTotalPossible()),
                            String.format("%.2f%%", result.getFinalPercentage()),
                            result.getGrade(),
                            result.isPassing() ? "PASS" : "FAIL"
                        };
                        tableModel.addRow(rowData);
                    });
                    
                } catch (Exception e) {
                    System.err.println("Error calculating for student " + student.getName() + ": " + e.getMessage());
                    
                    // Add error row
                    SwingUtilities.invokeLater(() -> {
                        Object[] rowData = {
                            student.getName(),
                            student.getRollNumber(),
                            student.getSection(),
                            "Error",
                            "Error",
                            "Error",
                            "Error"
                        };
                        tableModel.addRow(rowData);
                    });
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error in calculatePreviewResults: " + e.getMessage());
            e.printStackTrace();
            
            SwingUtilities.invokeLater(() -> {
                tableModel.setRowCount(0);
                tableModel.addRow(new Object[]{"Error loading data", "", "", "", "", "", ""});
            });
        }
    }
    
    private List<Component> loadStudentComponentMarks(int studentId) {
        try {
            AnalyzerDAO dao = new AnalyzerDAO();
            
            // Get component IDs
            List<Integer> componentIds = components.stream()
                .map(Component::getId)
                .toList();
            
            // Get student marks
            java.util.Map<Integer, AnalyzerDAO.StudentComponentMark> studentMarks = 
                dao.getStudentComponentMarks(studentId, componentIds);
            
            List<Component> studentComponents = new java.util.ArrayList<>();
            
            for (Component comp : components) {
                AnalyzerDAO.StudentComponentMark mark = studentMarks.get(comp.getId());
                
                Component studentComp = new Component(
                    comp.getId(),
                    comp.getName(),
                    comp.getType(),
                    mark != null ? mark.marksObtained : 0,
                    comp.getMaxMarks(),
                    comp.getWeight()
                );
                
                studentComp.setCounted(mark != null ? mark.isCounted : false);
                studentComponents.add(studentComp);
            }
            
            return studentComponents;
            
        } catch (Exception e) {
            System.err.println("Error loading student component marks: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }
    
    private Component findMatchingComponent(List<Component> components, int componentId) {
        return components.stream()
            .filter(comp -> comp.getId() == componentId)
            .findFirst()
            .orElse(null);
    }
}