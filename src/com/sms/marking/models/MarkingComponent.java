package com.sms.marking.models;

import java.time.LocalDateTime;

public class MarkingComponent {
    private int id;
    private int schemeId;
    private int groupId;
    private String componentName;
    private String componentType; // "internal" or "external"
    private int actualMaxMarks;
    private int scaledToMarks;
    private String componentGroup; // Legacy field
    private boolean isBestOfGroup;
    private Integer bestOfCount;
    private int sequenceOrder;
    private boolean isOptional;
    private LocalDateTime createdAt;
    
    // Constructors
    public MarkingComponent() {
        this.sequenceOrder = 0;
        this.isOptional = false;
        this.isBestOfGroup = false;
    }
    
    public MarkingComponent(String componentName, int actualMaxMarks) {
        this();
        this.componentName = componentName;
        this.actualMaxMarks = actualMaxMarks;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getSchemeId() { return schemeId; }
    public void setSchemeId(int schemeId) { this.schemeId = schemeId; }
    
    public int getGroupId() { return groupId; }
    public void setGroupId(int groupId) { this.groupId = groupId; }
    
    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }
    
    public String getComponentType() { return componentType; }
    public void setComponentType(String componentType) { this.componentType = componentType; }
    
    public int getActualMaxMarks() { return actualMaxMarks; }
    public void setActualMaxMarks(int actualMaxMarks) { this.actualMaxMarks = actualMaxMarks; }
    
    public int getScaledToMarks() { return scaledToMarks; }
    public void setScaledToMarks(int scaledToMarks) { this.scaledToMarks = scaledToMarks; }
    
    public String getComponentGroup() { return componentGroup; }
    public void setComponentGroup(String componentGroup) { this.componentGroup = componentGroup; }
    
    public boolean isBestOfGroup() { return isBestOfGroup; }
    public void setBestOfGroup(boolean bestOfGroup) { isBestOfGroup = bestOfGroup; }
    
    public Integer getBestOfCount() { return bestOfCount; }
    public void setBestOfCount(Integer bestOfCount) { this.bestOfCount = bestOfCount; }
    
    public int getSequenceOrder() { return sequenceOrder; }
    public void setSequenceOrder(int sequenceOrder) { this.sequenceOrder = sequenceOrder; }
    
    public boolean isOptional() { return isOptional; }
    public void setOptional(boolean optional) { isOptional = optional; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    // Business Methods
    public double calculateScaledMarks(double actualMarks) {
        if (actualMaxMarks == 0) return 0;
        return (actualMarks / actualMaxMarks) * scaledToMarks;
    }
    
    public double getScalingFactor() {
        if (actualMaxMarks == 0) return 0;
        return (double) scaledToMarks / actualMaxMarks;
    }
    
    public boolean isInternal() {
        return "internal".equals(componentType);
    }
    
    public boolean isExternal() {
        return "external".equals(componentType);
    }
    
    @Override
    public String toString() {
        return String.format("%s (%d marks)", componentName, actualMaxMarks);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        MarkingComponent that = (MarkingComponent) obj;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}