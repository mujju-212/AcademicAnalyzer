package com.sms.analyzer;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import org.jfree.chart.*;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import java.util.List;
import com.sms.dao.SectionDAO;
import com.sms.database.DatabaseConnection;
import com.sms.dao.AnalyzerDAO;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;

public class SectionAnalyzer extends JPanel {

    // Modern color scheme - matching StudentAnalyzer
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
    private static final Color SELECTION_COLOR = new Color(99, 102, 241, 30);

    private HashMap<String, ArrayList<Student>> sectionStudents;
    private JRadioButton studentRadio, sectionRadio;
    private JPanel mainContentPanel;
    private List<SectionDAO.SectionInfo> availableSections;
    private int currentSectionId;
    private String currentSectionName;
    private JFrame parentFrame;
    private Runnable onCloseCallback;
    
    // Filter-related fields
    private Map<String, Set<String>> selectedFilters; // Track selected subject-component combinations
    private Map<String, Set<String>> availableComponents; // All available components per subject
    private JPanel filterCard;

    // Constructor for standalone dialog (backward compatibility)
    public SectionAnalyzer(JFrame parent, HashMap<String, ArrayList<Student>> sectionStudents) {
        this(parent, sectionStudents, null);
        if (parent != null) {
            JDialog dialog = new JDialog(parent, "Section Performance Analyzer", true);
            dialog.setSize(1300, 750);
            dialog.setLocationRelativeTo(parent);
            dialog.setLayout(new BorderLayout());
            dialog.getContentPane().setBackground(BACKGROUND_COLOR);
            dialog.add(this, BorderLayout.CENTER);
            dialog.setVisible(true);
        }
    }
    
    // Constructor for embedded panel
    public SectionAnalyzer(JFrame parent, HashMap<String, ArrayList<Student>> sectionStudents, Runnable onCloseCallback) {
        this.parentFrame = parent;
        this.sectionStudents = sectionStudents != null ? sectionStudents : new HashMap<>();
        this.onCloseCallback = onCloseCallback;
        
        // Get sections from database
        loadSectionsFromDatabase();
        
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);
        
