package com.sms.calculation.models;

import java.util.*;
import com.sms.dao.AnalyzerDAO;

public class CalculationResult {
    public double totalObtained;
    public double totalPossible;
    public double percentage;
    public double passingMarks;
    public boolean isPassing;
    public Map<Integer, AnalyzerDAO.StudentComponentMark> componentMarks;

    // Additional fields that StudentCalculator needs
    private int studentId;
    private String studentName;
    private List<Component> includedComponents;
    private String calculationMethod;
    private String grade;
    private double sgpa;
    private Map<String, Double> groupWiseScores;
    private List<String> missingComponents;

    // Constructors
    public CalculationResult() {
        this.componentMarks = new HashMap<>();
        this.includedComponents = new ArrayList<>();
        this.groupWiseScores = new HashMap<>();
        this.missingComponents = new ArrayList<>();
    }

    public CalculationResult(int studentId, String studentName) {
        this();
        this.studentId = studentId;
        this.studentName = studentName;
    }

    // REQUIRED METHODS FOR THE DASHBOARD
    public double getFinalPercentage() {
        return percentage;
    }

    public double getTotalObtained() {
        return totalObtained;
    }

    public double getTotalPossible() {
        return totalPossible;
    }

    public String getGrade() {
        if (grade != null) return grade;
        
        // Auto-calculate grade if not set
        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B";
        if (percentage >= 60) return "C";
        if (percentage >= 50) return "D";
        if (percentage >= 40) return "E";
        return "F";
    }

    // MISSING METHODS that StudentCalculator needs
    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setIncludedComponents(List<Component> includedComponents) {
        this.includedComponents = new ArrayList<>(includedComponents);
    }

    public List<Component> getIncludedComponents() {
        return includedComponents;
    }

    public void setFinalPercentage(double percentage) {
        this.percentage = percentage;
    }

    public void setCalculationMethod(String calculationMethod) {
        this.calculationMethod = calculationMethod;
    }

    public String getCalculationMethod() {
        return calculationMethod != null ? calculationMethod : "Simple Addition";
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public void setSgpa(double sgpa) {
        this.sgpa = sgpa;
    }

    public double getSgpa() {
        return sgpa;
    }

    public double getPassingMarks() {
        return passingMarks;
    }

    public boolean isPassing() {
        return isPassing;
    }

    public void setGroupWiseScores(Map<String, Double> groupWiseScores) {
        this.groupWiseScores = groupWiseScores;
    }

    public Map<String, Double> getGroupWiseScores() {
        return groupWiseScores;
    }

    public void setMissingComponents(List<String> missingComponents) {
        this.missingComponents = missingComponents;
    }

    public List<String> getMissingComponents() {
        return missingComponents;
    }

    // Existing setters
    public void setTotalObtained(double totalObtained) {
        this.totalObtained = totalObtained;
    }

    public void setTotalPossible(double totalPossible) {
        this.totalPossible = totalPossible;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public void setPassingMarks(double passingMarks) {
        this.passingMarks = passingMarks;
    }

    public void setPassing(boolean passing) {
        this.isPassing = passing;
    }

    public void setComponentMarks(Map<Integer, AnalyzerDAO.StudentComponentMark> componentMarks) {
        this.componentMarks = componentMarks;
    }

    public Map<Integer, AnalyzerDAO.StudentComponentMark> getComponentMarks() {
        return componentMarks;
    }

    @Override
    public String toString() {
        return String.format("CalculationResult{student='%s', percentage=%.2f%%, grade='%s', passing=%s}", 
                           studentName, percentage, getGrade(), isPassing);
    }
}
