package com.sms.dashboard.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.sms.database.DatabaseConnection;

/**
 * Service class for analytics and statistical operations
 */
public class AnalyticsService {
    
    /**
     * Gets the average percentage across all students for a user
     */
    public double getAveragePercentage(int userId) {
        String query = "SELECT AVG((sm.marks_obtained / ss.max_marks) * 100) as avg_percentage " +
                      "FROM student_marks sm " +
                      "INNER JOIN section_subjects ss ON sm.subject_id = ss.subject_id " +
                      "INNER JOIN students s ON sm.student_id = s.id " +
                      "WHERE s.created_by = ? AND sm.marks_obtained IS NOT NULL AND ss.max_marks > 0";
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(query);
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            if (rs.next()) {
                double avg = rs.getDouble("avg_percentage");
                return rs.wasNull() ? 0.0 : avg;
            }
        } catch (SQLException e) {
            System.err.println("Error getting average percentage: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                // Don't close shared singleton connection
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return 0.0;
    }

    /**
     * Gets the pass rate (students with >= 50% marks)
     */
    public double getPassRate(int userId) {
        String query = "SELECT " +
                      "COUNT(DISTINCT CASE WHEN avg_pct >= 50 THEN student_id END) * 100.0 / COUNT(DISTINCT student_id) as pass_rate " +
                      "FROM (" +
                      "  SELECT s.id as student_id, AVG((sm.marks_obtained / ss.max_marks) * 100) as avg_pct " +
                      "  FROM students s " +
                      "  INNER JOIN student_marks sm ON s.id = sm.student_id " +
                      "  INNER JOIN section_subjects ss ON sm.subject_id = ss.subject_id AND s.section_id = ss.section_id " +
                      "  WHERE s.created_by = ? AND sm.marks_obtained IS NOT NULL AND ss.max_marks > 0 " +
                      "  GROUP BY s.id" +
                      ") as student_averages";
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(query);
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            if (rs.next()) {
                double rate = rs.getDouble("pass_rate");
                return rs.wasNull() ? 0.0 : rate;
            }
        } catch (SQLException e) {
            System.err.println("Error getting pass rate: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                // Don't close shared singleton connection
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return 0.0;
    }

    /**
     * Gets the highest percentage achieved
     */
    public double getHighestPercentage(int userId) {
        String query = "SELECT MAX(avg_pct) as highest " +
                      "FROM (" +
                      "  SELECT AVG((sm.marks_obtained / ss.max_marks) * 100) as avg_pct " +
                      "  FROM student_marks sm " +
                      "  INNER JOIN students s ON sm.student_id = s.id " +
                      "  INNER JOIN section_subjects ss ON sm.subject_id = ss.subject_id AND s.section_id = ss.section_id " +
                      "  WHERE s.created_by = ? AND sm.marks_obtained IS NOT NULL AND ss.max_marks > 0 " +
                      "  GROUP BY s.id" +
                      ") as student_averages";
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(query);
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            if (rs.next()) {
                double highest = rs.getDouble("highest");
                return rs.wasNull() ? 0.0 : highest;
            }
        } catch (SQLException e) {
            System.err.println("Error getting highest percentage: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                // Don't close shared singleton connection
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return 0.0;
    }

    /**
     * Gets the lowest percentage achieved
     */
    public double getLowestPercentage(int userId) {
        String query = "SELECT MIN(avg_pct) as lowest " +
                      "FROM (" +
                      "  SELECT AVG((sm.marks_obtained / ss.max_marks) * 100) as avg_pct " +
                      "  FROM student_marks sm " +
                      "  INNER JOIN students s ON sm.student_id = s.id " +
                      "  INNER JOIN section_subjects ss ON sm.subject_id = ss.subject_id AND s.section_id = ss.section_id " +
                      "  WHERE s.created_by = ? AND sm.marks_obtained IS NOT NULL AND ss.max_marks > 0 " +
                      "  GROUP BY s.id" +
                      ") as student_averages";
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(query);
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            if (rs.next()) {
                double lowest = rs.getDouble("lowest");
                return rs.wasNull() ? 0.0 : lowest;
            }
        } catch (SQLException e) {
            System.err.println("Error getting lowest percentage: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                // Don't close shared singleton connection
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return 0.0;
    }
    
    /**
     * Gets comprehensive dashboard statistics
     */
    public java.util.HashMap<String, Object> getDashboardStatistics(int userId) {
        java.util.HashMap<String, Object> stats = new java.util.HashMap<>();
        
        System.out.println("Getting dashboard statistics for user: " + userId);
        
        try {
            // Total students
            String studentQuery = "SELECT COUNT(*) as count FROM students WHERE created_by = ?";
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(studentQuery);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            stats.put("totalStudents", rs.next() ? rs.getInt("count") : 0);
            rs.close();
            ps.close();
            // Don't close connection - it's shared
            
            // Total sections
            String sectionQuery = "SELECT COUNT(*) as count FROM sections WHERE created_by = ?";
            conn = DatabaseConnection.getConnection();
            ps = conn.prepareStatement(sectionQuery);
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            int sectionCount = rs.next() ? rs.getInt("count") : 0;
            stats.put("totalSections", sectionCount);
            System.out.println("Total sections from database: " + sectionCount);
            rs.close();
            ps.close();
            // Don't close connection - it's shared
            
            // Average score
            double avgScore = getAveragePercentage(userId);
            stats.put("averageScore", avgScore);
            System.out.println("Average score: " + avgScore);
            
            // Top performer - try with different possible column names
            String topPerformer = "N/A";
            String[] nameColumns = {"student_name", "name", "full_name"};
            
            for (String nameCol : nameColumns) {
                try {
                    String topPerformerQuery = "SELECT s." + nameCol + " as student_name, AVG((sm.marks_obtained / ss.max_marks) * 100) as avg_pct " +
                                             "FROM students s " +
                                             "INNER JOIN student_marks sm ON s.id = sm.student_id " +
                                             "INNER JOIN section_subjects ss ON sm.subject_id = ss.subject_id AND s.section_id = ss.section_id " +
                                             "WHERE s.created_by = ? AND sm.marks_obtained IS NOT NULL AND ss.max_marks > 0 " +
                                             "GROUP BY s.id, s." + nameCol + " " +
                                             "ORDER BY avg_pct DESC " +
                                             "LIMIT 1";
                    conn = DatabaseConnection.getConnection();
                    ps = conn.prepareStatement(topPerformerQuery);
                    ps.setInt(1, userId);
                    rs = ps.executeQuery();
                    if (rs.next()) {
                        topPerformer = rs.getString("student_name");
                        System.out.println("Top performer: " + topPerformer);
                        rs.close();
                        ps.close();
                        // Don't close connection - it's shared
                        break; // Found valid column, exit loop
                    }
                    rs.close();
                    ps.close();
                    // Don't close connection - it's shared
                } catch (SQLException e) {
                    // Try next column name
                    try {
                        if (rs != null) rs.close();
                        if (ps != null) ps.close();
                    } catch (SQLException ex) {
                        // Ignore
                    }
                    continue;
                }
            }
            stats.put("topPerformer", topPerformer);
            
            // Recent updates - count of student_marks entries from last 30 days
            String recentQuery = "SELECT COUNT(*) as count FROM student_marks sm " +
                               "INNER JOIN students s ON sm.student_id = s.id " +
                               "WHERE s.created_by = ? AND sm.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
            try {
                conn = DatabaseConnection.getConnection();
                ps = conn.prepareStatement(recentQuery);
                ps.setInt(1, userId);
                rs = ps.executeQuery();
                stats.put("recentUpdates", rs.next() ? rs.getInt("count") : 0);
                rs.close();
                ps.close();
                // Don't close connection - it's shared
            } catch (SQLException e) {
                // If created_at column doesn't exist, use a default value
                stats.put("recentUpdates", 0);
            }
            
            // Completion rate
            double completionRate = getPassRate(userId);
            stats.put("completionRate", completionRate);
            System.out.println("Completion rate: " + completionRate);
            
        } catch (SQLException e) {
            System.err.println("Error getting dashboard statistics: " + e.getMessage());
            e.printStackTrace();
        }
        
        return stats;
    }
}