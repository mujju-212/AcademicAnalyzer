package com.sms.resultlauncher;

import com.sms.database.DatabaseConnection;
import com.sms.util.ConfigLoader;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

/**
 * EmailService for sending result notifications using MailerSend API via HTTP.
 * Handles email notifications to students when results are launched.
 * 
 * @version 1.0
 * @since 2026-01-15
 */
public class EmailService {
    
    private static final String API_KEY = ConfigLoader.getMailerSendApiKey();
    private static final String FROM_EMAIL = ConfigLoader.getMailerSendFromEmail();
    private static final String FROM_NAME = ConfigLoader.getMailerSendFromName();
    private static final String API_URL = "https://api.mailersend.com/v1/email";
    
    /**
     * Send result notification emails to multiple students.
     * 
     * @param studentIds List of student IDs to send emails to
     * @param subject Email subject
     * @param messageBody Custom message from teacher
     * @param launchName Name of the launched result
     * @return true if all emails sent successfully, false otherwise
     */
    public static boolean sendResultNotifications(List<Integer> studentIds, String subject, 
                                                  String messageBody, String launchName) {
        
        if (API_KEY == null || API_KEY.isEmpty()) {
            System.err.println("⚠️ MailerSend API key not configured");
            return false;
        }
        
        System.out.println("📧 Sending emails to " + studentIds.size() + " students...");
        
        int successCount = 0;
        int failCount = 0;
        
        for (Integer studentId : studentIds) {
            try {
                String studentEmail = getStudentEmail(studentId);
                String studentName = getStudentName(studentId);
                
                if (studentEmail == null || studentEmail.isEmpty()) {
                    System.out.println("⚠️ No email for student ID " + studentId + ", skipping");
                    failCount++;
                    continue;
                }
                
                // Set subject
                String emailSubject = subject != null && !subject.isEmpty() ? 
                                subject : "Your Results Have Been Published";
                
                // Create HTML and plain text content
                String htmlContent = buildHtmlEmail(studentName, launchName, messageBody);
                String plainContent = buildPlainEmail(studentName, launchName, messageBody);
                
                // Send email via HTTP API
                boolean sent = sendEmailViaHttp(FROM_NAME, FROM_EMAIL, studentName, studentEmail, 
                                               emailSubject, htmlContent, plainContent);
                
                if (sent) {
                    System.out.println("✅ Email sent to " + studentName + " (" + studentEmail + ")");
                    successCount++;
                } else {
                    System.err.println("❌ Failed to send email to " + studentEmail);
                    failCount++;
                }
                
                // Small delay to avoid rate limiting
                Thread.sleep(100);
                
            } catch (Exception e) {
                System.err.println("❌ Error for student " + studentId + ": " + e.getMessage());
                failCount++;
            }
        }
        
        System.out.println("📊 Email Summary: " + successCount + " sent, " + failCount + " failed");
        
        // Consider successful if at least 50% sent
        return successCount > 0 && (successCount >= studentIds.size() / 2);
    }
    
    /**
     * Send email via MailerSend HTTP API.
     */
    private static boolean sendEmailViaHttp(String fromName, String fromEmail, 
                                           String toName, String toEmail,
                                           String subject, String htmlContent, String plainContent) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            // Set up connection
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setDoOutput(true);
            
            // Build JSON payload
            String jsonPayload = buildJsonPayload(fromName, fromEmail, toName, toEmail, 
                                                 subject, htmlContent, plainContent);
            
