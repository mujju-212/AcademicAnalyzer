package com.sms.calculation;

import com.sms.calculation.models.*;
import java.util.*;
import java.util.stream.Collectors;

public class SectionCalculator {
    
    private StudentCalculator studentCalculator;
    
    public SectionCalculator() {
        this.studentCalculator = new StudentCalculator();
    }
    
    public SectionCalculator(double passingThreshold) {
        this.studentCalculator = new StudentCalculator(passingThreshold);
    }

    /**
     * Calculate section-wide statistics
     */
    public SectionResult calculateSectionStatistics(List<CalculationResult> studentResults) {
        SectionResult sectionResult = new SectionResult();
        
        if (studentResults.isEmpty()) {
            return sectionResult;
        }
        
        // Basic counts
        int totalStudents = studentResults.size();
        int passedStudents = (int) studentResults.stream()
            .filter(CalculationResult::isPassing)
            .count();
        int failedStudents = totalStudents - passedStudents;
        
        // Percentage calculations
        List<Double> percentages = studentResults.stream()
            .map(CalculationResult::getFinalPercentage)
            .collect(Collectors.toList());
        
        double averagePercentage = percentages.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        
        double highestPercentage = percentages.stream()
            .mapToDouble(Double::doubleValue)
            .max()
            .orElse(0.0);
        
        double lowestPercentage = percentages.stream()
            .mapToDouble(Double::doubleValue)
            .min()
            .orElse(0.0);
        
        // Standard deviation
        double standardDeviation = calculateStandardDeviation(percentages, averagePercentage);
        
        // Grade distribution
        Map<String, Integer> gradeDistribution = calculateGradeDistribution(studentResults);
        
        // Top performers (top 10% or minimum 3)
        int topCount = Math.max(3, totalStudents / 10);
        List<CalculationResult> topPerformers = studentResults.stream()
                .sorted((r1, r2) -> Double.compare(r2.getFinalPercentage(), r1.getFinalPercentage()))
                .limit(topCount)
                .collect(Collectors.toList());
            
            // Bottom performers (students who failed)
            List<CalculationResult> bottomPerformers = studentResults.stream()
                .filter(r -> !r.isPassing())
                .sorted((r1, r2) -> Double.compare(r1.getFinalPercentage(), r2.getFinalPercentage()))
                .collect(Collectors.toList());
            
            // Set all values in section result
            sectionResult.setTotalStudents(totalStudents);
            sectionResult.setPassedStudents(passedStudents);
            sectionResult.setFailedStudents(failedStudents);
            sectionResult.setPassPercentage(CalculationUtils.roundToDecimalPlaces(
                (double) passedStudents / totalStudents * 100, 2));
            sectionResult.setAveragePercentage(CalculationUtils.roundToDecimalPlaces(averagePercentage, 2));
            sectionResult.setHighestPercentage(CalculationUtils.roundToDecimalPlaces(highestPercentage, 2));
            sectionResult.setLowestPercentage(CalculationUtils.roundToDecimalPlaces(lowestPercentage, 2));
            sectionResult.setStandardDeviation(CalculationUtils.roundToDecimalPlaces(standardDeviation, 2));
            sectionResult.setGradeDistribution(gradeDistribution);
            sectionResult.setTopPerformers(topPerformers);
            sectionResult.setBottomPerformers(bottomPerformers);
            
            return sectionResult;
        }

