package com.sms.marking.dialogs;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import com.sms.marking.models.*;
import com.sms.marking.utils.MarkCalculator;
import com.sms.database.DatabaseConnection;
import com.sms.marking.dao.*;
import com.sms.theme.ThemeManager;

public class FlexibleMarkEntryDialog extends JDialog {
    private ThemeManager themeManager;
    private int sectionId;
    private int subjectId;
    private String subjectName;
    private int userId;
    
    private MarkingScheme markingScheme;
    private List<Student> students;
    private Map<String, Map<Integer, StudentComponentMark>> marksByComponent;
    
    private JTabbedPane tabbedPane;
    private JLabel statusLabel;
    
    private StudentComponentMarkDAO markDAO;
    private boolean hasChanges = false;
    
    public FlexibleMarkEntryDialog(Frame parent, int sectionId, int subjectId, 
                                  String subjectName, int userId) {
        super(parent, "Mark Entry - " + subjectName, true);
        this.themeManager = ThemeManager.getInstance();
        this.sectionId = sectionId;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.userId = userId;
        this.markDAO = new StudentComponentMarkDAO();
        this.marksByComponent = new HashMap<>();
        
        loadData();
        initializeUI();
        
        setSize(1000, 700);
        setLocationRelativeTo(parent);
    }
    
