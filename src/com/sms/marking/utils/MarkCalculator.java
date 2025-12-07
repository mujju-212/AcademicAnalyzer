package com.sms.marking.utils;

import com.sms.marking.models.*;
import com.sms.marking.dao.*;
import java.sql.SQLException;
import java.util.*;

public class MarkCalculator {
    
    private final StudentComponentMarkDAO markDAO;
    private final MarkingSchemeDAO schemeDAO;
    
    public MarkCalculator() {
        this.markDAO = new StudentComponentMarkDAO();
        this.schemeDAO = new MarkingSchemeDAO();
    }
    
    // Calculate marks for a single student in a subject
    public CalculationResult calculateStudentSubjectMarks(int studentId, int sectionId, int subjectId) {
        try {
            // Get marking scheme
            MarkingScheme scheme = schemeDAO.getMarkingScheme(sectionId, subjectId);
            if (scheme == null) {
                return new CalculationResult("No marking scheme found for this subject");
            }
            
            // Get all student marks
            List<StudentComponentMark> studentMarks = markDAO.getStudentMarks(studentId, subjectId);
            
            // Create map for easy lookup
            Map<Integer, Double> marksMap = new HashMap<>();
            for (StudentComponentMark mark : studentMarks) {
                if (mark.hasMarks()) {
                    marksMap.put(mark.getComponentId(), mark.getMarksObtained());
                }
            }
            
            // Calculate for each group
            CalculationResult result = new CalculationResult();
            double totalInternal = 0;
            double totalExternal = 0;
            
            for (ComponentGroup group : scheme.getComponentGroups()) {
                GroupCalculation groupCalc = calculateGroupMarks(group, marksMap);
                result.addGroupCalculation(groupCalc);
                
                if ("internal".equals(group.getGroupType())) {
                    totalInternal += groupCalc.getScaledMarks();
                } else {
                    totalExternal += groupCalc.getScaledMarks();
                }
            }
            
            result.setInternalMarks(totalInternal);
            result.setExternalMarks(totalExternal);
            result.setTotalMarks(totalInternal + totalExternal);
            result.setMaxInternalMarks(scheme.getTotalInternalMarks());
            result.setMaxExternalMarks(scheme.getTotalExternalMarks());
            
            return result;
            
        } catch (SQLException e) {
            return new CalculationResult("Database error: " + e.getMessage());
        }
    }
    
    // Calculate marks for a group
    private GroupCalculation calculateGroupMarks(ComponentGroup group, Map<Integer, Double> marksMap) {
        GroupCalculation calc = new GroupCalculation(group.getGroupName(), group.getGroupType());
        calc.setMaxMarks(group.getTotalGroupMarks());
        calc.setSelectionType(group.getSelectionType());
        
        List<ComponentScore> scores = new ArrayList<>();
        
        // Collect all component scores
        for (MarkingComponent comp : group.getComponents()) {
            Double marks = marksMap.get(comp.getId());
            ComponentScore score = new ComponentScore(comp.getComponentName(), 
                                                    comp.getActualMaxMarks(),
                                                    marks != null ? marks : 0,
                                                    marks != null);
            scores.add(score);
        }
        
        // Sort by percentage for best-of selection
        if ("best_of".equals(group.getSelectionType())) {
            scores.sort((a, b) -> Double.compare(b.getPercentage(), a.getPercentage()));
        }
        
        // Calculate based on selection type
        double totalActual = 0;
        double totalMax = 0;
        int countToConsider = scores.size();
        
        if ("best_of".equals(group.getSelectionType()) && group.getSelectionCount() != null) {
            countToConsider = Math.min(group.getSelectionCount(), scores.size());
            calc.setSelectionCount(group.getSelectionCount());
        }
        
        // Mark which scores are counted
        for (int i = 0; i < scores.size(); i++) {
            ComponentScore score = scores.get(i);
            
            if (i < countToConsider && score.hasMarks()) {
                score.setCounted(true);
                totalActual += score.getMarksObtained();
                totalMax += score.getMaxMarks();
            } else {
                score.setCounted(false);
            }
            
            calc.addComponentScore(score);
        }
        
        // Calculate scaled marks
        if (totalMax > 0) {
            double scaledMarks = (totalActual / totalMax) * group.getTotalGroupMarks();
            calc.setScaledMarks(scaledMarks);
        } else {
            calc.setScaledMarks(0);
        }
        
        return calc;
    }
    
