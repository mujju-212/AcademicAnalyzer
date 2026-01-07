package com.sms.analyzer.utils;

import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.sms.analyzer.Student;
import com.sms.calculation.models.Component;
import com.sms.calculation.models.CalculationResult;

public class PDFExporter {
    
    // Existing exportStudentReport method remains the same...
    
    public boolean exportCompleteStudentReport(Student student, 
                                             Map<String, List<Component>> subjectComponents,
                                             Map<String, CalculationResult> subjectResults,
                                             String filePath) {
        try {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();
            
            // Add title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD);
            Paragraph title = new Paragraph("Complete Academic Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            
            // Student information
            addStudentInfo(document, student);
            
            // Overall performance summary
            addOverallSummary(document, subjectResults);
            
            // Subject-wise detailed analysis
            for (Map.Entry<String, List<Component>> entry : subjectComponents.entrySet()) {
                String subject = entry.getKey();
                List<Component> components = entry.getValue();
                CalculationResult result = subjectResults.get(subject);
                
                if (result != null) {
                    addSubjectAnalysis(document, subject, components, result);
                }
            }
            
            document.close();
            return true;
            
        } catch (Exception e) {
            System.err.println("Error exporting complete PDF report: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private void addStudentInfo(Document document, Student student) throws DocumentException {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
        
        Paragraph studentHeader = new Paragraph("Student Information", headerFont);
        studentHeader.setSpacingAfter(10);
        document.add(studentHeader);
        
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingAfter(20);
        
        addTableRow(infoTable, "Name:", student.getName(), normalFont);
        addTableRow(infoTable, "Roll Number:", student.getRollNumber(), normalFont);
        addTableRow(infoTable, "Section:", student.getSection(), normalFont);
        addTableRow(infoTable, "Academic Year:", "2023-2024", normalFont);
        
        document.add(infoTable);
    }
    
    private void addOverallSummary(Document document, Map<String, CalculationResult> subjectResults) 
            throws DocumentException {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
        
        Paragraph summaryHeader = new Paragraph("Overall Performance Summary", headerFont);
        summaryHeader.setSpacingAfter(10);
        document.add(summaryHeader);
        
        // Calculate overall statistics
        double totalObtained = 0, totalPossible = 0;
        int subjectCount = 0;
        
        for (CalculationResult result : subjectResults.values()) {
            totalObtained += result.getTotalObtained();
            totalPossible += result.getTotalPossible();
            subjectCount++;
        }
        
        double overallPercentage = totalPossible > 0 ? (totalObtained / totalPossible) * 100 : 0;
        String overallGrade = calculateGrade(overallPercentage);
        
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingAfter(20);
        
        addTableRow(summaryTable, "Total Subjects:", String.valueOf(subjectCount), normalFont);
        addTableRow(summaryTable, "Overall Marks:", String.format("%.1f/%.1f", totalObtained, totalPossible), normalFont);
        addTableRow(summaryTable, "Overall Percentage:", String.format("%.2f%%", overallPercentage), normalFont);
        addTableRow(summaryTable, "Overall Grade:", overallGrade, normalFont);
        
        document.add(summaryTable);
    }
    
    private void addSubjectAnalysis(Document document, String subject, List<Component> components, 
                                  CalculationResult result) throws DocumentException {
        Font subjectFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
        Font smallFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        
        // Subject header
        Paragraph subjectHeader = new Paragraph(subject + " - Detailed Analysis", subjectFont);
        subjectHeader.setSpacingAfter(10);
        document.add(subjectHeader);
        
        // Subject summary
        PdfPTable subjectSummary = new PdfPTable(4);
        subjectSummary.setWidthPercentage(100);
        subjectSummary.setSpacingAfter(15);
        
        // Header row
        addTableCell(subjectSummary, "Total Marks", normalFont, true);
        addTableCell(subjectSummary, "Percentage", normalFont, true);
        addTableCell(subjectSummary, "Grade", normalFont, true);
        addTableCell(subjectSummary, "SGPA", normalFont, true);
        
        // Data row
        addTableCell(subjectSummary, String.format("%.1f/%.1f", result.getTotalObtained(), result.getTotalPossible()), normalFont, false);
        addTableCell(subjectSummary, String.format("%.2f%%", result.getFinalPercentage()), normalFont, false);
        addTableCell(subjectSummary, result.getGrade(), normalFont, false);
        addTableCell(subjectSummary, String.format("%.2f", result.getSgpa()), normalFont, false);
        
        document.add(subjectSummary);
        
        // Component breakdown
        Paragraph componentHeader = new Paragraph("Component Breakdown:", normalFont);
        componentHeader.setSpacingAfter(8);
        document.add(componentHeader);
        
        PdfPTable componentTable = new PdfPTable(4);
        componentTable.setWidthPercentage(100);
        componentTable.setSpacingAfter(20);
        
        // Component table headers
        addTableCell(componentTable, "Component", smallFont, true);
        addTableCell(componentTable, "Type", smallFont, true);
        addTableCell(componentTable, "Marks", smallFont, true);
        addTableCell(componentTable, "Percentage", smallFont, true);
        
        // Component data
        for (Component component : components) {
            if (component.isCounted()) {
                addTableCell(componentTable, component.getName(), smallFont, false);
                addTableCell(componentTable, component.getType(), smallFont, false);
                addTableCell(componentTable, String.format("%.1f/%.1f", component.getObtainedMarks(), component.getMaxMarks()), smallFont, false);
                addTableCell(componentTable, String.format("%.1f%%", component.getPercentage()), smallFont, false);
            }
        }
        
        document.add(componentTable);
    }
    
    private void addTableRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        table.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }
    
    private void addTableCell(PdfPTable table, String text, Font font, boolean isHeader) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(8);
        
        if (isHeader) {
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        }
        
        table.addCell(cell);
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