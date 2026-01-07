package com.sms.analyzer.utils;

import java.awt.*;
import java.awt.print.*;
import java.util.List;
import java.util.Map;

import com.sms.analyzer.Student;
import com.sms.calculation.models.Component;
import com.sms.calculation.models.CalculationResult;

public class ReportPrinter implements Printable {
    
    private Student student;
    private Map<String, List<Component>> subjectComponents;
    private Map<String, CalculationResult> subjectResults;
    private boolean isCompleteReport;
    
    // Existing printStudentReport method remains the same...
    
    public boolean printCompleteStudentReport(Student student, 
                                            Map<String, List<Component>> subjectComponents,
                                            Map<String, CalculationResult> subjectResults) {
        this.student = student;
        this.subjectComponents = subjectComponents;
        this.subjectResults = subjectResults;
        this.isCompleteReport = true;
        
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable(this);
            
            if (job.printDialog()) {
                job.print();
                return true;
            }
            return false;
            
        } catch (PrinterException e) {
            System.err.println("Error printing complete report: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (isCompleteReport) {
            return printCompleteReport(graphics, pageFormat, pageIndex);
        } else {
            // Handle single subject report (existing logic)
            return NO_SUCH_PAGE;
        }
    }
    
    private int printCompleteReport(Graphics graphics, PageFormat pageFormat, int pageIndex) {
        if (pageIndex > 0) return NO_SUCH_PAGE;
        
        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        
        int y = 50;
        Font titleFont = new Font("Arial", Font.BOLD, 18);
        Font headerFont = new Font("Arial", Font.BOLD, 14);
        Font normalFont = new Font("Arial", Font.PLAIN, 12);
        
        // Title
        g2d.setFont(titleFont);
        g2d.drawString("Complete Academic Report", 200, y);
        y += 40;
        
        // Student info
        g2d.setFont(headerFont);
        g2d.drawString("Student Information:", 50, y);
        y += 25;
        
        g2d.setFont(normalFont);
        g2d.drawString("Name: " + student.getName(), 70, y);
        y += 20;
        g2d.drawString("Roll Number: " + student.getRollNumber(), 70, y);
        y += 20;
        g2d.drawString("Section: " + student.getSection(), 70, y);
        y += 30;
        
        // Overall summary
        g2d.setFont(headerFont);
        g2d.drawString("Overall Performance:", 50, y);
        y += 25;
        
        // Calculate overall stats
        double totalObtained = 0, totalPossible = 0;
        for (CalculationResult result : subjectResults.values()) {
            totalObtained += result.getTotalObtained();
            totalPossible += result.getTotalPossible();
        }
        
        double overallPercentage = totalPossible > 0 ? (totalObtained / totalPossible) * 100 : 0;
        String overallGrade = calculateGrade(overallPercentage);
        
        g2d.setFont(normalFont);
        g2d.drawString(String.format("Overall Marks: %.1f/%.1f (%.2f%%)", totalObtained, totalPossible, overallPercentage), 70, y);
        y += 20;
        g2d.drawString("Overall Grade: " + overallGrade, 70, y);
        y += 30;
        
        // Subject-wise summary
        g2d.setFont(headerFont);
        g2d.drawString("Subject-wise Performance:", 50, y);
        y += 25;
        
        g2d.setFont(normalFont);
        for (Map.Entry<String, CalculationResult> entry : subjectResults.entrySet()) {
            String subject = entry.getKey();
            CalculationResult result = entry.getValue();
            
            g2d.drawString(String.format("%s: %.1f/%.1f (%.2f%%) - Grade: %s", 
                subject, result.getTotalObtained(), result.getTotalPossible(), 
                result.getFinalPercentage(), result.getGrade()), 70, y);
            y += 20;
            
            // Check if we need a new page
            if (y > pageFormat.getImageableHeight() - 50) {
                break;
            }
        }
        
        return PAGE_EXISTS;
    }
    
    private String calculateGrade(double percentage) {
        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B";
        if (percentage >= 60) return "C";
        if (percentage >= 50) return "D";
        if (percentage >= 40) return "E";
        return "F";
    }
}