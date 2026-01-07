package com.sms.resultlauncher;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import com.formdev.flatlaf.FlatLightLaf;

import com.sms.calculation.models.Component;

public class ResultLauncher extends JPanel {
    
    // Components
    private JFrame parentFrame;
    private Runnable onCloseCallback;
    private SectionSelectionPanel sectionPanel;
    private StudentSelectionPanel studentPanel;
    private ComponentSelectionPanel componentPanel;
    private LaunchedResultsPanel resultsPanel;
    
    // Data
    private ResultLauncherDAO dao;
    private int selectedSectionId = -1;
    private List<Integer> selectedStudentIds;
    private List<Component> selectedComponents;
    
    // Constructor for standalone dialog (backward compatibility)
    public ResultLauncher(JFrame parent) {
        this(parent, null);
        if (parent != null) {
            JDialog dialog = new JDialog(parent, "Result Launcher", true);
            dialog.setSize(1400, 900);
            dialog.setMinimumSize(new Dimension(1200, 700));
            dialog.setLocationRelativeTo(parent);
            dialog.setLayout(new BorderLayout());
            dialog.getContentPane().setBackground(ResultLauncherUtils.BACKGROUND_COLOR);
            dialog.add(this, BorderLayout.CENTER);
            dialog.setVisible(true);
        }
    }
    
    // Constructor for embedded panel
    public ResultLauncher(JFrame parent, Runnable onCloseCallback) {
        this.parentFrame = parent;
        this.onCloseCallback = onCloseCallback;
        this.dao = new ResultLauncherDAO();
        this.selectedStudentIds = new ArrayList<>();
        this.selectedComponents = new ArrayList<>();
        
        // Set up FlatLaf
        try {
            FlatLightLaf.setup();
        } catch (Exception e) {
            System.err.println("Error setting up FlatLaf: " + e.getMessage());
        }
        
        setLayout(new BorderLayout());
        setBackground(ResultLauncherUtils.BACKGROUND_COLOR);
        
        initializeUI();
    }
    
    private void initializeUI() {
        setBackground(ResultLauncherUtils.BACKGROUND_COLOR);
        
        createHeaderPanel();
        createMainContent();
    }
    
    private void createHeaderPanel() {
        JPanel headerPanel = ResultLauncherUtils.createModernCard();
        headerPanel.setLayout(new BorderLayout(20, 0));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);
        
