package com.sms.dao;

import java.sql.*;
import com.sms.database.DatabaseConnection;

/**
 * Helper class for section editing and deletion operations
 */
public class SectionEditDAO {
    
    /**
     * Delete a section and all its associated data
     * @param sectionId Section ID to delete
     * @param userId User ID for verification
     * @return true if successful, false otherwise
     */
    public boolean deleteSection(int sectionId, int userId) {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            // Verify ownership
            String verifyQuery = "SELECT COUNT(*) FROM sections WHERE id = ? AND created_by = ?";
            ps = conn.prepareStatement(verifyQuery);
            ps.setInt(1, sectionId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next() || rs.getInt(1) == 0) {
                System.err.println("Section not found or unauthorized: " + sectionId);
                return false;
            }
            rs.close();
            ps.close();
            
            // Delete student marks (via student_id)
            String deleteMarks = "DELETE FROM student_marks WHERE student_id IN (SELECT id FROM students WHERE section_id = ?)";
            ps = conn.prepareStatement(deleteMarks);
            ps.setInt(1, sectionId);
            ps.executeUpdate();
            ps.close();
            
            // Delete component marks if exists (via student_id)
            String deleteComponentMarks = "DELETE FROM student_component_marks WHERE student_id IN (SELECT id FROM students WHERE section_id = ?)";
            ps = conn.prepareStatement(deleteComponentMarks);
            ps.setInt(1, sectionId);
            ps.executeUpdate();
            ps.close();
            
            // Delete students
            String deleteStudents = "DELETE FROM students WHERE section_id = ?";
            ps = conn.prepareStatement(deleteStudents);
            ps.setInt(1, sectionId);
            ps.executeUpdate();
            ps.close();
            
            // Delete section-subject mappings
            String deleteMappings = "DELETE FROM section_subjects WHERE section_id = ?";
            ps = conn.prepareStatement(deleteMappings);
            ps.setInt(1, sectionId);
            ps.executeUpdate();
            ps.close();
            
            // Delete marking schemes if exists
            try {
                String deleteSchemes = "DELETE FROM marking_schemes WHERE section_id = ?";
                ps = conn.prepareStatement(deleteSchemes);
                ps.setInt(1, sectionId);
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                // Table might not exist, ignore
            }
            
            // Finally delete section
            String deleteSection = "DELETE FROM sections WHERE id = ?";
            ps = conn.prepareStatement(deleteSection);
            ps.setInt(1, sectionId);
            ps.executeUpdate();
            ps.close();
            
            conn.commit();
            System.out.println("Section deleted successfully: " + sectionId);
            return true;
            
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
                if (ps != null) ps.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                    // Don't close singleton connection
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Update section name
     * @param sectionId Section ID
     * @param newName New section name
     * @param userId User ID for verification
     * @return true if successful
     */
    public boolean updateSectionName(int sectionId, String newName, int userId) {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            
            // Verify ownership
            String verifyQuery = "SELECT COUNT(*) FROM sections WHERE id = ? AND created_by = ?";
            ps = conn.prepareStatement(verifyQuery);
            ps.setInt(1, sectionId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next() || rs.getInt(1) == 0) {
                System.err.println("Section not found or unauthorized: " + sectionId);
                return false;
            }
            rs.close();
            ps.close();
            
            // Update section name
            String updateQuery = "UPDATE sections SET section_name = ? WHERE id = ?";
            ps = conn.prepareStatement(updateQuery);
            ps.setString(1, newName);
            ps.setInt(2, sectionId);
            int rows = ps.executeUpdate();
            
            return rows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating section name: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (ps != null) ps.close();
                // Don't close singleton connection
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
