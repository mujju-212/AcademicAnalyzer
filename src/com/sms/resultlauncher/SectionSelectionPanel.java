package com.sms.resultlauncher;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import com.sms.resultlauncher.ResultLauncher;
import com.sms.resultlauncher.ResultLauncherUtils;
import com.sms.dao.SectionDAO;
import com.sms.login.LoginScreen;

public class SectionSelectionPanel extends JPanel {
    
    private ResultLauncher parentLauncher;
    private JComboBox<SectionItem> sectionCombo;
    private List<SectionDAO.SectionInfo> availableSections;
    
    public SectionSelectionPanel(ResultLauncher parent) {
        this.parentLauncher = parent;
        
        setPreferredSize(new Dimension(380, 160));
        setMaximumSize(new Dimension(380, 160));
        setMinimumSize(new Dimension(380, 160));
        
        initializeUI();
        loadSections();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        setOpaque(false);
        
        JPanel cardPanel = ResultLauncherUtils.createModernCard();
        cardPanel.setLayout(new BorderLayout());
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        contentPanel.setOpaque(false);
        
        JLabel headerLabel = new JLabel("📚 Select Section");
        headerLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        headerLabel.setForeground(ResultLauncherUtils.PRIMARY_COLOR);
        headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel descLabel = new JLabel("Choose the section for result launch");
        descLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        descLabel.setForeground(ResultLauncherUtils.TEXT_SECONDARY);
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
     // Replace this line:
     // sectionCombo = ResultLauncherUtils.createModernComboBox();

     // With this:
     sectionCombo = new JComboBox<>();
     sectionCombo.setFont(new Font("SansSerif", Font.PLAIN, 14));
     sectionCombo.setPreferredSize(new Dimension(350, 35));        sectionCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        sectionCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        sectionCombo.setPreferredSize(new Dimension(350, 35));
        
        sectionCombo.addActionListener(e -> {
            SectionItem selected = (SectionItem) sectionCombo.getSelectedItem();
            if (selected != null && selected.getId() != -1) {
                parentLauncher.onSectionSelected(selected.getId());
            }
        });
        
        contentPanel.add(headerLabel);
        contentPanel.add(Box.createVerticalStrut(5));
        contentPanel.add(descLabel);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(sectionCombo);
        
        cardPanel.add(contentPanel, BorderLayout.CENTER);
        add(cardPanel, BorderLayout.CENTER);
    }
    
    public void loadSections() {
        SwingWorker<List<SectionDAO.SectionInfo>, Void> worker = new SwingWorker<List<SectionDAO.SectionInfo>, Void>() {
            @Override
            protected List<SectionDAO.SectionInfo> doInBackground() throws Exception {
                SectionDAO sectionDAO = new SectionDAO();
                return sectionDAO.getSectionsByUser(LoginScreen.currentUserId);
            }

            @Override
            protected void done() {
                try {
                    availableSections = get();
                    
                    sectionCombo.removeAllItems();
                    sectionCombo.addItem(new SectionItem(-1, "Select a section..."));
                    
                    for (SectionDAO.SectionInfo section : availableSections) {
                        sectionCombo.addItem(new SectionItem(section.id, section.sectionName));
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error loading sections: " + e.getMessage());
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(SectionSelectionPanel.this,
                        "Error loading sections: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    public void refreshSections() {
        loadSections();
    }
    
    // Method to programmatically select a section by ID
    public void selectSection(int sectionId) {
        if (sectionCombo != null) {
            for (int i = 0; i < sectionCombo.getItemCount(); i++) {
                Object item = sectionCombo.getItemAt(i);
                if (item instanceof SectionItem) {
                    SectionItem sectionItem = (SectionItem) item;
                    if (sectionItem.getId() == sectionId) {
                        sectionCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }
    }
    
    // Helper class for combo box items
    private static class SectionItem {
        private int id;
        private String name;
        
        public SectionItem(int id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public int getId() { return id; }
        public String getName() { return name; }
        
        @Override
        public String toString() { return name; }
    }
}