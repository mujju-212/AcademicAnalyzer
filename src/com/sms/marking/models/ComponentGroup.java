package com.sms.marking.models;

import java.util.*;

public class ComponentGroup {
    private int id;
    private int schemeId;
    private String groupName;
    private String groupType; // "internal" or "external"
    private String selectionType; // "all", "best_of", "average_of"
    private Integer selectionCount; // For best_of
    private int totalGroupMarks;
    private int sequenceOrder;
    
    // Relationships
    private List<MarkingComponent> components;
    
    // Constructors
    public ComponentGroup() {
        this.components = new ArrayList<>();
        this.selectionType = "all";
        this.sequenceOrder = 0;
    }
    
    public ComponentGroup(String groupName, String groupType, int totalGroupMarks) {
        this();
        this.groupName = groupName;
        this.groupType = groupType;
        this.totalGroupMarks = totalGroupMarks;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getSchemeId() { return schemeId; }
    public void setSchemeId(int schemeId) { this.schemeId = schemeId; }
    
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    
    public String getGroupType() { return groupType; }
    public void setGroupType(String groupType) { this.groupType = groupType; }
    
    public String getSelectionType() { return selectionType; }
    public void setSelectionType(String selectionType) { this.selectionType = selectionType; }
    
    public Integer getSelectionCount() { return selectionCount; }
    public void setSelectionCount(Integer selectionCount) { this.selectionCount = selectionCount; }
    
    public int getTotalGroupMarks() { return totalGroupMarks; }
    public void setTotalGroupMarks(int totalGroupMarks) { this.totalGroupMarks = totalGroupMarks; }
    
    public int getSequenceOrder() { return sequenceOrder; }
    public void setSequenceOrder(int sequenceOrder) { this.sequenceOrder = sequenceOrder; }
    
    public List<MarkingComponent> getComponents() { return components; }
    public void setComponents(List<MarkingComponent> components) { this.components = components; }
    
    // Business Methods
    public void addComponent(MarkingComponent component) {
        components.add(component);
        component.setGroupId(this.id);
    }
    
    public void removeComponent(MarkingComponent component) {
        components.remove(component);
    }
    
    public boolean isBestOfGroup() {
        return "best_of".equals(selectionType);
    }
    
    public boolean isAverageGroup() {
        return "average_of".equals(selectionType);
    }
    
    public String getDisplayName() {
        StringBuilder sb = new StringBuilder(groupName);
        
        if ("best_of".equals(selectionType) && selectionCount != null) {
            sb.append(String.format(" (Best %d of %d)", selectionCount, components.size()));
        } else if ("average_of".equals(selectionType)) {
            sb.append(" (Average)");
        }
        
        sb.append(String.format(" → %d marks", totalGroupMarks));
        
        return sb.toString();
    }
    
    // Calculate scaled marks for a student based on their component marks
    public double calculateGroupMarks(Map<Integer, Double> studentComponentMarks) {
        if (components.isEmpty()) return 0;
        
        List<ComponentScore> scores = new ArrayList<>();
        double totalActualMarks = 0;
        double totalMaxMarks = 0;
        
        // Collect valid scores
        for (MarkingComponent comp : components) {
            Double marks = studentComponentMarks.get(comp.getId());
            if (marks != null && marks >= 0) {
                scores.add(new ComponentScore(comp, marks));
            }
        }
        
        if (scores.isEmpty()) return 0;
        
        switch (selectionType) {
            case "best_of":
                // Sort by percentage score (descending)
                scores.sort((a, b) -> Double.compare(
                    b.getPercentage(), a.getPercentage()
                ));
                
                // Take best N
                int count = Math.min(
                    selectionCount != null ? selectionCount : scores.size(), 
                    scores.size()
                );
                
                for (int i = 0; i < count; i++) {
                    totalActualMarks += scores.get(i).marks;
                    totalMaxMarks += scores.get(i).component.getActualMaxMarks();
                }
                break;
                
            case "average_of":
                // Calculate average
                for (ComponentScore score : scores) {
                    totalActualMarks += score.marks;
                    totalMaxMarks += score.component.getActualMaxMarks();
                }
                if (!scores.isEmpty()) {
                    totalActualMarks = totalActualMarks / scores.size();
                    totalMaxMarks = totalMaxMarks / scores.size();
                }
                break;
                
            case "all":
            default:
                // Sum all
                for (ComponentScore score : scores) {
                    totalActualMarks += score.marks;
                    totalMaxMarks += score.component.getActualMaxMarks();
                }
                break;
        }
        
        // Scale to group marks
        if (totalMaxMarks > 0) {
            return (totalActualMarks / totalMaxMarks) * totalGroupMarks;
        }
        
        return 0;
    }
    
    // Helper class for sorting
    private static class ComponentScore {
        MarkingComponent component;
        double marks;
        
        ComponentScore(MarkingComponent component, double marks) {
            this.component = component;
            this.marks = marks;
        }
        
        double getPercentage() {
            return component.getActualMaxMarks() > 0 ? 
                (marks / component.getActualMaxMarks()) * 100 : 0;
        }
    }
    
    @Override
    public String toString() {
        return getDisplayName();
    }
}