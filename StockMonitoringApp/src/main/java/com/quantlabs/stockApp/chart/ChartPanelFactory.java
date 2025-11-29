package com.quantlabs.stockApp.chart;

import java.awt.Font;
import java.text.SimpleDateFormat;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;

public class ChartPanelFactory {
    public static ChartPanel createChartPanel(JFreeChart chart, boolean useBuffer) {
        ChartPanel panel = new ChartPanel(chart, useBuffer);
        panel.setMouseWheelEnabled(true);
        panel.setMouseZoomable(true);
        panel.setDisplayToolTips(true);
        return panel;
    }
    
    public static DateAxis createSharedDateAxis() {
        DateAxis axis = new DateAxis("Time");
        axis.setDateFormatOverride(new SimpleDateFormat("dd/MM HH:mm"));
        return axis;
    }
    
    public static JLabel createCrosshairLabel() {
        JLabel label = new JLabel("Hover over the chart to see values.");
        label.setFont(new Font("Monospaced", Font.PLAIN, 14));
        label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        return label;
    }
}