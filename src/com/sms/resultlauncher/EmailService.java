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
     * Build HTML email content with professional template.
     */
    private static String buildHtmlEmail(String studentName, String launchName, String customMessage) {
        String portalUrl = ConfigLoader.getResultPortalUrl();
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<style>");
        html.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; background-color: #f4f4f4; margin: 0; padding: 0; }");
        html.append(".email-wrapper { max-width: 600px; margin: 20px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }");
        html.append(".header { background: linear-gradient(135deg, #4285f4 0%, #34a853 100%); color: white; padding: 40px 30px; text-align: center; }");
        html.append(".header h1 { margin: 0; font-size: 28px; font-weight: 600; }");
        html.append(".header p { margin: 10px 0 0 0; font-size: 14px; opacity: 0.9; }");
        html.append(".content { padding: 40px 30px; background: #ffffff; }");
        html.append(".greeting { font-size: 16px; color: #333; margin-bottom: 20px; }");
        html.append(".info-box { background: #f8f9fa; border-left: 4px solid #4285f4; padding: 20px; margin: 25px 0; border-radius: 4px; }");
        html.append(".info-box h3 { margin: 0 0 10px 0; font-size: 16px; color: #4285f4; }");
        html.append(".message-box { background: #fff3cd; border-left: 4px solid #ffc107; padding: 20px; margin: 25px 0; border-radius: 4px; }");
        html.append(".message-box h3 { margin: 0 0 10px 0; font-size: 16px; color: #856404; }");
        html.append(".message-box p { margin: 0; color: #856404; line-height: 1.5; }");
        html.append(".button { display: inline-block; padding: 14px 32px; background: #4285f4; color: white !important; text-decoration: none; border-radius: 6px; margin: 25px 0; font-weight: 600; text-align: center; transition: background 0.3s; }");
        html.append(".button:hover { background: #3367d6; }");
        html.append(".link-text { font-size: 13px; color: #6c757d; margin-top: 15px; word-break: break-all; }");
        html.append(".link-url { background: #e9ecef; padding: 12px; display: block; margin-top: 10px; border-radius: 4px; color: #495057; font-family: monospace; font-size: 12px; }");
        html.append(".footer { background: #f8f9fa; padding: 30px; text-align: center; border-top: 1px solid #dee2e6; }");
        html.append(".footer p { margin: 5px 0; color: #6c757d; font-size: 13px; }");
        html.append(".footer .brand { color: #4285f4; font-weight: 600; }");
        html.append(".divider { height: 1px; background: #dee2e6; margin: 25px 0; }");
        html.append("@media only screen and (max-width: 600px) { .content { padding: 30px 20px; } .header { padding: 30px 20px; } }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='email-wrapper'>");
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1>🎓 Academic Results Published</h1>");
        html.append("<p>Your academic performance is now available</p>");
        html.append("</div>");
        
        // Content
        html.append("<div class='content'>");
        html.append("<p class='greeting'>Dear <strong>").append(studentName).append("</strong>,</p>");
        html.append("<p>We are pleased to inform you that your academic results have been published and are now available for viewing.</p>");
        
        // Result Info Box
        html.append("<div class='info-box'>");
        html.append("<h3>📊 Result Details</h3>");
        html.append("<p><strong>Assessment:</strong> ").append(launchName).append("</p>");
        html.append("<p><strong>Status:</strong> Published</p>");
        html.append("</div>");
        
        // Custom Message (if any)
        if (customMessage != null && !customMessage.isEmpty()) {
            html.append("<div class='message-box'>");
            html.append("<h3>📝 Message from your Instructor</h3>");
            html.append("<p>").append(customMessage.replace("\n", "<br>")).append("</p>");
            html.append("</div>");
        }
        
        html.append("<div class='divider'></div>");
        
        // Call to Action
        html.append("<p>Access your results by clicking the button below:</p>");
        html.append("<center>");
        html.append("<a href='").append(portalUrl).append("/results' class='button'>View My Results</a>");
        html.append("</center>");
        
        // Alternative Link
        html.append("<p class='link-text'>Or copy and paste this link into your browser:</p>");
        html.append("<div class='link-url'>").append(portalUrl).append("/results</div>");
        
        html.append("<div class='divider'></div>");
        
        html.append("<p style='font-size: 13px; color: #6c757d;'>");
        html.append("<strong>Need help?</strong> If you have any questions about your results, please contact your instructor or the academic office.");
        html.append("</p>");
        html.append("</div>");
        
        // Footer
        html.append("<div class='footer'>");
        html.append("<p>This is an automated notification from <span class='brand'>Academic Analyzer</span></p>");
        html.append("<p>Please do not reply to this email.</p>");
        html.append("<p style='margin-top: 15px;'>&copy; 2026 Academic Analyzer. All rights reserved.</p>");
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
        String portalUrl = ConfigLoader.getResultPortalUrl();
        StringBuilder plain = new StringBuilder();
        
        plain.append("=============================================\n");
        plain.append("   ACADEMIC RESULTS PUBLISHED\n");
        plain.append("=============================================\n\n");
        
        plain.append("Dear ").append(studentName).append(",\n\n");
        
        plain.append("We are pleased to inform you that your academic results have been published and are now available for viewing.\n\n");
        
        plain.append("RESULT DETAILS\n");
        plain.append("------------------------------------------\n");
        plain.append("Assessment: ").append(launchName).append("\n");
        plain.append("Status: Published\n\n");
        
        if (customMessage != null && !customMessage.isEmpty()) {
            plain.append("MESSAGE FROM YOUR INSTRUCTOR\n");
            plain.append("------------------------------------------\n");
            plain.append(customMessage).append("\n\n");
        }
        
        plain.append("ACCESS YOUR RESULTS\n");
        plain.append("------------------------------------------\n");
        plain.append("Visit the student portal to view your results:\n");
        plain.append(portalUrl).append("/results\n\n");
        
        plain.append("NEED HELP?\n");
        plain.append("------------------------------------------\n");
        plain.append("If you have any questions about your results, please contact your instructor or the academic office.\n\n");
        
        plain.append("--\n");
        plain.append("This is an automated notification from Academic Analyzer.\n");
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
