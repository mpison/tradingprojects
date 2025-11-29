package com.quantlabs.stockApp.indicator.management;

import javax.swing.*;

import com.quantlabs.stockApp.model.WatchlistData;

import java.awt.*;
import java.awt.event.*;
import java.io.Serializable;
import java.util.*;
import java.util.List;

public class WatchlistManagerDialog extends JDialog {
    private Map<String, WatchlistData> watchlists;
    private boolean saved = false;
    
    private DefaultListModel<String> watchlistModel;
    private JList<String> watchlistList;
    private JTextField watchlistNameField;
    private JTextField symbolField;
    private DefaultListModel<String> symbolModel;
    private JList<String> symbolList;
    private JComboBox<String> primarySymbolCombo;
    
    // Constructor for new format (WatchlistData)
    public WatchlistManagerDialog(JFrame parent, Map<String, WatchlistData> existingWatchlists) {
        super(parent, "Manage Watchlists", true);
        this.watchlists = new HashMap<>(existingWatchlists);
        initializeUI();
        populateWatchlists();
        pack();
        setLocationRelativeTo(parent);
        setSize(600, 500);
    }
    
    
    
    private Map<String, WatchlistData> convertOldFormatToNew(Map<String, Set<String>> oldWatchlists) {
        Map<String, WatchlistData> newWatchlists = new HashMap<>();
        if (oldWatchlists != null) {
            for (Map.Entry<String, Set<String>> entry : oldWatchlists.entrySet()) {
                newWatchlists.put(entry.getKey(), new WatchlistData(entry.getValue(), ""));
            }
        }
        return newWatchlists;
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Watchlist list on left
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(200, 0));
        
        watchlistModel = new DefaultListModel<>();
        watchlistList = new JList<>(watchlistModel);
        watchlistList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane watchlistScroll = new JScrollPane(watchlistList);
        watchlistScroll.setBorder(BorderFactory.createTitledBorder("Watchlists"));
        
        // Watchlist controls
        JPanel watchlistControlPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        JButton addWatchlistButton = new JButton("Add");
        JButton removeWatchlistButton = new JButton("Remove");
        
        addWatchlistButton.addActionListener(e -> addWatchlist());
        removeWatchlistButton.addActionListener(e -> removeWatchlist());
        
        watchlistControlPanel.add(addWatchlistButton);
        watchlistControlPanel.add(removeWatchlistButton);
        
        leftPanel.add(watchlistScroll, BorderLayout.CENTER);
        leftPanel.add(watchlistControlPanel, BorderLayout.SOUTH);
        
        // Symbols panel on right
        JPanel rightPanel = new JPanel(new BorderLayout());
        
        // Watchlist name panel
        JPanel namePanel = new JPanel(new BorderLayout(5, 5));
        namePanel.add(new JLabel("Watchlist Name:"), BorderLayout.WEST);
        watchlistNameField = new JTextField();
        namePanel.add(watchlistNameField, BorderLayout.CENTER);
        
        // Primary Symbol Panel
        JPanel primarySymbolPanel = new JPanel(new BorderLayout(5, 5));
        primarySymbolPanel.add(new JLabel("Primary Symbol:"), BorderLayout.WEST);
        primarySymbolCombo = new JComboBox<>();
        primarySymbolCombo.setToolTipText("Select the primary symbol for this watchlist");
        primarySymbolPanel.add(primarySymbolCombo, BorderLayout.CENTER);
        
        // Symbols list
        JPanel symbolsPanel = new JPanel(new BorderLayout());
        symbolModel = new DefaultListModel<>();
        symbolList = new JList<>(symbolModel);
        JScrollPane symbolScroll = new JScrollPane(symbolList);
        symbolScroll.setBorder(BorderFactory.createTitledBorder("Symbols"));
        
        // Symbol controls
        JPanel symbolControlPanel = new JPanel(new GridLayout(1, 3, 5, 5));
        JPanel symbolInputPanel = new JPanel(new BorderLayout(5, 5));
        symbolInputPanel.add(new JLabel("Add Symbol:"), BorderLayout.WEST);
        symbolField = new JTextField();
        symbolInputPanel.add(symbolField, BorderLayout.CENTER);
        
        JButton addSymbolButton = new JButton("Add");
        JButton removeSymbolButton = new JButton("Remove");
        JButton clearSymbolsButton = new JButton("Clear");
        