    private void loadData() {
        try {
            // Load marking scheme
            MarkingSchemeDAO schemeDAO = new MarkingSchemeDAO();
            markingScheme = schemeDAO.getMarkingScheme(sectionId, subjectId);
            
            if (markingScheme == null) {
                throw new Exception("No marking scheme found for this subject");
            }
            
            // Load students
            loadStudents();
            
            // Load existing marks
            loadExistingMarks();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error loading data: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }
    
    private void loadStudents() throws SQLException {
        students = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            String query = "SELECT id, roll_number, student_name FROM students " +
                          "WHERE section_id = ? ORDER BY roll_number";
            pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, sectionId);
            
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Student student = new Student();
                student.setId(rs.getInt("id"));
                student.setRollNumber(rs.getString("roll_number"));
                student.setStudentName(rs.getString("student_name"));
                students.add(student);
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) { }
            if (conn != null) try { conn.close(); } catch (SQLException e) { }
        }
    }
    
    private void loadExistingMarks() throws SQLException {
        for (ComponentGroup group : markingScheme.getComponentGroups()) {
            for (MarkingComponent component : group.getComponents()) {
                Map<Integer, StudentComponentMark> componentMarks = 
                    markDAO.getComponentMarksForSection(sectionId, component.getId());
                marksByComponent.put(component.getComponentName(), componentMarks);
            }
        }
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(themeManager.getBackgroundColor());
        
        // Header
        add(createHeaderPanel(), BorderLayout.NORTH);
        
        // Main content - Tabbed pane for component groups
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        // Create tabs for each component group
        for (ComponentGroup group : markingScheme.getComponentGroups()) {
            JPanel groupPanel = createGroupPanel(group);
            String tabTitle = group.getGroupName() + 
                             " (" + group.getTotalGroupMarks() + " marks)";
            tabbedPane.addTab(tabTitle, groupPanel);
            
            // Set tab color based on type
            int index = tabbedPane.getTabCount() - 1;
            if ("internal".equals(group.getGroupType())) {
                tabbedPane.setBackgroundAt(index, new Color(34, 197, 94, 30));
            } else {
                tabbedPane.setBackgroundAt(index, new Color(59, 130, 246, 30));
            }
        }
        
        // Add summary tab
        JPanel summaryPanel = createSummaryPanel();
        tabbedPane.addTab("Summary", summaryPanel);
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Footer
        add(createFooterPanel(), BorderLayout.SOUTH);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(themeManager.getCardColor());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, themeManager.getBorderColor()),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        // Title and info
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        infoPanel.setBackground(themeManager.getCardColor());
        
        JLabel titleLabel = new JLabel("Flexible Mark Entry System");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLabel.setForeground(themeManager.getTextPrimaryColor());
        
        JLabel subjectLabel = new JLabel("Subject: " + subjectName);
        subjectLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subjectLabel.setForeground(themeManager.getTextSecondaryColor());
        
        JLabel schemeLabel = new JLabel("Scheme: " + markingScheme.getSchemeName());
        schemeLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        schemeLabel.setForeground(themeManager.getTextSecondaryColor());
        
        infoPanel.add(titleLabel);
        infoPanel.add(subjectLabel);
        infoPanel.add(schemeLabel);
        
        // Quick actions
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionsPanel.setBackground(themeManager.getCardColor());
        
        JButton importBtn = createStyledButton("Import from Excel", 
                                             themeManager.getSecondaryColor());
        JButton exportBtn = createStyledButton("Export to Excel", 
                                             themeManager.getSecondaryColor());
        
        actionsPanel.add(importBtn);
        actionsPanel.add(exportBtn);
        
        panel.add(infoPanel, BorderLayout.WEST);
        panel.add(actionsPanel, BorderLayout.EAST);
        
        return panel;
    }
    
   
        private JPanel createGroupPanel(ComponentGroup group) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(themeManager.getBackgroundColor());
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            
            // Group info panel
            JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            infoPanel.setBackground(themeManager.getCardColor());
            infoPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
            
            JLabel groupInfoLabel = new JLabel(group.getDisplayName());
            groupInfoLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            groupInfoLabel.setForeground(
                "internal".equals(group.getGroupType()) ? 
                new Color(34, 197, 94) : new Color(59, 130, 246)
            );
            
            infoPanel.add(groupInfoLabel);
            
            // Create table for components in this group
            String[] columnNames = createColumnNames(group);
            DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    // Only mark columns are editable (not roll number and name)
                    return column > 1;
                }
                
                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    if (columnIndex > 1) {
                        return Double.class;
                    }
                    return String.class;
                }
            };
            
            // Populate table with student data
            for (Student student : students) {
                Object[] rowData = new Object[columnNames.length];
                rowData[0] = student.getRollNumber();
                rowData[1] = student.getStudentName();
                
                // Add marks for each component
                int colIndex = 2;
                for (MarkingComponent component : group.getComponents()) {
                    Map<Integer, StudentComponentMark> componentMarks = 
                        marksByComponent.get(component.getComponentName());
                    
                    if (componentMarks != null && componentMarks.containsKey(student.getId())) {
                        StudentComponentMark mark = componentMarks.get(student.getId());
                        if (mark.hasMarks()) {
                            rowData[colIndex] = mark.getMarksObtained();
                        }
                    }
                    colIndex++;
                }
                
                tableModel.addRow(rowData);
            }
            
            JTable table = createStyledTable(tableModel);
            
            // Add cell editor with validation
            for (int i = 2; i < table.getColumnCount(); i++) {
                final int componentIndex = i - 2;
                final MarkingComponent component = group.getComponents().get(componentIndex);
                
                table.getColumnModel().getColumn(i).setCellEditor(
                    new DefaultCellEditor(new JTextField()) {
                        @Override
                        public boolean stopCellEditing() {
                            String value = (String) getCellEditorValue();
                            if (value == null || value.trim().isEmpty()) {
                                return super.stopCellEditing();
                            }
                            
                            try {
                                double marks = Double.parseDouble(value);
                                if (marks < 0) {
                                    JOptionPane.showMessageDialog(null, 
                                        "Marks cannot be negative", 
                                        "Invalid Input", 
                                        JOptionPane.ERROR_MESSAGE);
                                    return false;
                                }
                                if (marks > component.getActualMaxMarks()) {
                                    JOptionPane.showMessageDialog(null, 
                                        "Marks cannot exceed " + component.getActualMaxMarks(), 
                                        "Invalid Input", 
                                        JOptionPane.ERROR_MESSAGE);
                                    return false;
                                }
                            } catch (NumberFormatException e) {
                                JOptionPane.showMessageDialog(null, 
                                    "Please enter a valid number", 
                                    "Invalid Input", 
                                    JOptionPane.ERROR_MESSAGE);
                                return false;
                            }
                            
                            return super.stopCellEditing();
                        }
                    }
                );
            }
            
            // Add table model listener to track changes
            tableModel.addTableModelListener(e -> {
                if (e.getType() == TableModelEvent.UPDATE) {
                    hasChanges = true;
                    int row = e.getFirstRow();
                    int col = e.getColumn();
                    
                    if (col > 1) {
                        // Save mark immediately
                        Student student = students.get(row);
                        MarkingComponent component = group.getComponents().get(col - 2);
                        Object value = tableModel.getValueAt(row, col);
                        
                        saveMarkForStudent(student.getId(), component, value);
                    }
                }
            });
            
            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setBorder(BorderFactory.createLineBorder(themeManager.getBorderColor()));
            
            // Quick fill panel
            JPanel quickFillPanel = createQuickFillPanel(table, group);
            
            panel.add(infoPanel, BorderLayout.NORTH);
            panel.add(scrollPane, BorderLayout.CENTER);
            panel.add(quickFillPanel, BorderLayout.SOUTH);
            
            return panel;
        }
        
        private String[] createColumnNames(ComponentGroup group) {
            List<String> columns = new ArrayList<>();
            columns.add("Roll No");
            columns.add("Student Name");
            
            for (MarkingComponent component : group.getComponents()) {
                columns.add(component.getComponentName() + " (" + component.getActualMaxMarks() + ")");
            }
            
            return columns.toArray(new String[0]);
        }
        
        private JPanel createQuickFillPanel(JTable table, ComponentGroup group) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            panel.setBackground(themeManager.getCardColor());
            panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
            
            JLabel quickFillLabel = new JLabel("Quick Fill:");
            quickFillLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
            
            JComboBox<String> componentCombo = new JComboBox<>();
            for (MarkingComponent comp : group.getComponents()) {
                componentCombo.addItem(comp.getComponentName());
            }
            
            JTextField marksField = new JTextField(10);
            marksField.setToolTipText("Enter marks to fill for all students");
            
            JButton fillAllBtn = new JButton("Fill All");
            fillAllBtn.setFont(new Font("SansSerif", Font.PLAIN, 12));
            
            JButton fillAbsentBtn = new JButton("Mark All Absent");
            fillAbsentBtn.setFont(new Font("SansSerif", Font.PLAIN, 12));
            
            fillAllBtn.addActionListener(e -> {
                String marksText = marksField.getText().trim();
                if (marksText.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please enter marks to fill");
                    return;
                }
                
                try {
                    double marks = Double.parseDouble(marksText);
                    int componentIndex = componentCombo.getSelectedIndex();
                    MarkingComponent component = group.getComponents().get(componentIndex);
                    
                    if (marks < 0 || marks > component.getActualMaxMarks()) {
                        JOptionPane.showMessageDialog(this, 
                            "Marks must be between 0 and " + component.getActualMaxMarks());
                        return;
                    }
                    
                    int confirm = JOptionPane.showConfirmDialog(this,
                        "Fill " + marks + " marks for all students in " + 
                        component.getComponentName() + "?",
                        "Confirm Fill All",
                        JOptionPane.YES_NO_OPTION);
                    
                    if (confirm == JOptionPane.YES_OPTION) {
                        DefaultTableModel model = (DefaultTableModel) table.getModel();
                        int col = componentIndex + 2;
                        
                        for (int row = 0; row < model.getRowCount(); row++) {
                            model.setValueAt(marks, row, col);
                        }
                    }
                    
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Please enter a valid number");
                }
            });
            
            fillAbsentBtn.addActionListener(e -> {
                int componentIndex = componentCombo.getSelectedIndex();
                MarkingComponent component = group.getComponents().get(componentIndex);
                
                int confirm = JOptionPane.showConfirmDialog(this,
                    "Mark all students as absent for " + component.getComponentName() + "?",
                    "Confirm Mark Absent",
                    JOptionPane.YES_NO_OPTION);
                
                if (confirm == JOptionPane.YES_OPTION) {
                    for (Student student : students) {
                        StudentComponentMark mark = new StudentComponentMark();
                        mark.setStudentId(student.getId());
                        mark.setComponentId(component.getId());
                        mark.setStatus("absent");
                        mark.setEnteredBy(userId);
                        
                        try {
                            markDAO.saveStudentMark(mark);
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    }
                    
                    // Clear marks in table
                    DefaultTableModel model = (DefaultTableModel) table.getModel();
                    int col = componentIndex + 2;
                    for (int row = 0; row < model.getRowCount(); row++) {
                        model.setValueAt(null, row, col);
                    }
                }
            });
            
            panel.add(quickFillLabel);
            panel.add(componentCombo);
            panel.add(marksField);
            panel.add(fillAllBtn);
            panel.add(fillAbsentBtn);
            
            return panel;
        }
        
        private JPanel createSummaryPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(themeManager.getBackgroundColor());
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            
            // Create summary table
            String[] columns = {"Roll No", "Student Name", "Internal", "External", "Total", "Grade"};
            DefaultTableModel summaryModel = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            
            // Calculate and populate summary
            MarkCalculator calculator = new MarkCalculator();
            
            for (Student student : students) {
                MarkCalculator.CalculationResult result = 
                    calculator.calculateStudentSubjectMarks(student.getId(), sectionId, subjectId);
                
                Object[] rowData = {
                    student.getRollNumber(),
                    student.getStudentName(),
                    String.format("%.2f / %d", result.getInternalMarks(), 
                                 markingScheme.getTotalInternalMarks()),
                    String.format("%.2f / %d", result.getExternalMarks(), 
                                 markingScheme.getTotalExternalMarks()),
                    String.format("%.2f / %d", result.getTotalMarks(), 
                                 markingScheme.getTotalMarks()),
                    calculateGrade(result.getTotalPercentage())
                };
                
                summaryModel.addRow(rowData);
            }
            
            JTable summaryTable = createStyledTable(summaryModel);
            
            // Color code based on performance
            summaryTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                        boolean isSelected, boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, 
                        isSelected, hasFocus, row, column);
                    
                    if (column == 5 && !isSelected) { // Grade column
                        String grade = (String) value;
                        if ("A+".equals(grade) || "A".equals(grade)) {
                            c.setBackground(new Color(34, 197, 94, 30));
                        } else if ("F".equals(grade)) {
                            c.setBackground(new Color(239, 68, 68, 30));
                        } else {
                            c.setBackground(table.getBackground());
                        }
                    }
                    
                    return c;
                }
            });
            
            JScrollPane scrollPane = new JScrollPane(summaryTable);
            
            // Statistics panel
            JPanel statsPanel = createStatisticsPanel();
            
            panel.add(scrollPane, BorderLayout.CENTER);
            panel.add(statsPanel, BorderLayout.SOUTH);
            
            return panel;
        }
        
        private JPanel createStatisticsPanel() {
            JPanel panel = new JPanel(new GridLayout(2, 3, 20, 10));
            panel.setBackground(themeManager.getCardColor());
            panel.setBorder(BorderFactory.createTitledBorder("Class Statistics"));
            
            // Calculate statistics
            int totalStudents = students.size();
            int passed = 0;
            double totalPercentage = 0;
            double highest = 0;
            double lowest = 100;
            
            MarkCalculator calculator = new MarkCalculator();
            
            for (Student student : students) {
                MarkCalculator.CalculationResult result = 
                    calculator.calculateStudentSubjectMarks(student.getId(), sectionId, subjectId);
                
                double percentage = result.getTotalPercentage();
                totalPercentage += percentage;
                
                if (percentage >= 40) passed++; // Assuming 40% is pass
                if (percentage > highest) highest = percentage;
                if (percentage < lowest) lowest = percentage;
            }
            
            double average = totalPercentage / totalStudents;
            double passPercentage = (passed * 100.0) / totalStudents;
            
            // Add statistics labels
            panel.add(createStatLabel("Total Students:", String.valueOf(totalStudents)));
            panel.add(createStatLabel("Passed:", String.format("%d (%.1f%%)", passed, passPercentage)));
            panel.add(createStatLabel("Failed:", String.valueOf(totalStudents - passed)));
            panel.add(createStatLabel("Average:", String.format("%.2f%%", average)));
            panel.add(createStatLabel("Highest:", String.format("%.2f%%", highest)));
            panel.add(createStatLabel("Lowest:", String.format("%.2f%%", lowest)));
            
            return panel;
        }
        
        private JLabel createStatLabel(String label, String value) {
            JLabel statLabel = new JLabel(label + " " + value);
            statLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            return statLabel;
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
        
        private void saveMarkForStudent(int studentId, MarkingComponent component, Object value) {
            StudentComponentMark mark = new StudentComponentMark();
            mark.setStudentId(studentId);
            mark.setComponentId(component.getId());
            mark.setEnteredBy(userId);
            
            if (value != null && !value.toString().trim().isEmpty()) {
                try {
                    double marks = Double.parseDouble(value.toString());
                    mark.setMarksObtained(marks);
                    mark.setStatus("present");
                } catch (NumberFormatException e) {
                    return;
                }
            } else {
                mark.setMarksObtained(null);
                mark.setStatus("present");
            }
            
            try {
                markDAO.saveStudentMark(mark);
                
                // Update local cache
                Map<Integer, StudentComponentMark> componentMarks = 
                    marksByComponent.computeIfAbsent(component.getComponentName(), k -> new HashMap<>());
                componentMarks.put(studentId, mark);
                
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, 
                    "Error saving mark: " + e.getMessage(), 
                    "Database Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
        
        private JPanel createFooterPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(themeManager.getCardColor());
            panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, themeManager.getBorderColor()),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
            ));
            
            // Status label
            statusLabel = new JLabel("Ready");
            statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
            
            // Buttons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            buttonPanel.setBackground(themeManager.getCardColor());
            
            JButton refreshBtn = createStyledButton("Refresh", themeManager.getSecondaryColor());
            JButton saveAllBtn = createStyledButton("Save All", themeManager.getPrimaryColor());
            JButton closeBtn = createStyledButton("Close", themeManager.getBackgroundColor());
            closeBtn.setBorder(BorderFactory.createLineBorder(themeManager.getBorderColor()));
            
            refreshBtn.addActionListener(e -> {
                loadData();
                refreshTables();
                statusLabel.setText("Data refreshed");
            });
            
            saveAllBtn.addActionListener(e -> {
                if (hasChanges) {
                    statusLabel.setText("All changes are automatically saved");
                    hasChanges = false;
                } else {
                    statusLabel.setText("No changes to save");
                }
            });
            
            closeBtn.addActionListener(e -> {
                if (hasChanges) {
                    int confirm = JOptionPane.showConfirmDialog(this,
                        "You have unsaved changes. Close anyway?",
                        "Confirm Close",
                        JOptionPane.YES_NO_OPTION);
                    
                    if (confirm == JOptionPane.YES_OPTION) {
                        dispose();
                    }
                } else {
                    dispose();
                }
            });
            
            buttonPanel.add(refreshBtn);
            buttonPanel.add(saveAllBtn);
            buttonPanel.add(closeBtn);
            
            panel.add(statusLabel, BorderLayout.WEST);
            panel.add(buttonPanel, BorderLayout.EAST);
            
            return panel;
        }
        
        private void refreshTables() {
            // Refresh all tables in tabs
            for (int i = 0; i < tabbedPane.getTabCount() - 1; i++) { // Exclude summary tab
                Component comp = tabbedPane.getComponentAt(i);
                if (comp instanceof JPanel) {
                    JPanel panel = (JPanel) comp;
                    for (Component c : panel.getComponents()) {
                        if (c instanceof JScrollPane) {
                            JScrollPane scrollPane = (JScrollPane) c;
                            JViewport viewport = scrollPane.getViewport();
                            if (viewport.getView() instanceof JTable) {
                                JTable table = (JTable) viewport.getView();
                                ((DefaultTableModel) table.getModel()).fireTableDataChanged();
                            }
                        }
                    }
                }
            }
        }
        
        private JTable createStyledTable(DefaultTableModel model) {
            JTable table = new JTable(model);
            table.setRowHeight(35);
            table.setFont(new Font("SansSerif", Font.PLAIN, 13));
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setBackground(themeManager.getCardColor());
            table.setForeground(themeManager.getTextPrimaryColor());
            table.setSelectionBackground(new Color(99, 102, 241, 50));
            table.setSelectionForeground(themeManager.getTextPrimaryColor());
            table.setGridColor(themeManager.getBorderColor());
            
            // Style header
            JTableHeader header = table.getTableHeader();
            header.setFont(new Font("SansSerif", Font.BOLD, 13));
            header.setBackground(themeManager.getCardColor());
            header.setForeground(themeManager.getTextPrimaryColor());
            header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, themeManager.getBorderColor()));
            
            // Set column widths
            table.getColumnModel().getColumn(0).setPreferredWidth(80);  // Roll No
            table.getColumnModel().getColumn(1).setPreferredWidth(200); // Name
            
            return table;
        }
        
        private JButton createStyledButton(String text, Color bgColor) {
            JButton button = new JButton(text);
            button.setFont(new Font("SansSerif", Font.BOLD, 13));
            button.setBackground(bgColor);
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            button.setPreferredSize(new Dimension(100, 35));
            return button;
        }
        
        // Inner class for Student
        private static class Student {
            private int id;
            private String rollNumber;
            private String studentName;
            
            public int getId() { return id; }
            public void setId(int id) { this.id = id; }
            
            public String getRollNumber() { return rollNumber; }
            public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
            
            public String getStudentName() { return studentName; }
            public void setStudentName(String studentName) { this.studentName = studentName; }
        }
    }