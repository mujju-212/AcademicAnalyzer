package com.sms.resultlauncher;


public class ResultConfiguration {
    private String launchName;
    private boolean sendEmailNotification;
    private String emailSubject;
    private String emailMessage;
    private boolean allowPdfDownload;
    private boolean showDetailedResults;
    
    // Enhanced visibility controls
    private boolean showComponentMarks;
    private boolean showSubjectDetails;
    private boolean showRank;
    private boolean showClassStats;
    
    // Constructors
    public ResultConfiguration() {
        this.sendEmailNotification = true;
        this.allowPdfDownload = true;
        this.showDetailedResults = true;
        this.showComponentMarks = true;
        this.showSubjectDetails = true;
        this.showRank = false; // Privacy: default off
        this.showClassStats = true;
        this.emailSubject = "Your Academic Results are Now Available";
        this.emailMessage = "Dear Student,\n\nYour academic results are now available for viewing. Please log in to the student portal to access your results.\n\nBest regards,\nAcademic Team";
    }
    
    // Getters and Setters
    public String getLaunchName() { return launchName; }
    public void setLaunchName(String launchName) { this.launchName = launchName; }
    
    public boolean isSendEmailNotification() { return sendEmailNotification; }
    public void setSendEmailNotification(boolean sendEmailNotification) { this.sendEmailNotification = sendEmailNotification; }
    
    public String getEmailSubject() { return emailSubject; }
    public void setEmailSubject(String emailSubject) { this.emailSubject = emailSubject; }
    
    public String getEmailMessage() { return emailMessage; }
    public void setEmailMessage(String emailMessage) { this.emailMessage = emailMessage; }
    
    public boolean isAllowPdfDownload() { return allowPdfDownload; }
    public void setAllowPdfDownload(boolean allowPdfDownload) { this.allowPdfDownload = allowPdfDownload; }
    
    public boolean isShowDetailedResults() { return showDetailedResults; }
    public void setShowDetailedResults(boolean showDetailedResults) { this.showDetailedResults = showDetailedResults; }
    
    // Enhanced visibility controls getters/setters
    public boolean isShowComponentMarks() { return showComponentMarks; }
    public void setShowComponentMarks(boolean showComponentMarks) { this.showComponentMarks = showComponentMarks; }
    
    public boolean isShowSubjectDetails() { return showSubjectDetails; }
    public void setShowSubjectDetails(boolean showSubjectDetails) { this.showSubjectDetails = showSubjectDetails; }
    
    public boolean isShowRank() { return showRank; }
    public void setShowRank(boolean showRank) { this.showRank = showRank; }
    
    public boolean isShowClassStats() { return showClassStats; }
    public void setShowClassStats(boolean showClassStats) { this.showClassStats = showClassStats; }
}