        addSymbolButton.addActionListener(e -> addSymbol());
        removeSymbolButton.addActionListener(e -> removeSymbol());
        clearSymbolsButton.addActionListener(e -> clearSymbols());
        
        symbolControlPanel.add(addSymbolButton);
        symbolControlPanel.add(removeSymbolButton);
        symbolControlPanel.add(clearSymbolsButton);
        
        symbolsPanel.add(symbolScroll, BorderLayout.CENTER);
        
        JPanel symbolBottomPanel = new JPanel(new BorderLayout());
        symbolBottomPanel.add(symbolInputPanel, BorderLayout.CENTER);
        symbolBottomPanel.add(symbolControlPanel, BorderLayout.SOUTH);
        
        symbolsPanel.add(symbolBottomPanel, BorderLayout.SOUTH);
        
        // Combine right panel components
        JPanel topRightPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        topRightPanel.add(namePanel);
        topRightPanel.add(primarySymbolPanel);
        
        rightPanel.add(topRightPanel, BorderLayout.NORTH);
        rightPanel.add(symbolsPanel, BorderLayout.CENTER);
        
        // Add sample symbols button
        JButton addSampleButton = new JButton("Add Sample Symbols");
        addSampleButton.addActionListener(e -> addSampleSymbols());
        rightPanel.add(addSampleButton, BorderLayout.SOUTH);
        
        // Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(200);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> saveWatchlists());
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Selection listener
        watchlistList.addListSelectionListener(e -> updateSymbolsList());
        
