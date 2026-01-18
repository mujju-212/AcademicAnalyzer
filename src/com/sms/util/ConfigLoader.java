package com.sms.util;

import java.io.*;
import java.util.*;

/**
 * Environment configuration loader for Academic Analyzer
 * Loads configuration from .env file or environment variables
 */
public class ConfigLoader {
    private static final Map<String, String> config = new HashMap<>();
    private static boolean initialized = false;
    
    static {
        loadConfiguration();
    }
    
    private static void loadConfiguration() {
        if (initialized) return;
        
        try {
            boolean loaded = false;
            
            // Try 1: Look for .env next to the JAR file (for installed apps)
            try {
                String jarPath = ConfigLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
                File jarFile = new File(jarPath);
                File jarDir = jarFile.getParentFile();
                File envFile = new File(jarDir, ".env");
                
                if (envFile.exists()) {
                    loadFromFile(envFile);
                    System.out.println("Configuration loaded from .env file next to JAR: " + envFile.getAbsolutePath());
                    loaded = true;
                }
            } catch (Exception e) {
                System.err.println("Could not check for .env next to JAR: " + e.getMessage());
            }
            
            // Try 2: Look for .env in current working directory
            if (!loaded) {
                File envFile = new File(".env");
                if (envFile.exists()) {
                    loadFromFile(envFile);
                    System.out.println("Configuration loaded from .env file in current directory: " + envFile.getAbsolutePath());
                    loaded = true;
                }
            }
            
            // Try 3: Load from classpath resource (inside JAR)
            if (!loaded) {
                System.out.println("No external .env file found, trying classpath...");
                if (loadFromClasspath()) {
                    loaded = true;
                } else {
                    System.out.println("No .env found in classpath either");
                }
            }
            
            if (!loaded) {
                System.err.println("WARNING: No .env file found in any location!");
                System.err.println("Working directory: " + System.getProperty("user.dir"));
                System.err.println("Using environment variables or defaults");
            }
            
            // Override with system environment variables if they exist
            loadFromEnvironmentVariables();
            
            initialized = true;
            
        } catch (Exception e) {
            System.err.println("Error loading configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static boolean loadFromClasspath() {
        try (InputStream is = ConfigLoader.class.getResourceAsStream("/.env")) {
            if (is == null) {
                return false;
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    
                    // Skip empty lines and comments
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    
                    // Parse key=value pairs
                    int equalsIndex = line.indexOf('=');
                    if (equalsIndex > 0) {
                        String key = line.substring(0, equalsIndex).trim();
                        String value = line.substring(equalsIndex + 1).trim();
                        config.put(key, value);
                    }
                }
            }
            
            System.out.println("Configuration loaded from classpath .env resource");
            return true;
        } catch (IOException e) {
            System.err.println("Error loading .env from classpath: " + e.getMessage());
            return false;
        }
    }
    
    private static void loadFromFile(File envFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // Parse key=value pairs
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();
                    config.put(key, value);
                }
            }
        }
    }
    
    private static void loadFromEnvironmentVariables() {
        // Override with system environment variables
        Map<String, String> envVars = System.getenv();
        for (String key : config.keySet()) {
            if (envVars.containsKey(key)) {
                config.put(key, envVars.get(key));
            }
        }
    }
    
    /**
     * Get configuration value by key
     * @param key Configuration key
     * @return Configuration value or null if not found
     */
    public static String get(String key) {
        return config.get(key);
    }
    
    /**
     * Get configuration value by key with default value
     * @param key Configuration key
     * @param defaultValue Default value if key not found
     * @return Configuration value or default value
     */
    public static String get(String key, String defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }
    
    /**
     * Get integer configuration value
     * @param key Configuration key
     * @param defaultValue Default value if key not found or invalid
     * @return Integer value
     */
    public static int getInt(String key, int defaultValue) {
        try {
            String value = config.get(key);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Check if configuration key exists
     * @param key Configuration key
     * @return true if key exists
     */
    public static boolean has(String key) {
        return config.containsKey(key);
    }
    
    /**
     * Get database URL
     * @return Complete database URL
     */
    public static String getDatabaseUrl() {
        String host = get("DB_HOST");
        if (host == null || host.isEmpty()) {
            System.err.println("ERROR: DB_HOST not configured!");
            System.err.println("Please ensure .env file is present with Azure database credentials.");
            System.err.println("Current working directory: " + System.getProperty("user.dir"));
            return "jdbc:mysql://localhost:3306/academic_analyzer"; // Fallback to prevent crash
        }
        int port = getInt("DB_PORT", 3306);
        String dbName = get("DB_NAME", "academic_analyzer");
        return String.format("jdbc:mysql://%s:%d/%s", host, port, dbName);
    }
    
    /**
     * Get database username
     * @return Database username
     */
    public static String getDatabaseUsername() {
        String username = get("DB_USERNAME");
        if (username == null || username.isEmpty()) {
            System.err.println("ERROR: DB_USERNAME not configured!");
            return "root"; // Fallback
        }
        return username;
    }
    
    /**
     * Get database password
     * @return Database password
     */
    public static String getDatabasePassword() {
        String password = get("DB_PASSWORD");
        if (password == null || password.isEmpty()) {
            System.err.println("ERROR: DB_PASSWORD not configured!");
            return ""; // Fallback
        }
        return password;
    }
    
    /**
     * Check if database configuration is valid
     * @return true if all required database config is present
     */
    public static boolean isDatabaseConfigValid() {
        String host = get("DB_HOST");
        String username = get("DB_USERNAME");
        String password = get("DB_PASSWORD");
        return host != null && !host.isEmpty() && 
               username != null && !username.isEmpty() &&
               password != null && !password.isEmpty();
    }
    
    /**
     * Get email configuration
     */
    public static String getEmailHost() {
        return get("EMAIL_HOST", "smtp.gmail.com");
    }
    
    public static int getEmailPort() {
        return getInt("EMAIL_PORT", 587);
    }
    
    public static String getEmailUsername() {
        return get("EMAIL_USERNAME", "");
    }
    
    public static String getEmailPassword() {
        return get("EMAIL_PASSWORD", "");
    }
    
    /**
     * Get EmailJS configuration
     */
    public static String getEmailJsServiceId() {
        return get("EMAILJS_SERVICE_ID", "");
    }
    
    public static String getEmailJsTemplateId() {
        return get("EMAILJS_TEMPLATE_ID", "");
    }
    
    public static String getEmailJsPublicKey() {
        return get("EMAILJS_PUBLIC_KEY", "");
    }
    
    /**
     * Get MailerSend configuration
     */
    public static String getMailerSendApiKey() {
        return get("MAILERSEND_API_KEY", "");
    }
    
    public static String getMailerSendFromEmail() {
        return get("MAILERSEND_FROM_EMAIL", "noreply@yourdomain.com");
    }
    
    public static String getMailerSendFromName() {
        return get("MAILERSEND_FROM_NAME", "Academic Analyzer");
    }
    
    /**
     * Get Result Portal URL
     */
    public static String getResultPortalUrl() {
        return get("RESULT_PORTAL_URL", "https://academicanalyzer-portal.azurewebsites.net");
    }
}