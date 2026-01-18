package com.sms.calculation;

import com.sms.calculation.models.*;
import java.util.*;
import java.util.stream.Collectors;

public class StudentCalculator {
    
    private ComponentWeightManager weightManager;
    private double passingThreshold;
    
    public StudentCalculator() {
        this.weightManager = new ComponentWeightManager();
        this.passingThreshold = 40.0; // Default 40%
    }
    
    public StudentCalculator(double passingThreshold) {
        this.weightManager = new ComponentWeightManager();
        this.passingThreshold = passingThreshold;
    }

    /**
     * Calculate final marks for a student using individual components
     * Now with intelligent component type detection and appropriate calculation methods
     */
    public CalculationResult calculateStudentMarks(int studentId, String studentName, List<Component> selectedComponents) {
        CalculationResult result = new CalculationResult();
        
        result.setStudentId(studentId);
        result.setStudentName(studentName);
        result.setIncludedComponents(new ArrayList<>(selectedComponents));
        
        // Analyze component types
        ComponentTypeAnalysis typeAnalysis = analyzeComponentTypes(selectedComponents);
        System.out.println("Component Type Analysis:");
        System.out.println("  Internal components: " + typeAnalysis.internalCount);
        System.out.println("  External components: " + typeAnalysis.externalCount);
        System.out.println("  Other components: " + typeAnalysis.otherCount);
        System.out.println("  Calculation type: " + typeAnalysis.calculationType);
        
        double totalObtained = 0.0;
        double totalPossible = 0.0;
        
        // Calculate based on component types
        for (Component component : selectedComponents) {
            if (component.isCounted()) {
                totalObtained += component.getObtainedMarks();
                totalPossible += component.getMaxMarks();
                
                System.out.println("Including: " + component.getName() + 
                                 " (" + component.getType() + ") - " + 
                                 component.getObtainedMarks() + "/" + component.getMaxMarks());
            } else {
                System.out.println("Skipping (not counted): " + component.getName());
            }
        }
        
        // Set the basic totals
        result.setTotalObtained(totalObtained);
        result.setTotalPossible(totalPossible);
        
        // Calculate percentage
        double percentage = 0.0;
        if (totalPossible > 0) {
            percentage = (totalObtained / totalPossible) * 100.0;
        }
        result.setFinalPercentage(percentage);
        
        // Set calculation method based on component types
        result.setCalculationMethod(typeAnalysis.calculationType);
        
        // Calculate passing marks with different thresholds based on type
        double passingThreshold = getPassingThresholdForType(typeAnalysis.calculationType);
        double passingMarks = totalPossible * (passingThreshold / 100.0);
        result.setPassingMarks(passingMarks);
        
        // Determine if passing
        boolean isPassing = totalObtained >= passingMarks;
        result.setPassing(isPassing);
        
        // Calculate grade
        String grade = calculateGrade(percentage);
        result.setGrade(grade);
        
        // Calculate SGPA
        double sgpa = calculateSGPA(percentage);
        result.setSgpa(sgpa);
        
        System.out.println("Final calculation:");
        System.out.println("  Total Obtained: " + result.getTotalObtained());
        System.out.println("  Total Possible: " + result.getTotalPossible());
        System.out.println("  Percentage: " + result.getFinalPercentage());
        System.out.println("  Calculation Method: " + result.getCalculationMethod());
        System.out.println("  Passing Threshold: " + passingThreshold + "%");
        System.out.println("  Passing Marks: " + result.getPassingMarks());
        System.out.println("  Grade: " + result.getGrade());
        System.out.println("  SGPA: " + result.getSgpa());
        System.out.println("  Is Passing: " + result.isPassing());
        
        return result;
    }

    /**
     * Analyze the types of components selected to determine calculation method
     */
    private ComponentTypeAnalysis analyzeComponentTypes(List<Component> components) {
        ComponentTypeAnalysis analysis = new ComponentTypeAnalysis();
        
        for (Component component : components) {
            if (component.isCounted()) {
                String type = component.getType();
                if (type != null) {
                    type = type.toLowerCase().trim();
                    
                    if (type.contains("internal") || type.contains("assignment") || 
                        type.contains("quiz") || type.contains("test") || 
                        type.contains("lab") || type.contains("attendance")) {
                        analysis.internalCount++;
                    } else if (type.contains("external") || type.contains("final") || 
                              type.contains("exam") || type.contains("semester")) {
                        analysis.externalCount++;
                    } else {
                        analysis.otherCount++;
                    }
                } else {
                    analysis.otherCount++;
                }
            }
        }
        
        // Determine calculation type
        if (analysis.internalCount > 0 && analysis.externalCount > 0) {
            analysis.calculationType = "COMBINED_MARKS";
        } else if (analysis.internalCount > 0 && analysis.externalCount == 0) {
            analysis.calculationType = "INTERNAL_ONLY";
        } else if (analysis.externalCount > 0 && analysis.internalCount == 0) {
            analysis.calculationType = "EXTERNAL_ONLY";
        } else {
            analysis.calculationType = "OTHER_COMPONENTS";
        }
        
        return analysis;
    }

    /**
     * Get appropriate passing threshold based on calculation type
     */
    private double getPassingThresholdForType(String calculationType) {
        switch (calculationType) {
            case "INTERNAL_ONLY":
                return 50.0; // 50% for internal assessments
            case "EXTERNAL_ONLY":
                return 40.0; // 40% for external exams
            case "COMBINED_MARKS":
                return 40.0; // 40% for combined final marks
            case "OTHER_COMPONENTS":
            default:
                return this.passingThreshold; // Use default threshold
        }
    }

