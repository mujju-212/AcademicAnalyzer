package com.sms.marking.models;

import java.time.LocalDateTime;

public class StudentComponentMark {
    private int id;
    private int studentId;
    private int componentId;
    private Double marksObtained;
    private String status; // "present", "absent", "medical"
    private LocalDateTime enteredAt;
    private int enteredBy;
    
    // For calculations
    private transient MarkingComponent component;
    private transient double scaledMarks;
    
    // Constructors
    public StudentComponentMark() {
        this.status = "present";
        this.enteredAt = LocalDateTime.now();
    }
    
    public StudentComponentMark(int studentId, int componentId) {
        this();
        this.studentId = studentId;
        this.componentId = componentId;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    
    public int getComponentId() { return componentId; }
    public void setComponentId(int componentId) { this.componentId = componentId; }
    
    public Double getMarksObtained() { return marksObtained; }
    public void setMarksObtained(Double marksObtained) { 
        this.marksObtained = marksObtained;
        calculateScaledMarks();
    }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getEnteredAt() { return enteredAt; }
    public void setEnteredAt(LocalDateTime enteredAt) { this.enteredAt = enteredAt; }
    
    public int getEnteredBy() { return enteredBy; }
    public void setEnteredBy(int enteredBy) { this.enteredBy = enteredBy; }
    
    public MarkingComponent getComponent() { return component; }
    public void setComponent(MarkingComponent component) { 
        this.component = component;
        calculateScaledMarks();
    }
    
    public double getScaledMarks() { return scaledMarks; }
    
    // Business Methods
    private void calculateScaledMarks() {
        if (component != null && marksObtained != null && "present".equals(status)) {
            this.scaledMarks = component.calculateScaledMarks(marksObtained);
        } else {
            this.scaledMarks = 0;
        }
    }
    
    public boolean isPresent() {
        return "present".equals(status);
    }
    
    public boolean isAbsent() {
        return "absent".equals(status);
    }
    
    public boolean isMedical() {
        return "medical".equals(status);
    }
    
    public boolean hasMarks() {
        return marksObtained != null && marksObtained >= 0 && isPresent();
    }
    
    @Override
    public String toString() {
        return String.format("Student %d - Component %d: %s marks (%s)", 
            studentId, componentId, 
            marksObtained != null ? marksObtained : "N/A", 
            status);
    }
}