package com.sms.resultlauncher;


import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import com.sms.resultlauncher.ResultLauncher;
import com.sms.resultlauncher.ResultLauncherUtils;
import com.sms.calculation.models.Component;
import com.sms.dao.AnalyzerDAO;

public class ComponentSelectionPanel extends JPanel {
    
    private ResultLauncher parentLauncher;
    private JPanel componentsPanel;
    private JScrollPane scrollPane;
    private JCheckBox selectAllCheckbox;
    private JLabel selectionCountLabel;
    private Map<Component, JCheckBox> componentCheckboxes;
    private List<Component> currentComponents;
    
    public ComponentSelectionPanel(ResultLauncher parent) {
        this.parentLauncher = parent;
        this.componentCheckboxes = new HashMap<>();
        this.currentComponents = new ArrayList<>();
        
        setPreferredSize(new Dimension(380, 220));
        setMaximumSize(new Dimension(380, 220));
        setMinimumSize(new Dimension(380, 220));
        
        initializeUI();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        setOpaque(false);
        
        JPanel cardPanel = ResultLauncherUtils.createModernCard();
        cardPanel.setLayout(new BorderLayout());
        
        JPanel headerPanel = createHeaderPanel();
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        componentsPanel = new JPanel();
        componentsPanel.setLayout(new BoxLayout(componentsPanel, BoxLayout.Y_AXIS));
        componentsPanel.setBackground(ResultLauncherUtils.CARD_COLOR);
        componentsPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        scrollPane = new JScrollPane(componentsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(0, 130)); // Reduced from 250 to 150
        scrollPane.getViewport().setBackground(ResultLauncherUtils.CARD_COLOR);
        
        JPanel footerPanel = createFooterPanel();
        footerPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        
        cardPanel.add(headerPanel, BorderLayout.NORTH);
        cardPanel.add(scrollPane, BorderLayout.CENTER);
        cardPanel.add(footerPanel, BorderLayout.SOUTH);
        
        add(cardPanel, BorderLayout.CENTER);
        
        showInitialMessage();
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("📋 Select Components");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(ResultLauncherUtils.PRIMARY_COLOR);
        
        selectAllCheckbox = new JCheckBox("Select All");
        selectAllCheckbox.setFont(new Font("SansSerif", Font.BOLD, 12));
        selectAllCheckbox.setOpaque(false);
        selectAllCheckbox.setEnabled(false);
        selectAllCheckbox.addActionListener(e -> toggleSelectAll());
        
        panel.add(titleLabel, BorderLayout.WEST);
        panel.add(selectAllCheckbox, BorderLayout.EAST);
        
        return panel;
    }
    
    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        selectionCountLabel = new JLabel("Select a section first");
        selectionCountLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        selectionCountLabel.setForeground(ResultLauncherUtils.TEXT_SECONDARY);
        
        panel.add(selectionCountLabel, BorderLayout.WEST);
        
