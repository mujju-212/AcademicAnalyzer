package com.sms.calculation;

import com.sms.calculation.models.Component;
import java.util.List;

public class CalculationUtils {
    
    // Grade calculation constants
    private static final double[] GRADE_THRESHOLDS = {90, 80, 70, 60, 50, 40};
    private static final String[] GRADES = {"A+", "A", "B", "C", "D", "E", "F"};
    
    // SGPA calculation constants
    private static final double[] SGPA_THRESHOLDS = {90, 80, 70, 60, 50, 40};
    private static final double[] SGPA_VALUES = {10.0, 9.0, 8.0, 7.0, 6.0, 5.0, 0.0};

    /**
     * Calculate grade based on percentage
     */
    public static String calculateGrade(double percentage) {
        for (int i = 0; i < GRADE_THRESHOLDS.length; i++) {
            if (percentage >= GRADE_THRESHOLDS[i]) {
                return GRADES[i];
            }
        }
        return GRADES[GRADES.length - 1]; // Return F if below all thresholds
    }

    /**
     * Calculate SGPA based on percentage
     */
    public static double calculateSGPA(double percentage) {
        // Calculate CGPA as percentage / 10 (e.g., 94.14% = 9.41 CGPA)
        return percentage / 10.0;
    }

    /**
     * Round to specified decimal places
     */
    public static double roundToDecimalPlaces(double value, int decimalPlaces) {
        double multiplier = Math.pow(10, decimalPlaces);
        return Math.round(value * multiplier) / multiplier;
    }

    /**
     * Calculate percentage from obtained and total marks
     */
    public static double calculatePercentage(double obtained, double total) {
        return total > 0 ? (obtained / total) * 100 : 0;
    }

    /**
     * Check if student is passing based on percentage and passing threshold
     */
    public static boolean isPassing(double percentage, double passingThreshold) {
        return percentage >= passingThreshold;
    }

    /**
     * Calculate weighted average of components
     */
    public static double calculateWeightedAverage(List<Component> components) {
        double totalWeightedScore = 0;
        double totalWeight = 0;

        for (Component component : components) {
            if (component.isCounted()) {
                totalWeightedScore += component.getWeightedScore();
                totalWeight += component.getWeight();
            }
        }

        return totalWeight > 0 ? totalWeightedScore / totalWeight * 100 : 0;
    }

    /**
     * Calculate simple average of components
     */
    public static double calculateSimpleAverage(List<Component> components) {
        double totalPercentage = 0;
        int count = 0;

        for (Component component : components) {
            if (component.isCounted()) {
                totalPercentage += component.getPercentage();
                count++;
            }
        }

        return count > 0 ? totalPercentage / count : 0;
    }

    /**
     * Validate if marks are within valid range
     */
    public static boolean isValidMarks(double obtained, double maximum) {
        return obtained >= 0 && obtained <= maximum;
    }

    /**
     * Format percentage for display
     */
    public static String formatPercentage(double percentage) {
        return String.format("%.2f%%", percentage);
    }

    /**
     * Format marks for display
     */
    public static String formatMarks(double obtained, double maximum) {
        return String.format("%.1f/%.1f", obtained, maximum);
    }
}