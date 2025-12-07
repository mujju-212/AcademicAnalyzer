package com.sms.dashboard.components;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import org.jfree.chart.*;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import com.sms.dao.SectionDAO.SectionInfo;

public class GradeDistributionPanel extends JPanel {
    private ChartPanel chartPanel;
    private JPanel legendPanel;
    
    // Modern color palette
    private Color[] sectionColors = {
        new Color(147, 197, 253), // Light Blue
        new Color(167, 243, 208), // Light Green
        new Color(253, 224, 71),  // Yellow
        new Color(251, 207, 232), // Pink
        new Color(196, 181, 253), // Purple
        new Color(165, 180, 252), // Indigo
        new Color(253, 186, 116), // Orange
        new Color(156, 163, 175)  // Gray
    };
    
    public GradeDistributionPanel() {
        setPreferredSize(new Dimension(400, 280)); // Significantly reduced size
        setMaximumSize(new Dimension(600, 350));   // Set maximum size
        setOpaque(false);
        setLayout(new BorderLayout());
        
        // Create card panel
        JPanel cardPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw shadow
                for (int i = 0; i < 5; i++) {
                    g2.setColor(new Color(0, 0, 0, 20 - (i * 4)));
                    g2.fillRoundRect(i, i, getWidth() - i, getHeight() - i, 15, 15);
                }
                
                // Draw card
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 5, getHeight() - 5, 15, 15);
                
                g2.dispose();
            }
        };
        cardPanel.setOpaque(false);
        cardPanel.setLayout(new BorderLayout());
        cardPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15)); // Reduced padding
        
        // Title
        JLabel title = new JLabel("Section Distribution");
        title.setFont(new Font("SansSerif", Font.BOLD, 16)); // Reduced font size
        title.setForeground(new Color(17, 24, 39));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0)); // Reduced spacing
        cardPanel.add(title, BorderLayout.NORTH);
        
        // Chart container
        JPanel chartContainer = new JPanel(new BorderLayout());
        chartContainer.setOpaque(false);
        cardPanel.add(chartContainer, BorderLayout.CENTER);
        
        add(cardPanel);
        
        // Initialize with empty chart
        updateData(null);
    }
    
    public void updateData(List<SectionInfo> sections) {
        // Get the chart container
        JPanel cardPanel = (JPanel) getComponent(0);
        JPanel chartContainer = (JPanel) cardPanel.getComponent(1);
        
        // Remove old components
        chartContainer.removeAll();
        
        if (sections == null || sections.isEmpty()) {
            // Show empty state
            JLabel emptyLabel = new JLabel("No sections created yet");
            emptyLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            emptyLabel.setForeground(new Color(107, 114, 128));
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            chartContainer.add(emptyLabel, BorderLayout.CENTER);
        } else {
            // Create chart panel
            JPanel chartAndLegend = new JPanel(new BorderLayout(20, 0)); // Reduced gap
            chartAndLegend.setOpaque(false);
            
            // Create new chart
            JFreeChart chart = createSectionChart(sections);
            chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(150, 150)); // Much smaller chart
            chartPanel.setMaximumSize(new Dimension(200, 200));
            chartPanel.setOpaque(false);
            chartPanel.setBackground(Color.WHITE);
            chartAndLegend.add(chartPanel, BorderLayout.CENTER);
            
            // Create legend
            legendPanel = createLegend(sections);
            chartAndLegend.add(legendPanel, BorderLayout.EAST);
            
            chartContainer.add(chartAndLegend, BorderLayout.CENTER);
        }
        
        // Refresh the panel
        revalidate();
        repaint();
    }
    
    private JFreeChart createSectionChart(List<SectionInfo> sections) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        
        // Calculate total
        int totalStudents = 0;
        for (SectionInfo section : sections) {
            totalStudents += section.totalStudents;
        }
        
        for (SectionInfo section : sections) {
            dataset.setValue(section.sectionName, section.totalStudents);
        }
        
        JFreeChart chart = ChartFactory.createPieChart(null, dataset, false, false, false);
        chart.setBackgroundPaint(Color.WHITE);
        chart.setBorderVisible(false);
        
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setShadowPaint(null);
        
        // Show percentages on pie sections
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{2}"));
        plot.setLabelFont(new Font("SansSerif", Font.BOLD, 11));
        plot.setLabelPaint(Color.WHITE);
        plot.setLabelBackgroundPaint(null);
        plot.setLabelOutlinePaint(null);
        plot.setLabelShadowPaint(null);
        
        plot.setSimpleLabels(true);
        plot.setCircular(true);
        plot.setStartAngle(90);
        
        // Set colors for sections
        for (int i = 0; i < sections.size(); i++) {
            plot.setSectionPaint(sections.get(i).sectionName, sectionColors[i % sectionColors.length]);
        }
        
        return chart;
    }
    
    private JPanel createLegend(List<SectionInfo> sections) {
        JPanel legendPanel = new JPanel();
        legendPanel.setLayout(new BoxLayout(legendPanel, BoxLayout.Y_AXIS));
        legendPanel.setOpaque(false);
        
        // Calculate total
        int totalStudents = 0;
        for (SectionInfo section : sections) {
            totalStudents += section.totalStudents;
        }
        
        for (int i = 0; i < sections.size(); i++) {
            SectionInfo section = sections.get(i);
            double percentage = totalStudents > 0 ? (section.totalStudents * 100.0 / totalStudents) : 0;
            legendPanel.add(createLegendRow(section.sectionName, sectionColors[i % sectionColors.length], percentage));
            if (i < sections.size() - 1) {
                legendPanel.add(Box.createVerticalStrut(8)); // Reduced spacing
            }
        }
        
        return legendPanel;
    }
    
    private JPanel createLegendRow(String label, Color dotColor, double percentage) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); // Reduced gap
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(120, 20)); // Reduced size
        
        // Color dot
        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(dotColor);
                g2.fillOval(0, 0, 10, 10); // Smaller dot
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(10, 10)); // Smaller dot
        dot.setOpaque(false);
        
        JLabel textLabel = new JLabel(String.format("%s %.0f%%", label, percentage));
        textLabel.setFont(new Font("SansSerif", Font.PLAIN, 12)); // Smaller font
        textLabel.setForeground(new Color(17, 24, 39));
        
        row.add(dot);
        row.add(textLabel);
        return row;
    }
}