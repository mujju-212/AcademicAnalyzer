package com.sms.analyzer;

import java.util.HashMap;
import java.util.Map;

public class Student {
    private int id; // Add student ID for database reference
    private String name;
    private String rollNumber;
    private Map<String, Map<String, Integer>> marks; // Changed to nested map: subject -> exam type -> marks
    private String section; // Add this field
    
    public Student(String name, String rollNumber, Map<String, Map<String, Integer>> marks) {
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
    
    // Add getter and setter for id
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    // Existing getters
    public String getName() {
        return name;
    }
 // Add this method to Student.java
    public int getTotalMarks() {
        int total = 0;
        for (Map<String, Integer> examTypes : marks.values()) {
            for (Integer mark : examTypes.values()) {
                if (mark != null) {
                    total += mark;
                }
            }
        }
        return total;
    }
    
    public String getRollNumber() {
        return rollNumber;
    }
    
    public Map<String, Map<String, Integer>> getMarks() {
        return marks;
    }
}