package com.sms.dashboard.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import com.sms.dao.SectionEditDAO;
import com.sms.dashboard.dialogs.CreateSectionDialog;

public class SectionCardPanel extends JPanel {
    private String sectionName;
    private int studentCount;
    private int sectionId;
    private int userId;
    private Runnable refreshCallback;
    private com.sms.dashboard.DashboardScreen dashboardScreen;
    private JLabel countLabel;
    private JLabel sectionLabel;
    private boolean isHovered = false;
    
    public SectionCardPanel(String sectionName, int studentCount, int sectionId, int userId, Runnable refreshCallback) {
        this(sectionName, studentCount, sectionId, userId, refreshCallback, null);
    }
    
    public SectionCardPanel(String sectionName, int studentCount, int sectionId, int userId, Runnable refreshCallback, com.sms.dashboard.DashboardScreen dashboardScreen) {
        this.sectionName = sectionName;
        this.studentCount = studentCount;
        this.sectionId = sectionId;
        this.userId = userId;
        this.refreshCallback = refreshCallback;
        this.dashboardScreen = dashboardScreen;
        
        setPreferredSize(new Dimension(140, 65));
        setOpaque(false);
        setLayout(new GridBagLayout());
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Create content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        
        // Section name at top
        sectionLabel = new JLabel(sectionName);
        sectionLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        sectionLabel.setForeground(new Color(31, 41, 55));
        sectionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Student count
        countLabel = new JLabel(String.valueOf(studentCount));
        countLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        countLabel.setForeground(new Color(31, 41, 55));
        countLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Students label
        JLabel studentsLabel = new JLabel("Students");
        studentsLabel.setFont(new Font("SansSerif", Font.PLAIN, 9));
        studentsLabel.setForeground(new Color(107, 114, 128));
        studentsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Add components with spacing
        contentPanel.add(Box.createVerticalGlue());
        contentPanel.add(sectionLabel);
        contentPanel.add(Box.createVerticalStrut(1));
        contentPanel.add(countLabel);
        contentPanel.add(Box.createVerticalStrut(0));
        contentPanel.add(studentsLabel);
        contentPanel.add(Box.createVerticalGlue());
        
        // Add content to card
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(6, 6, 6, 6);
        add(contentPanel, gbc);
        
        // Only add right-click menu if sectionId is valid (not 0)
        // For library cards (sectionId=0), YearSemesterPanel will add its own listeners
        if (sectionId > 0) {
            // Add hover effect and right-click menu
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
                
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        showContextMenu(e);
                    }
                }
                
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        showContextMenu(e);
                    }
                }
            });
        }
        // Note: For library view (sectionId=0), no listeners are added here
        // YearSemesterPanel will add click and hover listeners externally
    }
    
    private void showContextMenu(MouseEvent e) {
        JPopupMenu contextMenu = new JPopupMenu();
        
        JMenuItem editItem = new JMenuItem("✏️ Edit Section");
        editItem.addActionListener(evt -> showEditDialog());
        contextMenu.add(editItem);
        
        JMenuItem deleteItem = new JMenuItem("🗑️ Delete Section");
        deleteItem.addActionListener(evt -> confirmAndDeleteSection());
        contextMenu.add(deleteItem);
        
        contextMenu.show(e.getComponent(), e.getX(), e.getY());
    }
    
    private void showEditDialog() {
        if (dashboardScreen != null) {
            dashboardScreen.showEditSectionPanel(sectionId);
        }
        
        // Refresh will happen when user saves or cancels
        if (refreshCallback != null) {
            refreshCallback.run();
        }
    }
    
    private void confirmAndDeleteSection() {
        int result = JOptionPane.showConfirmDialog(
            SwingUtilities.getWindowAncestor(this),
            "⚠️ WARNING: This will permanently delete:\n\n" +
            "• All students in this section\n" +
            "• All marks and grades\n" +
            "• All subject mappings\n" +
            "• All marking schemes\n\n" +
            "This action cannot be undone!\n\n" +
            "Are you sure you want to delete \"" + sectionName + "\"?",
            "Confirm Delete Section",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            SectionEditDAO dao = new SectionEditDAO();
            if (dao.deleteSection(sectionId, userId)) {
                JOptionPane.showMessageDialog(
                    SwingUtilities.getWindowAncestor(this),
                    "Section deleted successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
                );
                if (refreshCallback != null) {
                    refreshCallback.run();
                }
            } else {
                JOptionPane.showMessageDialog(
                    SwingUtilities.getWindowAncestor(this),
                    "Failed to delete section. Please try again.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
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
        
        // Draw card background with white color
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
        this(title, 0, 0, 0, null);
    }
    
    // Simpler constructor without edit/delete functionality
    public SectionCardPanel(String sectionName, int studentCount) {
        this(sectionName, studentCount, 0, 0, null);
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
    
    // Public method to set hover state (for external listeners)
    public void setHovered(boolean hovered) {
        this.isHovered = hovered;
        repaint();
    }
}
