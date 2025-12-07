package com.sms.marking.dao;

import com.sms.marking.models.*;
import com.sms.database.DatabaseConnection;
import java.sql.*;
import java.util.*;

public class MarkingSchemeDAO {
    
    // Create a new marking scheme with all components
	// In MarkingSchemeDAO.java, update the createMarkingScheme method:

	// In MarkingSchemeDAO.java

	public int createMarkingScheme(MarkingScheme scheme) throws SQLException {
	    Connection conn = null;
	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	    int schemeId = 0; // Declare outside try block
	    
	    try {
	        conn = DatabaseConnection.getConnection();
	        conn.setAutoCommit(false);
	        
	        // 1. Insert marking scheme
	        String schemeQuery = "INSERT INTO marking_schemes (section_id, subject_id, scheme_name, " +
	                           "total_internal_marks, total_external_marks, created_by) VALUES (?, ?, ?, ?, ?, ?)";
	        
	        pstmt = conn.prepareStatement(schemeQuery, Statement.RETURN_GENERATED_KEYS);
	        pstmt.setInt(1, scheme.getSectionId());
	        pstmt.setInt(2, scheme.getSubjectId());
	        pstmt.setString(3, scheme.getSchemeName());
	        pstmt.setInt(4, scheme.getTotalInternalMarks());
	        pstmt.setInt(5, scheme.getTotalExternalMarks());
	        pstmt.setInt(6, scheme.getCreatedBy());
	        
	        pstmt.executeUpdate();
	        
	        rs = pstmt.getGeneratedKeys();
	        if (rs.next()) {
	            schemeId = rs.getInt(1);
	            scheme.setId(schemeId);
	            rs.close();
	            pstmt.close();
	            
	            // 2. Insert component groups
	            for (ComponentGroup group : scheme.getComponentGroups()) {
	                int groupId = insertComponentGroup(conn, schemeId, group);
	                group.setId(groupId);
	                
	                // 3. Insert components for each group
	                for (MarkingComponent component : group.getComponents()) {
	                    insertMarkingComponent(conn, schemeId, groupId, component, group.getGroupType());
	                }
	            }
	            
	            conn.commit();
	            // Return the schemeId after commit
	            return schemeId;
	        } else {
	            throw new SQLException("Failed to create marking scheme");
	        }
	        
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
	        // Close resources in reverse order
	        try {
	            if (rs != null && !rs.isClosed()) rs.close();
	        } catch (SQLException e) { }
	        try {
	            if (pstmt != null && !pstmt.isClosed()) pstmt.close();
	        } catch (SQLException e) { }
	        try {
	            if (conn != null && !conn.isClosed()) {
	                conn.setAutoCommit(true);
	                conn.close();
	            }
	        } catch (SQLException e) { }
	    }
	}

	// Remove or comment out the updateSectionSubject method since it's not needed
	// The calling method in SectionDAO already handles this update
    
    // Insert component group
    private int insertComponentGroup(Connection conn, int schemeId, ComponentGroup group) throws SQLException {
        String query = "INSERT INTO component_groups (scheme_id, group_name, group_type, " +
                      "selection_type, selection_count, total_group_marks, sequence_order) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, schemeId);
            pstmt.setString(2, group.getGroupName());
            pstmt.setString(3, group.getGroupType());
            pstmt.setString(4, group.getSelectionType());
            
            if (group.getSelectionCount() != null) {
                pstmt.setInt(5, group.getSelectionCount());
            } else {
                pstmt.setNull(5, Types.INTEGER);
            }
            
            pstmt.setInt(6, group.getTotalGroupMarks());
            pstmt.setInt(7, group.getSequenceOrder());
            
            pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        
        throw new SQLException("Failed to insert component group");
    }
    
