package com.quantlabs.QuantTester.repository.questdb;

import io.questdb.client.Sender;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import com.quantlabs.QuantTester.model.questdb.OHCL;
import com.quantlabs.QuantTester.tools.TimeSeriesConverter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Repository
public class OHCLRepository {

	private final Sender sender;
	private final JdbcTemplate jdbcTemplate;

	private static String OHCL_TABLE_NAME = "OHCL";

	public OHCLRepository(Sender sender, JdbcTemplate jdbcTemplate) {
		this.sender = sender;
		this.jdbcTemplate = jdbcTemplate;
	}

	public void insert(String broker, String timeframe, String symbol, List<OHCL> dataList) throws Exception {

		String tableName = getTableNameBy(broker, timeframe, symbol);

		String sql = """
				INSERT INTO """ + tableName + """
				    (symbol, tf, broker, open, high, low, close, volume, ts)
				    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
				""";

		for (OHCL data : dataList) {
			jdbcTemplate.update(sql, data.getSymbol(), data.getTf(), data.getBroker(), data.getOpen(), data.getHigh(),
					data.getLow(), data.getClose(), data.getVolume(), data.getTimestamp());
		}

	}

	public void insertBulk(String broker, String timeframe, String symbol, List<OHCL> dataList) throws Exception {

		String tableName = getTableNameBy(broker, timeframe, symbol);

		String sql = """
				INSERT INTO %s
				    (symbol, tf, broker, open, high, low, close, volume, ts)
				    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
				""".formatted(tableName);

		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			private final List<OHCL> batch = List.copyOf(dataList);

			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {

				OHCL data = batch.get(i);

				Timestamp tmp = TimeSeriesConverter.convertToQuestDBTimestamp2(data.getTimestamp());

				ps.setString(1, data.getSymbol());
				ps.setString(2, data.getTf());
				ps.setString(3, data.getBroker());
				ps.setFloat(4, data.getOpen());
				ps.setFloat(5, data.getHigh());
				ps.setFloat(6, data.getLow());
				ps.setFloat(7, data.getClose());
				ps.setLong(8, data.getVolume());
				ps.setTimestamp(9, tmp);
			}

			@Override
			public int getBatchSize() {
				return batch.size();
			}
		});
	}

	@SuppressWarnings("deprecation")
	public List<OHCL> getBySymbol(String broker, String timeframe, String symbol) {

		String tableName = getTableNameBy(broker, timeframe, symbol);

		return jdbcTemplate.query("SELECT * FROM " + tableName + " WHERE symbol = ? ORDER BY ts DESC LIMIT 100",
				new Object[] { symbol },
				(rs, rowNum) -> new OHCL(rs.getString("symbol"), rs.getString("tf"), rs.getString("broker"),
						rs.getFloat("open"), rs.getFloat("high"), rs.getFloat("low"), rs.getFloat("close"),
						rs.getLong("volume"),
						TimeSeriesConverter.convertLocaleTimeToJavaStrFormat(rs.getTimestamp("ts").toLocalDateTime())));
	}

	public int update(OHCL updatedData) {

		String tableName = getTableNameBy(updatedData.getBroker(), updatedData.getTf(), updatedData.getSymbol());

		return jdbcTemplate.update("""
				UPDATE ?
				SET open = ?, high = ?, low = ?, close = ?, volume = ?
				WHERE symbol = ? AND tf = ? AND broker = ? AND ts = ?
				""", tableName, updatedData.getOpen(), updatedData.getHigh(), updatedData.getLow(),
				updatedData.getClose(), updatedData.getVolume(), updatedData.getSymbol(), updatedData.getTf(),
				updatedData.getBroker(), updatedData.getTimestamp());
	}

	public int delete(String symbol, String tf, String broker, String timestamp) {

		String tableName = getTableNameBy(broker, tf, symbol);

		return jdbcTemplate.update("""
				DELETE FROM ?
				WHERE symbol = ? AND tf = ? AND broker = ? AND ts = ?
				""", tableName, symbol, tf, broker, timestamp);
	}

	public String getTableNameBy(String broker, String timeframe, String symbol) {
		return broker + "_" + symbol + "_" + timeframe + "_" + OHCL_TABLE_NAME;
	}

	public boolean isOHCLTableExistBy(String broker, String timeframe, String symbol) {
		boolean returnVal = false;

		String tableName = getTableNameBy(broker, timeframe, symbol);

		returnVal = jdbcTemplate
				.queryForList("SELECT table_name FROM tables() WHERE table_name = ?", String.class, tableName)
				.size() > 0;

		if (returnVal) {
			System.out.println("✅ Table exists: " + tableName);
		} else {
			System.out.println("❌ Table does not exist: " + tableName);
		}

		return returnVal;

	}

	public boolean createOHCLTableBy(String broker, String timeframe, String symbol) {

		boolean returnVal = false;

		if (!isOHCLTableExistBy(broker, timeframe, symbol)) {

			String tableName = getTableNameBy(broker, timeframe, symbol);

			// SQL to create table
			String createTableSql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" + "   symbol SYMBOL CACHE, "
					+ "   tf SYMBOL CACHE, " + "   broker SYMBOL CACHE, " + "   open FLOAT, " + "   high FLOAT, "
					+ "   low FLOAT,  " + "   close FLOAT, " + "   volume LONG, " + "   ts TIMESTAMP, "
					+ "   is_deleted BOOLEAN " + ") TIMESTAMP(ts);";

			try {
				jdbcTemplate.execute(createTableSql);
				System.out.println("✅ Table created or already exists.");
				returnVal = true;
			} catch (Exception e) {
				System.err.println("❌ Error creating table: " + e.getMessage());
			}

			return returnVal;
		} else {

			return true;
		}

	}

	public void convertM1ToOtherTimeFramesBy(String broker, String timeframe, String symbol) {

		// Define timeframes to convert to
		String[] timeframes = { "M5", "M10", "M15", "M30", "H1", "H4", "D1", "W1", "MN1" };

		for (String tf : timeframes) {
			if (createOHCLTableBy(broker, tf, symbol)) {
				convertM1To(broker, "M1", symbol, tf);
				System.out.println("✅ Converted: " + tf);
			}
		}

	}

	private void convertM1To(String broker, String timeframe, String symbol, String targetTf) {

		String interval = mapTimeframeToInterval(targetTf);

		if (interval == null) {
			System.out.println("⛔ Unsupported timeframe: " + targetTf);
			return;
		}

		System.out.println("▶ Converting M1 to " + targetTf);

		String tableNameFromTf = getTableNameBy(broker, timeframe, symbol);

		String tableNameTargetTf = getTableNameBy(broker, targetTf, symbol);

		// Convert and insert if ts doesn't already exist for that tf
		String sql = """
				    INSERT INTO %s
				    SELECT
				        symbol,
				        '%s' as tf,
				        broker,
				        FIRST(open) as open,
				        MAX(high) as high,
				        MIN(low) as low,
				        LAST(close) as close,
				        SUM(volume) as volume,
				        ts,
				        FALSE as is_deleted
				    FROM %s 
				    WHERE tf = 'M1' 
				    SAMPLE BY %s ALIGN TO CALENDAR
				""".formatted(tableNameTargetTf, targetTf, tableNameFromTf, interval);

		try {
			jdbcTemplate.execute(sql);
			System.out.println("✅ Converted to " + targetTf);
		} catch (Exception e) {
			System.err.println("❌ Error converting to " + targetTf + ": " + e.getMessage());
		}
	}

	private String mapTimeframeToInterval(String tf) {
		return switch (tf) {
		case "M5" -> "5m";
		case "M10" -> "10m";
		case "M15" -> "15m";
		case "M30" -> "30m";
		case "H1" -> "1h";
		case "H4" -> "4h";
		case "D1" -> "1d";
		case "W1" -> "1w";
		case "MN1" -> "1M";
		default -> null;
		};
	}

}