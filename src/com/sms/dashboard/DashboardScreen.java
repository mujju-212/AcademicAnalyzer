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
import com.sms.dashboard.dialogs.MarkEntryDialog;
import com.sms.dashboard.dialogs.StudentEntryDialog;
import com.sms.database.DatabaseConnection;
import com.sms.login.LoginScreen;
import com.sms.analyzer.StudentAnalyzer;
import com.sms.viewtool.ViewSelectionTool;
import com.sms.dao.AnalyzerDAO;
import com.sms.dao.SectionDAO;
import com.sms.dao.SectionDAO.SectionInfo;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.plaf.basic.BasicScrollBarUI;
public class DashboardScreen extends JFrame implements DashboardActions {
    // Modern color scheme
    private static final Color BACKGROUND_COLOR = new Color(245, 247, 250);
    private static final Color CARD_COLOR = Color.WHITE;
    private static final Color PRIMARY_COLOR = new Color(99, 102, 241);
    private static final Color TEXT_PRIMARY = new Color(17, 24, 39);
    private static final Color TEXT_SECONDARY = new Color(107, 114, 128);
    private static final Color BORDER_COLOR = new Color(229, 231, 235);
    private int userId;
    private DashboardDataManager dataManager;
    private JPanel sectionCardsPanel;
    private GradeDistributionPanel gradeDistPanel;
    private JPanel rowPanel;
    private JPanel summaryPanel;
    private Timer autoRefreshTimer;
    private boolean autoRefreshEnabled = false;
    public DashboardScreen() {
        // Set modern theme
        try {
            FlatLightLaf.setup();
            UIManager.put("Button.arc", 15);
            UIManager.put("Component.arc", 15);
            UIManager.put("TextField.arc", 15);
            UIManager.put("Panel.arc", 15);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.userId = LoginScreen.currentUserId;
        dataManager = new DashboardDataManager();
        setTitle("Academic Analyzer - Dashboard");
        setSize(1300, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BACKGROUND_COLOR);
        
        // === Modern Sidebar ===
        SidebarPanel sidebar = new SidebarPanel(this);
        add(sidebar, BorderLayout.WEST);
        
        // === Main Content ===
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(BACKGROUND_COLOR);
        content.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        // Title Panel
     // In the title panel section of constructor:
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

        content.add(titlePanel, BorderLayout.NORTH);
        // === Main Dashboard Content ===
        JPanel dashboardContent = new JPanel(new BorderLayout(0, 30));
        dashboardContent.setOpaque(false);
        
        // === Section Cards Container ===
        JPanel sectionContainer = new JPanel(new BorderLayout());
        sectionContainer.setOpaque(false);
        
        // Add "Sections" heading
        JLabel sectionsHeading = new JLabel("Sections");
        sectionsHeading.setFont(new Font("SansSerif", Font.BOLD, 24));
        sectionsHeading.setForeground(TEXT_PRIMARY);
        sectionsHeading.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        sectionContainer.add(sectionsHeading, BorderLayout.NORTH);
        
        // Section Cards Panel with horizontal scroll
        sectionCardsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        sectionCardsPanel.setOpaque(false);
        
        JScrollPane sectionScrollPane = new JScrollPane(sectionCardsPanel);
        sectionScrollPane.setBorder(null);
        sectionScrollPane.setOpaque(false);
        sectionScrollPane.getViewport().setOpaque(false);
        sectionScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sectionScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        sectionScrollPane.setPreferredSize(new Dimension(0, 140));
        
        // Custom scroll bar UI
        JScrollBar horizontalBar = sectionScrollPane.getHorizontalScrollBar();
        horizontalBar.setPreferredSize(new Dimension(0, 8));
        horizontalBar.setOpaque(false);
        horizontalBar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(203, 213, 225);
                this.trackColor = new Color(241, 245, 249);
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
                return button;
            }
            
            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(thumbColor);
                g2.fillRoundRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height, 10, 10);
                g2.dispose();
            }
            
            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(trackColor);
                g2.fillRoundRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height, 10, 10);
                g2.dispose();
            }
        });
        
        sectionContainer.add(sectionScrollPane, BorderLayout.CENTER);
        
     // In the Analytics Container section:
     // === Analytics Container ===
     JPanel analyticsContainer = new JPanel(new BorderLayout());
     analyticsContainer.setOpaque(false);
     analyticsContainer.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

     // Create a panel for the entire analytics section
     JPanel analyticsContent = new JPanel(new BorderLayout(30, 20));
     analyticsContent.setOpaque(false);
  // After analyticsContainer, add:
  // === Quick Actions Panel ===
  JPanel quickActionsPanel = createQuickActionsPanel();
  dashboardContent.add(quickActionsPanel, BorderLayout.SOUTH);

     // Left side - Pie chart
     gradeDistPanel = new GradeDistributionPanel();
     analyticsContent.add(gradeDistPanel, BorderLayout.WEST);

     // Right side - Summary statistics (2x3 grid)
     summaryPanel = new JPanel(new GridLayout(2, 3, 15, 15));
     summaryPanel.setOpaque(false);
     summaryPanel.setPreferredSize(new Dimension(700, 280));
     analyticsContent.add(summaryPanel, BorderLayout.CENTER);

     analyticsContainer.add(analyticsContent, BorderLayout.NORTH);

     // Add containers to dashboard content
     dashboardContent.add(sectionContainer, BorderLayout.NORTH);
     dashboardContent.add(analyticsContainer, BorderLayout.CENTER);
        
        content.add(dashboardContent, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
        
        // Load initial data from database
        refreshSectionData();
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
     // In DashboardScreen constructor:
     // Create sidebar
  ;
        setVisible(true);
    }
    
    // DashboardActions implementation
    @Override
    public void openCreateSectionDialog() {
        CreateSectionDialog dialog = new CreateSectionDialog(this, userId);
        dialog.setVisible(true);
    }
    
    @Override
    public void openStudentEntryDialog() {
        try {
            // Update dataManager with latest sections from database
            refreshDataManager();
            StudentEntryDialog dialog = new StudentEntryDialog(this, dataManager);
            dialog.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error opening dialog: " + e.getMessage());
        }
    }
    
    @Override
    public void openAnalyzer() {
        try {
            // Initialize DAOs
            SectionDAO sectionDAO = new SectionDAO();
            AnalyzerDAO analyzerDAO = new AnalyzerDAO();
            
            // Get sections data
            HashMap<String, List<Student>> sectionStudents = new HashMap<>();
            
            // Get sections for current user
            List<SectionDAO.SectionInfo> sections = sectionDAO.getSectionsByUser(LoginScreen.currentUserId);
            
            if (sections.isEmpty()) {
                JOptionPane.showMessageDialog(
                    this,
                    "No sections found. Please create a section first.",
                    "No Sections",
                    JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }
            
            // Get students for each section
            for (SectionDAO.SectionInfo section : sections) {
                List<Student> students = analyzerDAO.getStudentsBySection(
                    section.id, 
                    LoginScreen.currentUserId
                );
                sectionStudents.put(section.sectionName, students);
            }

            // Open the analyzer
            StudentAnalyzer.openAnalyzer(
                this,                  // parent frame
                sectionStudents
            );
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Error opening analyzer: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    @Override
    public void openViewTool() { 
        // Make sure data is refreshed before opening
        dataManager.refreshData();
        
        HashMap<String, java.util.List<com.sms.analyzer.Student>> sectionStudents = dataManager.getSectionStudents();
        
        // Debug
        System.out.println("Passing sections to ViewTool: " + sectionStudents.keySet());
        
        if (sectionStudents == null || sectionStudents.isEmpty()) { 
            JOptionPane.showMessageDialog(this, "No student data available. Please add students first.", 
                "No Data", JOptionPane.INFORMATION_MESSAGE);
            return;
        } 
        
        ViewSelectionTool.openViewTool(this, sectionStudents);
    }
 // Add this method to DashboardScreen class
    
   

    private JPanel createCompactStatCard(String title, String value, Color accentColor) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Shadow
                for (int i = 0; i < 3; i++) {
                    g2.setColor(new Color(0, 0, 0, 10 - (i * 3)));
                    g2.fillRoundRect(i, i, getWidth() - i, getHeight() - i, 12, 12);
                }
                
                // Card background
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 3, getHeight() - 3, 12, 12);
                
                // Left accent stripe
                g2.setColor(accentColor);
                g2.fillRoundRect(0, 0, 4, getHeight() - 3, 2, 2);
                
                g2.dispose();
            }
        };
        
        card.setLayout(new BorderLayout());
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(250, 55));
        card.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        titleLabel.setForeground(new Color(107, 114, 128));
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        valueLabel.setForeground(accentColor);
        
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        
        return card;
    }
    private void updateSummaryStatistics(List<SectionInfo> sections) {
        summaryPanel.removeAll();
        
        // Calculate statistics
        int totalStudents = 0;
        String largestSection = "N/A";
        int maxStudents = 0;
        
        for (SectionInfo section : sections) {
            totalStudents += section.totalStudents;
            if (section.totalStudents > maxStudents) {
                maxStudents = section.totalStudents;
                largestSection = section.sectionName;
            }
        }
        
        int totalSections = sections.size();
        int avgStudents = totalSections > 0 ? totalStudents / totalSections : 0;
        
        // Get additional data from database
        String topStudent = getTopStudent();
        int studentsWithData = getStudentsWithMarksCount();
        
        // Create 2x3 grid for 6 cards
        summaryPanel.setLayout(new GridLayout(2, 3, 15, 15));
        summaryPanel.setPreferredSize(new Dimension(700, 280));
        
        // Add stat cards
        summaryPanel.add(createEnhancedStatCard(
            "Total Students", 
            String.valueOf(totalStudents),
            "Enrolled across all sections",
            new Color(99, 102, 241),
            "👥"
        ));
        
        summaryPanel.add(createEnhancedStatCard(
            "Total Sections", 
            String.valueOf(totalSections),
            "Active sections",
            new Color(34, 197, 94),
            "📚"
        ));
        
        summaryPanel.add(createEnhancedStatCard(
            "Average per Section", 
            String.valueOf(avgStudents),
            "Students per section",
            new Color(251, 146, 60),
            "📊"
        ));
        
        summaryPanel.add(createEnhancedStatCard(
            "Largest Section", 
            largestSection,
            maxStudents + " students",
            new Color(168, 85, 247),
            "🏆"
        ));
        
        summaryPanel.add(createEnhancedStatCard(
            "Data Entered", 
            String.valueOf(studentsWithData),
            "Students with marks",
            new Color(236, 72, 153),
            "✓"
        ));
        
        summaryPanel.add(createEnhancedStatCard(
            "Top Performer", 
            topStudent.equals("No data") ? "N/A" : topStudent,
            "Highest average marks",
            new Color(14, 165, 233),
            "⭐"
        ));
        
        summaryPanel.revalidate();
        summaryPanel.repaint();
    }
    private JPanel createQuickActionsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 15, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        
        // Register Students Button
        JButton registerBtn = createQuickActionButton(
            "🎓 Register Students", 
            "Add new students",
            new Color(66, 133, 244)
        );
        registerBtn.addActionListener(e -> openStudentEntryDialog());
        
        // Enter Marks Button
        JButton marksBtn = createQuickActionButton(
            "📝 Enter Marks", 
            "Enter exam marks",
            new Color(52, 168, 83)
        );
        marksBtn.addActionListener(e -> openMarkEntryDialog());
        
        // View Reports Button
        JButton reportsBtn = createQuickActionButton(
            "📊 View Reports", 
            "Generate reports",
            new Color(251, 146, 60)
        );
        reportsBtn.addActionListener(e -> openReportsDialog());
        
        // Import/Export Button
        JButton importExportBtn = createQuickActionButton(
            "📤 Import/Export", 
            "Bulk operations",
            new Color(234, 67, 53)
        );
        importExportBtn.addActionListener(e -> openImportExportDialog());
        
        panel.add(registerBtn);
        panel.add(marksBtn);
        panel.add(reportsBtn);
        panel.add(importExportBtn);
        
        return panel;
    }
    private void openMarkEntryDialog() {
        MarkEntryDialog dialog = new MarkEntryDialog(this);
        dialog.setVisible(true);
    }

    private void openReportsDialog() {
        // You'll create this dialog later
        JOptionPane.showMessageDialog(this, "Reports feature coming soon!");
    }

    private void openImportExportDialog() {
        // You'll create this dialog later
        JOptionPane.showMessageDialog(this, "Import/Export feature coming soon!");
    }
    private JPanel createEnhancedStatCard(String title, String value, String subtitle, Color accentColor, String icon) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Shadow
                for (int i = 0; i < 4; i++) {
                    g2.setColor(new Color(0, 0, 0, 15 - (i * 3)));
                    g2.fillRoundRect(i, i, getWidth() - i, getHeight() - i, 15, 15);
                }
                
                // Card background
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 15, 15);
                
                // Top colored bar
                g2.setColor(accentColor);
                g2.fillRoundRect(0, 0, getWidth() - 4, 5, 15, 15);
                g2.fillRect(0, 3, getWidth() - 4, 3);
                
                g2.dispose();
            }
        };
        
        card.setLayout(new BorderLayout());
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(200, 120));
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));
        
        // Content panel
        JPanel content = new JPanel(new BorderLayout(10, 5));
        content.setOpaque(false);
        
        // Header with icon and title
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 20));
        iconLabel.setForeground(accentColor);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        titleLabel.setForeground(new Color(107, 114, 128));
        
        header.add(iconLabel, BorderLayout.WEST);
        header.add(titleLabel, BorderLayout.CENTER);
        
        // Value
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        valueLabel.setForeground(new Color(17, 24, 39));
        
        // Subtitle
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        subtitleLabel.setForeground(new Color(156, 163, 175));
        
        content.add(header, BorderLayout.NORTH);
        content.add(valueLabel, BorderLayout.CENTER);
        content.add(subtitleLabel, BorderLayout.SOUTH);
        
        card.add(content);
        return card;
    }

    // Helper method to get top student
    private String getTopStudent() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            // First check if there are any marks entered
            String checkQuery = "SELECT COUNT(*) as count FROM student_marks WHERE created_by = ?";
            PreparedStatement checkPs = conn.prepareStatement(checkQuery);
            checkPs.setInt(1, LoginScreen.currentUserId);
            ResultSet checkRs = checkPs.executeQuery();
            
            int marksCount = 0;
            if (checkRs.next()) {
                marksCount = checkRs.getInt("count");
            }
            checkRs.close();
            checkPs.close();
            
            if (marksCount == 0) {
                conn.close();
                return "No marks data";
            }
            
            // Get top student - MySQL compatible version
            String query = "SELECT s.student_name, " +
                          "AVG((sm.marks_obtained * 100.0) / ss.max_marks) as avg_percentage " +
                          "FROM students s " +
                          "INNER JOIN student_marks sm ON s.id = sm.student_id " +
                          "INNER JOIN section_subjects ss ON (sm.subject_id = ss.subject_id AND s.section_id = ss.section_id) " +
                          "WHERE s.created_by = ? " +
                          "AND sm.marks_obtained IS NOT NULL " +
                          "AND sm.marks_obtained > 0 " +
                          "AND ss.max_marks > 0 " +
                          "GROUP BY s.id, s.student_name " +
                          "ORDER BY avg_percentage DESC " +
                          "LIMIT 1";
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, LoginScreen.currentUserId);
            ResultSet rs = ps.executeQuery();
            
            String topStudent = "No data";
            if (rs.next()) {
                topStudent = rs.getString("student_name");
                double percentage = rs.getDouble("avg_percentage");
                System.out.println("Top student: " + topStudent + " with " + percentage + "%");
                
                if (topStudent != null && topStudent.length() > 15) {
                    topStudent = topStudent.substring(0, 12) + "...";
                }
            }
            
            rs.close();
            ps.close();
            conn.close();
            
            return topStudent;
            
        } catch (SQLException e) {
            System.err.println("Error getting top student: " + e.getMessage());
            e.printStackTrace();
            return "N/A";
        }
    }

    private int getStudentsWithMarksCount() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT COUNT(DISTINCT sm.student_id) as count " +
                          "FROM student_marks sm " +
                          "INNER JOIN students s ON sm.student_id = s.id " +
                          "WHERE sm.created_by = ? " +
                          "AND sm.marks_obtained IS NOT NULL " +
                          "AND sm.marks_obtained > 0";
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, LoginScreen.currentUserId);
            ResultSet rs = ps.executeQuery();
            
            int count = 0;
            if (rs.next()) {
                count = rs.getInt("count");
            }
            
            rs.close();
            ps.close();
            conn.close();
            
            return count;
        } catch (SQLException e) {
            System.err.println("Error getting students with marks count: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    public void refreshDashboard() {
        // Show loading indicator
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Refresh data manager
                dataManager.refreshData();
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    // Refresh UI components
                    refreshSectionData();
                    
                    // Show success message (optional)
                    showRefreshNotification("Dashboard refreshed successfully");
                    
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(DashboardScreen.this, 
                        "Error refreshing dashboard: " + e.getMessage(),
                        "Refresh Error", 
                        JOptionPane.ERROR_MESSAGE);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };
        
        worker.execute();
    }

  
 

    // Add this method to initialize auto-refresh
    private void initializeAutoRefresh() {
        autoRefreshTimer = new Timer(10000, e -> { // 10 seconds
            if (autoRefreshEnabled) {
                refreshDashboard();
            }
        });
    }

    // Add toggle method
    public void toggleAutoRefresh() {
        autoRefreshEnabled = !autoRefreshEnabled;
        if (autoRefreshEnabled) {
            autoRefreshTimer.start();
            showRefreshNotification("Auto-refresh enabled (10s)");
        } else {
            autoRefreshTimer.stop();
            showRefreshNotification("Auto-refresh disabled");
        }
    }
    private void showRefreshNotification(String message) {
        // Create a semi-transparent notification panel
        JPanel notification = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                    RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Background with transparency
                g2.setColor(new Color(99, 102, 241, 230));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
        };
        
        notification.setOpaque(false);
        notification.setLayout(new BorderLayout());
        notification.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        JLabel label = new JLabel(message);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("SansSerif", Font.PLAIN, 14));
        notification.add(label);
        
        // Position at top-right corner
        JLayeredPane layeredPane = getLayeredPane();
        notification.setBounds(getWidth() - 250, 20, 230, 40);
        layeredPane.add(notification, JLayeredPane.POPUP_LAYER);
        
        // Fade out after 2 seconds
        Timer fadeTimer = new Timer(2000, e -> {
            layeredPane.remove(notification);
            layeredPane.repaint();
        });
        fadeTimer.setRepeats(false);
        fadeTimer.start();
    }
    public void refreshSectionData() {
    	
        SwingUtilities.invokeLater(() -> {
            // Clear existing section cards
            sectionCardsPanel.removeAll();
            
            // Load sections from database for current user
            SectionDAO sectionDAO = new SectionDAO();
            List<SectionInfo> sections = sectionDAO.getSectionsByUser(
                com.sms.login.LoginScreen.currentUserId);
            
            // Create cards for each section
            for (SectionInfo section : sections) {
                SectionCardPanel card = new SectionCardPanel(
                    section.sectionName, 
                    section.totalStudents
                );
                
                // Add click listener to card
                card.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        handleSectionClick(section);
                    }
                });
                
                sectionCardsPanel.add(card);
            }
            
            // Add "Create New Section" card
            sectionCardsPanel.add(createAddSectionCard());
            
            // Update pie chart with section distribution
            gradeDistPanel.updateData(sections);
            
            // Update summary statistics
            updateSummaryStatistics(sections);
            
            // Update data manager with latest sections
            updateDataManagerSections(sections);
         // In refreshSectionData(), after updating summary statistics:
         // Refresh marks data if needed
         dataManager.refreshData();
            // Refresh the UI
            sectionCardsPanel.revalidate();
            sectionCardsPanel.repaint();
        });
    }
    private JButton createQuickActionButton(String text, String tooltip, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setToolTipText(tooltip);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(200, 50));
        
        // Add hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.darker());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }
    
    private JPanel createPerformanceOverview() {
        JPanel performancePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Shadow and background
                for (int i = 0; i < 5; i++) {
                    g2.setColor(new Color(0, 0, 0, 20 - (i * 4)));
                    g2.fillRoundRect(i, i, getWidth() - i, getHeight() - i, 15, 15);
                }
                
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 5, getHeight() - 5, 15, 15);
                g2.dispose();
            }
        };
        
        performancePanel.setLayout(new BorderLayout());
        performancePanel.setOpaque(false);
        performancePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        performancePanel.setPreferredSize(new Dimension(400, 200));
        
        JLabel title = new JLabel("Performance Overview");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(new Color(17, 24, 39));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        // Add performance metrics
        JPanel metricsPanel = new JPanel(new GridLayout(3, 1, 0, 10));
        metricsPanel.setOpaque(false);
        
        // You can add metrics like:
        // - Overall class average
        // - Pass/Fail ratio
        // - Subject-wise performance
        
        performancePanel.add(title, BorderLayout.NORTH);
        performancePanel.add(metricsPanel, BorderLayout.CENTER);
        
        return performancePanel;
    }
    
    private JPanel createAddSectionCard() {
        // Store colors as final variables for inner class access
        final Color primaryColor = PRIMARY_COLOR;
        final Color hoverTextColor = new Color(120, 120, 120);
        final Color normalTextColor = new Color(150, 150, 150);
        
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw dashed border
                g2.setColor(new Color(200, 200, 200));
                g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 
                    10.0f, new float[]{10.0f}, 0.0f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 20, 20);
                
                g2.dispose();
            }
        };
        
        card.setPreferredSize(new Dimension(180, 120));
        card.setOpaque(false);
        card.setLayout(new GridBagLayout());
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        
        JLabel plusLabel = new JLabel("+");
        plusLabel.setFont(new Font("SansSerif", Font.PLAIN, 40));
        plusLabel.setForeground(normalTextColor);
        plusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel textLabel = new JLabel("Add Section");
        textLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        textLabel.setForeground(hoverTextColor);
        textLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        content.add(plusLabel);
        content.add(Box.createVerticalStrut(5));
        content.add(textLabel);
        
        card.add(content);
        
        // Add hover effect
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(new Color(250, 250, 250));
                plusLabel.setForeground(primaryColor);
                textLabel.setForeground(primaryColor);
                card.repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(null);
                plusLabel.setForeground(normalTextColor);
                textLabel.setForeground(hoverTextColor);
                card.repaint();
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                openCreateSectionDialog();
            }
        });
        
        return card;
    }
    
    private void handleSectionClick(SectionInfo section) {
        // Create a popup menu for section options
        JPopupMenu popupMenu = new JPopupMenu();
        
        JMenuItem viewDetails = new JMenuItem("View Details");
        viewDetails.addActionListener(e -> {
            // Show section details
            String details = String.format(
                "Section: %s\nTotal Students: %d\n\nSubjects:",
                section.sectionName, section.totalStudents
            );
            
            // Get subjects for this section
            SectionDAO dao = new SectionDAO();
            List<SectionDAO.SubjectInfo> subjects = dao.getSectionSubjects(section.id);
            for (SectionDAO.SubjectInfo subject : subjects) {
                details += String.format("\n- %s (Max: %d, Credit: %d)", 
                    subject.subjectName, subject.totalMarks, subject.credit);
            }
            
            JOptionPane.showMessageDialog(this, details, "Section Details", 
                JOptionPane.INFORMATION_MESSAGE);
        });
        
        JMenuItem addStudents = new JMenuItem("Add Students");
        addStudents.addActionListener(e -> openStudentEntryDialog());
        
        JMenuItem viewStudents = new JMenuItem("View Students");
        viewStudents.addActionListener(e -> openViewTool());
        
        // Add Delete Section option
        JMenuItem deleteSection = new JMenuItem("Delete Section");
        deleteSection.setForeground(new Color(220, 53, 69)); // Red color for delete
        deleteSection.addActionListener(e -> {
            // Get student count for this section
            int studentCount = getStudentCountForSection(section.id);
            
            String message;
            if (studentCount > 0) {
                message = String.format(
                    "Are you sure you want to delete '%s'?\n\n" +
                    "This section contains %d student(s).\n" +
                    "All students and their marks will be permanently deleted.\n\n" +
                    "This action cannot be undone!",
                    section.sectionName, studentCount
                );
            } else {
                message = String.format(
                    "Are you sure you want to delete '%s'?\n\n" +
                    "This action cannot be undone!",
                    section.sectionName
                );
            }
            
                      
            		int result = JOptionPane.showConfirmDialog(
                    this,
                    message,
                    "Confirm Delete Section",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                
                if (result == JOptionPane.YES_OPTION) {
                    SectionDAO dao = new SectionDAO();
                    boolean success = dao.deleteSection(section.id, com.sms.login.LoginScreen.currentUserId);
                    
                    if (success) {
                        JOptionPane.showMessageDialog(
                            this,
                            "Section '" + section.sectionName + "' deleted successfully.",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                        // Refresh the dashboard
                        refreshSectionData();
                    } else {
                        JOptionPane.showMessageDialog(
                            this,
                            "Failed to delete section. Please try again.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            });
            
            popupMenu.add(viewDetails);
            popupMenu.addSeparator();
            popupMenu.add(addStudents);
            popupMenu.add(viewStudents);
            popupMenu.addSeparator();
            popupMenu.add(deleteSection);
            
            // Show popup at mouse location
            Point mousePos = MouseInfo.getPointerInfo().getLocation();
            SwingUtilities.convertPointFromScreen(mousePos, this);
            popupMenu.show(this, mousePos.x, mousePos.y);
        }

        // Add this helper method to get student count
        private int getStudentCountForSection(int sectionId) {
            try {
                Connection conn = DatabaseConnection.getConnection();
                String query = "SELECT COUNT(*) FROM students WHERE section_id = ? AND created_by = ?";
                PreparedStatement ps = conn.prepareStatement(query);
                ps.setInt(1, sectionId);
                ps.setInt(2, com.sms.login.LoginScreen.currentUserId);
                ResultSet rs = ps.executeQuery();
                
                int count = 0;
                if (rs.next()) {
                    count = rs.getInt(1);
                }
                
                rs.close();
                ps.close();
                return count;
            } catch (SQLException e) {
                e.printStackTrace();
                return 0;
            }
        }
        
        private void refreshDataManager() {
            // Update dataManager with latest sections from database
            SectionDAO sectionDAO = new SectionDAO();
            List<SectionInfo> sections = sectionDAO.getSectionsByUser(
                com.sms.login.LoginScreen.currentUserId);
            updateDataManagerSections(sections);
        }
        
        private void updateDataManagerSections(List<SectionInfo> sections) {
            // Clear existing data
            dataManager.getSectionStudents().clear();
            
            // Add sections to dataManager (for compatibility with existing code)
            for (SectionInfo section : sections) {
                dataManager.getSectionStudents().put(section.sectionName, 
                    new java.util.ArrayList<>());
            }
        }
        
        
        public static void main(String[] args) {
            SwingUtilities.invokeLater(() -> {
                try {
                    FlatLightLaf.setup();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                // Test database connection
                DatabaseConnection.testConnection();
                
                new DashboardScreen();
            });
        }
    }