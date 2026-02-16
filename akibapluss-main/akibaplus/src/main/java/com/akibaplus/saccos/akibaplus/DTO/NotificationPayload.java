package com.akibaplus.saccos.akibaplus.DTO;

public class NotificationPayload {
    private String message;
    private String recipientGroup;
    private boolean sms;
    private boolean app;
    private boolean email;

    // Getters
    public String getMessage() { return message; }
    public String getRecipientGroup() { return recipientGroup; }
    public boolean isSms() { return sms; }
    public boolean isApp() { return app; }
    public boolean isEmail() { return email; }
}