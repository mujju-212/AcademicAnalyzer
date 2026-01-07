package com.sms.calculation.models;

import java.util.List;
import java.util.ArrayList;

public class ComponentGroup {
    private int id;
    private String name;
    private String selectionMode; // "all", "bestN", "dropLowest"
    private int selectionValue; // N for bestN, number to drop for dropLowest
    private double groupWeight; // Total weight of this group
    private List<Component> components;

    // Constructors
    public ComponentGroup() {
        this.components = new ArrayList<>();
    }

    public ComponentGroup(int id, String name, String selectionMode, 
                         int selectionValue, double groupWeight) {
        this.id = id;
        this.name = name;
        this.selectionMode = selectionMode;
        this.selectionValue = selectionValue;
        this.groupWeight = groupWeight;
        this.components = new ArrayList<>();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSelectionMode() { return selectionMode; }
    public void setSelectionMode(String selectionMode) { this.selectionMode = selectionMode; }

    public int getSelectionValue() { return selectionValue; }
    public void setSelectionValue(int selectionValue) { this.selectionValue = selectionValue; }

    public double getGroupWeight() { return groupWeight; }
    public void setGroupWeight(double groupWeight) { this.groupWeight = groupWeight; }

    public List<Component> getComponents() { return components; }
    public void setComponents(List<Component> components) { this.components = components; }

    public void addComponent(Component component) {
        this.components.add(component);
    }

    public int getComponentCount() {
        return components.size();
    }

    public int getCountedComponentCount() {
        return (int) components.stream().filter(Component::isCounted).count();
    }
}