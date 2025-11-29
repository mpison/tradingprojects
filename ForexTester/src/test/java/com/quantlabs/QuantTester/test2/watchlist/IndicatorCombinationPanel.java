package com.quantlabs.QuantTester.test2.watchlist;

import java.awt.BorderLayout;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.json.JSONArray;
import org.json.JSONObject;

//==================== IndicatorCombinationPanel ====================
public class IndicatorCombinationPanel extends JPanel {
    private DefaultListModel<IndicatorCombination> comboListModel;
    private JList<IndicatorCombination> comboList;
    private JTextField comboNameField;
    private JButton addButton, removeButton, editButton;
    private JButton saveButton, loadButton;
    
    private DefaultListModel<Indicator> indicatorListModel;
    private JList<Indicator> indicatorList;
    
    public IndicatorCombinationPanel() {
        setLayout(new BorderLayout());
        
        // Combination list
        comboListModel = new DefaultListModel<>();
        comboList = new JList<>(comboListModel);
        comboList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane comboScrollPane = new JScrollPane(comboList);
        comboScrollPane.setBorder(BorderFactory.createTitledBorder("Indicator Combinations"));
        
        // Combination controls
        JPanel comboControlPanel = new JPanel(new BorderLayout());
        comboNameField = new JTextField();
        
        JPanel comboButtonPanel = new JPanel();
        addButton = new JButton("Add");
        removeButton = new JButton("Remove");
        editButton = new JButton("Edit");
        
        comboButtonPanel.add(addButton);
        comboButtonPanel.add(removeButton);
        comboButtonPanel.add(editButton);
        
        comboControlPanel.add(new JLabel("Combination Name:"), BorderLayout.NORTH);
        comboControlPanel.add(comboNameField, BorderLayout.CENTER);
        comboControlPanel.add(comboButtonPanel, BorderLayout.SOUTH);
        
        // Indicators list
        indicatorListModel = new DefaultListModel<>();
        indicatorList = new JList<>(indicatorListModel);
        indicatorList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane indicatorScrollPane = new JScrollPane(indicatorList);
        indicatorScrollPane.setBorder(BorderFactory.createTitledBorder("Indicators in Combination"));
        
        // File operations
        JPanel filePanel = new JPanel();
        saveButton = new JButton("Save to File");
        loadButton = new JButton("Load from File");
        
        filePanel.add(saveButton);
        filePanel.add(loadButton);
        
        // Layout
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(comboScrollPane, BorderLayout.CENTER);
        leftPanel.add(comboControlPanel, BorderLayout.SOUTH);
        
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(indicatorScrollPane, BorderLayout.CENTER);
        rightPanel.add(filePanel, BorderLayout.SOUTH);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(300);
        
        add(splitPane, BorderLayout.CENTER);
        
        // Event listeners
        addButton.addActionListener(e -> addCombination());
        removeButton.addActionListener(e -> removeCombination());
        editButton.addActionListener(e -> editCombination());
        saveButton.addActionListener(e -> saveToFile());
        loadButton.addActionListener(e -> loadFromFile());
        comboList.addListSelectionListener(e -> loadSelectedCombination());
    }
    
