package com.quantlabs.QuantTester.test2;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TradingIndicatorAppGUI extends JFrame {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private DefaultListModel<IndicatorCombination> combinationsModel = new DefaultListModel<>();
    private JList<IndicatorCombination> combinationsList;
    private JButton addButton, editButton, removeButton, saveButton, loadButton;
    
    public TradingIndicatorAppGUI() {
        setTitle("Trading Indicator Combination Manager");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        initComponents();
        layoutComponents();
    }
    
    private void initComponents() {
        combinationsList = new JList<>(combinationsModel);
        combinationsList.setCellRenderer(new CombinationListRenderer());
        combinationsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        addButton = new JButton("Add New Combination");
        addButton.addActionListener(e -> addNewCombination());
        
        editButton = new JButton("Edit Selected");
        editButton.addActionListener(e -> editSelectedCombination());
        editButton.setEnabled(false);
        
        removeButton = new JButton("Remove Selected");
        removeButton.addActionListener(e -> removeSelectedCombination());
        removeButton.setEnabled(false);
        
        saveButton = new JButton("Save to File");
        saveButton.addActionListener(e -> saveToFile());
        
        loadButton = new JButton("Load from File");
        loadButton.addActionListener(e -> loadFromFile());
        
        combinationsList.addListSelectionListener(e -> {
            boolean hasSelection = !combinationsList.isSelectionEmpty();
            editButton.setEnabled(hasSelection);
            removeButton.setEnabled(hasSelection);
        });
    }
    
    private void layoutComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(combinationsList);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new GridLayout(1, 5, 10, 10));
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(loadButton);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(mainPanel);
    }
    
    private void addNewCombination() {
        CombinationEditorDialog editor = new CombinationEditorDialog(this, null);
        editor.setVisible(true);
        
        if (editor.isSaved()) {
            combinationsModel.addElement(editor.getCombination());
        }
    }
    
    private void editSelectedCombination() {
        IndicatorCombination selected = combinationsList.getSelectedValue();
        if (selected != null) {
            int selectedIndex = combinationsList.getSelectedIndex();
            
            CombinationEditorDialog editor = new CombinationEditorDialog(this, selected);
            editor.setVisible(true);
            
            if (editor.isSaved()) {
                combinationsModel.set(selectedIndex, editor.getCombination());
            }
        }
    }
    
    private void removeSelectedCombination() {
        int selectedIndex = combinationsList.getSelectedIndex();
        if (selectedIndex != -1) {
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to remove this combination?",
                "Confirm Removal",
                JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                combinationsModel.remove(selectedIndex);
            }
        }
    }
    
    private void saveToFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Combinations");
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON Files", "json"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".json")) {
                file = new File(file.getParentFile(), file.getName() + ".json");
            }
            
            try {
                List<IndicatorCombination> combinations = new ArrayList<>();
                for (int i = 0; i < combinationsModel.size(); i++) {
                    combinations.add(combinationsModel.get(i));
                }
                
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, combinations);
                JOptionPane.showMessageDialog(this, "Combinations saved successfully!");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void loadFromFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Load Combinations");
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON Files", "json"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            try {
                JavaType type = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, IndicatorCombination.class);
                
                List<IndicatorCombination> combinations = objectMapper.readValue(file, type);
                
                combinationsModel.clear();
                for (IndicatorCombination combo : combinations) {
                    combinationsModel.addElement(combo);
                }
                
                JOptionPane.showMessageDialog(this, "Combinations loaded successfully!");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error loading file: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TradingIndicatorAppGUI app = new TradingIndicatorAppGUI();
            app.setVisible(true);
        });
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IndicatorCombination {
        private String name;
        private List<IndicatorConfig> indicators;
        
        @JsonCreator
        public IndicatorCombination(
            @JsonProperty("name") String name,
            @JsonProperty("indicators") List<IndicatorConfig> indicators) {
            this.name = name;
            this.indicators = indicators != null ? indicators : new ArrayList<>();
        }
        
        public IndicatorCombination() {
            this.indicators = new ArrayList<>();
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public List<IndicatorConfig> getIndicators() {
            return indicators;
        }
        
        public void setIndicators(List<IndicatorConfig> indicators) {
            this.indicators = indicators;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IndicatorConfig {
        private String name;
        private String timeframe;
        private int shift;
        private Map<String, Object> params;
        
        public IndicatorConfig(String name, Map<String, Object> params, String timeframe, int shift) {
            this.name = name;
            this.params = params != null ? params : new HashMap<>();
            this.timeframe = timeframe;
            this.shift = shift;
        }
        
        
        @JsonCreator
        public IndicatorConfig(
            @JsonProperty("name") String name,
            @JsonProperty("timeframe") String timeframe,
            @JsonProperty("shift") int shift,
            @JsonProperty("params") Map<String, Object> params) {
            this.name = name;
            this.timeframe = timeframe;
            this.shift = shift;
            this.params = params != null ? params : new HashMap<>();
        }
        
        public IndicatorConfig() {
            this.params = new HashMap<>();
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getTimeframe() {
            return timeframe;
        }
        
        public void setTimeframe(String timeframe) {
            this.timeframe = timeframe;
        }
        
        public int getShift() {
            return shift;
        }
        
        public void setShift(int shift) {
            this.shift = shift;
        }
        
        public Map<String, Object> getParams() {
            return params;
        }
        
        public void setParams(Map<String, Object> params) {
            this.params = params;
        }
    }

    class CombinationEditorDialog extends JDialog {
        private IndicatorCombination combination;
        private boolean saved = false;
        
        private DefaultListModel<IndicatorConfig> indicatorsModel = new DefaultListModel<>();
        private JList<IndicatorConfig> indicatorsList;
        private JButton addButton, editButton, removeButton, saveButton, cancelButton;
        private JTextField nameField;
        
        public CombinationEditorDialog(JFrame parent, IndicatorCombination existingCombo) {
            super(parent, existingCombo == null ? "Add New Combination" : "Edit Combination", true);
            setSize(800, 600);
            setLocationRelativeTo(parent);
            
            this.combination = existingCombo != null ? 
                new IndicatorCombination(existingCombo.getName(), new ArrayList<>(existingCombo.getIndicators())) : 
                new IndicatorCombination();
            
            initComponents();
            layoutComponents();
            loadExistingIndicators();
        }
        
        private void initComponents() {
            nameField = new JTextField(25);
            if (combination.getName() != null) {
                nameField.setText(combination.getName());
            }
            
            indicatorsList = new JList<>(indicatorsModel);
            indicatorsList.setCellRenderer(new IndicatorListRenderer());
            indicatorsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            
            addButton = new JButton("Add Indicator");
            addButton.addActionListener(e -> addNewIndicator());
            
            editButton = new JButton("Edit Indicator");
            editButton.addActionListener(e -> editSelectedIndicator());
            editButton.setEnabled(false);
            
            removeButton = new JButton("Remove Indicator");
            removeButton.addActionListener(e -> removeSelectedIndicator());
            removeButton.setEnabled(false);
            
            saveButton = new JButton("Save");
            saveButton.addActionListener(e -> saveCombination());
            
            cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(e -> dispose());
            
            indicatorsList.addListSelectionListener(e -> {
                boolean hasSelection = !indicatorsList.isSelectionEmpty();
                editButton.setEnabled(hasSelection);
                removeButton.setEnabled(hasSelection);
            });
        }
        
        private void layoutComponents() {
            JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
            mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            namePanel.add(new JLabel("Combination Name:"));
            namePanel.add(nameField);
            
            JScrollPane scrollPane = new JScrollPane(indicatorsList);
            
            JPanel buttonPanel = new JPanel(new GridLayout(1, 5, 10, 10));
            buttonPanel.add(addButton);
            buttonPanel.add(editButton);
            buttonPanel.add(removeButton);
            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);
            
            mainPanel.add(namePanel, BorderLayout.NORTH);
            mainPanel.add(scrollPane, BorderLayout.CENTER);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);
            
            add(mainPanel);
        }
        
        private void loadExistingIndicators() {
            for (IndicatorConfig config : combination.getIndicators()) {
                indicatorsModel.addElement(config);
            }
        }
        
        private void addNewIndicator() {
            IndicatorConfig config = showIndicatorSelectionDialog(null);
            if (config != null) {
                indicatorsModel.addElement(config);
                combination.getIndicators().add(config);
            }
        }
        
        private void editSelectedIndicator() {
            IndicatorConfig selected = indicatorsList.getSelectedValue();
            if (selected != null) {
                int selectedIndex = indicatorsList.getSelectedIndex();
                
                IndicatorConfig edited = showIndicatorSelectionDialog(selected);
                if (edited != null) {
                    indicatorsModel.set(selectedIndex, edited);
                    combination.getIndicators().set(selectedIndex, edited);
                }
            }
        }
        
        private void removeSelectedIndicator() {
            int selectedIndex = indicatorsList.getSelectedIndex();
            if (selectedIndex != -1) {
                indicatorsModel.remove(selectedIndex);
                combination.getIndicators().remove(selectedIndex);
            }
        }
        
        private IndicatorConfig showIndicatorSelectionDialog(IndicatorConfig existing) {
            JDialog dialog = new JDialog(this, "Configure Indicator", true);
            dialog.setSize(550, 500);  // Increased size for better visibility
            dialog.setLocationRelativeTo(this);
            
            JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
            mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            
            // Main content panel with vertical layout
            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            
            // 1. Indicator Type Section - Made more prominent
            JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            typePanel.setBorder(BorderFactory.createTitledBorder("1. Select Indicator Type"));
            String[] indicatorTypes = {"RSI", "MACD", "Stochastic", "PSAR", "Moving Average", "Heikin-Ashi"};
            JComboBox<String> typeCombo = new JComboBox<>(indicatorTypes);
            typeCombo.setPreferredSize(new Dimension(200, 25));
            typePanel.add(typeCombo);
            contentPanel.add(typePanel);
            
            // 2. Timeframe and Shift Section
            JPanel metaPanel = new JPanel(new GridLayout(2, 2, 10, 10));
            metaPanel.setBorder(BorderFactory.createTitledBorder("2. Set Timeframe and Shift"));
            
            JPanel timeframePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            timeframePanel.add(new JLabel("Timeframe:"));
            JComboBox<String> timeframeCombo = new JComboBox<>(new String[]{"1m", "5m", "15m", "30m", "1h", "4h", "1d", "1w", "1M"});
            timeframePanel.add(timeframeCombo);
            metaPanel.add(timeframePanel);
            
            JPanel shiftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            shiftPanel.add(new JLabel("Shift (bars):"));
            JSpinner shiftSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
            shiftSpinner.setPreferredSize(new Dimension(60, 25));
            shiftPanel.add(shiftSpinner);
            metaPanel.add(shiftPanel);
            
            contentPanel.add(metaPanel);
            
            // 3. Parameters Section
            JPanel paramsPanel = new JPanel(new GridLayout(0, 2, 10, 10));
            paramsPanel.setBorder(BorderFactory.createTitledBorder("3. Configure Parameters"));
            contentPanel.add(paramsPanel);
            
            // Button panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton okButton = new JButton("OK");
            JButton cancelButton = new JButton("Cancel");
            buttonPanel.add(okButton);
            buttonPanel.add(cancelButton);
            
            // Set initial values if editing existing indicator
            if (existing != null) {
                typeCombo.setSelectedItem(existing.getName());
                if (existing.getTimeframe() != null) {
                    timeframeCombo.setSelectedItem(existing.getTimeframe());
                }
                shiftSpinner.setValue(existing.getShift());
            }
            
            // Update parameters panel when type changes
            typeCombo.addActionListener(e -> updateParamsPanel(paramsPanel, 
                (String) typeCombo.getSelectedItem(), existing));
            
            // Initial panel update
            updateParamsPanel(paramsPanel, (String) typeCombo.getSelectedItem(), existing);
            
            // Dialog result
            final IndicatorConfig[] result = new IndicatorConfig[1];
            
            okButton.addActionListener(e -> {
                String type = (String) typeCombo.getSelectedItem();
                String timeframe = (String) timeframeCombo.getSelectedItem();
                int shift = (Integer) shiftSpinner.getValue();
                Map<String, Object> params = new HashMap<>();
                
                
                switch (type) {
                    case "RSI":
                        params.put("timeFrame", Integer.parseInt(
                            ((JTextField) paramsPanel.getComponent(1)).getText()));
                        params.put("overbought", Double.parseDouble(
                            ((JTextField) paramsPanel.getComponent(3)).getText()));
                        params.put("oversold", Double.parseDouble(
                            ((JTextField) paramsPanel.getComponent(5)).getText()));
                        break;
                        
                    case "MACD":
                        params.put("shortTimeFrame", Integer.parseInt(
                            ((JTextField) paramsPanel.getComponent(1)).getText()));
                        params.put("longTimeFrame", Integer.parseInt(
                            ((JTextField) paramsPanel.getComponent(3)).getText()));
                        params.put("signalTimeFrame", Integer.parseInt(
                            ((JTextField) paramsPanel.getComponent(5)).getText()));
                        break;
                        
                    case "Stochastic":
                        params.put("kTimeFrame", Integer.parseInt(
                            ((JTextField) paramsPanel.getComponent(1)).getText()));
                        params.put("dTimeFrame", Integer.parseInt(
                            ((JTextField) paramsPanel.getComponent(3)).getText()));
                        params.put("overbought", Double.parseDouble(
                            ((JTextField) paramsPanel.getComponent(5)).getText()));
                        params.put("oversold", Double.parseDouble(
                            ((JTextField) paramsPanel.getComponent(7)).getText()));
                        break;
                        
                    case "PSAR":
                        params.put("accelerationFactor", Double.parseDouble(
                            ((JTextField) paramsPanel.getComponent(1)).getText()));
                        params.put("maxAcceleration", Double.parseDouble(
                            ((JTextField) paramsPanel.getComponent(3)).getText()));
                        break;
                        
                    case "Moving Average":
                        params.put("type", ((JComboBox<?>) paramsPanel.getComponent(1)).getSelectedItem());
                        params.put("timeFrame", Integer.parseInt(
                            ((JTextField) paramsPanel.getComponent(3)).getText()));
                        break;
                        
                    case "Heikin-Ashi":
                        break;
                }
                
                result[0] = new IndicatorConfig(type, params, timeframe, shift);
                dialog.dispose();
            });
            
            cancelButton.addActionListener(e -> {
                result[0] = null;
                dialog.dispose();
            });
            
            mainPanel.add(contentPanel, BorderLayout.CENTER);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);
            dialog.add(mainPanel);
            dialog.setVisible(true);
            
            return result[0];
        }
        
        private void updateParamsPanel(JPanel paramsPanel, String indicatorType, IndicatorConfig existing) {
            paramsPanel.removeAll();
            
            Map<String, Object> existingParams = existing != null ? existing.getParams() : null;
            
            switch (indicatorType) {
                case "RSI":
                    paramsPanel.add(new JLabel("Time Frame:"));
                    JTextField rsiTimeFrame = new JTextField(
                        existingParams != null ? existingParams.get("timeFrame").toString() : "14");
                    paramsPanel.add(rsiTimeFrame);
                    
                    paramsPanel.add(new JLabel("Overbought Threshold:"));
                    JTextField rsiOverbought = new JTextField(
                        existingParams != null ? existingParams.get("overbought").toString() : "70");
                    paramsPanel.add(rsiOverbought);
                    
                    paramsPanel.add(new JLabel("Oversold Threshold:"));
                    JTextField rsiOversold = new JTextField(
                        existingParams != null ? existingParams.get("oversold").toString() : "30");
                    paramsPanel.add(rsiOversold);
                    break;
                    
                case "MACD":
                    paramsPanel.add(new JLabel("Short Time Frame:"));
                    JTextField macdShort = new JTextField(
                        existingParams != null ? existingParams.get("shortTimeFrame").toString() : "12");
                    paramsPanel.add(macdShort);
                    
                    paramsPanel.add(new JLabel("Long Time Frame:"));
                    JTextField macdLong = new JTextField(
                        existingParams != null ? existingParams.get("longTimeFrame").toString() : "26");
                    paramsPanel.add(macdLong);
                    
                    paramsPanel.add(new JLabel("Signal Time Frame:"));
                    JTextField macdSignal = new JTextField(
                        existingParams != null ? existingParams.get("signalTimeFrame").toString() : "9");
                    paramsPanel.add(macdSignal);
                    break;
                    
                case "Stochastic":
                    paramsPanel.add(new JLabel("%K Time Frame:"));
                    JTextField stochK = new JTextField(
                        existingParams != null ? existingParams.get("kTimeFrame").toString() : "14");
                    paramsPanel.add(stochK);
                    
                    paramsPanel.add(new JLabel("%D Time Frame:"));
                    JTextField stochD = new JTextField(
                        existingParams != null ? existingParams.get("dTimeFrame").toString() : "3");
                    paramsPanel.add(stochD);
                    
                    paramsPanel.add(new JLabel("Overbought Threshold:"));
                    JTextField stochOverbought = new JTextField(
                        existingParams != null ? existingParams.get("overbought").toString() : "80");
                    paramsPanel.add(stochOverbought);
                    
                    paramsPanel.add(new JLabel("Oversold Threshold:"));
                    JTextField stochOversold = new JTextField(
                        existingParams != null ? existingParams.get("oversold").toString() : "20");
                    paramsPanel.add(stochOversold);
                    break;
                    
                case "PSAR":
                    paramsPanel.add(new JLabel("Acceleration Factor:"));
                    JTextField psarAf = new JTextField(
                        existingParams != null ? existingParams.get("accelerationFactor").toString() : "0.02");
                    paramsPanel.add(psarAf);
                    
                    paramsPanel.add(new JLabel("Max Acceleration:"));
                    JTextField psarMaxAf = new JTextField(
                        existingParams != null ? existingParams.get("maxAcceleration").toString() : "0.2");
                    paramsPanel.add(psarMaxAf);
                    break;
                    
                case "Moving Average":
                    paramsPanel.add(new JLabel("MA Type:"));
                    String[] maTypes = {"SMA", "EMA", "WMA"};
                    JComboBox<String> maTypeCombo = new JComboBox<>(maTypes);
                    if (existingParams != null) {
                        maTypeCombo.setSelectedItem(existingParams.get("type"));
                    }
                    paramsPanel.add(maTypeCombo);
                    
                    paramsPanel.add(new JLabel("Time Frame:"));
                    JTextField maTimeFrame = new JTextField(
                        existingParams != null ? existingParams.get("timeFrame").toString() : "20");
                    paramsPanel.add(maTimeFrame);
                    break;
                    
                case "Heikin-Ashi":
                    paramsPanel.add(new JLabel("No parameters needed"));
                    break;
            }
            
            paramsPanel.revalidate();
            paramsPanel.repaint();
        }
        
        private void saveCombination() {
            combination.setName(nameField.getText().trim());
            saved = true;
            dispose();
        }
        
        public IndicatorCombination getCombination() {
            return combination;
        }
        
        public boolean isSaved() {
            return saved;
        }
    }
    
    class CombinationListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, 
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof IndicatorCombination) {
                IndicatorCombination combo = (IndicatorCombination) value;
                StringBuilder sb = new StringBuilder();
                sb.append("<html><b>");
                
                if (combo.getName() != null && !combo.getName().isEmpty()) {
                    sb.append(combo.getName());
                } else {
                    sb.append("Combination ").append(index + 1);
                }
                
                sb.append("</b> (");
                
                for (int i = 0; i < combo.getIndicators().size(); i++) {
                    if (i > 0) sb.append(", ");
                    IndicatorConfig config = combo.getIndicators().get(i);
                    sb.append(config.getName());
                    if (config.getTimeframe() != null) {
                        sb.append("[").append(config.getTimeframe()).append("]");
                    }
                    if (config.getShift() > 0) {
                        sb.append("←").append(config.getShift());
                    }
                }
                
                sb.append(")");
                setText(sb.toString());
            }
            
            return this;
        }
    }
    
    class IndicatorListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, 
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof IndicatorConfig) {
                IndicatorConfig config = (IndicatorConfig) value;
                StringBuilder sb = new StringBuilder();
                sb.append("<html><b>").append(config.getName()).append("</b>");
                
                if (config.getTimeframe() != null) {
                    sb.append(" [").append(config.getTimeframe()).append("]");
                }
                
                if (config.getShift() > 0) {
                    sb.append(" ←").append(config.getShift());
                }
                
                if (!config.getParams().isEmpty()) {
                    sb.append(" (");
                    boolean first = true;
                    for (Map.Entry<String, Object> entry : config.getParams().entrySet()) {
                        if (!first) sb.append(", ");
                        sb.append(entry.getKey()).append(": ").append(entry.getValue());
                        first = false;
                    }
                    sb.append(")");
                }
                
                setText(sb.toString());
            }
            
            return this;
        }
    }
}