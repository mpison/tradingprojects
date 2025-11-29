package com.quantlabs.QuantTester.test2.watchlist;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import org.json.JSONArray;
import org.json.JSONObject;

public class StockWatchlistApp {
    private JFrame frame;
    private JTabbedPane tabbedPane;
    private WatchlistManagerPanel watchlistPanel;
    private IndicatorCombinationPanel combinationPanel;
    private WatchlistMapperPanel mapperPanel;

    public StockWatchlistApp() {
        frame = new JFrame("Stock Watchlist Application");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setLayout(new BorderLayout());

        // Create menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem saveAllItem = new JMenuItem("Save All Configurations");
        JMenuItem loadAllItem = new JMenuItem("Load All Configurations");
        
        fileMenu.add(saveAllItem);
        fileMenu.add(loadAllItem);
        menuBar.add(fileMenu);
        frame.setJMenuBar(menuBar);

        // Initialize panels
        watchlistPanel = new WatchlistManagerPanel();
        combinationPanel = new IndicatorCombinationPanel();
        mapperPanel = new WatchlistMapperPanel(watchlistPanel, combinationPanel);

        // Set up tabbed interface
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Watchlists", watchlistPanel);
        tabbedPane.addTab("Indicator Combinations", combinationPanel);
        tabbedPane.addTab("Watchlist Mappings", mapperPanel);
        
        frame.add(tabbedPane, BorderLayout.CENTER);

        // Add event listeners for menu items
        saveAllItem.addActionListener(e -> saveAllConfigurations());
        loadAllItem.addActionListener(e -> loadAllConfigurations());
        
        frame.setVisible(true);
    }

    private void saveAllConfigurations() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save All Configurations");
        
        if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            try {
                JSONObject root = new JSONObject();
                
                // 1. Save watchlists
                JSONArray watchlistsArray = new JSONArray();
                for (Watchlist watchlist : watchlistPanel.getWatchlists()) {
                    JSONObject watchlistObj = new JSONObject();
                    watchlistObj.put("name", watchlist.getName());
                    
                    JSONArray stocksArray = new JSONArray();
                    for (int i = 0; i < watchlist.getStocks().size(); i++) {
                        Stock stock = watchlist.getStocks().get(i);
                        JSONObject stockObj = new JSONObject();
                        stockObj.put("symbol", stock.getSymbol());
                        stockObj.put("name", stock.getName());
                        stocksArray.put(stockObj);
                    }
                    watchlistObj.put("stocks", stocksArray);
                    watchlistsArray.put(watchlistObj);
                }
                root.put("watchlists", watchlistsArray);
                
                // 2. Save indicator combinations
                JSONArray combinationsArray = new JSONArray();
                for (IndicatorCombination combo : combinationPanel.getCombinations()) {
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
                    combinationsArray.put(comboObj);
                }
                root.put("combinations", combinationsArray);
                
                // 3. Save watchlist mappings
                JSONArray mappingsArray = new JSONArray();
                for (WatchlistMapping mapping : mapperPanel.getMappings()) {
                    JSONObject mappingObj = new JSONObject();
                    mappingObj.put("name", mapping.getName());
                    mappingObj.put("watchlist", mapping.getWatchlist().getName());
                    mappingObj.put("combination", mapping.getCombination().getName());
                    mappingsArray.put(mappingObj);
                }
                root.put("mappings", mappingsArray);
                
                // Write to file
                try (FileWriter writer = new FileWriter(fileChooser.getSelectedFile())) {
                    writer.write(root.toString(2));
                    JOptionPane.showMessageDialog(frame, 
                        "All configurations saved successfully", 
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, 
                    "Error saving configurations: " + ex.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadAllConfigurations() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Load All Configurations");
        
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            try {
                String content = new String(java.nio.file.Files.readAllBytes(fileChooser.getSelectedFile().toPath()));
                JSONObject root = new JSONObject(content);
                
                // Clear existing data
                watchlistPanel.clear();
                combinationPanel.clear();
                mapperPanel.clear();
                
                // 1. Load watchlists
                if (root.has("watchlists")) {
                    JSONArray watchlistsArray = root.getJSONArray("watchlists");
                    for (int i = 0; i < watchlistsArray.length(); i++) {
                        JSONObject watchlistObj = watchlistsArray.getJSONObject(i);
                        Watchlist watchlist = new Watchlist(watchlistObj.getString("name"));
                        
                        JSONArray stocksArray = watchlistObj.getJSONArray("stocks");
                        for (int j = 0; j < stocksArray.length(); j++) {
                            JSONObject stockObj = stocksArray.getJSONObject(j);
                            watchlist.getStocks().addElement(new Stock(
                                stockObj.getString("symbol"),
                                stockObj.getString("name")));
                        }
                        watchlistPanel.addWatchlist(watchlist);
                    }
                }
                
                // 2. Load indicator combinations
                if (root.has("combinations")) {
                    JSONArray combinationsArray = root.getJSONArray("combinations");
                    for (int i = 0; i < combinationsArray.length(); i++) {
                        JSONObject comboObj = combinationsArray.getJSONObject(i);
                        String name = comboObj.getString("name");
                        
                        List<Indicator> indicators = new ArrayList<>();
                        JSONArray indicatorsArray = comboObj.getJSONArray("indicators");
                        for (int j = 0; j < indicatorsArray.length(); j++) {
                            JSONObject indicatorObj = indicatorsArray.getJSONObject(j);
                            String indicatorType = indicatorObj.getString("type");
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
                                        value = ((Number) value).doubleValue();
                                    }
                                    params.put(key, value);
                                }
                            }
                            
                            indicators.add(new Indicator(indicatorType, timeframe, shift, params));
                        }
                        
                        combinationPanel.addCombination(new IndicatorCombination(name, indicators));
                    }
                }
                
                // 3. Load mappings (must be done after watchlists and combinations are loaded)
                if (root.has("mappings")) {
                    JSONArray mappingsArray = root.getJSONArray("mappings");
                    for (int i = 0; i < mappingsArray.length(); i++) {
                        JSONObject mappingObj = mappingsArray.getJSONObject(i);
                        String mappingName = mappingObj.getString("name");
                        String watchlistName = mappingObj.getString("watchlist");
                        String comboName = mappingObj.getString("combination");
                        
                        Watchlist watchlist = watchlistPanel.getWatchlistByName(watchlistName);
                        IndicatorCombination combo = combinationPanel.getCombinationByName(comboName);
                        
                        if (watchlist != null && combo != null) {
                            mapperPanel.addMapping(new WatchlistMapping(mappingName, watchlist, combo));
                        } else {
                            System.err.println("Could not find watchlist or combination for mapping: " + mappingName);
                        }
                    }
                }
                
                JOptionPane.showMessageDialog(frame, 
                    "All configurations loaded successfully", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, 
                    "Error loading configurations: " + ex.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StockWatchlistApp());
    }
}