        return panel;
    }
    
    private void showInitialMessage() {
        componentsPanel.removeAll();
        
        JLabel messageLabel = new JLabel("Select a section to load components");
        messageLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));
        messageLabel.setForeground(ResultLauncherUtils.TEXT_SECONDARY);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        componentsPanel.add(Box.createVerticalGlue());
        componentsPanel.add(messageLabel);
        componentsPanel.add(Box.createVerticalGlue());
        
        revalidate();
        repaint();
    }
    
    public void loadComponentsForSection(int sectionId) {
        componentsPanel.removeAll();
        componentCheckboxes.clear();
        
        JLabel loadingLabel = new JLabel("Loading components...");
        loadingLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));
        loadingLabel.setForeground(ResultLauncherUtils.TEXT_SECONDARY);
        loadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        componentsPanel.add(Box.createVerticalGlue());
        componentsPanel.add(loadingLabel);
        componentsPanel.add(Box.createVerticalGlue());
        
        revalidate();
        repaint();
        
        SwingWorker<List<Component>, Void> worker = new SwingWorker<List<Component>, Void>() {
            @Override
            protected List<Component> doInBackground() throws Exception {
                AnalyzerDAO dao = new AnalyzerDAO();
                List<AnalyzerDAO.SubjectInfo> subjects = dao.getSubjectsForSection(sectionId);
                
                List<Component> allComponents = new ArrayList<>();
                
                for (AnalyzerDAO.SubjectInfo subject : subjects) {
                    List<AnalyzerDAO.ComponentInfo> componentsInfo = dao.getComponentsForSubject(
                        sectionId, subject.id, "all");
                    
                    for (AnalyzerDAO.ComponentInfo compInfo : componentsInfo) {
                        Component component = new Component(
                            compInfo.id,
                            compInfo.name,
                            compInfo.type,
                            0, // obtained marks not relevant for launcher
                            compInfo.maxMarks,
                            compInfo.scaledMarks
                        );
                        component.setGroupName(compInfo.groupName);
                        component.setSequenceOrder(compInfo.sequenceOrder);
                        allComponents.add(component);
                    }
                }
                
                return allComponents;
            }

            @Override
            protected void done() {
                try {
                    currentComponents = get();
                    updateComponentsList();
                    
                } catch (Exception e) {
                    System.err.println("Error loading components: " + e.getMessage());
                    e.printStackTrace();
                    showErrorMessage("Error loading components: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
    
    private void updateComponentsList() {
        componentsPanel.removeAll();
        componentCheckboxes.clear();
        
        if (currentComponents.isEmpty()) {
            JLabel noComponentsLabel = new JLabel("No components found for this section");
            noComponentsLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));
            noComponentsLabel.setForeground(ResultLauncherUtils.TEXT_SECONDARY);
            noComponentsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            componentsPanel.add(Box.createVerticalGlue());
            componentsPanel.add(noComponentsLabel);
            componentsPanel.add(Box.createVerticalGlue());
        } else {
            // Group components by type
            Map<String, List<Component>> groupedComponents = groupComponentsByType(currentComponents);
            
            for (Map.Entry<String, List<Component>> entry : groupedComponents.entrySet()) {
                String type = entry.getKey();
                List<Component> typeComponents = entry.getValue();
                
                JPanel typeHeaderPanel = createTypeHeader(type, typeComponents.size());
                componentsPanel.add(typeHeaderPanel);
                componentsPanel.add(Box.createVerticalStrut(8));
                
                for (Component component : typeComponents) {
                    JPanel componentPanel = createComponentPanel(component);
                    componentsPanel.add(componentPanel);
                    componentsPanel.add(Box.createVerticalStrut(3));
                }
                
                componentsPanel.add(Box.createVerticalStrut(10));
            }
        }
        
        selectAllCheckbox.setEnabled(!currentComponents.isEmpty());
        updateSelectionCount();
        
        revalidate();
        repaint();
    }
    
    private Map<String, List<Component>> groupComponentsByType(List<Component> components) {
        Map<String, List<Component>> grouped = new LinkedHashMap<>();
        
        for (Component component : components) {
            String type = component.getType();
            if (type == null || type.isEmpty()) {
                type = "Other";
            }
            grouped.computeIfAbsent(type, k -> new ArrayList<>()).add(component);
        }
        
        return grouped;
    }
    
    private JPanel createTypeHeader(String type, int count) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        
        JLabel typeLabel = new JLabel(type.toUpperCase() + " (" + count + ")");
        typeLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        typeLabel.setForeground(ResultLauncherUtils.PRIMARY_COLOR);
        
        JCheckBox typeSelectAll = new JCheckBox("Select All");
        typeSelectAll.setFont(new Font("SansSerif", Font.PLAIN, 10));
        typeSelectAll.setOpaque(false);
        typeSelectAll.addActionListener(e -> toggleTypeSelection(type, typeSelectAll.isSelected()));
        
        panel.add(typeLabel, BorderLayout.WEST);
        panel.add(typeSelectAll, BorderLayout.EAST);
        
        return panel;
    }
    
    private JPanel createComponentPanel(Component component) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 5));
        
        JCheckBox checkbox = new JCheckBox();
        checkbox.setOpaque(false);
        checkbox.setSelected(true); // Default to selected
        checkbox.addActionListener(e -> {
            updateSelectionCount();
            updateSelectAllState();
            notifySelectionChanged();
        });
        
        componentCheckboxes.put(component, checkbox);
        
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        
        JLabel nameLabel = new JLabel(component.getName());
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        nameLabel.setForeground(ResultLauncherUtils.TEXT_PRIMARY);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        String details = String.format("Max Marks: %.0f | Weight: %.0f", 
            component.getMaxMarks(), component.getWeight());
        
        JLabel detailsLabel = new JLabel(details);
        detailsLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        detailsLabel.setForeground(ResultLauncherUtils.TEXT_SECONDARY);
        detailsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        infoPanel.add(nameLabel);
        infoPanel.add(detailsLabel);
        
        JLabel typeLabel = new JLabel(component.getType());
        typeLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        typeLabel.setForeground(ResultLauncherUtils.INFO_COLOR);
        
        panel.add(checkbox, BorderLayout.WEST);
        panel.add(infoPanel, BorderLayout.CENTER);
        panel.add(typeLabel, BorderLayout.EAST);
        
        return panel;
    }
    
    private void showErrorMessage(String message) {
        componentsPanel.removeAll();
        
        JLabel errorLabel = new JLabel("Error: " + message);
        errorLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));
        errorLabel.setForeground(ResultLauncherUtils.DANGER_COLOR);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        componentsPanel.add(Box.createVerticalGlue());
        componentsPanel.add(errorLabel);
        componentsPanel.add(Box.createVerticalGlue());
        
        revalidate();
        repaint();
    }
    
    private void toggleSelectAll() {
        boolean selectAll = selectAllCheckbox.isSelected();
        for (JCheckBox checkbox : componentCheckboxes.values()) {
            checkbox.setSelected(selectAll);
        }
        updateSelectionCount();
        notifySelectionChanged();
    }
    
    private void toggleTypeSelection(String type, boolean selected) {
        for (Map.Entry<Component, JCheckBox> entry : componentCheckboxes.entrySet()) {
            Component component = entry.getKey();
            JCheckBox checkbox = entry.getValue();
            
            if (type.equals(component.getType())) {
                checkbox.setSelected(selected);
            }
        }
        updateSelectionCount();
        updateSelectAllState();
        notifySelectionChanged();
    }
    
    private void updateSelectionCount() {
        int selectedCount = 0;
        for (JCheckBox checkbox : componentCheckboxes.values()) {
            if (checkbox.isSelected()) {
                selectedCount++;
            }
        }
        
        if (currentComponents.isEmpty()) {
            selectionCountLabel.setText("No components available");
        } else {
            selectionCountLabel.setText(selectedCount + " of " + currentComponents.size() + " components selected");
        }
    }
    
    private void updateSelectAllState() {
        int totalComponents = componentCheckboxes.size();
        int selectedComponents = 0;
        
        for (JCheckBox checkbox : componentCheckboxes.values()) {
            if (checkbox.isSelected()) {
                selectedComponents++;
            }
        }
        
        if (selectedComponents == 0) {
            selectAllCheckbox.setSelected(false);
        } else if (selectedComponents == totalComponents) {
            selectAllCheckbox.setSelected(true);
        } else {
            selectAllCheckbox.setSelected(false);
        }
    }
    
    private void notifySelectionChanged() {
        List<Component> selectedComponents = new ArrayList<>();
        
        for (Map.Entry<Component, JCheckBox> entry : componentCheckboxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selectedComponents.add(entry.getKey());
            }
        }
        
        parentLauncher.onComponentsSelected(selectedComponents);
    }
    
    public List<Component> getSelectedComponents() {
        List<Component> selectedComponents = new ArrayList<>();
        for (Map.Entry<Component, JCheckBox> entry : componentCheckboxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selectedComponents.add(entry.getKey());
            }
        }
        return selectedComponents;
    }
}