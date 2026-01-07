package com.sms.dashboard.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static com.sms.dashboard.constants.DashboardConstants.*;

/**
 * Factory class for creating styled UI components
 */
public class UIComponentFactory {
    
    /**
     * Creates a styled panel with shadow effect
     */
    public static JPanel createStyledPanel(final Color backgroundColor, final int borderRadius) {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Shadow effect
                for (int i = 0; i < SHADOW_LAYERS; i++) {
                    g2.setColor(new Color(0, 0, 0, SHADOW_OPACITY_START - (i * SHADOW_OPACITY_DECREMENT)));
                    g2.fillRoundRect(i, i, getWidth() - i, getHeight() - i, borderRadius, borderRadius);
                }
                
                // Background
                g2.setColor(backgroundColor != null ? backgroundColor : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - SHADOW_OFFSET, getHeight() - SHADOW_OFFSET, borderRadius, borderRadius);
                g2.dispose();
            }
        };
    }
    
    /**
     * Creates a styled button with hover effects
     */
    public static JButton createStyledButton(String text, String tooltip, Color bgColor, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setToolTipText(tooltip);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(QUICK_ACTION_BUTTON_SIZE);
        
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
        
        // Add action listener
        if (action != null) {
            button.addActionListener(e -> action.run());
        }
        
        return button;
    }
    
    /**
     * Creates a styled label with specified font and color
     */
    public static JLabel createStyledLabel(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }
    
    /**
     * Creates a titled label with consistent styling
     */
    public static JLabel createTitleLabel(String text) {
        return createStyledLabel(text, TITLE_FONT, TEXT_PRIMARY);
    }
    
    /**
     * Creates a subtitle label with consistent styling
     */
    public static JLabel createSubtitleLabel(String text) {
        return createStyledLabel(text, SUBTITLE_FONT, TEXT_PRIMARY);
    }
    
    /**
     * Creates a body text label with consistent styling
     */
    public static JLabel createBodyLabel(String text) {
        return createStyledLabel(text, BODY_FONT, TEXT_SECONDARY);
    }
    
    /**
     * Creates an add section card with dashed border
     */
    public static JPanel createAddSectionCard(Runnable onClickAction) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw dashed border
                g2.setColor(DASHED_BORDER_COLOR);
                g2.setStroke(new BasicStroke(DASHED_BORDER_WIDTH, BasicStroke.CAP_BUTT, 
                    BasicStroke.JOIN_MITER, 10.0f, DASHED_BORDER_PATTERN, 0.0f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 
                    SMALL_BORDER_RADIUS, SMALL_BORDER_RADIUS);
                
                g2.dispose();
            }
        };
        
        card.setPreferredSize(SECTION_CARD_SIZE);
        card.setOpaque(false);
        card.setLayout(new GridBagLayout());
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        
        JLabel plusLabel = createStyledLabel(PLUS_SYMBOL, LARGE_ICON_FONT, NORMAL_TEXT_COLOR);
        plusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel textLabel = createStyledLabel(ADD_SECTION_TEXT, BODY_FONT, HOVER_TEXT_COLOR);
        textLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        content.add(plusLabel);
        content.add(Box.createVerticalStrut(5));
        content.add(textLabel);
        
        card.add(content);
        
        // Add hover effect
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(CARD_HOVER_BACKGROUND);
                plusLabel.setForeground(PRIMARY_COLOR);
                textLabel.setForeground(PRIMARY_COLOR);
                card.repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(null);
                plusLabel.setForeground(NORMAL_TEXT_COLOR);
                textLabel.setForeground(HOVER_TEXT_COLOR);
                card.repaint();
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (onClickAction != null) {
                    onClickAction.run();
                }
            }
        });
        
        return card;
    }
    
    /**
     * Creates a notification panel with transparency
     */
    public static JPanel createNotificationPanel(String message) {
        JPanel notification = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Background with transparency
                g2.setColor(NOTIFICATION_BACKGROUND);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), SMALL_BORDER_RADIUS, SMALL_BORDER_RADIUS);
                g2.dispose();
            }
        };
        
        notification.setOpaque(false);
        notification.setLayout(new BorderLayout());
        notification.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        JLabel label = createStyledLabel(message, BODY_FONT, NOTIFICATION_TEXT);
        notification.add(label);
        
        return notification;
    }
    
    /**
     * Creates a primary button with neumorphic styling
     */
    public static JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setBackground(PRIMARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(120, 40));
        
        // Add hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(PRIMARY_COLOR.darker());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(PRIMARY_COLOR);
            }
        });
        
        return button;
    }
    
    /**
     * Creates a secondary button with neumorphic styling
     */
    public static JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setBackground(CARD_BACKGROUND);
        button.setForeground(TEXT_PRIMARY);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(120, 40));
        
        // Add hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(CARD_BACKGROUND.darker());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(CARD_BACKGROUND);
            }
        });
        
        return button;
    }
    
    /**
     * Creates a card panel with neumorphic styling
     */
    public static JPanel createCard() {
        return createStyledPanel(CARD_BACKGROUND, BORDER_RADIUS);
    }
}