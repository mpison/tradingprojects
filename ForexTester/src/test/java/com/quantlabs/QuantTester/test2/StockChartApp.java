package com.quantlabs.QuantTester.test2;

import okhttp3.*;
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.data.time.*;
import org.jfree.data.xy.*;
import org.json.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.List;

import okhttp3.*;
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.data.time.*;
import org.jfree.data.xy.*;
import org.json.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.List;

public class StockChartApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // 1. Fetch data
                YahooFinanceFetcher fetcher = new YahooFinanceFetcher();
                List<OHLCDataItem> data = fetcher.fetchData("GOOGL", "1h", 30); // Last 30 days

                // 2. Create dataset
                OHLCDataset dataset = createOHLCDataset(data);

                // 3. Create and configure chart
                JFreeChart chart = createChart(dataset);

                // 4. Display chart
                displayChart(chart);

            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Error fetching data: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });
    }

    private static OHLCDataset createOHLCDataset(List<OHLCDataItem> items) {
        return new DefaultOHLCDataset("GOOGL", items.toArray(new OHLCDataItem[0]));
    }

    private static JFreeChart createChart(OHLCDataset dataset) {
        // Create candlestick chart
        JFreeChart chart = ChartFactory.createCandlestickChart(
            "GOOGL Hourly Price (Market Hours Only)", 
            "Time (Market Hours)", 
            "Price ($)", 
            dataset, 
            false
        );

        XYPlot plot = (XYPlot) chart.getPlot();
        
        // Configure axis to show hourly labels
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setTickUnit(new DateTickUnit(DateTickUnitType.HOUR, 1)); // Show every hour
        
        // Custom date format for hourly display
        axis.setDateFormatOverride(new SimpleDateFormat("HH:mm") {
            @Override
            public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                
                // Only show labels for full hours (XX:00)
                if (cal.get(Calendar.MINUTE) == 0) {
                    return super.format(date, toAppendTo, pos);
                }
                return new StringBuffer(); // Skip non-hourly labels
            }
        });

        // Styling
        chart.setBackgroundPaint(Color.WHITE);
        plot.setBackgroundPaint(new Color(240, 240, 240));
        plot.setDomainGridlinePaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.WHITE);

        return chart;
    }

    private static void displayChart(JFreeChart chart) {
        JFrame frame = new JFrame("Stock Chart");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(1000, 600));
        frame.add(chartPanel, BorderLayout.CENTER);
        
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

