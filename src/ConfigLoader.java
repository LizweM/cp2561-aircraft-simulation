/*
 * Copyright (C) 2025 Shivaji Patil, College of the North Atlantic
 * All rights reserved.
 *
 * Aircraft Simulation Project
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Configuration loader that reads parameters from a properties file.
 */
public class ConfigLoader {
    private Properties properties;

    public ConfigLoader(String configFile) {
        properties = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            properties.load(fis);
            System.out.println("Loaded configuration from " + configFile);
        } catch (IOException e) {
            System.err.println("Error loading configuration: " + e.getMessage());
            System.out.println("Using default values");
        }
    }

    public double getDouble(String key, double defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                System.err.println("Invalid number in config for " + key + ": " + value);
            }
        }
        return defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.err.println("Invalid integer in config for " + key + ": " + value);
            }
        }
        return defaultValue;
    }

    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }
}
