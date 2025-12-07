package com.sms.dashboard.util;

import javax.swing.*;
import java.awt.*;

public class UIHelper {
    
    // Colors
    public static final Color PRIMARY_COLOR = new Color(66, 133, 244);
    public static final Color SUCCESS_COLOR = new Color(52, 168, 83);
    public static final Color DANGER_COLOR = new Color(234, 67, 53);
    public static final Color WARNING_COLOR = new Color(251, 188, 5);
    public static final Color BACKGROUND_COLOR = new Color(245, 245, 245);
    public static final Color CARD_BACKGROUND = Color.WHITE;
    public static final Color BORDER_COLOR = new Color(220, 220, 220);
    public static final Color TEXT_PRIMARY = new Color(50, 50, 50);
    public static final Color TEXT_SECONDARY = new Color(120, 120, 120);
    
    public static void styleTextField(JTextField textField) {
        textField.setFont(new Font("Arial", Font.PLAIN, 14));
        textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        textField.setPreferredSize(new Dimension(textField.getPreferredSize().width, 40));
        textField.setAlignmentX(Component.LEFT_ALIGNMENT);
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
    }
    
    public static JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(PRIMARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(120, 40));
        
        // Add hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(PRIMARY_COLOR.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(PRIMARY_COLOR);
            }
        });
        
        return button;
    }
    
    public static JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(new Color(220, 220, 220));
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(120, 40));
        
        return button;
    }
    
    public static JPanel createCard() {
        JPanel card = new JPanel();
        card.setBackground(CARD_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        return card;
    }
    
    public static void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        comboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        comboBox.setPreferredSize(new Dimension(comboBox.getPreferredSize().width, 40));
        comboBox.setBackground(Color.WHITE);
        comboBox.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true));
    }
    
    public static void showSuccessMessage(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Success", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    public static void showErrorMessage(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", 
            JOptionPane.ERROR_MESSAGE);
    }
    
    public static void showWarningMessage(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Warning", 
            JOptionPane.WARNING_MESSAGE);
    }
}