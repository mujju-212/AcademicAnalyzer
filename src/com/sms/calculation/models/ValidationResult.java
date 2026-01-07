package com.sms.calculation.models;

import java.util.List;

public class ValidationResult {
    private boolean isValid;
    private List<String> missingComponents;
    private List<String> invalidComponents;
    private List<String> warnings;
    private String message;

    // Constructors
    public ValidationResult() {}

    public ValidationResult(boolean isValid, String message) {
        this.isValid = isValid;
        this.message = message;
    }

    // Getters and Setters
    public boolean isValid() { return isValid; }
    public void setValid(boolean valid) { isValid = valid; }

    public List<String> getMissingComponents() { return missingComponents; }
    public void setMissingComponents(List<String> missingComponents) { this.missingComponents = missingComponents; }

    public List<String> getInvalidComponents() { return invalidComponents; }
    public void setInvalidComponents(List<String> invalidComponents) { this.invalidComponents = invalidComponents; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    // Utility methods
    public boolean hasMissingComponents() {
        return missingComponents != null && !missingComponents.isEmpty();
    }

    public boolean hasInvalidComponents() {
        return invalidComponents != null && !invalidComponents.isEmpty();
    }

    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }

    public int getTotalIssues() {
        int count = 0;
        if (hasMissingComponents()) count += missingComponents.size();
        if (hasInvalidComponents()) count += invalidComponents.size();
        if (hasWarnings()) count += warnings.size();
        return count;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ValidationResult{");
        sb.append("isValid=").append(isValid);
        if (message != null) sb.append(", message='").append(message).append("'");
        if (hasMissingComponents()) sb.append(", missing=").append(missingComponents.size());
        if (hasInvalidComponents()) sb.append(", invalid=").append(invalidComponents.size());
        if (hasWarnings()) sb.append(", warnings=").append(warnings.size());
        sb.append("}");
        return sb.toString();
    }
}