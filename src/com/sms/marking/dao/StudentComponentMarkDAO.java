package com.sms.marking.dao;

import com.sms.marking.models.*;
import com.sms.database.DatabaseConnection;
import java.sql.*;
import java.util.*;
public class StudentComponentMarkDAO {
    
    // Save or update student marks for a component
    public boolean saveStudentMark(StudentComponentMark mark) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            
            // Check if mark already exists
            if (markExists(conn, mark.getStudentId(), mark.getComponentId())) {
                // Update existing mark
                String updateQuery = "UPDATE student_component_marks SET marks_obtained = ?, " +
                                   "status = ?, entered_by = ?, entered_at = NOW() " +
                                   "WHERE student_id = ? AND component_id = ?";
                
                pstmt = conn.prepareStatement(updateQuery);
                
                if (mark.getMarksObtained() != null) {
                    pstmt.setDouble(1, mark.getMarksObtained());
                } else {
                    pstmt.setNull(1, Types.DECIMAL);
                }
                
                pstmt.setString(2, mark.getStatus());
                pstmt.setInt(3, mark.getEnteredBy());
                pstmt.setInt(4, mark.getStudentId());
                pstmt.setInt(5, mark.getComponentId());
                
            } else {
                // Insert new mark
                String insertQuery = "INSERT INTO student_component_marks " +
                                   "(student_id, component_id, marks_obtained, status, entered_by) " +
                                   "VALUES (?, ?, ?, ?, ?)";
                
                pstmt = conn.prepareStatement(insertQuery);
                pstmt.setInt(1, mark.getStudentId());
                pstmt.setInt(2, mark.getComponentId());
                
                if (mark.getMarksObtained() != null) {
                    pstmt.setDouble(3, mark.getMarksObtained());
                } else {
                    pstmt.setNull(3, Types.DECIMAL);
                }
                
                pstmt.setString(4, mark.getStatus());
                pstmt.setInt(5, mark.getEnteredBy());
            }
            
