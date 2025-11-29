package com.quantlabs.stockApp;

import javax.swing.JTable;

import com.quantlabs.stockApp.alert.ZScoreAlertManager;
import com.quantlabs.stockApp.alert.ui.ZScoreAlertConfigDialog;
import com.quantlabs.stockApp.utils.ZScoreColumnHelper;

public interface IStockDashboard0 {
	public void logToConsole(String message);
	public ZScoreAlertManager getZScoreAlertManager();
	public FilterableTableModel getTableModel();
	public ZScoreColumnHelper getZScoreColumnHelper();
	public void updateTableRenderers() ;
	public JTable getDashboardTable();
	public ZScoreAlertConfigDialog getZScoreAlertConfigDialog();
	public boolean isAlarmActive();
	public void startBuzzAlert(String alertMessage);
}
