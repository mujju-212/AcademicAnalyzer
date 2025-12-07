package com.sms.analyzer;

import java.util.HashMap;
import java.util.Map;

public class Student {
    private String name;
    private String rollNumber;
    private Map<String, Integer> marks;
    private String section; // Add this field
    
    public Student(String name, String rollNumber, Map<String, Integer> marks) {
        this.name = name;
        this.rollNumber = rollNumber;
        this.marks = marks != null ? marks : new HashMap<>();
    }
    
    // Add getter and setter for section
    public String getSection() {
        return section;
    }
    
    public void setSection(String section) {
        this.section = section;
    }
    
    // Existing getters
    public String getName() {
        return name;
    }
 // Add this method to Student.java
    public int getTotalMarks() {
        int total = 0;
        for (Integer mark : marks.values()) {
            if (mark != null) {
                total += mark;
            }
        }
        return total;
    }
    
    public String getRollNumber() {
        return rollNumber;
    }
    
    public Map<String, Integer> getMarks() {
        return marks;
    }
}