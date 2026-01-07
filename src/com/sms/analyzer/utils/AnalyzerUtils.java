package com.sms.analyzer.utils;

import javax.swing.*;
import java.awt.*;

public class AnalyzerUtils {
    // Enhanced Colors for better visibility
    public static final Color PRIMARY_COLOR = new Color(99, 102, 241);
    public static final Color PRIMARY_DARK = new Color(79, 70, 229);
    public static final Color PRIMARY_LIGHT = new Color(165, 180, 252);
    public static final Color SUCCESS_COLOR = new Color(16, 185, 129);
    public static final Color SUCCESS_LIGHT = new Color(110, 231, 183);
    public static final Color WARNING_COLOR = new Color(245, 158, 11);
    public static final Color WARNING_LIGHT = new Color(252, 211, 77);
    public static final Color DANGER_COLOR = new Color(239, 68, 68);
    public static final Color DANGER_LIGHT = new Color(252, 165, 165);
    public static final Color BACKGROUND_COLOR = new Color(245, 247, 250);
    public static final Color CARD_COLOR = Color.WHITE;
    public static final Color TEXT_PRIMARY = new Color(17, 24, 39);
    public static final Color TEXT_SECONDARY = new Color(107, 114, 128);
    public static final Color BORDER_COLOR = new Color(229, 231, 235);

    // Create modern card panel with enhanced shadow
    public static JPanel createModernCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                } catch (Exception e) {
                    // Ignore antialiasing if not supported
                }

                // Enhanced shadow with gradient
                g2.setColor(new Color(0, 0, 0, 20));
                g2.fillRoundRect(4, 6, getWidth() - 8, getHeight() - 10, 20, 20);
                g2.setColor(new Color(0, 0, 0, 10));
                g2.fillRoundRect(2, 4, getWidth() - 4, getHeight() - 6, 20, 20);
                
                // Card background
                g2.setColor(CARD_COLOR);
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 6, 20, 20);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        return card;
    }

    // Create modern button
    public static JButton createModernButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Background
                if (getModel().isPressed()) {
                    g2.setColor(PRIMARY_DARK);
                } else if (getModel().isRollover()) {
                    g2.setColor(PRIMARY_COLOR.brighter());
                } else {
                    g2.setColor(PRIMARY_COLOR);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                
                g2.dispose();
                super.paintComponent(g);
            }
        };
        
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(120, 35));
        
        return button;
    }

    // Create secondary button
    public static JButton createSecondaryButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Background
                if (getModel().isPressed()) {
                    g2.setColor(new Color(243, 244, 246));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(249, 250, 251));
                } else {
                    g2.setColor(Color.WHITE);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                
                // Border
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                
                g2.dispose();
                super.paintComponent(g);
            }
        };
        
        button.setFont(new Font("SansSerif", Font.PLAIN, 14));
        button.setForeground(TEXT_PRIMARY);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(120, 35));
        
        return button;
    }

    // Create action button with specific color
    public static JButton createActionButton(String text, Color backgroundColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Background
                if (getModel().isPressed()) {
                    g2.setColor(backgroundColor.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(backgroundColor.brighter());
                } else {
                    g2.setColor(backgroundColor);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                
                g2.dispose();
                super.paintComponent(g);
            }
        };
        
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(100, 30));
        
        return button;
    }

    // Create modern combo box
    public static JComboBox<String> createModernComboBox() {
        JComboBox<String> comboBox = new JComboBox<>();
        comboBox.setFont(new Font("SansSerif", Font.PLAIN, 14));
        comboBox.setBackground(Color.WHITE);
        comboBox.setForeground(TEXT_PRIMARY);
        comboBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        comboBox.setPreferredSize(new Dimension(150, 35));
        return comboBox;
    }

    // Safe double value conversion
    public static double safeDoubleValue(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // Get grade color based on grade
    public static Color getGradeColor(String grade) {
        if (grade == null || grade.trim().isEmpty()) {
            return TEXT_SECONDARY;
        }
        
        switch (grade.trim().toUpperCase()) {
            case "A+":
            case "A":
                return SUCCESS_COLOR;
            case "B":
                return PRIMARY_COLOR;
            case "C":
                return WARNING_COLOR;
            case "D":
            case "E":
                return new Color(251, 146, 60); // Orange
            case "F":
                return DANGER_COLOR;
            default:
                return TEXT_SECONDARY;
        }
    }
}
