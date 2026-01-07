package com.sms.calculation;

import com.sms.calculation.models.Component;
import com.sms.calculation.models.ComponentGroup;
import java.util.*;

public class ComponentWeightManager {
    
    private Map<Integer, Double> componentWeightOverrides;
    private Map<Integer, Double> groupWeightOverrides;
    
    public ComponentWeightManager() {
        this.componentWeightOverrides = new HashMap<>();
        this.groupWeightOverrides = new HashMap<>();
    }

    /**
     * Set weight override for a specific component
     */
    public void setComponentWeightOverride(int componentId, double weight) {
        componentWeightOverrides.put(componentId, weight);
    }

    /**
     * Set weight override for a component group
     */
    public void setGroupWeightOverride(int groupId, double weight) {
        groupWeightOverrides.put(groupId, weight);
    }

    /**
     * Get effective weight for a component (override or default)
     */
    public double getEffectiveComponentWeight(Component component) {
        return componentWeightOverrides.getOrDefault(component.getId(), component.getWeight());
    }

    /**
     * Get effective weight for a group (override or default)
     */
    public double getEffectiveGroupWeight(ComponentGroup group) {
        return groupWeightOverrides.getOrDefault(group.getId(), group.getGroupWeight());
    }

    /**
     * Apply weight overrides to a list of components
     */
    public void applyWeightOverrides(List<Component> components) {
        for (Component component : components) {
            if (componentWeightOverrides.containsKey(component.getId())) {
                component.setWeight(componentWeightOverrides.get(component.getId()));
            }
        }
    }

    /**
     * Apply weight overrides to a list of groups
     */
    public void applyGroupWeightOverrides(List<ComponentGroup> groups) {
        for (ComponentGroup group : groups) {
            if (groupWeightOverrides.containsKey(group.getId())) {
                group.setGroupWeight(groupWeightOverrides.get(group.getId()));
            }
        }
    }

    /**
     * Validate that total weights sum to 100%
     */
    public boolean validateTotalWeights(List<Component> components) {
        double totalWeight = components.stream()
            .filter(Component::isCounted)
            .mapToDouble(this::getEffectiveComponentWeight)
            .sum();
        
        return Math.abs(totalWeight - 100.0) < 0.01; // Allow small floating point errors
    }

    /**
     * Validate that group weights sum to 100%
     */
    public boolean validateGroupWeights(List<ComponentGroup> groups) {
        double totalWeight = groups.stream()
            .mapToDouble(this::getEffectiveGroupWeight)
            .sum();
        
        return Math.abs(totalWeight - 100.0) < 0.01;
    }

    /**
     * Normalize weights to sum to 100%
     */
    public void normalizeComponentWeights(List<Component> components) {
        double totalWeight = components.stream()
            .filter(Component::isCounted)
            .mapToDouble(this::getEffectiveComponentWeight)
            .sum();
        
        if (totalWeight > 0 && Math.abs(totalWeight - 100.0) > 0.01) {
            double factor = 100.0 / totalWeight;
            for (Component component : components) {
                if (component.isCounted()) {
                    double newWeight = getEffectiveComponentWeight(component) * factor;
                    setComponentWeightOverride(component.getId(), newWeight);
                }
            }
        }
    }

    /**
     * Clear all overrides
     */
    public void clearAllOverrides() {
        componentWeightOverrides.clear();
        groupWeightOverrides.clear();
    }

    /**
     * Get summary of current overrides
     */
    public Map<String, Object> getOverrideSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("componentOverrides", new HashMap<>(componentWeightOverrides));
        summary.put("groupOverrides", new HashMap<>(groupWeightOverrides));
        summary.put("totalComponentOverrides", componentWeightOverrides.size());
        summary.put("totalGroupOverrides", groupWeightOverrides.size());
        return summary;
    }
}