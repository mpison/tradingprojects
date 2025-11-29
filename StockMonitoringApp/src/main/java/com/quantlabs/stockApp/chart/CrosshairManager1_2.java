package com.quantlabs.stockApp.chart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.event.MouseInputAdapter;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.Layer;
import org.jfree.data.xy.XYDataset;

public class CrosshairManager1_2 extends MouseInputAdapter {
    private final List<ChartPanel> chartPanels = new ArrayList<>();
    private final List<ValueMarker> domainMarkers = new ArrayList<>();
    private final List<ValueMarker> rangeMarkers = new ArrayList<>();
    private final JLabel infoLabel;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    double lastX = Double.NaN;

    public CrosshairManager1_2(JLabel infoLabel) {
        this.infoLabel = infoLabel;
    }

    public void addChartPanel(ChartPanel chartPanel) {
        System.out.println("[DEBUG] Initializing crosshair for chart panel");

        // 1. Null checks
        if (chartPanel == null) {
            System.err.println("[ERROR] Null ChartPanel provided");
            return;
        }
        
        // 2. Verify chart exists
        JFreeChart chart = chartPanel.getChart();
        if (chart == null) {
            System.err.println("[ERROR] ChartPanel has no JFreeChart");
            return;
        }

        // 3. Verify plot type
        Plot plot = chart.getPlot();
        if (!(plot instanceof XYPlot)) {
            System.err.println("[ERROR] Plot is not XYPlot");
            return;
        }
        XYPlot xyPlot = (XYPlot) plot;

        // 4. Create BRIGHTLY VISIBLE markers (magenta/yellow for debugging)
        ValueMarker domainMarker = new ValueMarker(Double.NaN, Color.MAGENTA, new BasicStroke(2f));
        ValueMarker rangeMarker = new ValueMarker(Double.NaN, Color.YELLOW, new BasicStroke(2f));
        
        // 5. Add to foreground with notification
        xyPlot.addDomainMarker(domainMarker, Layer.FOREGROUND);
        xyPlot.addRangeMarker(rangeMarker, Layer.FOREGROUND);
        System.out.println("[DEBUG] Markers added to plot");

        // 6. Store references
        domainMarkers.add(domainMarker);
        rangeMarkers.add(rangeMarker);
        chartPanels.add(chartPanel);

        // 7. Remove any existing listeners
        Arrays.stream(chartPanel.getMouseMotionListeners())
              .forEach(chartPanel::removeMouseMotionListener);

        // 8. Add listeners with debug
        chartPanel.addMouseMotionListener(this);
        chartPanel.addMouseListener(this);
        System.out.println("[DEBUG] Listeners added to panel");

        // 9. Enable focus and verify
        chartPanel.setFocusable(true);
        chartPanel.requestFocusInWindow();
        System.out.println("[DEBUG] Focus requested. Current focus: " + 
            (chartPanel.hasFocus() ? "SUCCESS" : "FAILED"));
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        System.out.println("\n[EVENT] Mouse moved detected!");
        
        if (!(e.getSource() instanceof ChartPanel)) {
            System.err.println("[ERROR] Event source is not ChartPanel");
            return;
        }

        ChartPanel panel = (ChartPanel) e.getSource();
        System.out.println("[DEBUG] Event from: " + panel.getName());
        
        if (!panel.hasFocus()) {
            System.out.println("[WARNING] Panel lacks focus - requesting focus");
            panel.requestFocusInWindow();
        }

        try {
            XYPlot plot = (XYPlot) panel.getChart().getPlot();
            Rectangle2D dataArea = panel.getScreenDataArea();
            
            if (dataArea == null) {
                System.err.println("[ERROR] Null data area");
                return;
            }

            // Convert coordinates with debug
            double x = plot.getDomainAxis().java2DToValue(e.getX(), dataArea, plot.getDomainAxisEdge());
            double y = plot.getRangeAxis().java2DToValue(e.getY(), dataArea, plot.getRangeAxisEdge());
            System.out.printf("[COORDS] X: %.2f, Y: %.2f%n", x, y);

            // Update markers
            updateCrosshairs(x, y);
            updateInfoLabel(x);
            lastX = x;

            System.out.println("[SUCCESS] Crosshairs updated");
        } catch (Exception ex) {
            System.err.println("[ERROR] In mouseMoved: " + ex.getMessage());
            ex.printStackTrace();
            hideMarkers();
        }
    }

    void updateCrosshairs(double x, double y) {
        for (int i = 0; i < chartPanels.size(); i++) {
            ChartPanel panel = chartPanels.get(i);
            XYPlot plot = (XYPlot) panel.getChart().getPlot();
            
            // Update vertical line
            domainMarkers.get(i).setValue(x);
            
            // Find closest Y value
            XYDataset dataset = plot.getDataset();
            if (dataset != null) {
                for (int series = 0; series < dataset.getSeriesCount(); series++) {
                    for (int item = 0; item < dataset.getItemCount(series); item++) {
                        if (Math.abs(dataset.getXValue(series, item) - x) < 0.0001) {
                            rangeMarkers.get(i).setValue(dataset.getYValue(series, item));
                            break;
                        }
                    }
                }
            }
            
            panel.repaint();
        }
    }

    private void updateInfoLabel(double x) {
        StringBuilder sb = new StringBuilder("<html><b>Time:</b> ");
        sb.append(dateFormat.format(new java.util.Date((long)x))).append("<br>");
        
        for (int i = 0; i < chartPanels.size(); i++) {
            XYPlot plot = (XYPlot) chartPanels.get(i).getChart().getPlot();
            XYDataset dataset = plot.getDataset();
            
            if (dataset != null) {
                for (int series = 0; series < dataset.getSeriesCount(); series++) {
                    for (int item = 0; item < dataset.getItemCount(series); item++) {
                        if (Math.abs(dataset.getXValue(series, item) - x) < 0.0001) {
                            String seriesName = dataset.getSeriesKey(series).toString();
                            double yValue = dataset.getYValue(series, item);
                            sb.append("<b>").append(seriesName).append(":</b> ")
                              .append(String.format("%.4f", yValue)).append("<br>");
                        }
                    }
                }
            }
        }
        
        infoLabel.setText(sb.append("</html>").toString());
    }

    private void hideMarkers() {
        for (int i = 0; i < chartPanels.size(); i++) {
            domainMarkers.get(i).setValue(Double.NaN);
            rangeMarkers.get(i).setValue(Double.NaN);
            chartPanels.get(i).repaint();
        }
        infoLabel.setText("Move mouse over chart to see values");
    }

    @Override
    public void mouseExited(MouseEvent e) {
        hideMarkers();
    }

    public void cleanup() {
        for (ChartPanel panel : chartPanels) {
            panel.removeMouseMotionListener(this);
            panel.removeMouseListener(this);
            
            XYPlot plot = (XYPlot) panel.getChart().getPlot();
            domainMarkers.forEach(plot::removeDomainMarker);
            rangeMarkers.forEach(plot::removeRangeMarker);
        }
        chartPanels.clear();
        domainMarkers.clear();
        rangeMarkers.clear();
    }

    // Required by MouseInputAdapter
    @Override 
    public void mouseEntered(MouseEvent e) {}
    
    @Override
    public void mouseDragged(MouseEvent e) {}
}