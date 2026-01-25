package com.sms.dashboard.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import com.sms.dao.SectionDAO.SectionInfo;
import com.sms.dao.SectionEditDAO;
import com.sms.analyzer.SectionAnalyzer;
import com.sms.analyzer.Student;
import com.sms.dao.StudentDAO;

/**
 * Hierarchical panel displaying sections organized by Year → Semester
 * with collapsible sections and modern themed design
 */
public class YearSemesterPanel extends JPanel {
    private static final Color BACKGROUND_COLOR = new Color(248, 250, 252);
    private static final Color CARD_BACKGROUND = Color.WHITE;
    private static final Color TEXT_PRIMARY = new Color(31, 41, 55);
    private static final Color TEXT_SECONDARY = new Color(107, 114, 128);
    private static final Color BORDER_COLOR = new Color(229, 231, 235);
    private static final Color ACCENT_COLOR = new Color(99, 102, 241);
    
    private int userId;
    private Runnable refreshCallback;
    private com.sms.dashboard.DashboardScreen dashboardScreen;
    
    public YearSemesterPanel(int userId, Runnable refreshCallback) {
        this(userId, refreshCallback, null);
    }
    
    public YearSemesterPanel(int userId, Runnable refreshCallback, com.sms.dashboard.DashboardScreen dashboardScreen) {
        this.userId = userId;
        this.refreshCallback = refreshCallback;
        this.dashboardScreen = dashboardScreen;
        
        setBackground(BACKGROUND_COLOR);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }
    
    /**
     * Update panel with sections grouped by year and semester
     */
    public void updateSections(List<SectionInfo> sections) {
        removeAll();
        
        if (sections == null || sections.isEmpty()) {
            add(createEmptyState());
            revalidate();
            repaint();
            return;
        }
        
        // Auto-assign year 2025 semester 1 to sections without year/semester
        for (SectionInfo section : sections) {
            if (section.academicYear == 0) {
                section.academicYear = 2025;
                section.semester = 1;
            }
        }
        
        // Group sections by year and semester
        Map<Integer, Map<Integer, List<SectionInfo>>> yearSemesterMap = groupSectionsByYearAndSemester(sections);
        
        // Create UI for each year
        List<Integer> years = new ArrayList<>(yearSemesterMap.keySet());
        Collections.sort(years, Collections.reverseOrder()); // Most recent year first
        
        for (Integer year : years) {
            if (year == 0) continue; // Skip sections without year
            
            JPanel yearPanel = createYearPanel(year, yearSemesterMap.get(year));
            add(yearPanel);
            add(Box.createVerticalStrut(15));
        }
        
        // Add sections without year/semester at the end
        if (yearSemesterMap.containsKey(0)) {
            JPanel unassignedPanel = createUnassignedPanel(yearSemesterMap.get(0));
            add(unassignedPanel);
        }
        
        revalidate();
        repaint();
    }
    
    private Map<Integer, Map<Integer, List<SectionInfo>>> groupSectionsByYearAndSemester(List<SectionInfo> sections) {
        Map<Integer, Map<Integer, List<SectionInfo>>> map = new HashMap<>();
        
        for (SectionInfo section : sections) {
            int year = section.academicYear;
            int semester = section.semester;
            
            map.putIfAbsent(year, new HashMap<>());
            map.get(year).putIfAbsent(semester, new ArrayList<>());
            map.get(year).get(semester).add(section);
        }
        
        return map;
    }
    
