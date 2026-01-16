package com.sms.resultlauncher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

import com.sms.resultlauncher.ResultLauncher;
import com.sms.resultlauncher.ResultLauncherUtils;
import com.sms.dao.AnalyzerDAO;
import com.sms.analyzer.Student;
import com.sms.login.LoginScreen;

public class StudentSelectionPanel extends JPanel {
    
    private ResultLauncher parentLauncher;
    private JPanel studentsPanel;
    private JScrollPane scrollPane;
    private JCheckBox selectAllCheckbox;
    private JLabel selectionCountLabel;
    private JTextField searchField;
    private Map<Integer, JCheckBox> studentCheckboxes;
    private List<Student> currentStudents;
    private List<Student> filteredStudents;
    
    public StudentSelectionPanel(ResultLauncher parent) {
        this.parentLauncher = parent;
        this.studentCheckboxes = new HashMap<>();
        this.currentStudents = new ArrayList<>();
        this.filteredStudents = new ArrayList<>();
        
        setPreferredSize(new Dimension(380, 280)); // Increased height
        setMaximumSize(new Dimension(380, 280));
        setMinimumSize(new Dimension(380, 280));
        
        initializeUI();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        setOpaque(false);
        
        JPanel cardPanel = ResultLauncherUtils.createModernCard();
        cardPanel.setLayout(new BorderLayout());
        
        JPanel headerPanel = createHeaderPanel();
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        JPanel searchPanel = createSearchPanel();
        searchPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);
        topPanel.add(headerPanel);
        topPanel.add(searchPanel);
        
        studentsPanel = new JPanel();
        studentsPanel.setLayout(new BoxLayout(studentsPanel, BoxLayout.Y_AXIS));
        studentsPanel.setBackground(ResultLauncherUtils.CARD_COLOR);
        studentsPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        scrollPane = new JScrollPane(studentsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(0, 160)); // Increased from 130
        scrollPane.getViewport().setBackground(ResultLauncherUtils.CARD_COLOR);
        
        JPanel footerPanel = createFooterPanel();
        footerPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        cardPanel.add(topPanel, BorderLayout.NORTH);
        cardPanel.add(scrollPane, BorderLayout.CENTER);
        cardPanel.add(footerPanel, BorderLayout.SOUTH);
        
        add(cardPanel, BorderLayout.CENTER);
        
        showInitialMessage();
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("👥 Select Students");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(ResultLauncherUtils.PRIMARY_COLOR);
        
        leftPanel.add(titleLabel);
        
        selectAllCheckbox = new JCheckBox("Select All");
        selectAllCheckbox.setFont(new Font("SansSerif", Font.BOLD, 12));
        selectAllCheckbox.setOpaque(false);
        selectAllCheckbox.setSelected(false); // Initialize to unchecked
        selectAllCheckbox.setEnabled(false);
        selectAllCheckbox.addActionListener(e -> {
            toggleSelectAll();
        });
        
        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(selectAllCheckbox, BorderLayout.EAST);
        
