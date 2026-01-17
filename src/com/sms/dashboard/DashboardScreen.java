package com.sms.dashboard;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Map;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import com.sms.analyzer.Student;
import com.sms.dashboard.data.DashboardDataManager;
import com.sms.dashboard.components.SidebarPanel;
import com.sms.dashboard.components.SectionCardPanel;
import com.sms.dashboard.components.GradeDistributionPanel;
import com.sms.dashboard.dialogs.CreateSectionDialog;
import com.sms.dashboard.dialogs.CreateSectionPanel;
import com.sms.dashboard.components.YearSemesterPanel;
import com.sms.dashboard.dialogs.MarkEntryDialog;
import com.sms.dashboard.dialogs.StudentEntryDialog;
import com.sms.database.DatabaseConnection;
import com.sms.login.LoginScreen;
import com.sms.analyzer.StudentAnalyzer;
import com.sms.analyzer.SectionAnalyzer;
import com.sms.viewtool.ViewSelectionTool;
import com.sms.dao.AnalyzerDAO;
import com.sms.dao.SectionDAO;
import com.sms.dao.SectionDAO.SectionInfo;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.plaf.basic.BasicScrollBarUI;

// New service layer imports
import com.sms.dashboard.services.StudentService;
import com.sms.dashboard.services.SectionService;
import com.sms.dashboard.services.AnalyticsService;
import com.sms.dashboard.util.DashboardErrorHandler;
import com.sms.dashboard.util.UIComponentFactory;
import com.sms.dashboard.util.BackgroundTaskUtil;
import com.sms.dashboard.constants.DashboardConstants;
import static com.sms.dashboard.constants.DashboardConstants.*;

public class DashboardScreen extends JFrame implements DashboardActions {
    
    // Service layer instances
    private final StudentService studentService;
    private final SectionService sectionService;
    private final AnalyticsService analyticsService;
    
    private int userId;
    private DashboardDataManager dataManager;
    private JPanel sectionCardsPanel;
    private YearSemesterPanel yearSemesterPanel;
    private GradeDistributionPanel gradeDistPanel;
    private JPanel summaryPanel;
    
    // Year filter
    private JComboBox<String> yearFilterComboBox;
    private int selectedYear = 0; // 0 means "All Years"
    
    // Auto-refresh timer
    private Timer autoRefreshTimer;
    private boolean autoRefreshEnabled = false;
    
    // CardLayout for switching between dashboard and create section panel
    private CardLayout cardLayout;
    private JPanel mainContentPanel;
    private CreateSectionPanel createSectionPanel;
    private StudentEntryDialog studentEntryPanel;
    private MarkEntryDialog markEntryPanel;
    private StudentAnalyzer studentAnalyzerPanel;
    private ViewSelectionTool viewDataPanel;
    private com.sms.resultlauncher.ResultLauncher resultLauncherPanel;
    
    private static final String DASHBOARD_VIEW = "dashboard";
    private static final String LIBRARY_VIEW = "library";
    private static final String CREATE_SECTION_VIEW = "createSection";
    private static final String STUDENT_ENTRY_VIEW = "studentEntry";
    private static final String MARK_ENTRY_VIEW = "markEntry";
    private static final String STUDENT_ANALYZER_VIEW = "studentAnalyzer";
    private static final String VIEW_DATA_VIEW = "viewData";
    private static final String RESULT_LAUNCHER_VIEW = "resultLauncher";

    public DashboardScreen(int userId) {
        this.userId = userId;
        
        // Initialize services
        this.studentService = new StudentService();
        this.sectionService = new SectionService();
        this.analyticsService = new AnalyticsService();
        
        try {
            this.dataManager = new DashboardDataManager(userId);
        } catch (Exception e) {
            DashboardErrorHandler.handleError("Failed to initialize dashboard", e);
        }
        
        initializeUI();
    }
    
