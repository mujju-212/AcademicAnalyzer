package com.sms.calculation.models;

import java.util.List;
import java.util.Map;

public class SectionResult {
    private int totalStudents;
    private int passedStudents;
    private int failedStudents;
    private double passPercentage;
    private double averagePercentage;
    private double highestPercentage;
    private double lowestPercentage;
    private double standardDeviation;
    private Map<String, Integer> gradeDistribution;
    private List<CalculationResult> topPerformers;
    private List<CalculationResult> bottomPerformers;

    // Constructors
    public SectionResult() {}

    // Getters and Setters
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

    public double getStandardDeviation() { return standardDeviation; }
    public void setStandardDeviation(double standardDeviation) { this.standardDeviation = standardDeviation; }

    public Map<String, Integer> getGradeDistribution() { return gradeDistribution; }
    public void setGradeDistribution(Map<String, Integer> gradeDistribution) { this.gradeDistribution = gradeDistribution; }

    public List<CalculationResult> getTopPerformers() { return topPerformers; }
    public void setTopPerformers(List<CalculationResult> topPerformers) { this.topPerformers = topPerformers; }

    public List<CalculationResult> getBottomPerformers() { return bottomPerformers; }
    public void setBottomPerformers(List<CalculationResult> bottomPerformers) { this.bottomPerformers = bottomPerformers; }
}