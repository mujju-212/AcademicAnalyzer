package com.sms.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//import com.sms.dashboard.dialogs.CreateSectionDialog;
import com.sms.database.DatabaseConnection;
import com.sms.marking.models.ComponentGroup;
import com.sms.marking.models.MarkingComponent;
import com.sms.marking.models.MarkingScheme;
import com.sms.marking.dao.MarkingSchemeDAO;

public class SectionDAO {
    
    // ... (keeping all the inner classes as they were)
    
    public static class SubjectInfo {
        public String subjectName;
        public int totalMarks;
        public int credit;
        public int passMarks;
        
        // Add this field for the new constructor
        public String name;
        
        public SubjectInfo(String subjectName, int totalMarks, int credit) {
            this.subjectName = subjectName;
            this.name = subjectName; // For compatibility
            this.totalMarks = totalMarks;
            this.credit = credit;
            this.passMarks = (int)(totalMarks * 0.4); // Default 40% passing marks
        }
        
        public SubjectInfo(String subjectName, int totalMarks, int credit, int passMarks) {
            this.subjectName = subjectName;
            this.name = subjectName; // For compatibility
            this.totalMarks = totalMarks;
            this.credit = credit;
            this.passMarks = passMarks;
        }
    }
    
    public static class SectionInfo {
        public int id;
        public String sectionName;
        public int totalStudents;
        public List<SubjectInfo> subjects;
        
        public SectionInfo(int id, String sectionName, int totalStudents) {
            this.id = id;
            this.sectionName = sectionName;
            this.totalStudents = totalStudents;
            this.subjects = new ArrayList<>();
        }
    }
    
    public static class ExamTypeInfo {
        public String examName;
        public String type;
        public int weightage;
        
        public ExamTypeInfo(String examName, String type, int weightage) {
            this.examName = examName;
            this.type = type;
            this.weightage = weightage;
        }
    }
    
    public static class MarkDistribution {
        public String examType;
        public int maxMarks;
        public double weightage;
        
        public MarkDistribution(String examType, int maxMarks, double weightage) {
            this.examType = examType;
            this.maxMarks = maxMarks;
            this.weightage = weightage;
        }
    }
    
    // Helper method to close connection safely
    private void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    public boolean createSection(String sectionName, List<SubjectInfo> subjects, int totalStudents, int userId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            // Check if section already exists for this user
            String checkQuery = "SELECT COUNT(*) FROM sections WHERE section_name = ? AND created_by = ?";
            ps = conn.prepareStatement(checkQuery);
            ps.setString(1, sectionName);
            ps.setInt(2, userId);
            rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                System.err.println("Section already exists: " + sectionName);
                return false;
            }
            rs.close();
            ps.close();
            
            // 1. Insert section
            String insertSection = "INSERT INTO sections (section_name, total_students, created_by) VALUES (?, ?, ?)";
            ps = conn.prepareStatement(insertSection, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, sectionName);
            ps.setInt(2, totalStudents);
            ps.setInt(3, userId);
            
            int rowsAffected = ps.executeUpdate();
            
            // Get the generated section ID
            rs = ps.getGeneratedKeys();
            int sectionId = 0;
            if (rs.next()) {
                sectionId = rs.getInt(1);
            } else {
                throw new SQLException("Failed to get generated section ID");
            }
            rs.close();
            ps.close();
            
            // 2. For each subject, check if it exists or create it
            for (SubjectInfo subjectInfo : subjects) {
                int subjectId = getOrCreateSubject(conn, subjectInfo.subjectName, userId);
                
                // 3. Create section-subject mapping
                String insertMapping = "INSERT INTO section_subjects (section_id, subject_id, max_marks, passing_marks, credit) VALUES (?, ?, ?, ?, ?)";
                ps = conn.prepareStatement(insertMapping);
                ps.setInt(1, sectionId);
                ps.setInt(2, subjectId);
                ps.setInt(3, subjectInfo.totalMarks);
                ps.setInt(4, subjectInfo.passMarks);
                ps.setInt(5, subjectInfo.credit);
                ps.executeUpdate();
                ps.close();
            }
            