    // Insert marking component
    private void insertMarkingComponent(Connection conn, int schemeId, int groupId, 
                                      MarkingComponent component, String componentType) throws SQLException {
        String query = "INSERT INTO marking_components (scheme_id, group_id, component_name, " +
                      "component_type, actual_max_marks, scaled_to_marks, sequence_order, is_optional) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, schemeId);
            pstmt.setInt(2, groupId);
            pstmt.setString(3, component.getComponentName());
            pstmt.setString(4, componentType);
            pstmt.setInt(5, component.getActualMaxMarks());
            pstmt.setInt(6, 0); // scaled_to_marks will be calculated based on group
            pstmt.setInt(7, component.getSequenceOrder());
            pstmt.setBoolean(8, component.isOptional());
            
            pstmt.executeUpdate();
        }
    }
    
    // Update section_subjects to use new marking system
   /** private void updateSectionSubject(Connection conn, int sectionId, int subjectId, int schemeId) throws SQLException {
        String query = "UPDATE section_subjects SET use_new_marking_system = TRUE, " +
                      "marking_scheme_id = ? WHERE section_id = ? AND subject_id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, schemeId);
            pstmt.setInt(2, sectionId);
            pstmt.setInt(3, subjectId);
            pstmt.executeUpdate();
        }
    }**/
    
    // Get marking scheme by section and subject
    public MarkingScheme getMarkingScheme(int sectionId, int subjectId) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            
            // Get scheme
            String query = "SELECT * FROM marking_schemes WHERE section_id = ? AND subject_id = ? AND is_active = TRUE";
            pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, sectionId);
            pstmt.setInt(2, subjectId);
            
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                MarkingScheme scheme = new MarkingScheme();
                scheme.setId(rs.getInt("id"));
                scheme.setSectionId(rs.getInt("section_id"));
                scheme.setSubjectId(rs.getInt("subject_id"));
                scheme.setSchemeName(rs.getString("scheme_name"));
                scheme.setTotalInternalMarks(rs.getInt("total_internal_marks"));
                scheme.setTotalExternalMarks(rs.getInt("total_external_marks"));
                scheme.setActive(rs.getBoolean("is_active"));
                scheme.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                scheme.setCreatedBy(rs.getInt("created_by"));
                
                // Load component groups
                List<ComponentGroup> groups = getComponentGroups(conn, scheme.getId());
                scheme.setComponentGroups(groups);
                
                return scheme;
            }
            
            return null;
            
        } finally {
            DatabaseConnection.closeResources(conn, pstmt, rs);
        }
    }
    
    
    // Get component groups for a scheme
    private List<ComponentGroup> getComponentGroups(Connection conn, int schemeId) throws SQLException {
        List<ComponentGroup> groups = new ArrayList<>();
        
        String query = "SELECT * FROM component_groups WHERE scheme_id = ? ORDER BY sequence_order";
        
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, schemeId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ComponentGroup group = new ComponentGroup();
                    group.setId(rs.getInt("id"));
                    group.setSchemeId(rs.getInt("scheme_id"));
                    group.setGroupName(rs.getString("group_name"));
                    group.setGroupType(rs.getString("group_type"));
                    group.setSelectionType(rs.getString("selection_type"));
                    
                    int selectionCount = rs.getInt("selection_count");
                    if (!rs.wasNull()) {
                        group.setSelectionCount(selectionCount);
                    }
                    
                    group.setTotalGroupMarks(rs.getInt("total_group_marks"));
                    group.setSequenceOrder(rs.getInt("sequence_order"));
                    
                    // Load components for this group
                    List<MarkingComponent> components = getMarkingComponents(conn, group.getId());
                    group.setComponents(components);
                    
                    groups.add(group);
                }
            }
        }
        
        return groups;
    }
    
    // Get marking components for a group
    private List<MarkingComponent> getMarkingComponents(Connection conn, int groupId) throws SQLException {
        List<MarkingComponent> components = new ArrayList<>();
        
        String query = "SELECT * FROM marking_components WHERE group_id = ? ORDER BY sequence_order";
        
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, groupId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    MarkingComponent component = new MarkingComponent();
                    component.setId(rs.getInt("id"));
                    component.setSchemeId(rs.getInt("scheme_id"));
                    component.setGroupId(rs.getInt("group_id"));
                    component.setComponentName(rs.getString("component_name"));
                    component.setComponentType(rs.getString("component_type"));
                    component.setActualMaxMarks(rs.getInt("actual_max_marks"));
                    component.setScaledToMarks(rs.getInt("scaled_to_marks"));
                    component.setSequenceOrder(rs.getInt("sequence_order"));
                    component.setOptional(rs.getBoolean("is_optional"));
                    component.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    
                    components.add(component);
                }
            }
        }
        
        return components;
    }
    
    // Update marking scheme
    public boolean updateMarkingScheme(MarkingScheme scheme) throws SQLException {
        Connection conn = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            // 1. Update scheme
            String updateQuery = "UPDATE marking_schemes SET scheme_name = ?, " +
                               "total_internal_marks = ?, total_external_marks = ? WHERE id = ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
                pstmt.setString(1, scheme.getSchemeName());
                pstmt.setInt(2, scheme.getTotalInternalMarks());
                pstmt.setInt(3, scheme.getTotalExternalMarks());
                pstmt.setInt(4, scheme.getId());
                pstmt.executeUpdate();
            }
            
            // 2. Delete existing groups and components (cascade will handle components)
            String deleteGroups = "DELETE FROM component_groups WHERE scheme_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteGroups)) {
                pstmt.setInt(1, scheme.getId());
                pstmt.executeUpdate();
            }
            
            // 3. Re-insert groups and components
            for (ComponentGroup group : scheme.getComponentGroups()) {
                int groupId = insertComponentGroup(conn, scheme.getId(), group);
                group.setId(groupId);
                
                for (MarkingComponent component : group.getComponents()) {
                    insertMarkingComponent(conn, scheme.getId(), groupId, component, group.getGroupType());
                }
            }
            
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
            if (conn != null) {
                DatabaseConnection.closeConnection(conn);
            }
        }
    }
    
    // Get all marking schemes for a section
    public List<MarkingScheme> getMarkingSchemesBySection(int sectionId) throws SQLException {
        List<MarkingScheme> schemes = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            
            String query = "SELECT ms.*, s.subject_name FROM marking_schemes ms " +
                          "JOIN subjects s ON ms.subject_id = s.id " +
                          "WHERE ms.section_id = ? AND ms.is_active = TRUE";
            
            pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, sectionId);
            
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                MarkingScheme scheme = new MarkingScheme();
                scheme.setId(rs.getInt("id"));
                scheme.setSectionId(rs.getInt("section_id"));
                scheme.setSubjectId(rs.getInt("subject_id"));
                scheme.setSchemeName(rs.getString("scheme_name"));
                scheme.setTotalInternalMarks(rs.getInt("total_internal_marks"));
                scheme.setTotalExternalMarks(rs.getInt("total_external_marks"));
                scheme.setActive(rs.getBoolean("is_active"));
                
                schemes.add(scheme);
            }
            
            return schemes;
            
        } finally {
            DatabaseConnection.closeResources(conn, pstmt, rs);
        }
    }
    
    // Check if section-subject uses new marking system
    public boolean usesNewMarkingSystem(int sectionId, int subjectId) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            
            String query = "SELECT use_new_marking_system FROM section_subjects " +
                          "WHERE section_id = ? AND subject_id = ?";
            
            pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, sectionId);
            pstmt.setInt(2, subjectId);
            
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getBoolean("use_new_marking_system");
            }
            
            return false;
            
        } finally {
            DatabaseConnection.closeResources(conn, pstmt, rs);
        }
    }
}