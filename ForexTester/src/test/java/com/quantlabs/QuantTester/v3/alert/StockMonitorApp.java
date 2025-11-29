package com.quantlabs.QuantTester.v3.alert;

import javax.swing.*;

import com.quantlabs.QuantTester.v3.alert.service.StockMonitorController;
import com.quantlabs.QuantTester.v3.alert.ui.MainFrame;

public class StockMonitorApp {
    private final StockMonitorController controller;

    public StockMonitorApp() {
        this.controller = new StockMonitorController();
        SwingUtilities.invokeLater(this::startGUI);
    }

    private void startGUI() {
        MainFrame mainFrame = new MainFrame(controller);
        mainFrame.setVisible(true);
    }

    public static void main(String[] args) {
        new StockMonitorApp();
    }
}