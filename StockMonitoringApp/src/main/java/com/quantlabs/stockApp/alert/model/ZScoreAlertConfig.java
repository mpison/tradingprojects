package com.quantlabs.stockApp.alert.model;

public class ZScoreAlertConfig {
    private boolean enabled;
    private int monitorRange;
    private String previousTopSymbols;
    private boolean alarmOn;
    private boolean monteCarloEnabled;
    private String strategy; // New field for strategy association
    private String zScoreColumn; // New field for Z-Score column name
    private String configId; // New field for unique configuration ID

    public ZScoreAlertConfig() {
        this(false, 5);
    }

    public ZScoreAlertConfig(boolean enabled, int monitorRange) {
        this.enabled = enabled;
        this.monitorRange = monitorRange;
        this.previousTopSymbols = "";
        this.monteCarloEnabled = false;
        this.alarmOn = false;
        this.strategy = ""; // Empty string means no strategy filter
        this.zScoreColumn = "";
        this.configId = generateConfigId();
    }

    public ZScoreAlertConfig(boolean enabled, int monitorRange, String zScoreColumn, String strategy) {
        this.enabled = enabled;
        this.monitorRange = monitorRange;
        this.zScoreColumn = zScoreColumn;
        this.strategy = strategy != null ? strategy : "";
        this.previousTopSymbols = "";
        this.monteCarloEnabled = false;
        this.alarmOn = false;
        this.configId = generateConfigId();
    }

    // Generate unique configuration ID
    private String generateConfigId() {
        return "config_" + System.currentTimeMillis() + "_" + Math.abs(this.hashCode());
    }

    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getMonitorRange() { return monitorRange; }
    public void setMonitorRange(int monitorRange) { this.monitorRange = monitorRange; }

    public String getPreviousTopSymbols() { return previousTopSymbols; }
    public void setPreviousTopSymbols(String previousTopSymbols) { this.previousTopSymbols = previousTopSymbols; }

    public boolean isMonteCarloEnabled() { return monteCarloEnabled; }
    public void setMonteCarloEnabled(boolean monteCarloEnabled) { this.monteCarloEnabled = monteCarloEnabled; }
    
    public boolean isAlarmOn() { return alarmOn; }
    public void setAlarmOn(boolean alarmOn) { this.alarmOn = alarmOn; }

    // New getters and setters for additional fields
    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy != null ? strategy : ""; }

    public String getZScoreColumn() { return zScoreColumn; }
    public void setZScoreColumn(String zScoreColumn) { this.zScoreColumn = zScoreColumn != null ? zScoreColumn : ""; }

    public String getConfigId() { return configId; }
    public void setConfigId(String configId) { this.configId = configId != null ? configId : generateConfigId(); }

    // Helper methods
    public boolean hasStrategy() {
        return strategy != null && !strategy.trim().isEmpty();
    }

    public boolean hasZScoreColumn() {
        return zScoreColumn != null && !zScoreColumn.trim().isEmpty();
    }

    public String getUniqueKey() {
        if (hasStrategy()) {
            return zScoreColumn + "|" + strategy;
        } else {
            return zScoreColumn;
        }
    }

    public boolean isSameConfiguration(ZScoreAlertConfig other) {
        if (other == null) return false;
        return this.zScoreColumn.equals(other.zScoreColumn) && 
               this.strategy.equals(other.strategy);
    }

    // Create a copy of this configuration
    public ZScoreAlertConfig copy() {
        ZScoreAlertConfig copy = new ZScoreAlertConfig();
        copy.enabled = this.enabled;
        copy.monitorRange = this.monitorRange;
        copy.previousTopSymbols = this.previousTopSymbols;
        copy.alarmOn = this.alarmOn;
        copy.monteCarloEnabled = this.monteCarloEnabled;
        copy.strategy = this.strategy;
        copy.zScoreColumn = this.zScoreColumn;
        copy.configId = generateConfigId(); // New ID for the copy
        return copy;
    }

    // Update from another configuration (useful for editing)
    public void updateFrom(ZScoreAlertConfig other) {
        if (other == null) return;
        
        this.enabled = other.enabled;
        this.monitorRange = other.monitorRange;
        this.previousTopSymbols = other.previousTopSymbols;
        this.alarmOn = other.alarmOn;
        this.monteCarloEnabled = other.monteCarloEnabled;
        this.strategy = other.strategy;
        this.zScoreColumn = other.zScoreColumn;
        // Note: configId is not updated to maintain identity
    }

    // Validation methods
    public boolean isValid() {
        return hasZScoreColumn() && monitorRange > 0;
    }

    public String getValidationErrors() {
        if (!hasZScoreColumn()) {
            return "Z-Score column is required";
        }
        if (monitorRange <= 0) {
            return "Monitor range must be greater than 0";
        }
        return null; // No errors
    }

    @Override
    public String toString() {
        return "ZScoreAlertConfig{" +
                "enabled=" + enabled +
                ", monitorRange=" + monitorRange +
                ", zScoreColumn='" + zScoreColumn + '\'' +
                ", strategy='" + strategy + '\'' +
                ", monteCarloEnabled=" + monteCarloEnabled +
                ", alarmOn=" + alarmOn +
                ", configId='" + configId + '\'' +
                ", previousTopSymbols='" + (previousTopSymbols != null ? 
                    previousTopSymbols.substring(0, Math.min(20, previousTopSymbols.length())) + "..." : "null") + '\'' +
                '}';
    }

    // Detailed string representation for debugging
    public String toDetailedString() {
        return String.format(
            "ZScoreAlertConfig [%s]\n" +
            "  Enabled: %s\n" +
            "  Z-Score Column: %s\n" +
            "  Strategy: %s\n" +
            "  Monitor Range: %d\n" +
            "  Monte Carlo: %s\n" +
            "  Alarm: %s\n" +
            "  Previous Symbols: %s",
            configId,
            enabled ? "YES" : "NO",
            zScoreColumn,
            hasStrategy() ? strategy : "None",
            monitorRange,
            monteCarloEnabled ? "ENABLED" : "DISABLED",
            alarmOn ? "ON" : "OFF",
            previousTopSymbols != null && previousTopSymbols.length() > 50 ? 
                previousTopSymbols.substring(0, 50) + "..." : previousTopSymbols
        );
    }

    // JSON serialization helper
    public String toJsonString() {
        return String.format(
            "{\"enabled\":%s,\"monitorRange\":%d,\"zScoreColumn\":\"%s\",\"strategy\":\"%s\"," +
            "\"monteCarloEnabled\":%s,\"alarmOn\":%s,\"configId\":\"%s\"}",
            enabled,
            monitorRange,
            escapeJson(zScoreColumn),
            escapeJson(strategy),
            monteCarloEnabled,
            alarmOn,
            escapeJson(configId)
        );
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ZScoreAlertConfig that = (ZScoreAlertConfig) o;
        
        if (enabled != that.enabled) return false;
        if (monitorRange != that.monitorRange) return false;
        if (alarmOn != that.alarmOn) return false;
        if (monteCarloEnabled != that.monteCarloEnabled) return false;
        if (!zScoreColumn.equals(that.zScoreColumn)) return false;
        return strategy.equals(that.strategy);
    }

    @Override
    public int hashCode() {
        int result = (enabled ? 1 : 0);
        result = 31 * result + monitorRange;
        result = 31 * result + (alarmOn ? 1 : 0);
        result = 31 * result + (monteCarloEnabled ? 1 : 0);
        result = 31 * result + zScoreColumn.hashCode();
        result = 31 * result + strategy.hashCode();
        return result;
    }
}