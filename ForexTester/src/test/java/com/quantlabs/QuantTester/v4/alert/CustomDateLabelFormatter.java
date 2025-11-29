package com.quantlabs.QuantTester.v4.alert;

import javax.swing.JFormattedTextField;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Calendar;

public class CustomDateLabelFormatter extends JFormattedTextField.AbstractFormatter {
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public String valueToString(Object value) throws ParseException {
        if (value != null) {
            Calendar cal = (Calendar) value;
            return dateFormatter.format(cal.getTime());
        }
        return "";
    }

    @Override
    public Object stringToValue(String text) throws ParseException {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(dateFormatter.parse(text));
        return cal;
    }
}