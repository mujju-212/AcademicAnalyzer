package com.sms.theme;

import javax.swing.*;
import java.awt.*;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import java.util.prefs.Preferences;

public class ThemeManager {
    private static ThemeManager instance;
    private boolean isDarkMode = false;
    private static final String THEME_PREF_KEY = "app_theme_dark";
    private Preferences prefs;
    
    // Light theme colors
    public static final Color LIGHT_BACKGROUND = new Color(245, 247, 250);
    public static final Color LIGHT_CARD = Color.WHITE;
    public static final Color LIGHT_PRIMARY = new Color(99, 102, 241);
    public static final Color LIGHT_TEXT_PRIMARY = new Color(17, 24, 39);
    public static final Color LIGHT_TEXT_SECONDARY = new Color(107, 114, 128);
    public static final Color LIGHT_BORDER = new Color(229, 231, 235);
    
    // Dark theme colors
    public static final Color DARK_BACKGROUND = new Color(17, 24, 39);
    public static final Color DARK_CARD = new Color(31, 41, 55);
    public static final Color DARK_PRIMARY = new Color(99, 102, 241);
    public static final Color DARK_TEXT_PRIMARY = new Color(243, 244, 246);
    public static final Color DARK_TEXT_SECONDARY = new Color(156, 163, 175);
    public static final Color DARK_BORDER = new Color(55, 65, 81);
    
    private ThemeManager() {
        prefs = Preferences.userNodeForPackage(ThemeManager.class);
        isDarkMode = prefs.getBoolean(THEME_PREF_KEY, false);
    }
    
    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }
    
    public void toggleTheme() {
        isDarkMode = !isDarkMode;
        prefs.putBoolean(THEME_PREF_KEY, isDarkMode);
        applyTheme();
    }
    
    public void applyTheme() {
        try {
            if (isDarkMode) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
            
            // Update UI defaults
            UIManager.put("Button.arc", 15);
            UIManager.put("Component.arc", 15);
            UIManager.put("TextField.arc", 15);
            UIManager.put("Panel.arc", 15);
            
            // Update all windows
            for (Window window : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(window);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public boolean isDarkMode() {
        return isDarkMode;
    }
    
    // Get current theme colors
    public Color getBackgroundColor() {
        return isDarkMode ? DARK_BACKGROUND : LIGHT_BACKGROUND;
    }
    
    public Color getCardColor() {
        return isDarkMode ? DARK_CARD : LIGHT_CARD;
    }
    
    public Color getPrimaryColor() {
        return isDarkMode ? DARK_PRIMARY : LIGHT_PRIMARY;
    }
    
    public Color getTextPrimaryColor() {
        return isDarkMode ? DARK_TEXT_PRIMARY : LIGHT_TEXT_PRIMARY;
    }
    
    public Color getTextSecondaryColor() {
        return isDarkMode ? DARK_TEXT_SECONDARY : LIGHT_TEXT_SECONDARY;
    }
    
    public Color getBorderColor() {
        return isDarkMode ? DARK_BORDER : LIGHT_BORDER;
    }

	public Color getSecondaryColor() {
		// TODO Auto-generated method stub
		return null;
	}
}