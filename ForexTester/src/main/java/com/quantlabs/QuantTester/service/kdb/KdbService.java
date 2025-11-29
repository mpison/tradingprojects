package com.quantlabs.QuantTester.service.kdb;

import java.util.List;
import java.util.Map;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.kx.c;
import com.kx.c.KException;
import com.quantlabs.QuantTester.model.OhlcRecord;
import com.quantlabs.QuantTester.tools.TimeSeriesConverter;

public class KdbService {
	private final String host;
	private final int port;
	private final String username;
	private final String password;

	c connection;

	public KdbService(String host, int port, String username, String password) throws KException, IOException {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;

		// Connect to KDB+

		if (username.equals("") || password.equals("")) {
			this.connection = new c(host, port);
		} else {
			this.connection = new c(host, port, username + ":" + password);
		}
	}

	public List<OhlcRecord> executeOhlcQuery(String query) throws Exception {

		List<OhlcRecord> results = new ArrayList<>();

		try {

			// Execute query
			Object result = this.connection.k(query);

			// Object result = null;

			// Process the result
			if (result instanceof c.Flip) { // Handle table (Flip)
				c.Flip flip = (c.Flip) result;
				String[] columnNames = flip.x;
				Object[] columnData = flip.y;
				int rowCount = c.n(columnData[0]); // Get row count from the first column

				for (int i = 0; i < rowCount; i++) {
					// Map<String, Object> row = new HashMap<>();

					OhlcRecord ohclrecord = new OhlcRecord();

					for (int j = 0; j < columnNames.length; j++) {
						String columnName = columnNames[j];
						Object column = columnData[j];

						// setOhlcField(ohclrecord, columnName, data[i]);

						if (column instanceof Object[]) {
							setOhlcField(ohclrecord, columnName, ((Object[]) column)[i]);
						} else if (column instanceof int[]) {
							setOhlcField(ohclrecord, columnName, ((int[]) column)[i]);
							// row.put(columnName, ((int[]) column)[i]);
						} else if (column instanceof long[]) {
							setOhlcField(ohclrecord, columnName, ((long[]) column)[i]);
							// row.put(columnName, );
						} else if (column instanceof double[]) {
							// row.put(columnName, ((double[]) column)[i]);
							setOhlcField(ohclrecord, columnName, ((double[]) column)[i]);
						} else if (column instanceof boolean[]) {
							// row.put(columnName, ((boolean[]) column)[i]);
							setOhlcField(ohclrecord, columnName, ((boolean[]) column)[i]);
						} else if (column instanceof String[]) {
							// row.put(columnName, ((String[]) column)[i]);
							setOhlcField(ohclrecord, columnName, ((String[]) column)[i]);
						} else if (column instanceof Date[]) {
							// row.put(columnName, ((Date[]) column)[i]);
							setOhlcField(ohclrecord, columnName, ((Date[]) column)[i]);
						} else if (column instanceof Instant[]) {
							// row.put(columnName, ((Date[]) column)[i]);
							setOhlcField(ohclrecord, columnName, ((Instant[]) column)[i]);
						} else {
							// row.put(columnName, column);
							setOhlcField(ohclrecord, columnName, column);
						}

					}
					results.add(ohclrecord);
				}
			} else if (result instanceof Object[]) { // general list
				/*
				 * for (Object o : (Object[]) result) { Map<String, Object> row = new
				 * HashMap<>(); row.put("result", o); //resultList.add(row); }
				 */

			} else if (result instanceof c.Dict) {
				// scalar
				/*
				 * Map<String, Object> row = new HashMap<>(); row.put("result", result);
				 * //resultList.add(row);
				 */ }

			return results;
		} finally {
			if (connection != null) {
				connection.close();
			}
		}
	}

	private void setOhlcField(OhlcRecord record, String fieldName, Object value) {
		try {
			switch (fieldName.toLowerCase()) {
			case "ts":
				if (value instanceof Instant) {
					// Convert KDB+ nanoseconds since 2000.01.01 to LocalDateTime
					// long nanosSince2000 = (Long) value;
					// LocalDateTime epoch = LocalDateTime.of(2000, 1, 1, 0, 0);

					String ts = TimeSeriesConverter.convertInstantToJavaStrFormat((Instant) value);
					record.setTimestamp(ts);
				}
				break;
			case "tf":
				record.setTf(value.toString());
				break;
			case "symbol":
				record.setSymbol(value.toString());
				break;
			case "broker":
				record.setBroker(value.toString());
				break;
			case "open":
				record.setOpen(((Number) value).doubleValue());
				break;
			case "high":
				record.setHigh(((Number) value).doubleValue());
				break;
			case "low":
				record.setLow(((Number) value).doubleValue());
				break;
			case "close":
				record.setClose(((Number) value).doubleValue());
				break;
			case "spread":
				record.setSpread(((Number) value).doubleValue());
				break;
			case "ticks":
				record.setTicks(((Number) value).longValue());
				break;
			case "liquidity":
				record.setLiquidity(((Number) value).doubleValue());
				break;
			}
		} catch (Exception e) {
			System.err.println("Error mapping field " + fieldName + ": " + e.getMessage());
		}
	}

	public void insertToKdb(String symbol, String tf, String broker, double open, double high, double low, double close,
			long volume, String date) {

		try {

			/*
			 * String symbol = "EURUSD"; String tf = "m1"; String broker = "OANDA"; double
			 * open = 1.1050, high = 1.1060, low = 1.1045, close = 1.1058; long volume =
			 * 120;
			 */
			// long tsNano = KdbTimestampConverter.stringToKdbTimestamp("2023.05.15
			// 14:30:45");//System.currentTimeMillis() * 1_000_000L;

			// String date = KdbTimestampConverter.convertToKdbTimestamp("2023.05.15
			// 14:30");

			/*
			 * String q = String.format(
			 * "insert[`chartForexDataHistory; (`%s; `%s; `%s; %f; %f; %f; %f; %d; 0Np + %d)]"
			 * , symbol, tf, broker, open, high, low, close, volume, tsNano );
			 */

			String q = String.format("insert[`chartForexDataHistory; (`%s; `%s; `%s; %f; %f; %f; %f; %d; %s)]", symbol,
					tf, broker, open, high, low, close, volume, date);

			connection.ks(q);
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

	public void insertBatch(List<String> symbol, List<String> tf, List<String> broker, List<Double> open, List<Double> high,
			List<Double> low, List<Double> close, List<Long> volume, List<String> ts) throws Exception {

		String[] tsArr = ts.toArray(new String[0]);

		c.Dict batch = new c.Dict(
				new String[] { "symbol", "tf", "broker", "open", "high", "low", "close", "volume", "ts" },
				new Object[] { symbol.toArray(new String[0]), tf.toArray(new String[0]), broker.toArray(new String[0]),
						toPrimitiveDoubleArray(open), toPrimitiveDoubleArray(high), toPrimitiveDoubleArray(low),
						toPrimitiveDoubleArray(close), toPrimitiveLongArray(volume), tsArr });

		connection.k("insert", "ohlc", batch);
		
		//System.out.println(response);
	}

	static double[] toPrimitiveDoubleArray(List<Double> list) {
		double[] result = new double[list.size()];
		for (int i = 0; i < list.size(); i++)
			result[i] = list.get(i);
		return result;
	}

	static long[] toPrimitiveLongArray(List<Long> list) {
		long[] result = new long[list.size()];
		for (int i = 0; i < list.size(); i++)
			result[i] = list.get(i);
		return result;
	}

}