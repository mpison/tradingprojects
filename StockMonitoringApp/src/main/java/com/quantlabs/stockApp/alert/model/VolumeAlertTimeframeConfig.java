package com.quantlabs.stockApp.alert.model;

import java.util.Objects;

public class VolumeAlertTimeframeConfig {
    private boolean enabled;
    private double percentage;
    
    // Validation constants
    public static final double MIN_PERCENTAGE = 0.0;
    public static final double MAX_PERCENTAGE = 1000.0;
    public static final double DEFAULT_PERCENTAGE = 100.0;
    
    public VolumeAlertTimeframeConfig() {
        this(false, DEFAULT_PERCENTAGE);
    }
    
    public VolumeAlertTimeframeConfig(boolean enabled, double percentage) {
        this.enabled = enabled;
        setPercentage(percentage); // Use setter for validation
    }
    
    // Getters and setters
    public boolean isEnabled() { 
        return enabled; 
    }
    
    public void setEnabled(boolean enabled) { 
        this.enabled = enabled; 
    }
    
    public double getPercentage() { 
        return percentage; 
    }
    
    public void setPercentage(double percentage) {
        if (percentage < MIN_PERCENTAGE || percentage > MAX_PERCENTAGE) {
            throw new IllegalArgumentException(
                String.format("Percentage must be between %.1f and %.1f, got: %.1f", 
                    MIN_PERCENTAGE, MAX_PERCENTAGE, percentage));
        }
        this.percentage = percentage;
    }
    
    // ADD THIS COPY METHOD
    public VolumeAlertTimeframeConfig copy() {
        return new VolumeAlertTimeframeConfig(this.enabled, this.percentage);
    }
    
    // Validation methods
    public boolean isValid() {
        return percentage >= MIN_PERCENTAGE && percentage <= MAX_PERCENTAGE;
    }
    
    // ... rest of your existing methods
}