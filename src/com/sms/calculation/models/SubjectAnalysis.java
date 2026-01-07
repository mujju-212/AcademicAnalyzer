package com.sms.calculation.models;
public class SubjectAnalysis {
    private String subjectName;
    private int totalStudents;
    private int passedStudents;
    private int failedStudents;
    private double passPercentage;
    private double averagePercentage;
    private double highestPercentage;
    private double lowestPercentage;

    // Constructors
    public SubjectAnalysis() {}

    // Getters and Setters (similar pattern as above)
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public int getTotalStudents() { return totalStudents; }
    public void setTotalStudents(int totalStudents) { this.totalStudents = totalStudents; }

    public int getPassedStudents() { return passedStudents; }
    public void setPassedStudents(int passedStudents) { this.passedStudents = passedStudents; }

    public int getFailedStudents() { return failedStudents; }
    public void setFailedStudents(int failedStudents) { this.failedStudents = failedStudents; }

    public double getPassPercentage() { return passPercentage; }
    public void setPassPercentage(double passPercentage) { this.passPercentage = passPercentage; }

    public double getAveragePercentage() { return averagePercentage; }
    public void setAveragePercentage(double averagePercentage) { this.averagePercentage = averagePercentage; }

    public double getHighestPercentage() { return highestPercentage; }
    public void setHighestPercentage(double highestPercentage) { this.highestPercentage = highestPercentage; }

    public double getLowestPercentage() { return lowestPercentage; }
    public void setLowestPercentage(double lowestPercentage) { this.lowestPercentage = lowestPercentage; }
}