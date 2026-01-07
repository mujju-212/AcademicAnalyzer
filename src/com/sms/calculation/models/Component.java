package com.sms.calculation.models;

import java.time.LocalDate;

public class Component {
    public static final float CENTER_ALIGNMENT = 0;
	public static float LEFT_ALIGNMENT;
	public static float CENTER_ALIGNMENT1;
	private int id;
    private String name;
    private String type; // "internal", "external", "assignment", etc.
    private double obtainedMarks;
    private double maxMarks;
    private double weight; // percentage this component contributes
    private int groupId;
    private String groupName;
    private LocalDate assessmentDate;
    private boolean isCounted;
    private int sequenceOrder;

    // Constructors
    public Component() {}

    public Component(int id, String name, String type, double obtainedMarks, 
                    double maxMarks, double weight) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.obtainedMarks = obtainedMarks;
        this.maxMarks = maxMarks;
        this.weight = weight;
        this.isCounted = true;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getObtainedMarks() { return obtainedMarks; }
    public void setObtainedMarks(double obtainedMarks) { this.obtainedMarks = obtainedMarks; }

    public double getMaxMarks() { return maxMarks; }
    public void setMaxMarks(double maxMarks) { this.maxMarks = maxMarks; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public int getGroupId() { return groupId; }
    public void setGroupId(int groupId) { this.groupId = groupId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public LocalDate getAssessmentDate() { return assessmentDate; }
    public void setAssessmentDate(LocalDate assessmentDate) { this.assessmentDate = assessmentDate; }

    public boolean isCounted() { return isCounted; }
    public void setCounted(boolean counted) { isCounted = counted; }

    public int getSequenceOrder() { return sequenceOrder; }
    public void setSequenceOrder(int sequenceOrder) { this.sequenceOrder = sequenceOrder; }

    // Utility methods
    public double getPercentage() {
        return maxMarks > 0 ? (obtainedMarks / maxMarks) * 100 : 0;
    }

    public double getWeightedScore() {
        return getPercentage() * (weight / 100);
    }

    @Override
    public String toString() {
        return String.format("%s: %.1f/%.1f (%.1f%%)", name, obtainedMarks, maxMarks, getPercentage());
    }
}