package com.sms.theme;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicButtonUI;

import java.awt.*;

public class NeumorphicUtils {

    private static final Color BACKGROUND = new Color(245, 245, 245);
    private static final Color BORDER = new Color(225, 225, 225);
    private static final Color SHADOW = new Color(220, 220, 220);
    private static final Color LIGHT_EDGE = new Color(255, 255, 255);

    public static JPanel createNeumorphicPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();

                // Shadow effect
                g2.setColor(SHADOW);
                g2.fillRoundRect(6, 6, width - 12, height - 12, 20, 20);

                g2.setColor(LIGHT_EDGE);
                g2.fillRoundRect(0, 0, width - 12, height - 12, 20, 20);

                g2.dispose();
            }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        return panel;
    }

    public static JTextField createTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("SansSerif", Font.PLAIN, 16));
        field.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        field.setBackground(BACKGROUND);
        return field;
    }

    public static JButton createNeumorphicButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 16));
        button.setBackground(BACKGROUND);
        button.setFocusPainted(false);
        button.setForeground(Color.DARK_GRAY);
        button.setUI(new BasicButtonUI());
        button.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        return button;
    }

    public static JLabel createTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 22));
        label.setForeground(Color.DARK_GRAY);
        return label;
    }

    // Add more utility components as needed
}