        // Enter key handlers
        watchlistNameField.addActionListener(e -> updateWatchlistName());
        symbolField.addActionListener(e -> addSymbol());
    }
    
    private void populateWatchlists() {
        watchlistModel.clear();
        for (String watchlistName : watchlists.keySet()) {
            watchlistModel.addElement(watchlistName);
        }
        if (!watchlistModel.isEmpty()) {
            watchlistList.setSelectedIndex(0);
        }
    }
    
    private void updateSymbolsList() {
        String selected = watchlistList.getSelectedValue();
        if (selected != null) {
            watchlistNameField.setText(selected);
            symbolModel.clear();
            
            WatchlistData watchlistData = watchlists.get(selected);
            if (watchlistData != null) {
                for (String symbol : watchlistData.getSymbols()) {
                    symbolModel.addElement(symbol);
                }
            }
            
            // Update primary symbol combo
            updatePrimarySymbolCombo(selected);
        }
    }
    
    private void updatePrimarySymbolCombo(String watchlistName) {
        primarySymbolCombo.removeAllItems();
        primarySymbolCombo.addItem(""); // Empty option for no primary symbol
        
        WatchlistData watchlistData = watchlists.get(watchlistName);
        if (watchlistData != null) {
            for (String symbol : watchlistData.getSymbols()) {
                primarySymbolCombo.addItem(symbol);
            }
            
            // Set current primary symbol
            String currentPrimary = watchlistData.getPrimarySymbol();
            primarySymbolCombo.setSelectedItem(currentPrimary != null ? currentPrimary : "");
        }
    }
    
    private void addWatchlist() {
        String name = JOptionPane.showInputDialog(this, "Enter watchlist name:");
        if (name != null && !name.trim().isEmpty()) {
            if (!watchlists.containsKey(name)) {
                watchlists.put(name, new WatchlistData());
                watchlistModel.addElement(name);
                watchlistList.setSelectedValue(name, true);
            } else {
                JOptionPane.showMessageDialog(this, "Watchlist name already exists!");
            }
        }
    }
    
    private void removeWatchlist() {
        String selected = watchlistList.getSelectedValue();
        if (selected != null) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete watchlist '" + selected + "'?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
                
            if (confirm == JOptionPane.YES_OPTION) {
                watchlists.remove(selected);
                watchlistModel.removeElement(selected);
                if (!watchlistModel.isEmpty()) {
                    watchlistList.setSelectedIndex(0);
                } else {
                    watchlistNameField.setText("");
                    symbolModel.clear();
                    primarySymbolCombo.removeAllItems();
                }
            }
        }
    }
    
    private void updateWatchlistName() {
        String selected = watchlistList.getSelectedValue();
        String newName = watchlistNameField.getText().trim();
        
        if (selected != null && !newName.isEmpty() && !newName.equals(selected)) {
            if (!watchlists.containsKey(newName)) {
                WatchlistData watchlistData = watchlists.remove(selected);
                watchlists.put(newName, watchlistData);
                
                int index = watchlistModel.indexOf(selected);
                watchlistModel.set(index, newName);
                watchlistList.setSelectedValue(newName, true);
            } else {
                JOptionPane.showMessageDialog(this, "Watchlist name already exists!");
            }
        }
    }
    
    private void addSymbol() {
        String selectedWatchlist = watchlistList.getSelectedValue();
        String symbol = symbolField.getText().trim().toUpperCase();
        
        if (selectedWatchlist == null) {
            JOptionPane.showMessageDialog(this, "Please select a watchlist first!");
            return;
        }
        
        if (!symbol.isEmpty()) {
            WatchlistData watchlistData = watchlists.get(selectedWatchlist);
            if (watchlistData.addSymbol(symbol)) {
                symbolModel.addElement(symbol);
                symbolField.setText("");
                
                // Update primary symbol combo
                primarySymbolCombo.addItem(symbol);
            } else {
                JOptionPane.showMessageDialog(this, "Symbol already exists in watchlist!");
            }
        }
    }
    
    private void removeSymbol() {
        String selectedWatchlist = watchlistList.getSelectedValue();
        String selectedSymbol = symbolList.getSelectedValue();
        
        if (selectedWatchlist != null && selectedSymbol != null) {
            WatchlistData watchlistData = watchlists.get(selectedWatchlist);
            watchlistData.removeSymbol(selectedSymbol);
            symbolModel.removeElement(selectedSymbol);
            
            // Update primary symbol combo
            updatePrimarySymbolCombo(selectedWatchlist);
        }
    }
    
    private void clearSymbols() {
        String selectedWatchlist = watchlistList.getSelectedValue();
        if (selectedWatchlist != null) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to clear all symbols from '" + selectedWatchlist + "'?",
                "Confirm Clear", JOptionPane.YES_NO_OPTION);
                
            if (confirm == JOptionPane.YES_OPTION) {
                WatchlistData watchlistData = watchlists.get(selectedWatchlist);
                watchlistData.clearSymbols();
                symbolModel.clear();
                updatePrimarySymbolCombo(selectedWatchlist);
            }
        }
    }
    
    private void addSampleSymbols() {
        String selectedWatchlist = watchlistList.getSelectedValue();
        if (selectedWatchlist == null) {
            JOptionPane.showMessageDialog(this, "Please select a watchlist first!");
            return;
        }
        
        String[] sampleSymbols = {"AAPL", "GOOGL", "MSFT", "AMZN", "META", "NVDA", "TSLA", "JPM", "JNJ", "V"};
        WatchlistData watchlistData = watchlists.get(selectedWatchlist);
        
        int added = 0;
        for (String symbol : sampleSymbols) {
            if (watchlistData.addSymbol(symbol)) {
                symbolModel.addElement(symbol);
                added++;
            }
        }
        
        // Update primary symbol combo
        updatePrimarySymbolCombo(selectedWatchlist);
        
        JOptionPane.showMessageDialog(this, "Added " + added + " sample symbols to watchlist.");
    }
    
    private void saveWatchlists() {
        // Save the current primary symbol before closing
        String selectedWatchlist = watchlistList.getSelectedValue();
        if (selectedWatchlist != null) {
            WatchlistData watchlistData = watchlists.get(selectedWatchlist);
            String selectedPrimary = (String) primarySymbolCombo.getSelectedItem();
            watchlistData.setPrimarySymbol(selectedPrimary != null ? selectedPrimary : "");
        }
        
        saved = true;
        dispose();
    }
    
    public boolean isSaved() { 
        return saved; 
    }
    
    public Map<String, WatchlistData> getWatchlists() { 
        return new HashMap<>(watchlists); 
    }
    
    // For backward compatibility with old code
    public Map<String, Set<String>> getWatchlistsOldFormat() {
        Map<String, Set<String>> oldFormat = new HashMap<>();
        for (Map.Entry<String, WatchlistData> entry : watchlists.entrySet()) {
            oldFormat.put(entry.getKey(), new HashSet<>(entry.getValue().getSymbols()));
        }
        return oldFormat;
    }
    
   
}