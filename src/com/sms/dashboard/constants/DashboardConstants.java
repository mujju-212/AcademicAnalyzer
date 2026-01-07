package com.sms.dashboard.constants;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Constants class for Dashboard UI and behavior configuration
 */
public class DashboardConstants {
    
    // Colors
    public static final Color PRIMARY_COLOR = new Color(99, 102, 241);
    public static final Color SECONDARY_COLOR = new Color(236, 237, 240);
    public static final Color BACKGROUND_COLOR = new Color(248, 250, 252);
    public static final Color CARD_BACKGROUND = Color.WHITE;
    public static final Color TEXT_PRIMARY = new Color(17, 24, 39);
    public static final Color TEXT_SECONDARY = new Color(75, 85, 99);
    public static final Color SUCCESS_COLOR = new Color(34, 197, 94);
    public static final Color WARNING_COLOR = new Color(251, 191, 36);
    public static final Color ERROR_COLOR = new Color(220, 53, 69);
    
    // Notification colors
    public static final Color NOTIFICATION_BACKGROUND = new Color(99, 102, 241, 230);
    public static final Color NOTIFICATION_TEXT = Color.WHITE;
    
    // Hover colors
    public static final Color HOVER_TEXT_COLOR = new Color(120, 120, 120);
    public static final Color NORMAL_TEXT_COLOR = new Color(150, 150, 150);
    
    // Card colors
    public static final Color CARD_HOVER_BACKGROUND = new Color(250, 250, 250);
    public static final Color DASHED_BORDER_COLOR = new Color(200, 200, 200);
    
    // Dimensions
    public static final Dimension SECTION_CARD_SIZE = new Dimension(180, 120);
    public static final Dimension QUICK_ACTION_BUTTON_SIZE = new Dimension(200, 50);
    public static final Dimension PERFORMANCE_PANEL_SIZE = new Dimension(400, 200);
    public static final Dimension NOTIFICATION_SIZE = new Dimension(230, 40);
    
    // Layout constants
    public static final int SIDEBAR_WIDTH = 280;
    public static final int CARD_SPACING = 15;
    public static final int PANEL_PADDING = 20;
    public static final int BORDER_RADIUS = 15;
    public static final int SMALL_BORDER_RADIUS = 10;
    public static final int SHADOW_OFFSET = 5;
    
    // Timing constants (milliseconds)
    public static final int AUTO_REFRESH_INTERVAL = 10000; // 10 seconds
    public static final int NOTIFICATION_DISPLAY_TIME = 2000; // 2 seconds
    public static final int FADE_ANIMATION_DELAY = 50;
    
    // Fonts
    public static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 20);
    public static final Font SUBTITLE_FONT = new Font("SansSerif", Font.BOLD, 16);
    public static final Font BODY_FONT = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font SMALL_FONT = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 14);
    public static final Font LARGE_ICON_FONT = new Font("SansSerif", Font.PLAIN, 40);
    
    // String length limits
    public static final int MAX_STUDENT_NAME_DISPLAY_LENGTH = 15;
    public static final int TRUNCATED_NAME_LENGTH = 12;
    
    // Grid layout configurations
    public static final int SUMMARY_STATS_ROWS = 2;
    public static final int SUMMARY_STATS_COLS = 3;
    public static final int PERFORMANCE_METRICS_ROWS = 3;
    public static final int PERFORMANCE_METRICS_COLS = 1;
    
    // Border configurations
    public static final float DASHED_BORDER_WIDTH = 2.0f;
    public static final float[] DASHED_BORDER_PATTERN = {10.0f};
    
    // Animation and visual effects
    public static final int SHADOW_LAYERS = 5;
    public static final int SHADOW_OPACITY_START = 20;
    public static final int SHADOW_OPACITY_DECREMENT = 4;
    
    // Database query limits
    public static final int TOP_STUDENT_LIMIT = 1;
    
    // UI Messages
    public static final String NO_DATA_MESSAGE = "No data";
    public static final String NA_MESSAGE = "N/A";
    public static final String ADD_SECTION_TEXT = "Add Section";
    public static final String PLUS_SYMBOL = "+";
    
    // Tooltips
    public static final String REFRESH_TOOLTIP = "Refresh dashboard data";
    public static final String AUTO_REFRESH_ENABLED_MESSAGE = "Auto-refresh enabled (10s)";
    public static final String AUTO_REFRESH_DISABLED_MESSAGE = "Auto-refresh disabled";
    public static final String DASHBOARD_REFRESHED_MESSAGE = "Dashboard refreshed successfully";
    
    // Dialog messages
    public static final String DELETE_SECTION_TITLE = "Confirm Delete Section";
    public static final String DELETE_SECTION_WARNING = "This action cannot be undone!";
    public static final String SUCCESS_TITLE = "Success";
    public static final String ERROR_TITLE = "Error";
    public static final String REFRESH_ERROR_TITLE = "Refresh Error";
    
    // Performance thresholds
    public static final double PASS_THRESHOLD = 50.0;
    
    // Private constructor to prevent instantiation
    private DashboardConstants() {
        throw new IllegalStateException("Constants class should not be instantiated");
    }
}