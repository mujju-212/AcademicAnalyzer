package com.sms.analyzer;

import com.sms.analyzer.SectionAnalyzer;
import com.sms.dao.AnalyzerDAO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
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
    
    private JTextField studentNameField;
    private JTextField rollNumberField;
    private JPanel resultsPanel;
    private JPanel analysisPanel;
    private JPanel chartPanel;
    private JRadioButton studentRadio;
    private JRadioButton sectionRadio;
    private JPanel subjectPerformancePanel;
    private HashMap<String, List<Student>> sectionStudents;
    private Student currentStudent;

    public StudentAnalyzer(JFrame parent, HashMap<String, List<Student>> sectionStudents) {
        this(parent, sectionStudents, null);
    }
    
    public StudentAnalyzer(JFrame parent, HashMap<String, List<Student>> sectionStudents, Runnable onCloseCallback) {
        this.parentFrame = parent;
        this.onCloseCallback = onCloseCallback;
        this.sectionStudents = sectionStudents != null ? sectionStudents : new HashMap<>();

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
        
        // Input Card - REDUCED SIZE
        JPanel inputCard = createModernCard();
        inputCard.setLayout(new BoxLayout(inputCard, BoxLayout.Y_AXIS));
        inputCard.setMaximumSize(new Dimension(550, 220)); // Reduced from 280
        
        // Student Name Field
        JLabel nameLabel = new JLabel("Student Name");
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        nameLabel.setForeground(TEXT_PRIMARY);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        studentNameField = createModernTextField();
        studentNameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        studentNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); // Smaller height
        
        // Roll Number Field
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
        
        inputCard.add(nameLabel);
        inputCard.add(Box.createVerticalStrut(5)); // Reduced spacing
        inputCard.add(studentNameField);
        inputCard.add(Box.createVerticalStrut(15)); // Reduced spacing
        inputCard.add(rollLabel);
        inputCard.add(Box.createVerticalStrut(5)); // Reduced spacing
        inputCard.add(rollNumberField);
        inputCard.add(Box.createVerticalStrut(20)); // Reduced spacing
        inputCard.add(analyzeButton);
        
        // Results Card
        resultsPanel = createModernCard();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setVisible(false);
        resultsPanel.setMaximumSize(new Dimension(550, 550)); // Increased size
        
        leftPanel.add(inputCard);
        leftPanel.add(Box.createVerticalStrut(20));
        leftPanel.add(resultsPanel);
        leftPanel.add(Box.createVerticalGlue());
        
        // Right side panel (Analysis + Chart)
        JPanel rightPanel = new JPanel(new BorderLayout(0, 20));
        rightPanel.setOpaque(false);
        
        // Analysis Panel (top of right side) - SINGLE ROW LAYOUT
        analysisPanel = createModernCard();
        analysisPanel.setVisible(false);
        analysisPanel.setPreferredSize(new Dimension(0, 120)); // Reduced height for single row
        
        // Chart Panel (bottom of right side)
        subjectPerformancePanel = createModernCard();
        subjectPerformancePanel.setLayout(new BorderLayout(0, 15));
        subjectPerformancePanel.setVisible(false);
        
        rightPanel.add(analysisPanel, BorderLayout.NORTH);
        rightPanel.add(subjectPerformancePanel, BorderLayout.CENTER);
        
        // Add panels to content area
        contentArea.add(leftPanel, BorderLayout.WEST);
        contentArea.add(rightPanel, BorderLayout.CENTER);
        
        mainPanel.add(contentArea, BorderLayout.CENTER);
        add(mainPanel);
    }

    // Update the updateAnalysisPanel method for single row layout
    private void updateAnalysisPanel(int totalMarks) {
        analysisPanel.removeAll();
        analysisPanel.setLayout(new BorderLayout());
        
        // Main panel for analysis
        JPanel mainAnalysisPanel = new JPanel();
        mainAnalysisPanel.setLayout(new BoxLayout(mainAnalysisPanel, BoxLayout.Y_AXIS));
        mainAnalysisPanel.setOpaque(false);
        
        // Analysis header
        JLabel analysisHeader = new JLabel("Analysis Summary");
        analysisHeader.setFont(new Font("SansSerif", Font.BOLD, 20));
        analysisHeader.setForeground(TEXT_PRIMARY);
        analysisHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        mainAnalysisPanel.add(analysisHeader);
        mainAnalysisPanel.add(Box.createVerticalStrut(15));
        
        // Create single row for metric cards
        JPanel metricsRow = new JPanel(new GridLayout(1, 4, 20, 0)); // Single row, 4 columns
        metricsRow.setOpaque(false);
        metricsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        double percentage = calculatePercentage(currentStudent.getMarks());
        double sgpa = calculateSGPA(percentage);
        
        // Total Marks Card
        metricsRow.add(createCompactMetricCard("📊", "Total Marks", String.valueOf(totalMarks), WARNING_COLOR));
        
        // SGPA Card
        metricsRow.add(createCompactMetricCard("🎯", "SGPA", String.format("%.1f", sgpa), SUCCESS_COLOR));
        
        // Percentage Card
        metricsRow.add(createCompactMetricCard("📈", "Percentage", String.format("%.1f%%", percentage), PRIMARY_COLOR));
        
        // Subjects Card
        metricsRow.add(createCompactMetricCard("📚", "Subjects", String.valueOf(currentStudent.getMarks().size()), INFO_COLOR));
        
        mainAnalysisPanel.add(metricsRow);
        
        analysisPanel.add(mainAnalysisPanel, BorderLayout.CENTER);
    }

    // Create a more compact metric card for single row layout
    private JPanel createCompactMetricCard(String icon, String title, String value, Color color) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Left border accent (thicker and more prominent)
                g2.setColor(color);
                g2.fillRoundRect(0, 0, 5, getHeight(), 3, 3);
                
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 10));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 0, 8);
        
        // Icon and title panel
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        labelPanel.setOpaque(false);
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        titleLabel.setForeground(TEXT_SECONDARY);
        
        labelPanel.add(iconLabel);
        labelPanel.add(titleLabel);
        
        // Value
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        valueLabel.setForeground(color);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        card.add(labelPanel, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.insets = new Insets(5, 0, 0, 0);
        card.add(valueLabel, gbc);
        
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
        String studentName = studentNameField.getText().trim();
        String rollNumber = rollNumberField.getText().trim();
        
        if (studentName.isEmpty() || rollNumber.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please enter both student name and roll number", 
                "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get student from database
        AnalyzerDAO analyzerDAO = new AnalyzerDAO();
        currentStudent = analyzerDAO.getStudentByNameAndRoll(
            studentName, 
            rollNumber, 
            com.sms.login.LoginScreen.currentUserId
        );
        
        if (currentStudent == null) {
            JOptionPane.showMessageDialog(this, 
                "Student not found. Please check the name and roll number.", 
                "Student Not Found", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        displayResults();
    }
    
    private void displayResults() {
        // Update results panel
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
        
        // Create modern table for marks with Status column
        String[] columns = {"Subject", "Marks", "Grade", "Status"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        // Add subject marks to table
        int totalMarks = 0;
        for (Map.Entry<String, Integer> entry : currentStudent.getMarks().entrySet()) {
            String grade = getGrade(entry.getValue());
            String status = entry.getValue() >= 40 ? "Pass" : "Fail"; // Assuming 40 is passing marks
            tableModel.addRow(new Object[]{entry.getKey(), entry.getValue(), grade, status});
            totalMarks += entry.getValue();
        }
        
        JTable marksTable = new JTable(tableModel);
        marksTable.setRowHeight(40);
        marksTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
        marksTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        marksTable.getTableHeader().setBackground(new Color(249, 250, 251));
        marksTable.getTableHeader().setForeground(TEXT_PRIMARY);
        marksTable.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_COLOR));
        marksTable.setShowGrid(false);
        marksTable.setIntercellSpacing(new Dimension(0, 0));
        marksTable.setBorder(null);
        marksTable.setBackground(Color.WHITE);
        
        // Custom cell renderer for modern look
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
                
                // Consistent styling
                label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                    BorderFactory.createEmptyBorder(0, 10, 0, 10)
                ));
                label.setBackground(Color.WHITE);
                label.setForeground(TEXT_PRIMARY);
                label.setFont(new Font("SansSerif", Font.PLAIN, 14));
                
                if (column == 1 || column == 2 || column == 3) {
                    label.setHorizontalAlignment(JLabel.CENTER);
                }
                
                if (column == 2) { // Grade column
                    String grade = value.toString();
                    label.setFont(new Font("SansSerif", Font.BOLD, 14));
                    if (grade.equals("A") || grade.equals("B")) {
                        label.setForeground(SUCCESS_COLOR);
                    } else if (grade.equals("F")) {
                        label.setForeground(DANGER_COLOR);
                    } else {
                        label.setForeground(TEXT_PRIMARY);
                    }
                }
                
                if (column == 3) { // Status column
                    String status = value.toString();
                    label.setFont(new Font("SansSerif", Font.BOLD, 14));
                    if (status.equals("Pass")) {
                        label.setForeground(SUCCESS_COLOR);
                    } else {
                        label.setForeground(DANGER_COLOR);
                    }
                }
                
                return label;
            }
        };
        
        for (int i = 0; i < marksTable.getColumnCount(); i++) {
            marksTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
        
        // Adjust column widths
        marksTable.getColumnModel().getColumn(0).setPreferredWidth(150); // Subject
        marksTable.getColumnModel().getColumn(1).setPreferredWidth(80);  // Marks
        marksTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // Grade
        marksTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // Status
        
        JScrollPane scrollPane = new JScrollPane(marksTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1, true));
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setPreferredSize(new Dimension(500, 250));
        scrollPane.setMaximumSize(new Dimension(500, 300));
        
        // Total marks label
        JLabel totalMarksLabel = new JLabel("Total Marks: " + totalMarks);
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
        
        // Update analysis panel
        updateAnalysisPanel(totalMarks);
        
        // Create bar chart
        createModernBarChart(currentStudent.getMarks());
        
        // Update subject performance panel
        updateSubjectPerformancePanel(totalMarks);
        
        // Make panels visible
        resultsPanel.setVisible(true);
        analysisPanel.setVisible(true);
        subjectPerformancePanel.setVisible(true);
        
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
    
    private double calculatePercentage(Map<String, Integer> marks) {
        if (marks.isEmpty()) return 0.0;
        
        int total = 0;
        for (Integer mark : marks.values()) {
            total += mark;
        }
        
        return (double) total / marks.size();
    }
    
    private double calculateSGPA(double percentage) {
        if (percentage >= 90) return 10.0;
        if (percentage >= 80) return 9.0;
        if (percentage >= 70) return 8.0;
        if (percentage >= 60) return 7.0;
        if (percentage >= 50) return 6.0;
        return 0.0;
    }
    
    private void createModernBarChart(Map<String, Integer> marks) {
        subjectPerformancePanel.removeAll();
        subjectPerformancePanel.setLayout(new BorderLayout(0, 15));
        
        // Chart header
        JLabel chartTitle = new JLabel("📊 Subject Performance");
        chartTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        chartTitle.setForeground(TEXT_PRIMARY);
        chartTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        // Find max marks for scaling and calculate total
        int maxMark = 0;
        int totalMarks = 0;
        for (Map.Entry<String, Integer> entry : marks.entrySet()) {
            dataset.addValue(entry.getValue(), "Marks", entry.getKey());
            if (entry.getValue() > maxMark) {
            	maxMark = entry.getValue();
            }
            totalMarks += entry.getValue();
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
    
    private void closePanel() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }
}
