package com.sms.marking.models;

import java.time.LocalDateTime;
import java.util.*;

public class MarkingScheme {
    private int id;
    private int sectionId;
    private int subjectId;
    private String schemeName;
    private int totalInternalMarks;
    private int totalExternalMarks;
    private boolean isActive;
    private LocalDateTime createdAt;
    private int createdBy;
    
    // Relationships
    private List<ComponentGroup> componentGroups;
    
    // Constructors
    public MarkingScheme() {
        this.componentGroups = new ArrayList<>();
        this.totalInternalMarks = 50;
        this.totalExternalMarks = 50;
        this.isActive = true;
    }
    
    public MarkingScheme(int sectionId, int subjectId, String schemeName) {
        this();
        this.sectionId = sectionId;
        this.subjectId = subjectId;
        this.schemeName = schemeName;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getSectionId() { return sectionId; }
    public void setSectionId(int sectionId) { this.sectionId = sectionId; }
    
    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }
    
    public String getSchemeName() { return schemeName; }
    public void setSchemeName(String schemeName) { this.schemeName = schemeName; }
    
    public int getTotalInternalMarks() { return totalInternalMarks; }
    public void setTotalInternalMarks(int totalInternalMarks) { 
        this.totalInternalMarks = totalInternalMarks; 
    }
    
    public int getTotalExternalMarks() { return totalExternalMarks; }
    public void setTotalExternalMarks(int totalExternalMarks) { 
        this.totalExternalMarks = totalExternalMarks; 
    }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }
    
    public List<ComponentGroup> getComponentGroups() { return componentGroups; }
    public void setComponentGroups(List<ComponentGroup> componentGroups) { 
        this.componentGroups = componentGroups; 
    }
    
    // Business Methods
    public int getTotalMarks() {
        return totalInternalMarks + totalExternalMarks;
    }
    
    public void addComponentGroup(ComponentGroup group) {
        componentGroups.add(group);
        group.setSchemeId(this.id);
    }
    
    public void removeComponentGroup(ComponentGroup group) {
        componentGroups.remove(group);
    }
    
    public List<ComponentGroup> getInternalGroups() {
        List<ComponentGroup> internal = new ArrayList<>();
        for (ComponentGroup group : componentGroups) {
            if ("internal".equals(group.getGroupType())) {
                internal.add(group);
            }
        }
        return internal;
    }
    
    public List<ComponentGroup> getExternalGroups() {
        List<ComponentGroup> external = new ArrayList<>();
        for (ComponentGroup group : componentGroups) {
            if ("external".equals(group.getGroupType())) {
                external.add(group);
            }
        }
        return external;
    }
    
    // Validation
    public ValidationResult validate() {
        ValidationResult result = new ValidationResult();
        
        // Calculate totals
        int internalTotal = 0;
        int externalTotal = 0;
        
        for (ComponentGroup group : componentGroups) {
            if ("internal".equals(group.getGroupType())) {
                internalTotal += group.getTotalGroupMarks();
            } else {
                externalTotal += group.getTotalGroupMarks();
            }
        }
        
        // Validate totals
        if (internalTotal != totalInternalMarks) {
            result.addError(String.format(
                "Internal marks mismatch: Expected %d, got %d", 
                totalInternalMarks, internalTotal
            ));
        }
        
        if (externalTotal != totalExternalMarks) {
            result.addError(String.format(
                "External marks mismatch: Expected %d, got %d", 
                totalExternalMarks, externalTotal
            ));
        }
        
        // Validate groups
        if (componentGroups.isEmpty()) {
            result.addError("No component groups defined");
        }
        
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("%s (Internal: %d, External: %d)", 
            schemeName, totalInternalMarks, totalExternalMarks);
    }
}