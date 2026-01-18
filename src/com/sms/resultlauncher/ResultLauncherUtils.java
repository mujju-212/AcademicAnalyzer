package com.sms.resultlauncher;


import javax.swing.*;
import java.awt.*;
import java.awt.Dialog.ModalityType;
import java.awt.geom.RoundRectangle2D;

public class ResultLauncherUtils {
    // Color Constants
    public static final Color BACKGROUND_COLOR = new Color(245, 247, 250);
    public static final Color CARD_COLOR = Color.WHITE;
    public static final Color PRIMARY_COLOR = new Color(99, 102, 241);
    public static final Color PRIMARY_DARK = new Color(79, 70, 229);
    public static final Color TEXT_PRIMARY = new Color(17, 24, 39);
    public static final Color TEXT_SECONDARY = new Color(107, 114, 128);
    public static final Color BORDER_COLOR = new Color(229, 231, 235);
    public static final Color SUCCESS_COLOR = new Color(34, 197, 94);
    public static final Color DANGER_COLOR = new Color(239, 68, 68);
    public static final Color WARNING_COLOR = new Color(251, 146, 60);
    public static final Color INFO_COLOR = new Color(59, 130, 246);

    /**
     * Create a modern card panel with shadow and rounded corners
     */
    public static JPanel createModernCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

                int arc = 15;
                int shadowOffset = 2;

                // Draw shadow
                g2.setColor(new Color(0, 0, 0, 20));
                g2.fillRoundRect(shadowOffset, shadowOffset, getWidth() - shadowOffset, getHeight() - shadowOffset, arc, arc);

                // Draw card background
                g2.setColor(CARD_COLOR);
                g2.fillRoundRect(0, 0, getWidth() - shadowOffset, getHeight() - shadowOffset, arc, arc);

                // Draw border
                g2.setColor(new Color(0, 0, 0, 30));
                g2.drawRoundRect(0, 0, getWidth() - shadowOffset - 1, getHeight() - shadowOffset - 1, arc, arc);

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        return card;
    }

    /**
     * Create a modern primary button
     */
    public static JButton createModernButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int arc = 10;

                if (getModel().isPressed()) {
                    g2.setPaint(new GradientPaint(0, 0, PRIMARY_DARK.darker(), 0, getHeight(), PRIMARY_COLOR.darker()));
                } else if (getModel().isRollover()) {
                    g2.setPaint(new GradientPaint(0, 0, PRIMARY_COLOR.brighter(), 0, getHeight(), PRIMARY_COLOR.darker()));
                } else {
                    g2.setPaint(new GradientPaint(0, 0, PRIMARY_COLOR, 0, getHeight(), PRIMARY_DARK));
                }
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), arc, arc));

                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);

                g2.dispose();
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

    /**
     * Create a modern secondary button
     */
    public static JButton createSecondaryButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int arc = 10;

                if (getModel().isPressed()) {
                    g2.setColor(new Color(240, 240, 240));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(250, 250, 250));
                } else {
                    g2.setColor(Color.WHITE);
                }
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), arc, arc));

                g2.setColor(BORDER_COLOR);
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1.5, getHeight() - 1.5, arc, arc));

                g2.setColor(TEXT_PRIMARY);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);

                g2.dispose();
            }
        };
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setForeground(TEXT_PRIMARY);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(120, 35));
        return button;
    }

    /**
     * Create an action button with custom color
     */
    public static JButton createActionButton(String text, Color color) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int arc = 8;

                if (getModel().isPressed()) {
                    g2.setColor(color.darker().darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(color.brighter());
                } else {
                    g2.setColor(color);
                }
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), arc, arc));

                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);

                g2.dispose();
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

    /**
     * Create a modern combo box
     */
    public static JComboBox<String> createModernComboBox() {
        JComboBox<String> combo = new JComboBox<String>() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int arc = 8;

                g2.setColor(new Color(249, 250, 251));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), arc, arc));

                g2.setColor(BORDER_COLOR);
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1.5, getHeight() - 1.5, arc, arc));

                g2.dispose();
                super.paintComponent(g);
            }
        };
        combo.setOpaque(false);
        combo.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        combo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        combo.setPreferredSize(new Dimension(0, 35));
        return combo;
    }

    /**
     * Show a modern loading dialog
     */
 // In ResultLauncherUtils.java, change the method signature:
    public static JDialog showLoadingDialog(Window parent, String message) {
        System.out.println("Creating loading dialog with message: " + message);
        
        JDialog dialog = new JDialog(parent, "Loading", ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setSize(300, 150);
        dialog.setLocationRelativeTo(parent);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        panel.setBackground(CARD_COLOR);
        
        JLabel spinnerLabel = new JLabel("⏳");
        spinnerLabel.setFont(new Font("SansSerif", Font.PLAIN, 24));
        spinnerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        messageLabel.setForeground(TEXT_PRIMARY);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        panel.add(spinnerLabel);
        panel.add(Box.createVerticalStrut(15));
        panel.add(messageLabel);
        
        dialog.add(panel);
        
        System.out.println("Loading dialog created successfully");
        return dialog;
    }

    /**
     * Format percentage for display
     */
    public static String formatPercentage(double percentage) {
        return String.format("%.2f%%", percentage);
    }
    
    /**
     * Format marks for display
     */
    public static String formatMarks(double obtained, double maximum) {
        return String.format("%.1f/%.1f", obtained, maximum);
    }
    
    /**
     * Truncate text to specified length
     */
    public static String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}