package com.quantlabs.QuantTester.v4.alert;

import java.io.*;
import javax.swing.*;
import org.json.JSONObject;

public class ConfigManager {
    private JSONObject config;
    private JSONObject alertConfig; // New field for alert configurations

    public ConfigManager() {
        config = new JSONObject();
        alertConfig = new JSONObject();
    }

    public void loadConfigFile(JFrame parent, JLabel statusLabel) {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (FileReader reader = new FileReader(file)) {
                StringBuilder content = new StringBuilder();
                int c;
                while ((c = reader.read()) != -1) {
                    content.append((char) c);
                }
                config = new JSONObject(content.toString());
                statusLabel.setText("Config loaded: " + file.getName());
            } catch (Exception ex) {
                statusLabel.setText("Error loading config: " + ex.getMessage());
            }
        }
    }

    // New method to load alert configuration
    public void loadAlertConfig(String filePath, JLabel statusLabel) {
        try (FileReader reader = new FileReader(filePath)) {
            StringBuilder content = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                content.append((char) c);
            }
            alertConfig = new JSONObject(content.toString());
            statusLabel.setText("Alert config loaded: " + filePath);
        } catch (Exception ex) {
            statusLabel.setText("Error loading alert config: " + ex.getMessage());
            alertConfig = new JSONObject(); // Reset to empty on error
        }
    }

    // New method to save alert configuration
    public void saveAlertConfig(String filePath, JSONObject newConfig, JLabel statusLabel) {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(newConfig.toString(2)); // Pretty print with indent
            alertConfig = newConfig;
            statusLabel.setText("Alert config saved: " + filePath);
        } catch (Exception ex) {
            statusLabel.setText("Error saving alert config: " + ex.getMessage());
        }
    }

    public JSONObject getConfig() {
        return config;
    }

    public JSONObject getAlertConfig() {
        return alertConfig;
    }
}