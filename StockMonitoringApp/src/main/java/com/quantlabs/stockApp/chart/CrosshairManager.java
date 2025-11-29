package com.quantlabs.stockApp.chart;

import org.jfree.chart.*;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.*;
import org.jfree.data.time.ohlc.OHLCSeries;
import org.jfree.data.time.ohlc.OHLCItem;
import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CrosshairManager {
    private final DateAxis sharedAxis;
    private final JFreeChart priceChart;
    private final JFreeChart indicatorChart;
    private final JLabel crosshairLabel;
    private final OHLCSeries ohlcSeries;
    private final TimeSeriesCollection indicatorDataset;

    public CrosshairManager(DateAxis sharedAxis, JFreeChart priceChart, 
                          JFreeChart indicatorChart, JLabel crosshairLabel,
                          OHLCSeries ohlcSeries, TimeSeriesCollection indicatorDataset) {
        this.sharedAxis = sharedAxis;
        this.priceChart = priceChart;
        this.indicatorChart = indicatorChart;
        this.crosshairLabel = crosshairLabel;
        this.ohlcSeries = ohlcSeries;
        this.indicatorDataset = indicatorDataset;
        
        configureCrosshairs();
    }

    private void configureCrosshairs() {
        priceChart.getXYPlot().setDomainCrosshairVisible(true);
        priceChart.getXYPlot().setRangeCrosshairVisible(true);
        indicatorChart.getXYPlot().setDomainCrosshairVisible(true);
        indicatorChart.getXYPlot().setRangeCrosshairVisible(true);
    }

    public void install(ChartPanel pricePanel, ChartPanel indicatorPanel) {
        MouseMotionListener listener = new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {}
            
            @Override
            public void mouseMoved(MouseEvent e) {
                ChartPanel sourcePanel = (ChartPanel) e.getSource();
                XYPlot plot = sourcePanel.getChart().getXYPlot();
                ChartRenderingInfo info = sourcePanel.getChartRenderingInfo();
                
                if (info == null) return;
                
                Point2D p = sourcePanel.translateScreenToJava2D(e.getPoint());
                Rectangle2D dataArea = info.getPlotInfo().getDataArea();
                
                double x = plot.getDomainAxis().java2DToValue(p.getX(), dataArea, plot.getDomainAxisEdge());
                Date time = new Date((long) x);
                
                updateCrosshairs(x, sourcePanel);
                updateCrosshairLabel(time);
            }
        };
        
        pricePanel.addMouseMotionListener(listener);
        indicatorPanel.addMouseMotionListener(listener);
    }

    private void updateCrosshairs(double xValue, ChartPanel sourcePanel) {
        priceChart.getXYPlot().setDomainCrosshairValue(xValue);
        indicatorChart.getXYPlot().setDomainCrosshairValue(xValue);
        
        if (sourcePanel.getChart() == priceChart) {
            ChartRenderingInfo info = sourcePanel.getChartRenderingInfo();
            if (info != null) {
                Point2D p = sourcePanel.translateScreenToJava2D(sourcePanel.getMousePosition());
                if (p != null) {
                    Rectangle2D dataArea = info.getPlotInfo().getDataArea();
                    double y = priceChart.getXYPlot().getRangeAxis().java2DToValue(
                        p.getY(), dataArea, priceChart.getXYPlot().getRangeAxisEdge());
                    priceChart.getXYPlot().setRangeCrosshairValue(y);
                }
            }
        }
    }

    private void updateCrosshairLabel(Date time) {
        StringBuilder sb = new StringBuilder("<html>");
        SimpleDateFormat timeFormat = new SimpleDateFormat("dd/MM HH:mm");
        
        sb.append("🕒 <b>").append(timeFormat.format(time)).append("</b>");
        
        // Price data - find nearest OHLC item
        OHLCItem nearestOhlcItem = findNearestOhlcItem(time);
        if (nearestOhlcItem != null) {
            sb.append("<br>💰 <b>Price:</b> O:").append(String.format("%.4f", nearestOhlcItem.getOpenValue()))
              .append(" H:").append(String.format("%.4f", nearestOhlcItem.getHighValue()))
              .append(" L:").append(String.format("%.4f", nearestOhlcItem.getLowValue()))
              .append(" C:").append(String.format("%.4f", nearestOhlcItem.getCloseValue()));
        } else {
            sb.append("<br>💰 No price data available");
        }
        
        // Indicator data
        try {
            if (indicatorDataset != null && indicatorDataset.getSeriesCount() > 0) {
                sb.append("<br>📊 <b>Indicators:</b> ");
                for (int i = 0; i < indicatorDataset.getSeriesCount(); i++) {
                    TimeSeries series = indicatorDataset.getSeries(i);
                    TimeSeriesDataItem nearestIndicatorItem = findNearestTimeSeriesItem(series, time);
                    if (nearestIndicatorItem != null) {
                        sb.append(series.getKey()).append(":")
                          .append(String.format("%.4f", nearestIndicatorItem.getValue())).append(" ");
                    }
                }
            }
        } catch (Exception e) {
            sb.append("<br>📊 No indicator data available");
        }
        
        sb.append("</html>");
        crosshairLabel.setText(sb.toString());
    }

    private OHLCItem findNearestOhlcItem(Date time) {
        long targetTime = time.getTime();
        long minDiff = Long.MAX_VALUE;
        OHLCItem nearestItem = null;
        
        for (int i = 0; i < ohlcSeries.getItemCount(); i++) {
            OHLCItem item = (OHLCItem) ohlcSeries.getDataItem(i);
            long itemTime = item.getPeriod().getStart().getTime();
            long diff = Math.abs(itemTime - targetTime);
            
            if (diff < minDiff) {
                minDiff = diff;
                nearestItem = item;
            }
        }
        
        return nearestItem;
    }

    private TimeSeriesDataItem findNearestTimeSeriesItem(TimeSeries series, Date time) {
        long targetTime = time.getTime();
        long minDiff = Long.MAX_VALUE;
        TimeSeriesDataItem nearestItem = null;
        
        for (int i = 0; i < series.getItemCount(); i++) {
            TimeSeriesDataItem item = series.getDataItem(i);
            long itemTime = item.getPeriod().getStart().getTime();
            long diff = Math.abs(itemTime - targetTime);
            
            if (diff < minDiff) {
                minDiff = diff;
                nearestItem = item;
            }
        }
        
        return nearestItem;
    }
}