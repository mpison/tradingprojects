package com.kx;

import com.quantlabs.QuantTester.model.OhlcRecord;
import com.quantlabs.QuantTester.service.kdb.KdbService;
import com.quantlabs.QuantTester.tools.TimeSeriesConverter;
import com.kx.c;
import com.kx.c.KException;

import java.io.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

public class KdbChartApp {

	static Properties props = new Properties();

	static c conn;

	String host;

	int port;

	static String csvPath;

	{
		try (InputStream input = new FileInputStream(
				"C:\\Users\\Melchedick Pison\\Documents\\ForexProject\\ForexTester\\ForexTester\\src\\main\\resources\\config.properties")) {
			props.load(input);

			host = props.getProperty("kdb.host", "localhost");
			port = Integer.parseInt(props.getProperty("kdb.port", "5000"));
			csvPath = props.getProperty("csv.path", "C:\\Users\\Melchedick Pison\\Downloads\\FBS_EURUSD1.csv");

			conn = new c(host, port);
			System.out.println("Connected to kdb+ @ " + host + ":" + port);

		} catch (IOException e) {
			System.err.println("Failed to load config.properties");
		} catch (KException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void insertToKdb(KdbService kdbService) {

		try {

			String symbol = "EURUSD";
			String tf = "m1";
			String broker = "OANDA";
			double open = 1.1050, high = 1.1060, low = 1.1045, close = 1.1058;
			long volume = 120;
			// long tsNano = KdbTimestampConverter.stringToKdbTimestamp("2023.05.15
			// 14:30:45");//System.currentTimeMillis() * 1_000_000L;

			String date = TimeSeriesConverter.convertToKdbTimestamp("2023.05.15 14:30");

			/*
			 * String q = String.format(
			 * "insert[`chartForexDataHistory; (`%s; `%s; `%s; %f; %f; %f; %f; %d; 0Np + %d)]"
			 * , symbol, tf, broker, open, high, low, close, volume, tsNano );
			 */

			String q = String.format("insert[`chartForexDataHistory; (`%s; `%s; `%s; %f; %f; %f; %f; %d; %s)]", symbol,
					tf, broker, open, high, low, close, volume, date);

			conn.ks(q);
			System.out.println("Inserted 1 record to chartForexDataHistory.");

			/*
			 * try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
			 * String line = br.readLine(); // skip header while ((line = br.readLine()) !=
			 * null) { String[] parts = line.split(","); String symbol = parts[0]; String tf
			 * = parts[1]; String broker = parts[2]; double open =
			 * Double.parseDouble(parts[3]); double high = Double.parseDouble(parts[4]);
			 * double low = Double.parseDouble(parts[5]); double close =
			 * Double.parseDouble(parts[6]); long volume = Long.parseLong(parts[7]);
			 * 
			 * SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			 * df.setTimeZone(TimeZone.getTimeZone("UTC")); Timestamp ts = new
			 * Timestamp(df.parse(parts[8]).getTime()); long tsNano = ts.getTime() *
			 * 1_000_000L;
			 * 
			 * String q = String.format(
			 * "insert[`chart; (`%s; `%s; `%s; %f; %f; %f; %f; %d; 0Np + %d)]", symbol, tf,
			 * broker, open, high, low, close, volume, tsNano ); conn.ks(q);
			 * System.out.println("Inserted: " + parts[8]); } }
			 */

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void bulkUpsertToKdb(KdbService kdbService) throws Exception {

		BufferedReader reader = new BufferedReader(new FileReader(csvPath));
		String line;
		boolean weekend = false;

		List<String> symbolList = new ArrayList<>();
		List<String> tfList = new ArrayList<>();
		List<String> brokerList = new ArrayList<>();
		List<Double> openList = new ArrayList<>();
		List<Double> highList = new ArrayList<>();
		List<Double> lowList = new ArrayList<>();
		List<Double> closeList = new ArrayList<>();
		List<Long> volumeList = new ArrayList<>();
		List<String> tsList = new ArrayList<>();
		
		int i =0;
		
		int BATCH_SIZE = 1;

		while ((line = reader.readLine()) != null) {
			if (line.startsWith("timestamp"))
				continue;
			String[] fields = line.split(",");
			String date = fields[0];
			String time = fields[1];
			Double open = Double.valueOf(fields[2]);
			Double high = Double.valueOf(fields[3]);
			Double low = Double.valueOf(fields[4]);
			Double close = Double.valueOf(fields[5]);
			Long volume = Long.valueOf(fields[6]);

			String ts = TimeSeriesConverter.convertToKdbTimestamp(date + " " + time);

			System.out.println(ts);

			//kdbService.insertToKdb("EURUSD", "M1", "FBS", open, high, low, close, volume, ts);
			
			symbolList.add("EURUSD");
			tfList.add("M1");
			brokerList.add("FBS");
			openList.add(open);
			highList.add(high);
			lowList.add(low);
			closeList.add(close);
			volumeList.add(volume);
			tsList.add(ts);
			
			// Insert batch every 100k
            if ((i + 1) % BATCH_SIZE == 0) {
            	kdbService.insertBatch(symbolList, tfList, brokerList, openList, highList, lowList, closeList, volumeList, tsList);
            	symbolList.clear(); 
            	tfList.clear(); 
            	brokerList.clear();
            	openList.clear(); 
            	highList.clear(); 
            	lowList.clear(); 
            	closeList.clear();
            	volumeList.clear(); 
            	tsList.clear();
                System.out.println("Inserted batch: " + (i + 1));
            }
            
            i++;
		}
		
		if(symbolList.size() > 0) {
			
			kdbService.insertBatch(symbolList, tfList, brokerList, openList, highList, lowList, closeList, volumeList, tsList);
		}
	}

	public static void query(KdbService kdbService) {
		try {
			// Example query - adjust based on your table structure
			String query = "select symbol, broker, tf, ts, open, high, low, close, volume "
					+ "from chartForexDataHistory where symbol=`EURUSD, ts within (1975.05.01D00:00:00.000; 2025.05.02D00:00:00.000)";

			List<OhlcRecord> records = kdbService.executeOhlcQuery(query);

			// Process results
			var i = 0;
			for (OhlcRecord record : records) {
				System.out.println(record + " " + ++i);
				// Do something with each record
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void readFromCSVAndSaveToKDB(KdbService kdbService) throws IOException {

		BufferedReader reader = new BufferedReader(new FileReader(csvPath));
		String line;
		boolean weekend = false;

		while ((line = reader.readLine()) != null) {
			if (line.startsWith("timestamp"))
				continue;
			String[] fields = line.split(",");
			String date = fields[0];
			String time = fields[1];
			Double open = Double.valueOf(fields[2]);
			Double high = Double.valueOf(fields[3]);
			Double low = Double.valueOf(fields[4]);
			Double close = Double.valueOf(fields[5]);
			Long volume = Long.valueOf(fields[6]);

			String ts = TimeSeriesConverter.convertToKdbTimestamp(date + " " + time);

			System.out.println(ts);

			kdbService.insertToKdb("EURUSD", "M1", "FBS", open, high, low, close, volume, ts);
			/*
			 * LocalDate candleDate = LocalDate.parse(timestamp.substring(0, 10)); DayOfWeek
			 * candleDay = candleDate.getDayOfWeek(); if (candleDay == DayOfWeek.SATURDAY ||
			 * candleDay == DayOfWeek.SUNDAY) { weekend = true; continue; } if (weekend) {
			 * System.out.println("Market gap after weekend: " + timestamp); weekend =
			 * false; }
			 * 
			 * double open = Double.parseDouble(fields[1]); if (weekend) { Random rand = new
			 * Random(); int pipGap = rand.nextInt(5) + 1; boolean gapUp =
			 * rand.nextBoolean(); double slippage = pipGap / 10000.0; open += gapUp ?
			 * slippage : -slippage; System.out.
			 * printf("Simulated slippage applied: %s by %d pips. Adjusted open = %.5f\n",
			 * gapUp ? "upward gap" : "downward gap", pipGap, open); }
			 * 
			 * Candle candle = new Candle(timestamp, open, Double.parseDouble(fields[2]),
			 * Double.parseDouble(fields[3]), Double.parseDouble(fields[4]));
			 * 
			 * Trade trade = strategy.evaluate(candle); if (trade != null) { double
			 * marginRequired = trade.getMarginRequired(); if (balance >= marginRequired) {
			 * trades.add(trade); balance += trade.profit; } else { System.out.
			 * printf("Trade skipped due to insufficient margin. Required: %.2f USD, Available: %.2f USD\n"
			 * , marginRequired, balance); } }
			 */
		}
	}

	public static void main(String[] args) throws Exception {
		KdbChartApp kdbChartApp = new KdbChartApp();

		//
		KdbService kdbService = new KdbService(kdbChartApp.host, kdbChartApp.port, "", "");

		bulkUpsertToKdb(kdbService);
		
		// insertToKdb(kdbService);
		//query(kdbService);

		// readFromCSVAndSaveToKDB(kdbService);
	}
}