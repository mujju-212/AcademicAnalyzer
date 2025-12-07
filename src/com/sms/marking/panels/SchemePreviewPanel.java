package com.sms.marking.panels;

import javax.swing.*;
import java.awt.*;
import com.sms.marking.models.*;
import com.sms.theme.ThemeManager;

public class SchemePreviewPanel extends JPanel {
    private ThemeManager themeManager;
    private MarkingScheme scheme;
    private JTextArea previewArea;
    
    public SchemePreviewPanel(MarkingScheme scheme) {
        this.themeManager = ThemeManager.getInstance();
        this.scheme = scheme;
        
        initializeUI();
        updatePreview();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(themeManager.getCardColor());
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(themeManager.getBorderColor()),
                "Scheme Preview",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 14),
                themeManager.getTextPrimaryColor()
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        previewArea = new JTextArea();
        previewArea.setEditable(false);
        previewArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        previewArea.setBackground(themeManager.getBackgroundColor());
        previewArea.setForeground(themeManager.getTextPrimaryColor());
        
        JScrollPane scrollPane = new JScrollPane(previewArea);
        scrollPane.setBorder(null);
        
        add(scrollPane, BorderLayout.CENTER);
    }
    
    public void updatePreview() {
        if (scheme == null) {
            previewArea.setText("No scheme configured");
            return;
        }
        
        StringBuilder preview = new StringBuilder();
        preview.append("MARKING SCHEME PREVIEW\n");
        preview.append("======================\n\n");
        
        preview.append("Scheme: ").append(scheme.getSchemeName()).append("\n");
        preview.append("Total Marks: ").append(scheme.getTotalMarks()).append("\n");
        preview.append("Internal: ").append(scheme.getTotalInternalMarks());
        preview.append(" | External: ").append(scheme.getTotalExternalMarks()).append("\n\n");
        
        // Validation status
        ValidationResult validation = scheme.validate();
        if (validation.isValid()) {
            preview.append("Status: ✓ Valid\n\n");
        } else {
            preview.append("Status: ✗ Invalid\n");
            preview.append("Errors:\n");
            for (String error : validation.getErrors()) {
                preview.append("  - ").append(error).append("\n");
            }
            preview.append("\n");
        }
        
        // Internal components
        preview.append("INTERNAL COMPONENTS\n");
        preview.append("-------------------\n");
        
        int internalTotal = 0;
        for (ComponentGroup group : scheme.getInternalGroups()) {
            preview.append("\n").append(group.getDisplayName()).append("\n");
            
            for (MarkingComponent comp : group.getComponents()) {
                preview.append("  • ").append(comp.toString());
                if (comp.isOptional()) {
                    preview.append(" [Optional]");
                }
                preview.append("\n");
            }
            
            internalTotal += group.getTotalGroupMarks();
        }
        
        preview.append("\nInternal Total: ").append(internalTotal).append(" marks\n");
        
        // External components
        preview.append("\n\nEXTERNAL COMPONENTS\n");
        preview.append("-------------------\n");
        
        int externalTotal = 0;
        for (ComponentGroup group : scheme.getExternalGroups()) {
            preview.append("\n").append(group.getDisplayName()).append("\n");
            
            for (MarkingComponent comp : group.getComponents()) {
                preview.append("  • ").append(comp.toString());
                if (comp.isOptional()) {
                    preview.append(" [Optional]");
                }
                preview.append("\n");
            }
            
            externalTotal += group.getTotalGroupMarks();
        }
        
        preview.append("\nExternal Total: ").append(externalTotal).append(" marks\n");
        
        // Example calculation
        preview.append("\n\nEXAMPLE CALCULATION\n");
        preview.append("-------------------\n");
        preview.append("If a student scores:\n");
        
        for (ComponentGroup group : scheme.getComponentGroups()) {
            preview.append("\n").append(group.getGroupName()).append(":\n");
            
            int componentCount = 0;
            for (MarkingComponent comp : group.getComponents()) {
                if (group.isBestOfGroup() && group.getSelectionCount() != null && 
                    componentCount >= group.getSelectionCount()) {
                    preview.append("  • ").append(comp.getComponentName())
                          .append(": (not counted)\n");
                } else {
                    double exampleScore = comp.getActualMaxMarks() * 0.8; // 80% score
                    preview.append("  • ").append(comp.getComponentName())
                          .append(": ").append(String.format("%.0f", exampleScore))
                          .append("/").append(comp.getActualMaxMarks()).append("\n");
                }
                componentCount++;
            }
        }
        
        previewArea.setText(preview.toString());
        previewArea.setCaretPosition(0);
    }
    
    public void setScheme(MarkingScheme scheme) {
        this.scheme = scheme;
        updatePreview();
    }
}