package com.sms.resultlauncher;


public class ResultConfiguration {
    private String launchName;
    private boolean sendEmailNotification;
    private String emailSubject;
    private String emailMessage;
    private boolean allowPdfDownload;
    private boolean showDetailedResults;
    
    // Constructors
    public ResultConfiguration() {
        this.sendEmailNotification = true;
        this.allowPdfDownload = true;
        this.showDetailedResults = true;
        this.emailSubject = "Your Academic Results are Now Available";
        this.emailMessage = "Dear Student,\n\nYour academic results are now available for viewing. Please log in to the student portal to access your results.\n\nBest regards,\nAcademic Team";
    }
    
    public ResultConfiguration(String launchName, boolean sendEmailNotification, 
                             String emailSubject, String emailMessage, 
                             boolean allowPdfDownload, boolean showDetailedResults) {
        this.launchName = launchName;
        this.sendEmailNotification = sendEmailNotification;
        this.emailSubject = emailSubject;
        this.emailMessage = emailMessage;
        this.allowPdfDownload = allowPdfDownload;
        this.showDetailedResults = showDetailedResults;
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
}