        return panel;
    }
    
    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        
        JLabel searchLabel = new JLabel("\ud83d\udd0d");
        searchLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        searchField = new JTextField();
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 12));
        searchField.putClientProperty("JTextField.placeholderText", "Search by name or roll number...");
        searchField.setEnabled(false);
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                filterStudents();
            }
        });
        
        panel.add(searchLabel, BorderLayout.WEST);
        panel.add(searchField, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        selectionCountLabel = new JLabel("Select a section first");
        selectionCountLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        selectionCountLabel.setForeground(ResultLauncherUtils.TEXT_SECONDARY);
        
        panel.add(selectionCountLabel, BorderLayout.WEST);
        
        return panel;
    }
    
    private void showInitialMessage() {
        studentsPanel.removeAll();
        
        JLabel messageLabel = new JLabel("Select a section to load students");
        messageLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));
        messageLabel.setForeground(ResultLauncherUtils.TEXT_SECONDARY);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        studentsPanel.add(Box.createVerticalGlue());
        studentsPanel.add(messageLabel);
        studentsPanel.add(Box.createVerticalGlue());
        
        revalidate();
        repaint();
    }
    
    public void loadStudentsForSection(int sectionId) {
        studentsPanel.removeAll();
        studentCheckboxes.clear();
        
        JLabel loadingLabel = new JLabel("Loading students...");
        loadingLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));
        loadingLabel.setForeground(ResultLauncherUtils.TEXT_SECONDARY);
        loadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        studentsPanel.add(Box.createVerticalGlue());
        studentsPanel.add(loadingLabel);
        studentsPanel.add(Box.createVerticalGlue());
        
        revalidate();
        repaint();
        
        SwingWorker<List<Student>, Void> worker = new SwingWorker<List<Student>, Void>() {
            @Override
            protected List<Student> doInBackground() throws Exception {
                AnalyzerDAO dao = new AnalyzerDAO();
                return dao.getStudentsBySection(sectionId, LoginScreen.currentUserId);
            }

            @Override
            protected void done() {
                try {
                    currentStudents = get();
                    filteredStudents = new ArrayList<>(currentStudents);
                    searchField.setEnabled(true);
                    searchField.setText("");
                    updateStudentsList();
                    
                } catch (Exception e) {
                    System.err.println("Error loading students: " + e.getMessage());
                    e.printStackTrace();
                    showErrorMessage("Error loading students: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
    
    private void updateStudentsList() {
        studentsPanel.removeAll();
        studentCheckboxes.clear();
        
        List<Student> studentsToDisplay = filteredStudents.isEmpty() ? currentStudents : filteredStudents;
        
        if (studentsToDisplay.isEmpty()) {
            String message = searchField != null && !searchField.getText().trim().isEmpty() 
                ? "No students match your search" 
                : "No students found in this section";
            
            JLabel noStudentsLabel = new JLabel(message);
            noStudentsLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));
            noStudentsLabel.setForeground(ResultLauncherUtils.TEXT_SECONDARY);
            noStudentsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            studentsPanel.add(Box.createVerticalGlue());
            studentsPanel.add(noStudentsLabel);
            studentsPanel.add(Box.createVerticalGlue());
        } else {
            for (Student student : studentsToDisplay) {
                JPanel studentPanel = createStudentPanel(student);
                studentsPanel.add(studentPanel);
                studentsPanel.add(Box.createVerticalStrut(5));
            }
        }
        
        selectAllCheckbox.setEnabled(!studentsToDisplay.isEmpty());
        updateSelectionCount();
        updateSelectAllState();
        
        revalidate();
        repaint();
    }
    
    private JPanel createStudentPanel(Student student) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));
        
        JCheckBox checkbox = new JCheckBox();
        checkbox.setOpaque(false);
        checkbox.setSelected(true); // Default to selected
        checkbox.addActionListener(e -> {
            updateSelectionCount();
            updateSelectAllState();
            notifySelectionChanged();
        });
        
        studentCheckboxes.put(student.getId(), checkbox);
        
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        
        JLabel nameLabel = new JLabel(student.getName());
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        nameLabel.setForeground(ResultLauncherUtils.TEXT_PRIMARY);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel detailsLabel = new JLabel("Roll: " + student.getRollNumber() + " | Section: " + student.getSection());
        detailsLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        detailsLabel.setForeground(ResultLauncherUtils.TEXT_SECONDARY);
        detailsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        infoPanel.add(nameLabel);
        infoPanel.add(detailsLabel);
        
        panel.add(checkbox, BorderLayout.WEST);
        panel.add(infoPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void showErrorMessage(String message) {
        studentsPanel.removeAll();
        
        JLabel errorLabel = new JLabel("Error: " + message);
        errorLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));
        errorLabel.setForeground(ResultLauncherUtils.DANGER_COLOR);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        studentsPanel.add(Box.createVerticalGlue());
        studentsPanel.add(errorLabel);
        studentsPanel.add(Box.createVerticalGlue());
        
        revalidate();
        repaint();
    }
    
    private void filterStudents() {
        String searchText = searchField.getText().trim().toLowerCase();
        
        if (searchText.isEmpty()) {
            filteredStudents = new ArrayList<>(currentStudents);
        } else {
            filteredStudents = new ArrayList<>();
            for (Student student : currentStudents) {
                String name = student.getName().toLowerCase();
                String roll = student.getRollNumber().toLowerCase();
                if (name.contains(searchText) || roll.contains(searchText)) {
                    filteredStudents.add(student);
                }
            }
        }
        
        updateStudentsList();
        selectAllCheckbox.setEnabled(!filteredStudents.isEmpty());
        updateSelectionCount();
    }
    
    private void toggleSelectAll() {
        boolean selectAll = selectAllCheckbox.isSelected();
        System.out.println("=== TOGGLE SELECT ALL: " + selectAll + " ===");
        System.out.println("Checkboxes in map: " + studentCheckboxes.size());
        
        int changed = 0;
        for (JCheckBox checkbox : studentCheckboxes.values()) {
            checkbox.setSelected(selectAll);
            changed++;
        }
        
        System.out.println("Changed " + changed + " checkboxes");
        updateSelectionCount();
        notifySelectionChanged();
    }
    
    private void updateSelectionCount() {
        int selectedCount = 0;
        for (JCheckBox checkbox : studentCheckboxes.values()) {
            if (checkbox.isSelected()) {
                selectedCount++;
            }
        }
        
        int displayedCount = studentCheckboxes.size();
        
        if (displayedCount == 0) {
            selectionCountLabel.setText("No students available");
        } else {
            selectionCountLabel.setText(selectedCount + " of " + displayedCount + " students selected");
        }
    }
    
    private void updateSelectAllState() {
        if (studentCheckboxes.isEmpty()) {
            selectAllCheckbox.setSelected(false);
            return;
        }
        
        int totalStudents = studentCheckboxes.size();
        int selectedStudents = 0;
        
        for (JCheckBox checkbox : studentCheckboxes.values()) {
            if (checkbox.isSelected()) {
                selectedStudents++;
            }
        }
        
        // Remove listener temporarily to avoid triggering toggleSelectAll
        ActionListener[] listeners = selectAllCheckbox.getActionListeners();
        for (ActionListener listener : listeners) {
            selectAllCheckbox.removeActionListener(listener);
        }
        
        // Update state: only check if ALL displayed students are selected
        selectAllCheckbox.setSelected(selectedStudents == totalStudents && totalStudents > 0);
        
        // Re-add listeners
        for (ActionListener listener : listeners) {
            selectAllCheckbox.addActionListener(listener);
        }
    }
    
    private void notifySelectionChanged() {
        List<Integer> selectedStudentIds = new ArrayList<>();
        
        for (Map.Entry<Integer, JCheckBox> entry : studentCheckboxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selectedStudentIds.add(entry.getKey());
            }
        }
        
        parentLauncher.onStudentsSelected(selectedStudentIds);
    }
    
    public List<Integer> getSelectedStudentIds() {
        List<Integer> selectedIds = new ArrayList<>();
        for (Map.Entry<Integer, JCheckBox> entry : studentCheckboxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selectedIds.add(entry.getKey());
            }
        }
        return selectedIds;
    }
    
    /**
     * Preselect students by their IDs (for edit mode).
     */
    public void preselectStudents(List<Integer> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return;
        }
        
        Set<Integer> idsToSelect = new HashSet<>(studentIds);
        int selectedCount = 0;
        
        for (Map.Entry<Integer, JCheckBox> entry : studentCheckboxes.entrySet()) {
            if (idsToSelect.contains(entry.getKey())) {
                entry.getValue().setSelected(true);
                selectedCount++;
            } else {
                entry.getValue().setSelected(false);
            }
        }
        
        // Update the select all checkbox
        selectAllCheckbox.setSelected(selectedCount == studentCheckboxes.size());
        
        // Update selection count label
        updateSelectionCount();
        
        System.out.println("Preselected " + selectedCount + " students");
    }

}
