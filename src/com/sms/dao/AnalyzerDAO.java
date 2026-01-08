package com.sms.dao;

import java.sql.*;
import java.util.*;

import javax.swing.JOptionPane;

import com.sms.database.DatabaseConnection;
import com.sms.analyzer.Student;

public class AnalyzerDAO {
    
    // Get student by roll number and section for current user
    public Student getStudentByRollAndSection(String rollNumber, String sectionName, int userId) {
        Student student = null;
        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT s.*, sec.section_name, sec.id as section_id FROM students s " +
                          "JOIN sections sec ON s.section_id = sec.id " +
                          "WHERE s.roll_number = ? " +
                          "AND sec.section_name = ? AND s.created_by = ?";
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, rollNumber);
            ps.setString(2, sectionName);
            ps.setInt(3, userId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int studentId = rs.getInt("id");
                int sectionId = rs.getInt("section_id");
                Map<String, Map<String, Integer>> marks = getStudentMarks(studentId, sectionId);
                student = new Student(
                    rs.getString("student_name"),
                    rs.getString("roll_number"),
                    marks
                );
                student.setId(studentId); // Set the student ID
                student.setSection(rs.getString("section_name"));
                
                // Debug output
                System.out.println("Student found: " + rs.getString("student_name"));
                System.out.println("Student ID: " + studentId);
                System.out.println("Section ID: " + sectionId);
                System.out.println("Marks map size: " + marks.size());
            }
            
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return student;
    }
    
    // Get student by name and roll number for current user
    public Student getStudentByNameAndRoll(String name, String rollNumber, int userId) {
        Student student = null;
        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT s.*, sec.section_name, sec.id as section_id FROM students s " +
                          "JOIN sections sec ON s.section_id = sec.id " +
                          "WHERE s.student_name = ? AND s.roll_number = ? " +
                          "AND s.created_by = ?";
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, name);
            ps.setString(2, rollNumber);
            ps.setInt(3, userId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                int studentId = rs.getInt("id");
                int sectionId = rs.getInt("section_id");
                Map<String, Map<String, Integer>> marks = getStudentMarks(studentId, sectionId);
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
                int studentId = rs.getInt("student_id");
                Map<String, Map<String, Integer>> marks = getStudentMarks(studentId, sectionId);
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
    
    // Get marks for a student - returns nested map: subject -> exam type -> marks
    // Only fetches subjects that are linked to the student's section
    private Map<String, Map<String, Integer>> getStudentMarks(int studentId, int sectionId) {
        Map<String, Map<String, Integer>> marks = new HashMap<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            // Query 1: Get marks from student_marks table (old system with text exam_type)
            String query1 = "SELECT sub.subject_name, sm.exam_type, sm.marks_obtained " +
                          "FROM student_marks sm " +
                          "JOIN subjects sub ON sm.subject_id = sub.id " +
                          "JOIN section_subjects ss ON ss.subject_id = sub.id AND ss.section_id = ? " +
                          "WHERE sm.student_id = ?";
            
            PreparedStatement ps1 = conn.prepareStatement(query1);
            ps1.setInt(1, sectionId);
            ps1.setInt(2, studentId);
            ResultSet rs1 = ps1.executeQuery();
            
            System.out.println("Fetching marks for student ID: " + studentId + ", Section ID: " + sectionId);
            
            while (rs1.next()) {
                String subjectName = rs1.getString("subject_name");
                String examType = rs1.getString("exam_type");
                int marksObtained = rs1.getInt("marks_obtained");
                
                marks.putIfAbsent(subjectName, new HashMap<>());
                marks.get(subjectName).put(examType, marksObtained);
                
                System.out.println("  [Old] Subject: " + subjectName + ", Exam: " + examType + ", Marks: " + marksObtained);
            }
            rs1.close();
            ps1.close();
            
            // Query 2: Get marks from marks table (new system with exam_type_id)
            String query2 = "SELECT sub.subject_name, et.exam_name, m.marks " +
                          "FROM marks m " +
                          "JOIN subjects sub ON m.subject_id = sub.id " +
                          "JOIN exam_types et ON m.exam_type_id = et.id " +
                          "JOIN section_subjects ss ON ss.subject_id = sub.id AND ss.section_id = ? " +
                          "WHERE m.student_id = ?";
            
            PreparedStatement ps2 = conn.prepareStatement(query2);
            ps2.setInt(1, sectionId);
            ps2.setInt(2, studentId);
            ResultSet rs2 = ps2.executeQuery();
            
            while (rs2.next()) {
                String subjectName = rs2.getString("subject_name");
                String examType = rs2.getString("exam_name");
                double marksObtained = rs2.getDouble("marks");
                
                marks.putIfAbsent(subjectName, new HashMap<>());
                // Only add if this exam doesn't already exist (avoid duplicates from old table)
                marks.get(subjectName).putIfAbsent(examType, (int) marksObtained);
                
                System.out.println("  [New] Subject: " + subjectName + ", Exam: " + examType + ", Marks: " + marksObtained);
            }
            rs2.close();
            ps2.close();
            
            System.out.println("Total subjects with marks: " + marks.size());
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return marks;
    }
    
    // Get section analysis data (without filters - calls filtered version with all selected)
    public SectionAnalysisData getSectionAnalysis(int sectionId, int userId) {
        return getSectionAnalysisWithFilters(sectionId, userId, null);
    }
    
    // Get section analysis data with component filters
    public SectionAnalysisData getSectionAnalysisWithFilters(int sectionId, int userId, Map<String, Set<String>> selectedFilters) {
        SectionAnalysisData data = new SectionAnalysisData();
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            // Build filter WHERE clause for exam_types
            StringBuilder filterClause = new StringBuilder();
            java.util.List<String> filterParams = new ArrayList<>();
            
            if (selectedFilters != null && !selectedFilters.isEmpty()) {
                filterClause.append(" AND (");
                boolean firstSubject = true;
                for (Map.Entry<String, Set<String>> entry : selectedFilters.entrySet()) {
                    // Skip subjects with no components (empty set)
                    if (entry.getValue() == null || entry.getValue().isEmpty()) {
                        continue;
                    }
                    
                    if (!firstSubject) filterClause.append(" OR ");
                    filterClause.append("(sub.subject_name = ? AND sm.exam_type IN (");
                    filterParams.add(entry.getKey());
                    
                    boolean firstComponent = true;
                    for (String component : entry.getValue()) {
                        if (!firstComponent) filterClause.append(", ");
                        filterClause.append("?");
                        filterParams.add(component);
                        firstComponent = false;
                    }
                    filterClause.append("))");
                    firstSubject = false;
                }
                filterClause.append(")");
            }
            
            // Get subject-wise analysis with FILTER support - Simplified approach
            String subjectQuery = 
                "SELECT " +
                "    sub.subject_name, " +
                "    (SELECT COUNT(DISTINCT id) FROM students WHERE section_id = ? AND created_by = ?) as total_students, " +
                "    AVG(sm.marks_obtained) as avg_marks " +
                "FROM section_subjects ss " +
                "INNER JOIN subjects sub ON ss.subject_id = sub.id " +
                "INNER JOIN student_marks sm ON sm.subject_id = sub.id " +
                "INNER JOIN students s ON sm.student_id = s.id AND s.section_id = ss.section_id AND s.created_by = ? " +
                "WHERE ss.section_id = ? " + filterClause.toString() +
                " GROUP BY sub.id, sub.subject_name";
            
            PreparedStatement ps = conn.prepareStatement(subjectQuery);
            int paramIndex = 1;
            ps.setInt(paramIndex++, sectionId);  // for total_students subquery
            ps.setInt(paramIndex++, userId);     // for total_students subquery
            ps.setInt(paramIndex++, userId);     // for students join
            ps.setInt(paramIndex++, sectionId);  // for WHERE clause
            // Apply filter params
            for (String param : filterParams) {
                ps.setString(paramIndex++, param);
            }
            
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                String subjectName = rs.getString("subject_name");
                // Only include if in filter or no filter
                if (selectedFilters == null || selectedFilters.isEmpty() || selectedFilters.containsKey(subjectName)) {
                    SubjectAnalysis sa = new SubjectAnalysis();
                    sa.subjectName = subjectName;
                    sa.totalStudents = rs.getInt("total_students");
                    sa.averageMarks = rs.getDouble("avg_marks");
                    if (Double.isNaN(sa.averageMarks)) {
                        sa.averageMarks = 0.0;
                    }
                    
                    // Now calculate pass/dc/fc/sc/fail for this subject with filters
                    calculateSubjectStats(conn, sectionId, userId, subjectName, selectedFilters, sa);
                    
                    data.subjectAnalysisList.add(sa);
                }
            }
            
            // Get top 5 students - this query should work fine
         // In AnalyzerDAO.java, update the topStudentsQuery:

            // Get top 5 students - with filters applied and percentage calculation
            // When filters are active, only sum marks for filtered subjects
            // When no filters, sum all marks
            String topStudentsQuery;
            if (selectedFilters != null && !selectedFilters.isEmpty() && filterParams.size() > 0) {
                // With filters: only consider subjects/components in the filter
                topStudentsQuery = 
                    "SELECT s.roll_number, s.student_name, " +
                    "SUM(sm.marks_obtained) as total_marks, " +
                    "SUM(ss.max_marks) as total_max_marks, " +
                    "(SUM(sm.marks_obtained) * 100.0 / SUM(ss.max_marks)) as percentage " +
                    "FROM students s " +
                    "INNER JOIN student_marks sm ON s.id = sm.student_id " +
                    "INNER JOIN subjects sub ON sm.subject_id = sub.id " +
                    "INNER JOIN section_subjects ss ON sub.id = ss.subject_id AND s.section_id = ss.section_id " +
                    "WHERE s.section_id = ? AND s.created_by = ? " +
                    filterClause.toString() +
                    "GROUP BY s.id, s.roll_number, s.student_name " +
                    "ORDER BY percentage DESC " +
                    "LIMIT 5";
            } else {
                // No filters: consider ALL subjects from section_subjects
                topStudentsQuery = 
                    "SELECT s.roll_number, s.student_name, " +
                    "COALESCE(SUM(sm.marks_obtained), 0) as total_marks, " +
                    "SUM(ss.max_marks) as total_max_marks, " +
                    "(COALESCE(SUM(sm.marks_obtained), 0) * 100.0 / SUM(ss.max_marks)) as percentage " +
                    "FROM students s " +
                    "CROSS JOIN section_subjects ss " +
                    "INNER JOIN subjects sub ON ss.subject_id = sub.id " +
                    "LEFT JOIN student_marks sm ON s.id = sm.student_id AND sm.subject_id = sub.id " +
                    "WHERE s.section_id = ? AND s.created_by = ? AND ss.section_id = ? " +
                    "GROUP BY s.id, s.roll_number, s.student_name " +
                    "ORDER BY percentage DESC " +
                    "LIMIT 5";
            }
            
            ps = conn.prepareStatement(topStudentsQuery);
            paramIndex = 1;
            
            if (selectedFilters != null && !selectedFilters.isEmpty() && filterParams.size() > 0) {
                // With filters: section, user, then filter params
                ps.setInt(paramIndex++, sectionId);
                ps.setInt(paramIndex++, userId);
                for (String param : filterParams) {
                    ps.setString(paramIndex++, param);
                }
            } else {
                // No filters: section, user, section again
                ps.setInt(paramIndex++, sectionId);
                ps.setInt(paramIndex++, userId);
                ps.setInt(paramIndex++, sectionId);
            }
            
            rs = ps.executeQuery();
            
            while (rs.next()) {
                TopStudent ts = new TopStudent();
                ts.rollNumber = rs.getString("roll_number");
                ts.name = rs.getString("student_name");
                ts.totalMarks = rs.getInt("total_marks");
                double pct = rs.getDouble("percentage");
                ts.percentage = Double.isNaN(pct) ? 0.0 : pct;
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
            
            // Calculate pass/fail students based on overall percentage (>= 50%) with filters
            String passFailQuery;
            if (selectedFilters != null && !selectedFilters.isEmpty() && filterParams.size() > 0) {
                // With filters: only consider filtered subjects
                passFailQuery = 
                    "SELECT " +
                    "    COUNT(CASE WHEN percentage >= 50 THEN 1 END) as pass_count, " +
                    "    COUNT(CASE WHEN percentage < 50 OR percentage IS NULL THEN 1 END) as fail_count " +
                    "FROM ( " +
                    "    SELECT s.id, " +
                    "    (SUM(sm.marks_obtained) * 100.0 / SUM(ss.max_marks)) as percentage " +
                    "    FROM students s " +
                    "    INNER JOIN student_marks sm ON s.id = sm.student_id " +
                    "    INNER JOIN subjects sub ON sm.subject_id = sub.id " +
                    "    INNER JOIN section_subjects ss ON sub.id = ss.subject_id AND s.section_id = ss.section_id " +
                    "    WHERE s.section_id = ? AND s.created_by = ? " +
                    filterClause.toString() +
                    "    GROUP BY s.id " +
                    ") as student_percentages";
            } else {
                // No filters: consider ALL subjects from section_subjects
                passFailQuery = 
                    "SELECT " +
                    "    COUNT(CASE WHEN percentage >= 50 THEN 1 END) as pass_count, " +
                    "    COUNT(CASE WHEN percentage < 50 OR percentage IS NULL THEN 1 END) as fail_count " +
                    "FROM ( " +
                    "    SELECT s.id, " +
                    "    (COALESCE(SUM(sm.marks_obtained), 0) * 100.0 / SUM(ss.max_marks)) as percentage " +
                    "    FROM students s " +
                    "    CROSS JOIN section_subjects ss " +
                    "    INNER JOIN subjects sub ON ss.subject_id = sub.id " +
                    "    LEFT JOIN student_marks sm ON s.id = sm.student_id AND sm.subject_id = sub.id " +
                    "    WHERE s.section_id = ? AND s.created_by = ? AND ss.section_id = ? " +
                    "    GROUP BY s.id " +
                    ") as student_percentages";
            }
            
            ps = conn.prepareStatement(passFailQuery);
            paramIndex = 1;
            
            if (selectedFilters != null && !selectedFilters.isEmpty() && filterParams.size() > 0) {
                // With filters: section, user, then filter params
                ps.setInt(paramIndex++, sectionId);
                ps.setInt(paramIndex++, userId);
                for (String param : filterParams) {
                    ps.setString(paramIndex++, param);
                }
            } else {
                // No filters: section, user, section again
                ps.setInt(paramIndex++, sectionId);
                ps.setInt(paramIndex++, userId);
                ps.setInt(paramIndex++, sectionId);
            }
            
            rs = ps.executeQuery();
            
            if (rs.next()) {
                data.passStudents = rs.getInt("pass_count");
                data.failStudents = rs.getInt("fail_count");
            }
            
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
    
    // Helper method to calculate subject-specific statistics with filters
    private void calculateSubjectStats(Connection conn, int sectionId, int userId, String subjectName, 
                                       Map<String, Set<String>> selectedFilters, SubjectAnalysis sa) throws SQLException {
        // Build filter clause for this specific subject
        StringBuilder filterClause = new StringBuilder();
        List<String> filterParams = new ArrayList<>();
        
        if (selectedFilters != null && !selectedFilters.isEmpty() && selectedFilters.containsKey(subjectName)) {
            Set<String> components = selectedFilters.get(subjectName);
            if (components != null && !components.isEmpty()) {
                filterClause.append(" AND sm.exam_type IN (");
                boolean first = true;
                for (String component : components) {
                    if (!first) filterClause.append(", ");
                    filterClause.append("?");
                    filterParams.add(component);
                    first = false;
                }
                filterClause.append(")");
            }
        }
        
        // Query to get per-student statistics for this subject
        String statsQuery = 
            "SELECT " +
            "    s.id, " +
            "    SUM(sm.marks_obtained) as total_marks, " +
            "    ss.max_marks as total_max, " +
            "    (SUM(sm.marks_obtained) * 100.0 / ss.max_marks) as percentage " +
            "FROM students s " +
            "INNER JOIN section_subjects ss ON s.section_id = ss.section_id " +
            "INNER JOIN subjects sub ON ss.subject_id = sub.id " +
            "LEFT JOIN student_marks sm ON s.id = sm.student_id AND sm.subject_id = sub.id " +
            "WHERE s.section_id = ? AND s.created_by = ? AND sub.subject_name = ?" +
            filterClause.toString() + " " +
            "GROUP BY s.id, ss.max_marks";
        
        PreparedStatement ps = conn.prepareStatement(statsQuery);
        int paramIndex = 1;
        ps.setInt(paramIndex++, sectionId);
        ps.setInt(paramIndex++, userId);
        ps.setString(paramIndex++, subjectName);
        
        // Add filter parameters
        for (String param : filterParams) {
            ps.setString(paramIndex++, param);
        }
        
        ResultSet rs = ps.executeQuery();
        
        // Initialize counters
        sa.passCount = 0;
        sa.distinctionCount = 0;
        sa.firstClassCount = 0;
        sa.secondClassCount = 0;
        sa.failCount = 0;
        
        // Categorize each student
        while (rs.next()) {
            double percentage = rs.getDouble("percentage");
            
            if (percentage >= 75) {
                sa.distinctionCount++;
                sa.passCount++;
            } else if (percentage >= 60) {
                sa.firstClassCount++;
                sa.passCount++;
            } else if (percentage >= 50) {
                sa.secondClassCount++;
                sa.passCount++;
            } else {
                sa.failCount++;
            }
        }
        
        rs.close();
        ps.close();
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
    
    // Get maximum marks for a subject from section_subjects table
    public int getMaxMarksForSubject(int studentId, String subjectName) {
        int maxMarks = 0;
        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT ss.max_marks " +
                          "FROM section_subjects ss " +
                          "JOIN subjects sub ON ss.subject_id = sub.id " +
                          "JOIN students s ON s.section_id = ss.section_id " +
                          "WHERE s.id = ? AND sub.subject_name = ?";
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, studentId);
            ps.setString(2, subjectName);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                maxMarks = rs.getInt("max_marks");
            }
            
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return maxMarks;
    }
    
    // Get credit for a subject from section_subjects table
    public int getCreditForSubject(int studentId, String subjectName) {
        int credit = 0;
        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT ss.credit " +
                          "FROM section_subjects ss " +
                          "JOIN subjects sub ON ss.subject_id = sub.id " +
                          "JOIN students s ON s.section_id = ss.section_id " +
                          "WHERE s.id = ? AND sub.subject_name = ?";
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, studentId);
            ps.setString(2, subjectName);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                credit = rs.getInt("credit");
            }
            
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return credit;
    }
    
    // Class to hold subject configuration info
    public static class SubjectConfig {
        public int maxMarks;
        public int credit;
        public SubjectConfig(int maxMarks, int credit) {
            this.maxMarks = maxMarks;
            this.credit = credit;
        }
    }
    
    public SubjectConfig getSubjectInfo(int studentId, String subjectName) {
        SubjectConfig info = new SubjectConfig(0, 0);
        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT ss.max_marks, ss.credit " +
                          "FROM section_subjects ss " +
                          "JOIN subjects sub ON ss.subject_id = sub.id " +
                          "JOIN students s ON s.section_id = ss.section_id " +
                          "WHERE s.id = ? AND sub.subject_name = ?";
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, studentId);
            ps.setString(2, subjectName);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                info.maxMarks = rs.getInt("max_marks");
                info.credit = rs.getInt("credit");
            }
            
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return info;
    }
    
    // Grade Distribution Data
    public static class GradeDistribution {
        public String grade;
        public int count;
        
        public GradeDistribution(String grade, int count) {
            this.grade = grade;
            this.count = count;
        }
    }
    
    public List<GradeDistribution> getGradeDistribution(int sectionId, Map<String, Set<String>> selectedFilters) {
        List<GradeDistribution> distribution = new ArrayList<>();
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            // Build filter clause
            List<String> filterParams = new ArrayList<>();
            StringBuilder filterClause = new StringBuilder();
            
            if (selectedFilters != null && !selectedFilters.isEmpty()) {
                List<String> subjectFilters = new ArrayList<>();
                
                for (Map.Entry<String, Set<String>> entry : selectedFilters.entrySet()) {
                    if (entry.getValue() == null || entry.getValue().isEmpty()) {
                        continue;
                    }
                    
                    String subjectName = entry.getKey();
                    Set<String> components = entry.getValue();
                    
                    String componentList = String.join(",", Collections.nCopies(components.size(), "?"));
                    subjectFilters.add("(sub.subject_name = ? AND sm.exam_type IN (" + componentList + "))");
                    
                    filterParams.add(subjectName);
                    filterParams.addAll(components);
                }
                
                if (!subjectFilters.isEmpty()) {
                    filterClause.append(" AND (").append(String.join(" OR ", subjectFilters)).append(")");
                }
            }
            
            String query;
            
            if (selectedFilters != null && !selectedFilters.isEmpty() && filterParams.size() > 0) {
                // With filters: INNER JOIN
                query = "SELECT " +
                       "    s.id AS student_id, " +
                       "    s.student_name AS student_name, " +
                       "    SUM(sm.marks_obtained) AS total_marks, " +
                       "    SUM(ss.max_marks) AS total_max_marks " +
                       "FROM students s " +
                       "INNER JOIN student_marks sm ON sm.student_id = s.id " +
                       "INNER JOIN subjects sub ON sm.subject_id = sub.id " +
                       "INNER JOIN section_subjects ss ON ss.subject_id = sub.id AND ss.section_id = s.section_id " +
                       "WHERE s.section_id = ? " + filterClause.toString() +
                       " GROUP BY s.id, s.student_name " +
                       "ORDER BY total_marks DESC";
            } else {
                // No filters: CROSS JOIN + LEFT JOIN
                query = "SELECT " +
                       "    s.id AS student_id, " +
                       "    s.student_name AS student_name, " +
                       "    COALESCE(SUM(sm.marks_obtained), 0) AS total_marks, " +
                       "    SUM(ss.max_marks) AS total_max_marks " +
                       "FROM students s " +
                       "CROSS JOIN section_subjects ss ON ss.section_id = s.section_id " +
                       "LEFT JOIN student_marks sm ON sm.student_id = s.id AND sm.subject_id = ss.subject_id " +
                       "WHERE s.section_id = ? " +
                       "GROUP BY s.id, s.student_name " +
                       "ORDER BY total_marks DESC";
            }
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, sectionId);
            
            int paramIndex = 2;
            for (String param : filterParams) {
                ps.setString(paramIndex++, param);
            }
            
            ResultSet rs = ps.executeQuery();
            
            // Count students by grade
            Map<String, Integer> gradeCounts = new LinkedHashMap<>();
            gradeCounts.put("A+", 0);
            gradeCounts.put("A", 0);
            gradeCounts.put("B+", 0);
            gradeCounts.put("B", 0);
            gradeCounts.put("C", 0);
            gradeCounts.put("D", 0);
            gradeCounts.put("F", 0);
            
            while (rs.next()) {
                double totalMarks = rs.getDouble("total_marks");
                double totalMaxMarks = rs.getDouble("total_max_marks");
                double percentage = totalMaxMarks > 0 ? (totalMarks * 100.0 / totalMaxMarks) : 0;
                
                String grade = getGradeFromPercentage(percentage);
                gradeCounts.put(grade, gradeCounts.get(grade) + 1);
            }
            
            // Convert to list
            for (Map.Entry<String, Integer> entry : gradeCounts.entrySet()) {
                distribution.add(new GradeDistribution(entry.getKey(), entry.getValue()));
            }
            
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return distribution;
    }
    
    private String getGradeFromPercentage(double percentage) {
        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B+";
        if (percentage >= 60) return "B";
        if (percentage >= 50) return "C";
        if (percentage >= 40) return "D";
        return "F";
    }
    
    // At-Risk Students Data
    public static class AtRiskStudent {
        public String rollNo;
        public String name;
        public double percentage;
        public String riskLevel;
        public String failedSubjects;
        
        public AtRiskStudent(String rollNo, String name, double percentage, String riskLevel, String failedSubjects) {
            this.rollNo = rollNo;
            this.name = name;
            this.percentage = percentage;
            this.riskLevel = riskLevel;
            this.failedSubjects = failedSubjects;
        }
    }
    
    public List<AtRiskStudent> getAtRiskStudents(int sectionId, Map<String, Set<String>> selectedFilters) {
        List<AtRiskStudent> atRiskStudents = new ArrayList<>();
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            // Build filter clause
            List<String> filterParams = new ArrayList<>();
            StringBuilder filterClause = new StringBuilder();
            
            if (selectedFilters != null && !selectedFilters.isEmpty()) {
                List<String> subjectFilters = new ArrayList<>();
                
                for (Map.Entry<String, Set<String>> entry : selectedFilters.entrySet()) {
                    if (entry.getValue() == null || entry.getValue().isEmpty()) {
                        continue;
                    }
                    
                    String subjectName = entry.getKey();
                    Set<String> components = entry.getValue();
                    
                    String componentList = String.join(",", Collections.nCopies(components.size(), "?"));
                    subjectFilters.add("(sub.subject_name = ? AND sm.exam_type IN (" + componentList + "))");
                    
                    filterParams.add(subjectName);
                    filterParams.addAll(components);
                }
                
                if (!subjectFilters.isEmpty()) {
                    filterClause.append(" AND (").append(String.join(" OR ", subjectFilters)).append(")");
                }
            }
            
            String query;
            
            if (selectedFilters != null && !selectedFilters.isEmpty() && filterParams.size() > 0) {
                // With filters: INNER JOIN
                query = "SELECT " +
                       "    s.roll_number, " +
                       "    s.student_name AS student_name, " +
                       "    SUM(sm.marks_obtained) AS total_marks, " +
                       "    SUM(ss.max_marks) AS total_max_marks " +
                       "FROM students s " +
                       "INNER JOIN student_marks sm ON sm.student_id = s.id " +
                       "INNER JOIN subjects sub ON sm.subject_id = sub.id " +
                       "INNER JOIN section_subjects ss ON ss.subject_id = sub.id AND ss.section_id = s.section_id " +
                       "WHERE s.section_id = ? " + filterClause.toString() +
                       " GROUP BY s.id, s.roll_number, s.student_name " +
                       "HAVING (SUM(sm.marks_obtained) * 100.0 / SUM(ss.max_marks)) < 60 " +
                       "ORDER BY (SUM(sm.marks_obtained) * 100.0 / SUM(ss.max_marks)) ASC";
            } else {
                // No filters: CROSS JOIN + LEFT JOIN
                query = "SELECT " +
                       "    s.roll_number, " +
                       "    s.student_name AS student_name, " +
                       "    COALESCE(SUM(sm.marks_obtained), 0) AS total_marks, " +
                       "    SUM(ss.max_marks) AS total_max_marks " +
                       "FROM students s " +
                       "CROSS JOIN section_subjects ss ON ss.section_id = s.section_id " +
                       "LEFT JOIN student_marks sm ON sm.student_id = s.id AND sm.subject_id = ss.subject_id " +
                       "WHERE s.section_id = ? " +
                       "GROUP BY s.id, s.roll_number, s.student_name " +
                       "HAVING (COALESCE(SUM(sm.marks_obtained), 0) * 100.0 / SUM(ss.max_marks)) < 60 " +
                       "ORDER BY (COALESCE(SUM(sm.marks_obtained), 0) * 100.0 / SUM(ss.max_marks)) ASC";
            }
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, sectionId);
            
            int paramIndex = 2;
            for (String param : filterParams) {
                ps.setString(paramIndex++, param);
            }
            
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                String rollNo = rs.getString("roll_number");
                String name = rs.getString("student_name");
                double totalMarks = rs.getDouble("total_marks");
                double totalMaxMarks = rs.getDouble("total_max_marks");
                double percentage = totalMaxMarks > 0 ? (totalMarks * 100.0 / totalMaxMarks) : 0;
                
                String riskLevel;
                if (percentage < 50) {
                    riskLevel = "Critical";
                } else {
                    riskLevel = "Borderline";
                }
                
                // Get failed subjects for this student
                String failedSubjects = getFailedSubjectsForStudent(sectionId, rollNo, selectedFilters);
                
                atRiskStudents.add(new AtRiskStudent(rollNo, name, percentage, riskLevel, failedSubjects));
            }
            
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return atRiskStudents;
    }
    
    private String getFailedSubjectsForStudent(int sectionId, String rollNo, Map<String, Set<String>> selectedFilters) {
        List<String> failedSubjects = new ArrayList<>();
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            // Build filter clause
            List<String> filterParams = new ArrayList<>();
            StringBuilder filterClause = new StringBuilder();
            
            if (selectedFilters != null && !selectedFilters.isEmpty()) {
                List<String> subjectFilters = new ArrayList<>();
                
                for (Map.Entry<String, Set<String>> entry : selectedFilters.entrySet()) {
                    if (entry.getValue() == null || entry.getValue().isEmpty()) {
                        continue;
                    }
                    
                    String subjectName = entry.getKey();
                    Set<String> components = entry.getValue();
                    
                    String componentList = String.join(",", Collections.nCopies(components.size(), "?"));
                    subjectFilters.add("(sub.subject_name = ? AND sm.exam_type IN (" + componentList + "))");
                    
                    filterParams.add(subjectName);
                    filterParams.addAll(components);
                }
                
                if (!subjectFilters.isEmpty()) {
                    filterClause.append(" AND (").append(String.join(" OR ", subjectFilters)).append(")");
                }
            }
            
            String query;
            
            if (selectedFilters != null && !selectedFilters.isEmpty() && filterParams.size() > 0) {
                // With filters: INNER JOIN
                query = "SELECT " +
                       "    sub.subject_name, " +
                       "    SUM(sm.marks_obtained) AS subject_marks, " +
                       "    ss.max_marks AS subject_max_marks " +
                       "FROM students s " +
                       "INNER JOIN student_marks sm ON sm.student_id = s.id " +
                       "INNER JOIN subjects sub ON sm.subject_id = sub.id " +
                       "INNER JOIN section_subjects ss ON ss.subject_id = sub.id AND ss.section_id = s.section_id " +
                       "WHERE s.section_id = ? AND s.roll_number = ? " + filterClause.toString() +
                       " GROUP BY sub.subject_name, ss.max_marks " +
                       "HAVING (SUM(sm.marks_obtained) * 100.0 / ss.max_marks) < 50";
            } else {
                // No filters: CROSS JOIN + LEFT JOIN
                query = "SELECT " +
                       "    sub.subject_name, " +
                       "    COALESCE(SUM(sm.marks_obtained), 0) AS subject_marks, " +
                       "    ss.max_marks AS subject_max_marks " +
                       "FROM students s " +
                       "CROSS JOIN section_subjects ss ON ss.section_id = s.section_id " +
                       "JOIN subjects sub ON ss.subject_id = sub.id " +
                       "LEFT JOIN student_marks sm ON sm.student_id = s.id AND sm.subject_id = ss.subject_id " +
                       "WHERE s.section_id = ? AND s.roll_number = ? " +
                       "GROUP BY sub.subject_name, ss.max_marks " +
                       "HAVING (COALESCE(SUM(sm.marks_obtained), 0) * 100.0 / ss.max_marks) < 50";
            }
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, sectionId);
            ps.setString(2, rollNo);
            
            int paramIndex = 3;
            for (String param : filterParams) {
                ps.setString(paramIndex++, param);
            }
            
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                failedSubjects.add(rs.getString("subject_name"));
            }
            
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return failedSubjects.isEmpty() ? "None" : String.join(", ", failedSubjects);
    }
}