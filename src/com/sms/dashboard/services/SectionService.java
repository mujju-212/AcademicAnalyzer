package com.sms.dashboard.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import com.sms.database.DatabaseConnection;
import com.sms.dao.SectionDAO;
import com.sms.dao.SectionDAO.SectionInfo;
import com.sms.dao.SectionDAO.SubjectInfo;

/**
 * Service class for section-related database operations
 */
public class SectionService {
    
    private final SectionDAO sectionDAO = new SectionDAO();
    
    /**
     * Gets all sections for a specific user
     */
    public List<SectionInfo> getSectionsByUser(int userId) {
        System.out.println("SectionService.getSectionsByUser called for userId: " + userId);
        return sectionDAO.getSectionsByUser(userId);
    }
    
    /**
     * Gets all sections for a specific user (alias for consistency)
     */
    public List<SectionInfo> getUserSections(int userId) {
        return getSectionsByUser(userId);
    }
    
    /**
     * Gets subjects for a specific section
     */
    public List<SubjectInfo> getSectionSubjects(int sectionId) {
        return sectionDAO.getSectionSubjects(sectionId);
    }
    
    /**
     * Deletes a section and all associated data
     */
    public boolean deleteSection(int sectionId, int userId) {
        return sectionDAO.deleteSection(sectionId, userId);
    }
    
    /**
     * Gets the total count of sections for a user
     */
    public int getTotalSectionsCount(int userId) {
        String query = "SELECT COUNT(*) as count FROM sections WHERE created_by = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("count") : 0;
            }
        } catch (SQLException e) {
            System.err.println("Error getting total sections count: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * Gets the total count of subjects across all sections for a user
     */
    public int getTotalSubjectsCount(int userId) {
        String query = "SELECT COUNT(*) as count FROM section_subjects ss " +
                      "INNER JOIN sections s ON ss.section_id = s.id " +
                      "WHERE s.created_by = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("count") : 0;
            }
        } catch (SQLException e) {
            System.err.println("Error getting total subjects count: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
}