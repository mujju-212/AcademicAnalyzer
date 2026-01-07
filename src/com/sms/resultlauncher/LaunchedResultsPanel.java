package com.sms.resultlauncher;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.sms.resultlauncher.ResultLauncher;
import com.sms.resultlauncher.ResultLauncherUtils;
import com.sms.resultlauncher.LaunchedResult;

public class LaunchedResultsPanel extends JPanel {
    
    private ResultLauncher parentLauncher;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private List<LaunchedResult> launchedResults;
    
    public LaunchedResultsPanel(ResultLauncher parent) {
        this.parentLauncher = parent;
        initializeUI();
        loadLaunchedResults();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        setOpaque(false);
        
        JPanel cardPanel = ResultLauncherUtils.createModernCard();
        cardPanel.setLayout(new BorderLayout());
        
        JPanel headerPanel = createHeaderPanel();
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        createTable();
        JScrollPane scrollPane = new JScrollPane(resultsTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(ResultLauncherUtils.CARD_COLOR);
        
        cardPanel.add(headerPanel, BorderLayout.NORTH);
        cardPanel.add(scrollPane, BorderLayout.CENTER);
        
        add(cardPanel, BorderLayout.CENTER);
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("🚀 Launched Results");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(ResultLauncherUtils.PRIMARY_COLOR);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setOpaque(false);
        
        JButton refreshButton = ResultLauncherUtils.createSecondaryButton("🔄 Refresh");
        refreshButton.setPreferredSize(new Dimension(100, 30));
        refreshButton.addActionListener(e -> refreshLaunchedResults());
        
        buttonPanel.add(refreshButton);
        
        panel.add(titleLabel, BorderLayout.WEST);
        panel.add(buttonPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    private void createTable() {
        String[] columns = {"Launch Name", "Section", "Students", "Components", "Status", "Launch Date", "Actions"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // Only actions column is editable
            }
        };
        
        resultsTable = new JTable(tableModel);
        resultsTable.setRowHeight(45);
        resultsTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        resultsTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        resultsTable.getTableHeader().setBackground(new Color(249, 250, 251));
        resultsTable.getTableHeader().setReorderingAllowed(false);
        resultsTable.setShowGrid(false);
        resultsTable.setIntercellSpacing(new Dimension(0, 1));
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Set column widths
        resultsTable.getColumnModel().getColumn(0).setPreferredWidth(150); // Launch Name
        resultsTable.getColumnModel().getColumn(1).setPreferredWidth(80);  // Section
        resultsTable.getColumnModel().getColumn(2).setPreferredWidth(70);  // Students
        resultsTable.getColumnModel().getColumn(3).setPreferredWidth(90);  // Components
        resultsTable.getColumnModel().getColumn(4).setPreferredWidth(80);  // Status
        resultsTable.getColumnModel().getColumn(5).setPreferredWidth(120); // Launch Date
        resultsTable.getColumnModel().getColumn(6).setPreferredWidth(150); // Actions
        
        // Custom cell renderer
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
                
                label.setBackground(ResultLauncherUtils.CARD_COLOR);
                label.setForeground(ResultLauncherUtils.TEXT_PRIMARY);
                
                if (column == 4) { // Status column
                    String status = value.toString();
                    if ("Active".equals(status)) {
                        label.setForeground(ResultLauncherUtils.SUCCESS_COLOR);
                        label.setText("🟢 Active");
                    } else {
                        label.setForeground(ResultLauncherUtils.DANGER_COLOR);
                        label.setText("🔴 Inactive");
                    }
                    label.setHorizontalAlignment(JLabel.CENTER);
                } else if (column >= 2 && column <= 3) { // Numbers
                    label.setHorizontalAlignment(JLabel.CENTER);
                } else if (column == 6) { // Actions
                    label.setText(""); // Actions will be handled by button renderer
                } else {
                    label.setHorizontalAlignment(JLabel.LEFT);
                }
                
                return label;
            }
        };
        
        // Apply renderer to all columns except actions
        for (int i = 0; i < resultsTable.getColumnCount() - 1; i++) {
            resultsTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
        
        // Custom renderer for actions column
        resultsTable.getColumnModel().getColumn(6).setCellRenderer(new ActionButtonRenderer());
        resultsTable.getColumnModel().getColumn(6).setCellEditor(new ActionButtonEditor());
        
        // Add mouse listener for row selection
        resultsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = resultsTable.getSelectedRow();
                    if (row != -1) {
                        viewResultDetails(row);
                    }
                }
            }
        });
    }
    
    public void refreshLaunchedResults() {
        loadLaunchedResults();
    }
    
    private void loadLaunchedResults() {
        SwingWorker<List<LaunchedResult>, Void> worker = new SwingWorker<List<LaunchedResult>, Void>() {
            @Override
            protected List<LaunchedResult> doInBackground() throws Exception {
                return parentLauncher.getDAO().getLaunchedResults();
            }

            @Override
            protected void done() {
                try {
                    launchedResults = get();
                    updateTable();
                } catch (Exception e) {
                    System.err.println("Error loading launched results: " + e.getMessage());
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(LaunchedResultsPanel.this,
                        "Error loading launched results: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
    
    private void updateTable() {
        tableModel.setRowCount(0);
        
        if (launchedResults != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            
            for (LaunchedResult result : launchedResults) {
                Object[] rowData = {
                    result.getLaunchName(),
                    result.getSectionName(),
                    result.getStudentCount(),
                    result.getComponentCount(),
                    result.isActive() ? "Active" : "Inactive",
                    result.getLaunchDate().format(formatter),
                    "" // Actions column will be handled by custom renderer
                };
                tableModel.addRow(rowData);
            }
        }
    }
    
    private void viewResultDetails(int row) {
        if (row >= 0 && row < launchedResults.size()) {
            LaunchedResult result = launchedResults.get(row);
            // Show result details dialog
            ResultDetailsDialog detailsDialog = new ResultDetailsDialog(
                SwingUtilities.getWindowAncestor(this), result);
            detailsDialog.setVisible(true);
        }
    }
    
    private void takeDownResult(int row) {
        if (row >= 0 && row < launchedResults.size()) {
            LaunchedResult result = launchedResults.get(row);
            
            int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to take down the result: " + result.getLaunchName() + "?\n" +
                "Students will no longer be able to access these results.",
                "Confirm Take Down",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            
            if (choice == JOptionPane.YES_OPTION) {
                SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                    @Override
                    protected Boolean doInBackground() throws Exception {
                        return parentLauncher.getDAO().takeDownResult(result.getId());
                    }

                    @Override
                    protected void done() {
                        try {
                            Boolean success = get();
                            if (success) {
                                JOptionPane.showMessageDialog(LaunchedResultsPanel.this,
                                    "Result taken down successfully!",
                                    "Success", JOptionPane.INFORMATION_MESSAGE);
                                refreshLaunchedResults();
                            } else {
                                JOptionPane.showMessageDialog(LaunchedResultsPanel.this,
                                    "Failed to take down result.",
                                    "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(LaunchedResultsPanel.this,
                                "Error taking down result: " + e.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                };
                worker.execute();
            }
        }
    }
    
    private void editResult(int row) {
        if (row >= 0 && row < launchedResults.size()) {
            LaunchedResult result = launchedResults.get(row);
            JOptionPane.showMessageDialog(this,
                "Edit functionality will be implemented soon!",
                "Feature Coming Soon", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    // Custom renderer for action buttons
    private class ActionButtonRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
        private JButton takeDownButton;
        private JButton editButton;
        
        public ActionButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
            setOpaque(true);
            
            takeDownButton = new JButton("Take Down");
            takeDownButton.setFont(new Font("SansSerif", Font.BOLD, 10));
            takeDownButton.setPreferredSize(new Dimension(80, 25));
            takeDownButton.setBackground(ResultLauncherUtils.DANGER_COLOR);
            takeDownButton.setForeground(Color.WHITE);
            takeDownButton.setBorderPainted(false);
            takeDownButton.setFocusPainted(false);
            
            editButton = new JButton("Edit");
            editButton.setFont(new Font("SansSerif", Font.BOLD, 10));
            editButton.setPreferredSize(new Dimension(60, 25));
            editButton.setBackground(ResultLauncherUtils.INFO_COLOR);
            editButton.setForeground(Color.WHITE);
            editButton.setBorderPainted(false);
            editButton.setFocusPainted(false);
            
            add(editButton);
            add(takeDownButton);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            if (row < launchedResults.size()) {
                LaunchedResult result = launchedResults.get(row);
                takeDownButton.setEnabled(result.isActive());
                if (!result.isActive()) {
                    takeDownButton.setText("Taken Down");
                    takeDownButton.setBackground(Color.GRAY);
                } else {
                    takeDownButton.setText("Take Down");
                    takeDownButton.setBackground(ResultLauncherUtils.DANGER_COLOR);
                }
            }
            
            return this;
        }
    }
    
    // Custom editor for action buttons
    private class ActionButtonEditor extends DefaultCellEditor {
        private JPanel panel;
        private JButton takeDownButton;
        private JButton editButton;
        private int currentRow;
        
        public ActionButtonEditor() {
            super(new JCheckBox());
            
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
            panel.setOpaque(true);
            
            takeDownButton = new JButton("Take Down");
            takeDownButton.setFont(new Font("SansSerif", Font.BOLD, 10));
            takeDownButton.setPreferredSize(new Dimension(80, 25));
            takeDownButton.setBackground(ResultLauncherUtils.DANGER_COLOR);
            takeDownButton.setForeground(Color.WHITE);
            takeDownButton.setBorderPainted(false);
            takeDownButton.setFocusPainted(false);
            takeDownButton.addActionListener(e -> {
                fireEditingStopped();
                takeDownResult(currentRow);
            });
            
            editButton = new JButton("Edit");
            editButton.setFont(new Font("SansSerif", Font.BOLD, 10));
            editButton.setPreferredSize(new Dimension(60, 25));
            editButton.setBackground(ResultLauncherUtils.INFO_COLOR);
            editButton.setForeground(Color.WHITE);
            editButton.setBorderPainted(false);
            editButton.setFocusPainted(false);
            editButton.addActionListener(e -> {
                fireEditingStopped();
                editResult(currentRow);
            });
            
            panel.add(editButton);
            panel.add(takeDownButton);
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            currentRow = row;
            
            if (row < launchedResults.size()) {
                LaunchedResult result = launchedResults.get(row);
                takeDownButton.setEnabled(result.isActive());
                if (!result.isActive()) {
                    takeDownButton.setText("Taken Down");
                    takeDownButton.setBackground(Color.GRAY);
                } else {
                    takeDownButton.setText("Take Down");
                    takeDownButton.setBackground(ResultLauncherUtils.DANGER_COLOR);
                }
            }
            
            return panel;
        }
        
        @Override
        public Object getCellEditorValue() {
            return "";
        }
    }
}