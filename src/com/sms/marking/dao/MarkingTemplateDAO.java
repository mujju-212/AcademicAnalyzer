package com.sms.marking.dao;

import com.sms.marking.models.*;
import com.sms.database.DatabaseConnection;
import java.sql.*;
import java.util.*;

public class MarkingTemplateDAO {
    
    // Get all active templates
    public List<MarkingTemplate> getAllTemplates() throws SQLException {
        List<MarkingTemplate> templates = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            
            String query = "SELECT * FROM marking_templates WHERE is_active = TRUE ORDER BY category, template_name";
            pstmt = conn.prepareStatement(query);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                MarkingTemplate template = new MarkingTemplate();
                template.setId(rs.getInt("id"));
                template.setTemplateName(rs.getString("template_name"));
                template.setTemplateCode(rs.getString("template_code"));
                template.setDescription(rs.getString("description"));
                template.setTemplateData(rs.getString("template_data"));
                template.setCategory(rs.getString("category"));
                template.setActive(rs.getBoolean("is_active"));
                template.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                
                templates.add(template);
            }
            
            return templates;
            
        } finally {
            // Close resources manually
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) { }
            if (conn != null) try { conn.close(); } catch (SQLException e) { }
        }
    }
    
    // Get template by code
    public MarkingTemplate getTemplateByCode(String templateCode) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            
            String query = "SELECT * FROM marking_templates WHERE template_code = ? AND is_active = TRUE";
            pstmt = conn.prepareStatement(query);
            pstmt.setString(1, templateCode);
            
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                MarkingTemplate template = new MarkingTemplate();
                template.setId(rs.getInt("id"));
                template.setTemplateName(rs.getString("template_name"));
                template.setTemplateCode(rs.getString("template_code"));
                template.setDescription(rs.getString("description"));
                template.setTemplateData(rs.getString("template_data"));
                template.setCategory(rs.getString("category"));
                template.setActive(rs.getBoolean("is_active"));
                
                return template;
            }
            
            return null;
            
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) { }
            if (conn != null) try { conn.close(); } catch (SQLException e) { }
        }
    }
    
    // Save template - simplified version without JSON
    public int saveTemplate(MarkingTemplate template) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            
            String query = "INSERT INTO marking_templates (template_name, template_code, description, " +
                          "template_data, category) VALUES (?, ?, ?, ?, ?)";
            
            pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, template.getTemplateName());
            pstmt.setString(2, template.getTemplateCode());
            pstmt.setString(3, template.getDescription());
            pstmt.setString(4, template.getTemplateData()); // Store as simple string for now
            pstmt.setString(5, template.getCategory());
            
            pstmt.executeUpdate();
            
            rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
            
            throw new SQLException("Failed to save template");
            
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (pstmt != null) try { pstmt.close(); } catch (SQLException e) { }
            if (conn != null) try { conn.close(); } catch (SQLException e) { }
        }
    }
    
    // Create predefined templates without JSON conversion
    public void createPredefinedTemplates() throws SQLException {
        // Check if templates exist
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT COUNT(*) FROM marking_templates");
            
            if (rs.next() && rs.getInt(1) > 0) {
                return; // Templates already exist
            }
            
            // Insert templates directly
            String[] insertQueries = {
                // Theory Only Template
                "INSERT INTO marking_templates (template_name, template_code, description, category, template_data) VALUES " +
                "('Theory Only (3 IAs + Final)', 'THEORY_ONLY', 'Standard theory subject with 3 internal assessments and final exam', 'Theory', " +
                "'Internal:IA(3,best2,25)+Assignment(25)|External:Final(50)')",
                
                // Theory with Lab Template
                "INSERT INTO marking_templates (template_name, template_code, description, category, template_data) VALUES " +
                "('Theory with Lab', 'THEORY_LAB', 'Theory subject with laboratory component', 'Theory+Lab', " +
                "'Internal:IA(3,best2,15)+Lab(20)+Assignment(15)|External:Theory(30)+Lab(20)')",
                
                // Theory with Project Template
                "INSERT INTO marking_templates (template_name, template_code, description, category, template_data) VALUES " +
                "('Theory with Project', 'THEORY_PROJECT', 'Theory subject with project component', 'Theory+Project', " +
                "'Internal:IA(3,best2,20)+Project(30)|External:Final(50)')",
                
                // Practical Only Template
                "INSERT INTO marking_templates (template_name, template_code, description, category, template_data) VALUES " +
                "('Practical Only', 'PRACTICAL_ONLY', 'Pure practical/lab subject', 'Practical', " +
                "'Internal:LabTests(30)+Record(20)|External:FinalLab(50)')"
            };
            
            for (String query : insertQueries) {
                stmt.executeUpdate(query);
            }
            
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) { }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
            if (conn != null) try { conn.close(); } catch (SQLException e) { }
        }
    }
}