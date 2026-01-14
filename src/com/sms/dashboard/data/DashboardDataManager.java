package com.sms.dashboard.data;

import java.sql.*;
import java.util.*;
import com.sms.analyzer.Student;
import com.sms.database.DatabaseConnection;
import com.sms.dao.SectionDAO;

public class DashboardDataManager {
    private HashMap<String, List<Student>> sectionStudents;
    private int currentUserId;
    
    public DashboardDataManager() {
        this.currentUserId = com.sms.login.LoginScreen.currentUserId;
        loadDataFromDatabase();
    }
    
    public DashboardDataManager(int userId) {
        this.currentUserId = userId;
        loadDataFromDatabase();
    }
    
    private void loadDataFromDatabase() {
        sectionStudents = new HashMap<>();
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            // Get all sections for current user
            SectionDAO sectionDAO = new SectionDAO();
            List<SectionDAO.SectionInfo> sections = sectionDAO.getSectionsByUser(currentUserId);
            
            for (SectionDAO.SectionInfo section : sections) {
                List<Student> students = getStudentsForSection(section.id);
                sectionStudents.put(section.sectionName, students);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            // Fall back to empty data
            sectionStudents = new HashMap<>();
        }
    }
    
    private List<Student> getStudentsForSection(int sectionId) {
        List<Student> students = new ArrayList<>();
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT s.student_name, s.roll_number, " +
                          "sub.subject_name, et.exam_name as exam_type, sm.marks_obtained " +
                          "FROM students s " +
                          "LEFT JOIN student_marks sm ON s.id = sm.student_id " +
                          "LEFT JOIN subjects sub ON sm.subject_id = sub.id " +
                          "LEFT JOIN exam_types et ON sm.exam_type_id = et.id " +
                          "WHERE s.section_id = ? AND s.created_by = ?";
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, sectionId);
            ps.setInt(2, currentUserId);
            ResultSet rs = ps.executeQuery();
            
            Map<String, Student> studentMap = new HashMap<>();
            
            while (rs.next()) {
                String rollNumber = rs.getString("roll_number");
                String name = rs.getString("student_name");
                String subject = rs.getString("subject_name");
                int marks = rs.getInt("marks_obtained");
                
                Student student = studentMap.get(rollNumber);
                if (student == null) {
                    student = new Student(name, rollNumber, new HashMap<>());
                    studentMap.put(rollNumber, student);
                }
                
                if (subject != null) {
                    String examType = rs.getString("exam_type");
                    student.getMarks().putIfAbsent(subject, new HashMap<>());
                    student.getMarks().get(subject).put(examType != null ? examType : "Default", marks);
                }
            }
            
            students.addAll(studentMap.values());
            
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return students;
    }
    
    public void refreshData() {
        loadDataFromDatabase();
    }
    
    // Add this method that DashboardScreen is looking for
    public HashMap<String, List<Student>> getSectionStudents() {
        return sectionStudents;
    }
    
    // Keep these methods from the original implementation
    public String[] getSectionNames() {
        return sectionStudents.keySet().toArray(new String[0]);
    }
    
    public void addStudentEntry(String section, String name, String roll, HashMap<String, Map<String, Integer>> marks) {
        Student newStudent = new Student(name, roll, marks);
        sectionStudents.computeIfAbsent(section, k -> new ArrayList<>()).add(newStudent);
    }
    public void refreshForCurrentUser() {
        this.currentUserId = com.sms.login.LoginScreen.currentUserId;
        loadDataFromDatabase();
    }

    public String[] getSubjectsForSection(String section) {
        // This should ideally fetch from database
        // For now, return subjects if students exist in the section
        List<Student> students = sectionStudents.get(section);
        if (students != null && !students.isEmpty()) {
            Student firstStudent = students.get(0);
            if (firstStudent.getMarks() != null) {
                return firstStudent.getMarks().keySet().toArray(new String[0]);
            }
        }
        return new String[0];
    }
}