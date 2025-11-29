package com.quantlabs.QuantTester.v4.alert;


import java.time.format.DateTimeFormatter;

public final class Constants {
    public static final DateTimeFormatter TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Constants() {} // Prevent instantiation
}