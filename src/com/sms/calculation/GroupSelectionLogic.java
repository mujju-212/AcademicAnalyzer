package com.sms.calculation;

import com.sms.calculation.models.Component;
import com.sms.calculation.models.ComponentGroup;
import java.util.*;
import java.util.stream.Collectors;

public class GroupSelectionLogic {

    /**
     * Apply group selection rules to get final components for calculation
     */
    public static List<Component> applyGroupSelection(ComponentGroup group) {
        List<Component> countedComponents = group.getComponents().stream()
            .filter(Component::isCounted)
            .collect(Collectors.toList());

        switch (group.getSelectionMode().toLowerCase()) {
            case "bestn":
                return selectBestN(countedComponents, group.getSelectionValue());
            case "droplowest":
                return dropLowest(countedComponents, group.getSelectionValue());
            case "all":
            default:
                return countedComponents;
        }
    }

    /**
     * Select best N components based on percentage
     */
    private static List<Component> selectBestN(List<Component> components, int n) {
        if (n <= 0 || components.isEmpty()) {
            return new ArrayList<>();
        }

        return components.stream()
            .sorted((c1, c2) -> Double.compare(c2.getPercentage(), c1.getPercentage()))
            .limit(Math.min(n, components.size()))
            .collect(Collectors.toList());
    }

    /**
     * Drop lowest N components
     */
    private static List<Component> dropLowest(List<Component> components, int dropCount) {
        if (dropCount <= 0 || components.isEmpty()) {
            return components;
        }

        if (dropCount >= components.size()) {
            return new ArrayList<>();
        }

        return components.stream()
            .sorted((c1, c2) -> Double.compare(c2.getPercentage(), c1.getPercentage()))
            .limit(components.size() - dropCount)
            .collect(Collectors.toList());
    }

    /**
     * Calculate group score based on selected components
     */
    public static double calculateGroupScore(ComponentGroup group) {
        List<Component> selectedComponents = applyGroupSelection(group);
        
        if (selectedComponents.isEmpty()) {
            return 0.0;
        }

        // Calculate average percentage of selected components
        double totalPercentage = selectedComponents.stream()
            .mapToDouble(Component::getPercentage)
            .sum();

        return totalPercentage / selectedComponents.size();
    }

    /**
     * Get components that were excluded by group selection
     */
    public static List<Component> getExcludedComponents(ComponentGroup group) {
        List<Component> allComponents = group.getComponents().stream()
            .filter(Component::isCounted)
            .collect(Collectors.toList());
        
        List<Component> selectedComponents = applyGroupSelection(group);
        
        List<Component> excluded = new ArrayList<>(allComponents);
        excluded.removeAll(selectedComponents);
        
        return excluded;
    }

    /**
     * Validate group selection configuration
     */
    public static boolean isValidGroupConfiguration(ComponentGroup group) {
        int componentCount = group.getCountedComponentCount();
        
        switch (group.getSelectionMode().toLowerCase()) {
            case "bestn":
                return group.getSelectionValue() > 0 && 
                       group.getSelectionValue() <= componentCount;
            case "droplowest":
                return group.getSelectionValue() >= 0 && 
                       group.getSelectionValue() < componentCount;
            case "all":
                return true;
            default:
                return false;
        }
    }

    /**
     * Get selection summary for display
     */
    public static String getSelectionSummary(ComponentGroup group) {
        List<Component> selected = applyGroupSelection(group);
        int total = group.getCountedComponentCount();
        
        switch (group.getSelectionMode().toLowerCase()) {
            case "bestn":
                return String.format("Best %d of %d components", 
                    selected.size(), total);
            case "droplowest":
                return String.format("All except lowest %d (using %d of %d)", 
                    group.getSelectionValue(), selected.size(), total);
            case "all":
                return String.format("All %d components", total);
            default:
                return "Unknown selection mode";
        }
    }
}