        JButton backButton = createBackButton();
        JLabel titleLabel = new JLabel("Result Launcher");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setForeground(ResultLauncherUtils.PRIMARY_COLOR);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        
        leftPanel.add(backButton);
        leftPanel.add(titleLabel);
        
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);
        
        JButton refreshButton = ResultLauncherUtils.createActionButton("🔄 Refresh", ResultLauncherUtils.INFO_COLOR);
        JButton helpButton = ResultLauncherUtils.createActionButton("❓ Help", ResultLauncherUtils.WARNING_COLOR);
        
        refreshButton.addActionListener(e -> refreshData());
        helpButton.addActionListener(e -> showHelp());
        
        rightPanel.add(helpButton);
        rightPanel.add(refreshButton);
        
        headerPanel.add(leftPanel, BorderLayout.WEST);
        headerPanel.add(rightPanel, BorderLayout.EAST);
        
        add(headerPanel, BorderLayout.NORTH);
    }
    
    private JButton createBackButton() {
        JButton backButton = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int arc = 10;
                if (getModel().isPressed()) {
                    g2.setColor(ResultLauncherUtils.PRIMARY_DARK.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(ResultLauncherUtils.PRIMARY_COLOR.brighter());
                } else {
                    g2.setColor(ResultLauncherUtils.PRIMARY_COLOR);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2));
                int centerY = getHeight() / 2;
                int centerX = getWidth() / 2;
                
                g2.drawLine(centerX - 5, centerY, centerX + 5, centerY - 5);
                g2.drawLine(centerX - 5, centerY, centerX + 5, centerY + 5);
                g2.drawLine(centerX - 5, centerY, centerX + 8, centerY);
                
                g2.dispose();
            }
        };
        
        backButton.setPreferredSize(new Dimension(40, 35));
        backButton.setContentAreaFilled(false);
        backButton.setBorderPainted(false);
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.setToolTipText("Back to Dashboard");
        
        backButton.addActionListener(e -> {
            if (onCloseCallback != null) {
                onCloseCallback.run();
            }
        });
        
        return backButton;
    }
    
    private void createMainContent() {
        JPanel mainPanel = new JPanel(new BorderLayout(15, 0));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 15, 15, 15));
        mainPanel.setOpaque(false);
        
        // Left panel with selection components
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setPreferredSize(new Dimension(400, 0));
        leftPanel.setOpaque(false);
        
        // Section selection
        sectionPanel = new SectionSelectionPanel(this);
        sectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Student selection
        studentPanel = new StudentSelectionPanel(this);
        studentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Component selection
        componentPanel = new ComponentSelectionPanel(this);
        componentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Launch button
        JPanel launchPanel = createLaunchPanel();
        launchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        leftPanel.add(sectionPanel);
        leftPanel.add(Box.createVerticalStrut(5));
        leftPanel.add(studentPanel);
        leftPanel.add(Box.createVerticalStrut(5));
        leftPanel.add(componentPanel);
        leftPanel.add(Box.createVerticalStrut(8));
        leftPanel.add(launchPanel);
        leftPanel.add(Box.createVerticalGlue());
        
        JScrollPane leftScrollPane = new JScrollPane(leftPanel);
        leftScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        leftScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        leftScrollPane.setBorder(BorderFactory.createEmptyBorder());
        leftScrollPane.setPreferredSize(new Dimension(420, 0));
        leftScrollPane.getViewport().setOpaque(false);
        leftScrollPane.setOpaque(false);
        
        // Right panel with launched results
        resultsPanel = new LaunchedResultsPanel(this);
        
        mainPanel.add(leftScrollPane, BorderLayout.WEST);
        mainPanel.add(resultsPanel, BorderLayout.CENTER);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private JPanel createLaunchPanel() {
        JPanel panel = ResultLauncherUtils.createModernCard();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20)); // Reduced padding from 20 to 15
        
        // Make the panel have a fixed height
        panel.setPreferredSize(new Dimension(380, 100)); // Fixed height
        panel.setMaximumSize(new Dimension(380, 100));
        panel.setMinimumSize(new Dimension(380, 100));
        
        JLabel titleLabel = new JLabel("🚀 Launch Results");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(ResultLauncherUtils.PRIMARY_COLOR);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5)); // Reduced vertical gap
        buttonPanel.setOpaque(false);
        
        JButton launchButton = ResultLauncherUtils.createModernButton("🚀 Launch Results");
        launchButton.setPreferredSize(new Dimension(150, 35)); // Slightly smaller
        launchButton.addActionListener(e -> launchResults());
        
        JButton previewButton = ResultLauncherUtils.createSecondaryButton("👁️ Preview");
        previewButton.setPreferredSize(new Dimension(100, 35)); // Slightly smaller
        previewButton.addActionListener(e -> previewResults());
        
        buttonPanel.add(previewButton);
        buttonPanel.add(launchButton);
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    // Event handlers
    public void onSectionSelected(int sectionId) {
        this.selectedSectionId = sectionId;
        studentPanel.loadStudentsForSection(sectionId);
        componentPanel.loadComponentsForSection(sectionId);
        System.out.println("Section selected: " + sectionId);
    }
    
    public void onStudentsSelected(List<Integer> studentIds) {
        this.selectedStudentIds = new ArrayList<>(studentIds);
        System.out.println("Students selected: " + studentIds.size());
    }
    
    public void onComponentsSelected(List<Component> components) {
        this.selectedComponents = new ArrayList<>(components);
        System.out.println("Components selected: " + components.size());
    }
    private void launchResults() {
        System.out.println("=== UI LAUNCH DEBUG ===");
        System.out.println("Launch button clicked");
        
        if (!validateSelections()) {
            System.out.println("Validation failed");
            return;
        }
        
        System.out.println("Validation passed");
        
        // Show launch configuration dialog
        LaunchConfigurationDialog configDialog = new LaunchConfigurationDialog(parentFrame);
        configDialog.setVisible(true);
        
        if (configDialog.isConfirmed()) {
            ResultConfiguration config = configDialog.getConfiguration();
            System.out.println("Got configuration: " + config.getLaunchName());
            
            // Disable the launch button and show progress
            JButton launchButton = findLaunchButton();
            if (launchButton != null) {
                launchButton.setEnabled(false);
                launchButton.setText("🔄 Launching...");
            }
            
            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    System.out.println("=== SWING WORKER STARTED ===");
                    System.out.println("Calling DAO.launchResults...");
                    
                    boolean result = dao.launchResults(selectedSectionId, selectedStudentIds, 
                                                   selectedComponents, config);
                    
                    System.out.println("DAO.launchResults returned: " + result);
                    return result;
                }
                
                @Override
                protected void done() {
                    System.out.println("=== SWING WORKER DONE ===");
                    
                    // Re-enable the launch button
                    if (launchButton != null) {
                        launchButton.setEnabled(true);
                        launchButton.setText("🚀 Launch Results");
                    }
                    
                    try {
                        Boolean success = get();
                        System.out.println("Worker result: " + success);
                        
                        if (success) {
                            JOptionPane.showMessageDialog(ResultLauncher.this,
                                "Results launched successfully!",
                                "Launch Successful", JOptionPane.INFORMATION_MESSAGE);
                            resultsPanel.refreshLaunchedResults();
                        } else {
                            JOptionPane.showMessageDialog(ResultLauncher.this,
                                "Failed to launch results. Please check console for details.",
                                "Launch Failed", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception e) {
                        System.err.println("Error in worker done(): " + e.getMessage());
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(ResultLauncher.this,
                            "Error launching results: " + e.getMessage(),
                            "Launch Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            
            System.out.println("Starting swing worker...");
            worker.execute();
            System.out.println("Swing worker started");
        }
    }

    // Helper method to find the launch button
    private JButton findLaunchButton() {
        // You'll need to store a reference to the launch button or find it in the component tree
        // For now, return null - we'll implement this if needed
        return null;
    }
    
    private void previewResults() {
        if (!validateSelections()) {
            return;
        }
        
        // Show preview dialog
        ResultPreviewDialog previewDialog = new ResultPreviewDialog(parentFrame, 
            selectedSectionId, selectedStudentIds, selectedComponents);
        previewDialog.setVisible(true);
    }
    
    private boolean validateSelections() {
        if (selectedSectionId == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a section first.",
                "No Section Selected", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        if (selectedStudentIds.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please select at least one student.",
                "No Students Selected", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        if (selectedComponents.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please select at least one component.",
                "No Components Selected", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        return true;
    }
    
    private void refreshData() {
        sectionPanel.refreshSections();
        resultsPanel.refreshLaunchedResults();
    }
    
    private void showHelp() {
        String helpText = """
            Result Launcher Help:
            
            1. Select Section: Choose the section for which you want to launch results.
            2. Select Students: Choose specific students or select all students in the section.
            3. Select Components: Choose which assessment components to include in the results.
            4. Preview: Review the results before launching.
            5. Launch: Make the results live for students to view on the web portal.
            
            Features:
            - Email notifications are sent to students when results are launched
            - You can take down results anytime from the Launched Results panel
            - Students can access results via the web portal using their credentials
            
            Note: Make sure student email addresses are properly configured in the system.
            """;
        
        JOptionPane.showMessageDialog(this, helpText, 
            "Help", JOptionPane.INFORMATION_MESSAGE);
    }
    
    // Getters
    public ResultLauncherDAO getDAO() { return dao; }
    public int getSelectedSectionId() { return selectedSectionId; }
    public List<Integer> getSelectedStudentIds() { return selectedStudentIds; }
    public List<Component> getSelectedComponents() { return selectedComponents; }
    
    // Static method to open from dashboard
    public static void openResultLauncher(JFrame parent) {
        SwingUtilities.invokeLater(() -> {
            new ResultLauncher(parent).setVisible(true);
        });
    }
}