            return pstmt.executeUpdate() > 0;
            
        } finally {
            DatabaseConnection.closeResources(conn, pstmt, null);
        }
    }
    
    // Check if mark exists
    private boolean markExists(Connection conn, int studentId, int componentId) throws SQLException {
        String query = "SELECT COUNT(*) FROM student_component_marks WHERE student_id = ? AND component_id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, componentId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        
        return false;
    }
    
    // Get all marks for a student in a subject
    public List<StudentComponentMark> getStudentMarks(int studentId, int subjectId) throws SQLException {
        List<StudentComponentMark> marks = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            
            String query = "SELECT scm.*, mc.component_name, mc.actual_max_marks, mc.component_type " +
                          "FROM student_component_marks scm " +
                          "JOIN marking_components mc ON scm.component_id = mc.id " +
                          "JOIN marking_schemes ms ON mc.scheme_id = ms.id " +
                          "WHERE scm.student_id = ? AND ms.subject_id = ? " +
                          "ORDER BY mc.sequence_order";
            
            pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, subjectId);
            
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                StudentComponentMark mark = new StudentComponentMark();
                mark.setId(rs.getInt("id"));
                mark.setStudentId(rs.getInt("student_id"));
                mark.setComponentId(rs.getInt("component_id"));
                
                Double marksObtained = rs.getDouble("marks_obtained");
                if (!rs.wasNull()) {
                    mark.setMarksObtained(marksObtained);
                }
                
                mark.setStatus(rs.getString("status"));
                mark.setEnteredAt(rs.getTimestamp("entered_at").toLocalDateTime());
                mark.setEnteredBy(rs.getInt("entered_by"));
                
                // Create component info
                MarkingComponent component = new MarkingComponent();
                component.setId(rs.getInt("component_id"));
                component.setComponentName(rs.getString("component_name"));
                component.setActualMaxMarks(rs.getInt("actual_max_marks"));
                component.setComponentType(rs.getString("component_type"));
                mark.setComponent(component);
                
                marks.add(mark);
            }
            
            return marks;
            
        } finally {
            DatabaseConnection.closeResources(conn, pstmt, rs);
        }
    }
    
    // Get marks for all students in a section for a specific component
    public Map<Integer, StudentComponentMark> getComponentMarksForSection(int sectionId, int componentId) throws SQLException {
        Map<Integer, StudentComponentMark> marksMap = new HashMap<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            
            String query = "SELECT scm.*, s.roll_number, s.student_name " +
                          "FROM student_component_marks scm " +
                          "JOIN students s ON scm.student_id = s.id " +
                          "WHERE s.section_id = ? AND scm.component_id = ? " +
                          "ORDER BY s.roll_number";
            
            pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, sectionId);
            pstmt.setInt(2, componentId);
            
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                StudentComponentMark mark = new StudentComponentMark();
                mark.setId(rs.getInt("id"));
                mark.setStudentId(rs.getInt("student_id"));
                mark.setComponentId(rs.getInt("component_id"));
                
                Double marksObtained = rs.getDouble("marks_obtained");
                if (!rs.wasNull()) {
                    mark.setMarksObtained(marksObtained);
                }
                
                mark.setStatus(rs.getString("status"));
                mark.setEnteredAt(rs.getTimestamp("entered_at").toLocalDateTime());
                mark.setEnteredBy(rs.getInt("entered_by"));
                
                marksMap.put(mark.getStudentId(), mark);
            }
            
            return marksMap;
            
        } finally {
            DatabaseConnection.closeResources(conn, pstmt, rs);
        }
    }
    
    // Batch save marks for multiple students
    public boolean saveMultipleMarks(List<StudentComponentMark> marks) throws SQLException {
        if (marks == null || marks.isEmpty()) {
            return true;
        }
        
        Connection conn = null;
        PreparedStatement insertPstmt = null;
        PreparedStatement updatePstmt = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            String insertQuery = "INSERT INTO student_component_marks " +
                               "(student_id, component_id, marks_obtained, status, entered_by) " +
                               "VALUES (?, ?, ?, ?, ?)";
            
            String updateQuery = "UPDATE student_component_marks SET marks_obtained = ?, " +
                               "status = ?, entered_by = ?, entered_at = NOW() " +
                               "WHERE student_id = ? AND component_id = ?";
            
            insertPstmt = conn.prepareStatement(insertQuery);
            updatePstmt = conn.prepareStatement(updateQuery);
            
            for (StudentComponentMark mark : marks) {
                if (markExists(conn, mark.getStudentId(), mark.getComponentId())) {
                    // Update
                    if (mark.getMarksObtained() != null) {
                        updatePstmt.setDouble(1, mark.getMarksObtained());
                    } else {
                        updatePstmt.setNull(1, Types.DECIMAL);
                    }
                    
                    updatePstmt.setString(2, mark.getStatus());
                    updatePstmt.setInt(3, mark.getEnteredBy());
                    updatePstmt.setInt(4, mark.getStudentId());
                    updatePstmt.setInt(5, mark.getComponentId());
                    
                    updatePstmt.addBatch();
                } else {
                    // Insert
                    insertPstmt.setInt(1, mark.getStudentId());
                    insertPstmt.setInt(2, mark.getComponentId());
                    
                    if (mark.getMarksObtained() != null) {
                        insertPstmt.setDouble(3, mark.getMarksObtained());
                    } else {
                        insertPstmt.setNull(3, Types.DECIMAL);
                    }
                    
                    insertPstmt.setString(4, mark.getStatus());
                    insertPstmt.setInt(5, mark.getEnteredBy());
                    
                    insertPstmt.addBatch();
                }
            }
            
            insertPstmt.executeBatch();
            updatePstmt.executeBatch();
            
            conn.commit();
            return true;
            
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
            DatabaseConnection.closeResources(null, insertPstmt, null);
            DatabaseConnection.closeResources(conn, updatePstmt, null);
        }
    }
    
    // Calculate final marks for a student in a subject
    public StudentSubjectMarks calculateStudentMarks(int studentId, int subjectId) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            
            // Get marking scheme
            MarkingSchemeDAO schemeDAO = new MarkingSchemeDAO();
            
            // First get section_id for the student
            String sectionQuery = "SELECT section_id FROM students WHERE id = ?";
            int sectionId = 0;
            
            pstmt = conn.prepareStatement(sectionQuery);
            pstmt.setInt(1, studentId);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                sectionId = rs.getInt("section_id");
            }
            rs.close();
            pstmt.close();
            
            MarkingScheme scheme = schemeDAO.getMarkingScheme(sectionId, subjectId);
            if (scheme == null) {
                return null;
            }
            
            // Get all student marks for this subject
            Map<Integer, Double> componentMarks = new HashMap<>();
            
            String marksQuery = "SELECT component_id, marks_obtained, status " +
                              "FROM student_component_marks " +
                              "WHERE student_id = ? AND component_id IN " +
                              "(SELECT id FROM marking_components WHERE scheme_id = ?)";
            
            pstmt = conn.prepareStatement(marksQuery);
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, scheme.getId());
            
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                if ("present".equals(rs.getString("status"))) {
                    Double marks = rs.getDouble("marks_obtained");
                    if (!rs.wasNull()) {
                        componentMarks.put(rs.getInt("component_id"), marks);
                    }
                }
            }
            
            // Calculate marks for each group
            StudentSubjectMarks result = new StudentSubjectMarks();
            result.setStudentId(studentId);
            result.setSubjectId(subjectId);
            
            double totalInternal = 0;
            double totalExternal = 0;
            
            for (ComponentGroup group : scheme.getComponentGroups()) {
                double groupMarks = group.calculateGroupMarks(componentMarks);
                
                if ("internal".equals(group.getGroupType())) {
                    totalInternal += groupMarks;
                } else {
                    totalExternal += groupMarks;
                }
                
                result.addGroupMarks(group.getGroupName(), groupMarks);
            }
            
            result.setInternalMarks(totalInternal);
            result.setExternalMarks(totalExternal);
            result.setTotalMarks(totalInternal + totalExternal);
            
            return result;
            
        } finally {
            DatabaseConnection.closeResources(conn, pstmt, rs);
        }
    }
    
    // Inner class for student subject marks summary
    public static class StudentSubjectMarks {
        private int studentId;
        private int subjectId;
        private double internalMarks;
        private double externalMarks;
        private double totalMarks;
        private Map<String, Double> groupMarks;
        
        public StudentSubjectMarks() {
            this.groupMarks = new HashMap<>();
        }
        
        // Getters and setters
        public int getStudentId() { return studentId; }
        public void setStudentId(int studentId) { this.studentId = studentId; }
        
        public int getSubjectId() { return subjectId; }
        public void setSubjectId(int subjectId) { this.subjectId = subjectId; }
        
        public double getInternalMarks() { return internalMarks; }
        public void setInternalMarks(double internalMarks) { this.internalMarks = internalMarks; }
        
        public double getExternalMarks() { return externalMarks; }
        public void setExternalMarks(double externalMarks) { this.externalMarks = externalMarks; }
        
        public double getTotalMarks() { return totalMarks; }
        public void setTotalMarks(double totalMarks) { this.totalMarks = totalMarks; }
        
        public Map<String, Double> getGroupMarks() { return groupMarks; }
        public void addGroupMarks(String groupName, double marks) {
            groupMarks.put(groupName, marks);
        }
    }
}