    // Calculate marks for entire section
    public List<StudentResult> calculateSectionMarks(int sectionId, int subjectId) {
        List<StudentResult> results = new ArrayList<>();
        
        try {
            // Get all students in section
            String query = "SELECT id, roll_number, student_name FROM students WHERE section_id = ? ORDER BY roll_number";
            // Execute query and calculate for each student
            // (Implementation depends on your Student DAO)
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return results;
    }
    
    // Result classes
    public static class CalculationResult {
        private boolean success;
        private String errorMessage;
        private double internalMarks;
        private double externalMarks;
        private double totalMarks;
        private int maxInternalMarks;
        private int maxExternalMarks;
        private List<GroupCalculation> groupCalculations;
        
        public CalculationResult() {
            this.success = true;
            this.groupCalculations = new ArrayList<>();
        }
        
        public CalculationResult(String errorMessage) {
            this.success = false;
            this.errorMessage = errorMessage;
            this.groupCalculations = new ArrayList<>();
        }
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        
        public double getInternalMarks() { return internalMarks; }
        public void setInternalMarks(double internalMarks) { this.internalMarks = internalMarks; }
        
        public double getExternalMarks() { return externalMarks; }
        public void setExternalMarks(double externalMarks) { this.externalMarks = externalMarks; }
        
        public double getTotalMarks() { return totalMarks; }
        public void setTotalMarks(double totalMarks) { this.totalMarks = totalMarks; }
        
        public int getMaxInternalMarks() { return maxInternalMarks; }
        public void setMaxInternalMarks(int maxInternalMarks) { this.maxInternalMarks = maxInternalMarks; }
        
        public int getMaxExternalMarks() { return maxExternalMarks; }
        public void setMaxExternalMarks(int maxExternalMarks) { this.maxExternalMarks = maxExternalMarks; }
        
        public List<GroupCalculation> getGroupCalculations() { return groupCalculations; }
        public void addGroupCalculation(GroupCalculation calc) { groupCalculations.add(calc); }
        
        public double getInternalPercentage() {
            return maxInternalMarks > 0 ? (internalMarks / maxInternalMarks) * 100 : 0;
        }
        
        public double getExternalPercentage() {
            return maxExternalMarks > 0 ? (externalMarks / maxExternalMarks) * 100 : 0;
        }
        
        public double getTotalPercentage() {
            int maxTotal = maxInternalMarks + maxExternalMarks;
            return maxTotal > 0 ? (totalMarks / maxTotal) * 100 : 0;
        }
    }
    
    public static class GroupCalculation {
        private String groupName;
        private String groupType;
        private String selectionType;
        private Integer selectionCount;
        private double maxMarks;
        private double scaledMarks;
        private List<ComponentScore> componentScores;
        
        public GroupCalculation(String groupName, String groupType) {
            this.groupName = groupName;
            this.groupType = groupType;
            this.componentScores = new ArrayList<>();
        }
        
        // Getters and setters
        public String getGroupName() { return groupName; }
        public String getGroupType() { return groupType; }
        
        public String getSelectionType() { return selectionType; }
        public void setSelectionType(String selectionType) { this.selectionType = selectionType; }
        
        public Integer getSelectionCount() { return selectionCount; }
        public void setSelectionCount(Integer selectionCount) { this.selectionCount = selectionCount; }
        
        public double getMaxMarks() { return maxMarks; }
        public void setMaxMarks(double maxMarks) { this.maxMarks = maxMarks; }
        
        public double getScaledMarks() { return scaledMarks; }
        public void setScaledMarks(double scaledMarks) { this.scaledMarks = scaledMarks; }
        
        public List<ComponentScore> getComponentScores() { return componentScores; }
        public void addComponentScore(ComponentScore score) { componentScores.add(score); }
        
        public String getDisplayText() {
            if ("best_of".equals(selectionType) && selectionCount != null) {
                return String.format("%s (Best %d of %d)", groupName, selectionCount, componentScores.size());
            }
            return groupName;
        }
    }
    
    public static class ComponentScore {
        private String componentName;
        private double maxMarks;
        private double marksObtained;
        private boolean hasMarks;
        private boolean isCounted;
        
        public ComponentScore(String componentName, double maxMarks, double marksObtained, boolean hasMarks) {
            this.componentName = componentName;
            this.maxMarks = maxMarks;
            this.marksObtained = marksObtained;
            this.hasMarks = hasMarks;
            this.isCounted = false;
        }
        
        // Getters and setters
        public String getComponentName() { return componentName; }
        public double getMaxMarks() { return maxMarks; }
        public double getMarksObtained() { return marksObtained; }
        public boolean hasMarks() { return hasMarks; }
        public boolean isCounted() { return isCounted; }
        public void setCounted(boolean counted) { isCounted = counted; }
        
        public double getPercentage() {
            return maxMarks > 0 ? (marksObtained / maxMarks) * 100 : 0;
        }
    }
    
    public static class StudentResult {
        private int studentId;
        private String rollNumber;
        private String studentName;
        private CalculationResult calculation;
        
        // Getters and setters
        public int getStudentId() { return studentId; }
        public void setStudentId(int studentId) { this.studentId = studentId; }
        
        public String getRollNumber() { return rollNumber; }
        public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
        
        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
        
        public CalculationResult getCalculation() { return calculation; }
        public void setCalculation(CalculationResult calculation) { this.calculation = calculation; }
    }
}