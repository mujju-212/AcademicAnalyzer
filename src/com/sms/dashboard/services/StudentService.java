package com.sms.dashboard.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.sms.database.DatabaseConnection;

/**
 * Service class for student-related database operations
 */
public class StudentService {
    
    /**
     * Gets the total count of students for a specific user
     */
    public int getTotalStudentsCount(int userId) {
        String query = "SELECT COUNT(*) as count FROM students WHERE created_by = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("count") : 0;
            }
        } catch (SQLException e) {
            System.err.println("Error getting total students count: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * Gets the count of students who have marks recorded
     */
    public int getStudentsWithMarksCount(int userId) {
        String query = "SELECT COUNT(DISTINCT sm.student_id) as count " +
                      "FROM entered_exam_marks sm " +
                      "INNER JOIN students s ON sm.student_id = s.id " +
                      "WHERE sm.created_by = ? " +
                      "AND sm.marks_obtained IS NOT NULL " +
                      "AND sm.marks_obtained > 0";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("count") : 0;
            }
        } catch (SQLException e) {
            System.err.println("Error getting students with marks count: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * Gets the top performing student name
     */
    public String getTopStudent(int userId) {
        String query = "SELECT s.student_name, AVG((sm.marks_obtained / ss.max_marks) * 100) as avg_percentage " +
                      "FROM students s " +
                      "INNER JOIN entered_exam_marks sm ON s.id = sm.student_id " +
                      "INNER JOIN section_subjects ss ON sm.subject_id = ss.subject_id AND s.section_id = ss.section_id " +
                      "WHERE s.created_by = ? " +
                      "AND sm.marks_obtained IS NOT NULL AND ss.max_marks > 0 " +
                      "GROUP BY s.id, s.student_name " +
                      "ORDER BY avg_percentage DESC " +
                      "LIMIT 1";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String topStudent = rs.getString("student_name");
                    double percentage = rs.getDouble("avg_percentage");
                    System.out.println("Top student: " + topStudent + " with " + percentage + "%");
                    
                    if (topStudent != null && topStudent.length() > 15) {
                        topStudent = topStudent.substring(0, 12) + "...";
                    }
                    return topStudent;
                }
                return "No data";
            }
        } catch (SQLException e) {
            System.err.println("Error getting top student: " + e.getMessage());
            e.printStackTrace();
            return "N/A";
        }
    }
    
    /**
     * Gets the count of students in a specific section
     */
    public int getStudentCountForSection(int sectionId, int userId) {
        String query = "SELECT COUNT(*) FROM students WHERE section_id = ? AND created_by = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setInt(1, sectionId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            System.err.println("Error getting student count for section: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * Gets the count of students in a specific section (overload without userId)
     */
    public int getStudentCountForSection(int sectionId) {
        String query = "SELECT COUNT(*) FROM students WHERE section_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setInt(1, sectionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            System.err.println("Error getting student count for section: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
}