    private void initializeUI() {
        // Set up CardLayout for switching between views
        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.setBackground(BACKGROUND_COLOR);
        mainContentPanel.setOpaque(true);
        
        // Initialize and add dashboard panel
        JPanel dashboardPanel = createDashboardPanel();
        mainContentPanel.add(dashboardPanel, DASHBOARD_VIEW);
        
        // Initialize library panel (hierarchical year/semester view)
        JPanel libraryPanel = createLibraryPanel();
        mainContentPanel.add(libraryPanel, LIBRARY_VIEW);
        
        // Initialize create section panel
        createSectionPanel = new CreateSectionPanel(this, userId, this::closeSectionCreationPanel);
        mainContentPanel.add(createSectionPanel, CREATE_SECTION_VIEW);
        
        // Initialize student entry panel
        studentEntryPanel = new StudentEntryDialog(this, dataManager, this::closeStudentEntryPanel);
        mainContentPanel.add(studentEntryPanel, STUDENT_ENTRY_VIEW);
        
        // Initialize mark entry panel
        markEntryPanel = new MarkEntryDialog(this, this::closeMarkEntryPanel);
        mainContentPanel.add(markEntryPanel, MARK_ENTRY_VIEW);
        
        // Initialize student analyzer panel
        studentAnalyzerPanel = new StudentAnalyzer(this, dataManager.getSectionStudents(), this::closeStudentAnalyzerPanel);
        mainContentPanel.add(studentAnalyzerPanel, STUDENT_ANALYZER_VIEW);
        
        // Initialize view data panel (Field selection & export)
        viewDataPanel = new ViewSelectionTool(this, dataManager.getSectionStudents(), this::closeViewDataPanel);
        mainContentPanel.add(viewDataPanel, VIEW_DATA_VIEW);
        
        // Initialize result launcher panel
        resultLauncherPanel = new com.sms.resultlauncher.ResultLauncher(this, this::closeResultLauncherPanel);
        mainContentPanel.add(resultLauncherPanel, RESULT_LAUNCHER_VIEW);
        
        // Set up main frame
        setTitle("Academic Analyzer - Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout());
        
        // Create sidebar
        SidebarPanel sidebar = new SidebarPanel(this);
        sidebar.setMinimumSize(new Dimension(280, 600));
        sidebar.setPreferredSize(new Dimension(280, getHeight()));
        add(sidebar, BorderLayout.WEST);
        
        // Add main content with CardLayout
        add(mainContentPanel, BorderLayout.CENTER);
        
        setVisible(true);
        
        // Show dashboard by default
        cardLayout.show(mainContentPanel, DASHBOARD_VIEW);
        
        // Load initial dashboard data
        refreshDashboard();
        
        // Initialize auto-refresh timer
        initializeAutoRefresh();
        
        // Add window listener to stop timer when closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (autoRefreshTimer != null) {
                    autoRefreshTimer.stop();
                }
            }
        });
    }
    
    // Method to show dashboard
    public void showDashboard() {
        cardLayout.show(mainContentPanel, DASHBOARD_VIEW);
    }
    
    // Method to show library (year/semester view)
    public void showLibrary() {
        // Refresh library data when showing
        refreshLibrary();
        cardLayout.show(mainContentPanel, LIBRARY_VIEW);
    }
    
    // Method to show create section panel
    public void showCreateSectionPanel() {
        // Remove old panel and create new one in create mode (null sectionId)
        mainContentPanel.remove(createSectionPanel);
        createSectionPanel = new CreateSectionPanel(this, userId, null, this::closeSectionCreationPanel);
        mainContentPanel.add(createSectionPanel, CREATE_SECTION_VIEW);
        cardLayout.show(mainContentPanel, CREATE_SECTION_VIEW);
    }
    
    // Method to show edit section panel
    public void showEditSectionPanel(int sectionId) {
        // Remove old create section panel and create new one in edit mode
        mainContentPanel.remove(createSectionPanel);
        createSectionPanel = new CreateSectionPanel(this, userId, sectionId, this::closeSectionCreationPanel);
        mainContentPanel.add(createSectionPanel, CREATE_SECTION_VIEW);
        cardLayout.show(mainContentPanel, CREATE_SECTION_VIEW);
    }
    
    // Method to show student entry panel
    public void showStudentEntryPanel() {
        cardLayout.show(mainContentPanel, STUDENT_ENTRY_VIEW);
    }
    
    // Method to close create section panel and return to dashboard
    public void closeSectionCreationPanel() {
        cardLayout.show(mainContentPanel, DASHBOARD_VIEW);
        refreshDashboard(); // Refresh dashboard to show any new sections
    }
    
    // Method to close student entry panel and return to dashboard
    public void closeStudentEntryPanel() {
        cardLayout.show(mainContentPanel, DASHBOARD_VIEW);
        refreshDashboard(); // Refresh dashboard to show any new students
    }
    
    private JPanel createDashboardPanel() {
        JPanel dashboardPanel = new JPanel(new BorderLayout());
        dashboardPanel.setBackground(BACKGROUND_COLOR);
        dashboardPanel.setOpaque(true);
        
        // Title Panel
        JPanel titlePanel = createDashboardTitlePanel();
        dashboardPanel.add(titlePanel, BorderLayout.NORTH);
        
        // Main Dashboard Content with scroll
        JPanel dashboardContent = createMainDashboardContent();
        JScrollPane scrollPane = new JScrollPane(dashboardContent);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.setBackground(BACKGROUND_COLOR);
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR);
        
        dashboardPanel.add(scrollPane, BorderLayout.CENTER);
        
        return dashboardPanel;
    }
    
    private JPanel createLibraryPanel() {
        JPanel libraryPanel = new JPanel(new BorderLayout());
        libraryPanel.setBackground(BACKGROUND_COLOR);
        libraryPanel.setOpaque(true);
        
        // Title Panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        
        JLabel title = new JLabel("Section Library");
        title.setFont(new Font("SansSerif", Font.BOLD, 32));
        title.setForeground(TEXT_PRIMARY);
        
        JButton backBtn = new JButton("← Back to Dashboard");
        backBtn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        backBtn.setForeground(PRIMARY_COLOR);
        backBtn.setBorderPainted(false);
        backBtn.setContentAreaFilled(false);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> showDashboard());
        
        titlePanel.add(title, BorderLayout.WEST);
        titlePanel.add(backBtn, BorderLayout.EAST);
        libraryPanel.add(titlePanel, BorderLayout.NORTH);
        
        // Year/Semester hierarchical panel
        yearSemesterPanel = new YearSemesterPanel(userId, this::refreshLibrary);
        
        JScrollPane scrollPane = new JScrollPane(yearSemesterPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        
        libraryPanel.add(scrollPane, BorderLayout.CENTER);
        
        return libraryPanel;
    }
    
    private JPanel createDashboardTitlePanel() {
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("Dashboard");
        title.setFont(new Font("SansSerif", Font.BOLD, 32));
        title.setForeground(TEXT_PRIMARY);

        JButton refreshBtn = new JButton("↻ Refresh");
        refreshBtn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        refreshBtn.setForeground(PRIMARY_COLOR);
        refreshBtn.setBorderPainted(false);
        refreshBtn.setContentAreaFilled(false);
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> refreshDashboard());

        titlePanel.add(title, BorderLayout.WEST);
        titlePanel.add(refreshBtn, BorderLayout.EAST);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));
        
        return titlePanel;
    }
    
    private JPanel createMainDashboardContent() {
        JPanel dashboardContent = new JPanel(new BorderLayout(0, 30));
        dashboardContent.setBackground(BACKGROUND_COLOR);
        dashboardContent.setOpaque(true);
        dashboardContent.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        
        // Section Cards Container
        JPanel sectionContainer = createSectionContainer();
        
        // Analytics Container
        JPanel analyticsContainer = createAnalyticsContainer();
        
        // Quick Actions Panel
        JPanel quickActionsPanel = createQuickActionsPanel();
        
        // Add all containers
        dashboardContent.add(sectionContainer, BorderLayout.NORTH);
        dashboardContent.add(analyticsContainer, BorderLayout.CENTER);
        dashboardContent.add(quickActionsPanel, BorderLayout.SOUTH);
        
        return dashboardContent;
    }
    
    private JPanel createSectionContainer() {
        JPanel sectionContainer = new JPanel(new BorderLayout(0, 15));
        sectionContainer.setOpaque(false);
        
        // Header with title and year filter
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        // "Sections" heading
        JLabel sectionsHeading = new JLabel("Sections");
        sectionsHeading.setFont(new Font("SansSerif", Font.BOLD, 24));
        sectionsHeading.setForeground(TEXT_PRIMARY);
        headerPanel.add(sectionsHeading, BorderLayout.WEST);
        
        // Year filter panel (right side)
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filterPanel.setOpaque(false);
        
        JLabel filterLabel = new JLabel("Academic Year:");
        filterLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        filterLabel.setForeground(TEXT_SECONDARY);
        
        // Create year filter dropdown
        yearFilterComboBox = new JComboBox<>();
        yearFilterComboBox.setFont(new Font("SansSerif", Font.PLAIN, 14));
        yearFilterComboBox.setPreferredSize(new Dimension(150, 35));
        yearFilterComboBox.setBackground(Color.WHITE);
        yearFilterComboBox.addActionListener(e -> onYearFilterChanged());
        
        filterPanel.add(filterLabel);
        filterPanel.add(yearFilterComboBox);
        headerPanel.add(filterPanel, BorderLayout.EAST);
        
        sectionContainer.add(headerPanel, BorderLayout.NORTH);
        
        // Horizontal scrolling cards panel
        sectionCardsPanel = new JPanel();
        sectionCardsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 0));
        sectionCardsPanel.setOpaque(false);
        sectionCardsPanel.setPreferredSize(new Dimension(2000, 100));
        
        JScrollPane sectionScrollPane = new JScrollPane(sectionCardsPanel);
        sectionScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        sectionScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sectionScrollPane.setBorder(null);
        sectionScrollPane.setOpaque(false);
        sectionScrollPane.getViewport().setOpaque(false);
        sectionScrollPane.setPreferredSize(new Dimension(0, 100));
        
        // Custom scroll bar styling
        sectionScrollPane.getHorizontalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = CARD_BACKGROUND;
                this.trackColor = BACKGROUND_COLOR;
            }
            
            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }
            
            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }
            
            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }
            
            @Override
            public void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(trackColor);
                g2.fillRoundRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height, 10, 10);
                g2.dispose();
            }
        });
        
        sectionContainer.add(sectionScrollPane, BorderLayout.CENTER);
        
        return sectionContainer;
    }
    
    private JPanel createAnalyticsContainer() {
        JPanel analyticsContainer = new JPanel(new BorderLayout());
        analyticsContainer.setBackground(BACKGROUND_COLOR);
        analyticsContainer.setOpaque(true);
        analyticsContainer.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Create a panel for the entire analytics section
        JPanel analyticsContent = new JPanel(new BorderLayout(30, 20));
        analyticsContent.setBackground(BACKGROUND_COLOR);
        analyticsContent.setOpaque(true);

        // Left side - Pie chart
        gradeDistPanel = new GradeDistributionPanel();
        analyticsContent.add(gradeDistPanel, BorderLayout.WEST);

        // Right side - Summary statistics (2x3 grid) with scroll
        summaryPanel = new JPanel(new GridLayout(2, 3, 15, 15));
        summaryPanel.setBackground(BACKGROUND_COLOR);
        summaryPanel.setOpaque(true);
        
        JScrollPane summaryScrollPane = new JScrollPane(summaryPanel);
        summaryScrollPane.setPreferredSize(new Dimension(700, 140));
        summaryScrollPane.setBorder(null);
        summaryScrollPane.setBackground(BACKGROUND_COLOR);
        summaryScrollPane.getViewport().setBackground(BACKGROUND_COLOR);
        
        analyticsContent.add(summaryScrollPane, BorderLayout.CENTER);

        analyticsContainer.add(analyticsContent, BorderLayout.CENTER);
        
        return analyticsContainer;
    }
    
    private JPanel createQuickActionsPanel() {
        JPanel quickActionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        quickActionsPanel.setOpaque(false);
        quickActionsPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        
        // Quick action buttons
        JButton addStudentBtn = UIComponentFactory.createPrimaryButton("+ Add Student");
        JButton bulkImportBtn = UIComponentFactory.createSecondaryButton("📊 Bulk Import");
        JButton exportDataBtn = UIComponentFactory.createSecondaryButton("📤 Export Data");
        
        addStudentBtn.addActionListener(e -> openStudentEntryDialog());
        bulkImportBtn.addActionListener(e -> openBulkImportDialog());
        exportDataBtn.addActionListener(e -> exportDashboardData());
        
        quickActionsPanel.add(addStudentBtn);
        quickActionsPanel.add(bulkImportBtn);
        quickActionsPanel.add(exportDataBtn);
        
        return quickActionsPanel;
    }

    @Override
    public void openCreateSectionDialog() {
        // Show create section panel instead of dialog
        showCreateSectionPanel();
    }
    
    @Override
    public void openStudentEntryDialog() {
        showStudentEntryPanel();
    }
    
    @Override
    public void openMarkEntryDialog() {
        try {
            showMarkEntryPanel();
        } catch (Exception e) {
            DashboardErrorHandler.handleError("Failed to open mark entry dialog", e);
        }
    }
    
    public void showMarkEntryPanel() {
        cardLayout.show(mainContentPanel, MARK_ENTRY_VIEW);
    }
    
    public void closeMarkEntryPanel() {
        cardLayout.show(mainContentPanel, DASHBOARD_VIEW);
        refreshDashboard();
    }
    
    @Override
    public void openAnalysisView() {
        showViewDataPanel();  // View Data shows ViewSelectionTool (field selection & export)
    }
    
    @Override
    public void openStudentAnalyzer() {
        showResultLauncherPanel();  // Result Launcher shows ResultLauncher
    }
    
    public void showResultLauncherPanel() {
        cardLayout.show(mainContentPanel, RESULT_LAUNCHER_VIEW);
    }
    
    public void closeResultLauncherPanel() {
        cardLayout.show(mainContentPanel, DASHBOARD_VIEW);
        refreshDashboard();
    }
    
    public void showViewDataPanel() {
        cardLayout.show(mainContentPanel, VIEW_DATA_VIEW);
    }
    
    public void closeViewDataPanel() {
        cardLayout.show(mainContentPanel, DASHBOARD_VIEW);
        refreshDashboard();
    }
    
    public void showStudentAnalyzerPanel() {
        cardLayout.show(mainContentPanel, STUDENT_ANALYZER_VIEW);
    }
    
    @Override
    public void openStudentAnalyzerPanel() {
        showStudentAnalyzerPanel();
    }
    
    public void closeStudentAnalyzerPanel() {
        cardLayout.show(mainContentPanel, DASHBOARD_VIEW);
        refreshDashboard();
    }
    
    // New method to show section ranking table from library
    public void showSectionRankingTable(int sectionId, String sectionName) {
        System.out.println("@@@ DASHBOARD: Showing ranking table for section " + sectionName + " (ID: " + sectionId + ") @@@");
        
        // Create a new panel for the ranking table view
        String SECTION_RANKING_VIEW = "sectionRanking_" + sectionId;
        
        // Remove any existing section ranking view
        for (Component comp : mainContentPanel.getComponents()) {
            if (comp.getName() != null && comp.getName().startsWith("sectionRanking_")) {
                mainContentPanel.remove(comp);
            }
        }
        
        // Create the ranking table panel
        JPanel rankingPanel = createSectionRankingPanel(sectionId, sectionName);
        rankingPanel.setName(SECTION_RANKING_VIEW);
        mainContentPanel.add(rankingPanel, SECTION_RANKING_VIEW);
        
        // Show the ranking table
        cardLayout.show(mainContentPanel, SECTION_RANKING_VIEW);
    }
    
    private JPanel createSectionRankingPanel(int sectionId, String sectionName) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Header with back button and title
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        // Back button
        JButton backButton = new JButton("← Back to Library");
        backButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        backButton.setForeground(PRIMARY_COLOR);
        backButton.setBackground(Color.WHITE);
        backButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PRIMARY_COLOR, 2),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> showLibrary());
        
        // Title
        JLabel titleLabel = new JLabel("Section Ranking: " + sectionName);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setForeground(TEXT_PRIMARY);
        
        headerPanel.add(backButton, BorderLayout.WEST);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Get ranking table from AnalyzerDAO
        AnalyzerDAO analyzerDAO = new AnalyzerDAO();
        JPanel rankingTable = createRankingTableFromDAO(analyzerDAO, sectionId);
        
        // Add to scrollpane
        JScrollPane scrollPane = new JScrollPane(rankingTable);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createRankingTableFromDAO(AnalyzerDAO analyzerDAO, int sectionId) {
        AnalyzerDAO.DetailedRankingData rankingData = analyzerDAO.getDetailedStudentRanking(sectionId, new HashMap<>());
        
        if (rankingData == null || rankingData.students == null || rankingData.students.isEmpty()) {
            JPanel emptyPanel = new JPanel();
            emptyPanel.setBackground(Color.WHITE);
            JLabel noData = new JLabel("No ranking data available");
            noData.setFont(new Font("SansSerif", Font.ITALIC, 14));
            noData.setForeground(new Color(107, 114, 128));
            emptyPanel.add(noData);
            return emptyPanel;
        }
        
        // Create hierarchical header structure (2-row header)
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, PRIMARY_COLOR));
        
        // ROW 1: Subject Names with Total Marks in brackets
        JPanel subjectNameRow = new JPanel();
        subjectNameRow.setLayout(new BoxLayout(subjectNameRow, BoxLayout.X_AXIS));
        subjectNameRow.setBackground(PRIMARY_COLOR);
        subjectNameRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        
        // Fixed columns - Student Info
        int studentInfoWidth = 330; // 50 + 100 + 180
        addHeaderCell(subjectNameRow, "Student Info", studentInfoWidth, PRIMARY_COLOR, Color.WHITE, Font.BOLD, 12);
        
        // Build column list and widths for data table
        List<String> columnList = new ArrayList<>();
        List<Integer> columnWidths = new ArrayList<>();
        columnList.add("Rank");
        columnList.add("Roll No.");
        columnList.add("Student Name");
        columnWidths.add(50);
        columnWidths.add(100);
        columnWidths.add(180);
        
        // ROW 2: Exam Types and Column Names
        JPanel examTypeRow = new JPanel();
        examTypeRow.setLayout(new BoxLayout(examTypeRow, BoxLayout.X_AXIS));
        examTypeRow.setBackground(PRIMARY_COLOR);
        examTypeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        
        // Fixed columns
        addHeaderCell(examTypeRow, "Rank", 50, PRIMARY_COLOR, Color.WHITE, Font.BOLD, 10);
        addHeaderCell(examTypeRow, "Roll No.", 100, PRIMARY_COLOR, Color.WHITE, Font.BOLD, 10);
        addHeaderCell(examTypeRow, "Student Name", 180, PRIMARY_COLOR, Color.WHITE, Font.BOLD, 10);
        
        // Subject name cells with max marks in brackets - CALCULATE WIDTH FROM CHILD COLUMNS
        for (AnalyzerDAO.SubjectInfoDetailed subject : rankingData.subjects) {
            // First, calculate the total width needed for all exam type columns under this subject
            int totalSubjectWidth = 0;
            List<Integer> examTypeWidths = new ArrayList<>();
            
            // Calculate width for each exam type column
            for (String examType : subject.examTypes) {
                Integer examMaxMarks = subject.examTypeMaxMarks.get(examType);
                String examHeader = examMaxMarks != null && examMaxMarks > 0 ? 
                                   examType + " (" + examMaxMarks + ")" : examType;
                int width = Math.max(calculateTextWidth(examHeader, 10) + 20, 85);
                examTypeWidths.add(width);
                totalSubjectWidth += width;
            }
            
            // Add width for Total column
            int totalColWidth = Math.max(calculateTextWidth("Total", 10) + 20, 85);
            examTypeWidths.add(totalColWidth);
            totalSubjectWidth += totalColWidth;
            
            // Now add subject header cell with the calculated total width
            String subjectHeader = subject.subjectName + " (" + subject.maxMarks + ")";
            addHeaderCell(subjectNameRow, subjectHeader, totalSubjectWidth, PRIMARY_COLOR, Color.WHITE, Font.BOLD, 12);
            
            // Add exam type columns to row 2
            int examIdx = 0;
            for (String examType : subject.examTypes) {
                Integer examMaxMarks = subject.examTypeMaxMarks.get(examType);
                String examHeader = examMaxMarks != null && examMaxMarks > 0 ? 
                                   examType + " (" + examMaxMarks + ")" : examType;
                int width = examTypeWidths.get(examIdx++);
                addHeaderCell(examTypeRow, examHeader, width, PRIMARY_COLOR, Color.WHITE, Font.PLAIN, 10);
                columnList.add(examHeader);
                columnWidths.add(width);
            }
            
            // Add Total column
            int width = examTypeWidths.get(examIdx);
            addHeaderCell(examTypeRow, "Total", width, PRIMARY_COLOR, Color.WHITE, Font.BOLD, 10);
            columnList.add("Total");
            columnWidths.add(width);
        }
        
        // Overall metrics
        addHeaderCell(examTypeRow, "Overall Total", 85, PRIMARY_COLOR, Color.WHITE, Font.BOLD, 10);
        addHeaderCell(examTypeRow, "Percentage", 85, PRIMARY_COLOR, Color.WHITE, Font.BOLD, 10);
        addHeaderCell(examTypeRow, "Grade", 85, PRIMARY_COLOR, Color.WHITE, Font.BOLD, 10);
        addHeaderCell(examTypeRow, "CGPA", 85, PRIMARY_COLOR, Color.WHITE, Font.BOLD, 10);
        columnList.add("Overall Total");
        columnList.add("Percentage");
        columnList.add("Grade");
        columnList.add("CGPA");
        columnWidths.add(85);
        columnWidths.add(85);
        columnWidths.add(85);
        columnWidths.add(85);
        
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
        table.setGridColor(new Color(229, 231, 235));
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setTableHeader(null); // Remove default header since we have custom one
        table.setFont(new Font("SansSerif", Font.PLAIN, 11));
        table.setSelectionBackground(new Color(99, 102, 241, 30));
        table.setSelectionForeground(new Color(17, 24, 39));
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
                            c.setForeground(new Color(17, 24, 39));
                            setFont(getFont().deriveFont(Font.PLAIN));
                        }
                    } catch (NumberFormatException e) {
                        c.setForeground(new Color(17, 24, 39));
                    }
                } else {
                    c.setForeground(new Color(17, 24, 39));
                }
                
                return c;
            }
        };
        
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
        
        // Populate table with data - USE WEIGHTED CALCULATION FOR TOTALS
        AnalyzerDAO dao = new AnalyzerDAO();
        
        for (AnalyzerDAO.StudentRankingDetail student : rankingData.students) {
            Object[] rowData = new Object[columnList.size()];
            int col = 0;
            
            rowData[col++] = student.rank;
            rowData[col++] = student.rollNumber;
            rowData[col++] = student.studentName;
            
            // Get student ID for weighted calculation
            int studentId = getStudentIdByRollNumber(student.rollNumber, sectionId);
            
            // Add marks for each subject's exam types and total
            for (AnalyzerDAO.SubjectInfoDetailed subject : rankingData.subjects) {
                Map<String, Double> studentSubjectMarks = student.subjectMarks.get(subject.subjectName);
                
                if (studentSubjectMarks != null) {
                    // Add exam type marks
                    for (String examType : subject.examTypes) {
                        Double marks = studentSubjectMarks.get(examType);
                        rowData[col++] = marks != null ? String.format("%.1f", marks) : "-";
                    }
                    
                    // Calculate WEIGHTED subject total using same method as SectionAnalyzer
                    // Use subject name and exam types filter (null = use all exam types)
                    AnalyzerDAO.SubjectPassResult result = dao.calculateWeightedSubjectTotalWithPass(
                        studentId, 
                        sectionId, 
                        subject.subjectName,
                        null  // null = use all exam types
                    );
                    rowData[col++] = result.percentage >= 0 ? String.format("%.2f", result.percentage) : "-";
                } else {
                    // No marks for this subject - fill with dashes
                    for (int j = 0; j < subject.examTypes.size() + 1; j++) {
                        rowData[col++] = "-";
                    }
                }
            }
            
            rowData[col++] = String.format("%.2f", student.totalMarks);
            rowData[col++] = String.format("%.2f%%", student.percentage);
            rowData[col++] = student.grade != null ? student.grade : "-";
            rowData[col++] = String.format("%.2f", student.cgpa);
            
            model.addRow(rowData);
        }
        
        // Wrap table and header in container
        JPanel tableWithHeaderPanel = new JPanel(new BorderLayout());
        tableWithHeaderPanel.setBackground(Color.WHITE);
        tableWithHeaderPanel.add(headerPanel, BorderLayout.NORTH);
        tableWithHeaderPanel.add(table, BorderLayout.CENTER);
        
        // Create scroll pane with optimized scrolling performance
        JScrollPane scrollPane = new JScrollPane(tableWithHeaderPanel);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235), 1));
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        // Optimize scroll performance
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        table.setDoubleBuffered(true);
        scrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(Color.WHITE);
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        
        return tablePanel;
    }
    
    // Helper method to add header cell with specified properties
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
    
    // Helper method to get subject ID by name
    private int getSubjectIdByName(String subjectName, int sectionId) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT subject_id FROM section_subjects ss " +
                          "JOIN subjects sub ON ss.subject_id = sub.id " +
                          "WHERE ss.section_id = ? AND sub.subject_name = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, sectionId);
            ps.setString(2, subjectName);
            ResultSet rs = ps.executeQuery();
            
            int subjectId = 0;
            if (rs.next()) {
                subjectId = rs.getInt("subject_id");
            }
            rs.close();
            ps.close();
            return subjectId;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    @Override
    public void openSectionAnalyzer() {
        // No longer needed - accessed through StudentAnalyzer's Section radio button
    }
    
    private HashMap<String, java.util.ArrayList<Student>> convertToSectionStudents() {
        HashMap<String, java.util.ArrayList<Student>> converted = new HashMap<>();
        HashMap<String, List<Student>> original = dataManager.getSectionStudents();
        
        for (String key : original.keySet()) {
            converted.put(key, new java.util.ArrayList<>(original.get(key)));
        }
        
        return converted;
    }
    
    @Override
    public void performLogout() {
        int choice = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to logout?", 
            "Confirm Logout", 
            JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            new LoginScreen().setVisible(true);
        }
    }
    
    @Override
    public void refreshDashboard() {
        SwingUtilities.invokeLater(() -> {
            try {
                populateYearFilter(); // Populate year dropdown first
                loadSectionCards();
                updateAnalytics();
            } catch (Exception e) {
                DashboardErrorHandler.handleError("Failed to refresh dashboard", e);
            }
        });
    }
    
    private void onYearFilterChanged() {
        String selected = (String) yearFilterComboBox.getSelectedItem();
        if (selected != null) {
            if (selected.equals("All Years")) {
                selectedYear = 0;
            } else {
                try {
                    selectedYear = Integer.parseInt(selected);
                } catch (NumberFormatException e) {
                    selectedYear = 0;
                }
            }
            loadSectionCards(); // Reload sections with new filter
            updateAnalytics(); // Update analytics for filtered year
        }
    }
    
    private void populateYearFilter() {
        BackgroundTaskUtil.executeAsync(() -> {
            try {
                List<SectionInfo> allSections = sectionService.getUserSections(userId);
                java.util.Set<Integer> years = new java.util.TreeSet<>(java.util.Collections.reverseOrder());
                
                // Collect unique years
                for (SectionInfo section : allSections) {
                    if (section.academicYear > 0) {
                        years.add(section.academicYear);
                    }
                }
                
                SwingUtilities.invokeLater(() -> {
                    yearFilterComboBox.removeAllItems();
                    yearFilterComboBox.addItem("All Years");
                    
                    for (Integer year : years) {
                        yearFilterComboBox.addItem(String.valueOf(year));
                    }
                    
                    // Set selected item based on current filter
                    if (selectedYear == 0) {
                        yearFilterComboBox.setSelectedItem("All Years");
                    } else {
                        yearFilterComboBox.setSelectedItem(String.valueOf(selectedYear));
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> 
                    DashboardErrorHandler.handleError("Failed to load year filter", e));
            }
        });
    }
    
    private void refreshLibrary() {
        BackgroundTaskUtil.executeAsync(() -> {
            try {
                List<SectionInfo> sections = sectionService.getUserSections(userId);
                SwingUtilities.invokeLater(() -> {
                    if (yearSemesterPanel != null) {
                        yearSemesterPanel.updateSections(sections);
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> 
                    DashboardErrorHandler.handleError("Failed to load library sections", e));
            }
        });
    }
    
    private void loadSectionCards() {
        BackgroundTaskUtil.executeAsync(() -> {
            try {
                List<SectionInfo> allSections = sectionService.getUserSections(userId);
                
                // Filter sections by selected year
                List<SectionInfo> filteredSections = new java.util.ArrayList<>();
                for (SectionInfo section : allSections) {
                    if (selectedYear == 0 || section.academicYear == selectedYear) {
                        filteredSections.add(section);
                    }
                }
                
                SwingUtilities.invokeLater(() -> updateSectionCards(filteredSections));
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> 
                    DashboardErrorHandler.handleError("Failed to load sections", e));
            }
        });
    }
    
    private void updateSectionCards(List<SectionInfo> sections) {
        sectionCardsPanel.removeAll();
        
        if (sections == null || sections.isEmpty()) {
            sectionCardsPanel.add(createEmptyState());
        } else {
            for (SectionInfo section : sections) {
                SectionCardPanel card = new SectionCardPanel(
                    section.sectionName,
                    section.totalStudents,
                    section.id,
                    userId,
                    this::refreshDashboard
                );
                sectionCardsPanel.add(card);
            }
        }
        
        sectionCardsPanel.revalidate();
        sectionCardsPanel.repaint();
    }
    
    private JPanel createEmptyState() {
        JPanel emptyState = new JPanel();
        emptyState.setOpaque(false);
        emptyState.setLayout(new BoxLayout(emptyState, BoxLayout.Y_AXIS));
        emptyState.setPreferredSize(new Dimension(400, 250));
        emptyState.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));
        
        JLabel emptyIcon = new JLabel("📚", JLabel.CENTER);
        emptyIcon.setFont(new Font("SansSerif", Font.PLAIN, 48));
        emptyIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel emptyTitle = new JLabel("No Sections Yet");
        emptyTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        emptyTitle.setForeground(TEXT_SECONDARY);
        emptyTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel emptySubtitle = new JLabel("Create your first section to get started");
        emptySubtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        emptySubtitle.setForeground(TEXT_SECONDARY);
        emptySubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JButton createFirstSection = UIComponentFactory.createPrimaryButton("+ Create Section");
        createFirstSection.setAlignmentX(Component.CENTER_ALIGNMENT);
        createFirstSection.addActionListener(e -> showCreateSectionPanel());
        
        emptyState.add(emptyIcon);
        emptyState.add(Box.createVerticalStrut(15));
        emptyState.add(emptyTitle);
        emptyState.add(Box.createVerticalStrut(10));
        emptyState.add(emptySubtitle);
        emptyState.add(Box.createVerticalStrut(25));
        emptyState.add(createFirstSection);
        
        return emptyState;
    }
    
    private void updateAnalytics() {
        BackgroundTaskUtil.executeAsync(() -> {
            try {
                // Update grade distribution with year filter
                SwingUtilities.invokeLater(() -> gradeDistPanel.updateData(userId, selectedYear));
                
                // Update summary statistics with year filter
                HashMap<String, Object> stats = analyticsService.getDashboardStatistics(userId, selectedYear);
                SwingUtilities.invokeLater(() -> updateSummaryPanel(stats));
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> 
                    DashboardErrorHandler.handleError("Failed to update analytics", e));
            }
        });
    }
    
    private void updateSummaryPanel(HashMap<String, Object> stats) {
        summaryPanel.removeAll();
        
        // Create summary cards with modern soft colors
        addSummaryCard("Total Students", String.valueOf(stats.getOrDefault("totalStudents", 0)), "👥", new Color(139, 92, 246));
        addSummaryCard("Total Sections", String.valueOf(stats.getOrDefault("totalSections", 0)), "📚", new Color(16, 185, 129));
        addSummaryCard("Average Score", String.format("%.1f", (Double) stats.getOrDefault("averageScore", 0.0)), "📊", new Color(249, 115, 22));
        addSummaryCard("Top Performer", (String) stats.getOrDefault("topPerformer", "N/A"), "🏆", new Color(139, 92, 246));
        addSummaryCard("Recent Activity", String.valueOf(stats.getOrDefault("recentUpdates", 0)) + " updates", "⏰", new Color(236, 72, 153));
        addSummaryCard("Completion Rate", String.format("%.1f%%", (Double) stats.getOrDefault("completionRate", 0.0)), "✅", new Color(14, 165, 233));
        
        summaryPanel.revalidate();
        summaryPanel.repaint();
    }
    
    private void addSummaryCard(String title, String value, String icon, Color accentColor) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw white background with rounded corners
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                
                // Draw colored top border
                g2.setColor(accentColor);
                g2.fillRoundRect(0, 0, getWidth(), 4, 15, 15);
                
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(6, 6));
        card.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 18));
        iconLabel.setForeground(accentColor);
        
        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        titleLabel.setForeground(new Color(107, 114, 128));
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        valueLabel.setForeground(new Color(31, 41, 55));
        
        textPanel.add(titleLabel, BorderLayout.NORTH);
        textPanel.add(valueLabel, BorderLayout.CENTER);
        
        card.add(iconLabel, BorderLayout.WEST);
        card.add(textPanel, BorderLayout.CENTER);
        
        summaryPanel.add(card);
    }
    
    private void openBulkImportDialog() {
        JOptionPane.showMessageDialog(this, "Bulk Import feature coming soon!", "Info", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void exportDashboardData() {
        JOptionPane.showMessageDialog(this, "Export feature coming soon!", "Info", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private int getStudentCountForSection(int sectionId) {
        try {
            return studentService.getStudentCountForSection(sectionId);
        } catch (Exception e) {
            System.err.println("Error getting student count for section " + sectionId + ": " + e.getMessage());
            return 0;
        }
    }
    
    // Auto-refresh feature
    private void initializeAutoRefresh() {
        autoRefreshTimer = new Timer(30000, e -> { // 30 seconds
            if (autoRefreshEnabled) {
                refreshDashboard();
            }
        });
    }
    
    public void toggleAutoRefresh() {
        autoRefreshEnabled = !autoRefreshEnabled;
        if (autoRefreshEnabled) {
            autoRefreshTimer.start();
            JOptionPane.showMessageDialog(this, 
                "Auto-refresh enabled (30 seconds)", 
                "Auto-Refresh", 
                JOptionPane.INFORMATION_MESSAGE);
        } else {
            autoRefreshTimer.stop();
            JOptionPane.showMessageDialog(this, 
                "Auto-refresh disabled", 
                "Auto-Refresh", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
}