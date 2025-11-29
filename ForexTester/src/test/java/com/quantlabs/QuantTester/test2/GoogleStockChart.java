package com.quantlabs.QuantTester.test2;
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.data.time.*;
import org.jfree.data.xy.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.List;

public class GoogleStockChart {

    public static void main(String[] args) {
        try {
            // 1. Fetch GOOGL data from Yahoo Finance
            List<OHLCDataItem> dataItems = fetchYahooFinanceData("GOOGL", "1h");
            
            // 2. Create dataset
            OHLCDataset dataset = createOHLCDataset(dataItems);
            
            // 3. Create chart with market hours only
            JFreeChart chart = createChart(dataset);
            
            // 4. Display chart
            ChartFrame frame = new ChartFrame("GOOGL Hourly Chart (Market Hours Only)", chart);
            frame.pack();
            frame.setVisible(true);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<OHLCDataItem> fetchYahooFinanceData(String symbol, String interval) throws IOException {
        String url = String.format("https://query1.finance.yahoo.com/v8/finance/chart/%s?interval=%s&range=1mo", 
                                 symbol, interval);
        
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
        }
        
        // Parse JSON response (simplified - you may want to use a proper JSON parser)
        List<OHLCDataItem> items = new ArrayList<>();
        String[] lines = response.toString().split("timestamp");
        // Actual parsing would be more complex - this is simplified
        // In reality, you'd parse the timestamps and OHLC values properly
        
        // For demo purposes, we'll create some mock data
        return createMockData();
    }

    private static List<OHLCDataItem> createMockData() {
        List<OHLCDataItem> items = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_MONTH, -30); // Go back 30 days
        
        Random random = new Random();
        
        // Create mock data for market hours only
        for (int i = 0; i < 30; i++) {
            // Skip weekends
            if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || 
                cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                cal.add(Calendar.DAY_OF_MONTH, 1);
                continue;
            }
            
            // Market hours (9:30 AM to 4:00 PM)
            for (int hour = 9; hour <= 16; hour++) {
                if (hour == 9 && i == 0) continue; // Skip if before 9:30
                
                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.set(Calendar.MINUTE, hour == 9 ? 30 : 0);
                cal.set(Calendar.SECOND, 0);
                
                double open = 100 + random.nextDouble() * 10;
                double close = open + (random.nextDouble() - 0.5) * 2;
                double high = Math.max(open, close) + random.nextDouble();
                double low = Math.min(open, close) - random.nextDouble();
                double volume = 1000000 + random.nextInt(5000000);
                
                items.add(new OHLCDataItem(cal.getTime(), open, high, low, close, volume));
                
                if (hour == 16) break; // Stop at 4:00 PM
            }
            
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        return items;
    }

    private static OHLCDataset createOHLCDataset(List<OHLCDataItem> items) {
        DefaultOHLCDataset dataset = new DefaultOHLCDataset("GOOGL", items.toArray(new OHLCDataItem[0]));
        return dataset;
    }

    private static JFreeChart createChart(OHLCDataset dataset) {
        // Create candlestick chart
        JFreeChart chart = ChartFactory.createCandlestickChart(
            "GOOGL Hourly Price (Market Hours Only)", 
            "Date/Time", 
            "Price", 
            dataset, 
            false
        );
        
        XYPlot plot = (XYPlot) chart.getPlot();
        
        // Configure axis to only show market hours and weekdays
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        
        // Create timeline that excludes non-market hours and weekends
        SegmentedTimeline timeline = new SegmentedTimeline(
            SegmentedTimeline.MINUTE_SEGMENT_SIZE, // 1 minute segments
            1, 
            24 * 60 // 24 hours in minutes
        );
        
        // Exclude weekends
        timeline.setBaseTimeline(SegmentedTimeline.newMondayThroughFridayTimeline());
        
        // Exclude non-market hours (before 9:30 and after 16:00)
        timeline.addException(0, 9 * 60 + 30); // midnight to 9:30
        timeline.addException(16 * 60, 24 * 60); // 16:00 to midnight
        
        axis.setTimeline(timeline);
        
        // Custom date format
        axis.setDateFormatOverride(new SimpleDateFormat("MMM-dd HH:mm") {
            @Override
            public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                
                // Show date at market open (9:30)
                if (cal.get(Calendar.HOUR_OF_DAY) == 9 && cal.get(Calendar.MINUTE) == 30) {
                    return new SimpleDateFormat("MMM-dd").format(date, toAppendTo, pos);
                }
                // Show time for other market hours
                return new SimpleDateFormat("HH:mm").format(date, toAppendTo, pos);
            }
        });
        
        // Set chart styling
        chart.setBackgroundPaint(Color.WHITE);
        plot.setBackgroundPaint(Color.LIGHT_GRAY);
        plot.setDomainGridlinePaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.WHITE);
        
        return chart;
    }
}