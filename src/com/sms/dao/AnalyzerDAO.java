package com.sms.dao;

import java.sql.*;
import java.util.*;

import javax.swing.JOptionPane;

import com.sms.database.DatabaseConnection;
import com.sms.analyzer.Student;

/**
 * Data Access Object for Student and Section Analysis
 * 
 * WEIGHTED MARKS CALCULATION SYSTEM (v2.0 - Current):
 * ====================================================
 * This DAO implements a weighted contribution system for calculating subject totals
 * with DUAL PASSING REQUIREMENTS (component-level AND subject-level).
 * 
 * KEY CONCEPTS:
 * 1. Weightage % = Max marks for component (20% weightage = enter 0-20 marks)
 * 2. Marks entered DIRECTLY out of weightage (NO scaling/conversion)
 * 3. Subject Total = ??(all component marks obtained) out of 100
 * 4. All component weightages MUST sum to 100%
 * 
 * DUAL PASSING REQUIREMENT:
 * - Component Pass: Each component score >= component passing marks
 * - Subject Pass: Total score >= subject passing marks (typically 40)
 * - Failure Condition: Failing ANY component = SUBJECT FAIL (even if total passes)
 * 
 * REALISTIC EXAMPLE (CLOUD COMPUTING):
 * - Internal 1: 20%, Pass 8  ??? Student enters 0-20, must score >= 8
 * - Internal 2: 25%, Pass 10 ??? Student enters 0-25, must score >= 10
 * - Internal 3: 15%, Pass 6  ??? Student enters 0-15, must score >= 6
 * - Final Exam: 40%, Pass 16 ??? Student enters 0-40, must score >= 16
 * - Subject Total = Int1 + Int2 + Int3 + Final (out of 100, need >= 40)
 * 
 * Pass Scenario: 18 + 22 + 13 + 35 = 88/100 ??? (all components passed)
 * Fail Scenario: 5 + 24 + 14 + 38 = 81/100 ??? (Int1 failed: 5 < 8)
 * 
 * See WEIGHTED_CALCULATION_SYSTEM.md for complete documentation.
 */
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
                student.setId(studentId); // Set the student ID
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
    // UPDATED: Uses entered_exam_marks table with exam_type_id FK
    private Map<String, Map<String, Integer>> getStudentMarks(int studentId, int sectionId) {
        Map<String, Map<String, Integer>> marks = new HashMap<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            // Simplified query: Just get marks from entered_exam_marks table
            // entered_exam_marks already has the correct subject_id and exam_type_id
            String query = "SELECT sub.subject_name, et.exam_name, sm.marks_obtained " +
                          "FROM entered_exam_marks sm " +
                          "JOIN subjects sub ON sm.subject_id = sub.id " +
                          "JOIN exam_types et ON sm.exam_type_id = et.id " +
                          "WHERE sm.student_id = ? " +
                          "ORDER BY sub.subject_name, et.exam_name";
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
                String subjectName = rs.getString("subject_name");
                String examName = rs.getString("exam_name");
                int marksObtained = rs.getInt("marks_obtained");
                
                marks.putIfAbsent(subjectName, new HashMap<>());
                marks.get(subjectName).put(examName, marksObtained);
                
            }
            
            if (rowCount == 0) {
                // No marks found for this student

            } else {
            }
            rs.close();
            ps.close();
            
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return marks;
    }
    
    /**
     * Calculate weighted total marks for a subject
     * Formula: Sum of ((obtained_marks / max_marks) ?? weightage) for all components
     * @param studentId Student ID
     * @param subjectId Subject ID
     * @param sectionId Section ID
     * @return Weighted total (out of 100) or -1 if no data
     * @deprecated Use calculateWeightedSubjectTotalWithPass() for DUAL PASSING
     */
    @Deprecated
    public double calculateWeightedSubjectTotal(int studentId, int subjectId, int sectionId) {
        // OLD METHOD - NOT USED ANYMORE
        // Use calculateWeightedSubjectTotalWithPass() instead
        return -1;
    }
    
    // Get section analysis data (without filters - calls filtered version with all selected)
    public SectionAnalysisData getSectionAnalysis(int sectionId, int userId) {
        return getSectionAnalysisWithFilters(sectionId, userId, null);
    }
    
    /**
     * EFFICIENT BATCH METHOD: Calculate weighted percentages for ALL students at once
     * This method eliminates the N+1 query problem by fetching all data in bulk
     * @return Map of studentId -> weighted percentage
     */
    private Map<Integer, Double> calculateAllStudentPercentagesBatch(int sectionId, int userId, Map<String, Set<String>> selectedFilters) {
        Map<Integer, Double> studentPercentages = new HashMap<>();
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            // Step 1: Get all students
            String studentsQuery = "SELECT id FROM students WHERE section_id = ? AND created_by = ?";
            PreparedStatement psStudents = conn.prepareStatement(studentsQuery);
            psStudents.setInt(1, sectionId);
            psStudents.setInt(2, userId);
            ResultSet rsStudents = psStudents.executeQuery();
            
            List<Integer> studentIds = new ArrayList<>();
            while (rsStudents.next()) {
                studentIds.add(rsStudents.getInt("id"));
            }
            rsStudents.close();
            psStudents.close();
            
            if (studentIds.isEmpty()) {
                return studentPercentages;
            }
            
            // Step 2: Get all subjects for this section
            String subjectsQuery = "SELECT sub.id, sub.subject_name FROM subjects sub " +
                                  "JOIN section_subjects ss ON sub.id = ss.subject_id " +
                                  "WHERE ss.section_id = ?";
            PreparedStatement psSubjects = conn.prepareStatement(subjectsQuery);
            psSubjects.setInt(1, sectionId);
            ResultSet rsSubjects = psSubjects.executeQuery();
            
            List<SubjectInfo> subjects = new ArrayList<>();
            while (rsSubjects.next()) {
                int subjectId = rsSubjects.getInt("id");
                String subjectName = rsSubjects.getString("subject_name");
                
                // Apply filter
                if (selectedFilters != null && !selectedFilters.isEmpty() && !selectedFilters.containsKey(subjectName)) {
                    continue;
                }
                
                subjects.add(new SubjectInfo(subjectId, subjectName));
            }
            rsSubjects.close();
            psSubjects.close();
            
            if (subjects.isEmpty()) {
                return studentPercentages;
            }
            
            // Step 3: For each subject, get exam type configurations and marks for ALL students
            Map<Integer, Map<Integer, Double>> studentSubjectWeighted = new HashMap<>(); // studentId -> subjectId -> weighted total
            
            for (SubjectInfo subject : subjects) {
                // Get exam types with weights
                List<ExamTypeConfig> examTypes = getExamTypesForSubject(sectionId, subject.id);
                if (examTypes.isEmpty()) {
                    continue;
                }
                
                // Filter exam types if needed
                Set<String> examFilter = (selectedFilters != null) ? selectedFilters.get(subject.name) : null;
                
                // Get marks for ALL students for this subject in ONE query
                StringBuilder marksQuery = new StringBuilder(
                    "SELECT sm.student_id, et.exam_name, sm.marks_obtained " +
                    "FROM entered_exam_marks sm " +
                    "JOIN exam_types et ON sm.exam_type_id = et.id " +
                    "WHERE sm.subject_id = ? AND sm.student_id IN ("
                );
                for (int i = 0; i < studentIds.size(); i++) {
                    if (i > 0) marksQuery.append(",");
                    marksQuery.append("?");
                }
                marksQuery.append(")");
                
                PreparedStatement psMarks = conn.prepareStatement(marksQuery.toString());
                psMarks.setInt(1, subject.id);
                for (int i = 0; i < studentIds.size(); i++) {
                    psMarks.setInt(i + 2, studentIds.get(i));
                }
                ResultSet rsMarks = psMarks.executeQuery();
                
                // Build marks map per student
                Map<Integer, Map<String, Integer>> studentMarksMap = new HashMap<>();
                while (rsMarks.next()) {
                    int studentId = rsMarks.getInt("student_id");
                    String examName = rsMarks.getString("exam_name");
                    int marks = rsMarks.getInt("marks_obtained");
                    
                    studentMarksMap.putIfAbsent(studentId, new HashMap<>());
                    studentMarksMap.get(studentId).put(examName, marks);
                }
                rsMarks.close();
                psMarks.close();
                
                // Calculate weighted total for each student for this subject WITH DUAL PASSING CHECK
                for (int studentId : studentIds) {
                    Map<String, Integer> marksMap = studentMarksMap.getOrDefault(studentId, new HashMap<>());
                    
                    // DEBUG for students 166, 170, 171 in GEN AI
                    boolean debugThis = (studentId == 166 || studentId == 170 || studentId == 171) && subject.name.equals("GEN AI");
                    if (debugThis) {
                    }
                    
                    double weightedTotal = 0.0;
                    boolean allComponentsPassed = true;
                    int componentsChecked = 0;
                    
                    for (ExamTypeConfig examType : examTypes) {
                        // Skip if filter active and exam not selected
                        if (examFilter != null && !examFilter.contains(examType.examName)) {
                            if (debugThis) {
                            }
                            continue;
                        }
                        
                        Integer marksObtained = marksMap.get(examType.examName);
                        if (marksObtained != null && examType.maxMarks > 0) {
                            // DUAL PASSING CHECK: Component must pass its passing marks
                            if (marksObtained < examType.passingMarks) {
                                allComponentsPassed = false;
                                if (debugThis) {
                                }
                            } else {
                                if (debugThis) {
                                }
                            }
                            
                            double contribution = (marksObtained.doubleValue() / examType.maxMarks) * examType.weightage;
                            weightedTotal += contribution;
                            componentsChecked++;
                            
                            if (debugThis) {
                            }
                        } else {
                            if (debugThis) {
                            }
                        }
                    }
                    
                    if (debugThis) {
                    }
                    
                    // Only store if we checked at least one component
                    // Mark as -1 if component failed (will be treated as fail later)
                    if (componentsChecked > 0) {
                        // If component failed OR total < 50, mark as fail (negative to indicate failure)
                        if (!allComponentsPassed || weightedTotal < 50) {
                            weightedTotal = -Math.abs(weightedTotal); // Negative indicates FAIL
                        }
                        studentSubjectWeighted.putIfAbsent(studentId, new HashMap<>());
                        studentSubjectWeighted.get(studentId).put(subject.id, weightedTotal);
                        
                        // DEBUG for students 166, 170, 171
                        if (studentId == 166 || studentId == 170 || studentId == 171) {
                            String status = weightedTotal < 0 ? "FAILED" : "PASSED";
                        }
                    } else {
                        // DEBUG for students 166, 170, 171
                        if (studentId == 166 || studentId == 170 || studentId == 171) {
                        }
                    }
                }
            }
            
            // Step 4: Calculate average percentage per student with DUAL PASSING
            for (int studentId : studentIds) {
                Map<Integer, Double> subjectTotals = studentSubjectWeighted.get(studentId);
                if (subjectTotals == null || subjectTotals.isEmpty()) {
                    studentPercentages.put(studentId, 0.0);
                    continue;
                }
                
                // DEBUG FOR PROBLEM STUDENTS 166, 170, 171
                if (studentId == 166 || studentId == 170 || studentId == 171) {

                    for (Map.Entry<Integer, Double> entry : subjectTotals.entrySet()) {
                        String status = entry.getValue() < 0 ? "FAILED" : "PASSED";
                    }
                }
                
                double totalWeighted = 0.0;
                int subjectCount = 0;
                int failedSubjectCount = 0;
                
                for (Double weighted : subjectTotals.values()) {
                    if (weighted < 0) {
                        // Subject failed (negative value)
                        failedSubjectCount++;
                        totalWeighted += Math.abs(weighted); // Use absolute for average calculation
                    } else {
                        totalWeighted += weighted;
                    }
                    subjectCount++;
                }
                
                double percentage = subjectCount > 0 ? (totalWeighted / subjectCount) : 0.0;
                
                // DEBUG FOR PROBLEM STUDENTS 166, 170, 171
                if (studentId == 166 || studentId == 170 || studentId == 171) {
                }
                
                // If ANY subject failed, student fails overall (mark with negative)
                if (failedSubjectCount > 0) {
                    percentage = -Math.abs(percentage);
                }
                
                // DEBUG FOR PROBLEM STUDENTS 166, 170, 171
                if (studentId == 166 || studentId == 170 || studentId == 171) {
                }
                
                studentPercentages.put(studentId, percentage);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return studentPercentages;
    }
    
    // Helper class for student basic info
    private static class StudentBasicInfo {
        String rollNumber;
        String name;
        StudentBasicInfo(String rollNumber, String name) {
            this.rollNumber = rollNumber;
            this.name = name;
        }
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
                    filterClause.append("(sub.subject_name = ? AND et.exam_name IN (");
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
            
            // Get subject-wise analysis with FILTER support - Use LEFT JOIN to include subjects without marks
            String subjectQuery = 
                "SELECT " +
                "    sub.subject_name, " +
                "    (SELECT COUNT(DISTINCT id) FROM students WHERE section_id = ? AND created_by = ?) as total_students, " +
                "    AVG(sm.marks_obtained) as avg_marks " +
                "FROM section_subjects ss " +
                "INNER JOIN subjects sub ON ss.subject_id = sub.id " +
                "LEFT JOIN entered_exam_marks sm ON sm.subject_id = sub.id " +
                "LEFT JOIN exam_types et ON sm.exam_type_id = et.id " +
                "LEFT JOIN students s ON sm.student_id = s.id AND s.section_id = ss.section_id AND s.created_by = ? " +
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

            // Get top 5 students using WEIGHTED CALCULATION (not raw sums)
            // Get all students first, then calculate weighted totals for each
            String studentsQuery = "SELECT id, roll_number, student_name FROM students WHERE section_id = ? AND created_by = ?";
            ps = conn.prepareStatement(studentsQuery);
            ps.setInt(1, sectionId);
            ps.setInt(2, userId);
            rs = ps.executeQuery();
            
            // List to hold all students with their weighted totals
            List<TopStudent> allStudents = new ArrayList<>();
            
            while (rs.next()) {
                int studentId = rs.getInt("id");
                String rollNumber = rs.getString("roll_number");
                String studentName = rs.getString("student_name");
                
                // Calculate weighted total and percentage for this student
                double weightedTotal = getStudentWeightedTotal(studentId, sectionId, selectedFilters);
                double percentage = getStudentWeightedPercentage(studentId, sectionId, selectedFilters);
                
                if (weightedTotal > 0) {
                    TopStudent ts = new TopStudent();
                    ts.rollNumber = rollNumber;
                    ts.name = studentName;
                    ts.totalMarks = (int) Math.round(weightedTotal);
                    ts.percentage = percentage;
                    allStudents.add(ts);
                }
            }
            rs.close();
            ps.close();
            
            // Sort by percentage descending and take top 5
            allStudents.sort((a, b) -> Double.compare(b.percentage, a.percentage));
            data.topStudents = allStudents.stream().limit(5).collect(java.util.stream.Collectors.toList());
            
            // ===== CALCULATE FAILED SUBJECTS COUNT WITH DUAL PASSING =====
            // We must use the same logic as calculateWeightedSubjectTotalWithPass()
            // NOT the old SQL query that only checked single marks
            
            
            // Get all students for failed subjects count
            String studentsQueryForFailed = "SELECT id FROM students WHERE section_id = ? AND created_by = ?";
            ps = conn.prepareStatement(studentsQueryForFailed);
            ps.setInt(1, sectionId);
            ps.setInt(2, userId);
            rs = ps.executeQuery();
            
            List<Integer> studentIdsForFailed = new ArrayList<>();
            while (rs.next()) {
                studentIdsForFailed.add(rs.getInt("id"));
            }
            rs.close();
            ps.close();
            
            // Get all subjects for this section (JOIN with subjects table to get subject_name)
            String subjectsQueryForFailed = 
                "SELECT s.subject_name FROM section_subjects ss " +
                "JOIN subjects s ON ss.subject_id = s.id " +
                "WHERE ss.section_id = ?";
            ps = conn.prepareStatement(subjectsQueryForFailed);
            ps.setInt(1, sectionId);
            rs = ps.executeQuery();
            
            List<String> subjectsForFailed = new ArrayList<>();
            while (rs.next()) {
                subjectsForFailed.add(rs.getString("subject_name"));
            }
            rs.close();
            ps.close();
            
            
            // Count failed subjects per student using DUAL PASSING logic
            Map<Integer, Integer> studentFailCounts = new HashMap<>();
            
            for (int studentId : studentIdsForFailed) {
                int failedSubjectCount = 0;
                
                for (String subject : subjectsForFailed) {
                    // Apply filters if present
                    Set<String> examTypes = (selectedFilters != null && selectedFilters.containsKey(subject)) 
                                            ? selectedFilters.get(subject) : null;
                    
                    SubjectPassResult result = calculateWeightedSubjectTotalWithPass(studentId, sectionId, subject, examTypes);
                    
                    // Student failed this subject if !result.passed (component fail OR total < 40)
                    if (!result.passed) {
                        failedSubjectCount++;
                    }
                }
                
                if (failedSubjectCount > 0) {
                    studentFailCounts.put(studentId, failedSubjectCount);
                }
            }
            
            // Group students by number of failed subjects
            Map<Integer, Integer> failedSubjectsDistribution = new HashMap<>();
            for (int failCount : studentFailCounts.values()) {
                int bucket = (failCount >= 6) ? 6 : failCount;
                failedSubjectsDistribution.put(bucket, failedSubjectsDistribution.getOrDefault(bucket, 0) + 1);
            }
            
            // Populate data.failedStudentsMap
            for (int i = 1; i <= 6; i++) {
                int count = failedSubjectsDistribution.getOrDefault(i, 0);
                if (count > 0) {
                    data.failedStudentsMap.put(i, count);
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
            
            // Calculate pass/fail students using EFFICIENT BATCH METHOD with DUAL PASSING
            Map<Integer, Double> allPercentages = calculateAllStudentPercentagesBatch(sectionId, userId, selectedFilters);
            

            
            int passCount = 0;
            int failCount = 0;
            List<Integer> failedStudentIds = new ArrayList<>();
            
            for (Map.Entry<Integer, Double> entry : allPercentages.entrySet()) {
                // Negative percentage means failed ANY subject (component or total failure)
                // Student passes ONLY if positive percentage (all subjects passed)
                if (entry.getValue() < 0) {
                    failCount++;
                    failedStudentIds.add(entry.getKey());
                } else {
                    passCount++;
                }
            }
            
            
            data.passStudents = passCount;
            data.failStudents = failCount;
            
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
        // Get all students for this section
        String studentsQuery = "SELECT id FROM students WHERE section_id = ? AND created_by = ?";
        PreparedStatement psStudents = conn.prepareStatement(studentsQuery);
        psStudents.setInt(1, sectionId);
        psStudents.setInt(2, userId);
        ResultSet rsStudents = psStudents.executeQuery();
        
        List<Integer> studentIds = new ArrayList<>();
        while (rsStudents.next()) {
            studentIds.add(rsStudents.getInt("id"));
        }
        rsStudents.close();
        psStudents.close();
        
        // Calculate WEIGHTED percentage for each student in this subject with DUAL PASSING
        Set<String> examTypes = (selectedFilters != null && selectedFilters.containsKey(subjectName)) 
                                ? selectedFilters.get(subjectName) : null;
        

        
        // Initialize counters
        sa.passCount = 0;
        sa.distinctionCount = 0;
        sa.firstClassCount = 0;
        sa.secondClassCount = 0;
        sa.failCount = 0;
        double totalPercentage = 0.0;
        int countedStudents = 0;
        
        List<Integer> failedStudentIds = new ArrayList<>();
        
        for (int studentId : studentIds) {
            SubjectPassResult result = calculateWeightedSubjectTotalWithPass(studentId, sectionId, subjectName, examTypes);
            
            if (result.percentage >= 0) {
                totalPercentage += result.percentage;
                countedStudents++;
                
                // Categorize by weighted percentage AND passing status
                if (result.passed && result.percentage >= 75) {
                    sa.distinctionCount++;
                    sa.passCount++;
                } else if (result.passed && result.percentage >= 60) {
                    sa.firstClassCount++;
                    sa.passCount++;
                } else if (result.passed && result.percentage >= 50) {
                    sa.secondClassCount++;
                    sa.passCount++;
                } else {
                    sa.failCount++;  // Fail if percentage < 50 OR component failed OR total < 40
                    failedStudentIds.add(studentId);
                }
            }
        }
        
        
        // Set average marks (using weighted percentage)
        sa.averageMarks = countedStudents > 0 ? (totalPercentage / countedStudents) : 0.0;
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
        
        public SubjectInfo() {}
        
        public SubjectInfo(int id, String name) {
            this.id = id;
            this.name = name;
        }
        
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
            
            // First check which marking system this section uses
            String checkQuery = "SELECT marking_system FROM sections WHERE id = ?";
            PreparedStatement checkPs = conn.prepareStatement(checkQuery);
            checkPs.setInt(1, sectionId);
            ResultSet checkRs = checkPs.executeQuery();
            
            String markingSystem = "old"; // default
            if (checkRs.next()) {
                markingSystem = checkRs.getString("marking_system");
            }
            checkRs.close();
            checkPs.close();
            
            // If flexible system, get components from marking_components
            if ("flexible".equals(markingSystem)) {
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
            } else {
                // Old system - get from exam_types (simple structure)
                // Join with entered_exam_marks to get only exam types used for this specific subject
                StringBuilder query = new StringBuilder();
                query.append("SELECT DISTINCT et.id, et.exam_name as component_name, 'exam' as component_type, ")
                     .append("et.max_marks as actual_max_marks, et.weightage as scaled_to_marks, ")
                     .append("NULL as component_group, et.id as sequence_order, ")
                     .append("NULL as group_name, NULL as selection_type, 0 as selection_count ")
                     .append("FROM exam_types et ")
                     .append("JOIN entered_exam_marks eem ON et.id = eem.exam_type_id ")
                     .append("WHERE et.section_id = ? AND eem.subject_id = ? ");
                
                if (!"all".equals(componentType)) {
                    query.append("AND 'exam' = ? ");
                }
                
                query.append("ORDER BY et.id");
                
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
            }
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
            
            // Query student_component_marks table (with LEFT JOIN to handle missing components)
            String query = "SELECT scm.component_id, scm.marks_obtained, scm.scaled_marks, " +
                          "scm.is_counted, et.exam_name as component_name, et.max_marks as actual_max_marks, " +
                          "et.max_marks as scaled_to_marks " +
                          "FROM student_component_marks scm " +
                          "LEFT JOIN exam_types et ON scm.component_id = et.id " +
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
            
            // First try the new system (section_subjects)
            String query = "SELECT DISTINCT s.id, s.subject_name " +
                          "FROM subjects s " +
                          "JOIN section_subjects ss ON s.id = ss.subject_id " +
                          "WHERE ss.section_id = ? " +
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
            
            // If no subjects found, try the old system (marking_schemes)
            if (subjects.isEmpty()) {
                query = "SELECT DISTINCT s.id, s.subject_name " +
                       "FROM subjects s " +
                       "JOIN marking_schemes ms ON s.id = ms.subject_id " +
                       "WHERE ms.section_id = ? " +
                       "ORDER BY s.subject_name";
                
                ps = conn.prepareStatement(query);
                ps.setInt(1, sectionId);
                rs = ps.executeQuery();
                
                while (rs.next()) {
                    SubjectInfo subject = new SubjectInfo();
                    subject.id = rs.getInt("id");
                    subject.name = rs.getString("subject_name");
                    subjects.add(subject);
                }
                
                rs.close();
                ps.close();
            }
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
    
    // Class to hold exam type configuration (for weighted calculation)
    public static class ExamTypeConfig {
        public int id;
        public String examName;
        public int maxMarks;      // Paper maximum (what student can score)
        public int weightage;     // Contribution % to subject total 100
        public int passingMarks;  // Component pass threshold
        
        public ExamTypeConfig(int id, String examName, int maxMarks, int weightage, int passingMarks) {
            this.id = id;
            this.examName = examName;
            this.maxMarks = maxMarks;
            this.weightage = weightage;
            this.passingMarks = passingMarks;
        }
    }
    
    // Result class for DUAL PASSING REQUIREMENT
    public static class SubjectPassResult {
        public double percentage;           // Weighted percentage (0-100)
        public boolean passed;              // True if BOTH component AND total pass
        public boolean totalPassed;         // True if total >= 50
        public boolean allComponentsPassed; // True if all components >= passing marks
        public List<String> failedComponents;  // List of failed component names
        
        public SubjectPassResult(double percentage, boolean passed, boolean totalPassed, 
                                boolean allComponentsPassed, List<String> failedComponents) {
            this.percentage = percentage;
            this.passed = passed;
            this.totalPassed = totalPassed;
            this.allComponentsPassed = allComponentsPassed;
            this.failedComponents = failedComponents;
        }
    }
    
    /**
     * Get exam type configurations for a subject in a section
     * Used for scaled/weighted calculation
     */
    public List<ExamTypeConfig> getExamTypesForSubject(int sectionId, int subjectId) {
        List<ExamTypeConfig> examTypes = new ArrayList<>();
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            String query = "SELECT et.id, et.exam_name, et.max_marks, et.weightage, et.passing_marks " +
                          "FROM exam_types et " +
                          "JOIN subject_exam_types set_tbl ON et.id = set_tbl.exam_type_id " +
                          "WHERE set_tbl.section_id = ? AND set_tbl.subject_id = ? " +
                          "ORDER BY et.exam_name";
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, sectionId);
            ps.setInt(2, subjectId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                ExamTypeConfig config = new ExamTypeConfig(
                    rs.getInt("id"),
                    rs.getString("exam_name"),
                    rs.getInt("max_marks"),
                    rs.getInt("weightage"),
                    rs.getInt("passing_marks")
                );
                examTypes.add(config);
            }
            
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return examTypes;
    }
    
    /**
     * Calculate weighted subject total using SCALED FORMULA with DUAL PASSING REQUIREMENT:
     * Total = ??((marks_obtained / max_marks) ?? weightage) for all components
     * PASS REQUIREMENTS:
     * 1. Each component marks >= component passing_marks (e.g., Internal1 >= 8)
     * 2. Total weighted percentage >= 40
     * FAIL if ANY component fails OR total < 40
     * @param studentId Student ID
     * @param sectionId Section ID  
     * @param subjectName Subject name
     * @param selectedExamTypes If not null, only include these exam types in calculation
     * @return SubjectPassResult with percentage and pass/fail status
     */
    public SubjectPassResult calculateWeightedSubjectTotalWithPass(int studentId, int sectionId, String subjectName, Set<String> selectedExamTypes) {
        List<String> failedComponents = new ArrayList<>();
        double weightedTotal = 0.0;
        int componentsIncluded = 0;
        boolean allComponentsPassed = true;
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            // Get subject ID
            String subjectQuery = "SELECT id FROM subjects WHERE subject_name = ?";
            PreparedStatement psSubject = conn.prepareStatement(subjectQuery);
            psSubject.setString(1, subjectName);
            ResultSet rsSubject = psSubject.executeQuery();
            
            int subjectId = 0;
            if (rsSubject.next()) {
                subjectId = rsSubject.getInt("id");
            }
            rsSubject.close();
            psSubject.close();
            
            if (subjectId == 0) {
                return new SubjectPassResult(-1, false, false, false, failedComponents);
            }
            
            // Get exam type configurations with passing marks
            List<ExamTypeConfig> examTypes = getExamTypesForSubject(sectionId, subjectId);
            
            if (examTypes.isEmpty()) {
                return new SubjectPassResult(-1, false, false, false, failedComponents);
            }
            
            // Get student's marks for this subject
            String marksQuery = "SELECT et.exam_name, sm.marks_obtained " +
                              "FROM entered_exam_marks sm " +
                              "JOIN exam_types et ON sm.exam_type_id = et.id " +
                              "WHERE sm.student_id = ? AND sm.subject_id = ?";
            PreparedStatement psMarks = conn.prepareStatement(marksQuery);
            psMarks.setInt(1, studentId);
            psMarks.setInt(2, subjectId);
            ResultSet rsMarks = psMarks.executeQuery();
            
            Map<String, Integer> marksMap = new HashMap<>();
            while (rsMarks.next()) {
                marksMap.put(rsMarks.getString("exam_name"), rsMarks.getInt("marks_obtained"));
            }
            rsMarks.close();
            psMarks.close();
            
            // COMMENT OUT for now - too much output
            
            for (ExamTypeConfig examType : examTypes) {
                // Skip if filter is active and this exam not selected
                if (selectedExamTypes != null && !selectedExamTypes.contains(examType.examName)) {
                    continue;
                }
                
                Integer marksObtained = marksMap.get(examType.examName);
                if (marksObtained == null) {
                    failedComponents.add(examType.examName);
                    allComponentsPassed = false;
                    continue;
                }
                
                // PROTECTION: Skip if max_marks is 0 or invalid (data error)
                if (examType.maxMarks <= 0) {
                    continue;
                }
                
                // DUAL PASSING CHECK 1: Component passing marks
                boolean componentPassed = (marksObtained >= examType.passingMarks);
                if (!componentPassed) {
                    failedComponents.add(examType.examName);
                    allComponentsPassed = false;
                }
                
                // SCALED FORMULA: (marks_obtained / max_marks) ?? weightage
                double contribution = (marksObtained.doubleValue() / examType.maxMarks) * examType.weightage;
                weightedTotal += contribution;
                componentsIncluded++;
                
                //                  " (pass>=" + examType.passingMarks + ") ?? " + examType.weightage + "% = " + 
                //                  String.format("%.2f", contribution) + " [" + (componentPassed ? "PASS" : "FAIL") + "]");
            }
            
            if (componentsIncluded == 0) {
                // System.out.println("No valid components found");
                return new SubjectPassResult(-1, false, false, false, failedComponents);
            }
            
            // DUAL PASSING CHECK 2: Total marks >= 50
            boolean totalPassed = (weightedTotal >= 50);
            
            // FINAL RESULT: Pass only if BOTH component AND total pass
            boolean overallPassed = allComponentsPassed && totalPassed;
            
            // if (!failedComponents.isEmpty()) {
            // }
            
            return new SubjectPassResult(weightedTotal, overallPassed, totalPassed, allComponentsPassed, failedComponents);
            
        } catch (SQLException e) {
            e.printStackTrace();
            return new SubjectPassResult(-1, false, false, false, failedComponents);
        }
    }
    
    /**
     * Legacy method - kept for backward compatibility
     * @deprecated Use calculateWeightedSubjectTotalWithPass() for DUAL PASSING REQUIREMENT
     */
    @Deprecated
    public double calculateWeightedSubjectTotal(int studentId, int sectionId, String subjectName, Set<String> selectedExamTypes) {
        // For backward compatibility, return the percentage from the new method
        SubjectPassResult result = calculateWeightedSubjectTotalWithPass(studentId, sectionId, subjectName, selectedExamTypes);
        return Math.abs(result.percentage); // Always return positive percentage for display
    }
    
    /**
     * Calculate total weighted marks for a student across all filtered subjects
     * @param studentId Student ID
     * @param sectionId Section ID
     * @param selectedFilters Map of subject -> Set of exam types to include
     * @return Total weighted marks out of (number of subjects ?? 100)
     */
    public double getStudentWeightedTotal(int studentId, int sectionId, Map<String, Set<String>> selectedFilters) {
        double totalWeighted = 0.0;
        int subjectCount = 0;
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            // Get all subjects for this section
            String subjectQuery = "SELECT DISTINCT sub.subject_name FROM section_subjects ss " +
                                 "JOIN subjects sub ON ss.subject_id = sub.id " +
                                 "WHERE ss.section_id = ?";
            PreparedStatement ps = conn.prepareStatement(subjectQuery);
            ps.setInt(1, sectionId);
            ResultSet rs = ps.executeQuery();
            
            List<String> subjects = new ArrayList<>();
            while (rs.next()) {
                String subjectName = rs.getString("subject_name");
                // Only include if in filter or no filter
                if (selectedFilters == null || selectedFilters.isEmpty() || selectedFilters.containsKey(subjectName)) {
                    subjects.add(subjectName);
                }
            }
            rs.close();
            ps.close();
            
            // Calculate weighted total for each subject using DUAL PASSING
            for (String subject : subjects) {
                Set<String> examTypes = (selectedFilters != null) ? selectedFilters.get(subject) : null;
                SubjectPassResult result = calculateWeightedSubjectTotalWithPass(studentId, sectionId, subject, examTypes);
                
                // Add absolute percentage to total regardless of pass/fail
                // This gives total weighted marks across all subjects
                totalWeighted += Math.abs(result.percentage);
                subjectCount++;
            }
        
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return totalWeighted;
    }
    
    /**
     * Calculate overall percentage for a student based on weighted totals
     * @param studentId Student ID
     * @param sectionId Section ID
     * @param selectedFilters Map of subject -> Set of exam types to include
     * @return Percentage (0-100) based on weighted calculations
     */
    public double getStudentWeightedPercentage(int studentId, int sectionId, Map<String, Set<String>> selectedFilters) {
        double totalWeighted = 0.0;
        int subjectCount = 0;
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            // Get all subjects for this section
            String subjectQuery = "SELECT DISTINCT sub.subject_name FROM section_subjects ss " +
                                 "JOIN subjects sub ON ss.subject_id = sub.id " +
                                 "WHERE ss.section_id = ?";
            PreparedStatement ps = conn.prepareStatement(subjectQuery);
            ps.setInt(1, sectionId);
            ResultSet rs = ps.executeQuery();
            
            List<String> subjects = new ArrayList<>();
            while (rs.next()) {
                String subjectName = rs.getString("subject_name");
                // Only include if in filter or no filter
                if (selectedFilters == null || selectedFilters.isEmpty() || selectedFilters.containsKey(subjectName)) {
                    subjects.add(subjectName);
                }
            }
            rs.close();
            ps.close();
            
            // Calculate weighted total for each subject using DUAL PASSING
            for (String subject : subjects) {
                Set<String> examTypes = (selectedFilters != null) ? selectedFilters.get(subject) : null;
                SubjectPassResult result = calculateWeightedSubjectTotalWithPass(studentId, sectionId, subject, examTypes);
                
                // Add absolute percentage to total regardless of pass/fail
                // This gives total weighted marks across all subjects
                totalWeighted += Math.abs(result.percentage);
                subjectCount++;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // Return average percentage across all subjects
        return subjectCount > 0 ? (totalWeighted / subjectCount) : 0.0;
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
            
            // Get user ID for this section
            String userQuery = "SELECT created_by FROM students WHERE section_id = ? LIMIT 1";
            PreparedStatement psUser = conn.prepareStatement(userQuery);
            psUser.setInt(1, sectionId);
            ResultSet rsUser = psUser.executeQuery();
            int userId = rsUser.next() ? rsUser.getInt("created_by") : 0;
            rsUser.close();
            psUser.close();
            
            // Use EFFICIENT BATCH METHOD to get all percentages at once (with DUAL PASSING)
            Map<Integer, Double> allPercentages = calculateAllStudentPercentagesBatch(sectionId, userId, selectedFilters);
            
            // Count students by grade
            Map<String, Integer> gradeCounts = new LinkedHashMap<>();
            gradeCounts.put("A+", 0);
            gradeCounts.put("A", 0);
            gradeCounts.put("B+", 0);
            gradeCounts.put("B", 0);
            gradeCounts.put("C", 0);
            gradeCounts.put("D", 0);
            gradeCounts.put("F", 0);
            
            for (Double percentage : allPercentages.values()) {
                // Negative means failed due to component/total failure
                if (percentage < 0) {
                    gradeCounts.put("F", gradeCounts.get("F") + 1);
                } else {
                    String grade = getGradeFromPercentage(percentage);
                    gradeCounts.put(grade, gradeCounts.get(grade) + 1);
                }
            }
            
            // Convert to list
            for (Map.Entry<String, Integer> entry : gradeCounts.entrySet()) {
                distribution.add(new GradeDistribution(entry.getKey(), entry.getValue()));
            }
            
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return distribution;
    }
    
    public String getGradeFromPercentage(double percentage) {
        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B+";
        if (percentage >= 60) return "B";
        if (percentage >= 50) return "C";
        if (percentage >= 40) return "D";
        return "F";
    }
    
    /**
     * Public method to get student marks for result launcher
     */
    public Map<String, Map<String, Integer>> getStudentMarksDetailed(int studentId, int sectionId) {
        return getStudentMarks(studentId, sectionId);
    }
    
    /**
     * Get exam type configuration details
     */
    public ExamTypeConfig getExamTypeConfig(int sectionId, String examName) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT id, exam_name, max_marks, weightage, passing_marks " +
                          "FROM exam_types WHERE section_id = ? AND exam_name = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, sectionId);
            ps.setString(2, examName);
            ResultSet rs = ps.executeQuery();
            
            ExamTypeConfig config = null;
            if (rs.next()) {
                config = new ExamTypeConfig(
                    rs.getInt("id"),
                    rs.getString("exam_name"),
                    rs.getInt("max_marks"),
                    rs.getInt("weightage"),
                    rs.getInt("passing_marks")
                );
            }
            rs.close();
            ps.close();
            return config;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
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
            
            // Get user ID and student details
            String studentsQuery = "SELECT id, roll_number, student_name, created_by FROM students WHERE section_id = ?";
            PreparedStatement ps = conn.prepareStatement(studentsQuery);
            ps.setInt(1, sectionId);
            ResultSet rs = ps.executeQuery();
            
            Map<Integer, StudentBasicInfo> studentInfoMap = new HashMap<>();
            int userId = 0;
            while (rs.next()) {
                int studentId = rs.getInt("id");
                String rollNo = rs.getString("roll_number");
                String name = rs.getString("student_name");
                userId = rs.getInt("created_by");
                studentInfoMap.put(studentId, new StudentBasicInfo(rollNo, name));
            }
            rs.close();
            ps.close();
            
            // Use EFFICIENT BATCH METHOD to get all percentages at once (with DUAL PASSING)
            Map<Integer, Double> allPercentages = calculateAllStudentPercentagesBatch(sectionId, userId, selectedFilters);
            
            // Filter students below 60% OR with component failures (negative percentage)
            for (Map.Entry<Integer, Double> entry : allPercentages.entrySet()) {
                int studentId = entry.getKey();
                double percentage = entry.getValue();
                boolean failed = (percentage < 0);
                double absPercentage = Math.abs(percentage);
                
                // At risk if: failed due to components OR percentage < 60
                if (failed || absPercentage < 60) {
                    StudentBasicInfo info = studentInfoMap.get(studentId);
                    if (info == null) continue;
                    
                    String riskLevel;
                    if (failed) {
                        riskLevel = "Critical (Component Fail)";
                    } else if (absPercentage < 50) {
                        riskLevel = "Critical";
                    } else {
                        riskLevel = "Borderline";
                    }
                    
                    String failedSubjects = getFailedSubjectsForStudent(sectionId, info.rollNumber, selectedFilters);
                    
                    atRiskStudents.add(new AtRiskStudent(info.rollNumber, info.name, absPercentage, riskLevel, failedSubjects));
                }
            }
            
            // Sort by percentage ascending (worst first)
            atRiskStudents.sort((a, b) -> Double.compare(a.percentage, b.percentage));
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return atRiskStudents;
    }
    
    private String getFailedSubjectsForStudent(int sectionId, String rollNo, Map<String, Set<String>> selectedFilters) {
        List<String> failedSubjects = new ArrayList<>();
        
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            // Get student ID from roll number
            String studentQuery = "SELECT id FROM students WHERE section_id = ? AND roll_number = ?";
            PreparedStatement psStudent = conn.prepareStatement(studentQuery);
            psStudent.setInt(1, sectionId);
            psStudent.setString(2, rollNo);
            ResultSet rsStudent = psStudent.executeQuery();
            
            int studentId = 0;
            if (rsStudent.next()) {
                studentId = rsStudent.getInt("id");
            }
            rsStudent.close();
            psStudent.close();
            
            if (studentId == 0) {
                return "None";
            }
            
            // Get all subjects for this section
            String subjectsQuery = "SELECT DISTINCT sub.subject_name FROM section_subjects ss " +
                                  "JOIN subjects sub ON ss.subject_id = sub.id " +
                                  "WHERE ss.section_id = ?";
            PreparedStatement psSubjects = conn.prepareStatement(subjectsQuery);
            psSubjects.setInt(1, sectionId);
            ResultSet rsSubjects = psSubjects.executeQuery();
            
            List<String> subjects = new ArrayList<>();
            while (rsSubjects.next()) {
                String subjectName = rsSubjects.getString("subject_name");
                // Only check subjects in filter or all if no filter
                if (selectedFilters == null || selectedFilters.isEmpty() || selectedFilters.containsKey(subjectName)) {
                    subjects.add(subjectName);
                }
            }
            rsSubjects.close();
            psSubjects.close();
            
            // For each subject, calculate WEIGHTED percentage with DUAL PASSING CHECK
            for (String subject : subjects) {
                Set<String> examTypes = (selectedFilters != null) ? selectedFilters.get(subject) : null;
                SubjectPassResult result = calculateWeightedSubjectTotalWithPass(studentId, sectionId, subject, examTypes);
                
                if (!result.failedComponents.isEmpty()) {
                }
                
                // Subject fails if: component failed OR total < 50
                // Check both valid calculation (>= 0) AND any failure condition
                if (result.percentage >= 0 && (!result.passed || !result.allComponentsPassed || !result.totalPassed)) {
                    // Include failure reason
                    if (!result.allComponentsPassed && !result.failedComponents.isEmpty()) {
                        failedSubjects.add(subject + " [" + String.join(", ", result.failedComponents) + "]");
                    } else if (!result.totalPassed) {
                        failedSubjects.add(subject + " [Total < 50]");
                    } else {
                        failedSubjects.add(subject);
                    }
                } else if (result.percentage < 0) {
                    // Invalid calculation - likely no marks
                    failedSubjects.add(subject + " [No marks]");
                } else {
                }
            }
            
            
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return failedSubjects.isEmpty() ? "None" : String.join(", ", failedSubjects);
    }
    
    // REMOVED OLD QUERY-BASED METHOD - Now using weighted calculation
    private String getFailedSubjectsForStudentOLD(int sectionId, String rollNo, Map<String, Set<String>> selectedFilters) {
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
                    subjectFilters.add("(sub.subject_name = ? AND et.exam_name IN (" + componentList + "))");
                    
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
                       "INNER JOIN entered_exam_marks sm ON sm.student_id = s.id " +
                       "INNER JOIN exam_types et ON sm.exam_type_id = et.id " +
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
                       "LEFT JOIN entered_exam_marks sm ON sm.student_id = s.id AND sm.subject_id = ss.subject_id " +
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
    
    // StudentRanking class for all students ranking table
    public static class StudentRanking {
        public int rank;
        public String rollNumber;
        public String studentName;
        public double totalMarks;
        public double percentage;
        public double cgpa;
        public double cgpaPercentage;
        
        public StudentRanking(int rank, String rollNumber, String studentName, double totalMarks, 
                            double percentage, double cgpa, double cgpaPercentage) {
            this.rank = rank;
            this.rollNumber = rollNumber;
            this.studentName = studentName;
            this.totalMarks = totalMarks;
            this.percentage = percentage;
            this.cgpa = cgpa;
            this.cgpaPercentage = cgpaPercentage;
        }
    }
    
    public List<StudentRanking> getAllStudentsRanking(int sectionId, Map<String, Set<String>> selectedFilters) {
        List<StudentRanking> rankings = new ArrayList<>();
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            // Get user ID for this section
            String userQuery = "SELECT created_by FROM students WHERE section_id = ? LIMIT 1";
            PreparedStatement psUser = conn.prepareStatement(userQuery);
            psUser.setInt(1, sectionId);
            ResultSet rsUser = psUser.executeQuery();
            int userId = rsUser.next() ? rsUser.getInt("created_by") : 0;
            rsUser.close();
            psUser.close();
            
            // Use EFFICIENT BATCH METHOD with DUAL PASSING REQUIREMENT
            Map<Integer, Double> allPercentages = calculateAllStudentPercentagesBatch(sectionId, userId, selectedFilters);
            
            // Get student names and roll numbers
            String studentsQuery = "SELECT id, student_name, roll_number FROM students WHERE section_id = ? AND created_by = ?";
            PreparedStatement psStudents = conn.prepareStatement(studentsQuery);
            psStudents.setInt(1, sectionId);
            psStudents.setInt(2, userId);
            ResultSet rsStudents = psStudents.executeQuery();
            
            // Create list of student data with percentages
            List<StudentRankingData> studentData = new ArrayList<>();
            while (rsStudents.next()) {
                int studentId = rsStudents.getInt("id");
                String rollNumber = rsStudents.getString("roll_number");
                String studentName = rsStudents.getString("student_name");
                
                Double percentage = allPercentages.get(studentId);
                if (percentage == null) continue; // Skip students with no marks
                
                // Calculate CGPA based on actual performance (negative = failed = 0.0 CGPA)
                double cgpa = 0.0;
                double cgpaPercentage = 0.0;
                
                if (percentage < 0) {
                    // Student FAILED at least one subject - CGPA = 0.0
                    cgpa = 0.0;
                    cgpaPercentage = 0.0;
                } else {
                    // Student PASSED all subjects - Calculate CGPA as percentage/10 (same as StudentAnalyzer)
                    double displayPercentage = Math.abs(percentage); // For calculation only
                    cgpa = displayPercentage / 10.0; // Same formula as StudentAnalyzer
                    cgpaPercentage = displayPercentage;
                }
                
                // Calculate total marks (percentage * number of subjects * 100 / 100)
                int subjectCount = 0;
                try {
                    String countQuery = "SELECT COUNT(DISTINCT subject_id) FROM section_subjects WHERE section_id = ?";
                    PreparedStatement psCount = conn.prepareStatement(countQuery);
                    psCount.setInt(1, sectionId);
                    ResultSet rsCount = psCount.executeQuery();
                    if (rsCount.next()) {
                        subjectCount = rsCount.getInt(1);
                    }
                    rsCount.close();
                    psCount.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                
                double displayPercentage = Math.abs(percentage); // For display and calculations
                double totalMarks = displayPercentage * subjectCount; // Since each subject is out of 100
                
                studentData.add(new StudentRankingData(
                    studentId, rollNumber, studentName, totalMarks, displayPercentage, cgpa, cgpaPercentage
                ));
            }  // End of while loop
            
            rsStudents.close();
            psStudents.close();
            
            // Sort by total marks (descending) - use absolute percentage for ranking
            studentData.sort((a, b) -> Double.compare(b.totalMarks, a.totalMarks));
            
            // Assign ranks
            int rank = 1;
            for (StudentRankingData data : studentData) {
                rankings.add(new StudentRanking(
                    rank++,
                    data.rollNumber,
                    data.studentName,
                    data.totalMarks,
                    data.percentage,
                    data.cgpa,
                    data.cgpaPercentage
                ));
            }
            
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return rankings;
    }
    
    // Helper class for ranking calculation
    private static class StudentRankingData {
        int studentId;
        String rollNumber;
        String studentName;
        double totalMarks;
        double percentage;
        double cgpa;
        double cgpaPercentage;
        
        StudentRankingData(int studentId, String rollNumber, String studentName, 
                          double totalMarks, double percentage, double cgpa, double cgpaPercentage) {
            this.studentId = studentId;
            this.rollNumber = rollNumber;
            this.studentName = studentName;
            this.totalMarks = totalMarks;
            this.percentage = percentage;
            this.cgpa = cgpa;
            this.cgpaPercentage = cgpaPercentage;
        }
    }
    
    // Detailed Ranking Data Classes
    public static class DetailedRankingData {
        public List<SubjectInfoDetailed> subjects = new ArrayList<>();
        public List<StudentRankingDetail> students = new ArrayList<>();
    }
    
    public static class SubjectInfoDetailed {
        public String subjectName;
        public int maxMarks;
        public List<String> examTypes = new ArrayList<>();
        public Map<String, Integer> examTypeMaxMarks = new LinkedHashMap<>();
        public Map<String, Integer> examTypeWeightage = new LinkedHashMap<>();
        
        // Helper method to add exam type only if not already present
        public void addExamType(String examType) {
            if (!examTypes.contains(examType)) {
                examTypes.add(examType);
            }
        }
    }
    
    public static class StudentRankingDetail {
        public int rank;
        public String rollNumber;
        public String studentName;
        public double totalMarks;
        public double percentage;
        public String grade;
        public double cgpa;
        public Map<String, Map<String, Double>> subjectMarks = new HashMap<>(); // subjectName -> examType -> marks
    }
    
    public DetailedRankingData getDetailedStudentRanking(int sectionId, Map<String, Set<String>> selectedFilters) {
        DetailedRankingData data = new DetailedRankingData();
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            // Step 1: Get subjects and their max marks for this section
            String subjectQuery = 
                "SELECT DISTINCT sub.subject_name, sub.id, ss.max_marks " +
                "FROM section_subjects ss " +
                "JOIN subjects sub ON ss.subject_id = sub.id " +
                "WHERE ss.section_id = ? " +
                "ORDER BY sub.subject_name";
            
            PreparedStatement ps1 = conn.prepareStatement(subjectQuery);
            ps1.setInt(1, sectionId);
            ResultSet rs1 = ps1.executeQuery();
            
            Map<String, SubjectInfoDetailed> subjectMap = new LinkedHashMap<>();
            Map<String, Integer> subjectIds = new HashMap<>();
            
            while (rs1.next()) {
                String subjectName = rs1.getString("subject_name");
                int subjectId = rs1.getInt("id");
                int maxMarks = rs1.getInt("max_marks");
                
                SubjectInfoDetailed subInfo = new SubjectInfoDetailed();
                subInfo.subjectName = subjectName;
                subInfo.maxMarks = maxMarks;
                
                subjectMap.put(subjectName, subInfo);
                subjectIds.put(subjectName, subjectId);
            }
            rs1.close();
            ps1.close();
            
            // Step 2: Get exam types and their actual max marks for each subject
            for (Map.Entry<String, Integer> entry : subjectIds.entrySet()) {
                String subjectName = entry.getKey();
                int subjectId = entry.getValue();
                
                System.out.println("@@@ PROCESSING SUBJECT: " + subjectName + " (ID: " + subjectId + ") @@@");
                
                SubjectInfoDetailed subInfo = subjectMap.get(subjectName);
                Set<String> examTypes = new LinkedHashSet<>();
                Map<String, Integer> examTypeMaxMarks = new LinkedHashMap<>();
                Map<String, Integer> examTypeWeightage = new LinkedHashMap<>();
                
                // First, try to get from marking_components (new system with schemes)
                String componentQuery = 
                    "SELECT mc.component_name, mc.actual_max_marks, mc.scaled_to_marks " +
                    "FROM marking_components mc " +
                    "JOIN marking_schemes ms ON mc.scheme_id = ms.id " +
                    "WHERE ms.section_id = ? AND ms.subject_id = ? AND ms.is_active = 1 " +
                    "ORDER BY mc.sequence_order, mc.component_name";
                PreparedStatement ps2 = conn.prepareStatement(componentQuery);
                ps2.setInt(1, sectionId);
                ps2.setInt(2, subjectId);
                ResultSet rs2 = ps2.executeQuery();
                
                boolean hasComponents = false;
                while (rs2.next()) {
                    String componentName = rs2.getString("component_name");
                    int actualMaxMarks = rs2.getInt("actual_max_marks");  // For display in brackets
                    int scaledMarks = rs2.getInt("scaled_to_marks");     // For weighted calculation
                    if (componentName != null && !componentName.trim().isEmpty()) {
                        examTypes.add(componentName);
                        examTypeMaxMarks.put(componentName, actualMaxMarks);  // Display actual max marks
                        // Weightage is the scaled marks (weighted component out of 100 total)
                        examTypeWeightage.put(componentName, scaledMarks);
                        hasComponents = true;
                    }
                }
                rs2.close();
                ps2.close();
                
                System.out.println("@@@ QUERY COMPLETE: hasComponents=" + hasComponents + ", found " + examTypeMaxMarks.size() + " components @@@");
                
                // If no components found in new system, get from old system (entered_exam_marks)
                if (!hasComponents) {
                    String examTypeQuery1 = 
                        "SELECT DISTINCT et.exam_name, et.max_marks " +
                        "FROM entered_exam_marks sm " +
                        "JOIN exam_types et ON sm.exam_type_id = et.id " +
                        "JOIN students s ON sm.student_id = s.id " +
                        "WHERE sm.subject_id = ? AND s.section_id = ? " +
                        "ORDER BY et.exam_name";
                    
                    PreparedStatement ps3 = conn.prepareStatement(examTypeQuery1);
                    ps3.setInt(1, subjectId);
                    ps3.setInt(2, sectionId);
                    ResultSet rs3 = ps3.executeQuery();
                    
                    while (rs3.next()) {
                        String examType = rs3.getString("exam_name");
                        int maxMarks = rs3.getInt("max_marks");
                        if (examType != null && !examType.trim().isEmpty()) {
                            examTypes.add(examType);
                            examTypeMaxMarks.put(examType, maxMarks);
                        }
                    }
                    rs3.close();
                    ps3.close();
                    
                    // Also check marks table with exam_types
                    String examTypeQuery2 = 
                        "SELECT DISTINCT et.exam_name, et.max_marks " +
                        "FROM marks m " +
                        "JOIN exam_types et ON m.exam_type_id = et.id " +
                        "JOIN students s ON m.student_id = s.id " +
                        "WHERE m.subject_id = ? AND s.section_id = ? " +
                        "ORDER BY et.exam_name";
                    
                    PreparedStatement ps4 = conn.prepareStatement(examTypeQuery2);
                    ps4.setInt(1, subjectId);
                    ps4.setInt(2, sectionId);
                    ResultSet rs4 = ps4.executeQuery();
                    
                    while (rs4.next()) {
                        String examName = rs4.getString("exam_name");
                        int maxMarks = rs4.getInt("max_marks");
                        if (examName != null && !examName.trim().isEmpty()) {
                            examTypes.add(examName);
                            if (!examTypeMaxMarks.containsKey(examName)) {
                                examTypeMaxMarks.put(examName, maxMarks);
                                System.out.println("@@@ OLD SYSTEM (marks table): " + examName + " | max_marks=" + maxMarks + " @@@");
                            }
                        }
                    }
                    rs4.close();
                    ps4.close();
                }
                
                // Add exam types to subject info (prevent duplicates)
                for (String examType : examTypes) {
                    subInfo.addExamType(examType);
                }
                
                // If we have actual max marks from components, use them
                if (!examTypeMaxMarks.isEmpty()) {
                    subInfo.examTypeMaxMarks.putAll(examTypeMaxMarks);
                    subInfo.examTypeWeightage.putAll(examTypeWeightage);
                } else {
                    // For old system without component max marks, don't show individual marks
                    // Just leave examTypeMaxMarks empty - we'll handle display differently
                }
            }
            
            data.subjects.addAll(subjectMap.values());
            
            // Step 3: Get all students and their marks
            String studentQuery = 
                "SELECT s.id, s.roll_number, s.student_name " +
                "FROM students s " +
                "WHERE s.section_id = ? " +
                "ORDER BY s.roll_number";
            
            PreparedStatement ps4 = conn.prepareStatement(studentQuery);
            ps4.setInt(1, sectionId);
            ResultSet rs4 = ps4.executeQuery();
            
            List<StudentRankingDetail> studentList = new ArrayList<>();
            
            while (rs4.next()) {
                StudentRankingDetail student = new StudentRankingDetail();
                student.rollNumber = rs4.getString("roll_number");
                student.studentName = rs4.getString("student_name");
                int studentId = rs4.getInt("id");
                
                double totalWeightedMarks = 0;
                int subjectCount = 0;
                
                // Get marks for each subject using weighted calculation
                Map<String, Map<String, Integer>> studentMarks = getStudentMarks(studentId, sectionId);
                
                for (SubjectInfoDetailed subject : data.subjects) {
                    // Skip if not in filter
                    if (selectedFilters != null && !selectedFilters.isEmpty() && !selectedFilters.containsKey(subject.subjectName)) {
                        continue;
                    }
                    
                    Map<String, Double> subjectMarksMap = new HashMap<>();
                    Map<String, Integer> marksForSubject = studentMarks.get(subject.subjectName);
                    
                    if (marksForSubject != null) {
                        for (String examType : subject.examTypes) {
                            Integer marks = marksForSubject.get(examType);
                            if (marks != null) {
                                subjectMarksMap.put(examType, marks.doubleValue());
                            }
                        }
                    }
                    
                    student.subjectMarks.put(subject.subjectName, subjectMarksMap);
                    
                    // Calculate WEIGHTED TOTAL for this subject using DUAL PASSING
                    Set<String> examTypesFilter = (selectedFilters != null) ? selectedFilters.get(subject.subjectName) : null;
                    SubjectPassResult result = calculateWeightedSubjectTotalWithPass(studentId, sectionId, subject.subjectName, examTypesFilter);
                    
                    // Include all subjects - track pass/fail for grade calculation
                    totalWeightedMarks += Math.abs(result.percentage);
                    subjectCount++;
                    
                    // Count failed subjects (component failure OR total < 40)
                    if (!result.passed) {
                        // Student failed this subject
                    }
                }
                
                // Store total weighted marks (sum across all subjects)
                student.totalMarks = totalWeightedMarks;
                
                // Calculate percentage (average across subjects)
                student.percentage = subjectCount > 0 ? (totalWeightedMarks / subjectCount) : 0.0;
                
                // Check if student failed ANY subject using batch calculation
                int userId = 0;
                try {
                    String userQuery = "SELECT created_by FROM students WHERE section_id = ? LIMIT 1";
                    PreparedStatement psUser = conn.prepareStatement(userQuery);
                    psUser.setInt(1, sectionId);
                    ResultSet rsUser = psUser.executeQuery();
                    if (rsUser.next()) userId = rsUser.getInt("created_by");
                    rsUser.close();
                    psUser.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                
                Map<Integer, Double> batchPercentages = calculateAllStudentPercentagesBatch(sectionId, userId, selectedFilters);
                Double studentBatchPercentage = batchPercentages.get(studentId);
                boolean studentFailed = (studentBatchPercentage != null && studentBatchPercentage < 0);
                
                // Calculate Grade and CGPA based on DUAL PASSING
                if (studentFailed) {
                    // Student FAILED at least one subject - Grade F, CGPA 0.0
                    student.grade = "F";
                    student.cgpa = 0.0;
                } else {
                    // Student PASSED all subjects - Calculate grade/CGPA from percentage
                    
                    // Calculate CGPA as percentage/10 (same as StudentAnalyzer)
                    student.cgpa = student.percentage / 10.0;
                    
                    // Determine grade based on percentage ranges
                    if (student.percentage >= 90) {
                        student.grade = "A+";
                    } else if (student.percentage >= 80) {
                        student.grade = "A";
                    } else if (student.percentage >= 70) {
                        student.grade = "B+";
                    } else if (student.percentage >= 60) {
                        student.grade = "B";
                    } else if (student.percentage >= 50) {
                        student.grade = "C";
                    } else {
                        student.grade = "F";
                        student.cgpa = 0.0; // Override for failing grade
                    }

                }
                
                studentList.add(student);
            }
            rs4.close();
            ps4.close();
            
            // Sort by WEIGHTED percentage (descending) and assign ranks
            studentList.sort((a, b) -> Double.compare(b.percentage, a.percentage));
            for (int i = 0; i < studentList.size(); i++) {
                studentList.get(i).rank = i + 1;
            }
            
            data.students = studentList;
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return data;
    }
    
    /**
     * Fetch marks from entered_exam_marks table (for result launch).
     * This is used when launching results - reads manual entry marks.
     * 
     * @param studentId Student ID
     * @param examTypeIds List of exam_type IDs (from exam_types table)
     * @return Map of exam_type_id to StudentComponentMark (reusing same structure)
     */
    public Map<Integer, StudentComponentMark> getStudentExamMarks(int studentId, List<Integer> examTypeIds) {
        Map<Integer, StudentComponentMark> marks = new HashMap<>();
        
        if (examTypeIds.isEmpty()) return marks;
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            String placeholders = String.join(",", Collections.nCopies(examTypeIds.size(), "?"));
            
            // Read from entered_exam_marks table with exam_type_id
            String query = "SELECT sm.exam_type_id, sm.marks_obtained, " +
                          "et.exam_name, et.max_marks, et.weightage " +
                          "FROM entered_exam_marks sm " +
                          "JOIN exam_types et ON sm.exam_type_id = et.id " +
                          "WHERE sm.student_id = ? AND sm.exam_type_id IN (" + placeholders + ")";
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, studentId);
            
            for (int i = 0; i < examTypeIds.size(); i++) {
                ps.setInt(i + 2, examTypeIds.get(i));
            }
            
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                StudentComponentMark mark = new StudentComponentMark();
                mark.componentId = rs.getInt("exam_type_id");
                mark.marksObtained = rs.getDouble("marks_obtained");
                mark.scaledMarks = rs.getDouble("marks_obtained"); // No scaling in simple system
                mark.isCounted = true; // All marks are counted
                mark.componentName = rs.getString("exam_name");
                mark.maxMarks = rs.getInt("max_marks");
                mark.scaledToMarks = rs.getInt("max_marks");
                
                marks.put(mark.componentId, mark);
            }
            
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return marks;
    }
}