            conn.commit();
            return true;
            
        } catch (SQLException e) {
            System.err.println("SQL Error in createSection: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                    closeConnection(conn);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    private int getOrCreateSubject(Connection conn, String subjectName, int userId) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            // Check if subject exists
            String checkQuery = "SELECT id FROM subjects WHERE subject_name = ?";
            ps = conn.prepareStatement(checkQuery);
            ps.setString(1, subjectName);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("id");
            }
            rs.close();
            ps.close();
            
            // Create new subject
            String insertQuery = "INSERT INTO subjects (subject_name, subject_code) VALUES (?, ?)";
            ps = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, subjectName);
            ps.setString(2, generateSubjectCode(subjectName));
            ps.executeUpdate();
            
            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                throw new SQLException("Failed to create subject: " + subjectName);
            }
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
    }
    
    private String generateSubjectCode(String subjectName) {
        String code = subjectName.replaceAll("\\s+", "").toUpperCase();
        if (code.length() > 3) {
            code = code.substring(0, 3);
        }
        return code + "101";
    }
    
 // In SectionDAO.java, update the createSectionWithDetailedMarks method:

    public boolean createSectionWithDetailedMarks(String sectionName, 
            List<SubjectInfo> subjects,
            List<ExamTypeInfo> examTypes,
            Map<String, List<MarkDistribution>> distributions,
            int totalStudents, 
            int userId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            // 1. Create section
            String sectionQuery = "INSERT INTO sections (section_name, total_students, created_by) VALUES (?, ?, ?)";
            ps = conn.prepareStatement(sectionQuery, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, sectionName);
            ps.setInt(2, totalStudents);
            ps.setInt(3, userId);
            ps.executeUpdate();
            
            rs = ps.getGeneratedKeys();
            int sectionId = 0;
            if (rs.next()) {
                sectionId = rs.getInt(1);
                System.out.println("Created section with ID: " + sectionId);
            }
            rs.close();
            ps.close();
            
            // 2. Create exam types for this section - THIS WAS MISSING OR INCORRECT
            Map<String, Integer> examTypeIds = new HashMap<>();
            
            if (examTypes != null && !examTypes.isEmpty()) {
                System.out.println("Inserting " + examTypes.size() + " exam types...");
                
                String examTypeQuery = "INSERT INTO exam_types (section_id, exam_name, weightage, created_by, created_at) VALUES (?, ?, ?, ?, NOW())";
                ps = conn.prepareStatement(examTypeQuery, Statement.RETURN_GENERATED_KEYS);
                
                for (ExamTypeInfo examType : examTypes) {
                    ps.setInt(1, sectionId);
                    ps.setString(2, examType.examName);
                    ps.setInt(3, examType.weightage);
                    ps.setInt(4, userId);
                    
                    System.out.println("Inserting exam type: " + examType.examName + " with weightage: " + examType.weightage);
                    
                    ps.executeUpdate();
                    
                    rs = ps.getGeneratedKeys();
                    if (rs.next()) {
                        int examTypeId = rs.getInt(1);
                        examTypeIds.put(examType.examName, examTypeId);
                        System.out.println("  Created exam type ID: " + examTypeId);
                    }
                    rs.close();
                }
                ps.close();
            } else {
                System.out.println("WARNING: No exam types provided!");
            }
            
            // 3. Create subjects and their mark distributions
            for (SubjectInfo subject : subjects) {
                // Get or create subject
                int subjectId = getOrCreateSubject(conn, subject.subjectName, userId);
                
                // Link subject to section
                String linkQuery = "INSERT INTO section_subjects (section_id, subject_id, max_marks, credit, passing_marks) VALUES (?, ?, ?, ?, ?)";
                ps = conn.prepareStatement(linkQuery);
                ps.setInt(1, sectionId);
                ps.setInt(2, subjectId);
                ps.setInt(3, subject.totalMarks);
                ps.setInt(4, subject.credit);
                ps.setInt(5, subject.passMarks);
                ps.executeUpdate();
                ps.close();
                
                // Insert mark distributions if provided
                List<MarkDistribution> subjectDist = distributions.get(subject.subjectName);
                if (subjectDist != null && !subjectDist.isEmpty()) {
                    // First check if subject_mark_distribution table exists
                    try {
                        String distQuery = "INSERT INTO subject_mark_distribution (section_id, subject_id, exam_type_id, max_marks, weightage) VALUES (?, ?, ?, ?, ?)";
                        ps = conn.prepareStatement(distQuery);
                        
                        for (MarkDistribution dist : subjectDist) {
                            Integer examTypeId = examTypeIds.get(dist.examType);
                            if (examTypeId != null) {
                                ps.setInt(1, sectionId);
                                ps.setInt(2, subjectId);
                                ps.setInt(3, examTypeId);
                                ps.setInt(4, dist.maxMarks);
                                ps.setDouble(5, dist.weightage);
                                ps.addBatch();
                            }
                        }
                        ps.executeBatch();
                        ps.close();
                    } catch (SQLException e) {
                        // Table might not exist, continue without distributions
                        System.out.println("subject_mark_distribution table might not exist: " + e.getMessage());
                    }
                }
            }
            
            // 4. Create students
            String studentQuery = "INSERT INTO students (roll_number, student_name, section_id, created_by) VALUES (?, ?, ?, ?)";
            ps = conn.prepareStatement(studentQuery);
            
            for (int i = 1; i <= totalStudents; i++) {
                String rollNumber = String.format("%03d", i);
                String studentName = "Student " + i;
                
                ps.setString(1, rollNumber);
                ps.setString(2, studentName);
                ps.setInt(3, sectionId);
                ps.setInt(4, userId);
                ps.addBatch();
            }
            
            ps.executeBatch();
            ps.close();
            
            conn.commit();
            System.out.println("Section created successfully with " + examTypeIds.size() + " exam types");
            return true;
            
        } catch (SQLException e) {
            System.err.println("Error creating section: " + e.getMessage());
            e.printStackTrace();
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return false;
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                    closeConnection(conn);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    public boolean createSectionWithExams(String sectionName, ArrayList<SubjectInfo> subjects, 
            ArrayList<ExamTypeInfo> examTypes,  // Changed from CreateSectionDialog.ExamTypeInfo
            int totalStudents, int userId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            // Check if section already exists
            String checkQuery = "SELECT id FROM sections WHERE section_name = ? AND created_by = ?";
            ps = conn.prepareStatement(checkQuery);
            ps.setString(1, sectionName);
            ps.setInt(2, userId);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                conn.rollback();
                return false;
            }
            rs.close();
            ps.close();
            
            // Insert section
            String sectionQuery = "INSERT INTO sections (section_name, total_students, created_by) VALUES (?, ?, ?)";
            ps = conn.prepareStatement(sectionQuery, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, sectionName);
            ps.setInt(2, totalStudents);
            ps.setInt(3, userId);
            ps.executeUpdate();
            
            rs = ps.getGeneratedKeys();
            int sectionId = 0;
            if (rs.next()) {
                sectionId = rs.getInt(1);
            }
            rs.close();
            ps.close();
            
            // Insert subjects
            for (SubjectInfo subject : subjects) {
                // Get or create subject
                int subjectId = getOrCreateSubject(conn, subject.subjectName, userId);
                
                // Link subject to section
                String linkQuery = "INSERT INTO section_subjects (section_id, subject_id, max_marks, credit, passing_marks) VALUES (?, ?, ?, ?, ?)";
                ps = conn.prepareStatement(linkQuery);
                ps.setInt(1, sectionId);
                ps.setInt(2, subjectId);
                ps.setInt(3, subject.totalMarks);
                ps.setInt(4, subject.credit);
                ps.setInt(5, subject.passMarks);
                ps.executeUpdate();
                ps.close();
            }
            
            // Insert exam types
            String examQuery = "INSERT INTO exam_types (section_id, exam_name, weightage, created_by) VALUES (?, ?, ?, ?)";
            ps = conn.prepareStatement(examQuery);
            
            for (ExamTypeInfo examType : examTypes) {  // Now using local ExamTypeInfo
                ps.setInt(1, sectionId);
                ps.setString(2, examType.examName);
                ps.setInt(3, examType.weightage);
                ps.setInt(4, userId);
                ps.addBatch();
            }
            ps.executeBatch();
            ps.close();
            
            conn.commit();
            return true;
            
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                    closeConnection(conn);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    // Fixed method signature - added return type
 // In SectionDAO.java, update the createSectionWithFlexibleMarking method:

    public int createSectionWithFlexibleMarking(String sectionName, 
            List<SubjectInfo> subjects,
            Map<String, MarkingScheme> markingSchemes,
            int totalStudents, 
            int userId) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            String sectionQuery = "INSERT INTO sections (section_name, total_students, created_by, marking_type) VALUES (?, ?, ?, ?)";
            pstmt = conn.prepareStatement(sectionQuery, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, sectionName);
            pstmt.setInt(2, totalStudents);
            pstmt.setInt(3, userId);
            pstmt.setString(4, "FLEXIBLE");

            pstmt.executeUpdate();
            
            rs = pstmt.getGeneratedKeys();
            if (!rs.next()) {
                throw new SQLException("Failed to create section");
            }
            
            int sectionId = rs.getInt(1);
            rs.close();
            pstmt.close();
            
            // 2. Collect all unique components across all subjects to create as exam types
            Set<String> uniqueComponents = new HashSet<>();
            Map<String, Integer> componentMaxMarks = new HashMap<>();
            
            for (MarkingScheme scheme : markingSchemes.values()) {
                for (ComponentGroup group : scheme.getInternalGroups()) {
                    for (MarkingComponent comp : group.getComponents()) {
                        String componentName = comp.getComponentName();
                        uniqueComponents.add(componentName);
                        // Store the max marks for this component
                        componentMaxMarks.put(componentName, comp.getActualMaxMarks());
                    }
                }
                for (ComponentGroup group : scheme.getExternalGroups()) {
                    for (MarkingComponent comp : group.getComponents()) {
                        String componentName = comp.getComponentName();
                        uniqueComponents.add(componentName);
                        componentMaxMarks.put(componentName, comp.getActualMaxMarks());
                    }
                }
            }
            
            // 3. Create exam types for each unique component
            System.out.println("Creating exam types for flexible components...");
            String examTypeQuery = "INSERT INTO exam_types (section_id, exam_name, weightage, created_by) VALUES (?, ?, ?, ?)";
            pstmt = conn.prepareStatement(examTypeQuery);
            
            for (String componentName : uniqueComponents) {
                pstmt.setInt(1, sectionId);
                pstmt.setString(2, componentName);
                pstmt.setInt(3, 0); // Weightage will be calculated based on actual marks
                pstmt.setInt(4, userId);
                pstmt.addBatch();
                
                System.out.println("Adding exam type: " + componentName);
            }
            
            pstmt.executeBatch();
            pstmt.close();
            
            // 4. Create subjects and marking schemes (existing code)
            Map<String, Integer> subjectIdMap = new HashMap<>();
            
            for (SubjectInfo subject : subjects) {
                // Check if subject exists
                String checkQuery = "SELECT id FROM subjects WHERE subject_name = ?";
                pstmt = conn.prepareStatement(checkQuery);
                pstmt.setString(1, subject.subjectName);
                rs = pstmt.executeQuery();
                
                int subjectId;
                if (rs.next()) {
                    subjectId = rs.getInt("id");
                } else {
                    // Create new subject
                    rs.close();
                    pstmt.close();
                    
                    String subjectQuery = "INSERT INTO subjects (subject_name) VALUES (?)";
                    pstmt = conn.prepareStatement(subjectQuery, Statement.RETURN_GENERATED_KEYS);
                    pstmt.setString(1, subject.subjectName);
                    pstmt.executeUpdate();
                    
                    rs = pstmt.getGeneratedKeys();
                    if (!rs.next()) {
                        throw new SQLException("Failed to create subject: " + subject.subjectName);
                    }
                    subjectId = rs.getInt(1);
                }
                
                subjectIdMap.put(subject.subjectName, subjectId);
                rs.close();
                pstmt.close();
                
                // Link subject to section with flexible marking flag
                String linkQuery = "INSERT INTO section_subjects (section_id, subject_id, max_marks, " +
                                 "passing_marks, credit, use_new_marking_system) VALUES (?, ?, ?, ?, ?, TRUE)";
                pstmt = conn.prepareStatement(linkQuery);
                pstmt.setInt(1, sectionId);
                pstmt.setInt(2, subjectId);
                pstmt.setInt(3, subject.totalMarks);
                pstmt.setInt(4, subject.passMarks);
                pstmt.setInt(5, subject.credit);
                pstmt.executeUpdate();
                pstmt.close();
            }
            
            // 5. Create marking schemes
            MarkingSchemeDAO schemeDAO = new MarkingSchemeDAO();
            
            for (Map.Entry<String, MarkingScheme> entry : markingSchemes.entrySet()) {
                String subjectName = entry.getKey();
                MarkingScheme scheme = entry.getValue();
                
                Integer subjectId = subjectIdMap.get(subjectName);
                if (subjectId == null) {
                    throw new SQLException("Subject not found: " + subjectName);
                }
                
                // Set section and subject IDs
                scheme.setSectionId(sectionId);
                scheme.setSubjectId(subjectId);
                scheme.setCreatedBy(userId);
                
                // Save marking scheme
                int schemeId = schemeDAO.createMarkingScheme(scheme);
                
                // Update section_subjects with marking_scheme_id
                String updateQuery = "UPDATE section_subjects SET marking_scheme_id = ? " +
                                   "WHERE section_id = ? AND subject_id = ?";
                pstmt = conn.prepareStatement(updateQuery);
                pstmt.setInt(1, schemeId);
                pstmt.setInt(2, sectionId);
                pstmt.setInt(3, subjectId);
                pstmt.executeUpdate();
                pstmt.close();
            }
            
            // 6. Create students
            String studentQuery = "INSERT INTO students (roll_number, student_name, section_id, created_by) VALUES (?, ?, ?, ?)";
            pstmt = conn.prepareStatement(studentQuery);
            
            for (int i = 1; i <= totalStudents; i++) {
                String rollNumber = String.format("%03d", i);
                String studentName = "Student " + i;
                
                pstmt.setString(1, rollNumber);
                pstmt.setString(2, studentName);
                pstmt.setInt(3, sectionId);
                pstmt.setInt(4, userId);
                pstmt.addBatch();
            }
            
            pstmt.executeBatch();
            pstmt.close();
            
            conn.commit();
            System.out.println("Section created with " + uniqueComponents.size() + " exam types from flexible components");
            return sectionId;
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) { }
            if (conn != null) try { conn.close(); } catch (SQLException e) { }
        }
    }
    
    // Method to check if a section uses flexible marking
    public boolean usesFlexibleMarking(int sectionId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT use_new_marking_system FROM section_subjects " +
                          "WHERE section_id = ? LIMIT 1";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, sectionId);
            ResultSet rs = ps.executeQuery();
            
            return rs.next() && rs.getBoolean(1);
        }
    }
    
    // Get marking scheme for a section subject
    public MarkingScheme getMarkingScheme(int sectionId, int subjectId) throws SQLException {
        MarkingSchemeDAO schemeDAO = new MarkingSchemeDAO();
        return schemeDAO.getMarkingScheme(sectionId, subjectId);
    }
    
    public List<SectionInfo> getAllSections() {
        List<SectionInfo> sections = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            String query = "SELECT id, section_name, total_students FROM sections ORDER BY section_name";
            ps = conn.prepareStatement(query);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                SectionInfo section = new SectionInfo(
                    rs.getInt("id"),
                    rs.getString("section_name"),
                    rs.getInt("total_students")
                );
                sections.add(section);
            }
            
        } catch (SQLException e) {
            System.err.println("Error in getAllSections: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                closeConnection(conn);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return sections;
    }
    
    public List<SectionInfo> getSectionsByUser(int userId) {
        List<SectionInfo> sections = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            String query = "SELECT id, section_name, total_students FROM sections " +
                          "WHERE created_by = ? ORDER BY section_name";
            ps = conn.prepareStatement(query);
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                SectionInfo section = new SectionInfo(
                    rs.getInt("id"),
                    rs.getString("section_name"),
                    rs.getInt("total_students")
                );
                sections.add(section);
            }
            
        } catch (SQLException e) {
            System.err.println("Error in getSectionsByUser: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                closeConnection(conn);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return sections;
    }
    
    public boolean deleteSection(int sectionId, int userId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            // First verify that this section belongs to the user
            String verifyQuery = "SELECT COUNT(*) FROM sections WHERE id = ? AND created_by = ?";
            ps = conn.prepareStatement(verifyQuery);
            ps.setInt(1, sectionId);
            ps.setInt(2, userId);
            rs = ps.executeQuery();
            
            if (!rs.next() || rs.getInt(1) == 0) {
                System.err.println("Section not found or doesn't belong to user");
                return false;
            }
            rs.close();
            ps.close();
            
            // Delete in order due to foreign key constraints:
            
            // 1. Delete subject mark distributions
            String deleteDistributions = "DELETE FROM subject_mark_distribution WHERE section_id = ?";
            ps = conn.prepareStatement(deleteDistributions);
            ps.setInt(1, sectionId);
            ps.executeUpdate();
            ps.close();
            
            // 2. Delete student marks for students in this section
            String deleteMarks = "DELETE FROM student_marks WHERE student_id IN (SELECT id FROM students WHERE section_id = ?)";
            ps = conn.prepareStatement(deleteMarks);
            ps.setInt(1, sectionId);
            ps.executeUpdate();
            ps.close();
            
            // 3. Delete students in this section
            String deleteStudents = "DELETE FROM students WHERE section_id = ?";
            ps = conn.prepareStatement(deleteStudents);
            ps.setInt(1, sectionId);
            ps.executeUpdate();
            ps.close();
            
            // 4. Delete section-subject mappings
            String deleteSectionSubjects = "DELETE FROM section_subjects WHERE section_id = ?";
            ps = conn.prepareStatement(deleteSectionSubjects);
            ps.setInt(1, sectionId);
            ps.executeUpdate();
            ps.close();
            
            // 5. Delete exam types for this section
            String deleteExamTypes = "DELETE FROM exam_types WHERE section_id = ?";
            ps = conn.prepareStatement(deleteExamTypes);
            ps.setInt(1, sectionId);
            ps.executeUpdate();
            ps.close();
            
            // 6. Delete section exam types if exists
            String deleteSectionExamTypes = "DELETE FROM section_exam_types WHERE section_id = ?";
            ps = conn.prepareStatement(deleteSectionExamTypes);
            ps.setInt(1, sectionId);
            ps.executeUpdate();
            ps.close();
            
            // 7. Finally delete the section
            String deleteSection = "DELETE FROM sections WHERE id = ? AND created_by = ?";
            ps = conn.prepareStatement(deleteSection);
            ps.setInt(1, sectionId);
            ps.setInt(2, userId);
            int rowsAffected = ps.executeUpdate();
            
            conn.commit();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting section: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                    closeConnection(conn);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    public List<SubjectInfo> getSectionSubjects(int sectionId) {
        List<SubjectInfo> subjects = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            String query = "SELECT s.subject_name, ss.max_marks, ss.credit, ss.passing_marks " +
                          "FROM section_subjects ss " +
                          "JOIN subjects s ON ss.subject_id = s.id " +
                          "WHERE ss.section_id = ? " +
                          "ORDER BY s.subject_name";
            ps = conn.prepareStatement(query);
            ps.setInt(1, sectionId);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                subjects.add(new SubjectInfo(
                    rs.getString("subject_name"),
                    rs.getInt("max_marks"),
                    rs.getInt("credit"),
                    rs.getInt("passing_marks")
                ));
            }
            
        } catch (SQLException e) {
            System.err.println("Error in getSectionSubjects: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                closeConnection(conn);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return subjects;
    }
    
    // Additional helper method to get section details with subjects
    public SectionInfo getSectionWithSubjects(int sectionId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            
            // Get section info
            String sectionQuery = "SELECT id, section_name, total_students FROM sections WHERE id = ?";
            ps = conn.prepareStatement(sectionQuery);
            ps.setInt(1, sectionId);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                SectionInfo section = new SectionInfo(
                    rs.getInt("id"),
                    rs.getString("section_name"),
                    rs.getInt("total_students")
                );
                
                // Get subjects for this section
                section.subjects = getSectionSubjects(sectionId);
                
                return section;
            }
            
        } catch (SQLException e) {
            System.err.println("Error in getSectionWithSubjects: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                closeConnection(conn);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    
    // Method to check if a section name already exists for a user
    public boolean sectionExists(String sectionName, int userId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            String query = "SELECT COUNT(*) FROM sections WHERE section_name = ? AND created_by = ?";
            ps = conn.prepareStatement(query);
            ps.setString(1, sectionName);
            ps.setInt(2, userId);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error in sectionExists: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                closeConnection(conn);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}