        /**
         * Calculate component-wise section analysis
         */
        public Map<String, ComponentAnalysis> calculateComponentWiseAnalysis(
                Map<Integer, List<Component>> studentComponents) {
            
            Map<String, ComponentAnalysis> componentAnalysis = new HashMap<>();
            
            // Group all components by name
            Map<String, List<Component>> componentsByName = new HashMap<>();
            
            for (List<Component> components : studentComponents.values()) {
                for (Component component : components) {
                    componentsByName.computeIfAbsent(component.getName(), k -> new ArrayList<>())
                        .add(component);
                }
            }
            
            // Analyze each component
            for (Map.Entry<String, List<Component>> entry : componentsByName.entrySet()) {
                String componentName = entry.getKey();
                List<Component> components = entry.getValue();
                
                ComponentAnalysis analysis = new ComponentAnalysis();
                analysis.setComponentName(componentName);
                analysis.setTotalStudents(components.size());
                
                // Calculate statistics
                List<Double> percentages = components.stream()
                    .map(Component::getPercentage)
                    .collect(Collectors.toList());
                
                double average = percentages.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
                
                double highest = percentages.stream()
                    .mapToDouble(Double::doubleValue)
                    .max()
                    .orElse(0.0);
                
                double lowest = percentages.stream()
                    .mapToDouble(Double::doubleValue)
                    .min()
                    .orElse(0.0);
                
                int passCount = (int) percentages.stream()
                    .filter(p -> p >= studentCalculator.getPassingThreshold())
                    .count();
                
                analysis.setAveragePercentage(CalculationUtils.roundToDecimalPlaces(average, 2));
                analysis.setHighestPercentage(CalculationUtils.roundToDecimalPlaces(highest, 2));
                analysis.setLowestPercentage(CalculationUtils.roundToDecimalPlaces(lowest, 2));
                analysis.setPassCount(passCount);
                analysis.setFailCount(components.size() - passCount);
                analysis.setPassPercentage(CalculationUtils.roundToDecimalPlaces(
                    (double) passCount / components.size() * 100, 2));
                
                componentAnalysis.put(componentName, analysis);
            }
            
            return componentAnalysis;
        }

        /**
         * Calculate subject-wise section analysis
         */
        public Map<String, SubjectAnalysis> calculateSubjectWiseAnalysis(
                Map<Integer, Map<String, List<Component>>> studentSubjectComponents) {
            
            Map<String, SubjectAnalysis> subjectAnalysis = new HashMap<>();
            
            // Get all unique subjects
            Set<String> allSubjects = studentSubjectComponents.values().stream()
                .flatMap(map -> map.keySet().stream())
                .collect(Collectors.toSet());
            
            for (String subject : allSubjects) {
                SubjectAnalysis analysis = new SubjectAnalysis();
                analysis.setSubjectName(subject);
                
                List<CalculationResult> subjectResults = new ArrayList<>();
                
                // Calculate each student's result for this subject
                for (Map.Entry<Integer, Map<String, List<Component>>> studentEntry : 
                     studentSubjectComponents.entrySet()) {
                    
                    int studentId = studentEntry.getKey();
                    Map<String, List<Component>> subjects = studentEntry.getValue();
                    
                    if (subjects.containsKey(subject)) {
                        List<Component> components = subjects.get(subject);
                        CalculationResult result = studentCalculator.calculateStudentMarks(
                            studentId, "Student_" + studentId, components);
                        subjectResults.add(result);
                    }
                }
                
                // Calculate subject statistics
                if (!subjectResults.isEmpty()) {
                    SectionResult subjectSectionResult = calculateSectionStatistics(subjectResults);
                    analysis.setTotalStudents(subjectSectionResult.getTotalStudents());
                    analysis.setPassedStudents(subjectSectionResult.getPassedStudents());
                    analysis.setFailedStudents(subjectSectionResult.getFailedStudents());
                    analysis.setAveragePercentage(subjectSectionResult.getAveragePercentage());
                    analysis.setHighestPercentage(subjectSectionResult.getHighestPercentage());
                    analysis.setLowestPercentage(subjectSectionResult.getLowestPercentage());
                    analysis.setPassPercentage(subjectSectionResult.getPassPercentage());
                }
                
                subjectAnalysis.put(subject, analysis);
            }
            
            return subjectAnalysis;
        }

        /**
         * Calculate standard deviation
         */
        private double calculateStandardDeviation(List<Double> values, double mean) {
            if (values.size() <= 1) return 0.0;
            
            double sumSquaredDifferences = values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .sum();
            
            return Math.sqrt(sumSquaredDifferences / (values.size() - 1));
        }

        /**
         * Calculate grade distribution
         */
        private Map<String, Integer> calculateGradeDistribution(List<CalculationResult> results) {
            Map<String, Integer> distribution = new HashMap<>();
            
            for (CalculationResult result : results) {
                String grade = result.getGrade();
                distribution.put(grade, distribution.getOrDefault(grade, 0) + 1);
            }
            
            return distribution;
        }

        // Getter and Setter
        public StudentCalculator getStudentCalculator() {
            return studentCalculator;
        }

        public void setStudentCalculator(StudentCalculator studentCalculator) {
            this.studentCalculator = studentCalculator;
        }
    }