package com.sms.marking.panels;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import com.sms.marking.models.*;
import com.sms.theme.ThemeManager;

public class ComponentGroupPanel extends JPanel {
    private ThemeManager themeManager;
    private ComponentGroup group;
    private ActionListener editListener;
    private ActionListener deleteListener;
    private ActionListener componentListener;
    
    public ComponentGroupPanel(ComponentGroup group) {
        this.themeManager = ThemeManager.getInstance();
        this.group = group;
        
        initializeUI();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(themeManager.getCardColor());
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(
                group.getGroupType().equals("internal") ? 
                new Color(34, 197, 94) : new Color(59, 130, 246), 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        
        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(themeManager.getCardColor());
        
        // Group info
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        infoPanel.setBackground(themeManager.getCardColor());
        
        JLabel nameLabel = new JLabel(group.getGroupName());
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        nameLabel.setForeground(themeManager.getTextPrimaryColor());
        
        JLabel typeLabel = new JLabel(group.getGroupType().toUpperCase());
        typeLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        typeLabel.setForeground(Color.WHITE);
        typeLabel.setOpaque(true);
        typeLabel.setBackground(
            group.getGroupType().equals("internal") ? 
            new Color(34, 197, 94) : new Color(59, 130, 246)
        );
        typeLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        
        JLabel marksLabel = new JLabel(group.getTotalGroupMarks() + " marks");
        marksLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        marksLabel.setForeground(themeManager.getTextSecondaryColor());
        
        infoPanel.add(nameLabel);
        infoPanel.add(typeLabel);
        infoPanel.add(marksLabel);
        
        // Selection info
        if (group.isBestOfGroup()) {
            JLabel selectionLabel = new JLabel("Best " + group.getSelectionCount() + " of " + 
                                             group.getComponents().size());
            selectionLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
            selectionLabel.setForeground(themeManager.getTextSecondaryColor());
            infoPanel.add(selectionLabel);
        }
        
        // Action buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        actionPanel.setBackground(themeManager.getCardColor());
        
        JButton addComponentBtn = createIconButton("+", new Color(34, 197, 94));
        addComponentBtn.setToolTipText("Add Component");
        
        JButton editBtn = createIconButton("✎", new Color(59, 130, 246));
        editBtn.setToolTipText("Edit Group");
        
        JButton deleteBtn = createIconButton("×", new Color(239, 68, 68));
        deleteBtn.setToolTipText("Delete Group");
        
        actionPanel.add(addComponentBtn);
        actionPanel.add(editBtn);
        actionPanel.add(deleteBtn);
        
        headerPanel.add(infoPanel, BorderLayout.WEST);
        headerPanel.add(actionPanel, BorderLayout.EAST);
        
        // Components list
        JPanel componentsPanel = new JPanel();
        componentsPanel.setLayout(new BoxLayout(componentsPanel, BoxLayout.Y_AXIS));
        componentsPanel.setBackground(themeManager.getCardColor());
        componentsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        if (group.getComponents().isEmpty()) {
            JLabel emptyLabel = new JLabel("No components added yet");
            emptyLabel.setFont(new Font("SansSerif", Font.ITALIC, 13));
            emptyLabel.setForeground(themeManager.getTextSecondaryColor());
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 0));
            componentsPanel.add(emptyLabel);
        } else {
            for (MarkingComponent component : group.getComponents()) {
                JPanel compPanel = createComponentPanel(component);
                componentsPanel.add(compPanel);
                componentsPanel.add(Box.createVerticalStrut(5));
            }
        }
        
        // Add listeners
        addComponentBtn.addActionListener(e -> {
            if (componentListener != null) {
                componentListener.actionPerformed(e);
            }
        });
        
        editBtn.addActionListener(e -> {
            if (editListener != null) {
                editListener.actionPerformed(e);
            }
        });
        
        deleteBtn.addActionListener(e -> {
            if (deleteListener != null) {
                deleteListener.actionPerformed(e);
            }
        });
        
        add(headerPanel, BorderLayout.NORTH);
        add(componentsPanel, BorderLayout.CENTER);
    }
    
    private JPanel createComponentPanel(MarkingComponent component) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 245));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(themeManager.getBorderColor()),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        // Component info
        JLabel nameLabel = new JLabel("• " + component.getComponentName());
        nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        
        JLabel marksLabel = new JLabel(component.getActualMaxMarks() + " marks");
        marksLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        marksLabel.setForeground(themeManager.getTextSecondaryColor());
        
        if (component.isOptional()) {
            JLabel optionalLabel = new JLabel("[Optional]");
            optionalLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
            optionalLabel.setForeground(new Color(255, 140, 0));
            
            JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            leftPanel.setBackground(panel.getBackground());
            leftPanel.add(nameLabel);
            leftPanel.add(optionalLabel);
            
            panel.add(leftPanel, BorderLayout.WEST);
        } else {
            panel.add(nameLabel, BorderLayout.WEST);
        }
        
        panel.add(marksLabel, BorderLayout.EAST);
        
        return panel;
    }
    
    private JButton createIconButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 16));
        button.setForeground(color);
        button.setBackground(themeManager.getCardColor());
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(30, 30));
        
        // Add hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setContentAreaFilled(true);
                button.setBackground(color.brighter().brighter());
                button.setForeground(Color.WHITE);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setContentAreaFilled(false);
                button.setForeground(color);
            }
        });
        
        return button;
    }
    
    public void addEditListener(ActionListener listener) {
        this.editListener = listener;
    }
    
    public void addDeleteListener(ActionListener listener) {
        this.deleteListener = listener;
    }
    
    public void addComponentListener(ActionListener listener) {
        this.componentListener = listener;
    }
}