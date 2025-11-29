package com.quantlabs.stockApp;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;

import javax.swing.JRadioButton;
import javax.swing.JTable;

import com.quantlabs.stockApp.alert.ZScoreAlertManager;
import com.quantlabs.stockApp.alert.ui.ZScoreAlertConfigDialog;
import com.quantlabs.stockApp.utils.ZScoreColumnHelper;

public interface IStockDashboard {
	public void logToConsole(String message);
	public ZScoreAlertManager getZScoreAlertManager();
	public FilterableTableModel getTableModel();
	public ZScoreColumnHelper getZScoreColumnHelper();
	public void updateTableRenderers() ;
	public JTable getDashboardTable();
	public ZScoreAlertConfigDialog getZScoreAlertConfigDialog();
	public boolean isAlarmActive();
	public void startBuzzAlert(String alertMessage);
	public String getCurrentWatchlistName();
	public Set<String> getSelectedBullishTimeframes();
	public Map<String, Set<String>> getCustomIndicatorCombinations();
	public void setCustomIndicatorCombinations(Map<String, Set<String>> combinations);
	public void updateTableColumnsForCustomIndicators();
	public JRadioButton getCurrentTimeRadio();
	public ZonedDateTime getStartDateTime();
	public ZonedDateTime getEndDateTime();
}
