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
        return getAveragePercentage(userId, 0); // Default to all years
    }
    
    /**
     * Gets the average percentage across all students for a user filtered by academic year
     */
    public double getAveragePercentage(int userId, int academicYear) {
        String yearFilter = (academicYear > 0) ? " AND sec.academic_year = " + academicYear : "";
        
        String query = "SELECT AVG((sm.marks_obtained / ss.max_marks) * 100) as avg_percentage " +
                      "FROM entered_exam_marks sm " +
                      "INNER JOIN section_subjects ss ON sm.subject_id = ss.subject_id " +
                      "INNER JOIN students s ON sm.student_id = s.id " +
                      "INNER JOIN sections sec ON s.section_id = sec.id " +
                      "WHERE s.created_by = ? AND sm.marks_obtained IS NOT NULL AND ss.max_marks > 0" + yearFilter;
        
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
                if (conn != null) conn.close(); // Return connection to pool!
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
                      "  INNER JOIN entered_exam_marks sm ON s.id = sm.student_id " +
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
                if (conn != null) conn.close(); // Return connection to pool!
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
                      "  FROM entered_exam_marks sm " +
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
                if (conn != null) conn.close(); // Return connection to pool!
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
                      "  FROM entered_exam_marks sm " +
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
                if (conn != null) conn.close(); // Return connection to pool!
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
        return getDashboardStatistics(userId, 0); // 0 means all years
    }
    
    public java.util.HashMap<String, Object> getDashboardStatistics(int userId, int academicYear) {
        java.util.HashMap<String, Object> stats = new java.util.HashMap<>();
        
        try {
            // Build year filter condition
            String yearFilter = "";
            if (academicYear > 0) {
                yearFilter = " AND sec.academic_year = " + academicYear;
            }
            
            // Total students (filtered by year)
            String studentQuery = "SELECT COUNT(*) as count FROM students st " +
                                 "INNER JOIN sections sec ON st.section_id = sec.id " +
                                 "WHERE st.created_by = ?" + yearFilter;
            Connection conn = DatabaseConnection.getConnection();
            try {
                PreparedStatement ps = conn.prepareStatement(studentQuery);
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                stats.put("totalStudents", rs.next() ? rs.getInt("count") : 0);
                rs.close();
                ps.close();
            } finally {
                if (conn != null) conn.close(); // CRITICAL: Return connection to pool!
            }
            
            // Total sections (filtered by year)
            String sectionQuery = "SELECT COUNT(*) as count FROM sections WHERE created_by = ?" + 
                                 (academicYear > 0 ? " AND academic_year = " + academicYear : "");
            conn = DatabaseConnection.getConnection();
            try {
                PreparedStatement ps = conn.prepareStatement(sectionQuery);
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                int sectionCount = rs.next() ? rs.getInt("count") : 0;
                stats.put("totalSections", sectionCount);
                rs.close();
                ps.close();
            } finally {
                if (conn != null) conn.close(); // CRITICAL: Return connection to pool!
            }
            
            // Average score (filtered by year)
            double avgScore = getAveragePercentage(userId, academicYear);
            stats.put("averageScore", avgScore);
            
            // Top performer - try with different possible column names
            String topPerformer = "N/A";
            String[] nameColumns = {"student_name", "name", "full_name"};
            
            for (String nameCol : nameColumns) {
                Connection topConn = null;
                PreparedStatement topPs = null;
                ResultSet topRs = null;
                try {
                    String topPerformerQuery = "SELECT s." + nameCol + " as student_name, AVG((sm.marks_obtained / ss.max_marks) * 100) as avg_pct " +
                                             "FROM students s " +
                                             "INNER JOIN sections sec ON s.section_id = sec.id " +
                                             "INNER JOIN entered_exam_marks sm ON s.id = sm.student_id " +
                                             "INNER JOIN section_subjects ss ON sm.subject_id = ss.subject_id AND s.section_id = ss.section_id " +
                                             "WHERE s.created_by = ? AND sm.marks_obtained IS NOT NULL AND ss.max_marks > 0" + yearFilter + " " +
                                             "GROUP BY s.id, s." + nameCol + " " +
                                             "ORDER BY avg_pct DESC " +
                                             "LIMIT 1";
                    topConn = DatabaseConnection.getConnection();
                    topPs = topConn.prepareStatement(topPerformerQuery);
                    topPs.setInt(1, userId);
                    topRs = topPs.executeQuery();
                    if (topRs.next()) {
                        topPerformer = topRs.getString("student_name");
                        break; // Found valid column, exit loop
                    }
                } catch (SQLException e) {
                    // Try next column name
                    continue;
                } finally {
                    // CRITICAL: Always close resources in finally block!
                    try {
                        if (topRs != null) topRs.close();
                        if (topPs != null) topPs.close();
                        if (topConn != null) topConn.close();
                    } catch (SQLException ex) {
                        // Ignore cleanup errors
                    }
                }
            }
            stats.put("topPerformer", topPerformer);
            
            // Recent updates - count of entered_exam_marks entries from last 30 days
            String recentQuery = "SELECT COUNT(*) as count FROM entered_exam_marks sm " +
                               "INNER JOIN students s ON sm.student_id = s.id " +
                               "WHERE s.created_by = ? AND sm.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
            conn = DatabaseConnection.getConnection();
            try {
                PreparedStatement ps = conn.prepareStatement(recentQuery);
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                stats.put("recentUpdates", rs.next() ? rs.getInt("count") : 0);
                rs.close();
                ps.close();
            } catch (SQLException e) {
                // If created_at column doesn't exist, use a default value
                stats.put("recentUpdates", 0);
            } finally {
                if (conn != null) conn.close(); // CRITICAL: Return connection to pool!
            }
            
            // Completion rate
            double completionRate = getPassRate(userId);
            stats.put("completionRate", completionRate);
            
        } catch (SQLException e) {
            System.err.println("Error getting dashboard statistics: " + e.getMessage());
            e.printStackTrace();
        }
        
        return stats;
    }
}
