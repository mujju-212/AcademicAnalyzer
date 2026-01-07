package com.sms.resultlauncher;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

import com.sms.database.DatabaseConnection;

public class EmailService {
    
    // EmailJS configuration - you'll need to set these up in EmailJS dashboard
    private static final String EMAILJS_SERVICE_ID = "service_wzfqoqi";
    private static final String EMAILJS_TEMPLATE_ID = "template_8yadxkw";
    private static final String EMAILJS_USER_ID = "qRvUql80LlODg7Dtu";
    private static final String EMAILJS_API_URL = "https://api.emailjs.com/api/v1.0/email/send";
    
    /**
     * Send email notifications to students
     */
    private void sendEmailNotifications(int launchId, List<Integer> studentIds, ResultConfiguration config) {
        if (!config.isSendEmailNotification()) {
            System.out.println("Email notifications disabled in config");
            return;
        }
        
        try {
            System.out.println("=== EMAIL NOTIFICATION DEBUG ===");
            System.out.println("Launch ID: " + launchId);
            System.out.println("Student IDs: " + studentIds);
            System.out.println("Email subject: " + config.getEmailSubject());
            
            // Debug student emails first
            getStudentEmails(studentIds);
            
            // Check if EmailJS is configured
            if (EmailService.EMAILJS_SERVICE_ID.equals("your_service_id") || 
                EmailService.EMAILJS_TEMPLATE_ID.equals("your_template_id") ||
                EmailService.EMAILJS_USER_ID.equals("your_user_id")) {
                
                System.out.println("EmailJS not configured - skipping email sending");
                System.out.println("Please update EmailJS configuration in EmailService.java");
                
                // Mark as email sent = false
                Connection conn = DatabaseConnection.getConnection();
                String query = "UPDATE launched_results SET email_sent = false WHERE id = ?";
                PreparedStatement ps = conn.prepareStatement(query);
                ps.setInt(1, launchId);
                ps.executeUpdate();
                ps.close();
                return;
            }
            
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
            
            System.out.println("Email notification process completed. Success: " + emailSuccess);
            
        } catch (Exception e) {
            System.err.println("Error in email notification process: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    static boolean sendResultNotifications(List<Integer> studentIds, String emailSubject, String emailMessage,
			String launchName) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
     * Send email to a single student using EmailJS
     */
    private static boolean sendSingleEmail(StudentEmail studentEmail, String subject, 
                                         String message, String launchName) {
        try {
            // Create EmailJS payload
            String jsonPayload = createEmailPayload(studentEmail, subject, message, launchName);
            
            // Send HTTP POST request to EmailJS
            URL url = new URL(EMAILJS_API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            
            // Write payload
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            
            // Check response
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                System.out.println("Email sent successfully to: " + studentEmail.email);
                return true;
            } else {
                System.err.println("Failed to send email to " + studentEmail.email + 
                                 ". Response code: " + responseCode);
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("Error sending email to " + studentEmail.email + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Create EmailJS JSON payload
     */
    private static String createEmailPayload(StudentEmail studentEmail, String subject, 
            String message, String launchName) {
// Customize the message for each student
String personalizedMessage = message.replace("Dear Student", "Dear " + studentEmail.name);

// Your portal URL - UPDATE THIS WITH YOUR ACTUAL PORTAL URL
String portalUrl = "http://localhost:5000"; // Change this to your actual web portal URL

// Create JSON payload for EmailJS
StringBuilder json = new StringBuilder();
json.append("{");
json.append("\"service_id\":\"").append(EMAILJS_SERVICE_ID).append("\",");
json.append("\"template_id\":\"").append(EMAILJS_TEMPLATE_ID).append("\",");
json.append("\"user_id\":\"").append(EMAILJS_USER_ID).append("\",");
json.append("\"template_params\":{");
json.append("\"to_email\":\"").append(escapeJson(studentEmail.email)).append("\",");
json.append("\"to_name\":\"").append(escapeJson(studentEmail.name)).append("\",");
json.append("\"subject\":\"").append(escapeJson(subject)).append("\",");
json.append("\"message\":\"").append(escapeJson(personalizedMessage)).append("\",");
json.append("\"launch_name\":\"").append(escapeJson(launchName)).append("\",");
json.append("\"student_section\":\"").append(escapeJson(studentEmail.section)).append("\",");
json.append("\"portal_url\":\"").append(portalUrl).append("\",");
json.append("\"launch_date\":\"").append(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\"");
json.append("}");
json.append("}");

return json.toString();
}

//Add this helper method to escape JSON strings
private static String escapeJson(String text) {
if (text == null) return "";
return text.replace("\"", "\\\"")
.replace("\n", "\\n")
.replace("\r", "\\r")
.replace("\t", "\\t");
}
    
    /**
     * Get student emails from database
     */
 // In EmailService.java, replace the database connection part:
    private static List<StudentEmail> getStudentEmails(List<Integer> studentIds) {
        List<StudentEmail> emails = new ArrayList<>();
        
        try {
            java.sql.Connection conn = com.sms.database.DatabaseConnection.getConnection();
            
            if (studentIds.isEmpty()) {
                return emails;
            }
            
            // Create placeholders for IN clause
            String placeholders = String.join(",", java.util.Collections.nCopies(studentIds.size(), "?"));
            
            String query = "SELECT s.id, s.student_name, s.email, sec.section_name " +
                          "FROM students s " +
                          "JOIN sections sec ON s.section_id = sec.id " +
                          "WHERE s.id IN (" + placeholders + ") AND s.email IS NOT NULL AND s.email != ''";
            
            java.sql.PreparedStatement ps = conn.prepareStatement(query);
            for (int i = 0; i < studentIds.size(); i++) {
                ps.setInt(i + 1, studentIds.get(i));
            }
            
            java.sql.ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                StudentEmail studentEmail = new StudentEmail();
                studentEmail.id = rs.getInt("id");
                studentEmail.name = rs.getString("student_name");
                studentEmail.email = rs.getString("email");
                studentEmail.section = rs.getString("section_name");
                emails.add(studentEmail);
            }
            
            rs.close();
            ps.close();
            
        } catch (Exception e) {
            System.err.println("Error getting student emails: " + e.getMessage());
            e.printStackTrace();
        }
        
        return emails;
    }
    
    /**
     * Test email configuration
     */
    public static boolean testEmailConfiguration() {
        try {
            // Send a test email to verify configuration
            StudentEmail testEmail = new StudentEmail();
            testEmail.name = "Test User";
            testEmail.email = "test@example.com"; // Replace with your test email
            testEmail.section = "Test Section";
            
            return sendSingleEmail(testEmail, "Test Email", 
                "This is a test email from the Result Launcher system.", "Test Launch");
                
        } catch (Exception e) {
            System.err.println("Email configuration test failed: " + e.getMessage());
            return false;
        }
    }
    
    // Helper class for student email data
    private static class StudentEmail {
        int id;
        String name;
        String email;
        String section;
    }
}