            // Send request
            try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(jsonPayload);
                writer.flush();
            }
            
            // Check response
            int responseCode = conn.getResponseCode();
            
            if (responseCode == 202 || responseCode == 200) {
                return true;
            } else {
                System.err.println("Email API error: HTTP " + responseCode);
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("HTTP email error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Build JSON payload for MailerSend API.
     */
    private static String buildJsonPayload(String fromName, String fromEmail,
                                          String toName, String toEmail,
                                          String subject, String htmlContent, String plainContent) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"from\":{");
        json.append("\"email\":\"").append(escapeJson(fromEmail)).append("\",");
        json.append("\"name\":\"").append(escapeJson(fromName)).append("\"");
        json.append("},");
        json.append("\"to\":[{");
        json.append("\"email\":\"").append(escapeJson(toEmail)).append("\",");
        json.append("\"name\":\"").append(escapeJson(toName)).append("\"");
        json.append("}],");
        json.append("\"subject\":\"").append(escapeJson(subject)).append("\",");
        json.append("\"text\":\"").append(escapeJson(plainContent)).append("\",");
        json.append("\"html\":\"").append(escapeJson(htmlContent)).append("\"");
        json.append("}");
        return json.toString();
    }
    
    /**
     * Escape JSON special characters.
     */
    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    /**
     * Send a single test email to verify configuration.
     * 
     * @param recipientEmail Email address to send test to
     * @param recipientName Name of recipient
     * @return true if sent successfully
     */
    public static boolean sendTestEmail(String recipientEmail, String recipientName) {
        String html = "<html><body>" +
                     "<h2>Test Email</h2>" +
                     "<p>This is a test email from Academic Analyzer.</p>" +
                     "<p>If you received this, your email configuration is working correctly!</p>" +
                     "</body></html>";
        
        String plain = "Test Email\n\n" +
                      "This is a test email from Academic Analyzer.\n" +
                      "If you received this, your email configuration is working correctly!";
        
        return sendEmailViaHttp(FROM_NAME, FROM_EMAIL, recipientName, recipientEmail,
                               "Test Email from Academic Analyzer", html, plain);
    }
    
    /**
     * Get student email from database.
     */
    private static String getStudentEmail(int studentId) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT email FROM students WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            String email = null;
            if (rs.next()) {
                email = rs.getString("email");
            }
            
            rs.close();
            ps.close();
            return email;
            
        } catch (Exception e) {
            System.err.println("Error getting student email: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get student name from database.
     */
    private static String getStudentName(int studentId) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT student_name FROM students WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            String name = "Student";
            if (rs.next()) {
                name = rs.getString("student_name");
            }
            
            rs.close();
            ps.close();
            return name;
            
        } catch (Exception e) {
            System.err.println("Error getting student name: " + e.getMessage());
            return "Student";
        }
    }
    
    /**
     * Build HTML email content.
     */
    private static String buildHtmlEmail(String studentName, String launchName, String customMessage) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<style>");
        html.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; }");
        html.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        html.append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }");
        html.append(".content { background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px; }");
        html.append(".button { display: inline-block; padding: 12px 30px; background: #667eea; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }");
        html.append(".footer { text-align: center; margin-top: 20px; color: #6c757d; font-size: 12px; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='container'>");
        html.append("<div class='header'>");
        html.append("<h1>📊 Results Published</h1>");
        html.append("</div>");
        html.append("<div class='content'>");
        html.append("<p>Dear <strong>").append(studentName).append("</strong>,</p>");
        html.append("<p>Your results for <strong>").append(launchName).append("</strong> have been published and are now available for viewing.</p>");
        
        if (customMessage != null && !customMessage.isEmpty()) {
            html.append("<div style='background: white; padding: 15px; border-left: 4px solid #667eea; margin: 20px 0;'>");
            html.append("<p><strong>Message from your teacher:</strong></p>");
            html.append("<p>").append(customMessage.replace("\n", "<br>")).append("</p>");
            html.append("</div>");
        }
        
        html.append("<p>To view your results, please log in to the student portal:</p>");
        html.append("<a href='#' class='button'>View My Results</a>");
        html.append("<p style='color: #6c757d; font-size: 14px;'>If the button doesn't work, copy and paste this link into your browser:<br>");
        html.append("<code style='background: #e9ecef; padding: 5px; display: block; margin-top: 10px;'>https://your-portal-url.com/student/results</code></p>");
        html.append("</div>");
        html.append("<div class='footer'>");
        html.append("<p>This is an automated email from Academic Analyzer. Please do not reply to this email.</p>");
        html.append("<p>&copy; 2026 Academic Analyzer. All rights reserved.</p>");
        html.append("</div>");
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }
    
    /**
     * Build plain text email content.
     */
    private static String buildPlainEmail(String studentName, String launchName, String customMessage) {
        StringBuilder plain = new StringBuilder();
        
        plain.append("RESULTS PUBLISHED\n");
        plain.append("==================\n\n");
        plain.append("Dear ").append(studentName).append(",\n\n");
        plain.append("Your results for '").append(launchName).append("' have been published and are now available for viewing.\n\n");
        
        if (customMessage != null && !customMessage.isEmpty()) {
            plain.append("MESSAGE FROM YOUR TEACHER:\n");
            plain.append("---------------------------\n");
            plain.append(customMessage).append("\n\n");
        }
        
        plain.append("To view your results, please log in to the student portal at:\n");
        plain.append("https://your-portal-url.com/student/results\n\n");
        plain.append("--\n");
        plain.append("This is an automated email from Academic Analyzer.\n");
        plain.append("Please do not reply to this email.\n\n");
        plain.append("© 2026 Academic Analyzer. All rights reserved.\n");
        
        return plain.toString();
    }
    
    /**
     * Validate MailerSend configuration.
     * 
     * @return true if configuration is valid
     */
    public static boolean isConfigured() {
        return API_KEY != null && !API_KEY.isEmpty() && 
               FROM_EMAIL != null && !FROM_EMAIL.isEmpty();
    }
    
    /**
     * Get configuration status message.
     */
    public static String getConfigStatus() {
        if (!isConfigured()) {
            return "❌ MailerSend not configured. Please add MAILERSEND_API_KEY to .env file";
        }
        return "✅ MailerSend configured correctly";
    }
}
