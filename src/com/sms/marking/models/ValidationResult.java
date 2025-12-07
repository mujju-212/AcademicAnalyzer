package com.sms.marking.models;

import java.util.*;

public class ValidationResult {
    private List<String> errors;
    private List<String> warnings;
    
    public ValidationResult() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }
    
    public void addError(String error) {
        errors.add(error);
    }
    
    public void addWarning(String warning) {
        warnings.add(warning);
    }
    
    public boolean isValid() {
        return errors.isEmpty();
    }
    
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public String getErrorMessage() {
        return String.join("\n", errors);
    }
    
    public String getWarningMessage() {
        return String.join("\n", warnings);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        if (!errors.isEmpty()) {
            sb.append("Errors:\n");
            for (String error : errors) {
                sb.append("- ").append(error).append("\n");
            }
        }
        
        if (!warnings.isEmpty()) {
            sb.append("Warnings:\n");
            for (String warning : warnings) {
                sb.append("- ").append(warning).append("\n");
            }
        }
        
        return sb.toString();
    }
}