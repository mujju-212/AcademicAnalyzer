package com.sms.login;

/**
 * Legacy compatibility class - stores current user ID for backward compatibility
 * The actual authentication is now handled by AuthenticationFrame
 */
public class LoginScreen {
    // Static field to store current user ID for legacy code compatibility
    public static int currentUserId = -1;
}
