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
    private JComboBox<String> sectionDropdown;
    
    // Filter-related fields
    private Map<String, Set<String>> selectedFilters; // Track selected subject-component combinations
    private Map<String, Set<String>> availableComponents; // All available components per subject
    private JPanel filterCard;

    // Constructor for standalone dialog (backward compatibility)
    public SectionAnalyzer(JFrame parent, HashMap<String, ArrayList<Student>> sectionStudents) {
        this(parent, sectionStudents, null);
        if (parent != null) {
            JDialog dialog = new JDialog(parent, "Section Performance Analyzer", true);
            dialog.setSize(1000, 750);
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
            // Check if a specific section was passed in the constructor
            String selectedSectionName = null;
            if (this.sectionStudents != null && !this.sectionStudents.isEmpty()) {
                selectedSectionName = this.sectionStudents.keySet().iterator().next();
            }
            
            // Find the matching section from database or default to first
            boolean sectionFound = false;
            if (selectedSectionName != null) {
                for (SectionDAO.SectionInfo section : sections) {
                    if (section.sectionName.equals(selectedSectionName)) {
                        this.currentSectionId = section.id;
                        this.currentSectionName = section.sectionName;
                        sectionFound = true;
                        break;
                    }
                }
            }
            
            // If not found or no selection, use first section
            if (!sectionFound) {
                this.currentSectionId = sections.get(0).id;
                this.currentSectionName = sections.get(0).sectionName;
            }
            
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
        headerCard.setPreferredSize(new Dimension(0, 150));

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

        // Section dropdown selector
        JPanel dropdownPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        dropdownPanel.setOpaque(false);
        
        JLabel sectionLabel = new JLabel("Select Section:");
        sectionLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        sectionLabel.setForeground(TEXT_PRIMARY);
        dropdownPanel.add(sectionLabel);
        
        // Create dropdown with section names
        String[] sectionNames = availableSections.stream()
            .map(s -> s.sectionName)
            .toArray(String[]::new);
        sectionDropdown = new JComboBox<>(sectionNames);
        sectionDropdown.setFont(new Font("SansSerif", Font.PLAIN, 14));
        sectionDropdown.setPreferredSize(new Dimension(200, 35));
        sectionDropdown.setBackground(Color.WHITE);
        sectionDropdown.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        sectionDropdown.setFocusable(true);
        sectionDropdown.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Set currently selected section
        if (currentSectionName != null) {
            sectionDropdown.setSelectedItem(currentSectionName);
        }
        
        // Add listener to handle section selection
        sectionDropdown.addActionListener(e -> {
            String selectedSection = (String) sectionDropdown.getSelectedItem();
            if (selectedSection != null && !selectedSection.equals(currentSectionName)) {
                // Find the section info for the selected name
                for (SectionDAO.SectionInfo section : availableSections) {
                    if (section.sectionName.equals(selectedSection)) {
                        currentSectionId = section.id;
                        currentSectionName = section.sectionName;
                        refreshSectionData();
                        break;
                    }
                }
            }
        });
        
        dropdownPanel.add(sectionDropdown);

        // Create center panel for radio and dropdown
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.add(radioPanel);
        centerPanel.add(dropdownPanel);

        headerCard.add(topPanel, BorderLayout.NORTH);
        headerCard.add(centerPanel, BorderLayout.CENTER);
        
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
                int tabWidth = Math.min(Math.max(40, textWidth + 8), 70); // Min 40px, +8 padding, Max 70px
                tabButton.setPreferredSize(new Dimension(tabWidth, 24));
                
                // Fix closure issue - create final copies for lambda
                final int sectionId = section.id;
                final String sectionName = section.sectionName;
                
                tabButton.addActionListener(e -> {
                    currentSectionId = sectionId;  // Use final copy
                    currentSectionName = sectionName;  // Use final copy
                    updateTabAppearance(tabsPanel, tabButton);
                    refreshSectionData();
                });
                
                tabGroup.add(tabButton);
                tabsPanel.add(tabButton);
            }
        }
        
        // Wrap tabs in horizontal scroll pane
        JScrollPane tabsScrollPane = new JScrollPane(tabsPanel);
        tabsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tabsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        tabsScrollPane.setBorder(BorderFactory.createEmptyBorder());
        tabsScrollPane.setOpaque(false);
        tabsScrollPane.getViewport().setOpaque(false);
        tabsScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        tabsScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        contentWrapper.add(tabsScrollPane);
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
        subjectRow.setMaximumSize(new Dimension(935, 240));
        
        JPanel subjectPanel = createSubjectTable(analysisData.subjectAnalysisList);
        subjectRow.add(subjectPanel, BorderLayout.CENTER);
        
        contentWrapper.add(subjectRow);
        contentWrapper.add(Box.createVerticalStrut(15));

        // ROW 2: Marks Analysis + Grade Distribution
        JPanel row2 = new JPanel(new GridLayout(1, 2, 15, 0));
        row2.setOpaque(false);
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);
        row2.setMaximumSize(new Dimension(935, 260));
        
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
        row3.setMaximumSize(new Dimension(935, 240));
        
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
        atRiskRow.setMaximumSize(new Dimension(935, 300));
        
        JPanel atRiskCard = createModernCard();
        atRiskCard.setLayout(new BorderLayout());
        atRiskCard.add(createAtRiskStudentsPanel(analyzerDAO), BorderLayout.CENTER);
        
        atRiskRow.add(atRiskCard, BorderLayout.CENTER);
        contentWrapper.add(atRiskRow);
        contentWrapper.add(Box.createVerticalStrut(15));
        
        // ROW 5: All Students Ranking Table
        JPanel rankingRow = new JPanel(new BorderLayout());
        rankingRow.setOpaque(false);
        rankingRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        rankingRow.setMaximumSize(new Dimension(935, 400));
        
        JPanel rankingCard = createModernCard();
        rankingCard.setLayout(new BorderLayout());
        rankingCard.add(createAllStudentsRankingTable(analyzerDAO), BorderLayout.CENTER);
        
        rankingRow.add(rankingCard, BorderLayout.CENTER);
        contentWrapper.add(rankingRow);
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
        panel.setMaximumSize(new Dimension(935, 120));
        
        // Header
        JLabel titleLabel = new JLabel("📊 Section Result Analysis");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Create metric cards in a row
        JPanel metricsPanel = new JPanel(new GridLayout(1, 5, 12, 0));
        metricsPanel.setOpaque(false);
        metricsPanel.setMaximumSize(new Dimension(935, 70));
        
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
        card.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        card.setPreferredSize(new Dimension(130, 60));
        
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
                            
                            // Wrap tabs in horizontal scroll pane
                            JScrollPane tabsScrollPane = new JScrollPane(tabsPanel);
                            tabsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                            tabsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
                            tabsScrollPane.setBorder(BorderFactory.createEmptyBorder());
                            tabsScrollPane.setOpaque(false);
                            tabsScrollPane.getViewport().setOpaque(false);
                            tabsScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
                            tabsScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
                            
                            contentWrapper.add(tabsScrollPane);
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
                            subjectRow.setMaximumSize(new Dimension(935, 240));
                            
                            JPanel subjectPanel = createSubjectTable(analysisData.subjectAnalysisList);
                            subjectRow.add(subjectPanel, BorderLayout.CENTER);
                            
                            contentWrapper.add(subjectRow);
                            contentWrapper.add(Box.createVerticalStrut(15));

                            // ROW 2: Marks Analysis + Grade Distribution
                            JPanel row2 = new JPanel(new GridLayout(1, 2, 15, 0));
                            row2.setOpaque(false);
                            row2.setAlignmentX(Component.LEFT_ALIGNMENT);
                            row2.setMaximumSize(new Dimension(935, 260));
                            
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
                            row3.setMaximumSize(new Dimension(935, 240));
                            
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
                            atRiskRow.setMaximumSize(new Dimension(935, 300));
                            
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
                "SELECT DISTINCT sub.subject_name, et.exam_name " +
                "FROM section_subjects ss " +
                "INNER JOIN subjects sub ON ss.subject_id = sub.id " +
                "LEFT JOIN entered_exam_marks sm ON sm.subject_id = sub.id " +
                "LEFT JOIN students s ON sm.student_id = s.id AND s.section_id = ss.section_id " +
                "LEFT JOIN exam_types et ON sm.exam_type_id = et.id " +
                "WHERE ss.section_id = ? " +
                "ORDER BY sub.subject_name, et.exam_name";
            
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
            
            // Create PDF with LANDSCAPE orientation for wide tables
            com.itextpdf.text.Document document = new com.itextpdf.text.Document(com.itextpdf.text.PageSize.A4.rotate(), 30, 30, 30, 30);
            com.itextpdf.text.pdf.PdfWriter writer = com.itextpdf.text.pdf.PdfWriter.getInstance(document, new java.io.FileOutputStream(filePath));
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
                    document.add(new com.itextpdf.text.Paragraph(" "));
                }
            } catch (Exception ex) {
                // If logo not found, continue without it
                System.err.println("Warning: Could not load logo: " + ex.getMessage());
            }
            
            // Modern Styled Fonts
            com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 22, com.itextpdf.text.Font.BOLD, new com.itextpdf.text.BaseColor(79, 70, 229));
            com.itextpdf.text.Font headingFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 16, com.itextpdf.text.Font.BOLD, new com.itextpdf.text.BaseColor(17, 24, 39));
            com.itextpdf.text.Font subheadingFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12, com.itextpdf.text.Font.BOLD, new com.itextpdf.text.BaseColor(55, 65, 81));
            com.itextpdf.text.Font normalFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.NORMAL, new com.itextpdf.text.BaseColor(55, 65, 81));
            com.itextpdf.text.Font smallFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8, com.itextpdf.text.Font.NORMAL, new com.itextpdf.text.BaseColor(107, 114, 128));
            
            // Modern Header with Border
            com.itextpdf.text.Paragraph title = new com.itextpdf.text.Paragraph("📊 Section Analysis Report", titleFont);
            title.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            title.setSpacingAfter(5);
            document.add(title);
            
            com.itextpdf.text.Paragraph subtitle = new com.itextpdf.text.Paragraph("Section: " + currentSectionName + " | Generated: " + new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a").format(new java.util.Date()), normalFont);
            subtitle.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(15);
            document.add(subtitle);
            
            // Add separator line
            com.itextpdf.text.pdf.draw.LineSeparator line = new com.itextpdf.text.pdf.draw.LineSeparator();
            line.setLineColor(new com.itextpdf.text.BaseColor(229, 231, 235));
            document.add(new com.itextpdf.text.Chunk(line));
            document.add(new com.itextpdf.text.Paragraph(" "));
            
            // ==== SECTION 1: OVERVIEW CARDS ====
            document.add(new com.itextpdf.text.Paragraph("📈 Overview", headingFont));
            document.add(new com.itextpdf.text.Paragraph(" "));
            
            // Create modern styled summary cards in a table
            com.itextpdf.text.pdf.PdfPTable overviewTable = new com.itextpdf.text.pdf.PdfPTable(5);
            overviewTable.setWidthPercentage(100);
            overviewTable.setSpacingBefore(10);
            overviewTable.setSpacingAfter(15);
            
            double passPercentage = (analysisData.totalStudents > 0) ? 
                (analysisData.passStudents * 100.0 / analysisData.totalStudents) : 0;
            double failPercentage = (analysisData.totalStudents > 0) ? 
                (analysisData.failStudents * 100.0 / analysisData.totalStudents) : 0;
            
            // Style cells with background colors
            com.itextpdf.text.BaseColor headerBg = new com.itextpdf.text.BaseColor(99, 102, 241);
            com.itextpdf.text.BaseColor cellBg = new com.itextpdf.text.BaseColor(249, 250, 251);
            
            // Headers with colored background
            com.itextpdf.text.pdf.PdfPCell headerCell1 = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase("Total Students", new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD, com.itextpdf.text.BaseColor.WHITE)));
            headerCell1.setBackgroundColor(headerBg);
            headerCell1.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            headerCell1.setPadding(8);
            overviewTable.addCell(headerCell1);
            
            com.itextpdf.text.pdf.PdfPCell headerCell2 = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase("Pass", new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD, com.itextpdf.text.BaseColor.WHITE)));
            headerCell2.setBackgroundColor(headerBg);
            headerCell2.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            headerCell2.setPadding(8);
            overviewTable.addCell(headerCell2);
            
            com.itextpdf.text.pdf.PdfPCell headerCell3 = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase("Fail", new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD, com.itextpdf.text.BaseColor.WHITE)));
            headerCell3.setBackgroundColor(headerBg);
            headerCell3.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            headerCell3.setPadding(8);
            overviewTable.addCell(headerCell3);
            
            com.itextpdf.text.pdf.PdfPCell headerCell4 = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase("Pass %", new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD, com.itextpdf.text.BaseColor.WHITE)));
            headerCell4.setBackgroundColor(headerBg);
            headerCell4.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            headerCell4.setPadding(8);
            overviewTable.addCell(headerCell4);
            
            com.itextpdf.text.pdf.PdfPCell headerCell5 = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase("Fail %", new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD, com.itextpdf.text.BaseColor.WHITE)));
            headerCell5.setBackgroundColor(headerBg);
            headerCell5.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            headerCell5.setPadding(8);
            overviewTable.addCell(headerCell5);
            
            // Data cells
            addStyledCell(overviewTable, String.valueOf(analysisData.totalStudents), cellBg, true);
            addStyledCell(overviewTable, String.valueOf(analysisData.passStudents), new com.itextpdf.text.BaseColor(220, 252, 231), true);
            addStyledCell(overviewTable, String.valueOf(analysisData.failStudents), new com.itextpdf.text.BaseColor(254, 226, 226), true);
            addStyledCell(overviewTable, String.format("%.1f%%", passPercentage), new com.itextpdf.text.BaseColor(220, 252, 231), true);
            addStyledCell(overviewTable, String.format("%.1f%%", failPercentage), new com.itextpdf.text.BaseColor(254, 226, 226), true);
            
            document.add(overviewTable);
            document.add(new com.itextpdf.text.Paragraph(" "));
            
            // ==== SECTION 2: SUBJECT ANALYSIS ====
            document.add(new com.itextpdf.text.Paragraph("📚 Subject-wise Performance", headingFont));
            document.add(new com.itextpdf.text.Paragraph(" "));
            
            com.itextpdf.text.pdf.PdfPTable subjectTable = new com.itextpdf.text.pdf.PdfPTable(7);
            subjectTable.setWidthPercentage(100);
            subjectTable.setSpacingBefore(10);
            subjectTable.setSpacingAfter(15);
            
            com.itextpdf.text.BaseColor subjectHeaderBg = new com.itextpdf.text.BaseColor(99, 102, 241);
            com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.BOLD, com.itextpdf.text.BaseColor.WHITE);
            
            // Headers
            String[] subHeaders = {"Subject", "Total", "Pass", "DC", "FC", "SC", "Fail"};
            for (String header : subHeaders) {
                com.itextpdf.text.pdf.PdfPCell hCell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(header, headerFont));
                hCell.setBackgroundColor(subjectHeaderBg);
                hCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                hCell.setPadding(6);
                subjectTable.addCell(hCell);
            }
            
            // Data rows with alternating colors
            com.itextpdf.text.BaseColor row1Bg = new com.itextpdf.text.BaseColor(255, 255, 255);
            com.itextpdf.text.BaseColor row2Bg = new com.itextpdf.text.BaseColor(249, 250, 251);
            int rowIdx = 0;
            
            for (AnalyzerDAO.SubjectAnalysis subject : analysisData.subjectAnalysisList) {
                com.itextpdf.text.BaseColor rowBg = (rowIdx++ % 2 == 0) ? row1Bg : row2Bg;
                addStyledCell(subjectTable, subject.subjectName, rowBg, false);
                addStyledCell(subjectTable, String.valueOf(subject.totalStudents), rowBg, true);
                addStyledCell(subjectTable, String.valueOf(subject.passCount), new com.itextpdf.text.BaseColor(220, 252, 231), true);
                addStyledCell(subjectTable, String.valueOf(subject.distinctionCount), new com.itextpdf.text.BaseColor(254, 249, 195), true);
                addStyledCell(subjectTable, String.valueOf(subject.firstClassCount), new com.itextpdf.text.BaseColor(254, 249, 195), true);
                addStyledCell(subjectTable, String.valueOf(subject.secondClassCount), new com.itextpdf.text.BaseColor(254, 249, 195), true);
                addStyledCell(subjectTable, String.valueOf(subject.failCount), new com.itextpdf.text.BaseColor(254, 226, 226), true);
            }
            
            document.add(subjectTable);
            document.add(new com.itextpdf.text.Paragraph(" "));
            
            // ==== SECTION 3: TOP 5 STUDENTS ====
            document.add(new com.itextpdf.text.Paragraph("🏆 Top 5 Students", headingFont));
            document.add(new com.itextpdf.text.Paragraph(" "));
            
            com.itextpdf.text.pdf.PdfPTable topStudentsTable = new com.itextpdf.text.pdf.PdfPTable(4);
            topStudentsTable.setWidthPercentage(100);
            topStudentsTable.setSpacingBefore(10);
            topStudentsTable.setSpacingAfter(15);
            
            // Headers
            String[] topHeaders = {"Rank", "Roll No.", "Name", "Percentage"};
            for (String header : topHeaders) {
                com.itextpdf.text.pdf.PdfPCell hCell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(header, headerFont));
                hCell.setBackgroundColor(subjectHeaderBg);
                hCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                hCell.setPadding(6);
                topStudentsTable.addCell(hCell);
            }
            
            // Data with medal colors for top 3
            int rank = 1;
            for (AnalyzerDAO.TopStudent student : analysisData.topStudents) {
                com.itextpdf.text.BaseColor rankBg = row1Bg;
                if (rank == 1) rankBg = new com.itextpdf.text.BaseColor(255, 215, 0);  // Gold
                else if (rank == 2) rankBg = new com.itextpdf.text.BaseColor(192, 192, 192);  // Silver
                else if (rank == 3) rankBg = new com.itextpdf.text.BaseColor(205, 127, 50);  // Bronze
                else rankBg = (rank % 2 == 0) ? row1Bg : row2Bg;
                
                addStyledCell(topStudentsTable, String.valueOf(rank), rankBg, true);
                addStyledCell(topStudentsTable, student.rollNumber, rankBg, true);
                addStyledCell(topStudentsTable, student.name, rankBg, false);
                addStyledCell(topStudentsTable, String.format("%.1f%%", student.percentage), rankBg, true);
                rank++;
            }
            
            document.add(topStudentsTable);
            document.add(new com.itextpdf.text.Paragraph(" "));
            
            // ==== SECTION 4: GRADE DISTRIBUTION ====
            document.add(new com.itextpdf.text.Paragraph("📊 Grade Distribution", headingFont));
            document.add(new com.itextpdf.text.Paragraph(" "));
            
            List<AnalyzerDAO.GradeDistribution> gradeDistribution = analyzerDAO.getGradeDistribution(currentSectionId, selectedFilters);
            if (gradeDistribution != null && !gradeDistribution.isEmpty()) {
                com.itextpdf.text.pdf.PdfPTable gradeTable = new com.itextpdf.text.pdf.PdfPTable(2);
                gradeTable.setWidthPercentage(60);
                gradeTable.setSpacingBefore(10);
                gradeTable.setSpacingAfter(15);
                
                // Headers
                String[] gradeHeaders = {"Grade", "Count"};
                for (String header : gradeHeaders) {
                    com.itextpdf.text.pdf.PdfPCell hCell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(header, headerFont));
                    hCell.setBackgroundColor(subjectHeaderBg);
                    hCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                    hCell.setPadding(6);
                    gradeTable.addCell(hCell);
                }
                
                // Data with color coding
                rowIdx = 0;
                for (AnalyzerDAO.GradeDistribution gradeData : gradeDistribution) {
                    com.itextpdf.text.BaseColor gradeBg = row1Bg;
                    if (gradeData.grade.equals("O") || gradeData.grade.equals("A+")) {
                        gradeBg = new com.itextpdf.text.BaseColor(220, 252, 231);  // Green for excellent
                    } else if (gradeData.grade.equals("F")) {
                        gradeBg = new com.itextpdf.text.BaseColor(254, 226, 226);  // Red for fail
                    } else {
                        gradeBg = (rowIdx++ % 2 == 0) ? row1Bg : row2Bg;
                    }
                    
                    addStyledCell(gradeTable, gradeData.grade, gradeBg, true);
                    addStyledCell(gradeTable, String.valueOf(gradeData.count), gradeBg, true);
                }
                
                document.add(gradeTable);
            }
            document.add(new com.itextpdf.text.Paragraph(" "));
            
            // ==== SECTION 5: FAILED ANALYSIS ====
            if (analysisData.failedStudentsMap != null && !analysisData.failedStudentsMap.isEmpty()) {
                document.add(new com.itextpdf.text.Paragraph("⚠️ Failed Students Analysis", headingFont));
                document.add(new com.itextpdf.text.Paragraph(" "));
                
                com.itextpdf.text.pdf.PdfPTable failedTable = new com.itextpdf.text.pdf.PdfPTable(2);
                failedTable.setWidthPercentage(60);
                failedTable.setSpacingBefore(10);
                failedTable.setSpacingAfter(15);
                
                // Headers
                String[] failedHeaders = {"No. of Failed Subjects", "Student Count"};
                for (String header : failedHeaders) {
                    com.itextpdf.text.pdf.PdfPCell hCell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(header, headerFont));
                    hCell.setBackgroundColor(new com.itextpdf.text.BaseColor(239, 68, 68));  // Red header
                    hCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                    hCell.setPadding(6);
                    failedTable.addCell(hCell);
                }
                
                // Data rows
                rowIdx = 0;
                for (Map.Entry<Integer, Integer> entry : analysisData.failedStudentsMap.entrySet()) {
                    com.itextpdf.text.BaseColor rowBg = (rowIdx++ % 2 == 0) ? row1Bg : row2Bg;
                    addStyledCell(failedTable, String.valueOf(entry.getKey()), rowBg, true);
                    addStyledCell(failedTable, String.valueOf(entry.getValue()), rowBg, true);
                }
                
                document.add(failedTable);
            }
            document.add(new com.itextpdf.text.Paragraph(" "));
            
            // ==== SECTION 6: AT-RISK STUDENTS ====
            List<AnalyzerDAO.AtRiskStudent> atRiskStudents = analyzerDAO.getAtRiskStudents(currentSectionId, selectedFilters);
            if (atRiskStudents != null && !atRiskStudents.isEmpty()) {
                document.add(new com.itextpdf.text.Paragraph("🚨 Students at Risk", headingFont));
                document.add(new com.itextpdf.text.Paragraph(" "));
                
                com.itextpdf.text.pdf.PdfPTable atRiskTable = new com.itextpdf.text.pdf.PdfPTable(5);
                atRiskTable.setWidthPercentage(100);
                atRiskTable.setSpacingBefore(10);
                atRiskTable.setSpacingAfter(15);
                
                // Headers
                String[] riskHeaders = {"Roll No.", "Name", "Percentage", "Risk Level", "Failed Subjects"};
                for (String header : riskHeaders) {
                    com.itextpdf.text.pdf.PdfPCell hCell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(header, headerFont));
                    hCell.setBackgroundColor(new com.itextpdf.text.BaseColor(251, 146, 60));  // Orange header
                    hCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                    hCell.setPadding(6);
                    atRiskTable.addCell(hCell);
                }
                
                // Data rows with risk level colors
                for (AnalyzerDAO.AtRiskStudent student : atRiskStudents) {
                    com.itextpdf.text.BaseColor rowBg = row1Bg;
                    if (student.riskLevel.toLowerCase().contains("high")) {
                        rowBg = new com.itextpdf.text.BaseColor(254, 226, 226);  // Red for high risk
                    } else if (student.riskLevel.toLowerCase().contains("moderate")) {
                        rowBg = new com.itextpdf.text.BaseColor(254, 249, 195);  // Yellow for moderate
                    }
                    
                    addStyledCell(atRiskTable, String.valueOf(student.rollNo), rowBg, true);
                    addStyledCell(atRiskTable, student.name, rowBg, false);
                    addStyledCell(atRiskTable, String.format("%.1f%%", student.percentage), rowBg, true);
                    addStyledCell(atRiskTable, student.riskLevel, rowBg, true);
                    addStyledCell(atRiskTable, student.failedSubjects, rowBg, false);
                }
                
                document.add(atRiskTable);
            }
            document.add(new com.itextpdf.text.Paragraph(" "));
            
            // ==== SECTION 7: DETAILED RANKING TABLE ====
            AnalyzerDAO.DetailedRankingData rankingData = analyzerDAO.getDetailedStudentRanking(currentSectionId, selectedFilters);
            if (rankingData != null && !rankingData.students.isEmpty()) {
                // Add page break before ranking table
                document.newPage();
                
                document.add(new com.itextpdf.text.Paragraph("📋 Detailed Student Ranking", headingFont));
                document.add(new com.itextpdf.text.Paragraph("Complete performance breakdown with subject-wise and exam-wise marks", subheadingFont));
                document.add(new com.itextpdf.text.Paragraph(" "));
                
                // Build dynamic columns for the hierarchical table
                int columnCount = 3; // Rank, Roll No, Name
                for (AnalyzerDAO.SubjectInfoDetailed subject : rankingData.subjects) {
                    columnCount += subject.examTypes.size() + 1; // exam types + total
                }
                columnCount += 4; // Overall Total, Percentage, Grade, CGPA
                
                com.itextpdf.text.pdf.PdfPTable rankingTable = new com.itextpdf.text.pdf.PdfPTable(columnCount);
                rankingTable.setWidthPercentage(100);
                rankingTable.setSpacingBefore(10);
                rankingTable.setHeaderRows(2); // 2-row header for hierarchical structure
                
                // Colors
                com.itextpdf.text.BaseColor primaryHeaderBg = new com.itextpdf.text.BaseColor(79, 70, 229);  // Primary purple
                com.itextpdf.text.BaseColor secondaryHeaderBg = new com.itextpdf.text.BaseColor(99, 102, 241);  // Secondary blue
                com.itextpdf.text.Font header1Font = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.BOLD, com.itextpdf.text.BaseColor.WHITE);
                com.itextpdf.text.Font header2Font = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8, com.itextpdf.text.Font.BOLD, com.itextpdf.text.BaseColor.WHITE);
                
                // ==== ROW 1: Subject Headers with colspan ====
                // Student Info header spanning 3 columns (Rank, Roll, Name)
                com.itextpdf.text.pdf.PdfPCell studentInfoCell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase("Student Info", header1Font));
                studentInfoCell.setColspan(3);
                studentInfoCell.setBackgroundColor(primaryHeaderBg);
                studentInfoCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                studentInfoCell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
                studentInfoCell.setPadding(6);
                rankingTable.addCell(studentInfoCell);
                
                // Subject headers spanning their exam types + total column
                for (AnalyzerDAO.SubjectInfoDetailed subject : rankingData.subjects) {
                    int subjectColspan = subject.examTypes.size() + 1; // exam types + total
                    com.itextpdf.text.pdf.PdfPCell subjectCell = new com.itextpdf.text.pdf.PdfPCell(
                        new com.itextpdf.text.Phrase(subject.subjectName + " (" + subject.maxMarks + ")", header1Font));
                    subjectCell.setColspan(subjectColspan);
                    subjectCell.setBackgroundColor(primaryHeaderBg);
                    subjectCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                    subjectCell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
                    subjectCell.setPadding(6);
                    rankingTable.addCell(subjectCell);
                }
                
                // Overall Metrics header spanning 4 columns
                com.itextpdf.text.pdf.PdfPCell overallCell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase("Overall Performance", header1Font));
                overallCell.setColspan(4);
                overallCell.setBackgroundColor(primaryHeaderBg);
                overallCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                overallCell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
                overallCell.setPadding(6);
                rankingTable.addCell(overallCell);
                
                // ==== ROW 2: Individual Column Headers ====
                // Student info columns
                String[] studentCols = {"Rank", "Roll No.", "Name"};
                for (String col : studentCols) {
                    com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(col, header2Font));
                    cell.setBackgroundColor(secondaryHeaderBg);
                    cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                    cell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
                    cell.setPadding(5);
                    rankingTable.addCell(cell);
                }
                
                // Exam type columns for each subject
                for (AnalyzerDAO.SubjectInfoDetailed subject : rankingData.subjects) {
                    for (String examType : subject.examTypes) {
                        Integer maxMarks = subject.examTypeMaxMarks.get(examType);
                        String header = examType + (maxMarks != null && maxMarks > 0 ? " (" + maxMarks + ")" : "");
                        com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(header, header2Font));
                        cell.setBackgroundColor(secondaryHeaderBg);
                        cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                        cell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
                        cell.setPadding(5);
                        rankingTable.addCell(cell);
                    }
                    // Total column for subject
                    com.itextpdf.text.pdf.PdfPCell totalCell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase("Total", header2Font));
                    totalCell.setBackgroundColor(secondaryHeaderBg);
                    totalCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                    totalCell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
                    totalCell.setPadding(5);
                    rankingTable.addCell(totalCell);
                }
                
                // Overall metric columns
                String[] overallCols = {"Total", "Percentage", "Grade", "CGPA"};
                for (String col : overallCols) {
                    com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(col, header2Font));
                    cell.setBackgroundColor(secondaryHeaderBg);
                    cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                    cell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
                    cell.setPadding(5);
                    rankingTable.addCell(cell);
                }
                
                // ==== DATA ROWS ====
                com.itextpdf.text.Font dataFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 7, com.itextpdf.text.Font.NORMAL);
                com.itextpdf.text.BaseColor gold = new com.itextpdf.text.BaseColor(255, 215, 0);
                com.itextpdf.text.BaseColor silver = new com.itextpdf.text.BaseColor(192, 192, 192);
                com.itextpdf.text.BaseColor bronze = new com.itextpdf.text.BaseColor(205, 127, 50);
                
                rowIdx = 0;
                for (AnalyzerDAO.StudentRankingDetail student : rankingData.students) {
                    // Determine row background color (with rank highlighting)
                    com.itextpdf.text.BaseColor rowBg;
                    if (student.rank == 1) rowBg = gold;
                    else if (student.rank == 2) rowBg = silver;
                    else if (student.rank == 3) rowBg = bronze;
                    else rowBg = (rowIdx % 2 == 0) ? row1Bg : row2Bg;
                    
                    rowIdx++;
                    
                    // Student info cells
                    addStyledDataCell(rankingTable, String.valueOf(student.rank), rowBg, true, dataFont);
                    addStyledDataCell(rankingTable, student.rollNumber, rowBg, true, dataFont);
                    addStyledDataCell(rankingTable, student.studentName, rowBg, false, dataFont);
                    
                    // Subject marks with WEIGHTED TOTAL calculation
                    for (AnalyzerDAO.SubjectInfoDetailed subject : rankingData.subjects) {
                        Map<String, Double> subjectMarks = student.subjectMarks.get(subject.subjectName);
                        
                        if (subjectMarks != null) {
                            // Add exam type marks
                            for (String examType : subject.examTypes) {
                                Double marks = subjectMarks.get(examType);
                                String display = (marks != null && marks >= 0) ? String.format("%.1f", marks) : "-";
                                addStyledDataCell(rankingTable, display, rowBg, true, dataFont);
                            }
                            
                            // Calculate WEIGHTED subject total using AnalyzerDAO method
                            // Get student ID from database
                            int studentId = getStudentIdByRollNumber(student.rollNumber, currentSectionId);
                            double weightedPercentage = -1;  // Default to -1 (will show "-")
                            if (studentId > 0) {
                                AnalyzerDAO.SubjectPassResult result = analyzerDAO.calculateWeightedSubjectTotalWithPass(
                                    studentId, currentSectionId, subject.subjectName, null);  // null = use all exam types
                                // Use percentage directly (0-100 scale) - same as screen table
                                weightedPercentage = result.percentage;
                            }
                            addStyledDataCell(rankingTable, weightedPercentage >= 0 ? String.format("%.2f", weightedPercentage) : "-", rowBg, true, dataFont);
                        } else {
                            // No marks for this subject
                            for (String examType : subject.examTypes) {
                                addStyledDataCell(rankingTable, "-", rowBg, true, dataFont);
                            }
                            addStyledDataCell(rankingTable, "-", rowBg, true, dataFont);
                        }
                    }
                    
                    // Overall metrics
                    addStyledDataCell(rankingTable, String.format("%.1f", student.totalMarks), rowBg, true, dataFont);
                    addStyledDataCell(rankingTable, String.format("%.2f%%", student.percentage), rowBg, true, dataFont);
                    addStyledDataCell(rankingTable, student.grade, rowBg, true, dataFont);
                    addStyledDataCell(rankingTable, String.format("%.2f", student.cgpa), rowBg, true, dataFont);
                }
                
                document.add(rankingTable);
                
                // Add legend at the bottom
                document.add(new com.itextpdf.text.Paragraph(" "));
                com.itextpdf.text.Paragraph legend = new com.itextpdf.text.Paragraph(
                    "Legend: DC = Distinction Class | FC = First Class | SC = Second Class | Subject totals are weighted based on exam type weightage.", 
                    smallFont);
                legend.setAlignment(com.itextpdf.text.Element.ALIGN_LEFT);
                document.add(legend);
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
    
    private JPanel createAllStudentsRankingTable(AnalyzerDAO analyzerDAO) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        // Header
        JLabel titleLabel = new JLabel("🏅 All Students Ranking with Subject Details");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Get detailed ranking data with subject breakdowns
        AnalyzerDAO.DetailedRankingData rankingData = analyzerDAO.getDetailedStudentRanking(currentSectionId, selectedFilters);
        
        if (rankingData == null || rankingData.students.isEmpty()) {
            JLabel noDataLabel = new JLabel("No data available");
            noDataLabel.setHorizontalAlignment(SwingConstants.CENTER);
            noDataLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));
            noDataLabel.setForeground(TEXT_SECONDARY);
            panel.add(titleLabel, BorderLayout.NORTH);
            panel.add(noDataLabel, BorderLayout.CENTER);
            return panel;
        }
        
        // Create hierarchical header structure (2-row header)
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, PRIMARY_DARK));
        
        // ROW 1: Subject Names with Total Marks in brackets
        JPanel subjectNameRow = new JPanel();
        subjectNameRow.setLayout(new BoxLayout(subjectNameRow, BoxLayout.X_AXIS));
        subjectNameRow.setBackground(PRIMARY_COLOR);
        subjectNameRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        
        // Fixed columns - Student Info (330 = 50+100+180)
        addHeaderCell(subjectNameRow, "Student Info", 330, PRIMARY_COLOR, Color.WHITE, Font.BOLD, 12, 3);
        
        // Build column list and widths FIRST to calculate subject header widths
        List<String> columnList = new ArrayList<>();
        List<Integer> columnWidths = new ArrayList<>();
        columnList.add("Rank");
        columnList.add("Roll No.");
        columnList.add("Student Name");
        columnWidths.add(50);
        columnWidths.add(100);
        columnWidths.add(180);
        
        // Calculate subject widths from actual exam type widths
        List<Integer> subjectWidths = new ArrayList<>();
        for (AnalyzerDAO.SubjectInfoDetailed subject : rankingData.subjects) {
            System.out.println("@@@ SUBJECT: " + subject.subjectName + 
                " | examTypeMaxMarks map size: " + subject.examTypeMaxMarks.size() + " @@@");
            
            int totalSubjectWidth = 0;
            for (String examType : subject.examTypes) {
                Integer examMaxMarks = subject.examTypeMaxMarks.get(examType);
                String examHeader = examMaxMarks != null && examMaxMarks > 0 ? 
                                   examType + " (" + examMaxMarks + ")" : examType;
                int width = Math.max(calculateTextWidth(examHeader, 10) + 20, 85);
                columnList.add(examHeader);
                columnWidths.add(width);
                totalSubjectWidth += width;
            }
            // Total column
            String totalHeader = "Total";
            int totalWidth = Math.max(calculateTextWidth(totalHeader, 10) + 20, 85);
            columnList.add(totalHeader);
            columnWidths.add(totalWidth);
            totalSubjectWidth += totalWidth;
            
            subjectWidths.add(totalSubjectWidth);
        }
        
        // Overall metrics
        columnList.add("Overall Total");
        columnList.add("Percentage");
        columnList.add("Grade");
        columnList.add("CGPA");
        columnWidths.add(85);
        columnWidths.add(85);
        columnWidths.add(85);
        columnWidths.add(85);
        
        // Now add subject header cells with calculated widths
        for (int i = 0; i < rankingData.subjects.size(); i++) {
            AnalyzerDAO.SubjectInfoDetailed subject = rankingData.subjects.get(i);
            String subjectHeader = subject.subjectName + " (" + subject.maxMarks + ")";
            int subjectColSpan = subject.examTypes.size() + 1;
            addHeaderCell(subjectNameRow, subjectHeader, subjectWidths.get(i), PRIMARY_COLOR, Color.WHITE, Font.BOLD, 12, subjectColSpan);
        }
        
        addHeaderCell(subjectNameRow, "Overall Metrics", 340, PRIMARY_COLOR, Color.WHITE, Font.BOLD, 12, 4);
        
        // ROW 2: Exam Types and Column Names
        JPanel examTypeRow = new JPanel();
        examTypeRow.setLayout(new BoxLayout(examTypeRow, BoxLayout.X_AXIS));
        examTypeRow.setBackground(PRIMARY_COLOR);
        examTypeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        
        // Fixed columns
        addHeaderCell(examTypeRow, "Rank", 50, PRIMARY_COLOR, Color.WHITE, Font.BOLD, 10, 1);
        addHeaderCell(examTypeRow, "Roll No.", 100, PRIMARY_COLOR, Color.WHITE, Font.BOLD, 10, 1);
        addHeaderCell(examTypeRow, "Student Name", 180, PRIMARY_COLOR, Color.WHITE, Font.BOLD, 10, 1);
        
        // Add exam type and total cells (widths already in columnWidths list)
        int colIdx = 3; // Start after fixed columns
        for (AnalyzerDAO.SubjectInfoDetailed subject : rankingData.subjects) {
            for (String examType : subject.examTypes) {
                String examHeader = columnList.get(colIdx);
                int width = columnWidths.get(colIdx);
                addHeaderCell(examTypeRow, examHeader, width, PRIMARY_COLOR, Color.WHITE, Font.PLAIN, 10, 1);
                colIdx++;
            }
            // Total column
            addHeaderCell(examTypeRow, "Total", columnWidths.get(colIdx), PRIMARY_COLOR, Color.WHITE, Font.BOLD, 10, 1);
            colIdx++;
        }
        
        // Overall metrics
        addHeaderCell(examTypeRow, "Overall Total", 85, PRIMARY_COLOR, Color.WHITE, Font.BOLD, 10, 1);
        addHeaderCell(examTypeRow, "Percentage", 85, PRIMARY_COLOR, Color.WHITE, Font.BOLD, 10, 1);
        addHeaderCell(examTypeRow, "Grade", 85, PRIMARY_COLOR, Color.WHITE, Font.BOLD, 10, 1);
        addHeaderCell(examTypeRow, "CGPA", 85, PRIMARY_COLOR, Color.WHITE, Font.BOLD, 10, 1);
        
        headerPanel.add(subjectNameRow);
        headerPanel.add(examTypeRow);
        
        // Create data table
        String[] columns = columnList.toArray(new String[0]);
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable table = new JTable(model);
        table.setRowHeight(35);
        table.setShowGrid(true);
        table.setGridColor(BORDER_COLOR);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setTableHeader(null); // Remove default header since we have custom one
        table.setFont(new Font("SansSerif", Font.PLAIN, 11));
        table.setSelectionBackground(SELECTION_COLOR);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        // Set column widths based on calculated values
        for (int i = 0; i < columnWidths.size(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(columnWidths.get(i));
        }
        
        // Custom renderer
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected) {
                    if (row % 2 == 0) {
                        c.setBackground(Color.WHITE);
                    } else {
                        c.setBackground(new Color(249, 250, 251));
                    }
                }
                
                // Center alignment for all columns except student name
                if (column == 2) {
                    ((JLabel) c).setHorizontalAlignment(SwingConstants.LEFT);
                } else {
                    ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                }
                
                // Special formatting for rank column
                if (column == 0 && value != null) {
                    try {
                        int rank = Integer.parseInt(value.toString());
                        if (rank == 1) {
                            c.setForeground(new Color(255, 215, 0)); // Gold
                            setFont(getFont().deriveFont(Font.BOLD));
                        } else if (rank == 2) {
                            c.setForeground(new Color(192, 192, 192)); // Silver
                            setFont(getFont().deriveFont(Font.BOLD));
                        } else if (rank == 3) {
                            c.setForeground(new Color(205, 127, 50)); // Bronze
                            setFont(getFont().deriveFont(Font.BOLD));
                        } else {
                            c.setForeground(TEXT_PRIMARY);
                            setFont(getFont().deriveFont(Font.PLAIN));
                        }
                    } catch (NumberFormatException e) {
                        c.setForeground(TEXT_PRIMARY);
                    }
                } else {
                    c.setForeground(TEXT_PRIMARY);
                }
                
                return c;
            }
        };
        
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
        
        // Populate table with weighted calculation for subject totals
        AnalyzerDAO dao = new AnalyzerDAO();
        
        for (AnalyzerDAO.StudentRankingDetail student : rankingData.students) {
            List<Object> rowData = new ArrayList<>();
            rowData.add(student.rank);
            rowData.add(student.rollNumber);
            rowData.add(student.studentName);
            
            // Add subject marks with WEIGHTED calculation for Total (using DAO method)
            for (AnalyzerDAO.SubjectInfoDetailed subject : rankingData.subjects) {
                Map<String, Double> subjectMarks = student.subjectMarks.get(subject.subjectName);
                
                // Add individual exam type marks
                for (String examType : subject.examTypes) {
                    Double marks = subjectMarks != null ? subjectMarks.get(examType) : null;
                    rowData.add(marks != null ? String.format("%.0f", marks) : "-");
                }
                
                // Calculate weighted total for this subject using the same method as StudentAnalyzer
                // Find the student ID first
                int studentId = getStudentIdByRollNumber(student.rollNumber, currentSectionId);
                
                if (studentId > 0) {
                    // Use the same calculation method
                    Set<String> examTypesFilter = (selectedFilters != null) ? selectedFilters.get(subject.subjectName) : null;
                    AnalyzerDAO.SubjectPassResult result = dao.calculateWeightedSubjectTotalWithPass(
                        studentId, currentSectionId, subject.subjectName, examTypesFilter
                    );
                    
                    // Display the weighted percentage (not the raw marks sum)
                    if (result.percentage >= 0) {
                        rowData.add(String.format("%.2f", result.percentage));
                    } else {
                        rowData.add("FAIL");
                    }
                } else {
                    rowData.add("-");
                }
            }
            
            rowData.add(String.format("%.0f", student.totalMarks));
            rowData.add(String.format("%.2f%%", student.percentage));
            rowData.add(student.grade);
            rowData.add(String.format("%.2f", student.cgpa));
            
            model.addRow(rowData.toArray());
        }
        
        // Create wrapper panel to hold both custom header and table
        JPanel tableWithHeaderPanel = new JPanel(new BorderLayout());
        tableWithHeaderPanel.setBackground(Color.WHITE);
        tableWithHeaderPanel.add(headerPanel, BorderLayout.NORTH);
        tableWithHeaderPanel.add(table, BorderLayout.CENTER);
        
        // Create scroll pane with both horizontal and vertical scrolling
        JScrollPane scrollPane = new JScrollPane(tableWithHeaderPanel);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(1000, 320));
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    // Helper method to add header cell with specified properties
    private void addHeaderCell(JPanel row, String text, int width, Color bg, Color fg, int fontStyle, int fontSize, int colspan) {
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
    
    // Helper method to calculate text width for dynamic column sizing
    private int calculateTextWidth(String text, int fontSize) {
        JLabel dummyLabel = new JLabel(text);
        dummyLabel.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
        FontMetrics metrics = dummyLabel.getFontMetrics(dummyLabel.getFont());
        return metrics.stringWidth(text);
    }
    
    // Helper method to get student ID by roll number
    private int getStudentIdByRollNumber(String rollNumber, int sectionId) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT id FROM students WHERE roll_number = ? AND section_id = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, rollNumber);
            ps.setInt(2, sectionId);
            ResultSet rs = ps.executeQuery();
            
            int studentId = 0;
            if (rs.next()) {
                studentId = rs.getInt("id");
            }
            rs.close();
            ps.close();
            return studentId;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    private void closePanel() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }
    
    // Helper method for styled PDF cells
    private void addStyledCell(com.itextpdf.text.pdf.PdfPTable table, String content, com.itextpdf.text.BaseColor bg, boolean center) {
        com.itextpdf.text.Font cellFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.NORMAL);
        com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(content, cellFont));
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        if (center) {
            cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        }
        table.addCell(cell);
    }
    
    // Helper method for data cells with custom font
    private void addStyledDataCell(com.itextpdf.text.pdf.PdfPTable table, String content, com.itextpdf.text.BaseColor bg, boolean center, com.itextpdf.text.Font font) {
        com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(content, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(4);
        cell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
        if (center) {
            cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        }
        table.addCell(cell);
    }
}
