package com.quantlabs.QuantTester.v3.alert.ui;

import com.quantlabs.QuantTester.v3.alert.Message;
import javax.swing.table.DefaultTableModel;
import java.util.List;

public class AlertTableModel extends DefaultTableModel {
    private static final String[] COLUMN_NAMES = {
        "", // Checkbox
        "Alert", // Header with combination name and symbol
        "Message", // Partial body
        "Status" // Message status
    };
    
    private List<Message> messages;

    public AlertTableModel() {
        super(COLUMN_NAMES, 0);
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        fireTableDataChanged();
    }

    public List<Message> getMessages() {
		return messages;
	}

	@Override
    public int getRowCount() {
        return messages != null ? messages.size() : 0;
    }

    @Override
    public Object getValueAt(int row, int column) {
        if (messages == null || row >= messages.size()) {
            return null;
        }
        
        Message message = messages.get(row);
        
        switch (column) {
            case 0: // Checkbox column
                return false; // Default unchecked
            case 1: // Header column
                return extractHeader(message.getHeader());
            case 2: // Message preview column
                return getMessagePreview(message.getBody());
            case 3: // Status column
                return message.getStatus().toString();
            default:
                return null;
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) {
            return Boolean.class; // Checkbox column
        }
        return String.class; // All other columns
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return column == 0 || column == 3; // Only checkbox and status columns are editable
    }

    @Override
    public void setValueAt(Object aValue, int row, int column) {
        Message message = messages.get(row);
        
        if (column == 0) {
            // Checkbox changed - handled by controller
            fireTableCellUpdated(row, column);
        } else if (column == 3) {
            // Status changed
            message.setStatus(Message.MessageStatus.valueOf(aValue.toString()));
            fireTableCellUpdated(row, column);
        }
    }

    private String extractHeader(String fullHeader) {
        // Example format: "[2023-01-01 12:00:00] AAPL: Alert triggered"
        // We want to extract the symbol and combination name
        String[] parts = fullHeader.split(" ");
        if (parts.length >= 3) {
            return parts[2].replace(":", "") + " - " + 
                   fullHeader.substring(fullHeader.indexOf(":") + 2);
        }
        return fullHeader;
    }

    private String getMessagePreview(String fullBody) {
        // Return first 50 characters of the message
        return fullBody.length() > 50 ? 
               fullBody.substring(0, 50) + "..." : 
               fullBody;
    }
}