    private void addCombination() {
        String name = comboNameField.getText().trim();
        if (!name.isEmpty()) {
            comboListModel.addElement(new IndicatorCombination(name, new ArrayList<>()));
            comboNameField.setText("");
        } else {
            JOptionPane.showMessageDialog(this, "Please enter a combination name", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void removeCombination() {
        int selectedIndex = comboList.getSelectedIndex();
        if (selectedIndex != -1) {
            comboListModel.remove(selectedIndex);
            indicatorListModel.clear();
        } else {
            JOptionPane.showMessageDialog(this, "Please select a combination to remove", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void editCombination() {
        int selectedIndex = comboList.getSelectedIndex();
        if (selectedIndex != -1) {
            IndicatorCombination combo = comboListModel.get(selectedIndex);
            JFrame parentFrame = (JFrame)SwingUtilities.getWindowAncestor(this);
            new IndicatorEditorDialog(parentFrame, combo).setVisible(true);
            comboListModel.set(selectedIndex, combo); // Refresh the list
            loadSelectedCombination(); // Refresh indicators display
        } else {
            JOptionPane.showMessageDialog(this, "Please select a combination to edit", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadSelectedCombination() {
        IndicatorCombination selected = comboList.getSelectedValue();
        indicatorListModel.clear();
        if (selected != null) {
            for (Indicator indicator : selected.getIndicators()) {
                indicatorListModel.addElement(indicator);
            }
        }
    }
    
    private void saveToFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (FileWriter writer = new FileWriter(fileChooser.getSelectedFile())) {
                JSONArray jsonArray = new JSONArray();
                for (int i = 0; i < comboListModel.size(); i++) {
                    IndicatorCombination combo = comboListModel.get(i);
                    JSONObject comboObj = new JSONObject();
                    comboObj.put("name", combo.getName());
                    
                    JSONArray indicatorsArray = new JSONArray();
                    for (Indicator indicator : combo.getIndicators()) {
                        JSONObject indicatorObj = new JSONObject();
                        indicatorObj.put("type", indicator.getType());
                        indicatorObj.put("timeframe", indicator.getTimeframe());
                        indicatorObj.put("shift", indicator.getShift());
                        
                        JSONObject paramsObj = new JSONObject();
                        for (Map.Entry<String, Object> entry : indicator.getParams().entrySet()) {
                            paramsObj.put(entry.getKey(), entry.getValue());
                        }
                        indicatorObj.put("params", paramsObj);
                        indicatorsArray.put(indicatorObj);
                    }
                    comboObj.put("indicators", indicatorsArray);
                    jsonArray.put(comboObj);
                }
                writer.write(jsonArray.toString(2));
                JOptionPane.showMessageDialog(this, "Combinations saved successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void loadFromFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String content = new String(java.nio.file.Files.readAllBytes(fileChooser.getSelectedFile().toPath()));
                JSONArray jsonArray = new JSONArray(content);
                comboListModel.clear();
                
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject comboObj = jsonArray.getJSONObject(i);
                    String name = comboObj.getString("name");
                    
                    List<Indicator> indicators = new ArrayList<>();
                    JSONArray indicatorsArray = comboObj.getJSONArray("indicators");
                    for (int j = 0; j < indicatorsArray.length(); j++) {
                        JSONObject indicatorObj = indicatorsArray.getJSONObject(j);
                        String indicatorName = indicatorObj.getString("name");
                        String timeframe = indicatorObj.getString("timeframe");
                        int shift = indicatorObj.getInt("shift");
                        
                        Map<String, Object> params = new HashMap<>();
                        if (indicatorObj.has("params")) {
                            JSONObject paramsObj = indicatorObj.getJSONObject("params");
                            Iterator<String> keys = paramsObj.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                Object value = paramsObj.get(key);
                                if (value instanceof Number) {
                                	// Convert to Long for volumeValue if needed
                                    if (key.equals("volumeValue") && indicatorName.equals("Specific Volume")) {
                                        value = ((Number) value).longValue();
                                    }
                                    // Other number conversions as needed
                                    else {
                                        value = ((Number) value).doubleValue();
                                    }
                                }
                                params.put(key, value);
                            }
                        }
                        
                        indicators.add(new Indicator(indicatorName, timeframe, shift, params));
                    }
                    
                    comboListModel.addElement(new IndicatorCombination(name, indicators));
                }
                
                JOptionPane.showMessageDialog(this, "Combinations loaded successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error loading file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    public void clear() {
        comboListModel.clear();
        indicatorListModel.clear();
    }
    
    public void addCombination(IndicatorCombination combination) {
        comboListModel.addElement(combination);
    }
    
    public List<IndicatorCombination> getCombinations() {
        List<IndicatorCombination> combinations = new ArrayList<>();
        for (int i = 0; i < comboListModel.size(); i++) {
            combinations.add(comboListModel.get(i));
        }
        return combinations;
    }
    
    public IndicatorCombination getCombinationByName(String name) {
        for (int i = 0; i < comboListModel.size(); i++) {
            IndicatorCombination combo = comboListModel.get(i);
            if (combo.getName().equals(name)) {
                return combo;
            }
        }
        return null;
    }
}