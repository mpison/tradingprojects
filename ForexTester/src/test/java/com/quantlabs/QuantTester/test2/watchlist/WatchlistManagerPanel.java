package com.quantlabs.QuantTester.test2.watchlist;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class WatchlistManagerPanel extends JPanel {
    private DefaultListModel<Watchlist> watchlistModel;
    private JList<Watchlist> watchlistList;
    private JTextField watchlistNameField;
    private JButton addWatchlistButton, removeWatchlistButton;
    
    private JPanel stockPanel;
    private DefaultListModel<Stock> stockListModel;
    private JList<Stock> stockList;
    private JTextField symbolField, nameField;
    private JButton addStockButton, updateStockButton, deleteStockButton, clearStockButton;
    
    public WatchlistManagerPanel() {
        setLayout(new BorderLayout());
        
        // Watchlist management panel
        JPanel watchlistControlPanel = new JPanel(new BorderLayout());
        watchlistModel = new DefaultListModel<>();
        watchlistList = new JList<>(watchlistModel);
        watchlistList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane watchlistScrollPane = new JScrollPane(watchlistList);
        
        JPanel watchlistInputPanel = new JPanel(new BorderLayout(5, 5));
        watchlistNameField = new JTextField();
        watchlistInputPanel.add(new JLabel("Watchlist Name:"), BorderLayout.NORTH);
        watchlistInputPanel.add(watchlistNameField, BorderLayout.CENTER);
        
        addWatchlistButton = new JButton("Add Watchlist");
        removeWatchlistButton = new JButton("Remove Watchlist");
        
        JPanel watchlistButtonPanel = new JPanel();
        watchlistButtonPanel.add(addWatchlistButton);
        watchlistButtonPanel.add(removeWatchlistButton);
        
        watchlistControlPanel.add(watchlistScrollPane, BorderLayout.CENTER);
        watchlistControlPanel.add(watchlistInputPanel, BorderLayout.NORTH);
        watchlistControlPanel.add(watchlistButtonPanel, BorderLayout.SOUTH);
        
        // Stock management panel
        stockPanel = new JPanel(new BorderLayout());
        stockPanel.setBorder(BorderFactory.createTitledBorder("Stocks in Watchlist"));
        
        stockListModel = new DefaultListModel<>();
        stockList = new JList<>(stockListModel);
        stockList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane stockScrollPane = new JScrollPane(stockList);
        
        JPanel stockInputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        symbolField = new JTextField();
        nameField = new JTextField();
        
        stockInputPanel.add(new JLabel("Symbol:"));
        stockInputPanel.add(symbolField);
        stockInputPanel.add(new JLabel("Name:"));
        stockInputPanel.add(nameField);
        
        addStockButton = new JButton("Add Stock");
        updateStockButton = new JButton("Update Stock");
        deleteStockButton = new JButton("Delete Stock");
        clearStockButton = new JButton("Clear");
        
        JPanel stockButtonPanel = new JPanel();
        stockButtonPanel.add(addStockButton);
        stockButtonPanel.add(updateStockButton);
        stockButtonPanel.add(deleteStockButton);
        stockButtonPanel.add(clearStockButton);
        
        stockPanel.add(stockScrollPane, BorderLayout.CENTER);
        stockPanel.add(stockInputPanel, BorderLayout.NORTH);
        stockPanel.add(stockButtonPanel, BorderLayout.SOUTH);
        
        // Add components to main panel
        add(watchlistControlPanel, BorderLayout.WEST);
        add(stockPanel, BorderLayout.CENTER);
        
        // Event listeners
        addWatchlistButton.addActionListener(e -> addWatchlist());
        removeWatchlistButton.addActionListener(e -> removeWatchlist());
        addStockButton.addActionListener(e -> addStock());
        updateStockButton.addActionListener(e -> updateStock());
        deleteStockButton.addActionListener(e -> deleteStock());
        clearStockButton.addActionListener(e -> clearStockFields());
        watchlistList.addListSelectionListener(e -> loadSelectedWatchlist());
        
        // Add sample data
        addSampleData();
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private void addSampleData() {
        Watchlist techWatchlist = new Watchlist("Tech Stocks");
        try {
            techWatchlist.addStock(new Stock("AAPL", "Apple Inc."));
            techWatchlist.addStock(new Stock("MSFT", "Microsoft"));
            techWatchlist.addStock(new Stock("GOOGL", "Alphabet"));
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
        watchlistModel.addElement(techWatchlist);
        
        Watchlist financeWatchlist = new Watchlist("Finance Stocks");
        try {
            financeWatchlist.addStock(new Stock("JPM", "JPMorgan Chase"));
            financeWatchlist.addStock(new Stock("BAC", "Bank of America"));
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
        watchlistModel.addElement(financeWatchlist);
    }
    
    private void addWatchlist() {
        String name = watchlistNameField.getText().trim();
        if (!name.isEmpty()) {
            watchlistModel.addElement(new Watchlist(name));
            watchlistNameField.setText("");
        } else {
            showError("Please enter a watchlist name");
        }
    }
    
    private void removeWatchlist() {
        int selectedIndex = watchlistList.getSelectedIndex();
        if (selectedIndex != -1) {
            watchlistModel.remove(selectedIndex);
            stockListModel.clear();
        } else {
            showError("Please select a watchlist to remove");
        }
    }
    
    private void addStock() {
        Watchlist selectedWatchlist = watchlistList.getSelectedValue();
        if (selectedWatchlist == null) {
            showError("Please select a watchlist first");
            return;
        }

        String symbol = symbolField.getText().trim().toUpperCase();
        String name = nameField.getText().trim();
        
        if (symbol.isEmpty() || name.isEmpty()) {
            showError("Please enter both symbol and name");
            return;
        }

        try {
            Stock stock = new Stock(symbol, name);
            selectedWatchlist.addStock(stock);
            stockListModel.addElement(stock);
            clearStockFields();
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }
    
    private void updateStock() {
        Watchlist selectedWatchlist = watchlistList.getSelectedValue();
        int selectedStockIndex = stockList.getSelectedIndex();
        
        if (selectedWatchlist == null || selectedStockIndex == -1) {
            showError("Please select a watchlist and stock to update");
            return;
        }

        String symbol = symbolField.getText().trim().toUpperCase();
        String name = nameField.getText().trim();
        
        if (symbol.isEmpty() || name.isEmpty()) {
            showError("Please enter both symbol and name");
            return;
        }

        try {
            // First remove the old stock
            Stock oldStock = stockListModel.get(selectedStockIndex);
            selectedWatchlist.removeStock(oldStock);
            
            // Add the updated stock
            Stock newStock = new Stock(symbol, name);
            selectedWatchlist.addStock(newStock);
            stockListModel.set(selectedStockIndex, newStock);
            
            clearStockFields();
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            // Reload the selected watchlist to maintain consistency
            loadSelectedWatchlist();
        }
    }
    
    private void deleteStock() {
        Watchlist selectedWatchlist = watchlistList.getSelectedValue();
        int selectedStockIndex = stockList.getSelectedIndex();
        
        if (selectedWatchlist != null && selectedStockIndex != -1) {
            Stock stockToRemove = stockListModel.get(selectedStockIndex);
            selectedWatchlist.removeStock(stockToRemove);
            stockListModel.remove(selectedStockIndex);
            clearStockFields();
        } else {
            showError("Please select a watchlist and stock to delete");
        }
    }
    
    private void loadSelectedWatchlist() {
        Watchlist selected = watchlistList.getSelectedValue();
        stockListModel.clear();
        if (selected != null) {
            for (int i = 0; i < selected.getStocks().size(); i++) {
                stockListModel.addElement(selected.getStocks().get(i));
            }
            stockPanel.setBorder(BorderFactory.createTitledBorder("Stocks in " + selected.getName()));
        }
    }
    
    private void clearStockFields() {
        symbolField.setText("");
        nameField.setText("");
        stockList.clearSelection();
    }
    
    public void clear() {
        watchlistModel.clear();
        stockListModel.clear();
    }
    
    public void addWatchlist(Watchlist watchlist) {
        watchlistModel.addElement(watchlist);
    }
    
    public List<Watchlist> getWatchlists() {
        List<Watchlist> watchlists = new ArrayList<>();
        for (int i = 0; i < watchlistModel.size(); i++) {
            watchlists.add(watchlistModel.get(i));
        }
        return watchlists;
    }
    
    public Watchlist getWatchlistByName(String name) {
        for (int i = 0; i < watchlistModel.size(); i++) {
            Watchlist w = watchlistModel.get(i);
            if (w.getName().equals(name)) {
                return w;
            }
        }
        return null;
    }
}