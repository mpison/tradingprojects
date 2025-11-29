package com.quantlabs.QuantTester.test2.watchlist;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

public class WatchlistMapperPanel extends JPanel {
    private DefaultListModel<WatchlistMapping> mappingModel;
    private JList<WatchlistMapping> mappingList;
    private JComboBox<Watchlist> watchlistCombo;
    private JComboBox<IndicatorCombination> comboCombo;
    private JTextField mappingNameField;
    private JButton addButton, removeButton, refreshButton;
    private WatchlistManagerPanel watchlistPanel;
    private IndicatorCombinationPanel combinationPanel;
    
    public WatchlistMapperPanel(WatchlistManagerPanel watchlistPanel, IndicatorCombinationPanel combinationPanel) {
        this.watchlistPanel = watchlistPanel;
        this.combinationPanel = combinationPanel;
        
        setLayout(new BorderLayout());
        mappingModel = new DefaultListModel<>();
        mappingList = new JList<>(mappingModel);
        mappingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Combo boxes and name field
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        watchlistCombo = new JComboBox<>();
        comboCombo = new JComboBox<>();
        mappingNameField = new JTextField();
        
        inputPanel.add(new JLabel("Mapping Name:"));
        inputPanel.add(mappingNameField);
        inputPanel.add(new JLabel("Watchlist:"));
        inputPanel.add(watchlistCombo);
        inputPanel.add(new JLabel("Indicator Combination:"));
        inputPanel.add(comboCombo);
        
        // Buttons
        JPanel buttonPanel = new JPanel();
        addButton = new JButton("Add Mapping");
        removeButton = new JButton("Remove Mapping");
        refreshButton = new JButton("Refresh Lists");
        
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(refreshButton);
        
        add(new JScrollPane(mappingList), BorderLayout.CENTER);
        add(inputPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.SOUTH);
        
        refreshComboboxes();
        
        // Event listeners
        addButton.addActionListener(e -> addMapping());
        removeButton.addActionListener(e -> removeMapping());
        refreshButton.addActionListener(e -> refreshComboboxes());
    }
    
    private void refreshComboboxes() {
        watchlistCombo.removeAllItems();
        comboCombo.removeAllItems();
        
        for (Watchlist watchlist : watchlistPanel.getWatchlists()) {
            watchlistCombo.addItem(watchlist);
        }
        
        for (IndicatorCombination combo : combinationPanel.getCombinations()) {
            comboCombo.addItem(combo);
        }
    }
    
    private void addMapping() {
        String mappingName = mappingNameField.getText().trim();
        Watchlist watchlist = (Watchlist) watchlistCombo.getSelectedItem();
        IndicatorCombination combination = (IndicatorCombination) comboCombo.getSelectedItem();
        
        if (mappingName.isEmpty()) {
            showError("Please enter a mapping name");
            return;
        }
        
        if (watchlist == null || combination == null) {
            showError("Please select both a watchlist and a combination");
            return;
        }
        
        // Check for duplicate mapping names
        if (hasMappingWithName(mappingName)) {
            showError("A mapping with this name already exists");
            return;
        }
        
        // NEW VALIDATION: Check if this watchlist-combination pair already exists
        if (hasExistingMapping(watchlist, combination)) {
            showError("This watchlist is already mapped to this combination");
            return;
        }
        
        mappingModel.addElement(new WatchlistMapping(mappingName, watchlist, combination));
        mappingNameField.setText("");
    }
    
    /**
     * Checks if a mapping with this name already exists (case insensitive)
     * @param name The mapping name to check
     * @return true if a mapping with this name exists, false otherwise
     */
    private boolean hasMappingWithName(String name) {
        for (int i = 0; i < mappingModel.size(); i++) {
            if (mappingModel.get(i).getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if this watchlist-combination pair already exists
     * (regardless of mapping name)
     * @param watchlist The watchlist to check
     * @param combination The indicator combination to check
     * @return true if this pair already exists in mappings, false otherwise
     */
    private boolean hasExistingMapping(Watchlist watchlist, IndicatorCombination combination) {
        for (int i = 0; i < mappingModel.size(); i++) {
            WatchlistMapping mapping = mappingModel.get(i);
            if (mapping.getWatchlist().equals(watchlist) && 
                mapping.getCombination().equals(combination)) {
                return true;
            }
        }
        return false;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private void removeMapping() {
        int selectedIndex = mappingList.getSelectedIndex();
        if (selectedIndex != -1) {
            mappingModel.remove(selectedIndex);
        } else {
            JOptionPane.showMessageDialog(this, 
                "Please select a mapping to remove", 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void clear() {
        mappingModel.clear();
    }
    
    public void addMapping(WatchlistMapping mapping) {
        mappingModel.addElement(mapping);
    }
    
    public List<WatchlistMapping> getMappings() {
        List<WatchlistMapping> mappings = new ArrayList<>();
        for (int i = 0; i < mappingModel.size(); i++) {
            mappings.add(mappingModel.get(i));
        }
        return mappings;
    }
}

