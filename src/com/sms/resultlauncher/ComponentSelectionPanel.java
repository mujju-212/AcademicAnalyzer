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
    private JComboBox<String> subjectFilterCombo;
    private Map<Component, JCheckBox> componentCheckboxes;
    private List<Component> currentComponents;
    private List<Component> filteredComponents;
    private Map<String, List<Component>> componentsBySubject;
    
    public ComponentSelectionPanel(ResultLauncher parent) {
        this.parentLauncher = parent;
        this.componentCheckboxes = new HashMap<>();
        this.currentComponents = new ArrayList<>();
        this.filteredComponents = new ArrayList<>();
        this.componentsBySubject = new HashMap<>();
        
        setPreferredSize(new Dimension(380, 280)); // Increased height
        setMaximumSize(new Dimension(380, 280));
        setMinimumSize(new Dimension(380, 280));
        
        initializeUI();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        setOpaque(false);
        
        JPanel cardPanel = ResultLauncherUtils.createModernCard();
        cardPanel.setLayout(new BorderLayout());
        
        JPanel headerPanel = createHeaderPanel();
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        JPanel filterPanel = createFilterPanel();
        filterPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);
        topPanel.add(headerPanel);
        topPanel.add(filterPanel);
        
        componentsPanel = new JPanel();
        componentsPanel.setLayout(new BoxLayout(componentsPanel, BoxLayout.Y_AXIS));
        componentsPanel.setBackground(ResultLauncherUtils.CARD_COLOR);
        componentsPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        scrollPane = new JScrollPane(componentsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(0, 160)); // Increased from 130
        scrollPane.getViewport().setBackground(ResultLauncherUtils.CARD_COLOR);
        
        JPanel footerPanel = createFooterPanel();
        footerPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        cardPanel.add(topPanel, BorderLayout.NORTH);
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
    
    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        
        JLabel filterLabel = new JLabel("📚 Subject:");
        filterLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        
        subjectFilterCombo = new JComboBox<>();
        subjectFilterCombo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subjectFilterCombo.setEnabled(false);
        subjectFilterCombo.addActionListener(e -> filterComponentsBySubject());
        
        panel.add(filterLabel, BorderLayout.WEST);
        panel.add(subjectFilterCombo, BorderLayout.CENTER);
        
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
                System.out.println("=== LOADING COMPONENTS FOR SECTION: " + sectionId + " ===");
                AnalyzerDAO dao = new AnalyzerDAO();
                List<AnalyzerDAO.SubjectInfo> subjects = dao.getSubjectsForSection(sectionId);
                
                System.out.println("Found " + subjects.size() + " subjects");
                
                List<Component> allComponents = new ArrayList<>();
                Map<String, List<Component>> bySubject = new HashMap<>();
                
                for (AnalyzerDAO.SubjectInfo subject : subjects) {
                    System.out.println("Loading components for subject: " + subject.name + " (ID: " + subject.id + ")");
                    List<AnalyzerDAO.ComponentInfo> componentsInfo = dao.getComponentsForSubject(
                        sectionId, subject.id, "all");
                    
                    System.out.println("  Found " + componentsInfo.size() + " components");
                    
                    List<Component> subjectComponents = new ArrayList<>();
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
                        subjectComponents.add(component);
                        System.out.println("    - " + compInfo.name + " (" + compInfo.type + ")");
                    }
                    
                    if (!subjectComponents.isEmpty()) {
                        bySubject.put(subject.name, subjectComponents);
                    }
                }
                
                componentsBySubject = bySubject;
                
                System.out.println("Total components loaded: " + allComponents.size());
                return allComponents;
            }

            @Override
            protected void done() {
                try {
                    currentComponents = get();
                    filteredComponents = new ArrayList<>(currentComponents);
                    populateSubjectFilter();
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
    
    private void populateSubjectFilter() {
        subjectFilterCombo.removeAllItems();
        subjectFilterCombo.addItem("All Subjects");
        
        List<String> subjects = new ArrayList<>(componentsBySubject.keySet());
        Collections.sort(subjects);
        
        for (String subject : subjects) {
            subjectFilterCombo.addItem(subject);
        }
        
        subjectFilterCombo.setEnabled(true);
    }
    
    private void filterComponentsBySubject() {
        String selectedSubject = (String) subjectFilterCombo.getSelectedItem();
        
        if (selectedSubject == null || "All Subjects".equals(selectedSubject)) {
            filteredComponents = new ArrayList<>(currentComponents);
        } else {
            filteredComponents = componentsBySubject.getOrDefault(selectedSubject, new ArrayList<>());
        }
        
        updateComponentsList();
    }
    
    private void updateComponentsList() {
        componentsPanel.removeAll();
        componentCheckboxes.clear();
        
        List<Component> componentsToDisplay = filteredComponents.isEmpty() ? currentComponents : filteredComponents;
        
        if (componentsToDisplay.isEmpty()) {
            String message = subjectFilterCombo != null && subjectFilterCombo.getSelectedIndex() > 0
                ? "No components found for selected subject"
                : "No components found for this section";
            
            JLabel noComponentsLabel = new JLabel(message);
            noComponentsLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));
            noComponentsLabel.setForeground(ResultLauncherUtils.TEXT_SECONDARY);
            noComponentsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            componentsPanel.add(Box.createVerticalGlue());
            componentsPanel.add(noComponentsLabel);
            componentsPanel.add(Box.createVerticalGlue());
        } else {
            // Group components by subject first if showing all
            String selectedSubject = (String) subjectFilterCombo.getSelectedItem();
            boolean showingAll = selectedSubject == null || "All Subjects".equals(selectedSubject);
            
            if (showingAll) {
                // Group by subject
                for (Map.Entry<String, List<Component>> entry : componentsBySubject.entrySet()) {
                    String subject = entry.getKey();
                    List<Component> subjectComps = entry.getValue();
                    
                    JPanel subjectHeaderPanel = createSubjectHeader(subject, subjectComps.size());
                    componentsPanel.add(subjectHeaderPanel);
                    componentsPanel.add(Box.createVerticalStrut(5));
                    
                    for (Component component : subjectComps) {
                        JPanel componentPanel = createComponentPanel(component);
                        componentsPanel.add(componentPanel);
                        componentsPanel.add(Box.createVerticalStrut(3));
                    }
                    
                    componentsPanel.add(Box.createVerticalStrut(10));
                }
            } else {
                // Show single subject components
                for (Component component : componentsToDisplay) {
                    JPanel componentPanel = createComponentPanel(component);
                    componentsPanel.add(componentPanel);
                    componentsPanel.add(Box.createVerticalStrut(5));
                }
            }
        }
        
        selectAllCheckbox.setEnabled(!componentsToDisplay.isEmpty());
        updateSelectionCount();
        
        revalidate();
        repaint();
    }
    
    private JPanel createSubjectHeader(String subject, int count) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        
        JLabel subjectLabel = new JLabel("📚 " + subject);
        subjectLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        subjectLabel.setForeground(ResultLauncherUtils.PRIMARY_COLOR);
        
        JLabel countLabel = new JLabel(count + " components");
        countLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        countLabel.setForeground(ResultLauncherUtils.TEXT_SECONDARY);
        
        panel.add(subjectLabel, BorderLayout.WEST);
        panel.add(countLabel, BorderLayout.EAST);
        
        return panel;
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
    
    /**
     * Preselect components by their IDs (for edit mode).
     */
    public void preselectComponents(List<Integer> componentIds) {
        if (componentIds == null || componentIds.isEmpty()) {
            return;
        }
        
        Set<Integer> idsToSelect = new HashSet<>(componentIds);
        int selectedCount = 0;
        
        for (Map.Entry<Component, JCheckBox> entry : componentCheckboxes.entrySet()) {
            if (idsToSelect.contains(entry.getKey().getId())) {
                entry.getValue().setSelected(true);
                selectedCount++;
            } else {
                entry.getValue().setSelected(false);
            }
        }
        
        // Update selection count label
        updateSelectionCount();
        
        System.out.println("Preselected " + selectedCount + " components");
    }
}
