package com.sms.calculation.models;

public class ComponentAnalysis {
    private String componentName;
    private int totalStudents;
    private double averagePercentage;
    private double highestPercentage;
    private double lowestPercentage;
    private int passCount;
    private int failCount;
    private double passPercentage;

    // Constructors
    public ComponentAnalysis() {}

    // Getters and Setters
    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }

    public int getTotalStudents() { return totalStudents; }
    public void setTotalStudents(int totalStudents) { this.totalStudents = totalStudents; }

    public double getAveragePercentage() { return averagePercentage; }
    public void setAveragePercentage(double averagePercentage) { this.averagePercentage = averagePercentage; }

    public double getHighestPercentage() { return highestPercentage; }
    public void setHighestPercentage(double highestPercentage) { this.highestPercentage = highestPercentage; }

    public double getLowestPercentage() { return lowestPercentage; }
    public void setLowestPercentage(double lowestPercentage) { this.lowestPercentage = lowestPercentage; }

    public int getPassCount() { return passCount; }
    public void setPassCount(int passCount) { this.passCount = passCount; }

    public int getFailCount() { return failCount; }
    public void setFailCount(int failCount) { this.failCount = failCount; }

    public double getPassPercentage() { return passPercentage; }
    public void setPassPercentage(double passPercentage) { this.passPercentage = passPercentage; }
}