    private JPanel createYearPanel(int year, Map<Integer, List<SectionInfo>> semesterMap) {
        JPanel yearPanel = new JPanel();
        yearPanel.setBackground(CARD_BACKGROUND);
        yearPanel.setLayout(new BoxLayout(yearPanel, BoxLayout.Y_AXIS));
        yearPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            new EmptyBorder(15, 15, 15, 15)
        ));
        yearPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        // Year header
        JPanel yearHeader = new JPanel(new BorderLayout());
        yearHeader.setOpaque(false);
        yearHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        JLabel yearLabel = new JLabel("📅 Academic Year " + year);
        yearLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        yearLabel.setForeground(TEXT_PRIMARY);
        yearHeader.add(yearLabel, BorderLayout.WEST);
        
        yearPanel.add(yearHeader);
        yearPanel.add(Box.createVerticalStrut(10));
        
        // Add semesters
        List<Integer> semesters = new ArrayList<>(semesterMap.keySet());
        Collections.sort(semesters);
        
        for (Integer semester : semesters) {
            if (semester == 0) continue;
            
            JPanel semesterPanel = createSemesterPanel(semester, semesterMap.get(semester));
            yearPanel.add(semesterPanel);
            yearPanel.add(Box.createVerticalStrut(10));
        }
        
        return yearPanel;
    }
    
    private JPanel createSemesterPanel(int semester, List<SectionInfo> sections) {
        JPanel semesterPanel = new JPanel();
        semesterPanel.setBackground(BACKGROUND_COLOR);
        semesterPanel.setLayout(new BoxLayout(semesterPanel, BoxLayout.Y_AXIS));
        semesterPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        semesterPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        // Collapsible semester header
        JPanel semesterHeader = new JPanel(new BorderLayout());
        semesterHeader.setOpaque(false);
        semesterHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        semesterHeader.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JLabel semesterLabel = new JLabel("▼ Semester " + semester + " (" + sections.size() + " sections)");
        semesterLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        semesterLabel.setForeground(ACCENT_COLOR);
        semesterHeader.add(semesterLabel, BorderLayout.WEST);
        
        // Sections container
        JPanel sectionsContainer = new JPanel();
        sectionsContainer.setOpaque(false);
        sectionsContainer.setLayout(new BoxLayout(sectionsContainer, BoxLayout.X_AXIS));
        sectionsContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        
        // Add section cards
        for (SectionInfo section : sections) {
            SectionCardPanel card = new SectionCardPanel(section.sectionName, section.totalStudents);
            addContextMenuToCard(card, section);
            addClickHandlerToCard(card, section);
            sectionsContainer.add(card);
            sectionsContainer.add(Box.createHorizontalStrut(15));
        }
        
        // Add expand/collapse functionality
        final boolean[] isExpanded = {true};
        semesterHeader.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                isExpanded[0] = !isExpanded[0];
                sectionsContainer.setVisible(isExpanded[0]);
                semesterLabel.setText((isExpanded[0] ? "▼" : "►") + " Semester " + semester + " (" + sections.size() + " sections)");
                semesterPanel.revalidate();
                semesterPanel.repaint();
            }
        });
        
        semesterPanel.add(semesterHeader);
        semesterPanel.add(Box.createVerticalStrut(10));
        semesterPanel.add(sectionsContainer);
        
        return semesterPanel;
    }
    
    private JPanel createUnassignedPanel(Map<Integer, List<SectionInfo>> semesterMap) {
        JPanel unassignedPanel = new JPanel();
        unassignedPanel.setBackground(CARD_BACKGROUND);
        unassignedPanel.setLayout(new BoxLayout(unassignedPanel, BoxLayout.Y_AXIS));
        unassignedPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            new EmptyBorder(15, 15, 15, 15)
        ));
        unassignedPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        
        JLabel headerLabel = new JLabel("📂 Other Sections");
        headerLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        headerLabel.setForeground(TEXT_SECONDARY);
        unassignedPanel.add(headerLabel);
        unassignedPanel.add(Box.createVerticalStrut(10));
        
        // Add all unassigned sections
        for (List<SectionInfo> sections : semesterMap.values()) {
            JPanel sectionsRow = new JPanel();
            sectionsRow.setOpaque(false);
            sectionsRow.setLayout(new BoxLayout(sectionsRow, BoxLayout.X_AXIS));
            
            for (SectionInfo section : sections) {
                SectionCardPanel card = new SectionCardPanel(section.sectionName, section.totalStudents);
                addContextMenuToCard(card, section);
                addClickHandlerToCard(card, section);
                sectionsRow.add(card);
                sectionsRow.add(Box.createHorizontalStrut(15));
            }
            
            unassignedPanel.add(sectionsRow);
            unassignedPanel.add(Box.createVerticalStrut(10));
        }
        
        return unassignedPanel;
    }
    
    private void addContextMenuToCard(SectionCardPanel card, SectionInfo section) {
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBackground(CARD_BACKGROUND);
        
        // Edit menu item
        JMenuItem editItem = new JMenuItem("✏️ Edit Section");
        editItem.setFont(new Font("SansSerif", Font.PLAIN, 13));
        editItem.addActionListener(e -> showEditDialog(section));
        popupMenu.add(editItem);
        
        popupMenu.addSeparator();
        
        // Delete menu item
        JMenuItem deleteItem = new JMenuItem("🗑️ Delete Section");
        deleteItem.setFont(new Font("SansSerif", Font.PLAIN, 13));
        deleteItem.setForeground(new Color(220, 38, 38));
        deleteItem.addActionListener(e -> confirmAndDeleteSection(section));
        popupMenu.add(deleteItem);
        
        // Add right-click listener
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(card, e.getX(), e.getY());
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(card, e.getX(), e.getY());
                }
            }
        });
    }
    
    private void addClickHandlerToCard(SectionCardPanel card, SectionInfo section) {
        // Add combined mouse listener for click and hover effects
        MouseAdapter clickListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Only trigger on left-click, not right-click (which opens context menu)
                if (e.getButton() == MouseEvent.BUTTON1) {
                    openSectionRanking(section);
                }
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setHovered(true);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                card.setHovered(false);
            }
        };
        
        card.addMouseListener(clickListener);
    }
    
    private void openSectionRanking(SectionInfo section) {
        if (dashboardScreen != null) {
            dashboardScreen.showSectionRankingTable(section.id, section.sectionName);
        }
    }
    
    private void showEditDialog(SectionInfo section) {
        String newName = JOptionPane.showInputDialog(
            this,
            "Enter new section name:",
            section.sectionName
        );
        
        if (newName != null && !newName.trim().isEmpty() && !newName.equals(section.sectionName)) {
            SectionEditDAO editDAO = new SectionEditDAO();
            if (editDAO.updateSectionName(section.id, newName.trim(), userId)) {
                JOptionPane.showMessageDialog(
                    this,
                    "Section name updated successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
                );
                if (refreshCallback != null) {
                    refreshCallback.run();
                }
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "Failed to update section name.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
    
    private void confirmAndDeleteSection(SectionInfo section) {
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete section '" + section.sectionName + "'?\n\n" +
            "This will permanently delete:\n" +
            "• All students in this section\n" +
            "• All marks and grades\n" +
            "• All associated data\n\n" +
            "This action cannot be undone!",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            SectionEditDAO editDAO = new SectionEditDAO();
            if (editDAO.deleteSection(section.id, userId)) {
                JOptionPane.showMessageDialog(
                    this,
                    "Section deleted successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
                );
                if (refreshCallback != null) {
                    refreshCallback.run();
                }
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "Failed to delete section. Please try again.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
    
    private JPanel createEmptyState() {
        JPanel emptyPanel = new JPanel();
        emptyPanel.setOpaque(false);
        emptyPanel.setLayout(new BoxLayout(emptyPanel, BoxLayout.Y_AXIS));
        emptyPanel.setBorder(new EmptyBorder(50, 50, 50, 50));
        
        JLabel emptyIcon = new JLabel("📚", JLabel.CENTER);
        emptyIcon.setFont(new Font("SansSerif", Font.PLAIN, 48));
        emptyIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel emptyTitle = new JLabel("No Sections Yet");
        emptyTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        emptyTitle.setForeground(TEXT_SECONDARY);
        emptyTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel emptySubtitle = new JLabel("Create your first section to get started");
        emptySubtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        emptySubtitle.setForeground(TEXT_SECONDARY);
        emptySubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        emptyPanel.add(emptyIcon);
        emptyPanel.add(Box.createVerticalStrut(15));
        emptyPanel.add(emptyTitle);
        emptyPanel.add(Box.createVerticalStrut(8));
        emptyPanel.add(emptySubtitle);
        
        return emptyPanel;
    }
}
