package com.sms.dao;

import java.sql.*;
import java.util.*;
import com.sms.database.DatabaseConnection;

public class StudentDAO {
    
    public static class StudentInfo {
        public int id;
        public String rollNumber;
        public String name;
        public int sectionId;
        public String sectionName;
        public String email;
        public String phone;
        public Map<String, Integer> marks;
        public Timestamp createdAt;  // Changed from Date to Timestamp
        public Timestamp updatedAt;  // Changed from Date to Timestamp
        
        public StudentInfo() {
            marks = new HashMap<>();
        }
    }
    public boolean testConnection() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("Database connection successful!");
            return true;
        } catch (SQLException e) {
            System.out.println("Database connection failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    public boolean addStudent(String rollNumber, String name, int sectionId, 
            String email, String phone, int createdBy) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("Roll: " + rollNumber);
            System.out.println("Name: " + name);
            System.out.println("SectionId: " + sectionId);
            System.out.println("CreatedBy: " + createdBy);
            
            // Check if section exists and belongs to user
            String checkSectionSQL = "SELECT id FROM sections WHERE id = ? AND created_by = ?";
            try (PreparedStatement checkSectionPS = conn.prepareStatement(checkSectionSQL)) {
                checkSectionPS.setInt(1, sectionId);
                checkSectionPS.setInt(2, createdBy);
                try (ResultSet rs = checkSectionPS.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Section not found or access denied: " + sectionId);
                        return false;
                    }
                }
            }

            // Check for duplicate roll number within the same section only
            String checkRollSQL = "SELECT id FROM students WHERE roll_number = ? AND section_id = ?";
            try (PreparedStatement checkRollPS = conn.prepareStatement(checkRollSQL)) {
                checkRollPS.setString(1, rollNumber);
                checkRollPS.setInt(2, sectionId);
                try (ResultSet rs = checkRollPS.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("Duplicate roll number in this section: " + rollNumber);
                        return false;
                    }
                }
            }

            // Insert student
            String insertSQL = "INSERT INTO students (roll_number, student_name, section_id, email, phone, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW())";
            try (PreparedStatement insertPS = conn.prepareStatement(insertSQL)) {
                insertPS.setString(1, rollNumber);
                insertPS.setString(2, name);
                insertPS.setInt(3, sectionId);
                insertPS.setString(4, email != null && !email.isEmpty() ? email : null);
                insertPS.setString(5, phone != null && !phone.isEmpty() ? phone : null);
                insertPS.setInt(6, createdBy);
                
                int result = insertPS.executeUpdate();
                System.out.println("Insert result: " + result);
                System.out.println("Insert success: " + (result > 0));
                return result > 0;
            }
            
        } catch (SQLException e) {
            System.out.println("=== SQL ERROR ===");
            System.out.println("Error Code: " + e.getErrorCode());
            System.out.println("SQL State: " + e.getSQLState());
            System.out.println("Message: " + e.getMessage());
            
            // Handle specific error cases
            if (e.getErrorCode() == 1062) { // MySQL duplicate entry error
                System.out.println("Duplicate entry detected - likely roll_number constraint");
            }
            
            e.printStackTrace();
            return false;
        }
    }


