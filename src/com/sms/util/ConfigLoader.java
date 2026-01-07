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
            // Try to load from .env file first
            File envFile = new File(".env");
            if (envFile.exists()) {
                loadFromFile(envFile);
                System.out.println("Configuration loaded from .env file");
            } else {
                System.out.println("No .env file found, using environment variables");
            }
            
            // Override with system environment variables if they exist
            loadFromEnvironmentVariables();
            
            initialized = true;
            
        } catch (Exception e) {
            System.err.println("Error loading configuration: " + e.getMessage());
            e.printStackTrace();
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
        String host = get("DB_HOST", "localhost");
        int port = getInt("DB_PORT", 3306);
        String dbName = get("DB_NAME", "academic_analyzer");
        return String.format("jdbc:mysql://%s:%d/%s", host, port, dbName);
    }
    
    /**
     * Get database username
     * @return Database username
     */
    public static String getDatabaseUsername() {
        return get("DB_USERNAME", "root");
    }
    
    /**
     * Get database password
     * @return Database password
     */
    public static String getDatabasePassword() {
        return get("DB_PASSWORD", "");
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
}