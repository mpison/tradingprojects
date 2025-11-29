package com.quantlabs.QuantTester.tools;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

public class TimeSeriesConverter {
	private static final SimpleDateFormat TS_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm");
    private static final long KDB_EPOCH_OFFSET = 946684800000L; // Milliseconds from 1970 to 2000
    
    static {
        TS_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    // Convert 'yyyy.mm.dd hh:mm:ss' to KDB+ nanoseconds
    public static long stringToKdbTimestamp(String formatted) throws Exception {
        Date date = TS_FORMAT.parse(formatted);
        long javaMillis = date.getTime();
        long kdbNanos = (javaMillis - KDB_EPOCH_OFFSET) * 1_000_000L;
        return kdbNanos;
    }

    // Convert KDB+ nanoseconds to 'yyyy.mm.dd hh:mm:ss'
    public static String kdbTimestampToString(long kdbNanos) {
        long javaMillis = (kdbNanos / 1_000_000L) + KDB_EPOCH_OFFSET;
        return TS_FORMAT.format(new Date(javaMillis));
    }
    
    public static String convertToKdbTimestamp(String javaFormatDateTime) {
        // Parse the input string using Java's format
        DateTimeFormatter javaFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
        LocalDateTime dateTime = LocalDateTime.parse(javaFormatDateTime, javaFormatter);
        
        // Format to KDB+ timestamp format
        DateTimeFormatter kdbFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd'T'HH:mm:ss.000000000");
        String kdbTimestamp = dateTime.format(kdbFormatter)
                              .replace("T", "D"); // Replace T with D for KDB+ format
        
        return kdbTimestamp;
    }
    
    public static String convertKdbToJava(String kdbTimestamp) {
        // Replace 'D' with 'T' to make it ISO-8601 compatible
        String isoFormat = kdbTimestamp.replace('D', 'T');
        
        // Parse the KDB+ timestamp
        DateTimeFormatter kdbFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd'T'HH:mm:ss.nnnnnnnnn");
        LocalDateTime dateTime = LocalDateTime.parse(isoFormat, kdbFormatter);
        
        // Format to Java's desired output
        DateTimeFormatter javaFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
        return dateTime.format(javaFormatter);
    }
    
    // Formatter for KDB+ compatible date-time format
    private static final DateTimeFormatter TS_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm") .withZone(ZoneId.of("UTC"));;
    
    public static String convertInstantToJavaStrFormat(Instant instant) {
        return TS_FORMATTER.format(instant);
    }
    
    public static String convertLocaleTimeToJavaStrFormat(LocalDateTime ldt) {
    	
    	return ldt.format(TS_FORMATTER);
    }
    
    public static String convertToQuestDBTimestamp(String javaFormatDateTime) {
        // Parse the input string using Java's format
        DateTimeFormatter javaFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
        LocalDateTime dateTime = LocalDateTime.parse(javaFormatDateTime, javaFormatter);
        
        // Format to KDB+ timestamp format
        DateTimeFormatter kdbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.000000000");
        String kdbTimestamp = dateTime.format(kdbFormatter)
                              .replace("T", "D"); // Replace T with D for KDB+ format
        
        return kdbTimestamp;
    }
    
    public static Timestamp convertToQuestDBTimestamp2(String javaFormatDateTime) {
        // Parse the input string using Java's format
        DateTimeFormatter javaFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
        LocalDateTime dateTime = LocalDateTime.parse(javaFormatDateTime, javaFormatter);
        
        
        return Timestamp.from(dateTime.atZone(ZoneOffset.UTC).toInstant());
        
		/*
		 * // Format to KDB+ timestamp format DateTimeFormatter kdbFormatter =
		 * DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.000000000"); String
		 * kdbTimestamp = dateTime.format(kdbFormatter) .replace("T", "D"); // Replace T
		 * with D for KDB+ format
		 */        
        //return kdbTimestamp;
    }

    
    public static void main(String[] args) throws Exception {
        // 1. Convert formatted string to KDB timestamp
        String formatted = "2023.05.15 14:30:45";
        long kdbTs = stringToKdbTimestamp(formatted);
        System.out.println("KDB timestamp (ns): " + kdbTs);
        
        // 2. Convert back to formatted string
        String convertedBack = kdbTimestampToString(kdbTs);
        System.out.println("Formatted string: " + convertedBack);
        
        // 3. Current time conversions
        long currentKdbTs = System.currentTimeMillis() * 1_000_000L - 
                           (KDB_EPOCH_OFFSET * 1_000_000L);
        System.out.println("Current time formatted: " + 
            kdbTimestampToString(currentKdbTs));
    }
}
