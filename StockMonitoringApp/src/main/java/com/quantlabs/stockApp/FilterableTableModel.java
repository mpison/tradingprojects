package com.quantlabs.stockApp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public class FilterableTableModel extends DefaultTableModel {
	private final Set<String> hiddenSymbols;
	private final Map<String, Integer> symbolToRowMap;
	Map<String, double[]> columnFilters = new HashMap<>();
	IStockDashboard stockDashboard;

	// private Vector<String> columnIdentifiers;

	public FilterableTableModel(Vector<Vector<Object>> data, Vector<String> columnNames, IStockDashboard stockDashboard) {
		super(data, columnNames);
		this.hiddenSymbols = Collections.synchronizedSet(new HashSet<>());
		this.symbolToRowMap = new ConcurrentHashMap<>();
		this.stockDashboard = stockDashboard;
	}

	public FilterableTableModel(Object[] columnNames, int rowCount, IStockDashboard stockDashboard)  {
		super(columnNames, rowCount);
		this.hiddenSymbols = Collections.synchronizedSet(new HashSet<>());
		this.symbolToRowMap = new ConcurrentHashMap<>();
		this.columnIdentifiers = new Vector<>(Arrays.asList((String[]) columnNames));
		this.stockDashboard = stockDashboard;
	}

	public void setColumnFilters(Map<String, double[]> newFilters) {
		this.columnFilters = newFilters != null ? new HashMap<>(newFilters) : new HashMap<>();
	}

	@Override
	public synchronized int getRowCount() {
		try {
			return super.getRowCount() - (hiddenSymbols != null ? hiddenSymbols.size() : 0);
		} catch (Exception e) {
			//logToConsole("Error in getRowCount: " + e.getMessage());
			return 0;
		}
	}

	@Override
	public synchronized Object getValueAt(int row, int column) {
		try {
			int modelRow = convertViewRowToModel(row);
			return super.getValueAt(modelRow, column);
		} catch (Exception e) {
			//logToConsole("Error in getValueAt: " + e.getMessage());
			return null;
		}
	}

	private synchronized int convertViewRowToModel(int viewRow) {
		try {
			int visibleCount = -1;
			for (int i = 0; i < super.getRowCount(); i++) {
				String symbol = (String) super.getValueAt(i, 1);
				if (symbol != null && !hiddenSymbols.contains(symbol)) {
					visibleCount++;
					if (visibleCount == viewRow) {
						return i;
					}
				}
			}
			return -1;
		} catch (Exception e) {
			//logToConsole("Error in convertViewRowToModel: " + e.getMessage());
			return -1;
		}
	}

	@Override
	public synchronized void addRow(Object[] rowData) {
		try {
			super.addRow(rowData);
			if (rowData != null && rowData.length > 1 && rowData[1] instanceof String) {
				String symbol = (String) rowData[1];
				symbolToRowMap.put(symbol, super.getRowCount() - 1);
			}
		} catch (Exception e) {
			//logToConsole("Error adding row: " + e.getMessage());
		}
	}

	@Override
	public synchronized void removeRow(int row) {
		try {
			String symbol = (String) getValueAt(row, 1);
			super.removeRow(row);
			if (symbol != null) {
				symbolToRowMap.remove(symbol);
				hiddenSymbols.remove(symbol);
			}
			rebuildSymbolMap();
		} catch (Exception e) {
			//logToConsole("Error removing row: " + e.getMessage());
		}
	}

	private synchronized void rebuildSymbolMap() {
		try {
			symbolToRowMap.clear();
			for (int i = 0; i < super.getRowCount(); i++) {
				Object value = super.getValueAt(i, 1);
				if (value instanceof String) {
					symbolToRowMap.put((String) value, i);
				}
			}
		} catch (Exception e) {
			//logToConsole("Error rebuilding symbol map: " + e.getMessage());
		}
	}

	public synchronized void toggleSymbolsVisibility(Set<String> symbolsToToggle) {
		try {
			if (symbolsToToggle == null || symbolsToToggle.isEmpty()) {
				return;
			}

			for (String symbol : symbolsToToggle) {
				if (hiddenSymbols.contains(symbol)) {
					hiddenSymbols.remove(symbol);
				} else {
					hiddenSymbols.add(symbol);
				}
			}
			fireTableDataChanged();
		} catch (Exception e) {
			//logToConsole("Error in toggleSymbolsVisibility: " + e.getMessage());
		}
	}

	public synchronized boolean isSymbolHidden(String symbol) {
		try {
			return hiddenSymbols != null && hiddenSymbols.contains(symbol);
		} catch (Exception e) {
			//logToConsole("Error in isSymbolHidden: " + e.getMessage());
			return false;
		}
	}

	public synchronized void showAllSymbols() {
		try {
			if (hiddenSymbols != null) {
				hiddenSymbols.clear();
				fireTableDataChanged();
			}
		} catch (Exception e) {
			//logToConsole("Error in showAllSymbols: " + e.getMessage());
		}
	}

	@Override
	public synchronized boolean isCellEditable(int row, int column) {
		try {
			int modelRow = convertViewRowToModel(row);
			return modelRow != -1 && column == 0;
		} catch (Exception e) {
			//logToConsole("Error in isCellEditable: " + e.getMessage());
			return false;
		}
	}

	@Override
	public synchronized Class<?> getColumnClass(int column) {
		
		String colName = getColumnName(column);
	    
	    // Z-Score column detection
	    if (colName != null) {
	        if (colName.startsWith("ZScore_")) {
	            if (colName.endsWith("_Rank")) {
	                return Integer.class;  // Use Integer for ranks
	            } else if (colName.endsWith("_ZScore")) {
	                return Double.class;   // Use Double for Z-Scores
	            }
	        }
	    }

		List<String> datacolumns = new ArrayList<>(Arrays.asList("postmarketClose", "premarketChange",
				"changeFromOpen", "percentChange", "postmarketChange", "gap", "premarketVolume", "currentVolume",
				"postmarketVolume", "previousVolume", "prevLastDayPrice", "averageVol", "analystRating",
				"premarketHigh", "high", "postmarketHigh", "premarketLow", "low", "postmarketLow",
				"premarketHighestPercentile", "marketHighestPercentile", "marketLowestPercentile","postmarketHighestPercentile"));

		if (colName == null) {
			return String.class; // Fallback
		}
		if (colName.equals("Select") || colName.equals("Checkbox")) {
			return Boolean.class;
		} else if (colName.equals("Symbol")) {
			return String.class;
		} else if (colName.equals("Price") || colName.equals("% Change") || colName.contains("Change")) {
			return Double.class;
		} else if (colName.equals("Volume") || colName.equals("Prev Volume") || colName.contains("Vol")) {
			return Long.class;
		} else if (colName.contains("Breakout") || colName.contains("Breakout Count")) {
			return BreakoutValue.class;
		} else if (datacolumns.contains(colName)) {
			return Double.class;
		}
		// Add more mappings for other specific columns if needed (e.g., RSI, MACD)
		return String.class;
	}

	@Override
	public synchronized void setValueAt(Object value, int row, int column) {
	    try {
	        String colName = getColumnName(column);
	        
	        // Handle Z-Score columns specifically
	        if (colName != null && colName.startsWith("ZScore_")) {
	            int modelRow = convertViewRowToModel(row);
	            if (modelRow != -1) {
	                // Ensure data type consistency
	                if (colName.endsWith("_Rank") && value instanceof Double) {
	                    value = ((Double) value).intValue();
	                }
	                super.setValueAt(value, modelRow, column);
	            }
	        } else {
	            // Existing logic for other columns
	            int modelRow = convertViewRowToModel(row);
	            if (modelRow != -1) {
	                super.setValueAt(value, modelRow, column);
	            }
	        }
	    } catch (Exception e) {
	        //logToConsole("Error in setValueAt: " + e.getMessage());
	    }
	}

	// Update existing methods to use columnIdentifiers
	@Override
	public String getColumnName(int column) {
		return (String) this.columnIdentifiers.get(column);
	}

	@Override
	public int getColumnCount() {
		return columnIdentifiers.size();
	}

	// Add this method to remove columns
	public void removeColumn(int column) {
		try {
			// Remove from column identifiers
			columnIdentifiers.remove(column);

			// Remove from each row's data
			for (Object rowObj : dataVector) {
				if (rowObj instanceof Vector) {
					((Vector<?>) rowObj).remove(column);
				}
			}

			// Update column count by calling super's method
			// We don't need to directly modify columnCount since it's handled by the model
			fireTableStructureChanged();

			// Rebuild symbol map if needed
			if (column == 1) { // If removing symbol column
				rebuildSymbolMap();
			}

			// New: Refresh sorter (assuming dashboardTable is accessible; otherwise, expose
			// it)
			
			JTable dashboardTable = stockDashboard.getDashboardTable();
			
			if (dashboardTable != null && dashboardTable.getRowSorter() instanceof TableRowSorter) {
				TableRowSorter<FilterableTableModel> sorter = (TableRowSorter<FilterableTableModel>) dashboardTable
						.getRowSorter();
				sorter.setModel(this); // Rebind model to sorter
				sorter.modelStructureChanged(); // Force sorter update
			}

			SwingUtilities.invokeLater(() -> stockDashboard.updateTableRenderers());
		} catch (ArrayIndexOutOfBoundsException e) {
			//logToConsole("Error removing column: " + e.getMessage());
		}
	}

	public Vector<?> getColumnIdentifiers() {
		return this.columnIdentifiers;
	}

	public boolean rowSatisfiesFilters(int modelRow) {
		for (Map.Entry<String, double[]> entry : columnFilters.entrySet()) {
			String colName = entry.getKey();
			int col = findColumn(colName);
			if (col == -1)
				continue;

			Object val = super.getValueAt(modelRow, col);
			if (val == null)
				return false;

			double v;
			if (val instanceof Number) {
				v = ((Number) val).doubleValue();
			} else {
				return false;
			}

			double[] range = entry.getValue();
			double min = range[0];
			double max = range[1];

			if (v < min || v > max) {
				return false;
			}
		}
		return true;
	}
	
	private static class BreakoutValue implements Comparable<BreakoutValue> {
		final int sortValue;
		final String displayValue;

		public BreakoutValue(int sortValue, String displayValue) {
			this.sortValue = sortValue;
			this.displayValue = displayValue;
		}

		@Override
		public int compareTo(BreakoutValue o) {
			return Integer.compare(this.sortValue, o.sortValue);
		}

		@Override
		public String toString() {
			return displayValue;
		}
	}
}
