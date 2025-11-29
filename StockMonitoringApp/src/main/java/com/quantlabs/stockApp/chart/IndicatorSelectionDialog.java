// Add this class to your project
package com.quantlabs.stockApp.chart;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

public class IndicatorSelectionDialog extends JDialog {
    private Set<String> selectedIndicators;
    
    private JCheckBox rsiCheckBox = new JCheckBox();
    private JCheckBox macdFastCheckBox = new JCheckBox();
    private JCheckBox macdSlowCheckBox = new JCheckBox();
    private JCheckBox psar001CheckBox = new JCheckBox();
    private JCheckBox psar005CheckBox = new JCheckBox();
    private JCheckBox heikenAshiCheckBox = new JCheckBox();
    private JCheckBox ma8CheckBox = new JCheckBox();
    private JCheckBox ma20CheckBox = new JCheckBox();
    private JCheckBox ma200CheckBox = new JCheckBox();
    private JCheckBox vwapCheckBox = new JCheckBox();
    
    private boolean confirmed = false;

    public IndicatorSelectionDialog(Frame parent, Set<String> currentIndicators) {
        super(parent, "Select Indicators", true);
        this.selectedIndicators = new HashSet<>(currentIndicators);
        
        initializeComponents();
        setupLayout();
        setLocationRelativeTo(parent);
        pack();
    }

    private void initializeComponents() {
        // Create checkboxes for each indicator
    	// Configure the pre-initialized checkboxes
        rsiCheckBox.setText("RSI(14)");
        rsiCheckBox.setSelected(selectedIndicators.contains("RSI(14)"));
        
        macdFastCheckBox.setText("MACD(5,8,9)");
        macdFastCheckBox.setSelected(selectedIndicators.contains("MACD(5,8,9)"));
        
        // ... configure all other checkboxes similarly
        macdSlowCheckBox.setText("MACD(12,26,9)");
        macdSlowCheckBox.setSelected(selectedIndicators.contains("MACD(12,26,9)"));
        
        psar001CheckBox.setText("PSAR(0.01)");
        psar001CheckBox.setSelected(selectedIndicators.contains("PSAR(0.01)"));
        
        psar005CheckBox.setText("PSAR(0.05)");
        psar005CheckBox.setSelected(selectedIndicators.contains("PSAR(0.05)"));
        
        heikenAshiCheckBox.setText("Heiken Ashi");
        heikenAshiCheckBox.setSelected(selectedIndicators.contains("HeikenAshi"));
        
        ma8CheckBox.setText("MA 8");
        ma8CheckBox.setSelected(selectedIndicators.contains("MA(8)"));
        
        ma20CheckBox.setText("MA 20");
        ma20CheckBox.setSelected(selectedIndicators.contains("MA(20)"));
        
        ma200CheckBox.setText("MA 200");
        ma200CheckBox.setSelected(selectedIndicators.contains("MA(200)"));
        
        vwapCheckBox.setText("Advanced VWAP");
        vwapCheckBox.setSelected(selectedIndicators.contains("VWAP"));
    }

    private void setupLayout() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Indicators panel
        JPanel indicatorsPanel = new JPanel(new GridLayout(0, 2, 10, 5));
        indicatorsPanel.setBorder(BorderFactory.createTitledBorder("Available Indicators"));
        
        indicatorsPanel.add(rsiCheckBox);
        indicatorsPanel.add(macdFastCheckBox);
        indicatorsPanel.add(macdSlowCheckBox);
        indicatorsPanel.add(psar001CheckBox);
        indicatorsPanel.add(psar005CheckBox);
        indicatorsPanel.add(heikenAshiCheckBox);
        indicatorsPanel.add(ma8CheckBox);
        indicatorsPanel.add(ma20CheckBox);
        indicatorsPanel.add(ma200CheckBox);
        indicatorsPanel.add(vwapCheckBox);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> {
            confirmed = true;
            updateSelectedIndicators();
            dispose();
        });

        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        mainPanel.add(indicatorsPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void updateSelectedIndicators() {
        selectedIndicators.clear();
        
        if (rsiCheckBox.isSelected()) selectedIndicators.add("RSI(14)");
        if (macdFastCheckBox.isSelected()) selectedIndicators.add("MACD(5,8,9)");
        if (macdSlowCheckBox.isSelected()) selectedIndicators.add("MACD(12,26,9)");
        if (psar001CheckBox.isSelected()) selectedIndicators.add("PSAR(0.01)");
        if (psar005CheckBox.isSelected()) selectedIndicators.add("PSAR(0.05)");
        if (heikenAshiCheckBox.isSelected()) selectedIndicators.add("HeikenAshi");
        if (ma8CheckBox.isSelected()) selectedIndicators.add("MA(8)");
        if (ma20CheckBox.isSelected()) selectedIndicators.add("MA(20)");
        if (ma200CheckBox.isSelected()) selectedIndicators.add("MA(200)");
        if (vwapCheckBox.isSelected()) selectedIndicators.add("VWAP");
    }

    public Set<String> getSelectedIndicators() {
        return confirmed ? new HashSet<>(selectedIndicators) : null;
    }
}