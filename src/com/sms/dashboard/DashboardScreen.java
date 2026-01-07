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
import com.sms.analyzer.Student;
import com.sms.dashboard.data.DashboardDataManager;
import com.sms.dashboard.components.SidebarPanel;
import com.sms.dashboard.components.SectionCardPanel;
import com.sms.dashboard.components.GradeDistributionPanel;
import com.sms.dashboard.dialogs.CreateSectionDialog;
import com.sms.dashboard.dialogs.CreateSectionPanel;
import com.sms.dashboard.dialogs.ModernStudentEntryDialog;
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
        JPanel sectionContainer = new JPanel(new BorderLayout());
        sectionContainer.setOpaque(false);
        
        // Add "Sections" heading
        JLabel sectionsHeading = new JLabel("Sections");
        sectionsHeading.setFont(new Font("SansSerif", Font.BOLD, 24));
        sectionsHeading.setForeground(TEXT_PRIMARY);
        sectionsHeading.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        sectionContainer.add(sectionsHeading, BorderLayout.NORTH);
        
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
                loadSectionCards();
                updateAnalytics();
            } catch (Exception e) {
                DashboardErrorHandler.handleError("Failed to refresh dashboard", e);
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
                List<SectionInfo> sections = sectionService.getUserSections(userId);
                SwingUtilities.invokeLater(() -> updateSectionCards(sections));
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
                // Update grade distribution
                SwingUtilities.invokeLater(() -> gradeDistPanel.updateData(userId));
                
                // Update summary statistics
                HashMap<String, Object> stats = analyticsService.getDashboardStatistics(userId);
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