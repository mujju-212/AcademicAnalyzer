package com.sms.dao;

import java.sql.*;
import java.util.*;

import javax.swing.JOptionPane;

import com.sms.database.DatabaseConnection;
import com.sms.analyzer.Student;

public class AnalyzerDAO {
    
    // Get student by name and roll number for current user
    public Student getStudentByNameAndRoll(String name, String rollNumber, int userId) {
        Student student = null;
        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT s.*, sec.section_name FROM students s " +
                          "JOIN sections sec ON s.section_id = sec.id " +
                          "WHERE s.student_name = ? AND s.roll_number = ? " +
                          "AND s.created_by = ?";
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, name);
            ps.setString(2, rollNumber);
            ps.setInt(3, userId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                Map<String, Integer> marks = getStudentMarks(rs.getInt("id"));
                student = new Student(
                    rs.getString("student_name"),
                    rs.getString("roll_number"),
                    marks
                );
                student.setSection(rs.getString("section_name"));
            }
            
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return student;
    }

    public List<Student> getStudentsBySection(int sectionId, int userId) {
        List<Student> students = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            // Use alias for student id to avoid ambiguity
            String query = "SELECT s.id as student_id, s.student_name, s.roll_number, " +
                          "sec.section_name FROM students s " +
                          "JOIN sections sec ON s.section_id = sec.id " +
                          "WHERE s.section_id = ? AND s.created_by = ?";
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, sectionId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                // Use the aliased column name
                Map<String, Integer> marks = getStudentMarks(rs.getInt("student_id"));
                Student student = new Student(
                    rs.getString("student_name"),
                    rs.getString("roll_number"),
                    marks
                );
                student.setSection(rs.getString("section_name"));
                students.add(student);
            }
            
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return students;
    }
    
    // Get marks for a student
    private Map<String, Integer> getStudentMarks(int studentId) {
        Map<String, Integer> marks = new HashMap<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT sub.subject_name, sm.marks_obtained " +
                          "FROM student_marks sm " +
                          "JOIN subjects sub ON sm.subject_id = sub.id " +
                          "WHERE sm.student_id = ?";
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                marks.put(rs.getString("subject_name"), rs.getInt("marks_obtained"));
            }
            
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return marks;
    }
    
    // Get section analysis data
    public SectionAnalysisData getSectionAnalysis(int sectionId, int userId) {
        SectionAnalysisData data = new SectionAnalysisData();
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            // Get subject-wise analysis - REWRITTEN QUERY
            String subjectQuery = 
                "SELECT " +
                "    sub.subject_name, " +
                "    ss.max_marks, " +
                "    ss.passing_marks, " +
                "    (SELECT COUNT(DISTINCT id) FROM students WHERE section_id = ? AND created_by = ?) as total_students, " +
                "    COUNT(DISTINCT CASE WHEN sm.marks_obtained >= ss.passing_marks THEN sm.student_id END) as pass_count, " +
                "    COUNT(DISTINCT CASE WHEN sm.marks_obtained >= ss.max_marks * 0.75 THEN sm.student_id END) as distinction_count, " +
                "    COUNT(DISTINCT CASE WHEN sm.marks_obtained >= ss.max_marks * 0.60 AND sm.marks_obtained < ss.max_marks * 0.75 THEN sm.student_id END) as first_class_count, " +
                "    COUNT(DISTINCT CASE WHEN sm.marks_obtained >= ss.max_marks * 0.50 AND sm.marks_obtained < ss.max_marks * 0.60 THEN sm.student_id END) as second_class_count, " +
                "    COUNT(DISTINCT CASE WHEN sm.marks_obtained < ss.passing_marks OR sm.marks_obtained IS NULL THEN sm.student_id END) as fail_count, " +
                "    AVG(sm.marks_obtained) as avg_marks " +
                "FROM section_subjects ss " +
                "INNER JOIN subjects sub ON ss.subject_id = sub.id " +
                "LEFT JOIN student_marks sm ON sm.subject_id = ss.subject_id " +
                "    AND sm.student_id IN (SELECT id FROM students WHERE section_id = ? AND created_by = ?) " +
                "WHERE ss.section_id = ? " +
                "GROUP BY sub.id, sub.subject_name, ss.max_marks, ss.passing_marks";
            
            PreparedStatement ps = conn.prepareStatement(subjectQuery);
            ps.setInt(1, sectionId);  // for total_students subquery
            ps.setInt(2, userId);     // for total_students subquery
            ps.setInt(3, sectionId);  // for student_marks join
            ps.setInt(4, userId);     // for student_marks join
            ps.setInt(5, sectionId);  // for WHERE clause
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                SubjectAnalysis sa = new SubjectAnalysis();
                sa.subjectName = rs.getString("subject_name");
                sa.totalStudents = rs.getInt("total_students");
                sa.passCount = rs.getInt("pass_count");
                sa.distinctionCount = rs.getInt("distinction_count");
                sa.firstClassCount = rs.getInt("first_class_count");
                sa.secondClassCount = rs.getInt("second_class_count");
                sa.failCount = rs.getInt("fail_count");
                sa.averageMarks = rs.getDouble("avg_marks");
                if (Double.isNaN(sa.averageMarks)) {
                    sa.averageMarks = 0.0;
                }
                data.subjectAnalysisList.add(sa);
            }
            
            // Get top 5 students - this query should work fine
         // In AnalyzerDAO.java, update the topStudentsQuery:

         // Get top 5 students - FIXED PERCENTAGE CALCULATION
         String topStudentsQuery = 
             "SELECT s.roll_number, s.student_name, " +
             "SUM(sm.marks_obtained) as total_marks, " +
             "COUNT(sm.subject_id) as subject_count, " +
             "(SUM(sm.marks_obtained) * 100.0 / SUM(ss.max_marks)) as percentage " +
             "FROM students s " +
             "INNER JOIN student_marks sm ON s.id = sm.student_id " +
             "INNER JOIN section_subjects ss ON sm.subject_id = ss.subject_id AND s.section_id = ss.section_id " +
             "WHERE s.section_id = ? AND s.created_by = ? " +
             "GROUP BY s.id, s.roll_number, s.student_name " +
             "ORDER BY total_marks DESC " +
             "LIMIT 5";
            
            ps = conn.prepareStatement(topStudentsQuery);
            ps.setInt(1, sectionId);
            ps.setInt(2, userId);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                TopStudent ts = new TopStudent();
                ts.rollNumber = rs.getString("roll_number");
                ts.name = rs.getString("student_name");
                ts.totalMarks = rs.getInt("total_marks");
                ts.percentage = rs.getDouble("percentage");
                data.topStudents.add(ts);
            }
            
            // Get failed students analysis - SIMPLIFIED
            String failedStudentsQuery = 
                "SELECT " +
                "    COUNT(CASE WHEN fail_count = 1 THEN 1 END) as failed_1, " +
                "    COUNT(CASE WHEN fail_count = 2 THEN 1 END) as failed_2, " +
                "    COUNT(CASE WHEN fail_count = 3 THEN 1 END) as failed_3, " +
                "    COUNT(CASE WHEN fail_count = 4 THEN 1 END) as failed_4, " +
                "    COUNT(CASE WHEN fail_count = 5 THEN 1 END) as failed_5, " +
                "    COUNT(CASE WHEN fail_count >= 6 THEN 1 END) as failed_6_plus " +
                "FROM ( " +
                "    SELECT s.id, COUNT(DISTINCT ss.subject_id) - COUNT(DISTINCT CASE WHEN sm.marks_obtained >= ss.passing_marks THEN ss.subject_id END) as fail_count " +
                "    FROM students s " +
                "    CROSS JOIN section_subjects ss " +
                "    LEFT JOIN student_marks sm ON s.id = sm.student_id AND ss.subject_id = sm.subject_id " +
                "    WHERE s.section_id = ? AND s.created_by = ? AND ss.section_id = ? " +
                "    GROUP BY s.id " +
                ") as student_failures";
            
            ps = conn.prepareStatement(failedStudentsQuery);
            ps.setInt(1, sectionId);
            ps.setInt(2, userId);
            ps.setInt(3, sectionId);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                for (int i = 1; i <= 6; i++) {
                    int count = (i < 6) ? rs.getInt("failed_" + i) : rs.getInt("failed_6_plus");
                    if (count > 0) {
                        data.failedStudentsMap.put(i, count);
                    }
                }
            }
            
            // Get total students count
            String totalStudentsQuery = "SELECT COUNT(*) as total FROM students WHERE section_id = ? AND created_by = ?";
            ps = conn.prepareStatement(totalStudentsQuery);
            ps.setInt(1, sectionId);
            ps.setInt(2, userId);
            rs = ps.executeQuery();
            if (rs.next()) {
                data.totalStudents = rs.getInt("total");
            }
            
            // Calculate pass/fail students
            int totalFailed = 0;
            for (Integer count : data.failedStudentsMap.values()) {
                totalFailed += count;
            }
            data.failStudents = totalFailed;
            data.passStudents = data.totalStudents - totalFailed;
            
            // Calculate section average
            double totalAverage = 0.0;
            int subjectCount = 0;
            for (SubjectAnalysis subject : data.subjectAnalysisList) {
                if (subject.averageMarks > 0) {
                    totalAverage += subject.averageMarks;
                    subjectCount++;
                }
            }
            data.sectionAverage = (subjectCount > 0) ? totalAverage / subjectCount : 0.0;
            
            rs.close();
            ps.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, 
                "Error loading section analysis: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
        }
        
        return data;
    }
    
    // Inner classes for data structures
    public static class SectionAnalysisData {
        public List<SubjectAnalysis> subjectAnalysisList = new ArrayList<>();
        public List<TopStudent> topStudents = new ArrayList<>();
        public Map<Integer, Integer> failedStudentsMap = new HashMap<>();
        public int totalStudents;
        public int passStudents;
        public int failStudents;
        public double sectionAverage;  // ADDED THIS FIELD
    }
    
    public static class SubjectAnalysis {
        public String subjectName;
        public int totalStudents;
        public int passCount;
        public int distinctionCount;
        public int firstClassCount;
        public int secondClassCount;
        public int failCount;
        public double averageMarks;
    }
    
    public static class TopStudent {
        public String rollNumber;
        public String name;
        public int totalMarks;
        public double percentage;
    }
    
    // ===== ADDED CLASSES FOR RESULT LAUNCHER =====
    
    public static class ComponentInfo {
        public int id;
        public String name;
        public String type; // internal/external
        public int maxMarks;
        public int scaledMarks;
        public String groupName;
        public int sequenceOrder;
        public String groupSelectionType;
        public int groupSelectionCount;
        
        @Override
        public String toString() {
            return name + " (" + maxMarks + ")";
        }
    }
    
    public static class StudentComponentMark {
        public int componentId;
        public double marksObtained;
        public double scaledMarks;
        public boolean isCounted;
        public String componentName;
        public int maxMarks;
        public int scaledToMarks;
    }
    
    public static class SubjectInfo {
        public int id;
        public String name;
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    // ===== ADDED METHODS FOR RESULT LAUNCHER =====
    
    public List<ComponentInfo> getComponentsForSubject(int sectionId, int subjectId, String componentType) {
        List<ComponentInfo> components = new ArrayList<>();
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            StringBuilder query = new StringBuilder();
            query.append("SELECT mc.id, mc.component_name, mc.component_type, mc.actual_max_marks, ")
                 .append("mc.scaled_to_marks, mc.component_group, mc.sequence_order, ")
                 .append("cg.group_name, cg.selection_type, cg.selection_count ")
                 .append("FROM marking_components mc ")
                 .append("LEFT JOIN component_groups cg ON mc.group_id = cg.id ")
                 .append("JOIN marking_schemes ms ON mc.scheme_id = ms.id ")
                 .append("WHERE ms.section_id = ? AND ms.subject_id = ? ");
            
            if (!"all".equals(componentType)) {
                query.append("AND mc.component_type = ? ");
            }
            
            query.append("ORDER BY mc.sequence_order, mc.component_name");
            
            PreparedStatement ps = conn.prepareStatement(query.toString());
            ps.setInt(1, sectionId);
            ps.setInt(2, subjectId);
            
            if (!"all".equals(componentType)) {
                ps.setString(3, componentType);
            }
            
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                ComponentInfo comp = new ComponentInfo();
                comp.id = rs.getInt("id");
                comp.name = rs.getString("component_name");
                comp.type = rs.getString("component_type");
                comp.maxMarks = rs.getInt("actual_max_marks");
                comp.scaledMarks = rs.getInt("scaled_to_marks");
                comp.groupName = rs.getString("component_group");
                comp.sequenceOrder = rs.getInt("sequence_order");
                comp.groupSelectionType = rs.getString("selection_type");
                comp.groupSelectionCount = rs.getInt("selection_count");
                
                components.add(comp);
            }
            
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return components;
    }
    
    public Map<Integer, StudentComponentMark> getStudentComponentMarks(int studentId, List<Integer> componentIds) {
        Map<Integer, StudentComponentMark> marks = new HashMap<>();
        
        if (componentIds.isEmpty()) return marks;
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            String placeholders = String.join(",", Collections.nCopies(componentIds.size(), "?"));
            
            String query = "SELECT scm.component_id, scm.marks_obtained, scm.scaled_marks, " +
                          "scm.is_counted, mc.component_name, mc.actual_max_marks, mc.scaled_to_marks " +
                          "FROM student_component_marks scm " +
                          "JOIN marking_components mc ON scm.component_id = mc.id " +
                          "WHERE scm.student_id = ? AND scm.component_id IN (" + placeholders + ")";
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, studentId);
            
            for (int i = 0; i < componentIds.size(); i++) {
                ps.setInt(i + 2, componentIds.get(i));
            }
            
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                StudentComponentMark mark = new StudentComponentMark();
                mark.componentId = rs.getInt("component_id");
                mark.marksObtained = rs.getDouble("marks_obtained");
                mark.scaledMarks = rs.getDouble("scaled_marks");
                mark.isCounted = rs.getBoolean("is_counted");
                mark.componentName = rs.getString("component_name");
                mark.maxMarks = rs.getInt("actual_max_marks");
                mark.scaledToMarks = rs.getInt("scaled_to_marks");
                
                marks.put(mark.componentId, mark);
            }
            
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return marks;
    }
    
    public List<SubjectInfo> getSubjectsForSection(int sectionId) {
        List<SubjectInfo> subjects = new ArrayList<>();
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT DISTINCT s.id, s.subject_name " +
                          "FROM subjects s " +
                          "JOIN marking_schemes ms ON s.id = ms.subject_id " +
                          "WHERE ms.section_id = ? " +
                          "ORDER BY s.subject_name";
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, sectionId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                SubjectInfo subject = new SubjectInfo();
                subject.id = rs.getInt("id");
                subject.name = rs.getString("subject_name");
                subjects.add(subject);
            }
            
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return subjects;
    }
}