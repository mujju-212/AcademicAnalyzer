package com.sms.dashboard.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class SectionCardPanel extends JPanel {
    private String sectionName;
    private int studentCount;
    private JLabel countLabel;
    private JLabel sectionLabel;
    private boolean isHovered = false;
    
    public SectionCardPanel(String sectionName, int studentCount) {
        this.sectionName = sectionName;
        this.studentCount = studentCount;
        
        setPreferredSize(new Dimension(180, 120));  // Reduced from 220x160
        setOpaque(false);
        setLayout(new GridBagLayout());
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Create content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        
        // Section name at top
        sectionLabel = new JLabel(sectionName);
        sectionLabel.setFont(new Font("SansSerif", Font.BOLD, 16));  // Reduced from 20
        sectionLabel.setForeground(new Color(31, 41, 55));
        sectionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Student count
        countLabel = new JLabel(String.valueOf(studentCount));
        countLabel.setFont(new Font("SansSerif", Font.BOLD, 32));  // Reduced from 42
        countLabel.setForeground(new Color(31, 41, 55));
        countLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Students label
        JLabel studentsLabel = new JLabel("Students");
        studentsLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));  // Reduced from 16
        studentsLabel.setForeground(new Color(107, 114, 128));
        studentsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Add components with spacing
        contentPanel.add(Box.createVerticalGlue());
        contentPanel.add(sectionLabel);
        contentPanel.add(Box.createVerticalStrut(10));  // Reduced from 15
        contentPanel.add(countLabel);
        contentPanel.add(Box.createVerticalStrut(3));   // Reduced from 5
        contentPanel.add(studentsLabel);
        contentPanel.add(Box.createVerticalGlue());
        
       
    
        
        // Add content to card
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(20, 20, 20, 20);
        add(contentPanel, gbc);
        
        // Add hover effect
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        int shadowGap = 8;
        int cornerRadius = 20;
        
        // Draw shadow
        if (isHovered) {
            // Enhanced shadow on hover
            g2.setColor(new Color(0, 0, 0, 30));
            g2.fillRoundRect(4, 4, width - 8, height - 8, cornerRadius, cornerRadius);
            g2.setColor(new Color(0, 0, 0, 20));
            g2.fillRoundRect(2, 2, width - 4, height - 4, cornerRadius, cornerRadius);
        } else {
            // Subtle shadow
            g2.setColor(new Color(0, 0, 0, 15));
            g2.fillRoundRect(2, 2, width - 4, height - 4, cornerRadius, cornerRadius);
        }
        
        // Draw card background
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(0, 0, width - shadowGap, height - shadowGap, cornerRadius, cornerRadius);
        
        // Draw border
        g2.setColor(new Color(229, 231, 235));
        g2.setStroke(new BasicStroke(1));
        g2.drawRoundRect(0, 0, width - shadowGap - 1, height - shadowGap - 1, cornerRadius, cornerRadius);
        
        g2.dispose();
    }
    
    // Alternative constructor to maintain compatibility
    public SectionCardPanel(String title, String subject) {
        this(title, 0);
    }
    
    public void updateStudentCount(int newCount) {
        this.studentCount = newCount;
        countLabel.setText(String.valueOf(newCount));
        repaint();
    }
    
    public String getSectionName() {
        return sectionName;
    }
    
    public int getStudentCount() {
        return studentCount;
    }
    
    // Add click listener support
    public void addCardClickListener(ActionListener listener) {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                listener.actionPerformed(new ActionEvent(SectionCardPanel.this, 
                    ActionEvent.ACTION_PERFORMED, sectionName));
            }
        });
    }
}