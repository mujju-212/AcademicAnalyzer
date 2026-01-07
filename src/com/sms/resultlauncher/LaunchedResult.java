package com.sms.resultlauncher;


import java.time.LocalDateTime;
import java.util.List;

public class LaunchedResult {
    private int id;
    private String launchName;
    private int sectionId;
    private String sectionName;
    private List<Integer> componentIds;
    private List<Integer> studentIds;
    private int launchedBy;
    private String launchedByName;
    private LocalDateTime launchDate;
    private String status; // "active" or "inactive"
    private boolean emailSent;
    private int studentCount;
    private int componentCount;
    
    // Constructors
    public LaunchedResult() {}
    
    public LaunchedResult(String launchName, int sectionId, List<Integer> componentIds, 
                         List<Integer> studentIds, int launchedBy) {
        this.launchName = launchName;
        this.sectionId = sectionId;
        this.componentIds = componentIds;
        this.studentIds = studentIds;
        this.launchedBy = launchedBy;
        this.status = "active";
        this.emailSent = false;
        this.launchDate = LocalDateTime.now();
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getLaunchName() { return launchName; }
    public void setLaunchName(String launchName) { this.launchName = launchName; }
    
    public int getSectionId() { return sectionId; }
    public void setSectionId(int sectionId) { this.sectionId = sectionId; }
    
    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }
    
    public List<Integer> getComponentIds() { return componentIds; }
    public void setComponentIds(List<Integer> componentIds) { this.componentIds = componentIds; }
    
    public List<Integer> getStudentIds() { return studentIds; }
    public void setStudentIds(List<Integer> studentIds) { this.studentIds = studentIds; }
    
    public int getLaunchedBy() { return launchedBy; }
    public void setLaunchedBy(int launchedBy) { this.launchedBy = launchedBy; }
    
    public String getLaunchedByName() { return launchedByName; }
    public void setLaunchedByName(String launchedByName) { this.launchedByName = launchedByName; }
    
    public LocalDateTime getLaunchDate() { return launchDate; }
    public void setLaunchDate(LocalDateTime launchDate) { this.launchDate = launchDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public boolean isEmailSent() { return emailSent; }
    public void setEmailSent(boolean emailSent) { this.emailSent = emailSent; }
    
    public int getStudentCount() { return studentCount; }
    public void setStudentCount(int studentCount) { this.studentCount = studentCount; }
    
    public int getComponentCount() { return componentCount; }
    public void setComponentCount(int componentCount) { this.componentCount = componentCount; }
    
    public boolean isActive() { return "active".equals(status); }
    
    @Override
    public String toString() {
        return launchName + " (" + sectionName + ")";
    }
}