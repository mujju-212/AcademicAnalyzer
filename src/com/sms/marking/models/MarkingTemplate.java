package com.sms.marking.models;

import java.time.LocalDateTime;
import java.util.*;

public class MarkingTemplate {
    private int id;
    private String templateName;
    private String templateCode;
    private String description;
    private String templateData; // JSON stored as string
    private String category;
    private boolean isActive;
    private LocalDateTime createdAt;
    
    // For runtime use
    private transient MarkingScheme templateScheme;
    
    // Constructors
    public MarkingTemplate() {
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
    }
    
    public MarkingTemplate(String templateName, String templateCode, String category) {
        this();
        this.templateName = templateName;
        this.templateCode = templateCode;
        this.category = category;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getTemplateData() { return templateData; }
    public void setTemplateData(String templateData) { this.templateData = templateData; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public MarkingScheme getTemplateScheme() { return templateScheme; }
    public void setTemplateScheme(MarkingScheme templateScheme) { 
        this.templateScheme = templateScheme; 
    }
    
    // Business Methods
    public MarkingScheme createSchemeInstance(int sectionId, int subjectId) {
        MarkingScheme scheme = new MarkingScheme(sectionId, subjectId, templateName);
        
        // Copy template structure
        if (templateScheme != null) {
            scheme.setTotalInternalMarks(templateScheme.getTotalInternalMarks());
            scheme.setTotalExternalMarks(templateScheme.getTotalExternalMarks());
            
            // Deep copy component groups
            for (ComponentGroup templateGroup : templateScheme.getComponentGroups()) {
                ComponentGroup newGroup = new ComponentGroup(
                    templateGroup.getGroupName(),
                    templateGroup.getGroupType(),
                    templateGroup.getTotalGroupMarks()
                );
                newGroup.setSelectionType(templateGroup.getSelectionType());
                newGroup.setSelectionCount(templateGroup.getSelectionCount());
                newGroup.setSequenceOrder(templateGroup.getSequenceOrder());
                
                // Copy components
                for (MarkingComponent templateComp : templateGroup.getComponents()) {
                    MarkingComponent newComp = new MarkingComponent(
                        templateComp.getComponentName(),
                        templateComp.getActualMaxMarks()
                    );
                    newComp.setSequenceOrder(templateComp.getSequenceOrder());
                    newComp.setOptional(templateComp.isOptional());
                    newGroup.addComponent(newComp);
                }
                
                scheme.addComponentGroup(newGroup);
            }
        }
        
        return scheme;
    }
    
    @Override
    public String toString() {
        return String.format("%s (%s)", templateName, category);
    }
}