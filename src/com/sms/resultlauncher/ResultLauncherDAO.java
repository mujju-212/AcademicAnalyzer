package com.sms.resultlauncher;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import javax.swing.JOptionPane;
import com.sms.database.DatabaseConnection;
import com.sms.calculation.models.Component;
import com.sms.dao.AnalyzerDAO;
import com.sms.calculation.models.CalculationResult;
import com.sms.calculation.StudentCalculator;
import com.sms.login.LoginScreen;

public class ResultLauncherDAO {
    

  public boolean launchResults(int sectionId, List<Integer> studentIds, 
                           List<com.sms.calculation.models.Component> components, ResultConfiguration config) {
    
    System.out.println("=== FULL LAUNCH IMPLEMENTATION ===");
    System.out.println("Section ID: " + sectionId);
    System.out.println("Student IDs: " + studentIds);
    System.out.println("Components: " + components.size());
    System.out.println("Config: " + config.getLaunchName());
    
    try {
        Connection conn = DatabaseConnection.getConnection();
        System.out.println("Database connection successful");
        
        // Convert lists to JSON
        List<Integer> componentIds = components.stream()
            .map(com.sms.calculation.models.Component::getId)
            .collect(java.util.stream.Collectors.toList());
        
        String studentIdsJson = convertListToJson(studentIds);
        String componentIdsJson = convertListToJson(componentIds);
        
        System.out.println("Student IDs JSON: " + studentIdsJson);
        System.out.println("Component IDs JSON: " + componentIdsJson);
        
        // Insert into launched_results table with proper data
        String insertLaunchQuery = "INSERT INTO launched_results " +
            "(launch_name, section_id, component_ids, student_ids, launched_by, " +
            "launch_date, status, email_sent) " +
            "VALUES (?, ?, ?, ?, ?, NOW(), 'active', false)";
        
        PreparedStatement ps = conn.prepareStatement(insertLaunchQuery, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, config.getLaunchName());
        ps.setInt(2, sectionId);
        ps.setString(3, componentIdsJson);  // Store component IDs JSON
        ps.setString(4, studentIdsJson);    // Store student IDs JSON
        ps.setInt(5, LoginScreen.currentUserId);
        
        int rowsAffected = ps.executeUpdate();
        System.out.println("Launch record inserted, rows affected: " + rowsAffected);
        
        if (rowsAffected == 0) {
            System.err.println("Failed to insert launch record");
            return false;
        }
        
        // Get the generated launch ID
        ResultSet generatedKeys = ps.getGeneratedKeys();
        int launchId = -1;
        if (generatedKeys.next()) {
            launchId = generatedKeys.getInt(1);
        }
        
        System.out.println("Generated launch ID: " + launchId);
        ps.close();
        
        if (launchId == -1) {
            System.err.println("Failed to get launch ID");
            return false;
        }
        
        // Calculate and store results for each student
        System.out.println("Starting student calculations...");
        StudentCalculator calculator = new StudentCalculator(40.0);
        
        int processedStudents = 0;
        for (Integer studentId : studentIds) {
            try {
                System.out.println("Processing student ID: " + studentId);
                
                // Get student name
                String studentName = getStudentName(studentId);
                System.out.println("Student name: " + studentName);
                
                // Load student component marks
                List<com.sms.calculation.models.Component> studentComponents = loadStudentComponentMarks(studentId, components);
                System.out.println("Loaded " + studentComponents.size() + " components for student");
                
                // Calculate results
                CalculationResult result = calculator.calculateStudentMarks(
                    studentId, studentName, studentComponents);
                
                System.out.println("Calculated results - Total: " + result.getTotalObtained() + "/" + result.getTotalPossible());
                
                // Store in student_web_results table
                String insertResultQuery = "INSERT INTO student_web_results " +
                    "(launch_id, student_id, result_data, created_at) " +
                    "VALUES (?, ?, ?, NOW())";
                
                PreparedStatement resultPs = conn.prepareStatement(insertResultQuery);
                resultPs.setInt(1, launchId);
                resultPs.setInt(2, studentId);
                resultPs.setString(3, convertResultToJson(result, studentComponents));
                
                resultPs.executeUpdate();
                resultPs.close();
                
                processedStudents++;
                System.out.println("Processed student " + processedStudents + "/" + studentIds.size());
                
            } catch (Exception e) {
                System.err.println("Error calculating results for student " + studentId + ": " + e.getMessage());
                e.printStackTrace();
                // Continue with other students
            }
        }
        
        System.out.println("Student calculations completed");
        
        // Send email notifications if enabled
        if (config.isSendEmailNotification()) {
            System.out.println("Sending email notifications...");
            sendEmailNotifications(launchId, studentIds, config);
        } else {
            System.out.println("Email notifications disabled");
        }
        
        System.out.println("Launch completed successfully");
        return true;
        
    } catch (Exception e) {
        System.err.println("Error in launchResults: " + e.getMessage());
        e.printStackTrace();
        return false;
    }
}

// Add the missing helper method for loading student component marks
private List<com.sms.calculation.models.Component> loadStudentComponentMarks(int studentId, List<com.sms.calculation.models.Component> components) {
    try {
        AnalyzerDAO dao = new AnalyzerDAO();
        
        // Get component IDs
        List<Integer> componentIds = components.stream()
            .map(com.sms.calculation.models.Component::getId)
            .collect(java.util.stream.Collectors.toList());
        
        // Get student marks
        java.util.Map<Integer, AnalyzerDAO.StudentComponentMark> studentMarks = 
            dao.getStudentComponentMarks(studentId, componentIds);
        
        List<com.sms.calculation.models.Component> studentComponents = new java.util.ArrayList<>();
        
        for (com.sms.calculation.models.Component comp : components) {
            AnalyzerDAO.StudentComponentMark mark = studentMarks.get(comp.getId());
            
            com.sms.calculation.models.Component studentComp = new com.sms.calculation.models.Component(
                comp.getId(),
                comp.getName(),
                comp.getType(),
                mark != null ? mark.marksObtained : 0,
                comp.getMaxMarks(),
                comp.getWeight()
            );
            
            studentComp.setCounted(mark != null ? mark.isCounted : false);
            studentComponents.add(studentComp);
        }
        
        return studentComponents;
        
    } catch (Exception e) {
        System.err.println("Error loading student component marks: " + e.getMessage());
        e.printStackTrace();
        return new java.util.ArrayList<>();
    }
}
    /**
     * Get all launched results
     */
  
    
    /**
     * Take down a launched result
     */
    public boolean takeDownResult(int launchId) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "UPDATE launched_results SET status = 'inactive' WHERE id = ? AND launched_by = ?";
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, launchId);
            ps.setInt(2, LoginScreen.currentUserId);
            
            int rowsAffected = ps.executeUpdate();
            ps.close();
            
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error taking down result: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get student name by ID
     */
    private String getStudentName(int studentId) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT student_name FROM students WHERE id = ?";
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            String name = "Unknown Student";
            if (rs.next()) {
                name = rs.getString("student_name");
            }
            
            rs.close();
            ps.close();
            return name;
            
        } catch (SQLException e) {
            System.err.println("Error getting student name: " + e.getMessage());
            return "Unknown Student";
        }
    }
    
    /**
     * Convert list to JSON string
     */
    /**
     * Convert list to JSON string - Fixed version
     */
    private String convertListToJson(List<Integer> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) json.append(",");
            json.append(list.get(i));
        }
        json.append("]");
        
        String result = json.toString();
        System.out.println("Converting list to JSON: " + list + " -> " + result);
        return result;
    }

    /**
     * Convert JSON string to list - Fixed version
     */
    private List<Integer> convertJsonToList(String json) {
        List<Integer> list = new ArrayList<>();
        System.out.println("Converting JSON to list: " + json);
        
        if (json == null || json.trim().isEmpty() || "[]".equals(json.trim())) {
            return list;
        }
        
        try {
            // Simple JSON parsing for integer arrays
            String cleaned = json.replace("[", "").replace("]", "").trim();
            if (!cleaned.isEmpty()) {
                String[] parts = cleaned.split(",");
                for (String part : parts) {
                    list.add(Integer.parseInt(part.trim()));
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing JSON list: " + e.getMessage());
        }
        
        System.out.println("Converted to list: " + list);
        return list;
    }

    /**
     * Updated getLaunchedResults method to properly count students and components
     */
    public List<LaunchedResult> getLaunchedResults() {
        List<LaunchedResult> results = new ArrayList<>();
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT lr.*, s.section_name, u.username as launched_by_name " +
                          "FROM launched_results lr " +
                          "JOIN sections s ON lr.section_id = s.id " +
                          "LEFT JOIN users u ON lr.launched_by = u.id " +
                          "WHERE lr.launched_by = ? " +
                          "ORDER BY lr.launch_date DESC";
            
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, LoginScreen.currentUserId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                LaunchedResult result = new LaunchedResult();
                result.setId(rs.getInt("id"));
                result.setLaunchName(rs.getString("launch_name"));
                result.setSectionId(rs.getInt("section_id"));
                result.setSectionName(rs.getString("section_name"));
                result.setLaunchedBy(rs.getInt("launched_by"));
                result.setLaunchedByName(rs.getString("launched_by_name"));
                result.setLaunchDate(rs.getTimestamp("launch_date").toLocalDateTime());
                result.setStatus(rs.getString("status"));
                result.setEmailSent(rs.getBoolean("email_sent"));
                
                // Count students from JSON
                String studentIds = rs.getString("student_ids");
                if (studentIds != null && !studentIds.isEmpty()) {
                    List<Integer> studentIdList = convertJsonToList(studentIds);
                    result.setStudentCount(studentIdList.size());
                    System.out.println("Student count for launch " + result.getId() + ": " + studentIdList.size());
                } else {
                    result.setStudentCount(0);
                }
                
                // Count components from JSON
                String componentIds = rs.getString("component_ids");
                if (componentIds != null && !componentIds.isEmpty()) {
                    List<Integer> componentIdList = convertJsonToList(componentIds);
                    result.setComponentCount(componentIdList.size());
                    System.out.println("Component count for launch " + result.getId() + ": " + componentIdList.size());
                } else {
                    result.setComponentCount(0);
                }
                
                results.add(result);
            }
            
            rs.close();
            ps.close();
            
        } catch (SQLException e) {
            System.err.println("Error loading launched results: " + e.getMessage());
            e.printStackTrace();
        }
        
        return results;
    }
    
    /**
     * Convert calculation result to JSON
     */
    /**
     * Debug method to check student emails
     */
    public void debugStudentEmails(List<Integer> studentIds) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            if (studentIds.isEmpty()) {
                System.out.println("No student IDs provided for email debug");
                return;
            }
            
            String placeholders = String.join(",", java.util.Collections.nCopies(studentIds.size(), "?"));
            String query = "SELECT s.id, s.student_name, s.email, sec.section_name " +
                          "FROM students s " +
                          "JOIN sections sec ON s.section_id = sec.id " +
                          "WHERE s.id IN (" + placeholders + ")";
            
            PreparedStatement ps = conn.prepareStatement(query);
            for (int i = 0; i < studentIds.size(); i++) {
                ps.setInt(i + 1, studentIds.get(i));
            }
            
            ResultSet rs = ps.executeQuery();
            
            System.out.println("=== STUDENT EMAIL DEBUG ===");
            int totalStudents = 0;
            int studentsWithEmail = 0;
            
            while (rs.next()) {
                totalStudents++;
                String email = rs.getString("email");
                String name = rs.getString("student_name");
                
                System.out.println("Student: " + name + " | Email: " + (email != null ? email : "NULL"));
                
                if (email != null && !email.trim().isEmpty()) {
                    studentsWithEmail++;
                }
            }
            
            System.out.println("Total students: " + totalStudents);
            System.out.println("Students with email: " + studentsWithEmail);
            System.out.println("Students without email: " + (totalStudents - studentsWithEmail));
            
            rs.close();
            ps.close();
            
        } catch (Exception e) {
            System.err.println("Error in debugStudentEmails: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private String convertResultToJson(CalculationResult result, List<Component> components) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"student_id\":").append(result.getStudentId()).append(",");
        json.append("\"student_name\":\"").append(result.getStudentName()).append("\",");
        json.append("\"total_obtained\":").append(result.getTotalObtained()).append(",");
        json.append("\"total_possible\":").append(result.getTotalPossible()).append(",");
        json.append("\"percentage\":").append(result.getFinalPercentage()).append(",");
        json.append("\"grade\":\"").append(result.getGrade()).append("\",");
        json.append("\"sgpa\":").append(result.getSgpa()).append(",");
        json.append("\"is_passing\":").append(result.isPassing()).append(",");
        json.append("\"calculation_method\":\"").append(result.getCalculationMethod()).append("\",");
        
        // Add components
        json.append("\"components\":[");
        for (int i = 0; i < components.size(); i++) {
            if (i > 0) json.append(",");
            Component comp = components.get(i);
            json.append("{");
            json.append("\"id\":").append(comp.getId()).append(",");
            json.append("\"name\":\"").append(comp.getName()).append("\",");
            json.append("\"type\":\"").append(comp.getType()).append("\",");
            json.append("\"obtained_marks\":").append(comp.getObtainedMarks()).append(",");
            json.append("\"max_marks\":").append(comp.getMaxMarks()).append(",");
            json.append("\"percentage\":").append(comp.getPercentage());
            json.append("}");
        }
        json.append("]");
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Send email notifications (placeholder)
     */
 // In ResultLauncherDAO.java, replace the sendEmailNotifications method:
    private void sendEmailNotifications(int launchId, List<Integer> studentIds, ResultConfiguration config) {
        if (!config.isSendEmailNotification()) {
            return;
        }
        
        try {
            // Send emails using EmailJS
            boolean emailSuccess = EmailService.sendResultNotifications(
                studentIds, 
                config.getEmailSubject(), 
                config.getEmailMessage(), 
                config.getLaunchName()
            );
            
            // Update email sent status
            Connection conn = DatabaseConnection.getConnection();
            String query = "UPDATE launched_results SET email_sent = ? WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setBoolean(1, emailSuccess);
            ps.setInt(2, launchId);
            ps.executeUpdate();
            ps.close();
            
            if (emailSuccess) {
                System.out.println("Email notifications sent successfully");
            } else {
                System.out.println("Some email notifications failed to send");
            }
            
        } catch (Exception e) {
            System.err.println("Error in email notification process: " + e.getMessage());
            e.printStackTrace();
        }
    }
}