    /**
     * Inner class to hold component type analysis results
     */
    private static class ComponentTypeAnalysis {
        int internalCount = 0;
        int externalCount = 0;
        int otherCount = 0;
        String calculationType = "SIMPLE_ADDITION";
    }

    // Helper methods
    private String calculateGrade(double percentage) {
        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B";
        if (percentage >= 60) return "C";
        if (percentage >= 50) return "D";
        if (percentage >= 40) return "E";
        return "F";
    }

    private double calculateSGPA(double percentage) {
        // Calculate CGPA as percentage / 10 (e.g., 94.14% = 9.41 CGPA)
        return percentage / 10.0;
    }

    /**
     * Calculate final marks using component groups
     */
    public CalculationResult calculateStudentMarksWithGroups(int studentId, String studentName,
                                                           List<ComponentGroup> groups) {
        CalculationResult result = new CalculationResult(studentId, studentName);
        
        if (weightManager == null) {
            weightManager = new ComponentWeightManager();
        }
        
        // Apply group weight overrides
        weightManager.applyGroupWeightOverrides(groups);
        
        Map<String, Double> groupWiseScores = new HashMap<>();
        double totalWeightedScore = 0.0;
        double totalWeight = 0.0;
        List<Component> allIncludedComponents = new ArrayList<>();
        List<String> missingComponents = new ArrayList<>();
        
        for (ComponentGroup group : groups) {
            try {
                // Calculate group score
                double groupScore = GroupSelectionLogic.calculateGroupScore(group);
                groupWiseScores.put(group.getName(), groupScore);
                
                // Add to weighted total
                double groupWeight = weightManager.getEffectiveGroupWeight(group);
                totalWeightedScore += groupScore * (groupWeight / 100);
                totalWeight += groupWeight;
                
                // Collect included components
                allIncludedComponents.addAll(GroupSelectionLogic.applyGroupSelection(group));
                
                // Check for missing components in group
                for (Component component : group.getComponents()) {
                    if (!component.isCounted()) {
                        missingComponents.add(group.getName() + " - " + component.getName());
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing group " + group.getName() + ": " + e.getMessage());
                // Continue with other groups
            }
        }
        
        double finalPercentage = totalWeight > 0 ? totalWeightedScore : 0;
        
        // Calculate totals from included components
        double totalObtained = allIncludedComponents.stream()
            .mapToDouble(Component::getObtainedMarks)
            .sum();
        double totalPossible = allIncludedComponents.stream()
            .mapToDouble(Component::getMaxMarks)
            .sum();
        
        // Set result values
        result.setTotalObtained(totalObtained);
        result.setTotalPossible(totalPossible);
        result.setFinalPercentage(roundToDecimalPlaces(finalPercentage, 2));
        result.setPassingMarks(totalPossible * (passingThreshold / 100));
        result.setPassing(isPassing(finalPercentage, passingThreshold));
        result.setGrade(calculateGrade(finalPercentage));
        result.setSgpa(calculateSGPA(finalPercentage));
        result.setIncludedComponents(allIncludedComponents);
        result.setGroupWiseScores(groupWiseScores);
        result.setMissingComponents(missingComponents);
        result.setCalculationMethod("group_based");
        
        return result;
    }

    /**
     * Calculate simple total (sum of all marks)
     */
    public CalculationResult calculateSimpleTotal(int studentId, String studentName,
                                                List<Component> components) {
        CalculationResult result = new CalculationResult(studentId, studentName);
        
        List<Component> countedComponents = components.stream()
            .filter(Component::isCounted)
            .collect(Collectors.toList());
        
        double totalObtained = countedComponents.stream()
            .mapToDouble(Component::getObtainedMarks)
            .sum();
        double totalPossible = countedComponents.stream()
            .mapToDouble(Component::getMaxMarks)
            .sum();
        
        double percentage = calculatePercentage(totalObtained, totalPossible);
        
        result.setTotalObtained(totalObtained);
        result.setTotalPossible(totalPossible);
        result.setFinalPercentage(roundToDecimalPlaces(percentage, 2));
        result.setPassingMarks(totalPossible * (passingThreshold / 100));
        result.setPassing(isPassing(percentage, passingThreshold));
        result.setGrade(calculateGrade(percentage));
        result.setSgpa(calculateSGPA(percentage));
        result.setIncludedComponents(countedComponents);
        result.setCalculationMethod("simple_total");
        
        return result;
    }

    // Utility methods (replacing CalculationUtils calls)
    private double calculatePercentage(double obtained, double possible) {
        return possible > 0 ? (obtained / possible) * 100 : 0;
    }

    private double roundToDecimalPlaces(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }

    private boolean isPassing(double percentage, double threshold) {
        return percentage >= threshold;
    }

    // Getters and Setters
    public ComponentWeightManager getWeightManager() {
        return weightManager;
    }

    public void setWeightManager(ComponentWeightManager weightManager) {
        this.weightManager = weightManager;
    }

    public double getPassingThreshold() {
        return passingThreshold;
    }

    public void setPassingThreshold(double passingThreshold) {
        this.passingThreshold = passingThreshold;
    }
}