//Add this method to help with debugging
public boolean isStudentExists(String rollNumber) {
try (Connection conn = DatabaseConnection.getConnection();
PreparedStatement ps = conn.prepareStatement("SELECT id FROM students WHERE roll_number = ?")) {
ps.setString(1, rollNumber);
ResultSet rs = ps.executeQuery();
return rs.next();
} catch (SQLException e) {
e.printStackTrace();
return false;
}
}
    
    // Update student information
    public boolean updateStudent(int studentId, String name, String email, String phone, int updatedBy) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "UPDATE students SET student_name = ?, email = ?, phone = ?, updated_at = NOW() WHERE id = ? AND created_by = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, name);
            ps.setString(2, email.isEmpty() ? null : email);
            ps.setString(3, phone.isEmpty() ? null : phone);
            ps.setInt(4, studentId);
            ps.setInt(5, updatedBy);
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Update student roll number (with duplicate check)
    public boolean updateStudentRollNumber(int studentId, String newRollNumber, int updatedBy) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // First check if new roll number already exists in the same section
            String checkQuery = "SELECT COUNT(*) FROM students s1, students s2 " +
                               "WHERE s1.id = ? AND s2.roll_number = ? " +
                               "AND s1.section_id = s2.section_id AND s2.id != s1.id";
            
            PreparedStatement checkPs = conn.prepareStatement(checkQuery);
            checkPs.setInt(1, studentId);
            checkPs.setString(2, newRollNumber);
            ResultSet rs = checkPs.executeQuery();
            
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("Roll number " + newRollNumber + " already exists in this section");
                return false;
            }
            
            // Update roll number
            String updateQuery = "UPDATE students SET roll_number = ?, updated_at = NOW() WHERE id = ? AND created_by = ?";
            PreparedStatement updatePs = conn.prepareStatement(updateQuery);
            updatePs.setString(1, newRollNumber);
            updatePs.setInt(2, studentId);
            updatePs.setInt(3, updatedBy);
            
            return updatePs.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.out.println("Error updating roll number: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // Update student with all fields including roll number
    public boolean updateStudentComplete(int studentId, String rollNumber, String name, 
                                       String email, String phone, int updatedBy) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Check if new roll number already exists (excluding current student)
            String checkQuery = "SELECT COUNT(*) FROM students s1, students s2 " +
                               "WHERE s1.id = ? AND s2.roll_number = ? " +
                               "AND s1.section_id = s2.section_id AND s2.id != s1.id";
            
            PreparedStatement checkPs = conn.prepareStatement(checkQuery);
            checkPs.setInt(1, studentId);
            checkPs.setString(2, rollNumber);
            ResultSet rs = checkPs.executeQuery();
            
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("Roll number " + rollNumber + " already exists in this section");
                return false;
            }
            
            // Update all fields
            String updateQuery = "UPDATE students SET roll_number = ?, student_name = ?, email = ?, phone = ?, updated_at = NOW() " +
                               "WHERE id = ? AND created_by = ?";
            PreparedStatement updatePs = conn.prepareStatement(updateQuery);
            updatePs.setString(1, rollNumber);
            updatePs.setString(2, name);
            updatePs.setString(3, email.isEmpty() ? null : email);
            updatePs.setString(4, phone.isEmpty() ? null : phone);
            updatePs.setInt(5, studentId);
            updatePs.setInt(6, updatedBy);
            
            int result = updatePs.executeUpdate();
            System.out.println("Updated student " + studentId + " with roll number " + rollNumber + ": " + (result > 0));
            return result > 0;
            
        } catch (SQLException e) {
            System.out.println("Error updating student: " + e.getMessage());
            if (e.getErrorCode() == 1062) { // MySQL duplicate entry error
                System.out.println("Duplicate roll number constraint violation");
            }
            e.printStackTrace();
            return false;
        }
    }
    
    // Delete student
    public boolean deleteStudent(int studentId, int deletedBy) {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            // Delete marks first
            String deleteMarks = "DELETE FROM entered_exam_marks WHERE student_id = ?";
            ps = conn.prepareStatement(deleteMarks);
            ps.setInt(1, studentId);
            ps.executeUpdate();
            ps.close();
            
            // Delete student
            String deleteStudent = "DELETE FROM students WHERE id = ? AND created_by = ?";
            ps = conn.prepareStatement(deleteStudent);
            ps.setInt(1, studentId);
            ps.setInt(2, deletedBy);
            int rowsAffected = ps.executeUpdate();
            
            conn.commit();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
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
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    // Get students by section
    public List<StudentInfo> getStudentsBySection(int sectionId, int createdBy) {
        List<StudentInfo> students = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT s.*, sec.section_name FROM students s " +
                          "JOIN sections sec ON s.section_id = sec.id " +
                          "WHERE s.section_id = ? AND s.created_by = ? " +
                          "ORDER BY s.roll_number";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, sectionId);
            ps.setInt(2, createdBy);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                StudentInfo student = new StudentInfo();
                student.id = rs.getInt("id");
                student.rollNumber = rs.getString("roll_number");
                student.name = rs.getString("student_name");
                student.sectionId = rs.getInt("section_id");
                student.sectionName = rs.getString("section_name");
                student.email = rs.getString("email");
                student.phone = rs.getString("phone");
                student.createdAt = rs.getTimestamp("created_at");
                student.updatedAt = rs.getTimestamp("updated_at");
                
                students.add(student);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return students;
    }
    
    // Get student by roll number
    public StudentInfo getStudentByRollNumber(String rollNumber, int createdBy) {
        StudentInfo student = null;
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT s.*, sec.section_name FROM students s " +
                          "JOIN sections sec ON s.section_id = sec.id " +
                          "WHERE s.roll_number = ? AND s.created_by = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, rollNumber);
            ps.setInt(2, createdBy);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                student = new StudentInfo();
                student.id = rs.getInt("id");
                student.rollNumber = rs.getString("roll_number");
                student.name = rs.getString("student_name");
                student.sectionId = rs.getInt("section_id");
                student.sectionName = rs.getString("section_name");
                student.email = rs.getString("email");
                student.phone = rs.getString("phone");
                student.createdAt = rs.getTimestamp("created_at");
                student.updatedAt = rs.getTimestamp("updated_at");
                
                // Get marks
                student.marks = getStudentMarks(student.id);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return student;
    }
    
    // Get student marks
    private Map<String, Integer> getStudentMarks(int studentId) {
        Map<String, Integer> marks = new HashMap<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT sub.subject_name, sm.marks_obtained " +
                          "FROM entered_exam_marks sm " +
                          "JOIN subjects sub ON sm.subject_id = sub.id " +
                          "WHERE sm.student_id = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                marks.put(rs.getString("subject_name"), rs.getInt("marks_obtained"));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return marks;
    }
    
    // Get total student count for a user
    public int getTotalStudentCount(int userId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String query = "SELECT COUNT(*) FROM students WHERE created_by = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}