package com.quantlabs.stockApp.alert.model;

public class ZScoreAlertConfig0 {
    private boolean enabled;
    private int monitorRange;
    private String previousTopSymbols;
    private boolean alarmOn;
    private boolean monteCarloEnabled;

    public ZScoreAlertConfig0() {
        this(false, 5);
    }

    public ZScoreAlertConfig0(boolean enabled, int monitorRange) {
        this.enabled = enabled;
        this.monitorRange = monitorRange;
        this.previousTopSymbols = "";
        this.monteCarloEnabled = false;
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
    
    public boolean isAlarmOn() {
		return alarmOn;
	}

	public void setAlarmOn(boolean alarmOn) {
		this.alarmOn = alarmOn;
	}

	@Override
    public String toString() {
        return "ZScoreAlertConfig{" +
                "enabled=" + enabled +
                ", monitorRange=" + monitorRange +
                ", previousTopSymbols='" + previousTopSymbols + '\'' +
                ", monteCarloEnabled=" + monteCarloEnabled +
                '}';
    }
}