        initializeUI();
    }

    private void loadSectionsFromDatabase() {
        SectionDAO sectionDAO = new SectionDAO();
        List<SectionDAO.SectionInfo> sections = sectionDAO.getSectionsByUser(
            com.sms.login.LoginScreen.currentUserId
        );
        
        this.availableSections = sections;
        if (!sections.isEmpty()) {
            this.currentSectionId = sections.get(0).id;
            this.currentSectionName = sections.get(0).sectionName;
            
            // Initialize filters for this section
            loadAvailableComponentsForSection(this.currentSectionId);
            initializeFilters();
        }
    }

    public static void openSectionAnalyzer(JFrame parent, HashMap<String, ArrayList<Student>> sectionStudents) {
        SwingUtilities.invokeLater(() -> new SectionAnalyzer(parent, sectionStudents));
    }

    private void initializeUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Header Card
        JPanel headerCard = createModernCard();
        headerCard.setLayout(new BorderLayout(15, 10));
        headerCard.setPreferredSize(new Dimension(0, 100));

        // Title and tabs panel
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        
        // Add back button if callback is present
        if (onCloseCallback != null) {
            JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            leftPanel.setOpaque(false);
            
            JButton backButton = new JButton("← Back");
            backButton.setFont(new Font("SansSerif", Font.BOLD, 14));
            backButton.setForeground(PRIMARY_COLOR);
            backButton.setBackground(CARD_COLOR);
            backButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY_COLOR, 2),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
            ));
            backButton.setFocusPainted(false);
            backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            backButton.addActionListener(e -> closePanel());
            
            leftPanel.add(backButton);
            topPanel.add(leftPanel, BorderLayout.WEST);
        }

        JLabel titleLabel = new JLabel("SECTION PERFORMANCE ANALYZER");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        titleLabel.setForeground(PRIMARY_COLOR);
        topPanel.add(titleLabel, onCloseCallback != null ? BorderLayout.CENTER : BorderLayout.WEST);

        // Radio buttons panel
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        radioPanel.setOpaque(false);
        
        ButtonGroup bg = new ButtonGroup();
        studentRadio = createModernRadioButton("Student", false);
        sectionRadio = createModernRadioButton("Section", true);
        
        studentRadio.addActionListener(e -> {
            closePanel();
            HashMap<String, List<Student>> studentMap = new HashMap<>();
            if (currentSectionName != null) {
                AnalyzerDAO analyzerDAO = new AnalyzerDAO();
                List<Student> students = analyzerDAO.getStudentsBySection(
                    currentSectionId, 
                    com.sms.login.LoginScreen.currentUserId
                );
                studentMap.put(currentSectionName, students);
            }
            StudentAnalyzer.openAnalyzer((JFrame) getParent(), studentMap);
        });
        
        bg.add(studentRadio);
        bg.add(sectionRadio);
        radioPanel.add(studentRadio);
        radioPanel.add(sectionRadio);

        headerCard.add(topPanel, BorderLayout.NORTH);
        headerCard.add(radioPanel, BorderLayout.CENTER);
        
        mainPanel.add(headerCard, BorderLayout.NORTH);

        // Main content panel
        mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setOpaque(false);
        
        createMainContent();
        
        mainPanel.add(mainContentPanel, BorderLayout.CENTER);
        add(mainPanel);
    }

    private void createMainContent() {
        if (currentSectionId == 0) {
            JLabel noDataLabel = new JLabel("No sections available");
            noDataLabel.setHorizontalAlignment(SwingConstants.CENTER);
            noDataLabel.setFont(new Font("SansSerif", Font.ITALIC, 16));
            noDataLabel.setForeground(TEXT_SECONDARY);
            mainContentPanel.add(noDataLabel);
            return;
        }
        
        AnalyzerDAO analyzerDAO = new AnalyzerDAO();
        AnalyzerDAO.SectionAnalysisData analysisData = analyzerDAO.getSectionAnalysisWithFilters(
            currentSectionId, 
            com.sms.login.LoginScreen.currentUserId,
            selectedFilters
        );
        
        if (analysisData == null) {
            JLabel noDataLabel = new JLabel("No data available for this section");
            noDataLabel.setHorizontalAlignment(SwingConstants.CENTER);
            noDataLabel.setFont(new Font("SansSerif", Font.ITALIC, 16));
            noDataLabel.setForeground(TEXT_SECONDARY);
            mainContentPanel.add(noDataLabel);
            return;
        }

        // Main layout with filter on left
        JPanel mainLayout = new JPanel(new BorderLayout(0, 0));
        mainLayout.setOpaque(false);
        
        // Add filter panel on the left
        JPanel filterPanel = createFilterPanel();
        mainLayout.add(filterPanel, BorderLayout.WEST);
        
        // Create scrollable content wrapper
        JPanel contentWrapper = new JPanel();
        contentWrapper.setLayout(new BoxLayout(contentWrapper, BoxLayout.Y_AXIS));
        contentWrapper.setOpaque(false);
        contentWrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15));

        // Section tabs
        JPanel tabsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabsPanel.setOpaque(false);
        tabsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        ButtonGroup tabGroup = new ButtonGroup();
        
        if (availableSections == null || availableSections.isEmpty()) {
            JLabel noSectionsLabel = new JLabel("No sections available");
            noSectionsLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
            noSectionsLabel.setForeground(TEXT_SECONDARY);
            tabsPanel.add(noSectionsLabel);
        } else {
            for (int i = 0; i < availableSections.size(); i++) {
                SectionDAO.SectionInfo section = availableSections.get(i);
                JToggleButton tabButton = createModernTab(section.sectionName, section.id == currentSectionId);
                
                // Calculate width based on text length
                FontMetrics fm = tabButton.getFontMetrics(tabButton.getFont());
                int textWidth = fm.stringWidth(section.sectionName);
                int tabWidth = Math.max(45, textWidth + 16); // Min 45px, +16 for padding
                tabButton.setPreferredSize(new Dimension(tabWidth, 28));
                
                tabButton.addActionListener(e -> {
                    currentSectionId = section.id;
                    currentSectionName = section.sectionName;
                    updateTabAppearance(tabsPanel, tabButton);
                    refreshSectionData();
                });
                
                tabGroup.add(tabButton);
                tabsPanel.add(tabButton);
            }
        }
        
        contentWrapper.add(tabsPanel);
        contentWrapper.add(Box.createVerticalStrut(5));

        // PDF Export Button
        JPanel exportPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        exportPanel.setOpaque(false);
        exportPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        exportPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        JButton exportButton = createExportButton();
        exportPanel.add(exportButton);
        contentWrapper.add(exportPanel);
        contentWrapper.add(Box.createVerticalStrut(5));

        // Section Result Analysis at the TOP
        JPanel sectionResultPanel = createSectionResultAnalysis(analysisData);
        sectionResultPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentWrapper.add(sectionResultPanel);
        contentWrapper.add(Box.createVerticalStrut(15));

        // ROW 1: Subject Analysis (FULL WIDTH)
        JPanel subjectRow = new JPanel(new BorderLayout());
        subjectRow.setOpaque(false);
        subjectRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        subjectRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
        
        JPanel subjectPanel = createSubjectTable(analysisData.subjectAnalysisList);
        subjectRow.add(subjectPanel, BorderLayout.CENTER);
        
        contentWrapper.add(subjectRow);
        contentWrapper.add(Box.createVerticalStrut(15));

        // ROW 2: Marks Analysis + Grade Distribution
        JPanel row2 = new JPanel(new GridLayout(1, 2, 15, 0));
        row2.setOpaque(false);
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);
        row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        
        JPanel chartCard = createModernCard();
        chartCard.setLayout(new BorderLayout());
        chartCard.add(createChart(analysisData), BorderLayout.CENTER);
        
        JPanel gradeDistCard = createModernCard();
        gradeDistCard.setLayout(new BorderLayout());
        gradeDistCard.add(createGradeDistributionChart(analyzerDAO), BorderLayout.CENTER);
        
        row2.add(chartCard);
        row2.add(gradeDistCard);
        
        contentWrapper.add(row2);
        contentWrapper.add(Box.createVerticalStrut(15));
        
        // ROW 3: Top 5 Students + Failed Analysis
        JPanel row3 = new JPanel(new GridLayout(1, 2, 15, 0));
        row3.setOpaque(false);
        row3.setAlignmentX(Component.LEFT_ALIGNMENT);
        row3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
        
        JPanel topStudentsCard = createModernCard();
        topStudentsCard.setLayout(new BorderLayout());
        topStudentsCard.add(createTopStudentsTable(analysisData.topStudents), BorderLayout.CENTER);
        
        JPanel failedStudentsCard = createModernCard();
        failedStudentsCard.setLayout(new BorderLayout());
        failedStudentsCard.add(createFailedStudentTable(analysisData.failedStudentsMap), BorderLayout.CENTER);
        
        row3.add(topStudentsCard);
        row3.add(failedStudentsCard);
        
        contentWrapper.add(row3);
        contentWrapper.add(Box.createVerticalStrut(15));
        
        // ROW 4: Student at Risk Panel
        JPanel atRiskRow = new JPanel(new BorderLayout());
        atRiskRow.setOpaque(false);
        atRiskRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        atRiskRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        
        JPanel atRiskCard = createModernCard();
        atRiskCard.setLayout(new BorderLayout());
        atRiskCard.add(createAtRiskStudentsPanel(analyzerDAO), BorderLayout.CENTER);
        
        atRiskRow.add(atRiskCard, BorderLayout.CENTER);
        contentWrapper.add(atRiskRow);
        contentWrapper.add(Box.createVerticalStrut(20));
        
        // Wrap content in JScrollPane
        JScrollPane scrollPane = new JScrollPane(contentWrapper);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        
        mainLayout.add(scrollPane, BorderLayout.CENTER);
        mainContentPanel.add(mainLayout, BorderLayout.CENTER);
    }

    private JPanel createSectionResultAnalysis(AnalyzerDAO.SectionAnalysisData analysisData) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(1250, 120));
        
        // Header
        JLabel titleLabel = new JLabel("📊 Section Result Analysis");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Create metric cards in a row
        JPanel metricsPanel = new JPanel(new GridLayout(1, 5, 12, 0));
        metricsPanel.setOpaque(false);
        metricsPanel.setMaximumSize(new Dimension(1000, 70));
        
        if (analysisData != null) {
            double passPercentage = (analysisData.totalStudents > 0) ? 
                (analysisData.passStudents * 100.0 / analysisData.totalStudents) : 0;
            double failPercentage = (analysisData.totalStudents > 0) ? 
                (analysisData.failStudents * 100.0 / analysisData.totalStudents) : 0;
            
            // Add metric cards
            metricsPanel.add(createResultCard("👥", "Total Students", 
                String.valueOf(analysisData.totalStudents), PRIMARY_COLOR));
            
            metricsPanel.add(createResultCard("✅", "Pass", 
                String.valueOf(analysisData.passStudents), SUCCESS_COLOR));
            
            metricsPanel.add(createResultCard("❌", "Fail", 
                String.valueOf(analysisData.failStudents), DANGER_COLOR));
            
            metricsPanel.add(createResultCard("📈", "Pass %", 
                String.format("%.1f%%", passPercentage), SUCCESS_COLOR));
            
            metricsPanel.add(createResultCard("📉", "Fail %", 
                String.format("%.1f%%", failPercentage), DANGER_COLOR));
        }
        
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(metricsPanel);
        
        return panel;
    }

    private JPanel createResultCard(String icon, String title, String value, Color color) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Background with color
                g2.setColor(color);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(5, 2));
        card.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        card.setPreferredSize(new Dimension(150, 60));
        
        // Top panel with icon and title
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        topPanel.setOpaque(false);
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        iconLabel.setForeground(Color.WHITE);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        titleLabel.setForeground(new Color(255, 255, 255, 200));
        
        topPanel.add(iconLabel);
        topPanel.add(titleLabel);
        
        // Value
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        card.add(topPanel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        
        return card;
    }

    private JPanel createTopStudentsTable(List<AnalyzerDAO.TopStudent> topStudents) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        // Header
        JLabel titleLabel = new JLabel("🏆 Top 5 Students");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        String[] columns = {"Rank", "Roll No.", "Name", "Marks", "Percentage"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(249, 250, 251));
        table.getTableHeader().setForeground(TEXT_PRIMARY);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_COLOR));
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setSelectionBackground(SELECTION_COLOR);
        table.setSelectionForeground(TEXT_PRIMARY);
        
        // Custom renderer
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
                
                label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                    BorderFactory.createEmptyBorder(0, 8, 0, 8)
                ));
                
                if (isSelected) {
                    label.setBackground(SELECTION_COLOR);
                    label.setForeground(TEXT_PRIMARY);
                } else {
                    label.setBackground(row % 2 == 0 ? Color.WHITE : new Color(249, 250, 251));
                    label.setForeground(TEXT_PRIMARY);
                }
                
                if (column == 0) { // Rank column
                    label.setHorizontalAlignment(JLabel.CENTER);
                    label.setFont(new Font("SansSerif", Font.BOLD, 14));
                    if (row == 0) {
                        label.setForeground(new Color(255, 215, 0)); // Gold
                        label.setText("1");
                    } else if (row == 1) {
                        label.setForeground(new Color(192, 192, 192)); // Silver
                        label.setText("2");
                    } else if (row == 2) {
                        label.setForeground(new Color(205, 127, 50)); // Bronze
                        label.setText("3");
                    } else {
                        if (!isSelected) label.setForeground(TEXT_PRIMARY);
                        label.setText(String.valueOf(row + 1));
                    }
                } else if (column == 3 || column == 4) { // Marks and Percentage columns
                    label.setHorizontalAlignment(JLabel.CENTER);
                    label.setFont(new Font("SansSerif", Font.BOLD, 12));
                    if (!isSelected) label.setForeground(SUCCESS_COLOR);
                }
                
                return label;
            }
        };
        
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(50);  // Rank
        table.getColumnModel().getColumn(1).setPreferredWidth(80);  // Roll No
        table.getColumnModel().getColumn(2).setPreferredWidth(150); // Name
        table.getColumnModel().getColumn(3).setPreferredWidth(80);  // Marks
        table.getColumnModel().getColumn(4).setPreferredWidth(80);  // Percentage

        if (topStudents != null) {
            int rank = 1;
            for (AnalyzerDAO.TopStudent student : topStudents) {
                model.addRow(new Object[]{
                    rank++,
                    student.rollNumber,
                    student.name,
                    student.totalMarks,
                    String.format("%.1f%%", student.percentage)
                });
            }
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    

    private JPanel createChart(AnalyzerDAO.SectionAnalysisData analysisData) {
        JPanel chartPanel = new JPanel(new BorderLayout());
        chartPanel.setOpaque(false);
        
        // Header
        JLabel titleLabel = new JLabel("📈 Marks Analysis");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        double maxPossibleMarks = 100; // Default
        
        if (analysisData != null && analysisData.subjectAnalysisList != null) {
            try {
                Connection conn = DatabaseConnection.getConnection();
                String maxMarksQuery = "SELECT MAX(ss.max_marks) as max_marks " +
                                     "FROM section_subjects ss " +
                                     "WHERE ss.section_id = ?";
                PreparedStatement ps = conn.prepareStatement(maxMarksQuery);
                ps.setInt(1, currentSectionId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    maxPossibleMarks = rs.getDouble("max_marks");
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            for (AnalyzerDAO.SubjectAnalysis subject : analysisData.subjectAnalysisList) {
                double avgMarks = Math.round(subject.averageMarks * 100.0) / 100.0;
                dataset.addValue(avgMarks, "Average Marks", subject.subjectName);
            }
        }

        JFreeChart chart = ChartFactory.createBarChart(
            null,
            "Subjects",
            "Average Marks",
            dataset,
            org.jfree.chart.plot.PlotOrientation.VERTICAL,
            false,
            true,
            false
        );
        
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(BORDER_COLOR);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(true);
        plot.setOutlineVisible(false);
        
        chart.setBorderVisible(false);
        chart.setBackgroundPaint(Color.WHITE);
        
        // X-axis configuration
        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setLowerMargin(0.05);
        domainAxis.setUpperMargin(0.05);
        domainAxis.setCategoryLabelPositions(org.jfree.chart.axis.CategoryLabelPositions.UP_45);
        domainAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        domainAxis.setTickLabelPaint(TEXT_PRIMARY);
        
        // Y-axis configuration
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        rangeAxis.setTickLabelPaint(TEXT_PRIMARY);
        rangeAxis.setLowerBound(0);
        if (maxPossibleMarks > 0) {
            rangeAxis.setUpperBound(maxPossibleMarks * 1.1);
        }
        
        // Bar renderer with gradient
        BarRenderer renderer = new BarRenderer() {
            @Override
            public Paint getItemPaint(int row, int column) {
                return new GradientPaint(
                    0, 0, PRIMARY_COLOR,
                    0, 200, PRIMARY_DARK
                );
            }
        };
        renderer.setShadowVisible(false);
        renderer.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());
        renderer.setMaximumBarWidth(0.08);
        renderer.setItemMargin(0.1);
        
        // Add value labels
        renderer.setDefaultItemLabelGenerator(new org.jfree.chart.labels.StandardCategoryItemLabelGenerator());
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelFont(new Font("SansSerif", Font.BOLD, 10));
        renderer.setDefaultItemLabelPaint(TEXT_PRIMARY);

        plot.setRenderer(renderer);
        
        ChartPanel chartComponent = new ChartPanel(chart);
        chartComponent.setPreferredSize(new Dimension(350, 160));
        chartComponent.setBackground(Color.WHITE);
        
        chartPanel.add(titleLabel, BorderLayout.NORTH);
        chartPanel.add(chartComponent, BorderLayout.CENTER);
        
        return chartPanel;
    }

    private JPanel createSubjectTable(List<AnalyzerDAO.SubjectAnalysis> subjectAnalysisList) {
        JPanel panel = createModernCard();
        panel.setLayout(new BorderLayout());
        
        // Header
        JLabel titleLabel = new JLabel("📊 Subject Analysis");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        String[] columns = {"Subject", "Total Students", "Pass", "DC", "FC", "SC", "Fail"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(249, 250, 251));
        table.getTableHeader().setForeground(TEXT_PRIMARY);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_COLOR));
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setSelectionBackground(SELECTION_COLOR);
        table.setSelectionForeground(TEXT_PRIMARY);
        
        // Custom cell renderer
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
                
                label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                    BorderFactory.createEmptyBorder(0, 8, 0, 8)
                ));
                
                if (isSelected) {
                    label.setBackground(SELECTION_COLOR);
                    label.setForeground(TEXT_PRIMARY);
                } else {
                    label.setBackground(row % 2 == 0 ? Color.WHITE : new Color(249, 250, 251));
                    label.setForeground(TEXT_PRIMARY);
                }
                
                if (column == 0) {
                    label.setFont(new Font("SansSerif", Font.BOLD, 12));
                } else {
                    label.setHorizontalAlignment(JLabel.CENTER);
                    
                    // Color coding for different columns
                    if (column == 2 && !isSelected) { // Pass column
                        label.setForeground(SUCCESS_COLOR);
                        label.setFont(new Font("SansSerif", Font.BOLD, 12));
                    } else if (column == 6 && !isSelected) { // Fail column
                        label.setForeground(DANGER_COLOR);
                        label.setFont(new Font("SansSerif", Font.BOLD, 12));
                    }
                }
                
                return label;
            }
        };
        
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        if (subjectAnalysisList != null) {
            for (AnalyzerDAO.SubjectAnalysis subject : subjectAnalysisList) {
                model.addRow(new Object[]{
                    subject.subjectName,
                    subject.totalStudents,
                    subject.passCount,
                    subject.distinctionCount,
                    subject.firstClassCount,
                    subject.secondClassCount,
                    subject.failCount
                });
            }
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }

    private JPanel createFailedStudentTable(Map<Integer, Integer> failedStudentsMap) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        // Header
        JLabel titleLabel = new JLabel("📉 Failed Analysis");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        String[] columns = {"No. of Failed Subjects", "Student Count"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(249, 250, 251));
        table.getTableHeader().setForeground(TEXT_PRIMARY);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_COLOR));
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setSelectionBackground(SELECTION_COLOR);
        table.setSelectionForeground(TEXT_PRIMARY);
        
        // Custom renderer
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
                
                label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                    BorderFactory.createEmptyBorder(0, 8, 0, 8)
                ));
                
                if (isSelected) {
                    label.setBackground(SELECTION_COLOR);
                    label.setForeground(TEXT_PRIMARY);
                } else {
                    label.setBackground(row % 2 == 0 ? Color.WHITE : new Color(249, 250, 251));
                    label.setForeground(TEXT_PRIMARY);
                }
                
                label.setHorizontalAlignment(JLabel.CENTER);
                
                if (column == 0) {
                    label.setFont(new Font("SansSerif", Font.BOLD, 12));
                    if (!isSelected) label.setForeground(DANGER_COLOR);
                } else if (column == 1) {
                    int count = Integer.parseInt(value.toString());
                    if (count > 0 && !isSelected) {
                        label.setForeground(WARNING_COLOR);
                        label.setFont(new Font("SansSerif", Font.BOLD, 12));
                    }
                }
                
                return label;
            }
        };
        
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        if (failedStudentsMap != null) {
            for (int i = 1; i <= 6; i++) {
                int count = failedStudentsMap.getOrDefault(i, 0);
                model.addRow(new Object[]{i + " Subject" + (i > 1 ? "s" : ""), count});
            }
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }

    private JPanel createModernCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw shadow
                for (int i = 0; i < 3; i++) {
                    g2.setColor(new Color(0, 0, 0, 15 - (i * 5)));
                    g2.fillRoundRect(i, i, getWidth() - i, getHeight() - i, 20, 20);
                }
                
                // Draw card
                g2.setColor(CARD_COLOR);
                g2.fillRoundRect(0, 0, getWidth() - 3, getHeight() - 3, 20, 20);
                
                // Subtle border
                g2.setColor(new Color(0, 0, 0, 20));
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth() - 3, getHeight() - 3, 20, 20);
                
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        return card;
    }

    private JToggleButton createModernTab(String text, boolean selected) {
        JToggleButton tab = new JToggleButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (isSelected()) {
                    g2.setColor(PRIMARY_COLOR);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(Color.WHITE);
                } else {
                    g2.setColor(new Color(249, 250, 251));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(BORDER_COLOR);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                    g2.setColor(TEXT_SECONDARY);
                }
                
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                
                g2.dispose();
            }
        };
        tab.setFont(new Font("SansSerif", Font.PLAIN, 11));
        tab.setContentAreaFilled(false);
        tab.setBorderPainted(false);
        tab.setFocusPainted(false);
        tab.setCursor(new Cursor(Cursor.HAND_CURSOR));
        tab.setPreferredSize(new Dimension(45, 28)); // Will be overridden individually
        tab.setSelected(selected);
        return tab;
    }
    
    private JRadioButton createModernRadioButton(String text, boolean selected) {
        JRadioButton radio = new JRadioButton(text, selected);
        radio.setFont(new Font("SansSerif", Font.PLAIN, 14));
        radio.setOpaque(false);
        radio.setForeground(TEXT_PRIMARY);
        radio.setIcon(new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BORDER_COLOR);
                g2.drawOval(x, y, 16, 16);
                g2.dispose();
            }
            @Override
            public int getIconWidth() { return 18; }
            @Override
            public int getIconHeight() { return 18; }
        });
        radio.setSelectedIcon(new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PRIMARY_COLOR);
                g2.fillOval(x + 4, y + 4, 8, 8);
                g2.setColor(PRIMARY_COLOR);
                g2.drawOval(x, y, 16, 16);
                g2.dispose();
            }
            @Override
            public int getIconWidth() { return 18; }
            @Override
            public int getIconHeight() { return 18; }
        });
        return radio;
    }
    
    private void refreshSectionData() {
        // Reload components and filters for new section
        loadAvailableComponentsForSection(currentSectionId);
        initializeFilters();
        
        SwingUtilities.invokeLater(() -> {
            mainContentPanel.removeAll();
            
            JLabel loadingLabel = new JLabel("Loading section data...");
            loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
            loadingLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));
            loadingLabel.setForeground(TEXT_SECONDARY);
            mainContentPanel.add(loadingLabel, BorderLayout.CENTER);
            mainContentPanel.revalidate();
            mainContentPanel.repaint();
            
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    Thread.sleep(300);
                    return null;
                }
                
                @Override
                protected void done() {
                    mainContentPanel.removeAll();
                    createMainContent();
                    mainContentPanel.revalidate();
                    mainContentPanel.repaint();
                }
            };
            worker.execute();
        });
    }
    
    private void refreshDataOnly() {
        // Only refresh the data content, NOT the filter panel
        System.out.println("Refreshing data only with filters: " + selectedFilters.keySet());
        
        SwingUtilities.invokeLater(() -> {
            // Find the content wrapper and update only that
            Component[] components = mainContentPanel.getComponents();
            if (components.length > 0 && components[0] instanceof JPanel) {
                JPanel mainLayout = (JPanel) components[0];
                Component[] mainComponents = mainLayout.getComponents();
                
                // Find and update only the CENTER component (content), not WEST (filter)
                for (int i = 0; i < mainComponents.length; i++) {
                    Object constraints = ((BorderLayout) mainLayout.getLayout()).getConstraints(mainComponents[i]);
                    if (BorderLayout.CENTER.equals(constraints)) {
                        // Remove and recreate only the content area
                        mainLayout.remove(mainComponents[i]);
                        
                        AnalyzerDAO analyzerDAO = new AnalyzerDAO();
                        AnalyzerDAO.SectionAnalysisData analysisData = analyzerDAO.getSectionAnalysisWithFilters(
                            currentSectionId, 
                            com.sms.login.LoginScreen.currentUserId,
                            selectedFilters
                        );
                        
                        if (analysisData != null) {
                            // Create scrollable content wrapper
                            JPanel contentWrapper = new JPanel();
                            contentWrapper.setLayout(new BoxLayout(contentWrapper, BoxLayout.Y_AXIS));
                            contentWrapper.setOpaque(false);
                            contentWrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15));

                            // Section tabs
                            JPanel tabsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                            tabsPanel.setOpaque(false);
                            tabsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                            tabsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

                            ButtonGroup tabGroup = new ButtonGroup();
                            
                            if (availableSections == null || availableSections.isEmpty()) {
                                JLabel noSectionsLabel = new JLabel("No sections available");
                                noSectionsLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
                                noSectionsLabel.setForeground(TEXT_SECONDARY);
                                tabsPanel.add(noSectionsLabel);
                            } else {
                                for (int j = 0; j < availableSections.size(); j++) {
                                    SectionDAO.SectionInfo section = availableSections.get(j);
                                    JToggleButton tabButton = createModernTab(section.sectionName, section.id == currentSectionId);
                                    
                                    // Calculate width based on text length
                                    FontMetrics fm = tabButton.getFontMetrics(tabButton.getFont());
                                    int textWidth = fm.stringWidth(section.sectionName);
                                    int tabWidth = Math.max(45, textWidth + 16); // Min 45px, +16 for padding
                                    tabButton.setPreferredSize(new Dimension(tabWidth, 28));
                                    
                                    tabButton.addActionListener(e -> {
                                        currentSectionId = section.id;
                                        currentSectionName = section.sectionName;
                                        updateTabAppearance(tabsPanel, tabButton);
                                        refreshSectionData();
                                    });
                                    
                                    tabGroup.add(tabButton);
                                    tabsPanel.add(tabButton);
                                }
                            }
                            
                            contentWrapper.add(tabsPanel);
                            contentWrapper.add(Box.createVerticalStrut(5));

                            // PDF Export Button
                            JPanel exportPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                            exportPanel.setOpaque(false);
                            exportPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                            exportPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
                            JButton exportButton = createExportButton();
                            exportPanel.add(exportButton);
                            contentWrapper.add(exportPanel);
                            contentWrapper.add(Box.createVerticalStrut(5));

                            // Section Result Analysis at the TOP
                            JPanel sectionResultPanel = createSectionResultAnalysis(analysisData);
                            sectionResultPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                            contentWrapper.add(sectionResultPanel);
                            contentWrapper.add(Box.createVerticalStrut(15));

                            // ROW 1: Subject Analysis (FULL WIDTH)
                            JPanel subjectRow = new JPanel(new BorderLayout());
                            subjectRow.setOpaque(false);
                            subjectRow.setAlignmentX(Component.LEFT_ALIGNMENT);
                            subjectRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
                            
                            JPanel subjectPanel = createSubjectTable(analysisData.subjectAnalysisList);
                            subjectRow.add(subjectPanel, BorderLayout.CENTER);
                            
                            contentWrapper.add(subjectRow);
                            contentWrapper.add(Box.createVerticalStrut(15));

                            // ROW 2: Marks Analysis + Grade Distribution
                            JPanel row2 = new JPanel(new GridLayout(1, 2, 15, 0));
                            row2.setOpaque(false);
                            row2.setAlignmentX(Component.LEFT_ALIGNMENT);
                            row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
                            
                            JPanel chartCard = createModernCard();
                            chartCard.setLayout(new BorderLayout());
                            chartCard.add(createChart(analysisData), BorderLayout.CENTER);
                            
                            JPanel gradeDistCard = createModernCard();
                            gradeDistCard.setLayout(new BorderLayout());
                            gradeDistCard.add(createGradeDistributionChart(analyzerDAO), BorderLayout.CENTER);
                            
                            row2.add(chartCard);
                            row2.add(gradeDistCard);
                            
                            contentWrapper.add(row2);
                            contentWrapper.add(Box.createVerticalStrut(15));
                            
                            // ROW 3: Top 5 Students + Failed Analysis
                            JPanel row3 = new JPanel(new GridLayout(1, 2, 15, 0));
                            row3.setOpaque(false);
                            row3.setAlignmentX(Component.LEFT_ALIGNMENT);
                            row3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
                            
                            JPanel topStudentsCard = createModernCard();
                            topStudentsCard.setLayout(new BorderLayout());
                            topStudentsCard.add(createTopStudentsTable(analysisData.topStudents), BorderLayout.CENTER);
                            
                            JPanel failedStudentsCard = createModernCard();
                            failedStudentsCard.setLayout(new BorderLayout());
                            failedStudentsCard.add(createFailedStudentTable(analysisData.failedStudentsMap), BorderLayout.CENTER);
                            
                            row3.add(topStudentsCard);
                            row3.add(failedStudentsCard);
                            
                            contentWrapper.add(row3);
                            contentWrapper.add(Box.createVerticalStrut(15));
                            
                            // ROW 4: Student at Risk Panel
                            JPanel atRiskRow = new JPanel(new BorderLayout());
                            atRiskRow.setOpaque(false);
                            atRiskRow.setAlignmentX(Component.LEFT_ALIGNMENT);
                            atRiskRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
                            
                            JPanel atRiskCard = createModernCard();
                            atRiskCard.setLayout(new BorderLayout());
                            atRiskCard.add(createAtRiskStudentsPanel(analyzerDAO), BorderLayout.CENTER);
                            
                            atRiskRow.add(atRiskCard, BorderLayout.CENTER);
                            contentWrapper.add(atRiskRow);
                            contentWrapper.add(Box.createVerticalStrut(20));
                            
                            // Wrap content in JScrollPane
                            JScrollPane scrollPane = new JScrollPane(contentWrapper);
                            scrollPane.setBorder(BorderFactory.createEmptyBorder());
                            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
                            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
                            scrollPane.setOpaque(false);
                            scrollPane.getViewport().setOpaque(false);
                            
                            mainLayout.add(scrollPane, BorderLayout.CENTER);
                        }
                        
                        mainLayout.revalidate();
                        mainLayout.repaint();
                        break;
                    }
                }
            }
        });
    }
    
    private void updateTabAppearance(JPanel tabsPanel, JToggleButton selectedTab) {
        for (Component c : tabsPanel.getComponents()) {
            if (c instanceof JToggleButton) {
                c.repaint();
            }
        }
    }
    
    // ==================== Filter Methods ====================
    
    private void loadAvailableComponentsForSection(int sectionId) {
        availableComponents = new HashMap<>();
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            // Get all subjects configured for this section (even if no marks yet)
            // Plus all exam types that have marks data
            String query = 
                "SELECT DISTINCT sub.subject_name, sm.exam_type as exam_name " +
                "FROM section_subjects ss " +
                "INNER JOIN subjects sub ON ss.subject_id = sub.id " +
                "LEFT JOIN student_marks sm ON sm.subject_id = sub.id " +
                "LEFT JOIN students s ON sm.student_id = s.id AND s.section_id = ss.section_id " +
                "WHERE ss.section_id = ? " +
                "ORDER BY sub.subject_name, sm.exam_type";
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, sectionId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                String subjectName = rs.getString("subject_name");
                String examType = rs.getString("exam_name");
                
                availableComponents.putIfAbsent(subjectName, new HashSet<>());
                // Only add exam type if it's not null (subjects without marks will have null exam_type)
                if (examType != null && !examType.trim().isEmpty()) {
                    availableComponents.get(subjectName).add(examType);
                }
            }
            
            rs.close();
            ps.close();
            
            System.out.println("Loaded " + availableComponents.size() + " subjects for section " + sectionId);
            for (String subject : availableComponents.keySet()) {
                System.out.println("  Subject: " + subject + ", Components: " + availableComponents.get(subject));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Error loading available components: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void initializeFilters() {
        selectedFilters = new HashMap<>();
        // Select all by default (Overall)
        for (String subject : availableComponents.keySet()) {
            Set<String> components = new HashSet<>(availableComponents.get(subject));
            selectedFilters.put(subject, components);
        }
        System.out.println("Initialized filters with " + selectedFilters.size() + " subjects");
    }
    
    private boolean isAllSelected() {
        if (selectedFilters.size() != availableComponents.size()) return false;
        
        for (String subject : availableComponents.keySet()) {
            if (!selectedFilters.containsKey(subject)) return false;
            if (!selectedFilters.get(subject).equals(availableComponents.get(subject))) return false;
        }
        return true;
    }
    
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
        
        System.out.println("Creating filter panel with " + availableComponents.size() + " subjects");
        
        // Overall checkbox
        JCheckBox overallCheck = new JCheckBox("Overall (All)", true);
        overallCheck.setFont(new Font("SansSerif", Font.BOLD, 14));
        overallCheck.setBackground(Color.WHITE);
        overallCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        overallCheck.addActionListener(e -> {
            System.out.println("Overall checkbox clicked: " + overallCheck.isSelected());
            if (overallCheck.isSelected()) {
                initializeFilters(); // Select all
                refreshFilterCheckboxes(treePanel, overallCheck);
            } else {
                selectedFilters.clear(); // Deselect all
                refreshFilterCheckboxes(treePanel, overallCheck);
            }
            refreshDataOnly();
        });
        
        treePanel.add(overallCheck);
        treePanel.add(Box.createVerticalStrut(10));
        treePanel.add(new JSeparator());
        treePanel.add(Box.createVerticalStrut(10));
        
        // Subject and component checkboxes
        for (String subject : availableComponents.keySet()) {
            // Subject checkbox
            JCheckBox subjectCheck = new JCheckBox("+ " + subject, true);
            subjectCheck.setFont(new Font("SansSerif", Font.BOLD, 13));
            subjectCheck.setForeground(TEXT_PRIMARY);
            subjectCheck.setBackground(Color.WHITE);
            subjectCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            // Store component checkboxes for this subject
            java.util.List<JCheckBox> componentChecks = new java.util.ArrayList<>();
            
            final String currentSubject = subject;
            subjectCheck.addActionListener(e -> {
                boolean selected = subjectCheck.isSelected();
                // Select/deselect all components for this subject
                for (JCheckBox compCheck : componentChecks) {
                    compCheck.setSelected(selected);
                }
                
                if (selected) {
                    Set<String> components = new HashSet<>(availableComponents.get(currentSubject));
                    selectedFilters.put(currentSubject, components);
                } else {
                    selectedFilters.remove(currentSubject);
                }
                overallCheck.setSelected(isAllSelected());
                refreshDataOnly();
            });
            
            treePanel.add(subjectCheck);
            
            // Component checkboxes (indented)
            for (String component : availableComponents.get(subject)) {
                final String currentComponent = component;
                final String currentSubject2 = subject;
                JCheckBox compCheck = new JCheckBox("   - " + component, true);
                compCheck.setFont(new Font("SansSerif", Font.PLAIN, 12));
                compCheck.setBackground(Color.WHITE);
                compCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
                componentChecks.add(compCheck);
                
                compCheck.addActionListener(e -> {
                    if (compCheck.isSelected()) {
                        selectedFilters.putIfAbsent(currentSubject2, new HashSet<>());
                        selectedFilters.get(currentSubject2).add(currentComponent);
                    } else {
                        if (selectedFilters.containsKey(currentSubject2)) {
                            selectedFilters.get(currentSubject2).remove(currentComponent);
                            if (selectedFilters.get(currentSubject2).isEmpty()) {
                                selectedFilters.remove(currentSubject2);
                            }
                        }
                    }
                    // Update subject checkbox
                    boolean allSelected = componentChecks.stream().allMatch(JCheckBox::isSelected);
                    subjectCheck.setSelected(allSelected);
                    overallCheck.setSelected(isAllSelected());
                    refreshDataOnly();
                });
                
                treePanel.add(compCheck);
            }
            
            treePanel.add(Box.createVerticalStrut(5));
        }
        
        JScrollPane scrollPane = new JScrollPane(treePanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        scrollPane.setBackground(Color.WHITE);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setPreferredSize(new Dimension(280, 600));
        scrollPane.setMaximumSize(new Dimension(280, Integer.MAX_VALUE));
        
        filterCard.add(filterLabel, BorderLayout.NORTH);
        filterCard.add(scrollPane, BorderLayout.CENTER);
        
        return filterCard;
    }
    
    private void refreshFilterCheckboxes(JPanel treePanel, JCheckBox overallCheck) {
        treePanel.removeAll();
        treePanel.add(overallCheck);
        treePanel.add(Box.createVerticalStrut(10));
        treePanel.add(new JSeparator());
        treePanel.add(Box.createVerticalStrut(10));
        
        for (String subject : availableComponents.keySet()) {
            boolean subjectSelected = selectedFilters.containsKey(subject);
            JCheckBox subjectCheck = new JCheckBox("+ " + subject, subjectSelected);
            subjectCheck.setFont(new Font("SansSerif", Font.BOLD, 13));
            subjectCheck.setForeground(TEXT_PRIMARY);
            subjectCheck.setBackground(Color.WHITE);
            subjectCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            java.util.List<JCheckBox> componentChecks = new java.util.ArrayList<>();
            
            final String refreshSubject = subject;
            subjectCheck.addActionListener(e -> {
                boolean selected = subjectCheck.isSelected();
                for (JCheckBox compCheck : componentChecks) {
                    compCheck.setSelected(selected);
                }
                if (selected) {
                    selectedFilters.put(refreshSubject, new HashSet<>(availableComponents.get(refreshSubject)));
                } else {
                    selectedFilters.remove(refreshSubject);
                }
                overallCheck.setSelected(isAllSelected());
                refreshDataOnly();
            });
            
            treePanel.add(subjectCheck);
            
            for (String component : availableComponents.get(subject)) {
                final String refreshComponent = component;
                final String refreshSubject2 = subject;
                boolean compSelected = subjectSelected && selectedFilters.get(subject) != null && selectedFilters.get(subject).contains(component);
                JCheckBox compCheck = new JCheckBox("   - " + component, compSelected);
                compCheck.setFont(new Font("SansSerif", Font.PLAIN, 12));
                compCheck.setBackground(Color.WHITE);
                compCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
                componentChecks.add(compCheck);
                
                compCheck.addActionListener(e -> {
                    if (compCheck.isSelected()) {
                        selectedFilters.putIfAbsent(refreshSubject2, new HashSet<>());
                        selectedFilters.get(refreshSubject2).add(refreshComponent);
                    } else {
                        if (selectedFilters.containsKey(refreshSubject2)) {
                            selectedFilters.get(refreshSubject2).remove(refreshComponent);
                            if (selectedFilters.get(refreshSubject2).isEmpty()) {
                                selectedFilters.remove(refreshSubject2);
                            }
                        }
                    }
                    boolean allSelected = componentChecks.stream().allMatch(JCheckBox::isSelected);
                    subjectCheck.setSelected(allSelected);
                    overallCheck.setSelected(isAllSelected());
                    refreshDataOnly();
                });
                
                treePanel.add(compCheck);
            }
            
            treePanel.add(Box.createVerticalStrut(5));
        }
        
        treePanel.revalidate();
        treePanel.repaint();
    }
    
    private JPanel createGradeDistributionChart(AnalyzerDAO analyzerDAO) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        // Header
        JLabel titleLabel = new JLabel("📊 Grade Distribution");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Get grade distribution data
        List<AnalyzerDAO.GradeDistribution> gradeData = analyzerDAO.getGradeDistribution(currentSectionId, selectedFilters);
        
        // Create dataset
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (AnalyzerDAO.GradeDistribution grade : gradeData) {
            dataset.addValue(grade.count, "Students", grade.grade);
        }
        
        // Create chart
        JFreeChart chart = ChartFactory.createBarChart(
            null,
            "Grade",
            "Number of Students",
            dataset,
            org.jfree.chart.plot.PlotOrientation.VERTICAL,
            false,
            false,
            false
        );
        
        chart.setBackgroundPaint(Color.WHITE);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(new Color(220, 220, 220));
        plot.setDomainGridlinesVisible(false);
        plot.setOutlineVisible(false);
        
        // Customize renderer
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());
        renderer.setSeriesPaint(0, PRIMARY_COLOR);
        renderer.setShadowVisible(false);
        renderer.setMaximumBarWidth(0.1);
        
        // Customize axes
        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 11));
        domainAxis.setTickLabelPaint(TEXT_PRIMARY);
        domainAxis.setCategoryMargin(0.2);
        
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 11));
        rangeAxis.setTickLabelPaint(TEXT_PRIMARY);
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setOpaque(false);
        chartPanel.setBackground(Color.WHITE);
        chartPanel.setPreferredSize(new Dimension(400, 200));
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(chartPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createAtRiskStudentsPanel(AnalyzerDAO analyzerDAO) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        // Header
        JLabel titleLabel = new JLabel("⚠️ Students at Risk");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Get at-risk students data
        List<AnalyzerDAO.AtRiskStudent> atRiskStudents = analyzerDAO.getAtRiskStudents(currentSectionId, selectedFilters);
        
        String[] columns = {"Roll No.", "Name", "Percentage", "Risk Level", "Failed Subjects"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable table = new JTable(model);
        table.setRowHeight(35);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(249, 250, 251));
        table.getTableHeader().setForeground(TEXT_PRIMARY);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_COLOR));
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setSelectionBackground(SELECTION_COLOR);
        table.setSelectionForeground(TEXT_PRIMARY);
        
        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(200);
        
        // Custom renderer
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
                
                label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                    BorderFactory.createEmptyBorder(0, 8, 0, 8)
                ));
                
                if (isSelected) {
                    label.setBackground(SELECTION_COLOR);
                    label.setForeground(TEXT_PRIMARY);
                } else {
                    label.setBackground(row % 2 == 0 ? Color.WHITE : new Color(249, 250, 251));
                    label.setForeground(TEXT_PRIMARY);
                }
                
                if (column == 0 || column == 1) {
                    label.setHorizontalAlignment(JLabel.LEFT);
                } else {
                    label.setHorizontalAlignment(JLabel.CENTER);
                }
                
                if (column == 2) {
                    label.setFont(new Font("SansSerif", Font.BOLD, 12));
                    if (!isSelected && value != null) {
                        try {
                            double percentage = Double.parseDouble(value.toString().replace("%", ""));
                            if (percentage < 50) {
                                label.setForeground(DANGER_COLOR);
                            } else {
                                label.setForeground(WARNING_COLOR);
                            }
                        } catch (NumberFormatException e) {
                            // If parsing fails, just use default color
                        }
                    }
                } else if (column == 3) {
                    label.setFont(new Font("SansSerif", Font.BOLD, 12));
                    if (!isSelected) {
                        if (value.equals("Critical")) {
                            label.setForeground(DANGER_COLOR);
                        } else {
                            label.setForeground(WARNING_COLOR);
                        }
                    }
                }
                
                return label;
            }
        };
        
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
        
        // Populate table
        if (atRiskStudents != null && !atRiskStudents.isEmpty()) {
            for (AnalyzerDAO.AtRiskStudent student : atRiskStudents) {
                model.addRow(new Object[]{
                    student.rollNo,
                    student.name,
                    String.format("%.1f%%", student.percentage),
                    student.riskLevel,
                    student.failedSubjects
                });
            }
        } else {
            model.addRow(new Object[]{"—", "No students at risk", "—", "—", "—"});
        }
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setPreferredSize(new Dimension(800, 220));
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JButton createExportButton() {
        JButton exportButton = new JButton("📄 Export PDF");
        exportButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        exportButton.setForeground(Color.WHITE);
        exportButton.setBackground(PRIMARY_COLOR);
        exportButton.setFocusPainted(false);
        exportButton.setBorderPainted(false);
        exportButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        exportButton.setPreferredSize(new Dimension(140, 36));
        
        exportButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportButton.setBackground(PRIMARY_DARK);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportButton.setBackground(PRIMARY_COLOR);
            }
        });
        
        exportButton.addActionListener(e -> exportToPDF());
        
        return exportButton;
    }
    
    private void exportToPDF() {
        try {
            // Get analysis data
            AnalyzerDAO analyzerDAO = new AnalyzerDAO();
            AnalyzerDAO.SectionAnalysisData analysisData = analyzerDAO.getSectionAnalysisWithFilters(
                currentSectionId,
                com.sms.login.LoginScreen.currentUserId,
                selectedFilters
            );
            
            if (analysisData == null) {
                JOptionPane.showMessageDialog(this, 
                    "No data available to export.", 
                    "Export Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Show file chooser
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save PDF Report");
            fileChooser.setSelectedFile(new java.io.File("Section_Analysis_" + currentSectionName.replace(" ", "_") + ".pdf"));
            
            int userSelection = fileChooser.showSaveDialog(this);
            if (userSelection != JFileChooser.APPROVE_OPTION) {
                return;
            }
            
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".pdf")) {
                filePath += ".pdf";
            }
            
            // Create PDF
            com.itextpdf.text.Document document = new com.itextpdf.text.Document(com.itextpdf.text.PageSize.A4, 50, 50, 50, 50);
            com.itextpdf.text.pdf.PdfWriter.getInstance(document, new java.io.FileOutputStream(filePath));
            document.open();
            
            // Title
            com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Paragraph title = new com.itextpdf.text.Paragraph("Section Analysis Report: " + currentSectionName, titleFont);
            title.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            // Section Result Summary
            com.itextpdf.text.Font headingFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 14, com.itextpdf.text.Font.BOLD);
            document.add(new com.itextpdf.text.Paragraph("Section Result Summary", headingFont));
            document.add(new com.itextpdf.text.Paragraph(" "));
            
            com.itextpdf.text.pdf.PdfPTable summaryTable = new com.itextpdf.text.pdf.PdfPTable(5);
            summaryTable.setWidthPercentage(100);
            summaryTable.addCell("Total Students");
            summaryTable.addCell("Pass");
            summaryTable.addCell("Fail");
            summaryTable.addCell("Pass %");
            summaryTable.addCell("Fail %");
            
            double passPercentage = (analysisData.totalStudents > 0) ? 
                (analysisData.passStudents * 100.0 / analysisData.totalStudents) : 0;
            double failPercentage = (analysisData.totalStudents > 0) ? 
                (analysisData.failStudents * 100.0 / analysisData.totalStudents) : 0;
            
            summaryTable.addCell(String.valueOf(analysisData.totalStudents));
            summaryTable.addCell(String.valueOf(analysisData.passStudents));
            summaryTable.addCell(String.valueOf(analysisData.failStudents));
            summaryTable.addCell(String.format("%.1f%%", passPercentage));
            summaryTable.addCell(String.format("%.1f%%", failPercentage));
            
            document.add(summaryTable);
            document.add(new com.itextpdf.text.Paragraph(" "));
            document.add(new com.itextpdf.text.Paragraph(" "));
            
            // Subject Analysis
            document.add(new com.itextpdf.text.Paragraph("Subject Analysis", headingFont));
            document.add(new com.itextpdf.text.Paragraph(" "));
            
            com.itextpdf.text.pdf.PdfPTable subjectTable = new com.itextpdf.text.pdf.PdfPTable(7);
            subjectTable.setWidthPercentage(100);
            subjectTable.addCell("Subject");
            subjectTable.addCell("Total Students");
            subjectTable.addCell("Pass");
            subjectTable.addCell("DC");
            subjectTable.addCell("FC");
            subjectTable.addCell("SC");
            subjectTable.addCell("Fail");
            
            for (AnalyzerDAO.SubjectAnalysis subject : analysisData.subjectAnalysisList) {
                subjectTable.addCell(subject.subjectName);
                subjectTable.addCell(String.valueOf(subject.totalStudents));
                subjectTable.addCell(String.valueOf(subject.passCount));
                subjectTable.addCell(String.valueOf(subject.distinctionCount));
                subjectTable.addCell(String.valueOf(subject.firstClassCount));
                subjectTable.addCell(String.valueOf(subject.secondClassCount));
                subjectTable.addCell(String.valueOf(subject.failCount));
            }
            
            document.add(subjectTable);
            document.add(new com.itextpdf.text.Paragraph(" "));
            document.add(new com.itextpdf.text.Paragraph(" "));
            
            // Top 5 Students
            document.add(new com.itextpdf.text.Paragraph("Top 5 Students", headingFont));
            document.add(new com.itextpdf.text.Paragraph(" "));
            
            com.itextpdf.text.pdf.PdfPTable topStudentsTable = new com.itextpdf.text.pdf.PdfPTable(4);
            topStudentsTable.setWidthPercentage(100);
            topStudentsTable.addCell("Rank");
            topStudentsTable.addCell("Roll No.");
            topStudentsTable.addCell("Name");
            topStudentsTable.addCell("Percentage");
            
            int rank = 1;
            for (AnalyzerDAO.TopStudent student : analysisData.topStudents) {
                topStudentsTable.addCell(String.valueOf(rank++));
                topStudentsTable.addCell(student.rollNumber);
                topStudentsTable.addCell(student.name);
                topStudentsTable.addCell(String.format("%.1f%%", student.percentage));
            }
            
            document.add(topStudentsTable);
            document.add(new com.itextpdf.text.Paragraph(" "));
            document.add(new com.itextpdf.text.Paragraph(" "));
            
            // At-Risk Students
            List<AnalyzerDAO.AtRiskStudent> atRiskStudents = analyzerDAO.getAtRiskStudents(currentSectionId, selectedFilters);
            if (atRiskStudents != null && !atRiskStudents.isEmpty()) {
                document.add(new com.itextpdf.text.Paragraph("Students at Risk", headingFont));
                document.add(new com.itextpdf.text.Paragraph(" "));
                
                com.itextpdf.text.pdf.PdfPTable atRiskTable = new com.itextpdf.text.pdf.PdfPTable(5);
                atRiskTable.setWidthPercentage(100);
                atRiskTable.addCell("Roll No.");
                atRiskTable.addCell("Name");
                atRiskTable.addCell("Percentage");
                atRiskTable.addCell("Risk Level");
                atRiskTable.addCell("Failed Subjects");
                
                for (AnalyzerDAO.AtRiskStudent student : atRiskStudents) {
                    atRiskTable.addCell(String.valueOf(student.rollNo));
                    atRiskTable.addCell(student.name);
                    atRiskTable.addCell(String.format("%.1f%%", student.percentage));
                    atRiskTable.addCell(student.riskLevel);
                    atRiskTable.addCell(student.failedSubjects);
                }
                
                document.add(atRiskTable);
            }
            
            document.close();
            
            JOptionPane.showMessageDialog(this,
                "PDF report exported successfully to:\n" + filePath,
                "Export Successful",
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error exporting PDF: " + ex.getMessage(),
                "Export Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void closePanel() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }
}
